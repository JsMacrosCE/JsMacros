#!/usr/bin/env python3
"""Durable, read-only validation phase for generated Javadoc artifacts."""
from __future__ import annotations

import argparse
import copy
import fcntl
import fnmatch
from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, wait
import hashlib
import json
import os
from pathlib import Path
import re
import sqlite3
import subprocess
import sys
import threading
import time
import uuid

import docs_prune as prune
import docs_prune_artifacts as artifacts

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / "doc-prune-work" / "campaign"
DB = WORK / "artifact-validation.sqlite3"
ARTIFACT_STOP = WORK / "ARTIFACT_STOP"
MODEL = {"providerID": "openai", "id": "gpt-6-luna"}
MAX_PROMPT_PART_BYTES = 20 * 1024
MAX_PROMPT_SEGMENT_CHARS = 250
MAX_PROMPT_PART_LINE_CHARS = 1800
MAX_PROMPT_PARTS = 16
MAX_WIRE_BYTES = 180 * 1024
PREFERRED_INLINE_BYTES = 60 * 1024
MAX_PACKET_STRING_CHARS = 300


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def targets(root: Path) -> list[str]:
    text = (root / "settings.gradle.kts").read_text(encoding="utf-8")
    # Blank comments without disturbing braces/offsets; numeric plugin versions and
    # examples outside the Stonecutter declaration must never enter the target set.
    chars = list(text); i = 0; state = "code"
    while i < len(text):
        if state == "code":
            if text.startswith("//", i): state = "line"; chars[i:i+2] = "  "; i += 2; continue
            if text.startswith("/*", i): state = "block"; chars[i:i+2] = "  "; i += 2; continue
            if text[i] == '"': state = "string"
        elif state == "line":
            if text[i] in "\r\n": state = "code"
            else: chars[i] = " "
        elif state == "block":
            if text.startswith("*/", i): chars[i:i+2] = "  "; i += 2; state = "code"; continue
            if text[i] not in "\r\n": chars[i] = " "
        elif state == "string":
            if text[i] == "\\": i += 2; continue
            if text[i] == '"': state = "code"
        i += 1
    visible = "".join(chars)
    stone = re.search(r"\bstonecutter\s*\{", visible)
    if not stone: raise RuntimeError("No Stonecutter block found in settings.gradle.kts")

    def block_end(opening: int) -> int:
        depth, quote, escaped = 0, False, False
        for pos in range(opening, len(text)):
            char = text[pos]
            if quote:
                if escaped: escaped = False
                elif char == "\\": escaped = True
                elif char == '"': quote = False
            elif char == '"': quote = True
            elif char == "{": depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0: return pos
        raise RuntimeError("Unclosed Stonecutter settings block")

    stone_open = visible.find("{", stone.start())
    stone_close = block_end(stone_open)
    body_visible, body = visible[stone_open + 1:stone_close], text[stone_open + 1:stone_close]
    create = re.search(r"\bcreate\s*\(\s*rootProject\s*\)\s*\{", body_visible)
    scope = body
    if create:
        open_pos = body_visible.find("{", create.start())
        close_pos = block_end(stone_open + 1 + open_pos) - (stone_open + 1)
        scope = body[open_pos + 1:close_pos]
    # The create(rootProject) direct versions call is authoritative. In compact
    # fixtures that omit create, use a direct Stonecutter versions call.
    call = re.search(r"\bversions\s*\(([^)]*)\)", scope)
    if call:
        found = re.findall(r'"(\d+\.\d+(?:\.\d+)?)"', call.group(1))
    else:
        common = re.search(r"\bbranch\s*\(\s*['\"]common['\"]\s*\)\s*\{", body_visible)
        if not common: raise RuntimeError("No root/common Stonecutter versions(...) declaration found")
        open_pos = body_visible.find("{", common.start())
        close_pos = block_end(stone_open + 1 + open_pos) - (stone_open + 1)
        branch = body[open_pos + 1:close_pos]
        call = re.search(r"\bversions\s*\(([^)]*)\)", branch)
        found = re.findall(r'"(\d+\.\d+(?:\.\d+)?)"', call.group(1)) if call else []
    found = sorted(set(found))
    if not found:
        raise RuntimeError("No supported Minecraft targets found in settings.gradle.kts")
    return found


def inputs(root: Path) -> list[Path]:
    paths = list(prune.source_files(root))
    # Fingerprint all source candidates, not merely paths present in a possibly
    # stale scheduler inventory. Generated build trees are intentionally excluded.
    for base in (root / "common/src/main/java", root / "buildSrc/src/main/java"):
        if base.exists(): paths.extend(base.rglob("*.java"))
    extension = root / "extension"
    if extension.exists():
        paths.extend(extension.glob("*/src/main/java/**/*.java"))
    for base in (root / "buildSrc/src/main", root / "gradle"):
        if base.exists():
            paths.extend(p for p in base.rglob("*") if p.is_file() and p.suffix in {".java", ".kt", ".kts", ".groovy", ".properties", ".toml"})
    paths.extend(p for p in root.glob("**/*.gradle") if p.is_file() and not any(part in {"build", ".gradle", ".git"} for part in p.parts))
    paths.extend(p for p in root.glob("**/*.gradle.kts") if p.is_file() and not any(part in {"build", ".gradle", ".git"} for part in p.parts))
    paths.extend(p for p in (root / "gradle.properties", root / "gradle/libs.versions.toml") if p.is_file())
    return sorted(set(p.resolve() for p in paths))


def fingerprint(root: Path) -> str:
    h = hashlib.sha256()
    for path in inputs(root):
        h.update(path.relative_to(root.resolve()).as_posix().encode()); h.update(b"\0"); h.update(path.read_bytes()); h.update(b"\0")
    return h.hexdigest()


def outside_javadocs(data: bytes) -> bytes:
    """Preserve every byte outside lexical Javadocs, including comments/directives."""
    text = data.decode("utf-8")
    spans = prune.lexical_comments(text)
    pieces, cursor = [], 0
    for start, end, _ in spans:
        pieces.append(text[cursor:start]); cursor = end
    pieces.append(text[cursor:])
    return "".join(pieces).encode("utf-8")


def runtime_baseline_changes(root: Path) -> list[str]:
    db = root / "doc-prune-work/campaign/campaign.sqlite3"
    c = sqlite3.connect(db)
    try:
        rows = c.execute("SELECT path,baseline_hash FROM sources").fetchall()
        has_meta = c.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name='meta'").fetchone()
        raw = c.execute("SELECT value FROM meta WHERE key='approved_javadoc_layouts'").fetchone()[0] if has_meta and c.execute("SELECT 1 FROM meta WHERE key='approved_javadoc_layouts'").fetchone() else "{}"
    except sqlite3.Error:
        return ["<invalid source campaign metadata>"]
    finally: c.close()
    try:
        approvals = json.loads(raw)
        if not isinstance(approvals, dict): raise ValueError("approval metadata must be an object")
    except (TypeError, json.JSONDecodeError, ValueError):
        return ["<malformed approved_javadoc_layouts metadata>"]
    baseline = db.parent / "baseline"; changed = []
    for rel, indexed_baseline_hash in rows:
        current = root / rel; snapshot = baseline / rel
        if not current.is_file() or not snapshot.is_file(): changed.append(rel); continue
        baseline_bytes = snapshot.read_bytes(); current_bytes = current.read_bytes()
        baseline_outside = outside_javadocs(baseline_bytes)
        current_outside = outside_javadocs(current_bytes)
        if baseline_outside == current_outside: continue
        approval = approvals.get(rel)
        if not isinstance(approval, dict):
            changed.append(rel); continue
        required = {"baseline_hash", "java_token_hash", "outside_javadocs_sha256", "reason", "review_session"}
        if not required.issubset(approval) or not all(isinstance(approval.get(k), str) and approval[k] for k in required):
            changed.append(rel); continue
        baseline_hash = digest(baseline_bytes)
        java_hash = prune.code_fingerprint(baseline_bytes)
        current_java_hash = prune.code_fingerprint(current_bytes)
        exact_match = (
            approval["baseline_hash"] == baseline_hash == indexed_baseline_hash
            and approval["java_token_hash"] == java_hash == current_java_hash
            and approval["outside_javadocs_sha256"] == digest(current_outside)
        )
        if not exact_match: changed.append(rel)
    return changed


