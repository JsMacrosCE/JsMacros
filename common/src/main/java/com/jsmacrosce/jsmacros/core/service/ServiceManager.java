package com.jsmacrosce.jsmacros.core.service;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jetbrains.annotations.NotNull;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.Pair;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.classes.Registrable;
import com.jsmacrosce.jsmacros.core.config.CoreConfigV2;
import com.jsmacrosce.jsmacros.core.event.BaseEventRegistry;
import com.jsmacrosce.jsmacros.core.event.IEventListener;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.core.library.impl.FJsMacros;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages registered script services, their running contexts, and reloads triggered by file changes.
 * @author Wagyourtail
 * @since 1.6.3
 */
public class ServiceManager {
    private static final Set<Object> autoUnregisterKeepAlive = new HashSet<>();
    protected boolean reloadOnModify;
    protected final Object2LongMap<String> lastModifiedMap = new Object2LongArrayMap<>();
    protected final Set<String> crashedServices = new HashSet<>();

    protected final Core<?, ?> runner;
    protected final Map<String, Pair<ServiceTrigger, EventContainer<?>>> registeredServices = new LinkedHashMap<>();
    private final Map<BaseScriptContext<?>, Thread> stoppingContexts = new ConcurrentHashMap<>();

    /** Stores the core used for subsequent service operations; this constructor does not register or start services.
     * @param runner core that owns this manager
     */
    public ServiceManager(Core<?, ?> runner) {
        this.runner = runner;
    }

    /** Registers a service from a file under the macro folder and starts it immediately.
     * @param name service name
     * @param pathToFile path relative to the macro folder
     * @return {@code false} if a service with this name is already registered
     */
    public synchronized boolean registerService(String name, String pathToFile) {
        return registerService(name, new ServiceTrigger(runner.config.macroFolder.getAbsoluteFile().toPath().resolve(pathToFile), true));
    }

    /** Registers a service from a file under the macro folder, optionally starting it immediately.
     * @param name service name
     * @param pathToFile path relative to the macro folder
     * @param enabled whether to start the service immediately
     * @return {@code false} if a service with this name is already registered
     */
    public synchronized boolean registerService(String name, String pathToFile, boolean enabled) {
        return registerService(name, new ServiceTrigger(runner.config.macroFolder.getAbsoluteFile().toPath().resolve(pathToFile), enabled));
    }

    /** Registers a service with the given trigger and starts it if the trigger is enabled.
     * @param name service name
     * @param trigger service trigger configuration
     * @return {@code false} if a service with this name is already registered
     */
    public synchronized boolean registerService(String name, ServiceTrigger trigger) {
        if (registeredServices.containsKey(name)) {
            return false;
        }
        registeredServices.put(name, new Pair<>(trigger, null));
        if (trigger.enabled) {
            startService(name);
        }
        return true;
    }

    /** Stops and unregisters a service if present.
     * @param name service name
     * @return {@code true} if a service was registered and removed
     */
    public synchronized boolean unregisterService(String name) {
        stopService(name);
        disableReload(name);
        return registeredServices.remove(name) != null;
    }

    /**
     * Removes cached crash and file-timestamp state for the registered service; this does not stop it.
     * @param serviceName the name of the service to disable the reload feature for
     * @since 1.8.4
     */
    public synchronized void disableReload(String serviceName) {
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(serviceName);
        if (service == null) {
            return;
        }
        crashedServices.remove(serviceName);
        lastModifiedMap.removeLong(service.getT().file);
    }

    /**
     * Renames a registered service, preserving its trigger and running context.
     * @param oldName current service name
     * @param newName replacement service name
     * @return {@code false} if the new name is already registered or the old name is unknown
     */
    public synchronized boolean renameService(String oldName, String newName) {
        if (registeredServices.containsKey(newName)) {
            return false;
        }
        if (crashedServices.contains(oldName)) {
            crashedServices.remove(oldName);
            crashedServices.add(newName);
        }
        if (lastModifiedMap.containsKey(oldName)) {
            lastModifiedMap.put(newName, lastModifiedMap.removeLong(oldName));
        }
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.remove(oldName);
        if (service == null) {
            return false;
        }
        registeredServices.put(newName, service);
        return true;
    }

    /**
     * @return an immutable snapshot of the registered service names
     */
    public synchronized Set<String> getServices() {
        return ImmutableSet.copyOf(registeredServices.keySet());
    }

