# Scythe Mod

This repository contains the multiversion Scythe Mod source layout.

## Current supported targets in this repository

- Minecraft **1.20.1 Fabric** — `legacy` family, target Java 17
- Minecraft **1.21.1 Fabric** — `modern` family, target Java 21
- Minecraft **1.21.11 Fabric** — `modern` family, target Java 21
- Minecraft **26.1.2 Fabric** — `current` family, target Java 25
- Minecraft **26.2 Fabric** — `current` family, target Java 25

The Gradle/Loom build JVM is Java 21 for the legacy/modern generations and Java 25 for the current generation in CI. The 1.20.1 mod itself still compiles to Java 17 bytecode.

Minecraft 26.1+ uses unobfuscated Mojang names instead of Yarn mappings in this project, so it is intentionally isolated from the 1.21.x source/build generation instead of forcing mapping-generation differences into the same family.

The gameplay/source snapshots from the existing branches are integrated without changing their target-specific implementation. Shared files are stored once; family, Fabric and target-specific differences are layered on top.

See [SOURCE_FAMILIES.md](SOURCE_FAMILIES.md) for the source layout and build commands.

## IntelliJ IDEA

Open the repository root in IntelliJ IDEA. The shared IDEA Gradle configuration links all independent family builds using their own Gradle wrappers:

- `builds/legacy` — Minecraft 1.20.1 Fabric
- `builds/modern` — Minecraft 1.21.1 + 1.21.11 Fabric
- `builds/current` — Minecraft 26.1.2 + 26.2 Fabric

After Gradle synchronization, the Run/Debug selector contains:

- `ScytheMod 1.20.1 Fabric Client`
- `ScytheMod 1.21.1 Fabric Client`
- `ScytheMod 1.21.11 Fabric Client`
- `ScytheMod 26.1.2 Fabric Client`
- `ScytheMod 26.2 Fabric Client`

The modern and current targets are Gradle subprojects, so IDEA can load the targets at the same time and launch the correct Loom `runClient` task without per-version launcher scripts.

## Quick build

Windows PowerShell:

```powershell
./build-all.ps1
```

Linux/macOS:

```bash
./build-all.sh
```

Release jars are collected into `build/release/`.

## License

All Rights Reserved.
