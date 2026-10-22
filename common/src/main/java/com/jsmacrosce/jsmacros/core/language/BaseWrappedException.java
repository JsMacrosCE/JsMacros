package com.jsmacrosce.jsmacros.core.language;

import java.io.File;

/**
 * A linked diagnostic record for an exception or stack frame.
 * The {@code next} links form a diagnostic chain, not a Java {@code Throwable} cause chain; values are stored by reference without validation or copying.
 * <pre>
 * const Wrapped = Java.type("com.jsmacrosce.jsmacros.core.language.BaseWrappedException");
 * const entry = new Wrapped("script", "diagnostic", null, null);
 * Chat.log(entry.message);
 * </pre>
 * @param <T> type of the wrapped exception or stack-frame object
 */
public class BaseWrappedException<T> {
    /** The supplied exception or stack-frame object. */
    public final T stackFrame;
    /** Source location for this entry, or {@code null} when unavailable. */
    public final SourceLocation location;
    /** Diagnostic text for this entry. */
    public final String message;
    /** The next diagnostic entry, or {@code null} at the end of the chain. */
    public final BaseWrappedException<?> next;

    /**
     * Stores the supplied object, message, location, and next entry without copying or validation.
     * @param exception wrapped exception or stack-frame object
     * @param message diagnostic text for this entry
     * @param location source location, or {@code null} when unavailable
     * @param next next diagnostic entry, or {@code null} at the end of the chain
     */
    public BaseWrappedException(T exception, String message, SourceLocation location, BaseWrappedException<?> next) {
        this.stackFrame = exception;
        this.message = message;
        this.location = location;
        this.next = next;
    }

    private static String unQualifyClassName(String name) {
        String[] parts = name.split("\\.");
        return parts[parts.length - 1];
    }

    /**
     * Wraps one host stack-frame element as a diagnostic entry linked to {@code next}.
     * Its message uses the unqualified class and method names; the location describes native frames, source file and line when available, or an unknown source.
     * @param t host stack-frame element
     * @param next next diagnostic entry, or {@code null} at the end of the chain
     * @return an entry containing the host frame and its display location
     */
    public static BaseWrappedException<StackTraceElement> wrapHostElement(StackTraceElement t, BaseWrappedException<?> next) {
        String message = " at " + unQualifyClassName(t.getClassName()) + "." + t.getMethodName();
        HostLocation loc = new HostLocation(t.isNativeMethod() ? "Native Method" : (t.getFileName() != null && t.getLineNumber() >= 0 ?
                t.getFileName() + " " + t.getLineNumber() + ":?" :
                (t.getFileName() != null ? t.getFileName() : "Unknown Source")));
        return new BaseWrappedException<>(t, message, loc, next);
    }

    /** Base type for guest and host source-location payloads attached to diagnostic entries.
     * <pre>
     * const HostLocation = Java.type("com.jsmacrosce.jsmacros.core.language.BaseWrappedException$HostLocation");
     * Chat.log(new HostLocation("Unknown Source").toString());
     * </pre>
     */
    public static abstract class SourceLocation {
    }

    /**
     * A guest-source location retaining the supplied file and diagnostic positions.
     * <pre>
     * const GuestLocation = Java.type("com.jsmacrosce.jsmacros.core.language.BaseWrappedException$GuestLocation");
     * Chat.log(new GuestLocation(null, 0, 0, 1, -1).toString());
     * </pre>
     */
    public static class GuestLocation extends SourceLocation {
        /** Source file, or {@code null} when unknown. */
        public final File file;
        /** Supplied diagnostic line number. */
        public final int line;
        /** Supplied diagnostic column; {@code -1} denotes an unknown column. */
        public final int column;
        /** Supplied source-start offset. */
        public final int startIndex;
        /** Supplied source-end offset. */
        public final int endIndex;

        /** Stores the supplied file and diagnostic positions unchanged.
         * @param file source file, or {@code null} when unknown
         * @param startIndex supplied source-start offset
         * @param endIndex supplied source-end offset
         * @param line supplied diagnostic line number
         * @param column supplied diagnostic column; {@code -1} denotes an unknown column
         */
        public GuestLocation(File file, int startIndex, int endIndex, int line, int column) {
            this.file = file;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.line = line;
            this.column = column;
        }

        /** Returns diagnostic text using the file name, line, and column; a null file is shown as {@code unknown}, and column {@code -1} as {@code ?}.
         * @return diagnostic file/line/column text
         */
        public String toString() {
            if (column == -1) {
                return String.format("%s %d:?", file == null ? "unknown" : file.getName(), line);
            }
            return String.format("%s %d:%d", file == null ? "unknown" : file.getName(), line, column);
        }

    }

    /** A source location carrying host display text, such as {@code Native Method}, a file location, or {@code Unknown Source}.
     * <pre>
     * const HostLocation = Java.type("com.jsmacrosce.jsmacros.core.language.BaseWrappedException$HostLocation");
     * Chat.log(new HostLocation("Unknown Source").toString());
     * </pre>
     */
    public static class HostLocation extends SourceLocation {
        /** Stored host display-location text. */
        public final String location;

        /** Stores the supplied host display-location text.
         * @param location host display-location text
         */
        public HostLocation(String location) {
            this.location = location;
        }

        /** Returns the stored display-location text unchanged.
         * @return stored display-location text
         */
        public String toString() {
            return location;
        }

    }

}
