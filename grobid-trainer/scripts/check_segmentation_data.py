#!/usr/bin/env python3
"""
Validate and analyze GROBID segmentation training TEI files.

Combines two analysis modes:
  validate  - Check XML well-formedness, valid labels, untagged text,
              empty labels, raw file consistency, footnote classification
  analyze   - Detect headnote/footnote content inconsistencies

By default runs both modes on the full segmentation training data,
recursively discovering all tei/raw directory pairs.

Usage:
  python check_segmentation_data.py                  # both modes, full segmentation dir
  python check_segmentation_data.py validate         # validation only
  python check_segmentation_data.py analyze          # analysis only
  python check_segmentation_data.py --root /path     # custom root directory
"""

import os
import sys
import re
import argparse
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path
import difflib

# ---------------------------------------------------------------------------
# Default root: full segmentation dataset
# ---------------------------------------------------------------------------
_SCRIPT_DIR = Path(__file__).resolve().parent
_DEFAULT_ROOT = (
    _SCRIPT_DIR.parent
    / "resources"
    / "dataset"
    / "segmentation"
)

NS = {"tei": "http://www.tei-c.org/ns/1.0"}

# ---------------------------------------------------------------------------
# Allowed tags / attributes for segmentation TEI
# ---------------------------------------------------------------------------
ALLOWED_TAGS = {
    "titlePage", "front", "body", "note", "listBibl", "page", "div", "lb"
}
ALLOWED_NOTE_PLACES = {"headnote", "footnote"}
ALLOWED_DIV_TYPES = {
    "toc", "conflict", "contribution", "acknowledgement",
    "availability", "funding", "annex"
}

# ---------------------------------------------------------------------------
# Bibliographic-reference patterns (for footnote classification check)
# ---------------------------------------------------------------------------
BIBLIO_PATTERNS = [
    re.compile(r'10\.\d{4,}/\S+', re.IGNORECASE),
    re.compile(r'ISBN[\s:-]*[\dX-]{10,}', re.IGNORECASE),
    re.compile(r'\bp+\.\s*\d+', re.IGNORECASE),
    re.compile(r'\bvol\.\s*\d+', re.IGNORECASE),
    re.compile(r'\b\d{1,4}\s*\(\d{1,4}\)\s*[,:]\s*\d+'),
    re.compile(r'\(\d{4}[a-z]?\)'),
    re.compile(r'\bet\s+al\.', re.IGNORECASE),
    re.compile(r'\bjournal\s+of\b', re.IGNORECASE),
    re.compile(r'\b\w+\s+review\b', re.IGNORECASE),
    re.compile(r'\beds?\.\s', re.IGNORECASE),
    re.compile(r'\b\w+\s+press\b', re.IGNORECASE),
    re.compile(r'university\s+press', re.IGNORECASE),
    re.compile(r'\blaw\s+(review|journal|quarterly)\b', re.IGNORECASE),
    re.compile(r'ISSN[\s:-]*\d{4}', re.IGNORECASE),
]
STRONG_BIBLIO_PATTERNS = [
    re.compile(r'10\.\d{4,}/\S+', re.IGNORECASE),
    re.compile(r'ISBN[\s:-]*[\dX-]{10,}', re.IGNORECASE),
    re.compile(r'ISSN[\s:-]*\d{4}', re.IGNORECASE),
]
BOILERPLATE_PATTERNS = [
    re.compile(r'^https?://doi\.org/\S+\s+Published\s+online\s+by\b', re.IGNORECASE),
    re.compile(r'^Downloaded\s+from\b', re.IGNORECASE),
    re.compile(r'^\s*\u00a9\s+\d{4}\b', re.IGNORECASE),
]

# ===================================================================
#  Shared utilities
# ===================================================================

def strip_ns(tag):
    """Remove XML namespace prefix from a tag name."""
    if "}" in tag:
        return tag.split("}", 1)[1]
    return tag


def get_all_text(element):
    """Recursively collect all text (including tails) from an element."""
    texts = []
    if element.text:
        texts.append(element.text)
    for child in element:
        texts.extend(get_all_text(child))
        if child.tail:
            texts.append(child.tail)
    return texts


