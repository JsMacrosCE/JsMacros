package com.jsmacrosce.jsmacros.core.config;

/**
 * Describes editor metadata attached to an {@link Option}; it is not a setting value or a global script API.
 * <pre>
 * const Config = Java.type("com.jsmacrosce.jsmacros.core.config.CoreConfigV2");
 * const Option = Java.type("com.jsmacrosce.jsmacros.core.config.Option");
 * const type = Config.class.getField("maxLockTime").getAnnotation(Option.class).type();
 * Chat.log(type.value());
 * </pre>
 */
public @interface OptionType {
    /** Identifies the setting editor kind; defaults to {@code "primitive"}.
     * @return editor-kind identifier
     */
    String value() default "primitive";

    /** Returns editor-kind-specific option strings, such as {@code "topLevel=CONFIG"} for a file setting; empty by default.
     * @return editor-kind-specific option strings, empty by default
     */
    String[] options() default {};

}
