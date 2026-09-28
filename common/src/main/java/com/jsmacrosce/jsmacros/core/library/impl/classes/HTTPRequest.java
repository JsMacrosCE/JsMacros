package com.jsmacrosce.jsmacros.core.library.impl.classes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

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
 * one request, built up and then sent, which is what {@code Request.create} hands back.<br>
 * The {@code Request} library's own one call forms are enough for most things, and this is the
 * form to reach for when a request needs more than they cover: several headers, a body on a
 * method other than POST, or one of the {@code send} overloads for a method that has no shortcut.
 * Every call on it returns the same object, so the setup and the send chain.<br>
 * The four public fields can be written directly as well, and the two setters do nothing but
 * assign to two of them, so a header can go on either way. What none of them is is a limit: see
 * the note on {@link #setConnectTimeout(int) setConnectTimeout} before relying on either timeout.
 * <br>
 * The call blocks. A script waits for the server rather than for a callback, and that is true of
 * every form here, so pointing one at an unreachable host from inside a listener leaves the
 * event it is handling waiting as well. What comes back is a {@link Response}, and a status of
 * four hundred or more is not a failure: the body is the error page rather than the answer, and
 * the status is there to be checked. A connection that never gets that far is an
 * {@link IOException} instead.
 * example:
 * <pre>
 * // built once and then sent, with the setup chained onto the front
 * const request = Request.create("https://example.com/api/echo")
 *   .addHeader("Content-Type", "application/json")
 *   .addHeader("User-Agent", "JsMacrosCE");
 *
 * const response = request.post('{"ping": true}');
 * print(`status ${response.responseCode}`);
 * print(response.text());
 *
 * // a four hundred or more is a response rather than a failure, so the status
 * // is what gets checked and the body is the server's own
 * if (response.responseCode === 400) {
 *   print(`the server refused it: ${response.text()}`);
 * }
 *
 * // the same object can be sent again with a different method, and the byte
 * // array form of the body is the same call without the encoding step
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
     * the headers to put on the request, keyed by name, as a plain Java {@link Map} rather than a
     * script object.<br>
     * A new one is made for every request, so it can be written to directly and is not shared
     * with anything. {@link #addHeader(String, String) addHeader} is the usual way to fill it in,
     * and putting the same name twice replaces the earlier value rather than sending it twice.
     */
    public Map<String, String> headers = new HashMap<>();
    /**
     * the address this request goes to, as a {@link URL}, which is what it was built from. It is
     * never rewritten, so a request that is sent more than once always goes to the same place
     * even if the fields around it are changed in between.
     */
    public URL conn;
    /**
     * the value handed to {@link #setConnectTimeout(int) setConnectTimeout}, in milliseconds,
     * zero if it was never set.<br>
     * As of the source this was written against, nothing reads it, and no timeout is applied to
     * the connection, so a request built through this class has no time limit of its own and
     * falls back to what {@link java.net.HttpURLConnection HttpURLConnection} does by default.
     * The field and the setter are still there, so the value is worth writing down in a script
     * that wants to be explicit about it, but nothing should be built on it taking effect.
     */
    public int connectTimeout;
    /**
     * the value handed to {@link #setReadTimeout(int) setReadTimeout}, in milliseconds, zero if
     * it was never set.<br>
     * As of the source this was written against, nothing reads it, and no timeout is applied to
     * the connection, so a response that never arrives keeps the calling script waiting. The
     * field and the setter are still there, but nothing should be built on them taking effect.
     */
    public int readTimeout;

    public HTTPRequest(String url) throws IOException {
        this.conn = new URL(url);
    }

    /**
     * puts a header on the request, and is the same as putting the name and the value into
     * {@link #headers}.<br>
     * A name that is already there is replaced rather than added to, so a request cannot end up
     * carrying the same header twice from this side.
     * example:
     * <pre>
     * const request = Request.create("https://example.com/api/echo")
     *   .addHeader("Content-Type", "application/json")
     *   .addHeader("Authorization", "Bearer not-a-real-token");
     * print(request.headers.get("Content-Type"));
     * </pre>
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
     * records the connection timeout in milliseconds, and does nothing else with it.<br>
     * This assigns {@link #connectTimeout} and returns. As of the source this was written against
     * nothing reads that field and no timeout is put on the connection, so a request built through
     * this class waits for as long as the host takes whatever the platform default is. Do not
     * count on this bounding a slow or unreachable host.
     * example:
     * <pre>
     * // recorded on the request, so it can be read back, but not applied to it
     * const request = Request.create("https://example.com/api/status")
     *   .setConnectTimeout(5000);
     * print(request.connectTimeout);
     * </pre>
     *
     * @param timeout the value to record, in milliseconds
     * @return self for chaining
     * @since 1.8.6
     */
    public HTTPRequest setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    /**
     * records the read timeout in milliseconds, and does nothing else with it.<br>
     * This assigns {@link #readTimeout} and returns. As of the source this was written against
     * nothing reads that field and no timeout is put on the connection, so a server that accepts
     * the request and then never answers leaves the calling script waiting. Do not count on this
     * bounding a slow response.
     *
     * @param timeout the value to record, in milliseconds
     * @return self for chaining
     * @since 1.8.6
     */
    public HTTPRequest setReadTimeout(int timeout) {
        this.readTimeout = timeout;
        return this;
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
     * example:
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
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.1.8
     */
    public Response get() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) this.conn.openConnection();
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
     * example:
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
        HttpURLConnection conn = (HttpURLConnection) this.conn.openConnection();
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
     * sends the request as a POST with a byte array body and hands back what came of it.<br>
     * This is {@link #post(java.lang.String) post} without the encoding step, so the bytes go out
     * exactly as given, which is what makes it the form to use for a body that is not text. The
     * length is put on as the {@code Content-Length} just the same.
     * example:
     * <pre>
     * // posting a file a script ships, without going through text on the way
     * const body = FS.open("payload.json").readBytes();
     * const response = Request.create("https://example.com/api/upload")
     *   .addHeader("Content-Type", "application/json")
     *   .post(body);
     * print(`status ${response.responseCode}`);
     * </pre>
     *
     * @param data the body, which is sent as it is
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.8.4
     */
    public Response post(byte[] data) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) this.conn.openConnection();
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
     * sends the request as a PUT with a text body and hands back what came of it.<br>
     * This is {@link #post(java.lang.String) post} with {@code PUT} as the method, so the body is
     * encoded as UTF-8 and the length is put on for you. There is no stream form of this, so a
     * body that is not text goes through {@link #send(java.lang.String, byte[]) send} with
     * {@code "PUT"} instead.
     *
     * @param data the body, which is encoded as UTF-8
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.8.4
     */
    public Response put(String data) throws IOException {
        byte[] b = data.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) this.conn.openConnection();
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
     * sends the request as a PUT with a byte array body and hands back what came of it.<br>
     * This is {@link #put(java.lang.String) put} without the encoding step, so the bytes go out
     * exactly as given, and the length is put on as the {@code Content-Length} just the same.
     *
     * @param data the body, which is sent as it is
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent or the answer could not be reached
     * @since 1.8.4
     */
    public Response put(byte[] data) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) this.conn.openConnection();
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
     * sends the request with the method given and no body, and hands back what came of it.<br>
     * This is the form for the verbs the other calls do not cover, such as {@code DELETE} or
     * {@code HEAD}. The method is passed to the connection as it is, so one the platform does not
     * know is refused there rather than being checked first.<br>
     * Nothing is written to the connection, so this cannot carry a body; for one that can, use
     * {@link #send(java.lang.String, java.lang.String) send} with the method and the data.
     * example:
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
        HttpURLConnection conn = (HttpURLConnection) this.conn.openConnection();
        for (Entry<String, String> e : headers.entrySet()) {
            conn.addRequestProperty(e.getKey(), e.getValue());
        }
        conn.setRequestMethod(method);

        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        return new Response(stream, conn.getResponseCode(), conn.getHeaderFields());
    }

    /**
     * sends the request with the method given and a text body, and hands back what came of it.<br>
     * This is the general form of the calls that send a body: the method is passed to the
     * connection as it is, the text is encoded as UTF-8, and the length is put on as the
     * {@code Content-Length}. It is what the {@code post} and {@code put} calls are, with the
     * method spelled out, and it is the one to reach for a verb that has no shortcut and does
     * want a body.
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
        HttpURLConnection conn = (HttpURLConnection) this.conn.openConnection();
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
     * sends the request with the method given and a byte array body, and hands back what came of
     * it.<br>
     * This is {@link #send(java.lang.String, java.lang.String) send} without the encoding step, so
     * the bytes go out exactly as given, which is what makes it the form to use for a body that is
     * not text. The length is put on as the {@code Content-Length} just the same.
     *
     * @param method the HTTP method to use, which must be one the platform knows
     * @param data   the body, which is sent as it is
     * @return the response, with its body still unread
     * @throws IOException if the request could not be sent, if the platform does not know the
     *                     method, or if the answer could not be reached
     * @since 1.8.4
     */
    public Response send(String method, byte[] data) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) this.conn.openConnection();
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
     * what came back: the status, the headers, and a body that has not been read yet.<br>
     * A script gets one of these from any of the sending calls on {@link HTTPRequest}, and never
     * makes one, so the three fields are all there is to look at. A status of four hundred or more
     * is not a special case: the body has already been taken from the connection's error stream
     * in that case, so {@link #text()} gives the server's error page and
     * {@link #responseCode} is the thing to branch on.<br>
     * The body is read out of the stream the first time {@link #text()} or
     * {@link #byteArray()} asks for it, and only once, so asking a second time gives the same
     * answer rather than an empty one. The two forms do not read independently, though: whichever
     * comes first reads the stream to its end, so a {@link #text()} followed by a
     * {@link #byteArray()} gives an empty array rather than the body.
     * example:
     * <pre>
     * const response = Request.get("https://example.com/api/status");
     * if (response.responseCode === 200) {
     *   print(response.text());
     * } else {
     *   print(`status ${response.responseCode} said: ${response.text()}`);
     * }
     *
     * // the response headers are a java Map of lists, since a name can come
     * // back more than once
     * const type = response.headers === null ? null : response.headers.get("Content-Type");
     * print(`the server called it ${type}`);
     * </pre>
     * @author Wagyourtail
     * @since 1.1.8
     */
    public static class Response {
        private final InputStream raw;
        @Nullable
        private String text;
        /**
         * the headers the server sent, as a plain Java {@link Map} from a name to every value
         * that came back under it, which is why a name is a list rather than a single string. A
         * name is spelled as the platform spelled it, so it can be in a different case than a
         * header a script sent.<br>
         * This is {@code null} when the response was made without any, which is the case for a
         * {@code Response} built by hand rather than by one of the sending calls, so it has to be
         * checked before use.
         */
        @Nullable
        public Map<String, List<String>> headers;
        /**
         * the status the server answered with, such as 200 or 404. This is an ordinary number
         * rather than anything wrapped, so it is compared directly.
         */
        public int responseCode;

        /**
         * wraps a connection's stream, status and headers. A script does not call this; it is what
         * every sending call on {@link HTTPRequest} builds once the answer has arrived, and the
         * stream is the connection's, so the body is only there to be read once.
         *
         * @param inputStream  the body stream, taken from the error stream when the status was
         *                     four hundred or more and from the input stream otherwise
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
         * the body as text, decoded as UTF-8 whatever charset the server named in its headers.<br>
         * The stream is read to its end the first time this is called and the result is kept, so
         * calling it again gives the same string rather than an empty one. Line endings are joined
         * back with a single {@code \n}, so a body that arrived with other endings comes back
         * with these.<br>
         * A body that is not text is still handed over, with the bytes decoded, so
         * {@link #byteArray()} is the form to use for anything binary. Whichever of the two is
         * called first is the one that does the reading, and the other then finds nothing left.
         * example:
         * <pre>
         * const response = Request.get("https://example.com/api/status");
         * // asking twice is fine, the second one is the same string
         * print(response.text().length);
         * print(response.text().length);
         * </pre>
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
         * Don't use this. Parse {@link HTTPRequest.Response#text()} in the guest language
         *
         * @return
         * @since 1.1.8
         * @deprecated
         */
        @Deprecated
        public Object json() {
            text();
            return null;
        }

        /**
         * the body as raw bytes, with no decoding and no charset involved. This is the form to use
         * for an image, an archive or anything else that is not text, and for a body a script wants
         * to hand straight on to a file.<br>
         * The stream is read to its end, so if {@link #text()} was called first there is nothing
         * left and this gives an empty array rather than failing. The two are not interchangeable
         * after the fact; pick one before reading.
         * example:
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
