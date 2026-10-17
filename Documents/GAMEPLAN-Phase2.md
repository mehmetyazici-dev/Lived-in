# Lived-In — Phase 2 Gameplan

Last updated: 2026-10-17

**Status: milestones 1–6 complete, verified 2026-10-17.** The user approved one starter house as sufficient for Phase 2. Profiles, natural homes, night routines, defense, private barter, conversations, and unique invitations now exist. Verification and limitations are recorded below and in the development guide. Player villages, beacon crafting, invitation plots, paid construction, and resident resurrection remain milestones 7–10.

## Goal and stopping point

Let the player meet a traveler, build a relationship through barter, invite that same person into a village, and provide a house that appears the following day.

Phase 2 also gives eligible travelers a life outside the player's village: some own homes, others make a camp at night, and they can defend themselves. Nearby naturally housed travelers should form small groups of houses.

Phase 2 includes the chosen house being built, the original invited NPC moving in, and residents returning at the next dawn if killed. Companions, village upgrades, and house upgrades belong to **Phase 3**. We will discuss house growth when we reach house design; this phase prepares for it but does not implement growth.

## The intended player experience

1. Find an eligible NPC and give them valued items from the accepted list. Each accepted item earns a consumable food item in return; building blocks are not accepted.
2. After the first accepted item, a progress bar appears above that NPC. It shows that player's progress, reaching the invitation threshold at 20 item units, whether all the same kind or different kinds.
3. Once the threshold is reached, right-clicking opens a conversation screen. The NPC introduces themselves and displays their randomized, saved stats.
4. Ask the NPC to join. They agree and give an item named **"[NPC's Name]'s invite"**, showing their head with a key over it.
5. Craft and place a **Village Beacon**: a silver version of the normal village bell. It establishes a 180 × 180-block village with the bell at its center.
6. Place the invitation inside the village to reserve a 20 × 20 living plot for that NPC. The whole plot must fit inside the village.
7. Right-click the placed invitation, choose a house, and fill four side-by-side slots: 64 matching planks, 64 cobblestone, 16 iron ingots, and 4 matching wool.
8. Confirm the choice. Materials are committed, a construction sound plays, and construction is scheduled.
9. At the next dawn, the chosen house appears and the same NPC moves in, provided the site and NPC are available. Unloaded sites wait for loading.
10. A resident who is killed returns at the next dawn within their own 20 × 20 home plot.

## Rules from the requested design

### Homes, nights, and exceptions

- Each eligible NPC gets a **50% chance of having their own home**. Save the result once; do not reroll it on every night, visit, or reload.
- At night, housed land NPCs return home. Homeless NPCs place a temporary campfire and carry their assigned weapon. Boat riders go ashore and camp even if they have a natural-home claim; at dawn they return to their original boat.
- The existing lute camp is an exception: its musician and two companions do not receive the random-home or new nightly-camp behavior.
- The lute camp's two companions always visibly carry their assigned weapons and defend the group. Preserve the musician's musical role.
- Exclude the lute camp group from village recruitment. Future NPC types must be able to opt out independently of their daytime activity.
- Fisher/cat and boat-fisher scenes must work with the new routines. Decide how the cat follows and where a boat rider docks before marking night routines complete.

### Houses and natural neighborhoods

- The user's existing **Starter House** is sufficient for the rest of Phase 2. Additional designs can be added to the catalog later without replacing saved homes or NPC identities.
- Eligible NPCs assigned homes should have real houses, with a saved entrance and resting location.
- Neighborhood centers occur approximately **once every 700 blocks**. Eligible NPCs falling within a center's area have a one-time **50% chance** of a house in that neighborhood. This is spacing between centers, not a 700-block membership radius.
- Houses belonging to one neighborhood sit next to each other, with distinct plots and access paths. Exact coverage regions, terrain adjustment, house spacing, and capacity remain design choices for milestone 3.
- Natural neighborhoods and player-founded villages are separate concepts. A neighborhood is not automatically a player-owned village and needs no player beacon.
- Future house growth must be possible without replacing the resident's identity. Growth rules and upgrade costs are Phase 3 discussions.

### Recruitment, village, and construction

- Barter returns one dropped consumable food per valued item unit. Accepted items are emeralds, diamonds, gold ingots, and iron ingots. Rewards are bread, apples, baked potatoes, cooked cod, cooked salmon, or cooked beef, with equal chances. Item tags make these lists editable through datapacks; building blocks remain rejected even if tagged.
- Twenty valued item units unlock conversation and an invitation request. Inputs can be the same kind or different kinds. Progress belongs to each player/NPC pair and is never shared. The progress bar starts after the first accepted item.
- Names, personal descriptions, and randomized stats remain stable after saving and reopening.
- Invitations identify an individual NPC, not just an NPC type or appearance. Only one invitation is issued for that person; losing or destroying it does not allow another to be issued.
- The beacon looks like a normal village bell with a silver appearance and establishes a 180 × 180 area. Its recipe is a bell in the center with **eight iron ingots** around it.
- A placed invitation reserves a 20 × 20 plot completely inside that area.
- The construction screen offers house types, material inputs, a wool color choice through the supplied wool, and a confirm button.
- The four input slots sit side by side. Costs are **64 wood planks of one kind, 64 cobblestone, 16 iron ingots, and 4 wool of one color**. Mixed plank types and mixed wool colors cannot fill their respective requirements.
- Selected wool changes designated wool, accents, and other colored parts of the chosen house. Design a suitable palette for each color as well as the available blocks allow, rather than recoloring every block indiscriminately. Plank type is an accepted payment choice; automatically changing the house's wood palette is not part of the agreed scope.
- Construction plays a sound on confirmation. The house appears the following day, with the original NPC assigned to it.
- Design village sizes as three tiers: **180 × 180 now**, then **210 × 210** and **240 × 240** in Phase 3. Only the first tier is available in Phase 2.

