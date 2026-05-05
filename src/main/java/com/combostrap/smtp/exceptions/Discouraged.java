package com.combostrap.smtp.exceptions;

import java.lang.annotation.*;

/**
 * Marks a program element as discouraged from use.
 * It still works, but developers should prefer alternatives.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
    ElementType.METHOD,
    ElementType.CONSTRUCTOR,
    ElementType.TYPE,
    ElementType.FIELD
})
public @interface Discouraged {

    /**
     * The reason why this element is discouraged.
     */
    String reason();

    /**
     * A suggested alternative to use instead.
     */
    String alternative() default "";
}