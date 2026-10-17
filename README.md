# Lived-In

A Minecraft 1.21.1 / NeoForge mod for a more inhabited singleplayer world.

**Phase 1 is complete:** compact lute camps, fishers with cats, and seated boat riders appear naturally in suitable new Overworld terrain. Lute camps now receive one tenth of the preceding quarter-rate opportunities (2.5% of the original opportunities); fisher and boat grids remain unchanged. Boat riders fish forward for 25 seconds, retract, then explore random connected-water points. Fishers stand directly beside water. See the guides for placement rules, code responsibilities, and verification history.

**Phase 2 milestones 1–6 are complete:** the user's Starter House supports all wool colors; natural neighborhoods, night routines, saved sword/axe/bow groups, private barter progress, introductions/stats, and unique invitations now work. Camp companions visibly carry weapons; boat riders camp ashore at night and return to their original boats at dawn. Temporary fires are removed at dawn. Build and **87 server tests** passed, with client checks recorded in the development guide. Player beacons, invitation plots, and paid house construction remain the next milestones.

## Project documents

- [Phase 1 gameplan](Documents/GAMEPLAN-Phase1.md): scene ideas, milestones, acceptance criteria, and future scope.
- [Phase 2 gameplan](Documents/GAMEPLAN-Phase2.md): completed house handoff, milestones 1–6, checks, and the remaining village-building work.
- [Development guide](Documents/Lived-in-Development.md): NeoForge, the mod's entry point, build/loading workflow, toolchain, and verification.
- [Phase 1 code rundown](C:/Lived-in/Documents/Code-Rundown-Phase1.md): callers, responsibilities, control flow, flowcharts, and a linked glossary.

These documents are updated alongside project improvements. The workflow is recorded in [AGENTS.md](AGENTS.md).

## Quick start

```powershell
cd C:\Lived-in
. .\scripts\dev-env.ps1
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

Double-click **Open-IntelliJ.cmd** in the project folder, or run .\scripts\open-idea.ps1. The current artifact is build/libs/livedin-0.1.0-dev.jar. See the development guide for installation paths, supported versions, and packaging details.

Create a normal **Default** world or explore new terrain in an existing normal world to discover encounters. Existing generated chunks and superflat worlds receive no natural scenes. For controlled development checks, use the Traveler Spawn Egg or operator commands `/livedin camp`, `/livedin fishing <shore> <water>`, and `/livedin boat <start> <end>`. The development guide explains coordinates and terrain requirements.

Original work is temporarily All Rights Reserved. Upstream template material retains its MIT license in TEMPLATE_LICENSE.txt.
