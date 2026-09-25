# Scythe Mod

Twelve targets, one balance definition, six independent Gradle build families.

| Family | Minecraft / loader | Mod bytecode | Gradle JVM used by CI |
| --- | --- | --- | --- |
| `legacy` | 1.20.1 Fabric | Java 17 | Java 21 |
| `legacy-forge` | 1.20.1 Forge | Java 17 | Java 21 |
| `modern` | 1.21.1, 1.21.11 Fabric | Java 21 | Java 21 |
| `modern-neoforge` | 1.21.1, 1.21.11 NeoForge | Java 21 | Java 21 |
| `current` | 26.1.2, 26.2, 26.3 Fabric | Java 25 | Java 25 |
| `current-neoforge` | 26.1.2, 26.2, 26.3 NeoForge | Java 25 | Java 25 |

Forge 1.20.1 remains an isolated Gradle build because it must coexist with the legacy Fabric toolchain. Both modern NeoForge targets live together in `builds/modern-neoforge`, use Architectury Loom 1.13, and reuse the same `modern` gameplay sources as their Fabric siblings. NeoForge 26.1.2, 26.2 and 26.3 live together in `builds/current-neoforge`, use ModDevGradle with the official named APIs, and reuse the same `current` gameplay sources as their Fabric siblings.

## IntelliJ IDEA

Open this repository root. `.idea/gradle.xml` links `builds/legacy`, `builds/legacy-forge`, `builds/modern`, `builds/modern-neoforge`, `builds/current` and `builds/current-neoforge` as independent Gradle builds. Select the appropriate **Gradle
JVM** from the table for each build, and synchronize Gradle. A JDK and **Python
3.10+** are required; `SCYTHE_PYTHON` can point to the Python executable.

Every target, including legacy, is a Gradle subproject. Shared `.run` configurations
appear as `ScytheMod <version> <Loader> Client`. They call the corresponding target's
`runClient`, not the currently active development target. JEI compile integration is configured for every target; development runtime loading is target-specific where remapped loader environments require it to be disabled.

Run configurations are generated from `targets.properties` during Gradle settings
loading. They can also be refreshed without Gradle:

```text
python scripts/sync_idea.py
```

The first IDEA Gradle sync materializes source files before IDEA indexes them.
Subsequent syncs reuse unchanged output. Edit the maintained source layers, **not**
`build/effective-source` files: those are generated and will be overwritten.
There are no per-client `.bat`, `.ps1` or `.sh` launchers.

## Balance: edit one file

```text
source-shared/src/main/java/com/shipovskijkorp/scythes/mod/balance/ScytheBalance.java
```

This is a compile-time balance table, not a runtime configuration file. Its nested
sections contain base material/weapon parameters, active and passive ability
values, cooldowns, costs, status effects, projectiles, minions, loot, enchantments,
food and progression values. Rebuild the targets after changing it.

For example, change `Base.DURABILITY` for durability, `Base.ATTACK_DAMAGE_BONUS`
and `Base.ATTACK_SPEED_MODIFIER` for weapon modifiers, or the corresponding
`Blood`, `Toxic`, `Withering`, `Golden` and `Frozen` sections for abilities.
`Base.ATTACK_DAMAGE` is derived from the material, modifier and vanilla player
reference value; it is not a second independent damage setting.

Gameplay reads the Java fields directly. Data-driven enchantment JSON and numeric
translation descriptions are generated from those **same fields**. This avoids
changing the damage in Java while leaving the old cooldown or chance in a tooltip.
Technical constants such as network IDs, flags, slot indices, rendering geometry
and numerical tolerances are not balance entries. Delegated vanilla behavior is
not reimplemented just to make every vanilla internal constant configurable.

## Build

Use the existing `build-all.ps1`, `build-all.sh` or `build-all.bat` entry point.
They all delegate to the same matrix-driven Python script; family names are not
copied into each shell script. The selected Gradle JVM must satisfy each wrapper
and plugin you run; a Java 21 process is not the configured current-family JVM.
Families can also be built separately with their respective JDKs.

```powershell
./build-all.ps1
./build-all.ps1 --family modern
```

```sh
./build-all.sh
./build-all.sh --family current
```

Release files go to `build/release/` and include the loader in their name, for
example `scythe-mod-fabric-5.0+26.3.jar`. Remapped builds collect `remapJar` output;
the named 26.x build collects `jar` output.

A direct target build or launch remains available:

```text
cd builds/modern
./gradlew :1.21.11-fabric:build
./gradlew :1.21.11-fabric:runClient

cd ../modern-neoforge
./gradlew :1.21.1-neoforge:build
./gradlew :1.21.1-neoforge:runClient
./gradlew :1.21.11-neoforge:build
./gradlew :1.21.11-neoforge:runClient

cd ../current-neoforge
./gradlew :26.1.2-neoforge:build
./gradlew :26.1.2-neoforge:runClient
./gradlew :26.2-neoforge:build
./gradlew :26.2-neoforge:runClient
./gradlew :26.3-neoforge:build
./gradlew :26.3-neoforge:runClient
```

Use `gradlew.bat` on Windows. Each target has its own build and runtime directories.

## Publishing

Publishing is automatic when a GitHub Release is published. The release tag must
match `common.mod.version` (for example, tag `5.2` for
`common.mod.version=5.2`). GitHub Actions validates and builds all configured
targets, attaches the JARs to the GitHub Release, and then publishes them in a
stable order: every Fabric target from the oldest Minecraft version to the newest,
then Forge targets in version order, then NeoForge targets in version order.
Publication is serialized inside each loader group so versions are not uploaded
out of order.

Publication metadata is derived from the same target matrix as the builds. Fabric
files are marked as Fabric + Quilt and automatically declare Fabric API as a
required dependency. Forge and NeoForge publication steps do not receive Fabric
API dependency metadata. Tag pushes are excluded from the ordinary build workflow
because the release workflow already performs the same source checks and Gradle
builds before publishing.

The full Modrinth changelog is attached only to the configured
`publish.modrinth.changelog_target` (currently `1.20.1-fabric`); other
Modrinth files are published with an empty changelog. CurseForge files all receive
the GitHub Release body as their changelog.

Repository Actions secrets `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN` are required.
The publication matrix can be inspected locally with:

```text
python scripts/source_layout.py --publish-matrix
```

## Checks

```text
python scripts/validate_source_layout.py --report
python scripts/test_source_layout.py
python scripts/verify_java.py
```

The last command requires JDK 21+ and checks Java syntax, pure-core logic and small
adapter contracts against test stubs. It **does not compile against Minecraft**.
CI runs these checks and then performs actual Gradle builds for each family.

See [SOURCE_FAMILIES.md](SOURCE_FAMILIES.md) for ownership rules, resource templates and loader boundaries. See [REFACTOR_VERIFICATION.md](REFACTOR_VERIFICATION.md)
for the checks and limitations of this refactoring delivery.

## License

All Rights Reserved. Shared metadata in `build-config/common.properties` is the
single source for generated Fabric, Forge and NeoForge descriptors.
