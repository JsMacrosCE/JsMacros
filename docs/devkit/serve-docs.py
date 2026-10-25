#!/usr/bin/env python3
"""Serve the bundled VitePress output locally, including its clean URLs."""

import argparse
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class DocsHandler(SimpleHTTPRequestHandler):
    def translate_path(self, path):
        resolved = super().translate_path(path)
        if not Path(resolved).exists() and Path(resolved + ".html").is_file():
            return resolved + ".html"
        return resolved


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()
    site = Path(__file__).resolve().parent / "vitepress"
    if not (site / "index.html").is_file():
        parser.error("vitepress/index.html is missing; extract the complete devkit first")
    handler = partial(DocsHandler, directory=str(site))
    with ThreadingHTTPServer(("127.0.0.1", args.port), handler) as server:
        print(f"JsMacrosCE docs: http://127.0.0.1:{server.server_port}/ (Ctrl+C to stop)")
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


if __name__ == "__main__":
    main()
