#!/usr/bin/env python3
"""Build deterministic, review-only packets from generated API artifacts.

No packet produced here is a correctness verdict. Parsers establish presence and
extractable text only; a reviewer must compare the source contract and renderings.
"""
from __future__ import annotations

import argparse
import ast
from bisect import bisect_right
import hashlib
from html.parser import HTMLParser
import json
import io
from pathlib import Path
import re
import sys
import tokenize
from collections import defaultdict
from functools import lru_cache


FORMATS = ("Python", "TypeScript", "HTML", "VitePress")
MAX_FRAGMENT = 1600
PYTHON_KEYWORDS = frozenset((
    "False", "await", "else", "import", "pass", "None", "break", "except",
    "in", "raise", "True", "class", "finally", "is", "return", "and",
    "continue", "for", "lambda", "try", "as", "def", "from", "nonlocal",
    "while", "assert", "del", "global", "not", "with", "async", "elif",
    "if", "or", "yield",
))


def digest(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def python_output_name(name: str) -> str:
    """Match PythonWriter.getVarName without duplicating its keyword set."""
    return name + "_" if name in PYTHON_KEYWORDS else name


def _mask_java_annotations(source: str) -> str:
    """Blank annotation syntax for declaration matching, preserving offsets/text."""
    chars = list(source); i = 0; quote = None; line = block = False
    while i < len(source):
        c = source[i]; n = source[i + 1] if i + 1 < len(source) else ""
        if line:
            if c == "\n": line = False
            i += 1; continue
        if block:
            if c == "*" and n == "/": block = False; i += 2
            else: i += 1
            continue
        if quote:
            if c == "\\": i += 2
            elif c == quote: quote = None; i += 1
            else: i += 1
            continue
        if c == "/" and n == "/": line = True; i += 2; continue
        if c == "/" and n == "*": block = True; i += 2; continue
        if c in "'\"": quote = c; i += 1; continue
        if c != "@": i += 1; continue
        end = i + 1
        while end < len(source) and (source[end].isalnum() or source[end] in "_.$"): end += 1
        if end == i + 1: i += 1; continue
        while end < len(source) and source[end].isspace(): end += 1
        if end < len(source) and source[end] == "(":
            depth = 0; q = None; j = end
            while j < len(source):
                ch = source[j]
                if q:
                    if ch == "\\": j += 2; continue
                    if ch == q: q = None
                elif ch in "'\"": q = ch
                elif ch == "(": depth += 1
                elif ch == ")":
                    depth -= 1
                    if depth == 0:
                        j += 1; break
                j += 1
            end = j
        for pos in range(i, end):
            if chars[pos] not in "\r\n": chars[pos] = " "
        i = end
    return "".join(chars)


def normalize(text: str) -> str:
    text = re.sub(r"https?://[^\s)\]]+", "<URL>", text)
    return "\n".join(line.rstrip() for line in text.splitlines()).strip()


def clipped(text: str) -> str:
    text = text.strip()
    return text if len(text) <= MAX_FRAGMENT else text[:MAX_FRAGMENT] + "…[truncated]"


def source_contract(path: Path) -> dict:
    """Lex Java doc comments and declaration headers without guessing overloads."""
    text = path.read_text(encoding="utf-8")
    package = re.search(r"(?m)^\s*package\s+([\w.]+)\s*;", text)
    comments = []; type_decls = []; unresolved = []
    i = 0; quote = None; line = block = False; brace_depth = 0; type_stack = []; pending_type = None
    while i < len(text):
        c = text[i]; n = text[i+1] if i+1 < len(text) else ""
        if line:
            if c == "\n": line = False
        elif block:
            if c == "*" and n == "/": block = False; i += 1
        elif quote:
            if quote == '"""' and text.startswith('"""', i): quote = None; i += 2
            elif quote != '"""' and c == "\\": i += 1
            elif quote != '"""' and c == quote: quote = None
        elif c == "/" and n == "/": line = True; i += 1
        elif c == "/" and n == "*":
            end = text.find("*/", i + 2)
            if end < 0: break
            if i + 2 < len(text) and text[i+2] == "*": comments.append((i, end + 2, text[i+3:end]))
            i = end + 1
        elif text.startswith('"""', i): quote = '"""'; i += 2
        elif c in "'\"": quote = c
        elif c == "{":
            brace_depth += 1
            if pending_type:
                type_stack.append((pending_type["name"], brace_depth, pending_type)); pending_type = None
        elif c == "}":
            brace_depth = max(0, brace_depth - 1)
            while type_stack and type_stack[-1][1] > brace_depth: type_stack.pop()[2].update(end=i)
        elif c.isalpha() or c == "_":
            if i == 0 or not (text[i-1].isalnum() or text[i-1] in "_$"):
                m = re.match(r"(?:class|interface|enum|record)\s+(\w+)", text[i:])
            else: m = None
            if m:
                owner = ".".join(x[0] for x in type_stack)
                pending_type = {"name": m.group(1), "start": i, "owner": owner, "depth": brace_depth, "end": len(text)}
                type_decls.append(pending_type)
        i += 1
    entries = []
    for start, end, raw in comments:
        # Only inspect the immediate declaration prefix; never cross another comment.
        boundary = min((x for x in (text.find("/**", end), text.find("/*", end), text.find("//", end)) if x >= 0), default=len(text))
        between = text[end:boundary]
        searchable = _mask_java_annotations(between)
        candidates = []
        for kind_name, pattern in (
            ("type", r"\b(class|interface|enum|record)\s+(\w+)"),
            ("member", r"(?:[\w.<>,?\[\] @]+\s+)?(\w+)\s*\([^;{}]*\)\s*(?:throws\s+[\w., ]+)?\s*[;{]"),
            ("field", r"(?:public|protected|private)\s+(?:static\s+|final\s+|volatile\s+)*[\w.<>,?\[\]]+\s+(\w+)\s*(?:=|;)")
        ):
            candidate = re.search(pattern, searchable)
            if candidate: candidates.append((candidate.start(), kind_name, candidate))
        _, decl_kind, decl = min(candidates, key=lambda x: x[0]) if candidates else (0, "", None)
        if not decl:
            unresolved.append({"offset": start, "raw_javadoc": raw, "reason": "no_following_declaration"})
            continue
        lines = [re.sub(r"^\s*\* ?", "", ln).rstrip() for ln in raw.splitlines()]
        body, tags, active = [], [], False
        for line in lines:
            if line.lstrip().startswith("@"):
                active = True
                tags.append(line.strip())
            elif active:
                if tags: tags[-1] += " " + line.strip()
            else:
                body.append(line)
        bodytext = re.sub(r"\{@(code|literal)\s+([^}]+)\}", r"\2", "\n".join(body))
        bodytext = re.sub(r"\{@link\s+([^}]+)\}", r"\1", bodytext)
        bodytext = re.sub(r"<[^>]*>", " ", bodytext)
        bodytext = re.sub(r"\s+", " ", bodytext).strip()
        kind = "type" if decl_kind == "type" else "member"
        name = decl.group(2) if decl_kind == "type" else decl.group(1)
        # Preserve signatures; same-name overloads are deliberately not merged.
        header = re.sub(r"\s+", " ", between.strip().split("{")[0].split(";")[0])[:300]
        key = f"{kind}:{name}" + (f":{header}" if kind == "member" else "")
        if kind == "type":
            td = next((t for t in type_decls if t["start"] >= end), None)
            if td:
                key = "type:" + (td["owner"] + "." if td["owner"] else "") + name
        else:
            td = None
        enclosing = [t for t in type_decls if t["start"] <= start < t["end"]]
        if td:
            owner = td["owner"]
        elif enclosing:
            enclosing.sort(key=lambda t: t["depth"])
            owner = ".".join(x["name"] for x in enclosing)
        else:
            owner = ""
        package_prefix = (package.group(1) + ".") if package else ""
        if kind == "type" and td:
            qualified_owner = package_prefix + (td["owner"] + "." if td["owner"] else "") + name
            key = "type:" + qualified_owner
        else:
            qualified_owner = package_prefix + owner
            key = f"member:{qualified_owner}#{name}:{header}"
        entries.append({"key": key, "name": name, "kind": kind, "owner": owner,
                        "owner_qualified": qualified_owner,
                        "body": bodytext, "raw_javadoc": raw, "tags": tags, "signature": header,
                        "excluded": "@DocletIgnore" in text[max(0, start-300):start]})
    top_types = [t for t in type_decls if t["depth"] == 0]
    # A source file's identity is its top-level Java declaration, not whichever
    # nested type happens to have the first Javadoc comment.
    top_name = next((t["name"] for t in top_types if t["name"] == path.stem), None)
    if top_name is None and len(top_types) == 1: top_name = top_types[0]["name"]
    fqcn = (package.group(1) + "." if package else "") + (top_name or path.stem)
    owned_types = []
    for t in type_decls:
        full = (package.group(1) + "." if package else "") + (t["owner"] + "." if t["owner"] else "") + t["name"]
        owned_types.append({"qualified_type": full, "name": t["name"], "owner": t["owner"], "depth": t["depth"]})
    return {"qualified_type": fqcn, "owned_types": owned_types, "entries": entries,
            "extraction_unresolved": unresolved, "source_sha256": digest(text)}


class TextHTML(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True); self.parts = []; self.skip = 0; self.links = []; self.active_link = None; self.elements = []
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        self.elements.append({"tag": tag, "attrs": attrs})
        if tag == "a":
            self.active_link = {"href": attrs.get("href"), "label": ""}
        if tag in ("script", "style"): self.skip += 1
        if tag in ("p", "pre", "div", "li", "h1", "h2", "h3", "br"): self.parts.append("\n")
    def handle_endtag(self, tag):
        if tag == "a" and self.active_link is not None:
            self.links.append(self.active_link); self.active_link = None
        if tag in ("script", "style") and self.skip: self.skip -= 1
        if tag in ("p", "pre", "div", "li", "h1", "h2", "h3"): self.parts.append("\n")
    def handle_data(self, data):
        if not self.skip:
            self.parts.append(data)
            if self.active_link is not None: self.active_link["label"] += data


class BoundedHTMLParser(HTMLParser):
    """Locate balanced class/member spans and retain their exact source markup."""
    VOID = {"area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"}

    def __init__(self, raw: str):
        super().__init__(convert_charrefs=False)
        self.raw = raw
        self.lines = [0]
        self.lines.extend(i + 1 for i, char in enumerate(raw) if char == "\n")
        self.stack = []
        self.items = []
        self.titles = []
        self.h3_starts = []
        self.error = None

    def source_offset(self):
        line, col = self.getpos()
        return self.lines[line - 1] + col

    def handle_starttag(self, tag, attrs):
        start = self.source_offset()
        values = dict(attrs)
        classes = set((values.get("class") or "").split())
        frame = {"tag": tag, "start": start, "attrs": values, "classes": classes}
        if tag == "h2" and "classTitle" in classes:
            self.titles.append(frame)
        if tag == "h3":
            self.h3_starts.append(start)
        if tag not in self.VOID:
            self.stack.append(frame)

    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)
        if tag not in self.VOID:
            self.handle_endtag(tag)

    def handle_endtag(self, tag):
        end_start = self.source_offset()
        close = self.raw.find(">", end_start)
        end = close + 1 if close >= 0 else end_start
        found = next((i for i in range(len(self.stack) - 1, -1, -1) if self.stack[i]["tag"] == tag), None)
        if found is None:
            return
        popped = self.stack[found:]
        del self.stack[found:]
        for frame in popped:
            if frame["tag"] == "h2" and "classTitle" in frame["classes"]:
                frame["end"] = end
            if frame["tag"] == "div" and "classItem" in frame["classes"]:
                kind = next((k for k in ("constructor", "method", "field") if k in frame["classes"]), None)
                if kind:
                    ident = frame["attrs"].get("id")
                    if not ident:
                        self.error = "classItem_missing_id"
                        continue
                    name = ident.split("-", 1)[0]
                    self.items.append({"kind": kind, "id": ident, "name": name, "start": frame["start"], "end": end})


