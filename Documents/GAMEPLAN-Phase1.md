# Lived-In — Phase 1 Gameplan

Last updated: 2026-10-17

## Goal

Phase 1 is the implemented scene baseline. [GAMEPLAN-Phase2.md](GAMEPLAN-Phase2.md) now implements milestones 1–6: persistent profiles, the user's house, natural neighborhoods, night routines/defense, private barter, conversations, and unique invitations. These extend the original scene activities while preserving their identities and associations. Player-founded villages, paid construction, and next-dawn resident returns remain planned outside Phase 1.

Make singleplayer Minecraft feel inhabited through three small scenes the player can observe while exploring:

1. A campfire with a few NPCs gathered around it, including one playing an instrument.
2. An NPC fishing beside a body of water, with their pet cat nearby.
3. An NPC fishing forward from a boat for 25 seconds, retracting, then rowing to a random point on the same body of water.

Start with these simple encounters and make them convincing. Larger settlements and more ambitious systems can follow once the basics work.

**Current state:** milestones 0–7 are complete, so Phase 1 is complete. The campfire musician and companions, fisher with a cat, and mounted boat rider now appear naturally in suitable newly generated normal Overworld terrain. Travelers use focused activity components and persist with their scenes. The latest build and all 56 server tests passed; three recorded seed surveys found all scene types, and the packaged JAR preserved the selected natural scenes through unload/revisit and save/reopen. Technical details and verification history live in [Lived-in-Development.md](Lived-in-Development.md).

## Design priorities

Latest follow-up on 2026-10-17: natural lute camps now receive **one tenth of the preceding quarter-rate opportunities**, or **2.5% of the original camp opportunities**. The earlier reduction was to 25%; this is a further tenfold reduction. Fisher and boat opportunity grids stay unchanged; unsuitable/skipped camp sites can still become fishing scenes. Existing saved camps remain. Phase 2 adds persistent, equally likely sword/axe/bow choices, visibly armed camp companions, shore camping for boat riders, and owned-fire cleanup at dawn. Current verification is recorded in the development guide; the density surveys below describe earlier builds.

Follow-up requested on 2026-10-14 and verified on 2026-10-15: camps and shoreline fishers now have more placement opportunities and more practical terrain rules. Natural camps use a compact 7 by 7 layout and get first choice of suitable clearings. The original boat opportunity rate is retained, and boat riders pause to fish at route endpoints. All 51 tests passed. In matched 36-region areas, seed 42 changed from two camps/one fisher to 16 camps/25 fishers; seed 8675309 changed from two camps/no fishers to 17 camps/seven fishers. Initial milestone verification paragraphs below remain historical records.

Boat behavior follow-up verified on 2026-10-15: replaced endpoint shuttling with this repeating cycle: fish forward for 25 seconds, visibly retract for one second, then row to a random reachable destination. Prefer a destination at least ten blocks away; allow shorter trips in small ponds. Stay within connected loaded water, stop/replan around obstacles, and preserve existing boat/passenger identities. The initial placement command still validates two safe water positions. All 53 server tests passed. Packaged-client checks confirmed forward casting and random travel; saving/reopening retained the same seated boat/rider pair. Details are recorded in the development guide.

Shoreline correction on 2026-10-15: the fisher must stand directly beside exposed water, with the cat on nearby safe dry ground. The three-to-five-block bobber range no longer permits a dry gap before the shoreline. New scenes use this rule; older inland fishers receive one bounded attempt to move their original pair to a safe nearby bank when loaded. The build and all 56 server tests passed, including shoreline rejection and identity-preserving correction. Remaining limits and checks are recorded in the development guide.

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
| 4. Boat rider | Complete | Real seated passenger, forward fishing, random connected-water trips, obstacle stopping, and verified persistence/recovery |
| 5. Combined scene verification | Complete | Nine scenes coexist; identities/audio survive revisits and saves; packaged JAR verified |
| 6. NPC activity composition | Complete | Focused activity components own scene state/behavior; existing saves and all scenes verified |
| 7. Natural encounters in normal worlds | Complete | Sparse one-time placement in new normal Overworld terrain; three seed surveys and packaged-client persistence verified |

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

Completed on 2026-10-09: /livedin camp places a lit fire, benches, supplies, one lute musician, and two companions on validated flat ground. Saved roles and activity spots, campfire clearance, a custom lute model/strumming pose, and an initial quiet original melody using vanilla harp notes were implemented. A dedicated drawing layer corrects the lute's placement against the supporting and strumming hands.

