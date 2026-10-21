#!/usr/bin/env python3
"""Resumable, fail-closed scheduler for bounded Javadoc pruning tasks.

The scheduler never restores source files. Its baseline is an immutable evidence
copy, not a checkout to which edits can be reverted.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, wait, FIRST_COMPLETED
import fnmatch
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sqlite3
import threading
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / "doc-prune-work" / "campaign"
DB = WORK / "campaign.sqlite3"
MAX_COMMENTS = 10
MAX_COMMENT_CHARS = 9000
DEFAULT_MODEL = {"providerID": "openai", "id": "gpt-6-luna"}


class _NullContext:
    def __enter__(self): return self
    def __exit__(self, *args): return False


class SourceSafetyError(RuntimeError):
    """Conflict with source outside the task's selected Javadocs."""


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def source_files(root: Path) -> list[Path]:
    root = root.resolve()
    def allowed(path: Path) -> bool:
        parts = path.relative_to(root.resolve()).parts
        if parts[:3] == ("common", "src", "main") and len(parts) > 3 and parts[3] == "java":
            return True
        return parts[0] == "extension" and any(parts[i:i+3] == ("src", "main", "java") for i in range(1, len(parts)-2))

    inventory = root / "doc-prune-work" / "inventory-sources.txt"
    files: set[Path] = set()
    if inventory.exists():
        for line in inventory.read_text(encoding="utf-8").splitlines():
            name = line.strip()
            if name.endswith(".java"):
                p = (root / name).resolve()
                if not p.is_relative_to(root.resolve()) or not allowed(p):
                    raise ValueError(f"Inventory path is not a shared Java source: {name}")
                if not p.is_file(): raise FileNotFoundError(f"Inventory source missing: {name}")
                files.add(p)
    else:
        bases = [root / "common/src/main/java", root / "extension"]
        for base in bases:
            if base.exists():
                files.update(p.resolve() for p in base.rglob("*.java") if p.is_file() and allowed(p.resolve()))
    return sorted(files)


def lexical_comments(text: str) -> list[tuple[int, int, str]]:
    """Return Javadoc spans, ignoring comment-like text inside Java literals."""
    result = []
    i, n, state = 0, len(text), "code"
    while i < n:
        if state == "code":
            if text.startswith('"""', i):
                state, i = "text", i + 3
            elif text[i] == '"':
                state, i = "string", i + 1
            elif text[i] == "'":
                state, i = "char", i + 1
            elif text.startswith("//", i):
                state, i = "line", i + 2
            elif text.startswith("/*", i):
                start = i
                javadoc = text.startswith("/**", i)
                end = text.find("*/", i + 2)
                if end < 0:
                    break
                i = end + 2
                if javadoc:
                    result.append((start, i, text[start:i]))
            else:
                i += 1
        elif state == "line":
            if text[i] in "\r\n": state = "code"
            i += 1
        elif state in ("string", "char"):
            if text[i] == "\\": i += 2
            elif (state == "string" and text[i] == '"') or (state == "char" and text[i] == "'"):
                state = "code"; i += 1
            else: i += 1
        else:  # Java text block
            if text.startswith('"""', i): state = "code"; i += 3
            elif text[i] == "\\": i += 2
            else: i += 1
    return result


def code_fingerprint(data: bytes) -> str:
    """Hash Java tokens outside comments; catches any runtime/declaration edit."""
    text = data.decode("utf-8")
    tokens, i, n, state = [], 0, len(text), "code"
    while i < n:
        if state == "code":
            if text.startswith('"""', i): state = "text"; tokens.append('"""'); i += 3
            elif text.startswith("//", i): state = "line"; i += 2
            elif text.startswith("/*", i): state = "block"; i += 2
            elif text[i] == '"': state = "string"; tokens.append('"'); i += 1
            elif text[i] == "'": state = "char"; tokens.append("'"); i += 1
            elif text[i].isspace(): i += 1
            elif text[i].isalpha() or text[i] in "_$" or text[i].isdigit():
                j = i + 1
                while j < n and (text[j].isalnum() or text[j] in "_$."): j += 1
                tokens.append(text[i:j]); i = j
            else: tokens.append(text[i]); i += 1
        elif state == "line":
            if text[i] in "\r\n": state = "code"
            i += 1
        elif state == "block":
            if text.startswith("*/", i): state = "code"; i += 2
            else: i += 1
        elif state in ("string", "char"):
            delim = '"' if state == "string" else "'"
            if text[i] == "\\": tokens.append(text[i:i+2]); i += 2
            elif text[i] == delim: tokens.append(delim); state = "code"; i += 1
            else: tokens.append(text[i]); i += 1
        else:
            if text.startswith('"""', i): tokens.append('"""'); state = "code"; i += 3
            else: tokens.append(text[i]); i += 1
    return sha("\0".join(tokens).encode())


def protected_source_fingerprint(data: bytes, selected_ordinals: set[int]) -> str:
    """Hash all bytes except selected Javadocs; preserve guards and block code."""
    text = data.decode("utf-8")
    spans = lexical_comments(text)
    pieces, cursor = [], 0
    for ordinal, (start, end, _comment) in enumerate(spans):
        pieces.append(text[cursor:start])
        pieces.append(f"\0SELECTED_JAVADOC_{ordinal}\0" if ordinal in selected_ordinals else text[start:end])
        cursor = end
    pieces.append(text[cursor:])
    return sha("".join(pieces).encode())


