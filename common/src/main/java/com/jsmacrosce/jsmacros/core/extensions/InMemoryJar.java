package com.jsmacrosce.jsmacros.core.extensions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Random-access reader for a ZIP archive held in memory.
 *
 * <p>Java's own tooling cannot open a JAR that is nested inside another JAR: neither
 * {@link java.net.URLClassLoader} nor {@link java.net.JarURLConnection} traverse the second
 * level. Extension archives are nested that way, so the loader reads them into memory and uses
 * this reader to serve their entries.
 *
 * <p>The central directory is parsed once up front, which gives constant-time lookup of an
 * entry's position without decompressing the whole archive. Entry data is inflated on demand.
 * Only the stored (compressed) form is retained, so holding an archive costs about what its
 * file on disk would cost.
 *
 * <p>Entries stored with the deflate method are inflated when read; entries stored uncompressed
 * are returned as they are. The ZIP64 and encrypted extensions are not supported.
 *
 * <p>The archive is not trusted to be well formed. Extension jars are user-installed, and a
 * malformed or repackaged archive must fail with a clear message instead of producing a
 * silently truncated class file or an unbounded allocation. Every read therefore checks the
 * entry name and size against the central directory, refuses entries above
 * {@link #MAX_ENTRY_BYTES}, and verifies the CRC-32 the archive declares.
 */
final class InMemoryJar {

    private static final int EOCD_SIGNATURE = 0x06054b50;
    private static final int CENTRAL_HEADER_SIGNATURE = 0x02014b50;
    private static final int LOCAL_HEADER_SIGNATURE = 0x04034b50;
    private static final int EOCD_MIN_SIZE = 22;
    private static final int CENTRAL_HEADER_SIZE = 46;
    private static final int LOCAL_HEADER_SIZE = 30;
    private static final int METHOD_STORED = 0;
    private static final int METHOD_DEFLATED = 8;
    /** Sentinel the ZIP format uses in a 32-bit field to mean "the real value is in the ZIP64 record". */
    private static final int ZIP64_SENTINEL = 0xffffffff;

    /**
     * Largest entry this reader will hand out. Classes and language files are far smaller; the
     * bound keeps a corrupt or hostile archive from turning one read into an out-of-memory error.
     */
    private static final int MAX_ENTRY_BYTES = 64 * 1024 * 1024;

    /** Location, size, and checksum of one archive entry, as declared by the central directory. */
    private record Entry(int method, int dataOffset, int compressedSize, int uncompressedSize, int crc) {
    }

    private final byte[] data;
    private final String name;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    /**
     * Parses the archive's central directory.
     *
     * @param data    the complete archive bytes
     * @param name    archive name, used in error messages and resource URLs
     * @throws IOException if the bytes are not a readable ZIP archive
     */
    InMemoryJar(byte[] data, String name) throws IOException {
        this.data = data;
        this.name = name;
        if (data.length < EOCD_MIN_SIZE) {
            throw new IOException(name + " is not a ZIP archive");
        }
        int eocd = findEndOfCentralDirectory();
        int entryCount = readShort(eocd + 10);
        int directoryOffset = readInt(eocd + 16);
        if (directoryOffset == ZIP64_SENTINEL) {
            throw new IOException(name + " uses the ZIP64 extension, which is not supported");
        }
        int offset = directoryOffset;
        for (int i = 0; i < entryCount; i++) {
            if (readInt(offset) != CENTRAL_HEADER_SIGNATURE) {
                throw new IOException("Malformed central directory in " + name);
            }
            int method = readShort(offset + 10);
            int crc = readInt(offset + 16);
            int compressedSize = readInt(offset + 20);
            int uncompressedSize = readInt(offset + 24);
            int nameLength = readShort(offset + 28);
            int extraLength = readShort(offset + 30);
            int commentLength = readShort(offset + 32);
            int localOffset = readInt(offset + 42);
            if (compressedSize == ZIP64_SENTINEL || uncompressedSize == ZIP64_SENTINEL
                    || localOffset == ZIP64_SENTINEL) {
                throw new IOException(name + " uses the ZIP64 extension, which is not supported");
            }
            String entryName = new String(data, offset + CENTRAL_HEADER_SIZE, nameLength, StandardCharsets.UTF_8);
            // A ZIP may list the same name twice. The classpath convention every other loader
            // follows is first-wins, so keep the first declaration rather than the last.
            entries.putIfAbsent(entryName, new Entry(method, localOffset, compressedSize, uncompressedSize, crc));
            offset += CENTRAL_HEADER_SIZE + nameLength + extraLength + commentLength;
        }
    }

    /**
     * Returns the archive's name.
     *
     * @return the name this archive was given
     */
    String name() {
        return name;
    }

    /**
     * Reports whether the archive contains an entry with the given name.
     *
     * @param name entry name, using forward slashes
     * @return whether the entry is present
     */
    boolean contains(String name) {
        return entries.containsKey(name);
    }

    /**
     * Reads and decompresses one entry.
     *
     * @param name entry name, using forward slashes
     * @return the entry's bytes, or {@code null} when the archive has no such entry
     * @throws IOException if the entry cannot be read, is truncated, exceeds the size bound, or
     *                     does not match the size or checksum the archive declares
     */
    byte[] read(String name) throws IOException {
        Entry entry = entries.get(name);
        if (entry == null) {
            return null;
        }
        if (entry.uncompressedSize() < 0 || entry.uncompressedSize() > MAX_ENTRY_BYTES) {
            throw new IOException("Entry " + name + " in " + this.name + " declares an unsupported size");
        }
        if (readInt(entry.dataOffset()) != LOCAL_HEADER_SIGNATURE) {
            throw new IOException("Malformed local header for entry " + name + " in " + this.name);
        }
        int localNameLength = readShort(entry.dataOffset() + 26);
        int localExtraLength = readShort(entry.dataOffset() + 28);
        long dataStartLong = (long) entry.dataOffset() + LOCAL_HEADER_SIZE + localNameLength + localExtraLength;
        if (dataStartLong > data.length) {
            throw new IOException("Truncated entry " + name + " in " + this.name);
        }
        int dataStart = (int) dataStartLong;
        if ((long) dataStart + entry.compressedSize() > data.length) {
            throw new IOException("Truncated entry " + name + " in " + this.name);
        }
        verifyLocalName(entry, name, localNameLength);

        byte[] result;
        if (entry.method() == METHOD_STORED) {
            if (entry.compressedSize() != entry.uncompressedSize()) {
                throw new IOException("Stored entry " + name + " in " + this.name + " declares inconsistent sizes");
            }
            result = new byte[entry.uncompressedSize()];
            System.arraycopy(data, dataStart, result, 0, result.length);
        } else if (entry.method() == METHOD_DEFLATED) {
            result = inflate(entry, name, dataStart);
        } else {
            throw new IOException("Unsupported compression method " + entry.method()
                    + " for entry " + name + " in " + this.name);
        }
        verifyCrc(result, entry.crc(), name);
        return result;
    }

    /**
     * Inflates a deflated entry, rejecting a stream that ends early or grows past the size the
     * archive declares. Both cases otherwise surface much later as a corrupt class or a resource
     * with missing content.
     */
    private byte[] inflate(Entry entry, String entryName, int dataStart) throws IOException {
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(data, dataStart, entry.compressedSize());
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, entry.uncompressedSize()));
            byte[] chunk = new byte[16384];
            while (!inflater.finished()) {
                int read = inflater.inflate(chunk);
                if (read > 0) {
                    if (out.size() + read > entry.uncompressedSize()) {
                        throw new IOException("Entry " + entryName + " in " + this.name
                                + " inflates beyond its declared size");
                    }
                    out.write(chunk, 0, read);
                } else if (inflater.finished()) {
                    // A valid empty deflated entry produces no bytes and finishes immediately.
                    break;
                } else if (inflater.needsInput() || inflater.needsDictionary()) {
                    throw new IOException("Truncated entry " + entryName + " in " + this.name);
                } else {
                    // A zero-length inflate with input still available would spin forever.
                    throw new IOException("Could not inflate entry " + entryName + " in " + this.name);
                }
            }
            return out.toByteArray();
        } catch (DataFormatException e) {
            throw new IOException("Corrupt entry " + entryName + " in " + this.name, e);
        } finally {
            inflater.end();
        }
    }

    /**
     * Compares the name in the local header with the name that was requested. A file can point
     * two central-directory entries at one local header, and a reader that trusts only the
     * central directory would serve one entry's bytes under another entry's name.
     */
    private void verifyLocalName(Entry entry, String entryName, int localNameLength) throws IOException {
        // dataStart already proved the local header and its name fit inside the archive.
        int localNameStart = entry.dataOffset() + LOCAL_HEADER_SIZE;
        String localName = new String(data, localNameStart, localNameLength, StandardCharsets.UTF_8);
        if (!localName.equals(entryName)) {
            throw new IOException("Local header name does not match entry " + entryName + " in " + this.name);
        }
    }

    private void verifyCrc(byte[] bytes, int expected, String entryName) throws IOException {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        if ((int) crc.getValue() != expected) {
            throw new IOException("Checksum mismatch for entry " + entryName + " in " + this.name);
        }
    }

    private int findEndOfCentralDirectory() throws IOException {
        int min = Math.max(0, data.length - EOCD_MIN_SIZE - 65535);
        for (int i = data.length - EOCD_MIN_SIZE; i >= min; i--) {
            if (readInt(i) == EOCD_SIGNATURE) {
                return i;
            }
        }
        throw new IOException("Not a ZIP archive");
    }

    private int readShort(int offset) throws IOException {
        if (offset < 0 || (long) offset + 2 > data.length) {
            throw new IOException("Truncated ZIP archive");
        }
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private int readInt(int offset) throws IOException {
        if (offset < 0 || (long) offset + 4 > data.length) {
            throw new IOException("Truncated ZIP archive");
        }
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }

}
