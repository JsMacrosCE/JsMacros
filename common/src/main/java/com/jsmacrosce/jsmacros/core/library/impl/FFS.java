package com.jsmacrosce.jsmacros.core.library.impl;

import com.google.common.io.Files;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.PerExecLibrary;
import com.jsmacrosce.jsmacros.core.library.impl.classes.FileHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

/**
 * File operations resolved against the running script's folder; the {@code FS} library is
 * available to scripts as a global variable. Paths are not confined to that folder: absolute paths
 * and {@code ..} components can refer elsewhere.
 * <p>
 * Use {@link #open(String)} to read or write file contents; this library provides the path
 * operations.
 * <pre>
 * // Paths are relative to the script's folder.
 * if (!FS.exists("notes")) {
 *   FS.createFile("notes", "readme.txt", true);
 * }
 *
 * // A FileHandler reads and writes the file.
 * const handle = FS.open("notes/readme.txt");
 * handle.write("first line\n");
 * handle.append("second line\n");
 * print(handle.read());
 *
 * const entries = FS.list("notes");
 * if (entries !== null) {
 *   print(`notes holds ${JavaUtils.arrayToString(entries)}`);
 * }
 * </pre>
 * @author Wagyourtail
 * @since 1.1.8
 */
@Library("FS")
@SuppressWarnings("unused")
public class FFS extends PerExecLibrary {

