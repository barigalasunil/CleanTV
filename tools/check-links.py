#!/usr/bin/env python3
"""Checks the local files and images the docs point at, without touching the network.

Scans the Markdown the repository ships (the root docs, themes/ and examples/), takes the target of
every inline link, image, and HTML href/src, and checks that a file or folder meant to exist in this
repository really does. Anything that needs the network (http, https, mailto), an anchor alone, or
an absolute site path is skipped on purpose: this is a packaging check, not a link crawler.

Run:  tools/venv/Scripts/python.exe tools/check-links.py
Exit: 0 when every local target resolves, 1 with one line per broken target.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parent.parent
PATTERNS = ("*.md", "themes/*.md", "examples/*.md")

MARKDOWN = re.compile(r"!?\[[^\]]*\]\(\s*<?([^)\s>]+)>?(?:\s+(?:\"[^\"]*\"|'[^']*'))?\s*\)")
HTML = re.compile(r"""(?:href|src)\s*=\s*["']([^"']+)["']""")

SKIP_PREFIXES = ("http://", "https://", "mailto:", "tel:", "data:", "#", "//")


def documents() -> list[Path]:
    """The Markdown files the checks run over, without duplicates."""
    found: set[Path] = set()
    for pattern in PATTERNS:
        found.update(path for path in ROOT.glob(pattern) if path.is_file())
    return sorted(found)


def targets(text: str):
    """Every link and image target in one document, Markdown and HTML alike."""
    for match in MARKDOWN.finditer(text):
        yield match.group(1)
    for match in HTML.finditer(text):
        yield match.group(1)


def clean(target: str) -> str:
    """The path part of a target: no fragment, no query, no percent escapes."""
    path = target.split("#", 1)[0].split("?", 1)[0].strip()
    return unquote(path)


def main() -> int:
    docs = documents()
    checked = 0
    broken: list[tuple[str, str]] = []

    for doc in docs:
        text = doc.read_text(encoding="utf-8", errors="replace")
        for raw in targets(text):
            target = raw.strip()
            if not target or target.lower().startswith(SKIP_PREFIXES) or target.startswith("/"):
                continue
            path = clean(target)
            if not path:
                continue
            checked += 1
            if not (doc.parent / path).resolve().exists():
                broken.append((doc.relative_to(ROOT).as_posix(), target))

    for doc, target in broken:
        print(f"broken: {doc} -> {target}")
    print(f"{len(docs)} docs, {checked} local links, {len(broken)} broken")
    return 1 if broken else 0


if __name__ == "__main__":
    sys.exit(main())