### Defense and death: confirmed decisions

- Updated 2026-10-17: armed NPCs have three equally likely, saved weapon groups. Iron swords use **Sharpness III / Knockback I**; iron axes use **Sharpness IV**; bows use **Power III / Punch I**. Every defensive weapon is indestructible. This replaces the earlier all-bow rule, including for lute companions. Equal chances do not guarantee one of each in every small group.
- NPCs drop nothing when killed, including held equipment. Plan for no death loot or experience reward. Barter food and construction refunds remain deliberate interactions, not death rewards.
- NPCs retaliate against attacking players, except that a player's own villagers do not retaliate against that player. How a resident responds to other players remains a separate permissions decision.
- An NPC who dies before joining cannot join afterward. An issued invitation becomes unusable; do not replace that NPC with a lookalike.
- Once a resident has joined, being killed schedules their return at the next dawn within their assigned 20 × 20 home field. Keep their name, stats, relationship progress, and village membership. This is resurrection after death, not immunity to damage.
- **Working interpretation to settle before implementation:** joining happens when the house is ready and move-in completes. Accepting an invitation or confirming construction alone does not yet grant resident resurrection.

### Invitations, refunds, and time: confirmed decisions

- Each NPC has one unique invitation. If it is lost or destroyed, no replacement can be requested. Dropping an intact invitation does not itself invalidate it; it can still be picked up. An issued-invitation record prevents reissuing without needing to guess whether an item has been lost.
- Before **Confirm**, closing the house UI returns all deposited items and schedules no building. Reopening does not count as confirmation.
- Confirming saves the exact supplied materials for that construction request and plays the construction sound.
- After confirming, breaking the invitation marker **before the next dawn** cancels the request, releases the plot, and drops **the same invitation and all committed resources**. Preserve plank type, wool color, and other item details. Replacing it and confirming again creates a new request with a new next-dawn due time.
- At dawn, the pre-dawn cancellation window ends even if construction is waiting for the area's chunks to load. The server decides the order of breaking a marker and processing dawn so the same request cannot both refund and build.
- Use Minecraft world time, not real time. Sleeping through the night counts. A confirmation just before dawn can finish shortly afterward; confirmation at/after a dawn targets the following dawn.
- The normal case is construction at the next dawn. Unloaded or obstructed sites wait safely; they do not overwrite player changes. The refund process for a blocked job after dawn is still to be settled.

## Accepted stats and introductions

The user accepted these five stats on **2026-10-16**. Milestone 1 rolls and saves them. Health, strength, and walking pace set real entity attributes; marksmanship and craftsmanship are saved informational scores with no gameplay effects yet. The conversation/stat screen belongs to milestone 6. A default is the baseline used when a saved stat is absent. Player comparisons describe an ordinary vanilla player without equipment or effects.

| Stat | Accepted NPC range | NPC default | Player comparison | Why it could be useful |
| --- | --- | --- | --- | --- |
| Health | 16–30 health points (8–15 hearts) | 20 points / 10 hearts | 20 points / 10 hearts | How much damage the NPC can take before falling; still relevant for residents who return the following dawn. |
| Strength | 1–4 base melee damage points | 1 point | 1 point of base unarmed attack damage | A simple future companion combat distinction. Weapons, armor, and other modifiers change actual damage. It does not multiply the approved bow's damage. |
| Walking pace | 90–110% of ordinary player walking pace | 100% | 100% reference, excluding sprinting | Distinguishes brisk and leisurely travelers without huge differences. Calibrate actual movement; mob movement settings are not interchangeable with the player's raw speed attribute. |
| Marksmanship | 0–100 skill | 50, ordinary skill | No vanilla numeric stat; the player aims manually | A possible future measure of accuracy and shot timing. Keep it informational in Phase 2 unless its combat effect is separately agreed. |
| Craftsmanship | 0–100 skill | 50, ordinary skill | No vanilla numeric stat; crafting is recipe-based | A future village-job or upgrade specialty. Informational in Phase 2: it does not change the approved costs or next-dawn construction time. |

Health, strength, and pace connect to actual attributes. Registering strength does not add attack goals or defensive bows; combat behavior remains milestone 4. Walking pace describes level-ground walking at a goal speed of 1; strolling intentionally uses a slower goal. Boat rowing still has its own speed. The two skill scores belong to this mod and have no equivalent default player score. A score of 100 does not automatically mean twice as effective as 50; that relationship would need its own rule.

Values are rolled once with a triangular distribution: values near the baseline are more common than extremes. The entity UUID seeds initial generation; the resulting values are then saved explicitly. Reloading and changing activities do not reroll them. Future dialogue, invitation cancellation, and resurrection must also preserve them. Player health and base attack references were checked against the project's pinned Minecraft 1.21.1 sources; walking pace was checked with actual movement in a GameTest.

Profiles now save a name, a short background, and one personality detail. A future conversation can add the current occupation: “I'm Rowan. I fish these waters. I used to live near a spruce forest, and I prefer quiet mornings.” These are flavor and identity; they do not imply quests, factions, or a full personality simulation. Existing custom names are preserved during migration; later name-tag overrides remain vanilla display names without rerolling the underlying profile.

