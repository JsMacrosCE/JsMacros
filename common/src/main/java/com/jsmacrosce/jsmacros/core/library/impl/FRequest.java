package com.jsmacrosce.jsmacros.core.library.impl;

import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.impl.classes.HTTPRequest;
import com.jsmacrosce.jsmacros.core.library.impl.classes.HTTPRequest.Response;
import com.jsmacrosce.jsmacros.core.library.impl.classes.Websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends HTTP requests and creates WebSocket handlers through the {@code Request} script library.
 * <p>
 * An instance of this class is passed to scripts as the {@code Request} variable.
 * The {@link #get(String)} and {@link #post(String, String)} methods send one-call requests;
 * {@link #create(String)} returns a configurable HTTP request handler, and
 * {@link #createWS(String)} creates a WebSocket handler that connects only when asked.
 * HTTP calls block until a response or connection failure; HTTP error status codes are responses,
 * not connection failures. Response text is cached after its first read.
 * <pre>
 * // the quick form: one call, one response
 * const response = Request.get("https://example.com/api/status");
 * print(`status ${response.responseCode}: ${response.text()}`);
 *
 * // the builder form is for anything the quick form does not cover
 * const request = Request.create("https://example.com/api/echo")
 *   .addHeader("User-Agent", "JsMacrosCE")
 *   .setConnectTimeout(5000)
 *   .setReadTimeout(5000);
 * print(`posted, status ${request.post("hello").responseCode}`);
 *
 * // the quick forms take headers as a java Map rather than as a script object,
 * // so the map has to be built through JavaUtils
 * const headers = JavaUtils.createHashMap();
 * headers.put("Accept", "application/json");
 * print(`with headers, status ${Request.post("https://example.com/api/echo", "hello", headers).responseCode}`);
 *
 * // a websocket installs its handlers by writing to public fields, then connects
 * const socket = Request.createWS("wss://example.com/socket");
 * socket.onTextMessage = JavaWrapper.methodToJava(function (ws, text) {
 *   print(`from the socket: ${text}`);
 * });
 * socket.connect();
 * </pre>
 * @author Wagyourtail
 * @since 1.1.8
 */
@Library("Request")
public class FRequest extends BaseLibrary {

    /**
     * Associates this library with its core; this constructor does not send a request or connect a WebSocket.
     *
     * @param runner the core used by this library
     */
    public FRequest(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Creates a configurable HTTP request handler for the URL. The request is not sent until a send method is called.
     *
     * @param url the request URL
     * @return a handler for configuring and sending requests to the URL
     * @throws IOException if the URL is malformed
     * @since 1.1.8
     */
    public HTTPRequest create(String url) throws IOException {
        return new HTTPRequest(url);
    }

    /**
     * Sends a GET request without custom headers. The call blocks until a response or connection failure.
     *
     * @param url the request URL
     * @return the response
     * @throws IOException if the connection fails
     * @since 1.1.8
     */
    public Response get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * Sends a GET request to the URL.
     * <br>
     * The headers are a Java {@link Map}, not a script object, so they have to be built through
     * {@code JavaUtils.createHashMap()} rather than written as an object literal. The map is
     * copied, so changing it afterwards does not change the request. Passing {@code null} for it
     * is the same as using the no header form.<br>
     * The call blocks until the server answers or the connection fails, and a failed connection
     * is an {@link IOException} rather than a response with a bad status. A response that came
     * back with an error status is not a failure here, it is a response with a
     * {@code responseCode} to check.
     * <pre>
     * // headers are a java Map, so they are built through JavaUtils rather than
     * // written as a script object
     * const headers = JavaUtils.createHashMap();
     * headers.put("Accept", "application/json");
     * headers.put("User-Agent", "JsMacrosCE");
     *
     * const response = Request.get("https://example.com/api/status", headers);
     * if (response.responseCode === 200) {
     *   print(response.text());
     * } else {
     *   print(`the server said ${response.responseCode}`);
     * }
     * // an unreachable host is an exception, not a bad status
     * try {
     *   Request.get("https://not-a-real-host.invalid/");
     * } catch (error) {
     *   print(`that one did not get that far: ${error}`);
     * }
     * </pre>
     *
     * @param url     the URL to send the request to
     * @param headers the request headers as a java Map, or {@code null} for none
     * @return the response, including its status code
     * @throws IOException if the connection fails or the host does not answer
     * @see HTTPRequest.Response
     * @since 1.1.8
     */
    public Response get(String url, @Nullable Map<String, String> headers) throws IOException {
        HTTPRequest req = new HTTPRequest(url);
        if (headers != null) {
            req.headers = new HashMap<>(headers);
        }
        return req.get();
    }

    /**
     * Sends a POST request without custom headers. The call blocks until a response or connection failure.
     *
     * @param url the request URL
     * @param data the request body
     * @return the response
     * @throws IOException if the connection fails
     * @since 1.1.8
     */
    public Response post(String url, String data) throws IOException {
        return post(url, data, null);
    }

    /**
     * Sends a POST request to the URL.
     * <br>
     * The headers are a Java {@link Map} rather than a script object, the same as on the GET form,
     * and the data is sent as the request body with no content type set for it, so an endpoint
     * that needs one wants the builder form and its {@code addHeader}. The call blocks until the
     * server answers, and a connection failure is an {@link IOException}.
     * <pre>
     * // the body is the second argument, and the headers are a java Map
     * const headers = JavaUtils.createHashMap();
     * headers.put("Content-Type", "text/plain");
     * const response = Request.post("https://example.com/api/echo", "hello", headers);
     * print(`status ${response.responseCode}, echoed back as ${response.text()}`);
     * </pre>
     *
     * @param url     the URL to send the request to
     * @param data    the request body
     * @param headers the request headers as a java Map, or {@code null} for none
     * @return the response, including its status code
     * @throws IOException if the connection fails or the host does not answer
     * @since 1.1.8
     */
    public Response post(String url, String data, @Nullable Map<String, String> headers) throws IOException {
        HTTPRequest req = new HTTPRequest(url);
        if (headers != null) {
            req.headers = new HashMap<>(headers);
        }
        return req.post(data);
    }

    /**
     * Creates a WebSocket handler without connecting it. Callbacks are read when their events
     * occur, so assign them before the events they should handle. The client invokes each
     * callback wrapper for its event; script execution and whether the invocation waits depend
     * on the wrapper's scheduling mode. Calling {@link Websocket#connect()} performs the
     * handshake on that call's thread.
     * <pre>
     * const socket = Request.createWS("wss://example.com/socket");
     *
     * // the handlers are public fields, so they are installed by assignment
     * socket.onTextMessage = JavaWrapper.methodToJava(function (ws, text) {
     *   print(`received: ${text}`);
     *   ws.sendText(`echo: ${text}`);
     * });
     * socket.onError = JavaWrapper.methodToJava(function (ws, error) {
     *   print(`the socket failed: ${error}`);
     * });
     *
     * // and connecting is a separate step
     * socket.connect();
     * // later
     * socket.close();
     * </pre>
     *
     * @param url the websocket address to open
     * @return a handler that has not yet connected
     * @throws IOException if the address cannot be parsed as a URL
     * @see Websocket
     * @since 1.2.7
     */
    public Websocket createWS(String url) throws IOException {
        return new Websocket(url);
    }

    /**
     * Creates a WebSocket handler without connecting it. Use {@link #createWS(String)} instead.
     *
     * @param url the websocket address to open
     * @return a handler that is not yet connected
     * @throws IOException if the address cannot be parsed as a URL
     * @since 1.1.9
     * @deprecated 1.2.7
     */
    @DocletIgnore
    @Deprecated
    public Websocket createWS2(String url) throws IOException {
        return new Websocket(url);
    }

}
