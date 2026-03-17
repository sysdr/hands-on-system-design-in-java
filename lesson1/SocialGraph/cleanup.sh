#!/bin/bash
# Cleanup Docker containers and unused resources for this environment.
set -euo pipefail

echo "--- Stopping all running containers (if any) ---"
docker stop $(docker ps -aq 2>/dev/null) 2>/dev/null || true

echo "--- Pruning unused Docker resources (images, containers, networks, volumes, cache) ---"
docker system prune -af --volumes

echo "--- Docker cleanup complete ---"
