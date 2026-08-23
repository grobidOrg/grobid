#!/usr/bin/env python3
"""
Add missing <lb/> from reference (B) TEI XML files into target (A) TEI XML
files, without moving or removing existing <lb/> in A.

A = manually annotated TEI files (rich headers, segmentation tags)
B = machine-generated TEI files with correct <lb/> positions from PDF layout

The script aligns lb-delimited text lines between A and B.  Where B splits
one of A's lines with an extra <lb/>, the split is inserted into A.
Existing <lb/> in A are left untouched.

Usage:
  python sync_lb_positions.py                     # default directories
  python sync_lb_positions.py --dir-a /path/to/A --dir-b /path/to/B
  python sync_lb_positions.py --dry-run            # preview without writing
"""

import argparse
import difflib
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

_SCRIPT_DIR = Path(__file__).resolve().parent
_DEFAULT_DIR_A = (
    _SCRIPT_DIR.parent
    / "resources"
    / "dataset"
    / "segmentation"
    / "article"
    / "dh-law-footnotes"
)
_DEFAULT_DIR_B = Path("/Users/lfoppiano/Downloads/grobid-law/segmentation-generated")

NS = {"tei": "http://www.tei-c.org/ns/1.0"}

_ENTITY_RE = re.compile(r"&(amp|lt|gt|apos|quot|#\d+|#x[0-9a-fA-F]+);")
_LB_RE = re.compile(r"<lb\s*/?>")
_TEXT_OPEN_RE = re.compile(r"<text\b[^>]*>", re.DOTALL)
_TEXT_CLOSE_RE = re.compile(r"</text\s*>")

_ENTITIES = {
    "amp": "&", "lt": "<", "gt": ">",
    "apos": "'", "quot": '"',
}


def _decode_entity(match):
    name = match.group(1)
    if name in _ENTITIES:
        return _ENTITIES[name]
    if name.startswith("#x"):
        return chr(int(name[2:], 16))
    if name.startswith("#"):
        return chr(int(name[1:]))
    return match.group(0)


def _decode_entities(text):
    return _ENTITY_RE.sub(_decode_entity, text)


def _normalize(text):
    """Collapse whitespace and strip for comparison."""
    return re.sub(r"\s+", " ", text).strip()


# ---------------------------------------------------------------
# Extract lb-delimited lines from XML tree
# ---------------------------------------------------------------

def _find_text_element(tree):
    root = tree.getroot()
    for finder in [
        lambda: root.find(".//tei:text", NS),
        lambda: root.find(".//text"),
    ]:
        elem = finder()
        if elem is not None:
            return elem
    for elem in root.iter():
        tag = elem.tag.split("}")[-1] if "}" in elem.tag else elem.tag
        if tag == "text":
            return elem
    return None


def extract_lb_lines(filepath):
    """
    Return list of normalized plain-text lines delimited by <lb/>.
    Parses with ET so entities are decoded.
    """
    tree = ET.parse(filepath)
    text_elem = _find_text_element(tree)
    if text_elem is None:
        return None

    lines = []
    current = []

    def walk(elem):
        tag = elem.tag.split("}")[-1] if "}" in elem.tag else elem.tag
        if tag == "lb":
            lines.append(_normalize("".join(current)))
            current.clear()
        if elem.text:
            current.append(elem.text)
        for child in elem:
            walk(child)
            if child.tail:
                current.append(child.tail)

    walk(text_elem)
    rest = _normalize("".join(current))
    if rest:
        lines.append(rest)
    return lines


# ---------------------------------------------------------------
# Extract lb-delimited lines from A's raw text (with byte offsets)
# ---------------------------------------------------------------

def _extract_text_section(raw):
    """Return (start, end) byte offsets of content inside <text>...</text>."""
    m_open = _TEXT_OPEN_RE.search(raw)
    if not m_open:
        return None
    m_close = _TEXT_CLOSE_RE.search(raw, m_open.end())
    if not m_close:
        return None
    return m_open.end(), m_close.start()


