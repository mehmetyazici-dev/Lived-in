# Lived-In — Phase 1 Gameplan

Last updated: 2026-10-10

## Goal

Make singleplayer Minecraft feel inhabited through three small scenes the player can observe while exploring:

1. A campfire with a few NPCs gathered around it, including one playing an instrument.
2. An NPC fishing beside a body of water, with their pet cat nearby.
3. An NPC riding in a boat.

Start with these simple encounters and make them convincing. Larger settlements and more ambitious systems can follow once the basics work.

**Current state:** milestones 1–5 are complete. Travelers persist near their saved activity spots. The camp has a lute musician and two companions; the fishing scene has a rod-holding fisher and a sitting pet cat; the boat scene has a seated rider on a short return route. Combined verification passed with three copies of each scene, repeated unloads and saves, and the built JAR in a separate NeoForge instance. Technical details and verification history live in [Lived-in-Development.md](Lived-in-Development.md).

## Design priorities

- Give each NPC an activity the player can recognize at a glance.
- Fit Minecraft's blocky appearance, scale, and world.
- Use restrained movement and sound; preserve quiet stretches of wilderness.
- Keep encounters optional and allow players to linger or move on.
- Reuse vanilla blocks and suitable game systems where practical.
- Keep simulation local to loaded areas. Avoid keeping chunks loaded just for ambience.

NPC dialogue is outside Phase 1. Greetings, conversation screens, trading, quests, and other deliberate player interactions belong to later work. Structure the NPC code so interactions can be added later without building those systems now.

## Milestones at a glance

| Milestone | Status | Result |
| --- | --- | --- |
| 0. Working foundation | Complete | Buildable starter, development client, debugger, and local Git |
| 1. Basic ambient NPC | Complete | Traveler Spawn Egg, bounded idle movement, and verified persistence |
| 2. Campfire and musician | Complete | Three travelers, a lute musician, and verified music/persistence lifecycle |
| 3. Fishing NPC and cat | Complete | Visible fishing cycle, linked sitting cat, and verified interruption/persistence |
| 4. Boat rider | Complete | Real vanilla passenger, bounded return route, obstacle stopping, and verified persistence/recovery |
| 5. Combined scene verification | Complete | Nine scenes coexist; identities/audio survive revisits and saves; packaged JAR verified |

“Complete” means the described behavior was implemented and verified. Update this table as the project develops.

## Milestone 0 — Working foundation

Completed: Minecraft 1.21.1 with NeoForge 21.1.256, Java 21, IntelliJ IDEA, a minimal Lived-In mod, a successful build, a singleplayer save/reload check, and a working debugger breakpoint. Local Git uses the main branch.

This proves the project can load; it does not mean gameplay scenes already exist.

## Milestone 1 — Basic ambient NPC

Create the shared starting point for the three scenes: an NPC that renders correctly, can remain near an assigned activity spot, and persists in a saved world. The implemented placement methods are the Traveler Spawn Egg in the Spawn Eggs creative tab and /summon livedin:ambient_npc.

Begin with a simple appearance. Establish basic movement, looking, collision, and safe placement before investing in detailed clothing or animations.

**Done when:**

- We can place one NPC in the development world and see it correctly.
- It remains near its assigned scene and does not continually wander away.
- Saving, reopening, and unloading/reloading its chunk preserve it without creating duplicates.
- Its behavior is controlled by the game server, with the client displaying the result.
- The foundation can support scene-specific activities without separate copies of the entire NPC implementation.

Completed on 2026-10-09. The traveler uses a temporary Steve appearance, walks within a six-block activity area, looks at nearby players, and tries to return if displaced. The server owns its behavior. Its activity spot and vanilla identity are saved with the entity; it unloads normally with its chunk. Shared entity and goal classes provide the foundation for later activities.

Verification: five server GameTests passed, including 600 ticks of bounded movement, displacement recovery, passenger checks, and save/load identity. In the isolated Lived-In Milestone 1 world, the spawn egg and rendering worked; scene chunks unloaded and reloaded with one traveler; closing and reopening the world preserved the same UUID, activity spot, and one traveler. See the development guide for limits and test commands.

## Milestone 2 — Campfire with a musician

