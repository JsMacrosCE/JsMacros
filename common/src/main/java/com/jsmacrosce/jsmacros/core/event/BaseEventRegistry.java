package com.jsmacrosce.jsmacros.core.event;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.ApiStatus;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.library.impl.FJsMacros;

import java.util.*;

/**
 * Registry of event names, listeners, and dispatch metadata. Event classes are registered from
 * their {@link Event} annotations; bare names can also be registered. Dispatch uses event-instance
 * cancellability and joinability, with {@link #joinableEvents} supplying joinability for named
 * events. {@link #cancellableEvents} is registration metadata and does not control
 * {@link BaseEvent#cancel()}. Script listeners and profile macro triggers are represented by
 * distinct listener types.
 *
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Events and Event Handling")
public abstract class BaseEventRegistry {
    protected final Core runner;
    /**
     * Listeners grouped by event name, preserving insertion order within each name.
     */
    protected final Map<String, Set<IEventListener>> listeners = new LinkedHashMap<>();
    /**
     * Aliases mapped to current event names, including registered event classes' simple names and
     * any {@link Event#oldName() old names}. The client registry uses these aliases when loading
     * profile triggers; script subscriptions are not remapped.
     */
    public final Map<String, String> oldEvents = new LinkedHashMap<>();
    /**
     * All registered event names, whether joinable or not.
     */
    public final Set<String> events = new LinkedHashSet<>();
    /**
     * Names registered with the cancellable flag. This metadata does not control
     * {@link BaseEvent#cancel()}, which checks the event instance.
     */
    public final Set<String> cancellableEvents = new HashSet<>();
    /**
     * Names treated as joinable by named-event dispatch. Registering a cancellable annotated event
     * or bare name also marks its name joinable.
     */
    public final Set<String> joinableEvents = new HashSet<>();
    /**
     * Registered event names mapped to their declared filterer classes, when an event annotation
     * specifies one.
     */
    public final Map<String, Class<? extends EventFilterer>> filterableEvents = new HashMap<>();

    /**
     * Creates a registry associated with the given core. Registration is performed separately.
     *
     * @param runner the core this registry belongs to.
     */
    public BaseEventRegistry(Core runner) {
        this.runner = runner;
    }

    /**
     * Removes non-script listeners from the registry, leaving script listeners registered.
     */
    public synchronized void clearMacros() {
        for (Set<IEventListener> value : listeners.values()) {
            value.removeIf(listener -> !(listener instanceof FJsMacros.ScriptEventListener));
        }
    }

    /**
     * Registers a profile macro trigger as a listener. Implementations determine how triggers are
     * converted; the client implementation remaps old event names and recognizes legacy
     * {@code Joined}-prefixed event names.
     *
     * @param rawmacro the trigger to register, taken from the profile as it was saved.
     * @since 1.2.9
     */
    public abstract void addScriptTrigger(ScriptTrigger rawmacro);

    /**
     * Adds a listener under an event name, preserving insertion order and ignoring duplicates
     * within that name. This method does not check whether the name is registered.
     *
     * @param event the name to associate with the listener.
     * @param listener the listener to add.
     * @since 1.2.3
     */
    public synchronized void addListener(String event, IEventListener listener) {
        listeners.putIfAbsent(event, new LinkedHashSet<>());
        listeners.get(event).add(listener);
    }

    /**
     * Removes a listener from one event name. If the name has no listener set, this creates an
     * empty set for it.
     *
     * @param event the name the listener was filed under.
     * @param listener the listener to take off.
     * @return {@code true} if the listener was registered under that name and has now been
     * removed, {@code false} if it was not there to begin with.
     * @since 1.2.3
     */
    public synchronized boolean removeListener(String event, IEventListener listener) {
        listeners.putIfAbsent(event, new LinkedHashSet<>());
        return listeners.get(event).remove(listener);
    }

    /**
     * Removes a listener from the first event-name set containing it.
     *
     * @param listener the listener to take off.
     * @return {@code true} if it was registered under some name and has now been removed from
     * that one, {@code false} if it was not registered at all.
     * @deprecated use {@code removeListener(event, listener)} instead, since this cannot say
     * which event it removed the listener from.
     * @since 1.2.3
     */
    @DocletIgnore
    @Deprecated
    public synchronized boolean removeListener(IEventListener listener) {
        for (Set<IEventListener> listeners : listeners.values()) {
            if (listeners.contains(listener)) {
                return listeners.remove(listener);
            }
        }
        return false;
    }

    /**
     * Removes the listener associated with a macro trigger, if present. The implementation
     * determines how triggers are matched to listeners.
     *
     * @param rawmacro the trigger to remove.
     * @return {@code true} if a listener for that trigger was registered and has now been
     * removed, {@code false} if it was not there.
     * @since 1.2.9
     */
    public abstract boolean removeScriptTrigger(ScriptTrigger rawmacro);

    /**
     * Returns the live listener map used by dispatch; changes to it affect dispatch.
     *
     * @return the live map of event name to its listeners.
     * @since 1.2.3
     */
    public synchronized Map<String, Set<IEventListener>> getListeners() {
        return listeners;
    }

    /**
     * Returns an immutable snapshot of listeners for a name. Looking up a missing name creates an
     * empty entry in the registry.
     *
     * @param key the event name to look up.
     * @return an immutable snapshot of that name's listeners, empty if there are none.
     * @since 1.2.3
     */
    public synchronized Set<IEventListener> getListeners(String key) {
        return ImmutableSet.copyOf(listeners.computeIfAbsent(key, (k) -> new LinkedHashSet<>()));
    }

    /**
     * Returns the macro triggers represented by registered {@link BaseListener} listeners, not
     * runtime script listeners.
     *
     * @return the triggers represented by registered macro listeners.
     * @since 1.2.9
     */
    public abstract List<ScriptTrigger> getScriptTriggers();

    /**
     * Registers a name without joinable or cancellable registry flags.
     *
     * @param eventName the name to register.
     * @since 1.0.2
     */
    @ApiStatus.Internal
    public synchronized void addEvent(String eventName) {
        events.add(eventName);
    }

    /**
     * Registers a bare event name and optionally marks it joinable. This does not mark it
     * cancellable.
     *
     * @param eventName the name to register.
     * @param joinable whether to mark the name joinable.
     */
    @ApiStatus.Internal
    public synchronized void addEvent(String eventName, boolean joinable) {
        events.add(eventName);
        if (joinable) {
            joinableEvents.add(eventName);
        }
    }

    /**
     * Registers a bare event name with joinable and cancellable flags. A cancellable name is also
     * marked joinable.
     *
     * @param eventName the name to register.
     * @param joinable whether to mark the name joinable.
     * @param cancellable whether to mark the name cancellable; this also marks it joinable.
     */
    @ApiStatus.Internal
    public synchronized void addEvent(String eventName, boolean joinable, boolean cancellable) {
        events.add(eventName);
        if (joinable || cancellable) {
            joinableEvents.add(eventName);
        }
        if (cancellable) {
            cancellableEvents.add(eventName);
        }
    }

    /**
     * Registers the name and flags from an event class's {@link Event} annotation. Its old name and
     * simple class name map to the current name; declared filterers are recorded as well. Throws
     * {@link RuntimeException} if the class has no {@link Event} annotation.
     *
     * @param clazz the event class to register.
     */
    public synchronized void addEvent(Class<? extends BaseEvent> clazz) {
        if (clazz.isAnnotationPresent(Event.class)) {
            Event e = clazz.getAnnotation(Event.class);
            if (!e.oldName().isEmpty()) {
                oldEvents.put(e.oldName(), e.value());
            }
            oldEvents.put(clazz.getSimpleName(), e.value());
            events.add(e.value());
            if (e.cancellable()) {
                cancellableEvents.add(e.value());
                joinableEvents.add(e.value());
            }
            if (e.joinable()) {
                joinableEvents.add(e.value());
            }
            if (e.filterer() != EventFilterer.class) {
                filterableEvents.put(e.value(), e.filterer());
            }
        } else {
            throw new RuntimeException("Tried to add event that doesn't have proper event annotation, " + clazz.getSimpleName());
        }
    }

}
