"""
Diagnose why Wapiti drops sequences from a GROBID segmentation .train file.

Background: when training a GROBID CRF, Wapiti sometimes prints

    * Load training data
    warning: missing tokens, cannot apply pattern

and the final "nb train: N" count is lower than the "training sequences: M"
count printed by GROBID just before. The gap is the number of sequences Wapiti
silently dropped (typically because of row-level column-count mismatches —
see grobid-trainer/scripts/check_training_data.py).

This script partitions the .train file by sequence (blank-line separated),
flags each sequence as OK / suspicious / likely-dropped, and (optionally) maps
each sequence index back to the source TEI filename using the trainer's
"Processing: <name>" log lines.

Usage:
    python3 diagnose_segmentation_train.py <train_file> [--log <log_file>] \
        [--template <template_file>]
"""

import argparse
import os
import re
import sys
from collections import Counter


def parse_template_max_col(template_path):
    """Return the largest column index K referenced as %x[*,K] in the template."""
    pat = re.compile(r"%x\[[-+]?\d+\s*,\s*(\d+)\s*\]")
    max_col = -1
    with open(template_path) as fp:
        for line in fp:
            line = line.split("#", 1)[0]
            for m in pat.finditer(line):
                k = int(m.group(1))
                if k > max_col:
                    max_col = k
    return max_col


def split_columns(line):
    """Match Wapiti's tokenizer: tab if present, else single space."""
    if "\t" in line:
        return line.split("\t")
    return line.split(" ")


def iter_sequences(train_path):
    """Yield lists of non-blank rows, one list per blank-line-separated block."""
    seq = []
    with open(train_path, encoding="utf-8") as fp:
        for raw in fp:
            line = raw.rstrip("\n")
            if line.strip() == "":
                if seq:
                    yield seq
                    seq = []
            else:
                seq.append(line)
    if seq:
        yield seq


def parse_log_filenames(log_path):
    """Return the ordered list of TEI filenames from 'Processing: <name>' log lines."""
    pat = re.compile(r"Processing:\s+(\S+)")
    names = []
    with open(log_path, encoding="utf-8", errors="replace") as fp:
        for line in fp:
            m = pat.search(line)
            if m:
                names.append(m.group(1))
    return names


def classify(seq_rows, required_cols):
    """
    Decide whether Wapiti is likely to drop this sequence.

    Heuristic:
      - empty sequence       -> dropped (almost certainly)
      - any row with fewer than required_cols+1 columns (because column
        indices are 0-based) -> dropped or warning-spammed
      - column count varies within the sequence -> suspicious
      - otherwise OK
    Returns (status, reason, n_rows, col_dist).
      status in {"OK", "SUSPICIOUS", "DROPPED"}
    """
    n_rows = len(seq_rows)
    if n_rows == 0:
        return "DROPPED", "empty-sequence", 0, {}

    col_dist = Counter(len(split_columns(r)) for r in seq_rows)
    min_cols = min(col_dist)
    max_cols = max(col_dist)

    # Wapiti needs every row to expose at least required_cols+1 columns
    # (the label is the last column; data columns are 0..required_cols).
    # Some GROBID templates count the label column inside the data, so we
    # use a slightly conservative threshold: require_cols + 1 columns of data
    # + 1 label column = required_cols + 2. We surface both possibilities.
    needed_loose = required_cols + 1  # label may not be counted in template
    needed_strict = required_cols + 2  # label as separate trailing column

    if min_cols < needed_loose:
        return "DROPPED", f"row-too-narrow (min={min_cols} < {needed_loose})", n_rows, dict(col_dist)
    if min_cols != max_cols:
        return "SUSPICIOUS", f"mixed-cols ({dict(col_dist)})", n_rows, dict(col_dist)
    if min_cols < needed_strict:
        # all rows uniform but tight — note but don't flag as dropped
        return "OK", f"uniform-tight ({min_cols} cols, strict-needed {needed_strict})", n_rows, dict(col_dist)
    return "OK", "uniform", n_rows, dict(col_dist)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("train_file", help="path to the .train file")
    ap.add_argument("--log", help="path to the trainer log (for index->TEI mapping)")
    ap.add_argument(
        "--template",
        default=os.path.join(
            os.path.dirname(os.path.abspath(__file__)),
            "..",
            "resources",
            "dataset",
            "segmentation",
            "article",
            "dh-law-footnotes",
            "crfpp-templates",
            "segmentation.template",
        ),
        help="Wapiti CRF template (used to derive required column count)",
    )
    args = ap.parse_args()

    if not os.path.isfile(args.train_file):
        sys.exit(f"train file not found: {args.train_file}")
    if not os.path.isfile(args.template):
        sys.exit(f"template file not found: {args.template}")

    max_col = parse_template_max_col(args.template)
    if max_col < 0:
        sys.exit(f"could not parse any %x[*,K] from template: {args.template}")
    print(f"template max column index referenced: {max_col} "
          f"(so each data row needs >= {max_col + 1} data columns plus 1 label column)")
    print(f"train file: {args.train_file}")

    names = []
    if args.log:
        if not os.path.isfile(args.log):
            sys.exit(f"log file not found: {args.log}")
        names = parse_log_filenames(args.log)
        print(f"log file:   {args.log}  ({len(names)} 'Processing:' lines)")

    sequences = list(iter_sequences(args.train_file))
    print(f"sequences in train file: {len(sequences)}")
    if names and len(names) != len(sequences):
        print(f"  WARNING: log has {len(names)} 'Processing:' lines but train file has "
              f"{len(sequences)} sequences — index→TEI mapping may be off")

    statuses = Counter()
    suspicious_rows = []
    for i, seq in enumerate(sequences):
        status, reason, n_rows, col_dist = classify(seq, max_col)
        statuses[status] += 1
        if status != "OK":
            tei = names[i] if i < len(names) else "?"
            suspicious_rows.append((i, status, reason, n_rows, col_dist, tei))

    if suspicious_rows:
        print("\n--- suspicious / dropped sequences ---")
        for i, status, reason, n_rows, col_dist, tei in suspicious_rows:
            print(f"  seq #{i:03d}  [{status}]  rows={n_rows}  TEI={tei}\n             reason: {reason}")

    print("\n--- summary ---")
    print(f"  OK           : {statuses['OK']}")
    print(f"  SUSPICIOUS   : {statuses['SUSPICIOUS']}")
    print(f"  DROPPED      : {statuses['DROPPED']}")
    print(f"  total        : {sum(statuses.values())}")
    print(f"  expected gap vs 'nb train': {statuses['DROPPED']} (plus possibly the SUSPICIOUS ones)")


if __name__ == "__main__":
    main()
