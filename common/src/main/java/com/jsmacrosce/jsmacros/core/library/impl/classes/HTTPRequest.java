package com.jsmacrosce.jsmacros.core.library.impl.classes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletIgnore;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * A request configured with headers and timeouts, then sent synchronously to obtain a
 * {@link Response}. Use the {@code send} overloads for methods or bodies not covered by the
 * convenience methods; repeated sends open new connections using the current fields.
 * Responses for status codes 400 and above are returned normally with the error stream as their
 * body; connection or I/O failures throw {@link IOException}.
 * <pre>
 * // Configure once, then send.
 * const request = Request.create("https://example.com/api/echo")
 *   .addHeader("Content-Type", "application/json")
 *   .addHeader("User-Agent", "JsMacrosCE");
 *
 * const response = request.post('{"ping": true}');
 * print(`status ${response.responseCode}`);
 * print(response.text());
 *
 * // HTTP error statuses are responses too.
 * if (response.responseCode === 400) {
 *   print(`the server refused it: ${response.text()}`);
 * }
 *
 * // The same request can be sent again with another method.
 * const second = request.send("PUT", "replacement");
 * print(`that one came back with ${second.responseCode} and ${second.text().length} characters`);
 * </pre>
 * @author Wagyourtail
 * @since 1.1.8
 */
@SuppressWarnings("unused")
public class HTTPRequest {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /**
     * Headers to add to each request, keyed by name. This map is created per instance;
     * {@link #addHeader(String, String)} updates it, replacing an existing entry with the same key.
     */
    public Map<String, String> headers = new HashMap<>();
    /**
     * URL opened for each send. The field is public and may be reassigned between sends.
     */
    public URL conn;
    /**
     * Connection timeout in milliseconds, applied to each newly opened connection. Zero means no
     * timeout.
     */
    public int connectTimeout;
    /**
     * Read timeout in milliseconds, applied to each newly opened connection. Zero means no
     * timeout.
     */
    public int readTimeout;

    /**
     * Parses and stores a URL for later requests; this does not open a connection or send a request.
     * An invalid URL string causes {@link java.net.MalformedURLException}.
     *
     * @param url the URL string to parse
     */
    public HTTPRequest(String url) throws IOException {
        this.conn = new URL(url);
    }

