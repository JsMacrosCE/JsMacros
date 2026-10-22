package com.jsmacrosce.jsmacros.core.library.impl;

import org.jetbrains.annotations.Nullable;
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
 * Functions for getting and using raw java classes, methods and functions.
 * <p>
 * An instance of this class is passed to scripts as the {@code Request} variable.
 * <br>
 * The one-call forms are the quick ones: {@link #get(String) get} and
 * {@link #post(String, String) post} send a request and hand back the response in a single step.
 * The {@link #create(String) create} form is the one to reach for when a request needs more than
 * that, such as headers, a body on a method other than POST, or a timeout, and it hands back a
 * builder that every call on returns so the steps chain. {@link #createWS(String) createWS} is the
 * websocket side of the same library.
 * <br>
 * Two things to know before using any of it. The calls are blocking: the script waits for the
 * server rather than for a callback, so a slow or unreachable host stops the script where it
 * stands, which in a listener means the event it is handling waits too. And a response body is
 * read once and kept, so reading it twice gives the same text rather than an empty second read.
 * example:
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

    public FRequest(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * create a HTTPRequest handler to the specified URL
     *
     * @param url
     * @return Request Wrapper
     * @throws IOException
     * @see HTTPRequest
     * @since 1.1.8
     */
    public HTTPRequest create(String url) throws IOException {
        return new HTTPRequest(url);
    }

    /**
     * @param url
     * @return
     * @throws IOException
     * @see FRequest#get(String, Map)
     * @since 1.1.8
     */
    public Response get(String url) throws IOException {
        return get(url, null);
    }

    /**
     * send a GET request to the specified URL.
     * <br>
     * The headers are a Java {@link Map}, not a script object, so they have to be built through
     * {@code JavaUtils.createHashMap()} rather than written as an object literal. The map is
     * copied, so changing it afterwards does not change the request. Passing {@code null} for it
     * is the same as using the no header form.<br>
     * The call blocks until the server answers or the connection fails, and a failed connection
     * is an {@link IOException} rather than a response with a bad status. A response that came
     * back with an error status is not a failure here, it is a response with a
     * {@code responseCode} to check.
     * example:
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
     * @return Response Data
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
     * @param url
     * @param data
     * @return
     * @throws IOException
     * @see FRequest#post(String, String, Map)
     * @since 1.1.8
     */
    public Response post(String url, String data) throws IOException {
        return post(url, data, null);
    }

    /**
     * send a POST request to the specified URL.
     * <br>
     * The headers are a Java {@link Map} rather than a script object, the same as on the GET form,
     * and the data is sent as the request body with no content type set for it, so an endpoint
     * that needs one wants the builder form and its {@code addHeader}. The call blocks until the
     * server answers, and a connection failure is an {@link IOException}.
     * example:
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
     * @return Response Data
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
     * Create a Websocket handler.
     * <br>
     * The handler is not connected by this: creating it only opens the address, and
     * {@code connect()} is what starts it. The handlers are public fields rather than methods, so
     * they are installed by assignment and any of them can be left out; the ones available are
     * {@code onConnect}, {@code onTextMessage}, {@code onFrame}, {@code onDisconnect} and
     * {@code onError}, each taking a
     * {@link java.util.function.BiConsumer BiConsumer} of the socket and its own argument. Since
     * they are read at the moment the thing happens, a handler assigned after {@code connect()} can
     * still catch the first message, and one assigned inside a handler does not affect that same
     * event.<br>
     * The socket runs on its own thread, so a handler runs there rather than on the script's
     * thread, which is the usual caveat for anything that touches the game from a callback.
     * example:
     * <pre>
     * // creating it does not connect it
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
     * @return a handler with its callbacks as public fields, not yet connected
     * @throws IOException if the address is not a usable websocket URL
     * @see Websocket
     * @since 1.2.7
     */
    public Websocket createWS(String url) throws IOException {
        return new Websocket(url);
    }

    /**
     * Create a Websocket handler.
     *
     * @param url
     * @return
     * @throws IOException
     * @since 1.1.9
     * @deprecated 1.2.7
     */
    @Deprecated
    public Websocket createWS2(String url) throws IOException {
        return new Websocket(url);
    }

}