    /**
     * Starts a service if it is not already running.
     *
     * @param name service name
     * @return {@link ServiceStatus#UNKNOWN} if unknown; otherwise {@link ServiceStatus#STOPPED} if this call starts it, or {@link ServiceStatus#RUNNING} if it was already running
     */
    public synchronized ServiceStatus startService(String name) {
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(name);
        if (service == null) {
            return ServiceStatus.UNKNOWN;
        }
        if (service.getU() == null || service.getU().getCtx().isContextClosed()) {
            EventService event = new EventService(runner, name);
            EventContainer<?> container = runner.exec(service.getT().toScriptTrigger(), event);
            service.setU(container);
            event.ctx = container.getCtx();
            return ServiceStatus.STOPPED;
        }
        return ServiceStatus.RUNNING;
    }

    /**
     * Stops a running service, invoking its stop callbacks and unregistering its event listeners and registrables.
     * @param name service name
     * @return {@link ServiceStatus#UNKNOWN} if unknown; otherwise {@link ServiceStatus#RUNNING} if this call stopped it, or {@link ServiceStatus#STOPPED} if it was already stopped
     */
    public synchronized ServiceStatus stopService(String name) {
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(name);
        if (service == null) {
            return ServiceStatus.UNKNOWN;
        }
        if (service.getU() == null) {
            return ServiceStatus.STOPPED;
        }
        BaseScriptContext<?> ctx = service.getU().getCtx();
        if (ctx.isContextClosed()) {
            return ServiceStatus.STOPPED;
        }
        // A stop callback may itself request that its service stop.
        if (stoppingContexts.putIfAbsent(ctx, Thread.currentThread()) != null) return ServiceStatus.RUNNING;

        try {
            return finishStoppingService(ctx);
        } finally {
            stoppingContexts.remove(ctx);
        }
    }

    private ServiceStatus finishStoppingService(BaseScriptContext<?> ctx) {
        EventService event = (EventService) ctx.getTriggeringEvent();

        try {
            MethodWrapper<?, ?, ?, ?> sl = event.stopListener;
            if (sl != null) sl.run();
        } catch (Throwable t) {
            runner.profile.logError(t);
        }

        BaseEventRegistry reg = runner.eventRegistry;
        Map<IEventListener, String> listeners;
        synchronized (ctx) {
            listeners = new HashMap<>(ctx.eventListeners);
        }
        if (event.offEventsOnStop) {
            for (Map.Entry<IEventListener, String> ent : listeners.entrySet()) {
                reg.removeListener(ent.getValue(), ent.getKey());
            }
        } else {
            for (Map.Entry<IEventListener, String> ent : listeners.entrySet()) {
                IEventListener listener = ent.getKey();
                if (!(listener instanceof FJsMacros.ScriptEventListener sel)) continue;
                MethodWrapper<?, ?, ?, ?> wrapper = sel.getWrapper();
                if (wrapper == null || wrapper.getCtx() != ctx) continue;
                reg.removeListener(ent.getValue(), ent.getKey());
            }
        }

        Registrable<?>[] list = event.registrableList;
        if (list != null) {
            for (Registrable<?> e : list) {
                try {
                    e.unregister();
                } catch (Throwable t) {
                    runner.profile.logError(t);
                }
            }
        }

        try {
            MethodWrapper<?, ?, ?, ?> psl = event.postStopListener;
            if (psl != null) psl.run();
        } catch (Throwable t) {
            runner.profile.logError(t);
        }

        synchronized (autoUnregisterKeepAlive) {
            autoUnregisterKeepAlive.remove(ctx.getSyncObject());
            if (event.ctx != null && event.ctx != ctx) { // should not happen but just in case
                autoUnregisterKeepAlive.remove(event.ctx.getSyncObject());
            }
        }

        if (ctx.isScriptStopRequested()) {
            ctx.removeScriptListeners();
            disableReload(event.serviceName);
        }
        ctx.closeContext();
        return ServiceStatus.RUNNING;
    }

    /** Stops the service only if the supplied context is its current run. */
    @DocletIgnore
    public synchronized boolean stopServiceContext(BaseScriptContext<?> ctx) {
        if (!(ctx.getTriggeringEvent() instanceof EventService event)) return false;
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(event.serviceName);
        if (service == null || service.getU() == null || service.getU().getCtx() != ctx) return false;
        stopService(event.serviceName);
        return true;
    }

    /** Checks for a stop request made from this service's own stop hook. */
    @DocletIgnore
    public boolean isStoppingOnCurrentThread(BaseScriptContext<?> ctx) {
        return stoppingContexts.get(ctx) == Thread.currentThread();
    }

