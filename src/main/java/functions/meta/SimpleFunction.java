package functions.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a {@link functions.MathFunction} as available for selection in the UI.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleFunction {
    /**
     * Names in different locales. At least one entry is required.
     */
    LocalizedName[] names();

    /**
     * Priority for ordering in lists; higher values go first.
     */
    int priority() default 0;
}