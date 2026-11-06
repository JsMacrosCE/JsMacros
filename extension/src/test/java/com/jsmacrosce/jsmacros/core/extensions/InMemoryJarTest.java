package com.jsmacrosce.jsmacros.core.extensions;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryJarTest {

    private static byte[] archive(String... entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String entry : entries) {
                int separator = entry.indexOf('=');
                String name = entry.substring(0, separator);
                byte[] content = entry.substring(separator + 1).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ZipEntry zipEntry = new ZipEntry(name);
                if (content.length > 40) {
                    zipEntry.setMethod(ZipEntry.DEFLATED);
                } else {
                    zipEntry.setMethod(ZipEntry.STORED);
                    zipEntry.setSize(content.length);
                    zipEntry.setCompressedSize(content.length);
                    java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                    crc.update(content);
                    zipEntry.setCrc(crc.getValue());
                }
                zip.putNextEntry(zipEntry);
                zip.write(content);
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    void readsStoredAndDeflatedEntries() throws IOException {
        String large = "x".repeat(500);
        InMemoryJar jar = new InMemoryJar(archive("small.txt=hello", "large.txt=" + large), "test.jar");

        assertEquals("hello", new String(jar.read("small.txt"), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(large, new String(jar.read("large.txt"), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void reportsMissingEntries() throws IOException {
        InMemoryJar jar = new InMemoryJar(archive("small.txt=hello"), "test.jar");

        assertTrue(jar.contains("small.txt"));
        assertFalse(jar.contains("other.txt"));
        assertNull(jar.read("other.txt"));
    }

    @Test
    void listsEntryNames() throws IOException {
        InMemoryJar jar = new InMemoryJar(archive("a.txt=1", "b.txt=2"), "test.jar");

        assertTrue(jar.contains("a.txt"));
        assertTrue(jar.contains("b.txt"));
        assertFalse(jar.contains("c.txt"));
    }

    @Test
    void rejectsDataThatIsNotAnArchive() {
        assertThrows(IOException.class, () -> new InMemoryJar("not a zip file at all".getBytes(), "broken.jar"));
    }

    @Test
    void readsAnArchiveNestedInsideAnotherArchive() throws IOException {
        byte[] inner = archive("nested.txt=inner content");
        ByteArrayOutputStream outerBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(outerBytes)) {
            zip.putNextEntry(new ZipEntry("META-INF/jars/inner.jar"));
            zip.write(inner);
            zip.closeEntry();
        }

        InMemoryJar outer = new InMemoryJar(outerBytes.toByteArray(), "outer.jar");
        byte[] nested = outer.read("META-INF/jars/inner.jar");
        assertNotNull(nested);
        assertArrayEquals(inner, nested);

        InMemoryJar innerJar = new InMemoryJar(nested, "inner.jar");
        assertEquals("inner content", new String(innerJar.read("nested.txt"), java.nio.charset.StandardCharsets.UTF_8));
    }

}