def _get_plain_text_from_raw_span(raw, start, end):
    """
    Extract plain text from a raw XML span, stripping tags, decoding entities,
    normalizing whitespace.
    """
    span = raw[start:end]
    # Strip comments
    span = re.sub(r"<!--.*?-->", "", span, flags=re.DOTALL)
    # Strip tags
    span = re.sub(r"<[^>]+>", "", span)
    # Decode entities
    span = _decode_entities(span)
    return _normalize(span)


def extract_a_lines_with_offsets(raw):
    """
    From A's raw file, extract lb-delimited text lines and track the byte
    offset of each <lb/> in the raw string.
    Returns (lines, lb_offsets, text_start, text_end).
    - lines: list of normalized plain-text strings between <lb/> markers
    - lb_offsets: list of (match_start, match_end) for each <lb/> in the raw
    - text_start, text_end: byte range of <text> content
    """
    bounds = _extract_text_section(raw)
    if not bounds:
        return None
    text_start, text_end = bounds

    # Find all <lb/> positions within the text section
    lb_offsets = []
    for m in _LB_RE.finditer(raw, text_start, text_end):
        lb_offsets.append((m.start(), m.end()))

    # Build lines: text between consecutive lb markers
    lines = []
    prev = text_start
    for lb_start, lb_end in lb_offsets:
        line_text = _get_plain_text_from_raw_span(raw, prev, lb_start)
        lines.append(line_text)
        prev = lb_end
    # Last segment (after last lb or all text if no lb)
    last = _get_plain_text_from_raw_span(raw, prev, text_end)
    if last:
        lines.append(last)

    return lines, lb_offsets, text_start, text_end


# ---------------------------------------------------------------
# Find split point in raw text
# ---------------------------------------------------------------

def _skip_tag(raw, pos, end):
    """If pos points at '<', skip past the tag. Return new pos."""
    if pos < end and raw[pos] == "<":
        if raw[pos:pos+4] == "<!--":
            close = raw.find("-->", pos + 4, end)
            return (close + 3) if close >= 0 else end
        close = raw.find(">", pos, end)
        return (close + 1) if close >= 0 else end
    return pos


def _next_text_char(raw, pos, end):
    """
    Get the next decoded text character from raw[pos:end], skipping tags.
    Returns (char, new_pos) or (None, pos) if no more text.
    """
    while pos < end:
        if raw[pos] == "<":
            pos = _skip_tag(raw, pos, end)
            continue
        if raw[pos] == "&":
            m = _ENTITY_RE.match(raw, pos)
            if m and m.end() <= end:
                return _decode_entity(m), m.end()
        return raw[pos], pos + 1
    return None, pos


def find_split_offset(raw, span_start, span_end, first_line_text):
    """
    Within raw[span_start:span_end], find the byte offset where `first_line_text`
    ends.  This is where <lb/> should be inserted to split A's line.

    Returns the byte offset in raw, or None if alignment fails.
    The offset is placed right before the next text character after the split
    (i.e., after any trailing whitespace following the matched text).
    """
    normalized = _normalize(first_line_text)
    if not normalized:
        return span_start  # Empty first line: lb at the very start

    pos = span_start
    b_pos = 0
    b_len = len(normalized)

    while b_pos < b_len and pos < span_end:
        # Skip tags
        if raw[pos] == "<":
            pos = _skip_tag(raw, pos, span_end)
            continue

        a_char, next_pos = _next_text_char(raw, pos, span_end)
        if a_char is None:
            break

        b_char = normalized[b_pos]

        # Flexible whitespace matching
        if b_char in " \t\n\r":
            while b_pos < b_len and normalized[b_pos] in " \t\n\r":
                b_pos += 1
            # Skip whitespace in A (but not tags)
            if a_char in " \t\n\r":
                pos = next_pos
                while pos < span_end and raw[pos] in " \t\n\r":
                    pos += 1
            continue

        if a_char in " \t\n\r":
            # A has whitespace that B doesn't (after normalization)
            pos = next_pos
            while pos < span_end and raw[pos] in " \t\n\r":
                pos += 1
            continue

        if a_char == b_char:
            pos = next_pos
            b_pos += 1
        else:
            # Mismatch, try to continue
            pos = next_pos
            b_pos += 1

    if b_pos < b_len:
        return None  # Failed to match

    # pos is now right after the last matched character.
    # Advance past trailing whitespace (but not tags) so <lb/> sits right
    # before the next word.
    while pos < span_end and raw[pos] in " \t\n\r":
        pos += 1

    return pos


