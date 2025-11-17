package xyz.wagyourtail.jsmacros.forge.client;

import net.neoforged.fml.loading.FMLPaths;
import xyz.wagyourtail.jsmacros.client.ConfigFolder;

import java.io.File;

public class ConfigFolderImpl implements ConfigFolder {
    @Override
    public File getFolder() {
        return FMLPaths.CONFIGDIR.get().resolve("jsMacros").toFile();
    }

}
