package com.jsmacrosce.jsmacros.core.library.impl.classes;

import com.neovisionaries.ws.client.*;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * one websocket connection, its handlers, and the calls that drive it.<br>
 * A script gets one from {@code Request.createWS}, or from {@code Request.createWS2}, which is
 * deprecated since 1.2.7 and does the same thing down to its body. Making it opens
 * the address and nothing else: {@link #connect() connect} is what starts it, and a socket that
 * was made and never connected has not spoken to the server at all. The handlers are public
 * fields rather than methods, so they are installed by assignment and any of them can be left out.
 * Each is read at the moment the thing happens, so one assigned later can still catch a message
 * that has not arrived yet, and each of the five is {@code null} until it is assigned.
 * {@code onConnect} is the exception that timing does not help with: it only fires from inside
 * {@link #connect() connect}, so it has to be assigned before that call or it never runs.<br>
 * They are all two argument wrappers, {@code (socket, argument)}, made with
 * {@code JavaWrapper.methodToJava}, and they run on the websocket's own thread rather than on the
 * script's, which is the usual caveat for anything a callback does to the game. A handler that
 * throws is meant to be reported to the script's log rather than to break the connection, but
 * that reporting reads the {@code onConnect} handler to find the context, so a socket that throws
 * from one of the other four and never had {@code onConnect} assigned loses the original failure
 * and reports a null one instead. The one path with no handling at all is the connect error
 * path: it calls {@code onError} with no null check and no {@code try}, so a socket with no
 * {@code onError} assigned has a failure thrown at it by the client library, and a throwing
 * {@code onError} takes the library's own thread down with it. Nothing reaches it through this
 * class, since {@link #connect() connect} is the only thing here that connects and a handshake it
 * refuses is thrown out of there instead, but {@link #getWs() getWs} hands the client library's
 * own socket over, and connecting that goes through this path.<br>
 * Everything on this class that returns something returns the same socket, so the calls chain.
 * example:
 * <pre>
 * // a socket is not connected by being made, so connect is a separate step
 * const socket = Request.createWS("wss://example.com/socket");
 *
 * // any of the five can be left out, and each is read when the thing happens
 * socket.onConnect = JavaWrapper.methodToJava(function (ws, headers) {
 *   print("connected");
 *   ws.sendText("hello from the script");
 * });
 * socket.onTextMessage = JavaWrapper.methodToJava(function (ws, text) {
 *   print(`from the socket: ${text}`);
 * });
 * socket.onDisconnect = JavaWrapper.methodToJava(function (ws, info) {
 *   print(`gone, and it was the ${info.isServer ? "server" : "client"} side`);
 * });
 *
 * socket.connect();
 *
 * // sending goes through the socket rather than through the handlers
 * socket.sendText("second message");
 *
 * // close without a code, or with one
 * socket.close();
 * </pre>
 * @author Wagyourtail, R3alCl0ud
 */
@SuppressWarnings("unused")
public class Websocket {

    private final WebSocket ws;
    /**
     * calls your method as a {@link java.util.function.Consumer BiConsumer}&lt;{@link WebSocket}, {@link List}&lt;{@link String}&gt;&gt;
     * <br>
     * which is called once the opening handshake has succeeded, with the socket and the headers
     * the server answered the handshake with. The headers are a plain Java {@link Map} from a
     * name to every value under it, compared without regard to case, so the usual
     * {@code Sec-WebSocket-Accept} is there under that name.<br>
     * This is the first thing that runs once a socket is live, so it is the reliable moment to
     * send anything. Nothing is called if it is left {@code null}.
     */
    @Nullable
    public MethodWrapper<WebSocket, Map<String, List<String>>, Object, ?> onConnect;
    /**
     * calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link WebSocket}, {@link String}&gt;
     * <br>
     * which is called for each text message that arrives, with the socket and the text already
     * decoded. A frame that is not text does not come here; {@code onFrame} is the one that sees
     * every frame, and it runs first for a text one as well.
     */
    @Nullable
    public MethodWrapper<WebSocket, String, Object, ?> onTextMessage;
    /**
     * calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link WebSocket}, {@link Disconnected}&gt;
     * <br>
     * which is called once the closing handshake is over, with the socket and a
     * {@link Disconnected} saying which side started it and carrying the close frames. Both
     * frames can be {@code null}, since either side may have gone without sending one, so read
     * them as optional.
     */
    @Nullable
    public MethodWrapper<WebSocket, Disconnected, Object, ?> onDisconnect;
    /**
     * calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link WebSocket}, {@link WebSocketException}&gt;
     * <br>
     * which is called when the client library reports an error on a connection that is already
     * open, with the socket and the exception. A handshake the server refuses is not one of those:
     * that is thrown out of {@link #connect() connect} instead, so a socket that wants to know
     * about a bad address has to be wrapped in a try rather than watched for here.
     */
    @Nullable
    public MethodWrapper<WebSocket, WebSocketException, Object, ?> onError;
    /**
     * calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link WebSocket}, {@link WebSocketFrame}&gt;
     * <br>
     * which is called for every frame that arrives, before the call that goes with the frame's
     * type. A text message therefore reaches this and then {@code onTextMessage}, so a socket
     * that has both sees the same message twice, in that order. This is the one to use for
     * frames the other handlers do not cover, such as pings and binary data.
     */
    @Nullable
    public MethodWrapper<WebSocket, WebSocketFrame, Object, ?> onFrame;

    public Websocket(String address) throws IOException {
        ws = new WebSocketFactory().createSocket(address).addListener(new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket ws, Map<String, List<String>> headers) {
                if (onConnect != null) {
                    try {
                        onConnect.accept(ws, headers);
                    } catch (Throwable e) {
                        BaseScriptContext<?> ctx = onConnect.getCtx();
                        if (ctx != null) {
                            ctx.runner.profile.logError(e);
                        } else {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onDisconnected(WebSocket ws, WebSocketFrame serverFrame, WebSocketFrame clientFrame, boolean isServer) {
                if (onDisconnect != null) {
                    try {
                        onDisconnect.accept(ws, new Disconnected(serverFrame, clientFrame, isServer));
                    } catch (Throwable e) {
                        BaseScriptContext<?> ctx = onConnect.getCtx();
                        if (ctx != null) {
                            ctx.runner.profile.logError(e);
                        } else {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onConnectError(WebSocket websocket, WebSocketException exception) {
                onError(websocket, exception);
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException ex) {
                if (onError != null) {
                    try {
                        onError.accept(websocket, ex);
                    } catch (Throwable e) {
                        BaseScriptContext<?> ctx = onConnect.getCtx();
                        if (ctx != null) {
                            ctx.runner.profile.logError(e);
                        } else {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFrame(WebSocket ws, WebSocketFrame frame) {
                if (onFrame != null) {
                    try {
                        onFrame.accept(ws, frame);
                    } catch (Throwable e) {
                        BaseScriptContext<?> ctx = onConnect.getCtx();
                        if (ctx != null) {
                            ctx.runner.profile.logError(e);
                        } else {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onTextMessage(WebSocket ws, String text) {
                if (onTextMessage != null) {
                    try {
                        onTextMessage.accept(ws, text);
                    } catch (Throwable e) {
                        BaseScriptContext<?> ctx = onConnect.getCtx();
                        if (ctx != null) {
                            ctx.runner.profile.logError(e);
                        } else {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public Websocket(URL address) throws URISyntaxException, IOException {
        this(address.toURI().toString());
    }

    /**
     * opens the connection, which is what actually reaches the server.<br>
     * Making a socket and connecting it are two steps, and this is the second. The handshake runs
     * on the calling thread, and a socket that got that far comes back with {@code onConnect}
     * having run.<br>
     * A handshake the server refuses, and a host that cannot be reached at all, both fail here as
     * a {@link WebSocketException} rather than as an error handler call, so a socket that wants
     * to survive a bad address has to be wrapped in a try. A socket can only be connected once:
     * a second call is refused, since the state is no longer the one a connect starts from, and
     * making a new socket is the way to try again.
     * example:
     * <pre>
     * const socket = Request.createWS("wss://example.com/socket");
     * socket.onConnect = JavaWrapper.methodToJava(function (ws, headers) {
     *   print("connected");
     * });
     * try {
     *   socket.connect();
     * } catch (error) {
     *   // a refused handshake or an unreachable host lands here
     *   print(`it would not connect: ${error}`);
     * }
     * </pre>
     *
     * @return self
     * @throws WebSocketException if the handshake was refused or the host could not be reached
     * @since 1.1.9
     */
    public Websocket connect() throws WebSocketException {
        ws.connect();
        return this;
    }

    /**
     * the underlying {@link WebSocket} this wraps, which is the client library's own type rather
     * than something JsMacros put its own shape around.<br>
     * This is the way to reach the parts of the client that are not on this class: sending a
     * ping, setting a receive timeout, or asking for the state the connection is in. It is the
     * same object throughout, so the state it is asked about is the live one.
     * example:
     * <pre>
     * const socket = Request.createWS("wss://example.com/socket").connect();
     * print(`the socket state is ${socket.getWs().getState()}`);
     * </pre>
     *
     * @return the underlying websocket, which is the same one on every call
     * @since 1.1.9
     */
    public WebSocket getWs() {
        return ws;
    }

    /**
     * sends a text frame to the server.<br>
     * This is the text form only: what comes back is given to {@code onTextMessage} rather than
     * {@code onFrame}. Sending before the socket is connected, or after it is closed, is not
     * checked here and is left to the client library, so a message sent too early goes nowhere
     * rather than being queued.
     * example:
     * <pre>
     * const socket = Request.createWS("wss://example.com/socket");
     * socket.onConnect = JavaWrapper.methodToJava(function (ws, headers) {
     *   // sending from the connect handler is the reliable moment, since the
     *   // socket is open by the time it runs
     *   ws.sendText("hello");
     * });
     * socket.connect();
     * </pre>
     *
     * @param text the message to send
     * @return self
     * @since 1.1.9
     */
    public Websocket sendText(String text) {
        ws.sendText(text);
        return this;
    }

    /**
     * starts the closing handshake, with no close code, so the client library's default one is
     * used.<br>
     * Closing is a handshake rather than a cut: the close frame goes out and the server's answer
     * comes back afterwards, and {@code onDisconnect} runs when it does. Nothing here waits for
     * that, so a script that closes and ends has not yet seen the socket go down.
     * example:
     * <pre>
     * const socket = Request.createWS("wss://example.com/socket").connect();
     * socket.onDisconnect = JavaWrapper.methodToJava(function (ws, info) {
     *   print("the socket is done");
     * });
     * socket.close();
     * </pre>
     *
     * @return self
     * @since 1.1.9
     */
    public Websocket close() {
        ws.sendClose();
        return this;
    }

    /**
     * starts the closing handshake with a close code, which is {@link #close() close} with the
     * code spelled out.<br>
     * The code goes out in the close frame and the server's answer comes back afterwards, with
     * {@code onDisconnect} running when it does. The code is passed straight to the client
     * library, so what is legal here is whatever that library allows rather than anything checked
     * in this class.
     * example:
     * <pre>
     * const socket = Request.createWS("wss://example.com/socket").connect();
     * // 1000 is the ordinary "going away as planned" code
     * socket.close(1000);
     * </pre>
     *
     * @param closeCode the code to put on the close frame
     * @return self
     * @since 1.1.9
     */
    public Websocket close(int closeCode) {
        ws.sendClose(closeCode);
        return this;
    }

    /**
     * how a socket went down, handed to {@code onDisconnect} once the closing handshake is
     * over.<br>
     * The three fields are the close handshake as it happened: which side started it, and the
     * close frame each side sent if it sent one. A side that dropped without a close frame is
     * {@code null} here rather than absent, so a field that matters is worth checking.
     * example:
     * <pre>
     * socket.onDisconnect = JavaWrapper.methodToJava(function (ws, info) {
     *   const who = info.isServer ? "the server" : "this client";
     *   const code = info.clientFrame === null ? "nothing" : info.clientFrame.getCloseCode();
     *   print(`${who} started the close, and the code we sent was ${code}`);
     * });
     * </pre>
     * @author Perry "R3alCl0ud" Berman
     */
    public static class Disconnected {
        /**
         * the close frame the server sent, or {@code null} if it went without sending one, which
         * is what a dropped connection looks like rather than a clean close.
         */
        public WebSocketFrame serverFrame;
        /**
         * the close frame this client sent, or {@code null} if none was sent. The code on it is
         * the one given to {@link Websocket#close(int) close(int)}, and its absence is what tells
         * a close that was requested from one that was not.
         */
        public WebSocketFrame clientFrame;
        /**
         * whether the closing handshake was started by the server. {@code true} means the server
         * sent the first close frame, {@code false} that this client did, so a socket that was
         * closed on purpose and one that was dropped are told apart by this rather than by either
         * frame being missing.
         */
        public boolean isServer;

        /**
         * records how the closing handshake ended. A script does not make one of these; it is
         * what {@code onDisconnect} is handed once the handshake is over.
         *
         * @param serverFrame the close frame the server sent, or {@code null}
         * @param clientFrame the close frame this client sent, or {@code null}
         * @param isServer    whether the server started the handshake
         */
        public Disconnected(WebSocketFrame serverFrame, WebSocketFrame clientFrame, boolean isServer) {
            this.serverFrame = serverFrame;
            this.clientFrame = clientFrame;
            this.isServer = isServer;
        }

    }

}
