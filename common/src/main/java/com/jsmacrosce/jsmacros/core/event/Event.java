package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.doclet.DocletCategory;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@DocletCategory("Events and Event Handling")
public @interface Event {
    String value();

    String oldName() default "";

    boolean cancellable() default false;

    boolean joinable() default false;

    Class<? extends EventFilterer> filterer() default EventFilterer.class;

}
