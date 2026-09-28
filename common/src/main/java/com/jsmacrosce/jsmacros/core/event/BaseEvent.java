package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.Core;

/**
 * what every event is: the payload a listener is handed, the flag the game reads back afterwards,
 * and the {@link #cancel() cancel} that sets it.
 * <p>
 * A subclass adds only its own payload fields. Everything it says about itself, its name and
 * whether it can be cancelled or joined, comes from the {@link Event} annotation on the subclass
 * rather than from anything in here, so {@link #getEventName()}, {@link #cancellable()} and
 * {@link #joinable()} all read that annotation off the runtime class. That also means a subclass
 * carrying no {@code @Event} makes all three of those throw
 * {@link java.lang.NullPointerException}, since the annotation lookup has no null check.
 * {@link com.jsmacrosce.jsmacros.core.event.impl.EventCustom} overrides the two predicates with its
 * own fields and so is not affected.
 * <p>
 * The thing to read this for is cancellation, because it does not work the way the word suggests
 * unless the listener is joined.
 * <p>
 * {@link #cancel() cancel} only reliably takes effect from a listener registered with
 * {@code joined = true}. The game raises an event by calling {@link #trigger()}, and the mixin
 * that raised it reads {@link #isCanceled()} on the very next line. The profile walks the listeners
 * registered for the event name, and for each one either calls
 * {@link IEventListener#trigger(com.jsmacrosce.jsmacros.core.event.BaseEvent) trigger} and drops
 * the container it got back, or, when that listener reports
 * {@link IEventListener#joined() joined} and the event is joinable, parks the calling thread on
 * that container until the script has finished with it. Nothing waits in the first case: the
 * script callback is handed to a
 * {@link com.jsmacrosce.jsmacros.core.threads.JsMacrosThreadPool JsMacrosThreadPool}, a set of
 * pre-started daemon threads, and the container comes straight back, so the game has usually
 * already read the flag by the time the script body has even started.
 * <p>
 * The two convenient subscriptions, {@code JsMacros.on(event, callback)} and
 * {@code JsMacros.on(event, filterer, callback)}, both pass {@code joined = false}, so the default
 * subscription is the racy one. Pass the flag instead, {@code JsMacros.on(event, true, callback)},
 * and the dispatch waits for the script body to finish, so the cancel lands first. A macro trigger
 * in the profile editor is the same story and needs its Joined box ticked. The cost of joining is
 * that the thread which raised the event stands still for as long as the script runs, so a joined
 * listener wants to be short. Short is not a matter of taste either: a joined listener is put on
 * a watchdog that gives up on a body which overruns it, which
 * {@link IEventListener#joined()} spells out.
 * <p>
 * The race is not specific to cancelling. Anything else the game reads on the line after the event
 * is in the same position, so assigning to a payload field from a plain listener is racy for
 * exactly the same reason.
 * <p>
 * {@link #joinable()} and {@link IEventListener#joined()} are two different flags and neither does
 * anything on its own. {@link #joinable()} is the event's half: true when the event is cancellable
 * or is annotated {@code joinable = true}. {@link IEventListener#joined()} is the listener's half,
 * and defaults to {@code false}. For an ordinary event the profile joins only when both are true
 * and the event name is also in the registry's
 * {@link BaseEventRegistry#joinableEvents joinableEvents}. Every cancellable event goes into that
 * set as well as into {@link BaseEventRegistry#cancellableEvents cancellableEvents} when the
 * registry registers it, so a cancellable event is always joinable. A custom event registered by
 * name with {@code registerEvent} is not, because that only adds the name to
 * {@link BaseEventRegistry#events events}.
 * <p>
 * {@link #isCanceled()} is deliberately left out of the generated TypeScript definitions, where a
 * cancellable event is typed as carrying nothing but a {@code cancel(): void}, so a script has no
 * way to read the cancellation state back. It is the game's own check that decides what the cancel
 * did. That is also why the race above is invisible from a script: there is nothing to test it
 * with.
 * <p>
 * Calling {@link #cancel()} on an event that is not cancellable throws
 * {@link UnsupportedOperationException} rather than quietly doing nothing, so a cancel written
 * against the wrong event shows up as a logged error instead of a silent no-op.
 * example:
 * <pre>
 * // a cancellable event, subscribed to with joined = true. the true is what makes
 * // the cancel land before the game carries on
 * const listener = JsMacros.on("SendMessage", true, JavaWrapper.methodToJava(function (event) {
 *   if (event.message === "/hello") {
 *     // nothing is sent, and nothing is added to the chat history
 *     event.cancel();
 *   }
 *   // joining holds up the thread that raised the event, so rather than pay
 *   // that cost on every occurrence, a listener that has done its job takes
 *   // itself off from inside its own callback. the handle on() returned is
 *   // what off() takes, and the callback runs on a later run of the script
 *   JsMacros.off(listener);
 * }));
 * </pre>
 */
