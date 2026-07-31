#!/usr/bin/env bash
#
# Fetch the PyTorch (delft >= 1.0.0) GROBID sequence-labelling models from the delft
# GitHub repo and install them into grobid-home/models, replacing the legacy
# TensorFlow/Keras models (model_weights.hdf5) that delft 1.0.0 cannot load.
#
# delft publishes these under data/models/sequenceLabelling/ with a "grobid-" prefix,
# whereas GROBID looks them up as "<model>-<architecture>" (no prefix). This script
# downloads by the delft name and installs under the GROBID name.
#
# Usage:
#   fetch_delft_pytorch_models.sh [MODELS_DIR] [DELFT_REF]
#     MODELS_DIR  target grobid-home/models dir (default: dir relative to this script)
#     DELFT_REF   delft git ref/tag to pull from (default: v1.0.0)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELS_DIR="${1:-${SCRIPT_DIR}/../models}"
DELFT_REF="${2:-${DELFT_REF:-v1.0.0}}"
BASE="https://raw.githubusercontent.com/kermitt2/delft/${DELFT_REF}/data/models/sequenceLabelling"

# Each delft sequence model dir ships exactly these three artifacts.
FILES=(config.json preprocessor.json model_weights.pt)

# "GROBID local dir name : delft repo model name"
# Only the models set to engine: delft in grobid-full.yaml are listed.
# NOTE: patent-citation has no PyTorch model published in delft 1.0.0 and is therefore
# omitted here; it is not exercised by processFulltextDocument.
MODELS=(
  "header-BidLSTM_ChainCRF_FEATURES:grobid-header-BidLSTM_ChainCRF_FEATURES"
  "reference-segmenter-BidLSTM_ChainCRF_FEATURES:grobid-reference-segmenter-BidLSTM_ChainCRF_FEATURES"
  "affiliation-address-BidLSTM_CRF_FEATURES:grobid-affiliation-address-BidLSTM_CRF_FEATURES"
  "citation-BidLSTM_CRF_FEATURES:grobid-citation-BidLSTM_CRF_FEATURES"
  "funding-acknowledgement-BidLSTM_CRF_FEATURES:grobid-funding-acknowledgement-BidLSTM_CRF_FEATURES"
)

echo "Installing PyTorch delft models (ref=${DELFT_REF}) into ${MODELS_DIR}"
mkdir -p "${MODELS_DIR}"

for entry in "${MODELS[@]}"; do
  local_name="${entry%%:*}"
  delft_name="${entry##*:}"
  dest="${MODELS_DIR}/${local_name}"
  echo "  -> ${delft_name}  =>  ${local_name}"
  mkdir -p "${dest}"
  for f in "${FILES[@]}"; do
    curl -fsSL --retry 3 -o "${dest}/${f}" "${BASE}/${delft_name}/${f}"
  done
  # sanity: weights must be a non-trivial PyTorch file
  if [ ! -s "${dest}/model_weights.pt" ]; then
    echo "ERROR: ${dest}/model_weights.pt missing or empty" >&2
    exit 1
  fi
  # drop the legacy Keras weights so nothing tries to load them
  rm -f "${dest}/model_weights.hdf5"
done

echo "Done. Installed ${#MODELS[@]} PyTorch models."
