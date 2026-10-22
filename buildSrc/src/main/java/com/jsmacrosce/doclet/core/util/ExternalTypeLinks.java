package com.jsmacrosce.doclet.core.util;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.Map;
import java.util.regex.Pattern;

/** External class links shared by the Markdown and legacy HTML renderers. */
@DocletIgnore
public final class ExternalTypeLinks {
    private static final Pattern RELEASE_VERSION = Pattern.compile("(\\d{1,4})\\.(\\d{1,4})(?:\\.\\d{1,4})?");

    private ExternalTypeLinks() {}

    /**
     * Resolves a class, not a member. Minecraft browsers have different anchor conventions;
     * in particular, decompiler line numbers must not be guessed from our local sources.
     *
     * @param packageName the resolved Java package
     * @param className the package-relative class name, with {@code .} or {@code $} for nesting
     * @param minecraftVersion the exact documentation target, not the active IDE version
     * @param externalPackages package URLs registered with the doclet's {@code -link} option
     * @return the external URL, or {@code null} when no provider can be selected
     */
    public static String resolve(String packageName, String className, String minecraftVersion,
                                 Map<String, String> externalPackages) {
        String javadocBase = externalPackages.get(packageName);
        if (javadocBase != null) {
            return javadocBase + (javadocBase.endsWith("/") ? "" : "/")
                + className.replace('$', '.') + ".html";
        }

        // These Mojang packages are shipped in the game. Authlib, Brigadier, DFU, etc.
        // are separate libraries, and must not acquire fictitious Minecraft class URLs.
        if (!isGamePackage(packageName) || minecraftVersion == null) {
            return null;
        }
        var version = RELEASE_VERSION.matcher(minecraftVersion);
        if (!version.matches()) {
            return null;
        }
        int major = Integer.parseInt(version.group(1));
        int minor = Integer.parseInt(version.group(2));
        boolean unobfuscated = major > 26 || (major == 26 && minor >= 1);
        String classPath = packageName.replace('.', '/') + "/" + className.replace('.', '$');
        return unobfuscated
            ? "https://mcsrc.dev/2/" + minecraftVersion + "/" + classPath
            : "https://mappings.dev/" + minecraftVersion + "/" + classPath + ".html";
    }

    private static boolean isGamePackage(String packageName) {
        return inPackage(packageName, "net.minecraft")
            || inPackage(packageName, "com.mojang.blaze3d")
            || inPackage(packageName, "com.mojang.math")
            || inPackage(packageName, "com.mojang.realmsclient");
    }

    private static boolean inPackage(String packageName, String prefix) {
        return packageName.equals(prefix) || packageName.startsWith(prefix + ".");
    }
}