## Remaining choices at the relevant milestones

The recipe, four slots, costs, barter counting, private progress, bow enchantments, and pre-dawn cancellation rules above are decided. Do not reopen them as unresolved requirements.

| Choice | Proposed starting point / boundary |
| --- | --- |
| Valued items and foods | Implemented lists are recorded above. Each accepted unit gives one food item. Future balance changes can edit the tags. |
| Barter controls and stacks | Implemented: main-hand right-click offers one item before unlock. After 20, normal right-click opens dialogue; sneak-right-click continues bartering. Progress caps at 20. No ground-item pickup. Survival consumes offered items; Creative retains items through vanilla Creative interaction behavior. |
| Future skill effects | The five stats/ranges are accepted. Marksmanship and craftsmanship remain informational until separate effects are designed. |
| Neighborhood coverage | Implemented 700 × 700 regions. A saved center may shift up to 48 blocks on each axis to find a clearing. Up to six 20 × 20 houses occupy a compact 3 × 2 layout with four-block gaps. Unsuitable terrain can leave requests pending. |
| Night details | Implemented dusk at time 12000 and dawn at 0. NPC-owned temporary campfires are removed without drops at dawn. Cats follow on foot; boat riders seek a reachable loaded shore and reuse their boat. Blocked/unloaded routes wait or camp safely. |
| Ownership | Recommend only the invited player can claim that invitation, with the first successful invitation request reserving the NPC. Decide transfers, other-player retaliation/permissions, overlapping villages, and bell removal. Per-player barter progress remains private regardless. |
| Joining and recovery | Confirm the move-in boundary for resurrection. Decide post-dawn blocked-build refunds, pre-move-in death with funded construction, and unavailable/destroyed home plots. None may revive a traveler who died before joining. |

## Milestones at a glance

Milestones 1–6 are implemented. Milestone 0's decisions needed for these features are settled; remaining village permissions and construction recovery decisions belong to milestones 7–10. The current starter house is imported, supports all wool colors, and is the sole Phase 2 design for now.

| Milestone | Deliverable | Depends on |
| --- | --- | --- |
| 0. Agree on rules and data ownership | Resolve immediate choices and define who owns each saved record | This plan |
| 1. NPC identity and eligibility | Complete: persistent names, stat rolls, home request, and independent eligibility rules | Identity decisions in 0 |
| 2. House design and import workflow | Complete: Starter House, 16 wool colors, all rotations, extensible catalog | 0 |
| 3. Natural homes and neighborhoods | Complete: fresh-terrain homes, saved centers, claims, and compact grouping | 1, 2 |
| 4. Night routines and defense | Complete: home returns, temporary camps, saved sword/axe/bow groups, cats, and boat shore camping/resumption | 3 |
| 5. Barter and relationship progress | Complete: private progress, one food per item, and overhead bar | 1 |
| 6. Conversation and personal invitations | Complete: saved introduction/stats UI, unique owned invitation, death invalidation | 5 |
| 7. Village Beacon and territory | Craftable silver bell and a saved 180 × 180 village boundary | 0 |
| 8. Living plots and house selection | Placed invitations reserve valid 20 × 20 plots and open construction UI | 2, 6, 7 |
| 9. Construction, move-in, and resident return | Exact material refunds, next-dawn building, color, move-in, and resident resurrection | 8 |
| 10. Phase 2 integration and release checks | One complete, verified village-building loop with Phase 1 preserved | 4, 9 |

The user authorized finishing milestone 2 and implementing milestones 3–6 on 2026-10-17. Milestones 7–10 remain planned.

## Milestone 0 — Agree on rules and saved records

Use the confirmed rules above. Settle the remaining choices before implementing their affected feature; the exact valued-item list, stats, and recovery edge cases can be finalized at their relevant milestones.

Define separate records for a person's identity, relationship progress, home, neighborhood, village, living plot, invitation, construction request, and pending resident return. A record is simply the information saved about one thing. Link them with stable IDs so a reload cannot turn one person into two residents. Resident information must remain saved even while their entity is absent after death.

**Done when:**

- Eligibility and camp exceptions are explicit, including which existing roles can barter, be invited, own homes, and defend themselves.
- Rules distinguish a natural home from a player-village home, and a home assignment from an actual usable house.
- Invitation ownership, village ownership, and overlapping village policy have a documented default. There is one invitation per NPC, even when several players reach 20 items independently.
- Lost invites, death before joining, death after joining, closing the UI, and pre-dawn cancellation have distinct outcomes. The joining boundary and outstanding recovery cases are settled before affected code is written.
- Data ownership is clear: the server decides outcomes; screens request actions and display the result.

## Milestone 1 — Persistent NPC identity and eligibility

All travelers now receive a saved profile: name, introduction choices, accepted stat rolls, and independent permissions. Eligible travelers roll a one-time 50% natural-home request now. Milestone 3 now binds that saved request to a neighborhood when a suitable fresh site exists; profile generation itself does not place a house. Existing NPCs receive missing profiles once without changing their UUID, equipment, cat, or boat association. Moving or changing activity does not reroll home ownership or claim another house.

Separate “what this NPC is doing” from “what this NPC is allowed to do.” A fisher may own a home and be recruitable while still using FishingActivity during the day. A lute companion can use defense without gaining recruitment or nightly relocation.

