#!/bin/bash
# Build TwitterClone JAR (independent of parent setup.sh). Run from TwitterClone directory.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PROJECT_NAME="TwitterClone"

# Find Java (SCRIPT_DIR or parent .jdk, or JAVA_HOME, or PATH)
find_java() {
    if command -v javac &>/dev/null; then return; fi
    if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
        export PATH="$JAVA_HOME/bin:$PATH"
        return
    fi
    for d in "$SCRIPT_DIR"/.jdk/jdk-* "$SCRIPT_DIR/../.jdk/jdk-*" /usr/lib/jvm/java-21-* /usr/lib/jvm/java-17-* /usr/lib/jvm/*; do
        [[ -d "$d" && -x "$d/bin/javac" ]] || continue
        export JAVA_HOME="$d"
        export PATH="$JAVA_HOME/bin:$PATH"
        return
    done
    echo "ERROR: JDK not found. Set JAVA_HOME or install OpenJDK 17+." >&2
    exit 1
}
find_java

mkdir -p target/classes
echo "Compiling..."
javac -d target/classes src/main/java/com/example/twitterclone/storage/*.java
echo "Main-Class: com.example.twitterclone.storage.TweetServer" > target/manifest.txt
jar -cvfm target/$PROJECT_NAME.jar target/manifest.txt -C target/classes .
echo "Built target/$PROJECT_NAME.jar"
