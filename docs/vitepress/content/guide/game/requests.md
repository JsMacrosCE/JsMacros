---
outline: deep
---
# HTTP and WebSockets

The `Request` library sends HTTP requests and opens WebSocket connections. It is
the usual bridge between a script and a web service: a price feed, a chat relay, a
control panel, or your own server.

HTTP calls block the script thread until the server answers or the connection
fails. That is fine on a worker thread; it is not something to do on the client
thread. A slow request does not freeze the game, but it does keep that script
waiting.

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

The handlers receive the underlying socket as their first argument. Call
`socket.close()` or `ws.close()` when the script is done with the connection.

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
