package com.jsmacrosce.jsmacros.access;

/**
 * Platform-agnostic interface for custom click events
 * Platform-specific implementations should provide the actual click event functionality
 */
public interface ICustomClickEvent {

    /**
     * Gets the runnable event to execute
     * @return the event runnable
     */
    Runnable getEvent();

    /**
     * Creates a platform-specific click event from this custom click event
     * @return native platform click event object
     */
    Object toPlatformClickEvent();

    /**
     * Gets the action type for this click event
     * @return action type identifier
     */
    default String getActionType() {
        return "CUSTOM";
    }
}