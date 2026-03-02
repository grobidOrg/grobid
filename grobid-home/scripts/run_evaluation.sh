#!/usr/bin/env bash

# run_evaluation.sh
# Parameterized evaluation runner for Grobid end-to-end evaluation (jatsEval)
# Usage examples:
#   sh run_evaluation.sh -d /abs/path/to/dataset_root -s master
#   sh run_evaluation.sh -d /data/grobid-eval -s master -r 0 -f 0.1 -o /tmp/reports -n

set -o pipefail

usage() {
    cat <<EOF
Usage: $0 -d EVAL_ROOT [-s REPORT_SUFFIX] [-r RUN] [-f FILERATIO] [-l FLAVOR] [-g GRADLEW] [-j JAVA_NATIVE_LIB] [-o OUT_DIR] [-p PATTERN] [-n]

Options:
  -d EVAL_ROOT        Root folder containing one subdirectory per article-dataset (required)
  -s REPORT_SUFFIX    Suffix to append to report files (default: master)
  -r RUN              Whether to execute Grobid on PDFs (1) or only evaluate existing TEI (0). Default: 1
  -f FILERATIO        Ratio of files to evaluate (0.0-1.0). Default: 1
  -l FLAVOR           Optional flavor parameter passed to Gradle (default: empty)
  -g GRADLEW_PATH     Path to gradlew executable (default: ./gradlew)
  -j JAVA_NATIVE_LIB  Path to lmdb native library (optional). If provided, sets JAVA_TOOL_OPTIONS accordingly.
  -o OUT_DIR          Directory where per-dataset reports will be written (default: current directory)
  -p PATTERN          Glob pattern for dataset directories inside EVAL_ROOT (default: '*')
  -n                  Dry-run: print commands but do not execute them
  -h                  Show this help

Example:
  $0 -d /data/grobid-eval -s master -r 1 -f 1 -o ./reports
EOF
}

# defaults
REPORT_SUFFIX="master"
RUN=1
FILERATIO=1.0
FLAVOR=""
GRADLEW_PATH="./gradlew"
OUT_DIR="."
PATTERN='*'
DRY_RUN=0
JAVA_NATIVE_LIB=""

# record where the user invoked the script from (so ./gradlew is picked from here)
START_PWD="$(pwd)"

# determine the script dir and repository root (repo root = parent of grobid-home)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." >/dev/null 2>&1 && pwd)"
# default absolute report source path (repo-root based)
REPORT_SRC_DEFAULT="${REPO_ROOT}/grobid-home/tmp/report.md"
# also consider start-pwd rooted path when gradle output differs by working dir
REPORT_SRC_STARTPWD="${START_PWD%/}/grobid-home/tmp/report.md"

# Pre-process long options into short equivalents so getopts doesn't choke on --dry-run
NEWARGS=()
for a in "$@"; do
  case "$a" in
    --dry-run) NEWARGS+=("-n") ;;
    *) NEWARGS+=("$a") ;;
  esac
done
set -- "${NEWARGS[@]}"

# parse short options; we'll handle long options (like --dry-run) after getopts
while getopts ":d:s:r:f:l:g:j:o:p:nh" opt; do
  case ${opt} in
    d ) EVAL_ROOT="$OPTARG" ;;
    s ) REPORT_SUFFIX="$OPTARG" ;;
    r ) RUN="$OPTARG" ;;
    f ) FILERATIO="$OPTARG" ;;
    l ) FLAVOR="$OPTARG" ;;
    g ) GRADLEW_PATH="$OPTARG" ;;
    j ) JAVA_NATIVE_LIB="$OPTARG" ;;
    o ) OUT_DIR="$OPTARG" ;;
    p ) PATTERN="$OPTARG" ;;
    n ) DRY_RUN=1 ;; # legacy short flag and mapped --dry-run
    h ) usage; exit 0 ;;
    \? ) echo "Invalid option: -$OPTARG" >&2; usage; exit 2 ;;
    : ) echo "Missing option argument for -$OPTARG" >&2; usage; exit 2 ;;
  esac
done
shift $((OPTIND -1))

# support long option --dry-run (user-friendly); scan any remaining args (keeps backward compatibility)
for arg in "$@"; do
  if [ "${arg}" = "--dry-run" ]; then
    DRY_RUN=1
  fi
done

# required
if [ -z "${EVAL_ROOT}" ]; then
  echo "Error: evaluation root folder must be provided with -d" >&2
  usage
  exit 2
fi

# normalize paths
EVAL_ROOT=$(cd "${EVAL_ROOT}" 2>/dev/null && pwd) || { echo "Cannot access EVAL_ROOT=${EVAL_ROOT}" >&2; exit 2; }
OUT_DIR=$(mkdir -p "${OUT_DIR}" && cd "${OUT_DIR}" 2>/dev/null && pwd) || { echo "Cannot create/access OUT_DIR=${OUT_DIR}" >&2; exit 2; }

