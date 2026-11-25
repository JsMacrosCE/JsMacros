package com.jsmacrosce.jsmacros.core.extensions;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Copies archives that are nested inside other archives to files that a
 * {@link java.net.URLClassLoader} can read.
 *
 * <p>A nested jar entry keeps a key derived from cheap source metadata — the containing jar's
 * file URL, size and modification time plus the entry name, declared size and CRC — so a load
 * can recognize a file it wrote earlier without opening or inflating the entry. A URL that
 * does not expose that metadata is named by the SHA-256 of its bytes instead, because forms
 * such as NeoForge's {@code union:} URLs report a constant modification time.
 *
 * <p>The cache is not trusted. A hit is reused only when it is a readable jar and, for a
 * metadata-keyed entry, of the expected size. A fresh copy is streamed to EOF and must match
 * the declared size and, for a jar entry, the declared CRC before it is moved into place.
 * Anything else is replaced.
 *
 * <p>A file this load did not use is removed at the end of the load, including after a
 * failure, but only once it has been idle for a long time. A hit refreshes that timestamp when
 * it is reused, so a concurrent load's recently accessed archive is not mistaken for a stale
 * one; a file that was handed to the class loader is always kept.
 */
final class ExtensionArchiveCache {

    /** Name of this cache's directory inside the {@code Extensions} directory. */
    static final String CACHE_DIRECTORY_NAME = ".cache";

    /** Upper bound for one materialized archive, above the largest runtime currently shipped. */
    private static final long MAX_ARCHIVE_BYTES = 512L * 1024 * 1024;

    private static final String CACHE_KEY_VERSION = "extension-archive-cache-v1";

    /**
     * Files another load did not use are only removed after this idle period. A shorter
     * load-start cutoff cannot distinguish a concurrent loader's cache file from a stale one.
     */
    private static final long MAX_UNUSED_AGE_MS = TimeUnit.DAYS.toMillis(30);

    private final Path directory;
    private final Logger logger;
    /** True when {@link #directory} is a per-session fallback rather than {@code Extensions/.cache}. */
    private final boolean temporaryDirectory;
    /** Wall-clock reference for the idle-age cleanup cutoff. */
    private final long loadStartedAt;
    private final Set<Path> inUse = new HashSet<>();
    /** Temp files this cache instance created and has not moved or deleted yet. */
    private final Set<Path> temporaryFiles = new HashSet<>();

    private ExtensionArchiveCache(Path directory, Logger logger, boolean temporaryDirectory) {
        this.directory = directory;
        this.logger = logger;
        this.temporaryDirectory = temporaryDirectory;
        this.loadStartedAt = System.currentTimeMillis();
    }

    /**
     * Opens the extension cache, falling back to a per-session temporary directory when
     * {@code Extensions/.cache} cannot be created or written.
     *
     * @param extPath the {@code Extensions} directory
     * @param logger  logger for cache diagnostics
     * @return an opened cache
     * @throws IOException if neither the persistent nor the temporary cache can be created
     */
    static ExtensionArchiveCache open(Path extPath, Logger logger) throws IOException {
        Path directory = extPath.resolve(CACHE_DIRECTORY_NAME);
        try {
            Files.createDirectories(directory);
            // Creating the directory does not prove it is writable, for example when it
            // already exists with read-only permissions.
            Files.deleteIfExists(Files.createTempFile(directory, ".writable-", ".tmp"));
            return new ExtensionArchiveCache(directory, logger, false);
        } catch (IOException | SecurityException e) {
            Path temporary;
            try {
                temporary = Files.createTempDirectory("jsmacrosce-extensions-");
            } catch (IOException temporaryFailure) {
                temporaryFailure.addSuppressed(e);
                throw new IOException("Could not create the extension cache at " + directory, temporaryFailure);
            }
            temporary.toFile().deleteOnExit();
            logger.warn("Could not use the extension cache at {}; using {} for this session instead",
                    directory, temporary, e);
            return new ExtensionArchiveCache(temporary, logger, true);
        }
    }