# ---------------------------------------------------------------
# File matching
# ---------------------------------------------------------------

def find_file_pairs(dir_a, dir_b):
    """Find matching .tei.xml files between A (corpus+evaluation) and B."""
    a_files = {}
    for subdir in ("corpus/tei", "evaluation/tei"):
        d = dir_a / subdir
        if d.is_dir():
            for f in d.iterdir():
                if f.name.endswith(".tei.xml"):
                    a_files[f.name] = f

    b_dir = dir_b / "tei"
    if not b_dir.is_dir():
        b_dir = dir_b

    pairs = []
    for f in sorted(b_dir.iterdir()):
        if f.name.endswith(".tei.xml") and f.name in a_files:
            pairs.append((a_files[f.name], f))
    return pairs


# ---------------------------------------------------------------
# Main processing
# ---------------------------------------------------------------

def process_file(a_path, b_path, dry_run=False):
    """Process one file pair. Returns (status, message)."""
    with open(a_path, "r", encoding="utf-8") as f:
        a_raw = f.read()

    # Extract A's lines with byte offsets
    a_info = extract_a_lines_with_offsets(a_raw)
    if a_info is None:
        return "skip", "No <text> element in A"
    a_lines, a_lb_offsets, text_start, text_end = a_info

    # Extract B's lines
    b_lines = extract_lb_lines(b_path)
    if b_lines is None:
        return "skip", "No <text> element in B"

    # Align A and B lines
    sm = difflib.SequenceMatcher(None, a_lines, b_lines, autojunk=False)

    # Collect insertions: (byte_offset_in_raw, ) for each new <lb/> to add
    insertions = []  # list of byte offsets where <lb/> should be inserted
    added = 0

    def _get_a_span(a_idx):
        """Get the raw byte span for A-line at index a_idx."""
        s = a_lb_offsets[a_idx - 1][1] if a_idx > 0 else text_start
        e = a_lb_offsets[a_idx][0] if a_idx < len(a_lb_offsets) else text_end
        return s, e

    def _try_split_a_line(a_idx, b_sub_lines):
        """
        Try to split A-line a_idx into len(b_sub_lines) parts by inserting
        len(b_sub_lines)-1 new <lb/>.  Returns list of offsets or None.
        """
        span_start, span_end = _get_a_span(a_idx)
        offsets = []
        current = span_start
        for b_sub in b_sub_lines[:-1]:
            offset = find_split_offset(a_raw, current, span_end, b_sub)
            if offset is None:
                return None
            offsets.append(offset)
            current = offset
        return offsets

    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            continue

        a_count = i2 - i1
        b_count = j2 - j1

        if b_count <= a_count:
            # B has same or fewer lines — no missing <lb/>, skip
            continue

        if tag == "replace":
            # B has more lines than A within this block.
            # Do a sub-alignment to find which A-lines need splitting.
            a_sub = a_lines[i1:i2]
            b_sub = b_lines[j1:j2]
            sub_sm = difflib.SequenceMatcher(None, a_sub, b_sub, autojunk=False)

            for st, si1, si2, sj1, sj2 in sub_sm.get_opcodes():
                if st == "equal":
                    continue
                sa_count = si2 - si1
                sb_count = sj2 - sj1

                if sb_count <= sa_count:
                    continue

                if sa_count == 1:
                    # 1 A-line → multiple B-lines: split it
                    a_idx = i1 + si1
                    offsets = _try_split_a_line(a_idx, b_sub[sj1:sj2])
                    if offsets:
                        insertions.extend(offsets)
                        added += len(offsets)
                    else:
                        return "warn", (
                            f"Failed to split A line {a_idx} into "
                            f"{sb_count} B-lines"
                        )
                elif sa_count == 0 and sb_count > 0:
                    # Pure insert within the sub-block
                    # These B-lines split the A-line just before si1
                    a_idx = i1 + si1 - 1
                    if a_idx < 0:
                        a_idx = 0
                    offsets = _try_split_a_line(
                        a_idx,
                        [a_lines[a_idx]] + list(b_sub[sj1:sj2])
                    )
                    if offsets:
                        insertions.extend(offsets)
                        added += len(offsets)
                    else:
                        return "warn", (
                            f"Failed to insert {sb_count} B-lines "
                            f"near A line {a_idx}"
                        )
                else:
                    return "warn", (
                        f"Complex sub-mismatch: {sa_count} A-lines vs "
                        f"{sb_count} B-lines at A[{i1+si1}:{i1+si2}]"
                    )

        elif tag == "insert":
            # B has lines that A doesn't — find where to insert
            # These split the A-line just before i1
            a_idx = i1 - 1 if i1 > 0 else 0
            b_subs = list(b_lines[j1:j2])
            offsets = _try_split_a_line(
                a_idx,
                [a_lines[a_idx]] + b_subs
            )
            if offsets:
                insertions.extend(offsets)
                added += len(offsets)
            else:
                return "warn", (
                    f"Failed to insert {b_count} B-lines near A line {a_idx}"
                )

    if not insertions:
        return "ok", f"lb: {len(a_lb_offsets)} (no changes needed)"

    # Sort insertions and insert from end to start
    insertions.sort(reverse=True)
    result = a_raw
    for offset in insertions:
        result = result[:offset] + "<lb/>" + result[offset:]

    # Validate: plain text unchanged
    old_text = _get_plain_text_from_raw_span(a_raw, text_start, text_end)
    # Recompute text section bounds in the modified result
    new_bounds = _extract_text_section(result)
    if new_bounds:
        new_text = _get_plain_text_from_raw_span(result, new_bounds[0], new_bounds[1])
        if old_text != new_text:
            return "error", "Text content changed after insertion"

    if dry_run:
        return "ok", f"lb: {len(a_lb_offsets)} -> {len(a_lb_offsets) + added} (+{added})"

    with open(a_path, "w", encoding="utf-8") as f:
        f.write(result)

    return "ok", f"lb: {len(a_lb_offsets)} -> {len(a_lb_offsets) + added} (+{added})"


