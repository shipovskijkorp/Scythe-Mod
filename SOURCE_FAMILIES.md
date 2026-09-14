# Scythe Mod build families

Scythe Mod uses independent build families with one authoritative source repository.

Current targets:

- `legacy`: `1.20.1-fabric` (target Java 17; Gradle/Loom on Java 21)
- `modern`: `1.21.1-fabric`, `1.21.11-fabric` (target Java 21)
- `current`: `26.1.2-fabric`, `26.2-fabric` (target Java 25; Minecraft 26.1+ Mojang names)

The `modern` and `current` Gradle builds are multi-project builds: every configured target is loaded as its own Gradle subproject while sharing the same wrapper and family source layer. `current` now contains both 26.1.2 and 26.2 as sibling targets using the same Gradle/Loom/toolchain generation.

Each target is materialized in this order:

```text
source-shared
  + source-families/<family>
  + source-platforms/<loader>
  + version-src/<target>
  = effective target source tree
```

Later layers override earlier layers by logical path. The generated effective source is build output and is never authoritative source.

## Placement rules

- `source-shared`: loader-independent code/resources valid for every current target.
- `source-families/legacy`: Minecraft 1.20.1 generation implementation.
- `source-families/modern`: implementation shared by the Minecraft 1.21.x targets.
- `source-families/current`: Minecraft 26.x/Mojang-named implementation shared by current-generation targets.
- `source-platforms/fabric`: Fabric-specific implementation shared across families where the API and source names are actually compatible.
- `version-src/<target>`: irreducible target-specific code/resources and metadata such as `fabric.mod.json`.

The split deliberately keeps `net.fabricmc` imports out of `source-shared` and every `source-families/*` tree. This keeps future Forge/NeoForge platform layers possible without first untangling Fabric APIs from common gameplay code.

Some Fabric-specific files are duplicated in target overlays when their implementation is generation-specific. The four-layer model intentionally avoids introducing a family+platform cross-layer just for those cases.

Small Minecraft API differences may use the Stonecutter-style `//? if ...` directives supported by `scripts/source_layout.py`. Large differences should stay in family/platform/target layers.

## Why 26.x is a separate family

Minecraft 26.1+ in the supplied branch no longer uses Yarn mappings and compiles against unobfuscated Mojang names. It also targets Java 25, Fabric Loader 0.19.3, Loom 1.16-SNAPSHOT and Gradle 9.4.1. Fabric API and JEI remain target-specific (26.1.2 uses Fabric API 0.150.0+26.1.2 / JEI 29.6.2.31; 26.2 uses Fabric API 0.152.2+26.2 / JEI 30.2.0.15). Keeping this in `current` avoids contaminating the Java 21/Yarn-based `modern` family with mapping/build-tool conditionals.

## Commands

Build every target:

```text
./build-all.sh
```

PowerShell:

```text
./build-all.ps1
```

Build the legacy family:

```text
cd builds/legacy
./gradlew buildAndCollect
```

Build both modern targets:

```text
cd builds/modern
./gradlew buildAndCollect
```

Build the current family:

```text
cd builds/current
./gradlew buildAndCollect
```

Build or run one target directly:

```text
cd builds/modern
./gradlew :1.21.1-fabric:build
./gradlew :1.21.11-fabric:build
./gradlew :1.21.1-fabric:runClient
./gradlew :1.21.11-fabric:runClient

cd ../current
./gradlew :26.1.2-fabric:build
./gradlew :26.2-fabric:build
./gradlew :26.1.2-fabric:runClient
./gradlew :26.2-fabric:runClient
```

Materialize targets manually:

```text
python scripts/source_layout.py 1.20.1-fabric --output build/manual/1.20.1-fabric
python scripts/source_layout.py 1.21.1-fabric --output build/manual/1.21.1-fabric
python scripts/source_layout.py 1.21.11-fabric --output build/manual/1.21.11-fabric
python scripts/source_layout.py 26.1.2-fabric --output build/manual/26.1.2-fabric
python scripts/source_layout.py 26.2-fabric --output build/manual/26.2-fabric
```

## IntelliJ IDEA integration

`.idea/gradle.xml` links the independent build roots:

```text
builds/legacy
builds/modern
builds/current
```

Shared `.run` configurations launch:

```text
ScytheMod 1.20.1 Fabric Client  -> builds/legacy : runClient
ScytheMod 1.21.1 Fabric Client  -> builds/modern : :1.21.1-fabric:runClient
ScytheMod 1.21.11 Fabric Client -> builds/modern : :1.21.11-fabric:runClient
ScytheMod 26.1.2 Fabric Client  -> builds/current : :26.1.2-fabric:runClient
ScytheMod 26.2 Fabric Client    -> builds/current : :26.2-fabric:runClient
```

Keeping `legacy`, `modern` and `current` as independent Gradle builds lets each generation use the Gradle/Loom/toolchain stack it actually requires.
