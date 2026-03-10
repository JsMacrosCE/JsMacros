package com.jsmacrosce.doclet.options;

import jdk.javadoc.doclet.Doclet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Links implements Doclet.Option {
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
        for (String listFile : new String[] { "package-list", "element-list" }) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(baseUrl + listFile).openStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty()) {
                        continue;
                    }

                    // element-list may contain module entries like "module:java.base"
                    if (line.startsWith("module:")) {
                        continue;
                    }

                    externalPackages.put(
                            line,
                            baseUrl + "index.html?" + line.replace(".", "/") + "/");
                }

                // Return early if possible
                return true;
            } catch (Exception e) {
                lastException = e;
            }
        }

        if (lastException != null) {
            lastException.printStackTrace();
        }
        return false;
    }
}
