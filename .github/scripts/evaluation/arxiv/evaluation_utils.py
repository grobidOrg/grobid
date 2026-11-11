import re
import unicodedata
from dataclasses import dataclass, asdict, field
from typing import Tuple, List, Dict, Optional, Any
from rapidfuzz import fuzz, distance

@dataclass
class EvaluationMetrics:
    precision: float
    recall: float
    f1_score: float
    f05_score: float
    f15_score: float
    true_positives: int
    false_positives: int
    false_negatives: int

    def to_dict(self):
        return asdict(self)

    @classmethod
    def from_counts(cls, true_positives: int, false_positives: int,
                    false_negatives: int) -> 'EvaluationMetrics':
        precision = true_positives / (true_positives + false_positives) if (true_positives + false_positives) > 0 else 0.0
        recall = true_positives / (true_positives + false_negatives) if (true_positives + false_negatives) > 0 else 0.0
        f1_score = calculate_f_beta(precision, recall, 1.0)
        f05_score = calculate_f_beta(precision, recall, 0.5)
        f15_score = calculate_f_beta(precision, recall, 1.5)

        return cls(
            precision=precision,
            recall=recall,
            f1_score=f1_score,
            f05_score=f05_score,
            f15_score=f15_score,
            true_positives=true_positives,
            false_positives=false_positives,
            false_negatives=false_negatives
        )


def aggressive_normalize(text: str) -> str:
    if not text:
        return ""

    text = text.lower()
    text = unicodedata.normalize('NFKD', text)
    text = ''.join([c for c in text if not unicodedata.combining(c)])
    text = re.sub(r'[^a-z0-9\s]', ' ', text)
    text = re.sub(r'\s+', ' ', text)
    text = text.strip()

    return text


def calculate_edit_distance(str1: str, str2: str) -> int:
    return distance.Levenshtein.distance(str1, str2)


def calculate_similarity(text1: str, text2: str) -> float:
    if not text1 or not text2:
        return 0.0
    return fuzz.ratio(text1, text2) / 100.0


def calculate_f_beta(precision: float, recall: float, beta: float) -> float:
    if precision + recall == 0:
        return 0.0
    return (1 + beta**2) * (precision * recall) / (beta**2 * precision + recall)


def get_normalized(text: str, cache: Optional['NormalizedCache'] = None) -> str:
    if cache:
        return cache.get_normalized(text)
    return aggressive_normalize(text)


class NormalizedCache:
    def __init__(self):
        self._cache: Dict[str, str] = {}
        self._hits = 0
        self._misses = 0

    def get_normalized(self, text: str) -> str:
        if text in self._cache:
            self._hits += 1
            return self._cache[text]

        self._misses += 1
        normalized = aggressive_normalize(text)
        self._cache[text] = normalized
        return normalized

    def clear(self):
        self._cache.clear()
        self._hits = 0
        self._misses = 0

    def get_stats(self) -> Dict[str, int]:
        return {
            'hits': self._hits,
            'misses': self._misses,
            'cache_size': len(self._cache),
            'hit_rate': self._hits / (self._hits + self._misses) if (self._hits + self._misses) > 0 else 0
        }

@dataclass
class DocumentEvaluationResult:
    doc_id: str
    status: str
    error: Optional[str] = None
    metrics: Dict[str, Dict[str, EvaluationMetrics]] = field(default_factory=dict)
    details: Dict[str, Any] = field(default_factory=dict)

    def add_metric(self, category: str, mode: str, metric: EvaluationMetrics) -> None:
        category_metrics = self.metrics.setdefault(category, {})
        category_metrics[mode] = metric

    def add_detail(self, key: str, value: Any) -> None:
        self.details[key] = value

    def to_dict(self):
        result = {
            'doc_id': self.doc_id,
            'status': self.status
        }
        if self.error:
            result['error'] = self.error
        if self.metrics:
            result['metrics'] = {
                category: {
                    mode: metric.to_dict()
                    for mode, metric in per_mode.items()
                }
                for category, per_mode in self.metrics.items()
            }
        if self.details:
            result['details'] = self.details
        return result


def resolve_entity_label(schema: Dict[str, Any], config: Optional[Dict[str, Any]] = None) -> str:
    config = config or {}
    label_candidate = (
        schema.get("entity_name")
        or config.get("task_entity_name")
        or schema.get("entities_field")
        or "entity"
    )
    return str(label_candidate)


def normalize_identifier(name: str) -> str:
    token = re.sub(r"[^0-9a-zA-Z]+", "_", name.strip())
    token = re.sub(r"_+", "_", token).strip("_")
    if not token:
        return "entity"
    return token.lower()


def resolve_entity_category_key(schema: Dict[str, Any], config: Optional[Dict[str, Any]] = None) -> str:
    entity_label = resolve_entity_label(schema, config)
    return f"entity:{normalize_identifier(entity_label)}"