Build a small campfire gathering. Use three NPCs as the initial working target: one musician and two companions. Simple log seats, supplies, or shelters may support the scene, but a full campsite building system is unnecessary.

The musician should visibly hold and play an instrument. A short, original or appropriately licensed musical phrase should be audible nearby, quieter at a distance, and stop when the performance or scene stops. Choose the instrument, model, playing motion, and sound together during implementation.

The companions should appear to spend time around the fire through small idle movements, looking, or relaxed poses. Full day/night schedules can wait.

**Done when:**

- A player can identify the musician and understand that the group is gathered around the fire.
- The instrument, playing motion, and music agree about whether a performance is active.
- NPCs do not walk into the fire during their normal scene behavior.
- Music does not stack into multiple overlapping copies after moving away and returning.
- The gathering survives save/reload without duplicate NPCs or persistent orphaned audio.

Completed on 2026-10-09: /livedin camp places a lit fire, benches, supplies, one lute musician, and two companions on validated flat ground. Saved roles and activity spots, campfire clearance, a custom lute model/strumming pose, and a quiet original melody using vanilla harp notes are implemented. A dedicated drawing layer corrects the lute's placement against the supporting and strumming hands.

Verification: the final build and all eleven server GameTests passed, including group persistence, fire clearance, and performance start/stop conditions. The revised instrument was inspected from the front and side in the client. Extinguishing the fire stopped the pose and closed the music voice; relighting resumed one voice with active sound-engine playback. Leaving closed the voice, all four camp chunks unloaded, and returning restored exactly three members with the same identities and one musician voice. Saving to title cleared the voice; reopening preserved the same three identities and roles and resumed one voice.

Audio playback and cleanup were checked through the sound engine's diagnostic logs; subjective listening review of the melody and loudness remains polish work. No automatic camp respawning or natural generation is implemented.

## Milestone 3 — Fishing NPC with a pet cat

Place an NPC on suitable ground beside accessible water, holding a fishing rod and performing a readable cast, wait, and retrieve cycle. The line and bobber should make it clear that the NPC is fishing. Start with a visual activity; an inventory, loot economy, or realistic catch simulation is unnecessary.

Keep the NPC's pet cat nearby. Prefer a vanilla cat if it can support the intended behavior. Record the association between the scene, fisher, and cat so reloading does not create replacement cats. The exact ownership implementation is a development decision; do not assume vanilla taming automatically supports NPC owners.

Completed on 2026-10-10: `/livedin fishing <shore> <water>` places one rod-holding fisher and one sitting vanilla cat on a validated dry bank. The visual cycle prepares, casts, waits, retrieves, and rests. The line and bobber are drawn from synchronized NPC state rather than separate saved entities. Missing water, an obstructed casting corridor, or a missing rod pauses the activity. The saved cat association never creates replacement pets.

**Done when:**

- The fisher faces water and performs a repeatable fishing cycle.
- The cat stays nearby, with suitable sitting or idle behavior.
- The fisher and cat normally stay on safe ground.
- Missing water or an obstructed fishing spot causes the activity to pause or recover gracefully.
- Reloading preserves the association without duplicate cats or leftover bobbers.

Verification: the build and all seventeen server GameTests passed. The client showed the rod and arm poses, line, bobber, and sitting cat. Removing the target water or blocking the casting corridor hid the line and bobber; restoring the scene resumed casting. The fisher and cat occupied separate chunks, both of which unloaded before revisiting. Returning and saving/reopening each restored exactly one fisher and one cat with unchanged identities and references. There were no saved bobber entities to duplicate. Full pet navigation and fishing loot remain outside this milestone.

## Milestone 4 — NPC riding in a boat

Show an NPC as a real boat passenger. Use a vanilla boat where practical, with simple controlled movement through a small area of water so the encounter reads as a boat ride.

Focus first on boarding or spawning aboard, passenger alignment, movement, and persistence. Long river journeys, route networks, and sophisticated navigation belong to later phases.

**Done when:**

