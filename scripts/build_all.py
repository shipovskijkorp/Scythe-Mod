#!/usr/bin/env python3
"""Run independent family wrappers; the family list comes only from the matrix."""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import subprocess

from source_layout import ROOT, generation_config_paths


def main() -> int:
    families = generation_config_paths()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--family', action='append', choices=list(families))
    args, gradle_args = parser.parse_known_args()
    if gradle_args[:1] == ['--']:
        gradle_args = gradle_args[1:]
    gradle_args = gradle_args or ['buildAndCollect']
    if 'buildAndCollect' in gradle_args:
        shutil.rmtree(ROOT / 'build/release', ignore_errors=True)
    for family in args.family or families:
        directory = families[family].parent
        wrapper = directory / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
        command = ['cmd', '/c', str(wrapper)] if os.name == 'nt' else ['sh', str(wrapper)]
        print(f'==> ScytheMod {family}: {" ".join(gradle_args)}', flush=True)
        result = subprocess.run(command + gradle_args, cwd=directory, check=False)
        if result.returncode:
            return result.returncode
    for artifact in sorted((ROOT / 'build/release').glob('*.jar')):
        print(artifact)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
