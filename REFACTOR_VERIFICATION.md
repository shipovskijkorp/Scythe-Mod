# Refactor verification checkpoint

This report describes checks performed on the five-target refactoring delivery.
It is not a claim that Minecraft or Gradle successfully launched in the editing
environment.

## Baseline

The baseline was reconstructed from the multiversion 1.20.1/1.21.1 project, the
IDEA run-configuration patch, and the sequential 1.21.11, 26.1.2 and 26.2 patches,
including the 1.21.11 deletions. Original single-version snapshots were used for
source/resource and default-value comparisons. Per-client shell launcher scripts
were not part of this baseline.

## Maintained source reduction

Counts include the five maintained source-layer trees, excluding `.gitkeep` and
all generated files. These are physical maintained copies, not effective runtime
file counts.

| Metric | Before | After |
| --- | ---: | ---: |
| Source/resource files | 504 | 449 |
| Java source files | 282 | 232 |
| Target-overlay files | 198 | 116 |
| Source/resource bytes | 1,474,785 | 1,165,782 |

There are 30 small-diff conditional merges. The 26.1.2 and 26.2 target overlays
now retain one Java file each; common current-generation Fabric code lives in the
family-platform layer. New shared helpers can increase a target's effective file
count while still decreasing the number of independently maintained copies.

## Checks actually completed

- Source ownership/directives/resources/entrypoints/IDE validator passed for all
  five targets. The shared Java layers contain no direct Fabric/Forge/NeoForge
  imports, and sibling target copies are checked for redundant identical files.
- 31 Python tests passed: target resolution, derived roots, CI/IDE generation,
  properties, conditionals, JSON typing, numeric/effect-level formatting, language
  merging, output safety, stale-output detection and regeneration.
- The JDK compiler parser parsed all **381 effective Java files** without syntax
  errors. This checks syntax only, not Minecraft API linkage.
- The actual balance, cooldown storage and HUD contract classes were compiled
  with `javac --release 17`. The actual per-target cooldown/HUD/ability dispatch
  adapters were compiled alongside minimal Minecraft/test stubs. Each target
  passed **30 contract assertions** for cooldown isolation/expiry/cleanup,
  transport initialization, duration clamping and ability dispatch.
- Reflection of the compiled Java constants matched all **230** values read by
  the resource generator for every target, within floating-point tolerance.
- **638 default-constant comparisons** against the original snapshots passed.
  The original netherite material attack reference was evaluated as 4, and the
  old cross-class active durability reference as its original value of 100.
- Packing the transformed logical sources into layers and materializing them
  again reproduced every Java/binary file exactly and every JSON file semantically,
  excluding the deliberately regenerated Fabric descriptor.
- All original language values match at the default balance. All other original
  resources match as well, including enchantment data, recipes and textures,
  except for intentionally unified Fabric metadata.

The default-value regression comparisons are a migration checkpoint, not another
maintained gameplay table. Routine tests derive their invariants from the actual
balance file so future tuning does not require synchronizing a second set of
constants in tests.

## Deliberate changes beyond file placement

The refactor adds defensive rejection of dead-player activation requests where a
supplied handler lacked that guard, pruning/clamping in shared cooldown storage,
server-stop cooldown cleanup, explicit HUD transport bootstrap checks, source
output safety/caching and generated metadata. The 1.21.11 descriptor now uses the
same license/authors as common metadata, instead of its old divergent values.

The classic and current minion teleport algorithms are retained, with their
pre-existing different search parameters named explicitly in the balance table.
No new scythes or loader ports were implemented.

## Full-build limitation

Actual wrapper commands were attempted:

```text
builds/legacy:  sh gradlew --no-daemon build
builds/current: sh gradlew --no-daemon build
```

Both failed while downloading their Gradle distribution, before evaluating the
project build scripts or compiling against Minecraft:

```text
java.net.UnknownHostException: services.gradle.org
```

The attempted distributions were Gradle 9.2.0 and 9.4.1 respectively. No full
Minecraft-linked compilation, client/JEI launch, dedicated-server smoke test,
IntelliJ UI test or actual Forge/NeoForge execution was completed here. The CI
workflow contains real family build jobs, but their success is not asserted by
this report.

## Reproduce in a development environment

```text
python scripts/validate_source_layout.py --report
python scripts/test_source_layout.py
python scripts/verify_java.py
```

Then build each family with its configured Gradle JVM and use the five IDEA client
configurations. Check normal attacks, all abilities/costs/cooldowns, HUD timers,
tooltips, minion follow/regeneration, enchantments and loot, and run a dedicated
server before publishing the refactored artifacts.