    /**
     * Initializes this per-execution library with its script context and the context's core.
     *
     * @param context script context associated with this library
     */
    public FFS(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * Lists the entries in a directory.
     *
     * @param path relative to the script's folder.
     * @return the entry names, or {@code null} if listing fails
     * @since 1.1.8
     */
    @Nullable
    public String[] list(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().list();
    }

    /**
     * Checks whether a path exists.
     *
     * @param path relative to the script's folder.
     * @return whether the path exists
     * @since 1.1.8
     */
    public boolean exists(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().exists();
    }

    /**
     * Checks whether a path is a directory.
     *
     * @param path relative to the script's folder.
     * @return whether the path is a directory
     * @since 1.1.8
     */
    public boolean isDir(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().isDirectory();
    }

    /**
     * Checks whether the path resolves to a regular file.
     *
     * @param path relative to the script's folder
     * @return whether the path is a regular file
     * @since 1.8.4
     */
    public boolean isFile(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().isFile();
    }

    /**
     * Returns the final name component of the resolved path.
     *
     * @param path relative to the script's folder.
     * @return the final name component
     * @since 1.1.8
     */
    public String getName(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().getName();
    }

    /**
     * Relativizes an absolute path against the script's folder.
     *
     * @param absolutePath absolute path to relativize
     * @return the path relative to the script's folder
     * @since 1.8.4
     */
    public String toRelativePath(String absolutePath) {
        return ctx.getContainedFolder().toPath().relativize(Paths.get(absolutePath)).toString();
    }

    /**
     * Creates a new file in the specified path, relative to the script's folder. This will only
     * work if the parent directory already exists. See {@link #createFile(String, String, boolean)}
     * to automatically create all parent directories.
     *
     * @param path the path relative to the script's folder
     * @param name the name of the file
     * @return {@code true} if the file was created successfully, {@code false} otherwise.
     * @throws IOException if there occurs an error while creating the file.
     * @since 1.8.4
     */
    public boolean createFile(String path, String name) throws IOException {
        return createFile(path, name, false);
    }

    /**
     * Creates an empty file by resolving {@code name} under {@code path}. If {@code createDirs} is
     * true, it attempts to create missing parent directories before creating the file. Existing
     * files and directories are not replaced; creation returns {@code false} for either.
     *
     * @param path       the path relative to the script's folder
     * @param name       the name of the file
     * @param createDirs whether to attempt to create missing parent directories
     * @return whether a new file was created
     * @throws IOException if there occurs an error while creating the file.
     * @since 1.8.4
     */
    public boolean createFile(String path, String name, boolean createDirs) throws IOException {
        File file = ctx.getContainedFolder().toPath().resolve(path).resolve(name).toFile();
        if (createDirs) {
            file.getParentFile().mkdirs();
        }
        return file.createNewFile();
    }

    /**
     * Creates one directory; its parent must already exist. Returns false if it already exists or
     * creation fails.
     *
     * @param path relative to the script's folder.
     * @return whether the directory was created
     * @since 1.1.8
     */
    public boolean makeDir(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().mkdir();
    }

    /**
     * Moves the path {@code from} to {@code to}; both paths are resolved against the script's
     * folder. An existing destination is not replaced, and the move may fail if it exists.
     *
     * @param from source path, resolved against the script's folder
     * @param to   destination path, resolved against the script's folder
     * @throws IOException if the move fails
     * @since 1.1.8
     */
    public void move(String from, String to) throws IOException {
        Files.move(ctx.getContainedFolder().toPath().resolve(from).toFile(), ctx.getContainedFolder().toPath().resolve(to).toFile());
    }

    /**
     * Copies the path {@code from} to {@code to}; both paths are resolved against the script's
     * folder. An existing destination is not overwritten.
     *
     * @param from source path, resolved against the script's folder
     * @param to   destination path, resolved against the script's folder
     * @throws IOException if the copy fails
     * @since 1.1.8
     */
    public void copy(String from, String to) throws IOException {
        Files.copy(ctx.getContainedFolder().toPath().resolve(from).toFile(), ctx.getContainedFolder().toPath().resolve(to).toFile());
    }

    /**
     * Attempts to delete the path resolved against the script's folder.
     *
     * @param path relative to the script's folder.
     * @return whether deletion succeeded
     * @since 1.2.9
     */
    public boolean unlink(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().delete();
    }

    /**
     * Combines path components using the platform's path syntax; this does not resolve them
     * against the script's folder.
     *
     * @param patha first path component
     * @param pathb second path component
     * @return the combined path
     * @since 1.1.8
     */
    public String combine(String patha, String pathb) {
        return Paths.get(patha, pathb).toString();
    }

    /**
     * Returns the parent of the path resolved against the script's folder, expressed relative to
     * that folder.
     *
     * @param path relative to the script's folder.
     * @return the parent path relative to the script's folder
     * @since 1.1.8
     */
    public String getDir(String path) {
        File dir = ctx.getContainedFolder().toPath().resolve(path).toFile().getParentFile();
        return ctx.getContainedFolder().toPath().relativize(dir.toPath()).toString();
    }

    /**
     * Returns a {@link FileHandler} for the path resolved against the script's folder, using the
     * handler's default charset.
     *
     * @param path relative to the script's folder.
     * @return a handler for the resolved file
     * @since 1.1.8
     */
    public FileHandler open(String path) {
        return new FileHandler(ctx.getContainedFolder().toPath().resolve(path).toFile());
    }

    /**
     * Returns a {@link FileHandler} for the path resolved against the script's folder, using the
     * specified charset.
     *
     * @param path    relative to the script's folder.
     * @param charset charset name used for reading and writing
     * @return a handler for the resolved file
     * @since 1.8.4
     */
    public FileHandler open(String path, String charset) {
        return new FileHandler(ctx.getContainedFolder().toPath().resolve(path).toFile(), charset);
    }

    /**
     * Walks a directory tree and calls {@code visitor} for each encountered path with its basic
     * attributes. Each callback path is computed as {@code visitedPath.relativize(scriptFolder)},
     * yielding a path from the visited entry toward the script folder. {@code maxDepth}
     * uses {@link java.nio.file.Files#walk(Path, int, FileVisitOption...) Files.walk} semantics:
     * the root is at depth zero. Attribute-read failures are printed and skipped; exceptions from
     * the visitor propagate.
     *
     * @param path        path to the directory, resolved against the script's folder
     * @param maxDepth    maximum traversal depth
     * @param followLinks whether to follow symbolic links
     * @param visitor     callback for each visited entry, with its path and
     *                    attributes
     * @since 1.8.4
     */
    public void walkFiles(String path, int maxDepth, boolean followLinks, MethodWrapper<String, BasicFileAttributes, ?, ?> visitor) throws IOException {
        FileVisitOption[] options = followLinks ? new FileVisitOption[]{FileVisitOption.FOLLOW_LINKS} : new FileVisitOption[0];
        try (Stream<Path> paths = java.nio.file.Files.walk(ctx.getContainedFolder().toPath().resolve(path), maxDepth, options)) {
            paths.forEach(p -> {
                try {
                    visitor.apply(p.relativize(ctx.getContainedFolder().toPath()).toFile().getPath(), java.nio.file.Files.readAttributes(p, BasicFileAttributes.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Returns a {@link File} for the path resolved against the script's folder.
     *
     * @param path path resolved against the script's folder
     * @return the resolved file
     * @since 1.8.4
     */
    public File toRawFile(String path) {
        return toRawPath(path).toFile();
    }

    /**
     * Returns the path resolved against the script's folder.
     *
     * @param path path resolved against the script's folder
     * @return the resolved path
     * @since 1.8.4
     */
    public Path toRawPath(String path) {
        return ctx.getContainedFolder().toPath().resolve(path);
    }

    /**
     * Reads the basic attributes of the path resolved against the script's folder.
     *
     * @param path path to read, resolved against the script's folder
     * @return the path's basic file attributes
     * @throws IOException if the attributes cannot be read
     * @since 1.8.4
     */
    public BasicFileAttributes getRawAttributes(String path) throws IOException {
        return java.nio.file.Files.readAttributes(ctx.getContainedFolder().toPath().resolve(path), BasicFileAttributes.class);
    }

}
