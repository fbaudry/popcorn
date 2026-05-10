#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Library/Android/sdk" ]]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

if [[ -z "${ANDROID_SDK_ROOT:-}" && -n "${ANDROID_HOME:-}" ]]; then
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
fi

tasks=(
  ":app:testDebugUnitTest"
  ":app:lintDebug"
)

if [[ "${POPCORN_FULL_CHECK:-}" == "1" ]]; then
  tasks+=(":app:assembleDebug")
fi

./gradlew "${tasks[@]}"
