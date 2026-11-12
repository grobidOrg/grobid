from dataclasses import dataclass
from typing import List, Tuple, Dict, Any
import numpy as np
from scipy.optimize import linear_sum_assignment

from evaluation_utils import get_normalized, calculate_similarity, NormalizedCache


@dataclass
class Match:
    gt_index: int
    pred_index: int
    ground_truth: str
    prediction: str
    score: float


class BaseMatcher:
    def __init__(self, normalization: bool = True, threshold: float = 1.0,
                 cache: NormalizedCache | None = None) -> None:
        self.normalization = normalization
        self.threshold = threshold
        self.cache = cache if cache is not None else NormalizedCache()

    def _prep(self, text: str) -> str:
        return get_normalized(text, self.cache) if self.normalization else text

    def score(self, a: str, b: str) -> float:
        raise NotImplementedError

    def is_match(self, a: str, b: str) -> bool:
        return self.score(a, b) >= self.threshold

    def match_lists(self, gt_items: List[str], pred_items: List[str]) -> Tuple[List[Match], List[int], List[int]]:
        if not gt_items or not pred_items:
            return [], list(range(len(gt_items))), list(range(len(pred_items)))

        n_gt, n_pred = len(gt_items), len(pred_items)
        cost = np.ones((n_gt, n_pred), dtype=float)

        for i, gt in enumerate(gt_items):
            g = self._prep(gt)
            for j, pr in enumerate(pred_items):
                p = self._prep(pr)
                s = self.score(g, p)
                if s >= self.threshold:
                    cost[i, j] = 1.0 - s

        rows, cols = linear_sum_assignment(cost)
        matches: List[Match] = []
        used_gt, used_pred = set(), set()

        for r, c in zip(rows, cols):
            s = 1.0 - float(cost[r, c])
            if s >= self.threshold:
                matches.append(Match(
                    gt_index=int(r),
                    pred_index=int(c),
                    ground_truth=gt_items[r],
                    prediction=pred_items[c],
                    score=s,
                ))
                used_gt.add(r)
                used_pred.add(c)

        unmatched_gt = [i for i in range(n_gt) if i not in used_gt]
        unmatched_pred = [i for i in range(n_pred) if i not in used_pred]
        return matches, unmatched_gt, unmatched_pred


class StrictMatcher(BaseMatcher):
    def __init__(self, normalization: bool = True, cache: NormalizedCache | None = None) -> None:
        super().__init__(normalization=normalization, threshold=1.0, cache=cache)

    def score(self, a: str, b: str) -> float:
        return 1.0 if a == b else 0.0


class FuzzyMatcher(BaseMatcher):
    def __init__(self, normalization: bool = True, threshold: float = 0.95,
                 cache: NormalizedCache | None = None) -> None:
        super().__init__(normalization=normalization, threshold=threshold, cache=cache)

    def score(self, a: str, b: str) -> float:
        return float(calculate_similarity(a, b))
