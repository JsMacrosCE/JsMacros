package com.jsmacrosce.doclet.core.webdoclet;

import com.jsmacrosce.doclet.DocletIgnore;

/**
 * The site-level facts that appear in every generated page's {@code <head>}.
 *
 * <p>The legacy HTML pages are written by the webdoclet as standalone files that
 * VitePress never processes, so each one carries its own title, description, and
 * link-preview tags instead of inheriting a site shell. They are API pages, so they
 * preview as text: the social image and {@code og:url} are omitted. Every page
 * would show the same logo, and the files have no single public address that a
 * canonical could honestly point at.
 */
@DocletIgnore
public final class SiteMetadata {
    public static final String SITE_NAME = "JsMacrosCE";
    private static final int DESCRIPTION_LIMIT = 160;

    private SiteMetadata() {
    }

    /** Escapes text for an element body. */
    public static String text(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    /** Escapes text for a double-quoted attribute value. */
    public static String attribute(String value) {
        return text(value).replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * Flattens prose to the single line a meta description can hold.
     *
     * <p>Search engines show roughly 160 characters, and a longer value is cut
     * mid-word by the client, so cut it at a word boundary first.
     */
    public static String description(String value) {
        String clean = value == null ? "" : value
            .replaceAll("<[^>]+>", " ")
            .replace("`", "")
            .replaceAll("\\s+", " ")
            .trim();
        if (clean.length() <= DESCRIPTION_LIMIT) {
            return clean;
        }
        int cut = clean.lastIndexOf(' ', DESCRIPTION_LIMIT - 2);
        if (cut < 100) {
            cut = DESCRIPTION_LIMIT - 2;
        }
        return clean.substring(0, cut).trim() + "…";
    }
}
