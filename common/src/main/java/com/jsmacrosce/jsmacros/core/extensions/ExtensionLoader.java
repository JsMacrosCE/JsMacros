package com.jsmacrosce.jsmacros.core.extensions;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.Pair;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

    private boolean loadingDone;

    private final Path extPath;

    /** Stores the core and resolves its configured {@code Extensions} directory without loading extensions or creating that directory.
     * @param core associated core with initialized configuration
     */
    public ExtensionLoader(Core<?, ?> core) {
        this.core = core;
        this.extPath = core.config.configFolder.toPath().resolve("Extensions");
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

    /** Attempts extension discovery, initialization, and library registration unless a class loader has already been assigned; in that case, prints a diagnostic and returns.
     * <p>
     * Archives that the user dropped into the {@code Extensions} directory are read from disk, and
     * the extension archives shipped inside the mod jar are read into memory, because the JDK
     * cannot open an archive that is nested inside another archive. Extension dependencies are
     * read the same way, so nothing is ever copied to a temporary directory.
     * <p>
     * Discovery order is fixed: providers the parent class loader can see come first, then the
     * archives named by the bundled manifest in the order it lists them, then the user's archives
     * by file name. Within an archive, providers are visited in the order its service file lists
     * them. An extension that shares its name with an already discovered one is ignored, and the
     * first one found wins — so a user archive cannot replace a bundled extension of the same
     * name.
     * <p>
     * Each extension is handled on its own. A failure to load an extension's dependencies,
     * initialize it, or register its libraries is reported and skips that extension only; the
     * remaining extensions still load. The same applies to a provider that cannot be
     * instantiated.
     * <p>
     * A failure that prevents the load from starting at all leaves the loader unchanged, so a
     * later query can try again. The current thread's context class loader is set to the
     * extension class loader before extensions initialize, because an extension may load classes
     * of its own while initializing, and is left set afterwards.
     */
    public synchronized void loadExtensions() {
        if (classLoader != null) {
            System.err.println("Extensions already loaded");
            return;
        }

        ExtensionClassLoader loader = null;
        try {
            loader = new ExtensionClassLoader(listLocalArchives().toArray(URL[]::new));
            addBundledArchives(loader);

            List<Extension> discovered = discover(loader);
            System.out.println("Discovered " + discovered.size() + " extension providers");

            List<Extension> ordered = deduplicate(discovered);

            Thread.currentThread().setContextClassLoader(loader);
            for (Extension extension : ordered) {
                initialize(extension, loader);
            }
            // Report what was actually registered: a discovered extension can still be skipped
            // when its dependencies or its init fail.
            System.out.println("Loaded " + extensions.size() + " extensions");
        } catch (Exception e) {
            if (loader != null) {
                try {
                    loader.close();
                } catch (IOException ignored) {
                    // The loader is being discarded; a failure to close it is not actionable.
                }
            }
            throw new RuntimeException("Failed to load extensions", e);
        }

        classLoader = loader;
        loadingDone = true;
    }

    /**
     * Lists the archives the user placed in the {@code Extensions} directory, ordered by file name
     * so that discovery does not depend on the order the file system happens to report.
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
     * Reads the extension archives that ship inside the mod jar into the loader's memory.
     * <p>
     * A manifest that cannot be read is reported rather than thrown: the loader can still work
     * with the archives the user installed, and a broken build manifest is a packaging problem,
     * not a reason to disable every extension. The archives it names are added in the listed
     * order.
     *
     * @param loader loader to add the archives to
     * @throws IOException if a named archive cannot be read
     */
    private void addBundledArchives(ExtensionClassLoader loader) throws IOException {
        if (ExtensionLoader.class.getResource("/jsmacrosce.extension.json") == null) {
            report("The mod jar has no jsmacrosce.extension.json, so no bundled extension will be loaded", null);
            return;
        }
        Set<URL> archives;
        try {
            archives = Extension.getDependenciesInternal(ExtensionLoader.class, "jsmacrosce.extension.json");
        } catch (IllegalStateException e) {
            report("Could not read the bundled extension manifest; no bundled extension will be loaded", e);
            return;
        }
        for (URL archive : archives) {
            String name = fileName(archive);
            System.out.println("Adding bundled extension archive: " + name);
            loader.addMemoryJar(name, readAll(archive));
        }
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
            Extension kept = byName.putIfAbsent(extension.getExtensionName(), extension);
            if (kept != null) {
                System.out.println("Ignoring duplicate extension '" + extension.getExtensionName() + "' from "
                        + extension.getClass().getName() + "; already loaded " + kept.getClass().getName());
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
     */
    private void initialize(Extension extension, ExtensionClassLoader loader) {
        try {
            loadDependencies(extension, loader);
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
                for (Class<? extends BaseLibrary> lib : libExt.getLibraries()) {
                    core.libraryRegistry.addLibrary(lib);
                }
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
     * Reads an extension's declared dependency archives into the loader's memory.
     *
     * @param extension the extension whose dependencies to load
     * @param loader    loader to add the dependencies to
     * @throws IOException if a dependency cannot be read
     */
    private void loadDependencies(Extension extension, ExtensionClassLoader loader) throws IOException {
        Set<URL> dependencies = extension.getDependencies();
        if (dependencies.isEmpty()) {
            System.out.println("No dependencies for extension: " + extension.getClass().getName());
            return;
        }
        for (URL dependency : dependencies) {
            String name = fileName(dependency);
            System.out.println("Adding dependency " + name + " for extension " + extension.getExtensionName());
            loader.addMemoryJar(name, readAll(dependency));
        }
    }

    /**
     * Reads the whole content of a URL.
     *
     * @param url the URL to read
     * @return the bytes read
     * @throws IOException if the URL cannot be read
     */
    private static byte[] readAll(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return stream.readAllBytes();
        }
    }

    /**
     * Returns the file name part of a URL's path.
     *
     * @param url the URL to inspect
     * @return the last path segment
     */
    private static String fileName(URL url) {
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Reports a problem that did not stop the rest of the load.
     *
     * @param message description of the problem
     * @param error   the failure, or {@code null} when there is none
     */
    private void report(String message, Throwable error) {
        System.err.println("[JsMacrosExtensionManager] " + message + (error == null ? "" : ": " + error));
        if (error != null) {
            error.printStackTrace();
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
