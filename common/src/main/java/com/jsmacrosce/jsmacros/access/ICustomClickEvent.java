package com.jsmacrosce.jsmacros.access;

/** Provides a platform-independent view of a custom click event. */
public interface ICustomClickEvent {

    /** Returns the callback associated with this custom click event.
     * @return the callback associated with this custom click event
     */
    Runnable getEvent();

    /** Converts this custom click event to a platform-specific representation.
     * @return the platform-specific representation; its concrete type is implementation-defined
     */
    Object toPlatformClickEvent();

    /** Returns the action identifier.
     * @return the literal {@code "CUSTOM"}
     */
    default String getActionType() {
        return "CUSTOM";
    }
}