def since_signature(comment: str) -> tuple[tuple[str, ...], int]:
    """Capture both @since values and marker presence, including malformed tags."""
    values = tuple(re.findall(r"@since\s+([^\s*]+)", comment))
    markers = len(re.findall(r"@since\b", comment))
    return values, markers


def comment_tasks(path: Path, root: Path) -> list[dict]:
    data = path.read_bytes(); text = data.decode("utf-8")
    spans = lexical_comments(text)
    groups, current = [], []
    for index, (start, end, comment) in enumerate(spans):
        if current and (len(current) >= MAX_COMMENTS or sum(len(x[3]) for x in current) + len(comment) > MAX_COMMENT_CHARS):
            groups.append(current); current = []
        # Stable IDs derive from the source path and ordinal, not line positions.
        current.append((index, start, end, comment))
    if current: groups.append(current)
    rel = path.relative_to(root).as_posix()
    fingerprint = code_fingerprint(data)
    out = []
    for group in groups:
        ids = [f"{rel}#javadoc-{idx+1}" for idx, *_ in group]
        out.append({"id": hashlib.sha256((rel + "\0" + ",".join(ids)).encode()).hexdigest()[:20],
                    "path": rel, "comment_ids": ids, "comments": [x[3] for x in group],
                    "lines": [text.count("\n", 0, x[1]) + 1 for x in group],
                    "source_hash": sha(data), "code_fingerprint": fingerprint})
    return out


def selected_comment_context(text: str, path: str, comment_ids: list[str], excerpt_limit: int = 1000) -> list[dict]:
    """Resolve stable ordinal IDs to exact current lines and bounded member context."""
    spans = lexical_comments(text)
    by_id = {f"{path}#javadoc-{i+1}": (i, start, end, comment)
             for i, (start, end, comment) in enumerate(spans)}
    contexts = []
    for comment_id in comment_ids:
        try:
            ordinal, start, end, comment = by_id[comment_id]
        except KeyError as exc:
            raise SourceSafetyError(f"Selected Javadoc no longer exists: {comment_id}") from exc
        next_start = spans[ordinal + 1][0] if ordinal + 1 < len(spans) else len(text)
        following = text[end:next_start].strip()
        clipped = following[:excerpt_limit]
        if len(following) > excerpt_limit:
            clipped += "\n[excerpt clipped; read the assigned source file at these lines for more context]"
        contexts.append({
            "id": comment_id,
            "line_start": text.count("\n", 0, start) + 1,
            "line_end": text.count("\n", 0, end) + 1,
            "javadoc": comment,
            "following_source_excerpt": clipped,
        })
    return contexts


def connect(db: Path = DB) -> sqlite3.Connection:
    db.parent.mkdir(parents=True, exist_ok=True)
    c = sqlite3.connect(db, timeout=30)
    c.row_factory = sqlite3.Row
    c.execute("PRAGMA journal_mode=WAL")
    c.executescript("""
    CREATE TABLE IF NOT EXISTS meta(key TEXT PRIMARY KEY,value TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS sources(path TEXT PRIMARY KEY,baseline_hash TEXT NOT NULL,baseline_code_hash TEXT NOT NULL,current_hash TEXT NOT NULL,legacy_status TEXT);
    CREATE TABLE IF NOT EXISTS tasks(id TEXT PRIMARY KEY,path TEXT NOT NULL,comment_ids TEXT NOT NULL,comments TEXT NOT NULL,lines TEXT NOT NULL,source_hash TEXT NOT NULL,code_hash TEXT NOT NULL,state TEXT NOT NULL DEFAULT 'pending',edit_state TEXT NOT NULL DEFAULT 'pending',review_state TEXT NOT NULL DEFAULT 'pending',evidence_state TEXT NOT NULL DEFAULT 'pending',artifact_state TEXT NOT NULL DEFAULT 'pending',attempts INTEGER NOT NULL DEFAULT 0,review_attempts INTEGER NOT NULL DEFAULT 0,lease_owner TEXT,lease_until REAL,session_id TEXT,completed TEXT NOT NULL DEFAULT '[]',unresolved TEXT NOT NULL DEFAULT '[]',findings TEXT NOT NULL DEFAULT '[]',last_error TEXT);
    CREATE TABLE IF NOT EXISTS transcripts(id INTEGER PRIMARY KEY,task_id TEXT,session_id TEXT,role TEXT,body TEXT,created REAL);
    CREATE TABLE IF NOT EXISTS diffs(task_id TEXT PRIMARY KEY,before_hash TEXT,after_hash TEXT,patch TEXT,created REAL);
    CREATE TABLE IF NOT EXISTS unresolved(id INTEGER PRIMARY KEY,task_id TEXT,comment_id TEXT,reason TEXT,created REAL,resolved INTEGER DEFAULT 0);
    CREATE TABLE IF NOT EXISTS artifact_states(path TEXT PRIMARY KEY,state TEXT NOT NULL,detail TEXT);
    CREATE TABLE IF NOT EXISTS file_leases(path TEXT PRIMARY KEY,task_id TEXT NOT NULL,owner TEXT NOT NULL,lease_until REAL NOT NULL);
    CREATE TABLE IF NOT EXISTS legacy_evidence(name TEXT PRIMARY KEY,path TEXT,sha256 TEXT,payload TEXT);
    """)
    # Additive migration for databases initialized by earlier scheduler builds.
    task_columns = {r[1] for r in c.execute("PRAGMA table_info(tasks)")}
    for name in ("editor_session", "reviewer_session", "before_text", "after_hash", "pending_completed", "pending_unresolved", "pending_findings"):
        if name not in task_columns:
            c.execute(f"ALTER TABLE tasks ADD COLUMN {name} TEXT")
    if "review_attempts" not in task_columns:
        c.execute("ALTER TABLE tasks ADD COLUMN review_attempts INTEGER NOT NULL DEFAULT 0")
    return c