    /**
     * Returns a file containing the archive named by a URL.
     * <p>
     * A {@code file:} URL is already a file and is returned after validation. A {@code jar:}
     * entry is copied to the cache under a key derived from its container and entry metadata.
     * Any other URL is copied to a file named by the SHA-256 of its bytes, because the
     * connection's reported length and modification time can be stale or constant.
     *
     * @param source archive URL to materialize
     * @return a readable jar file
     * @throws IOException if the archive cannot be read, is too large, or is not a jar
     */
    Path materialize(URL source) throws IOException {
        if ("file".equalsIgnoreCase(source.getProtocol())) {
            Path file = fileOf(source);
            verifyArchive(file, source);
            return file;
        }
        if ("jar".equalsIgnoreCase(source.getProtocol())) {
            URLConnection connection = source.openConnection();
            if (connection instanceof JarURLConnection jarConnection) {
                return materializeJarEntry(jarConnection, source);
            }
            // A mod loader may register its own handler for jars inside jars. Its content
            // can still be materialized, but not from the metadata JarURLConnection exposes.
            return materializeStream(source);
        }
        return materializeStream(source);
    }

    /** Marks a materialized archive as used by this load so cleanup keeps it. */
    void markInUse(Path archive) {
        inUse.add(normalize(archive));
    }

