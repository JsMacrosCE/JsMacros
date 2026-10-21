package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.doclet.DocletCategory;

import java.lang.annotation.*;

/**
 * Declares an event type's registry metadata: its current name, aliases, dispatch flags, and
 * optional filterer class. This describes the annotated runtime class, not an event instance.
 * <pre>
 * const Event = Java.type("com.jsmacrosce.jsmacros.core.event.Event");
 * const Tick = Java.type("com.jsmacrosce.jsmacros.client.api.event.impl.world.EventTick");
 * const metadata = Tick.class.getAnnotation(Event.class);
 * Chat.log(metadata.value() + " (old name: " + metadata.oldName() + ")");
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@DocletCategory("Events and Event Handling")
public @interface Event {
    /** Returns the declared current event name used during registration.
     * @return the declared event name
     */
    String value();

    /** Returns an explicitly declared legacy alias; the registry also records the annotated
     * class's simple name as an alias.
     * @return the old-name alias, or an empty string if none was declared
     */
    String oldName() default "";

    /** Declares the event's cancellable metadata flag. Defaults to {@code false}. Registration also marks such an event
     * joinable; subclasses may define effective cancellability independently.
     * @return {@code true} if the event is declared cancellable
     */
    boolean cancellable() default false;

    /** Declares whether the event is joinable; defaults to {@code false}. Cancellable events are treated as joinable too.
     * @return {@code true} if the event is declared joinable
     */
    boolean joinable() default false;

    /** Declares the filterer class to register for this event, if any.
     * @return the filterer class, or {@code EventFilterer.class} when none is declared
     */
    Class<? extends EventFilterer> filterer() default EventFilterer.class;

}