Sheet-music update on 2026-10-10: the musician now plays the supplied twelve-bar melody at 92 BPM, using Minecraft's guitar sample. Its 64 notes include eighths, quarters, dotted quarters, and the ending half note; strumming follows each note's duration. The build and all 29 server tests passed, including four new melody checks. The updated packaged JAR passed client checks for a complete note sequence, strumming, fire stop/resume, and voice cleanup/restoration on save/reopen. This adds the requested music to the existing camp scene without changing Phase 1's scope.

Verification: the final build and all eleven server GameTests passed, including group persistence, fire clearance, and performance start/stop conditions. The revised instrument was inspected from the front and side in the client. Extinguishing the fire stopped the pose and closed the music voice; relighting resumed one voice with active sound-engine playback. Leaving closed the voice, all four camp chunks unloaded, and returning restored exactly three members with the same identities and one musician voice. Saving to title cleared the voice; reopening preserved the same three identities and roles and resumed one voice.

Audio playback and cleanup were checked through the sound engine's diagnostic logs; subjective listening review of the melody and loudness remains polish work. Milestone 7 later added natural placement; camps still never respawn missing members automatically.

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

The original straight return route has been superseded by the random-water follow-up below. Automatic boarding and long-distance river journeys remain deferred.

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

The separate instance used Minecraft 1.21.1 / NeoForge 21.1.256 and the actual `livedin-0.1.0-dev.jar` in its `mods` folder. It loaded through the production launcher path and passed the same combined checks. The development guide records the artifact checksum, setup, repeatable QA commands, test conditions, and remaining limits. This completed the original milestones 1–5; Phase 1 now also includes the planned milestones below.

## Milestone 6 — NPC activity composition

**Status: complete, verified on 2026-10-13.** Natural placement in milestone 7 can now use the reorganized NPC code.

Keep `AmbientNpc` as the one shared traveler entity. It should handle shared health, equipment, identity, basic movement, and connections to Minecraft. Give each traveler an activity object that owns the state and behavior needed for its current role. This is **composition**: the NPC has an activity, instead of containing every activity's details itself.

CampActivity owns fire-related state and behavior; FishingActivity owns the water target, cycle, and pet association; BoatActivity owns the boat association, steering, route, and recovery. TravelerActivity supplies ordinary movement rules. NpcActivity defines the common contract, and NpcActivities constructs activities and initializes their synchronized field definitions.

Minecraft still needs entry points for ticking, goals, saving, and synchronized entity data. Keep those connections explicit in the shared entity, with activity work delegated to its component. Moving fields alone is insufficient if the unrelated behavior remains concentrated in `AmbientNpc`.

**Done when:**

- Activity-specific state and behavior live in focused components; `AmbientNpc` coordinates shared entity responsibilities and delegates activity work.
- A common activity contract defines how an activity is assigned, updated, saved, restored, and stopped or changed. Adding another activity does not require another growing set of unrelated fields and role branches in the shared NPC.
- Rendering and sound obtain the current activity state through clear access points, and the server continues to own decisions.
- All three scenes retain their current behavior, including music cleanup, fishing interruption/pet handling, boat seating, route stopping, and lost-boat recovery.
- Existing saved scenes load with unchanged entity identities, roles, equipment, pet references, and boat/passenger relationships. Keep the registered entity ID and saved role IDs stable; verify compatibility with the earlier save fields.
- The existing 29 GameTests pass, with additional checks for activity switching/cleanup and loading the earlier save format where needed.
- Client checks cover the three scenes, unload/revisit, and save/reopen; the built JAR loads in the isolated production instance.
- The development guide and code rundown explain the implemented component responsibilities, callers, and control flow in simple terms.

This milestone reorganizes the existing activities. Dialogue, player lute-playing, and new scene types remain separate future work.

Verification: the build and all 36 GameTests passed, including the existing 29 checks and seven new tests for earlier save fields, unknown-role fallback, activity replacement, goal/performance cleanup, pet maintenance stopping, and releasing the original boat. The actual earlier QA save loaded in the separate production instance. Camp music/strumming, fishing rod/line/bobber/cat, and the moving seated boat rider were inspected. One unload/revisit cycle confirmed all eight scene chunks unloaded, and saving/reopening preserved the same 21 entity UUIDs and pet/passenger associations. Music reached zero on leaving, cleared on world exit, and resumed three distinct active voices. Comparisons against the milestone 5 identity baseline also passed. The guides now describe component responsibilities and control flow.