The 50% roll is a request to assign a real natural home. Successful rolls mean **awaiting a home** until the world registry assigns one; unsuccessful rolls mean **no natural home**. Camps are **excluded**. The world registry prevents duplicate claims. Milestone 4 supplies the night routine while preserving daytime activities.

| Initial scene | Natural home / recruit / barter / nightly camp | Defense permission |
| --- | --- | --- |
| Traveler, shore fisher, boat rider | All enabled | Enabled |
| Lute musician | All disabled | Disabled |
| Lute companion | All disabled | Enabled |

These permissions now control the housing, barter, and defense features below. They are saved separately from the current activity. Changing a camp companion's activity does not make that person recruitable. Future NPC types can set each permission independently; the initial camp policy excludes barter as well as recruitment.

Implementation: `AmbientNpc.getProfile()` initializes on the first server tick or save, after scene assignment. `NpcProfile`, `NpcStats`, and `NpcEligibility` own the data. Loading initializes older NPCs once and applies saved attributes without healing damaged NPCs. Health above a newly assigned maximum is clamped to that maximum. Fresh, undamaged NPCs begin at their rolled maximum health. No new networking or dependencies were added.

**Done when:** names, stats, eligibility, and the home roll survive reloads; camp exclusions are verified; older NPCs retain their identities; no duplicate home assignments arise from loading the same NPC twice.

**Historical milestone-1 verification, 2026-10-16:** `build` and all **64 required GameTests** passed. Eight new profile tests cover real entity serialization/reconstruction, saved names/stats/permissions/home requests, legacy fisher UUID/health/equipment/pet fields, boat passenger identity, role-change independence, pre-tick camp assignment, bounded rolls and both home outcomes, partial saved-data defaults, and calibrated walking pace. The original 56 scene checks still pass; the camp safety assertion now uses each person's rolled maximum instead of a fixed 20 health. No house claims existed at that point. Release packaging includes the profile classes and excludes GameTests. Log: `build/phase2-milestone1-check.log`. No new visual/client or terrain-density survey was run for this milestone.

## Milestone 2 — Design houses and prepare templates

Use the user's **Starter House** for this phase. A template is a saved arrangement of blocks the mod can place. [Its handoff record](House-Designs/Starter-House.md) contains the source corners, front, beds, accents, and imported files. `HouseCatalog` provides the design, colored template IDs, transformed entrance/bed coordinates, and placement. More designs can be added later; no additional house is required to finish Phase 2.

### How to make and hand over a house

1. Use a separate Creative design world with commands enabled. Mark a **20 × 20** plot on the ground and identify its front. Keep the entire house, roof overhang, steps, and permanent decorations inside it. Leave some usable entrance/outdoor space.
2. Build one complete starter house using vanilla blocks initially. Add an entrance and a bed/resting place. Avoid residents, pets, valuables in chests, or working command blocks inside the saved design.
3. Choose which wool blocks and other colored details should follow the four supplied wool blocks of one color. Use a consistent sample color, and list the exact parts to recolor. Prepare coordinated accents for each supported wool color, using the closest suitable available blocks. Decide the palette rather than replacing every block of that color automatically.
4. Tell me the house name, design-world folder, dimension, two opposite corners enclosing the full build, and which direction the front door faces. Include the bed and entrance coordinates and any foundation requirements.
5. Provide screenshots of the front, side, and interior to explain appearance. Screenshots supplement the world or template; they do not contain enough information to reproduce every block reliably on their own.
6. Preferred handoff: save the build with Minecraft's **structure block** tools and provide its exported `.nbt` structure file plus the notes. Alternatively, provide the isolated design save and coordinates so we can extract the structure together. The exact export steps are below.
7. Import a copy into the project's house resources, then test placement in an isolated world. Verify each allowed rotation, entrance, foundation, bed, footprint, and wool palette before calling the house ready.

Suggested note sheet for each house:

| Field | What to provide |
| --- | --- |
| House name | A readable name for the selection screen |
| Source | Design-world folder or exported structure file |
| Bounds | Minimum and maximum X/Y/Z enclosing all saved blocks |
| Front and origin | Front-door direction and which block anchors placement |
| Resident locations | Entrance, bed/rest spot, and an outdoor return point |
| Color choices | Wool and other details that should change with the supplied wool |
| Terrain needs | Flat foundation, permitted height adjustment, and any basement |
| Future growth | Reserved space or possible taller/larger versions to discuss in Phase 3 |

### Exact handoff on this PC

You build in Minecraft; I import and adapt the saved blocks. No modeling program is needed. Use an isolated **Minecraft Java 1.21.1 Creative world with commands enabled**, preferably superflat for easy building. The mod's homes are not implemented yet; this world is only a design workspace.

