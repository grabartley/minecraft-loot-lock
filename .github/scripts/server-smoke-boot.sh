#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

RUN_DIR=run
BOOT_TIMEOUT="${BOOT_TIMEOUT:-300}"
STOP_TIMEOUT="${STOP_TIMEOUT:-60}"
LOG="$(mktemp)"
FIFO="$(mktemp -u)"
OUT_FIFO="$(mktemp -u)"
SERVER_PID=
TEE_PID=

kill_tree() {
  local child
  for child in $(pgrep -P "$1" 2>/dev/null); do
    kill_tree "$child"
  done
  kill "$1" 2>/dev/null || true
}

cleanup() {
  if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    kill_tree "$SERVER_PID"
  fi
  if [[ -n "$TEE_PID" ]]; then
    kill "$TEE_PID" 2>/dev/null || true
  fi
  rm -f "$FIFO" "$OUT_FIFO" "$LOG"
}
trap cleanup EXIT

drain_log() {
  local deadline=$((SECONDS + STOP_TIMEOUT))
  while kill -0 "$TEE_PID" 2>/dev/null; do
    if (( SECONDS >= deadline )); then
      echo "::error::Server output was still open ${STOP_TIMEOUT}s after the server exited"
      exit 1
    fi
    sleep 1
  done
  TEE_PID=
}

mkdir -p "$RUN_DIR"
echo "eula=true" > "$RUN_DIR/eula.txt"

mkfifo "$FIFO" "$OUT_FIFO"
tee "$LOG" < "$OUT_FIFO" &
TEE_PID=$!
./gradlew runServer --no-daemon --stacktrace --args="nogui" < "$FIFO" > "$OUT_FIFO" 2>&1 &
SERVER_PID=$!
exec 3> "$FIFO"

deadline=$((SECONDS + BOOT_TIMEOUT))
until grep -q 'Done (' "$LOG"; do
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    drain_log
    if grep -q 'Done (' "$LOG"; then
      echo "::error::Server exited on its own right after reaching Done"
    else
      echo "::error::Server exited before reaching Done"
    fi
    exit 1
  fi
  if (( SECONDS >= deadline )); then
    echo "::error::Server did not reach Done within ${BOOT_TIMEOUT}s"
    exit 1
  fi
  sleep 1
done

echo "stop" >&3
exec 3>&-

deadline=$((SECONDS + STOP_TIMEOUT))
while kill -0 "$SERVER_PID" 2>/dev/null; do
  if (( SECONDS >= deadline )); then
    echo "::error::Server did not exit within ${STOP_TIMEOUT}s after stop"
    exit 1
  fi
  sleep 1
done

status=0
wait "$SERVER_PID" || status=$?
if (( status != 0 )); then
  echo "::error::Server exited with status ${status} after stop"
  exit "$status"
fi

drain_log

for marker in '(loot-lock) Loot Lock initialized (world: ' 'All dimensions are saved'; do
  if ! grep -qF "$marker" "$LOG"; then
    echo "::error::Server log is missing '${marker}'"
    exit 1
  fi
done

echo "Server reached Done and stopped cleanly"
