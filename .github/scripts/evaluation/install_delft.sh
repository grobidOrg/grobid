#!/usr/bin/env bash

# install_delft.sh
# Install DeLFT in a Python virtual environment, at the revision pinned in Dockerfile.delft
# so that the evaluation runs the same stack as the published image (on CPU).
#
# Usage:
#   install_delft.sh VENV_DIR             # DeLFT (with PyTorch) only, enough to preload embeddings
#   install_delft.sh VENV_DIR --with-jep  # also JEP, which needs a JDK (JAVA_HOME) to build

set -euo pipefail

VENV_DIR="${1:?usage: $0 VENV_DIR [--with-jep]}"
WITH_JEP="${2:-}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." >/dev/null 2>&1 && pwd)"
DOCKERFILE="${REPO_ROOT}/Dockerfile.delft"

# the DeLFT commit of the image: "ARG DELFT_REF=<sha>" in the Dockerfile
DELFT_REF="$(grep -oE '^ARG DELFT_REF=[0-9a-f]+' "${DOCKERFILE}" | head -n 1 | cut -d= -f2)"
if [ -z "${DELFT_REF}" ]; then
  echo "Error: no ARG DELFT_REF found in ${DOCKERFILE}" >&2
  exit 1
fi

# first "<package>==<version>" of the Dockerfile
pinned() {
  local requirement
  requirement="$(grep -oE "$1==[0-9][0-9.]*" "${DOCKERFILE}" | head -n 1)"
  if [ -z "${requirement}" ]; then
    echo "Error: no pinned version of $1 found in ${DOCKERFILE}" >&2
    exit 1
  fi
  echo "${requirement}"
}

[ -x "${VENV_DIR}/bin/python" ] || python3 -m venv "${VENV_DIR}"

# The CPU build of torch, from the PyTorch index: the default torch wheel of PyPI is a CUDA
# build on Linux, which pulls several GB of CUDA libraries the runner cannot use.
"${VENV_DIR}/bin/pip" install --no-cache-dir \
  "delft @ git+https://github.com/kermitt2/delft.git@${DELFT_REF}" \
  --extra-index-url https://download.pytorch.org/whl/cpu
"${VENV_DIR}/bin/python" -c 'import torch; assert torch.version.cuda is None, torch.__version__; print("torch", torch.__version__)'

if [ "${WITH_JEP}" = "--with-jep" ]; then
  # after DeLFT: JEP builds its numpy support against the numpy already installed
  "${VENV_DIR}/bin/pip" install --no-cache-dir "$(pinned jep)"
fi
