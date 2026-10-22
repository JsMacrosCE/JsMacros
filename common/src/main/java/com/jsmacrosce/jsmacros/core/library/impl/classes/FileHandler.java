package com.jsmacrosce.jsmacros.core.library.impl.classes;

import java.io.*;
import java.lang.ref.Cleaner;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;

/**
 * Represents a file and charset for script file operations. Obtain handlers with {@code FS.open};
 * each operation opens and closes its own file resource, except {@link #readLines()} and
 * {@link #streamBytes()}, whose returned resources remain open until closed. The read and write
 * methods distinguish whole-file, streaming, text, and byte operations; writes create the file
 * if needed but do not create parent directories.
 * <pre>
 * // FS.open resolves the path against this script's own folder
 * const notes = FS.open("notes/log.txt");
 *
 * // write replaces the file, append adds to the end
 * notes.write("first run\n");
 * notes.append("second run\n");
 *
 * // read and readBytes are the whole file at once, and readLines streams it,
 * // so the iterator is the one that has to be closed
 * print(notes.read());
 * const lines = notes.readLines();
 * try {
 *   while (lines.hasNext()) {
 *     print(lines.next());
 *   }
 * } finally {
 *   lines.close();
 * }
 *
 * </pre>
 * @author Wagyourtail
 * @since 1.1.8
 */
@SuppressWarnings("unused")
public class FileHandler {
    private final File f;
    private final Charset charset;

    /**
     * Sets up a handler for a path using UTF-8 for text operations. The path is used as a Java file path,
     * not rebased against the script folder; no file is opened or created here.
     *
     * @param path file path, interpreted relative to the Java process working directory when relative
     */
    public FileHandler(String path) {
        this(new File(path), StandardCharsets.UTF_8);
    }

    /**
     * Sets up a handler for a path using the named charset for text operations. The path is not rebased
     * against the script folder, and no file is opened or created here; an invalid or unsupported charset
     * name fails during construction.
     *
     * @param path file path, interpreted relative to the Java process working directory when relative
     * @param charset charset name resolved by {@link Charset#forName(String)}
     */
    public FileHandler(String path, String charset) {
        this(new File(path), charset);
    }

    /**
     * Sets up a handler for the supplied file using the named charset for text operations. No file is
     * opened or created here; an invalid or unsupported charset name fails during construction.
     *
     * @param path file associated with the handler
     * @param charset charset name resolved by {@link Charset#forName(String)}
     */
    public FileHandler(File path, String charset) {
        this(path, Objects.requireNonNull(Charset.forName(charset), () -> "Charset " + charset + " not found."));
    }

    /**
     * Sets up a handler for a path using the supplied charset for text operations. The path is not rebased
     * against the script folder, and no file is opened or created here.
     *
     * @param path file path, interpreted relative to the Java process working directory when relative
     * @param charset charset to use for text operations
     */
    public FileHandler(String path, Charset charset) {
        this(new File(path), charset);
    }

    /**
     * Sets up a handler for the supplied file using UTF-8 for text operations. No file is opened or
     * created here.
     *
     * @param path file associated with the handler
     */
    public FileHandler(File path) {
        this(path, StandardCharsets.UTF_8);
    }

    /**
     * Sets up a handler for the supplied file and charset for later text operations; no file is opened or
     * created here.
     *
     * @param path file associated with the handler
     * @param charset charset to use for text operations
     */
    public FileHandler(File path, Charset charset) {
        this.f = path;
        this.charset = charset;
    }

    /**
     * Replaces the file contents with text encoded using this handler's charset. Creates the file
     * if needed, but does not create parent directories.
     *
     * @param s the text to write, encoded in the handler's charset
     * @return self for chaining
     * @throws IOException if the file could not be written, including because a directory on the
     *                     way to it does not exist
     * @since 1.1.8
     */
    public FileHandler write(String s) throws IOException {
        try (FileWriter out = new FileWriter(f, charset, false)) {
            out.write(s);
        }
        return this;
    }

    /**
     * Replaces the file contents with the supplied bytes, unchanged and without using the handler's
     * charset. Creates the file if needed, but does not create parent directories.
     *
     * @param b the bytes to write
     * @return self for chaining
     * @throws IOException if the file could not be written, including because a directory on the
     *                     way to it does not exist
     * @since 1.1.8
     */
    public FileHandler write(byte[] b) throws IOException {
        try (FileOutputStream out = new FileOutputStream(f, false)) {
            out.write(b);
        }
        return this;
    }

    /**
     * Reads the whole file into memory and decodes it using this handler's charset.
     *
     * @return the whole file, or an empty string for an empty one
     * @throws IOException if the file could not be read, including because it is not there
     * @since 1.1.8
     */
    public String read() throws IOException {
        return new String(readBytes(), charset);
    }

