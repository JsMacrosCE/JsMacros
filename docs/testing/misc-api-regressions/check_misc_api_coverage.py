#!/usr/bin/env python3
"""Audit changed-file inventory, NOT runtime or branch coverage."""

import argparse
import re
import subprocess
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="61f12e64798806fdf50a9c6f4e3284cca6d179fe")
    parser.add_argument("--head", default="HEAD")
    parser.add_argument("--committed-only", action="store_true", help="Exclude uncommitted production-source changes")
    args = parser.parse_args()
    directory = Path(__file__).resolve().parent
    root = directory.parents[2]
    document = (directory / "misc_api_regressions.md").read_text()
    prefix = "common/src/main/java/com/jsmacrosce/jsmacros/"
    rows = re.findall(r"^\| `([^`]+)` \| ([^\n]+) \| ([AJODM+]+) \|$", document, re.MULTILINE)
    mapped = {}
    for source, coverage, mode in rows:
        path = source if source.startswith(("fabric/", "extension/")) or source == "KNOWN_ISSUES.md" else prefix + source
        if path in mapped:
            raise SystemExit(f"Duplicate coverage row: {path}")
        if not (root / path).is_file():
            raise SystemExit(f"Nonexistent coverage source: {path}")
        for case in re.findall(r"\b([GIEWLND]\d+)\b", coverage):
            if not re.search(rf"^## {case} —", document, re.MULTILINE):
                raise SystemExit(f"Missing manual scenario {case} for {path}")
        mapped[path] = mode
    scopes = ["common/src", "fabric/src", "neoforge/src", "extension/graal/src/main", "KNOWN_ISSUES.md"]
    changed = set(subprocess.check_output(
        ["git", "diff", "--name-only", args.base, args.head, "--", *scopes],
        cwd=root, text=True,
    ).splitlines())
    if not args.committed_only:
        changed.update(subprocess.check_output(
            ["git", "diff", "--name-only", "HEAD", "--", *scopes], cwd=root, text=True,
        ).splitlines())
        changed.update(subprocess.check_output(
            ["git", "ls-files", "--others", "--exclude-standard", "--", *scopes], cwd=root, text=True,
        ).splitlines())
    if not changed:
        raise SystemExit("No changed sources found; cannot establish coverage inventory")
    missing = sorted(changed - mapped.keys())
    if missing:
        raise SystemExit("Unmapped changes:\n" + "\n".join(missing))
    print(f"Inventory PASS: {len(changed)} changed files mapped; runtime results remain separate.")


if __name__ == "__main__":
    main()
