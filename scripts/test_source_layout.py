#!/usr/bin/env python3
"""Offline tests for the source matrix, resource templates and incremental staging."""
from __future__ import annotations

import json
from pathlib import Path
import tempfile
import unittest

import source_layout as sl
from resource_values import expand_json, metadata_values, read_balance
from sync_idea import run_config, sync, write_if_changed

BALANCE = sl.ROOT / 'source-shared/src/main/java/com/shipovskijkorp/scythes/mod/balance/ScytheBalance.java'


class MatrixTests(unittest.TestCase):
    def test_unique_targets_and_families(self):
        props = sl.load_properties()
        targets = sl.target_ids(props)
        self.assertEqual(len(targets), len(set(targets)))
        self.assertEqual(set(targets), {t for group in sl.generation_targets(props).values() for t in group})

    def test_ci_uses_the_same_family_matrix(self):
        matrix = sl.ci_matrix()
        self.assertEqual(set(sl.generation_config_paths()), {row['family'] for row in matrix['include']})
        self.assertTrue(all(int(row['java']) >= 21 for row in matrix['include']))

    def test_derived_layers_and_coordinates(self):
        props = sl.load_properties()
        for target in sl.target_ids(props):
            with self.subTest(target=target):
                layout = sl.target_layout(target, props)
                self.assertEqual(5, len(layout.layers))
                self.assertEqual(target.rsplit('-', 1)[0], props[f'target.{target}.deps.minecraft'])
                self.assertEqual(f'source-family-platforms/{layout.family}/{layout.platform}',
                                 layout.family_platform_root.relative_to(sl.ROOT).as_posix())
                self.assertTrue(all(p.is_dir() for p in layout.layers))

    def test_family_matrix_matches_global_resolution(self):
        all_props = sl.load_properties()
        for config in sl.generation_config_paths().values():
            props = sl.load_properties(config)
            for target in sl.target_ids(props):
                for key in props:
                    if key.startswith(f'target.{target}.'):
                        self.assertEqual(all_props[key], props[key])

    def test_properties_reject_duplicates(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'bad.properties'
            path.write_text('x=1\nx=2\n')
            with self.assertRaises(ValueError):
                sl.read_properties(path)

    def test_properties_continuation_and_comments(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'valid.properties'
            path.write_text('# comment\nx=first,\\\nsecond\n! comment\ny=three\n')
            self.assertEqual({'x': 'first,second', 'y': 'three'}, sl.read_properties(path))

    def test_unknown_target_rejected(self):
        with self.assertRaises(ValueError):
            sl.target_layout('999-fabric')

    def test_parent_traversal_rejected(self):
        layout = sl.target_layout(sl.target_ids()[0])
        with self.assertRaises(ValueError):
            layout.source_candidates('../secret')


class DirectiveTests(unittest.TestCase):
    def render(self, text, version='26.2', platform='fabric'):
        return sl.preprocess_text(text, minecraft=version, family='current', platform=platform)

    def test_new_and_old_branches(self):
        text = '//? if >=26.2 {\nnew\n//? } else {\nold\n//? }\n'
        self.assertEqual('new\n', self.render(text))
        self.assertEqual('old\n', self.render(text, '26.1.2'))

    def test_nested_conditions(self):
        text = '//? if >=26.1 {\n//? if fabric {\nfabric\n//? } else {\nother\n//? }\n//? }\n'
        self.assertEqual('fabric\n', self.render(text))
        self.assertEqual('other\n', self.render(text, platform='neoforge'))

    def test_real_comments_not_stripped(self):
        text = '//? if >=26 {\n/* keep this comment */\nvalue\n//? }\n'
        self.assertEqual('/* keep this comment */\nvalue\n', self.render(text))

    def test_wrapped_inactive_branch_activated(self):
        text = '//? if >=26 {\n/*\nvalue\n*/\n//? }\n'
        self.assertIn('value', self.render(text))
        self.assertNotIn('/*', self.render(text))

    def test_unclosed_and_unexpected_else_rejected(self):
        for text in ('//? if >=26 {\nx\n', '//? } else {\nx\n//? }\n'):
            with self.subTest(text=text), self.assertRaises(ValueError):
                self.render(text)

    def test_all_project_directives(self):
        sl.validate_all_directives()


class ResourceTests(unittest.TestCase):
    def setUp(self):
        self.balance = read_balance(BALANCE)

    def parse_sample(self, fields):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'ScytheBalance.java'
            path.write_text('public final class ScytheBalance {\n'
                            'public static final class Base {\n'
                            'public static final int DURABILITY = 100;\n' + fields + '\n}\n}\n')
            return read_balance(path)

    def test_derived_values(self):
        b = self.balance
        self.assertEqual(b['Base.PLAYER_ATTACK_DAMAGE'] + b['Base.MATERIAL_ATTACK_DAMAGE'] +
                         b['Base.ATTACK_DAMAGE_BONUS'], b['Base.ATTACK_DAMAGE'])
        self.assertEqual(b['Minion.OWNER_TELEPORT_DISTANCE'] ** 2, b['Minion.OWNER_TELEPORT_DISTANCE_SQUARED'])

    def test_numeric_and_string_substitution(self):
        b = {'x': 8, 'chance': .25, 'TICKS_PER_SECOND': 20}
        result = expand_json({'level': '${balance:x}', 'line': 'Chance ${balance:chance|percent}%'}, b, {})
        self.assertIsInstance(result['level'], int)
        self.assertEqual('Chance 25%', result['line'])

    def test_displayed_enchantment_and_effect_levels(self):
        self.assertEqual('II', expand_json('${balance:level|roman}', {'level': 2}, {}))
        self.assertEqual('II', expand_json('${balance:amp|amplifier}', {'amp': 1}, {}))
        self.assertEqual('IV', expand_json('${balance:amp|amplifier}', {'amp': 3}, {}))

    def test_invalid_displayed_level_rejected(self):
        with self.assertRaises(ValueError):
            expand_json('${balance:amp|amplifier}', {'amp': -1}, {})

    def test_time_formatting(self):
        b = {'time': 3000, 'TICKS_PER_SECOND': 20}
        self.assertEqual('150', expand_json('${balance:time|seconds}', b, {}))
        self.assertEqual('2.5', expand_json('${balance:time|minutes}', b, {}))

    def test_unknown_or_invalid_token_fails(self):
        for token in ('${balance:no_such_field}', '${balance:Base.DURABILITY|unknown}'):
            with self.subTest(token=token), self.assertRaises(ValueError):
                expand_json(token, self.balance, {})

    def test_java_integer_division_and_remainder(self):
        b = self.parse_sample('public static final int QUOTIENT = -7 / 3;\n'
                              'public static final int REMAINDER = -7 % 3;')
        self.assertEqual(-2, b['Base.QUOTIENT'])
        self.assertEqual(-1, b['Base.REMAINDER'])

    def test_unsupported_expression_and_cycle_rejected(self):
        for fields in ('public static final int A = evil();',
                       'public static final int A = B;\npublic static final int B = A;'):
            with self.subTest(fields=fields), self.assertRaises(ValueError):
                self.parse_sample(fields)

    def test_metadata_typed_authors_and_versions(self):
        props = sl.load_properties()
        for target in sl.target_ids(props):
            meta = metadata_values(target, props)
            self.assertIsInstance(expand_json('@mod.authors@', {}, meta), list)
            self.assertEqual(props[f'target.{target}.artifact.version'], meta['mod.version'])

    def test_language_merges_preserve_keys_and_allow_explicit_removal(self):
        with tempfile.TemporaryDirectory() as tmp:
            roots = [Path(tmp) / str(i) for i in range(5)]
            rel = 'src/main/resources/assets/test/lang/en_us.json'
            for root, data in zip(roots, ({'a': 'A', 'b': 'B'}, {'a': 'changed'}, {}, {}, {'b': None, 'c': 'C'})):
                path = root / rel
                path.parent.mkdir(parents=True)
                path.write_text(json.dumps(data))
            layout = sl.TargetLayout('26.2-fabric', 'current', 'current', 'fabric', *roots)
            self.assertEqual({'a': 'changed', 'c': 'C'}, sl.merged_language(
                layout, rel, {'target.26.2-fabric.deps.minecraft': '26.2'}))


class MaterializationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.props = sl.load_properties()

    def test_every_target_generates_valid_json_and_single_balance(self):
        with tempfile.TemporaryDirectory() as tmp:
            for target in sl.target_ids(self.props):
                with self.subTest(target=target):
                    out = sl.materialize_target(target, Path(tmp) / target, self.props)
                    self.assertEqual(1, len(list(out.rglob('ScytheBalance.java'))))
                    for path in out.rglob('*.json'):
                        json.loads(path.read_text(encoding="utf-8"))
                        self.assertNotIn('${balance:', path.read_text(encoding="utf-8"))
                    self.assertFalse(list(out.rglob('*HudS2CPacket.java')))
                    self.assertTrue(list(out.rglob('FabricHudTransport.java')))

    def test_cached_staging_unchanged_and_corruption_repaired(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = sl.target_ids(self.props)[0]
            out = sl.materialize_target(target, Path(tmp) / 'out', self.props)
            file = next(out.rglob('ScytheBalance.java'))
            before = file.read_bytes()
            timestamp = file.stat().st_mtime_ns
            sl.materialize_target(target, out, self.props, if_stale=True)
            self.assertEqual(timestamp, file.stat().st_mtime_ns)
            file.write_text('broken')
            sl.materialize_target(target, out, self.props, if_stale=True)
            self.assertEqual(before, file.read_bytes())
            extra = out / 'stale.java'
            extra.write_text('obsolete')
            sl.materialize_target(target, out, self.props, if_stale=True)
            self.assertFalse(extra.exists())

    def test_refuses_to_replace_repository_or_maintained_sources(self):
        target = sl.target_ids(self.props)[0]
        for dest in [sl.ROOT, sl.ROOT / 'source-shared', sl.ROOT / 'source-shared/src']:
            with self.subTest(dest=str(dest)), self.assertRaises(ValueError):
                sl.materialize_target(target, dest, self.props)

    def test_refuses_unknown_nonempty_directory(self):
        with tempfile.TemporaryDirectory() as tmp:
            (Path(tmp) / 'user-file.txt').write_text('keep me')
            with self.assertRaises(ValueError):
                sl.materialize_target(sl.target_ids()[0], Path(tmp))
            self.assertEqual('keep me', (Path(tmp) / 'user-file.txt').read_text(encoding="utf-8"))


class IdeaTests(unittest.TestCase):
    def test_committed_run_configs_match_matrix(self):
        self.assertEqual([], sync(check=True))

    def test_run_configuration_uses_target_not_active_alias(self):
        filename, text = run_config('26.2-fabric', 'current', 'fabric')
        self.assertIn('26.2', filename)
        self.assertIn(':26.2-fabric:runClient', text)
        self.assertNotIn('runActiveClient', text)

    def test_write_is_idempotent(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'config.run.xml'
            self.assertTrue(write_if_changed(path, 'one'))
            stamp = path.stat().st_mtime_ns
            self.assertFalse(write_if_changed(path, 'one'))
            self.assertEqual(stamp, path.stat().st_mtime_ns)
            self.assertTrue(write_if_changed(path, 'two'))


if __name__ == '__main__':
    unittest.main(verbosity=2)
