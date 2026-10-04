package com.jsmacrosce.doclet.core.util;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Runs the actual doclets on synthetic declarations, without downloading Minecraft or Javadoc. */
public final class DocletLinksTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Path stubs = root.resolve("stubs");
        Files.createDirectories(stubs);
        Path player = write(root.resolve("source/net/minecraft/client/multiplayer/PlayerInfo.java"),
            "package net.minecraft.client.multiplayer; public class PlayerInfo {}");
        Path hit = write(root.resolve("source/net/minecraft/world/phys/HitResult.java"),
            "package net.minecraft.world.phys; public class HitResult { public enum Type { MISS } }");
        Path profile = write(root.resolve("source/com/mojang/authlib/GameProfile.java"),
            "package com.mojang.authlib; public class GameProfile {}");
        int compile = ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "-d", stubs.toString(), player.toString(), hit.toString(), profile.toString());
        if (compile != 0) throw new AssertionError("Fixture compilation failed: " + compile);
        Path api = write(root.resolve("source/example/Api.java"), """
            package example;
            import net.minecraft.client.multiplayer.PlayerInfo;
            import net.minecraft.world.phys.HitResult;
            import com.mojang.authlib.GameProfile;
            /** A fixture preserving {@code a & b < c} and {@code \"player\"}.
             * example:
             * <pre>const name = \"player\";</pre>
             */
            public class Api {
                /** Creates an API fixture.
                 * @param player the {@code player & profile} entry
                 */
                public Api(java.util.UUID uuid, PlayerInfo player) {}
                /** A nested Minecraft type. */
                public HitResult.Type nested;
                /** A nested library type. */
                public java.util.Map.Entry<String, PlayerInfo> entry;
                /** An external library, not a class in the Minecraft JAR. */
                public GameProfile profile;
            }
            """);

        // An offline package-list tests the same -link option as a real docs build.
        Path javadoc = root.resolve("external");
        Files.createDirectories(javadoc);
        Files.writeString(javadoc.resolve("package-list"), "java.util\njava.lang\n");
        String external = javadoc.toUri().toString();
        String classpath = System.getProperty("java.class.path") + java.io.File.pathSeparator + stubs;
        for (String version : List.of("1.21.8", "26.1.2")) {
            for (String kind : List.of("mddoclet", "webdoclet", "tsdoclet", "pydoclet")) {
                Path out = root.resolve(version).resolve(kind);
                var command = new ArrayList<>(List.of(
                    Path.of(System.getProperty("java.home"), "bin", "javadoc").toString(),
                    "-quiet", "-doclet", "com.jsmacrosce.doclet.core." + kind + ".Main",
                    "-docletpath", classpath, "-classpath", classpath,
                    "-d", out.toString(), "-v", "fixture"));
                if (kind.equals("mddoclet") || kind.equals("webdoclet")) {
                    command.addAll(List.of("-mcv", version, "-link", external));
                }
                command.add(api.toString());
                Path log = root.resolve(version + "-" + kind + ".log");
                int result = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(log.toFile()).start().waitFor();
                if (result != 0) throw new AssertionError("Javadoc failed: " + Files.readString(log));
            }
            String md = Files.readString(root.resolve(version + "/mddoclet/content/fixture/classes/example/Api.md"));
            String html = Files.readString(root.resolve(version + "/webdoclet/fixture/example/Api.html"));
            String base = version.equals("1.21.8") ? "https://mappings.dev/1.21.8/" : "https://mcsrc.dev/2/26.1.2/";
            String suffix = version.equals("1.21.8") ? ".html" : "";
            for (String output : List.of(md, html)) {
                contains(output, "href=\"" + base + "net/minecraft/client/multiplayer/PlayerInfo" + suffix + "\"");
                contains(output, "href=\"" + base + "net/minecraft/world/phys/HitResult$Type" + suffix + "\"");
                contains(output, "java/util/Map.Entry.html");
                excludes(output, "MinecraftMappingViewer");
                excludes(output, base + "com/mojang/authlib");
                excludes(output, "PlayerInfo#L");
            }
        }
        for (String artifact : List.of("tsdoclet/JsMacros-fixture.d.ts", "pydoclet/Api.py")) {
            String old = Files.readString(root.resolve("1.21.8/" + artifact));
            String modern = Files.readString(root.resolve("26.1.2/" + artifact));
            if (!old.equals(modern)) throw new AssertionError("Link target changed typings: " + artifact);
            contains(modern, "a & b < c");
            contains(modern, "const name = \"player\";");
            contains(modern, "player & profile");
        }
        System.out.println("DocletLinks: 8 actual doclet runs, 32 artifact checks passed");
    }

    private static Path write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private static void contains(String output, String expected) {
        if (!output.contains(expected)) throw new AssertionError("Missing: " + expected + "\nOutput:\n" + output);
    }

    private static void excludes(String output, String unexpected) {
        if (output.contains(unexpected)) throw new AssertionError("Unexpected: " + unexpected);
    }
}
