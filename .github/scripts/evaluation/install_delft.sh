#!/usr/bin/env bash

# install_delft.sh
# Install DeLFT in a Python virtual environment, at the versions pinned in Dockerfile.delft
# so that the evaluation runs the same stack as the published image (on CPU).
#
# Usage:
#   install_delft.sh VENV_DIR             # TensorFlow and DeLFT only (enough to preload embeddings)
#   install_delft.sh VENV_DIR --with-jep  # also JEP, which needs a JDK (JAVA_HOME) to build

set -euo pipefail

VENV_DIR="${1:?usage: $0 VENV_DIR [--with-jep]}"
WITH_JEP="${2:-}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." >/dev/null 2>&1 && pwd)"
DOCKERFILE="${REPO_ROOT}/Dockerfile.delft"

# first "<package>==<version>" of the Dockerfile; for tensorflow this is the CPU-only
# build, the GPU one being spelled "tensorflow[and-cuda]=="
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

"${VENV_DIR}/bin/pip" install --no-cache-dir "$(pinned tensorflow)"
"${VENV_DIR}/bin/pip" install --no-cache-dir "$(pinned delft)"

if [ "${WITH_JEP}" = "--with-jep" ]; then
  # after DeLFT: JEP builds its numpy support against the numpy already installed
  "${VENV_DIR}/bin/pip" install --no-cache-dir "$(pinned jep)"
fi