1. Build your first house inside a 20 × 20 footprint. Include the foundation, roof edges, steps, bed, and decorations inside the bounds. Use vanilla blocks. Leave chests empty and exclude NPCs, pets, armor stands, and other entities from the export.
2. Run `/give @s minecraft:structure_block` and place the block just outside the house's bounds. Right-click it and switch its mode to **Save**.
3. Enter a lowercase structure name such as **`livedin:houses/starter_cottage`**. Use letters, numbers, underscores, or slashes, without spaces.
4. Set **Relative Position** to the house's minimum X/Y/Z corner minus the structure block's coordinates. Set **Structure Size** to `maximum - minimum + 1` on each axis. For example, a structure block at `(100, 64, 100)` and a build from `(101, 64, 101)` through `(120, 79, 120)` use relative position **1, 0, 1** and size **20, 16, 20**. This includes a floor at Y=64. The 16-block height is just an example; use your actual build height.
5. Enable the bounding-box display and check that it includes the entire house while excluding the structure block. Set **Include Entities: OFF**. Click the actual **Save** action to write the file; simply closing the screen is not an export.
6. Find the design world's folder: from the Singleplayer menu, select that world, choose **Edit → Open World Folder**. Inside it, the example export is **`generated/livedin/structures/houses/starter_cottage.nbt`**. The exported folder is named `structures` (plural).
7. Give me the **absolute path to that `.nbt` file on this PC**, or attach the file in chat. Include the note sheet below and optional front/interior screenshots. You do not need to send the entire world if the export worked. If you prefer to skip export, save/close the isolated design world and provide its folder path and build coordinates instead.
8. I will copy the design into `src/main/resources/data/livedin/structure/houses/` (Minecraft 1.21.1 mod resources use `structure`, singular), define the entrance/bed and color palette, and test its placement and rotations before we approve it for use. Exporting alone does not make it available in the mod.

Copy and fill in this handoff note:

```text
House name: Starter Cottage
Export file (absolute path):
World / dimension:
Structure block X Y Z:
Minimum build corner X Y Z:
Maximum build corner X Y Z:
Front door faces: north / south / east / west
Door and bed coordinates:
Sample accent color:
Blocks/areas that should follow the supplied wool color:
Blocks/areas that should keep their original color:
Foundation or terrain requirements:
Optional screenshot paths:
```

The four wool are a fixed gameplay cost, not a limit of four colored blocks in the design. Mark all intended accents; I will map them to coordinated variants. Use ordinary air inside rooms. Any foundation, clearing of vegetation, or terrain adaptation will need placement rules when we import the house; the template by itself does not choose a safe site.

Even-sized plots need a precise convention: a 20 × 20 square cannot be perfectly symmetric around one block. Define the invitation marker's position, plot offsets, rotation, and build height envelope once. Show the actual outline before placement so players do not have to understand coordinate conventions.

**Done when:** the agreed starter designs import correctly, every placed block stays inside its plot and approved height envelope, color changes affect only designated parts, and all rotations remain usable. House growth is documented as future work, not implemented.

**Complete:** Starter House is 20 × 6 × 20, with south-facing double-door front, two beds, empty chests, and 14 carpet accents. The empty extra sponge column was omitted; all 237 original non-air blocks were retained. Base plus 16 variants are packaged. Minecraft placement checks cover all colors and rotations. The original design world remains intact. One design is the approved Phase 2 scope; growth remains Phase 3.

## Milestone 3 — Natural homes and nearby neighborhoods

Implemented in `Neighborhoods`: saved home requests belong to the NPC's first 700-block region, without rerolling. Fresh generation searches for a suitable center within 48 blocks of the region's nominal center. Homes use six distinct 20 × 20 plots in a 3 × 2 layout, four blocks apart. Existing saved requests receive priority; otherwise new local travelers receive their own saved 50% home roll. False rolls stay homeless, and unsafe/full regions leave other requests pending.

Centers, requests, processed opportunities, and home assignments are saved. Fresh chunk opportunities expire after 200 simulation ticks; construction requires the whole footprint to be freshly generated and loaded. This never revisits old explored terrain to build a house. The queue checks at most four center sites per pass and performs at most one house placement per ten ticks. Production never force-loads the region. Ordinary encounters leave room around potential neighborhood centers; their wider world-generation rules otherwise remain.

Placement rejects water, trunks/canopies, steep terrain, existing builds, and surface structures. A foundation fills natural low ground by at most three blocks without digging; grass and ferns may be cleared. Deep underground structures alone do not block a safe surface house. Missing templates or canceled NPC spawns cannot leave a half-house or an assigned missing resident. Home records survive NPC unloading. Long commutes use loaded local pathfinding steps; an unreachable home causes a safe night camp rather than distant chunk loading or teleporting to the house.

**Done when:** home rolls lead to usable houses where terrain permits; nearby houses form readable groups; homes and residents keep their IDs across saves; player builds and existing structures are protected; rejected sites create no half-house; returning home has a feasible travel plan rather than assuming a 700-block path will always work.

**Verification:** the normal Default-world survey with seed 0 generated a real home at (292, 66, -348) for its original saved traveler. Tests cover protected sites, canceled spawn rollback, persistent claims, negative regions, and grouped house allocation. Density depends heavily on terrain; centers in water, woodland, or steep land can have no homes. Earlier seed-42/8675309 trial runs exposed overly conservative placement checks and are not density guarantees.

## Milestone 4 — Night routines and defensive weapons

Add a shared daily schedule that temporarily takes control from the daytime activity.

- Housed travelers stop their daytime activity at dusk and return to their saved home/resting location. Restore their daytime activity after dawn when safe.
- Homeless eligible travelers find safe nearby ground, place one temporary campfire, and equip their saved sword, axe, or bow at night. Preserve their daytime rod or other equipment so it can be restored. Remove the owned campfire without drops at dawn, including after saving/reloading.
- Record which campfire this NPC placed. Reuse or clean up that temporary camp according to the agreed dawn rule, without leaving a new fire every night, deleting a player's fire, or producing an unlimited source of free items.
- Lute musicians and their companions keep their existing camp. The two companions visibly carry their assigned weapons by day and night; they do not build extra night camps or abandon the musician for randomly assigned homes.
- Defenders retaliate against attackers, including players, while a player's own villagers never retaliate against that player. Resolve other-player permissions and ally checks before implementation. Disable NPC death loot, equipment drops, and experience rewards; death must not become a source of enchanted bows.
- Resolve fisher/cat travel and boat docking explicitly. Stop casting before movement. Keep ownership of the original cat/boat, find safe landing points, and define a fallback if there is no route home.
- Paused/unloaded NPCs must not force distant terrain to load. On reload, resume from saved home/camp/schedule information appropriate to the current world time.

