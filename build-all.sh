#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
rm -rf "$ROOT/build/release"
mkdir -p "$ROOT/build/release"

for family in legacy modern current; do
  echo "==> Building Scythe Mod family: $family"
  (cd "$ROOT/builds/$family" && ./gradlew buildAndCollect)
done

echo "==> Release jars"
find "$ROOT/build/release" -maxdepth 1 -type f -name '*.jar' -print
