#!/usr/bin/env bash
set -euo pipefail

# Batch capture screenshots and short videos from all Booted iOS simulators.
#
# Usage:
#   ./scripts/capture_ios_media_batch.sh
#   ./scripts/capture_ios_media_batch.sh --seconds 12
#   ./scripts/capture_ios_media_batch.sh --out-dir /tmp/onepic_media
#   ./scripts/capture_ios_media_batch.sh --no-video
#   ./scripts/capture_ios_media_batch.sh --no-screenshot

SECONDS_TO_RECORD=8
OUT_DIR="$(pwd)/artifacts/ios_media"
DO_SCREENSHOT=1
DO_VIDEO=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    --seconds)
      SECONDS_TO_RECORD="$2"
      shift 2
      ;;
    --out-dir)
      OUT_DIR="$2"
      shift 2
      ;;
    --no-video)
      DO_VIDEO=0
      shift
      ;;
    --no-screenshot)
      DO_SCREENSHOT=0
      shift
      ;;
    -h|--help)
      sed -n '1,40p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      exit 1
      ;;
  esac
done

if [[ "$DO_SCREENSHOT" -eq 0 && "$DO_VIDEO" -eq 0 ]]; then
  echo "Both screenshot and video are disabled; nothing to do." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
TS="$(date +%Y%m%d_%H%M%S)"

BOOTED_LINES="$(xcrun simctl list devices | awk '/\(Booted\)/')"
if [[ -z "$BOOTED_LINES" ]]; then
  echo "No Booted simulators found."
  exit 1
fi

declare -a REC_PIDS=()
declare -a REC_FILES=()

echo "Output dir: $OUT_DIR"
BOOTED_COUNT="$(printf "%s\n" "$BOOTED_LINES" | sed '/^$/d' | wc -l | tr -d ' ')"
echo "Booted devices: ${BOOTED_COUNT}"

while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  udid="$(echo "$line" | awk -F '[()]' '{print $2}')"
  name="$(echo "$line" | sed -E 's/^[[:space:]]*//' | sed -E 's/[[:space:]]*\([0-9A-F-]+\)[[:space:]]*\(Booted\)[[:space:]]*$//')"
  safe_name="$(echo "$name" | tr ' /' '__' | tr -cd '[:alnum:]_.-')"

  if [[ "$DO_SCREENSHOT" -eq 1 ]]; then
    shot_file="$OUT_DIR/${TS}_${safe_name}_${udid}.png"
    echo "[screenshot] $name -> $(basename "$shot_file")"
    xcrun simctl io "$udid" screenshot "$shot_file" >/dev/null
  fi

  if [[ "$DO_VIDEO" -eq 1 ]]; then
    video_file="$OUT_DIR/${TS}_${safe_name}_${udid}.mov"
    echo "[record start] $name -> $(basename "$video_file")"
    xcrun simctl io "$udid" recordVideo --force "$video_file" >/dev/null 2>&1 &
    REC_PIDS+=("$!")
    REC_FILES+=("$video_file")
  fi

done <<< "$BOOTED_LINES"

if [[ "$DO_VIDEO" -eq 1 && ${#REC_PIDS[@]} -gt 0 ]]; then
  sleep "$SECONDS_TO_RECORD"
  echo "Stopping recordings after ${SECONDS_TO_RECORD}s..."
  for pid in "${REC_PIDS[@]}"; do
    kill -INT "$pid" >/dev/null 2>&1 || true
  done
  for pid in "${REC_PIDS[@]}"; do
    wait "$pid" || true
  done
fi

echo
echo "Done. Files:"
ls -1 "$OUT_DIR" | sed 's/^/  - /'