**Done when:** dusk/dawn and sleep-skipping work; housed land NPCs return and resume daytime roles; homeless NPCs camp and defend; boat riders land, camp nearby, then remount their original boats; temporary fires disappear at dawn; companions visibly carry weapons and protect the lute camp; all three weapon types have the correct enchantments and do not wear out; owner-retaliation exceptions and no-death-drop rules work; blocked routes and save/reload do not duplicate fires, weapons, cats, boats, or NPCs.

**Implemented:** `NpcRoutine` pauses daytime activities from world time 12000 until dawn, routes land residents to beds, and returns them to their original activity anchors. Temporary campfires carry the placing NPC's ID; dawn cleanup never removes an unowned/player replacement fire. Breaking an NPC-owned temporary fire yields no materials. A boat rider finds connected loaded water leading to a safe bank with campfire space, lands, camps nearby, and later remounts the same available boat. Without a reachable campsite they wait aboard and retry. Cats follow their fisher on foot and resume the original shore association afterward. Defense takes priority, uses stationary bow shots or safe melee pursuit, protects allied travelers/cats from friendly attacks, and restores reserved daytime equipment. Sword, axe, and bow choices have equal chances and are saved; camp companions always show their weapon. Survival attackers trigger retaliation; Creative/spectator players are ignored. The future resident-owner exception is saved but actual player-village membership is milestone 9. NPCs drop no death loot, equipment, XP, or collectible defensive arrows.

**Verification:** tests perform actual walking through the starter doors, sleeping, waking, returning, docking, and remounting at dawn. Other tests check equipment/camp ownership across reload, exact enchantments, real arrow shots, friendly-fire protection, and no death rewards. Blocked routes and unloaded/stolen boats wait safely; no replacement boat is spawned.

**2026-10-17 follow-up:** build and all 87 server tests pass after adding the three weapon groups and reducing natural lute-camp opportunities to 25% of their previous rate. Companion weapons are visible without a threat in the client. The extended boat test verifies a real shore fire, staying nearby after displacement, cleanup at dawn, and returning to the original boat, even with a natural-home claim. Existing natural camps remain; lower camp frequency applies to newly generated terrain. Detailed checks and limits are in the development guide.

**Later 2026-10-17 adjustment:** camps receive a further tenfold reduction: 10% of the preceding quarter-rate opportunities, or 2.5% of the original opportunities. Existing camps remain. The invitation key's ring/shaft overlap is removed in all three appearance models. Neighborhood generation is unchanged; the development guide now explains why its limited center search and ten-second fresh-chunk window can produce long stretches without houses. A wider search and persistent safe opportunities remain proposed improvements.

## Milestone 5 — Barter and the relationship bar

Implement an agreed list of valued items and consumable food rewards. Each accepted item unit is consumed, gives one dropped food item, and adds one progress point, up to the unlock threshold. Both repeated and varied accepted items count. Reject building blocks and all other unsupported offers without consuming them or producing food.

Show the bar above the NPC only after that player's first successful contribution. Each player sees only their own progress with that NPC; one player's 19 items plus another's one item unlocks neither player's conversation. Reach full progress at 20 accepted units. Settle the proposed one-unit-per-offer controls and further barter behavior before implementation.

**Done when:** every accepted unit gives one food item; repeated and mixed valued items count; building blocks are rejected; the bar appears at one item and reaches full at 20; progress survives saves and is private per player; repeated clicks or two simultaneous offers cannot double-count or produce unpaid rewards.

**Implemented:** item tags define the valued items and foods above. Each successful main-hand offer returns one food item targeted to the contributing player and adds one point, capped at 20. The overhead bar appears only after that viewer contributes. A clientbound payload sends only that viewer's progress and checks the NPC's identity; another player's progress map is never sent. Tests cover mixed/repeated units, unsupported blocks, food counts, independent players, and saved progress. Client interaction confirmed a food reward and progress update; threshold handling is covered by server tests.

## Milestone 6 — Conversation and the NPC's invitation

After the barter threshold, right-click opens the conversation UI. Show the saved name, a short personal introduction, and the agreed randomized stats. The player can ask the NPC to join their village.

Acceptance issues **"[NPC's Name]'s invite"**, with that NPC's head and a key overlay. Save the NPC ID, unique invitation ID, and agreed recipient restriction. Record issuance once for that NPC, across all players. A full inventory must not consume their one opportunity without delivering the item; require space before issuing it. Reopening the conversation cannot issue another, including after the original is lost or destroyed.

An invitation registry tracks whether the unique claim is an item, a placed marker, funded construction, completed membership, or invalid because the traveler died. Copies of an item must not create extra valid claims. Canceling construction returns the existing claim; it does not reset the NPC's issued-invitation record. A traveler who dies before joining stays dead, and their invitation becomes unusable.

**Done when:** only eligible NPCs unlock recruitment; names/stats remain consistent; the item identifies the correct individual even when two NPCs share a name or skin; issuance is unique across players/reloads; lost/destroyed invites cannot be replaced; a dead traveler's invite cannot recruit them; full-inventory failures do not burn the invitation opportunity.