def get_text_content(element):
    """Get concatenated text content from an element."""
    return "".join(get_all_text(element))


def normalize_whitespace(text):
    """Collapse whitespace runs and strip."""
    return re.sub(r'\s+', ' ', text).strip()


def find_text_element(tree):
    """Find the <text> element in a TEI tree."""
    root = tree.getroot()
    text_elem = root.find(".//tei:text", NS)
    if text_elem is not None:
        return text_elem
    text_elem = root.find(".//text")
    if text_elem is not None:
        return text_elem
    for elem in root.iter():
        if strip_ns(elem.tag) == "text":
            return elem
    return None


def discover_splits(root_dir):
    """
    Recursively find all tei/ directories under *root_dir* and return
    (label, tei_dir, raw_dir) tuples.  The label is the relative path
    from root to the parent of tei/, e.g. "article/dh-law-footnotes/corpus".
    """
    root = Path(root_dir)
    splits = []
    for tei_dir in sorted(root.rglob("tei")):
        if not tei_dir.is_dir():
            continue
        # Skip if no TEI XML files inside
        if not any(tei_dir.glob("*.tei.xml")) and not any(tei_dir.glob("*.xml")):
            continue
        raw_dir = tei_dir.parent / "raw"
        label = str(tei_dir.parent.relative_to(root))
        splits.append((label, tei_dir, raw_dir))
    return splits

# ===================================================================
#  Validation checks (from validate_segmentation.py)
# ===================================================================

def check_wellformedness(filepath):
    tree = None
    errors = []
    try:
        tree = ET.parse(filepath)
    except ET.ParseError as e:
        errors.append(f"XML parse error: {e}")
    return tree, errors


def check_valid_labels(text_element):
    errors = []

    def _walk(parent):
        for child in parent:
            local = strip_ns(child.tag)
            if local not in ALLOWED_TAGS:
                errors.append(f"Invalid tag <{local}> inside <text>")
                continue
            if local == "note":
                place = child.get("place", "")
                if place not in ALLOWED_NOTE_PLACES:
                    errors.append(
                        f'Invalid <note place="{place}"> - '
                        f'allowed: {ALLOWED_NOTE_PLACES}'
                    )
            if local == "div":
                div_type = child.get("type", "")
                if div_type not in ALLOWED_DIV_TYPES:
                    errors.append(
                        f'Invalid <div type="{div_type}"> - '
                        f'allowed: {ALLOWED_DIV_TYPES}'
                    )
            if local != "lb":
                _walk(child)

    _walk(text_element)
    return errors


def check_untagged_text(text_element):
    errors = []
    if text_element.text and text_element.text.strip():
        preview = text_element.text.strip()[:80]
        suffix = "..." if len(text_element.text.strip()) > 80 else ""
        errors.append(f'Untagged text directly in <text>: "{preview}{suffix}"')
    for child in text_element:
        local = strip_ns(child.tag)
        if child.tail and child.tail.strip():
            preview = child.tail.strip()[:80]
            suffix = "..." if len(child.tail.strip()) > 80 else ""
            errors.append(f'Untagged text after </{local}>: "{preview}{suffix}"')
    return errors


def check_empty_labels(text_element):
    errors = []

    def _walk(elem):
        local = strip_ns(elem.tag)
        if local in ALLOWED_TAGS and local != "lb":
            if not get_text_content(elem).strip():
                attrs = ""
                if local == "note":
                    attrs = f' place="{elem.get("place", "")}"'
                elif local == "div":
                    attrs = f' type="{elem.get("type", "")}"'
                errors.append(f"Empty <{local}{attrs}> element")
        for child in elem:
            _walk(child)

    _walk(text_element)
    return errors