@DocletCategory("Events and Event Handling")
public class BaseEvent {
    /**
     * the core this event was raised in, which is where the profile that dispatches it, the event
     * registry it is looked up in and the thread pool its listeners run on all live. Every event
     * subclass hands its core up through the
     * {@link #BaseEvent(com.jsmacrosce.jsmacros.core.Core) constructor}.
     */
    public final Core<?, ?> runner;
    /**
     * whether {@link #cancel()} has been called on this instance. It starts out {@code false} and
     * the game reads it back through {@link #isCanceled()} once the listeners have had their turn.
     */
    protected boolean cancelled;

    /**
     * gives an event its core, which it needs before it can be raised: {@link #trigger()} dispatches
     * through it, and the annotation driven methods read the runtime class to find their own.
     *
     * @param runner the core this event is dispatched through. Nothing else is stored here, so a
     * subclass adding fields has to fill them in itself. The cancelled flag starts out unset, so a
     * freshly raised event is never already cancelled.
     */
    public BaseEvent(Core<?, ?> runner) {
        this.runner = runner;
    }

    /**
     * whether this event can be cancelled, read from {@link Event#cancellable()} on the subclass.
     * It is the flag {@link #cancel()} checks, so it is also what decides whether a cancel throws
     * rather than sticking. The annotation is read without a null check, so on a subclass with no
     * {@code @Event} on it this throws {@link java.lang.NullPointerException}.
     *
     * @return {@code true} if the event declares itself cancellable.
     */
    public boolean cancellable() {
        return this.getClass().getAnnotation(Event.class).cancellable();
    }

    /**
     * whether a joined listener may be parked on this event. This is the event's half of that
     * decision: the profile only joins when it is {@code true} and the listener also reports
     * {@link IEventListener#joined()}, so a joined listener on an event that is not joinable is
     * still dispatched without waiting. It is {@code true} whenever {@link #cancellable()} is,
     * which is why every cancellable event can be joined.
     * <p>
     * That is the event's own half of the answer, read off its annotation. For a named event the
     * profile does not actually ask the event: it tests
     * {@link BaseEventRegistry#joinableEvents joinableEvents} for the name, which
     * {@link BaseEventRegistry#addEvent(Class) addEvent} files the same annotation values under, so
     * for anything reachable the two agree. Only a custom event, whose name is not in that set, is
     * judged by asking it directly.
     *
     * @return {@code true} if the event is cancellable or declares itself joinable.
     */
    public boolean joinable() {
        return cancellable() || this.getClass().getAnnotation(Event.class).joinable();
    }

    /**
     * Cancel the event
     * <p>
     * Sets the flag the game reads back through {@link #isCanceled()}. From a script this only
     * reliably takes effect in a listener registered with {@code joined = true}, because a plain
     * listener is handed to a thread pool and the game does not wait for it. The class note has
     * the whole of that.
     *
     * @throws UnsupportedOperationException if the event is not cancellable, rather than doing
     * nothing silently.
     */
    public final void cancel() {
        if (cancellable()) {
            cancelled = true;
        } else {
            throw new UnsupportedOperationException("Event is not cancellable");
        }
    }

    /**
     * whether {@link #cancel()} has been called on this event. This is what the game checks on the
     * line straight after the event was raised, and it is not part of the generated TypeScript
     * definitions, so a script cannot call it. Java code that raised the event can.
     *
     * @return {@code true} once something has called {@link #cancel()} on this instance.
     */
    public final boolean isCanceled() {
        return cancelled;
    }

    /**
     * the name this event is registered and looked up under, read from {@link Event#value()} on
     * the subclass. It is the string a script passes to {@code JsMacros.on}, and it is the key this
     * event's listeners are filed under in the registry.
     *
     * @return the name from the annotation, for example {@code SendMessage}.
     * @throws java.lang.NullPointerException if the subclass carries no {@code @Event}, since the
     * annotation is read without a null check.
     */
    public String getEventName() {
        return this.getClass().getAnnotation(Event.class).value();
    }

    /**
     * raises the event: hands this instance to the profile, which walks the listeners registered
     * under {@link #getEventName()} and then any registered under {@code ANYTHING}, and only then
     * comes back. The game code that raised the event checks {@link #isCanceled()} as soon as it
     * does, which is what makes the joined rule on the class worth reading before relying on
     * {@link #cancel()}.
     * <p>
     * A {@link com.jsmacrosce.jsmacros.core.event.impl.EventCustom} goes through the same call, but
     * there the custom name rather than {@code Custom} is what its listeners are looked up under.
     */
    public void trigger() {
        runner.profile.triggerEvent(this);
    }

}
