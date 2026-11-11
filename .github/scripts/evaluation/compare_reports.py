#!/usr/bin/env python3
"""
Render the Markdown reports of an end-to-end evaluation, optionally against a baseline.

    compare_reports.py --reports reports/ [--baseline baseline/] > summary.md

Both directories hold the `report-<dataset>.md` files written by `jatsEval`, or by
score_comet.py for the COMET corpus, in the same layout. Each dataset gets a summary table,
the micro-average F1 of the header, citation and fulltext (the author, affiliation and combined
F1 for COMET) per matching, followed by its full report, collapsed, rendered as in the benchmark pages
(doc/benchmarks/): per section and matching, the field-level precision, recall, F1 and
support, then the instance-level results and the citation contexts. When a baseline is
given, every score that moved is followed by its delta in brackets. A dataset missing
from the baseline is rendered without deltas.
"""

import argparse
import glob
import os
import re
import sys

# qualifiers of the instance-level lines, as spelled in the report
INSTANCE_MATCHINGS = {
    "strict": "Strict",
    "soft": "Soft",
    "Levenshtein": "Levenshtein",
    "RatcliffObershelp": "Ratcliff/Obershelp",
    "ObservedRatcliffObershelp": "Ratcliff/Obershelp",
}

INSTANCE_LINE = re.compile(r"^Instance-level (recall|precision|f-score):\s*([\d.]+)\s*\((\w+)\)")
CONTEXT_LINE = re.compile(r"^(Precision|Recall|fscore) citation contexts:\s*([\d.]+)")
DOCUMENTS_LINE = re.compile(r"^Evaluation on (\d+) random PDF files")
MATCHINGS = ["Strict", "Soft", "Levenshtein", "Ratcliff/Obershelp", "Fuzzy"]
MICRO = "all fields (micro avg.)"
COMET = "Authors and affiliations (COMET)"
# rows of the summary table: (name, section, label of the row of the field-level results)
SUMMARY_ROWS = [
    ("Header", "Header metadata", MICRO),
    ("Citation", "Citation metadata", MICRO),
    ("Fulltext", "Fulltext structures", MICRO),
    ("Authors", COMET, "author"),
    ("Affiliations", COMET, "affiliations"),
    ("Authors with all their affiliations", COMET, "combined"),
]
TABLE_HEADER = "| label | precision | recall | f1 | support |\n|---|---|---|---|---|"


def to_number(cell):
    try:
        return float(cell)
    except ValueError:
        return None


def table_row(line):
    """Return (label, bold, [precision, recall, f1, support]) of a scores row, None for any other row."""
    cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
    if len(cells) != 5 or not cells[0]:
        return None
    values = [to_number(cell.strip("*")) for cell in cells[1:]]
    if None in values:
        return None
    return cells[0].strip("*"), cells[0].startswith("**"), values


def scan(path):
    """
    Yield (line, key) for each line of a report, key identifying the score the line holds:
      ("row", section, matching, table, label) for a row of a scores table,
      ("instance", section, measure, matching) for an instance-level score,
      ("context", measure) for a citation context score,
      None otherwise.
    """
    section = matching = table = None
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip()
            stripped = line.strip()
            key = None
            if stripped.startswith("## "):
                section, matching, table = stripped[3:].strip(), None, None
            elif stripped.startswith("#### "):
                title = stripped[5:].strip()
                matching = title.split(" Matching")[0] if " Matching" in title else None
                table = None
            elif stripped.startswith("**") and stripped.endswith("**") and "|" not in stripped:
                table = stripped.strip("*")
            elif stripped.startswith("|") and section and matching and table and (row := table_row(stripped)):
                key = ("row", section, matching, table, row[0])
            elif section and (m := INSTANCE_LINE.match(stripped)):
                key = ("instance", section, m.group(1), INSTANCE_MATCHINGS.get(m.group(3), m.group(3)))
            elif m := CONTEXT_LINE.match(stripped):
                key = ("context", m.group(1))
            yield line, key


def parse_report(path):
    """Return the scores of a report as {key: value}, the value of a table row being its [precision, recall, f1]."""
    scores = {"documents": None}
    for line, key in scan(path):
        stripped = line.strip()
        if key is None:
            if scores["documents"] is None and (m := DOCUMENTS_LINE.match(stripped)):
                scores["documents"] = int(m.group(1))
        elif key[0] == "row":
            scores[key] = table_row(stripped)[2][:3]
        elif key[0] == "instance":
            scores[key] = float(INSTANCE_LINE.match(stripped).group(2))
        else:
            scores[key] = float(CONTEXT_LINE.match(stripped).group(2))
    return scores


