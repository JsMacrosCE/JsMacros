package com.jsmacrosce.jsmacros.core.extensions;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.jsmacrosce.Pair;
import com.jsmacrosce.jsmacros.core.Core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/** Discovers and initializes extensions associated with a core; queries other than {@code notLoaded()} trigger loading when completion has not been recorded. Returned extension sets are independent live backing sets; mutating one does not reconcile the others or registered libraries.
 * <pre>
 * const Client = Java.type("com.jsmacrosce.jsmacros.client.JsMacrosClient");
 * Chat.log(Client.clientCore.extensions.getAllLanguageExtensions().size());
 * </pre>
 */
public class ExtensionLoader {
    private final Set<Extension> extensions = new LinkedHashSet<>();
    private final Set<LanguageExtension> languageExtensions = new LinkedHashSet<>();
    private final Set<LibraryExtension> libraryExtensions = new LinkedHashSet<>();

    private final Core<?, ?> core;

    private ExtensionClassLoader classLoader;

    private LanguageExtension highestPriorityExtension;

    private volatile boolean loadingDone;

    /** Set while a load is in progress on this thread, so an extension's init query cannot start a second one. */
    private boolean loading;

    private final Path extPath;

    /** Directory the previous loader unpacked extensions into; removed on load with no migration. */
    private static final String LEGACY_TMP_DIRECTORY = "tmp";

    /** Mod logger, shared with the core's profile; the profile exists before this loader is created. */
    private final Logger logger;

    /** Stores the core and resolves its configured {@code Extensions} directory without loading extensions or creating that directory.
     * @param core associated core with initialized configuration
     */
    public ExtensionLoader(Core<?, ?> core) {
        this.core = core;
        this.extPath = core.config.configFolder.toPath().resolve("Extensions");
        this.logger = core.profile.LOGGER;
    }

    /** Loads extensions if needed, then checks for an exact extension-name match in the registered set.
     * @param name exact extension name
     * @return whether a registered extension has that name
     */
    public boolean isExtensionLoaded(String name) {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions.stream().anyMatch(e -> e.getExtensionName().equals(name));
    }

    /** Reports whether extension loading has not yet completed.
     * @return {@code true} until loading completion is recorded, including during a load attempt
     */
    public boolean notLoaded() {
        return !loadingDone;
    }

    /** Loads extensions if needed, then selects the language extension with the greatest priority while the cache is empty. Equal priorities have no guaranteed selection; a non-null cache is not invalidated by changes to the live language-extension set.
     * @return the cached or selected extension, or {@code null} if there are no language extensions
     */
    public LanguageExtension getHighestPriorityExtension() {
        if (notLoaded()) {
            loadExtensions();
        }
        if (highestPriorityExtension == null) {
            highestPriorityExtension = languageExtensions.stream().max(Comparator.comparingInt(LanguageExtension::getPriority)).orElse(null);
        }
        return highestPriorityExtension;
    }

