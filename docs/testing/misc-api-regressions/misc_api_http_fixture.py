#!/usr/bin/env python3
"""Loopback-only HTTP/WebSocket fixture for the miscellaneous API regressions."""

import argparse
import base64
import hashlib
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def handle_request(self):
        if self.path.startswith("/ws/"):
            self.websocket()
            return
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length)
        if self.path == "/slow":
            time.sleep(1)
        elif self.path != "/ok":
            self.send_error(404)
            return
        result = b"misc-api-fixture " + self.command.encode() + b" " + body
        try:
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(result)))
            self.send_header("Connection", "close")
            self.end_headers()
            self.wfile.write(result)
        except (BrokenPipeError, ConnectionResetError):
            pass  # Expected when the timeout test closes its connection.
        self.close_connection = True

    def websocket(self):
        key = self.headers.get("Sec-WebSocket-Key")
        mode = self.path.removeprefix("/ws/")
        if not key or mode not in {"text", "frame", "disconnect", "error"}:
            self.send_error(400)
            return
        accept = base64.b64encode(hashlib.sha1(
            (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encode()
        ).digest()).decode()
        self.send_response(101)
        self.send_header("Upgrade", "websocket")
        self.send_header("Connection", "Upgrade")
        self.send_header("Sec-WebSocket-Accept", accept)
        self.end_headers()
        self.wfile.flush()
        time.sleep(0.2)
        try:
            if mode == "text":
                payload = b"misc-api-fixture"
                self.connection.sendall(bytes([0x81, len(payload)]) + payload)
            elif mode == "frame":
                self.connection.sendall(b"\x89\x00")  # Empty ping, delivered to onFrame.
            elif mode == "error":
                self.connection.sendall(b"\x83\x00")  # Reserved opcode: protocol error.
            if mode in {"text", "frame"}:
                time.sleep(0.2)
            if mode != "error":
                self.connection.sendall(b"\x88\x02\x03\xe8")  # Normal close (1000).
            self.connection.settimeout(2)
            self.connection.recv(4096)  # Best-effort client acknowledgement.
        except (OSError, TimeoutError):
            pass
        self.close_connection = True

    do_GET = handle_request
    do_POST = handle_request
    do_PUT = handle_request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=18729)
    args = parser.parse_args()
    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    print(f"Fixture listening on http://127.0.0.1:{args.port}; Ctrl-C to stop", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
