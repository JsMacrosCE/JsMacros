package com.jsmacrosce.doclet.options;

import com.jsmacrosce.doclet.DocletIgnore;
import jdk.javadoc.doclet.Doclet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;

@DocletIgnore
public class Links implements Doclet.Option {
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;
    private static final int FETCH_RETRIES = 3;

    public static Map<String, String> externalPackages = new HashMap<>();

    @Override
    public int getArgumentCount() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "link external javadoc";
    }

    @Override
    public Kind getKind() {
        return Kind.STANDARD;
    }

    @Override
    public List<String> getNames() {
        return List.of("-link");
    }

    @Override
    public String getParameters() {
        return "<javadocurl: URL>";
    }

    @Override
    public boolean process(String option, List<String> arguments) {
        String baseUrl = arguments.getFirst();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        Exception lastException = null;
        for (String listFile : new String[] { "element-list", "package-list" }) {
            for (int attempt = 0; attempt <= FETCH_RETRIES; attempt++) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(open(baseUrl + listFile), StandardCharsets.UTF_8))) {
                    String line;
                    String modulePath = "";
                    Map<String, String> packages = new HashMap<>();
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();

                        if (line.isEmpty()) {
                            continue;
                        }

                        // Modern Javadoc puts classes beneath their module directory.
                        if (line.startsWith("module:")) {
                            String module = line.substring("module:".length());
                            if (!SourceVersion.isName(module)) throw new NonRetryableIndexException("Invalid Javadoc module: " + line);
                            modulePath = module + "/";
                            continue;
                        }
                        // Some hosts return a soft-404 HTML page with HTTP 200. Do not
                        // mistake it for a successful package list and lose every link.
                        if (!SourceVersion.isName(line)) throw new NonRetryableIndexException("Invalid Javadoc package: " + line);
                        packages.put(
                                line,
                                baseUrl + modulePath + line.replace(".", "/") + "/");
                    }
                    if (packages.isEmpty()) throw new NonRetryableIndexException("Empty Javadoc package index: " + baseUrl + listFile);
                    externalPackages.putAll(packages);
                    return true;
                } catch (FileNotFoundException | NonRetryableIndexException e) {
                    // Older Javadoc has only package-list. Missing/invalid indexes
                    // should fall back immediately, not sleep through four attempts.
                    lastException = e;
                    break;
                } catch (Exception e) {
                    lastException = e;
                    // Retry a momentary blip; a persistent failure still fails generation.
                    if (attempt < FETCH_RETRIES) pauseBetweenAttempts();
                }
            }
        }

        // Javadoc reports a rejected option only as "an unknown error has occurred",
        // so name the index and the cause before returning false.
        System.err.println("error: cannot read external Javadoc index " + baseUrl
                + " (" + (lastException == null ? "unknown error" : lastException) + ")");
        return false;
    }

    void pauseBetweenAttempts() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static InputStream open(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "jsmacrosce-doclet");
        if (connection instanceof HttpURLConnection http) {
            int status = http.getResponseCode();
            if (status >= 400) {
                http.disconnect();
                String message = "HTTP " + status + " reading Javadoc index " + url;
                if (status == 408 || status == 429 || status >= 500) {
                    throw new IOException(message);
                }
                throw new NonRetryableIndexException(message);
            }
        }
        return connection.getInputStream();
    }

    private static final class NonRetryableIndexException extends IOException {
        private NonRetryableIndexException(String message) {
            super(message);
        }
    }
}
