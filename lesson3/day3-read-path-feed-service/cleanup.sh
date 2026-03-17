#!/bin/bash
# Stop FeedServer and remove unused Docker resources for this project.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "--- Stopping process on port 8081 (FeedServer) ---"
pid=$(lsof -i :8081 -t 2>/dev/null || true)
if [[ -n "$pid" ]]; then
    echo "Stopping FeedServer (PID $pid)"
    kill -9 $pid 2>/dev/null || true
fi

echo "--- Stopping and removing feed-service Docker container ---"
if docker ps -a -q --filter "name=^feed-service-container$" 2>/dev/null | grep -q .; then
    docker stop feed-service-container 2>/dev/null || true
    docker rm feed-service-container 2>/dev/null || true
fi

echo "--- Removing feed-service Docker image ---"
docker images -q --filter "reference=*feed-service*" 2>/dev/null | xargs -r docker rmi -f 2>/dev/null || true

echo "--- Pruning unused Docker resources ---"
docker system prune -f

echo "--- Cleanup complete ---"
