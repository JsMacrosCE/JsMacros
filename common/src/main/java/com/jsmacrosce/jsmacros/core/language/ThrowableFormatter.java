package com.jsmacrosce.jsmacros.core.language;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Formats an exception and its cause chain for diagnostics.
 * <p>
 * Script errors usually need the exception type and the first useful cause, not a full Java
 * stack trace. This formatter keeps the chain short and uses simple class names so it remains
 * readable in chat and logs.
 */
@DocletIgnore
public final class ThrowableFormatter {
    private static final int MAX_CAUSES = 5;
    private static final int MAX_ENTRIES = MAX_CAUSES + 1;

    private ThrowableFormatter() {
    }

    /**
     * Returns the exception's simple class name and message, followed by up to five causes.
     * A missing message omits the colon; a null throwable returns an empty string.
     *
     * @param throwable exception to format
     * @return diagnostic text
     */
    public static String format(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        int entries = 0;
        while (current != null && entries < MAX_ENTRIES && seen.add(current)) {
            if (entries > 0) {
                builder.append("\n  caused by ");
            }
            builder.append(simpleClassName(current.getClass()));
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                builder.append(": ").append(message);
            }
            current = current.getCause();
            entries++;
        }
        return builder.toString();
    }

    /**
     * Returns the simple class name, or the unqualified binary name for anonymous classes.
     *
     * @param type class to name
     * @return display name
     */
    public static String simpleClassName(Class<?> type) {
        String simpleName = type.getSimpleName();
        if (!simpleName.isEmpty()) {
            return simpleName;
        }
        String name = type.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot == -1 ? name : name.substring(lastDot + 1);
    }
}