def connect(db: Path = DB):
    db.parent.mkdir(parents=True, exist_ok=True)
    c = sqlite3.connect(db, timeout=30); c.row_factory = sqlite3.Row
    c.execute("PRAGMA journal_mode=WAL")
    c.executescript("""
    CREATE TABLE IF NOT EXISTS meta(key TEXT PRIMARY KEY,value TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS tasks(id TEXT PRIMARY KEY,source TEXT NOT NULL,target TEXT NOT NULL,packet TEXT NOT NULL,state TEXT NOT NULL DEFAULT 'pending',attempts INTEGER NOT NULL DEFAULT 0,session_id TEXT,last_error TEXT,completed TEXT NOT NULL DEFAULT '[]',unresolved TEXT NOT NULL DEFAULT '[]',findings TEXT NOT NULL DEFAULT '[]',generation TEXT NOT NULL DEFAULT '',scope_key TEXT NOT NULL DEFAULT '');
    CREATE TABLE IF NOT EXISTS attempts(id INTEGER PRIMARY KEY,task_id TEXT,session_id TEXT,started REAL,ended REAL,transcript TEXT,response TEXT,error TEXT);
    """)
    if "generation" not in {row[1] for row in c.execute("PRAGMA table_info(tasks)")}:
        c.execute("ALTER TABLE tasks ADD COLUMN generation TEXT NOT NULL DEFAULT ''")
    if "scope_key" not in {row[1] for row in c.execute("PRAGMA table_info(tasks)")}:
        c.execute("ALTER TABLE tasks ADD COLUMN scope_key TEXT NOT NULL DEFAULT ''")
    return c


def source_lock(root: Path):
    path = root / "doc-prune-work/campaign/campaign.lock"
    path.parent.mkdir(parents=True, exist_ok=True)
    handle = path.open("a+")
    try: fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        handle.close(); raise RuntimeError("Source campaign is active; artifact preparation/validation refused")
    return handle


def live_source_tasks(root: Path) -> int:
    db = root / "doc-prune-work/campaign/campaign.sqlite3"
    if not db.exists(): return 0
    c = sqlite3.connect(db)
    try:
        columns = {row[1] for row in c.execute("PRAGMA table_info(tasks)")}
        lease = " OR lease_owner IS NOT NULL" if "lease_owner" in columns else ""
        return c.execute(f"SELECT count(*) FROM tasks WHERE state='running' OR (lease_until IS NOT NULL AND lease_until>?) {lease}", (time.time(),)).fetchone()[0]
    finally: c.close()


def changed_sources(root: Path) -> list[str]:
    db = root / "doc-prune-work/campaign/campaign.sqlite3"
    if not db.exists(): raise RuntimeError("Source campaign baseline database is missing")
    c = sqlite3.connect(db)
    try: rows = c.execute("SELECT path,baseline_hash FROM sources").fetchall()
    finally: c.close()
    changed = {rel for rel, base in rows if not (root / rel).is_file() or digest((root / rel).read_bytes()) != base}
    # Include current tracked Java changes even when source campaign initialization predates them.
    proc = subprocess.run(["git", "diff", "--name-only", "HEAD", "--", "*.java"], cwd=root, capture_output=True, text=True)
    changed.update(line.strip() for line in proc.stdout.splitlines() if line.strip().endswith(".java") and (root / line.strip()).is_file())
    return sorted(changed)


def is_source_constructor(entry: dict) -> bool:
    if entry.get("kind") != "member": return False
    name = entry.get("name", "")
    if not name: return False
    header = artifacts._mask_java_annotations(entry.get("signature", "")).strip()
    header = re.sub(r"^(?:(?:public|protected|private|static|final|synchronized|abstract|native|strictfp)\s+)+", "", header)
    header = re.sub(r"^<[^>]+>\s*", "", header)
    return bool(re.match(re.escape(name) + r"\s*\(", header))


def source_owner_info(entry: dict, qualified_type: str) -> tuple[str, list[str], str]:
    """Return canonical source owner, nested type chain, and package prefix."""
    name = entry.get("name", "")
    owner_chain = [part for part in (entry.get("owner") or "").split(".") if part]
    if entry.get("kind") == "type":
        chain = owner_chain + ([name] if name else [])
        canonical = entry.get("owner_qualified") or qualified_type
    elif is_source_constructor(entry):
        chain = owner_chain + ([name] if not owner_chain or owner_chain[-1] != name else [])
        canonical = entry.get("owner_qualified") or qualified_type
        if name and canonical.rsplit(".", 1)[-1] != name: canonical += "." + name
    else:
        chain = owner_chain
        canonical = entry.get("owner_qualified") or qualified_type
    suffix = ".".join(chain)
    package = canonical[:-len(suffix) - 1] if suffix and canonical.endswith("." + suffix) else canonical.rpartition(".")[0]
    return canonical, chain or [name], package


def output_member_name(entry: dict, qualified_type: str, fmt: str) -> str:
    """Map nested Java declarations and constructors to emitted language names."""
    name = entry.get("name", "")
    if is_source_constructor(entry):
        return "__init__" if fmt == "Python" else "constructor" if fmt == "TypeScript" else name
    if entry.get("kind") == "type":
        _, chain, _ = source_owner_info(entry, qualified_type)
        return "_".join(chain) if fmt == "Python" else "$".join(chain) if fmt == "TypeScript" else name
    return artifacts.python_output_name(name) if fmt == "Python" else name


def output_member_kind(entry: dict, fmt: str) -> str:
    if entry.get("kind") == "type":
        return "class" if fmt == "Python" else "declaration"
    name = re.escape(entry.get("name", "")); signature = entry.get("signature", "")
    is_call = bool(name and re.search(r"\b" + name + r"\s*\(", signature))
    if fmt == "Python": return "method" if is_call else "field"
    return "member"


def select_output_fragments(entries: list[dict], fragments: list[dict], qualified_type: str, fmt: str) -> tuple[list[dict], list[str]]:
    def pair(entry): return output_member_name(entry, qualified_type, fmt), output_member_kind(entry, fmt)
    def owner_matches(entry, fragment):
        expected_owner, chain, package = source_owner_info(entry, qualified_type)
        actual_owner = fragment.get("source_owner") if fmt == "TypeScript" else fragment.get("owner")
        if actual_owner is not None: return actual_owner == expected_owner
        owner_path = fragment.get("owner_path") if fmt == "TypeScript" else None
        if owner_path is not None:
            expected_path = "Packages." + (package + "." if package else "") + "$".join(chain)
            return owner_path == expected_path
        # Older extractors omitted owner metadata. Accept it only when this name/kind
        # identifies one source owner and no conflicting explicit output owner exists.
        expected_pair = pair(entry)
        source_owners = {source_owner_info(item, qualified_type)[0] for item in entries if pair(item) == expected_pair}
        output_owners = set()
        for item in fragments:
            if (item.get("name"), item.get("kind")) != expected_pair: continue
            owner = item.get("source_owner") if fmt == "TypeScript" else item.get("owner")
            if owner is not None: output_owners.add(owner)
        return len(source_owners) <= 1 and (not output_owners or output_owners == {expected_owner})

    selected = [fragment for fragment in fragments if any(
        (fragment.get("name"), fragment.get("kind")) == pair(entry) and owner_matches(entry, fragment)
        for entry in entries)]
    missing = [entry.get("key", "") for entry in entries if not any(
        (fragment.get("name"), fragment.get("kind")) == pair(entry) and owner_matches(entry, fragment)
        for fragment in fragments)]
    return selected, sorted(missing)


def excluded_contract(entry: dict) -> dict | None:
    """Recognize explicitly non-public fields, methods, and constructors only."""
    if entry.get("kind") != "member": return None
    signature = artifacts._mask_java_annotations(entry.get("signature", ""))
    name = re.escape(entry.get("name", ""))
    if not name: return None
    executable = bool(re.search(r"\b" + name + r"\s*\(", signature))
    modifiers = r"(?:public|protected|private|static|final|transient|volatile|synchronized|abstract|native|strictfp|default)"
    declaration = re.match(r"^\s*((?:" + modifiers + r"\s+)+)", signature)
    if not declaration: return None
    access = re.search(r"\b(protected|private)\b", declaration.group(1))
    if not access: return None
    owner = entry.get("owner_qualified") or entry.get("owner") or ""
    owner_name = owner.rsplit(".", 1)[-1]
    kind = "CONSTRUCTOR" if executable and owner_name and entry.get("name") == owner_name else "METHOD" if executable else "FIELD"
    return {
        "key": entry.get("key"), "entry": entry,
        "reason": f"Explicitly {access.group(1)} {kind} is not emitted by the verified public-member filters.",
        "renderer_policy": {
            "verified": True,
            "doclet_model_builder": "DocletModelBuilder.java:236-259 filters non-public members before FIELD/METHOD/CONSTRUCTOR construction",
            "web_class_parser": "ClassParser.java:168-181 filters non-public constructors; 191-209 fields; 222-253 methods",
        },
    }


def split_contract_entries(entries: list[dict]) -> tuple[list[dict], list[dict]]:
    assigned, excluded = [], []
    for entry in entries:
        policy = excluded_contract(entry)
        if policy is None: assigned.append(entry)
        else: excluded.append(policy)
    return assigned, excluded


def wire_aliases(canonical_ids: list[str]) -> dict[str, str]:
    """Short, deterministic worker-facing IDs mapped to exact durable IDs."""
    aliases = {}
    for index, canonical in enumerate(canonical_ids, 1):
        alias = f"a{index:03d}"
        while alias in canonical_ids or alias in aliases: alias = "w" + alias
        aliases[alias] = canonical
    return aliases


