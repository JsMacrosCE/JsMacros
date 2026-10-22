package com.jsmacrosce.jsmacros.core.classes;

/**
 * Defines an object that can be registered and unregistered.
 * @param <R> the value returned by both lifecycle methods
 * @author aMelonRind
 * @since 1.9.1
 */
public interface Registrable<R> {

    /** Registers this object.
     * @return the implementation-defined registration result
     */
    R register();

    /** Unregisters this object; implementations may report failures.
     * @return the implementation-defined unregistration result
     */
    R unregister() throws Exception;

}