def _web_fragment(raw: str, owner: str, name: str, kind: str, anchor: str | None = None) -> dict:
    parser = TextHTML(); parser.feed(raw)
    return {"owner": owner, "name": name, "kind": kind, "text": " ".join(parser.parts),
            "raw_html" if kind != "declaration" else "raw_html": raw,
            "links": parser.links, "elements": parser.elements, "anchor": anchor,
            "oversize": len(raw) > MAX_FRAGMENT}


def web_fragments(path: Path, fmt: str, owner: str) -> tuple[list[dict], str | None]:
    """Extract a class overview and bounded member sections from rendered docs."""
    try: raw = path.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as exc: return [], type(exc).__name__
    simple = owner.rsplit(".", 1)[-1]
    if fmt == "HTML":
        parser = BoundedHTMLParser(raw); parser.feed(raw); parser.close()
        if parser.error: return [], parser.error
        titles = [x for x in parser.titles if "end" in x]
        if len(titles) != 1: return [], f"class_title_missing_or_ambiguous:{len(titles)}"
        title = titles[0]
        next_section = next((x for x in parser.h3_starts if x >= title["end"]), None)
        if next_section is None: return [], "class_member_sections_missing"
        ids = [item["id"] for item in parser.items]
        if len(ids) != len(set(ids)): return [], "classItem_duplicate_ids"
        fragments = [_web_fragment(raw[title["start"]:next_section], owner, simple, "declaration", simple)]
        for item in sorted(parser.items, key=lambda x: x["start"]):
            section = raw[item["start"]:item["end"]]
            name = simple if item["kind"] == "constructor" else item["name"]
            fragments.append(_web_fragment(section, owner, name, "member", item["id"]))
        if any(frame["tag"] == "div" and "classItem" in frame["classes"] for frame in parser.stack):
            return [], "classItem_unbalanced"
        return fragments, None
    if fmt != "VitePress": return [], f"unsupported_format:{fmt}"

    lines = raw.splitlines(keepends=True)
    starts = []; cursor = 0
    for line in lines: starts.append(cursor); cursor += len(line)
    sections = [(i, line.strip()) for i, line in enumerate(lines) if re.match(r"^##\s+", line)]
    if not sections: return [], "markdown_member_sections_missing"
    overview_end = starts[sections[0][0]]
    fragments = [{"owner": owner, "name": simple, "kind": "declaration", "text": raw[:overview_end],
                  "raw_markdown": raw[:overview_end], "anchor": None,
                  "oversize": overview_end > MAX_FRAGMENT}]
    for si, (line_index, heading) in enumerate(sections):
        section_end_line = sections[si + 1][0] if si + 1 < len(sections) else len(lines)
        headings = [(j, lines[j].strip()) for j in range(line_index + 1, section_end_line)
                    if re.match(r"^###\s+", lines[j])]
        for hi, (item_line, item_heading) in enumerate(headings):
            end_line = headings[hi + 1][0] if hi + 1 < len(headings) else section_end_line
            match = re.match(r"^###\s+(.*?)\s*(?:\{#([^}]+)\})?\s*$", item_heading)
            if not match: return [], f"markdown_heading_unparsed:{item_line + 1}"
            label, anchor = match.group(1).strip(), match.group(2)
            section_name = heading[3:].strip().lower()
            if section_name.startswith("constructor"):
                kind, name = "member", simple
            elif section_name.startswith("field"):
                kind, name = "member", label
            elif section_name.startswith("method"):
                kind, name = "member", label.split("(", 1)[0].strip()
            else:
                continue
            start_offset = starts[item_line]
            end_offset = starts[end_line] if end_line < len(starts) else len(raw)
            content = raw[start_offset:end_offset]
            fragments.append({"owner": owner, "name": name, "kind": kind, "text": content,
                              "raw_markdown": content, "anchor": anchor,
                              "oversize": len(content) > MAX_FRAGMENT})
    anchors = [fragment["anchor"] for fragment in fragments if fragment.get("anchor")]
    if len(anchors) != len(set(anchors)): return [], "markdown_duplicate_anchors"
    return fragments, None