def init_campaign(root: Path = ROOT, db: Path = DB) -> dict:
    root = root.resolve()
    db.parent.mkdir(parents=True, exist_ok=True)
    lock = (db.parent / "campaign.lock").open("a+")
    try:
        try: fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as exc: raise RuntimeError("cannot initialize while campaign runner is active") from exc
        c = connect(db)
        try:
            baseline = db.parent / "baseline"
            baseline.mkdir(parents=True, exist_ok=True)
            paths = source_files(root)
            for path in paths:
                rel = path.relative_to(root).as_posix(); data = path.read_bytes(); digest = sha(data)
                registered = c.execute("SELECT * FROM sources WHERE path=?", (rel,)).fetchone()
                snap = baseline / rel
                if registered:
                    # Existing sources keep their original task partition. Only
                    # scheduler-recorded changes are accepted on re-init.
                    if digest != registered["current_hash"]:
                        raise RuntimeError(f"Source changed outside recorded campaign state: {rel}; explicit reconciliation required")
                    continue
                if not snap.exists():
                    snap.parent.mkdir(parents=True, exist_ok=True)
                    snap.write_bytes(data)
                elif sha(snap.read_bytes()) != digest:
                    raise RuntimeError(f"Baseline collision for unregistered source {rel}")
                c.execute("INSERT INTO sources(path,baseline_hash,baseline_code_hash,current_hash) VALUES(?,?,?,?)",
                          (rel, sha(snap.read_bytes()), code_fingerprint(snap.read_bytes()), digest))
                for task in comment_tasks(path, root):
                    c.execute("INSERT OR IGNORE INTO tasks(id,path,comment_ids,comments,lines,source_hash,code_hash) VALUES(?,?,?,?,?,?,?)",
                              (task["id"], rel, json.dumps(task["comment_ids"]), json.dumps(task["comments"]), json.dumps(task["lines"]), digest, task["code_fingerprint"]))
            ledger = root / "doc-prune-work" / "review-ledger-20261004.tsv"
            if ledger.exists():
                import csv
                with ledger.open(encoding="utf-8", newline="") as f:
                    for row in csv.DictReader(f, delimiter="\t"):
                        rel = row.get("source", "").strip()
                        if rel and c.execute("SELECT 1 FROM sources WHERE path=?", (rel,)).fetchone():
                            c.execute("UPDATE sources SET legacy_status=? WHERE path=?", (row.get("status", ""), rel))
            extra = root / "doc-prune-work" / "campaign-legacy-evidence.json"
            if extra.exists():
                raw = extra.read_bytes()
                c.execute("INSERT OR REPLACE INTO legacy_evidence VALUES(?,?,?,?)",
                          (extra.name, str(extra.relative_to(root)), sha(raw), raw.decode("utf-8", errors="replace")))
            c.execute("INSERT OR IGNORE INTO meta VALUES('initialized',?)", (str(time.time()),))
            c.commit()
            total_tasks = c.execute("SELECT count(*) FROM tasks").fetchone()[0]
            return {"sources": len(paths), "tasks": total_tasks, "baseline": str(baseline)}
        finally:
            c.close()
    finally:
        lock.close()


def status(db: Path = DB) -> dict:
    c = connect(db)
    tasks = {r["state"]: r["n"] for r in c.execute("SELECT state,count(*) n FROM tasks GROUP BY state")}
    sources = c.execute("SELECT count(*) FROM sources").fetchone()[0]
    unresolved = c.execute("SELECT count(*) FROM unresolved WHERE resolved=0").fetchone()[0]
    leases = c.execute("SELECT count(*) FROM file_leases").fetchone()[0]
    result = {"sources": sources, "tasks": tasks, "unresolved": unresolved, "active_leases": leases,
              "edit": dict(c.execute("SELECT edit_state,count(*) FROM tasks GROUP BY edit_state").fetchall()),
              "review": dict(c.execute("SELECT review_state,count(*) FROM tasks GROUP BY review_state").fetchall()),
              "evidence": dict(c.execute("SELECT evidence_state,count(*) FROM tasks GROUP BY evidence_state").fetchall()),
              "artifacts": dict(c.execute("SELECT artifact_state,count(*) FROM tasks GROUP BY artifact_state").fetchall())}
    c.close(); return result


