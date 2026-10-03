import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.ClassWrapperFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.NumberCompareFilter;
import com.jsmacrosce.jsmacros.core.library.impl.classes.FileHandler;
import com.jsmacrosce.jsmacros.core.library.impl.classes.HTTPRequest;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Runs standalone assertions against complete production HTTP, file, and scanner
 * filter classes, without Minecraft, Mixins, or extracted method implementations.
 *
 * <p>HTTP checks inspect the connection actually used by each request overload
 * and exercise real read timeouts against a temporary loopback server. File checks
 * cover empty and multi-megabyte binary reads. Filter checks cover numeric
 * comparison diagnostics and inherited, overridden, bridge, and missing methods.</p>
 *
 * <p>The suite cleans up its temporary file and HTTP server. It does not validate
 * client integration or simulate a stalled TCP connection handshake.</p>
 */
public class MiscApiCoreRegressions {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface RequestCall {
        HTTPRequest.Response invoke(HTTPRequest request) throws IOException;
    }

    private record RequestCase(String name, String method, byte[] body, RequestCall call) {}

    private static List<RequestCase> requestCases() {
        byte[] bytes = "AéZ".getBytes(StandardCharsets.UTF_8);
        return List.of(
                new RequestCase("get", "GET", new byte[0], HTTPRequest::get),
                new RequestCase("post string", "POST", bytes, r -> r.post("AéZ")),
                new RequestCase("post bytes", "POST", bytes, r -> r.post(bytes)),
                new RequestCase("put string", "PUT", bytes, r -> r.put("AéZ")),
                new RequestCase("put bytes", "PUT", bytes, r -> r.put(bytes)),
                new RequestCase("send method", "GET", new byte[0], r -> r.send("GET")),
                new RequestCase("send string", "POST", bytes, r -> r.send("POST", "AéZ")),
                new RequestCase("send bytes", "PUT", bytes, r -> r.send("PUT", bytes))
        );
    }

    /** Records the connection actually used by each production request path, not a separate helper call. */
    private static class RecordingConnection extends HttpURLConnection {
        final ByteArrayOutputStream sent = new ByteArrayOutputStream();

        RecordingConnection(URL url) { super(url); }

        @Override public void connect() {}
        @Override public void disconnect() {}
        @Override public boolean usingProxy() { return false; }
        @Override public OutputStream getOutputStream() { return sent; }
        @Override public int getResponseCode() { return 200; }
        @Override public InputStream getInputStream() {
            return new ByteArrayInputStream("fixture-ok".getBytes(StandardCharsets.UTF_8));
        }
        @Override public Map<String, List<String>> getHeaderFields() { return Map.of(); }
    }

    private static void configuredHttpPaths() throws Exception {
        for (RequestCase test : requestCases()) {
            RecordingConnection[] opened = new RecordingConnection[1];
            URL url = new URL(null, "http://fixture.invalid/", new URLStreamHandler() {
                @Override protected URLConnection openConnection(URL address) {
                    check(opened[0] == null, test.name + " opens only one connection");
                    return opened[0] = new RecordingConnection(address);
                }
            });
            HTTPRequest request = new HTTPRequest("http://127.0.0.1/");
            request.conn = url;
            request.setConnectTimeout(321).setReadTimeout(654).addHeader("X-Misc-Test", "present");
            check(test.call.invoke(request).text().equals("fixture-ok"), test.name + " response");
            RecordingConnection connection = opened[0];
            check(connection != null, test.name + " actually opens connection");
            check(connection.getConnectTimeout() == 321, test.name + " actual connect timeout");
            check(connection.getReadTimeout() == 654, test.name + " actual read timeout");
            check(connection.getRequestMethod().equals(test.method), test.name + " method");
            check(Arrays.equals(connection.sent.toByteArray(), test.body), test.name + " body");
            check("present".equals(connection.getRequestProperty("X-Misc-Test")), test.name + " header");
        }
        System.out.println("PASS HTTP timeout configuration on all eight actual request paths");
    }

    private static void realReadTimeouts() throws Exception {
        // Handler signals after accepting and reading the request: connect timeout cannot satisfy the test.
        CountDownLatch[] accepted = {new CountDownLatch(1)};
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        var executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            accepted[0].countDown();
            try {
                Thread.sleep(600);
                byte[] data = "fixture-ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, data.length);
                exchange.getResponseBody().write(data);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // Client closing after read timeout is expected.
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
            // First prove this same endpoint responds normally with a sufficiently long deadline.
            for (RequestCase test : requestCases()) {
                accepted[0] = new CountDownLatch(1);
                check(test.call.invoke(new HTTPRequest(address).setConnectTimeout(2000).setReadTimeout(3000))
                        .text().equals("fixture-ok"), test.name + " long read deadline succeeds");
                check(accepted[0].await(1, TimeUnit.SECONDS), test.name + " normal request accepted");
                accepted[0] = new CountDownLatch(1);
                long start = System.nanoTime();
                try {
                    test.call.invoke(new HTTPRequest(address).setConnectTimeout(2000).setReadTimeout(100));
                    throw new AssertionError(test.name + " ignored read timeout");
                } catch (SocketTimeoutException expected) {
                    check(accepted[0].await(1, TimeUnit.SECONDS), test.name + " timed-out request accepted");
                    check(expected.getMessage().toLowerCase().contains("read"), test.name + " read timeout, not connect timeout");
                    long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    check(elapsed >= 50 && elapsed < 2000, test.name + " deadline elapsed=" + elapsed);
                }
            }
        } finally {
            server.stop(0);
            executor.shutdownNow();
            check(executor.awaitTermination(5, TimeUnit.SECONDS), "HTTP fixture executor stopped");
        }
        System.out.println("PASS real loopback read timeouts and successful control for all eight overloads");
    }