def py_fragments(path: Path) -> tuple[list[dict], str | None]:
    text = path.read_text(encoding="utf-8")
    try: tree = ast.parse(text)
    except SyntaxError as exc: return [], f"SyntaxError:{exc.lineno}:{exc.msg}"
    out = []
    lines = text.splitlines(keepends=True)
    line_offsets = [0]
    for line in lines:
        line_offsets.append(line_offsets[-1] + len(line))

    def declaration(node):
        """Return source from decorators (if any) through the suite colon."""
        decorators = [ast.get_source_segment(text, dec) or ast.unparse(dec)
                      for dec in getattr(node, "decorator_list", ())]
        first = min((dec.lineno for dec in node.decorator_list), default=node.lineno)
        start = line_offsets[first - 1] + (node.decorator_list[0].col_offset if node.decorator_list else node.col_offset)
        # Decorators can be on different lines; start at the first decorator's indentation.
        if node.decorator_list:
            start = line_offsets[first - 1] + max(0, node.decorator_list[0].col_offset - 1)
        depth = 0
        for tok in tokenize.generate_tokens(io.StringIO(text[start:]).readline):
            if tok.type != tokenize.OP:
                continue
            if tok.string in "([{": depth += 1
            elif tok.string in ")]}": depth -= 1
            elif tok.string == ":" and depth == 0:
                end_line, end_col = tok.end
                end = start + sum(len(line) for line in text[start:].splitlines(keepends=True)[:end_line - 1]) + end_col
                return text[start:end].strip(), decorators
        return ast.get_source_segment(text, node) or ast.unparse(node), decorators

    def visit(node, owners=()):
        if isinstance(node, ast.ClassDef):
            doc = ast.get_docstring(node, clean=False) or ""
            owner = owners + (node.name,)
            header, decorators = declaration(node)
            out.append({"owner": ".".join(owner), "name": node.name, "kind": "class", "declaration": header, "decorators": decorators, "text": doc})
            body = node.body
            for ix, child in enumerate(body):
                if isinstance(child, (ast.FunctionDef, ast.AsyncFunctionDef)):
                    sig = ast.unparse(child.args)
                    header, decorators = declaration(child)
                    out.append({"owner": ".".join(owner), "name": child.name, "kind": "method", "signature": sig, "declaration": header, "return_annotation": ast.unparse(child.returns) if child.returns else None, "decorators": decorators, "text": ast.get_docstring(child, clean=False) or ""})
                elif isinstance(child, ast.AnnAssign) and isinstance(child.target, ast.Name):
                    # The Python doclet emits a standalone string expression after
                    # an annotated field as its field documentation.
                    field_doc = ""
                    if ix + 1 < len(body) and isinstance(body[ix + 1], ast.Expr) and isinstance(body[ix + 1].value, ast.Constant) and isinstance(body[ix + 1].value.value, str):
                        field_doc = body[ix + 1].value.value
                    out.append({"owner": ".".join(owner), "name": child.target.id, "kind": "field", "annotation": ast.unparse(child.annotation), "declaration": ast.get_source_segment(text, child) or ast.unparse(child), "text": field_doc})
                elif isinstance(child, ast.ClassDef): visit(child, owner)
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            header, decorators = declaration(node)
            out.append({"owner": ".".join(owners), "name": node.name, "kind": "method", "signature": ast.unparse(node.args), "declaration": header, "return_annotation": ast.unparse(node.returns) if node.returns else None, "decorators": decorators, "text": ast.get_docstring(node, clean=False) or ""})
    for node in tree.body: visit(node)
    return out, None


