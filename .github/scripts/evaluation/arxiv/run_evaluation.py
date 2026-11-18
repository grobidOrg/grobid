import json
import yaml
import argparse
from pathlib import Path
from typing import Any, Dict, Optional

from evaluation_utils import (
    DocumentEvaluationResult,
    EvaluationMetrics,
    NormalizedCache,
    resolve_entity_category_key,
    resolve_entity_label,
)
from data_adapter import adapt_legacy_format
from generic_evaluator import GenericEvaluator


def load_config(path: str) -> Dict[str, Any]:
    if path.endswith((".yaml", ".yml")):
        with open(path, "r") as f:
            return yaml.safe_load(f)
    with open(path, "r") as f:
        return json.load(f)


def evaluate_dataset(
    ground_truth_path: str,
    predictions_path: str,
    cfg: Dict[str, Any],
    config_dir: Optional[Path] = None,
) -> Dict[str, Any]:
    schema_value = cfg["entity_schema_path"]
    schema_path = Path(schema_value)
    if not schema_path.is_absolute() and config_dir is not None:
        schema_path = (config_dir / schema_path).resolve()
    else:
        schema_path = schema_path.resolve()
    if not schema_path.exists():
        raise FileNotFoundError(f"Schema file not found at {schema_path}")
    with open(schema_path, "r") as f:
        schema = json.load(f)

    with open(ground_truth_path, "r") as f:
        gt_raw = json.load(f)
    with open(predictions_path, "r") as f:
        preds_raw = json.load(f)

    gt_map = adapt_legacy_format(gt_raw, schema)

    if isinstance(preds_raw, list):
        preds_map = {}
        doc_id_field = schema.get("doc_id_field")
        for item in preds_raw:
            if isinstance(item, dict) and doc_id_field in item:
                preds_map[str(item[doc_id_field])] = item
    else:
        preds_map = preds_raw if isinstance(preds_raw, dict) else {}

    preds_map_adapted = adapt_legacy_format(preds_map, schema)

    reporting_modes = cfg.get("reporting_modes", ["strict", "fuzzy"]) or ["strict"]
    cache = NormalizedCache()

    totals: Dict[str, Dict[str, EvaluationMetrics]] = {}

    def _ensure_metric(category: str, mode: str) -> EvaluationMetrics:
        category_bucket = totals.setdefault(category, {})
        metric = category_bucket.get(mode)
        if metric is None:
            metric = EvaluationMetrics.from_counts(0, 0, 0)
            category_bucket[mode] = metric
        return metric

    document_results = []

    for doc_id, gt_entities in gt_map.items():
        raw_pred_doc = preds_map.get(doc_id)
        if raw_pred_doc is None:
            dummy = DocumentEvaluationResult(doc_id=doc_id, status="error", error="Missing prediction")
            document_results.append(dummy.to_dict())
            continue

        entities_field = schema.get("entities_field", "authors")
        pred_list_key = f"predicted_{entities_field}"
        if isinstance(raw_pred_doc, dict) and raw_pred_doc.get(pred_list_key, []) is None:
            dummy = DocumentEvaluationResult(doc_id=doc_id, status="null_prediction", error=f"{pred_list_key} is None")
            document_results.append(dummy.to_dict())
            continue

        pred_entities = preds_map_adapted.get(doc_id, [])

        evaluator = GenericEvaluator(gt_entities, pred_entities, doc_id, cfg, schema, cache)
        doc_eval = evaluator.evaluate()
        document_results.append(doc_eval.to_dict())

        if doc_eval.status == "success":
            for category, per_mode in doc_eval.metrics.items():
                for mode, metric in per_mode.items():
                    if mode not in reporting_modes:
                        continue
                    _accumulate(_ensure_metric(category, mode), metric)

    def _category_sort_key(category: str) -> tuple[int, str]:
        if category.startswith("entity:"):
            return (0, category.split(":", 1)[1])
        if category == "entity":
            return (0, "")
        if category.startswith("field:"):
            return (1, category.split(":", 1)[1])
        if category == "combined":
            return (2, category)
        return (3, category)

    sorted_categories = sorted(totals.keys(), key=_category_sort_key)

    entity_label = resolve_entity_label(schema, cfg)
    entities_field = schema.get("entities_field", "entities")
    entity_category_key = resolve_entity_category_key(schema, cfg)
    entity_identifier = entity_category_key.split(":", 1)[1] if ":" in entity_category_key else entity_category_key

    category_labels: Dict[str, str] = {
        entity_category_key: f"{entity_identifier}_identification",
        "combined": f"combined_{entities_field}",
    }
    for category in sorted_categories:
        if category.startswith("field:"):
            field_name = category.split(":", 1)[1]
            category_labels.setdefault(category, f"{field_name}_matching")
        elif category.startswith("entity:"):
            identifier = category.split(":", 1)[1]
            category_labels.setdefault(category, f"{identifier}_identification")
        else:
            category_labels.setdefault(category, category)

    reports: Dict[str, Dict[str, Any]] = {}
    for mode in reporting_modes:
        mode_metrics: Dict[str, Any] = {}
        for category in sorted_categories:
            metric = totals.get(category, {}).get(mode)
            if metric:
                mode_metrics[category] = metric.to_dict()
        reports[mode] = mode_metrics

    result = {
        "task_name": cfg.get("task_name", "evaluation"),
        "reporting_modes": reporting_modes,
        "summary": {"total_documents": len(gt_map)},
        "category_labels": category_labels,
        "reports": reports,
        "document_results": document_results,
    }
    return result


def _accumulate(total: EvaluationMetrics, delta: EvaluationMetrics) -> None:
    total.true_positives += delta.true_positives
    total.false_positives += delta.false_positives
    total.false_negatives += delta.false_negatives
    updated = EvaluationMetrics.from_counts(
        total.true_positives, total.false_positives, total.false_negatives
    )
    total.precision = updated.precision
    total.recall = updated.recall
    total.f1_score = updated.f1_score
    total.f05_score = updated.f05_score
    total.f15_score = updated.f15_score


def main():
    parser = argparse.ArgumentParser(description="Run modular, config-driven evaluation")
    parser.add_argument("-g", "--ground-truth", required=True, help="Path to ground truth JSON file")
    parser.add_argument("-p", "--predictions", required=True, help="Path to predictions JSON file")
    parser.add_argument("-o", "--output", required=True, help="Output JSON path")
    parser.add_argument("-c", "--config", required=True, help="YAML or JSON evaluation config path")
    parser.add_argument("-v", "--verbose", action="store_true", help="Print progress")
    args = parser.parse_args()

    config_path = Path(args.config).expanduser()
    cfg = load_config(str(config_path))
    result = evaluate_dataset(args.ground_truth, args.predictions, cfg, config_path.parent)

    with open(args.output, "w") as f:
        json.dump(result, f, indent=2)
    if args.verbose:
        print(f"Saved results to {args.output}")


if __name__ == "__main__":
    main()
