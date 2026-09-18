# Source ownership and loader boundaries

## Five layers, not five independent mods

```text
source-shared/
    + source-families/<family>/
    + source-platforms/<loader>/
    + source-family-platforms/<family>/<loader>/
    + version-src/<target>/
    = materialized target source
```

A logical file uses the last applicable layer. The exception is language JSON:
its entries merge by key, from the broadest to the most specific layer.

| Layer | Owns |
| --- | --- |
| Shared | Cross-generation code/resources; pure balance, cooldown store and transport contract |
| Family | Minecraft-version/API adaptation with no direct loader imports |
| Platform | Loader-specific files usable across all participating generations |
| Family-platform | Loader hooks whose Minecraft/loader API signature is generation-specific |
| Target | Only irreducible differences for one target |

Use the narrowest layer that explains a difference. A Fabric event handler for
both 26.x versions belongs in `source-family-platforms/current/fabric`, not in two
target directories. Identical files are promoted rather than left shadowing one
another. The layout validator rejects direct loader imports in shared/family
code, identical consecutive overrides and identical sibling target overrides.
It also verifies all conditional sources, resources, balance references and
entrypoints. `--report` shows target-file and shadowed-path counts for review.

This refactor deliberately preserves the existing `legacy`, `modern` and `current`
families. The current family uses the names and toolchain from the supplied 26.x
branches; earlier families retain their existing Yarn-based setup. Moving a file
to a loader-independent layer does **not** automatically translate mapping names.

## Small version differences

`scripts/source_layout.py` preprocesses a deliberately limited Stonecutter-style
syntax. There is no hidden dependency on a Stonecutter Gradle plugin.

```java
//? if >=1.21.11 {
    return player.getEntityWorld().getTime();
//? } else {
    return player.getWorld().getTime();
//? }
```

Use short conditional sections for imports, signatures or small API changes.
Keep materially different implementations separate. Several nearly identical
1.21.x classes and common Fabric bootstrap/client classes use these branches;
this avoids maintaining an entire duplicate class for a changed method name.
All branches are materialized and validated across the configured target matrix.

## One balance table

`ScytheBalance.java` must exist exactly once, in `source-shared`. It contains 230
primitive constants and derived values at this refactoring checkpoint. Keep a
primitive constant declaration on one line and use only literals, references and
simple arithmetic: the resource generator deliberately rejects arbitrary Java
expressions rather than executing source code.

Durations use game ticks, distances use blocks, damage/healing use health points,
and effect amplifiers are zero-based. `TICKS_PER_SECOND` and the vanilla player
reference attributes describe units/reference values, not global changes to the
Minecraft simulation. `Base.DURABILITY`, material bonuses and weapon modifiers
are applied through the appropriate family `ScytheMaterial`/`ScytheSwordItem`.

Balance sections include base stats, Blood/Blood Harvest/Vampirism/Bleeding,
Toxic/Aura/Orb, Withering/Aura/Minion, Golden/Rain, Frozen/Storm/Ice Spike/Freezing,
Frozen Heart, Drops, Enchantments, Crafting, Progression and Presentation.

The old classic minion teleport search and the 26.x vertical-search implementation
remain different algorithms. Their pre-existing parameters have separate explicit
names (`CLASSIC_TELEPORT_*` versus `TELEPORT_*`) in this same table; the refactor
does not silently replace one algorithm with the other.

### Resource expressions

A whole JSON placeholder without a formatter expands to a typed JSON value:

```json
{"max_level": "${balance:Enchantments.ACIDITY_MAX_LEVEL}"}
```

Inside a language string, use the intended presentation:

```json
{
  "example.radius": "Radius: ${balance:GoldenRain.RADIUS|number}",
  "example.chance": "Chance: ${balance:Blood.BLEEDING_CHANCE|percent}%",
  "example.cooldown": "Cooldown: ${balance:GoldenRain.COOLDOWN_TICKS|seconds}s",
  "example.effect": "Slowness ${balance:Blood.BLENDER_SLOWNESS_AMPLIFIER|amplifier}"
}
```