def _extract_lines_from_tei(text_element):
    lines = []
    current_line = []

    def _walk(elem):
        local = strip_ns(elem.tag)
        if local == "lb":
            text = "".join(current_line).strip()
            if text:
                lines.append(text)
            current_line.clear()
        if elem.text:
            current_line.append(elem.text)
        for child in elem:
            _walk(child)
            if child.tail:
                current_line.append(child.tail)

    _walk(text_element)
    text = "".join(current_line).strip()
    if text:
        lines.append(text)
    return lines


def check_raw_consistency(text_element, raw_path):
    errors = []
    if not raw_path.exists():
        errors.append(f"No corresponding raw file: {raw_path.name}")
        return errors

    tei_first_words = []
    for line in _extract_lines_from_tei(text_element):
        words = line.split()
        if words:
            tei_first_words.append(words[0])

    raw_first_words = []
    with open(raw_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                parts = line.split()
                if parts:
                    raw_first_words.append(parts[0])

    if len(tei_first_words) != len(raw_first_words):
        errors.append(
            f"Line count mismatch: TEI {len(tei_first_words)} vs raw {len(raw_first_words)}"
        )

    mismatches = []
    for i in range(min(len(tei_first_words), len(raw_first_words))):
        if tei_first_words[i] != raw_first_words[i]:
            mismatches.append((i, tei_first_words[i], raw_first_words[i]))

    for idx, (ln, tw, rw) in enumerate(mismatches[:5]):
        errors.append(f'First-word mismatch line {ln+1}: TEI="{tw}" vs RAW="{rw}"')
    if len(mismatches) > 5:
        errors.append(f"... and {len(mismatches)-5} more first-word mismatches")
    return errors


def check_footnote_classification(text_element, tree):
    errors = []
    root = tree.getroot()
    article_doi = None
    for elem in root.iter():
        if strip_ns(elem.tag) == "idno" and elem.get("type") == "DOI":
            if elem.text:
                article_doi = elem.text.strip()
                break

    def _walk(elem):
        local = strip_ns(elem.tag)
        if local == "note" and elem.get("place") == "footnote":
            content = get_text_content(elem).strip()
            if content:
                if any(bp.search(content) for bp in BOILERPLATE_PATTERNS):
                    for child in elem:
                        _walk(child)
                    return

                matched_patterns = []
                strong_matches = []
                for pattern in BIBLIO_PATTERNS:
                    matches = pattern.findall(content)
                    if matches:
                        matched_patterns.append((pattern.pattern, matches[:3]))
                for pattern in STRONG_BIBLIO_PATTERNS:
                    matches = pattern.findall(content)
                    if matches:
                        if '10\\.' in pattern.pattern:
                            filtered = [
                                m for m in matches
                                if article_doi is None or article_doi not in m
                            ]
                            if filtered:
                                strong_matches.append((pattern.pattern, filtered[:3]))
                        else:
                            strong_matches.append((pattern.pattern, matches[:3]))

                if strong_matches or len(matched_patterns) >= 3:
                    preview = content[:150].replace('\n', ' ')
                    match_desc = ", ".join(
                        f'"{m[1][0]}"' for m in matched_patterns[:4]
                    )
                    suffix = "..." if len(content) > 150 else ""
                    errors.append(
                        f'Footnote may contain bibliographic reference '
                        f'(consider <listBibl>). Matches: {match_desc}. '
                        f'Text: "{preview}{suffix}"'
                    )
        for child in elem:
            _walk(child)

    _walk(text_element)
    return errors


# ===================================================================
#  Note consistency analysis (from analyze_notes / detailed_analysis)
# ===================================================================

def _extract_notes_with_context(file_path):
    """Extract headnote/footnote content with line numbers."""
    notes = []
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        for i, line in enumerate(lines):
            if '<note' not in line or 'place=' not in line:
                continue
            for place in ('headnote', 'footnote'):
                if f'place="{place}"' in line:
                    match = re.search(
                        rf'<note[^>]*place="{place}"[^>]*>(.*?)</note>',
                        line, re.DOTALL
                    )
                    if match:
                        content = re.sub(r'<[^>]+>', '', match.group(1))
                        content = re.sub(r'\s+', ' ', content).strip()
                        if content:
                            notes.append({
                                'type': place,
                                'content': content,
                                'line': i + 1,
                            })
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
    return notes


def _classify_content(content):
    """Categorize note content by type."""
    cl = content.lower()
    if 'doi' in cl or 'http' in cl:
        return 'identifier'
    if any(w in cl for w in ('vol', 'journal', 'pp', 'page', 'issue')):
        return 'citation'
    if re.search(r'\b(19|20)\d{2}\b', cl):
        return 'date/year'
    if '@' in cl:
        return 'email'
    if re.search(r'[A-Z][a-z]+ [A-Z][a-z]+', content):
        return 'author'
    return 'other'


def analyze_note_consistency(tei_dir):
    """
    Find files where the same content appears as both headnote and footnote.
    Returns (results_list, total_files_checked).
    """
    tei_files = sorted(
        f for f in os.listdir(tei_dir) if f.endswith('.tei.xml')
    )
    results = []

    for filename in tei_files:
        notes = _extract_notes_with_context(tei_dir / filename)
        if not notes:
            continue
        headnotes = [n for n in notes if n['type'] == 'headnote']
        footnotes = [n for n in notes if n['type'] == 'footnote']
        if not headnotes or not footnotes:
            continue

        matches = []
        for hn in headnotes:
            hn_norm = normalize_whitespace(hn['content']).lower()
            if len(hn_norm) < 10:
                continue
            for fn in footnotes:
                fn_norm = normalize_whitespace(fn['content']).lower()
                if len(fn_norm) < 10:
                    continue
                if hn_norm == fn_norm:
                    matches.append({
                        'headnote': hn, 'footnote': fn,
                        'match_type': 'exact', 'similarity': 1.0
                    })
                else:
                    sim = difflib.SequenceMatcher(None, hn_norm, fn_norm).ratio()
                    if sim >= 0.85:
                        matches.append({
                            'headnote': hn, 'footnote': fn,
                            'match_type': 'similar', 'similarity': sim
                        })

        if matches:
            content_types = defaultdict(list)
            for m in matches:
                content_types[_classify_content(m['headnote']['content'])].append(m)
            results.append({
                'file': filename,
                'matches': matches,
                'content_types': dict(content_types),
                'headnote_count': len(headnotes),
                'footnote_count': len(footnotes),
            })

    return results, len(tei_files)


# ===================================================================
#  Runners
# ===================================================================

CHECK_NAMES = {
    1: "XML Well-formedness",
    2: "Valid Segmentation Labels",
    3: "Text Outside Labels (Untagged Content)",
    4: "Empty Labels",
    5: "Raw File Text Consistency",
    6: "Footnote Classification (Possible Bibliographic References)",
}


def run_validate(splits):
    """Run all six validation checks on every split."""
    grand_total_issues = 0

    for split_name, tei_dir, raw_dir in splits:
        tei_files = sorted(
            f for f in os.listdir(tei_dir)
            if f.endswith(".tei.xml")
        )

        print(f"\n{'=' * 100}")
        print(f"VALIDATION — {split_name} ({len(tei_files)} files)")
        print(f"  TEI: {tei_dir}")
        print(f"  RAW: {raw_dir}")
        print(f"{'=' * 100}")

        all_issues = {i: defaultdict(list) for i in range(1, 7)}
        files_with_issues = set()

        for filename in tei_files:
            filepath = tei_dir / filename

            tree, errors = check_wellformedness(filepath)
            if errors:
                all_issues[1][filename].extend(errors)
                files_with_issues.add(filename)
                continue

            text_elem = find_text_element(tree)
            if text_elem is None:
                all_issues[2][filename].append("No <text> element found")
                files_with_issues.add(filename)
                continue

            for check_num, check_fn in [
                (2, lambda te: check_valid_labels(te)),
                (3, lambda te: check_untagged_text(te)),
                (4, lambda te: check_empty_labels(te)),
            ]:
                errs = check_fn(text_elem)
                if errs:
                    all_issues[check_num][filename].extend(errs)
                    files_with_issues.add(filename)

            raw_basename = filename.replace(".tei.xml", "")
            raw_path = raw_dir / raw_basename
            errs = check_raw_consistency(text_elem, raw_path)
            if errs:
                all_issues[5][filename].extend(errs)
                files_with_issues.add(filename)

            errs = check_footnote_classification(text_elem, tree)
            if errs:
                all_issues[6][filename].extend(errs)
                files_with_issues.add(filename)

        # Print per-check results
        total_issues = 0
        for ck in sorted(all_issues):
            issues = all_issues[ck]
            count = sum(len(v) for v in issues.values())
            total_issues += count
            print(f"\n  CHECK {ck}: {CHECK_NAMES[ck]}")
            if not issues:
                print(f"    PASSED")
            else:
                print(f"    {count} issue(s) in {len(issues)} file(s)")
                for fn in sorted(issues):
                    print(f"      {fn}:")
                    for msg in issues[fn]:
                        print(f"        - {msg}")

        print(f"\n  Summary: {len(tei_files)} files, "
              f"{len(files_with_issues)} with issues, "
              f"{total_issues} total issues")
        grand_total_issues += total_issues

    return grand_total_issues


def run_analyze(splits):
    """Run note consistency analysis on every split."""
    grand_total = 0

    for split_name, tei_dir, _raw_dir in splits:
        results, file_count = analyze_note_consistency(tei_dir)

        print(f"\n{'=' * 100}")
        print(f"NOTE CONSISTENCY ANALYSIS — {split_name} ({file_count} files)")
        print(f"  TEI: {tei_dir}")
        print(f"{'=' * 100}")

        if not results:
            print("  No inconsistencies found.")
            continue

        # Summary by content type
        all_types = defaultdict(int)
        for r in results:
            for ct, ms in r['content_types'].items():
                all_types[ct] += len(ms)

        print(f"\n  {len(results)} file(s) with inconsistencies")
        print("  By content type:")
        for ct, cnt in sorted(all_types.items(), key=lambda x: x[1], reverse=True):
            print(f"    {ct}: {cnt}")

        # Per-file detail
        for r in results:
            print(f"\n  --- {r['file']} ---")
            print(f"    headnotes: {r['headnote_count']}, "
                  f"footnotes: {r['footnote_count']}, "
                  f"matches: {len(r['matches'])}")
            for i, m in enumerate(r['matches'][:3]):
                hn = m['headnote']
                fn = m['footnote']
                print(f"    Match {i+1} ({m['match_type']}, "
                      f"sim={m['similarity']:.2f}):")
                print(f"      headnote L{hn['line']}: "
                      f"{hn['content'][:80]}...")
                print(f"      footnote L{fn['line']}: "
                      f"{fn['content'][:80]}...")
            if len(r['matches']) > 3:
                print(f"    ... and {len(r['matches'])-3} more matches")

        grand_total += sum(len(r['matches']) for r in results)

    return grand_total


# ===================================================================
#  CLI
# ===================================================================

def main():
    parser = argparse.ArgumentParser(
        description="Validate and analyze GROBID segmentation training data",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "mode",
        nargs="?",
        choices=["validate", "analyze"],
        default=None,
        help="Run only validation or analysis (default: both)",
    )
    parser.add_argument(
        "--root", "-r",
        type=Path,
        default=_DEFAULT_ROOT,
        help=f"Root directory of the flavour dataset (default: {_DEFAULT_ROOT})",
    )
    args = parser.parse_args()

    root = args.root.resolve()
    if not root.is_dir():
        print(f"Error: directory not found: {root}", file=sys.stderr)
        return 1

    splits = discover_splits(root)
    if not splits:
        print(f"Error: no corpus/ or evaluation/ with tei/ found under {root}",
              file=sys.stderr)
        return 1

    print(f"Root: {root}")
    print(f"Splits: {', '.join(s[0] for s in splits)}")

    issues = 0
    if args.mode in (None, "validate"):
        issues += run_validate(splits)
    if args.mode in (None, "analyze"):
        issues += run_analyze(splits)

    return 1 if issues > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