def ts_fragments(path: Path) -> tuple[list[dict], str | None]:
    text = path.read_text(encoding="utf-8")
    # Build lexical brace pairs first: comments, strings, and template literals do
    # not contribute braces. Regions retain exact enclosing namespace/class paths.
    pairs = {}; stack = []; doc_spans = []; i = 0; quote = None; line = block = False
    while i < len(text):
        c = text[i]; n = text[i+1] if i+1 < len(text) else ""
        if line:
            if c == "\n": line = False
        elif block:
            if c == "*" and n == "/": block = False; i += 1
        elif quote:
            if c == "\\": i += 1
            elif c == quote: quote = None
        elif c == "/" and n == "/": line = True; i += 1
        elif c == "/" and n == "*":
            end = text.find("*/", i + 2)
            if end < 0: return [], f"SyntaxError:unterminated_comment:{i}"
            if i + 2 < len(text) and text[i+2] == "*": doc_spans.append((i, end + 2, text[i:end+2]))
            i = end + 1
        elif c in "'\"`": quote = c
        elif c == "{": stack.append(i)
        elif c == "}" and stack: pairs[stack.pop()] = i
        i += 1
    # Declaration matching must not see keywords/braces in comments or strings:
    # otherwise prose such as "class at runtime {" can consume the next real
    # declaration in the regex's [^{;]* header span.
    code = list(text); i = 0; quote = None; line = block = False
    while i < len(text):
        c = text[i]; n = text[i + 1] if i + 1 < len(text) else ""
        if line:
            if c == "\n": line = False
            else: code[i] = " "
        elif block:
            if c == "*" and n == "/":
                code[i] = code[i + 1] = " "; block = False; i += 1
            elif c not in "\r\n": code[i] = " "
        elif quote:
            if c == "\\":
                code[i] = " "
                if i + 1 < len(text): code[i + 1] = " "
                i += 1
            elif c == quote:
                code[i] = " "; quote = None
            elif c not in "\r\n": code[i] = " "
        elif c == "/" and n == "/":
            code[i] = code[i + 1] = " "; line = True; i += 1
        elif c == "/" and n == "*":
            code[i] = code[i + 1] = " "; block = True; i += 1
        elif c in "'\"`":
            code[i] = " "; quote = c
        i += 1
    declaration_text = "".join(code)
    regions = []
    decl_re = re.compile(r"(?m)(?:export\s+|declare\s+|abstract\s+|default\s+)*(namespace|class|interface)\s+([\w.$]+)[^{;]*\{")
    for m in decl_re.finditer(declaration_text):
        opening = declaration_text.find("{", m.start(), m.end())
        if opening in pairs:
            kind, name = m.group(1), m.group(2)
            parent = [r for r in regions if r["start"] < m.start() < r["end"]]
            regions.append({"kind": kind, "name": name, "start": opening, "end": pairs[opening], "decl_start": m.start(), "header": re.sub(r"\s+", " ", m.group(0)).strip(), "parent": parent[-1] if parent else None})
    regions.sort(key=lambda r: r["start"])
    for r in regions:
        chain = []; p = r
        while p:
            chain.append(p["name"]); p = p["parent"]
        r["owner_path"] = ".".join(reversed(chain))
    out = []
    doc_ends = [doc[1] for doc in doc_spans]
    for r in regions:
        preceding = bisect_right(doc_ends, r["decl_start"]) - 1
        comment = ""
        if preceding >= 0:
            doc = doc_spans[preceding]
            if not text[doc[1]:r["decl_start"]].strip():
                comment = doc[2]
        out.append({"name": r["name"], "kind": "declaration", "declaration_kind": r["kind"], "owner_path": r["owner_path"], "signature": r["header"][:500], "text": comment})
        if r["kind"] == "namespace": continue
        body_start, body_end = r["start"] + 1, r["end"]
        # Direct members only: exclude nested declarations and their contents.
        nested = [x for x in regions if x is not r and body_start < x["start"] < body_end]
        direct = body_start
        while direct < body_end:
            child = next((x for x in nested if x["start"] >= direct), None)
            end = child["decl_start"] if child else body_end
            segment = text[direct:end]
            # TS emitters may order property modifiers as either `readonly
            # static` or `static readonly`; static readonly fields are common
            # for Java static final members.
            member_pattern = r"(?s)(/\*\*.*?\*/)?\s*((?:(?:readonly|static)\s+)*(?:constructor\s*\([^;{}]*\)\s*;|\w+\s*(?:<[^;{}]*>)?\??\s*\([^;{}]*\)\s*:\s*[^;{}]+;|\w+\??\s*:\s*[^;{}]+;))"
            for mm in re.finditer(member_pattern, segment):
                comment, sig = mm.group(1) or "", re.sub(r"\s+", " ", mm.group(2)).strip()
                nm = re.match(r"(?:(?:readonly|static)\s+)*(constructor|\w+)\b", sig)
                if nm: out.append({"owner_path": r["owner_path"], "name": nm.group(1), "kind": "member", "signature": sig, "text": comment.strip()})
            if not child: break
            direct = child["end"] + 1
    return out, None


