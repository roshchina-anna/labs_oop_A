package functions.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Declares a localized label for a simple function.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalizedName {
    /**
     * Locale tag in IETF BCP 47 format (e.g. "ru", "en").
     */
    String locale();

    /**
     * Human-readable function name for the given locale.
     */
    String value();
}