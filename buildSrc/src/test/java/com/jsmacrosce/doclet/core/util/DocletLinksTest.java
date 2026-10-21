package com.jsmacrosce.doclet.core.util;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.jsmacrosce.doclet.core.model.TypeKind;
import com.jsmacrosce.doclet.core.model.TypeRef;

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
        Path syntaxException = write(root.resolve("source/com/mojang/brigadier/exceptions/CommandSyntaxException.java"),
            "package com.mojang.brigadier.exceptions; public class CommandSyntaxException extends Exception {}");
        Path record = write(root.resolve("source/net/minecraft/fixture/RecordType.java"),
            "package net.minecraft.fixture; public record RecordType(int value) {}");
        Path commandSource = write(root.resolve("source/net/minecraft/commands/CommandSourceStack.java"),
            """
            package net.minecraft.commands;
            import java.util.Collection;
            /** A stand-in for the game's own command source. */
            public class CommandSourceStack {
                /** The entities the source has selected. */
                public Collection<String> getSelectedEntities() { return null; }
                /** An intermediary-style name with no meaning outside the obfuscated game. */
                public void method_1234() {}
            }
            """);
        Path libraryAnnotation = write(root.resolve("source/example/Library.java"),
            "package example; public @interface Library { String value(); }");
         Path eventAnnotation = write(root.resolve("source/example/Event.java"),
            "package example; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface Event { String value(); boolean cancellable() default false; }");
        Path nullableAnnotation = write(root.resolve("source/org/jetbrains/annotations/Nullable.java"),
            "package org.jetbrains.annotations; import java.lang.annotation.*; @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER}) public @interface Nullable {}");
        Path replaceReturnAnnotation = write(root.resolve("source/com/jsmacrosce/doclet/DocletReplaceReturn.java"),
            "package com.jsmacrosce.doclet; import java.lang.annotation.*; @Target(ElementType.METHOD) public @interface DocletReplaceReturn { String value(); }");
        Path ignoreAnnotation = write(root.resolve("source/com/jsmacrosce/doclet/DocletIgnore.java"),
            "package com.jsmacrosce.doclet; import java.lang.annotation.*; @Retention(RetentionPolicy.SOURCE) @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR}) public @interface DocletIgnore {}");
        int compile = ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "-d", stubs.toString(), player.toString(), hit.toString(), profile.toString(), syntaxException.toString(), record.toString(), commandSource.toString(), libraryAnnotation.toString(), eventAnnotation.toString(), nullableAnnotation.toString(), replaceReturnAnnotation.toString(), ignoreAnnotation.toString());
        if (compile != 0) throw new AssertionError("Fixture compilation failed: " + compile);
        String fixtureSource = """
            package example;
            import net.minecraft.client.multiplayer.PlayerInfo;
            import net.minecraft.world.phys.HitResult;
            import com.mojang.authlib.GameProfile;
            import com.mojang.brigadier.exceptions.CommandSyntaxException;
             /** A fixture preserving {@code a & b < c} and {@code \"player\"}; ranges 0–2, dash —, café, and 😀.
              * Literal escape {@code \\\\u2013} remains code; single-backslash {@code SINGLE_BACKSLASH_ESCAPE} remains code too.
             * References {@link PlayerInfo} and {@link net.minecraft.fixture.RecordType}.
             * References {@link HiddenType} and {@link #hiddenMember()}.
             * May throw {@link CommandSyntaxException}.
             * Under <a target="_blank" href="https://example.org/license">Example license</a>.
             * Literal: {@code <a target="_blank" href="https://example.org">$literal</a>}.
              * example:
               * <pre>const name = \"player\";</pre>
               * @deprecated Use {@link #nullableResult()} instead of this class.
             */
              @Deprecated public class Api {
                  /** Generic fixture docs.
                   * @param <T> values use {@code T} and {@code </code><bad>&}.
                   */
                   public static class GenericFixture<T> {
                       /** Builds a typed value.
                        * @param <T> constructor type marker {@code <T>} and {@link String}.
                        * @param value the constructor value argument
                        */
                       public <T> GenericFixture(T value) {}
                       /** Uses an ordinary value.
                       * @param value the ordinary {@code value < safe}
                       */
                        public void accept(String value) {}
                        /** Converts values.
                         * @param T actual value parameter named T
                         * @param other the other value
                         * @param <R> result marker {@code <R>}.
                         * @param <T> source marker {@code <T>} and {@link String}.
                         */
                       public <T, R> R convert(String T, R other) { return other; }
                       /** Has no type-parameter documentation. */
                       public <X> X undocumented(X value) { return value; }
                       /** Has an empty type-parameter description.
                        * @param <X>
                        */
                       public <X> X emptyTag(X value) { return value; }
                        /** Has an unmatched type-parameter tag.
                        * @param <Z> unmatched marker
                        * @param value the unmatched-control value argument
                        */
                       public <Y> Y unmatched(Y value) { return value; }
                   }
                  public static class NoTypeParameters {}
                  @com.jsmacrosce.doclet.DocletIgnore public static class HiddenType {
                      public static class NestedChild {}
                  }
                  @com.jsmacrosce.doclet.DocletIgnore public interface HiddenContract { @com.jsmacrosce.doclet.DocletIgnore void hiddenInherited(); }
                  public static class VisibleImplementation implements HiddenContract { public void hiddenInherited() {} }
                  @com.jsmacrosce.doclet.DocletIgnore public void hiddenMember() {}
                 public void getCustomTabSuggestions() {}
                 @com.jsmacrosce.doclet.DocletIgnore public void getCustomTabSugggestions() {}
                 /** Returns and accepts event aliases. */
                   public SyntheticEvents.WrappedScript rawEvent(SyntheticEvents.WrappedScript event) { return event; }
                 /** Returns and accepts a parameterized event alias. */
                   public SyntheticEvents.WrappedScript<Integer, Integer, Integer> parameterizedEvent(
                       SyntheticEvents.WrappedScript<Integer, Integer, Integer> event) { return event; }
                 /** @deprecated Use {@link #nullableResult()} with {@code safeCall()}. */
                 @Deprecated public void oldCall() {}
                /** A static helper that takes an argument. */
                public static int staticHelper(int value) { return value; }
                /** A static helper that takes nothing. */
                public static String staticNoArgs() { return ""; }
                /** Creates an API fixture.
                 * @param player the {@code player & profile} entry
                 */
                public Api(java.util.UUID uuid, PlayerInfo player) {}
                /** Creates an API fixture with an optional name. */
                public Api(@org.jetbrains.annotations.Nullable String optionalName) {}
                /** A nested Minecraft type. */
                public HitResult.Type nested;
                /** Nested generic type arguments, including a wildcard. */
                public java.util.Map<String, java.util.List<?>> nestedGenerics;
                /** A mutable mapping field. */
                public java.util.Map<String, Integer> mutableMap;
                /** A final mapping reference. */
                 public final java.util.Map<String, Integer> finalMap = java.util.Map.of();
                 /** Nullable mutable field contract. */
                 @org.jetbrains.annotations.Nullable public String nullableField;
                 /** Nullable final field contract. */
                 @org.jetbrains.annotations.Nullable public final String nullableFinalField = null;
                 /** Nullable generic field contract. */
                 @org.jetbrains.annotations.Nullable public java.util.Map<String, Integer> nullableMapField;
                 /** A field that starts out null. */
                 public String nullInitialized = null;
                 /** A field that starts out with a value. */
                 public String initialized = "value";
                 /** Old field contract.
                  * @deprecated Use {@link #nullableResult()} instead.
                  */
                 @Deprecated public String oldField;
                /** A raw mapping field. */
                 public java.util.Map rawMap;
                 /** Accepts erased Java marker types. */
                 public void erasedTypes(Enum<?> value, java.lang.annotation.Annotation marker) {}
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
                 /** Last receive time. */
                 public final long recvTime = 0;
                 /** Current ticks per second. */
                 public final double tps = 0;
                 /** An ignored primitive field. */
                 @com.jsmacrosce.doclet.DocletIgnore public final long ignoredPrimitive = 0;
                 /** Old constructor contract.
                  * @deprecated Use {@link #nullableResult()} before constructing this fixture.
                  */
                 @Deprecated public Api() {}
                 /** Accepts and returns mappings: 0–2, dash —, café, and 😀.
                  * @param value input – café 😀
                  * @return output — café 😀
                  */
                public java.util.Map<String, Integer> mapRoundTrip(java.util.Map<String, Integer> value) { return value; }
                 /** Lower, upper, unbounded and nested wildcard controls. */
                 public <R, V> void wildcardDirections(
                     java.util.function.Function<? super R, ? extends V> lowerUpper,
                     java.util.List<? extends Number> upper,
                     java.util.List<?> unbounded,
                     java.util.List<java.util.List<? super String>> nested) {}
                 /** Ordinary type parameter upper-bound control. */
                 public <T extends Number> void typeVariableBound(T value) {}
                /** Returns a nullable mapping. */
                @org.jetbrains.annotations.Nullable public java.util.Map<String, Integer> nullableMap() { return null; }
                /** Returns a nullable result.
                 * @return the value, or {@code null} when unavailable
                 */
                @org.jetbrains.annotations.Nullable public String nullableResult() { return null; }
                /** Returns an integral short. */
                @org.jetbrains.annotations.Nullable public short nullableShort() { return 0; }
                /** Returns an integral byte. */
                @org.jetbrains.annotations.Nullable public byte nullableByte() { return 0; }
                /** Accepts optional callback and integral values. */
                public void optionalValues(@org.jetbrains.annotations.Nullable Runnable callback, short shortValue, byte byteValue) {}
                /** Accepts an optional short. */
                public void optionalShort(@org.jetbrains.annotations.Nullable Short shortValue) {}
                /** Returns a nullable result with an overridden script type.
                 * @return the value, or {@code null} when unavailable
                 */
                @org.jetbrains.annotations.Nullable
                @com.jsmacrosce.doclet.DocletReplaceReturn("'Int' | 'String' | 'Double' | 'Boolean' | 'Object' | null")
                public String nullableOverriddenResult() { return null; }
                /** Returns a result.
                 * @return the value when available, or {@code null} when lookup
                 *         fails before a result can be returned
                 */
                public Object continuedReturn() { return null; }
                /** A nested library type. */
                public java.util.Map.Entry<String, PlayerInfo> entry;
                /** An external library, not a class in the Minecraft JAR. */
                public GameProfile profile;
                /** An unresolved external type. */
                public CommandSyntaxException syntaxException;
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
            """.replace("SINGLE_BACKSLASH_ESCAPE", "\\" + "u005c" + "u2013");
        Path api = write(root.resolve("source/example/Api.java"), fixtureSource);
        Path angleFixture = write(root.resolve("source/example/AngleFixture.java"), """
            package example;
            /** Plain before with < and >, {@code left < right && right > left}, and {@code \"&lt; &gt; &amp;\"}.
             * <pre>
             * {@code const marker = \"&lt; &gt; &amp;\";}
             * if (left > right && left < limit) use(`value > ${left}`);
             * </pre>
             * Between examples <code>first < second</code> stays inline.
             * <pre class="language-typescript">
             * const typed: string = \"<>&\";
             * if (typed.length > 0 && typed.length < limit) use(typed);
             * </pre>
             * Plain after with < and >.
             */
            public class AngleFixture {
                /** Member raw HTML example comparison.
                 * <pre>if (left > right && left < limit) use(\"<>&\");</pre>
                 */
                public void memberExample() {}
            }
            """);
        Path eventFixture = write(root.resolve("source/example/SyntheticEvents.java"), """
            package example;
            /** Synthetic event aliases used to exercise generic event rendering. */
            public class SyntheticEvents {
                /** Generic event payload. */
                @Event("WrappedScript")
                public static class WrappedScript<T extends Number, U extends T, R extends U> {
                    /** First event argument. */
                    @com.jsmacrosce.doclet.DocletReadOnly public T arg1;
                    /** Second event argument. */
                    @com.jsmacrosce.doclet.DocletReadOnly public U arg2;
                    /** Event result. */
                    public R result;
                    /** Transforms the payload. */
                    public R transform(T first, U second, R value) { return value; }
                }
                /** Nongeneric event control. */
                @Event("Plain")
                public static class Plain {}
                /** Cancellable event control. */
                 @Event(value = "Cancelled", cancellable = true)
                 public static class Cancelled {}
             }
             """);
        Path overrideFixture = write(root.resolve("source/example/OverridingSource.java"), """
            package example;
            import java.util.Collection;
            import net.minecraft.commands.CommandSourceStack;
            /** A source that overrides Minecraft calls with answers the client already has. */
            public class OverridingSource extends CommandSourceStack {
                /** The entity under the crosshair, as its id. */
                @Override
                public Collection<String> getSelectedEntities() { return null; }
                /** An intermediary-style name with no meaning outside the obfuscated game. */
                @Override
                public void method_1234() {}
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
                command.add(angleFixture.toString());
                command.add(eventFixture.toString());
                command.add(overrideFixture.toString());
                Path log = root.resolve(version + "-" + kind + ".log");
                int result = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(log.toFile()).start().waitFor();
                if (result != 0) throw new AssertionError("Javadoc failed: " + Files.readString(log));
            }
            String md = Files.readString(root.resolve(version + "/mddoclet/content/fixture/" + version + "/classes/example/Api.md"));
            String html = Files.readString(root.resolve(version + "/webdoclet/fixture/example/Api.html"));
            String ts = Files.readString(root.resolve(version + "/tsdoclet/JsMacros-fixture.d.ts"));
             String py = Files.readString(root.resolve(version + "/pydoclet/Api.py"));
             String eventTs = Files.readString(root.resolve(version + "/tsdoclet/JsMacros-fixture.d.ts"));
              String eventPy = Files.readString(root.resolve(version + "/pydoclet/SyntheticEvents_WrappedScript.py"));
              String eventMd = Files.readString(root.resolve(version + "/mddoclet/content/fixture/" + version + "/events/example/SyntheticEvents.WrappedScript.md"));
             String eventHtml = Files.readString(root.resolve(version + "/webdoclet/fixture/example/SyntheticEvents.WrappedScript.html"));
             assertEventAlias(eventTs, eventPy, eventMd, eventHtml);
             String overrideMd = Files.readString(root.resolve(version + "/mddoclet/content/fixture/" + version + "/classes/example/OverridingSource.md"));
             String overridePy = Files.readString(root.resolve(version + "/pydoclet/OverridingSource.py"));
             assertMinecraftOverrides(overrideMd, overridePy, ts);
             String wildcardMd = markdownMember(md, "example.Api_wildcardDirections-Function-List-List-List-");
            String angleMd = Files.readString(root.resolve(version + "/mddoclet/content/fixture/" + version + "/classes/example/AngleFixture.md"));
            String angleHtml = Files.readString(root.resolve(version + "/webdoclet/fixture/example/AngleFixture.html"));
            String anglePy = Files.readString(root.resolve(version + "/pydoclet/AngleFixture.py"));
            String angleTs = Files.readString(root.resolve(version + "/tsdoclet/JsMacros-fixture.d.ts"));
            contains(html, "<span class=\"type deadType\">CommandSyntaxException</span>");
            excludes(html, "HiddenType.html");
            excludes(html, "NestedChild.html");
            excludes(html, "hiddenMember-");
            excludes(html, "getCustomTabSugggestions");
             contains(html, "getCustomTabSuggestions");
             assertShortFieldLink(html, "recvTime");
             assertShortFieldLink(html, "tps");
             assertNoShortField(html, "ignoredPrimitive");
             contains(shortField(html, "mutableMap"), "href=\"#mutableMap\"");
             contains(shortMethod(html, "nullableResult"), "href=\"#nullableResult-\"");
            contains(html, "ranges 0–2, dash —, café, and 😀.");
            contains(html, "Literal escape <code class=\"inlineCode\">\\\\u2013</code>");
            contains(html, "single-backslash <code class=\"inlineCode\">\\u2013</code> remains code too.");
              int angleMembers = angleMd.indexOf("## Constructors");
              if (angleMembers < 0) throw new AssertionError("Missing AngleFixture member boundary");
              String angleOverview = angleMd.substring(0, angleMembers);
              contains(angleOverview, "Plain before with &lt; and &gt;, `left < right && right > left`, and `\"&lt; &gt; &amp;\"`.");
              excludes(angleOverview, "Plain before with < and >");
              List<String> fences = markdownFences(angleOverview);
              if (fences.size() != 2) throw new AssertionError("Expected exactly two class-overview fences: " + fences);
              assertEquals(fences.get(0), "const marker = \"&lt; &gt; &amp;\";\nif (left > right && left < limit) use(`value > ${left}`);", "JavaScript fence payload");
              assertEquals(fences.get(1), "const typed: string = \"<>&\";\nif (typed.length > 0 && typed.length < limit) use(typed);", "TypeScript fence payload");
              contains(angleOverview, "Plain after with &lt; and &gt;.");
              excludes(angleOverview, "Plain after with < and >");
              if (!(angleOverview.indexOf("Plain before") < angleOverview.indexOf("```js")
                  && angleOverview.indexOf("```js") < angleOverview.indexOf("Between examples")
                  && angleOverview.indexOf("Between examples") < angleOverview.indexOf("```ts")
                  && angleOverview.indexOf("```ts") < angleOverview.indexOf("Plain after"))) {
                  throw new AssertionError("AngleFixture class overview contexts are out of order");
              }
              int htmlMemberBoundary = angleHtml.indexOf("<h3>Constructors</h3>");
              if (htmlMemberBoundary < 0) throw new AssertionError("Missing AngleFixture HTML member boundary");
              String angleHtmlOverview = angleHtml.substring(0, htmlMemberBoundary);
              List<String> htmlFences = htmlPreText(angleHtmlOverview);
              if (htmlFences.size() != 2) throw new AssertionError("Expected exactly two class-overview HTML PRE blocks: " + htmlFences);
              // The decoded PRE text is a compatibility check, not proof that
              // entity-like source text round-trips literally in legacy HTML.
              assertEquals(htmlFences.get(0), "const marker = \"< > &\";\nif (left > right && left < limit) use(`value > ${left}`);", "HTML JavaScript PRE text");
              assertEquals(htmlFences.get(1), "const typed: string = \"<>&\";\nif (typed.length > 0 && typed.length < limit) use(typed);", "HTML TypeScript PRE text");
              List<String> rawHtmlFences = rawHtmlPreText(angleHtmlOverview);
              if (rawHtmlFences.size() != 2) throw new AssertionError("Expected exactly two raw HTML PRE blocks: " + rawHtmlFences);
              assertEquals(rawHtmlFences.get(0), "<code class=\"inlineCode\">const marker = \"&lt; &gt; &amp;\";</code>\nif (left > right && left < limit) use(`value > ${left}`);", "Raw HTML JavaScript PRE payload");
              assertEquals(rawHtmlFences.get(1), "const typed: string = \"<>&\";\nif (typed.length > 0 && typed.length < limit) use(typed);", "Raw HTML TypeScript PRE payload");
              contains(angleHtmlOverview, "<code class=\"inlineCode\">left < right && right > left</code>");
              contains(angleHtmlOverview, "Plain after with < and >.");
              contains(anglePy, "if (left > right && left < limit)");
              contains(anglePy, "const marker = \"&lt; &gt; &amp;\";");
              contains(angleTs, "if (typed.length > 0 && typed.length < limit)");
              contains(angleTs, "const marker = \"&lt; &gt; &amp;\";");
              String memberExample = htmlMember(angleHtml, "memberExample-");
              contains(memberExample, "if (left > right && left < limit) use(\"<>&\");");
            contains(html, "input – café 😀");
            contains(html, "output — café 😀");
            excludes(html, "– remains code");
            contains(html, "HiddenType");
            contains(html, "hiddenMember()");
            String search = Files.readString(root.resolve(version + "/webdoclet/fixture/search-list"));
            excludes(search, "HiddenType");
            excludes(search, "NestedChild");
            contains(search, "VisibleImplementation#hiddenInherited()");
            String implementation = Files.readString(root.resolve(version + "/webdoclet/fixture/example/Api.VisibleImplementation.html"));
            contains(implementation, "hiddenInherited");
            excludes(html, "href=\"\" class=\"type deadType\">CommandSyntaxException");
            contains(html, ">Map</a>&lt;<div class=\"typeParameter\"><a href=");
            contains(html, ">List</a>&lt;<div class=\"typeParameter\"><p class=\"type primitiveType\">?</p></div>&gt;");
             // Keep this Map type-argument wrapper regression scoped away from
             // legacy generic method titles, which prepend a literal '<'.
             excludes(htmlMember(html, "mapRoundTrip-Map-"), "<<div class=\"typeParameter\">");
            String genericPy = Files.readString(root.resolve(version + "/pydoclet/Api_GenericFixture.py"));
            String genericMd = Files.readString(root.resolve(version + "/mddoclet/content/fixture/" + version + "/classes/example/Api.GenericFixture.md"));
            String genericHtml = Files.readString(root.resolve(version + "/webdoclet/fixture/example/Api.GenericFixture.html"));
             contains(ts, "static readonly estimate: number;");
             assertEquals(normalizedSignature(wildcardMd),
                 "wildcardDirections(Function<?superR,?extendsV>lowerUpper,List<?extendsNumber>upper,List<?>unbounded,List<List<?superString>>nested):void",
                 "Markdown wildcard signature");
             String typeVariableMd = markdownMember(md, "example.Api_typeVariableBound-T-");
             assertEquals(normalizedSignature(typeVariableMd), "typeVariableBound(TextendsNumbervalue):void", "Markdown type-variable signature");
             contains(ts, "wildcardDirections<R, V>(lowerUpper: MethodWrapper<any, any, any>, upper: JavaList<any>, unbounded: JavaList<any>, nested: JavaList<JavaList<any>>): void;");
             contains(py, "def wildcardDirections(self, lowerUpper: Function, upper: List[Any], unbounded: List[Any], nested: List[List[Any]]) -> None:");
             String wildcardHtml = htmlMember(html, "wildcardDirections-Function-List-List-List-");
             assertEquals(htmlParameterType(wildcardHtml, "lowerUpper"), "Function<?,?>", "Legacy HTML lowerUpper type");
             assertEquals(htmlParameterType(wildcardHtml, "upper"), "List<?>", "Legacy HTML upper type");
             assertEquals(htmlParameterType(wildcardHtml, "unbounded"), "List<?>", "Legacy HTML unbounded type");
             assertEquals(htmlParameterType(wildcardHtml, "nested"), "List<List<?>>", "Legacy HTML nested type");
             String typeVariableHtml = htmlMember(html, "typeVariableBound-T-");
             contains(typeVariableHtml, "classExtends");
             contains(typeVariableHtml, "Number");
             contains(ts, "readonly names: JavaList<string>;");
             contains(ts, "readonly recvTime: number;");
             contains(ts, "readonly tps: number;");
             contains(ts, "static readonly LIMIT: number;");
            contains(ts, "writable: number;");
            excludes(ts, "readonly writable:");
            contains(ts, "constructor(optionalName: string | null);");
            contains(ts, "nullableResult(): string | null;");
            contains(ts, "optionalValues(callback: MethodWrapper | null, shortValue: short, byteValue: byte): void;");
            contains(ts, "optionalShort(shortValue: short | null): void;");
            contains(ts, "nullableOverriddenResult(): 'Int' | 'String' | 'Double' | 'Boolean' | 'Object' | null;");
            contains(py, "from typing import Final");
            contains(genericPy, "Type Parameters:");
            contains(genericPy, "T: values use T and </code><bad>&");
            contains(genericPy, "Args:\n            value: the ordinary value < safe");
            contains(genericMd, "## Type Parameters");
            contains(genericMd, "<code>T</code>: values use <code>T</code> and <code>&lt;/code&gt;&lt;bad&gt;&amp;</code>.");
            contains(genericMd, "<li><code>value</code>: the ordinary <code>value &lt; safe</code></li>");
            excludes(genericMd, "## Parameters");
            contains(genericHtml, "Type Parameters");
            contains(genericHtml, "&lt;T&gt;");
             contains(genericHtml, "<code class=\"inlineCode\">T</code> and <code class=\"inlineCode\">&lt;/code&gt;&lt;bad&gt;&amp;</code>");
             excludes(genericHtml, "<code class=\"inlineCode\"></code><bad>&");
             // Member type-parameter documentation must stay attached to its exact
             // overload; a value parameter named T is still an ordinary argument.
             String pyConvert = pythonMember(genericPy, "def convert(");
             String pyConstructor = pythonMember(genericPy, "def __init__(self, value: T)");
              assertMemberSections(pyConvert, "source marker <T> and String.", "result marker <R>.", "actual value parameter named T", "the other value", "Type Parameters:", "Args:");
              assertOrdered(pyConvert, "Type Parameters:", "source marker <T> and String.", "result marker <R>.");
              assertMemberSections(pyConstructor, "constructor type marker <T> and String.", null, "the constructor value argument", null, "Type Parameters:", "Args:");
              String pyUndocumented = pythonMember(genericPy, "def undocumented(");
              String pyEmpty = pythonMember(genericPy, "def emptyTag(");
              assertNoTypeParameters(pyUndocumented);
              assertNoTypeParameters(pyEmpty);
              contains(pyUndocumented, "def undocumented(self, value: X)");
              contains(pyUndocumented, "Has no type-parameter documentation.");
              contains(pyEmpty, "def emptyTag(self, value: X)");
              contains(pyEmpty, "Has an empty type-parameter description.");
              String pyUnmatched = pythonMember(genericPy, "def unmatched(");
              assertNoTypeParameters(pyUnmatched);
              excludes(pyUnmatched, "unmatched marker");
              excludes(pyUnmatched, "value: unmatched marker");
             String mdConvert = markdownMember(genericMd, "example.Api.GenericFixture_convert-String-R-");
             String mdConstructor = markdownMember(genericMd, "example.Api.GenericFixture_constructor-T-");
              assertMemberSections(mdConvert, "source marker", "result marker", "actual value parameter named T", "the other value", "<p><strong>Type Parameters:</strong></p>", "<p><strong>Parameters:</strong></p>");
              assertOrdered(mdConvert, "<p><strong>Type Parameters:</strong></p>", "source marker", "result marker");
              assertMemberSections(mdConstructor, "constructor type marker", null, "the constructor value argument", null, "<p><strong>Type Parameters:</strong></p>", "<p><strong>Parameters:</strong></p>");
              String mdUndocumented = markdownMember(genericMd, "example.Api.GenericFixture_undocumented-X-");
              String mdEmpty = markdownMember(genericMd, "example.Api.GenericFixture_emptyTag-X-");
              assertNoTypeParameters(mdUndocumented);
              assertNoTypeParameters(mdEmpty);
              contains(mdUndocumented, "Has no type-parameter documentation.");
              contains(mdEmpty, "Has an empty type-parameter description.");
              String mdUnmatched = markdownMember(genericMd, "example.Api.GenericFixture_unmatched-Y-");
              assertNoTypeParameters(mdUnmatched);
              excludes(mdUnmatched, "unmatched marker");
              String htmlConvert = htmlMember(genericHtml, "convert-String-R-");
              String htmlConstructor = htmlMember(genericHtml, "constructor-T-");
              String htmlAccept = htmlMember(genericHtml, "accept-String-");
              excludes(htmlAccept, "<div></div>");
              assertHtmlMemberSections(htmlConvert, "source marker", "result marker", "actual value parameter named T", "the other value");
              assertOrdered(balancedElement(htmlConvert, "<section class=\"typeParameterDocs\"", "section"), "", "source marker", "result marker");
              assertHtmlMemberSections(htmlConstructor, "constructor type marker", null, "the constructor value argument", null);
             contains(pyConvert, "source marker <T> and String.");
             contains(mdConvert, "<code>&lt;T&gt;</code>");
             contains(htmlConvert, "&lt;T&gt;");
             contains(mdConvert, "href=\"file:");
             contains(mdConvert, "/java/lang/String.html\"");
             contains(htmlConvert, "href=\"file:");
             contains(htmlConvert, "/java/lang/String.html\"");
              String htmlUndocumented = htmlMember(genericHtml, "undocumented-X-");
              String htmlEmpty = htmlMember(genericHtml, "emptyTag-X-");
              assertNoTypeParameters(htmlUndocumented);
              assertNoTypeParameters(htmlEmpty);
              excludes(htmlUndocumented, "<div></div>");
              excludes(htmlEmpty, "<div></div>");
              contains(htmlUndocumented, "Has no type-parameter documentation.");
              contains(htmlEmpty, "Has an empty type-parameter description.");
              String htmlUnmatched = htmlMember(genericHtml, "unmatched-Y-");
              assertNoTypeParameters(htmlUnmatched);
              excludes(htmlUnmatched, "unmatched marker");
              excludes(htmlUnmatched, "<div></div>");
             contains(ts, "@template T source marker");
             contains(ts, "@template R result marker");
             contains(ts, "@template T constructor type marker");
              assertTsMember(ts, "convert<T, R>(T: string, other: R)", "@template T source marker", "@param T actual value parameter named T");
              assertTsMember(ts, "convert<T, R>(T: string, other: R)", "@template R result marker", "@param other the other value");
               assertTsMember(ts, "constructor<T>(value: T)", "@template T constructor type marker", "@param value the constructor value argument");
              contains(ts, "@template Z unmatched marker");
               assertTsMember(ts, "unmatched<Y>(value: Y)", "@template Z unmatched marker", "@param value");
              assertTsNoTemplate(ts, "undocumented<X>(value: X)");
              assertTsNoTemplate(ts, "emptyTag<X>(value: X)");
            excludes(Files.readString(root.resolve(version + "/pydoclet/Api_NoTypeParameters.py")), "Type Parameters:");
            contains(ts, "@template T values use T and </code><bad>&.");
            if (ts.indexOf("@template T values use T and </code><bad>&.") != ts.lastIndexOf("@template T values use T and </code><bad>&.")) {
                throw new AssertionError("Type parameter JSDoc was duplicated in TypeScript output");
            }
             contains(py, "estimate: Final[float]");
             contains(py, "names: Final[List[str]]");
             contains(py, "recvTime: Final[float]");
             contains(py, "tps: Final[float]");
            contains(py, "from typing import MutableMapping");
            contains(py, "mutableMap: MutableMapping[str, int]");
            contains(py, "finalMap: Final[MutableMapping[str, int]]");
            contains(py, "nullableField: str | None");
            contains(py, "nullableFinalField: Final[str | None]");
            contains(py, "nullableMapField: MutableMapping[str, int] | None");
            contains(py, "Nullable mutable field contract.");
            contains(py, "Nullable final field contract.");
            contains(py, "Nullable generic field contract.");
            contains(py, "rawMap: MutableMapping[Any, Any]");
            contains(py, "def mapRoundTrip(self, value: MutableMapping[str, int]) -> MutableMapping[str, int]:");
            contains(py, "def erasedTypes(self, value: Any, marker: Any) -> None:");
            contains(py, "Deprecated:\n            Use #nullableResult() with safeCall().");
            contains(html, "class=\"deprecatedDescription\"");
            contains(html, "Use <a href=\"#nullableResult-\" class=\"type\">Api#nullableResult()</a> with <code class=\"inlineCode\">safeCall()</code>.");
            contains(html, "Use <a href=\"#nullableResult-\" class=\"type\">Api#nullableResult()</a> instead of this class.");
            contains(html, "Use <a href=\"#nullableResult-\" class=\"type\">Api#nullableResult()</a> instead.");
            contains(html, "Use <a href=\"#nullableResult-\" class=\"type\">Api#nullableResult()</a> before constructing this fixture.");
            contains(ts, "@deprecated Use {@link nullableResult} instead of this class.");
            contains(ts, "@deprecated Use {@link nullableResult} instead.");
            contains(ts, "@deprecated Use {@link nullableResult} before constructing this fixture.");
            contains(py, "Deprecated:\n        Use #nullableResult() instead of this class.");
            contains(py, "Deprecated:\n        Use #nullableResult() instead.");
            contains(py, "Deprecated:\n            Use #nullableResult() before constructing this fixture.");
            contains(md, "<Badge type=\"danger\" text=\"deprecated\" />");
            contains(md, "<p><strong>Deprecated:</strong> Use <a href=\"#example.Api_nullableResult-\">nullableResult()</a> instead.</p>");
            contains(md, "<p><strong>Deprecated:</strong> Use <a href=\"#example.Api_nullableResult-\">nullableResult()</a> before constructing this fixture.</p>");
            contains(py, "def nullableMap(self) -> MutableMapping[str, int] | None:");
            // A field that starts out null holds null until something fills it in.
            contains(py, "nullInitialized: str | None");
            contains(ts, "nullInitialized: string | null;");
            contains(py, "initialized: str");
            excludes(py, "initialized: str | None");
            excludes(ts, "initialized: string | null;");
            assertStaticPythonMethod(py, "staticHelper", "def staticHelper(value: int) -> int:");
            assertStaticPythonMethod(py, "staticNoArgs", "def staticNoArgs() -> str:");
            // The Markdown reference marks static members the way the legacy HTML does.
            contains(md, "<span class=\"syntax-token-keyword\">static</span> <span class=\"syntax-token-method\">staticHelper</span>");
            contains(md, "<span class=\"syntax-token-keyword\">static</span> <span class=\"syntax-token-method\">staticNoArgs</span>");
            contains(html, "class=\"flag staticFlag\">Static");
            contains(py, "LIMIT: Final[int]");
            contains(py, "writable: float");
            contains(py, "def nullableResult(self) -> str | None:");
            contains(py, "ranges 0–2, dash —, café, and 😀.");
            contains(py, "input – café 😀");
            contains(py, "output — café 😀");
            contains(py, "def __init__(self, optionalName: str | None) -> None:");
            contains(py, "def nullableShort(self) -> int | None:");
            contains(py, "def nullableByte(self) -> int | None:");
            contains(py, "def optionalValues(self, callback: Runnable | None, shortValue: int, byteValue: int) -> None:");
            contains(py, "def optionalShort(self, shortValue: float | None) -> None:");
            contains(py, "def nullableOverriddenResult(self) -> str | None:");
            contains(py, "def continuedReturn(self) -> object:");
            contains(py, "            the value when available, or null when lookup\n            fails before a result can be returned");
            contains(py, "\"\"\"A live estimate.\nSince: 1.0");
            excludes(py, "writable: Final[");
            contains(md, "text=\"read-only\"");
            contains(html, "class=\"flag readOnlyFlag\">Read-only");
            if (md.split("text=\"read-only\"", -1).length - 1 != 7) {
                throw new AssertionError("Expected exactly seven read-only Markdown fields");
            }
            if (html.split(">Read-only", -1).length - 1 != 7) {
                throw new AssertionError("Expected exactly seven read-only HTML fields");
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
        TypeRef lowerBound = new TypeRef(TypeKind.WILDCARD, "?", "?", List.of(), false, false,
            new TypeRef(TypeKind.DECLARED, "String", "java.lang.String", List.of(), false, false, null, false), false,
            TypeRef.WildcardBound.SUPER);
        TypeRef nullableLowerBound = lowerBound.withNullable(true);
        if (nullableLowerBound.wildcardBound() != TypeRef.WildcardBound.SUPER || !nullableLowerBound.nullable()) {
            throw new AssertionError("withNullable changed wildcard direction");
        }
        TypeRef legacyEightArgument = new TypeRef(TypeKind.WILDCARD, "?", "?", List.of(), false, false,
            lowerBound.bounds(), false);
        if (legacyEightArgument.wildcardBound() != TypeRef.WildcardBound.EXTENDS) {
            throw new AssertionError("Eight-argument TypeRef compatibility constructor changed");
        }
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

    private static void assertEquals(String actual, String expected, String label) {
        if (!actual.equals(expected)) throw new AssertionError(label + " mismatch\nExpected:\n" + expected + "\nActual:\n" + actual);
    }

    private static List<String> markdownFences(String overview) {
        String[] lines = overview.split("\\R", -1);
        List<String> fences = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String opening = lines[i].stripLeading();
            if (lines[i].length() - opening.length() > 3
                || !(opening.equals("```js") || opening.equals("```ts"))) continue;
            String marker = opening.substring(0, 3);
            List<String> payload = new ArrayList<>();
            boolean closed = false;
            while (++i < lines.length) {
                String line = lines[i];
                String candidate = line.stripLeading();
                int indentation = line.length() - candidate.length();
                if (indentation <= 3 && candidate.equals(marker)) {
                    closed = true;
                    break;
                }
                payload.add(line);
            }
            if (!closed) throw new AssertionError("Unclosed Markdown example fence: " + opening);
            fences.add(normalizeDocletLines(payload));
        }
        return fences;
    }

    private static List<String> htmlPreText(String overview) {
        var matcher = java.util.regex.Pattern.compile("(?is)<pre\\b[^>]*>(.*?)</pre\\s*>").matcher(overview);
        List<String> blocks = new ArrayList<>();
        while (matcher.find()) {
            // These fixture PRE blocks only wrap the {@code} node in the
            // generated inline-code element. Preserve all other angle text.
            String text = matcher.group(1)
                .replace("<code class=\"inlineCode\">", "").replace("</code>", "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
            List<String> lines = new ArrayList<>(List.of(text.split("\\R", -1)));
            while (!lines.isEmpty() && lines.getFirst().isBlank()) lines.removeFirst();
            while (!lines.isEmpty() && lines.getLast().isBlank()) lines.removeLast();
            blocks.add(normalizeDocletLines(lines));
        }
        return blocks;
    }

    private static List<String> rawHtmlPreText(String overview) {
        var matcher = java.util.regex.Pattern.compile("(?is)<pre\\b[^>]*>(.*?)</pre\\s*>").matcher(overview);
        List<String> blocks = new ArrayList<>();
        while (matcher.find()) {
            List<String> lines = new ArrayList<>(List.of(matcher.group(1).split("\\R", -1)));
            while (!lines.isEmpty() && lines.getFirst().isBlank()) lines.removeFirst();
            while (!lines.isEmpty() && lines.getLast().isBlank()) lines.removeLast();
            blocks.add(normalizeDocletLines(lines));
        }
        return blocks;
    }

    private static String normalizeDocletLines(List<String> lines) {
        int commonIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.isBlank()) continue;
            int indent = 0;
            while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) indent++;
            commonIndent = Math.min(commonIndent, indent);
        }
        if (commonIndent == Integer.MAX_VALUE) commonIndent = 0;
        final int indentToRemove = commonIndent;
        return lines.stream().map(line -> line.substring(Math.min(indentToRemove, line.length())).stripTrailing())
            .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String pythonMember(String output, String signature) {
        int start = output.indexOf(signature);
        if (start < 0) throw new AssertionError("Missing Python member " + signature);
        int end = output.indexOf("\n    @overload", start + signature.length());
        if (end < 0) end = output.indexOf("\n    pass", start + signature.length());
        if (end < 0) throw new AssertionError("Unbounded Python member " + signature);
        return output.substring(start, end);
    }

    private static String markdownMember(String output, String signature) {
        String marker = "id=\"" + signature + "\"";
        int id = output.indexOf(marker);
        int start = id < 0 ? -1 : output.lastIndexOf("<div", id);
        if (start < 0) throw new AssertionError("Missing Markdown member " + signature);
        int end = balancedDivEnd(output, start);
        return output.substring(start, end);
    }

    private static String htmlMember(String output, String signature) {
        String marker = "id=\"" + signature + "\"";
        int id = output.indexOf(marker);
        int start = id < 0 ? -1 : output.lastIndexOf("<div", id);
        if (start < 0) throw new AssertionError("Missing HTML member " + signature);
        int end = balancedDivEnd(output, start);
        return output.substring(start, end);
    }

    private static void assertShortFieldLink(String html, String field) {
        String summary = shortField(html, field);
        var link = java.util.regex.Pattern.compile("<a href=\"([^\"]*)\">" + java.util.regex.Pattern.quote(field)
            + "</a>").matcher(summary);
        if (!link.find()) throw new AssertionError("Missing short-field link for " + field + ": " + summary);
        assertEquals(link.group(1), "#" + field, "Legacy HTML short-field href for " + field);
        if (!html.contains("id=\"" + field + "\"")) {
            throw new AssertionError("Missing detailed legacy HTML field anchor for " + field);
        }
    }

    private static String shortField(String html, String field) {
        var matcher = java.util.regex.Pattern.compile("(?s)<div class=\\\"shortField shortClassItem\\\">\\s*<a href=\\\"[^\\\"]*\\\">"
            + java.util.regex.Pattern.quote(field) + "</a>.*?</div>").matcher(html);
        if (!matcher.find()) throw new AssertionError("Missing legacy HTML short-field entry for " + field);
        return matcher.group();
    }

    private static void assertNoShortField(String html, String field) {
        var matcher = java.util.regex.Pattern.compile("(?s)<div class=\\\"shortField shortClassItem\\\">.*?</div>").matcher(html);
        while (matcher.find()) {
            if (matcher.group().contains(">" + field + "</a>")) {
                throw new AssertionError("Unexpected ignored field in legacy HTML summary: " + field);
            }
        }
    }

    private static String shortMethod(String html, String method) {
        int start = html.indexOf("<div class=\"methodShorts\">");
        if (start < 0) throw new AssertionError("Missing legacy HTML method summary");
        String summary = html.substring(start, balancedDivEnd(html, start));
        if (!summary.contains(method)) throw new AssertionError("Missing method summary entry " + method);
        return summary;
    }

    private static String normalizedSignature(String member) {
        String marker = "<div class=\"overload-sig\"><code>";
        int start = member.indexOf(marker);
        int end = start < 0 ? -1 : member.indexOf("</code>", start + marker.length());
        if (start < 0 || end < 0) throw new AssertionError("Missing scoped Markdown overload signature:\n" + member);
        return normalizeTypeText(member.substring(start + marker.length(), end));
    }

    private static String htmlParameterType(String member, String parameter) {
        var row = java.util.regex.Pattern.compile("(?s)<tr[^>]*>\\s*<td[^>]*>\\s*" + java.util.regex.Pattern.quote(parameter)
            + "\\s*</td>\\s*<td[^>]*>(.*?)</td>").matcher(member);
        if (!row.find()) throw new AssertionError("Missing legacy HTML parameter row for " + parameter + ":\n" + member);
        return normalizeTypeText(row.group(1));
    }

    private static String normalizeTypeText(String html) {
        return html.replaceAll("<[^>]*>", "").replace("&lt;", "<").replace("&gt;", ">")
            .replaceAll("\\s+", "");
    }

    private static int balancedDivEnd(String output, int start) {
        int depth = 0;
        int cursor = start;
        while (cursor < output.length()) {
            int open = output.indexOf("<div", cursor);
            int close = output.indexOf("</div>", cursor);
            if (close < 0) throw new AssertionError("Missing closing div for fragment beginning at " + start);
            if (open >= 0 && open < close) {
                int endTag = output.indexOf('>', open);
                if (endTag < 0) throw new AssertionError("Malformed div opening tag");
                depth++;
                cursor = endTag + 1;
            } else {
                depth--;
                cursor = close + "</div>".length();
                if (depth == 0) return cursor;
                if (depth < 0) throw new AssertionError("Unbalanced div fragment");
            }
        }
        throw new AssertionError("Unclosed div fragment");
    }

    private static void assertMemberSections(String member, String typeOne, String typeTwo, String valueOne, String valueTwo, String typeHeading, String valueHeading) {
        int type = member.indexOf(typeHeading);
        int values = member.indexOf(valueHeading);
        if (type < 0 || values < 0) throw new AssertionError("Missing type/value sections in member:\n" + member);
        int typeEnd = nextSection(member, typeHeading, valueHeading);
        int valuesEnd = nextSection(member, valueHeading, typeHeading);
        String typeContent = member.substring(type, typeEnd);
        String valueContent = member.substring(values, valuesEnd);
        if (!typeContent.contains(typeOne) || (typeTwo != null && !typeContent.contains(typeTwo))) throw new AssertionError("Missing type parameter description:\n" + member);
        if (!valueContent.contains(valueOne) || (valueTwo != null && !valueContent.contains(valueTwo))) throw new AssertionError("Missing value parameter description:\n" + member);
        if (typeContent.contains(valueOne) || (valueTwo != null && typeContent.contains(valueTwo))
            || valueContent.contains(typeOne) || (typeTwo != null && valueContent.contains(typeTwo))) throw new AssertionError("Type/value documentation leaked across sections:\n" + member);
    }

    private static int nextSection(String member, String heading, String alternate) {
        int end = member.length();
        for (String candidate : List.of("Returns:", "<p><strong>Returns:", "Type Parameters:", "Args:", "Parameters:")) {
            int at = member.indexOf(candidate, member.indexOf(heading) + heading.length());
            if (at >= 0 && at < end) end = at;
        }
        int other = member.indexOf(alternate, member.indexOf(heading) + heading.length());
        if (other >= 0 && other < end) end = other;
        int close = member.indexOf("\n        \"\"\"", member.indexOf(heading) + heading.length());
        if (close >= 0 && close < end) end = close;
        return end;
    }

    private static void assertNoTypeParameters(String member) {
        if (member.contains("Type Parameters") || member.contains("## Type Parameters")) throw new AssertionError("Unexpected type-parameter section:\n" + member);
    }

    private static void assertStaticPythonMethod(String py, String name, String signature) {
        int at = py.indexOf("def " + name + "(");
        if (at < 0) throw new AssertionError("Missing Python method " + name);
        String member = pythonMember(py, "def " + name + "(");
        contains(member, signature);
        // A static method is called on the class, so it takes no receiver.
        excludes(member, "(" + name + "(self");
        int blockStart = py.lastIndexOf("\n\n", at);
        String decorators = py.substring(blockStart < 0 ? 0 : blockStart, at);
        contains(decorators, "@staticmethod");
    }

    private static void assertMinecraftOverrides(String md, String py, String ts) {
        // A mapped Minecraft override is a real call a script can make and belongs in the reference.
        contains(md, "getSelectedEntities");
        contains(py, "def getSelectedEntities(");
        contains(ts, "getSelectedEntities(");
        // An intermediary-style name carries no meaning outside the obfuscated game.
        excludes(md, "method_1234");
        excludes(py, "method_1234");
        excludes(ts, "method_1234");
    }

    private static void assertEventAlias(String ts, String py, String md, String html) {
        String alias = scoped(ts, "interface WrappedScript", "\n    }");
        contains(alias, "interface WrappedScript<T extends number = any, U extends T = any, R extends U = any> extends BaseEvent {");
        contains(alias, "readonly arg1: T;");
        contains(alias, "readonly arg2: U;");
        contains(alias, "First event argument.");
        contains(alias, "Second event argument.");
        contains(alias, "result: R;");
        contains(alias, "transform(first: T, second: U, value: R): R;");
        excludes(alias, "interface WrappedScript<T extends number, U extends T, R extends U>");
        contains(ts, "interface Plain extends BaseEvent {");
        excludes(scoped(ts, "interface Plain", "\n    }"), "<");
        contains(ts, "interface Cancelled extends BaseEvent, Cancellable {");
        String qualifiedClass = scoped(ts, "export class SyntheticEvents$WrappedScript<T extends number, U extends T, R extends U>", "\n    }");
        contains(qualifiedClass, "export class SyntheticEvents$WrappedScript<T extends number, U extends T, R extends U>");
        contains(qualifiedClass, "readonly arg1: T;");
        contains(qualifiedClass, "readonly arg2: U;");
        contains(qualifiedClass, "result: R;");
        contains(qualifiedClass, "transform(first: T, second: U, value: R): R;");
        String api = scoped(ts, "export class Api", "\n}");
        String rawEvent = scoped(api, "rawEvent(", ";");
         assertEquals(rawEvent, "rawEvent(event: Events.WrappedScript): Events.WrappedScript", "raw event alias signature");
        String parameterizedEvent = scoped(api, "parameterizedEvent(", ";");
         assertEquals(parameterizedEvent, "parameterizedEvent(event: Events.WrappedScript): Events.WrappedScript", "parameterized event alias signature");
         int pyClassStart = py.indexOf("class SyntheticEvents_WrappedScript(Generic[T, U, R]):");
         if (pyClassStart < 0) throw new AssertionError("Missing Python event class header");
         String pyClass = py.substring(pyClassStart);
         assertEquals(pyClass.substring(0, pyClass.indexOf('\n')), "class SyntheticEvents_WrappedScript(Generic[T, U, R]):", "Python event class header");
        contains(pyClass, "Generic event payload.");
        contains(pyClass, "arg1: Final[T]");
        contains(pyClass, "First event argument.");
        contains(pyClass, "arg2: Final[U]");
        contains(pyClass, "Second event argument.");
        contains(pyClass, "result: R");
        contains(pyClass, "Event result.");
         String transform = pythonMember(pyClass, "def transform(");
         contains(transform, "def transform(self, first: T, second: U, value: R) -> R:");
         contains(transform, "Transforms the payload.");

        String mdArg1 = markdownMember(md, "example.SyntheticEvents.WrappedScript_arg1");
        String mdArg2 = markdownMember(md, "example.SyntheticEvents.WrappedScript_arg2");
        String mdResult = markdownMember(md, "example.SyntheticEvents.WrappedScript_result");
        String mdTransform = markdownMember(md, "example.SyntheticEvents.WrappedScript_transform-T-U-R-");
        contains(mdArg1, "First event argument.");
        contains(mdArg2, "Second event argument.");
        contains(mdResult, "Event result.");
        contains(mdTransform, "Transforms the payload.");
        contains(md, "text=\"read-only\"");

        String htmlArg1 = htmlMember(html, "arg1");
        String htmlArg2 = htmlMember(html, "arg2");
        String htmlResult = htmlMember(html, "result");
        String htmlTransform = htmlMember(html, "transform-T-U-R-");
        contains(htmlArg1, "First event argument.");
        contains(htmlArg2, "Second event argument.");
        contains(htmlResult, "Event result.");
        contains(htmlTransform, "Transforms the payload.");
        contains(htmlArg1, "class=\"flag readOnlyFlag\">Read-only");
        contains(htmlArg2, "class=\"flag readOnlyFlag\">Read-only");
    }

    private static String scoped(String output, String startMarker, String endMarker) {
        int start = output.indexOf(startMarker);
        int end = start < 0 ? -1 : output.indexOf(endMarker, start + startMarker.length());
        if (start < 0 || end < 0) throw new AssertionError("Missing scoped output section: " + startMarker);
        return output.substring(start, end);
    }

    private static void assertOrdered(String fragment, String heading, String first, String second) {
        int section = heading.isEmpty() ? 0 : fragment.indexOf(heading);
        int firstAt = fragment.indexOf(first, section < 0 ? 0 : section + heading.length());
        int secondAt = fragment.indexOf(second, section < 0 ? 0 : section + heading.length());
        if (section < 0 || firstAt < 0 || secondAt < 0 || firstAt >= secondAt) {
            throw new AssertionError("Expected declared type-parameter order " + first + " then " + second + ":\n" + fragment);
        }
    }

    private static void assertHtmlMemberSections(String member, String typeOne, String typeTwo, String valueOne, String valueTwo) {
        String types = balancedElement(member, "<section class=\"typeParameterDocs\"", "section");
        String values = balancedElement(member, "<table class=\"paramTable\"", "table");
        if (!types.contains(typeOne) || (typeTwo != null && !types.contains(typeTwo))) throw new AssertionError("Missing HTML type descriptions:\n" + types);
        if (!values.contains(valueOne) || (valueTwo != null && !values.contains(valueTwo))) throw new AssertionError("Missing HTML value descriptions:\n" + values);
        if (types.contains(valueOne) || (valueTwo != null && types.contains(valueTwo)) || values.contains(typeOne)
            || (typeTwo != null && values.contains(typeTwo))) throw new AssertionError("HTML type/value description leaked between sections");
    }

    private static String balancedElement(String fragment, String opening, String name) {
        int start = fragment.indexOf(opening);
        if (start < 0) throw new AssertionError("Missing " + name + " section in member fragment");
        String openTag = "<" + name;
        String closeTag = "</" + name + ">";
        int depth = 0;
        int cursor = start;
        while (cursor < fragment.length()) {
            int open = fragment.indexOf(openTag, cursor);
            int close = fragment.indexOf(closeTag, cursor);
            if (close < 0) throw new AssertionError("Missing closing " + name);
            if (open >= 0 && open < close) {
                int endTag = fragment.indexOf('>', open);
                if (endTag < 0) throw new AssertionError("Malformed " + name + " opening tag");
                depth++;
                cursor = endTag + 1;
            } else {
                depth--;
                cursor = close + closeTag.length();
                if (depth == 0) return fragment.substring(start, cursor);
                if (depth < 0) throw new AssertionError("Unbalanced " + name + " section");
            }
        }
        throw new AssertionError("Unclosed " + name + " section");
    }

    private static void assertTsMember(String output, String signature, String template, String valueParam) {
        int sig = output.indexOf(signature);
        int start = output.lastIndexOf("/**", sig);
        int end = output.indexOf("*/", start);
        if (sig < 0 || start < 0 || end < 0 || end > sig) throw new AssertionError("Missing JSDoc before " + signature);
        String doc = output.substring(start, end);
        if (!doc.contains(template) || !doc.contains(valueParam)) throw new AssertionError("Missing member-local TS docs:\n" + doc);
    }

    private static void assertTsNoTemplate(String output, String signature) {
        int sig = output.indexOf(signature);
        int start = output.lastIndexOf("/**", sig);
        int end = output.indexOf("*/", start);
        if (sig < 0 || start < 0 || end < 0 || end > sig || output.substring(start, end).contains("@template")) throw new AssertionError("Unexpected TS template docs for " + signature);
    }
}