class API:
    """Authenticated client through the locally configured OpenCode CLI."""
    def __init__(self, executable: str | None = None):
        self.executable = executable or os.environ.get("OPENCODE_BIN", "opencode")

    def request(self, method: str, path: str, payload=None, timeout: int = 30):
        command = [self.executable, "api", method, path]
        if payload is not None:
            command.extend(["--data", json.dumps(payload)])
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, timeout=timeout, check=True)
        return json.loads(result.stdout) if result.stdout.strip() else None

    def create_session(self, directory: Path, title: str, agent: str, edit_path: Path | None):
        permissions = [
            {"action": "*", "resource": "*", "effect": "deny"},
            {"action": "read", "resource": "*", "effect": "allow"},
            {"action": "glob", "resource": "*", "effect": "allow"},
            {"action": "grep", "resource": "*", "effect": "allow"},
            {"action": "external_directory", "resource": "*", "effect": "allow"},
        ]
        if edit_path is not None:
            absolute = edit_path.resolve()
            relative = absolute.relative_to(directory.resolve()).as_posix()
            permissions.extend((
                {"action": "edit", "resource": relative, "effect": "allow"},
                {"action": "edit", "resource": str(absolute), "effect": "allow"},
            ))
        session = self.request("POST", "/api/session", {"title": title, "agent": agent,
                              "model": DEFAULT_MODEL, "location": {"directory": str(directory.resolve())},
                              "permissions": permissions})["data"]
        model = session.get("model") or {}
        if model.get("providerID") != DEFAULT_MODEL["providerID"] or model.get("id") != DEFAULT_MODEL["id"]:
            raise RuntimeError(f"OpenCode session model mismatch: {model!r}")
        return session["id"]

    def send(self, session: str, text: str):
        self.request("POST", f"/api/session/{session}/prompt", {"text": text})

    def prompt(self, directory: Path, title: str, text: str, agent: str, edit_path: Path | None):
        session = self.create_session(directory, title, agent, edit_path)
        self.send(session, text)
        return session


def parse_response(raw: str, allowed: list[str]) -> dict:
    # Require the assistant's final message to be exactly one JSON object.
    stripped = raw.strip()
    if not stripped.startswith("{"): raise ValueError("response must start with JSON object")
    start = 0
    decoder = json.JSONDecoder(); obj, end = decoder.raw_decode(stripped)
    if stripped[end:].strip(): raise ValueError("trailing text after JSON object")
    if not isinstance(obj, dict) or set(obj) != {"completed", "unresolved", "findings", "summary"}: raise ValueError("response schema keys must match exactly")
    if not isinstance(obj["completed"], list) or not isinstance(obj["unresolved"], list) or not isinstance(obj["findings"], list) or not isinstance(obj["summary"], str): raise ValueError("response field types are invalid")
    if any(not isinstance(x, str) or x not in allowed for x in obj["completed"]) or len(set(obj["completed"])) != len(obj["completed"]): raise ValueError("unknown or duplicate completed comment id")
    for label in ("unresolved", "findings"):
        entries = obj[label]
        if any(not isinstance(x, dict) or set(x) != {"id", "reason"} or x.get("id") not in allowed or not isinstance(x.get("reason"), str) or not x.get("reason") for x in entries):
            raise ValueError(f"invalid {label} entry")
    return obj


def acquire(c: sqlite3.Connection, owner: str, retries: int, sources: list[str] | None = None) -> sqlite3.Row | None:
    now = time.time()
    c.commit()
    c.execute("BEGIN IMMEDIATE")
    # Expired leases are not automatically reclaimed: a timed-out server session
    # may still be writing. Recovery is explicit after confirming session idle.
    states = "'pending','retry','awaiting_review'"
    rows = c.execute(f"SELECT tasks.* FROM tasks LEFT JOIN sources USING(path) WHERE tasks.state IN ({states}) AND ((tasks.state='awaiting_review' AND tasks.review_attempts<?) OR (tasks.state<>'awaiting_review' AND tasks.attempts<?)) ORDER BY CASE tasks.state WHEN 'awaiting_review' THEN 0 ELSE 1 END,CASE WHEN lower(coalesce(sources.legacy_status,'')) LIKE '%pending%' OR lower(coalesce(sources.legacy_status,'')) LIKE '%partial%' OR lower(coalesce(sources.legacy_status,'')) LIKE '%incomplete%' THEN 0 WHEN lower(coalesce(sources.legacy_status,'')) LIKE '%audited%' THEN 2 ELSE 1 END,tasks.path,tasks.id", (retries + 1, retries + 1)).fetchall()
    row = next((r for r in rows if (not sources or any(fnmatch.fnmatchcase(r["path"], pattern) for pattern in sources))
                and not c.execute("SELECT 1 FROM file_leases WHERE path=?", (r["path"],)).fetchone()), None)
    if row:
        c.execute("UPDATE tasks SET state='running',lease_owner=?,lease_until=?,attempts=attempts+CASE WHEN state='awaiting_review' THEN 0 ELSE 1 END,review_attempts=review_attempts+CASE WHEN state='awaiting_review' THEN 1 ELSE 0 END WHERE id=?", (owner, now + 900, row["id"]))
        c.execute("INSERT INTO file_leases(path,task_id,owner,lease_until) VALUES(?,?,?,?)", (row["path"], row["id"], owner, now + 900))
    c.commit()
    return row


def _assistant_text(api: API, session: str, timeout: int) -> str:
    api.request("POST", f"/api/experimental/session/{session}/wait", {}, timeout=timeout)
    info = api.request("GET", f"/api/session/{session}").get("data", {})
    if info.get("outcome") in ("failed", "interrupted"):
        raise RuntimeError(f"OpenCode session ended with outcome {info['outcome']}")
    messages = api.request("GET", f"/api/session/{session}/message?order=desc&type=assistant&limit=1")
    return next((part["text"] for msg in messages["data"] for part in msg.get("content", [])
                 if part.get("type") == "text" and isinstance(part.get("text"), str)), "")


