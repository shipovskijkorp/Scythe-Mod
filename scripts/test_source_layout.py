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

    def test_publish_matrix_matches_every_target(self):
        props = sl.load_properties()
        rows = sl.publish_matrix(props)['include']
        by_target = {row['target']: row for row in rows}
        self.assertEqual(set(sl.target_ids(props)), set(by_target))
        self.assertEqual(len(rows), len(by_target))

        changelog_targets = [row['target'] for row in rows if row['modrinth_changelog']]
        self.assertEqual(['1.20.1-fabric'], changelog_targets)

        for target, row in by_target.items():
            with self.subTest(target=target):
                prefix = f'target.{target}.'
                platform = props[prefix + 'source.platform']
                self.assertEqual(props[prefix + 'build.generation'], row['generation'])
                self.assertEqual(props[prefix + 'deps.minecraft'], row['minecraft'])
                self.assertEqual(props[prefix + 'artifact.version'], row['version'])
                self.assertEqual(
                    f"{props[prefix + 'mod.archive_name']}-{platform}-{row['version']}.jar",
                    row['artifact'],
                )
                self.assertEqual('x9i4tLyb', row['modrinth_id'])
                self.assertEqual('1576581', row['curseforge_id'])
                if platform == 'fabric':
                    self.assertEqual('fabric\nquilt', row['loaders'])
                    self.assertIn('{modrinth:P7dR8mSH}', row['dependencies'])
                    self.assertIn('{curseforge:306612}', row['dependencies'])
                else:
                    self.assertEqual(platform, row['loaders'])
                    self.assertEqual('', row['dependencies'])

    def test_publish_matrix_rejects_unknown_changelog_target(self):
        props = sl.load_properties()
        props['publish.modrinth.changelog_target'] = '999-fabric'
        with self.assertRaises(ValueError):
            sl.publish_matrix(props)

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
                    platform = sl.target_layout(target, self.props).platform
                    expected_transport = {
                        'fabric': 'FabricHudTransport.java',
                        'forge': 'ForgeHudTransport.java',
                        'neoforge': 'NeoForgeHudTransport.java',
                    }.get(platform)
                    if expected_transport is not None:
                        self.assertTrue(list(out.rglob(expected_transport)))

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


