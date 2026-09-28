package com.jsmacrosce.jsmacros.core.library.impl.classes;

import java.io.*;
import java.lang.ref.Cleaner;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;

/**
 * an open file, and the only way a script reads or writes whole files.<br>
 * A handler is made with {@code FS.open}, which resolves the path relative to the folder of the
 * running script and hands one of these back, and it is a plain object rather than a set of
 * global functions, so several files can be open at once and a single one can be used to read
 * then write. Nothing here is buffered between calls: every method opens the file, does its one
 * thing and closes it again, so a handler is a name and a charset rather than a live handle. That
 * also means it can be held onto and used later, and that two handlers for the same path do not
 * see each other's writes until the other call has finished.<br>
 * The three ways of reading are the same file seen three ways. {@link #read() read} and
 * {@link #readBytes() readBytes} load all of it at once, {@link #readLines() readLines} streams it
 * a line at a time and holds the file open until it is closed, and {@link #streamBytes() streamBytes}
 * hands over the raw stream. The writing half is {@link #write(java.lang.String) write} and
 * {@link #append(java.lang.String) append}, the first replacing the whole file and the second
 * adding to the end.<br>
 * Nothing here creates the file or the directories leading to it, and nothing checks where the
 * path goes, so a handler on a directory or on a file that is not there fails when it is used
 * rather than when it is made. A path reaching outside the script's own folder is not prevented
 * either, so a script from an untrusted source can read or write anywhere the game can.
 * example:
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
 * // a handler is just a path, so it can be kept and used again later
 * print(notes.getFile().getName());
 * </pre>
 * @author Wagyourtail
 * @since 1.1.8
 */
@SuppressWarnings("unused")
public class FileHandler {
    private final File f;
    private final Charset charset;

    public FileHandler(String path) {
        this(new File(path), StandardCharsets.UTF_8);
    }

    public FileHandler(String path, String charset) {
        this(new File(path), charset);
    }

    public FileHandler(File path, String charset) {
        this(path, Objects.requireNonNull(Charset.forName(charset), () -> "Charset " + charset + " not found."));
    }

    public FileHandler(String path, Charset charset) {
        this(new File(path), charset);
    }

    public FileHandler(File path) {
        this(path, StandardCharsets.UTF_8);
    }

    public FileHandler(File path, Charset charset) {
        this.f = path;
        this.charset = charset;
    }

    /**
     * writes a string to the file. this is a destructive operation that replaces the file contents.
     * <br>
     * The file is opened in the handler's charset, which is UTF-8 unless another one was given,
     * and is created if it was not there. Nothing leading to it is created, so a path whose
     * directory does not exist fails rather than being made.
     * example:
     * <pre>
     * // a whole run's worth of output, replacing whatever was there
     * const log = FS.open("notes/log.txt");
     * log.write("the build finished\n");
     * log.append("and passed\n");
     * print(log.read());
     * </pre>
     *
     * @param s the text to write, encoded in the handler's charset
     * @return self
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
     * writes a byte array to the file. this is a destructive operation that replaces the file contents.
     * <br>
     * The bytes go out as they are, so this is the one to use for anything that is not text; the
     * handler's charset is not involved. As with the text form, the file is created if it was not
     * there and nothing on the way to it is created either.
     *
     * @param b the bytes to write
     * @return self
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
     * the whole file as one string, decoded with the handler's charset.<br>
     * The file is read all at once into memory, so this is for files a script ships or generates
     * rather than for anything large. Reading does not move anything, so it can be called as
     * often as wanted and gives the same answer each time, unlike the two streaming forms.
     * example:
     * <pre>
     * const config = FS.open("notes/config.txt");
     * for (const line of config.read().split("\n")) {
     *   if (line.length === 0) continue;
     *   const parts = line.split("=");
     *   print(`${parts[0]} is ${parts[1]}`);
     * }
     * </pre>
     *
     * @return the whole file, or an empty string for an empty one
     * @throws IOException if the file could not be read, including because it is not there
     * @since 1.1.8
     */
    public String read() throws IOException {
        return new String(readBytes(), charset);
    }

