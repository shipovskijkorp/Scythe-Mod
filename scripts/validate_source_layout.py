#!/usr/bin/env python3
"""Enforce source ownership, balanced resources, and nonduplicated target overlays."""
from __future__ import annotations

import argparse
import collections
import json
import re
import tempfile
from pathlib import Path

from source_layout import (ROOT, load_properties, target_ids, target_layout,
                           family_targets, is_lang_path, validate_all_directives,
                           materialize_target)
from resource_values import read_balance
from sync_idea import sync

BALANCE_PATH = 'src/main/java/com/shipovskijkorp/scythes/mod/balance/ScytheBalance.java'
LOADER_IMPORT = re.compile(r'\b(?:net\.fabricmc|net\.minecraftforge|net\.neoforged)\.')


def validate() -> list[str]:
    props = load_properties()
    targets = target_ids(props)
    problems = []
    layouts = {t: target_layout(t, props) for t in targets}
    maintained = sorted(set(layer for layout in layouts.values() for layer in layout.layers))
    for layer in maintained:
        if not layer.is_dir():
            problems.append(f'Missing source layer: {layer.relative_to(ROOT)}')
    shared = ROOT / 'source-shared'
    families = [ROOT / 'source-families' / family for family in family_targets(props)]
    for root in [shared, *families]:
        for source in root.rglob('*.java'):
            text = source.read_text(encoding='utf-8')
            if LOADER_IMPORT.search(text) or re.search(r'\.platform\.(?:fabric|forge|neoforge)\.', text):
                problems.append(f'Loader API escaped into common code: {source.relative_to(ROOT)}')
            if any(part in source.parts for part in ('ability', 'effect', 'entity', 'item')):
                if re.search(r'import .*\.network\.(?:ModPackets|.*Packet);', text):
                    problems.append(f'Gameplay depends on a packet implementation: {source.relative_to(ROOT)}')
    copies = [p for layer in maintained for p in layer.rglob('ScytheBalance.java')]
    if copies != [shared / BALANCE_PATH]:
        problems.append('ScytheBalance.java must exist only in source-shared')
    balance = read_balance(shared / BALANCE_PATH)
    if balance['Base.DURABILITY'] <= 0 or balance['Base.MAX_STACK_SIZE'] != 1:
        problems.append('Scythes must be damageable, nonstackable items')
    for key, value in balance.items():
        if key.endswith('_CHANCE') and not 0 <= value <= 1:
            problems.append(f'Chance outside [0, 1]: {key}')
        if key.endswith(('_TICKS', '_COST', '_RADIUS')) and value < 0:
            problems.append(f'Negative duration/cost/radius: {key}')
    # 1.20.1 derives anvil cost from enum rarity. Reject silent version drift.
    rarity_cost = {10: 1, 5: 2, 2: 4, 1: 8}
    for enchantment in ('ACIDITY', 'ADDITIONAL_SLOT', 'SOUL_SIPHON', 'SPIKED_BLADE'):
        weight = balance[f'Enchantments.{enchantment}_WEIGHT']
        if weight not in rarity_cost or balance[f'Enchantments.{enchantment}_ANVIL_COST'] != rarity_cost[weight]:
            problems.append(f'{enchantment}: weight/anvil cost must agree with 1.20.1 enum rarity (10:1, 5:2, 2:4, 1:8)')

    for target, layout in layouts.items():
        for relative in layout.effective_files():
            candidates = layout.source_candidates(relative)
            if is_lang_path(relative):
                continue  # These are intentional key-wise deltas, not file replacement.
            for lower, upper in zip(candidates, candidates[1:]):
                if lower.read_bytes() == upper.read_bytes():
                    problems.append(f'{target}: redundant identical override: {upper.relative_to(ROOT)}')
    sibling_groups = dict(family_targets(props))
    for target, layout in layouts.items():
        sibling_groups.setdefault(f'{layout.family}/{layout.platform}', []).append(target)
    for family, siblings in sibling_groups.items():
        if len(siblings) < 2:
            continue
        paths = set.intersection(*(set(layouts[t].effective_files()) for t in siblings))
        for relative in paths:
            physical = [layouts[t].overlay_root / relative for t in siblings]
            if all(p.is_file() for p in physical) and len({p.read_bytes() for p in physical}) == 1:
                problems.append(f'Identical sibling overrides in {family}: {relative}; promote to family or family-platform')

    validate_all_directives(props)
    with tempfile.TemporaryDirectory(prefix='scythe-layout-check-') as temporary:
        for target in targets:
            layout = layouts[target]
            output = materialize_target(target, Path(temporary) / target, props)
            java = list(output.rglob('*.java'))
            own_classes = {re.search(r'package ([^;]+);', p.read_text(encoding="utf-8")).group(1) + '.' + p.stem for p in java}
            for source in java:
                text = source.read_text(encoding="utf-8")
                for match in re.finditer(r'ScytheBalance\.([A-Za-z]+\.[A-Z][A-Z_0-9]*)', text):
                    if match[1] not in balance:
                        problems.append(f'{target}: unknown balance field {match[1]} in {source.name}')
                for match in re.finditer(r'(?m)^import (?:static )?(com\.shipovskijkorp\.scythes\.mod\.[^;*]+);', text):
                    name = match[1]
                    if not any(name == cls or name.startswith(cls + '.') for cls in own_classes):
                        problems.append(f'{target}: unresolved project import {name} in {source.name}')
            for source in output.rglob('*.json'):
                data = json.loads(source.read_text(encoding="utf-8"))
                if '${balance:' in source.read_text(encoding="utf-8"):
                    problems.append(f'{target}: unresolved resource balance placeholder in {source.name}')
            if layout.platform == 'fabric':
                metadata = json.loads((output / 'src/main/resources/fabric.mod.json').read_text(encoding="utf-8"))
                if metadata['version'] != props[f'target.{target}.artifact.version']:
                    problems.append(f'{target}: generated metadata version mismatch')
                for names in metadata['entrypoints'].values():
                    for name in names:
                        if name not in own_classes:
                            problems.append(f'{target}: missing entrypoint class {name}')
            elif layout.platform == 'forge':
                metadata_path = output / 'src/main/resources/META-INF/mods.toml'
                if not metadata_path.is_file():
                    problems.append(f'{target}: missing META-INF/mods.toml')
                else:
                    metadata = metadata_path.read_text(encoding="utf-8")
                    expected_version = props[f'target.{target}.artifact.version']
                    if f'version="{expected_version}"' not in metadata:
                        problems.append(f'{target}: generated metadata version mismatch')
                    if 'modId="scythes"' not in metadata:
                        problems.append(f'{target}: Forge metadata has the wrong modId')
                entrypoint = output / 'src/main/java/com/shipovskijkorp/scythes/mod/ScytheMod.java'
                if not entrypoint.is_file() or '@Mod(ScytheMod.MOD_ID)' not in entrypoint.read_text(encoding="utf-8"):
                    problems.append(f'{target}: missing Forge @Mod entrypoint')
    problems.extend(sync(check=True))
    return problems


def report() -> None:
    props = load_properties()
    for target in target_ids(props):
        layout = target_layout(target, props)
        effective = layout.effective_files()
        overrides = sum(1 for rel in effective if (layout.overlay_root / rel).is_file())
        shadowed = sum(len(layout.source_candidates(rel)) > 1 for rel in effective if not is_lang_path(rel))
        print(f'{target}: {len(effective)} effective files, {overrides} target files, {shadowed} shadowed paths')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', action='store_true')
    args = parser.parse_args()
    errors = validate()
    if errors:
        raise SystemExit('\n'.join('ERROR: ' + error for error in errors))
    print('Source layout OK: ' + ', '.join(target_ids()))
    if args.report:
        report()
