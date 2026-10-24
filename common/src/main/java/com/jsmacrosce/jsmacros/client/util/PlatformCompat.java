package com.jsmacrosce.jsmacros.client.util;

import com.jsmacrosce.doclet.DocletIgnore;
import net.minecraft.Util;
import java.io.File;
import java.net.URI;

@DocletIgnore
public final class PlatformCompat {
    private PlatformCompat() {
    }

    public static void openFile(File file) {
        //? if >=26.3 {
        /*com.mojang.blaze3d.Blaze3D.openPath(file.toPath());
        *///?} else {
        Util.getPlatform().openFile(file);
        //?}
    }

    public static void openUri(String uri) {
        //? if >=26.3 {
        /*com.mojang.blaze3d.Blaze3D.openUri(URI.create(uri));
        *///?} else {
        Util.getPlatform().openUri(uri);
        //?}
    }
}
