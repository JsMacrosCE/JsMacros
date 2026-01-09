package xyz.wagyourtail.doclet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface DocletCategory {
    /**
     * Category used for grouping doclet-generated pages.
     */
    String value();
}