def delta(value, base):
    if base is None:
        return ""
    diff = round(value - base, 2)
    return "" if diff == 0 else f" ({diff:+.2f})"


def number(value):
    return f"{value:g}"


def render_summary(report, base, out):
    """Write the F1 of the summary rows of the report per matching, with its delta."""
    rows = [
        (name, {m: ("row", section, m, "Field-level results", label) for m in MATCHINGS})
        for name, section, label in SUMMARY_ROWS
    ]
    rows = [(name, keys) for name, keys in rows if any(key in report for key in keys.values())]
    # the matchings of the report only: the JATS reports have no fuzzy matching, COMET has only two
    matchings = [m for m in MATCHINGS if any(keys[m] in report for _, keys in rows)]
    if not rows:
        return
    out.write("| | " + " | ".join(matchings) + " |\n")
    out.write("|---|" + "---|" * len(matchings) + "\n")
    for name, keys in rows:
        cells = []
        for matching in matchings:
            key = keys[matching]
            if key in report:
                f1 = report[key][2]
                cells.append(number(f1) + delta(f1, base[key][2] if key in base else None))
            else:
                cells.append("—")
        out.write(f"| {name} | " + " | ".join(cells) + " |\n")
    out.write("\n")


def render_dataset(name, path, report, baseline, out):
    out.write(f"### {name}\n\n")
    if baseline is None:
        out.write("No baseline report for this dataset: scores are shown without deltas.\n\n")
    elif baseline["documents"] and report["documents"] and baseline["documents"] != report["documents"]:
        # a delta over different document sets also reflects which documents were covered
        out.write(
            f"**The baseline was evaluated on {baseline['documents']} PDF, this run on {report['documents']}**: "
            "the deltas are over different document sets.\n\n"
        )
    base = baseline or {}
    render_summary(report, base, out)

    out.write("<details>\n<summary>Full report</summary>\n\n")
    blank = True
    for line, key in scan(path):
        stripped = line.strip()
        if key is None:
            if stripped.startswith("## "):
                line = "#### " + stripped[3:].strip()
            elif stripped.startswith("#### "):
                line = "##### " + stripped[5:].strip()
            elif stripped.startswith("| label"):
                line = TABLE_HEADER
            elif stripped.startswith("|---"):
                continue
            elif stripped.startswith("|") and not stripped.strip("| "):
                line = "| | | | | |"
            elif stripped.startswith("Evaluation metrics produced in"):
                continue
            # the reports separate their blocks with several blank lines
            if not stripped:
                if blank:
                    continue
                blank = True
            else:
                blank = False
            out.write(line + "\n")
            continue

        blank = False
        if key[0] == "row":
            label, bold, values = table_row(stripped)
            mark = "**" if bold else ""
            base_values = base.get(key, [None] * 3)
            cells = [f"{mark}{label}{mark}"]
            cells += [f"{mark}{number(v)}{mark}{delta(v, b)}" for v, b in zip(values[:3], base_values)]
            cells.append(number(values[3]))
            out.write("| " + " | ".join(cells) + " |\n")
        else:
            value = report[key]
            out.write(line + delta(value, base.get(key)) + "\n")
    out.write("\n</details>\n\n")


def load_reports(directory):
    reports = {}
    for path in sorted(glob.glob(os.path.join(directory, "report-*.md"))):
        name = os.path.basename(path)[len("report-"):-len(".md")]
        reports[name] = path
    return reports


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--reports", required=True, help="directory holding the report-<dataset>.md of the run")
    parser.add_argument("--baseline", help="directory holding the report-<dataset>.md to compare against")
    args = parser.parse_args()

    reports = load_reports(args.reports)
    if not reports:
        sys.exit(f"No report-*.md found under {args.reports}")
    baselines = load_reports(args.baseline) if args.baseline and os.path.isdir(args.baseline) else {}

    for name, path in reports.items():
        baseline = parse_report(baselines[name]) if name in baselines else None
        render_dataset(name, path, parse_report(path), baseline, sys.stdout)


if __name__ == "__main__":
    main()
