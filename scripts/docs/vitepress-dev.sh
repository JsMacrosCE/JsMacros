#!/usr/bin/env bash
set -euo pipefail

WATCH_DIRS=(
  "./buildSrc"
  "./common"
  "./fabric"
  "./neoforge"
)

WORKING_DIR="./working/vitepress"
CONTENT_DIR="$WORKING_DIR/content"
DEBOUNCE_SECONDS=2

DEV_PID=""
BUILD_RUNNING=0
BUILD_PENDING=0

initial_build() {
  echo "==> Initial build"
  ./gradlew copyVitepressDoc

  mkdir -p "$WORKING_DIR"
  rm -rf "$WORKING_DIR"/*
  cp -R build/docs/vitepress/. "$WORKING_DIR"/

  cd "$WORKING_DIR"

  echo "==> Installing pnpm deps"
  pnpm i

  echo "==> Starting VitePress dev server"
  pnpm run dev &
  DEV_PID=$!

  cd - >/dev/null
}

incremental_build() {
  echo "==> Change detected, regenerating content"
  ./gradlew generateVitepressDoc

  rm -rf "$CONTENT_DIR"
  mkdir -p "$CONTENT_DIR"
  cp -R build/docs/vitepress/content/. "$CONTENT_DIR"/

  echo "==> Content updated"
}

run_build_loop() {
  if [[ "$BUILD_RUNNING" -eq 1 ]]; then
    BUILD_PENDING=1
    return
  fi

  BUILD_RUNNING=1
  while :; do
    BUILD_PENDING=0
    incremental_build
    [[ "$BUILD_PENDING" -eq 1 ]] || break
    echo "==> More changes arrived during build, running again"
  done
  BUILD_RUNNING=0
}

cleanup() {
  echo
  echo "==> Shutting down"
  if [[ -n "${DEV_PID:-}" ]] && kill -0 "$DEV_PID" 2>/dev/null; then
    kill "$DEV_PID" 2>/dev/null || true
    wait "$DEV_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

main() {
  require_cmd inotifywait
  require_cmd pnpm

  initial_build

  echo "==> Watching for changes in: ${WATCH_DIRS[*]}"
  echo "==> Debounce window: ${DEBOUNCE_SECONDS}s"

  while :; do
    # Wait for the first event
    inotifywait -qq -r -e modify,create,delete,move "${WATCH_DIRS[@]}"

    echo "==> Changes detected, waiting for quiet period..."

    # Keep extending the quiet window while more events arrive
    while inotifywait -qq -r -e modify,create,delete,move -t "$DEBOUNCE_SECONDS" "${WATCH_DIRS[@]}"; do
      :
    done

    run_build_loop
  done
}

main "$@"