**Implemented:** right-click after 20 opens a server-backed conversation with the saved introduction, occupation, five stats, and invitation button. A menu action rechecks eligibility, distance, living status, progress, inventory space, and the world's invitation registry. Each invite contains NPC, owner, and unique token IDs, carries the NPC's name, and shows a matching placeholder head with a gold key. Only the first eligible requester receives one; losing/destroying it never resets issuance. Death invalidates the registry claim. Tests cover independent players, full inventories, reload, invalid distance/menu actions, uniqueness, and death. Client checks cover the actual screen and issued item. Invites cannot be placed yet; village/plot mechanics are milestones 7–9.

## Milestone 7 — Village Beacon and territory

**Next planned work.** Milestones 2–6 finished with a successful build and **81 required server tests** on 2026-10-17, plus a normal-world home survey and client screen/item/reload checks. See the development guide for exact evidence and limitations. No milestone 7–10 implementation is included in this change.

Implement the beacon recipe: **one bell in the middle, eight iron ingots around it**. Use a silver bell appearance. Placing it creates a saved village record with an owner, dimension, beacon position, and tier-one boundary of **180 × 180** blocks.

Show an understandable boundary preview or outline. Define one consistent block-offset rule for even-sized regions, including negative coordinates. The beacon is the player's reference center; the precise one-block offset convention must be shared by previews and all placement checks.

Store the village tier separately from its computed size. Reserve the future sizes **210 × 210** and **240 × 240**, but provide no upgrade action in Phase 2. Agree on overlapping villages, moving/breaking a bell, and another player's permissions before enabling destructive changes.

**Done when:** crafting matches the approved recipe; placement creates one village; boundary checks and previews agree at every edge; records survive unloading and saves; replacing or breaking a beacon cannot accidentally duplicate or orphan villages and their reservations.

## Milestone 8 — Invitation markers, plots, and house selection

Placing an invitation turns it into an interactable plot marker tied to the original NPC. Preview its **20 × 20** living space and orientation. Validate the entire plot inside the village, not just the marker's block. Reject overlap with existing plots, the beacon, or protected construction space.

Right-click the marker to select a house. Show its appearance, orientation, required materials, and how the supplied wool affects the house. Use **four simple slots side by side**, ordered planks, cobblestone, iron ingots, and wool. Accept one plank type and one wool color per construction request.

The marker, reservation, and invitation represent one claim. Placing it transfers the inventory item into the marker's saved claim. Proposed unfunded-marker removal returns that same invitation and releases the plot. Closing the UI before Confirm returns every deposited item and builds nothing. Handle full inventories, disconnection, and a marker broken while the screen is open without losing or duplicating returns. The server checks that the player is allowed to use the plot and is still close enough to interact.

**Done when:** valid invites reserve exactly one plot; excluded/invalid/already-used invites fail safely; all corners and house blocks fit; overlapping plots fail; house previews match eventual placement; save/reload and repeated UI actions preserve the same reservation and deposited materials.

## Milestone 9 — Materials, construction, move-in, and resident return

Enable confirmation only with a selected house, valid plot, and the complete bill: **64 planks of one kind, 64 cobblestone, 16 iron ingots, and 4 wool of one color**. Reject mixed kinds/colors in their respective slots.

On confirmation, recheck the site, NPC eligibility, invitation, and payment on the server. Move the required item stacks out of the screen into the saved construction request once. Save their exact contents alongside the chosen template/orientation/color and next-dawn due time, then play the construction sound. Keeping these items reserved allows an exact refund. Give the player a clear pending-construction state; closing the screen after confirmation leaves this job queued.

Before the saved due dawn, breaking the marker cancels the job, releases the plot, and drops the same invitation plus the exact reserved stacks once. No house is placed. Serialize cancellation and construction on the server: a job can be canceled or built, never both. Canceling and re-placing the invitation is permitted; losing/destroying the returned item still prevents a replacement. Reopening, reloading, or breaking the marker repeatedly cannot repeat refunds.

At the next dawn, recheck the construction site and that the original NPC has not died before joining. If the relevant chunks/entities are unloaded, wait until they load; retain the original due time. Do not keep the whole village loaded for a building timer or interpret an unloaded NPC as dead. The pre-dawn refund window still ends at the saved due time. If the player has built something in the reserved space, pause and explain the obstruction rather than overwriting it or charging again. Agree on post-dawn blocked-job cancellation and pre-move-in death refunds before this milestone; neither may revive the dead traveler.

Place the full approved house with its selected colors, then assign it to the original invited NPC and mark the construction complete. Moving in must transfer that individual's home/village association, not summon a lookalike. If the NPC is unloaded, retain the pending transfer until it can be resolved safely. Settle relocation of an existing natural-home claim and a fisher's cat/boat associations before completion; do not erase the old house as an unnoticed side effect.

Once membership is established at the agreed joining boundary, save the resident profile independently of the live entity. Death queues one return at the next dawn. Restore that same resident's identity, stats, relationships, and home at a safe position inside their 20 × 20 plot. Check for an existing living instance before restoring it. If the plot is unloaded or has no safe position, keep the return pending until it becomes safe; do not create duplicates or silently choose another home. Define recovery for a permanently destroyed plot before completion. Resident resurrection does not automatically extend to their pet or boat.

