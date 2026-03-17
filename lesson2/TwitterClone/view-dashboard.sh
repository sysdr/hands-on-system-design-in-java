#!/bin/bash
# Generate dashboard with current metrics and open it. Run run-demo.sh first for non-zero values.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
METRICS_FILE="${SCRIPT_DIR}/data/dashboard_metrics.json"
DASH_HTML="${SCRIPT_DIR}/dashboard.html"
OUT="${SCRIPT_DIR}/dashboard_out.html"

if [[ ! -f "$METRICS_FILE" ]]; then
    echo "No metrics file yet. Run run-demo.sh first (with server running)." >&2
    echo "Creating placeholder metrics for dashboard..."
    mkdir -p "$(dirname "$METRICS_FILE")"
    echo '{"tweet_count":0,"tweets_posted_this_demo":0,"tweets_read_this_demo":0,"last_demo_run":null,"server":"not run","status":"no_demo"}' > "$METRICS_FILE"
fi

# Embed JSON into dashboard (Python for safe JSON embedding)
if command -v python3 &>/dev/null; then
    python3 - "$SCRIPT_DIR" << 'PY'
import json, sys, os
p = sys.argv[1]
with open(os.path.join(p, 'data', 'dashboard_metrics.json')) as f:
    j = json.load(f)
with open(os.path.join(p, 'dashboard.html')) as f:
    h = f.read()
h = h.replace('DASHBOARD_METRICS_JSON', json.dumps(j))
with open(os.path.join(p, 'dashboard_out.html'), 'w') as f:
    f.write(h)
PY
fi
if [[ -f "$OUT" ]]; then
    echo "Dashboard written to $OUT"
    if command -v xdg-open &>/dev/null; then xdg-open "$OUT"; elif command -v open &>/dev/null; then open "$OUT"; else echo "Open: $OUT"; fi
else
    echo "Could not generate dashboard. Open $DASH_HTML and ensure run-demo.sh has been run."
fi
