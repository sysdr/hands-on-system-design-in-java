#!/bin/bash
# Stop TweetServer, dashboard server, and remove Docker containers/images for this lesson.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "--- Stopping TweetServer (port 8080) ---"
pid=$(lsof -i :8080 -t 2>/dev/null || true)
if [[ -n "$pid" ]]; then
    kill -9 $pid 2>/dev/null || true
    echo "Stopped process $pid"
else
    echo "Nothing listening on 8080"
fi

echo "--- Stopping dashboard server (port 8765) ---"
pid=$(lsof -i :8765 -t 2>/dev/null || true)
if [[ -n "$pid" ]]; then
    kill -9 $pid 2>/dev/null || true
    echo "Stopped process $pid"
else
    echo "Nothing listening on 8765"
fi

echo "--- Stopping and removing Docker container (twitterclone-container) ---"
if docker ps -a -q --filter "name=twitterclone-container" 2>/dev/null | grep -q .; then
    docker stop twitterclone-container 2>/dev/null || true
    docker rm twitterclone-container 2>/dev/null || true
    echo "Removed twitterclone-container"
else
    echo "No twitterclone-container found"
fi

echo "--- Removing Docker image (twitterclone-image) ---"
if docker images -q twitterclone-image 2>/dev/null | grep -q .; then
    docker rmi twitterclone-image 2>/dev/null || true
    echo "Removed twitterclone-image"
else
    echo "No twitterclone-image found"
fi

echo "--- Optional: prune unused Docker resources (run 'docker system prune -af' to remove all unused images/volumes) ---"
docker system prune -f

echo "--- Cleanup complete ---"
