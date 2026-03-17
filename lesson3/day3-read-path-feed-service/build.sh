#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
PACKAGE_DIR="com/example/feedservice"
find_java() {
    if command -v javac &>/dev/null; then return 0; fi
    [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/javac" ]] && { export PATH="${JAVA_HOME}/bin:$PATH"; return 0; }
    # sdkman (common on WSL/dev envs)
    if [[ -x "${HOME}/.sdkman/candidates/java/current/bin/javac" ]]; then
        export JAVA_HOME="${HOME}/.sdkman/candidates/java/current"
        export PATH="${JAVA_HOME}/bin:$PATH"
        return 0
    fi
    for d in "$SCRIPT_DIR"/.jdk/jdk-* "$SCRIPT_DIR/../.jdk"/jdk-* /usr/lib/jvm/java-21-* /usr/lib/jvm/java-17-* /opt/java/* /usr/java/*; do
        [[ -d "$d" && -x "$d/bin/javac" ]] && { export JAVA_HOME="$d"; export PATH="$JAVA_HOME/bin:$PATH"; return 0; }
    done
    return 1
}
JAR="target/feed-service-1.0-SNAPSHOT.jar"
if command -v mvn &>/dev/null; then
    mvn clean install -Dmaven.compiler.parameters=true --add-opens java.base/java.lang=ALL-UNNAMED -q
elif find_java; then
    rm -rf target
    mkdir -p target/classes
    javac --enable-preview --release 21 -d target/classes src/main/java/com/example/feedservice/*.java
    echo "Main-Class: com.example.feedservice.FeedServer" > target/manifest.txt
    jar cfm "$JAR" target/manifest.txt -C target/classes .
else
    echo "ERROR: No JDK (javac/jar) or Maven found." >&2
    echo "Install one of:" >&2
    echo "  - Maven + JDK (apt), OR" >&2
    echo "  - JDK 21 (sdkman), OR" >&2
    echo "  - set JAVA_HOME to a JDK install" >&2
    exit 1
fi
if [[ -f "$JAR" ]]; then
    echo "Built $JAR"
else
    echo "ERROR: Build finished but JAR missing: $JAR" >&2
    exit 1
fi
