#!/usr/bin/env bash
# Connect to the TV over ADB and wait until it is really there.
#
# Usage: tools/connect.sh [host]        (default: $TV_HOST)
#
# It never waits forever: the wait stops after 60 seconds and the script says what is wrong
# (no answer, refused, unauthorised, or more than one device). The TV has to have
# "ADB debugging" switched on in its developer options, and its IP can change when it
# reconnects to the network: find the new one under Settings - Network.
set -euo pipefail
cd "$(dirname "$0")/.."
HOST="${1:-${TV_HOST:-}}"
[ -n "$HOST" ] || { echo "usage: connect.sh <TV IP>   (or set TV_HOST)"; exit 2; }
PORT="${TV_PORT:-5555}"
SERIAL="$HOST:$PORT"
LIMIT=60          # seconds to wait in total
POLL=2            # seconds between tries
ADB="${ADB:-adb}"

command -v "$ADB" >/dev/null 2>&1 || { echo "adb not found: install the Android platform-tools"; exit 3; }

say() { printf '%s\n' "$*"; }

# Already connected? Then there is nothing to do.
if "$ADB" devices | grep -q "^${SERIAL}\s.*device$"; then
  say "connected: $SERIAL"
  exit 0
fi

# A device in another state: say what is wrong instead of waiting.
state="$(printf '%s\n' "$SERIAL" | "$ADB" get-state 2>/dev/null || true)"
case "$state" in
  unauthorized)
    say "$SERIAL is unauthorised: open the TV and accept the 'Allow USB debugging?' prompt."
    say "If no prompt appears, revoke the USB debugging authorisation in the TV's developer options and reconnect."
    exit 4 ;;
  offline)
    say "$SERIAL is offline: the TV is connected but not answering. Try again in a moment."
    exit 5 ;;
esac

say "connecting to $SERIAL ..."
start=$(date +%s)
tries=0
while :; do
  if "$ADB" connect "$SERIAL" >/dev/null 2>&1; then
    if "$ADB" devices | grep -q "^${SERIAL}\s.*device$"; then
      say "connected: $SERIAL"
      exit 0
    fi
  fi
  tries=$((tries + 1))
  now=$(date +%s)
  if [ $((now - start)) -ge "$LIMIT" ]; then
    say "gave up after ${LIMIT}s: $SERIAL did not come up."
    say "Check that the TV is on the same network, that ADB debugging is on, and that the IP is right."
    exit 6
  fi
  if [ $((tries % 5)) -eq 1 ]; then
    say "  still trying ($((now - start))s of ${LIMIT}s) ..."
  fi
  sleep "$POLL"
done
