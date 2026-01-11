package com.jsmacrosce.jsmacros.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ServiceLoader;

public class JsMacros {
    public static final String MOD_ID = "jsmacrosce";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    protected static final File configFolder = ServiceLoader.load(ConfigFolder.class).findFirst().orElseThrow().getFolder();
    protected static final ModLoader modLoader = ServiceLoader.load(ModLoader.class).findFirst().orElseThrow();

//    public static final Core<Profile, EventRegistry> serverCore = new Core<>(EventRegistry::new, Profile::new, configFolder.getAbsoluteFile(), new File(configFolder, "Macros"), LOGGER);

    public static void onInitialize() {
    }

    public static ModLoader getModLoader() {
        return modLoader;
    }

    public static int[] range(int end) {
        return range(0, end, 1);
    }

    public static int[] range(int start, int end) {
        return range(start, end, 1);
    }

    public static int[] range(int start, int end, int iter) {
        int[] a = new int[end - start];
        for (int i = start; i < end; i += iter) {
            a[i - start] = i;
        }
        return a;
    }
}