def wire_packet(packet: dict, aliases: dict[str, str]) -> dict:
    """Make a prompt-only copy; durable packet IDs remain untouched."""
    wire = copy.deepcopy(packet)
    wire.pop("matrix_source_contract", None)
    canonical_to_alias = {canonical: alias for alias, canonical in aliases.items()}
    wire["id_map"] = aliases
    if isinstance(wire.get("assigned_ids"), list):
        wire["assigned_ids"] = [canonical_to_alias.get(item, item) for item in wire["assigned_ids"]]
    for target in wire.get("targets", []):
        if isinstance(target.get("assigned_ids"), list):
            target["assigned_ids"] = [canonical_to_alias.get(item, item) for item in target["assigned_ids"]]
    if isinstance(wire.get("extracted_fragments"), list) and wire.get("targets"):
        refs_in_scope = set()
        for target in wire["targets"]:
            for fmt, rendered in target.get("artifacts", {}).items():
                for file in rendered.get("files", []):
                    refs_in_scope.add((target.get("target"), fmt, file.get("path")))
        selected = []
        for fragment in wire["extracted_fragments"]:
            refs = [ref for ref in fragment.get("references", [])
                    if (ref.get("target"), ref.get("format"), ref.get("path")) in refs_in_scope]
            if refs:
                fragment["references"] = refs
                selected.append(fragment)
        wire["extracted_fragments"] = selected
    for item in wire.get("extracted_fragments", []):
        fragment = item.get("fragment", {})
        if "raw_markdown" in fragment and fragment.get("raw_markdown") == fragment.get("text"):
            fragment.pop("text", None)
    return wire


def canonicalize_response_ids(response: dict, aliases: dict[str, str], canonical_ids: list[str]) -> dict:
    """Translate only exact aliases or exact currently assigned canonical IDs."""
    allowed = set(canonical_ids); translated = copy.deepcopy(response)
    def convert(value):
        if not isinstance(value, str): raise ValueError("worker response ID must be a string")
        if value in aliases: return aliases[value]
        if value in allowed: return value
        raise ValueError(f"unknown worker response ID: {value!r}")
    if isinstance(translated.get("completed"), list):
        translated["completed"] = [convert(item) for item in translated["completed"]]
    for name in ("unresolved", "findings"):
        if isinstance(translated.get(name), list):
            for item in translated[name]:
                if isinstance(item, dict) and "id" in item: item["id"] = convert(item["id"])
    return translated


def select_final_response_text(message: dict) -> str:
    """Select the sole final-answer text block, or a legacy unphased response."""
    content = message.get("content") or []
    text_parts = [part for part in content if part.get("type") == "text" and isinstance(part.get("text"), str)]
    phased = [part for part in text_parts if isinstance(part.get("state"), dict) and "phase" in part["state"]]
    finals = [part for part in phased if part["state"].get("phase") == "final_answer"]
    if phased:
        if len(finals) != 1:
            raise ValueError("expected exactly one phased final_answer text block")
        return finals[0]["text"]
    if len(text_parts) != 1:
        raise ValueError("expected exactly one unphased legacy text block")
    return text_parts[0]["text"]


def build_worker_prompt(packet: dict, aliases: dict[str, str]) -> str:
    instructions = (
        "Read-only artifact validation across the explicit target bundle. Compare source_contract once, but validate every assigned target/member alias against that target's outputs. "
        "Use only exact aliases listed in id_map for response IDs; do not abbreviate or guess IDs. excluded_contracts are preserved source contracts with verified explicit protected/private declaration renderer-policy evidence; they are not assigned IDs, do not complete them, and do not treat unrelated absent members as excluded. "
        "Exact shared extracted fragments are only a review-efficiency hint: inspect each target reference, retain differences in types, links, examples, and URL/provenance metadata. HTML/Markdown full-page sections are included with exact file paths and hashes. "
        "Missing or unparsable outputs block only the affected assigned IDs; report findings/unresolved with aliases. Extraction/presence/hash identity is not correctness. A known missing fragment is a blocker: do not mark its alias completed. "
        "Return STRICT JSON exactly as this valid JSON shape: {\"completed\":[\"a001\"],\"unresolved\":[{\"id\":\"a002\",\"reason\":\"Concrete reason this assigned alias cannot be validated.\"}],\"findings\":[{\"id\":\"a003\",\"reason\":\"Concrete validation finding for this assigned alias.\"}],\"summary\":\"Brief review summary.\"}. "
        "In completed, use string IDs only. Never use bare string IDs in unresolved or findings; every entry there must be an object containing its assigned alias in id and a nonempty concrete reason. Every assigned alias must be accounted for.\nAllowed IDs: "
    )
    return instructions + json.dumps(list(aliases)) + "\nPACKET:\n" + json.dumps(packet, ensure_ascii=False)


def _make_bundle(source: str, index: int, target_groups: list[dict]) -> dict:
    first = target_groups[0]
    shared: dict[str, dict] = {}; targets_data = []; assigned = []
    for group in target_groups:
        target = group["target"]
        id_map = {key: f"{target}::{key}" for key in group["assigned_ids"]}
        assigned.extend(id_map.values())
        target_view = {k: copy.deepcopy(group.get(k)) for k in ("target", "artifacts", "output_snapshot", "format_errors", "missing_formats", "fragment_gaps", "ambiguous_output_names", "member_output_names", "excluded_contracts")}
        target_view["assigned_ids"] = list(id_map.values())
        for fmt, data in target_view.get("artifacts", {}).items():
            for rendered in data.get("files", []):
                fragments = rendered.pop("fragments", []); refs = []
                for fragment in fragments:
                    encoded = json.dumps(fragment, sort_keys=True, ensure_ascii=False, separators=(",", ":"))
                    identity = [fmt, rendered.get("sha256"), encoded] if fmt in ("HTML", "VitePress") else [fmt, encoded]
                    key = digest(json.dumps(identity, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))
                    shared.setdefault(key, {"sha256": key, "fragment": fragment, "references": []})
                    ref = {"target": target, "format": fmt, "path": rendered.get("path"), "file_sha256": rendered.get("sha256")}
                    shared[key]["references"].append(ref); refs.append(key)
                if refs: rendered["fragment_refs"] = refs
        targets_data.append(target_view)
    return {"schema": 1, "source": source, "source_sha256": first.get("source_sha256"), "group_index": index,
            "qualified_type": first.get("qualified_type"), "source_contract": first.get("source_contract", []),
            "matrix_source_contract": first.get("matrix_source_contract", first.get("source_contract", [])),
            "excluded_contracts": [{"target": group["target"], **item} for group in target_groups for item in group.get("excluded_contracts", [])],
            "targets": targets_data, "assigned_ids": assigned, "extracted_fragments": list(shared.values()),
            "dedup_note": "Byte-for-byte JSON identity only; shared extraction is not a correctness verdict. Every target occurrence and output provenance remains explicit."}


def serialize_packet_parts(packet: dict, aliases: dict[str, str]) -> list[str]:
    """Serialize a wire packet to bounded, independently valid JSON part objects."""
    records = API._packet_records(packet)
    lines = [json.dumps(record, ensure_ascii=False, separators=(",", ":")) for record in records]
    if any(len(line.encode("utf-8")) >= MAX_PROMPT_PART_LINE_CHARS for line in lines):
        raise ValueError("Structured packet record exceeds safe line length")
    def render(items, index, total):
        # The canonical alias map is already a packet record; do not repeat it in
        # every part. Only compact assigned aliases are repeated in the CLI prompt.
        header = json.dumps({"schema": 1, "part_index": index, "part_count": total}, separators=(",", ":"))
        payload = ",\n".join("    " + json.dumps(record, ensure_ascii=False, separators=(",", ":")) for record in items)
        return header[:-1] + ',\n  "records": [\n' + payload + "\n  ]\n}\n"
    packed, current = [], []
    for record in records:
        candidate = current + [record]
        if current and len(render(candidate, MAX_PROMPT_PARTS - 1, MAX_PROMPT_PARTS).encode("utf-8")) > MAX_PROMPT_PART_BYTES:
            packed.append(current); current = [record]
        else:
            current = candidate
        if len(render(current, MAX_PROMPT_PARTS - 1, MAX_PROMPT_PARTS).encode("utf-8")) > MAX_PROMPT_PART_BYTES:
            raise ValueError("A structured packet record exceeds safe part size")
    if current or not packed: packed.append(current)
    serialized = [render(items, index, len(packed)) for index, items in enumerate(packed)]
    for text in serialized:
        if len(text.encode("utf-8")) > MAX_PROMPT_PART_BYTES or any(
                len(line.encode("utf-8")) >= MAX_PROMPT_PART_LINE_CHARS for line in text.splitlines()):
            raise ValueError("Structured packet part exceeds transport bounds")
    return serialized


def inline_wire_bytes(bundle: dict) -> int:
    """Measure the exact JSON request body used by API.send's inline path."""
    aliases = wire_aliases(bundle.get("assigned_ids", []))
    wire = wire_packet(bundle, aliases)
    prompt = build_worker_prompt(wire, aliases)
    return len(json.dumps({"text": prompt}).encode("utf-8"))


