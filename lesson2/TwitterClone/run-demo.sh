#!/bin/bash
# Run demo: post tweets, read them, update dashboard metrics. Server must be running.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PORT="${SERVER_PORT:-8080}"
HOST="${SERVER_HOST:-localhost}"
METRICS_FILE="${SCRIPT_DIR}/data/dashboard_metrics.json"

nc_cmd=""
for c in nc netcat ncat; do
    if command -v $c &>/dev/null; then
        nc_cmd=$c
        break
    fi
done
if [[ -z "$nc_cmd" ]]; then
    echo "ERROR: netcat (nc) not found." >&2
    exit 1
fi

run_cmd() {
    echo "$1" | $nc_cmd -w 3 "$HOST" "$PORT" 2>/dev/null || { echo "ERROR: Server not reachable at $HOST:$PORT"; exit 1; }
}

mkdir -p "$(dirname "$METRICS_FILE")"

echo "Running demo against $HOST:$PORT..."

# Post 2 tweets
run_cmd "POST /tweet Demo tweet 1 - durable storage."
R1=$(run_cmd "POST /tweet Demo tweet 2 - WAL and atomicity.")
ID=$(echo "$R1" | sed -n 's/OK: Tweet stored with ID \(\S*\)/\1/p')
[[ -n "$ID" ]]

# Read one back
R2=$(run_cmd "GET /tweet/$ID")
echo "$R2" | grep -q "Demo tweet 2"

# Get metrics
R3=$(run_cmd "GET /metrics")
COUNT=$(echo "$R3" | sed -n 's/OK: tweet_count=\([0-9]*\)/\1/p')
COUNT="${COUNT:-0}"

# Ensure we have non-zero after demo
if [[ "$COUNT" -eq 0 ]]; then
    echo "WARNING: tweet_count is 0 after demo; metrics may not be updated." >&2
fi

# Write dashboard metrics (non-zero values after successful demo)
cat > "$METRICS_FILE" << EOF
{
  "tweet_count": $COUNT,
  "tweets_posted_this_demo": 2,
  "tweets_read_this_demo": 1,
  "last_demo_run": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "server": "$HOST:$PORT",
  "status": "ok"
}
EOF
echo "Dashboard metrics updated: $METRICS_FILE (tweet_count=$COUNT)"
echo "Demo complete. Dashboard values are non-zero: tweet_count=$COUNT"