**Done when:** confirmation charges once and sound plays once; closing before confirmation returns deposits; pre-dawn breaking returns exactly the original invite/resources once; sleep, reloading, unloaded plots, and blocked sites preserve the correct job state; the correct colored house appears once; the original NPC moves in and returns home at night; travelers who died before joining cannot move in; killed residents return once at the next dawn inside their own plot with saved identity/stats and no death drops.

## Milestone 10 — Complete-loop verification

Test in an isolated normal world and an older Phase 1 save. Run the entire player journey: barter 20 valued items for food, read the introduction/stats, accept the invite, craft/place the silver bell, reserve a plot, choose/color/fund a house, sleep, and find the original NPC moved in. Then verify a killed resident returns at the next dawn in their own plot.

Also check:

- Both home-roll outcomes, roughly 700-block center spacing, neighborhood grouping, night camps, and lute-companion defense.
- All three weapon groups and exact enchantments, indestructible weapons, visibly armed camp companions, no NPC death drops, retaliation against attackers, and no retaliation against the resident's owner.
- Repeated and varied valued items, rejection of building blocks, one food per accepted unit, private progress, and all required material types/amounts/colors.
- Two NPCs with the same display name, two plots, boundary corners, invalid placement, and invitation reuse attempts.
- Saving/reopening during barter, after issuing an invite, after reserving a plot, after payment, and after move-in.
- Unloading before dawn, returning after dawn, an obstructed plot, and distinguishing an unloaded NPC from a dead one.
- Closing before Confirm, exact pre-dawn refunds, cancel/re-place/reconfirm, full inventories, disconnections, and breaking at the dawn boundary. No request may both refund and build.
- Lost/destroyed invitations, no reissue, death before joining, and invitations copied in Creative mode cannot produce duplicate claims.
- Resident death before/after dawn, sleep-skipping, death/reload, unloaded/blocked home plots, repeated deaths, and no duplicate resident or rerolled profile after return.
- Two players trying to use the same NPC, marker, or construction inventory. Player-owned progress and permissions must not leak across users.
- Existing lute music, fishing/cat scenes, boat fishing/travel, natural generation, and cleanup still work.
- Release JAR contains required houses, textures, sounds, translations, and screens, with no test worlds or development helpers.

**Phase 2 is complete only when** the acceptance checks are verified, actual limitations are recorded, and the player can finish the whole loop using the packaged mod.

## Proposed code responsibilities

These are design boundaries, not classes that already exist. Retain the Phase 1 composition approach: the NPC holds focused objects that each own a small responsibility.

| Responsibility | What it should own |
| --- | --- |
| NPC profile and eligibility | Saved name/introduction/stats and independent permissions for homes, recruitment, barter, nightly camps, and defense |
| Relationship component | Valued-item progress per player, food exchanges, and conversation unlock state |
| Invitation registry | One issuance per NPC, unique claim state, recipient restriction, and pre-joining death invalidation |
| Home/schedule component | Home reference, one-time home roll, current day/night state, and temporary camp ownership |
| Defense behavior | Saved equal-chance sword/axe/bow groups, exact enchantments without durability loss, visible companion equipment, real melee/arrows, owner/ally checks, restoring daytime equipment, and no death rewards |
| Existing activity component | Daytime fishing, boat travel, music, or ordinary traveler behavior; paused/resumed by schedule and defense |
| Neighborhood manager | Saved natural-neighborhood centers, home reservations, and resident membership |
| Village manager | Village identity, owner, dimension, tier, boundaries, and plot reservations |
| House catalog | Stable template IDs, footprints, entrances/beds, previews, and approved color substitutions |
| Construction manager | Selected house, exact reserved item stacks, cancellation/refund state, due dawn, placement result, and move-in state |
| Resident registry and return scheduler | Resident identity saved beyond entity death, home plot, pending next-dawn return, and checks against duplicate living instances |
| Menus and rendering | Conversation, stats, barter bar, territory/plot previews, house choice, and material slots; display server-approved results |

Avoid turning AmbientNpc into one large class containing every inventory, village, construction, and nighttime rule. A UUID is an identifier; keep a stable person ID through recruitment and resurrection, with an explicit link to the current living entity. Most entity data normally disappears when that entity dies, so resident identity also needs a world-level saved record. A template ID identifies a house design, not a specific resident's house.

Construction and invitations need explicit saved states, for example: **invited → plot reserved → funded → due → house placed → moved in**. Each transition must tolerate a reload without repeating its item consumption, block placement, or NPC transfer. Completion should be recorded in a way that allows interrupted work to be reconciled safely.

Pre-dawn cancellation moves a funded claim back to its original invitation item and releases its plot. A destroyed invitation never goes back to “not yet issued.” Death before joining invalidates recruitment; death after joining enters **return pending → resident present**. Keep these paths separate so cancellation cannot grant extra invitations and resurrection cannot restore an unrecruited traveler.

## Documentation and future scope

Update this gameplan as decisions are agreed and milestones are verified. Update [Lived-in-Development.md](Lived-in-Development.md) with implemented behavior, entry points, saved fields, testing, and limitations. Keep the [Phase 1 gameplan](GAMEPLAN-Phase1.md) and [Phase 1 code rundown](Code-Rundown-Phase1.md) as the baseline; do not describe these proposals as working features.

Phase 3 can add companions, village tiers two and three, and house upgrades using the saved village tier, stable house template IDs, reserved plots, and persistent resident identities. It will need its own decisions about upgrade space, costs, and growth timing. Those systems are intentionally not part of Phase 2's completion criteria.