# Resolve GRADLEW_PATH so that relative paths are resolved against the directory where the user invoked the script (START_PWD)
if [[ "${GRADLEW_PATH}" != /* ]]; then
  GRADLEW_PATH="${START_PWD%/}/${GRADLEW_PATH}"
fi
# normalize path (if possible)
if [ -e "${GRADLEW_PATH}" ]; then
  GRADLEW_PATH=$(cd "$(dirname "${GRADLEW_PATH}")" 2>/dev/null && pwd)/$(basename "${GRADLEW_PATH}")
fi

# ensure gradlew exists: prefer resolved path, then try START_PWD/gradlew, then REPO_ROOT/gradlew
if [ ! -x "${GRADLEW_PATH}" ]; then
  echo "Warning: gradlew not executable at ${GRADLEW_PATH}. Trying ${START_PWD}/gradlew and ${REPO_ROOT}/gradlew" >&2
  if [ -x "${START_PWD}/gradlew" ]; then
    GRADLEW_PATH="${START_PWD}/gradlew"
  elif [ -x "${REPO_ROOT}/gradlew" ]; then
    GRADLEW_PATH="${REPO_ROOT}/gradlew"
  else
    echo "Error: gradlew not found or not executable. Provide with -g" >&2
    exit 2
  fi
fi

# prepare JAVA_TOOL_OPTIONS if requested (do not overwrite if already set externally)
if [ -n "${JAVA_NATIVE_LIB}" ]; then
  if [ -z "${JAVA_TOOL_OPTIONS}" ]; then
    export JAVA_TOOL_OPTIONS="-Dlmdbjava.native.lib=${JAVA_NATIVE_LIB}"
  else
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Dlmdbjava.native.lib=${JAVA_NATIVE_LIB}"
  fi
fi

# Set LD_LIBRARY_PATH to include Python environment libraries (e.g., libstdc++ for DeLFT/JEP)
PYTHON_ENV_PREFIX="${CONDA_PREFIX:-${VIRTUAL_ENV:-}}"
if [ -n "${PYTHON_ENV_PREFIX}" ] && [ -d "${PYTHON_ENV_PREFIX}/lib" ]; then
  export LD_LIBRARY_PATH="${PYTHON_ENV_PREFIX}/lib${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"
fi

echo "Evaluation root: ${EVAL_ROOT}"
echo "Output dir: ${OUT_DIR}"
echo "Gradle wrapper: ${GRADLEW_PATH}"
echo "run=${RUN}, fileRatio=${FILERATIO}, flavor='${FLAVOR}', pattern='${PATTERN}', report_suffix=${REPORT_SUFFIX}"
[ ${DRY_RUN} -eq 1 ] && echo "DRY RUN: commands will not be executed"

overall_status=0

# iterate over matching subdirectories (non-recursive)
shopt_cmd=""
case "$(basename "$SHELL")" in
  zsh)
    # zsh: use a for loop with globbing
    ;;
  bash)
    # bash supports nullglob if enabled, but keep POSIX fallback
    shopt -s nullglob 2>/dev/null || true
    shopt_cmd="shopt -s nullglob"
    ;;
  *)
    ;;
esac

# Build an array of dataset directories matching the pattern
cd "${EVAL_ROOT}" || { echo "Failed to cd into ${EVAL_ROOT}" >&2; exit 2; }

# Expand pattern safely
datasets=( )
for entry in ${PATTERN}; do
  if [ -d "${entry}" ]; then
    datasets+=("${EVAL_ROOT}/${entry}")
  fi
done

if [ ${#datasets[@]} -eq 0 ]; then
  echo "No datasets found matching pattern '${PATTERN}' in ${EVAL_ROOT}" >&2
  exit 3
fi

for ds in "${datasets[@]}"; do
  ds_basename=$(basename "${ds}")
  echo "===== Evaluation of ${ds_basename} (${ds}) ====="

  cmd=("${GRADLEW_PATH}" jatsEval -Pp2t="${ds}" -Prun="${RUN}" -PfileRatio="${FILERATIO}")
  if [ -n "${FLAVOR}" ]; then
    cmd+=("-Pflavor=${FLAVOR}")
  fi

  log_dst="${OUT_DIR}/report-${ds_basename}-${REPORT_SUFFIX}.txt"

  if [ ${DRY_RUN} -eq 1 ]; then
    echo "DRY: ${cmd[*]}"
    echo "DRY: would write log to ${log_dst}"
  else
    # execute gradlew from the directory where the script was invoked (START_PWD)
    (cd "${START_PWD}" && "${cmd[@]}") 2>&1 | tee "${log_dst}"
    exit_code=${PIPESTATUS[0]}

    if [ $exit_code -ne 0 ]; then
      echo "Gradle jatsEval failed for ${ds_basename} with exit code ${exit_code}" >&2
      overall_status=1
      # continue with other datasets
    else
      # use repo-root absolute report source by default to avoid ambiguity
      report_src="${REPORT_SRC_DEFAULT}"
      report_dst="${OUT_DIR}/report-${ds_basename}-${REPORT_SUFFIX}.md"

      # fall back to start-pwd based report path if needed
      if [ ! -f "${report_src}" ] && [ -f "${REPORT_SRC_STARTPWD}" ]; then
        report_src="${REPORT_SRC_STARTPWD}"
      fi

      if [ -f "${report_src}" ]; then
        mv "${report_src}" "${report_dst}" || { echo "Failed to move report to ${report_dst}" >&2; overall_status=1; }
        echo "Report saved to ${report_dst}"
      else
        echo "Warning: report not found at ${REPORT_SRC_DEFAULT} or ${REPORT_SRC_STARTPWD} after evaluation of ${ds_basename}" >&2
        overall_status=1
      fi
    fi
  fi

  echo "===== End evaluation of ${ds_basename} ====="
done

if [ ${DRY_RUN} -eq 1 ]; then
  echo "Dry-run complete. No commands executed."
  exit 0
fi

if [ ${overall_status} -eq 0 ]; then
  echo "All evaluations completed successfully."
else
  echo "One or more evaluations failed. Check output above." >&2
fi

exit ${overall_status}