    /**
     * Reads the whole file into a byte array without decoding. Files larger than
     * {@code Integer.MAX_VALUE} bytes are rejected with {@link IOException}.
     *
     * @return the whole file, or an empty array for an empty one
     * @throws IOException if the file could not be read, if it is not there, or if it is larger
     *                     than the largest array that can be made
     * @since 1.2.6
     */
    public byte[] readBytes() throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            if (f.length() > Integer.MAX_VALUE) {
                throw new IOException("File is too large to read into memory. (max size: " + Integer.MAX_VALUE + ")");
            }
            return in.readAllBytes();
        }
    }

    /**
     * Returns a line iterator decoded with this handler's charset. The reader remains open until
     * the iterator is closed; close it when finished. Opening failures and later read failures are
     * wrapped in {@link RuntimeException}.
     *
     * @return an iterator over the lines, which is also closeable
     * @throws RuntimeException if the file could not be opened for reading
     * @since 1.8.4
     */
    public FileLineIterator readLines() {
        try {
            return new FileLineIterator(f, charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a buffered stream of the file's raw bytes. The stream keeps the file open until
     * closed; close it when finished. Opening failures are wrapped in {@link RuntimeException}.
     *
     * @return a stream over the file's bytes, which the caller has to close
     * @throws RuntimeException if the file could not be opened for reading
     * @since 1.8.4
     */
    public BufferedInputStream streamBytes() {
        try {
            return new BufferedInputStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Appends text encoded with this handler's charset, creating the file if it does not exist.
     *
     * @param s the text to add, encoded in the handler's charset
     * @return self for chaining
     * @throws IOException if the file could not be written, including because a directory on the
     *                     way to it does not exist
     * @since 1.1.8
     */
    public FileHandler append(String s) throws IOException {
        try (FileWriter out = new FileWriter(f, charset, true)) {
            out.write(s);
        }
        return this;
    }

    /**
     * Appends bytes unchanged; the handler's charset is not used. Creates the file if it does not exist.
     *
     * @param b the bytes to add
     * @return self for chaining
     * @throws IOException if the file could not be written, including because a directory on the
     *                     way to it does not exist
     * @since 1.2.6
     */
    public FileHandler append(byte[] b) throws IOException {
        try (FileOutputStream out = new FileOutputStream(f, true)) {
            out.write(b);
        }
        return this;
    }

    /**
     * Returns the {@link File} object associated with this handler.
     * @return the file object supplied to or created for this handler
     */
    public File getFile() {
        return f;
    }

    /**
     * Returns a diagnostic representation containing the file's absolute path; it is not JSON serialization.
     *
     * @return diagnostic representation containing the file's absolute path
     */
    public String toString() {
        return String.format("FileHandler:{\"file\": \"%s\"}", f.getAbsolutePath());
    }

    /**
     * Reads lines from a file using a reader held open until this iterator is closed. Obtain it
     * through {@link FileHandler#readLines()}; iterate with {@link #hasNext()} and {@link #next()},
     * and close it when finished. Line endings are omitted, and a final unterminated line is returned.
     * <pre>
     * const lines = FS.open("notes/log.txt").readLines();
     * try {
     *   while (lines.hasNext()) {
     *     const line = lines.next();
     *     if (line.length === 0) continue;
     *     print(line);
     *   }
     * } finally {
     *   lines.close();
     * }
     * </pre>
     */
    public static class FileLineIterator implements Iterator<String>, AutoCloseable {
        private static final Cleaner CLEANER = Cleaner.create();

        private final BufferedReader reader;
        private String nextLine;

        /**
         * Opens the file and reads its first line immediately; use {@link FileHandler#readLines()}
         * to obtain an iterator for a handler's file.
         *
         * @param file    the file to read, which is opened in the charset given
         * @param charset the charset to decode the lines with
         * @throws IOException if the file could not be opened for reading
         */
        public FileLineIterator(File file, Charset charset) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
            this.nextLine = reader.readLine();
            CLEANER.register(this, () -> {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            });
            this.reader = reader;
        }

        /**
         * Reports whether a line is currently available. The next line is read ahead; an empty
         * file or exhausted iterator returns {@code false}.
         *
         * @return whether {@link #next() next} will return a line
         */
        @Override
        public boolean hasNext() {
            return nextLine != null;
        }

        /**
         * Returns the next line without its line ending, then reads ahead. Returns {@code null}
         * when exhausted; an empty line is an empty string. Read failures are wrapped in
         * {@link RuntimeException}.
         *
         * @return the next line, or {@code null} if there is not one
         * @throws RuntimeException if the line could not be read
         */
        @Override
        public String next() {
            String line = nextLine;
            try {
                nextLine = reader.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return line;
        }

        /**
         * Closes the reader. Close explicitly when finished; a cleaner is a fallback, but its
         * cleanup timing is not predictable. Repeated close calls are harmless.
         *
         * @throws IOException if the file could not be closed
         */
        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

}
