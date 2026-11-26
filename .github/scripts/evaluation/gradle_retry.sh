#!/usr/bin/env bash

# gradle_retry.sh
# Run ./gradlew with the given arguments, and run it again when it failed to download a
# dependency: Maven Central throttles the GitHub runners (HTTP 429, Too Many Requests) when
# many jobs resolve the build at the same time. Any other failure is returned at once.
#
# Usage:
#   gradle_retry.sh GRADLE_ARGUMENT...
#
# Environment:
#   GRADLE_RETRY_ATTEMPTS  runs at most (default 5)
#   GRADLE_RETRY_DELAY     seconds before the second run, doubled after each run (default 30)

set -uo pipefail

ATTEMPTS="${GRADLE_RETRY_ATTEMPTS:-5}"
DELAY="${GRADLE_RETRY_DELAY:-30}"

# what Gradle reports when a repository refuses or drops a request
DOWNLOAD_FAILURE='Could not (resolve|GET|HEAD|download|get resource) |status code (429|5[0-9][0-9]) from server|Too Many Requests|Read timed out|Connection (reset|refused|timed out)'

LOG="$(mktemp)"
trap 'rm -f "${LOG}"' EXIT

STATUS=1
for attempt in $(seq 1 "${ATTEMPTS}"); do
  ./gradlew "$@" 2>&1 | tee "${LOG}"
  STATUS=${PIPESTATUS[0]}
  [ "${STATUS}" -eq 0 ] && exit 0
  grep -qE "${DOWNLOAD_FAILURE}" "${LOG}" || exit "${STATUS}"
  [ "${attempt}" -eq "${ATTEMPTS}" ] && break
  echo "::warning::Gradle could not download a dependency (run ${attempt} of ${ATTEMPTS}), trying again in ${DELAY}s"
  sleep "${DELAY}"
  DELAY=$((DELAY * 2))
done

echo "::error::Gradle could not download its dependencies after ${ATTEMPTS} runs"
exit "${STATUS}"
