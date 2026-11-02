package com.jsmacrosce.doclet.core.webdoclet;

/**
 * Checks that a generated page description keeps literal type arguments.
 *
 * <p>The caller has already flattened Javadoc markup, so angle brackets that remain
 * are content such as a generic argument, not tags to remove. They must still be
 * escaped when written into an HTML attribute.
 */
public final class SiteMetadataTest {
    public static void main(String[] args) {
        String described = SiteMetadata.description("Returns a Map<String, Integer> from a List<Player>.");
        equals(described, "Returns a Map<String, Integer> from a List<Player>.");
        contains(SiteMetadata.attribute(described), "Map&lt;String, Integer&gt;");
        excludes(SiteMetadata.attribute(described), "<String,");
        System.out.println("SiteMetadata: literal type arguments and attribute escaping passed");
    }

    private static void equals(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected: " + expected + "\nActual: " + actual);
        }
    }

    private static void contains(String text, String expected) {
        if (!text.contains(expected)) throw new AssertionError("Missing: " + expected + "\nOutput:\n" + text);
    }

    private static void excludes(String text, String unexpected) {
        if (text.contains(unexpected)) throw new AssertionError("Unexpected: " + unexpected + "\nOutput:\n" + text);
    }
}
