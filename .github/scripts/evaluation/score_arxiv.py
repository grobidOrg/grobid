#!/usr/bin/env python3
"""
Score the authors and affiliations of the GROBID TEI of the arXiv corpus against its JSON gold.

    score_arxiv.py --data data/COMET_arxiv_1091 --output reports/report-COMET_arxiv_1091.md \\
        [--json arxiv-eval.json]

The corpus directory holds one directory per article, with the gold record (<stem>.gold.json)
and the TEI produced by `jatsEval` (<stem>.fulltext.tei.xml). The scorer is the one of
ScienciaLAB/grobid-affiliation-evaluation, vendored under arxiv/ (see arxiv/README.md). An
article without TEI counts as an article where GROBID found no author: all its authors and
affiliations are false negatives.

The Markdown report has the layout of the `jatsEval` reports, so that compare_reports.py renders
it and compares it with a baseline the same way. --json writes the full output of the scorer,
with the matches of each article.
"""

import argparse
import json
import sys
import tempfile
from pathlib import Path

SCORER_DIR = Path(__file__).resolve().parent / "arxiv"
sys.path.insert(0, str(SCORER_DIR))

from grobid_xml_to_json import GrobidXMLParser  # noqa: E402
from run_evaluation import evaluate_dataset, load_config  # noqa: E402

CONFIG = SCORER_DIR / "author_affiliation_eval_config.yaml"
SECTION = "Authors and affiliations (arXiv)"
MATCHINGS = {
    "strict": "Strict Matching (exact matches of the normalised strings)",
    "fuzzy": "Fuzzy Matching (similarity of the normalised strings of at least 0.95)",
}
# category of the scorer -> label in the report, the last one being the micro average of the report
CATEGORIES = {
    "entity:author": "author",
    "field:affiliations": "affiliations",
    "combined": "combined",
}


def load_corpus(data):
    """Return (gold records, {doc id: predicted authors}, ids of the articles without TEI)."""
    gold, predictions, without_tei = [], {}, []
    for gold_path in sorted(data.glob("*/*.gold.json")):
        record = json.loads(gold_path.read_text(encoding="utf-8"))
        doc_id = record["arxiv_id"]
        gold.append(record)
        tei = sorted(gold_path.parent.glob("*.fulltext*.tei.xml"))
        if len(tei) > 1:
            sys.exit(f"More than one TEI for {doc_id}: {', '.join(str(path) for path in tei)}")
        if not tei:
            without_tei.append(doc_id)
            predictions[doc_id] = []
            continue
        parser = GrobidXMLParser(tei[0].read_text(encoding="utf-8"), affiliation_mode="hybrid")
        predictions[doc_id] = parser.parse()
    return gold, predictions, without_tei


def number(value):
    return f"{round(100 * value, 2):g}"


def write_report(result, documents, without_tei, out):
    out.write(f"\n## {SECTION}\n\n")
    # the first sentence is the one of the jatsEval reports, which compare_reports.py reads
    out.write(
        f"Evaluation on {documents} random PDF files out of {documents} PDF (ratio 1.0). "
        f"{without_tei} of them have no TEI and count as articles without author.\n"
    )
    for mode, title in MATCHINGS.items():
        scores = result["reports"].get(mode, {})
        out.write(f"\n#### {title}\n\n**Field-level results**\n\n")
        out.write("| label | precision | recall | f1 | support |\n|--- |--- |--- |--- |--- |\n")
        for category, label in CATEGORIES.items():
            metric = scores.get(category)
            if metric is None:
                sys.exit(f"The scorer gave no '{category}' score in {mode} mode")
            support = metric["true_positives"] + metric["false_negatives"]
            cells = [label] + [number(metric[key]) for key in ("precision", "recall", "f1_score")]
            if category == "combined":
                out.write("| | | | | |\n")
                cells = [f"**{cell}**" for cell in cells]
            out.write("| " + " | ".join(cells) + f" | {support} |\n")
        out.write("\n")
        for category, label in CATEGORIES.items():
            metric = scores[category]
            out.write(
                f"{label}: {metric['true_positives']} true positives, {metric['false_positives']} false positives, "
                f"{metric['false_negatives']} false negatives\n\n"
            )


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--data", required=True, type=Path, help="corpus directory, e.g. data/COMET_arxiv_1091")
    parser.add_argument("--output", required=True, type=Path, help="Markdown report to write")
    parser.add_argument("--json", type=Path, help="full output of the scorer to write (JSON)")
    args = parser.parse_args()

    gold, predictions, without_tei = load_corpus(args.data)
    if not gold:
        sys.exit(f"No gold record (*/*.gold.json) under {args.data}")
    if len(without_tei) == len(gold):
        sys.exit(f"No TEI (*/*.fulltext.tei.xml) under {args.data}")

    with tempfile.TemporaryDirectory() as tmp:
        gold_path, predictions_path = Path(tmp, "gold.json"), Path(tmp, "predictions.json")
        gold_path.write_text(json.dumps(gold, ensure_ascii=False), encoding="utf-8")
        predictions_path.write_text(json.dumps(predictions, ensure_ascii=False), encoding="utf-8")
        result = evaluate_dataset(str(gold_path), str(predictions_path), load_config(str(CONFIG)), SCORER_DIR)

    failed = [d for d in result["document_results"] if d.get("status") != "success"]
    if failed:
        sys.exit(f"The scorer failed on {len(failed)} articles, e.g. {failed[0].get('doc_id')}: {failed[0].get('error')}")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as out:
        write_report(result, len(gold), len(without_tei), out)
    if args.json:
        result["summary"]["documents_without_tei"] = without_tei
        args.json.write_text(json.dumps(result, ensure_ascii=False, indent=1), encoding="utf-8")
    print(
        f"{len(gold)} gold records, {len(gold) - len(without_tei)} with TEI; report written to {args.output}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
