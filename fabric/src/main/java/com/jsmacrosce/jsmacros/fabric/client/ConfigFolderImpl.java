package com.jsmacrosce.jsmacros.fabric.client;

import net.fabricmc.loader.api.FabricLoader;
import com.jsmacrosce.jsmacros.client.ConfigFolder;

import java.io.File;

public class ConfigFolderImpl implements ConfigFolder {

    @Override
    public File getFolder() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), "jsMacros");
    }

}
