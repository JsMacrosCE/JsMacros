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
import java.nio.file.StandardOpenOption;
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
    private final Set<Extension> extensions = new HashSet<>();
    private final Set<LanguageExtension> languageExtensions = new HashSet<>();
    private final Set<LibraryExtension> libraryExtensions = new HashSet<>();

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
     * Creates the {@code Extensions} directory if needed, uses its regular files as the initial class-loader search path, discovers extension providers, and extracts bundled extensions and provider dependencies into {@code Extensions/tmp}. Completion is recorded only after provider initialization and registration finish.
     * A failure after class-loader assignment can leave partial state and prevents another attempt; earlier failures do not set that sentinel. After dependency loading, the current thread's context class loader is changed and not restored, even if later initialization fails. Caught I/O, dependency, and initialization exceptions are wrapped in {@code RuntimeException}.
     */
    public synchronized void loadExtensions() {
        if (classLoader != null) {
            System.err.println("Extensions already loaded");
            return;
        }
        if (!Files.exists(extPath)) {
            try {
                Files.createDirectories(extPath);
            } catch (Exception e) {
                throw new RuntimeException("Could not create LanguageExtensions directory", e);
            }
        }

        URL[] urls;
        try (Stream<Path> files = Files.list(extPath)) {
            urls = files.filter(Files::isRegularFile).map(e -> {
                try {
                    return e.toUri().toURL();
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                }
            }).toArray(URL[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        classLoader = new ExtensionClassLoader(urls);

        // extract lib to dependencies folder
        Path dependenciesPath = extPath.resolve("tmp");
        try {
            Files.createDirectories(dependenciesPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // add internal extensions
        Set<URL> internalExtensions = Extension.getDependenciesInternal(ExtensionLoader.class, "jsmacrosce.extension.json");
        for (URL lib : internalExtensions) {
            System.out.println("Adding internal extension: " + lib);
            // copy resource to dependencies folder
            Path path = dependenciesPath.resolve(lib.getPath().substring(lib.getPath().lastIndexOf('/') + 1));
            try (InputStream stream = lib.openStream()) {
                Files.write(path, stream.readAllBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                System.out.println("Extracted dependency " + path);
                classLoader.addURL(path.toUri().toURL());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // load extensions
        extensions.addAll(ServiceLoader.load(Extension.class, classLoader)
            .stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toSet()));

        System.out.println("Loaded " + extensions.size() + " extensions");

        // load extension deps
        for (Extension extension : extensions) {
            try {
                Set<URL> deps = extension.getDependencies();
                if (deps.isEmpty()) {
                    System.out.println("No dependencies for extension: " + extension.getClass().getName());
                }
                for (URL lib : deps) {
                    // copy resource to dependencies folder
                    Path path = dependenciesPath.resolve(lib.getPath().substring(lib.getPath().lastIndexOf('/') + 1));
                    try (InputStream stream = lib.openStream()) {
                        Files.write(path, stream.readAllBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                        System.out.println("Extracted dependency " + path);
                        classLoader.addURL(path.toUri().toURL());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load extension dependencies for: " + extension.getExtensionName(), e);
            }
        }
        Thread.currentThread().setContextClassLoader(classLoader);
        for (Extension extension : extensions) {
            try {
                extension.init(core);
                if (extension instanceof LibraryExtension libExt) {
                    libraryExtensions.add(libExt);
                    for (Class<? extends BaseLibrary> lib : libExt.getLibraries()) {
                        core.libraryRegistry.addLibrary(lib);
                    }
                }
                if (extension instanceof LanguageExtension langExt) {
                    languageExtensions.add(langExt);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load extension: " + extension.getExtensionName(), e);
            }
        }
        loadingDone = true;
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
