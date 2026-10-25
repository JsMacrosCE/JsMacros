"""Compare every curated export and example with all four real generated formats.

Run after generation: python3 buildSrc/src/test/python/test_script_type_artifacts.py 1.21.8 26.1.2
"""

import ast
from html.parser import HTMLParser
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[4]
JAVA = ROOT / "common/src/main/java"
CATALOGS = JAVA / "com/jsmacrosce/jsmacros/client/api/classes/types"


class Text(HTMLParser):
    def __init__(self, content):
        super().__init__()
        self.parts = []
        self.feed(content)

    def handle_data(self, data):
        self.parts.append(data)

    def normalized(self):
        return normalized(" ".join(self.parts))


def normalized(value):
    return " ".join(value.split())


def target_name(source, target):
    # Resolve the catalog's imports, rather than guessing internal packages from exported names.
    outer, *nested = target.split(".")
    candidates = []
    for package in re.findall(r"import ([\w.]+)(?:\.\*)?;", source):
        candidate = package if package.endswith("." + outer) else package + "." + outer
        if (JAVA / (candidate.replace(".", "/") + ".java")).exists():
            candidates.append(candidate)
    assert len(candidates) == 1, (target, candidates)
    return candidates[0] + ("." + ".".join(nested) if nested else "")


def check(version):
    generated = ROOT / "build/docs/targets" / version
    python = generated / "python/JsMacrosAC"
    typescript = next((generated / "typescript/headers").glob("JsMacros-*.d.ts")).read_text()
    markdown = next((generated / "vitepress/content").glob("*/" + version))
    html = next((generated / "web").iterdir())
    exports_checked = 0
    for catalog in sorted(CATALOGS.glob("*.java")):
        source = catalog.read_text()
        namespace = re.search(r'@ScriptNamespace\("(\w+)"\)', source)[1]
        aliases = re.findall(r"public static final Class<([\w.]+)> (\w+) = [\w.]+\.class;", source)
        assert aliases, namespace
        py = (python / (namespace + ".py")).read_text()
        tree = ast.parse(py)
        md = (markdown / "types" / (namespace + ".md")).read_text()
        web = Text((html / "com/jsmacrosce/jsmacros/client/api/classes/types" / (namespace + ".html")).read_text()).normalized()
        ts = typescript.split("declare namespace " + namespace + " {\n", 1)[1].split("\n}\n", 1)[0]
        assert len(re.findall(r"^    const \w+:", ts, re.M)) == len(aliases), namespace
        initializer = (python / "__init__.py").read_text()
        assert f"from . import {namespace} as {namespace}" in initializer
        # Parsing stubs is insufficient: silently damaged prose still forms a valid docstring.
        example = re.search(r"<pre>\n(.*?)\n \* </pre>", source, re.S)[1]
        example = re.sub(r"^ \* ?", "", example, flags=re.M)
        ts_prose = re.sub(r"^\s*\* ?", "", typescript, flags=re.M)
        for output in (ast.get_docstring(tree), md, web, ts_prose):
            assert normalized(example) in normalized(output), (version, namespace, "example damaged")
        for target, alias in aliases:
            qualified = target_name(source, target)
            display = target.replace(".", "_")
            assert f"from .{display} import {display} as {alias}" in py, (version, namespace, alias)
            assert re.search(r"^    const " + alias + r":", ts, re.M), (namespace, alias)
            assert re.search(r"^    type " + alias + r"(?:[< =])", ts, re.M), (namespace, alias)
            assert f"`{namespace}.{alias}`" in md
            assert f"{namespace}.{alias}" in web
            target_path = qualified.replace(".", "/")
            if "." in target:
                # Reference filenames retain dotted nested names; only package separators are '/'.
                outer, *nested = target.split(".")
                outer_qualified = qualified[: -(len(".".join(nested)) + 1)]
                target_path = outer_qualified.replace(".", "/") + "." + ".".join(nested)
            target_md = list(markdown.glob("*/" + target_path + ".md"))
            assert len(target_md) == 1, (namespace, alias, target_path)
            assert f"Available in scripts as `{namespace}.{alias}`." in target_md[0].read_text()
            assert f"Available in scripts as {namespace}.{alias}." in Text((html / (target_path + ".html")).read_text()).normalized()
            exports_checked += 1
    horse = ast.parse((python / "HorseEntityHelper.py").read_text())
    horse_docs = ast.get_docstring(next(n for n in horse.body if isinstance(n, ast.ClassDef)))
    assert "if (!(entity instanceof EntityHelpers.Horse)) continue;" in horse_docs
    assert "const packed = entity.getVariant();" in horse_docs
    assert "EntityHelpers.Horse" in (python / "EntityHelper.py").read_text()
    assert "const Enderman:" in typescript and "const EnderMan:" not in typescript
    print(f"{version}: all four formats match {exports_checked} exports and all namespace examples")


if __name__ == "__main__":
    for minecraft in sys.argv[1:] or ["26.1.2"]:
        check(minecraft)
