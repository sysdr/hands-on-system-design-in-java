#!/bin/bash
# Start TweetServer. Use full path so it works from any cwd.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
JAR="${SCRIPT_DIR}/target/TwitterClone.jar"
PORT="${SERVER_PORT:-8080}"

# Use project-local JDK if java not in PATH
if ! command -v java &>/dev/null; then
    for d in "$SCRIPT_DIR"/.jdk/jdk-* "$SCRIPT_DIR/../.jdk"/jdk-*; do
        [[ -d "$d" && -x "$d/bin/java" ]] || continue
        export JAVA_HOME="$d"
        export PATH="$JAVA_HOME/bin:$PATH"
        break
    done
fi

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: JAR not found. Run ./build.sh first: $JAR" >&2
    exit 1
fi

# Check for existing process on port
if command -v ss &>/dev/null; then
    if ss -tlnp 2>/dev/null | grep -q ":$PORT "; then
        echo "WARNING: Port $PORT already in use. Stop existing server or set SERVER_PORT." >&2
        exit 1
    fi
fi

echo "Starting TweetServer from $JAR on port $PORT (cwd=$SCRIPT_DIR)"
exec java -jar "$JAR" "$PORT"
