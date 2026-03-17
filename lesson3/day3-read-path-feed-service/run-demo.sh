#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PORT="${SERVER_PORT:-8081}"
HOST="${SERVER_HOST:-localhost}"
METRICS_FILE="${SCRIPT_DIR}/data/dashboard_metrics.json"
BASE="http://${HOST}:${PORT}"
if ! command -v curl &>/dev/null; then
    echo "ERROR: curl not found." >&2
    exit 1
fi
mkdir -p "$(dirname "$METRICS_FILE")"
echo "Running demo against $BASE..."
curl -s -o /dev/null "${BASE}/feed?viewerId=viewer1"
curl -s -o /dev/null "${BASE}/feed?viewerId=viewer1"
curl -s -o /dev/null "${BASE}/feed?viewerId=viewer2"
R=$(curl -s "${BASE}/metrics")
# Parse metrics from last line (HTTP body)
BODY=$(echo "$R" | tail -1)
feed_requests=$(echo "$BODY" | sed -n 's/.*feed_requests=\([0-9]*\).*/\1/p')
cache_hits=$(echo "$BODY" | sed -n 's/.*cache_hits=\([0-9]*\).*/\1/p')
cache_misses=$(echo "$BODY" | sed -n 's/.*cache_misses=\([0-9]*\).*/\1/p')
feed_requests=${feed_requests:-0}
cache_hits=${cache_hits:-0}
cache_misses=${cache_misses:-0}
cat > "$METRICS_FILE" << MET
{
  "feed_requests": $feed_requests,
  "cache_hits": $cache_hits,
  "cache_misses": $cache_misses,
  "last_demo_run": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "server": "$HOST:$PORT",
  "status": "ok"
}
MET
echo "Dashboard metrics updated: $METRICS_FILE (feed_requests=$feed_requests, cache_hits=$cache_hits, cache_misses=$cache_misses)"
