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
 * Better File-System functions.
 * <p>
 * An instance of this class is passed to scripts as the {@code FS} variable.
 * <br>
 * Every path these take is relative to the folder of the script that is running, not to the game's
 * working directory and not to the profile's macro folder, so a script and the files it ships
 * together can be moved around as a unit. A path reaching outside that folder is not prevented,
 * since nothing here checks for it, so a script built from an untrusted source could read or write
 * anywhere the game can.
 * <br>
 * These are plain file operations with no game state behind them, so none of them are the way to
 * read or write world data. What they are for is the files a script ships with itself.
 * <br>
 * The whole-file reads and writes go through a {@link FileHandler} rather than through this
 * library, so the writing half of a round trip is {@link #open(String) open} rather than something
 * on this class.
 * example:
 * <pre>
 * // everything is relative to this script's own folder
 * if (!FS.exists("notes")) {
 *   FS.createFile("notes", "readme.txt", true);
 * }
 *
 * // a FileHandler does the reading and writing, and it is the same object
 * // either way round, so the file is only opened once
 * const handle = FS.open("notes/readme.txt");
 * handle.write("first line\n");
 * handle.append("second line\n");
 * print(handle.read());
 *
 * // and the directory listing functions are here rather than on the handler
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

    public FFS(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * List files in path.
     *
     * @param path relative to the script's folder.
     * @return An array of file names as {@link java.lang.String Strings}.
     * @since 1.1.8
     */
    @Nullable
    public String[] list(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().list();
    }

    /**
     * Check if a file exists.
     *
     * @param path relative to the script's folder.
     * @return
     * @since 1.1.8
     */
    public boolean exists(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().exists();
    }

    /**
     * Check if a file is a directory.
     *
     * @param path relative to the script's folder.
     * @return
     * @since 1.1.8
     */
    public boolean isDir(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().isDirectory();
    }

    /**
     * @param path the path relative to the script's folder
     * @return {@code true} if the path leads to a file, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFile(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().isFile();
    }

    /**
     * Get the last part (name) of a file.
     *
     * @param path relative to the script's folder.
     * @return a {@link java.lang.String String} of the file name.
     * @since 1.1.8
     */
    public String getName(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().getName();
    }

    /**
     * @param absolutePath the absolute path to the file
     * @return a path relative to the script's folder to the given absolute path.
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
     * Creates a new file in the specified path, relative to the script's folder. Optionally parent
     * directories can be created if they do not exist.
     * <br>
     * The two are joined, so {@code path} is the folder and {@code name} is the file. The parent
     * directories are made with a call that makes every missing level, so {@code true} creates the
     * whole chain rather than only the last folder. An empty {@code path} is not a special case
     * that fails: the file then resolves to the script's own folder, which is already there, so
     * the directory call does nothing and the file is created in it directly.<br>
     * A file that is already there is not an error, but the answer is {@code false} rather than
     * {@code true}, since nothing was created. An existing directory at that name is likewise
     * {@code false}. The file is created empty, so anything to go in it is written afterwards
     * through {@link #open(String) open}.
     * example:
     * <pre>
     * // the whole chain is made, not just the last folder
     * if (FS.createFile("logs/today", "session.txt", true)) {
     *   print("created a fresh, empty file");
     * }
     * // running the same line again reports false rather than failing
     * print(`the second time: ${FS.createFile("logs/today", "session.txt", true)}`);
     *
     * // an empty path puts the file straight into the script's own folder, and
     * // there is nothing to make because that folder is already there
     * print(`created at the script folder root: ${FS.createFile("", "root.txt", true)}`);
     *
     * // the file is empty until something is written to it
     * FS.open("logs/today/session.txt").write("hello\n");
     * </pre>
     *
     * @param path       the path relative to the script's folder
     * @param name       the name of the file
     * @param createDirs whether to create parent directories if they do not exist or not
     * @return {@code true} if the file was created successfully, {@code false} otherwise.
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
     * Make a directory.
     * <br>
     * One level only, and only if the parent is already there, so a nested path fails with a
     * {@code false} rather than making the levels above it. Use
     * {@link #createFile(String, String, boolean) createFile(path, name, true)}, which creates the
     * parents, or {@link #createFile(String, String, boolean) createFile} with a name in a folder
     * that exists. An existing directory is not an error here, since the underlying call reports
     * {@code false} rather than throwing, and neither is an existing file, which gives the same
     * {@code false}.
     * example:
     * <pre>
     * // one level, and only when the folder above it is already there
     * if (FS.makeDir("logs")) {
     *   print("made logs");
     * }
     * // a nested path does not come for free
     * if (!FS.makeDir("logs/today")) {
     *   print("that needed its parent to exist first");
     * }
     * // creating a file with createDirs is the way to get the whole chain
     * FS.createFile("logs/today", "session.txt", true);
     * </pre>
     *
     * @param path relative to the script's folder.
     * @return a {@link java.lang.Boolean boolean} for success.
     * @since 1.1.8
     */
    public boolean makeDir(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().mkdir();
    }

    /**
     * Move a file.
     * <br>
     * {@code to} is the new path of the file itself rather than the folder to put it in, so moving
     * {@code a.txt} into an existing folder {@code logs} means passing {@code logs/a.txt}. Both
     * paths are resolved against the script's folder, and the destination is replaced rather than
     * refused: a rename is tried first, and a copy and a delete of the source if the rename does
     * not work. The one thing refused outright is moving a file onto itself, with an
     * {@link java.lang.IllegalArgumentException IllegalArgumentException}, and the check behind it
     * compares the two paths as path strings rather than as the files they name, so a second
     * spelling of the same file such as {@code a.txt} and {@code ./a.txt} is not caught by it. A
     * directory is moved the same way as a file, provided the destination is not inside it.
     * example:
     * <pre>
     * // the destination is the new path of the file, not the folder to put it in
     * FS.makeDir("archive");
     * FS.copy("notes/readme.txt", "archive/readme.txt");
     * FS.move("notes/readme.txt", "archive/old-readme.txt");
     * print(`left behind at the old path: ${FS.exists("notes/readme.txt")}`);
     * print(`and present at the new one: ${FS.exists("archive/old-readme.txt")}`);
     * </pre>
     *
     * @param from relative to the script's folder.
     * @param to   relative to the script's folder, the new path of the file itself
     * @throws IOException
     * @throws java.lang.IllegalArgumentException if both paths are the same file as the path
     *         strings spell it
     * @since 1.1.8
     */
    public void move(String from, String to) throws IOException {
        Files.move(ctx.getContainedFolder().toPath().resolve(from).toFile(), ctx.getContainedFolder().toPath().resolve(to).toFile());
    }

    /**
     * Copy a file.
     * <br>
     * Both paths are resolved against the script's folder and the destination is truncated and
     * written over, so an existing file at {@code to} is replaced rather than refused. The copy is
     * not atomic: if it is interrupted the destination can be left half written, so overwriting
     * something that matters is better done by copying aside and moving it into place. Copying a
     * file onto itself is refused with an {@link java.lang.IllegalArgumentException
     * IllegalArgumentException}, and the check behind it compares the two paths as path strings
     * rather than as the files they name: {@code copy("a.txt", "./a.txt")} is not caught by it and
     * truncates the source to nothing. Unlike a move, the source is left where it was either way.
     * A directory is not copied, only a file, and asking for one is an error rather than a skip:
     * the source is opened as a stream, so a directory gives a
     * {@link java.io.FileNotFoundException FileNotFoundException}.
     * example:
     * <pre>
     * // the destination is written over rather than refused
     * FS.copy("notes/readme.txt", "archive/readme.txt");
     * FS.copy("notes/readme.txt", "archive/readme.txt");
     * print(`copied twice, the source is still there: ${FS.exists("notes/readme.txt")}`);
     *
     * // for something that matters, copy aside and move into place, so the
     * // destination is never half written
     * FS.copy("notes/readme.txt", "archive/readme.new.txt");
     * FS.move("archive/readme.new.txt", "archive/readme.txt");
     * </pre>
     *
     * @param from relative to the script's folder.
     * @param to   relative to the script's folder, replaced if it is already there
     * @throws IOException
     * @throws java.lang.IllegalArgumentException if both paths are the same file as the path
     *         strings spell it
     * @since 1.1.8
     */
    public void copy(String from, String to) throws IOException {
        Files.copy(ctx.getContainedFolder().toPath().resolve(from).toFile(), ctx.getContainedFolder().toPath().resolve(to).toFile());
    }

    /**
     * Delete a file.
     *
     * @param path relative to the script's folder.
     * @return a {@link java.lang.Boolean boolean} for success.
     * @since 1.2.9
     */
    public boolean unlink(String path) {
        return ctx.getContainedFolder().toPath().resolve(path).toFile().delete();
    }

    /**
     * Combine 2 paths.
     *
     * @param patha path is relative to the script's folder.
     * @param pathb
     * @return a {@link java.lang.String String} of the combined path.
     * @throws java.nio.file.InvalidPathException
     * @since 1.1.8
     */
    public String combine(String patha, String pathb) {
        return Paths.get(patha, pathb).toString();
    }

    /**
     * Gets the directory part of a file path, or the parent directory of a folder.
     * <br>
     * The result is relative to the script's folder rather than absolute, so a path that points
     * outside it comes back with {@code ..} steps in it, and the parent of a path directly in the
     * script's folder comes back as an empty string. It is the parent of the path as given, so a
     * trailing separator does not add a level.<br>
     * The one failure this can report comes from asking for the parent of a path that has none,
     * such as a file system root, and it is a {@link NullPointerException} rather than anything
     * to do with the relativizing. In practice a script cannot reach it: the path is always
     * resolved against the script's folder, so it is absolute and underneath it, which always has
     * a parent of its own. The {@code @throws} below records what would happen if that were not so.
     * example:
     * <pre>
     * // the parent of a file, relative to this script's folder
     * print(`the parent is ${FS.getDir("notes/readme.txt")}`);
     * // a path climbing out of the folder comes back with the climbs still in it
     * print(`and that one is ${FS.getDir("../elsewhere/readme.txt")}`);
     * </pre>
     *
     * @param path relative to the script's folder.
     * @return a {@link java.lang.String String} of the combined path, relative to the script's
     *         folder.
     * @throws NullPointerException if the resolved path has no parent
     * @since 1.1.8
     */
    public String getDir(String path) {
        File dir = ctx.getContainedFolder().toPath().resolve(path).toFile().getParentFile();
        return ctx.getContainedFolder().toPath().relativize(dir.toPath()).toString();
    }

    /**
     * Open a FileHandler for the file at the specified path.
     *
     * @param path relative to the script's folder.
     * @return a {@link FileHandler FileHandler} for the file path.
     * @see FileHandler
     * @since 1.1.8
     */
    public FileHandler open(String path) {
        return new FileHandler(ctx.getContainedFolder().toPath().resolve(path).toFile());
    }

    /**
     * Open a FileHandler for the file at the specified path.
     *
     * @param path    relative to the script's folder.
     * @param charset the charset to use for reading/writing the file (default is UTF-8)
     * @return a {@link FileHandler FileHandler} for the file path.
     * @see FileHandler
     * @since 1.8.4
     */
    public FileHandler open(String path, String charset) {
        return new FileHandler(ctx.getContainedFolder().toPath().resolve(path).toFile(), charset);
    }

    /**
     * An advanced method to walk a directory tree and get some information about the files, as well
     * as their paths.
     * <br>
     * The paths handed to the visitor are relative to the script's folder, not to the directory
     * being walked, so a walk of a subfolder still gives paths that start at the script's folder
     * and can be handed straight to the rest of this library. The depth counts from the directory
     * being walked, and a depth of one is that directory itself.<br>
     * The walk is a real directory walk, so it is not something to point at a huge tree on every
     * tick. The visitor is called for every entry including directories, not only files, so check
     * {@link BasicFileAttributes#isDirectory()} rather than assuming a file. An
     * {@link java.io.IOException IOException} raised while reading one entry's attributes is
     * printed and the walk carries on rather than ending, and a visitor that throws ends the walk
     * by propagating out.
     * example:
     * <pre>
     * // the paths come back relative to the script's folder, so they can be used
     * // directly, and the walk includes directories as well as files
     * FS.walkFiles(".", 3, false, JavaWrapper.methodToJava(function (path, attrs) {
     *   if (attrs.isDirectory()) {
     *     print(`${path} is a directory`);
     *   } else {
     *     print(`${path} is ${attrs.size()} bytes`);
     *   }
     * }));
     *
     * // a depth of one is the directory itself and nothing below it
     * FS.walkFiles("notes", 1, false, JavaWrapper.methodToJava(function (path) {
     *   print(path);
     * }));
     * </pre>
     *
     * @param path        the relative path of the directory to walk through
     * @param maxDepth    the maximum depth to follow, can cause stack overflow if too high
     * @param followLinks whether to follow symbolic links
     * @param visitor     the visitor that is called for each file with the path of the file and its
     *                    attributes
     * @throws IOException
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
     * @param path the relative path to get the file object for
     * @return the file object for the specified path.
     * @since 1.8.4
     */
    public File toRawFile(String path) {
        return toRawPath(path).toFile();
    }

    /**
     * @param path the relative path to get the path object for
     * @return the path object for the specified path.
     * @since 1.8.4
     */
    public Path toRawPath(String path) {
        return ctx.getContainedFolder().toPath().resolve(path);
    }

    /**
     * @param path the path relative to the script's folder
     * @return the attributes of the file at the specified path.
     * @throws IOException
     * @since 1.8.4
     */
    public BasicFileAttributes getRawAttributes(String path) throws IOException {
        return java.nio.file.Files.readAttributes(ctx.getContainedFolder().toPath().resolve(path), BasicFileAttributes.class);
    }

}