def _finish_review(c, task, parsed, api, reviewer_agent, timeout, reviewer_gate, retries):
    rel = task["path"]
    diff_row = c.execute("SELECT patch FROM diffs WHERE task_id=?", (task["id"],)).fetchone()
    diff = diff_row[0] if diff_row else ""
    ids = json.loads(task["pending_completed"] or "[]")
    after = (ROOT / rel).read_bytes()
    if sha(after) != task["after_hash"]:
        raise SourceSafetyError("Source changed after edit checkpoint; review blocked")
    if diff and ids:
        contexts = selected_comment_context(after.decode(), rel, ids)
        active_file = ROOT / "stonecutter.active"
        target = active_file.read_text(encoding="utf-8").strip() if active_file.exists() else "unknown"
        prompt = ("Review only this selected Javadoc diff against current implementation; do not edit. "
                  f"The shared-source Minecraft target is {target}. For game behavior, consult only the matching snapshot /home/east_arctica/src/java/JsMacros/minecraft-source/{target}; do not infer behavior from other versions. "
                  "Return STRICT JSON {completed:[ids],unresolved:[{id,reason}],findings:[{id,reason}],summary:...}. "
                  "List every reviewed ID as completed, including accurate no-op prose. findings must be [] unless there is a specific actionable contract defect; each finding names its comment ID and reason.\n"
                  "Use these exact current file offsets; read only the named declarations and expand to additional implementation lines if a claim requires it. "
                  f"Source file: {rel}\nSelected context: {json.dumps(contexts, ensure_ascii=False)}\nDIFF:\n{diff}")
        with reviewer_gate:
            # A newly dispatched reviewer consumes one review attempt. acquire()
            # already charges resumed awaiting_review work, but its returned row
            # predates that update; compare against that snapshot to avoid a
            # second charge. Direct editor-to-review checkpoints have equal
            # counters and are charged here.
            live_attempts = c.execute("SELECT review_attempts FROM tasks WHERE id=?", (task["id"],)).fetchone()[0]
            if live_attempts == task["review_attempts"]:
                c.execute("UPDATE tasks SET review_attempts=review_attempts+1 WHERE id=?", (task["id"],))
                c.commit()
            sid = api.create_session(ROOT, f"docs-prune review {rel}", reviewer_agent, None)
            c.execute("UPDATE tasks SET reviewer_session=?,session_id=?,review_state='running' WHERE id=?", (sid, sid, task["id"])); c.commit()
            c.execute("INSERT INTO transcripts(task_id,session_id,role,body,created) VALUES(?,?,?,?,?)", (task["id"], sid, "user", prompt, time.time())); c.commit()
            api.send(sid, prompt)
            raw_review = _assistant_text(api, sid, timeout)
            if sha((ROOT / rel).read_bytes()) != task["after_hash"]:
                raise SourceSafetyError("Source changed while reviewer was running; review result discarded")
            c.execute("INSERT INTO transcripts(task_id,session_id,role,body,created) VALUES(?,?,?,?,?)", (task["id"], sid, "assistant", raw_review, time.time())); c.commit()
            reviewed = parse_response(raw_review, ids)
    else:
        reviewed = {"completed": ids, "unresolved": [], "findings": [], "summary": "No prose diff"}
    issue_ids = {x["id"] for x in reviewed["findings"]}
    editor_completed = set(json.loads(task["pending_completed"] or "[]"))
    previously_completed = set(json.loads(task["completed"] or "[]"))
    editor_incomplete = set(json.loads(task["comment_ids"])) - (editor_completed | previously_completed)
    review_incomplete = editor_completed - set(reviewed["completed"])
    remaining = editor_incomplete | review_incomplete | issue_ids
    prior_unresolved = json.loads(task["pending_unresolved"] or "[]")
    for item in prior_unresolved + reviewed["unresolved"]:
        c.execute("INSERT INTO unresolved(task_id,comment_id,reason,created) VALUES(?,?,?,?)", (task["id"], item["id"], item["reason"], time.time()))
    # Mark source prose completion separately from unresolved behavioral evidence.
    prior_findings = json.loads(task["pending_findings"] or "[]")
    editor_issue_ids = {x["id"] for x in prior_findings}
    remaining.update(editor_issue_ids)
    live_budgets = c.execute("SELECT attempts,review_attempts FROM tasks WHERE id=?", (task["id"],)).fetchone()
    exhausted = bool((editor_incomplete or editor_issue_ids) and live_budgets["attempts"] >= retries + 1) or bool((review_incomplete or issue_ids) and live_budgets["review_attempts"] >= retries + 1)
    state = "blocked" if exhausted else ("retry" if remaining else "done")
    all_findings = prior_findings + reviewed["findings"]
    accepted = (previously_completed | set(reviewed["completed"])) - issue_ids - editor_issue_ids
    review_state = "done" if ids else "pending"
    c.execute("UPDATE tasks SET state=?,review_state=?,edit_state='done',completed=?,unresolved=?,findings=?,evidence_state=?,lease_owner=NULL,lease_until=NULL,last_error=NULL WHERE id=?",
              (state, review_state, json.dumps(sorted(accepted)), json.dumps(prior_unresolved + reviewed["unresolved"]), json.dumps(all_findings), "unresolved" if prior_unresolved or reviewed["unresolved"] else "pending", task["id"]))
    c.execute("UPDATE sources SET current_hash=? WHERE path=?", (sha(after), rel))
    if remaining and not exhausted:
        original_ids = json.loads(task["comment_ids"])
        old_comments = json.loads(task["comments"])
        current_comments = [x[2] for x in lexical_comments(after.decode())]
        comment_by_id = {f"{rel}#javadoc-{i+1}": text for i, text in enumerate(current_comments)}
        keep = sorted(remaining)
        c.execute("UPDATE tasks SET comment_ids=?,comments=?,lines=?,source_hash=?,edit_state='pending',review_state='pending',state='pending',pending_completed=NULL,pending_unresolved=NULL,pending_findings=NULL WHERE id=?",
                  (json.dumps(keep), json.dumps([comment_by_id[x] for x in keep]), json.dumps([0] * len(keep)), sha(after), task["id"]))
    # Refresh source snapshots for other not-yet-leased chunks of this file.
    for row in c.execute("SELECT id,comment_ids FROM tasks WHERE path=? AND id<>? AND state IN ('pending','retry')", (rel, task["id"])).fetchall():
        current_comments = [x[2] for x in lexical_comments(after.decode())]
        ids_other = json.loads(row["comment_ids"])
        c.execute("UPDATE tasks SET source_hash=?,comments=?,lines=? WHERE id=?", (sha(after), json.dumps([current_comments[int(i.rsplit('-',1)[1])-1] for i in ids_other]), json.dumps([0]*len(ids_other)), row["id"]))
    c.execute("DELETE FROM file_leases WHERE path=? AND task_id=?", (rel, task["id"]))
    c.commit()


