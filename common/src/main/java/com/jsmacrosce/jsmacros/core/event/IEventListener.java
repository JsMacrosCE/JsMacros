package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.jsmacros.core.language.EventContainer;

/**
 * one subscription: what the event system calls when an event this listener is registered for
 * arrives. A script makes one by handing a function to {@code JsMacros.on} or
 * {@code JsMacros.once} and gets the handle back; the profile editor makes one per saved macro
 * trigger.
 * <p>
 * What the profile does with the container {@link #trigger(BaseEvent) trigger} returns is the
 * whole of the joined mechanism, and it is the reason a cancel written in a script does not always
 * land. The profile walks the listeners registered for the event name and, for each one, either
 * calls {@code trigger} and drops the container on the floor, or, if that listener reports
 * {@link #joined()} and the event is joinable, parks the calling thread on
 * {@link com.jsmacrosce.jsmacros.core.language.EventContainer#awaitLock(java.lang.Runnable) awaitLock}
 * until the script has finished and released it. Nothing waits in the first case, so whatever the
 * script does to the event arrives after the game has already read it. Read
 * {@link BaseEvent} for the whole of that rule and what it means for {@code cancel}.
 * <p>
 * {@link #joined()} therefore decides whether a listener is a real subscriber to what the game
 * does next rather than just an observer of it, and it defaults to {@code false}, so a listener is
 * unjoined unless it says otherwise. A joined listener holds up the thread that raised the event
 * for as long as it runs.
 * <p>
 * A {@code null} return from {@code trigger} means the listener declined the event rather than ran
 * it, which is how a filterer and a disabled macro trigger opt out, and the profile moves straight
 * on to the next listener without waiting.
 * example:
 * <pre>
 * // the default subscription, joined = false. right for watching an event, wrong for
 * // changing what the game then does with it
 * const listener = JsMacros.on("SendMessage", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`typed: ${event.message}`);
 * }));
 *
 * // nothing takes this listener off, and that is what joined = false buys: it
 * // costs the game nothing, so a watcher is free to stay on. when something does
 * // want the watch over, the handle on() returned is what off() takes, and it
 * // can be called from any later run of the script rather than right here
 * // JsMacros.off(listener);
 * </pre>
 */
public interface IEventListener {
    /**
     * a listener that does nothing. Its trigger returns {@code null}, which the profile reads as
     * the listener declining the event, so being this is the same as being registered and skipped.
     */
    IEventListener NULL = event -> null;

    /**
     * whether the profile should wait for this listener before letting the game carry on. This is
     * the listener's half of the decision and the event has to agree with it: the profile only
     * joins when this is {@code true} and {@link BaseEvent#joinable()} is too, which every
     * cancellable event is. The default is {@code false}, so a listener is unjoined unless it says
     * otherwise, and the two convenient forms of {@code JsMacros.on} that take no flag both leave
     * it that way.
     * <p>
     * There is a limit to how long that wait is, and it is not a soft one. A joined listener has a
     * watchdog armed on it, provided the event was raised on a thread the profile treats as
     * joinable, which on the client is the client thread. Once the configurable max lock time is
     * up, 500 ms by default, the watchdog stops waiting, closes the script context out from under
     * the body that is still running and takes the lock away so the game is let go. If the listener
     * is a macro trigger rather than a script, the trigger is then switched off outright, so a
     * single slow run leaves a macro that quietly no longer runs. The only sign is one watchdog
     * error in the log, so the safe shape is to keep a joined body short enough to finish well
     * inside that window and put anything that can overrun in an unjoined listener instead.
     *
     * @return {@code true} if the profile should park on the container this listener returned.
     */
    default boolean joined() {
        return false;
    }

    /**
     * called with the event, once per occurrence, on the thread that raised it. The return value
     * is the handle the joined path waits on rather than a result, and {@code null} means the
     * listener declined the event and the profile carries straight on to the next one.
     *
     * @param event the event that was raised. It is the live instance, so a listener that changes
     * it is changing the one the game is holding.
     * @return the container to park on when this listener is joined, or {@code null} to decline
     * the event. A script listener returns that container immediately and runs the callback on a
     * thread pool thread, so a non null return says nothing about whether the script has run yet,
     * which is the race described on the class.
     */
    EventContainer<?> trigger(BaseEvent event);

    /**
     * Used for self unregistering events.
     * <p>
     * A listener that can take itself off implements this, and it is the hook the script side
     * goes through: the calls that drop a script's own listeners do it by calling {@code off()},
     * and the ones that drop every listener do it the same way. A listener with nothing to
     * unregister leaves this alone, so calling it on one does nothing.
     *
     * @since 1.8.4
     */
    default void off() {}

}