    /**
     * Deletes cache files that this load did not use, ignoring cleanup failures.
     * <p>
     * A file is only a candidate when it has been idle for a long time; a concurrent load that
     * reuses it refreshes that timestamp. This avoids treating another loader's active archive as
     * stale merely because it was created before this load started.
     */
    void removeUnusedArchives() {
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".jar"))
                    .filter(file -> !inUse.contains(normalize(file)))
                    .filter(this::isRemovable)
                    .forEach(this::deleteUnused);
        } catch (IOException | SecurityException e) {
            logger.debug("Could not clean the extension cache at {}", directory, e);
        }
        for (Path temporary : new ArrayList<>(temporaryFiles)) {
            deleteQuietly(temporary);
        }
        temporaryFiles.clear();
    }

    private boolean isRemovable(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis() < loadStartedAt - MAX_UNUSED_AGE_MS;
        } catch (IOException | SecurityException e) {
            logger.debug("Could not read the timestamp of extension cache file {}", file, e);
            // Keeping an unknown file is safer than deleting one another load may be using.
            return false;
        }
    }

    private Path materializeJarEntry(JarURLConnection connection, URL source) throws IOException {
        connection.setUseCaches(false);
        URL jarUrl = connection.getJarFileURL();
        if (!"file".equalsIgnoreCase(jarUrl.getProtocol())) {
            // A nested jar whose container is not itself a file cannot contribute the size
            // and modification time the stable key normally uses.
            return materializeStream(source);
        }
        String entryName = connection.getEntryName();
        if (entryName == null) {
            throw new IOException("Extension archive " + source + " does not name a jar entry");
        }

        Path jarPath = fileOf(jarUrl);

        JarFile jar;
        try {
            jar = connection.getJarFile();
        } catch (IOException e) {
            throw new IOException("Could not read extension archive " + source, e);
        }
        try (jar) {
            JarEntry entry = jar.getJarEntry(entryName);
            if (entry == null) {
                throw new IOException("Extension archive " + source + " does not contain " + entryName);
            }
            long declaredSize = entry.getSize();
            // A cache file can itself be the container of a nested dependency — a bundled
            // extension jar holds its own dependencies. Identify the container by its file
            // name in that case, not its modification time: a cache hit refreshes the timestamp
            // below, and that would otherwise change every dependent's key on every load.
            String container;
            Path parent = normalize(jarPath).getParent();
            if (parent != null && parent.equals(normalize(directory))) {
                container = "cache/" + jarPath.getFileName();
            } else {
                container = jarUrl.toExternalForm() + "\n"
                        + Files.size(jarPath) + "\n"
                        + Files.getLastModifiedTime(jarPath).toMillis();
            }
            String descriptor = CACHE_KEY_VERSION + "\njar\n"
                    + container + "\n"
                    + entryName + "\n"
                    + declaredSize + "\n"
                    + entry.getCrc();
            Path cached = cachedPath(descriptor);
            checkDeclaredSize(declaredSize, source);
            if (isUsableCacheFile(cached, declaredSize)) {
                logger.debug("Reusing cached extension archive {}", cached);
                touch(cached);
                return cached;
            }
            // A hit with the wrong length or one that no longer opens as a jar is stale.
            deleteQuietly(cached);
            return extract(jar, entry, cached, source, declaredSize);
        }
    }

    private Path materializeStream(URL source) throws IOException {
        URLConnection connection = source.openConnection();
        connection.setUseCaches(false);
        long declaredSize = connection.getContentLengthLong();
        checkDeclaredSize(declaredSize, source);

        Path temporary = createTemporaryFile();
        try {
            MessageDigest digest = sha256Digest();
            CopyResult copied;
            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(temporary)) {
                copied = copyBounded(in, out, source, digest);
            }
            if (declaredSize >= 0 && copied.bytes() != declaredSize) {
                throw new IOException("Extension archive " + source + " declared " + declaredSize
                        + " bytes but produced " + copied.bytes());
            }
            // The connection's reported metadata is not usable as a key here: union: URLs,
            // for example, report a constant 1980 timestamp even after the entry changes.
            Path cached = directory.resolve(HexFormat.of().formatHex(digest.digest()) + ".jar");
            if (isReadableJar(cached)) {
                logger.debug("Reusing content-addressed extension archive {}", cached);
                deleteTemporary(temporary);
                touch(cached);
                return cached;
            }
            deleteQuietly(cached);
            verifyArchive(temporary, source);
            moveIntoPlace(temporary, cached);
            forgetTemporary(temporary);
            markCreated(cached);
            return cached;
        } catch (IOException | RuntimeException e) {
            deleteTemporary(temporary);
            throw e;
        }
    }

    private Path extract(JarFile jar, JarEntry entry, Path target, URL source, long declaredSize) throws IOException {
        checkDeclaredSize(declaredSize, source);
        Path temporary = createTemporaryFile();
        try {
            CopyResult copied;
            try (InputStream in = jar.getInputStream(entry);
                 OutputStream out = Files.newOutputStream(temporary)) {
                // Reading to EOF proves the entry was not truncated. The checksum below is
                // checked against the one the archive declares, so a corrupted entry is
                // rejected before it can become a cache file that later loads would trust.
                copied = copyBounded(in, out, source, null);
            }
            if (declaredSize >= 0 && copied.bytes() != declaredSize) {
                throw new IOException("Extension archive " + source + " declared " + declaredSize
                        + " bytes but produced " + copied.bytes());
            }
            if (entry.getCrc() >= 0 && copied.crc() != entry.getCrc()) {
                throw new IOException("Checksum mismatch for extension archive " + source);
            }
            moveIntoPlace(temporary, target);
            forgetTemporary(temporary);
        } catch (IOException | RuntimeException e) {
            deleteTemporary(temporary);
            deleteQuietly(target);
            throw e;
        }
        try {
            verifyArchive(target, source);
        } catch (IOException | RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
        markCreated(target);
        return target;
    }

    private boolean isUsableCacheFile(Path cached, long declaredSize) {
        try {
            if (!Files.isRegularFile(cached)) {
                return false;
            }
            if (declaredSize >= 0 && Files.size(cached) != declaredSize) {
                return false;
            }
            try (JarFile ignored = new JarFile(cached.toFile())) {
                return true;
            }
        } catch (IOException e) {
            logger.debug("Ignoring an unreadable cached extension archive {}", cached, e);
            return false;
        }
    }

    private boolean isReadableJar(Path cached) {
        try {
            if (!Files.isRegularFile(cached)) {
                return false;
            }
            try (JarFile ignored = new JarFile(cached.toFile())) {
                return true;
            }
        } catch (IOException e) {
            logger.debug("Ignoring an unreadable cached extension archive {}", cached, e);
            return false;
        }
    }

    private Path cachedPath(String descriptor) {
        return directory.resolve(sha256Hex(descriptor) + ".jar");
    }

    private static void checkDeclaredSize(long declaredSize, URL source) throws IOException {
        if (declaredSize > MAX_ARCHIVE_BYTES) {
            throw new IOException("Extension archive " + source + " declares " + declaredSize
                    + " bytes, above the " + MAX_ARCHIVE_BYTES + "-byte limit");
        }
    }

    private CopyResult copyBounded(InputStream in, OutputStream out, URL source, MessageDigest digest)
            throws IOException {
        byte[] buffer = new byte[64 * 1024];
        CRC32 checksum = new CRC32();
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > MAX_ARCHIVE_BYTES) {
                throw new IOException("Extension archive " + source + " is larger than the "
                        + MAX_ARCHIVE_BYTES + "-byte cache limit");
            }
            checksum.update(buffer, 0, read);
            if (digest != null) {
                digest.update(buffer, 0, read);
            }
            out.write(buffer, 0, read);
        }
        return new CopyResult(total, checksum.getValue());
    }

    /** Number of bytes copied and the CRC-32 of those bytes while streaming an archive. */
    private record CopyResult(long bytes, long crc) {
    }

    private static void verifyArchive(Path file, URL source) throws IOException {
        try (JarFile ignored = new JarFile(file.toFile())) {
            // Opening the jar parses its central directory, so a truncated or non-ZIP file
            // fails here instead of much later as a missing class.
        } catch (IOException e) {
            throw new IOException("Extension archive from " + source + " is not a readable jar", e);
        }
    }

    private static Path fileOf(URL url) throws IOException {
        try {
            return Path.of(url.toURI());
        } catch (Exception e) {
            throw new IOException("Could not resolve extension archive " + url, e);
        }
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256Hex(String value) {
        return HexFormat.of().formatHex(sha256Digest().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static Path normalize(Path file) {
        return file.toAbsolutePath().normalize();
    }

    /**
     * Creates a temp file and remembers it, so cleanup can remove only files this instance
     * created even when another load shares the cache directory.
     */
    private Path createTemporaryFile() throws IOException {
        Path temporary = Files.createTempFile(directory, "extract-", ".tmp");
        temporaryFiles.add(normalize(temporary));
        registerForExit(temporary);
        return temporary;
    }

    private void deleteTemporary(Path temporary) {
        deleteQuietly(temporary);
        temporaryFiles.remove(normalize(temporary));
    }

    private void forgetTemporary(Path temporary) {
        temporaryFiles.remove(normalize(temporary));
    }

    private void markCreated(Path archive) {
        registerForExit(archive);
    }

    /** Refreshes the idle timer of a cache file that this load is reusing. */
    private void touch(Path archive) {
        try {
            Files.setLastModifiedTime(archive,
                    java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
        } catch (IOException | SecurityException e) {
            logger.debug("Could not refresh extension cache file {}", archive, e);
        }
    }

    /**
     * A fallback cache lives in a temporary directory, so its kept files need to be removed
     * individually at exit; deleting the non-empty directory alone would fail.
     */
    private void registerForExit(Path file) {
        if (temporaryDirectory) {
            file.toFile().deleteOnExit();
        }
    }

    private void deleteUnused(Path file) {
        try {
            Files.deleteIfExists(file);
            logger.debug("Removed stale extension cache file {}", file);
        } catch (IOException | SecurityException e) {
            logger.debug("Could not remove stale extension cache file {}", file, e);
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException | SecurityException e) {
            logger.debug("Could not delete extension cache file {}", file, e);
        }
    }

}
