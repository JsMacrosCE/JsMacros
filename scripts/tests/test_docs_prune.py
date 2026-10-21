import json
from pathlib import Path
import tempfile
import threading
import time
import unittest
from unittest.mock import patch

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import docs_prune as dp


class DocsPruneTests(unittest.TestCase):
    def test_lexer_ignores_comment_markers_in_strings_text_blocks_and_line_comments(self):
        source = '''class X {
 String a = "/** nope */";
 String b = """ /** not doc */ """;
 // /** not doc */
 /** yes */ void a() {}
}'''
        self.assertEqual([x[2] for x in dp.lexical_comments(source)], ["/** yes */"])

    def test_fingerprint_ignores_comments_but_not_code_or_literals(self):
        a = b'class X { /** a */ int x=1; String s="//"; }'
        b = b'class X { /** changed */ int x=1; String s="//"; }'
        c = b'class X { /** a */ int x=2; String s="//"; }'
        self.assertEqual(dp.code_fingerprint(a), dp.code_fingerprint(b))
        self.assertNotEqual(dp.code_fingerprint(a), dp.code_fingerprint(c))

    def test_protected_fingerprint_preserves_guards_and_non_javadoc_comments(self):
        before = b"//? if <1.21.8>\n/* inactive Java { body } */\n/** prose */ int x;\n"
        allowed = b"//? if <1.21.8>\n/* inactive Java { body } */\n/** shorter */ int x;\n"
        guard_changed = b"//? if >=1.21.8\n/* inactive Java { body } */\n/** shorter */ int x;\n"
        block_changed = b"//? if <1.21.8>\n/* altered inactive Java {} */\n/** shorter */ int x;\n"
        self.assertEqual(dp.protected_source_fingerprint(before, {0}), dp.protected_source_fingerprint(allowed, {0}))
        self.assertNotEqual(dp.protected_source_fingerprint(before, {0}), dp.protected_source_fingerprint(guard_changed, {0}))
        self.assertNotEqual(dp.protected_source_fingerprint(before, {0}), dp.protected_source_fingerprint(block_changed, {0}))

    def test_since_signature_catches_removed_valid_or_empty_tag(self):
        valid = "/** Contract. @since 1.2.3 */"
        empty = "/** Contract. @since */"
        self.assertNotEqual(dp.since_signature(valid), dp.since_signature("/** Contract. */"))
        self.assertNotEqual(dp.since_signature(empty), dp.since_signature("/** Contract. */"))
        self.assertNotEqual(dp.since_signature(empty), dp.since_signature("/** Contract. @since 1.0 */"))

    def test_tasks_bounded_and_stable_for_line_insertions(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); path = root / "A.java"
            path.write_text("\n".join(f"/** comment {i} */\nvoid m{i}() {{}}" for i in range(25)))
            before = dp.comment_tasks(path, root)
            path.write_text("// added\n" + path.read_text())
            after = dp.comment_tasks(path, root)
            self.assertEqual([x["id"] for x in before], [x["id"] for x in after])
            self.assertTrue(all(len(t["comment_ids"]) <= dp.MAX_COMMENTS for t in after))

    def test_selected_prompt_context_disambiguates_duplicate_docs_and_bounds_large_files(self):
        members = [f"/** Same getter docs. */\npublic int getValue{i}() {{ return {i}; }}\n" for i in range(40)]
        members.append("public void large() {\n" + "  work();\n" * 800 + "}\n")
        source = "class A {\n" + "".join(members) + "}\n"
        selected = dp.selected_comment_context(source, "A.java", ["A.java#javadoc-2"])
        self.assertEqual(selected[0]["javadoc"], "/** Same getter docs. */")
        self.assertIn("getValue1", selected[0]["following_source_excerpt"])
        self.assertEqual(selected[0]["line_start"], 4)
        large_id = "A.java#javadoc-40"
        excerpt = dp.selected_comment_context(source, "A.java", [large_id])[0]["following_source_excerpt"]
        self.assertLessEqual(len(excerpt), 1100)
        self.assertIn("excerpt clipped", excerpt)

    def test_init_saves_dirty_source_as_immutable_baseline(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); src = root / "common/src/main/java/A.java"
            src.parent.mkdir(parents=True); src.write_text("/** dirty prose */ class A {}")
            db = root / "doc-prune-work/campaign/db.sqlite"
            dp.init_campaign(root, db)
            src.write_text("/** later edit */ class A {}")
            with self.assertRaisesRegex(RuntimeError, "changed outside recorded campaign state"):
                dp.init_campaign(root, db)
            baseline = db.parent / "baseline/common/src/main/java/A.java"
            self.assertEqual(baseline.read_text(), "/** dirty prose */ class A {}")

    def test_reinit_preserves_original_task_partition_after_comment_pruning(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); src = root / "common/src/main/java/A.java"
            src.parent.mkdir(parents=True)
            original = "/** " + "a" * 5500 + " */\n/** " + "b" * 5500 + " */\nclass A {}"
            src.write_text(original)
            db = root / "doc-prune-work/campaign/db.sqlite"
            dp.init_campaign(root, db)
            c = dp.connect(db)
            old_ids = [row[0] for row in c.execute("SELECT id FROM tasks ORDER BY id")]
            self.assertEqual(len(old_ids), 2)
            initialized = c.execute("SELECT value FROM meta WHERE key='initialized'").fetchone()[0]
            c.close()
            pruned = "/** concise */\n/** concise */\nclass A {}"
            src.write_text(pruned)
            c = dp.connect(db); c.execute("UPDATE sources SET current_hash=? WHERE path='common/src/main/java/A.java'", (dp.sha(pruned.encode()),)); c.commit(); c.close()
            result = dp.init_campaign(root, db)
            c = dp.connect(db)
            self.assertEqual([row[0] for row in c.execute("SELECT id FROM tasks ORDER BY id")], old_ids)
            self.assertEqual(c.execute("SELECT value FROM meta WHERE key='initialized'").fetchone()[0], initialized)
            c.close()
            self.assertEqual(result["tasks"], 2)
            self.assertEqual((db.parent / "baseline/common/src/main/java/A.java").read_text(), original)

    def test_inventory_fallback_excludes_generated_extension_java_and_inventory_rejects_it(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            shared = root / "extension/demo/src/main/java/A.java"
            generated = root / "extension/demo/build/generated/A.java"
            shared.parent.mkdir(parents=True); generated.parent.mkdir(parents=True)
            shared.write_text("/** shared */ class A {}")
            generated.write_text("/** generated */ class Generated {}")
            self.assertEqual(dp.source_files(root), [shared.resolve()])
            inventory = root / "doc-prune-work/inventory-sources.txt"
            inventory.parent.mkdir(parents=True); inventory.write_text("extension/demo/build/generated/A.java\n")
            with self.assertRaisesRegex(ValueError, "not a shared Java source"):
                dp.source_files(root)

    def test_legacy_inventory_is_not_required_and_task_states_are_distinct(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); src = root / "extension/a/src/main/java/A.java"
            src.parent.mkdir(parents=True); src.write_text("/** docs */ class A {}")
            db = root / "campaign/db.sqlite"; dp.init_campaign(root, db)
            result = dp.status(db)
            self.assertEqual(result["sources"], 1)
            self.assertEqual(result["tasks"], {"pending": 1})
            c = dp.connect(db)
            cols = {r[1] for r in c.execute("pragma table_info(tasks)")}
            self.assertTrue({"edit_state", "review_state", "evidence_state", "artifact_state"} <= cols)
            c.close()

    def test_strict_json_accepts_subset_and_rejects_malformed_or_unknown(self):
        value = json.dumps({"completed": ["a"], "unresolved": [], "findings": [], "summary": "ok"})
        self.assertEqual(dp.parse_response(value, ["a", "b"])["completed"], ["a"])
        self.assertEqual(dp.parse_response("  \n" + value + " \n", ["a", "b"])["completed"], ["a"])
        for raw in ("preface " + value, value + " trailing", value.replace('["a"]', '["x"]'), "{}"):
            with self.subTest(raw=raw), self.assertRaises(ValueError): dp.parse_response(raw, ["a", "b"])

    def test_wrong_returned_model_is_rejected(self):
        api = dp.API("opencode")
        response = {"data": {"id": "ses-1", "model": {"providerID": "openai", "id": "other", "variant": "default"}}}
        with patch.object(api, "request", return_value=response), self.assertRaisesRegex(RuntimeError, "model mismatch"):
            api.create_session(Path("/project"), "test", "docs-editor", None)

    def test_actionable_findings_require_assigned_ids_and_reasons(self):
        good = json.dumps({"completed": [], "unresolved": [], "findings": [{"id": "a", "reason": "unsupported claim"}], "summary": "needs work"})
        self.assertEqual(dp.parse_response(good, ["a"])["findings"][0]["id"], "a")
        for findings in (["positive note"], [{"id": "other", "reason": "wrong ID"}], [{"id": "a", "reason": ""}]):
            raw = json.dumps({"completed": [], "unresolved": [], "findings": findings, "summary": "bad"})
            with self.subTest(findings=findings), self.assertRaises(ValueError): dp.parse_response(raw, ["a"])

    def test_file_code_modification_is_detectable_and_never_restored(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); path = root / "A.java"; original = b"/** prose */ class A { int x; }"
            path.write_bytes(original); fingerprint = dp.code_fingerprint(original)
            path.write_bytes(b"/** prose */ class A { int y; }")
            self.assertNotEqual(dp.code_fingerprint(path.read_bytes()), fingerprint)
            self.assertEqual(path.read_bytes(), b"/** prose */ class A { int y; }")

    def test_acquire_uses_atomic_exclusive_lease_and_does_not_reclaim_running(self):
        with tempfile.TemporaryDirectory() as d:
            db = Path(d) / "c.sqlite"; c = dp.connect(db)
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash) VALUES('t','A.java','[]','[]','[]','s','c')")
            first = dp.acquire(c, "one", 1); self.assertEqual(first["id"], "t")
            self.assertIsNone(dp.acquire(c, "two", 1))
            c.close()

    def test_review_attempt_budget_exhausts_without_reclaim_loop(self):
        with tempfile.TemporaryDirectory() as d:
            c = dp.connect(Path(d) / "campaign.sqlite")
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,state,review_attempts) VALUES('t','A.java','[]','[]','[]','s','c','awaiting_review',0)")
            self.assertEqual(dp.acquire(c, "one", retries=0)["id"], "t")
            c.execute("DELETE FROM file_leases WHERE path='A.java'"); c.execute("UPDATE tasks SET state='awaiting_review' WHERE id='t'"); c.commit()
            self.assertIsNone(dp.acquire(c, "two", retries=0))
            self.assertEqual(c.execute("select review_attempts from tasks").fetchone()[0], 1)
            c.close()

    def test_legacy_provenance_imported_separately_from_completion(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); src = root / "common/src/main/java/A.java"
            src.parent.mkdir(parents=True); src.write_text("/** docs */ class A {}")
            ledger = root / "doc-prune-work/review-ledger-20261004.tsv"
            ledger.parent.mkdir(parents=True)
            ledger.write_text("status\tjavadoc_blocks\tsource\naudited\t1\tcommon/src/main/java/A.java\n")
            db = root / "campaign/c.sqlite"; dp.init_campaign(root, db)
            c = dp.connect(db)
            source = c.execute("select legacy_status from sources").fetchone()[0]
            task_state = c.execute("select state from tasks").fetchone()[0]
            c.close()
            self.assertEqual(source, "audited")
            self.assertEqual(task_state, "pending")

    def test_cli_api_uses_subprocess_cwd_timeout_and_v2_payload(self):
        api = dp.API("opencode")
        completed = type("Result", (), {"stdout": '{"data":{"ok":true}}'})()
        with patch("subprocess.run", return_value=completed) as run:
            self.assertEqual(api.request("POST", "/api/session", {"x": 1}, timeout=17), {"data": {"ok": True}})
            self.assertEqual(run.call_args.kwargs["cwd"], dp.ROOT)
            self.assertEqual(run.call_args.kwargs["timeout"], 17)
            self.assertEqual(run.call_args.args[0][1:4], ["api", "POST", "/api/session"])
            self.assertIn("--data", run.call_args.args[0])

    def test_session_payload_has_narrow_v2_permissions_and_exact_model(self):
        api = dp.API("opencode")
        response = {"data": {"id": "ses-1", "model": {**dp.DEFAULT_MODEL, "variant": "default"}}}
        with patch.object(api, "request", return_value=response) as request:
            session = api.create_session(Path("/project"), "test", "docs-editor", Path("/project/A.java"))
        self.assertEqual(session, "ses-1")
        payload = request.call_args.args[2]
        self.assertEqual(payload["model"], dp.DEFAULT_MODEL)
        self.assertEqual(payload["location"], {"directory": "/project"})
        rules = payload["permissions"]
        self.assertTrue(all(set(rule) == {"action", "resource", "effect"} for rule in rules))
        self.assertIn({"action": "edit", "resource": "/project/A.java", "effect": "allow"}, rules)
        self.assertIn({"action": "edit", "resource": "A.java", "effect": "allow"}, rules)
        self.assertEqual(rules[0], {"action": "*", "resource": "*", "effect": "deny"})

    def test_parallel_workers_overlap_but_file_leases_serialize_same_file(self):
        class FailingAPI:
            def __init__(self): self.active = 0; self.peak = 0; self.lock = threading.Lock()
            def create_session(self, *args):
                with self.lock:
                    self.active += 1; self.peak = max(self.peak, self.active)
                time.sleep(.04)
                with self.lock: self.active -= 1
                raise RuntimeError("offline fake")
            def request(self, *args, **kwargs): return None

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; c = dp.connect(db)
            for name in ("A.java", "B.java"):
                data = b"/** doc */ class X {}"
                (root / name).write_bytes(data)
                task = dp.comment_tasks(root / name, root)[0]
                c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash) VALUES(?,?,?,?,?,?,?)",
                          (task["id"], name, json.dumps(task["comment_ids"]), json.dumps(task["comments"]), json.dumps(task["lines"]), dp.sha(data), task["code_fingerprint"]))
            c.commit(); c.close(); api = FailingAPI()
            dp.run_campaign(db, max_tasks=2, workers=2, reviewers=1, retries=0, api=api)
            self.assertEqual(api.peak, 2)
        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; c = dp.connect(db); path = root / "A.java"
            data = b"/** one */ /** two */ class X {}"; path.write_bytes(data)
            for task in dp.comment_tasks(path, root):
                # force independent tasks for one file to exercise file-level lock
                pass
            for n in (1, 2):
                c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash) VALUES(?,?,?,?,?,?,?)",
                          (f"t{n}", "A.java", json.dumps([f"A.java#javadoc-{n}"]), json.dumps([f"/** {n} */"]), "[1]", dp.sha(data), dp.code_fingerprint(data)))
            c.commit(); c.close(); api = FailingAPI()
            dp.run_campaign(db, max_tasks=2, workers=2, reviewers=1, retries=0, api=api)
            self.assertEqual(api.peak, 1)

    def test_review_checkpoint_resumes_without_an_editor_session(self):
        class ReviewAPI:
            def __init__(self): self.created = []; self.sent = []
            def create_session(self, directory, title, agent, edit_path):
                self.created.append((agent, edit_path)); return "ses-review"
            def send(self, session, text): self.sent.append(text)
            def request(self, method, path, payload=None, timeout=30):
                if path.endswith("/wait"): return None
                if path == "/api/session/ses-review": return {"data": {"outcome": "succeeded"}}
                if "/message?" in path:
                    return {"data": [{"content": [{"type": "text", "text": json.dumps({"completed": ["A.java#javadoc-1"], "unresolved": [], "findings": [], "summary": "ok"})}]}]}
                raise AssertionError(path)

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; path = root / "A.java"
            current = b"/** edited */ class A {}"; path.write_bytes(current)
            c = dp.connect(db); ids = ["A.java#javadoc-1"]
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,state,edit_state,review_state,after_hash,pending_completed,pending_unresolved) VALUES('t','A.java',?,?,?,?,?,'awaiting_review','done','running',?,?,?)",
                      (json.dumps(ids), json.dumps(["/** edited */"]), "[1]", dp.sha(current), dp.code_fingerprint(current), dp.sha(current), json.dumps(ids), "[]"))
            c.execute("INSERT INTO diffs VALUES('t',?,?,?,?)", (dp.sha(b"/** before */ class A {}"), dp.sha(current), "--- before\n+++ after\n", time.time()))
            c.commit(); c.close(); api = ReviewAPI()
            result = dp.run_campaign(db, max_tasks=1, workers=1, reviewers=1, api=api)
            self.assertEqual(api.created, [("docs-reviewer", None)])
            self.assertEqual(result["tasks"], {"done": 1})

    def test_partial_editor_completion_requeues_only_exact_remaining_ids(self):
        class API:
            def create_session(self, *args): return "ses-review"
            def send(self, *args): pass
            def request(self, method, path, payload=None, timeout=30):
                if path.endswith("/wait"): return None
                if path == "/api/session/ses-review": return {"data": {"outcome": "succeeded"}}
                return {"data": [{"content": [{"type": "text", "text": json.dumps({"completed": ["A.java#javadoc-1"], "unresolved": [], "findings": [], "summary": "ok"})}]}]}

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; path = root / "A.java"
            source = b"/** edited one */ /** untouched two */ class A {}"; path.write_bytes(source)
            ids = ["A.java#javadoc-1", "A.java#javadoc-2"]
            c = dp.connect(db)
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,state,attempts,after_hash,pending_completed,pending_unresolved,pending_findings) VALUES('t','A.java',?,?,?, ?,?,'awaiting_review',1,?,?,?,?)",
                      (json.dumps(ids), json.dumps(["/** edited one */", "/** untouched two */"]), "[1,1]", dp.sha(source), dp.code_fingerprint(source), dp.sha(source), json.dumps([ids[0]]), json.dumps([{"id": ids[0], "reason": "needs exact behavior evidence"}]), "[]"))
            c.execute("INSERT INTO diffs VALUES('t',?,?,?,?)", (dp.sha(b"before"), dp.sha(source), "---\n+++\n", time.time()))
            c.execute("UPDATE tasks SET completed=? WHERE id='t'", (json.dumps(["A.java#javadoc-9"]),))
            task = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
            dp._finish_review(c, task, None, API(), "docs-reviewer", 5, threading.Semaphore(1), 1)
            updated = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
            self.assertEqual(json.loads(updated["comment_ids"]), [ids[1]])
            self.assertEqual(json.loads(updated["comments"]), ["/** untouched two */"])
            self.assertEqual(updated["state"], "pending")
            self.assertEqual(json.loads(updated["completed"]), ["A.java#javadoc-1", "A.java#javadoc-9"])
            self.assertEqual(c.execute("select count(*) from unresolved where resolved=0").fetchone()[0], 1)
            c.close()

    def test_direct_partial_review_charges_once_and_blocks_at_zero_remaining_budget(self):
        class PartialAPI:
            def create_session(self, *args): return "ses-review"
            def send(self, *args): pass
            def request(self, method, path, payload=None, timeout=30):
                if path.endswith("/wait"): return None
                if path == "/api/session/ses-review": return {"data": {"outcome": "succeeded"}}
                return {"data": [{"content": [{"type": "text", "text": json.dumps({
                    "completed": ["A.java#javadoc-1"], "unresolved": [], "findings": [], "summary": "partial"
                })}]}]}

        for retries, expected in ((0, "blocked"), (1, "pending")):
            with self.subTest(retries=retries), tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
                root = Path(d); db = root / "campaign/c.sqlite"; path = root / "A.java"
                source = b"/** one */ /** two */ class A {}"; path.write_bytes(source)
                ids = ["A.java#javadoc-1", "A.java#javadoc-2"]
                c = dp.connect(db)
                c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,attempts,after_hash,pending_completed) VALUES('t','A.java',?,?,?,?,?,1,?,?)",
                          (json.dumps(ids), json.dumps(["/** one */", "/** two */"]), "[1,1]", dp.sha(source), dp.code_fingerprint(source), dp.sha(source), json.dumps([ids[0]])))
                c.execute("INSERT INTO diffs VALUES('t',?,?,?,?)", (dp.sha(b"before"), dp.sha(source), "---\n+++\n", time.time()))
                task = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
                dp._finish_review(c, task, None, PartialAPI(), "docs-reviewer", 5, threading.Semaphore(1), retries)
                updated = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
                self.assertEqual(updated["review_attempts"], 1)
                self.assertEqual(updated["state"], expected)
                self.assertEqual(json.loads(updated["comment_ids"]), [ids[1]] if retries else ids)
                if retries == 1:
                    self.assertEqual(c.execute("SELECT 1 FROM tasks WHERE id='t' AND state IN ('pending','retry') AND review_attempts<2").fetchone()[0], 1)
                c.close()

    def test_no_diff_review_does_not_charge_review_attempt(self):
        class NoReviewAPI:
            def create_session(self, *args): raise AssertionError("no diff must not create a review session")

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; path = root / "A.java"
            source = b"/** unchanged */ class A {}"; path.write_bytes(source); ids = ["A.java#javadoc-1"]
            c = dp.connect(db)
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,after_hash,pending_completed) VALUES('t','A.java',?,?,?,?,?,?,?)",
                      (json.dumps(ids), json.dumps(["/** unchanged */"]), "[1]", dp.sha(source), dp.code_fingerprint(source), dp.sha(source), json.dumps(ids)))
            task = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
            dp._finish_review(c, task, None, NoReviewAPI(), "docs-reviewer", 5, threading.Semaphore(1), 0)
            updated = c.execute("SELECT state,review_attempts FROM tasks WHERE id='t'").fetchone()
            self.assertEqual(tuple(updated), ("done", 0))
            c.close()

    def test_external_edit_during_review_is_preserved_and_review_blocks(self):
        class MutatingAPI:
            def __init__(self, path): self.path = path; self.changed = False
            def create_session(self, *args): return "ses-review"
            def send(self, *args): pass
            def request(self, method, path, payload=None, timeout=30):
                if path.endswith("/wait"):
                    if not self.changed:
                        self.path.write_bytes(b"/** outside reviewer */ class A {}")
                        self.changed = True
                    return None
                if path == "/api/session/ses-review": return {"data": {"outcome": "succeeded"}}
                if "/message?" in path:
                    return {"data": [{"content": [{"type": "text", "text": json.dumps({"completed": ["A.java#javadoc-1"], "unresolved": [], "findings": [], "summary": "ok"})}]}]}
                return None

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; path = root / "A.java"
            original = b"/** edited */ class A {}"; path.write_bytes(original); ids = ["A.java#javadoc-1"]
            c = dp.connect(db)
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,state,edit_state,after_hash,pending_completed,pending_unresolved) VALUES('t','A.java',?,?,?,?,?,'awaiting_review','done',?,?,?)",
                      (json.dumps(ids), json.dumps(["/** edited */"]), "[1]", dp.sha(original), dp.code_fingerprint(original), dp.sha(original), json.dumps(ids), "[]"))
            c.execute("INSERT INTO diffs VALUES('t',?,?,?,?)", (dp.sha(b"before"), dp.sha(original), "---\n+++\n", time.time()))
            c.commit(); c.close()
            result = dp.run_campaign(db, max_tasks=1, workers=1, reviewers=1, retries=0, api=MutatingAPI(path))
            self.assertEqual(path.read_bytes(), b"/** outside reviewer */ class A {}")
            self.assertEqual(result["tasks"], {"blocked": 1})

    def test_checkpoint_source_conflict_blocks_without_dispatching_another_reviewer(self):
        class NoCallAPI:
            def create_session(self, *args): raise AssertionError("source conflict must not dispatch")
            def request(self, *args, **kwargs): return None

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; path = root / "A.java"
            expected = b"/** expected */ class A {}"; current = b"/** external change */ class A {}"; path.write_bytes(current)
            ids = ["A.java#javadoc-1"]
            c = dp.connect(db)
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,state,edit_state,after_hash,pending_completed,pending_unresolved) VALUES('t','A.java',?,?,?,?,?,'awaiting_review','done',?,?,?)",
                      (json.dumps(ids), json.dumps(["/** expected */"]), "[1]", dp.sha(expected), dp.code_fingerprint(expected), dp.sha(expected), json.dumps(ids), "[]"))
            c.execute("INSERT INTO diffs VALUES('t',?,?,?,?)", (dp.sha(expected), dp.sha(expected), "---\n+++\n", time.time()))
            c.commit(); c.close()
            result = dp.run_campaign(db, workers=1, reviewers=1, api=NoCallAPI())
            self.assertEqual(result["tasks"], {"blocked": 1})
            self.assertEqual(path.read_bytes(), current)

    def test_review_finding_requeues_only_its_id_and_keeps_other_completions(self):
        class FindingAPI:
            def create_session(self, *args): return "ses-review"
            def send(self, *args): pass
            def request(self, method, path, payload=None, timeout=30):
                if path.endswith("/wait"): return None
                if path == "/api/session/ses-review": return {"data": {"outcome": "succeeded"}}
                result = {"completed": ["A.java#javadoc-1", "A.java#javadoc-3"],
                          "unresolved": [], "findings": [{"id": "A.java#javadoc-2", "reason": "unsupported contract"}], "summary": "one finding"}
                return {"data": [{"content": [{"type": "text", "text": json.dumps(result)}]}]}

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; path = root / "A.java"
            source = b"/** one */ /** two */ /** three */ class A {}"; path.write_bytes(source)
            ids = [f"A.java#javadoc-{i}" for i in range(1, 4)]
            c = dp.connect(db)
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,state,attempts,review_attempts,after_hash,pending_completed,pending_unresolved) VALUES('t','A.java',?,?,?, ?,?,'awaiting_review',1,0,?,?,?)",
                      (json.dumps(ids), json.dumps(["/** one */", "/** two */", "/** three */"]), "[1,1,1]", dp.sha(source), dp.code_fingerprint(source), dp.sha(source), json.dumps(ids), "[]"))
            c.execute("INSERT INTO diffs VALUES('t',?,?,?,?)", (dp.sha(b"before"), dp.sha(source), "---\n+++\n", time.time()))
            task = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
            dp._finish_review(c, task, None, FindingAPI(), "docs-reviewer", 5, threading.Semaphore(1), 1)
            updated = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
            self.assertEqual(json.loads(updated["comment_ids"]), [ids[1]])
            self.assertEqual(json.loads(updated["completed"]), [ids[0], ids[2]])
            c.close()

    def test_resumed_review_subtracts_accepted_ids_without_hiding_findings(self):
        class ReviewAPI:
            def create_session(self, *args): return "ses-review"
            def send(self, *args): pass
            def request(self, method, path, payload=None, timeout=30):
                if path.endswith("/wait"): return None
                if path == "/api/session/ses-review": return {"data": {"outcome": "succeeded"}}
                return {"data": [{"content": [{"type": "text", "text": json.dumps({
                    "completed": ["A.java#javadoc-7"], "unresolved": [], "findings": [], "summary": "ok"
                })}]}]}

        ids = [f"A.java#javadoc-{i}" for i in range(1, 8)]
        historical = "A.java#javadoc-99"

        def finish(root, comment_ids, pending_completed, completed, pending_findings="[]"):
            db = root / "campaign/c.sqlite"; path = root / "A.java"
            source = b" ".join(f"/** {i} */".encode() for i in range(1, 8)) + b" class A {}"
            path.write_bytes(source)
            c = dp.connect(db)
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,state,attempts,after_hash,pending_completed,pending_unresolved,pending_findings,completed) VALUES('t','A.java',?,?,?,?,?,'awaiting_review',1,?,?,?,?,?)",
                      (json.dumps(comment_ids), json.dumps([f"/** {i} */" for i in range(1, 8)]), json.dumps([1]*len(comment_ids)), dp.sha(source), dp.code_fingerprint(source), dp.sha(source), json.dumps(pending_completed), "[]", pending_findings, json.dumps(completed)))
            c.execute("INSERT INTO diffs VALUES('t',?,?,?,?)", (dp.sha(b"before"), dp.sha(source), "---\n+++\n", time.time()))
            task = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
            dp._finish_review(c, task, None, ReviewAPI(), "docs-reviewer", 5, threading.Semaphore(1), 1)
            updated = c.execute("SELECT * FROM tasks WHERE id='t'").fetchone()
            result = {key: updated[key] for key in ("state", "comment_ids", "completed", "findings")}
            c.close()
            return result

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            result = finish(Path(d), ids, [ids[6]], ids[:6] + [historical])
            self.assertEqual(result["state"], "done")
            self.assertEqual(json.loads(result["completed"]), ids + [historical])

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            finding = json.dumps([{"id": ids[0], "reason": "still defective"}])
            result = finish(Path(d), ids, [ids[6]], ids[:6], finding)
            self.assertEqual(result["state"], "pending")
            self.assertEqual(json.loads(result["comment_ids"]), [ids[0]])

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            result = finish(Path(d), ids, [], ids[:6] + [historical])
            self.assertEqual(result["state"], "pending")
            self.assertEqual(json.loads(result["comment_ids"]), [ids[6]])
            self.assertEqual(json.loads(result["completed"]), ids[:6] + [historical])

    def test_timed_out_review_is_interrupted_and_blocks_at_budget(self):
        class TimeoutAPI:
            def create_session(self, *args): return "ses-timeout"
            def send(self, *args): pass
            def request(self, method, path, payload=None, timeout=30):
                if path.endswith("/interrupt"): return None
                if path.endswith("/wait") and method == "POST" and not hasattr(self, "timed_out"):
                    self.timed_out = True
                    raise TimeoutError("fake wait deadline")
                return None

        with tempfile.TemporaryDirectory() as d, patch.object(dp, "ROOT", Path(d)):
            root = Path(d); db = root / "campaign/c.sqlite"; path = root / "A.java"
            original = b"/** edited */ class A {}"; path.write_bytes(original); ids = ["A.java#javadoc-1"]
            c = dp.connect(db)
            c.execute("INSERT INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash,state,edit_state,after_hash,pending_completed,pending_unresolved) VALUES('t','A.java',?,?,?,?,?,'awaiting_review','done',?,?,?)",
                      (json.dumps(ids), json.dumps(["/** edited */"]), "[1]", dp.sha(original), dp.code_fingerprint(original), dp.sha(original), json.dumps(ids), "[]"))
            c.execute("INSERT INTO diffs VALUES('t',?,?,?,?)", (dp.sha(b"before"), dp.sha(original), "---\n+++\n", time.time()))
            c.commit(); c.close()
            result = dp.run_campaign(db, max_tasks=1, workers=1, reviewers=1, retries=0, timeout=1, api=TimeoutAPI())
            self.assertEqual(result["tasks"], {"blocked": 1})
            self.assertEqual(result["active_leases"], 0)

    def test_cli_and_progress_json(self):
        with tempfile.TemporaryDirectory() as d:
            db = Path(d) / "c.sqlite"; dp.connect(db).close()
            self.assertEqual(dp.status(db)["active_leases"], 0)
        with patch.object(dp, "init_campaign", return_value={"tasks": 3}), patch("builtins.print") as output:
            self.assertEqual(dp.main(["init"]), 0)
            self.assertEqual(json.loads(output.call_args.args[0]), {"tasks": 3})


if __name__ == "__main__":
    unittest.main()
