package com.jsmacrosce.jsmacros.core.extensions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records what generated test extensions observed while the loader initialized them.
 *
 * <p>Extension classes are generated into jars and loaded by the extension class loader, so they
 * cannot share a static field with the test directly. They can still call this class, because the
 * extension loader's parent is the test class loader. Every method is public for that reason.
 */
public final class IntegrationProbe {
    private static final Map<String, String> VALUES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> COUNTS = new ConcurrentHashMap<>();

    private IntegrationProbe() {
    }

    /** Records the latest value reported for an extension and how many times it reported. */
    public static void record(String extension, String value) {
        VALUES.put(extension, value);
        COUNTS.merge(extension, 1, Integer::sum);
    }

    /** Returns the latest value reported for an extension, or {@code null} when it never reported. */
    public static String value(String extension) {
        return VALUES.get(extension);
    }

    /** Returns how many times an extension reported, which shows whether {@code init} ran more than once. */
    public static int count(String extension) {
        return COUNTS.getOrDefault(extension, 0);
    }

    /** Clears everything so one test cannot see another test's reports. */
    public static void reset() {
        VALUES.clear();
        COUNTS.clear();
    }
}
