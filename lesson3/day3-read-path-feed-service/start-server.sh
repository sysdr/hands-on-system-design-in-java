#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
JAR="${SCRIPT_DIR}/target/feed-service-1.0-SNAPSHOT.jar"
PORT="${SERVER_PORT:-8081}"
JAVA_BIN="${JAVA_BIN:-java}"

# Use project .jdk if java not in PATH (same as build.sh)
if ! command -v "$JAVA_BIN" &>/dev/null; then
    for d in "$SCRIPT_DIR"/.jdk/jdk-* "$SCRIPT_DIR/../.jdk"/jdk-* "${HOME}/.sdkman/candidates/java/current"; do
        [[ -d "$d" && -x "$d/bin/java" ]] && { export JAVA_HOME="$d"; JAVA_BIN="$d/bin/java"; break; }
    done
fi
if [[ "$JAVA_BIN" != */* ]] && ! command -v "$JAVA_BIN" &>/dev/null; then
    echo "ERROR: java not found. Install JDK 17+ or run from lesson3 (uses .jdk if present)." >&2
    exit 1
fi
if [[ "$JAVA_BIN" == */* ]] && [[ ! -x "$JAVA_BIN" ]]; then
    echo "ERROR: JDK binary not executable: $JAVA_BIN" >&2
    exit 1
fi

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: JAR not found. Run setup.sh from lesson3 first: $JAR" >&2
    exit 1
fi
if command -v ss &>/dev/null; then
    if ss -tlnp 2>/dev/null | grep -q ":$PORT "; then
        echo "WARNING: Port $PORT already in use. Stop existing server or set SERVER_PORT." >&2
        exit 1
    fi
fi
echo "Starting FeedServer from $JAR on port $PORT (cwd=$SCRIPT_DIR)"
exec "$JAVA_BIN" --enable-preview -jar "$JAR" "$PORT"
