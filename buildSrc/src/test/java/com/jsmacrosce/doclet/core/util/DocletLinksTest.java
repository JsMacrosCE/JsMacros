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
        Path record = write(root.resolve("source/net/minecraft/fixture/RecordType.java"),
            "package net.minecraft.fixture; public record RecordType(int value) {}");
        Path libraryAnnotation = write(root.resolve("source/example/Library.java"),
            "package example; public @interface Library { String value(); }");
        int compile = ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "-d", stubs.toString(), player.toString(), hit.toString(), profile.toString(), record.toString(), libraryAnnotation.toString());
        if (compile != 0) throw new AssertionError("Fixture compilation failed: " + compile);
        Path api = write(root.resolve("source/example/Api.java"), """
            package example;
            import net.minecraft.client.multiplayer.PlayerInfo;
            import net.minecraft.world.phys.HitResult;
            import com.mojang.authlib.GameProfile;
            /** A fixture preserving {@code a & b < c} and {@code \"player\"}.
             * References {@link PlayerInfo} and {@link net.minecraft.fixture.RecordType}.
             * Under <a target="_blank" href="https://example.org/license">Example license</a>.
             * Literal: {@code <a target="_blank" href="https://example.org">$literal</a>}.
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
                /** A live estimate.
                 * @since 1.0
                 */
                @com.jsmacrosce.doclet.DocletReadOnly
                public static double estimate = 20;
                /** A read-only reference, not an immutable list. */
                @com.jsmacrosce.doclet.DocletReadOnly
                public java.util.List<String> names;
                /** A Java constant. */
                public static final int LIMIT = 10;
                /** A mutable field. */
                public double writable;
                /** A nested library type. */
                public java.util.Map.Entry<String, PlayerInfo> entry;
                /** An external library, not a class in the Minecraft JAR. */
                public GameProfile profile;
                /** A named global fixture. */
                @Library("Demo")
                public static class LibraryFixture {
                    /** A returned supporting type, not a global library. */
                    public static class Result {
                        /** Creates a result. */
                        public Result() {}
                    }
                }
            }
            """);

        // Do not put Gradle/Kotlin compiler jars on the doclet path. Their service
        // providers can initialize javac before Javadoc has finished parsing options.
        String docletClasspath = Path.of(com.jsmacrosce.doclet.core.mddoclet.Main.class
            .getProtectionDomain().getCodeSource().getLocation().toURI()).toString()
            + java.io.File.pathSeparator + Path.of(com.google.gson.Gson.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        for (String version : List.of("1.21.8", "26.1.2")) {
            // Exercise both legacy package-list and modern module-aware element-list.
            Path javadoc = root.resolve("external").resolve(version);
            Files.createDirectories(javadoc);
            String listFile = version.equals("1.21.8") ? "package-list" : "element-list";
            Files.writeString(javadoc.resolve(listFile),
                (version.equals("1.21.8") ? "" : "module:java.base\n") + "java.util\njava.lang\n");
            if (version.equals("1.21.8")) {
                Files.writeString(javadoc.resolve("element-list"), "<!DOCTYPE html><title>Not found</title>");
            }
            String external = javadoc.toUri().toString();
            for (String kind : List.of("mddoclet", "webdoclet", "tsdoclet", "pydoclet")) {
                Path out = root.resolve(version).resolve(kind);
                var command = new ArrayList<>(List.of(
                    Path.of(System.getProperty("java.home"), "bin", "javadoc").toString(),
                    "-quiet", "-source", "21", "-doclet", "com.jsmacrosce.doclet.core." + kind + ".Main",
                    "-docletpath", docletClasspath, "-classpath", stubs + java.io.File.pathSeparator + docletClasspath,
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
            String md = Files.readString(root.resolve(version + "/mddoclet/content/fixture/" + version + "/classes/example/Api.md"));
            String html = Files.readString(root.resolve(version + "/webdoclet/fixture/example/Api.html"));
            String ts = Files.readString(root.resolve(version + "/tsdoclet/JsMacros-fixture.d.ts"));
            String py = Files.readString(root.resolve(version + "/pydoclet/Api.py"));
            contains(ts, "static readonly estimate: number;");
            contains(ts, "readonly names: JavaList<string>;");
            contains(ts, "static readonly LIMIT: number;");
            contains(ts, "writable: number;");
            excludes(ts, "readonly writable:");
            contains(py, "from typing import Final");
            contains(py, "estimate: Final[float]");
            contains(py, "names: Final[List[str]]");
            contains(py, "LIMIT: Final[int]");
            contains(py, "writable: float");
            contains(py, "\"\"\"A live estimate.\nSince: 1.0");
            excludes(py, "writable: Final[");
            contains(md, "text=\"read-only\"");
            contains(html, "class=\"flag readOnlyFlag\">Read-only");
            if (md.split("text=\"read-only\"", -1).length - 1 != 3) {
                throw new AssertionError("Expected exactly three read-only Markdown fields");
            }
            if (html.split(">Read-only", -1).length - 1 != 3) {
                throw new AssertionError("Expected exactly three read-only HTML fields");
            }
            Path markdownRoot = root.resolve(version + "/mddoclet/content/fixture/" + version);
            String libraries = Files.readString(markdownRoot.resolve("libraries.md"));
            contains(libraries, "- [Demo](./libraries/example/Api.LibraryFixture.md)\n  - [Result]");
            excludes(libraries, "Uncategorized");
            String nested = Files.readString(markdownRoot.resolve("libraries/example/Api.LibraryFixture.Result.md"));
            contains(nested, "not a separate global library");
            contains(nested, "## Constructors");
            excludes(nested, "Accessible in scripts via the global");
            contains(md, "[Example license](https://example.org/license)");
            contains(md, "`<a target=\"_blank\" href=\"https://example.org\">$literal</a>`");
            String base = version.equals("1.21.8") ? "https://mappings.dev/1.21.8/" : "https://mcsrc.dev/2/26.1.2/";
            String suffix = version.equals("1.21.8") ? ".html" : "";
            for (String output : List.of(md, html)) {
                contains(output, "href=\"" + base + "net/minecraft/client/multiplayer/PlayerInfo" + suffix + "\"");
                contains(output, "href=\"" + base + "net/minecraft/world/phys/HitResult$Type" + suffix + "\"");
                contains(output, (version.equals("1.21.8") ? "" : "java.base/") + "java/util/Map.Entry.html");
                contains(output, base + "net/minecraft/fixture/RecordType" + suffix);
                excludes(output, "MinecraftMappingViewer");
                excludes(output, "index.html?");
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
            contains(modern, "[Example license](https://example.org/license)");
            contains(modern, "<a target=\"_blank\" href=\"https://example.org\">$literal</a>");
        }
        System.out.println("DocletLinks: 8 actual doclet runs, read-only fields, external links and nested-library presentation passed");
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