    /** Loads extensions if needed and returns the live mutable backing set of all registered extensions.
     * @return the backing set of registered extensions
     */
    public Set<Extension> getAllExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions;
    }

    /** Loads extensions if needed and returns the live mutable backing set of language extensions.
     * @return the backing set of language extensions
     */
    public Set<LanguageExtension> getAllLanguageExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return languageExtensions;
    }

    /** Loads extensions if needed and returns the live mutable backing set of library extensions.
     * @return the backing set of library extensions
     */
    public Set<LibraryExtension> getAllLibraryExtensions() {
        if (notLoaded()) {
            loadExtensions();
        }
        return libraryExtensions;
    }

    /** Loads extensions if needed and selects a language extension matching the file; when multiple matches include {@code MATCH_WITH_NAME}, only those named matches are considered.
     * Candidates are sorted by negated {@code int} priority, normally preferring higher priorities; {@code Integer.MIN_VALUE} overflows and sorts first. Equal priorities have no guaranteed selection.
     * @param file candidate file passed to language extensions for matching
     * @return the selected matching extension, or {@code null} if none match
     */
    public @Nullable LanguageExtension getExtensionForFile(File file) {
        if (notLoaded()) {
            loadExtensions();
        }
        List<Pair<LanguageExtension.ExtMatch, LanguageExtension>> extensions = this.languageExtensions.stream().map(e -> new Pair<>(e.extensionMatch(file), e)).filter(p -> p.getT().isMatch()).collect(Collectors.toList());
        if (extensions.size() > 1) {
            List<Pair<LanguageExtension.ExtMatch, LanguageExtension>> extensionsByName = extensions.stream().filter(p -> p.getT() == LanguageExtension.ExtMatch.MATCH_WITH_NAME).collect(Collectors.toList());
            if (!extensionsByName.isEmpty()) {
                extensionsByName.sort(Comparator.comparingInt(e -> -e.getU().getPriority()));
                return extensionsByName.get(0).getU();
            }
        }
        if (!extensions.isEmpty()) {
            extensions.sort(Comparator.comparingInt(e -> -e.getU().getPriority()));
            return extensions.get(0).getU();
        }
        return null;
    }

    /** Loads extensions if needed and returns the first registered extension with an exact name match; duplicate names have no guaranteed selection order.
     * @param extName exact registered extension name
     * @return a matching extension, or {@code null} if none matches
     */
    public Extension getExtensionForName(String extName) {
        if (notLoaded()) {
            loadExtensions();
        }
        return extensions.stream().filter(e -> e.getExtensionName().equals(extName)).findFirst().orElse(null);
    }

    /** Attempts extension discovery, initialization, and library registration unless a class loader has already been assigned; in that case, logs a diagnostic and returns.
     * <p>
     * User archives are read from the {@code Extensions} directory. An archive that is itself an
     * entry of another archive — an extension bundled in the mod jar, or a dependency nested in an
     * extension jar — cannot be opened by {@link java.net.URLClassLoader} where it lies, so
     * {@link ExtensionArchiveCache} copies it to a file under {@code Extensions/.cache} first. A
     * nested jar entry is named from the source archive and entry metadata; a URL that provides no
     * reliable metadata is named from a digest of its bytes. A cache file is reused when it is
     * still a readable jar and its key still matches the source, and is removed at the end of the
     * load if this load did not use it.
     * <p>
     * Discovery order is fixed: providers the parent class loader can see come first, then the
     * archives named by the bundled manifest in the order it lists them, then the user's archives
     * by file name. Within an archive, providers are visited in the order its service file lists
     * them. An extension that shares its name with an already discovered one is ignored, and the
     * first one found wins — so a user archive cannot replace a bundled extension of the same
     * name. Dependencies are appended to the search path after the initial archives.
     * <p>
     * Each extension is handled on its own. A failure to load an extension's dependencies,
     * initialize it, or register its libraries is reported and skips that extension only; the
     * remaining extensions still load. The same applies to a provider that cannot be
     * instantiated. An extension's dependencies are all materialized and validated before any of
     * them joins the search path, so a later invalid archive cannot publish the earlier ones.
     * Once registration is complete, registered extensions receive the post-load callback.
     * A callback failure is logged without unregistering the extension.
     * <p>
     * A failure that prevents the load from completing does not mark loading complete, so a
     * later query can try again. Extensions registered before the failure are not rolled back, so
     * a retry starts beside that partial state rather than from an empty loader. A query made
     * while an extension is initializing returns the extensions registered so far; it does not
     * start another discovery pass. The current thread's context class loader is set to the
     * extension class loader before extensions initialize, because an extension may load classes
     * of its own while initializing, and is left set on success. A failed load restores the
     * caller's previous context class loader.
     */
    public synchronized void loadExtensions() {
        if (classLoader != null) {
            logger.debug("Extensions are already loaded; ignoring the repeated request");
            return;
        }
        if (loading) {
            return;
        }
        loading = true;
        ClassLoader previousContextLoader = Thread.currentThread().getContextClassLoader();
        ExtensionArchiveCache cache = null;
        ExtensionClassLoader loader = null;
        try {
            cache = ExtensionArchiveCache.open(extPath, logger);
            removeLegacyTemporaryDirectory();

            List<Path> bundledArchives = extractBundledArchives(cache);
            List<URL> searchPath = new ArrayList<>();
            for (Path archive : bundledArchives) {
                searchPath.add(archive.toUri().toURL());
            }
            // User archives follow the bundled ones so that a bundled extension still wins when
            // both declare the same name, matching the documented discovery order.
            searchPath.addAll(listLocalArchives());

            loader = new ExtensionClassLoader(searchPath.toArray(URL[]::new));
            for (Path archive : bundledArchives) {
                cache.markInUse(archive);
            }

            List<Extension> discovered = discover(loader);
            logger.debug("Discovered {} extension providers", discovered.size());

            List<Extension> ordered = deduplicate(discovered);

            Thread.currentThread().setContextClassLoader(loader);
            for (Extension extension : ordered) {
                initialize(extension, loader, cache);
            }
            // Report what was actually registered: a discovered extension can still be skipped
            // when its dependencies or its init fail.
            logger.info("Loaded {} extensions", extensions.size());
        } catch (Exception e) {
            // Do not leave the thread pointing at a loader that failed and is about to close.
            try {
                Thread.currentThread().setContextClassLoader(previousContextLoader);
            } catch (SecurityException restoreFailure) {
                e.addSuppressed(restoreFailure);
            }
            if (loader != null) {
                try {
                    loader.close();
                } catch (IOException ignored) {
                    // The loader is being discarded; a failure to close it is not actionable.
                }
            }
            throw new RuntimeException("Failed to load extensions", e);
        } finally {
            if (cache != null) {
                cache.removeUnusedArchives();
            }
            loading = false;
        }

        List<Extension> registered = List.copyOf(extensions);
        classLoader = loader;
        loadingDone = true;
        for (Extension extension : registered) {
            try {
                extension.onExtensionsLoaded(core);
            } catch (Throwable t) {
                report("Post-load callback failed for extension " + extension.getClass().getName(), t);
            }
        }
    }

    /**
     * Deletes the directory the previous extraction-based loader wrote into. Its contents were
     * copies of extension jars, so there is nothing to migrate; the new cache replaces it. A
     * failure is reported and does not prevent extensions from loading.
     */
    private void removeLegacyTemporaryDirectory() {
        Path legacy = extPath.resolve(LEGACY_TMP_DIRECTORY);
        if (!Files.exists(legacy, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try {
            Files.walkFileTree(legacy, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
                    if (failure != null) {
                        throw failure;
                    }
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
            logger.info("Removed the old extension extraction directory {}", legacy);
        } catch (IOException | SecurityException e) {
            logger.warn("Could not remove the old extension extraction directory {}", legacy, e);
        }
    }

    /**
     * Lists the archives the user placed in the {@code Extensions} directory, ordered by file name
     * so that discovery does not depend on the order the file system happens to report. The
     * loader's own {@code .cache} child is reserved for the archive cache and is skipped even when
     * it is not a directory.
     *
     * @return the local archive URLs; empty when the directory does not exist
     */
    private List<URL> listLocalArchives() {
        if (!Files.isDirectory(extPath)) {
            try {
                Files.createDirectories(extPath);
            } catch (IOException e) {
                // The directory is a convenience for users; a missing one is not fatal.
                return List.of();
            }
        }
        try (Stream<Path> files = Files.list(extPath)) {
            return files.filter(Files::isRegularFile)
                    // Normally a directory, but the cache path can also be a file when the cache
                    // is unusable; either way it is not an extension archive.
                    .filter(path -> !path.getFileName().toString().equals(ExtensionArchiveCache.CACHE_DIRECTORY_NAME))
                    .map(Path::getFileName)
                    .sorted()
                    .map(name -> {
                        try {
                            return extPath.resolve(name).toUri().toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Could not read the Extensions directory", e);
        }
    }

    /**
     * Materializes the extension archives that ship inside the mod jar and returns their file URLs.
     * <p>
     * A manifest that cannot be read is reported rather than thrown: the loader can still work
     * with the archives the user installed, and a broken build manifest is a packaging problem,
     * not a reason to disable every extension. The archives it names are returned in the listed
     * order. An archive that cannot be materialized or is not a readable jar fails the load,
     * because the resulting search path would silently omit an extension that the mod ships.
     *
     * @param cache extraction cache to materialize the archives with
     * @return the cached archive files, in manifest order
     * @throws IOException if a named archive cannot be materialized or is not a readable jar
     */
    private List<Path> extractBundledArchives(ExtensionArchiveCache cache) throws IOException {
        if (ExtensionLoader.class.getResource("/jsmacrosce.extension.json") == null) {
            report("The mod jar has no jsmacrosce.extension.json, so no bundled extension will be loaded", null);
            return List.of();
        }
        Set<URL> archives;
        try {
            archives = Extension.getDependenciesInternal(ExtensionLoader.class, "jsmacrosce.extension.json");
        } catch (IllegalStateException e) {
            report("Could not read the bundled extension manifest; no bundled extension will be loaded", e);
            return List.of();
        }
        List<Path> extracted = new ArrayList<>();
        for (URL archive : archives) {
            logger.debug("Extracting bundled extension archive: {}", archive);
            extracted.add(cache.materialize(archive));
        }
        return extracted;
    }

    /**
     * Instantiates every extension provider the loader can see. A provider that cannot be found
     * or instantiated is reported and skipped, and the providers after it are still loaded.
     *
     * @param loader loader to discover providers with
     * @return the discovered extensions, in discovery order
     */
    private List<Extension> discover(ExtensionClassLoader loader) {
        List<Extension> discovered = new ArrayList<>();
        Iterator<Extension> iterator = ServiceLoader.load(Extension.class, loader).iterator();
        while (true) {
            Extension extension;
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                extension = iterator.next();
            } catch (ServiceConfigurationError e) {
                report("Skipping an extension provider that could not be loaded", e);
                continue;
            }
            discovered.add(extension);
        }
        return discovered;
    }

    /**
     * Keeps the first extension found for each name and reports the ones it ignores.
     *
     * @param discovered the discovered extensions, in discovery order
     * @return the retained extensions, in discovery order
     */
    private List<Extension> deduplicate(List<Extension> discovered) {
        Map<String, Extension> byName = new LinkedHashMap<>();
        for (Extension extension : discovered) {
            String name;
            try {
                name = extension.getExtensionName();
            } catch (Throwable t) {
                report("Skipping extension provider " + extension.getClass().getName()
                        + " because its name could not be read", t);
                continue;
            }
            Extension kept = byName.putIfAbsent(name, extension);
            if (kept != null) {
                logger.warn("Ignoring duplicate extension '{}' from {}; already loaded {}",
                        name, extension.getClass().getName(), kept.getClass().getName());
            }
        }
        return new ArrayList<>(byName.values());
    }

    /**
     * Loads one extension's dependencies, initializes it, and registers the libraries it provides.
     * A failure at any step is reported and skips the extension without affecting the others.
     *
     * @param extension the extension to load
     * @param loader    loader to add the extension's dependencies to
     * @param cache     extraction cache to materialize nested dependencies with
     */
    private void initialize(Extension extension, ExtensionClassLoader loader, ExtensionArchiveCache cache) {
        try {
            loadDependencies(extension, loader, cache);
        } catch (Throwable t) {
            report("Failed to load dependencies for extension '" + extension.getExtensionName() + "'", t);
            return;
        }

        try {
            extension.init(core);
        } catch (Throwable t) {
            report("Failed to initialize extension '" + extension.getExtensionName() + "'", t);
            return;
        }

        if (extension instanceof LibraryExtension libExt) {
            try {
                core.libraryRegistry.addLibraries(libExt.getLibraries());
            } catch (Throwable t) {
                report("Failed to register libraries for extension '" + extension.getExtensionName() + "'", t);
                return;
            }
            libraryExtensions.add(libExt);
        }
        if (extension instanceof LanguageExtension langExt) {
            languageExtensions.add(langExt);
        }
        extensions.add(extension);
    }

    /**
     * Materializes and validates an extension's declared dependency archives, then appends them to
     * the loader's search path.
     * <p>
     * A declaration that does not name a {@code .jar} file is reported and skipped. Extension
     * descriptors occasionally list non-archive artifacts, and an extension should not be
     * disabled because of one. A declared jar that cannot be materialized or is not readable still
     * fails the extension.
     * <p>
     * The whole batch is materialized before any URL is published, so a later invalid archive
     * cannot leave the earlier dependencies visible to an extension that failed to load. Cache
     * files created for the failed batch are not marked in use and are removed by the cache
     * cleanup at the end of the load.
     *
     * @param extension the extension whose dependencies to load
     * @param loader    loader to add the dependencies to
     * @param cache     extraction cache to materialize nested dependencies with
     * @throws IOException if a declared jar cannot be materialized or is not a readable jar
     */
    private void loadDependencies(Extension extension, ExtensionClassLoader loader, ExtensionArchiveCache cache) throws IOException {
        Set<URL> dependencies = extension.getDependencies();
        if (dependencies.isEmpty()) {
            logger.debug("No dependencies for extension: {}", extension.getClass().getName());
            return;
        }
        List<Path> batch = new ArrayList<>();
        for (URL dependency : dependencies) {
            String name = dependencyFileName(dependency);
            if (!name.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                report("Skipping dependency '" + name + "' declared by extension '"
                        + extension.getExtensionName() + "' because it is not a jar archive", null);
                continue;
            }
            batch.add(cache.materialize(dependency));
        }
        if (batch.isEmpty()) {
            logger.debug("No usable dependency archives for extension: {}", extension.getExtensionName());
            return;
        }
        for (Path dependency : batch) {
            logger.debug("Adding dependency {} for extension {}", dependency.getFileName(), extension.getExtensionName());
            cache.markInUse(dependency);
            loader.addURL(dependency.toUri().toURL());
        }
    }

    /**
     * Returns the last path segment of a dependency URL. The URL is not opened: only the declared
     * name matters here, so a non-jar entry can be skipped before any extraction is attempted.
     *
     * @param dependency dependency URL from an extension descriptor
     * @return the entry or file name, or an empty string when the URL has no name
     */
    private static String dependencyFileName(URL dependency) {
        String path = dependency.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Reports a problem that did not stop the rest of the load.
     *
     * @param message description of the problem
     * @param error   the failure, or {@code null} when there is none
     */
    private void report(String message, Throwable error) {
        if (error == null) {
            logger.warn(message);
        } else {
            logger.warn(message, error);
        }
    }

    /** Loads extensions if needed and asks the loaded language extensions whether they recognize the object.
     * @param obj object to inspect
     * @return {@code true} if any loaded language extension recognizes it; {@code false} otherwise
     */
    public boolean isGuestObject(Object obj) {
        if (notLoaded()) {
            loadExtensions();
        }
        return languageExtensions.stream().anyMatch(e -> e.isGuestObject(obj));
    }

}
