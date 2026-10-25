import http.client
import importlib.util
from pathlib import Path
import sys
import tempfile
import threading
import unittest
from functools import partial
from http.server import ThreadingHTTPServer

sys.dont_write_bytecode = True
repository = Path(__file__).resolve().parents[4]
spec = importlib.util.spec_from_file_location("devkit_server", repository / "docs/devkit/serve-docs.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class QuietHandler(module.DocsHandler):
    def log_message(self, *args):
        pass


class DevkitServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        temporary_root = repository / "build/tmp"
        temporary_root.mkdir(parents=True, exist_ok=True)
        cls.temporary = tempfile.TemporaryDirectory(prefix="devkit-server-", dir=temporary_root)
        cls.site = Path(cls.temporary.name) / "site"
        cls.site.mkdir()
        (cls.site / "index.html").write_text("home")
        (cls.site / "2.0.0").mkdir()
        (cls.site / "2.0.0/reference.html").write_text("reference")
        (cls.site / "com.example.Foo.html").write_text("dotted class name")
        (cls.site / "app.js").write_text("site asset")
        (Path(cls.temporary.name) / "outside.html").write_text("must not be served")
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), partial(QuietHandler, directory=str(cls.site)))
        cls.thread = threading.Thread(target=cls.server.serve_forever, kwargs={"poll_interval": 0.01})
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.thread.join()
        cls.server.server_close()
        cls.temporary.cleanup()

    def request(self, path):
        connection = http.client.HTTPConnection("127.0.0.1", self.server.server_port)
        try:
            connection.request("GET", path)
            response = connection.getresponse()
            return response.status, response.read().decode()
        finally:
            connection.close()

    def test_home(self):
        self.assertEqual(self.request("/"), (200, "home"))

    def test_clean_url(self):
        self.assertEqual(self.request("/2.0.0/reference"), (200, "reference"))

    def test_clean_url_with_query(self):
        self.assertEqual(self.request("/2.0.0/reference?q=hello"), (200, "reference"))

    def test_dotted_class_name(self):
        self.assertEqual(self.request("/com.example.Foo"), (200, "dotted class name"))

    def test_assets(self):
        self.assertEqual(self.request("/app.js"), (200, "site asset"))

    def test_missing_page(self):
        self.assertEqual(self.request("/missing")[0], 404)

    def test_cannot_serve_parent(self):
        self.assertEqual(self.request("/../outside")[0], 404)


if __name__ == "__main__":
    unittest.main()
