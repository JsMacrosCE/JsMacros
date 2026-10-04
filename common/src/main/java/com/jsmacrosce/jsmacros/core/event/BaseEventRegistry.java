package com.jsmacrosce.jsmacros.core.event;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.ApiStatus;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.library.impl.FJsMacros;

import java.util.*;

/**
 * the book of what events exist and who is listening to them: one entry per registered event name,
 * the listeners filed under each, and the sets the dispatch consults to decide whether a listener
 * may be joined and whether an event may be cancelled.
 * <p>
 * An event gets into here through one of the {@code addEvent} calls, and the class based one is
 * the useful one, because it reads a single {@link Event} annotation and files the event under
 * every name it can be reached by. The name based ones register a bare string, which is what a
 * custom event has to do since it has no class to read an annotation from.
 * <p>
 * {@link BaseEvent#cancellable() Cancellable} and {@link BaseEvent#joinable() joinable} are
 * properties of the event, and the two sets here are the registry's copy of them. They are what
 * the profile checks before parking a thread on a joined listener, so a listener that reports
 * {@link IEventListener#joined() joined} on an event name that is in neither
 * {@link #joinableEvents} nor registered as cancellable is dispatched without waiting no matter
 * what it says. Every cancellable event is added to both sets, which is why a cancellable event
 * is always joinable.
 * <p>
 * A script only ever reaches this indirectly, through the listener calls on
 * {@code JsMacros.on}, {@code JsMacros.once}, {@code JsMacros.waitForEvent} and
 * {@code JsMacros.off}, and through {@code JsMacros.listeners} and the disable calls. What a
 * script can see from there is its own listeners: the macro triggers loaded from the profile are
 * in the same map, but the script facing calls filter them out.
 * example:
 * <pre>
 * // what a script can see of the registry: the listeners it made itself on one
 * // event, since the profile's macro triggers are not included
 * Chat.log(`listeners on SendMessage: ${JsMacros.listeners("SendMessage").size()}`);
 *
 * // and the way to take a script's own listeners off an event, which is what a
 * // cleanup routine wants
 * JsMacros.disableScriptListeners("SendMessage");
 * </pre>
 *
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Events and Event Handling")
public abstract class BaseEventRegistry {
    protected final Core runner;
    /**
     * the listeners filed under each registered event name, in the order they were added. The
     * profile walks these sets in order when it dispatches, so two joined listeners on one event
     * run one after the other rather than side by side.
     */
    protected final Map<String, Set<IEventListener>> listeners = new LinkedHashMap<>();
    /**
     * every older name an event answers to, mapped to the name it is registered under. Both the
     * simple class name and the event's {@link Event#oldName() old name} go in here, so a profile
     * saved before an event was renamed still loads: a macro trigger is remapped through this when
     * it is added from a profile. A subscription a script makes is not remapped, so a script has
     * to use the current name.
     */
    public final Map<String, String> oldEvents = new LinkedHashMap<>();
    /**
     * every registered event name, joinable or not. This is the set a subscription is checked
     * against, so a name that is not here cannot be listened to at all and
     * {@code JsMacros.on} rejects it.
     */
    public final Set<String> events = new LinkedHashSet<>();
    /**
     * the names of the events that can be cancelled, which is the set
     * {@link BaseEvent#cancel()} is permitted on. It is a subset of what the events themselves
     * report, kept here so the registry can be asked without reflecting on a class.
     */
    public final Set<String> cancellableEvents = new HashSet<>();
    /**
     * the names of the events a joined listener may be parked on. A cancellable event is always in
     * here, since being cancellable is what the dispatch keys off, and a name added through
     * {@link #addEvent(String, boolean)} can be in here without being cancellable.
     */
    public final Set<String> joinableEvents = new HashSet<>();
    /**
     * the events that take a filterer, mapped to the filterer class their
     * {@link Event#filterer()} named. An event is only in here when its annotation actually named
     * a filterer, so this is the list {@code JsMacros.createEventFilterer} works from: a name
     * that is not here has no filterer to build, whether or not the event itself is registered.
     */
    public final Map<String, Class<? extends EventFilterer>> filterableEvents = new HashMap<>();

    /**
     * creates an empty registry. Nothing is registered by this; the profile's own setup is what
     * fills it in, by calling the {@code addEvent} calls and then loading a profile.
     *
     * @param runner the core this registry belongs to.
     */
    public BaseEventRegistry(Core runner) {
        this.runner = runner;
    }

    /**
     * takes out every macro trigger listener and leaves every listener a script made alone. A
     * listener is kept only if it is a {@link FJsMacros.ScriptEventListener}, and the macro
     * triggers loaded from a profile are not, since they are plain
     * {@link BaseListener} wrappers. The profile calls this before it loads a profile, so the
     * triggers of the one being left go and the ones being loaded take their place, while
     * listeners a running script registered carry on across a profile switch.
     */
    public synchronized void clearMacros() {
        for (Set<IEventListener> value : listeners.values()) {
            value.removeIf(listener -> !(listener instanceof FJsMacros.ScriptEventListener));
        }
    }

    /**
     * registers a macro trigger as a listener on the event it names. The implementation is what
     * knows how to turn a {@link ScriptTrigger} into a listener, and on the client side it is also
     * where a trigger saved under an event's old name is remapped through {@link #oldEvents}, and
     * where a legacy {@code Joined}-prefixed event name is read back as a joined trigger with the
     * prefix stripped.
     *
     * @param rawmacro the trigger to register, taken from the profile as it was saved.
     * @since 1.2.9
     */
    public abstract void addScriptTrigger(ScriptTrigger rawmacro);

    /**
     * files a listener under an event name. Adding the same listener twice under one name adds it
     * once, since the listeners for a name are a set, and the profile dispatches them in the order
     * they were added.
     *
     * @param event the name to file the listener under. The subscription calls check this against
     * {@link #events} themselves before they get here; this method does not.
     * @param listener the listener to add.
     * @since 1.2.3
     */
    public synchronized void addListener(String event, IEventListener listener) {
        listeners.putIfAbsent(event, new LinkedHashSet<>());
        listeners.get(event).add(listener);
    }

    /**
     * takes a listener back off one event name, leaving it registered on any other name it was
     * added under. Passing a name nothing was ever registered under creates an empty entry for it
     * rather than failing.
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
     * takes a listener back off whichever single event name it was found under, which is the
     * first one it turns up on. It stops at that one even if the listener was added under
     * several, so the two argument form is the one to reach for.
     *
     * @param listener the listener to take off.
     * @return {@code true} if it was registered under some name and has now been removed from
     * that one, {@code false} if it was not registered at all.
     * @deprecated use {@code removeListener(event, listener)} instead, since this cannot say
     * which event it removed the listener from.
     * @since 1.2.3
     */
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
     * takes a macro trigger back off the event it was registered against. The implementation is
     * what knows how to recognise a listener that belongs to a given trigger.
     *
     * @param rawmacro the trigger to remove.
     * @return {@code true} if a listener for that trigger was registered and has now been
     * removed, {@code false} if it was not there.
     * @since 1.2.9
     */
    public abstract boolean removeScriptTrigger(ScriptTrigger rawmacro);

    /**
     * the whole map, live. Changing it changes what the profile dispatches, since this is the map
     * the profile walks rather than a copy of it. Most callers want a read only view, which is
     * what {@link #getListeners(String)} gives.
     *
     * @return the live map of event name to its listeners.
     * @since 1.2.3
     */
    public synchronized Map<String, Set<IEventListener>> getListeners() {
        return listeners;
    }

    /**
     * the listeners on one event name, as an immutable snapshot. Changing what comes back does
     * nothing, so removing a listener has to go through
     * {@link #removeListener(String, IEventListener)} rather than through the returned set. A name
     * nothing was ever registered under comes back as an empty set, and is created in the map by
     * asking for it.
     *
     * @param key the event name to look up.
     * @return an immutable snapshot of that name's listeners, empty if there are none.
     * @since 1.2.3
     */
    public synchronized Set<IEventListener> getListeners(String key) {
        return ImmutableSet.copyOf(listeners.computeIfAbsent(key, (k) -> new LinkedHashSet<>()));
    }

    /**
     * the macro triggers currently registered, which is what the profile editor edits and what
     * saving the profile writes out. This is the profile's own triggers, the
     * {@link ScriptTrigger} objects behind the {@link BaseListener} listeners, and not the
     * listeners a script registered at runtime.
     *
     * @return the triggers currently registered, in the order the event names were first used.
     * @since 1.2.9
     */
    public abstract List<ScriptTrigger> getScriptTriggers();

    /**
     * registers a bare event name, without recording any joinable or cancellable flag against it.
     * This is what a {@link com.jsmacrosce.jsmacros.core.event.impl.EventCustom custom event} uses,
     * and the consequence is worth knowing: a listener
     * registered under that name is never parked on however its {@code joined} is set, since the
     * name is in neither set. What a custom event is judged on is its own
     * {@code joinable} and {@code cancelable} fields, which are plain
     * {@code boolean}s rather than something derived from an annotation,
     * and only for a listener registered under {@code ANYTHING}.
     *
     * @param eventName the name to register.
     * @since 1.0.2
     */
    @ApiStatus.Internal
    public synchronized void addEvent(String eventName) {
        events.add(eventName);
    }

    /**
     * registers a bare event name that a joined listener may be parked on.
     *
     * @param eventName the name to register.
     * @param joinable whether a joined listener may be parked on this name. It does not make the
     * event cancellable, so {@code cancel()} is still not permitted on it.
     */
    @ApiStatus.Internal
    public synchronized void addEvent(String eventName, boolean joinable) {
        events.add(eventName);
        if (joinable) {
            joinableEvents.add(eventName);
        }
    }

    /**
     * registers a bare event name with both flags. Passing {@code cancellable} also files it as
     * joinable, which is the same coupling {@link #addEvent(Class)} applies, so there is no way to
     * register a cancellable event that a joined listener cannot be parked on. The profile sets
     * its own catch-all name up this way, as {@code ANYTHING} with both flags true, which is how a
     * listener can be registered against every event at once.
     *
     * @param eventName the name to register.
     * @param joinable whether a joined listener may be parked on this name.
     * @param cancellable whether the event may be cancelled. This also makes it joinable.
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
     * registers an event class, reading its name and its flags off its {@link Event} annotation
     * and filing it under the current name, under the annotation's {@link Event#oldName() old
     * name} if it has one, and under the class's simple name, all of which
     * {@link #oldEvents oldEvents} maps back to the current one. A cancellable event goes into
     * both {@link #cancellableEvents cancellableEvents} and
     * {@link #joinableEvents joinableEvents}, an event annotated joinable goes into the latter, and
     * one that named a filterer is recorded in {@link #filterableEvents filterableEvents}.
     *
     * @param clazz the event class to register.
     * @throws RuntimeException if the class carries no {@link Event} annotation, since there is
     * then no name to register it under.
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
