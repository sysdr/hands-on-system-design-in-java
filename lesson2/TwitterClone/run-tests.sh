#!/bin/bash
# Run functional tests against TweetServer. Server must be running.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PORT="${SERVER_PORT:-8080}"
HOST="${SERVER_HOST:-localhost}"

nc_cmd=""
for c in nc netcat ncat; do
    if command -v $c &>/dev/null; then
        nc_cmd=$c
        break
    fi
done
if [[ -z "$nc_cmd" ]]; then
    echo "ERROR: netcat (nc) not found. Install netcat-openbsd or similar." >&2
    exit 1
fi

run_cmd() {
    echo "$1" | $nc_cmd -w 3 "$HOST" "$PORT" 2>/dev/null || { echo "ERROR: Server not reachable at $HOST:$PORT"; exit 1; }
}

echo "Testing TweetServer at $HOST:$PORT..."

# Post tweet
R1=$(run_cmd "POST /tweet Test tweet from run-tests.sh")
echo "POST response: $R1"
if ! echo "$R1" | grep -q "OK: Tweet stored with ID"; then
    echo "FAIL: POST /tweet" >&2
    exit 1
fi
ID=$(echo "$R1" | sed -n 's/OK: Tweet stored with ID \(\S*\)/\1/p')

# Get tweet
R2=$(run_cmd "GET /tweet/$ID")
echo "GET response: $R2"
if ! echo "$R2" | grep -q "Test tweet from run-tests"; then
    echo "FAIL: GET /tweet/$ID" >&2
    exit 1
fi

# Metrics
R3=$(run_cmd "GET /metrics")
echo "GET /metrics: $R3"
if ! echo "$R3" | grep -q "tweet_count="; then
    echo "FAIL: GET /metrics" >&2
    exit 1
fi

echo "All tests passed."
