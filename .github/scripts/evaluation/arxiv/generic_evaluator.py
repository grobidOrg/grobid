from typing import Dict, List, Any, Optional, Tuple

from evaluation_utils import (
    NormalizedCache,
    DocumentEvaluationResult,
    EvaluationMetrics,
    get_normalized,
    resolve_entity_category_key,
)
from matcher import StrictMatcher, FuzzyMatcher, Match


class GenericEvaluator:
    def __init__(
        self,
        gt_entities: List[Dict[str, Any]],
        pred_entities: List[Dict[str, Any]],
        doc_id: str,
        config: Dict[str, Any],
        schema: Dict[str, Any],
        cache: Optional[NormalizedCache] = None,
    ) -> None:
        self.doc_id = doc_id
        self.config = config
        self.schema = schema
        self.cache = cache or NormalizedCache()

        self.entities_field = schema.get("entities_field", "authors")
        self.key_field = config.get("key_field", "name")
        self.fields: Dict[str, Dict[str, Any]] = schema.get("fields", {})
        self.field_rules: Dict[str, Dict[str, Any]] = config.get("field_eval_rules", {})
        self.harsh_penalty: bool = bool(config.get("combined_eval", {}).get("harsh_penalty", True))
        self.array_fields = [
            name
            for name, spec in self.fields.items()
            if isinstance(spec, dict) and str(spec.get("type", "")).startswith("array[")
        ]

        self.gt_entities = [e for e in (gt_entities or []) if isinstance(
            e, dict) and self.key_field in e]
        self.pred_entities = [e for e in (pred_entities or []) if isinstance(
            e, dict) and self.key_field in e]
        self.entity_category_key = resolve_entity_category_key(schema, config)

    def evaluate(self) -> DocumentEvaluationResult:
        result = DocumentEvaluationResult(
            doc_id=self.doc_id,
            status="success",
        )

        (
            strict_pairs,
            strict_unmatched_gt,
            strict_unmatched_pred,
            all_pairs,
            fuzzy_pairs,
            fuzzy_unmatched_gt,
            fuzzy_unmatched_pred,
        ) = self._match_entities()

        pair_attr_eval = self._evaluate_attributes(all_pairs)

        strict_entity_metrics = EvaluationMetrics.from_counts(
            true_positives=len(strict_pairs),
            false_positives=len(strict_unmatched_pred),
            false_negatives=len(strict_unmatched_gt),
        )
        fuzzy_entity_metrics = EvaluationMetrics.from_counts(
            true_positives=len(all_pairs),
            false_positives=len(fuzzy_unmatched_pred),
            false_negatives=len(fuzzy_unmatched_gt),
        )
        result.add_metric(self.entity_category_key, "strict", strict_entity_metrics)
        result.add_metric(self.entity_category_key, "fuzzy", fuzzy_entity_metrics)

        for field_name in self.array_fields:
            strict_aff_tp, strict_aff_fp, strict_aff_fn = self._aggregate_array_field(
                field_name=field_name,
                pairs=strict_pairs,
                pair_attr_eval=pair_attr_eval,
                unmatched_gt=strict_unmatched_gt,
                unmatched_pred=strict_unmatched_pred,
                mode="strict",
            )
            fuzzy_aff_tp, fuzzy_aff_fp, fuzzy_aff_fn = self._aggregate_array_field(
                field_name=field_name,
                pairs=all_pairs,
                pair_attr_eval=pair_attr_eval,
                unmatched_gt=fuzzy_unmatched_gt,
                unmatched_pred=fuzzy_unmatched_pred,
                mode="fuzzy",
            )
            result.add_metric(
                f"field:{field_name}",
                "strict",
                EvaluationMetrics.from_counts(
                    strict_aff_tp, strict_aff_fp, strict_aff_fn
                ),
            )
            result.add_metric(
                f"field:{field_name}",
                "fuzzy",
                EvaluationMetrics.from_counts(
                    fuzzy_aff_tp, fuzzy_aff_fp, fuzzy_aff_fn
                ),
            )

        strict_tp, strict_fp, strict_fn = self._aggregate_combined(
            pairs=strict_pairs,
            pair_attr_eval=pair_attr_eval,
            unmatched_gt=strict_unmatched_gt,
            unmatched_pred=strict_unmatched_pred,
            mode="strict",
        )
        fuzzy_tp, fuzzy_fp, fuzzy_fn = self._aggregate_combined(
            pairs=all_pairs,
            pair_attr_eval=pair_attr_eval,
            unmatched_gt=fuzzy_unmatched_gt,
            unmatched_pred=fuzzy_unmatched_pred,
            mode="fuzzy",
        )
        result.add_metric(
            "combined",
            "strict",
            EvaluationMetrics.from_counts(strict_tp, strict_fp, strict_fn),
        )
        result.add_metric(
            "combined",
            "fuzzy",
            EvaluationMetrics.from_counts(fuzzy_tp, fuzzy_fp, fuzzy_fn),
        )

        result.add_detail(
            "entity_matches",
            self._build_entity_details(
                strict_pairs,
                fuzzy_pairs,
                strict_unmatched_gt,
                strict_unmatched_pred,
            ),
        )

        array_details: Dict[str, Any] = {}
        for field_name in self.array_fields:
            details = self._build_array_field_details(
                field_name=field_name,
                pairs=all_pairs,
                pair_attr_eval=pair_attr_eval,
            )
            if details:
                array_details[field_name] = details
        if array_details:
            result.add_detail("array_field_details", array_details)

        return result

    def _match_entities(self) -> Tuple[List[Tuple[int, int]], List[int], List[int],
                                       List[Tuple[int, int]], List[Tuple[int, int]], List[int], List[int]]:
        key_rules = self.field_rules.get(self.key_field, {})
        norm = bool(key_rules.get("normalization", True))
        match_type = key_rules.get("match_type", "strict")
        threshold = float(key_rules.get("similarity_threshold", 0.95))

        gt_keys = [str(e.get(self.key_field, "")) for e in self.gt_entities]
        pd_keys = [str(e.get(self.key_field, "")) for e in self.pred_entities]

        strict_matcher = StrictMatcher(normalization=norm, cache=self.cache)
        strict_matches, um_gt, um_pd = strict_matcher.match_lists(
            gt_keys, pd_keys)
        strict_pairs = [(m.gt_index, m.pred_index) for m in strict_matches]

        strict_unmatched_gt = um_gt[:]
        strict_unmatched_pred = um_pd[:]

        fuzzy_pairs: List[Tuple[int, int]] = []
        if match_type == "fuzzy":
            gt_left = [gt_keys[i] for i in um_gt]
            pd_left = [pd_keys[j] for j in um_pd]
            fuzzy_matcher = FuzzyMatcher(
                normalization=norm, threshold=threshold, cache=self.cache)
            f_matches, f_um_gt_local, f_um_pd_local = fuzzy_matcher.match_lists(
                gt_left, pd_left)
            for m in f_matches:
                gt_idx = um_gt[m.gt_index]
                pd_idx = um_pd[m.pred_index]
                fuzzy_pairs.append((gt_idx, pd_idx))

            fuzzy_unmatched_gt = [um_gt[i] for i in f_um_gt_local]
            fuzzy_unmatched_pred = [um_pd[i] for i in f_um_pd_local]
        else:
            fuzzy_unmatched_gt = strict_unmatched_gt[:]
            fuzzy_unmatched_pred = strict_unmatched_pred[:]

        all_pairs = strict_pairs + fuzzy_pairs
        return (
            strict_pairs,
            strict_unmatched_gt,
            strict_unmatched_pred,
            all_pairs,
            fuzzy_pairs,
            fuzzy_unmatched_gt,
            fuzzy_unmatched_pred,
        )

    def _evaluate_attributes(self, pairs: List[Tuple[int, int]]) -> Dict[Tuple[int, int], Dict[str, Any]]:
        eval_map: Dict[Tuple[int, int], Dict[str, Any]] = {}
        for gt_i, pd_i in pairs:
            per_field: Dict[str, Any] = {}
            for field_name, field_spec in self.fields.items():
                if field_name == self.key_field:
                    continue
                rules = self.field_rules.get(field_name, {})
                norm = bool(rules.get("normalization", True))
                ftype = field_spec.get("type", "string")
                match_type = rules.get("match_type", "strict")
                threshold = float(rules.get("similarity_threshold", 0.95))

                gt_val = self.gt_entities[gt_i].get(field_name)
                pd_val = self.pred_entities[pd_i].get(field_name)

                if ftype.startswith("array["):
                    gt_list = list(gt_val or [])
                    pd_list = list(pd_val or [])

                    def build_map(items: List[str]) -> Dict[str, str]:
                        m: Dict[str, str] = {}
                        for s in items:
                            key = get_normalized(
                                str(s), self.cache) if norm else str(s)
                            m[key] = str(s)
                        return m

                    gt_map = build_map(gt_list)
                    pd_map = build_map(pd_list)
                    gt_norms = set(gt_map.keys())
                    pd_norms = set(pd_map.keys())

                    strict_matched_norms = sorted(gt_norms & pd_norms)
                    s_um_gt_norms = sorted(gt_norms - pd_norms)
                    s_um_pd_norms = sorted(pd_norms - gt_norms)

                    f_um_gt_norms = list(s_um_gt_norms)
                    f_um_pd_norms = list(s_um_pd_norms)
                    fuzzy_additional_pairs: List[Tuple[str, str]] = []
                    if match_type == "fuzzy" and (s_um_gt_norms or s_um_pd_norms):
                        gt_left_vals = [gt_map[n] for n in s_um_gt_norms]
                        pd_left_vals = [pd_map[n] for n in s_um_pd_norms]
                        f_m = FuzzyMatcher(
                            normalization=norm, threshold=threshold, cache=self.cache)
                        f_matches, l_um_gt_idx, l_um_pd_idx = f_m.match_lists(
                            gt_left_vals, pd_left_vals)
                        for m in f_matches:
                            fuzzy_additional_pairs.append(
                                (gt_left_vals[m.gt_index], pd_left_vals[m.pred_index]))
                        f_um_gt_norms = [s_um_gt_norms[i] for i in l_um_gt_idx]
                        f_um_pd_norms = [s_um_pd_norms[i] for i in l_um_pd_idx]

                    per_field[field_name] = {
                        "type": "array",
                        "strict": {
                            "matched_norms": strict_matched_norms,
                            "unmatched_gt_norms": s_um_gt_norms,
                            "unmatched_pred_norms": s_um_pd_norms,
                            "full_match": len(s_um_gt_norms) == 0 and len(s_um_pd_norms) == 0,
                            "gt_map": gt_map,
                            "pred_map": pd_map,
                        },
                        "fuzzy": {
                            "matched_count": len(strict_matched_norms) + len(fuzzy_additional_pairs),
                            "unmatched_gt_norms": f_um_gt_norms,
                            "unmatched_pred_norms": f_um_pd_norms,
                            "full_match": len(f_um_gt_norms) == 0 and len(f_um_pd_norms) == 0,
                        },
                    }
                else:
                    gt_s = str(gt_val) if gt_val is not None else ""
                    pd_s = str(pd_val) if pd_val is not None else ""
                    s_m = StrictMatcher(normalization=norm, cache=self.cache)
                    strict_ok = s_m.is_match(gt_s, pd_s)
                    fuzzy_ok = strict_ok
                    if match_type == "fuzzy" and not strict_ok:
                        f_m = FuzzyMatcher(
                            normalization=norm, threshold=threshold, cache=self.cache)
                        fuzzy_ok = f_m.is_match(gt_s, pd_s)
                    per_field[field_name] = {
                        "type": "string",
                        "strict": {"matched": bool(strict_ok)},
                        "fuzzy": {"matched": bool(fuzzy_ok)},
                    }

            eval_map[(gt_i, pd_i)] = per_field
        return eval_map

    def _aggregate_array_field(
        self,
        field_name: str,
        pairs: List[Tuple[int, int]],
        pair_attr_eval: Dict[Tuple[int, int], Dict[str, Any]],
        unmatched_gt: List[int],
        unmatched_pred: List[int],
        mode: str,
    ) -> Tuple[int, int, int]:
        if not field_name:
            return (0, len(unmatched_pred), len(unmatched_gt))

        tp = fp = fn = 0
        for pair in pairs:
            f_eval = pair_attr_eval.get(pair, {}).get(field_name)
            if not f_eval or f_eval["type"] != "array":
                continue
            if mode == "strict":
                tp += len(f_eval["strict"].get("matched_norms", []))
                fp += len(f_eval["strict"].get("unmatched_pred_norms", []))
                fn += len(f_eval["strict"].get("unmatched_gt_norms", []))
            else:
                tp += int(f_eval["fuzzy"].get("matched_count", 0))
                fp += len(f_eval["fuzzy"].get("unmatched_pred_norms", []))
                fn += len(f_eval["fuzzy"].get("unmatched_gt_norms", []))

        for idx in unmatched_gt:
            gt_vals = self.gt_entities[idx].get(field_name, []) or []
            fn += len(gt_vals)
        for idx in unmatched_pred:
            pd_vals = self.pred_entities[idx].get(field_name, []) or []
            fp += len(pd_vals)
        return tp, fp, fn

    def _aggregate_combined(
        self,
        pairs: List[Tuple[int, int]],
        pair_attr_eval: Dict[Tuple[int, int], Dict[str, Any]],
        unmatched_gt: List[int],
        unmatched_pred: List[int],
        mode: str,
    ) -> Tuple[int, int, int]:
        tp = 0
        partial = 0
        for pair in pairs:
            if self._all_attributes_full(pair_attr_eval.get(pair, {}), mode):
                tp += 1
            else:
                partial += 1

        fp = len(unmatched_pred)
        fn = len(unmatched_gt)
        if self.harsh_penalty and partial:
            fp += partial
            fn += partial
        return tp, fp, fn

    def _all_attributes_full(self, fields_eval: Dict[str, Any], mode: str) -> bool:
        for field_name, spec in self.fields.items():
            if field_name == self.key_field:
                continue
            f_eval = fields_eval.get(field_name)
            if not f_eval:
                continue
            if f_eval["type"] == "array":
                if not f_eval[mode]["full_match"]:
                    return False
            else:
                if not f_eval[mode]["matched"]:
                    return False
        return True

    def _build_entity_details(
        self,
        strict_pairs: List[Tuple[int, int]],
        fuzzy_pairs: List[Tuple[int, int]],
        missed_gt_strict: List[int],
        extra_pred_strict: List[int],
    ) -> List[Dict[str, Any]]:
        details: List[Dict[str, Any]] = []

        key_rules = self.field_rules.get(self.key_field, {})
        fuzzy_threshold = float(key_rules.get("similarity_threshold", 0.95))

        def _similarity(gt_idx: int, pd_idx: int) -> float:
            gt_val = str(self.gt_entities[gt_idx].get(self.key_field, ""))
            pd_val = str(self.pred_entities[pd_idx].get(self.key_field, ""))
            if get_normalized(gt_val, self.cache) == get_normalized(pd_val, self.cache):
                return 1.0
            return fuzzy_threshold

        for gt_i, pd_i in strict_pairs:
            details.append(
                {
                    "match_type": "strict",
                    "matched": True,
                    "ground_truth": self.gt_entities[gt_i],
                    "prediction": self.pred_entities[pd_i],
                    "similarity": 1.0,
                }
            )

        for gt_i, pd_i in fuzzy_pairs:
            details.append(
                {
                    "match_type": "fuzzy",
                    "matched": True,
                    "ground_truth": self.gt_entities[gt_i],
                    "prediction": self.pred_entities[pd_i],
                    "similarity": _similarity(gt_i, pd_i),
                }
            )

        for gt_i in missed_gt_strict:
            details.append(
                {
                    "match_type": "unmatched_ground_truth",
                    "matched": False,
                    "ground_truth": self.gt_entities[gt_i],
                    "prediction": None,
                    "similarity": 0.0,
                }
            )

        for pd_i in extra_pred_strict:
            details.append(
                {
                    "match_type": "unmatched_prediction",
                    "matched": False,
                    "ground_truth": None,
                    "prediction": self.pred_entities[pd_i],
                    "similarity": 0.0,
                }
            )
        return details

    def _build_array_field_details(
        self,
        field_name: str,
        pairs: List[Tuple[int, int]],
        pair_attr_eval: Dict[Tuple[int, int], Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        details: List[Dict[str, Any]] = []

        for gt_i, pd_i in pairs:
            f_eval = pair_attr_eval.get((gt_i, pd_i), {}).get(field_name)
            if not f_eval or f_eval["type"] != "array":
                continue
            s = f_eval["strict"]
            f = f_eval["fuzzy"]
            s_metrics = EvaluationMetrics.from_counts(
                len(s.get("matched_norms", [])),
                len(s.get("unmatched_pred_norms", [])),
                len(s.get("unmatched_gt_norms", [])),
            )
            f_metrics = EvaluationMetrics.from_counts(
                int(f.get("matched_count", 0)),
                len(f.get("unmatched_pred_norms", [])),
                len(f.get("unmatched_gt_norms", [])),
            )
            gt_map = s.get("gt_map", {})
            pd_map = s.get("pred_map", {})
            details.append(
                {
                    "strict_metrics": s_metrics.to_dict(),
                    "fuzzy_metrics": f_metrics.to_dict(),
                    "strict": {
                        "matched": [gt_map[n] for n in s.get("matched_norms", [])],
                        "missed": [gt_map[n] for n in s.get("unmatched_gt_norms", [])],
                        "extra": [pd_map[n] for n in s.get("unmatched_pred_norms", [])],
                    },
                    "fuzzy": {
                        "matched_count": int(f.get("matched_count", 0)),
                        "unmatched_gt_norms": f.get("unmatched_gt_norms", []),
                        "unmatched_pred_norms": f.get("unmatched_pred_norms", []),
                    },
                }
            )
        return details