- The NPC is visibly mounted, with its seated hips inside the hull, and moves with the boat. Alignment stays correct during movement and after reloading.
- The boat can follow a short local route or bounded movement pattern on suitable water.
- Obstacles and shorelines cause the boat to stop or change course safely rather than continue pushing indefinitely.
- Save/reload preserves the intended boat/passenger relationship.
- If the boat is removed, the NPC recovers without crashes or endless replacement boats.

Completed on 2026-10-10: `/livedin boat <start> <end>` creates a real vanilla oak boat with a mounted traveler. It travels between two validated water positions, pauses and turns at endpoints, stops at unsafe water or obstacles, and saves its passenger and route. Losing the boat clears the route and attempts one nearby shore recovery; it never creates replacement boats.

Verification: the build and all twenty-five server GameTests passed, including 600 ticks of bounded travel, repeated turns, obstacles, missing water, saving the passenger tree, and removal/dismount recovery without replacements. Client checks confirmed the seated rider, moving hull/oars, obstacle stop and resume, both route chunks unloading, and world save/reopen. Returning and reopening preserved exactly one boat and one mounted rider with unchanged identities and route.

Seat-height correction on 2026-10-10: the original mounted body was too high. The entity now uses Minecraft's player vehicle attachment. Existing boat tests gained seat-height assertions for mounting, movement, and reload; the build and all twenty-five tests passed. Restarting the client and reopening the existing scene confirmed the original rider sits inside the hull during movement and turns, with unchanged boat/rider identities and one of each.

Short straight routes on prepared still water are the current limit; automatic boarding and river navigation remain deferred.

## Milestone 5 — Combined scene verification

Put the three scenes in a development test world and walk between them. Verify appearance, activity, sound, persistence, and behavior when a scene becomes unloaded.

**Done when:**

- All three scenes are recognizable and work independently in the same world.
- Repeated visits and save/reload cycles do not multiply NPCs, cats, boats, bobbers, or sounds.
- Several copies can run without an obvious sustained performance problem; record the scene count and test conditions.
- The built JAR passes a smoke test in a separate Minecraft 1.21.1 instance using the tested NeoForge version.
- The development guide records the implemented systems, how to test them, and known limitations.

Completed on 2026-10-10. A fresh isolated flat QA world ran three copies of each scene: three camps, three fishers with their sitting cats, and three mounted boat riders. Client inspection confirmed the activities together, including the corrected boat seat height. The build and all 25 server GameTests passed.

Three unload/revisit cycles each confirmed all eight scene chunks unloaded. Two save/reopen cycles, including a complete Minecraft restart, preserved the same 15 NPCs, three scene cats, three boats, pet references, and real boat passengers. Six recorded snapshots contained the same 21 entity UUIDs. There were zero saved fishing bobbers. Each musician had at most one music voice; leaving closed voices to zero, saving cleared them, and revisiting/reopening restored three voices with active sound-engine playback.

With nine scenes loaded, the integrated server ran 1,432 ticks in 71.57 seconds (20.01 ticks per second). Sampled client views showed approximately 97–100 FPS and 1.7–2.0 ms server ticks on this PC at 854 × 480, Fancy graphics, and 12-chunk render/simulation distances. This short flat-world check found no obvious sustained performance problem; large worlds, survival sessions, and modpacks still need later testing.

The separate instance used Minecraft 1.21.1 / NeoForge 21.1.256 and the actual `livedin-0.1.0-dev.jar` in its `mods` folder. It loaded through the production launcher path and passed the same combined checks. The development guide records the artifact checksum, setup, repeatable QA commands, test conditions, and remaining limits. Phase 1 is complete; natural generation and later features remain proposals for a future phase.

## Placement and future scope

For the first prototypes, use manually prepared scenes and development spawning. Natural world generation is a later milestone to plan after the three scenes are enjoyable and reliable. This keeps terrain placement and encounter frequency from blocking the first playable examples.

Future possibilities include player interactions, NPC dialogue, daily routines, travelers moving between locations, village-like structures, vanilla-style communities, human adventurers, and fantasy communities. Also defer quests, factions, settlement growth, custom economies, and additional loader/version support.

## Maintenance

Update this document whenever scene scope, milestone status, acceptance criteria, or priorities change. Record completed work accurately and keep proposed behavior marked as planned. Update the companion development guide when implementation or verification changes.
