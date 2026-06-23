# Benchmarking

## End-to-end evaluation

See the [End-to-end evaluation](../End-to-end-evaluation.md) page for explanations and for reproducing the evaluations from the reference PDF sets. For end-to-end results with holdout sets, the key points to consider are the following:

- The datasets are independent from the training data used to train the different models involved in the full document processing, in particular several models do not use data from PMC articles at all. As a stable holdout set, it should thus provide much more reliable evaluations than cross-validation metrics.

- The evaluation covers the whole process, including PDF extraction, PDF noisiness and error cascading. It should thus provide a more realistic evaluation for the end user than the model-specific metrics with "clean" data that are usually reported in the literature. 

- As the evaluation data come from XML PMC and the scientific publishers, it contains some encoding errors (publisher data are far from perfect) and are not always complete (for instance some bibliographical references are provided as raw string and not structured). The results are therefore more a relative indication of error rates than trustful absolute accuracy performances.

- We think that these metrics are very good to compare improvements over time and to catch possible regressions, because relative improvements can be reliable with slightly imperfect evaluation data. 

More recent versions of these benchmarks (for master snapshots) are available [here](https://github.com/kermitt2/grobid/tree/master/grobid-trainer/doc).


## General

The following reported end-to-end results are using BidLSTM_CRF_FEATURES for reference parsing and default CRF Wapiti as sequence labelling engine for the other tasks. Header extractions are consolidated by default with [biblio-glutton](https://github.com/kermitt2/biblio-glutton) service (the results with CrossRef REST API as consolidation service are similar but much slower):

- end-to-end evaluation using the [PMC 1943 set](Benchmarking-pmc.md)

- end-to-end evaluation using the [biorxiv-10k-test-2000](Benchmarking-biorxiv.md)

- end-to-end evaluation using the [PLOS dataset](Benchmarking-plos.md)

- end-to-end evaluation using the [eLife dataset](Benchmarking-elife.md)

See [here](https://github.com/kermitt2/grobid/tree/master/grobid-trainer/doc) for some older and additional evaluations. 