def estimate_wire_bytes(bundle: dict) -> int:
    inline_size = inline_wire_bytes(bundle)
    if inline_size <= 64 * 1024:
        return inline_size
    aliases = wire_aliases(bundle.get("assigned_ids", []))
    wire = wire_packet(bundle, aliases)
    try:
        parts = serialize_packet_parts(wire, aliases)
    except ValueError:
        return MAX_WIRE_BYTES + 1
    structured_size = sum(len(part.encode("utf-8")) for part in parts) + len(
        json.dumps({"text": "Allowed IDs: " + json.dumps(list(aliases))}).encode("utf-8"))
    if len(parts) > MAX_PROMPT_PARTS:
        return max(structured_size, MAX_WIRE_BYTES + 1)
    return structured_size


def build_member_group(packet: dict, index: int, entries: list[dict], exclusions: list[dict] | None = None) -> dict:
    """Select only artifacts attributable to this atomic set of member names."""
    group = {**packet, "group_index": index, "matrix_source_contract": packet.get("source_contract", []),
             "source_contract": entries, "excluded_contracts": list(exclusions or []),
             "assigned_ids": [entry["key"] for entry in entries]}
    group["member_output_names"] = {entry["key"]: {fmt: output_member_name(entry, packet["qualified_type"], fmt)
                                                     for fmt in artifacts.FORMATS} for entry in entries}
    selected_artifacts = {}; fragment_gaps = {}; output_snapshot = {}; format_errors = {}; missing_formats = []
    assigned_owners = {source_owner_info(entry, packet["qualified_type"])[0] for entry in entries}
    for fmt, data in packet.get("artifacts", {}).items():
        selected_files = []
        all_fragments = [fragment for rendered in data.get("files", [])
                         for fragment in rendered.get("fragments", [])]
        _, missing_entries = select_output_fragments(entries, all_fragments, packet["qualified_type"], fmt)
        for rendered in data.get("files", []):
            owner = rendered.get("owner")
            # Ownership is provenance, not inferred from a matching member name.
            if owner is None or owner in assigned_owners:
                picked, _ = select_output_fragments(entries, rendered.get("fragments", []), packet["qualified_type"], fmt)
            else:
                # An explicit owner must not be overridden by matching names.
                picked = []
            if picked and (owner is None or owner in assigned_owners):
                selected_files.append({**rendered, "fragments": picked,
                                       **({"review_instruction": "Review exact extracted full-page sections; retain path and byte-hash provenance."} if fmt in ("HTML", "VitePress") else {})})
        fragment_gaps[fmt] = missing_entries
        failures = []
        for failure in data.get("failed_files", []):
            owner = failure.get("owner")
            if owner in assigned_owners:
                failures.append(failure)
            elif owner is None or owner not in {source_owner_info(item, packet["qualified_type"])[0]
                                                for item in packet.get("source_contract", [])}:
                # An unattributed or unrecognized error cannot safely be ignored.
                failures.append(failure)
        file_errors = [item for item in selected_files if item.get("parse_error")]
        format_errors[fmt] = bool(failures or file_errors)
        relevant_present = bool(selected_files)
        relevant_missing = bool(failures) or not relevant_present
        if relevant_missing: missing_formats.append(fmt)
        state = "present" if relevant_present else ("parse_error" if failures else "missing")
        selected_artifacts[fmt] = {**data, "state": state, "files": selected_files, "failed_files": failures}
        output_snapshot[fmt] = {"state": state, "files": [
            {"path": item.get("path"), "sha256": item.get("sha256")} for item in selected_files]}
    group["artifacts"] = selected_artifacts
    group["ambiguous_output_names"] = []
    group["fragment_gaps"] = fragment_gaps
    group["output_snapshot"] = output_snapshot
    group["format_errors"] = format_errors
    group["missing_formats"] = missing_formats
    return group


def split_oversized_member_group(packet: dict, group: dict) -> list[dict]:
    """Greedily split an oversized target only at complete member-name boundaries."""
    names: dict[str, list[dict]] = {}
    for entry in group["source_contract"]:
        names.setdefault(entry.get("name", entry["key"]), []).append(entry)
    packed: list[list[dict]] = []; current: list[dict] = []
    for same_name in names.values():
        candidate = current + same_name
        candidate_group = build_member_group(packet, group["group_index"], candidate)
        size = inline_wire_bytes(_make_bundle(packet["source"], group["group_index"], [candidate_group]))
        if current and size > PREFERRED_INLINE_BYTES:
            packed.append(current); current = list(same_name)
        else:
            current = candidate
    if current: packed.append(current)
    if len(packed) <= 1: return [group]
    result = []
    for subindex, entries in enumerate(packed):
        # Keep derived scopes outside the original per-packet chunk range so
        # adjacent unchanged chunks can never overwrite a subdivision packet.
        index = 1_000_000 + group["group_index"] * 1000 + subindex
        result.append(build_member_group(packet, index, entries))
    return result


def write_bundles(out: Path, generation: str, groups: list[dict], bundle_targets: bool = True) -> list[dict]:
    """Bundle targets while staying within the worker prompt budget."""
    by_source_chunk: dict[tuple[str, int, str], list[dict]] = {}
    for group in groups:
        key = (group["source"], group["group_index"], "" if bundle_targets else group["target"])
        by_source_chunk.setdefault(key, []).append(group)
    tasks = []
    for (source, index, _target_key), target_groups in sorted(by_source_chunk.items()):
        target_groups.sort(key=lambda item: item["target"])
        shards: list[tuple[list[dict], dict, int | None]] = []; current = []
        for group in target_groups:
            candidate = current + [group]
            candidate_bundle = _make_bundle(source, index, candidate)
            if inline_wire_bytes(candidate_bundle) <= PREFERRED_INLINE_BYTES:
                current = candidate; continue
            if current:
                shards.append((current, _make_bundle(source, index, current), None)); current = []
            single = _make_bundle(source, index, [group]); size = estimate_wire_bytes(single)
            if size > MAX_WIRE_BYTES: shards.append(([group], single, size))
            else: current = [group]
        if current: shards.append((current, _make_bundle(source, index, current), None))
        sharded = len(shards) > 1 or any(size for _, _, size in shards)
        for shard_groups, bundle, oversize in shards:
            target_set = sorted(group["target"] for group in shard_groups)
            if oversize:
                bundle["oversize_blocker"] = {"estimated_wire_bytes": oversize, "limit_bytes": MAX_WIRE_BYTES,
                                              "reason": "Single-target packet exceeds bounded prompt budget; not dispatched or truncated."}
            suffix = ("-targets-" + digest(json.dumps(target_set).encode())[:8]) if sharded else (f"-{target_set[0]}" if len(target_groups) == 1 else "")
            filename = f"bundle-{hashlib.sha256(source.encode()).hexdigest()[:12]}-{index:03d}{suffix}.json"
            (out / filename).write_text(json.dumps(bundle, indent=2, sort_keys=True) + "\n", encoding="utf-8")
            task_id = hashlib.sha256(f"{generation}\0{source}\0{index}\0{digest(json.dumps(bundle, sort_keys=True).encode())}".encode()).hexdigest()[:24]
            member_keys = sorted(entry.get("key", "") for entry in shard_groups[0].get("source_contract", []))
            scope_key = digest(json.dumps([source, shard_groups[0].get("source_sha256"), target_set, index, member_keys], separators=(",", ":")).encode())
            tasks.append({"id": task_id, "source": source, "target": json.dumps(target_set), "packet": filename,
                          "generation": generation, "scope_key": scope_key, "oversize_bytes": oversize})
    return tasks


