#!/usr/bin/env python3
from pathlib import Path
import sys
import source_layout

ROOT = Path(__file__).resolve().parents[1]

def fail(msg: str) -> None:
    print(f"ERROR: {msg}", file=sys.stderr)
    raise SystemExit(1)

props = source_layout.load_properties()
targets = source_layout.target_ids(props)
if not targets:
    fail('No configured targets')

for target in targets:
    layout = source_layout.target_layout(target, props)
    for layer in layout.layers:
        if not layer.is_dir():
            fail(f'{target}: missing source layer {layer.relative_to(ROOT)}')

# Loader imports are kept out of common/family source so future NeoForge/Forge
# ports do not inherit Fabric APIs from their generation layer.
family_roots = [ROOT / 'source-families' / family for family in props.get('sourceFamilies', '').split(',') if family]
for root in [ROOT / 'source-shared', *family_roots]:
    for path in root.rglob('*.java'):
        if 'net.fabricmc' in path.read_text('utf-8', errors='ignore'):
            fail(f'Fabric import escaped platform/target layer: {path.relative_to(ROOT)}')

# Reject redundant byte-identical overrides; they should be promoted upward.
for target in targets:
    layout = source_layout.target_layout(target, props)
    seen: dict[str, Path] = {}
    for layer in layout.layers:
        if not layer.exists():
            continue
        for path in layer.rglob('*'):
            if not path.is_file():
                continue
            logical = path.relative_to(layer).as_posix()
            previous = seen.get(logical)
            if previous is not None and previous.read_bytes() == path.read_bytes():
                fail(f'{target}: redundant identical override {logical}: '
                     f'{previous.relative_to(ROOT)} and {path.relative_to(ROOT)}')
            seen[logical] = path

source_layout.validate_all_directives(props)
print('Source layout OK: ' + ', '.join(targets))
