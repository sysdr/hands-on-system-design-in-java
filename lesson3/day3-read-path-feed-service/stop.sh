#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${SERVER_PORT:-8081}"
for pid in $(lsof -ti ":$PORT" 2>/dev/null); do
    echo "Stopping process $pid on port $PORT"
    kill "$pid" 2>/dev/null || true
done
echo "Done."