Supported formatters are `number`, `percent`, `seconds`, `minutes`, `roman` (an
actual enchantment level) and `amplifier` (zero-based effect amplifier plus one).
Templates are expanded only in generated output; clients receive ordinary JSON.
Explicit effect levels in descriptions are bound too, not left as a literal `II`.
Existing descriptions that omit a value are not rewritten to invent new text.

Enchantment rarity/anvil-cost combinations must remain representable by the
legacy rarity enum. The validator checks that constraint, because allowing modern
JSON and legacy code to disagree would reintroduce gameplay drift. Numeric Java
balance/resource values are compared by the offline Java checker as well.

### Language deltas

Put common translations in shared, generation-specific changes in family and
actual one-version differences in the target layer. A later value replaces only
that key. To remove an inherited entry explicitly, set it to `null` in the delta;
no `null` values are emitted in the generated language file.

Other resources replace complete files. Recipes, texture data, registry IDs and
Mixin targets have not been converted into a speculative generic resource DSL.

## Server and client boundaries

`FabricServerHooks`, `ForgeServerHooks` and `NeoForgeServerHooks` own loader event subscriptions. They call
plain gameplay handlers for kills, loot, death, join, disconnect and server ticks.
`ScytheLifecycle` contains the tick and disconnect logic. Do not subscribe to loader
events from an ability, item, entity or effect implementation.

Ability key packets call `ScytheAbilityHandler.activate(player)`. The server
selects the held item and executes the matching skill; the client does not supply
an arbitrary damage amount, target list or cooldown. The main hand retains
priority, with an offhand fallback only when the main hand is not a scythe.

HUD trackers use `HudSync`, whose implementation is installed by the loader
bootstrap. `HudTransport<P>` is a tiny loader-independent contract; Fabric, Forge and NeoForge
implementations own their S2C calls while preserving the same timer semantics. A
second installation or use before installation fails explicitly. This is an
intentional bootstrap error, not a silently dropped HUD.

`CooldownStore<K>` is pure Java, shared by all scythes and all versions. The small
`ScytheCooldowns` adapter supplies player UUID and game time. Entries are separated
by owner and skill, expired entries are removed, and disconnect/server-stop hooks
clean them up. Existing Blood Harvest vanilla item cooldowns and the vampirism
internal timer remain separate where their semantics differ.

`ScytheTooltips` owns client presentation formerly copied into five item classes.
The common sword base invokes a delegate installed by the client entrypoint.
A dedicated server does not install or reference the actual tooltip renderer.
Client networking registration, keybindings and rendering belong to the loader
layer. The 1.20.1 Forge adapter implements those hooks separately instead of leaking
Forge APIs into shared gameplay classes.

## Build and metadata ownership

```text
build-config/common.properties             shared metadata, default build JVM
build-config/generations.properties        independent build roots
builds/<family>/targets.properties         targets and dependency overrides
builds/<family>/gradle.properties          family-specific Loom/ModDev bootstrap
builds/<family>/gradle/wrapper/             independent wrapper
build-logic/family-settings.gradle         resolve matrix, include target projects
build-logic/family-root.gradle             apply loader adapters and aggregate tasks
build-logic/common-target.gradle           sources, Java, packaging, publication
build-logic/fabric-target.gradle           Fabric/Loom-specific dependencies/tasks
build-logic/forge-target.gradle            Forge/Architectury-Loom dependencies/tasks
build-logic/neoforge-target.gradle         NeoForge Loom or ModDevGradle dependencies/tasks
```

