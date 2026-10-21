import importlib.util
import json
from pathlib import Path
import sqlite3
import sys
import tempfile
import threading
import time
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).parents[1] / "docs_prune_validate.py"
sys.path.insert(0, str(SCRIPT.parent))
spec = importlib.util.spec_from_file_location("docs_prune_validate", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ValidateTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        (self.root / "settings.gradle.kts").write_text('stonecutter { versions("1.21.5", "1.21.8", "26.1.2") }')
        (self.root / "common/src/main/java/api").mkdir(parents=True)
        (self.root / "common/src/main/java/api/Thing.java").write_text('package api; /** Contract. */ public class Thing {}')
        work = self.root / "doc-prune-work/campaign"
        work.mkdir(parents=True)
        c = sqlite3.connect(work / "campaign.sqlite3")
        c.executescript("CREATE TABLE sources(path TEXT,baseline_hash TEXT); CREATE TABLE tasks(state TEXT,lease_until REAL); CREATE TABLE meta(key TEXT PRIMARY KEY,value TEXT NOT NULL);")
        c.execute("INSERT INTO sources VALUES(?,?)", ("common/src/main/java/api/Thing.java", module.digest((self.root / "common/src/main/java/api/Thing.java").read_bytes())))
        c.commit(); c.close()
        baseline = work / "baseline/common/src/main/java/api"
        baseline.mkdir(parents=True)
        (baseline / "Thing.java").write_bytes((self.root / "common/src/main/java/api/Thing.java").read_bytes())

    def tearDown(self):
        self.tmp.cleanup()

    def test_retired_artifact_review_cannot_resume_or_dispatch(self):
        work = self.root / "doc-prune-work/campaign"
        (work / "ARTIFACT_REVIEW_RETIRED").write_text("retired by user")
        (work / "ARTIFACT_STOP").write_text("keep paused")
        with patch.object(module.API, "request", side_effect=AssertionError("no model/API dispatch")):
            with self.assertRaisesRegex(RuntimeError, "artifact review is retired"):
                module.run(root=self.root, db=work / "artifact-validation.sqlite3", resume=True)
        self.assertEqual("keep paused", (work / "ARTIFACT_STOP").read_text())

    def test_member_group_scopes_errors_and_snapshots_to_assigned_owner(self):
        assigned = {"key": "member:b", "name": "b", "kind": "member", "signature": "public void b()",
                    "owner_qualified": "api.Thing.Nested"}
        unrelated = {"key": "member:a", "name": "a", "kind": "member", "owner_qualified": "api.Thing"}
        packet = {"qualified_type": "api.Thing", "source_contract": [assigned, unrelated],
                  "artifacts": {"Python": {"state": "present", "files": [
                      {"path": "api/Thing.py", "owner": "api.Thing", "sha256": "a", "parse_error": "bad"},
                      {"path": "api/Thing_Nested.py", "owner": "api.Thing.Nested", "sha256": "b",
                       "fragments": [{"name": "b", "kind": "method", "owner": "api.Thing.Nested"}]}],
                      "failed_files": [{"owner": "api.Thing", "path": "api/Thing.py", "parse_error": "bad"}]}}}
        group = module.build_member_group(packet, 0, [assigned])
        self.assertFalse(group["format_errors"]["Python"])
        self.assertEqual([{"path": "api/Thing_Nested.py", "sha256": "b"}], group["output_snapshot"]["Python"]["files"])
        self.assertNotIn("Python", group["missing_formats"])

        packet["artifacts"]["Python"]["failed_files"].append(
            {"owner": "api.Thing.Nested", "path": "nested.py", "parse_error": "bad"})
        blocked = module.build_member_group(packet, 0, [assigned])
        self.assertTrue(blocked["format_errors"]["Python"])
        self.assertIn("Python", blocked["missing_formats"])

        packet["artifacts"]["Python"]["failed_files"] = [
            {"owner": "api.Unknown", "path": "unknown.py", "parse_error": "bad"}]
        self.assertTrue(module.build_member_group(packet, 0, [assigned])["format_errors"]["Python"])

    def test_member_group_marks_missing_assigned_format(self):
        assigned = {"key": "member:b", "name": "b", "kind": "member", "owner_qualified": "api.Thing.Nested"}
        packet = {"qualified_type": "api.Thing", "source_contract": [assigned],
                  "artifacts": {"Python": {"state": "present", "files": [
                      {"path": "api/Thing.py", "owner": "api.Thing", "sha256": "a", "fragments": []}],
                      "failed_files": []}}}
        group = module.build_member_group(packet, 0, [assigned])
        self.assertEqual("missing", group["output_snapshot"]["Python"]["state"])
        self.assertIn("Python", group["missing_formats"])

    def test_worker_prompt_shows_valid_strict_json_response_shape(self):
        prompt = module.build_worker_prompt({}, {"a001": "member:first", "a002": "member:second", "a003": "member:third"})
        example = '{"completed":["a001"],"unresolved":[{"id":"a002","reason":"Concrete reason this assigned alias cannot be validated."}],"findings":[{"id":"a003","reason":"Concrete validation finding for this assigned alias."}],"summary":"Brief review summary."}'
        self.assertIn(example, prompt)
        json.loads(example)
        self.assertIn("Never use bare string IDs in unresolved or findings", prompt)
        self.assertIn("every entry there must be an object containing its assigned alias in id and a nonempty concrete reason", prompt)
        self.assertIn("A known missing fragment is a blocker: do not mark its alias completed", prompt)

    def test_targets_are_discovered_explicitly(self):
        self.assertEqual(["1.21.5", "1.21.8", "26.1.2"], module.targets(self.root))

    def test_target_discovery_uses_root_stonecutter_declaration_only(self):
        settings = self.root / "settings.gradle.kts"
        settings.write_text('''plugins { id("example") version "9.9.9" }
// versions("8.8.8") is only an example
stonecutter {
  create(rootProject) {
    versions("1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2")
    branch("fabric") { versions("1.21.5", "1.21.8", "1.21.10", "1.21.11") }
  }
}
''')
        self.assertEqual(["1.21.10", "1.21.11", "1.21.5", "1.21.8", "26.1.2"], module.targets(self.root))

    def test_repository_settings_target_discovery_regression(self):
        actual = Path(__file__).parents[2]
        self.assertEqual(["1.21.10", "1.21.11", "1.21.5", "1.21.8", "26.1.2"], module.targets(actual))

    def test_constructor_contract_selects_python_init_not_class_doc(self):
        java = self.root / "common/src/main/java/api/EventListener.java"
        java.write_text('''package api;
/** Listener type contract. */ public class EventListener {
 /** Constructor contract. @param context context */ public EventListener(Context context) {}
 /** Event method contract. */ public void onEvent(Event event) {}
}
''')
        contract = module.artifacts.source_contract(java)
        ctor = next(entry for entry in contract["entries"] if entry["name"] == "EventListener" and entry["kind"] == "member")
        self.assertEqual("__init__", module.output_member_name(ctor, contract["qualified_type"], "Python"))
        self.assertEqual("constructor", module.output_member_name(ctor, contract["qualified_type"], "TypeScript"))
        source_entries = [entry for entry in contract["entries"] if entry["name"] == "EventListener"]
        python = [
            {"name": "EventListener", "kind": "class", "text": "Type docs, not constructor docs"},
            {"name": "__init__", "kind": "method", "signature": "self, context", "text": "Constructor docs"},
        ]
        selected, gaps = module.select_output_fragments(source_entries, python, contract["qualified_type"], "Python")
        self.assertEqual({"EventListener", "__init__"}, {fragment["name"] for fragment in selected})
        self.assertEqual([], gaps)
        ts = [{"name": "EventListener", "kind": "declaration", "signature": "export class EventListener"}]
        selected_ts, gaps_ts = module.select_output_fragments(source_entries, ts, contract["qualified_type"], "TypeScript")
        self.assertEqual("EventListener", selected_ts[0]["name"])
        self.assertIn(ctor["key"], gaps_ts)

    def test_member_selection_is_scoped_across_all_four_artifact_formats(self):
        entries = [
            {"key": "type:api.EventListener", "name": "EventListener", "kind": "type", "signature": "public class EventListener"},
            {"key": "member:constructor", "name": "EventListener", "kind": "member", "signature": "public EventListener(Context context)"},
            {"key": "member:onEvent", "name": "onEvent", "kind": "member", "signature": "public void onEvent(Event e)"},
            {"key": "member:enabled", "name": "enabled", "kind": "member", "signature": "public boolean enabled"},
        ]
        fixtures = {
            "Python": [{"name": "EventListener", "kind": "class"}, {"name": "__init__", "kind": "method"}, {"name": "onEvent", "kind": "method"}, {"name": "enabled", "kind": "field"}, {"name": "unassigned", "kind": "method"}],
            "TypeScript": [{"name": "EventListener", "kind": "declaration"}, {"name": "constructor", "kind": "member"}, {"name": "onEvent", "kind": "member"}, {"name": "enabled", "kind": "member"}, {"name": "unassigned", "kind": "member"}],
            "HTML": [{"name": "EventListener", "kind": "declaration", "raw_html": "<div>class</div>"}, {"name": "EventListener", "kind": "member", "raw_html": "<div>constructor</div>"}, {"name": "onEvent", "kind": "member", "raw_html": "<div>method</div>"}, {"name": "enabled", "kind": "member", "raw_html": "<div>field</div>"}, {"name": "unassigned", "kind": "member", "raw_html": "<div>other</div>"}],
            "VitePress": [{"name": "EventListener", "kind": "declaration", "raw_markdown": "# Class"}, {"name": "EventListener", "kind": "member", "raw_markdown": "### Constructor"}, {"name": "onEvent", "kind": "member", "raw_markdown": "### onEvent"}, {"name": "enabled", "kind": "member", "raw_markdown": "### enabled"}, {"name": "unassigned", "kind": "member", "raw_markdown": "### other"}],
        }
        for fmt, fragments in fixtures.items():
            with self.subTest(format=fmt):
                selected, gaps = module.select_output_fragments(entries, fragments, "api.EventListener", fmt)
                self.assertEqual(4, len(selected))
                self.assertNotIn("unassigned", [fragment["name"] for fragment in selected])
                self.assertEqual([], gaps)
                if fmt in ("HTML", "VitePress"):
                    self.assertTrue(all("raw_html" in f or "raw_markdown" in f for f in selected))
        constructors = [
            {"key": "member:constructor:Context", "name": "EventListener", "kind": "member", "signature": "public EventListener(Context context)"},
            {"key": "member:constructor:Config", "name": "EventListener", "kind": "member", "signature": "public EventListener(Config config)"},
        ]
        for fmt, name, kind in (("Python", "__init__", "method"), ("TypeScript", "constructor", "member"),
                                ("HTML", "EventListener", "member"), ("VitePress", "EventListener", "member")):
            selected, gaps = module.select_output_fragments(constructors,
                [{"name": name, "kind": kind, "signature": "Context"}, {"name": name, "kind": kind, "signature": "Config"}],
                "api.EventListener", fmt)
            self.assertEqual(2, len(selected)); self.assertEqual([], gaps)

    def test_nested_type_names_and_constructors_match_exact_owner_across_formats(self):
        entries = [
            {"key": "type:nested-list", "kind": "type", "name": "NBTListHelper", "owner": "NBTElementHelper",
             "owner_qualified": "com.example.NBTElementHelper.NBTListHelper", "signature": "public class NBTListHelper"},
            {"key": "ctor:nested-list", "kind": "member", "name": "NBTListHelper", "owner": "NBTElementHelper.NBTListHelper",
             "owner_qualified": "com.example.NBTElementHelper.NBTListHelper", "signature": "public NBTListHelper()"},
        ]
        fixtures = {
            "Python": [{"name": "NBTElementHelper_NBTListHelper", "kind": "class", "owner": "com.example.NBTElementHelper.NBTListHelper"},
                       {"name": "__init__", "kind": "method", "owner": "com.example.NBTElementHelper.NBTListHelper"}],
            "TypeScript": [{"name": "NBTElementHelper$NBTListHelper", "kind": "declaration", "owner_path": "Packages.com.example.NBTElementHelper$NBTListHelper"},
                           {"name": "constructor", "kind": "member", "owner_path": "Packages.com.example.NBTElementHelper$NBTListHelper"}],
            "HTML": [{"name": "NBTListHelper", "kind": "declaration", "owner": "com.example.NBTElementHelper.NBTListHelper"},
                     {"name": "NBTListHelper", "kind": "member", "owner": "com.example.NBTElementHelper.NBTListHelper"}],
            "VitePress": [{"name": "NBTListHelper", "kind": "declaration", "owner": "com.example.NBTElementHelper.NBTListHelper"},
                           {"name": "NBTListHelper", "kind": "member", "owner": "com.example.NBTElementHelper.NBTListHelper"}],
        }
        expected = {"Python": ["NBTElementHelper_NBTListHelper", "__init__"],
                    "TypeScript": ["NBTElementHelper$NBTListHelper", "constructor"],
                    "HTML": ["NBTListHelper", "NBTListHelper"], "VitePress": ["NBTListHelper", "NBTListHelper"]}
        for fmt, fragments in fixtures.items():
            with self.subTest(format=fmt):
                selected, missing = module.select_output_fragments(entries, fragments, "com.example.NBTElementHelper", fmt)
                self.assertEqual(expected[fmt], [f["name"] for f in selected])
                self.assertEqual([], missing)

    def test_duplicate_nested_member_names_require_matching_owner_evidence(self):
        entries = [
            {"key": "one", "kind": "member", "name": "read", "owner": "Outer.One", "owner_qualified": "com.example.Outer.One", "signature": "public void read()"},
            {"key": "two", "kind": "member", "name": "read", "owner": "Outer.Two", "owner_qualified": "com.example.Outer.Two", "signature": "public void read()"},
        ]
        fragments = [{"name": "read", "kind": "member", "owner": "com.example.Outer.One", "signature": "one read"},
                     {"name": "read", "kind": "member", "owner": "com.example.Outer.Two", "signature": "two read"},
                     {"name": "read", "kind": "member", "signature": "ownerless ambiguous fragment"}]
        selected, missing = module.select_output_fragments(entries, fragments, "com.example.Outer", "HTML")
        self.assertEqual(["one read", "two read"], [fragment["signature"] for fragment in selected])
        self.assertEqual([], missing)
        selected, missing = module.select_output_fragments(entries, fragments[2:], "com.example.Outer", "HTML")
        self.assertEqual([], selected)
        self.assertEqual(["one", "two"], missing)

    def test_explicit_nonpublic_fields_are_excluded_but_public_and_implicit_fields_remain(self):
        entries = [
            {"key": "field:hidden", "kind": "member", "name": "hidden", "signature": "protected String hidden"},
            {"key": "field:secret", "kind": "member", "name": "secret", "signature": "private int secret = 1"},
            {"key": "field:visible", "kind": "member", "name": "visible", "signature": "public String visible"},
            {"key": "field:implicit", "kind": "member", "name": "IMPLICIT", "signature": "String IMPLICIT"},
            {"key": "method:hidden", "kind": "member", "name": "hidden", "signature": "protected void hidden()"},
        ]
        assigned, excluded = module.split_contract_entries(entries)
        self.assertEqual({"field:visible", "field:implicit"}, {entry["key"] for entry in assigned})
        self.assertEqual({"field:hidden", "field:secret", "method:hidden"}, {entry["key"] for entry in (item["entry"] for item in excluded)})
        self.assertTrue(all(item["renderer_policy"]["verified"] for item in excluded))
        # Interface fields are implicitly public; no explicit protected/private token means no exclusion.
        self.assertIsNone(module.excluded_contract(entries[3]))

    def test_only_explicit_nonpublic_methods_and_constructors_are_excluded(self):
        entries = [
            {"key": "private-fromMap", "kind": "member", "name": "fromMap", "owner_qualified": "api.PlayerInput", "signature": "private static PlayerInput fromMap(Map<String, Object> map)"},
            {"key": "public-fromCsv", "kind": "member", "name": "fromCsv", "owner_qualified": "api.PlayerInput", "signature": "public static PlayerInput fromCsv(String csv)"},
            {"key": "protected-method", "kind": "member", "name": "internal", "owner_qualified": "api.PlayerInput", "signature": "protected void internal()"},
            {"key": "private-ctor", "kind": "member", "name": "PlayerInput", "owner_qualified": "api.PlayerInput", "signature": "private PlayerInput(Options options)"},
            {"key": "implicit-interface", "kind": "member", "name": "load", "owner_qualified": "api.Input", "signature": "load(String value)"},
            {"key": "annotated-public", "kind": "member", "name": "fromJson", "owner_qualified": "api.PlayerInput", "signature": "@VisibilityHint(value = \"private\") public PlayerInput fromJson(String json)"},
        ]
        assigned, excluded = module.split_contract_entries(entries)
        self.assertEqual({"public-fromCsv", "implicit-interface", "annotated-public"}, {entry["key"] for entry in assigned})
        reasons = {item["key"]: item["reason"] for item in excluded}
        self.assertIn("private METHOD", reasons["private-fromMap"])
        self.assertIn("protected METHOD", reasons["protected-method"])
        self.assertIn("private CONSTRUCTOR", reasons["private-ctor"])
        self.assertTrue(all(item["renderer_policy"]["verified"] for item in excluded))

    def test_real_player_input_private_from_map_is_excluded_public_from_csv_is_assigned(self):
        root = Path(__file__).parents[2]
        source = root / "common/src/main/java/com/jsmacrosce/jsmacros/api/PlayerInput.java"
        entries = module.artifacts.source_contract(source)["entries"]
        assigned, excluded = module.split_contract_entries(entries)
        self.assertIn("fromCsv", {entry["name"] for entry in assigned})
        hidden = next(item for item in excluded if item["entry"]["name"] == "fromMap")
        self.assertEqual("private", hidden["entry"]["signature"].split()[0])
        self.assertIn("private METHOD", hidden["reason"])

    def test_python_keyword_methods_select_writer_alias_without_losing_source_owner(self):
        java = self.root / "common/src/main/java/api/Filterer.java"
        java.write_text('''package api; public class Filterer {
 /** conjunction */ public boolean and() { return true; }
 /** disjunction */ public boolean or() { return true; }
 /** ordinary */ public boolean matches() { return true; }
}''')
        contract = module.artifacts.source_contract(java)
        entries = [e for e in contract["entries"] if e["kind"] == "member"]
        python = [
            {"name": "and_", "kind": "method", "owner": contract["qualified_type"], "text": "conjunction"},
            {"name": "or_", "kind": "method", "owner": contract["qualified_type"], "text": "disjunction"},
            {"name": "matches", "kind": "method", "owner": contract["qualified_type"], "text": "ordinary"},
        ]
        selected, gaps = module.select_output_fragments(entries, python, contract["qualified_type"], "Python")
        self.assertEqual([], gaps)
        self.assertEqual({"and_", "or_", "matches"}, {f["name"] for f in selected})
        self.assertEqual("and", next(e["name"] for e in entries if module.output_member_name(e, contract["qualified_type"], "Python") == "and_"))

    def test_generation_fingerprint_covers_source_inputs(self):
        before = module.fingerprint(self.root)
        (self.root / "common/src/main/java/api/Thing.java").write_text('package api; /** Revised. */ public class Thing {}')
        self.assertNotEqual(before, module.fingerprint(self.root))

    def test_prepare_refuses_live_source_campaign_lock(self):
        lock = (self.root / "doc-prune-work/campaign/campaign.lock").open("a+")
        import fcntl
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        try:
            with self.assertRaisesRegex(RuntimeError, "Source campaign is active"):
                module.prepare(self.root)
        finally:
            fcntl.flock(lock, fcntl.LOCK_UN); lock.close()

    def test_prepare_without_matching_stamp_blocks_without_generation(self):
        with patch.object(module.subprocess, "run", return_value=type("R", (), {"stdout": "", "stderr": "", "returncode": 0})()):
            got = module.prepare(self.root, generate=False)
        self.assertEqual("blocked", got["state"])
        self.assertIn("generation stamp", got["reason"])

    def test_prepare_hands_off_all_source_target_chunks_to_validation_queue(self):
        generation = "integration-generation"
        stamp = {"generation_id": generation, "input_fingerprint": module.fingerprint(self.root),
                 "targets": ["1.21.5", "1.21.8"]}
        (self.root / "doc-prune-work/campaign/generation.json").write_text(json.dumps(stamp))
        sources = ["common/src/main/java/api/Thing.java", "common/src/main/java/api/Other.java"]
        for rel in sources:
            path = self.root / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("package api; public class " + path.stem + " {}")
        stamp["input_fingerprint"] = module.fingerprint(self.root)
        (self.root / "doc-prune-work/campaign/generation.json").write_text(json.dumps(stamp))
        public = [{"key": f"member:m{i}", "name": f"m{i}", "kind": "member", "signature": f"public void m{i}()"}
                  for i in range(11)]
        private = {"key": "member:hidden", "name": "hidden", "kind": "member", "signature": "private void hidden()"}
        entries = public + [private]

        def fake_build(_root, selected, selected_targets, out):
            out.mkdir(parents=True, exist_ok=True)
            files = []
            for rel in selected:
                for target in selected_targets:
                    packet = {"source": rel, "target": target, "source_sha256": "source-hash",
                              "qualified_type": "api." + Path(rel).stem, "source_contract": entries,
                              "artifacts": {}, "output_snapshot": {}, "format_errors": {},
                              "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []}
                    filename = f"{Path(rel).stem}-{target}.json"
                    (out / filename).write_text(json.dumps(packet))
                    files.append(filename)
            return {"packet_files": files, "packets": len(files), "missing_format_count": 0}

        with patch.object(module, "runtime_baseline_changes", return_value=[]), \
             patch.object(module, "changed_sources", return_value=sources), \
             patch.object(module, "targets", return_value=["1.21.5", "1.21.8"]), \
             patch.object(module.artifacts, "build_packets", side_effect=fake_build):
            result = module.prepare(self.root)
        self.assertEqual("ready", result["state"])
        self.assertGreater(result["validation_tasks"], 0)
        conn = module.connect(self.root / "doc-prune-work/campaign/artifact-validation.sqlite3")
        rows = conn.execute("SELECT packet FROM tasks WHERE generation=?", (generation,)).fetchall()
        self.assertTrue(rows)
        ids = []; source_keys = set(); excluded = []
        packet_dir = self.root / "doc-prune-work/campaign" / f"artifact-packets-{generation}"
        for row in rows:
            bundle = json.loads((packet_dir / row["packet"]).read_text())
            source_keys.update(entry["key"] for entry in bundle["matrix_source_contract"])
            for view in bundle["targets"]:
                ids.extend((bundle["source"], item) for item in view["assigned_ids"])
                excluded.extend(item["key"] for item in view["excluded_contracts"])
        conn.close()
        expected = {(source, f"{target}::{entry['key']}") for source in sources for target in ("1.21.5", "1.21.8") for entry in public}
        self.assertEqual(expected, set(ids))
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual({entry["key"] for entry in entries}, source_keys)
        self.assertEqual(4, len(excluded))  # once for each source/target packet ownership
        self.assertEqual(["member:hidden"] * 4, excluded)

    def test_prepare_generate_refuses_non_javadoc_mutation(self):
        source = self.root / "common/src/main/java/api/Thing.java"
        source.write_text('package api; /** Contract. */ public class Thing { int runtime = 1; }')
        calls = []
        with patch.object(module.subprocess, "run", return_value=type("R", (), {"stdout": "", "stderr": "", "returncode": 0})()):
            with self.assertRaisesRegex(RuntimeError, "non-Javadoc"):
                module.prepare(self.root, generate=True, runner=lambda *a, **k: calls.append(1))
        self.assertEqual([], calls)

    def test_comment_length_change_is_not_runtime_mutation(self):
        source = self.root / "common/src/main/java/api/Thing.java"
        source.write_text('package api; /** A much longer current Javadoc contract. */ public class Thing {}')
        self.assertEqual([], module.runtime_baseline_changes(self.root))

    def test_approved_javadoc_layout_change_requires_exact_provenance(self):
        rel = "common/src/main/java/api/Thing.java"
        source = self.root / rel
        before = b"package api;\n@Deprecated\n/** Contract. */\npublic class Thing {}\n"
        after = b"package api;\n/** Contract. */\n@Deprecated\npublic class Thing {}\n"
        source.write_bytes(before)
        snapshot = self.root / "doc-prune-work/campaign/baseline" / rel
        snapshot.parent.mkdir(parents=True, exist_ok=True); snapshot.write_bytes(before)
        c = sqlite3.connect(self.root / "doc-prune-work/campaign/campaign.sqlite3")
        c.execute("UPDATE sources SET baseline_hash=? WHERE path=?", (module.digest(before), rel)); c.commit(); c.close()
        source.write_bytes(after)
        self.assertEqual([rel], module.runtime_baseline_changes(self.root))
        approval = {"baseline_hash": module.digest(before), "java_token_hash": module.prune.code_fingerprint(before),
                    "outside_javadocs_sha256": module.digest(module.outside_javadocs(after)),
                    "reason": "Reviewed Javadoc layout move", "review_session": "session-review-1"}
        db = self.root / "doc-prune-work/campaign/campaign.sqlite3"
        c = sqlite3.connect(db); c.execute("INSERT INTO meta VALUES('approved_javadoc_layouts',?)", (json.dumps({rel: approval}),)); c.commit(); c.close()
        self.assertEqual([], module.runtime_baseline_changes(self.root))
        self.assertEqual(before, snapshot.read_bytes(), "approval must not rewrite the immutable baseline")

        # New ordinary-comment/guard bytes change the explicitly approved outside hash.
        source.write_bytes(after.replace(b"@Deprecated", b"// guard\n@Deprecated"))
        self.assertEqual([rel], module.runtime_baseline_changes(self.root))
        source.write_bytes(after.replace(b"Thing {}", b"Thing { int runtime; }"))
        self.assertEqual([rel], module.runtime_baseline_changes(self.root))

    def test_approval_with_wrong_baseline_or_malformed_json_fails_closed(self):
        rel = "common/src/main/java/api/Thing.java"
        source = self.root / rel
        before = b"package api;\n@Deprecated\n/** Contract. */\npublic class Thing {}\n"
        after = b"package api;\n/** Contract. */\n@Deprecated\npublic class Thing {}\n"
        source.write_bytes(before)
        snapshot = self.root / "doc-prune-work/campaign/baseline" / rel
        snapshot.parent.mkdir(parents=True, exist_ok=True); snapshot.write_bytes(before)
        db = self.root / "doc-prune-work/campaign/campaign.sqlite3"
        c = sqlite3.connect(db); c.execute("UPDATE sources SET baseline_hash=? WHERE path=?", (module.digest(before), rel)); c.commit(); c.close()
        source.write_bytes(after)
        approval = {"baseline_hash": "wrong", "java_token_hash": module.prune.code_fingerprint(before),
                    "outside_javadocs_sha256": module.digest(module.outside_javadocs(after)), "reason": "reviewed", "review_session": "session-1"}
        c = sqlite3.connect(db); c.execute("INSERT INTO meta VALUES('approved_javadoc_layouts',?)", (json.dumps({rel: approval}),)); c.commit(); c.close()
        self.assertEqual([rel], module.runtime_baseline_changes(self.root))
        c = sqlite3.connect(db); c.execute("UPDATE meta SET value='not-json' WHERE key='approved_javadoc_layouts'"); c.commit(); c.close()
        self.assertEqual(["<malformed approved_javadoc_layouts metadata>"], module.runtime_baseline_changes(self.root))

    def test_model_default_variant_is_accepted(self):
        class Stub(module.API):
            def request(self, method, path, payload=None, timeout=30):
                self.payload = payload
                return {"data": {"id": "session-1", "model": {**module.MODEL, "variant": "default"}}}
        api = Stub(executable="unused")
        self.assertEqual("session-1", api.create_session(self.root, "title", "docs-validator"))
        self.assertEqual(module.MODEL, api.payload["model"])

    def seed_queue(self, count=4, running_first=False, session_first=None):
        source = self.root / "common/src/main/java/api/Thing.java"
        generation = "gen-test"
        out = self.root / "doc-prune-work/campaign" / f"artifact-packets-{generation}"
        out.mkdir(parents=True)
        stamp = {"generation_id": generation, "input_fingerprint": module.fingerprint(self.root)}
        db = self.root / "doc-prune-work/campaign/artifact-validation.sqlite3"
        c = module.connect(db)
        c.execute("INSERT INTO meta VALUES('generation',?)", (json.dumps(stamp),))
        c.execute("INSERT INTO meta VALUES('active_generation',?)", (generation,))
        c.execute("INSERT INTO meta VALUES('packet_dir',?)", (str(out),))
        for i in range(count):
            snapshot = {}
            for fmt in module.artifacts.FORMATS:
                artifact = self.root / "generated" / f"{i}-{fmt}.txt"
                artifact.parent.mkdir(exist_ok=True)
                content = f"{fmt} output {i}".encode()
                artifact.write_bytes(content)
                snapshot[fmt] = {"state": "present", "files": [{"path": artifact.relative_to(self.root).as_posix(), "sha256": module.digest(content)}]}
            packet_name = f"p{i}.json"
            packet = {"source": source.relative_to(self.root).as_posix(), "source_sha256": module.digest(source.read_bytes()),
                      "target": "1.21.5", "assigned_ids": [f"id-{i}"], "source_contract": [], "artifacts": {},
                      "output_snapshot": snapshot, "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []}
            (out / packet_name).write_text(json.dumps(packet))
            state = "running" if running_first and i == 0 else "pending"
            sid = session_first if i == 0 else None
            c.execute("INSERT INTO tasks(id,source,target,packet,state,session_id,generation) VALUES(?,?,?,?,?,?,?)",
                      (f"task-{i}", packet["source"], "1.21.5", packet_name, state, sid, generation))
        c.commit(); c.close()
        return db

    class FakeAPI:
        def __init__(self, fail_once=False, active_recovery=False, empty_final=False, empty_content=False, outcome="succeeded", finish=None):
            self.active_count = 0; self.max_active = 0; self.guard = threading.Lock(); self.fail_once = fail_once
            self.active_recovery = active_recovery; self.empty_final = empty_final; self.empty_content = empty_content; self.outcome = outcome; self.finish = finish; self.responses = {}; self.calls = 0; self.allowed_ids = []; self.message_requests = []; self.prompts = []
        def create_session(self, *args):
            self.calls += 1
            return f"session-{self.calls}"
        def send(self, sid, prompt):
            import re
            self.prompts.append(prompt)
            match = re.search(r'Allowed IDs: (\[[^\n]+\])', prompt)
            assigned = json.loads(match.group(1)); self.allowed_ids.append(assigned); ident = assigned[0]
            message = {"content": [] if self.empty_content else [{"type": "text", "text": json.dumps({"completed": [ident], "unresolved": [], "findings": [], "summary": "reviewed"})}]}
            if self.finish is not None: message["finish"] = self.finish
            self.responses[sid] = {"data": [message]}
        def request(self, method, path, payload=None, timeout=30):
            if method == "GET" and "/message?" in path: self.message_requests.append(path)
            if path.endswith("/wait"):
                with self.guard:
                    self.active_count += 1; self.max_active = max(self.max_active, self.active_count)
                time.sleep(0.04)
                if self.fail_once:
                    self.fail_once = False
                    with self.guard: self.active_count -= 1
                    raise TimeoutError("offline simulated timeout")
                with self.guard: self.active_count -= 1
                return {"data": None}
            if path.endswith("/message?order=desc&type=assistant&limit=1"):
                if self.empty_final: return {"data": []}
                return self.responses[path.split("/")[3]]
            if path.startswith("/api/session/session-") and "/message" not in path:
                return {"data": {"outcome": self.outcome}}
            if path == "/api/session/active": return {"data": ["old-session"] if self.active_recovery else []}
            if path == "/api/session/old-session": return {"data": {"status": {"type": "running" if self.active_recovery else "idle"}}}
            if path.endswith("/interrupt"): return {"data": None}
            raise AssertionError(path)

    def test_worker_pool_is_parallel_and_max_tasks_claims_only_requested_count(self):
        db = self.seed_queue(4)
        (self.root / "doc-prune-work/campaign/STOP").write_text("source scheduler is paused\n")
        api = self.FakeAPI()
        got = module.run(db=db, root=self.root, workers=2, max_tasks=3, retries=1, api=api)
        self.assertEqual(3, got["processed"])
        self.assertEqual(2, api.max_active)
        c = module.connect(db)
        states = dict(c.execute("SELECT state,count(*) FROM tasks GROUP BY state"))
        c.close()
        self.assertEqual({"passed": 3, "pending": 1}, states)
        self.assertEqual(3, len(api.message_requests))
        self.assertTrue(all(path.endswith("order=desc&type=assistant&limit=1") for path in api.message_requests))

    def test_missing_latest_assistant_answer_fails_closed_without_history_fallback(self):
        db = self.seed_queue(1)
        api = self.FakeAPI(empty_final=True)
        result = module.run(db=db, root=self.root, workers=1, max_tasks=1, retries=0, api=api)
        self.assertEqual(1, result["processed"])
        self.assertEqual(1, len(api.message_requests))
        self.assertTrue(api.message_requests[0].endswith("order=desc&type=assistant&limit=1"))
        c = module.connect(db); row = c.execute("SELECT state,last_error FROM tasks").fetchone(); c.close()
        self.assertEqual("blocked", row["state"])
        self.assertTrue(row["last_error"])

    def test_assistant_error_finish_is_recorded_before_response_parse(self):
        db = self.seed_queue(1)
        api = self.FakeAPI(finish="error")
        result = module.run(db=db, root=self.root, workers=1, max_tasks=1, retries=0, api=api)
        self.assertEqual(1, result["processed"])
        c = module.connect(db); row = c.execute("SELECT state,last_error FROM tasks").fetchone(); attempt = c.execute("SELECT error FROM attempts").fetchone(); c.close()
        self.assertEqual("blocked", row["state"])
        self.assertIn("finish='error'", row["last_error"])
        self.assertIn("finish='error'", attempt["error"])

    def test_failed_session_outcome_blocks_empty_assistant_content(self):
        db = self.seed_queue(1)
        api = self.FakeAPI(empty_content=True, outcome="failed")
        result = module.run(db=db, root=self.root, workers=1, max_tasks=1, retries=0, api=api)
        self.assertEqual(1, result["processed"])
        c = module.connect(db); row = c.execute("SELECT state,last_error FROM tasks").fetchone(); attempt = c.execute("SELECT error FROM attempts").fetchone(); c.close()
        self.assertEqual("blocked", row["state"])
        self.assertIn("outcome=failed", row["last_error"])
        self.assertIn("outcome=failed", attempt["error"])

    def test_provider_quota_stops_new_claims_and_preserves_evidence(self):
        db = self.seed_queue(4)
        api = self.FakeAPI()
        original_send = api.send
        quota_sent = threading.Event()
        def send(sid, prompt):
            original_send(sid, prompt)
            if not quota_sent.is_set():
                quota_sent.set()
                api.responses[sid] = {"data": [{"finish": "error", "error": {
                    "type": "provider.quota", "message": "The usage limit has been reached"}, "content": []}]}
        api.send = send
        result = module.run(db=db, root=self.root, workers=2, max_tasks=4, retries=1, api=api)
        self.assertEqual("quota_stopped", result["state"])
        self.assertEqual({"type": "provider.quota", "message": "The usage limit has been reached"}, result["quota"])
        c = module.connect(db)
        rows = c.execute("SELECT id,state,last_error FROM tasks ORDER BY id").fetchall()
        attempts = c.execute("SELECT transcript,error FROM attempts WHERE error LIKE '%provider.quota%'").fetchone()
        c.close()
        self.assertEqual(2, len([row for row in rows if row["state"] != "pending"]))
        self.assertEqual(2, len(api.message_requests))
        self.assertEqual(2, len([row for row in rows if row["state"] == "pending"]))
        self.assertEqual(1, len([row for row in rows if row["state"] == "blocked"]))
        self.assertIsNotNone(attempts)
        self.assertIn("provider.quota", attempts["transcript"])
        self.assertIn("The usage limit has been reached", attempts["transcript"])
        self.assertIn("provider.quota", attempts["error"])

    def test_nonquota_worker_error_does_not_trigger_quota_stop(self):
        db = self.seed_queue(2)
        api = self.FakeAPI(outcome="failed")
        original_send = api.send
        def send(sid, prompt):
            original_send(sid, prompt)
            api.responses[sid] = {"data": [{"finish": "error", "error": {
                "type": "provider.internal", "message": "temporary service failure"}, "content": []}]}
        api.send = send
        result = module.run(db=db, root=self.root, workers=1, max_tasks=1, retries=0, api=api)
        self.assertNotEqual("quota_stopped", result.get("state"))
        c = module.connect(db)
        row = c.execute("SELECT state,last_error FROM tasks WHERE id='task-0'").fetchone()
        attempt = c.execute("SELECT transcript,error FROM attempts").fetchone()
        c.close()
        self.assertEqual("blocked", row["state"])
        self.assertIn("provider.internal", row["last_error"])
        self.assertNotIn("provider.quota", row["last_error"])
        self.assertIn("temporary service failure", attempt["transcript"])

    def test_successful_session_without_finish_field_accepts_valid_json(self):
        db = self.seed_queue(1)
        result = module.run(db=db, root=self.root, workers=1, max_tasks=1, retries=0, api=self.FakeAPI(outcome="succeeded", finish=None))
        self.assertEqual(1, result["processed"])
        c = module.connect(db); state = c.execute("SELECT state FROM tasks").fetchone()[0]; c.close()
        self.assertEqual("passed", state)

    def test_large_prompt_uses_durable_file_and_bounded_authenticated_cli_body(self):
        work = self.root / "campaign-work"
        api = module.API(executable="opencode-test", work=work)
        commands = []
        def fake_run(command, **kwargs):
            commands.append(command)
            return type("Result", (), {"stdout": "{}"})()
        prompt = "FULL_PACKET_BEGIN\n" + ('artifact λ🙂 "quoted" \\\\ slash\nline\ttab\x01 ' * 5500) + "\nFULL_PACKET_END"
        with patch.object(module.subprocess, "run", side_effect=fake_run):
            api.send("session-large", prompt)
        self.assertEqual(1, len(commands))
        data = commands[0][commands[0].index("--data") + 1]
        self.assertLess(len(data.encode("utf-8")), 64 * 1024)
        self.assertLess(max(len(arg.encode("utf-8")) for arg in commands[0]), 64 * 1024)
        compact = json.loads(data)["text"]
        self.assertIn("Read EVERY listed part in order", compact)
        self.assertIn("concatenate its strings in order", compact)
        dispatch_dir = next((work / "worker-inputs").iterdir())
        archive = dispatch_dir / "prompt.txt"
        parts = sorted(dispatch_dir.glob("part-*.txt"), key=lambda path: path.name)
        self.assertGreater(len(prompt.encode("utf-8")), 200 * 1024)
        self.assertGreater(len(parts), 1)
        self.assertLessEqual(len(parts), module.MAX_PROMPT_PARTS)
        self.assertEqual(prompt, archive.read_text(encoding="utf-8"))
        decoded_parts = []
        for part in parts:
            content = part.read_text(encoding="utf-8")
            # Simulate a reader with a 2000-character-per-line cap.
            truncated_simulation = "\n".join(line[:2000] for line in content.splitlines()) + "\n"
            self.assertEqual(content, truncated_simulation)
            self.assertTrue(all(len(line) <= module.MAX_PROMPT_PART_LINE_CHARS for line in content.splitlines()))
            self.assertLessEqual(len(part.read_bytes()), module.MAX_PROMPT_PART_BYTES)
            decoded_parts.append(json.loads(truncated_simulation))
        reconstructed = "".join(segment for part in decoded_parts for segment in part)
        self.assertEqual(prompt, reconstructed)

    def test_large_prompt_over_part_budget_fails_before_dispatch(self):
        work = self.root / "campaign-work"
        api = module.API(executable="opencode-test", work=work)
        commands = []
        prompt = "λ" * (module.MAX_PROMPT_PART_BYTES * (module.MAX_PROMPT_PARTS + 1))
        with patch.object(module.subprocess, "run", side_effect=lambda command, **kwargs: (commands.append(command) or type("Result", (), {"stdout": "{}"})())):
            with self.assertRaisesRegex(ValueError, "refusing dispatch"):
                api.send("session-too-large", prompt)
        self.assertEqual([], commands)
        self.assertFalse((work / "worker-inputs").exists())

    def test_structured_packet_transport_roundtrips_independent_json_parts(self):
        work = self.root / "structured-work"
        api = module.API(executable="unused", work=work)
        raw = '<pre>raw λ🙂 markup {@code x}</pre>\n' + ("界🙂<tag>&\\\"\n" * 2600)
        packet = {"id_map": {"a001": "1.21.8::member:x"}, "targets": [
            {"target": "1.21.8", "artifacts": {"HTML": {"files": [{"path": "x.html", "fragment": {"raw_html": raw, "typing": "readonly: Final[str]"}}]}}},
            {"target": "26.1.2", "artifacts": {}, "empty": [], "nothing": {}}],
            "source_contract": [{"signature": "public <T> T x(T value)", "javadoc": "{@param value raw}"}],
            "null": None, "truth": True, "number": 13}
        prompt = "PACKET BEGIN\n" + json.dumps(packet, ensure_ascii=False) + "\nPACKET END"
        calls = []
        api.request = lambda method, path, payload=None, timeout=30: (calls.append((method, path, payload)) or {"data": None})
        api.send_packet("session-structured", packet, packet["id_map"], prompt)
        self.assertEqual(1, len(calls))
        dispatch = calls[0][2]["text"]
        self.assertIn("text_chunk", dispatch)
        directory = next((work / "worker-inputs").iterdir())
        self.assertEqual(prompt, (directory / "prompt.txt").read_text(encoding="utf-8"))
        files = sorted(directory.glob("part-*.json"))
        self.assertGreater(len(files), 1)
        self.assertLessEqual(len(files), module.MAX_PROMPT_PARTS)
        parsed = []
        for path in files:
            data = path.read_bytes()
            self.assertLessEqual(len(data), module.MAX_PROMPT_PART_BYTES)
            text = data.decode("utf-8")
            self.assertTrue(all(len(line.encode("utf-8")) < module.MAX_PROMPT_PART_LINE_CHARS for line in text.splitlines()))
            parsed.append(json.loads(text))
            self.assertNotIn("id_map", parsed[-1], "canonical aliases travel once as packet records")
        self.assertEqual(packet, module.API.reconstruct_packet_parts(parsed))

    def test_structured_reconstruction_rejects_missing_parts_and_chunks(self):
        packet = {"long": "λ" * 1200}
        records = module.API._packet_records(packet)
        parts = [{"schema": 1, "part_index": 0, "part_count": 1, "records": records}]
        self.assertEqual(packet, module.API.reconstruct_packet_parts(parts))
        broken = [{**parts[0], "records": records[:-1]}]
        with self.assertRaisesRegex(ValueError, "missing string chunk"):
            module.API.reconstruct_packet_parts(broken)

    def test_small_prompt_keeps_normal_api_body_unchanged(self):
        api = module.API(executable="opencode-test", work=self.root / "work")
        commands = []
        with patch.object(module.subprocess, "run", side_effect=lambda command, **kwargs: (commands.append(command) or type("Result", (), {"stdout": "{}"})())):
            api.send("session-small", "small payload")
        data = commands[0][commands[0].index("--data") + 1]
        self.assertEqual({"text": "small payload"}, json.loads(data))
        self.assertFalse((self.root / "work/worker-inputs").exists())

    def test_wire_alias_roundtrip_is_exact_and_never_mutates_durable_packet(self):
        ids = ["1.21.5::member:api.EventListener#constructor", "1.21.8::member:api.EventListener#constructor"]
        packet = {"assigned_ids": ids.copy(), "targets": [{"target": "1.21.5", "assigned_ids": [ids[0]], "artifacts": {"HTML": {"files": [{"path": "one.html", "fragment_refs": ["f1"]}]}}}],
                  "source_contract": [{"key": "member:api.EventListener#constructor", "signature": "long declaration"}],
                  "matrix_source_contract": [{"key": "lots of unassigned class entries"}],
                  "extracted_fragments": [{"sha256": "f1", "fragment": {"text": "selected"}, "references": [{"target": "1.21.5", "format": "HTML", "path": "one.html"}]},
                                          {"sha256": "f2", "fragment": {"text": "completed target"}, "references": [{"target": "1.21.8", "format": "HTML", "path": "two.html"}]}]}
        original = json.loads(json.dumps(packet))
        aliases = module.wire_aliases(ids)
        wire = module.wire_packet(packet, aliases)
        self.assertEqual(["a001", "a002"], wire["assigned_ids"])
        self.assertEqual(["a001"], wire["targets"][0]["assigned_ids"])
        self.assertNotIn("matrix_source_contract", wire)
        self.assertEqual(["f1"], [item["sha256"] for item in wire["extracted_fragments"]])
        self.assertEqual(ids, packet["assigned_ids"])
        self.assertEqual(original, packet)
        response = {"completed": ["a001", ids[1]], "unresolved": [{"id": "a002", "reason": "uncertain"}],
                    "findings": [], "summary": "checked"}
        canonical = module.canonicalize_response_ids(response, aliases, ids)
        self.assertEqual(ids, canonical["completed"])
        self.assertEqual(ids[1], canonical["unresolved"][0]["id"])
        with self.assertRaisesRegex(ValueError, "unknown worker response ID"):
            module.canonicalize_response_ids({**response, "completed": ["EventListener"]}, aliases, ids)

    def test_final_response_selection_uses_phase_and_preserves_legacy_shape(self):
        valid = json.dumps({"completed": [], "unresolved": [], "findings": [], "summary": "ok"})
        def text(value, phase=None):
            part = {"type": "text", "text": value}
            if phase is not None: part["state"] = {"phase": phase}
            return part
        self.assertEqual(valid, module.select_final_response_text({"content": [
            text("state.phase commentary"), text(valid, "final_answer")]}))
        with self.assertRaisesRegex(ValueError, "exactly one phased final_answer"):
            module.select_final_response_text({"content": [text(valid, "commentary")]})
        with self.assertRaisesRegex(ValueError, "exactly one phased final_answer"):
            module.select_final_response_text({"content": [text(valid, "commentary"), text(valid, "final_answer"), text(valid, "final_answer")]})
        self.assertEqual(valid, module.select_final_response_text({"content": [text(valid)]}))
        with self.assertRaisesRegex(ValueError, "exactly one unphased"):
            module.select_final_response_text({"content": [text("{\\\"summary\\\": \\\"not a response\\\"}"), text(valid)]})

    def test_invalid_phased_response_retains_transcript_and_selected_raw_evidence(self):
        db = self.seed_queue(1)
        api = self.FakeAPI()
        valid = json.dumps({"completed": ["id-0"], "unresolved": [], "findings": [], "summary": "ok"})
        api.send = lambda sid, prompt: api.responses.update({sid: {"data": [{"content": [
            {"type": "text", "text": "commentary", "state": {"phase": "commentary"}},
            {"type": "text", "text": valid, "state": {"phase": "final_answer"}}]}]}})
        original = module.select_final_response_text
        with patch.object(module, "select_final_response_text", side_effect=lambda message: (original(message) + " invalid")):
            module.run(db=db, root=self.root, workers=1, max_tasks=1, retries=0, api=api)
        c = module.connect(db)
        attempt = c.execute("SELECT transcript,response,error FROM attempts").fetchone(); c.close()
        self.assertIn("commentary", attempt["transcript"])
        self.assertEqual(valid + " invalid", attempt["response"])
        self.assertIn("Extra data", attempt["error"])

    def test_artifact_stop_is_independent_and_requires_explicit_resume(self):
        db = self.seed_queue(1)
        source_stop = self.root / "doc-prune-work/campaign/STOP"
        artifact_stop = self.root / "doc-prune-work/campaign/ARTIFACT_STOP"
        source_stop.write_text("source scheduler stop\n")
        artifact_stop.write_text("artifact stop\n")
        api = self.FakeAPI()
        stopped = module.run(db=db, root=self.root, workers=1, api=api)
        self.assertEqual("stopped", stopped["state"])
        self.assertEqual(0, api.calls)
        self.assertTrue(source_stop.exists())
        self.assertTrue(artifact_stop.exists())
        resumed = module.run(db=db, root=self.root, workers=1, max_tasks=1, api=api, resume=True)
        self.assertEqual(1, resumed["processed"])
        self.assertFalse(artifact_stop.exists())
        self.assertTrue(source_stop.exists())

    def test_timeout_retries_are_attempt_bounded(self):
        db = self.seed_queue(1)
        api = self.FakeAPI(fail_once=True)
        got = module.run(db=db, root=self.root, workers=1, max_tasks=2, retries=1, api=api)
        self.assertEqual(2, got["processed"])
        c = module.connect(db)
        row = c.execute("SELECT state,attempts FROM tasks").fetchone(); c.close()
        self.assertEqual(("passed", 2), (row["state"], row["attempts"]))

    def test_manual_retry_selects_only_named_blocked_task_once_and_preserves_evidence(self):
        db = self.seed_queue(3)
        c = module.connect(db)
        c.execute("UPDATE tasks SET state='blocked',attempts=7,last_error='quota evidence',findings=?,unresolved=? WHERE id='task-1'",
                  (json.dumps([{"id": "id-1", "reason": "existing finding"}]), json.dumps([{"id": "id-1", "reason": "existing uncertainty"}])))
        c.execute("INSERT INTO attempts(task_id,started,error,response) VALUES('task-1',1,'prior attempt error','prior response')")
        c.commit(); c.close()
        api = self.FakeAPI()
        result = module.run(db=db, root=self.root, workers=2, retries=99, api=api, manual_retry_ids=["task-1"])
        self.assertEqual(1, result["processed"])
        self.assertEqual(1, api.calls)
        c = module.connect(db)
        rows = {row["id"]: row for row in c.execute("SELECT id,state,attempts,last_error,findings,unresolved FROM tasks")}
        attempts = c.execute("SELECT error,response FROM attempts WHERE task_id='task-1' ORDER BY id").fetchall()
        c.close()
        self.assertEqual(("blocked", 8), (rows["task-1"]["state"], rows["task-1"]["attempts"]))
        self.assertIsNone(rows["task-1"]["last_error"])
        self.assertEqual("existing finding", json.loads(rows["task-1"]["findings"])[0]["reason"])
        self.assertEqual("existing uncertainty", json.loads(rows["task-1"]["unresolved"])[0]["reason"])
        self.assertEqual(("prior attempt error", "prior response"), (attempts[0]["error"], attempts[0]["response"]))
        self.assertEqual(("pending", 0), (rows["task-0"]["state"], rows["task-0"]["attempts"]))
        self.assertEqual(("pending", 0), (rows["task-2"]["state"], rows["task-2"]["attempts"]))

    def test_manual_retry_rejects_noncurrent_or_nonblocked_ids_before_dispatch(self):
        db = self.seed_queue(2)
        c = module.connect(db)
        c.execute("UPDATE tasks SET state='passed' WHERE id='task-0'")
        c.execute("INSERT INTO tasks(id,source,target,packet,state,generation) VALUES('old-task','old.java','1.21.5','none','blocked','old-generation')")
        c.commit(); c.close()
        for ids in (["unknown"], ["old-task"], ["task-0"], ["task-1", "task-1"]):
            with self.subTest(ids=ids):
                api = self.FakeAPI()
                with self.assertRaises(ValueError):
                    module.run(db=db, root=self.root, workers=1, api=api, manual_retry_ids=ids)
                self.assertEqual(0, api.calls)

    def test_recovery_does_not_redispatch_unconfirmed_live_session(self):
        db = self.seed_queue(1, running_first=True, session_first="old-session")
        api = self.FakeAPI(active_recovery=True)
        got = module.run(db=db, root=self.root, workers=1, retries=1, api=api)
        self.assertEqual(0, got["processed"])
        c = module.connect(db)
        row = c.execute("SELECT state,session_id FROM tasks").fetchone(); c.close()
        self.assertEqual(("blocked", "old-session"), (row["state"], row["session_id"]))

    def test_cross_target_bundle_deduplicates_exact_fragments_only(self):
        out = self.root / "bundles"; out.mkdir()
        common = {"name": "run", "signature": "run(x: number): void", "text": "same prose"}
        changed_type = {"name": "run", "signature": "run(x: string): void", "text": "same prose"}
        groups = []
        for target, fragment in (("1.21.5", common), ("1.21.8", common), ("1.21.10", changed_type)):
            groups.append({"source": "src/Thing.java", "source_sha256": "source-hash", "qualified_type": "api.Thing",
                           "group_index": 0, "target": target, "source_contract": [{"key": "member:run:signature", "name": "run"}],
                           "assigned_ids": ["member:run:signature"], "artifacts": {"TypeScript": {"state": "present", "files": [{"path": f"{target}/Thing.d.ts", "sha256": target, "fragments": [fragment]}]},
                           "HTML": {"state": "present", "files": [{"path": f"{target}/Thing.html", "sha256": f"html-{target}", "fragments": [{"name": "Thing", "kind": "page", "text": "same visible page prose"}]}]}},
                           "output_snapshot": {"TypeScript": {"state": "present", "files": [{"path": f"{target}/Thing.d.ts", "sha256": target}]},
                                               "HTML": {"state": "present", "files": [{"path": f"{target}/Thing.html", "sha256": f"html-{target}"}]}},
                           "format_errors": {}, "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []})
        tasks = module.write_bundles(out, "generation-x", groups)
        self.assertEqual(1, len(tasks))
        bundle = json.loads((out / tasks[0]["packet"]).read_text())
        self.assertEqual({"1.21.5", "1.21.8", "1.21.10"}, {item["target"] for item in bundle["targets"]})
        self.assertEqual(5, len(bundle["extracted_fragments"]))
        html_fragments = [item for item in bundle["extracted_fragments"] if item["references"][0]["format"] == "HTML"]
        self.assertEqual(3, len(html_fragments), "different HTML files must not collapse based only on extracted text")
        self.assertTrue(all(item["fragment"]["text"] == "same visible page prose" for item in html_fragments))
        self.assertEqual(3, len({item["references"][0]["path"] for item in html_fragments}))
        self.assertEqual(3, len({file["path"] for target in bundle["targets"] for file in target["output_snapshot"]["HTML"]["files"]}))
        self.assertEqual({"1.21.5::member:run:signature", "1.21.8::member:run:signature", "1.21.10::member:run:signature"}, set(bundle["assigned_ids"]))
        singles = module.write_bundles(out, "generation-y", groups, bundle_targets=False)
        self.assertEqual(3, len(singles))
        self.assertEqual(3, len({item["packet"] for item in singles}))

    def test_small_five_target_bundle_remains_one_task(self):
        out = self.root / "small-five"; out.mkdir()
        groups = [{"source": "Thing.java", "source_sha256": "source", "qualified_type": "api.Thing", "group_index": 0,
                   "target": target, "source_contract": [{"key": "type:api.Thing", "name": "Thing"}],
                   "assigned_ids": ["type:api.Thing"], "artifacts": {}, "output_snapshot": {}, "format_errors": {},
                   "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []}
                  for target in ("1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2")]
        tasks = module.write_bundles(out, "small-five-generation", groups)
        self.assertEqual(1, len(tasks))
        bundle = json.loads((out / tasks[0]["packet"]).read_text())
        self.assertEqual(5, len(bundle["targets"]))
        self.assertEqual(5, len(bundle["assigned_ids"]))

    def test_five_large_target_pages_shard_with_exact_coverage_and_unique_files(self):
        out = self.root / "large-bundles"; out.mkdir()
        groups = []
        for target in ("1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2"):
            raw = f"## InteractionManager {target}\n" + (f"{target} /method/link/ with distinct metadata\n" * 1800)
            groups.append({"source": "InteractionManager.java", "source_sha256": "same-source", "qualified_type": "api.InteractionManager",
                           "group_index": 2, "target": target, "source_contract": [{"key": "member:method", "name": "method"}],
                           "assigned_ids": ["member:method"],
                           "artifacts": {"VitePress": {"files": [{"path": f"{target}/InteractionManager.md", "sha256": f"file-{target}",
                               "fragments": [{"name": "method", "kind": "member", "raw_markdown": raw, "text": raw, "links": [target]}]}]}},
                           "output_snapshot": {"VitePress": {"state": "present", "files": [{"path": f"{target}/InteractionManager.md", "sha256": f"file-{target}"}]}},
                           "format_errors": {}, "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []})
        tasks = module.write_bundles(out, "g-large", groups)
        self.assertGreater(len(tasks), 1)
        self.assertEqual(len(tasks), len({task["packet"] for task in tasks}))
        covered = []
        for task in tasks:
            bundle = json.loads((out / task["packet"]).read_text())
            covered.extend(view["target"] for view in bundle["targets"])
            self.assertLessEqual(module.estimate_wire_bytes(bundle), module.MAX_WIRE_BYTES)
            if len(bundle["targets"]) > 1:
                self.assertLessEqual(module.inline_wire_bytes(bundle), module.PREFERRED_INLINE_BYTES)
            wire = module.wire_packet(bundle, module.wire_aliases(bundle["assigned_ids"]))
            self.assertLessEqual(len(module.serialize_packet_parts(wire, module.wire_aliases(bundle["assigned_ids"]))), module.MAX_PROMPT_PARTS)
            for extracted in bundle["extracted_fragments"]:
                self.assertIn(extracted["fragment"]["raw_markdown"], extracted["fragment"]["text"])
                self.assertTrue(extracted["references"][0]["file_sha256"].startswith("file-"))
            wire = module.wire_packet(bundle, module.wire_aliases(bundle["assigned_ids"]))
            for extracted in wire["extracted_fragments"]:
                self.assertNotIn("text", extracted["fragment"])
                self.assertTrue(extracted["fragment"]["raw_markdown"])
        self.assertEqual(sorted(group["target"] for group in groups), sorted(covered))

    def test_large_alias_maps_drive_multitarget_sharding_by_actual_parts(self):
        out = self.root / "many-alias-bundles"; out.mkdir()
        groups = []
        targets = ("1.21.5", "1.21.8", "1.21.10", "1.21.11", "26.1.2")
        keys = [f"member:{i:03d}:" + ("x" * 250) for i in range(100)]
        for target in targets:
            entries = [{"key": key, "name": "member"} for key in keys]
            groups.append({"source": "Thing.java", "source_sha256": "same-source", "qualified_type": "api.Thing",
                           "group_index": 0, "target": target, "source_contract": entries,
                           "assigned_ids": keys, "artifacts": {}, "output_snapshot": {}, "format_errors": {},
                           "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []})
        tasks = module.write_bundles(out, "many-alias-generation", groups)
        self.assertGreater(len(tasks), 1)
        seen = []
        for task in tasks:
            bundle = json.loads((out / task["packet"]).read_text())
            self.assertLessEqual(module.estimate_wire_bytes(bundle), module.MAX_WIRE_BYTES)
            aliases = module.wire_aliases(bundle["assigned_ids"])
            wire = module.wire_packet(bundle, aliases)
            parts = module.serialize_packet_parts(wire, aliases)
            self.assertLessEqual(len(parts), module.MAX_PROMPT_PARTS)
            self.assertTrue(all("id_map" not in json.loads(part) for part in parts))
            seen.extend(bundle["assigned_ids"])
        self.assertEqual(len(seen), len(set(seen)))
        self.assertEqual(sorted(f"{target}::{key}" for target in targets for key in keys), sorted(seen))

    def test_one_oversize_target_is_persisted_as_blocker_without_truncation(self):
        out = self.root / "one-oversize"; out.mkdir()
        raw = "# One huge member\n" + ("verbatim material " * 18000)
        group = {"source": "Thing.java", "source_sha256": "source", "qualified_type": "api.Thing", "group_index": 7,
                 "target": "1.21.5", "source_contract": [{"key": "member:x", "name": "x"}], "assigned_ids": ["member:x"],
                 "artifacts": {"VitePress": {"files": [{"path": "Thing.md", "sha256": "markdown-hash", "fragments": [{"name": "x", "kind": "member", "raw_markdown": raw, "text": raw}]}]}},
                 "output_snapshot": {}, "format_errors": {}, "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []}
        tasks = module.write_bundles(out, "g-oversize", [group])
        self.assertEqual(1, len(tasks)); self.assertGreater(tasks[0]["oversize_bytes"], module.MAX_WIRE_BYTES)
        packet = json.loads((out / tasks[0]["packet"]).read_text())
        self.assertEqual(raw, packet["extracted_fragments"][0]["fragment"]["raw_markdown"])
        self.assertIn("not dispatched or truncated", packet["oversize_blocker"]["reason"])
        db = self.root / "oversize.sqlite3"; conn = module.connect(db)
        module.insert_prepared_tasks(conn, tasks); conn.commit()
        state, error = conn.execute("SELECT state,last_error FROM tasks").fetchone(); conn.close()
        self.assertEqual("blocked", state); self.assertIn("not dispatched or truncated", error)

    def test_oversized_name_chunk_splits_at_member_boundaries(self):
        entries = [{"key": f"member:m{i}", "name": f"m{i}", "kind": "member"} for i in range(10)]
        fragments = [{"name": f"m{i}", "kind": "member", "raw_markdown": "x" * 18000, "text": "x" * 18000}
                     for i in range(10)]
        packet = {"source": "Thing.java", "source_sha256": "s", "qualified_type": "api.Thing", "target": "1.21.5",
                  "source_contract": entries, "artifacts": {"VitePress": {"state": "present", "files": [
                      {"path": "Thing.md", "sha256": "h", "fragments": fragments}]}}, "output_snapshot": {},
                  "format_errors": {}, "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []}
        original = module.build_member_group(packet, 0, entries)
        chunks = module.split_oversized_member_group(packet, original)
        self.assertGreaterEqual(len(chunks), 2)
        self.assertTrue(all(module.estimate_wire_bytes(module._make_bundle(packet["source"], g["group_index"], [g])) <= module.MAX_WIRE_BYTES for g in chunks))
        assigned = [key for g in chunks for key in g["assigned_ids"]]
        self.assertEqual([e["key"] for e in entries], assigned)
        self.assertEqual(len(assigned), len(set(assigned)))
        self.assertEqual({e["name"] for e in entries}, {f["name"] for g in chunks for file in g["artifacts"]["VitePress"]["files"] for f in file["fragments"]})
        self.assertTrue(all(module.inline_wire_bytes(module._make_bundle(packet["source"], g["group_index"], [g])) <= module.PREFERRED_INLINE_BYTES for g in chunks))

    def test_oversized_overload_name_group_stays_atomic(self):
        entries = [{"key": f"member:x:{i}", "name": "x", "kind": "member"} for i in range(8)]
        packet = {"source": "Thing.java", "source_sha256": "s", "qualified_type": "api.Thing", "target": "1.21.5",
                  "source_contract": entries, "artifacts": {"VitePress": {"files": [{"path": "Thing.md", "sha256": "h",
                       "fragments": [{"name": "x", "kind": "member", "raw_markdown": "z" * 70000, "text": "z" * 70000}]}]}},
                  "output_snapshot": {}, "format_errors": {}, "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []}
        original = module.build_member_group(packet, 4, entries)
        chunks = module.split_oversized_member_group(packet, original)
        self.assertEqual(1, len(chunks))
        self.assertEqual([e["key"] for e in entries], chunks[0]["assigned_ids"])
        size = module.inline_wire_bytes(module._make_bundle(packet["source"], chunks[0]["group_index"], chunks))
        self.assertGreater(size, module.PREFERRED_INLINE_BYTES)
        self.assertLessEqual(module.estimate_wire_bytes(module._make_bundle(packet["source"], chunks[0]["group_index"], chunks)), module.MAX_WIRE_BYTES)

    def test_missing_one_bundled_target_blocks_without_erasing_target_provenance(self):
        db = self.seed_queue(1)
        c = module.connect(db)
        row = c.execute("SELECT packet FROM tasks").fetchone()
        out = Path(c.execute("SELECT value FROM meta WHERE key='packet_dir'").fetchone()[0])
        packet = json.loads((out / row["packet"]).read_text())
        packet["assigned_ids"] = ["1.21.5::id", "1.21.8::id"]
        packet["targets"] = [{"target": "1.21.5", "assigned_ids": ["1.21.5::id"], "artifacts": {}, "output_snapshot": packet["output_snapshot"], "format_errors": {}, "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []},
                              {"target": "1.21.8", "assigned_ids": ["1.21.8::id"], "artifacts": {}, "output_snapshot": {**packet["output_snapshot"], "Python": {"state": "missing", "files": []}}, "format_errors": {}, "missing_formats": ["Python"], "fragment_gaps": {}, "ambiguous_output_names": []}]
        (out / row["packet"]).write_text(json.dumps(packet)); c.close()
        result = module.run(db=db, root=self.root, workers=1, max_tasks=1, retries=0, api=self.FakeAPI())
        self.assertEqual(1, result["processed"])
        c = module.connect(db); state = c.execute("SELECT state FROM tasks").fetchone()[0]; c.close()
        self.assertEqual("blocked", state)

    def test_partial_bundle_retry_reviews_only_remaining_target_ids(self):
        db = self.seed_queue(1)
        c = module.connect(db); row = c.execute("SELECT packet FROM tasks").fetchone()
        out = Path(c.execute("SELECT value FROM meta WHERE key='packet_dir'").fetchone()[0])
        packet = json.loads((out / row["packet"]).read_text())
        snapshot = packet["output_snapshot"]
        packet["assigned_ids"] = ["1.21.5::member", "1.21.8::member"]
        packet["source_contract"] = [{"key": "member", "name": "run"}]
        packet["targets"] = [{"target": target, "assigned_ids": [f"{target}::member"], "artifacts": {}, "output_snapshot": snapshot,
                              "format_errors": {}, "missing_formats": [], "fragment_gaps": {}, "ambiguous_output_names": []}
                             for target in ("1.21.5", "1.21.8")]
        (out / row["packet"]).write_text(json.dumps(packet)); c.close()
        api = self.FakeAPI()
        result = module.run(db=db, root=self.root, workers=1, max_tasks=2, retries=1, api=api)
        self.assertEqual(2, result["processed"])
        self.assertEqual([["a001", "a002"], ["a001"]], api.allowed_ids)
        self.assertIn('"a001": "1.21.8::member"', api.prompts[1])
        self.assertNotIn('"a001": "1.21.5::member"', api.prompts[1])
        c = module.connect(db); row = c.execute("SELECT state,completed FROM tasks").fetchone(); c.close()
        self.assertEqual("passed", row["state"])
        self.assertEqual({"1.21.5::member", "1.21.8::member"}, set(json.loads(row["completed"])))

    def test_reprepare_supersedes_only_replaced_same_scope_task(self):
        db = self.root / "scope.sqlite3"
        c = module.connect(db)
        scope0 = module.digest(b"same-source-targets-group-0")
        scope1 = module.digest(b"same-source-targets-group-1")
        first = [
            {"id": "old-group-0", "source": "Thing.java", "target": '["1.21.5","1.21.8"]', "packet": "old0.json", "generation": "g", "scope_key": scope0},
            {"id": "stable-group-1", "source": "Thing.java", "target": '["1.21.5","1.21.8"]', "packet": "old1.json", "generation": "g", "scope_key": scope1},
        ]
        module.insert_prepared_tasks(c, first)
        c.execute("UPDATE tasks SET state='blocked',findings='[{\"id\":\"1.21.5::x\",\"reason\":\"review evidence\"}]' WHERE id='old-group-0'")
        c.execute("INSERT INTO attempts(task_id,started,response) VALUES('old-group-0',1,'old transcript evidence')")
        rebuilt = [
            {"id": "new-group-0", "source": "Thing.java", "target": '["1.21.5","1.21.8"]', "packet": "new0.json", "generation": "g", "scope_key": scope0},
            first[1],
        ]
        module.insert_prepared_tasks(c, rebuilt); c.commit()
        states = dict(c.execute("SELECT id,state FROM tasks"))
        self.assertEqual({"old-group-0": "superseded", "new-group-0": "pending", "stable-group-1": "pending"}, states)
        self.assertEqual(1, c.execute("SELECT count(*) FROM tasks WHERE scope_key=? AND state IN ('pending','retry')", (scope0,)).fetchone()[0])
        self.assertEqual(1, c.execute("SELECT count(*) FROM attempts WHERE task_id='old-group-0'").fetchone()[0])
        self.assertIn("review evidence", c.execute("SELECT findings FROM tasks WHERE id='old-group-0'").fetchone()[0])
        c.close()

    def test_status_handles_empty_queue(self):
        db = self.root / "empty.sqlite3"
        got = module.status(db)
        self.assertEqual(0, got["total"])
        self.assertEqual({}, got["tasks"])


if __name__ == "__main__":
    unittest.main()
