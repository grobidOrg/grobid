# arXiv author and affiliation scorer

Scorer of the arXiv corpus of the end-to-end evaluation (`COMET_arxiv_1091`, see
doc/End-to-end-evaluation.md), whose gold data is not JATS but one JSON record per article,
from [cometadata/arxiv-author-affiliations](https://huggingface.co/datasets/cometadata/arxiv-author-affiliations):

```json
{"arxiv_id": "arXiv:0704.1459", "authors": [{"name": "Valentin Ferenczi", "affiliations": ["Institut de Mathématiques de Jussieu, ..."]}], ...}
```

`../score_arxiv.py` is the entry point. The other files are copied, unchanged, from
ScienciaLAB/grobid-affiliation-evaluation (commit 45babb7, plus its uncommitted change of
`grobid_xml_to_json.py` that strips the leading markers of the raw affiliations: `1`, `a)`, `*`,
`†`, `(1)`, mathematical italic letters...), so that the scores are the same as with that tool:

| file | in grobid-affiliation-evaluation |
|---|---|
| `grobid_xml_to_json.py` | `tools/convert_grobid_outputs/` — GROBID TEI to `{name, affiliations}` per author |
| `run_evaluation.py`, `generic_evaluator.py`, `matcher.py`, `evaluation_utils.py`, `data_adapter.py` | `tools/evaluation/` |
| `author_affiliation_eval_config.yaml`, `author_affiliation_schema.json` | `tools/evaluation/tasks/author-affiliation-extraction/` |

The authors of an article are matched one to one (Hungarian algorithm) on their normalised
name, exactly (`strict`) or with a similarity of at least 0.95 (`fuzzy`). Three scores follow:

* `author` — the matched names;
* `affiliations` — the affiliations of the matched authors, matched the same way, the
  affiliations of the unmatched authors counting as false positives or false negatives;
* `combined` — an author counts only if the name and all the affiliations match.

Dependencies: `rapidfuzz`, `numpy`, `scipy`, `pyyaml`.