class ForgeRuntimeSafetyTests(unittest.TestCase):
    def test_forge_registrables_are_created_during_register_event(self):
        path = sl.ROOT / 'source-family-platforms/legacy/forge/src/main/java/com/shipovskijkorp/scythes/mod/ScytheMod.java'
        text = path.read_text(encoding='utf-8')
        self.assertNotIn('public static final Item BLOODY_SCYTHE =', text)
        self.assertNotIn('public static final EntityType<IceSpikeEntity> ICE_SPIKE =', text)
        self.assertNotIn('public static final StatusEffect BLEEDING =', text)
        self.assertNotIn('public static final Enchantment SPIKED_BLADE =', text)
        self.assertIn('event.register(RegistryKeys.ITEM, id("bloody_scythe"),', text)
        self.assertIn('event.register(RegistryKeys.ENTITY_TYPE, id("ice_spike"),', text)
        self.assertIn('event.register(RegistryKeys.STATUS_EFFECT, id("bleeding"),', text)
        self.assertIn('event.register(RegistryKeys.ENCHANTMENT, id("spiked_blade"),', text)

    def test_forge_yarn_dev_runtime_does_not_auto_load_jei(self):
        props = sl.load_properties()
        self.assertEqual('false', props['target.1.20.1-forge.runtime.jei'])

    def test_forge_1201_resource_pack_metadata_and_models_are_materialized(self):
        props = sl.load_properties()
        with tempfile.TemporaryDirectory() as tmp:
            out = sl.materialize_target('1.20.1-forge', Path(tmp) / 'forge', props)
            metadata = json.loads((out / 'src/main/resources/pack.mcmeta').read_text(encoding='utf-8'))
            self.assertEqual(15, metadata['pack']['pack_format'])
            self.assertTrue((out / 'src/main/resources/assets/scythes/models/item/bloody_scythe.json').is_file())
            self.assertTrue((out / 'src/main/resources/assets/scythes/textures/item/bloody_scythe.png').is_file())

    def test_legacy_guide_icons_use_item_models(self):
        path = sl.ROOT / 'source-families/legacy/src/main/java/com/shipovskijkorp/scythes/mod/client/guide/GuideScreen.java'
        text = path.read_text(encoding='utf-8')
        self.assertIn('ItemStack icon = recipeStack(entry.item());', text)
        self.assertIn('context.drawItem(icon, x + 2, y + 2);', text)
        self.assertNotIn('textures/item/" + parts[1] + ".png', text)


    def test_modern_guides_do_not_blur_book_background_and_use_item_models(self):
        path = sl.ROOT / 'source-families/modern/src/main/java/com/shipovskijkorp/scythes/mod/client/guide/GuideScreen.java'
        text = path.read_text(encoding='utf-8')
        self.assertIn('renderBackground(context, mouseX, mouseY, delta);', text)
        self.assertIn('public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)', text)
        self.assertNotIn('0x90000000', text)
        self.assertIn('protected void applyBlur(float delta)', text)
        self.assertIn('protected void applyBlur(DrawContext context)', text)
        self.assertIn('ItemStack icon = recipeStack(entry.item());', text)
        self.assertIn('context.drawItem(icon, x + 2, y + 2);', text)
        self.assertNotIn('textures/item/" + parts[1] + ".png', text)

    def test_current_guides_use_item_models_for_index_icons(self):
        path = sl.ROOT / 'source-families/current/src/main/java/com/shipovskijkorp/scythes/mod/client/guide/GuideScreen.java'
        text = path.read_text(encoding='utf-8')
        self.assertIn('ItemStack icon = recipeStack(entry.item());', text)
        self.assertIn('graphics.item(icon, x + 2, y + 2);', text)
        self.assertNotIn('textures/item/" + parts[1] + ".png', text)

    def test_legacy_minion_uses_land_movement_without_idle_wandering(self):
        path = sl.ROOT / 'source-families/legacy/src/main/java/com/shipovskijkorp/scythes/mod/entity/WitheringMinionEntity.java'
        text = path.read_text(encoding='utf-8')
        self.assertNotIn('import net.minecraft.entity.ai.control.AquaticMoveControl;', text)
        self.assertNotIn('import net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation;', text)
        self.assertNotIn('new WanderAroundFarGoal(', text)
        self.assertIn('new SwimGoal(this)', text)
        self.assertIn('EnumSet.of(Control.MOVE)', text)
        self.assertIn('getNavigation().isIdle()', text)


    def test_all_guides_render_real_strong_poison_potion_stack(self):
        props = sl.load_properties()
        expected_api = {
            '1.20.1-fabric': 'PotionUtil.setPotion',
            '1.20.1-forge': 'PotionUtil.setPotion',
            '1.21.1-fabric': 'PotionContentsComponent.createStack',
            '1.21.1-neoforge': 'PotionContentsComponent.createStack',
            '1.21.11-fabric': 'DataComponentTypes.POTION_CONTENTS',
            '1.21.11-neoforge': 'DataComponentTypes.POTION_CONTENTS',
            '26.1.2-fabric': 'DataComponents.POTION_CONTENTS',
            '26.1.2-neoforge': 'DataComponents.POTION_CONTENTS',
            '26.2-fabric': 'DataComponents.POTION_CONTENTS',
            '26.2-neoforge': 'DataComponents.POTION_CONTENTS',
            '26.3-neoforge': 'DataComponents.POTION_CONTENTS',
            '26.3-fabric': 'DataComponents.POTION_CONTENTS',
        }
        with tempfile.TemporaryDirectory() as tmp:
            for target in sl.target_ids(props):
                with self.subTest(target=target):
                    out = sl.materialize_target(target, Path(tmp) / target, props)
                    path = out / 'src/main/java/com/shipovskijkorp/scythes/mod/client/guide/GuideScreen.java'
                    text = path.read_text(encoding='utf-8')
                    self.assertIn('ItemStack stack = recipeStack(slot);', text)
                    self.assertIn('"strong_poison".equals(slot.nameKey())', text)
                    self.assertIn(expected_api[target], text)



    def test_guide_recipe_slots_preserve_machine_name_key_separately_from_localized_tooltip(self):
        path = sl.ROOT / 'source-shared/src/main/java/com/shipovskijkorp/scythes/mod/guide/GuideResources.java'
        text = path.read_text(encoding='utf-8')
        self.assertIn('String nameKey = getString(object, "name", item);', text)
        self.assertIn('String name = names.getOrDefault(nameKey, nameKey);', text)
        self.assertIn('return new RecipeSlot(item, nameKey, name);', text)
        self.assertIn('record RecipeSlot(String item, String nameKey, String name)', text)

    def test_all_minions_use_ground_navigation_for_one_block_traversal(self):
        props = sl.load_properties()
        forbidden = (
            'AquaticMoveControl',
            'SmoothSwimmingMoveControl',
            'AmphibiousSwimNavigation',
            'AmphibiousPathNavigation',
        )
        with tempfile.TemporaryDirectory() as tmp:
            for target in sl.target_ids(props):
                with self.subTest(target=target):
                    out = sl.materialize_target(target, Path(tmp) / target, props)
                    path = out / 'src/main/java/com/shipovskijkorp/scythes/mod/entity/WitheringMinionEntity.java'
                    text = path.read_text(encoding='utf-8')
                    code = '\n'.join(line for line in text.splitlines() if not line.lstrip().startswith('//'))
                    self.assertFalse(any(token in code for token in forbidden), code)
                    self.assertNotIn('Control.MOVE, Control.LOOK', text)
                    self.assertNotIn('Flag.MOVE, Flag.LOOK', text)


    def test_modern_neoforge_targets_share_one_build_family(self):
        props = sl.load_properties()
        self.assertIn('1.21.1-neoforge', sl.target_ids(props))
        self.assertIn('1.21.11-neoforge', sl.target_ids(props))
        self.assertEqual('modern-neoforge', sl.target_layout('1.21.1-neoforge', props).generation)
        self.assertEqual('modern-neoforge', sl.target_layout('1.21.11-neoforge', props).generation)
        self.assertEqual('modern', props['target.1.21.1-neoforge.source.family'])
        self.assertEqual('modern', props['target.1.21.11-neoforge.source.family'])
        for target in ('1.21.1-neoforge', '1.21.11-neoforge'):
            neoforge_version = props[f'target.{target}.deps.neoforge']
            self.assertTrue(neoforge_version)
            self.assertEqual(
                f'[{neoforge_version},)',
                props[f'target.{target}.metadata.neoforge_min'],
            )
        self.assertEqual('1.21.11+build.5', props['target.1.21.11-neoforge.deps.yarn'])
        self.assertEqual('false', props['target.1.21.1-neoforge.runtime.jei'])
        self.assertEqual('false', props['target.1.21.11-neoforge.runtime.jei'])
        gradle_props = (sl.ROOT / 'builds/modern-neoforge/gradle.properties').read_text(encoding='utf-8')
        self.assertIn('architectury_loom_version=1.13.469', gradle_props)
        self.assertIn('loom.platform=neoforge', gradle_props)
        self.assertFalse((sl.ROOT / 'builds/modern-neoforge-12111').exists())

    def test_current_neoforge_ports_are_isolated_and_use_safe_freezing_render_state(self):
        props = sl.load_properties()
        targets = {
            '26.1.2-neoforge': ('26.1.2-fabric', '26.1.2.109', '[26.1.2.109,)', '[26.1.2]'),
            '26.2-neoforge': ('26.2-fabric', '26.2.0.88', '[26.2.0.88,)', '[26.2]'),
            '26.3-neoforge': ('26.3-fabric', '26.3.0.1-beta', '[26.3.0.1-beta,)', '[26.3]'),
        }
        for target, (fabric_target, neoforge_version, neoforge_range, minecraft_range) in targets.items():
            with self.subTest(target=target):
                self.assertIn(target, sl.target_ids(props))
                layout = sl.target_layout(target, props)
                self.assertEqual('current-neoforge', layout.generation)
                self.assertEqual('current', layout.family)
                self.assertEqual('neoforge', layout.platform)
                self.assertEqual(neoforge_version, props[f'target.{target}.deps.neoforge'])

                with tempfile.TemporaryDirectory() as tmp:
                    out = sl.materialize_target(target, Path(tmp) / target, props)
                    fabric_out = sl.materialize_target(fabric_target, Path(tmp) / fabric_target, props)
                    java = '\n'.join(path.read_text(encoding='utf-8') for path in out.rglob('*.java'))
                    neo_java_paths = {path.relative_to(out).as_posix() for path in out.rglob('*.java')}
                    fabric_java_paths = {path.relative_to(fabric_out).as_posix() for path in fabric_out.rglob('*.java')}
                    self.assertEqual(
                        {
                            'src/main/java/com/shipovskijkorp/scythes/mod/platform/fabric/FabricHudTransport.java',
                            'src/main/java/com/shipovskijkorp/scythes/mod/platform/fabric/FabricServerHooks.java',
                        },
                        fabric_java_paths - neo_java_paths,
                    )
                    self.assertEqual(
                        {
                            'src/main/java/com/shipovskijkorp/scythes/mod/client/FreezingVisualClientState.java',
                            'src/main/java/com/shipovskijkorp/scythes/mod/platform/neoforge/NeoForgeHudTransport.java',
                            'src/main/java/com/shipovskijkorp/scythes/mod/platform/neoforge/NeoForgeServerHooks.java',
                        },
                        neo_java_paths - fabric_java_paths,
                    )
                    tracker_path = 'src/main/java/com/shipovskijkorp/scythes/mod/ability/GoldenRainDimensionDayTracker.java'
                    self.assertIn(tracker_path, neo_java_paths)
                    self.assertEqual(
                        (fabric_out / tracker_path).read_text(encoding='utf-8'),
                        (out / tracker_path).read_text(encoding='utf-8'),
                    )
                    self.assertNotIn('net.fabricmc', java)
                    self.assertNotIn('platform.fabric', java)
                    self.assertNotIn('defineId(LivingEntity.class', java)
                    self.assertNotIn('SCYTHES_FROZEN_FOR_RENDERING', java)
                    client_java = (out / 'src/main/java/com/shipovskijkorp/scythes/mod/client/ScytheModClient.java').read_text(encoding='utf-8')
                    renderer_mixin_java = (out / 'src/main/java/com/shipovskijkorp/scythes/mod/mixin/FreezingLivingEntityRendererMixin.java').read_text(encoding='utf-8')
                    self.assertNotIn('RegisterRenderStateModifiersEvent', client_java)
                    self.assertNotIn('RenderLivingEvent.Post', client_java)
                    self.assertIn('FreezingOverlayRenderer.render(state, poseStack, collector)', renderer_mixin_java)
                    self.assertIn('FreezingVisualClientState.isFrozen(entity.getUUID())', renderer_mixin_java)
                    self.assertIn('FreezingVisualPayload', java)
                    self.assertIn('event.registrar("2")', java)
                    self.assertIn('sendToPlayersTrackingEntityAndSelf', java)
                    self.assertIn('onStartTracking(PlayerEvent.StartTracking event)', java)
                    self.assertIn('RegisterClientPayloadHandlersEvent', java)
                    self.assertIn('ClientPacketDistributor.sendToServer', java)

                    if target == '26.3-neoforge':
                        farmer = (out / 'src/main/java/com/shipovskijkorp/scythes/mod/ability/FarmerHarvestHandler.java').read_text(encoding='utf-8')
                        welcome = (out / 'src/main/java/com/shipovskijkorp/scythes/mod/ability/WelcomeAdvancementHandler.java').read_text(encoding='utf-8')
                        frozen_heart = (out / 'src/main/java/com/shipovskijkorp/scythes/mod/ability/FrozenHeartDropHandler.java').read_text(encoding='utf-8')
                        farmer_item = (out / 'src/main/java/com/shipovskijkorp/scythes/mod/item/FarmerScytheItem.java').read_text(encoding='utf-8')
                        self.assertIn('Prediction.SERVER_ONLY', farmer)
                        self.assertIn('Prediction.SERVER_ONLY', welcome)
                        self.assertIn('ContextIntProviders.exactly', frozen_heart)
                        self.assertNotIn('ConstantValue.exactly', frozen_heart)
                        self.assertIn('public class FarmerScytheItem extends Item', farmer_item)
                        self.assertIn('super(settings.hoe(', farmer_item)
                        self.assertIn('InputConstants.Type.KEYBOARD', client_java)
                        self.assertIn('InputConstants.KEY_R', client_java)
                        self.assertNotIn('GLFW.GLFW_KEY_R', client_java)

                    mixins = (out / 'src/main/resources/scythes.mixins.json').read_text(encoding='utf-8')
                    self.assertIn('FreezingLivingEntityRendererMixin', mixins)
                    self.assertIn('FreezingLivingEntityRenderStateMixin', mixins)

                    metadata = (out / 'src/main/resources/META-INF/neoforge.mods.toml').read_text(encoding='utf-8')
                    self.assertIn('modId="scythes"', metadata)
                    self.assertIn(f'versionRange="{neoforge_range}"', metadata)
                    self.assertIn(f'versionRange="{minecraft_range}"', metadata)
                    self.assertNotIn('@meta.', metadata)
                    self.assertNotIn('@mod.', metadata)

    def test_neoforge_12111_effective_source_has_no_fabric_loader_references(self):
        props = sl.load_properties()
        with tempfile.TemporaryDirectory() as tmp:
            out = sl.materialize_target('1.21.11-neoforge', Path(tmp) / 'neoforge-12111', props)
            java = '\n'.join(path.read_text(encoding='utf-8') for path in out.rglob('*.java'))
            self.assertNotIn('net.fabricmc', java)
            self.assertNotIn('platform.fabric', java)
            self.assertIn('NeoForgeHudTransport', java)
            self.assertIn('RegisterPayloadHandlersEvent', java)
            self.assertIn('registryKey(itemKey(name))', java)
            self.assertIn('.build(entityTypeKey("toxic_orb"))', java)
            client_text = (out / 'src/main/java/com/shipovskijkorp/scythes/mod/client/ScytheModClient.java').read_text(encoding='utf-8')
            self.assertIn('event.registerCategory(category);', client_text)
            c2s_text = (out / 'src/main/java/com/shipovskijkorp/scythes/mod/network/ScytheAbilityC2SPacket.java').read_text(encoding='utf-8')
            self.assertIn('ClientPacketDistributor.sendToServer', c2s_text)
            self.assertNotIn('import net.neoforged.neoforge.network.PacketDistributor;', c2s_text)

    def test_neoforge_1211_effective_source_has_no_fabric_loader_references(self):
        props = sl.load_properties()
        with tempfile.TemporaryDirectory() as tmp:
            out = sl.materialize_target('1.21.1-neoforge', Path(tmp) / 'neoforge', props)
            java = '\n'.join(path.read_text(encoding='utf-8') for path in out.rglob('*.java'))
            self.assertNotIn('net.fabricmc', java)
            self.assertNotIn('platform.fabric', java)
            self.assertIn('NeoForgeHudTransport', java)
            self.assertIn('RegisterPayloadHandlersEvent', java)

    def test_neoforge_1211_registrables_are_deferred(self):
        path = sl.ROOT / 'version-src/1.21.1-neoforge/src/main/java/com/shipovskijkorp/scythes/mod/ScytheMod.java'
        text = path.read_text(encoding='utf-8')
        self.assertNotIn('public static final Item BLOODY_SCYTHE =', text)
        self.assertNotIn('public static final EntityType<ToxicOrbEntity> TOXIC_ORB =', text)
        self.assertIn('event.register(RegistryKeys.ITEM, id("bloody_scythe"),', text)
        self.assertIn('event.register(RegistryKeys.ENTITY_TYPE, id("toxic_orb"),', text)
        self.assertIn('event.register(RegistryKeys.STATUS_EFFECT, helper ->', text)

    def test_neoforge_metadata_materializes_without_placeholders(self):
        props = sl.load_properties()
        with tempfile.TemporaryDirectory() as tmp:
            for target in ('1.21.1-neoforge', '1.21.11-neoforge'):
                with self.subTest(target=target):
                    out = sl.materialize_target(target, Path(tmp) / target, props)
                    path = out / 'src/main/resources/META-INF/neoforge.mods.toml'
                    text = path.read_text(encoding='utf-8')
                    self.assertIn('modId="scythes"', text)
                    self.assertIn('modId="neoforge"', text)
                    self.assertNotIn('@meta.', text)
                    self.assertNotIn('@mod.', text)


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
