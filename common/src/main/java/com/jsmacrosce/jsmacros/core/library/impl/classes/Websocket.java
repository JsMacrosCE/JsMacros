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
 * Wraps a websocket connection and exposes its lifecycle callbacks and operations.
 * Create it with {@code Request.createWS}; construction does not connect, so call
 * {@link #connect()} to start the connection. Assign callbacks with
 * {@code JavaWrapper.methodToJava}; each receives the underlying socket and event data.
 * <pre>
 * const socket = Request.createWS("wss://example.com/socket");
 * socket.onConnect = JavaWrapper.methodToJava(function (ws, headers) {
 *   print("connected");
 *   ws.sendText("hello from the script");
 * });
 * socket.onTextMessage = JavaWrapper.methodToJava(function (ws, text) {
 *   print(`from the socket: ${text}`);
 * });
 * socket.connect();
 * </pre>
 * @author Wagyourtail, R3alCl0ud
 */
@SuppressWarnings("unused")
public class Websocket {

    private final WebSocket ws;
    /**
     * Called after connection with the underlying socket and the server's handshake headers.
     */
    @Nullable
    public MethodWrapper<WebSocket, Map<String, List<String>>, Object, ?> onConnect;
    /**
     * Called with the underlying socket and the decoded text for each text message.
     */
    @Nullable
    public MethodWrapper<WebSocket, String, Object, ?> onTextMessage;
    /**
     * Called when the connection disconnects, with the underlying socket and disconnect details.
     */
    @Nullable
    public MethodWrapper<WebSocket, Disconnected, Object, ?> onDisconnect;
    /**
     * Called when the websocket client reports an error, with the socket and exception.
     */
    @Nullable
    public MethodWrapper<WebSocket, WebSocketException, Object, ?> onError;
    /**
     * Receives the underlying socket and each received frame. The library emits this
     * notification before any matching message notification; script execution order depends
     * on the callback wrappers. A frame is not necessarily a complete message.
     */
    @Nullable
    public MethodWrapper<WebSocket, WebSocketFrame, Object, ?> onFrame;

    /**
     * Sets up the underlying socket and installs the lifecycle callback listener without connecting.
     * Call {@link #connect()} to start the connection.
     *
     * @param address websocket server address
     */
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
                        BaseScriptContext<?> ctx = onDisconnect.getCtx();
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
                        BaseScriptContext<?> ctx = onError.getCtx();
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
                        BaseScriptContext<?> ctx = onFrame.getCtx();
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
                        BaseScriptContext<?> ctx = onTextMessage.getCtx();
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

    /**
     * Sets up the underlying socket from a URL without connecting, converting it to a URI string
     * and delegating to {@link #Websocket(String)}.
     *
     * @param address URL of the websocket server
     */
    public Websocket(URL address) throws URISyntaxException, IOException {
        this(address.toURI().toString());
    }

    /**
     * Connects to the server. The client library performs the connection attempt.
     *
     * @return self for chaining
     * @throws WebSocketException if the handshake was refused or the host could not be reached
     * @since 1.1.9
     */
    public Websocket connect() throws WebSocketException {
        ws.connect();
        return this;
    }

    /**
     * Returns the underlying websocket client object.
     *
     * @return the underlying websocket
     * @since 1.1.9
     */
    public WebSocket getWs() {
        return ws;
    }

    /**
     * Sends a text message through the underlying websocket client.
     *
     * @param text the message to send
     * @return self for chaining
     * @since 1.1.9
     */
    public Websocket sendText(String text) {
        ws.sendText(text);
        return this;
    }

    /**
     * Starts the closing handshake using the client library's default close code.
     *
     * @return self for chaining
     * @since 1.1.9
     */
    public Websocket close() {
        ws.sendClose();
        return this;
    }

    /**
     * Starts the closing handshake with the given close code; the code is passed to the client
     * library without validation by this class.
     *
     * @param closeCode the code to put on the close frame
     * @return self for chaining
     * @since 1.1.9
     */
    public Websocket close(int closeCode) {
        ws.sendClose(closeCode);
        return this;
    }

    /**
     * Details supplied to {@code onDisconnect} by the client library.
     * @author Perry "R3alCl0ud" Berman
     */
    public static class Disconnected {
        /**
         * The server close frame supplied by the client library, or {@code null}.
         */
        @Nullable
        public WebSocketFrame serverFrame;
        /**
         * The client close frame supplied by the client library, or {@code null}.
         */
        @Nullable
        public WebSocketFrame clientFrame;
        /**
         * Whether the server started the closing handshake, as reported by the client library.
         */
        public boolean isServer;

        /**
         * Holds the disconnection details passed to {@code onDisconnect}.
         *
         * @param serverFrame the close frame the server sent, or {@code null}
         * @param clientFrame the close frame this client sent, or {@code null}
         * @param isServer    whether the server started the handshake
         */
        public Disconnected(@Nullable WebSocketFrame serverFrame, @Nullable WebSocketFrame clientFrame, boolean isServer) {
            this.serverFrame = serverFrame;
            this.clientFrame = clientFrame;
            this.isServer = isServer;
        }

    }

}
