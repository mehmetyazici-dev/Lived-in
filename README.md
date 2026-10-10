# Lived-In

A Minecraft 1.21.1 / NeoForge mod for a more inhabited singleplayer world.

**Current state:** Phase 1 (milestones 1–5) is complete. Place a Traveler Spawn Egg, use `/livedin camp` for a musician and companions, `/livedin fishing <shore> <water>` for a fisher and sitting cat, or `/livedin boat <start> <end>` for a seated boat rider. The build, 25 server tests, and combined checks with nine scenes passed, including repeated revisits, saves, a full client restart, and the packaged JAR in a separate NeoForge instance. See the development guide for test conditions and limits.

## Project documents

- [Phase 1 gameplan](Documents/GAMEPLAN-Phase1.md): scene ideas, milestones, acceptance criteria, and future scope.
- [Development guide](Documents/Lived-in-Development.md): NeoForge, the mod's entry point, build/loading workflow, toolchain, and verification.

These documents are updated alongside project improvements. The workflow is recorded in [AGENTS.md](AGENTS.md).

## Quick start

```powershell
cd C:\Lived-in
. .\scripts\dev-env.ps1
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

Open IntelliJ with .\scripts\open-idea.ps1. The current artifact is build/libs/livedin-0.1.0-dev.jar. See the development guide for installation paths, supported versions, and packaging details.

In a Creative test world, search for **Traveler Spawn Egg**, or use /give @s livedin:ambient_npc_spawn_egg. Use open, dry ground. On flat terrain, /livedin camp places the campfire prototype five blocks ahead. For fishing, specify the fisher's feet on a dry bank and an exposed water block one block lower, three to five blocks away. For the boat scene, specify two water blocks at the same height, 4–12 blocks apart along X or Z, with a clear 3-wide source-water corridor and two air blocks above it. The development guide explains terrain requirements, behavior, test results, and limitations.

Original work is temporarily All Rights Reserved. Upstream template material retains its MIT license in TEMPLATE_LICENSE.txt.