    private static void fullFileReads() throws Exception {
        var path = Files.createTempFile("misc-api-core-", ".bin");
        try {
            FileHandler helper = new FileHandler(path.toFile());
            check(helper.readBytes().length == 0, "empty file");
            byte[] bytes = new byte[2 * 1024 * 1024 + 7];
            for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
            helper.write(bytes);
            check(Arrays.equals(helper.readBytes(), bytes), "multi-megabyte file all bytes");
            helper.write(new byte[]{-128, 0, 127});
            check(Arrays.equals(helper.readBytes(), new byte[]{-128, 0, 127}), "binary file and overwrite");
        } finally {
            Files.deleteIfExists(path);
            check(!Files.exists(path), "temporary file removed");
        }
        System.out.println("PASS FileHandler complete byte reads");
    }

    /** Supplies public methods for inherited and overridden filter-lookup checks. */
    public static class ParentFixture {
        public String inheritedName() { return "parent"; }
        public String overriddenName() { return "parent"; }
    }
    /** Adds a boolean property and overrides a parent method for lookup checks. */
    public static class Fixture extends ParentFixture {
        public boolean enabled() { return true; }
        @Override public String overriddenName() { return "child"; }
    }
    /**
     * Supplies a generic return type so a specialized override generates a bridge method.
     *
     * @param <T> return type specialized by the child fixture
     */
    public static class GenericParent<T> { public T value() { return null; } }
    /** Specializes the generic method to test collisions with compiler-generated bridges. */
    public static class BridgeFixture extends GenericParent<String> {
        @Override public String value() { return "bridge"; }
    }
    private static class FixtureFilter extends ClassWrapperFilter<Fixture> {
        FixtureFilter(String method, Object... args) {
            super(method, getPublicNoParameterMethods(Fixture.class), new Object[0], args);
        }
        static Map<String, Method> methods(Class<?> clazz) { return getPublicNoParameterMethods(clazz); }
    }

    private static void filterChecks() throws Exception {
        for (Number value : List.<Number>of((byte) 2, (short) 2, 2, 2L, 2F, 2D)) {
            String[] operations = {"<", ">", "<=", ">=", "==", "!="};
            boolean[][] outcomes = {
                    {true, false, false}, {false, false, true}, {true, true, false},
                    {false, true, true}, {false, true, false}, {true, false, true}
            };
            for (int op = 0; op < operations.length; op++) {
                for (int input = 1; input <= 3; input++) {
                    check(new NumberCompareFilter(operations[op], value).apply(input) == outcomes[op][input - 1],
                            value.getClass() + " " + input + " " + operations[op] + " 2");
                }
            }
            try {
                new NumberCompareFilter("=>", value).apply(value);
                throw new AssertionError("invalid operation accepted");
            } catch (IllegalArgumentException expected) {
                check(expected.getMessage().contains("< > <= >= == !="), value.getClass() + " diagnostic");
            }
        }
        check(new FixtureFilter("inheritedName", "EQUALS", "parent").apply(new Fixture()), "inherited filter method");
        check(FixtureFilter.methods(Fixture.class).get("inheritedName").getDeclaringClass() == ParentFixture.class,
                "inherited method declaring class");
        check(FixtureFilter.methods(Fixture.class).get("overriddenName").getDeclaringClass() == Fixture.class,
                "override prefers child declaring class");
        check(new FixtureFilter("overriddenName", "EQUALS", "child").apply(new Fixture()), "selected override invoked");
        check(!FixtureFilter.methods(Fixture.class).containsKey("getClass"), "Object methods excluded");
        check(FixtureFilter.methods(BridgeFixture.class).containsKey("value"), "bridge method collision merged");
        Method selected = FixtureFilter.methods(BridgeFixture.class).get("value");
        check(selected.getDeclaringClass() == BridgeFixture.class, "bridge collision keeps most-derived declaration");
        check(selected.invoke(new BridgeFixture()).equals("bridge"), "merged bridge entry is callable");
        try {
            new FixtureFilter("missing");
            throw new AssertionError("unknown method accepted");
        } catch (NullPointerException expected) {
            check(expected.getMessage().contains("Unknown filter method: missing"), "clear missing-method diagnostic");
        }
        System.out.println("PASS numeric diagnostics and inherited/bridge filter lookup");
    }

    /**
     * Runs all core assertions and prints a summary after they complete successfully.
     *
     * @param args unused command-line arguments
     * @throws Exception if fixture setup or a production operation fails unexpectedly
     * @throws AssertionError if a regression assertion fails
     */
    public static void main(String[] args) throws Exception {
        configuredHttpPaths();
        realReadTimeouts();
        fullFileReads();
        filterChecks();
        System.out.println("PASS " + checks + " standalone JVM assertions (not client/Mixin coverage)");
    }
}
