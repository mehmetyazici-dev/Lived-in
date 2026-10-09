# Lived-In

A Minecraft 1.21.1 / NeoForge mod foundation for a more inhabited singleplayer world.

**Current state:** a minimal entry point and startup message. NPCs, camps, routines, and world generation are design proposals, not implemented features. Read [the travelers' campsite concept](docs/concept.md).

## Pinned development stack

| Component | Version |
| --- | --- |
| Minecraft Java Edition | 1.21.1 |
| NeoForge | 21.1.256 |
| Microsoft OpenJDK, Windows x64 | 21.0.12.1+1 LTS |
| IntelliJ IDEA | 2026.2.3 (262.10968.63) |
| Gradle wrapper | 9.2.1 |
| ModDevGradle | 2.0.148 |
| Parchment mappings | 1.21.1 / 2024.11.17 |
| Foojay resolver plugin | 1.0.0 |

Based on [NeoForge's official 1.21.1 template](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle), commit `ad911709f06d3bdd5b1a500023a9156999216351`. The wrapper and template license are retained.

## Local setup

Portable tools are installed under `.tools`, excluded from Git. The JDK is in `.tools/jdk-21.0.12.1+1`; IntelliJ is in `.tools/idea`. Both downloads were checked against the vendors' SHA-256 checksums.

In PowerShell, from `C:\Lived-in`:

```powershell
. .\scripts\dev-env.ps1
java -version
javac -version
.\gradlew.bat --version
.\gradlew.bat build
.\gradlew.bat runClient
```

Dot-source the setup script in each new development terminal. It selects Java 21 for that terminal and places Gradle's cache in `.gradle-user-home`. A plain terminal may still resolve the previously installed Java 8. Do not use Java 8 to build this project.

The build output is `build/libs/livedin-0.1.0-dev.jar`. The development client runs in the project's `run` directory; its saves are disposable and ignored by Git. It does not use your regular `.minecraft/saves`. Initial setup needs internet access and can take time while Minecraft sources are prepared.

On another PC, install a 64-bit JDK 21 from [Microsoft](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21), set `JAVA_HOME` and prepend its `bin` to PATH, then use the wrapper. The local setup script expects the specific portable JDK path above; it is optional on other machines.

## IntelliJ and debugging

Run `.\scripts\open-idea.ps1` from PowerShell, or open `C:\Lived-in` in `.tools/idea/bin/idea64.exe`. The project is already imported on this PC. Use IDEA's free Java/Gradle features.

- Project SDK: Microsoft JDK 21 at `C:\Lived-in\.tools\jdk-21.0.12.1+1`.
- Gradle JVM: the same JDK 21. Use the Gradle wrapper.
- Gradle user home: `C:\Lived-in\.gradle-user-home`.
- Reload the Gradle project after editing build files or dependency versions.
- Minecraft and NeoForge sources are available through External Libraries after sync.
- Open `src/main/java/dev/livedin/LivedIn.java` and set a line breakpoint on `LOGGER.info(...)` in the constructor. Select the generated client run configuration and use **Debug**. Execution should pause before the title screen; resume to finish loading.

IntelliJ runs on its bundled JetBrains Runtime; the project's Java 21 setting is separate from the IDE runtime.

## Verification record

Verified on 2026-10-09 (Europe/Berlin):

- Java and javac report Microsoft 21.0.12.1; the VM is 64-bit.
- Gradle 9.2.1 uses the local Microsoft JDK for both launcher and daemon.
- `gradlew.bat build` passed; output is `build/libs/livedin-0.1.0-dev.jar`.
- `gradlew.bat runClient` launched Minecraft 1.21.1 with NeoForge 21.1.256.
- The Mods screen shows **Lived-In 0.1.0-dev**, mod ID `livedin`, the intended description, and All Rights Reserved.
- Created **Lived-In Smoke Test**, entered the world, saved to title, reopened the same world, and saved again. The client exited normally.
- IntelliJ imported the Gradle project. Both project SDK and Gradle JVM are explicitly set to SDK `21`, pointing to the local Microsoft JDK.
- IntelliJ's generated **Client** debug configuration stopped in `LivedIn.java:14` inside the constructor. The temporary breakpoint was removed and execution resumed.
- Opened Minecraft's `net.minecraft.world.entity.npc.Villager` as `Villager.java` with source documentation in IntelliJ.
- Git excludes tools, downloads, caches, build output, IDE settings, and development saves.

The starter has no gameplay tests because it has no gameplay behavior. A dedicated-server smoke test and modpack compatibility testing remain future work. The current verification covers the requested singleplayer foundation.

If a Minecraft class initially appears as a decompiled `.class`, use **Attach Sources**. The generated source archive is `build/moddev/artifacts/neoforge-21.1.256-sources.jar`.

IntelliJ's unified distribution displayed an automatically active Ultimate trial on first launch. This project only uses the free Java/Gradle capabilities; no subscription or account was purchased or created.

## Boundaries

Single target: NeoForge 1.21.1. Forge and other Minecraft versions require later ports. This starter has no custom packets, registries, configuration, public extension API, or gameplay tests. Validate the foundation with a build and client smoke test; add behavior tests when gameplay is introduced.

The project is local and unpublished. Original work is temporarily All Rights Reserved; upstream template material retains its MIT license in `TEMPLATE_LICENSE.txt`.
