#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "${SCYTHE_PYTHON:-${PYTHON:-python3}}" "$ROOT/scripts/build_all.py" "$@"