    /**
     * Adds or replaces a header entry in {@link #headers}.
     *
     * @param key   the header name, sent as it is given
     * @param value the header value
     * @return self for chaining
     * @since 1.1.8
     */
    public HTTPRequest addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    /**
     * Sets the connection-establishment timeout in milliseconds for subsequent sends.<br>
     * Zero means no timeout. This does not change a connection that has already been opened or
     * impose an overall deadline on the complete request.
     *
     * @param timeout the connection timeout, in milliseconds
     * @return self for chaining
     * @since 1.8.6
     */
    public HTTPRequest setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    /**
     * Sets the response-read timeout in milliseconds for subsequent sends.<br>
     * Zero means no timeout. This limits waiting for response data rather than the total duration
     * of a request, and does not change connections that have already been opened.
     *
     * @param timeout the read timeout, in milliseconds
     * @return self for chaining
     * @since 1.8.6
     */
    public HTTPRequest setReadTimeout(int timeout) {
        this.readTimeout = timeout;
        return this;
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) conn.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return connection;
    }

    /**
     * sends the request as a GET and hands back what came of it.<br>
     * The headers are put on first and the method is set to {@code GET}, and the body of the
     * request is whatever the platform sends for a request without one. The call blocks until the
     * server answers or the connection fails.<br>
     * A status of four hundred or more is not a failure here: the error stream is read instead of
     * the input stream, so the body is the server's own error page and
     * {@link Response#responseCode} is there to be checked. A connection that never gets that far
     * is an {@link IOException}.
     * <pre>
     * const response = Request.create("https://example.com/api/status")
     *   .addHeader("Accept", "application/json")
     *   .get();
     * if (response.responseCode === 200) {
     *   print(response.text());
     * } else {
     *   print(`the server said ${response.responseCode}`);
     * }
     * </pre>
     *
     * @return the response, with its body still unread; for an error status, the body stream may
     *         be {@code null} if the connection supplies no error body
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.1.8
     */
    public Response get() throws IOException {
        HttpURLConnection conn = openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.setRequestMethod("GET");
        
        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * sends the request as a POST with a text body and hands back what came of it.<br>
     * The text is encoded as UTF-8 and its length is put on as the {@code Content-Length}, so the
     * caller does not have to set either. The headers are on the request first, which means a
     * {@code Content-Length} added through {@link #addHeader(String, String) addHeader} is
     * replaced by the one computed here rather than being sent twice.<br>
     * The call blocks, and a status of four hundred or more comes back as a response carrying the
     * server's error page rather than as a failure.
     * <pre>
     * const response = Request.create("https://example.com/api/echo")
     *   .addHeader("Content-Type", "text/plain")
     *   .post("hello");
     * print(`status ${response.responseCode}, body ${response.text()}`);
     * </pre>
     *
     * @param data the body, which is encoded as UTF-8
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.1.8
     */
    public Response post(String data) throws IOException {
        byte[] b = data.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.addRequestProperty("Content-Length", Integer.toString(b.length));
        conn.setRequestMethod("POST");

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(b);
        os.flush();
        os.close();

        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * Sends a POST request with the given bytes as its body, without encoding or modifying them.
     * The byte count is sent as {@code Content-Length}; status codes 400 and above are returned
     * as responses using the error stream as the body.
     *
     * @param data the body, which is sent as it is
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.8.4
     */
    public Response post(byte[] data) throws IOException {
        HttpURLConnection conn = openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.addRequestProperty("Content-Length", Integer.toString(data.length));
        conn.setRequestMethod("POST");

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(data);
        os.flush();
        os.close();

        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * Sends a PUT request with a text body encoded as UTF-8. The byte count is sent as
     * {@code Content-Length}; use {@link #put(byte[]) put} to send bytes without text encoding.
     *
     * @param data the body, which is encoded as UTF-8
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.8.4
     */
    public Response put(String data) throws IOException {
        byte[] b = data.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.addRequestProperty("Content-Length", Integer.toString(b.length));
        conn.setRequestMethod("PUT");

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(b);
        os.flush();
        os.close();

        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * Sends a PUT request with the given bytes as its body, without encoding or modifying them.
     * The byte count is sent as {@code Content-Length}.
     *
     * @param data the body, which is sent as it is
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.8.4
     */
    public Response put(byte[] data) throws IOException {
        HttpURLConnection conn = openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.addRequestProperty("Content-Length", Integer.toString(data.length));
        conn.setRequestMethod("PUT");

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(data);
        os.flush();
        os.close();

        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * Sends a request with the given method and no body. The method is passed to the connection
     * unchanged; a method unsupported by the platform is rejected there. Use a body-taking
     * {@link #send(java.lang.String, java.lang.String) overload} to send a body.
     * <pre>
     * const response = Request.create("https://example.com/api/session/1")
     *   .addHeader("Authorization", "Bearer not-a-real-token")
     *   .send("DELETE");
     * print(response.responseCode);
     * </pre>
     *
     * @param method the HTTP method to use, which must be one the platform knows
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent, if the platform does not know the
     *                     method, or if the answer could not be reached
     * @since 1.8.6
     */
    public Response send(String method) throws IOException {
        HttpURLConnection conn = openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.setRequestMethod(method);

        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * Sends a request with the given method and a text body encoded as UTF-8. The byte count is
     * sent as {@code Content-Length}; the method is passed to the connection unchanged.
     *
     * @param method the HTTP method to use, which must be one the platform knows
     * @param data   the body, which is encoded as UTF-8
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent, if the platform does not know the
     *                     method, or if the answer could not be reached
     * @since 1.8.4
     */
    public Response send(String method, String data) throws IOException {
        byte[] b = data.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.addRequestProperty("Content-Length", Integer.toString(b.length));
        conn.setRequestMethod(method);

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(b);
        os.flush();
        os.close();

        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * Sends a request with the given method and the given bytes as its body, without encoding or
     * modifying them. The byte count is sent as {@code Content-Length}; the method is passed to
     * the connection unchanged.
     *
     * @param method the HTTP method to use, which must be one the platform knows
     * @param data   the body, which is sent as it is
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent, if the platform does not know the
     *                     method, or if the answer could not be reached
     * @since 1.8.4
     */
    public Response send(String method, byte[] data) throws IOException {
        HttpURLConnection conn = openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.addRequestProperty("Content-Length", Integer.toString(data.length));
        conn.setRequestMethod(method);

        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(data);
        os.flush();
        os.close();

        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * Holds a response status and headers plus its unread body stream. Sending methods return
     * responses for HTTP error statuses as well as successful statuses. The body is consumed by
     * {@link #text()} or {@link #byteArray()}; whichever reads first leaves no data for the other.
     * @author Wagyourtail
     * @since 1.1.8
     */
    public static class Response {
        private final InputStream raw;
        @Nullable
        private String text;
        /**
         * Response headers as a map from each name to its returned values. This is {@code null}
         * when the response was constructed with {@code null} headers.
         */
        @Nullable
        public Map<String, List<String>> headers;
        /**
         * The response status code.
         */
        public int responseCode;

        /**
         * Wraps a body stream, status code, and optional response headers.
         *
         * @param inputStream  the body stream
         * @param responseCode the status the server answered with
         * @param headers      the response headers, or {@code null} to leave
         *                     {@link #headers} unset
         */
        public Response(InputStream inputStream, int responseCode, @Nullable Map<String, List<String>> headers) {
            this.raw = inputStream;
            this.responseCode = responseCode;
            if (headers != null) {
                this.headers = new HashMap<>(headers);
            }
        }

        /**
         * Reads the response body as UTF-8 text, ignoring any charset named in the response headers.
         * Line terminators are removed and remaining lines are joined with {@code \n}. The result
         * is cached for subsequent calls. Reading consumes the body stream, so calling
         * {@link #byteArray()} afterward returns no bytes.
         *
         * @return the body, or an empty string for an empty one
         * @since 1.1.8
         */
        public String text() {
            if (text != null) {
                return text;
            }
            text = new BufferedReader(
                    new InputStreamReader(raw, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return text;
        }

        /**
         * Consumes the body by calling {@link #text()}, then returns {@code null}. This method is
         * deprecated; parse the result of {@link #text()} in the guest language instead.
         *
         * @return {@code null} after consuming the response body
         * @since 1.1.8
         * @deprecated
         */
        @DocletIgnore
        @Deprecated
        public Object json() {
            text();
            return null;
        }

        /**
         * Reads the response body as raw bytes, without decoding. This consumes the body stream;
         * if {@link #text()} was called first, this returns an empty array.
         * <pre>
         * // saving a download beside the script that fetched it
         * const response = Request.get("https://example.com/assets/logo.png");
         * if (response.responseCode === 200) {
         *   FS.open("assets/logo.png").write(response.byteArray());
         * }
         * </pre>
         *
         * @return the body, or an empty array for an empty one
         * @throws IOException if the body could not be read off the connection
         * @since 1.2.2
         */
        public byte[] byteArray() throws IOException {
            return IOUtils.toByteArray(raw);
        }

    }

}
