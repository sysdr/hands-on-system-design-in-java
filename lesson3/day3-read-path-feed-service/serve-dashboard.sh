#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PORT="${DASHBOARD_PORT:-8765}"
OUT="${SCRIPT_DIR}/dashboard_out.html"
if [[ ! -f "$OUT" ]]; then
    echo "Run ./view-dashboard.sh first to generate dashboard_out.html" >&2
    exit 1
fi
echo "Dashboard served at http://localhost:$PORT/dashboard_out.html"
python3 -m http.server "$PORT" 2>/dev/null || python -m SimpleHTTPServer "$PORT" 2>/dev/null
