---
title: "Training Workflow"
description: "End-to-end model training process"
sidebar_position: 3
---

# Training Workflow

Use this page for the practical training flow after you already understand which model you want to retrain.

## Dataset layout

Training data for a model lives under:

```text
grobid-trainer/resources/dataset/<MODEL>/
```

Typical directories:

- `corpus/` for training material
- `evaluation/` for held-out evaluation material when you manage the split manually

## Simple mode: train and evaluate with one Gradle task

For the common case, use the Gradle wrapper from the repo root:

```bash
./gradlew train_<model>
```

Examples:

```bash
./gradlew train_header
./gradlew train_name_header
```

This uses the data already arranged in the model's `corpus/` and `evaluation/` directories.

## Full mode: trainer jar commands

When you need more control, use the trainer jar directly.

Train only:

```bash
java -Xmx1024m -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-trainer/build/libs/grobid-trainer-<current version>-onejar.jar 0 <model> -gH grobid-home
```

Evaluate only:

```bash
java -Xmx1024m -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-trainer/build/libs/grobid-trainer-<current version>-onejar.jar 1 <model> -gH grobid-home
```

Automatically split, then train and evaluate:

```bash
java -Xmx1024m -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-trainer/build/libs/grobid-trainer-<current version>-onejar.jar 2 <model> -gH grobid-home -s 0.8
```

## Incremental training

If you already have a trained model and want to continue from it instead of starting from scratch, use `-i`.

```bash
java -Xmx1024m -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-trainer/build/libs/grobid-trainer-<current version>-onejar.jar 0 <model> -gH grobid-home -i
```

Use this carefully. A full retraining with all available data is usually the better final model, but incremental training is useful for faster iteration.

## N-fold evaluation

For more robust model-level evaluation, run n-fold cross-validation:

```bash
java -Xmx1024m -Djava.library.path=grobid-home/lib/lin-64:grobid-home/lib/lin-64/jep -jar grobid-trainer/build/libs/grobid-trainer-<current version>-onejar.jar 3 <model> -gH grobid-home -n 10
```

## Generating training data from PDFs

You can pre-generate model-specific TEI training files from PDFs using the batch-side training generation path (`createTraining`).

This creates model-specific TEI files and, for layout-aware models, companion feature files.

These outputs are not gold data yet. They still need manual review and correction before becoming trusted training material.

## Practical advice

- keep backups of existing trained models before overwriting them
- validate that you are training the correct model directory
- prefer targeted iterative improvement over large blind corpus growth
- keep evaluation separate from the examples that inspired your latest fix when possible

## Related pages

- [Training Overview](./training-overview)
- [Annotation Guidelines](./annotation-guidelines)
- [Evaluation](./evaluation)