def run_campaign(db: Path = DB, max_tasks: int | None = None, timeout: int = 600, retries: int = 1,
                 workers: int = 4, editor_agent: str = "docs-editor", reviewer_agent: str = "docs-reviewer",
                 api: API | None = None, sources: list[str] | None = None, reviewers: int = 2) -> dict:
    if not 1 <= workers <= 4 or not 1 <= reviewers <= 2: raise ValueError("editors must be 1..4 and reviewers 1..2")
    lockpath = db.parent / "campaign.lock"; lockpath.parent.mkdir(parents=True, exist_ok=True)
    lock = lockpath.open("a+")
    try: fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError: raise RuntimeError("campaign runner already active")
    stop = db.parent / "STOP"; api = api or API()
    claim_guard = threading.Lock(); started = 0; reviewer_gate = threading.Semaphore(reviewers)
    counts = {"processed": 0, "errors": 0}

    # Recover leases left by a process crash only after the recorded session has
    # reached idle. A checkpointed edit is resumed as review-only.
    c = connect(db)
    for lease in c.execute("SELECT * FROM file_leases").fetchall():
        task = c.execute("SELECT * FROM tasks WHERE id=?", (lease["task_id"],)).fetchone()
        safe = not task["session_id"]
        if task["session_id"]:
            try:
                api.request("POST", f"/api/experimental/session/{task['session_id']}/wait", {}, timeout=timeout)
                safe = True
            except Exception:
                safe = False
        if safe:
            if task["edit_state"] == "done" and task["after_hash"] and sha((ROOT / task["path"]).read_bytes()) == task["after_hash"]:
                state = "blocked" if task["review_attempts"] >= retries + 1 else "awaiting_review"
            elif sha((ROOT / task["path"]).read_bytes()) == task["source_hash"]:
                state = "retry"
            else:
                state = "blocked"
            c.execute("UPDATE tasks SET state=?,lease_owner=NULL,lease_until=NULL WHERE id=?", (state, task["id"]))
            c.execute("DELETE FROM file_leases WHERE path=?", (lease["path"],))
        else:
            c.execute("UPDATE tasks SET state='blocked',last_error='Previous worker session is not confirmed idle',lease_owner=?,lease_until=? WHERE id=?",
                      (lease["owner"], time.time()+timeout, task["id"]))
    c.commit(); c.close()

    def work_loop(index: int):
        nonlocal started
        owner = f"runner-{uuid.uuid4()}-{index}"
        while not stop.exists():
            with claim_guard:
                if max_tasks is not None and started >= max_tasks: return
                c = connect(db); task = acquire(c, owner, retries, sources)
                if task: started += 1
                c.close()
            if not task: return
            c = connect(db); sid = None
            try:
                path = ROOT / task["path"]; rel = task["path"]
                current = path.read_bytes()
                # A resumed review has a durable diff and must never invoke editor again.
                if task["state"] == "awaiting_review":
                    _finish_review(c, task, None, api, reviewer_agent, timeout, reviewer_gate, retries)
                    counts["processed"] += 1; continue
                if sha(current) != task["source_hash"] or code_fingerprint(current) != task["code_hash"]:
                    c.execute("UPDATE tasks SET state='blocked',last_error='Source changed since task snapshot',lease_owner=NULL,lease_until=NULL WHERE id=?", (task["id"],))
                    c.execute("DELETE FROM file_leases WHERE path=?", (rel,)); c.commit(); counts["errors"] += 1; continue
                ids = json.loads(task["comment_ids"])
                contexts = selected_comment_context(current.decode(), rel, ids)
                active_file = ROOT / "stonecutter.active"
                target = active_file.read_text(encoding="utf-8").strip() if active_file.exists() else "unknown"
                api_snapshot = ROOT / "common" / "versions" / target / "build/generated/stonecutter/main/java"
                minecraft_snapshot = Path("/home/east_arctica/src/java/JsMacros/minecraft-source") / target
                readonly = Path(rel).name == "FWorld.java"
                action_prompt = ("Read-only review only; do not edit. Report focused findings; do not mark the entire class audited. "
                                if readonly else "Edit ONLY selected Javadocs; do not change imports, annotations, declarations, runtime tokens, or @since. ")
                prompt = (f"Documentation target is the current shared Stonecutter source, version {target}. "
                          f"For Minecraft behavior claims, consult only matching Minecraft source snapshot {minecraft_snapshot}; "
                          f"for JsMacros/version-branch declarations, consult the generated shared-source view {api_snapshot}. "
                          "Do not audit unrelated versions: inspect another branch only when a selected Javadoc makes a cross-version claim. "
                           "Generated-output checks use offline tests and direct inspection, not model artifact validators. "
                          + action_prompt +
                          "Read docs/STYLE.md and docs/README.md. Return STRICT JSON {completed:[ids],unresolved:[{id,reason}],findings:[{id,reason}],summary:...}. "
                          "findings must be [] unless a specific actionable contract defect remains; each finding names its comment ID and reason. Accurate no-op prose has no finding. "
                          "A comment may be both completed and unresolved: concise prose is complete while behavioral evidence remains unresolved. "
                           "Address only the selected IDs and do not infer completion from legacy status. "
                            "Preserve audited historical metadata exactly, including @since punctuation, historical notes, and [citation needed] markers; this is not a history audit. Do not delete comments or internal section markers. "
                           "Leave existing @author tags unchanged. Keep and fill @param and non-void @return tags rather than replacing them with description-only prose; fluent return tags use 'self for chaining'. "
                           "Preserve useful usage restrictions, lifetime consequences, extension guidance, relationships, and compact value-shape examples. Concision must not erase useful information. "
                          f"Prior actionable findings (recheck against current source; do not repeat if fixed): {json.dumps(json.loads(task['findings'] or '[]'))}\n"
                          + f"File: {rel}\nCurrent selected contexts (IDs map to exact Javadoc ordinals; do not recount):\n"
                          + json.dumps(contexts, ensure_ascii=False))
                if task["edit_state"] == "done":
                    c.execute("UPDATE tasks SET state='awaiting_review' WHERE id=?", (task["id"],)); c.commit()
                    _finish_review(c, task, None, api, reviewer_agent, timeout, reviewer_gate, retries)
                    counts["processed"] += 1; continue
                agent = reviewer_agent if readonly else editor_agent
                gate = reviewer_gate if readonly else _NullContext()
                with gate:
                    sid = api.create_session(ROOT, f"docs-prune {'review' if readonly else 'edit'} {rel}", agent, None if readonly else path)
                    c.execute("UPDATE tasks SET session_id=?,editor_session=CASE WHEN ? THEN editor_session ELSE ? END,reviewer_session=CASE WHEN ? THEN ? ELSE reviewer_session END,edit_state=? WHERE id=?", (sid, int(readonly), None if readonly else sid, int(readonly), sid, "read_only" if readonly else "running", task["id"])); c.commit()
                    c.execute("INSERT INTO transcripts(task_id,session_id,role,body,created) VALUES(?,?,?,?,?)", (task["id"], sid, "user", prompt, time.time())); c.commit()
                    api.send(sid, prompt)
                    raw_response = _assistant_text(api, sid, timeout)
                    c.execute("INSERT INTO transcripts(task_id,session_id,role,body,created) VALUES(?,?,?,?,?)", (task["id"], sid, "assistant", raw_response, time.time())); c.commit()
                    parsed = parse_response(raw_response, ids)
                if readonly:
                    if sha(path.read_bytes()) != sha(current): raise SourceSafetyError("Read-only source changed during review")
                    for item in parsed["unresolved"]: c.execute("INSERT INTO unresolved(task_id,comment_id,reason,created) VALUES(?,?,?,?)", (task["id"], item["id"], item["reason"], time.time()))
                    remaining = set(ids) - set(parsed["completed"])
                    remaining.update(finding["id"] for finding in parsed["findings"])
                    attempts = c.execute("SELECT attempts FROM tasks WHERE id=?", (task["id"],)).fetchone()[0]
                    state = "blocked" if remaining and attempts >= retries + 1 else ("pending" if remaining else "done")
                    c.execute("UPDATE tasks SET state=?,review_state='done',edit_state='read_only',completed=?,findings=?,unresolved=?,lease_owner=NULL,lease_until=NULL WHERE id=?",
                              (state, json.dumps(parsed["completed"]), json.dumps(parsed["findings"]), json.dumps(parsed["unresolved"]), task["id"]))
                    if remaining and state == "pending":
                        comments = json.loads(task["comments"]); oldids = json.loads(task["comment_ids"])
                        c.execute("UPDATE tasks SET comment_ids=?,comments=?,lines=?,review_state='pending' WHERE id=?",
                                  (json.dumps(sorted(remaining)), json.dumps([x for ident, x in zip(oldids, comments) if ident in remaining]), json.dumps([0]*len(remaining)), task["id"]))
                    c.execute("DELETE FROM file_leases WHERE path=?", (rel,)); c.commit(); counts["processed"] += 1; continue
                after = path.read_bytes()
                before_comments = [x[2] for x in lexical_comments(current.decode())]
                after_comments = [x[2] for x in lexical_comments(after.decode())]
                selected = {int(x.rsplit("-", 1)[1]) - 1 for x in ids}
                if protected_source_fingerprint(current, selected) != protected_source_fingerprint(after, selected):
                    raise SourceSafetyError("Source outside assigned Javadocs changed; source preserved")
                if len(before_comments) != len(after_comments) or any(before_comments[i] != after_comments[i] for i in range(len(before_comments)) if i not in selected): raise SourceSafetyError("Unassigned Javadoc changed")
                for i in selected:
                    if since_signature(before_comments[i]) != since_signature(after_comments[i]): raise SourceSafetyError("@since changed")
                import difflib
                diff = "".join(difflib.unified_diff(current.decode().splitlines(True), after.decode().splitlines(True), fromfile="before", tofile="after"))
                c.execute("INSERT OR REPLACE INTO diffs VALUES(?,?,?,?,?)", (task["id"], sha(current), sha(after), diff, time.time()))
                editor_findings = parsed["findings"]
                editor_finding_ids = {finding["id"] for finding in editor_findings}
                completed = [comment_id for comment_id in parsed["completed"] if comment_id not in editor_finding_ids]
                unresolved = parsed["unresolved"]
                c.execute("UPDATE tasks SET state='awaiting_review',source_hash=?,after_hash=?,before_text=?,pending_completed=?,pending_unresolved=?,pending_findings=?,edit_state='done' WHERE id=?",
                          (sha(after), sha(after), current.decode(), json.dumps(completed), json.dumps(unresolved), json.dumps(editor_findings), task["id"]))
                c.execute("UPDATE sources SET current_hash=? WHERE path=?", (sha(after), rel))
                c.commit()
                checkpoint = c.execute("SELECT * FROM tasks WHERE id=?", (task["id"],)).fetchone()
                _finish_review(c, checkpoint, None, api, reviewer_agent, timeout, reviewer_gate, retries)
                counts["processed"] += 1
            except Exception as exc:
                active_row = c.execute("SELECT session_id FROM tasks WHERE id=?", (task["id"],)).fetchone()
                active_sid = active_row[0] if active_row and active_row[0] else sid
                safe = active_sid is None
                if active_sid:
                    try:
                        api.request("POST", f"/api/session/{active_sid}/interrupt", {}, timeout=timeout)
                        # Bounded wait is authoritative; only idle completion permits lease release.
                        api.request("POST", f"/api/experimental/session/{active_sid}/wait", {}, timeout=timeout)
                        safe = True
                    except Exception: safe = False
                current_task = c.execute("SELECT edit_state,state FROM tasks WHERE id=?", (task["id"],)).fetchone()
                awaiting = current_task["state"] == "awaiting_review" or current_task["edit_state"] == "done"
                budgets = c.execute("SELECT attempts,review_attempts FROM tasks WHERE id=?", (task["id"],)).fetchone()
                if isinstance(exc, SourceSafetyError):
                    new_state = "blocked"
                elif not safe:
                    new_state = "blocked"
                elif awaiting:
                    new_state = "blocked" if budgets["review_attempts"] >= retries + 1 else "awaiting_review"
                else:
                    new_state = "retry" if budgets["attempts"] < retries + 1 else "blocked"
                c.execute("UPDATE tasks SET state=?,last_error=?,lease_owner=?,lease_until=? WHERE id=?", (new_state, str(exc), None if safe else owner, None if safe else time.time()+timeout, task["id"]))
                if safe: c.execute("DELETE FROM file_leases WHERE path=? AND task_id=?", (task["path"], task["id"]))
                c.commit(); counts["errors"] += 1
            finally: c.close()

    try:
        with ThreadPoolExecutor(max_workers=workers, thread_name_prefix="docs-prune") as pool:
            futures = [pool.submit(work_loop, i) for i in range(workers)]
            wait(futures)
            for f in futures: f.result()
        return {**counts, **status(db)}
    finally: lock.close()


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("init"); sub.add_parser("status"); sub.add_parser("stop")
    run = sub.add_parser("run"); run.add_argument("--max-tasks", type=int); run.add_argument("--timeout", type=int, default=600); run.add_argument("--retries", type=int, default=1); run.add_argument("--workers", type=int, default=4); run.add_argument("--reviewers", type=int, default=2); run.add_argument("--source", action="append", dest="sources", help="relative source path/glob; repeatable"); run.add_argument("--editor-agent", default="docs-editor"); run.add_argument("--reviewer-agent", default="docs-reviewer")
    args = parser.parse_args(argv)
    if args.command == "init": result = init_campaign()
    elif args.command == "status": result = status()
    elif args.command == "stop":
        WORK.mkdir(parents=True, exist_ok=True); (WORK / "STOP").write_text("stop requested\n"); result = {"stop_requested": True}
    else:
        if (WORK / "STOP").exists(): (WORK / "STOP").unlink()
        result = run_campaign(max_tasks=args.max_tasks, timeout=args.timeout, retries=args.retries, workers=args.workers, reviewers=args.reviewers, sources=args.sources, editor_agent=args.editor_agent, reviewer_agent=args.reviewer_agent)
    print(json.dumps(result, indent=2, sort_keys=True)); return 0


if __name__ == "__main__":
    raise SystemExit(main())