    /** Adds or removes the context's sync object from the shared auto-unregister keep-alive set.
     * @param ctx context whose sync object is tracked
     * @param keepAlive {@code true} to add the sync object; {@code false} to remove it
     */
    public static void setAutoUnregisterKeepAlive(@NotNull BaseScriptContext<?> ctx, boolean keepAlive) {
        synchronized (autoUnregisterKeepAlive) {
            if (keepAlive) {
                autoUnregisterKeepAlive.add(ctx.getSyncObject());
            } else {
                autoUnregisterKeepAlive.remove(ctx.getSyncObject());
            }
        }
    }

    /** Checks whether a service-triggered context's non-null sync object is in the shared keep-alive set.
     * @param ctx context to check
     * @return {@code true} only when the triggering event is an {@link EventService} and its non-null sync object is in the set
     */
    public static boolean hasKeepAlive(@NotNull BaseScriptContext<?> ctx) {
        if (!(ctx.getTriggeringEvent() instanceof EventService)) return false;
        Object syncObject = ctx.getSyncObject();
        return syncObject != null && autoUnregisterKeepAlive.contains(syncObject);
    }

    /**
     * Stops the service, then starts it again if registered, even if disabled.
     * @param name service name
     * @return the result of stopping the service, or {@link ServiceStatus#UNKNOWN} if unregistered; a stopped service returns {@link ServiceStatus#STOPPED} even though it is then started
     */
    public synchronized ServiceStatus restartService(String name) {
        ServiceStatus state = stopService(name);
        startService(name);
        return state;
    }

    /**
     * Enables a registered service without starting it.
     * @param name service name
     * @return {@link ServiceStatus#UNKNOWN} if unregistered; otherwise {@link ServiceStatus#DISABLED} if this call enabled it, or {@link ServiceStatus#ENABLED} if already enabled
     */
    public synchronized ServiceStatus enableService(String name) {
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(name);
        if (service == null) {
            return ServiceStatus.UNKNOWN;
        }
        if (!service.getT().enabled) {
            service.getT().enabled = true;
            return ServiceStatus.DISABLED;
        }
        return ServiceStatus.ENABLED;
    }

    /**
     * Disables a registered service without stopping it.
     * @param name service name
     * @return {@link ServiceStatus#UNKNOWN} if unregistered; otherwise {@link ServiceStatus#ENABLED} if this call disabled it, or {@link ServiceStatus#DISABLED} if already disabled
     */
    public synchronized ServiceStatus disableService(String name) {
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(name);
        if (service == null) {
            return ServiceStatus.UNKNOWN;
        }
        disableReload(name);
        if (service.getT().enabled) {
            service.getT().enabled = false;
            return ServiceStatus.ENABLED;
        }
        return ServiceStatus.DISABLED;
    }

    /**
     * Returns whether the registered service's context is open, regardless of its enabled state.
     * @param name service name
     * @return {@code true} if registered with an open context; {@code false} otherwise
     * @since 1.8.4
     */
    public synchronized boolean isRunning(String name) {
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(name);
        if (service == null) {
            return false;
        }
        ServiceStatus status = status(name);
        return status == ServiceStatus.RUNNING || status == ServiceStatus.ENABLED;
    }

    /**
     * Returns whether a registered service is enabled.
     * @param name service name
     * @return {@code false} if unregistered or disabled; {@code true} if enabled
     * @since 1.8.4
     */
    public synchronized boolean isEnabled(String name) {
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(name);
        return service != null && service.getT().enabled;
    }

    /**
     * @param name service name
     * @return {@link ServiceStatus#UNKNOWN} if unregistered; otherwise {@link ServiceStatus#ENABLED} or {@link ServiceStatus#STOPPED} when enabled and respectively running or stopped, or {@link ServiceStatus#RUNNING} or {@link ServiceStatus#DISABLED} when disabled and respectively running or stopped
     */
    public synchronized ServiceStatus status(String name) {
        Pair<ServiceTrigger, EventContainer<?>> service = registeredServices.get(name);
        if (service == null) {
            return ServiceStatus.UNKNOWN;
        }
        if (service.getT().enabled) {
            if (service.getU() == null || service.getU().getCtx().isContextClosed()) {
                return ServiceStatus.STOPPED;
            }
            return ServiceStatus.ENABLED;
        }
        if (service.getU() == null || service.getU().getCtx().isContextClosed()) {
            return ServiceStatus.DISABLED;
        }
        return ServiceStatus.RUNNING;
    }

    /**
     * Returns the triggering event for a registered service's current context; the service must be running.
     *
     * @param name registered service name
     * @return the service event
     * @since 1.6.5
     */
    public EventService getServiceData(String name) {
        return (EventService) registeredServices.get(name).getU().getCtx().getTriggeringEvent();
    }

