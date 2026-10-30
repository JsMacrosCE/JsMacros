---
outline: deep
---
# HTTP and WebSockets

The `Request` library sends HTTP requests and opens WebSocket connections. It is
the usual bridge between a script and a web service: a price feed, a chat relay, a
control panel, or your own server.

HTTP calls block the script thread until the server answers or the connection
fails. A slow request on a worker thread keeps that script waiting without freezing
the game. A request from a joined listener does freeze the game, because the main
thread is waiting for the handler: release the event lock after the cancellation
decision, or move the request to an unjoined worker. The default request has no
connection or read timeout, so set one with `setConnectTimeout` and `setReadTimeout`
when the server may be slow.

## One-call requests

```js
const response = Request.get("https://example.com/api/status");
Chat.log(`status ${response.responseCode}: ${response.text()}`);
```

A response is returned even for HTTP error statuses such as 404 or 500.
`responseCode` is the server's answer; a connection failure is an exception
instead. Check the code when the difference matters.

```js
try {
  const response = Request.get("https://example.com/api/status");
  if (response.responseCode === 200) {
    Chat.log(response.text());
  } else {
    Chat.log(`the server said ${response.responseCode}`);
  }
} catch (error) {
  Chat.log(`request failed: ${error}`);
}
```

## Headers are Java maps

The quick header overload takes a Java `Map`, not a JavaScript object:

```js
const headers = JavaUtils.createHashMap();
headers.put("Accept", "application/json");
headers.put("User-Agent", "JsMacrosCE");

const response = Request.get("https://example.com/api/status", headers);
```

Writing `{ Accept: "application/json" }` does not work. Build the map through
`JavaUtils`.

## The configurable form

Use `Request.create(url)` when you need timeouts, a body, or a method other than
GET:

```js
const request = Request.create("https://example.com/api/echo")
  .addHeader("User-Agent", "JsMacrosCE")
  .setConnectTimeout(5000)
  .setReadTimeout(5000);

const response = request.post("hello");
Chat.log(`posted, status ${response.responseCode}`);
```

The request is not sent until a send method is called. Response text is cached
after the first read.

## WebSockets

A WebSocket is created unconnected. Install the handlers, then call `connect()`:

```js
const socket = Request.createWS("wss://echo.websocket.org");

socket.onConnect = JavaWrapper.methodToJava((ws, headers) => {
  Chat.log("connected");
  ws.sendText("hello from the script");
});

socket.onTextMessage = JavaWrapper.methodToJava((ws, text) => {
  Chat.log(`from the socket: ${text}`);
});

socket.onError = JavaWrapper.methodToJava((ws, error) => {
  Chat.log(`socket error: ${error}`);
});

socket.connect();
```

The handlers receive the underlying socket as their first argument. To close the
connection, call `socket.close()` on the `Websocket` you created, or
`ws.disconnect()` on the socket passed to a handler.

WebSocket callbacks keep the script context alive. Close the connection explicitly
when the script no longer needs it; do not assume the socket disappears on its own.

## Things to watch

- **Do not block the client thread.** A request belongs on a script thread.
- **Check the status code.** An HTTP error is a response, not an exception.
- **Close sockets.** A forgotten WebSocket keeps receiving and keeps the context
  alive.

## Related pages

- [Threading and the main thread](/guide/concepts/threading)
- API reference: [`Request`](/api/libraries/com/jsmacrosce/jsmacros/core/library/impl/FRequest),
  [`Websocket`](/api/classes/com/jsmacrosce/jsmacros/core/library/impl/classes/Websocket),
  [`JavaUtils`](/api/libraries/com/jsmacrosce/jsmacros/api/library/FJavaUtils)
