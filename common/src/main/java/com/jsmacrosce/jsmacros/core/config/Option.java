package com.jsmacrosce.jsmacros.core.config;

import java.lang.annotation.*;

/** Configures how an annotated configuration field or method is presented in the settings editor.
 * <pre>
 * const Config = Java.type("com.jsmacrosce.jsmacros.core.config.CoreConfigV2");
 * const Option = Java.type("com.jsmacrosce.jsmacros.core.config.Option");
 * const metadata = Config.class.getField("maxLockTime").getAnnotation(Option.class);
 * Chat.log(metadata.translationKey() + ": " + metadata.type().value());
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Option {
    /** Returns the translation key used for the setting's label.
     * @return the label translation key
     */
    String translationKey();

    /** Returns the ordered category path in which the setting appears.
     * @return category path segments
     */
    String[] group();

    /** Returns the declared setter method name, or empty to use direct field assignment for an annotated field.
     * A named setter takes one argument matching the annotated field type or, for an annotated method, its return type.
     * @return setter method name, or empty when none is specified
     */
    String setter() default "";

    /** Returns the declared no-argument getter method name for an annotated field, or empty to read that field directly. An annotated method is used as the getter regardless of this element.
     * @return getter method name, or empty when none is specified
     */
    String getter() default "";

    /** Returns the declared no-argument method name supplying choices for the setting, or empty when no provider is specified. The named method returns a {@code List} of choices. Without a provider, enum settings use their constants; non-enum settings have no choices.
     * @return choice-provider method name, or empty when none is specified
     */
    String options() default "";

    /** Returns the setting editor kind and its kind-specific metadata.
     * @return editor-kind and option metadata; by default the kind is {@code "primitive"} with no extra options
     */
    OptionType type() default @OptionType;

}
