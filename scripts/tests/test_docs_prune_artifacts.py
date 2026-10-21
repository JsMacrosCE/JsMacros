import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).parents[1] / "docs_prune_artifacts.py"
spec = importlib.util.spec_from_file_location("artifacts", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ArtifactTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.source = self.root / "src/api/Thing.java"
        self.source.parent.mkdir(parents=True)
        self.source.write_text('''package api;
/** A useful class contract.
 * <pre>Thing.go()</pre>
 */ public class Thing {
 /** First overload. @param x first value */ public void run(int x) {}
 /** Second overload. */ public void run(String x) {}
 /** A field contract. */ public final String label = "x";
}
''')
        self.target("1", True)
        self.target("2", True)

    def tearDown(self): self.tmp.cleanup()

    def target(self, target, valid):
        base = self.root / "build/docs/targets" / target
        py = base / "python/JsMacrosAC/Thing.py"; py.parent.mkdir(parents=True, exist_ok=True)
        py.write_text('class Thing:\n    """Class contract.\n\n    Example omitted.\n    """\n    label: str\n    """field prose"""\n    def run(self, x: int):\n        """First overload prose."""\n')
        ts = base / "typescript/headers/api.d.ts"; ts.parent.mkdir(parents=True, exist_ok=True)
        ts.write_text('declare namespace Packages.api { export class Thing {\n/** First overload prose */\nrun(x: number): void;\n/** Second overload prose */\nrun(x: string): void;\nlabel: string;\n} }\n')
        html = base / "web/2.0.0/api/Thing.html"; html.parent.mkdir(parents=True, exist_ok=True)
        html.write_text('''<main class="classDoc"><div id="subClasses">BIG UNRELATED INDEX</div>
<h2 class="classTitle">api.Thing</h2><div class="flags"></div><p class="description">Overview <pre>class example</pre></p>
<h3>Constructors</h3><div id="constructor-X" class="constructor classItem"><h4>new Thing()</h4><div class="flags">readonly</div><div class="constructorDesc"><a href="Other.html">Constructor</a></div><table class="paramTable"><tr><td>runner</td></tr></table></div>
<h3>Fields</h3><div id="label" class="field classItem"><h4>.label</h4><div class="fieldDesc">Field &amp; method</div><div class="fieldReturn">Type: <code>Readonly&lt;string&gt;</code></div></div>
<h3>Methods</h3><div id="run-int-" class="method classItem"><h4 class="methodTitle">.run(int)</h4><div class="methodDesc">first overload</div></div>
<div id="run-string-" class="method classItem"><h4 class="methodTitle">.run(string)</h4><div class="methodDesc">second overload</div></div></main>''')
        md = base / "vitepress/content/2.0.0" / target / "classes/api/Thing.md"; md.parent.mkdir(parents=True, exist_ok=True)
        md.write_text('''---
outline: deep
---
# Thing

Overview with `class example`.

## Constructors

### new Thing {#Thing_constructor}

<div class="overload-list"><div class="overload-item">constructor docs and parameter type Core</div></div>

## Fields

### label {#Thing_label}

<Badge type="info" text="read-only" />

## Methods

### run {#Thing_run}

<div class="overload-list"><div class="overload-item">first overload</div><div class="overload-item">second overload</div></div>
''')

    def test_content_extraction_not_just_parse(self):
        packet = module.artifacts(self.root, "1", self.source)
        htmlfrags = packet["HTML"]["files"][0]["fragments"]
        self.assertIn("class example", htmlfrags[0]["text"])
        self.assertFalse(any("BIG UNRELATED INDEX" in f.get("text", "") for f in htmlfrags))
        self.assertEqual(2, len([f for f in htmlfrags if f["name"] == "run"]))
        py = packet["Python"]["files"][0]["fragments"]
        self.assertTrue(any(f["kind"] == "field" for f in py))
        self.assertIn("Class contract", next(f["text"] for f in py if f["kind"] == "class"))
        self.assertEqual(2, len([f for f in packet["TypeScript"]["files"][0]["fragments"] if f["name"] == "run"]))
        mdfrags = packet["VitePress"]["files"][0]["fragments"]
        self.assertIn("class example", mdfrags[0]["text"])
        self.assertEqual(1, len([f for f in mdfrags if f["name"] == "run"]))
        runmd = next(f for f in mdfrags if f["name"] == "run")
        self.assertIn("second overload", runmd["raw_markdown"])
        fieldhtml = next(f for f in htmlfrags if f["name"] == "label")
        self.assertIn("&amp;", fieldhtml["raw_html"])
        self.assertTrue(fieldhtml["elements"])
        ctorhtml = next(f for f in htmlfrags if f["anchor"] == "constructor-X")
        self.assertIn({"href": "Other.html", "label": "Constructor"}, ctorhtml["links"])
        self.assertIn("Readonly&lt;string&gt;", fieldhtml["raw_html"])

    def test_python_fragments_include_complete_declarations(self):
        py = self.root / "python.py"
        py.write_text('''from typing import Final, overload\n\n@decorator\nclass Child(Base[Thing], metaclass=Meta):\n    field: Final[int] = 4\n    """Field prose."""\n\n    @overload\n    def getType(self, name: str) -> str | None: ...\n\n    def plain(self, value: int) -> int:\n        """Method prose."""\n        return value\n\n@api\ndef make(value: str) -> Child | None: ...\n''')
        fragments, error = module.py_fragments(py)
        self.assertIsNone(error)
        cls = next(f for f in fragments if f["kind"] == "class")
        self.assertEqual("@decorator\nclass Child(Base[Thing], metaclass=Meta):", cls["declaration"])
        self.assertEqual(["decorator"], cls["decorators"])
        field = next(f for f in fragments if f["kind"] == "field")
        self.assertEqual("field: Final[int] = 4", field["declaration"])
        self.assertEqual("Field prose.", field["text"])
        nullable = next(f for f in fragments if f["name"] == "getType")
        self.assertEqual("@overload\n    def getType(self, name: str) -> str | None:", nullable["declaration"])
        self.assertEqual("str | None", nullable["return_annotation"])
        self.assertEqual(["overload"], nullable["decorators"])
        plain = next(f for f in fragments if f["name"] == "plain")
        self.assertEqual("int", plain["return_annotation"])
        self.assertTrue(plain["declaration"].endswith("-> int:"))
        func = next(f for f in fragments if f["name"] == "make")
        self.assertEqual("@api\ndef make(value: str) -> Child | None:", func["declaration"])
        self.assertEqual("Child | None", func["return_annotation"])

    def test_malformed_python_is_reported_even_if_other_formats_present(self):
        p = self.root / "build/docs/targets/1/python/JsMacrosAC/Thing.py"
        p.write_text("class Thing(:\n")
        got, error = module.py_fragments(p)
        self.assertFalse(got); self.assertTrue(error.startswith("SyntaxError"))
        rendered = module.artifacts(self.root, "1", self.source)
        self.assertEqual("parse_error", rendered["Python"]["state"])

    def test_grouping_does_not_erase_target_presence_and_never_passes(self):
        out = self.root / "out"
        manifest = module.build_packets(self.root, ["src/api/Thing.java"], ["1", "2"], out)
        self.assertEqual(2, manifest["packets"])
        self.assertTrue(manifest["all_packets_require_review"])
        self.assertTrue(any(len(g["occurrences"]) == 2 for g in manifest["artifact_groups"]))
        self.assertEqual("present", __import__("json").loads((out / "packet-00001.json").read_text())["artifacts"]["Python"]["state"])

    def test_missing_is_not_assumed_excluded(self):
        base = self.root / "build/docs/targets/1/python/JsMacrosAC/Thing.py"
        base.unlink()
        result = module.artifacts(self.root, "1", self.source)
        self.assertEqual("missing", result["Python"]["state"])

    def test_artifacts_are_exact_class_owned_not_corpus_wide(self):
        other = self.root / "src/api/Other.java"
        other.write_text('package api; /** Other contract. */ public class Other {}')
        for target in ("1", "2"):
            base = self.root / "build/docs/targets" / target
            (base / "python/JsMacrosAC/Other.py").write_text('class Other:\n    """Other-only prose"""\n')
            html = base / "web/2.0.0/api/Other.html"; html.parent.mkdir(parents=True, exist_ok=True)
            html.write_text("Other-only page")
            md = base / "vitepress/content/2.0.0" / target / "classes/api/Other.md"; md.parent.mkdir(parents=True, exist_ok=True)
            md.write_text("Other-only page")
            ts = base / "typescript/headers/api.d.ts"
            with ts.open("a") as f: f.write("declare namespace Packages.api { export class Other {\n/** Other method */\nother(): void;\n} }\n")
        result = module.artifacts(self.root, "1", self.source)
        self.assertEqual("present", result["HTML"]["state"])
        self.assertNotIn("Other-only", result["HTML"]["files"][0]["fragments"][0]["text"])
        self.assertFalse(any(f.get("name") == "Other" for f in result["TypeScript"]["files"][0]["fragments"]))

    def test_shared_typescript_file_hash_does_not_define_owned_content_group(self):
        other = self.root / "src/api/Other.java"
        other.write_text('package api; /** Other. */ public class Other {}')
        shared = 'declare namespace Packages.api { export class Thing { one(): void; } }\ndeclare namespace Packages.other { export class Thing { two(): void; } }\n'
        (self.root / "build/docs/targets/1/typescript/headers/api.d.ts").write_text(shared)
        a = module.artifacts(self.root, "1", self.source)["TypeScript"]["files"][0]
        thing2 = self.root / "src/other/Thing.java"; thing2.parent.mkdir(parents=True); thing2.write_text('package other; /** Other. */ public class Thing {}')
        b = module.artifacts(self.root, "1", thing2)["TypeScript"]["files"][0]
        self.assertEqual(a["sha256"], b["sha256"])
        self.assertNotEqual(a["owned_sha256"], b["owned_sha256"])

    def test_undocumented_outer_class_owns_nested_documented_type(self):
        java = self.root / "src/api/Outer.java"
        java.write_text('package api; public class Outer { /** Inner docs. */ public class Inner {} }')
        contract = module.source_contract(java)
        self.assertEqual("api.Outer", contract["qualified_type"])
        nested = next(e for e in contract["entries"] if e["name"] == "Inner")
        self.assertEqual("type:api.Outer.Inner", nested["key"])

    def test_same_simple_name_in_other_namespace_is_not_attached(self):
        ts = self.root / "build/docs/targets/1/typescript/headers/api.d.ts"
        ts.write_text('declare namespace Packages.api { export class Thing { own(): void; } }\ndeclare namespace Packages.other { export class Thing { other(): void; } }')
        result = module.artifacts(self.root, "1", self.source)["TypeScript"]["files"][0]
        self.assertFalse(any(f["name"] == "other" for f in result["fragments"]))

    def test_same_owner_interface_class_declaration_merge_is_preserved(self):
        ts = self.root / "build/docs/targets/1/typescript/headers/api.d.ts"
        ts.write_text('''declare namespace Packages.api {
/** Interface docs */ export interface Thing { /** Instance contract */ value: string; }
/** Class docs */ export class Thing { /** Static contract */ static value: string; run(x: number): string; run(x: string): string; }
}
declare namespace Packages.other { export class Thing { other(): void; } }
''')
        artifact = module.artifacts(self.root, "1", self.source)["TypeScript"]["files"][0]
        self.assertEqual({"class", "interface"}, set(artifact["declaration_merge"]["kinds"]))
        self.assertTrue(artifact["declaration_merge"]["merged"])
        declarations = [f for f in artifact["fragments"] if f["kind"] == "declaration"]
        self.assertEqual(2, len(declarations))
        self.assertTrue(any("Instance contract" in f["text"] for f in artifact["fragments"]))
        self.assertTrue(any("Static contract" in f["text"] for f in artifact["fragments"]))
        overloads = [f for f in artifact["fragments"] if f["name"] == "run"]
        self.assertEqual({"run(x: number): string;", "run(x: string): string;"}, {f["signature"] for f in overloads})

    def test_typescript_extractor_accepts_static_readonly_fields_in_either_order(self):
        ts = self.root / "build/docs/targets/1/typescript/headers/api.d.ts"
        ts.write_text('''declare namespace Packages.api {
export class Thing {
  /** Shared origin. */
  static readonly ZERO: Packages.api.Thing;
  readonly static OTHER: number;
  run(x: number): void;
  run(x: string): void;
}
}
''')
        fragments, error = module.ts_fragments(ts)
        self.assertIsNone(error)
        fields = [item for item in fragments if item.get("name") in {"ZERO", "OTHER"}]
        self.assertEqual({"ZERO", "OTHER"}, {item["name"] for item in fields})
        self.assertEqual("/** Shared origin. */", next(item["text"] for item in fields if item["name"] == "ZERO"))
        self.assertIn("static readonly ZERO:", next(item["signature"] for item in fields if item["name"] == "ZERO"))
        self.assertEqual(2, len([item for item in fragments if item.get("name") == "run"]))

    def test_duplicate_same_kind_qualified_declaration_is_blocked(self):
        ts = self.root / "build/docs/targets/1/typescript/headers/api.d.ts"
        ts.write_text('declare namespace Packages.api { export class Thing {} export class Thing {} }')
        artifact = module.artifacts(self.root, "1", self.source)["TypeScript"]
        self.assertEqual("parse_error", artifact["state"])

    def test_typescript_class_constructor_and_generic_method_docs_are_attached(self):
        ts = self.root / "build/docs/targets/1/typescript/headers/api.d.ts"
        ts.write_text('''declare namespace Packages.api {
/** Runs the listener and returns its event container. */
export class EventListener {
  /** Creates a listener for this macro and runner. */
  constructor(macro: ScriptTrigger, runner: Core);
  /** Calls the listener with a typed payload. */
  trigger<T>(event: T): EventContainer;
}
}
''')
        java = self.root / "src/api/EventListener.java"; java.write_text('package api; /** Listener class. */ public class EventListener {}')
        artifact = module.artifacts(self.root, "1", java)["TypeScript"]["files"][0]
        cls = next(f for f in artifact["fragments"] if f["kind"] == "declaration")
        ctor = next(f for f in artifact["fragments"] if f.get("name") == "constructor")
        trigger = next(f for f in artifact["fragments"] if f.get("name") == "trigger")
        self.assertIn("Runs the listener", cls["text"])
        self.assertIn("Creates a listener", ctor["text"])
        self.assertIn("macro: ScriptTrigger", ctor["signature"])
        self.assertIn("Calls the listener", trigger["text"])
        self.assertIn("<T>", trigger["signature"])

    def test_typescript_declaration_match_ignores_comment_braces_and_generic_braces(self):
        ts = self.root / "build/docs/targets/1/typescript/headers/api.d.ts"
        ts.write_text('''declare namespace Packages.api {
/** Builds a class at runtime from declarations { and callbacks. */
export class ClassBuilder<T> extends java.lang.Object {
  /** Initializes it. */
  addClinit(): Packages.api.ClassBuilder$ConstructorBuilder;
  /** Uses a generic type. */
  map<U extends { value: string }>(input: U): U;
}
}
''')
        java = self.root / "src/api/ClassBuilder.java"
        java.write_text("package api; public class ClassBuilder<T> {}")
        artifact = module.artifacts(self.root, "1", java)["TypeScript"]["files"][0]
        self.assertEqual("api.ClassBuilder", artifact["owner"])
        declaration = next(f for f in artifact["fragments"] if f["kind"] == "declaration")
        self.assertEqual("Packages.api.ClassBuilder", declaration["owner_path"])
        self.assertTrue(any(f.get("name") == "addClinit" for f in artifact["fragments"]))

    def test_nested_type_member_is_owned_across_all_four_formats(self):
        src = self.root / "src/api/EventFilterer.java"
        src.write_text('''package api;
public interface EventFilterer {
    interface Compound extends EventFilterer {
        /** Check <pre>base != this</pre> and {@link IllegalArgumentException}. */
        default void checkCyclicRef(Compound base) {}
    }
}
''')
        root = self.root / "build/docs/targets/1"
        py = root / "python/JsMacrosAC/EventFilterer_Compound.py"; py.parent.mkdir(parents=True, exist_ok=True)
        py.write_text('class EventFilterer_Compound:\n    def checkCyclicRef(self, base):\n        """Throws if base is this compound filterer."""\n')
        ts = root / "typescript/headers/api.d.ts"; ts.parent.mkdir(parents=True, exist_ok=True)
        ts.write_text('declare namespace Packages.api { export interface EventFilterer$Compound { /** Check */ checkCyclicRef(base: Packages.api.EventFilterer$Compound): void; } }')
        web = root / "web/2.0.0/api/EventFilterer.Compound.html"; web.parent.mkdir(parents=True, exist_ok=True)
        web.write_text('<main class="classDoc"><h2 class="classTitle">EventFilterer.Compound</h2><p class="description">Compound docs</p><h3>Methods</h3><div id="checkCyclicRef-Compound-" class="method classItem"><h4>checkCyclicRef</h4><div class="methodDesc"><a href="x">check</a><pre><code>base != this</code></pre></div></div></main>')
        md = root / "vitepress/content/2.0.0/1/classes/api/EventFilterer.Compound.md"; md.parent.mkdir(parents=True, exist_ok=True)
        md.write_text('# Compound\n\nCompound docs\n\n## Methods\n\n### checkCyclicRef {#checkCyclicRef-Compound-}\n\n`base != this`\n')
        packet = module.artifacts(self.root, "1", src)
        for fmt in module.FORMATS: self.assertEqual("present", packet[fmt]["state"], fmt)
        pyfrag = next(f for a in packet["Python"]["files"] for f in a["fragments"] if f["name"] == "checkCyclicRef")
        tsfrag = next(f for a in packet["TypeScript"]["files"] for f in a["fragments"] if f["name"] == "checkCyclicRef")
        self.assertEqual("api.EventFilterer.Compound", pyfrag["owner"])
        self.assertEqual("api.EventFilterer.Compound", tsfrag["source_owner"])
        nested_doc = next(e for e in module.source_contract(src)["entries"] if e["name"] == "checkCyclicRef")
        self.assertIn("<pre>", nested_doc["raw_javadoc"])
        self.assertEqual("EventFilterer.Compound", nested_doc["owner"])

    def test_field_javadoc_does_not_attach_to_later_method_or_textblock(self):
        java = self.root / "src/api/Fields.java"
        java.write_text('''package api; class Fields {
String example = """ text /** fake */ still text """;
/** Field contract. */ public int value;
/** Method contract. */ public void next() {}
}''')
        contract = module.source_contract(java)
        self.assertNotIn("fake", str(contract["entries"]))
        field = next(e for e in contract["entries"] if e["name"] == "value")
        method = next(e for e in contract["entries"] if e["name"] == "next")
        self.assertIn("Field contract", field["raw_javadoc"])
        self.assertNotIn("Method contract", field["raw_javadoc"])
        self.assertIn("Method contract", method["raw_javadoc"])

    def test_web_extraction_fails_closed_on_unbalanced_html_or_missing_markdown_sections(self):
        html = self.root / "bad.html"
        html.write_text('<h2 class="classTitle">api.Thing</h2><h3>Methods</h3><div id="x" class="method classItem">')
        fragments, error = module.web_fragments(html, "HTML", "api.Thing")
        self.assertEqual([], fragments)
        self.assertEqual("classItem_unbalanced", error)
        md = self.root / "bad.md"; md.write_text("# Thing\nNo member headings\n")
        fragments, error = module.web_fragments(md, "VitePress", "api.Thing")
        self.assertEqual([], fragments)
        self.assertEqual("markdown_member_sections_missing", error)

    def test_web_fragment_marks_oversize_without_truncating(self):
        page = self.root / "large.html"
        body = "x" * (module.MAX_FRAGMENT + 10)
        page.write_text(f'<h2 class="classTitle">api.Thing</h2><p>{body}</p><h3>Methods</h3>')
        fragments, error = module.web_fragments(page, "HTML", "api.Thing")
        self.assertIsNone(error)
        self.assertTrue(fragments[0]["oversize"])
        self.assertIn(body, fragments[0]["raw_html"])

    def test_python_keyword_aliases_map_back_to_original_java_members(self):
        java = self.root / "src/api/Predicate.java"
        java.write_text('''package api; public class Predicate {
/** conjunction */ public boolean and();
/** disjunction */ public boolean or();
/** ordinary name */ public boolean matches();
}''')
        py = self.root / "build/docs/targets/1/python/JsMacrosAC/Predicate.py"
        py.parent.mkdir(parents=True, exist_ok=True)
        py.write_text('''class Predicate:
    def __init__(self): pass
    def and_(self): """conjunction"""
    def or_(self): """disjunction"""
    def matches(self): """ordinary name"""
''')
        packet = module.artifacts(self.root, "1", java)["Python"]["files"][0]
        fragments = packet["fragments"]
        for emitted, original in (("and_", "and"), ("or_", "or"), ("matches", "matches")):
            fragment = next(f for f in fragments if f["kind"] == "method" and f["name"] == emitted)
            self.assertEqual([original], fragment["source_member_names"])
            source_entry = next(e for e in module.source_contract(java)["entries"] if e["name"] == original)
            self.assertEqual([source_entry["key"]], fragment["source_member_ids"])
        self.assertTrue(any(f["name"] == "__init__" for f in fragments))

    def test_nested_identical_member_signatures_have_distinct_owner_keys(self):
        java = self.root / "src/api/Outer.java"
        java.write_text('''package api; public class Outer {
    interface A { /** A contract. */ void foo(); }
    interface B { /** B contract. */ void foo(); }
}''')
        entries = module.source_contract(java)["entries"]
        foo = [e for e in entries if e["name"] == "foo"]
        self.assertEqual(2, len(foo))
        self.assertEqual({"api.Outer.A", "api.Outer.B"}, {e["owner_qualified"] for e in foo})
        self.assertEqual(2, len({e["key"] for e in foo}))

    def test_source_declaration_matching_skips_annotations_but_keeps_them_in_signature(self):
        java = self.root / "src/api/Annotated.java"
        java.write_text('''package api;
/** Annotated type. */ @DocletCategory("Events") public class Annotated {
    /** Current game mode. */
    @DocletReplaceReturn("Gamemode") @Nullable
    public String getGameMode() { return ""; }
    /** Configured with nested call-looking text. */
    @DocletReplaceParams("x: Foo(bar), label: \\"a,(b)\\"")
    public void configure(String x, String label) {}
    /** Constructed with an annotated type. */
    @Nullable public Annotated() {}
}
''')
        contract = module.source_contract(java)
        getter = next(e for e in contract["entries"] if e["body"] == "Current game mode.")
        self.assertEqual("getGameMode", getter["name"])
        self.assertIn('@DocletReplaceReturn("Gamemode")', getter["signature"])
        self.assertIn("@Nullable", getter["signature"])
        self.assertTrue(getter["key"].startswith("member:api.Annotated#getGameMode:"))
        configured = next(e for e in contract["entries"] if e["body"] == "Configured with nested call-looking text.")
        self.assertEqual("configure", configured["name"])
        self.assertIn("@DocletReplaceParams", configured["signature"])
        constructor = next(e for e in contract["entries"] if e["body"] == "Constructed with an annotated type.")
        self.assertEqual("Annotated", constructor["name"])
        self.assertIn("@Nullable", constructor["signature"])

    def test_source_lexing_ignores_fake_javadoc_in_string(self):
        java = self.root / "src/api/Lex.java"
        java.write_text('package api; class Lex { String x = "/** fake */"; /** Real contract */ public void call() {} }')
        contract = module.source_contract(java)
        self.assertIn("Real contract", str(contract["entries"]))
        self.assertNotIn("fake", str(contract["entries"]))


if __name__ == "__main__": unittest.main()
