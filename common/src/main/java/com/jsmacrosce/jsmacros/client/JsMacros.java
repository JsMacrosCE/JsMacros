package com.jsmacrosce.jsmacros.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ServiceLoader;

/**
 * Shared bootstrap constants, loader access, and integer-array utilities.
 * Class initialization requests {@code ConfigFolder} and {@code ModLoader} service providers;
 * at least one provider of each must be available.
 * <pre>
 * const JsMacros = Java.type('com.jsmacrosce.jsmacros.client.JsMacros');
 * const values = JsMacros.range(3);
 * Chat.log(values[2]);
 * </pre>
 */
public class JsMacros {
    /** The mod identifier {@code jsmacrosce}. */
    public static final String MOD_ID = "jsmacrosce";
    /** Logger named with {@code MOD_ID}. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    protected static final File configFolder = ServiceLoader.load(ConfigFolder.class).findFirst().orElseThrow().getFolder();
    protected static final ModLoader modLoader = ServiceLoader.load(ModLoader.class).findFirst().orElseThrow();

//    public static final Core<Profile, EventRegistry> serverCore = new Core<>(EventRegistry::new, Profile::new, configFolder.getAbsoluteFile(), new File(configFolder, "Macros"), LOGGER);

    /** Common initialization hook; currently performs no initialization. */
    public static void onInitialize() {
    }

    /**
     * Returns the first {@code ModLoader} service provider selected during class initialization.
     * @return the selected loader
     */
    public static ModLoader getModLoader() {
        return modLoader;
    }

    /** Returns consecutive integers from zero up to, but not including, {@code end}. The array length is {@code end}.
     * @param end exclusive upper bound; must be nonnegative and representable as an array length
     * @return the consecutive values from zero to {@code end - 1}
     */
    public static int[] range(int end) {
        return range(0, end, 1);
    }

    /** Returns consecutive integers from {@code start} up to, but not including, {@code end}. The array length is {@code end - start}, which must be nonnegative and representable as an array length.
     * @param start inclusive first value
     * @param end exclusive upper bound, at least {@code start}
     * @return the consecutive values from {@code start} to {@code end - 1}
     */
    public static int[] range(int start, int end) {
        return range(start, end, 1);
    }

    /**
     * Allocates an array of length {@code end - start}; values reached by repeatedly adding {@code iter} from {@code start} are stored at their offset from {@code start}. Unvisited offsets remain zero; for example, {@code range(2, 7, 2)} returns {@code [2, 0, 4, 0, 6]}. A positive step advances toward {@code end} only if integer addition does not overflow; zero does not advance and the loop does not terminate when {@code start < end}, and a negative step is not a descending-range mode. The difference must produce a nonnegative array length; integer overflow and invalid steps are not checked.
     * @param start inclusive first value
     * @param end exclusive upper bound
     * @param iter step added after each value; use a positive step that does not overflow when added to advance toward {@code end}
     * @return the allocated array with reached values at their corresponding offsets
     */
    public static int[] range(int start, int end, int iter) {
        int[] a = new int[end - start];
        for (int i = start; i < end; i += iter) {
            a[i - start] = i;
        }
        return a;
    }
}
