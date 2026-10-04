package com.jsmacrosce.doclet.options;

import com.jsmacrosce.doclet.DocletIgnore;
import jdk.javadoc.doclet.Doclet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.SourceVersion;

@DocletIgnore
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
        for (String listFile : new String[] { "element-list", "package-list" }) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(baseUrl + listFile).openStream()))) {
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
                        if (!SourceVersion.isName(module)) throw new IOException("Invalid Javadoc module: " + line);
                        modulePath = module + "/";
                        continue;
                    }
                    // Some hosts return a soft-404 HTML page with HTTP 200. Do not
                    // mistake it for a successful package list and lose every link.
                    if (!SourceVersion.isName(line)) throw new IOException("Invalid Javadoc package: " + line);
                    packages.put(
                            line,
                            baseUrl + modulePath + line.replace(".", "/") + "/");
                }
                if (packages.isEmpty()) throw new IOException("Empty Javadoc package index: " + baseUrl + listFile);
                externalPackages.putAll(packages);
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
