package com.jsmacrosce.jsmacros.forge.client;

import net.neoforged.fml.loading.FMLPaths;
import com.jsmacrosce.jsmacros.client.ConfigFolder;

import java.io.File;

public class ConfigFolderImpl implements ConfigFolder {
    @Override
    public File getFolder() {
        return FMLPaths.CONFIGDIR.get().resolve("jsMacros").toFile();
    }

}