def insert_prepared_tasks(conn: sqlite3.Connection, tasks: list[dict]) -> None:
    """Supersede only replaced packet representations of the identical task scope."""
    prior = conn.execute("SELECT value FROM meta WHERE key='packet_dir'").fetchone()
    prior_dir = Path(prior[0]) if prior else None
    for task in tasks:
        scope = task["scope_key"]
        # Backfill precise scopes for rows created before scope_key existed. Their
        # immutable packet carries source hash/target coverage and group index.
        if prior_dir and prior_dir.exists():
            for old in conn.execute("SELECT id,packet,target FROM tasks WHERE generation=? AND source=? AND scope_key='' AND id<>?",
                                    (task["generation"], task["source"], task["id"])).fetchall():
                try:
                    old_packet = json.loads((prior_dir / old["packet"]).read_text(encoding="utf-8"))
                    group_index = old_packet.get("group_index")
                    if group_index is None:
                        match = re.search(r"group-(\d+)", old["packet"]) or re.search(r"-(\d+)(?:-[^.]+)?\.json$", old["packet"])
                        group_index = int(match.group(1)) if match else None
                    old_targets = [view["target"] for view in old_packet.get("targets", [])]
                    if not old_targets:
                        old_target = old_packet.get("target")
                        old_targets = json.loads(old["target"]) if str(old["target"]).startswith("[") else [old_target or old["target"]]
                    member_keys = sorted(entry.get("key", "") for entry in old_packet.get("source_contract", []))
                    old_scope = digest(json.dumps([old_packet.get("source"), old_packet.get("source_sha256"), sorted(old_targets), group_index, member_keys], separators=(",", ":")).encode())
                    if old_scope == scope:
                        conn.execute("UPDATE tasks SET scope_key=? WHERE id=?", (scope, old["id"]))
                except (OSError, ValueError, TypeError, json.JSONDecodeError):
                    continue
        conn.execute("UPDATE tasks SET state='superseded' WHERE generation=? AND scope_key=? AND id<>? AND state NOT IN ('stale','superseded')",
                     (task["generation"], scope, task["id"]))
        conn.execute("INSERT OR IGNORE INTO tasks(id,source,target,packet,generation,scope_key) VALUES(?,?,?,?,?,?)",
                     (task["id"], task["source"], task["target"], task["packet"], task["generation"], scope))
        if task.get("oversize_bytes"):
            conn.execute("UPDATE tasks SET state='blocked',last_error=? WHERE id=?",
                         (f"Single-target wire packet is {task['oversize_bytes']} bytes; limit is {MAX_WIRE_BYTES}; not dispatched or truncated.", task["id"]))
        if task.get("oversize_bytes"):
            conn.execute("UPDATE tasks SET state='blocked',last_error=? WHERE id=?",
                         (f"Single-target wire packet is {task['oversize_bytes']} bytes; limit is {MAX_WIRE_BYTES}; not dispatched or truncated.", task["id"]))


