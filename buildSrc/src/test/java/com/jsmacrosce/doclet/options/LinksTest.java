package com.jsmacrosce.doclet.options;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class LinksTest {
    private static int cases;

    public static void main(String[] args) throws Exception {
        var originalPackages = new HashMap<>(Links.externalPackages);
        try {
            for (int status : new int[] { 400, 403, 404, 410 }) {
                run("permanent HTTP " + status, () -> checkFallback(status, "not found"));
            }
            run("module-aware element-list", LinksTest::checkModernIndex);
            run("soft-404 HTML", () -> checkFallback(200, "valid.before.invalid\n<!DOCTYPE html>"));
            run("empty index", () -> checkFallback(200, " \n\n"));
            run("invalid module", () -> checkFallback(200, "module:invalid/module\njava.lang\n"));
            for (int status : new int[] { 408, 429, 503 }) {
                run("transient HTTP " + status, () -> checkTransientFailure(status));
            }
            run("missing both indexes", () -> checkFailure(404, 1, 0));
            run("bounded transient failures", () -> checkFailure(503, 4, 6));
            run("legacy file URL", () -> checkFileFallback(Path.of(args[0])));
        } finally {
            Links.externalPackages.clear();
            Links.externalPackages.putAll(originalPackages);
        }
        System.out.println("External Javadoc indexes: " + cases + " regression cases passed");
    }

    private static void checkFallback(int status, String content) throws Exception {
        try (var fixture = new Fixture(attempt -> new Reply(status, content),
                attempt -> new Reply(200, "com.example.legacy\n"))) {
            var links = new CountingLinks();
            check(links.process("-link", List.of(fixture.baseUrl())), "Legacy fallback failed");
            check(fixture.elementRequests.get() == 1, "Permanent/invalid element-list was retried");
            check(fixture.packageRequests.get() == 1, "Expected one package-list request");
            check(links.pauses == 0, "Permanent/invalid response caused a retry delay");
            check((fixture.baseUrl() + "/com/example/legacy/").equals(Links.externalPackages.get("com.example.legacy")),
                "Legacy package URL changed");
            check(!Links.externalPackages.containsKey("valid.before.invalid"), "Partial invalid index leaked packages");
        }
    }

    private static void checkModernIndex() throws Exception {
        try (var fixture = new Fixture(attempt -> new Reply(200,
                "\nmodule:java.base\njava.lang\njava.util\nmodule:java.logging\njava.util.logging\n"),
                attempt -> new Reply(404, ""))) {
            var links = new CountingLinks();
            check(links.process("-link", List.of(fixture.baseUrl() + "/")), "Modern index failed");
            check(fixture.elementRequests.get() == 1 && fixture.packageRequests.get() == 0, "Unnecessary fallback");
            check(links.pauses == 0, "Successful index caused a retry delay");
            check((fixture.baseUrl() + "/java.base/java/lang/").equals(Links.externalPackages.get("java.lang")),
                "java.base module prefix lost");
            check((fixture.baseUrl() + "/java.logging/java/util/logging/").equals(Links.externalPackages.get("java.util.logging")),
                "Second module prefix lost");
        }
    }

    private static void checkTransientFailure(int status) throws Exception {
        try (var fixture = new Fixture(attempt -> attempt < 3 ? new Reply(status, "retry") : new Reply(200, "java.util\n"),
                attempt -> new Reply(404, ""))) {
            var links = new CountingLinks();
            check(links.process("-link", List.of(fixture.baseUrl())), "Transient failure did not recover");
            check(fixture.elementRequests.get() == 3, "Expected two retries before success");
            check(fixture.packageRequests.get() == 0, "Fell back before completing transient retries");
            check(links.pauses == 2, "Transient retry delays were not preserved");
            check((fixture.baseUrl() + "/java/util/").equals(Links.externalPackages.get("java.util")), "Recovered package missing");
        }
    }

    private static void checkFailure(int status, int attemptsPerIndex, int pauses) throws Exception {
        try (var fixture = new Fixture(attempt -> new Reply(status, "unavailable"), attempt -> new Reply(status, "unavailable"))) {
            var links = new CountingLinks();
            var errors = new ByteArrayOutputStream();
            PrintStream original = System.err;
            try (var capture = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
                System.setErr(capture);
                check(!links.process("-link", List.of(fixture.baseUrl())), "Unavailable indexes were accepted");
            } finally {
                System.setErr(original);
            }
            check(fixture.elementRequests.get() == attemptsPerIndex, "Unexpected element-list attempt count");
            check(fixture.packageRequests.get() == attemptsPerIndex, "Unexpected package-list attempt count");
            check(links.pauses == pauses, "Unexpected failure delay count");
            check(Links.externalPackages.isEmpty(), "Failed index registered packages");
            String diagnostic = errors.toString(StandardCharsets.UTF_8);
            check(diagnostic.contains("cannot read external Javadoc index") && diagnostic.contains(fixture.baseUrl())
                && diagnostic.contains("HTTP " + status), "Failure diagnostic lost the URL or cause");
        }
    }

    private static void checkFileFallback(Path root) throws Exception {
        Files.createDirectories(root);
        Path legacy = Files.createTempDirectory(root, "legacy-");
        Files.writeString(legacy.resolve("package-list"), "java.lang\n");
        var links = new CountingLinks();
        String baseUrl = legacy.toUri().toString();
        check(links.process("-link", List.of(baseUrl)), "Legacy file URL failed");
        check(links.pauses == 0, "Missing element-list file caused retry delays");
        check((baseUrl + "java/lang/").equals(Links.externalPackages.get("java.lang")), "Legacy file package URL changed");
    }

    private static void run(String name, CheckedCase action) throws Exception {
        Links.externalPackages.clear();
        try {
            action.run();
            cases++;
        } catch (AssertionError e) {
            throw new AssertionError(name + ": " + e.getMessage(), e);
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface CheckedCase {
        void run() throws Exception;
    }

    private static final class CountingLinks extends Links {
        private int pauses;

        @Override
        void pauseBetweenAttempts() {
            pauses++;
        }
    }

    private record Reply(int status, String body) {}

    @FunctionalInterface
    private interface Response {
        Reply reply(int attempt);
    }

    private static final class Fixture implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger elementRequests = new AtomicInteger();
        private final AtomicInteger packageRequests = new AtomicInteger();

        private Fixture(Response elementResponse, Response packageResponse) throws Exception {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                Reply reply = switch (path) {
                    case "/element-list" -> elementResponse.reply(elementRequests.incrementAndGet());
                    case "/package-list" -> packageResponse.reply(packageRequests.incrementAndGet());
                    default -> new Reply(404, "unexpected path");
                };
                byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
                try (exchange) {
                    exchange.sendResponseHeaders(reply.status(), bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