def main():
    parser = argparse.ArgumentParser(
        description="Add missing <lb/> from B into A TEI XML files"
    )
    parser.add_argument(
        "--dir-a", type=Path, default=_DEFAULT_DIR_A,
        help=f"Root of target A directory (default: {_DEFAULT_DIR_A})",
    )
    parser.add_argument(
        "--dir-b", type=Path, default=_DEFAULT_DIR_B,
        help=f"Root of reference B directory (default: {_DEFAULT_DIR_B})",
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="Preview changes without writing files",
    )
    args = parser.parse_args()

    pairs = find_file_pairs(args.dir_a, args.dir_b)
    if not pairs:
        print("No matching file pairs found.", file=sys.stderr)
        return 1

    print(f"Found {len(pairs)} file pairs")
    if args.dry_run:
        print("DRY RUN — no files will be modified\n")

    stats = {"ok": 0, "warn": 0, "error": 0, "skip": 0}

    for a_path, b_path in pairs:
        status, msg = process_file(a_path, b_path, dry_run=args.dry_run)
        stats[status] += 1
        marker = {"ok": "OK", "warn": "WARN", "error": "ERR", "skip": "SKIP"}[status]
        print(f"  [{marker}] {a_path.name}: {msg}")

    print(f"\nDone: {stats['ok']} ok, {stats['warn']} warnings, "
          f"{stats['error']} errors, {stats['skip']} skipped")
    return 1 if stats["error"] > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