def prepare(root: Path = ROOT, generate: bool = False, runner=subprocess.run,
            source_filter: list[str] | None = None, target_filter: list[str] | None = None,
            bundle_targets: bool = True) -> dict:
    root = root.resolve(); lock = source_lock(root)
    try:
        active = live_source_tasks(root)
        if active: raise RuntimeError(f"Source campaign has {active} running task(s)/live lease(s)")
        runtime_changes = runtime_baseline_changes(root)
        if runtime_changes:
            raise RuntimeError("Baseline-to-current non-Javadoc source bytes changed; refusing artifact generation: " + ", ".join(runtime_changes[:20]))
        before = fingerprint(root); selected = changed_sources(root); matrix_targets = targets(root); generated_targets = list(matrix_targets)
        # Legacy ledger rows keep pre-existing edited sources in scope.
        ledger = root / "doc-prune-work/review-ledger-20261004.tsv"
        if ledger.exists():
            for row in ledger.read_text(encoding="utf-8").splitlines()[1:]:
                cols = row.split("\t")
                if len(cols) > 1 and cols[1].strip() and (root / cols[1].strip()).is_file(): selected.append(cols[1].strip())
        selected = sorted(set(selected))
        if source_filter: selected = [s for s in selected if any(fnmatch.fnmatchcase(s, pat) for pat in source_filter)]
        if target_filter:
            unsupported = sorted(set(target_filter) - set(matrix_targets))
            if unsupported: raise RuntimeError(f"Unsupported target(s): {', '.join(unsupported)}")
            matrix_targets = sorted(set(target_filter))
        c = connect(root / "doc-prune-work/campaign/artifact-validation.sqlite3")
        stamp = json.loads((root / "doc-prune-work/campaign/generation.json").read_text()) if (root / "doc-prune-work/campaign/generation.json").exists() else None
        if generate:
            result = runner(["./gradlew", "-PdocsMinecraftVersions=all", "generateDocsAll"], cwd=root, capture_output=True, text=True, check=True)
            after = fingerprint(root)
            if before != after: raise RuntimeError("Source/doclet inputs changed during artifact generation")
            runtime_changes = runtime_baseline_changes(root)
            if runtime_changes: raise RuntimeError("Non-Javadoc source bytes changed during generation: " + ", ".join(runtime_changes[:20]))
            stamp = {"created_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "generation_id": uuid.uuid4().hex, "input_fingerprint": after, "targets": generated_targets, "returncode": 0}
            stamp_path = root / "doc-prune-work/campaign/generation.json"; stamp_path.write_text(json.dumps(stamp, indent=2) + "\n")
        if not stamp or stamp.get("input_fingerprint") != fingerprint(root):
            c.close(); return {"state": "blocked", "reason": "No matching verified generation stamp; rerun prepare --generate after sources are stable", "sources": selected, "targets": matrix_targets}
        if sorted(stamp.get("targets", [])) != sorted(generated_targets):
            c.close(); return {"state": "blocked", "reason": "Generation stamp target set does not match current Stonecutter root targets; regenerate explicitly", "sources": selected, "targets": matrix_targets}
        generation = stamp.get("generation_id")
        if not generation:
            c.close(); return {"state": "blocked", "reason": "Generation stamp predates unique generation IDs; regenerate explicitly", "sources": selected, "targets": matrix_targets}
        out = root / "doc-prune-work/campaign" / f"artifact-packets-{generation}"
        manifest = artifacts.build_packets(root, selected, matrix_targets, out)
        member_groups = []
        for filename in manifest.get("packet_files", []):
            packet = json.loads((out / filename).read_text(encoding="utf-8"))
            source, target = packet["source"], packet["target"]
            entries = packet.get("source_contract", [])
            # Bound by member name groups so overloaded signatures are never split.
            name_groups = {}
            for entry in entries: name_groups.setdefault(entry.get("name", entry["key"]), []).append(entry)
            chunks, chunk = [], []
            for same_name in name_groups.values():
                if chunk and len(chunk) + len(same_name) > 10: chunks.append(chunk); chunk = []
                chunk.extend(same_name)
            if chunk or not chunks: chunks.append(chunk)
            prepared_groups = []; orphan_exclusions = []
            for index, chunk in enumerate(chunks):
                assigned_entries, excluded = split_contract_entries(chunk)
                if not assigned_entries:
                    orphan_exclusions.extend(excluded); continue
                group = build_member_group(packet, index, assigned_entries, orphan_exclusions + excluded)
                orphan_exclusions = []
                # Carry exclusions once, on the first resulting chunk only.
                subdivisions = split_oversized_member_group(packet, group)
                if len(subdivisions) > 1 and group["excluded_contracts"]:
                    subdivisions[0]["excluded_contracts"] = group["excluded_contracts"]
                    for extra in subdivisions[1:]: extra["excluded_contracts"] = []
                prepared_groups.extend(subdivisions)
            if orphan_exclusions and prepared_groups:
                prepared_groups[0]["excluded_contracts"].extend(orphan_exclusions)
            member_groups.extend(prepared_groups)
        validation_tasks = write_bundles(out, generation, member_groups, bundle_targets=bundle_targets)
        if any(group.get("source_contract") for group in member_groups) and not validation_tasks:
            c.close()
            raise RuntimeError("Public source contracts were prepared but produced zero validation tasks")
        insert_prepared_tasks(c, validation_tasks)
        c.execute("UPDATE tasks SET state='stale' WHERE generation<>? AND state<>'stale'", (generation,))
        c.execute("INSERT OR REPLACE INTO meta VALUES('generation',?)", (json.dumps(stamp),))
        c.execute("INSERT OR REPLACE INTO meta VALUES('packet_dir',?)", (str(out),)); c.execute("INSERT OR REPLACE INTO meta VALUES('active_generation',?)", (generation,)); c.commit(); c.close()
        return {"state": "ready", "sources": len(selected), "targets": matrix_targets, "matrix_packets": manifest.get("packets", 0), "validation_tasks": len(validation_tasks), "bundled_target_reviews": bundle_targets, "missing_formats": manifest.get("missing_format_count", 0), "generation": stamp}
    finally: lock.close()


def status(db: Path = DB) -> dict:
    c = connect(db)
    try:
        states = {r[0]: r[1] for r in c.execute("SELECT state,count(*) FROM tasks GROUP BY state")}
        return {"tasks": states, "total": sum(states.values()), "generation": json.loads((c.execute("SELECT value FROM meta WHERE key='generation'").fetchone() or [None])[0] or "null")}
    finally: c.close()


class API(prune.API):
    def __init__(self, executable: str | None = None, work: Path = WORK):
        super().__init__(executable)
        self.work = work

    def create_session(self, directory: Path, title: str, agent: str):
        payload = {"title": title, "agent": agent, "model": MODEL, "location": {"directory": str(directory.resolve())}, "permissions": [
            {"action": "*", "resource": "*", "effect": "deny"},
            *[{"action": a, "resource": "*", "effect": "allow"} for a in ("read", "glob", "grep", "external_directory")]]}
        session = self.request("POST", "/api/session", payload)["data"]
        selected = session.get("model") or {}
        if selected.get("providerID") != MODEL["providerID"] or selected.get("id") != MODEL["id"]:
            raise RuntimeError(f"OpenCode returned a model different from required gpt-6-luna: {selected!r}")
        return session["id"]

    def send(self, session: str, text: str):
        # `opencode api --data` accepts one argv string, not a file/stdin source.
        # Keep that argument bounded while retaining the complete prompt on disk.
        # Match API.request's default JSON encoding so escaped Unicode counts too.
        body = json.dumps({"text": text}).encode("utf-8")
        if len(body) <= 64 * 1024:
            return self.request("POST", f"/api/session/{session}/prompt", {"text": text})
        inputs = self.work / "worker-inputs"
        dispatch_dir = inputs / uuid.uuid4().hex[:12]
        archive_path = dispatch_dir / "prompt.txt"
        segments = [text[i:i + MAX_PROMPT_SEGMENT_CHARS] for i in range(0, len(text), MAX_PROMPT_SEGMENT_CHARS)]
        lines = [json.dumps(segment, ensure_ascii=False) for segment in segments]
        if any(len(line) > MAX_PROMPT_PART_LINE_CHARS for line in lines):
            raise ValueError("Prompt segment exceeds the safe line length")
        parts: list[str] = []
        current: list[str] = []; size = len(b"[\n]\n")
        for line in lines:
            addition = len(line.encode("utf-8")) + (2 if current else 0)
            if current and size + addition > MAX_PROMPT_PART_BYTES:
                parts.append("[\n" + ",\n".join(current) + "\n]\n")
                current = []; size = len(b"[\n]\n"); addition = len(line.encode("utf-8"))
            if size + addition > MAX_PROMPT_PART_BYTES:
                raise ValueError("A single prompt segment exceeds the safe part size")
            current.append(line); size += addition
        if current or not parts: parts.append("[\n" + ",\n".join(current) + "\n]\n")
        if len(parts) > MAX_PROMPT_PARTS:
            raise ValueError(f"Prompt requires {len(parts)} parts; refusing dispatch above {MAX_PROMPT_PARTS}")
        part_names = [f"part-{index:03d}.txt" for index in range(len(parts))]
        part_paths = [dispatch_dir / name for name in part_names]
        instruction = ("Read EVERY listed part in order. Each is a JSON array of sequential exact text segments; decode each array and "
                       "concatenate its strings in order, then concatenate parts in listed order to reconstruct the complete original "
                       "read-only task. Do not treat JSON quoting or part boundaries as task instructions. If any part is missing or "
                       "unreadable, do not claim completion. Do not read the unchunked archive (its long lines may be truncated).\n"
                       f"DIRECTORY: {dispatch_dir.resolve()}\nPARTS (ordered basenames):\n" + "\n".join(part_names) +
                       f"\nUnchunked archival copy, provenance only: {archive_path.resolve()}\nReturn the requested strict JSON response.")
        dispatch_body = json.dumps({"text": instruction}).encode("utf-8")
        if len(dispatch_body) > 64 * 1024:
            raise ValueError("Prompt part list exceeds the bounded OpenCode CLI request size")
        dispatch_dir.mkdir(parents=True, exist_ok=False)
        archive_path.write_text(text, encoding="utf-8")
        for part_path, part in zip(part_paths, parts):
            part_path.write_text(part, encoding="utf-8")
        return self.request("POST", f"/api/session/{session}/prompt", {"text": instruction})

    @staticmethod
    def _packet_records(value):
        """Flatten a JSON tree into independently inspectable path/value records."""
        records = []
        def visit(node, path):
            if isinstance(node, dict):
                if not node: records.append({"path": path, "container": "object"})
                for key, child in node.items(): visit(child, [*path, key])
            elif isinstance(node, list):
                if not node: records.append({"path": path, "container": "array"})
                for index, child in enumerate(node): visit(child, [*path, index])
            elif isinstance(node, str) and len(node) > MAX_PACKET_STRING_CHARS:
                fragment_id = digest(json.dumps(path, ensure_ascii=False, separators=(",", ":")).encode())
                chunks = [node[i:i + MAX_PACKET_STRING_CHARS] for i in range(0, len(node), MAX_PACKET_STRING_CHARS)]
                checksum = digest(node.encode("utf-8"))
                records.append({"path": path, "fragment_id": fragment_id, "chunk_count": len(chunks),
                                "total_sha256": checksum, "chunk_manifest": True})
                for index, chunk in enumerate(chunks):
                    records.append({"fragment_id": fragment_id, "chunk_index": index, "text_chunk": chunk})
            else:
                records.append({"path": path, "value": node})
        visit(value, [])
        return records

    @staticmethod
    def reconstruct_packet_parts(parts):
        """Validate and reconstruct packet records; used by transport regression tests."""
        parsed = [json.loads(part) if isinstance(part, (str, bytes)) else part for part in parts]
        if not parsed: raise ValueError("no packet parts")
        count = len(parsed)
        if any(item.get("schema") != 1 or item.get("part_count") != count for item in parsed):
            raise ValueError("invalid packet part manifest")
        if sorted(item.get("part_index") for item in parsed) != list(range(count)):
            raise ValueError("missing or duplicate packet part index")
        records = [record for item in parsed for record in item.get("records", [])]
        paths = {}; manifests = {}
        chunks = {}
        for record in records:
            if "text_chunk" in record:
                key = record["fragment_id"]
                entry = chunks.setdefault(key, {"values": {}})
                index = record["chunk_index"]
                if index in entry["values"]:
                    raise ValueError("inconsistent or duplicate string chunk")
                entry["values"][index] = record["text_chunk"]
            else:
                path = tuple(record["path"])
                if record.get("chunk_manifest"):
                    fragment_id = record["fragment_id"]
                    if fragment_id in manifests: raise ValueError("duplicate string chunk manifest")
                    manifests[fragment_id] = {"path": path, "count": record["chunk_count"], "hash": record["total_sha256"]}
                    continue
                if path in paths: raise ValueError("duplicate packet value path")
                paths[path] = ("container", record["container"]) if "container" in record else ("value", record["value"])
        if set(chunks) != set(manifests): raise ValueError("missing string chunk or manifest")
        for fragment_id, entry in chunks.items():
            manifest = manifests[fragment_id]; path = manifest["path"]
            if path in paths: raise ValueError("string chunk conflicts with packet value")
            if set(entry["values"]) != set(range(manifest["count"])): raise ValueError("missing string chunk")
            text = "".join(entry["values"][i] for i in range(manifest["count"]))
            if digest(text.encode("utf-8")) != manifest["hash"]: raise ValueError("string chunk hash mismatch")
            paths[path] = ("value", text)
        root = {}
        def put(path, value):
            if not path:
                return value
            head, *tail = path
            if isinstance(head, int):
                if not isinstance(root, list): raise ValueError("array root reconstruction unsupported")
            parent = root
            for pos, part in enumerate(path[:-1]):
                nxt = path[pos + 1]
                if isinstance(parent, dict): parent = parent.setdefault(part, [] if isinstance(nxt, int) else {})
                else:
                    while len(parent) <= part: parent.append(None)
                    if parent[part] is None: parent[part] = [] if isinstance(nxt, int) else {}
                    parent = parent[part]
            final = path[-1]
            if isinstance(parent, dict): parent[final] = value
            else:
                while len(parent) <= final: parent.append(None)
                parent[final] = value
            return root
        # The packet wire root is an object; child array paths are created from their parent record.
        for path, (kind, value) in sorted(paths.items(), key=lambda pair: (len(pair[0]), json.dumps(pair[0], ensure_ascii=False))):
            material = {} if kind == "container" and value == "object" else [] if kind == "container" else value
            if not path:
                root = material
            else:
                put(path, material)
        return root

    def send_packet(self, session: str, packet: dict, aliases: dict[str, str], original_prompt: str):
        """Dispatch large structured packets as self-contained JSON record parts."""
        body = json.dumps({"text": original_prompt}).encode("utf-8")
        if len(body) <= 64 * 1024:
            return self.send(session, original_prompt)
        instruction = ("Read each JSON part independently. Records are path-addressed leaves/empty containers of the same packet; "
                       "correlate records by path and preserve target/reference provenance. text_chunk records with a shared fragment_id "
                       "and consecutive chunk_index/count are deliberate lossless pieces of one string, not missing or truncated text. "
                       "Inspect assigned sections directly; do not require manual reconstruction of one whole prompt. Use exact aliases "
                       "from id_map in the requested strict JSON response.\n"
                       "Worker task instructions: " + build_worker_prompt({}, aliases).split("PACKET:\n", 1)[0] +
                       "\nAllowed IDs: " + json.dumps(list(aliases)) + "\n")
        encoded = json.dumps({"text": instruction}).encode("utf-8")
        if len(encoded) > 64 * 1024: raise ValueError("Structured packet dispatch instruction exceeds CLI request limit")
        workdir = self.work / "worker-inputs" / uuid.uuid4().hex[:12]
        archive = workdir / "prompt.txt"
        serialized = serialize_packet_parts(packet, aliases)
        parts = [json.loads(text)["records"] for text in serialized]
        if len(parts) > MAX_PROMPT_PARTS: raise ValueError(f"Structured packet requires {len(parts)} parts; refusing dispatch above {MAX_PROMPT_PARTS}")
        structured_bytes = sum(len(text.encode("utf-8")) for text in serialized) + len(
            json.dumps({"text": "Allowed IDs: " + json.dumps(list(aliases))}).encode("utf-8"))
        if structured_bytes > MAX_WIRE_BYTES:
            raise ValueError(f"Structured packet requires {structured_bytes} bytes; refusing dispatch above {MAX_WIRE_BYTES}")
        names = [f"part-{index:03d}.json" for index in range(len(parts))]
        dispatch = instruction + f"Assigned aliases: {json.dumps(list(aliases))}\nRead packet records in: {workdir.resolve()}\nParts: " + ", ".join(names) + f"\nOriginal prompt archive (provenance): {archive.resolve()}"
        if len(json.dumps({"text": dispatch}).encode("utf-8")) > 64 * 1024: raise ValueError("Structured packet part list exceeds CLI request limit")
        workdir.mkdir(parents=True, exist_ok=False)
        archive.write_text(original_prompt, encoding="utf-8")
        for name, text in zip(names, serialized): (workdir / name).write_text(text, encoding="utf-8")
        return self.request("POST", f"/api/session/{session}/prompt", {"text": dispatch})

def run(db: Path = DB, root: Path = ROOT, workers: int = 2, max_tasks: int | None = None, retries: int = 1, timeout: int = 600, api=None,
         source_filter: list[str] | None = None, target_filter: list[str] | None = None, resume: bool = False,
         manual_retry_ids: list[str] | None = None) -> dict:
    if (root.resolve() / "doc-prune-work/campaign/ARTIFACT_REVIEW_RETIRED").exists():
        raise RuntimeError("Model-based artifact review is retired. Use offline renderer tests and direct output inspection; --resume does not enable model dispatch.")
    if manual_retry_ids is not None and (not manual_retry_ids or len(manual_retry_ids) != len(set(manual_retry_ids))):
        raise ValueError("manual retry task IDs must be nonempty and unique")
    root = root.resolve(); lock = source_lock(root); artifact_lock_path = db.parent / "artifact-validation.lock"; artifact_lock_path.parent.mkdir(parents=True, exist_ok=True); own = artifact_lock_path.open("a+")
    try:
        fcntl.flock(own, fcntl.LOCK_EX | fcntl.LOCK_NB)
        artifact_stop = root / "doc-prune-work/campaign/ARTIFACT_STOP"
        if resume:
            artifact_stop.unlink(missing_ok=True)
        elif artifact_stop.exists():
            return {"state": "stopped", "processed": 0, **status(db)}
        if live_source_tasks(root): raise RuntimeError("Source campaign has live tasks/leases")
        c = connect(db); stamp = json.loads(c.execute("SELECT value FROM meta WHERE key='generation'").fetchone()[0]);
        if stamp.get("input_fingerprint") != fingerprint(root): raise RuntimeError("Source/doclet inputs changed since artifact generation; validation invalidated")
        out = Path(c.execute("SELECT value FROM meta WHERE key='packet_dir'").fetchone()[0]); api = api or API()
        generation = stamp.get("generation_id")
        quota_stop = threading.Event()
        quota_lock = threading.Lock()
        quota_info = {}
        if not generation or c.execute("SELECT value FROM meta WHERE key='active_generation'").fetchone()[0] != generation:
            raise RuntimeError("Artifact queue generation is stale")
        manual_ids = set(manual_retry_ids or [])
        if manual_retry_ids is not None:
            rows = c.execute("SELECT id,state FROM tasks WHERE id IN (%s) AND generation=?" % ",".join("?" for _ in manual_ids),
                             (*manual_retry_ids, generation)).fetchall() if manual_ids else []
            eligible = {row["id"] for row in rows if row["state"] == "blocked"}
            if eligible != manual_ids:
                c.close()
                raise ValueError("manual retry IDs must name blocked tasks in the current generation; invalid: " +
                                 ", ".join(sorted(manual_ids - eligible)))
        # Recover interrupted read-only attempts conservatively. Unknown/live sessions
        # remain blocked and retain their session ID; they are never dispatched twice.
        for stale in c.execute("SELECT * FROM tasks WHERE generation=? AND state='running'", (generation,)).fetchall():
            sid = stale["session_id"]
            safe = sid is None
            if sid:
                try:
                    api.request("POST", f"/api/session/{sid}/interrupt", {}, timeout=min(timeout, 30))
                    active = api.request("GET", "/api/session/active", timeout=min(timeout, 30)).get("data", [])
                    info = api.request("GET", f"/api/session/{sid}", timeout=min(timeout, 30)).get("data", {})
                    status_value = info.get("status", {})
                    idle = status_value == "idle" or (isinstance(status_value, dict) and status_value.get("type") == "idle")
                    safe = idle and not any((item.get("id") if isinstance(item, dict) else item) == sid for item in active)
                except Exception: safe = False
            state = ("retry" if stale["attempts"] <= retries else "blocked") if safe else "blocked"
            note = "Recovered after confirming session idle" if safe else "Recovery could not confirm session idle; retained session and blocked"
            c.execute("UPDATE tasks SET state=?,last_error=? WHERE id=?", (state, note, stale["id"]))
        c.commit()
        c.close()

        def process(task):
            conn = connect(db); sid = None; transcript = None; raw = None; attempt_id = conn.execute("INSERT INTO attempts(task_id,started) VALUES(?,?)", (task["id"], time.time())).lastrowid; conn.commit()
            try:
                if stamp.get("input_fingerprint") != fingerprint(root): raise RuntimeError("Source/doclet inputs changed during review; stop and regenerate")
                packet_path = out / task["packet"]; packet = json.loads(packet_path.read_text(encoding="utf-8")); ids = packet.get("assigned_ids", [])
                if not ids or len(ids) != len(set(ids)): raise RuntimeError("empty or duplicate assigned contract IDs; blocked")
                previously_completed = set(json.loads(task.get("completed") or "[]"))
                ids = [ident for ident in ids if ident not in previously_completed]
                if not ids:
                    conn.execute("UPDATE tasks SET state='passed' WHERE id=?", (task["id"],)); conn.commit(); return
                source_path = root / packet["source"]
                if not source_path.is_file() or digest(source_path.read_bytes()) != packet.get("source_sha256"):
                    raise RuntimeError("source changed since packet creation")
                target_views = packet.get("targets") or [{"target": packet.get("target"), "assigned_ids": ids,
                    "artifacts": packet.get("artifacts", {}), "output_snapshot": packet.get("output_snapshot", {}),
                    "format_errors": packet.get("format_errors", {}), "missing_formats": packet.get("missing_formats", []),
                    "fragment_gaps": packet.get("fragment_gaps", {}), "ambiguous_output_names": packet.get("ambiguous_output_names", [])}]
                if packet.get("targets"):
                    active_targets = {ident.split("::", 1)[0] for ident in ids if "::" in ident}
                    target_views = [view for view in target_views if view["target"] in active_targets]
                contract_keys = {ident.split("::", 1)[-1] for ident in ids}
                review_packet = {**packet, "targets": target_views, "assigned_ids": ids,
                                 "source_contract": [entry for entry in packet.get("source_contract", []) if entry.get("key") in contract_keys]}
                aliases = wire_aliases(ids)
                prompt_packet = wire_packet(review_packet, aliases)
                output_blockers = []
                for target_view in target_views:
                    target = target_view["target"]
                    snapshots = target_view.get("output_snapshot", {})
                    for fmt in artifacts.FORMATS:
                        info = snapshots.get(fmt, {}); files = info.get("files", [])
                        if info.get("state") != "present" or not files or target_view.get("format_errors", {}).get(fmt):
                            output_blockers.append(f"{target}:{fmt}:missing-or-unparsable"); continue
                        for file in files:
                            path = root / file["path"]
                            if not path.is_file() or digest(path.read_bytes()) != file.get("sha256"):
                                output_blockers.append(f"{target}:{fmt}:stale:{file['path']}")
                prompt = build_worker_prompt(prompt_packet, aliases)
                sid = api.create_session(root, f"docs artifact validation {task['source']} {task['target']}", "docs-validator")
                conn.execute("UPDATE tasks SET session_id=? WHERE id=?", (sid, task["id"])); conn.execute("UPDATE attempts SET session_id=? WHERE id=?", (sid, attempt_id)); conn.commit()
                if callable(getattr(api, "send_packet", None)):
                    api.send_packet(sid, prompt_packet, aliases, prompt)
                else:
                    api.send(sid, prompt)
                api.request("POST", f"/api/experimental/session/{sid}/wait", {}, timeout=timeout)
                session_info = api.request("GET", f"/api/session/{sid}").get("data", {})
                outcome = session_info.get("outcome")
                # Keep transport bounded: only the final assistant answer is evidence.
                # Earlier messages are never searched as a fallback for malformed output.
                messages = api.request("GET", f"/api/session/{sid}/message?order=desc&type=assistant&limit=1").get("data", [])
                transcript = json.dumps(messages, ensure_ascii=False)
                if not messages:
                    if outcome in ("failed", "interrupted"):
                        raise RuntimeError(f"Worker session outcome={outcome}; latest assistant message unavailable")
                    raise RuntimeError("Worker session outcome failed: wait completed without a final assistant message")
                latest = messages[0]
                finish = latest.get("finish")
                worker_error = latest.get("error")
                if isinstance(worker_error, dict) and worker_error.get("type") == "provider.quota":
                    detail = {"type": "provider.quota", "message": worker_error.get("message", "")}
                    with quota_lock:
                        quota_info.update(detail)
                        quota_stop.set()
                    raise RuntimeError(f"Worker provider error type={detail['type']}: {detail['message']}")
                if outcome in ("failed", "interrupted"):
                    raise RuntimeError(f"Worker session outcome={outcome}; latest assistant error={worker_error!r}")
                if worker_error or (finish is not None and finish != "stop"):
                    raise RuntimeError(f"Worker session outcome failed: finish={finish!r}, error={worker_error!r}")
                raw = select_final_response_text(latest)
                if not raw.strip():
                    raise RuntimeError(f"Worker session outcome failed: finish={finish!r}, final assistant text missing")
                obj = json.loads(raw)
                if set(obj) != {"completed", "unresolved", "findings", "summary"} or not isinstance(obj["completed"], list) or not isinstance(obj["unresolved"], list) or not isinstance(obj["findings"], list): raise ValueError("invalid strict response schema")
                obj = canonicalize_response_ids(obj, aliases, ids)
                if any(x not in ids for x in obj["completed"]): raise ValueError("unknown completed ID")
                for collection in ("unresolved", "findings"):
                    if any(not isinstance(x, dict) or x.get("id") not in ids or not x.get("reason") for x in obj[collection]): raise ValueError("invalid finding/unresolved ID")
                target_blocked = any(view.get("missing_formats") or view.get("ambiguous_output_names") or any(view.get("fragment_gaps", {}).values()) for view in target_views)
                all_completed = previously_completed | set(obj["completed"])
                remaining_ids = set(packet.get("assigned_ids", [])) - all_completed
                previous_unresolved = json.loads(task.get("unresolved") or "[]")
                previous_findings = json.loads(task.get("findings") or "[]")
                all_unresolved = previous_unresolved + [item for item in obj["unresolved"] if item not in previous_unresolved]
                all_findings = previous_findings + [item for item in obj["findings"] if item not in previous_findings]
                issue_free = not all_unresolved and not all_findings
                if not remaining_ids and issue_free and not target_blocked and not output_blockers: state = "passed"
                elif remaining_ids and all_completed and task["attempts"] <= retries and task["id"] not in manual_ids: state = "retry"
                else: state = "blocked"
                if fingerprint(root) != stamp["input_fingerprint"]: raise RuntimeError("source/doclet inputs changed during review")
                for target_view in target_views:
                    for info in target_view.get("output_snapshot", {}).values():
                        for file in info.get("files", []):
                            path = root / file["path"]
                            if file.get("sha256") and (not path.is_file() or digest(path.read_bytes()) != file.get("sha256")): raise RuntimeError("artifact changed during review")
                conn.execute("UPDATE tasks SET state=?,completed=?,unresolved=?,findings=?,last_error=NULL WHERE id=?", (state, json.dumps(sorted(all_completed)), json.dumps(all_unresolved), json.dumps(all_findings), task["id"]))
                conn.execute("UPDATE attempts SET ended=?,transcript=?,response=? WHERE id=?", (time.time(), transcript, raw, attempt_id)); conn.commit()
            except Exception as exc:
                safe = sid is None
                if sid:
                    try:
                        api.request("POST", f"/api/session/{sid}/interrupt", {})
                        active = api.request("GET", "/api/session/active").get("data", [])
                        safe = not any((item.get("id") if isinstance(item, dict) else item) == sid for item in active)
                    except Exception: safe = False
                state = "retry" if task["id"] not in manual_ids and safe and task["attempts"] <= retries and not quota_stop.is_set() else "blocked"
                conn.execute("UPDATE tasks SET state=?,last_error=? WHERE id=?", (state, str(exc), task["id"]))
                conn.execute("UPDATE attempts SET ended=?,transcript=COALESCE(?,transcript),response=COALESCE(?,response),error=? WHERE id=?", (time.time(), transcript, raw, str(exc), attempt_id)); conn.commit()
            finally: conn.close()

        def claim_one():
            # Serialize claim checks with quota detection so no claimant can pass
            # the run-scoped stop after a worker has observed provider exhaustion.
            with quota_lock:
                if quota_stop.is_set(): return None
                return claim_one_locked()

        dispatched_manual_ids = set()
        def claim_one_locked():
            conn = connect(db)
            try:
                conn.execute("BEGIN IMMEDIATE")
                if manual_retry_ids is not None:
                    candidates = conn.execute("SELECT * FROM tasks WHERE generation=? AND id IN (%s) AND state='blocked' ORDER BY source,target" % ",".join("?" for _ in manual_ids),
                                              (generation, *manual_retry_ids)).fetchall() if manual_ids else []
                    candidates = [row for row in candidates if row["id"] not in dispatched_manual_ids]
                else:
                    query = "SELECT * FROM tasks WHERE generation=? AND state IN ('pending','retry') AND attempts<=? ORDER BY source,target"
                    candidates = conn.execute(query, (generation, retries)).fetchall()
                def target_match(row):
                    if not target_filter: return True
                    try: listed = json.loads(row["target"])
                    except (TypeError, json.JSONDecodeError): listed = [row["target"]]
                    return set(listed).issubset(set(target_filter))
                chosen = next((row for row in candidates if (not source_filter or any(fnmatch.fnmatchcase(row["source"], pat) for pat in source_filter)) and target_match(row)), None)
                if chosen is None:
                    conn.commit(); return None
                allowed_states = "state='blocked'" if manual_retry_ids is not None else "state IN ('pending','retry')"
                changed = conn.execute(f"UPDATE tasks SET state='running',attempts=attempts+1,last_error=NULL WHERE id=? AND {allowed_states}", (chosen["id"],)).rowcount
                if changed and manual_retry_ids is not None: dispatched_manual_ids.add(chosen["id"])
                claimed = conn.execute("SELECT * FROM tasks WHERE id=?", (chosen["id"],)).fetchone() if changed else None
                conn.commit()
                return dict(claimed) if claimed else None
            finally: conn.close()

        processed = 0
        with ThreadPoolExecutor(max_workers=max(1, min(workers, 2))) as pool:
            pending = set()
            while pending or (max_tasks is None or processed < max_tasks):
                while len(pending) < max(1, min(workers, 2)) and (max_tasks is None or processed + len(pending) < max_tasks) and not artifact_stop.exists():
                    task = claim_one()
                    if task is None: break
                    pending.add(pool.submit(process, task))
                if not pending: break
                finished, pending = wait(pending, return_when=FIRST_COMPLETED)
                for future in finished: future.result(); processed += 1
        result = {"processed": processed, **status(db)}
        if quota_stop.is_set():
            result.update({"state": "quota_stopped", "quota": dict(quota_info)})
        return result
    finally: own.close(); lock.close()


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__); sub = p.add_subparsers(dest="command", required=True)
    prep = sub.add_parser("prepare"); prep.add_argument("--generate", action="store_true"); prep.add_argument("--source", action="append"); prep.add_argument("--target", action="append"); prep.add_argument("--no-bundle", action="store_true", help="create separate source/target validation tasks")
    sub.add_parser("status")
    stop = sub.add_parser("stop", help="stop new artifact-validation task claims")
    r = sub.add_parser("run"); r.add_argument("--workers", type=int, default=2); r.add_argument("--max-tasks", type=int); r.add_argument("--retries", type=int, default=1); r.add_argument("--timeout", type=int, default=600); r.add_argument("--source", action="append"); r.add_argument("--target", action="append"); r.add_argument("--resume", action="store_true", help="clear ARTIFACT_STOP and resume claims"); r.add_argument("--retry-task", action="append", dest="retry_task", help="manually retry one blocked task in the current generation (repeatable)")
    a = p.parse_args(argv)
    if a.command == "prepare": result = prepare(generate=a.generate, source_filter=a.source, target_filter=a.target, bundle_targets=not a.no_bundle)
    elif a.command == "status": result = status()
    elif a.command == "stop":
        ARTIFACT_STOP.parent.mkdir(parents=True, exist_ok=True); ARTIFACT_STOP.write_text("artifact validation stop requested\n", encoding="utf-8")
        result = {"stop_requested": True, "stop_file": str(ARTIFACT_STOP)}
    else: result = run(workers=a.workers, max_tasks=a.max_tasks, retries=a.retries, timeout=a.timeout, source_filter=a.source, target_filter=a.target, resume=a.resume, manual_retry_ids=a.retry_task)
    print(json.dumps(result, indent=2, sort_keys=True)); return 1 if result.get("state") == "blocked" else 0


if __name__ == "__main__": raise SystemExit(main())
