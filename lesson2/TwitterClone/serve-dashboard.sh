#!/bin/bash
# Serve dashboard_out.html so you can open it in a browser via URL (handy on WSL).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PORT="${DASHBOARD_PORT:-8765}"
OUT="$SCRIPT_DIR/dashboard_out.html"

if [[ ! -f "$OUT" ]]; then
    echo "Run ./view-dashboard.sh first to generate dashboard_out.html" >&2
    exit 1
fi

echo "Dashboard served at:"
echo "  http://localhost:$PORT/dashboard_out.html"
echo ""
echo "Open that URL in your Windows browser. Press Ctrl+C to stop."
echo "---"
python3 -m http.server "$PORT" 2>/dev/null || python -m SimpleHTTPServer "$PORT" 2>/dev/null