@lru_cache(maxsize=16)
def cached_ts_fragments(path_text: str, size: int, mtime_ns: int):
    """Parse each immutable generated header once for repeated source packets."""
    return ts_fragments(Path(path_text))


def artifacts(root: Path, target: str, source: Path) -> dict:
    base = root / "build/docs/targets" / target
    contract = source_contract(source)
    fqcn = contract["qualified_type"]
    md_base = base / "vitepress/content/2.0.0" / target
    package = fqcn.rpartition(".")[0]
    pyroot = base / "python/JsMacrosAC"
    webroot = base / "web/2.0.0"
    tsfiles = list((base / "typescript/headers").glob("*.d.ts"))
    try:
        ts_path = tsfiles[0] if len(tsfiles) == 1 else None
        if ts_path:
            st = ts_path.stat(); all_ts, ts_error = cached_ts_fragments(str(ts_path), st.st_size, st.st_mtime_ns)
        else: all_ts, ts_error = [], ("missing_header" if not tsfiles else "multiple_headers")
    except (OSError, UnicodeError) as exc:
        all_ts, ts_error = [], type(exc).__name__
    result = {name: {"state": "missing", "files": [], "failed_files": []} for name in FORMATS}
    for typ in contract["owned_types"]:
        full = typ["qualified_type"]
        chain = full[len(package) + 1:].split(".") if package else full.split(".")
        simple = chain[-1]
        pyname = "_".join(chain) + ".py"
        html_rel = Path(*package.split(".")) / (".".join(chain) + ".html")
        md_candidates = [p for p in md_base.rglob("*.md")
                         if tuple(p.with_suffix("").parts[-(len(package.split(".")) + 1):]) ==
                         tuple(package.split(".")) + (".".join(chain),)]
        paths = {"Python": [pyroot / pyname], "HTML": [webroot / html_rel], "VitePress": md_candidates}
        ts_owner = "Packages." + package + "." + "$".join(chain) if package else "Packages." + "$".join(chain)
        for fmt, files in paths.items():
            rec = result[fmt]
            for f in files:
                if not f.is_file(): continue
                try:
                    raw = f.read_text(encoding="utf-8")
                    if fmt == "Python":
                        frags, error = py_fragments(f)
                        if not error and not any(x.get("kind") == "class" and x["name"] == "_".join(chain) for x in frags): continue
                    else:
                        frags, error = web_fragments(f, fmt, full)
                    if error:
                        rec["failed_files"].append({"path": str(f.relative_to(root)), "owner": full, "parse_error": error}); continue
                    if fmt == "Python":
                        owned = [x for x in frags if x.get("owner") == "_".join(chain)]
                        mapped = []
                        for fragment in owned:
                            source_kind = "member" if fragment.get("kind") == "method" else fragment.get("kind")
                            candidates = [entry for entry in contract["entries"]
                                          if entry.get("owner_qualified") == full
                                          and entry.get("kind") == source_kind
                                          and (entry.get("name") == fragment.get("name")
                                               or (entry.get("name") in PYTHON_KEYWORDS
                                                   and fragment.get("name") == entry.get("name") + "_"))]
                            mapped.append(dict(fragment, owner=full,
                                               source_member_ids=[entry["key"] for entry in candidates],
                                               source_member_names=[entry["name"] for entry in candidates]))
                        frags = mapped
                    h = digest(raw); owned_hash = digest(json.dumps({"source_owner": full, "fragments": frags}, sort_keys=True, ensure_ascii=False))
                    rec["files"].append({"path": str(f.relative_to(root)), "owner": full, "sha256": h,
                                         "owned_sha256": owned_hash, "fragments": frags})
                except (OSError, UnicodeError) as exc:
                    rec["failed_files"].append({"path": str(f.relative_to(root)), "owner": full, "parse_error": type(exc).__name__})
        rec = result["TypeScript"]
        if ts_error:
            rec["failed_files"].append({"path": str(ts_path) if ts_path else "typescript/headers", "owner": full, "parse_error": ts_error}); continue
        matches = [x for x in all_ts if x.get("kind") == "declaration" and x.get("declaration_kind") in ("class", "interface") and x.get("owner_path") == ts_owner]
        kinds = [x["declaration_kind"] for x in matches]
        if not matches or len(kinds) != len(set(kinds)):
            rec["failed_files"].append({"path": str(ts_path.relative_to(root)), "owner": full, "parse_error": f"ambiguous_or_unresolved_owner:{ts_owner}:{kinds}"}); continue
        members = [x for x in all_ts if x.get("kind") == "member" and x.get("owner_path") == ts_owner]
        frags = [dict(x, source_owner=full) for x in matches + members]
        raw = ts_path.read_text(encoding="utf-8"); h = digest(raw)
        rec["files"].append({"path": str(ts_path.relative_to(root)), "owner": full, "sha256": h,
                             "owned_sha256": digest(json.dumps({"source_owner": full, "fragments": frags}, sort_keys=True, ensure_ascii=False)),
                             "fragments": frags, "declaration_merge": {"owner_path": ts_owner, "kinds": kinds, "merged": len(matches) > 1}})
    for name, rec in result.items():
        rec["state"] = "present" if rec["files"] else ("parse_error" if rec["failed_files"] else "missing")
    return result


