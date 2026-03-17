#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PORT="${SERVER_PORT:-8081}"
HOST="${SERVER_HOST:-localhost}"
BASE="http://${HOST}:${PORT}"
if ! command -v curl &>/dev/null; then
    echo "ERROR: curl not found." >&2
    exit 1
fi
echo "Testing FeedServer at $BASE..."
R1=$(curl -s "${BASE}/feed?viewerId=viewer1")
if ! echo "$R1" | grep -q "OK: posts="; then
    echo "FAIL: GET /feed" >&2
    exit 1
fi
R2=$(curl -s "${BASE}/metrics")
if ! echo "$R2" | grep -q "feed_requests="; then
    echo "FAIL: GET /metrics" >&2
    exit 1
fi
echo "All tests passed."