    /**
     * Returns the trigger configuration for a registered service.
     * @param name registered service name
     * @return the service trigger
     * @since 1.6.5 [named getServiceData previously]
     */
    public ServiceTrigger getTrigger(String name) {
        return registeredServices.get(name).getT();
    }

    /**
     * Loads configured services, registering missing services and updating existing triggers.
     */
    public synchronized void load() {
        Map<String, ServiceTrigger> configTriggers = runner.config.getOptions(CoreConfigV2.class).services;
        for (Map.Entry<String, Pair<ServiceTrigger, EventContainer<?>>> registered : registeredServices.entrySet()) {
            if (!configTriggers.containsKey(registered.getKey()) || !configTriggers.get(registered.getKey()).equals(registered.getValue().getT())) {
                ServiceStatus prevStatus = status(registered.getKey());
                registered.getValue().setT(configTriggers.get(registered.getKey()));
                ServiceStatus newStatus = status(registered.getKey());
                if (newStatus == ServiceStatus.RUNNING) {
                    stopService(registered.getKey());
                } else if (newStatus == ServiceStatus.ENABLED) {
                    restartService(registered.getKey());
                }
            }
        }
        for (Map.Entry<String, ServiceTrigger> trigger : configTriggers.entrySet()) {
            if (!registeredServices.containsKey(trigger.getKey())) {
                registeredServices.put(trigger.getKey(), new Pair<>(trigger.getValue(), null));
                if (trigger.getValue().enabled) {
                    startService(trigger.getKey());
                }
            }
        }
    }

    /**
     * Saves registered service triggers to configuration and writes the configuration.
     */
    public synchronized void save() {
        Map<String, ServiceTrigger> services = new HashMap<>();
        for (Map.Entry<String, Pair<ServiceTrigger, EventContainer<?>>> service : registeredServices.entrySet()) {
            services.put(service.getKey(), service.getValue().getT());
        }

        runner.config.getOptions(CoreConfigV2.class).services = services;
        runner.config.saveConfig();
    }

    /**
     * Disables service reloads in response to file changes.
     *
     * @since 1.8.4
     */
    public void stopReloadListener() {
        reloadOnModify = false;
    }

    /**
     * Enables service reloads in response to file changes.
     *
     * @since 1.8.4
     */
    public void startReloadListener() {
        reloadOnModify = true;
    }

    /**
     * Marks a service as crashed so the file-change listener can restart it after its file changes.
     *
     * @param serviceName the name of the service to mark as crashed
     * @since 1.8.4
     */
    public void markCrashed(String serviceName) {
        crashedServices.add(serviceName);
    }

    /**
     * @param serviceName the name of the service to check
     * @return {@code true} if the service is marked crashed
     * @since 1.8.4
     */
    public boolean isCrashed(String serviceName) {
        return crashedServices.contains(serviceName);
    }

    /**
     * Checks eligible services for file modification and restarts them when their file timestamp changes.
     *
     * @since 1.8.4
     */
    public void tickReloadListener() {
        if (!reloadOnModify) {
            return;
        }
        for (Map.Entry<String, Pair<ServiceTrigger, EventContainer<?>>> service : registeredServices.entrySet()) {
            File file = runner.config.macroFolder.toPath().resolve(service.getValue().getT().file).toFile();
            String name = service.getKey();
            // Only restart enabled and running services, i.e. services that are supposed to be running
            // If the service is not running because it crashed, try to restart it
            if (isEnabled(name) && (isRunning(name) || crashedServices.contains(name))) {
                long lastModified = file.lastModified();
                if (!lastModifiedMap.containsKey(file.getPath())) {
                    lastModifiedMap.put(file.getPath(), lastModified);
                    // Just assume that if the file was changed so was its content. Otherwise, use Adler-32 or MD5 checksum
                } else if (lastModifiedMap.getLong(file) != lastModified) {
                    lastModifiedMap.put(file.getPath(), lastModified);
                    crashedServices.remove(name);
                    restartService(name);
                }
            }
        }
    }

    /** Combined enabled/running state of a service; {@link #UNKNOWN} means it is not registered. */
    public enum ServiceStatus {
        /** Enabled and running. */
        ENABLED,
        /** Disabled and stopped. */
        DISABLED,
        /** Disabled but still running. */
        RUNNING,
        /** Enabled but stopped. */
        STOPPED,
        /** No service with the requested name is registered. */
        UNKNOWN
    }

}
