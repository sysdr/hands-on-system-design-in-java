#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
METRICS_FILE="${SCRIPT_DIR}/data/dashboard_metrics.json"
DASH_HTML="${SCRIPT_DIR}/dashboard.html"
OUT="${SCRIPT_DIR}/dashboard_out.html"
if [[ ! -f "$METRICS_FILE" ]]; then
    echo "No metrics file yet. Run run-demo.sh first (with server running)." >&2
    mkdir -p "$(dirname "$METRICS_FILE")"
    echo '{"feed_requests":0,"cache_hits":0,"cache_misses":0,"last_demo_run":null,"server":"not run","status":"no_demo"}' > "$METRICS_FILE"
fi
if command -v python3 &>/dev/null; then
    python3 - "$SCRIPT_DIR" << 'PY'
import json, sys, os
p = sys.argv[1]
with open(os.path.join(p, "data", "dashboard_metrics.json")) as f:
    j = json.load(f)
with open(os.path.join(p, "dashboard.html")) as f:
    h = f.read()
h = h.replace("DASHBOARD_METRICS_JSON", json.dumps(j))
with open(os.path.join(p, "dashboard_out.html"), "w") as f:
    f.write(h)
PY
fi
if [[ -f "$OUT" ]]; then
    echo "Dashboard written to $OUT"
else
    echo "Could not generate dashboard. Ensure run-demo.sh has been run."
fi
