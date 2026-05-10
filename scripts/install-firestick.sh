#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${FIRESTICK_IP:-}" ]]; then
  echo "Set FIRESTICK_IP first, for example: export FIRESTICK_IP=192.168.1.50" >&2
  exit 1
fi

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
ADB_BIN="${ADB_BIN:-adb}"

if ! command -v "$ADB_BIN" >/dev/null 2>&1; then
  DEFAULT_ADB="$HOME/Library/Android/sdk/platform-tools/adb"
  if [[ -x "$DEFAULT_ADB" ]]; then
    ADB_BIN="$DEFAULT_ADB"
  else
    echo "adb not found. Install Android SDK Platform-Tools or set ADB_BIN=/path/to/adb." >&2
    exit 1
  fi
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found at $APK_PATH. Run ./gradlew :app:assembleDebug first." >&2
  exit 1
fi

"$ADB_BIN" connect "$FIRESTICK_IP:5555"
"$ADB_BIN" install -r "$APK_PATH"