def build_packets(root: Path, sources: list[str], targets: list[str], output: Path) -> dict:
    root = root.resolve(); packets, groups, missing, errors = [], defaultdict(list), 0, 0
    for src in sorted(set(sources)):
        source = (root / src).resolve()
        if not source.is_relative_to(root) or not source.is_file():
            errors += 1; packets.append({"source": src, "state": "source_missing"}); continue
        contract = source_contract(source)
        for target in sorted(set(targets)):
            rendered = artifacts(root, target, source)
            absent = [k for k, v in rendered.items() if v["state"] == "missing"]
            missing += len(absent)
            for fmt, data in rendered.items():
                for f in data["files"]:
                    if f.get("parse_error"): errors += 1
                    else: groups[(fmt, f.get("owned_sha256", f["sha256"]))].append({"target": target, "source": src, "path": f["path"], "file_sha256": f["sha256"]})
            packet = {"target": target, "source": src, "qualified_type": contract["qualified_type"],
                      "source_sha256": contract["source_sha256"], "source_contract": contract["entries"],
                      "artifacts": rendered, "state": "needs_review", "missing_formats": absent,
                      "note": "Class-level packet: HTML and VitePress retain full page content, not member-scoped fragments. Extraction/presence only; no semantic or correctness verdict. HTML raw source is retained because text extraction loses markup semantics."}
            packets.append(packet)
    output.mkdir(parents=True, exist_ok=True)
    for i, packet in enumerate(packets):
        (output / f"packet-{i:05d}.json").write_text(json.dumps(packet, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    manifest = {"schema": 1, "packets": len(packets), "missing_format_count": missing,
                "parse_or_source_error_count": errors, "all_packets_require_review": True,
                "provenance": {"artifact_generation": "unverified", "target_build_stamp": "unavailable",
                               "source_hash_snapshot": "unavailable", "doclet_hash_snapshot": "unavailable",
                               "staleness": "cannot be determined; caller must establish fresh artifacts"},
                "artifact_groups": [{"format": k[0], "owned_content_sha256": k[1], "occurrences": v} for k, v in sorted(groups.items())],
                "packet_files": [f"packet-{i:05d}.json" for i in range(len(packets))]}
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return manifest


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--root", type=Path, default=Path.cwd()); ap.add_argument("--matrix", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True); ap.add_argument("--target", action="append")
    ap.add_argument("--source", action="append")
    args = ap.parse_args(argv)
    rows = [line.split("\t") for line in args.matrix.read_text(encoding="utf-8").splitlines()[1:] if line.strip()]
    targets = args.target or sorted({r[0] for r in rows if len(r) >= 2})
    sources = args.source or sorted({r[1] for r in rows if len(r) >= 2})
    manifest = build_packets(args.root, sources, targets, args.out)
    print(json.dumps({k: manifest[k] for k in ("packets", "missing_format_count", "parse_or_source_error_count")}, sort_keys=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
