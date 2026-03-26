---
title: "Model Selection"
description: "CRF vs Deep Learning models"
sidebar_position: 4
---

# Model Selection

Use this page when you need to decide whether a model should stay on CRF or move to a DeLFT-backed deep-learning implementation.

## Default recommendation

Keep CRF as the default unless you have a task-specific reason to change it.

Why:

- lower runtime complexity
- better throughput on commodity CPU hardware
- easier deployment and scaling

## Where deep learning is most worth considering

The repository history and older documentation point to the strongest deep-learning gains for:

- `citation`
- `affiliation-address`
- `reference-segmenter`
- `header`
- `funding-acknowledgement`

The practical value is not equal across these tasks. `citation` is usually the clearest candidate when accuracy matters.

## Where CRF still makes sense

CRF remains a strong default for:

- the overall default system configuration
- tasks where the measured gain from deep learning is small
- deployments where runtime predictability matters more than squeezing out a few extra F1 points
- `fulltext`, which is still not a good fit for the same deep-learning approach because the input sequences are too large

## Local configuration pattern

Model selection happens in `grobid.yaml`.

Example for moving `citation` to DeLFT:

```yaml
models:
  - name: "citation"
    engine: "delft"
    architecture: "BidLSTM_CRF_FEATURES"
```

Typical recommendation:

- use `BidLSTM_CRF_FEATURES` when layout features matter
- use simpler architectures only when you have a specific reason

## Transformer caution

Transformer-based options such as `BERT_CRF` can be configured, but they are not the practical default.

Why:

- larger model size
- extra training burden
- not all shipped models are available by default
- real deployment wins are not universal

## Best practical strategy

Use mixed mode.

That means:

- keep most models on CRF
- switch only the models that clearly improve your workload
- validate runtime and memory impact on real documents, not synthetic expectations

## Related pages

- [Deep Learning](../../explanation/deep-learning)
- [Training Workflow](./training-workflow)
- [Evaluation](./evaluation)