    /**
     * the whole file as raw bytes, with no decoding and no charset involved. This is what
     * {@link #read() read} is built on, and it is the form to use for anything that is not text,
     * such as an image or an archive a script ships with itself.<br>
     * The file is read all at once, and a file over two gigabytes is refused with an
     * {@link IOException} rather than being truncated, since the result has to fit in an array
     * whose length is an {@code int}.
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
            byte[] bytes = new byte[(int) f.length()];
            in.read(bytes);
            return bytes;
        }
    }

    /**
     * get an iterator for the lines in the file.
     * please call {@link FileLineIterator#close()} when you are done with the iterator to not leak resources.
     * <br>
     * This is the streaming form of reading. The file is decoded a line at a time in the
     * handler's charset and the underlying reader is held open until the iterator is closed, so
     * a file too large for {@link #read() read} can still be walked, at the cost of having to
     * remember to close it. A line is only read on when it is asked for, so the iterator sees
     * whatever the file holds at that point rather than a snapshot taken when it was made.<br>
     * There is a cleaner registered against it, so an iterator that is dropped without being
     * closed does eventually release the file handle, though not at any time you can rely on.
     * Closing is still the thing to do, and a {@code finally} is the place for it. Anything the
     * iteration throws on the way is wrapped in a {@link RuntimeException}.
     * example:
     * <pre>
     * const lines = FS.open("notes/log.txt").readLines();
     * try {
     *   while (lines.hasNext()) {
     *     const line = lines.next();
     *     if (line.length === 0) continue;
     *     print(line);
     *   }
     * } finally {
     *   // the reader stays open until this, so a walk that is cut short has to
     *   // come through here rather than just falling out of the loop
     *   lines.close();
     * }
     * </pre>
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
     * get an input stream for the file.
     * please call {@link java.io.BufferedInputStream#close()} when you are done with the stream to not leak resources.
     * <br>
     * The bytes come out exactly as they are on disk, with no charset involved, which makes this
     * the form to use for anything that is not text or that a script wants to copy through
     * unchanged. Unlike {@link #readBytes() readBytes} it is not read all at once: the file stays
     * open on this side until the stream is closed, and there is no cleaner on it, so forgetting
     * to close it leaks the handle for as long as the game runs.
     * example:
     * <pre>
     * // the bytes come out exactly as they are on disk, with no charset involved,
     * // so this is the form to use for an image or an archive rather than text
     * const stream = FS.open("assets/logo.png").streamBytes();
     * try {
     *   // it is a plain java stream, so it is java's methods rather than any
     *   // of its own
     *   print(`${stream.available()} bytes are buffered and ready to read`);
     * } finally {
     *   // nothing is attached to clean this up, so closing it is the job here
     *   stream.close();
     * }
     * </pre>
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
     * adds a string to the end of the file, leaving what is already there alone.<br>
     * The text is encoded in the handler's charset, and the file is created if it was not there,
     * so appending to a file a script has not written yet gives a file holding just this.
     * example:
     * <pre>
     * // a log that grows rather than one that is replaced
     * const log = FS.open("notes/log.txt");
     * log.append("started\n");
     * log.append("finished\n");
     * print(log.read());
     * </pre>
     *
     * @param s the text to add, encoded in the handler's charset
     * @return self
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
     * adds a byte array to the end of the file, leaving what is already there alone.<br>
     * The bytes go out as they are, so the handler's charset is not involved, which is what makes
     * this the form to use for appending to a file that is not text.
     *
     * @param b the bytes to add
     * @return self
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
     * the {@link File} this handler was made for, which is the path {@code FS.open} resolved
     * rather than the one that was asked for. It is the same file on every call, and it is a
     * plain {@code java.io.File}, so it can be used anywhere one is wanted: to check whether the
     * file is there, to ask for its size, or to get at its last modified time.
     * example:
     * <pre>
     * const handle = FS.open("notes/log.txt");
     * const file = handle.getFile();
     * print(`${file.getName()} is ${file.length()} bytes`);
     * </pre>
     */
    public File getFile() {
        return f;
    }

    public String toString() {
        return String.format("FileHandler:{\"file\": \"%s\"}", f.getAbsolutePath());
    }

    /**
     * the lines of one file, read a line at a time, and the reader behind them held open until
     * this is closed.<br>
     * A script gets one of these from {@link FileHandler#readLines() readLines} rather than making
     * one, and that is the only way to get at it. It is a plain {@link Iterator} of
     * {@link String Strings} and also closeable, so it is driven with
     * {@link #hasNext() hasNext} and {@link #next() next} and closed in a finally. Whether a
     * language's own iteration syntax accepts it is a question about that language rather than
     * about this class: it declares nothing but the three {@link Iterator} methods, so a for loop
     * over it works only where a guest language maps its own iteration onto those. The while
     * form below needs nothing from the language and is the one to write.<br>
     * A line here is whatever the charset's reader makes of a line ending, so a {@code \n}, a
     * {@code \r\n} and a lone {@code \r} all end one, and a file whose last line has no ending
     * still gives that line.
     * example:
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
         * opens a file and reads its first line straight away, so a file that is not there fails
         * here rather than on the first {@link #hasNext() hasNext}. A script has no reason to
         * call this directly, since it needs a {@link File} rather than a path relative to the
         * script, and {@link FileHandler#readLines() readLines} is the way in.
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
         * whether there is another line to read. The line after the current one has already been
         * read by the time this is asked, so a {@code true} here means {@link #next() next} will
         * give a line rather than {@code null}.<br>
         * An empty file answers {@code false} straight away, and so does a file that has been read
         * to its end.
         *
         * @return whether {@link #next() next} will return a line
         */
        @Override
        public boolean hasNext() {
            return nextLine != null;
        }

        /**
         * the next line, without its line ending, and reads the one after it ready for the call
         * after that.<br>
         * The line endings are dropped rather than handed on, so a line that was blank in the
         * file comes back as an empty string rather than as {@code null}. Asking for a line after
         * the last one gives {@code null} instead of failing, which is what the
         * {@link java.util.Iterator Iterator} contract asks for, so a loop that is driven by
         * {@link #hasNext() hasNext} is the right shape. A read that fails part way through is
         * wrapped in a {@link RuntimeException}, so it surfaces as that rather than as an
         * {@link IOException}.
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
         * closes the file behind the iterator, which releases the handle. Nothing else can be read
         * afterwards, and closing twice is harmless.<br>
         * This is the call the whole class exists to make possible: a cleaner is registered
         * against the iterator so a dropped one does eventually let go of the file, but at a time
         * that cannot be relied on, so closing is still the caller's job and a {@code finally} is
         * where it belongs.
         *
         * @throws IOException if the file could not be closed
         */
        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

}