## Milestone 7 — Natural encounters in normal worlds

**Status: complete, verified on 2026-10-14.** The new-chunk hook places the existing encounters on suitable terrain in a normal, non-superflat Overworld.

Players should discover a campfire group on suitable dry ground, a fisher and pet cat on a safe bank beside water, and a mounted boat rider on a short safe water route. Start with new terrain generated in normal worlds. An existing world's newly explored terrain may also receive encounters; placing scenes into already generated areas is outside the initial target.

Four additional land candidate chunks are now selected per 128 by 128-block region, one in each 4 by 4-chunk cell. The original boat candidate per region is retained; extra land candidates cannot create extra boats. Overlapping candidates are processed once. An unsuitable candidate receives no encounter. Initial positions and props stay inside their owner chunk. Placement tries a camp clearing first, then a safe bank, then an initial boat corridor only where boat placement is eligible. It skips structure-referenced chunks and saves a processed marker on success or rejection. Terrain is not flattened and no ponds are created. Manual commands remain development tools.

**Done when:**

- Each of the three scene types can appear in freshly generated normal Overworld terrain without `/livedin` placement commands, test datapacks, or manual terrain preparation.
- Camps use safe ground and clearance, fishers/cats use valid banks and casting space, and boats have valid source-water routes and space for the rider.
- Placement does not overwrite existing structures or player builds, force distant chunks to load, or leave partial scenes when a candidate is rejected.
- Generation and scene initialization do not duplicate members when neighboring chunks generate, chunks reload, or the world reopens. Record enough placement state to prevent repeated initialization.
- Visiting or revisiting an encounter does not recreate dead or removed travelers, cats, or boats.
- Frequency and spacing are defined, documented, and checked across several recorded world seeds and terrain types; all three scenes remain discoverable at the chosen settings.
- Automated checks cover placement rules, unsuitable terrain, and duplicate prevention. Client checks in normal worlds confirm appearance, activity, unload/revisit, and save/reopen.
- The packaged JAR works in the isolated NeoForge instance with a normal world, and a short exploration/performance check records the conditions and results.
- Documentation explains the implemented generation hook, supported terrain, encounter frequency, test seeds, and any limitations.

Natural encounters use the same activities as manually placed scenes. Larger settlements, long river navigation, settlement simulation, and additional dimensions remain future work.

Verification: the final build and all 45 required GameTests passed. Three fresh Default-world surveys each checked 100 candidate regions. Seed 42 produced three camps, one fisher, and 31 boats; seed 20261013 produced two camps and 53 boats; seed 8675309 produced four camps and 27 boats. Camps fitted gentle terrain in swamp, plains, savanna, and meadow areas without flattening it. Every structure-referenced candidate was skipped, and repeated processing was rejected. Fishing is rare under the strict dry-bank/casting requirements.

The final packaged JAR showed camp music/strumming and ground alignment, fishing with a sitting cat, and a correctly seated moving boat rider in a copy of the naturally generated seed-42 world. A fresh client-created Default world independently generated fishing and boating encounters; final-JAR exploration also generated encounters in new terrain. The selected three scenes preserved all seven entity UUIDs and pet/passenger references through one verified three-chunk unload/revisit cycle and save/reopen. Music returned to zero on leaving, cleared on world exit, and resumed one voice for the original musician. A 123.35-second exploration check measured 2,468 server ticks (20.01 ticks per second). The development guide records the artifact, commands, conditions, and limitations.

## Placement and future scope

Milestones 1–5 established manually placed scenes, milestone 6 reorganized their activity code through composition, and milestone 7 added verified natural encounters in normal worlds. Camps allow terrain within one block of their center and fit members/props to local ground height; steeper ground and obstructed areas are skipped. This completes the current Phase 1 scope.

Future possibilities include player interactions, NPC dialogue, daily routines, travelers moving between locations, village-like structures, vanilla-style communities, human adventurers, and fantasy communities. Also defer quests, factions, settlement growth, custom economies, and additional loader/version support.

## Maintenance

Update this document whenever scene scope, milestone status, acceptance criteria, or priorities change. Record completed work accurately and keep proposed behavior marked as planned. Update the companion development guide when implementation or verification changes.
