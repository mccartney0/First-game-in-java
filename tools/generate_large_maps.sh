#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

./gradlew classes --no-daemon >/dev/null
mkdir -p build/classes/java/main
javac -encoding UTF-8 -cp build/classes/java/main -d build/classes/java/main tools/GenerateLargeRpgMaps.java
java -Djava.awt.headless=true -cp "build/classes/java/main:res" GenerateLargeRpgMaps "$@"