Every build generation has the same multi-project shape. `legacy` remains Fabric-only, while `legacy-forge` points back to the same `legacy` source family. `modern-neoforge` contains both 1.21.1 and 1.21.11 NeoForge targets and points to the same `modern` family used by the 1.21.x Fabric targets. `current-neoforge` contains 26.1.2, 26.2 and 26.3 NeoForge, uses ModDevGradle, and points to the same `current` family as the matching Fabric targets. There is no
hand-maintained `build.gradle` in each target directory. Settings creates those
directories as needed and the root applies the common logic to every target.

The Python matrix resolver is authoritative for derived paths, family/platform
ownership and property inheritance: common values, general family defaults,
loader defaults, then explicit target overrides. Minecraft/loader identity comes
from the target ID. `java.version` is the mod bytecode/toolchain; `build.java` is
the Gradle JVM used by CI. Do not change the former to 21 merely because Loom
requires a Java 21 build process for the 1.20.1 target.

There is one descriptor template per implemented loader under `source-platforms`: `fabric/fabric.mod.json`, `forge/META-INF/mods.toml` and `neoforge/META-INF/neoforge.mods.toml`. Authors, license, name,
version and contact links come from common properties; Minecraft, Java and
loader/API constraints come from the resolved target. The
previous 1.21.11 MIT/single-author descriptor is normalized to the common
All Rights Reserved/two-author metadata. Explicit runtime-minimum overrides are
kept separate from build dependencies where the supplied targets differed.

IDE `.run` files and the CI matrix are generated from these same properties:

```text
python scripts/source_layout.py --describe
python scripts/source_layout.py --ci-matrix
python scripts/sync_idea.py --check
```

The IDEA generator preserves existing local Gradle JVM selections and unrelated
run configurations. It does not force one JDK on all family builds. Generated
sources are fingerprinted; stale, edited or extra generated files trigger a fresh
materialization. Arbitrary nonempty directories and maintained source paths are
rejected as output destinations.

## Adding a target or a loader

For another target on an implemented loader, add its ID/dependency overrides to
its family's `targets.properties`, create the source-layer directories (an empty
layer can contain `.gitkeep`), add only genuine source deltas, then run the layout
validator and IDEA generator. Build and launch it before publishing. No copied
client shell script, descriptor or target `build.gradle` is needed.

Forge 1.20.1 is implemented in the isolated `builds/legacy-forge` generation. It uses Architectury Loom 1.7.x with Yarn mappings, while `builds/legacy` keeps its original Fabric Loom. Both targets resolve the same `legacy` gameplay/family sources, while Forge-specific registration, entity attributes, loot/lifecycle hooks, networking, keybinds, HUD and render registration live under the Forge platform layers.

NeoForge 1.21.1 and 1.21.11 are both implemented in `builds/modern-neoforge`. The family uses Architectury Loom 1.13 with target-specific NeoForge Yarn patches and dependencies, while gameplay stays in the shared `modern` family. NeoForge 26.1.2, 26.2 and 26.3 are implemented in `builds/current-neoforge` with ModDevGradle 2 and Java 25, while gameplay stays in the shared `current` family. NeoForge-specific registry timing, events, payload networking, HUD/keybind/render registration and metadata live in loader layers. If a future loader/build changes mappings, treat that as a deliberate mapping migration or family/loader adaptation rather than a folder rename.

Reuse `ScytheBalance`, pure cooldown storage, ability rules and HUD contracts.
Implement the loader boundary instead of copying balance and event registration
back into each weapon. Shared gameplay still accesses the target's registry
facade/entrypoint; replacing that facade is part of a real loader port.

## Updating an existing checkout

This refactor moves and deletes old Java/resource copies. Apply the separate
**deletions patch** as well as the changed-files ZIP. Do not leave deleted source
files in lower layers: they can become inherited again and are not always an
identical duplicate that a validator can catch.

After applying both, run the validator, then refresh Gradle in IDEA. Previously
materialized target build directories are regenerated automatically. Do not copy
old per-target `build.gradle` files back into the new structure. A supplied full
project ZIP can instead be used as a clean root, without stale overlay files.
