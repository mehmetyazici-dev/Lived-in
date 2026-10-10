# Lived-In Development

Last updated: 2026-10-10

This guide explains how the project works and how to develop it. The scene ideas and milestones live in [GAMEPLAN-Phase1.md](GAMEPLAN-Phase1.md). Both documents are updated with each relevant project change.

## What NeoForge is

NeoForge is a mod loader and a set of tools for Minecraft Java Edition. The **loader** finds installed mods and starts them. The **API** gives our Java code ways to add content and respond to Minecraft.

Think of Minecraft as the application, NeoForge as the integration layer, and Lived-In as a Java library that the application loads. Our mod runs inside Minecraft's Java process. It is not a separate game or a standalone Java program.

Lived-In targets **Minecraft 1.21.1**, with **NeoForge 21.1.256** as the tested loader version. Minecraft's code changes between versions, so supporting another version or Forge requires a separate port and testing.

## How we build and install a mod

We write Java classes under `src/main/java` and resource files under `src/main/resources`. Resources include translations, models, and eventually our own textures and sounds.

Gradle is the build tool. The project includes a **Gradle wrapper**, so developers use `gradlew.bat` instead of installing Gradle separately. The ModDevGradle plugin prepares Minecraft and NeoForge for compilation and development launches. Parchment adds helpful parameter names and documentation to the game sources.

When we run `gradlew.bat build`:

1. Gradle reads the versions and mod identity from `gradle.properties`.
2. The Java compiler turns our classes into bytecode.
3. The `generateModMetadata` task fills in the metadata template.
4. Gradle puts our classes, resources, and generated metadata into `build/libs/livedin-0.1.0-dev.jar`.
5. Our development GameTests are compiled, but running them requires the separate command below.

The JAR is the mod we can eventually give to players. It does not contain Minecraft, IntelliJ, our local JDK, test worlds, or development GameTests.

Players install the matching Minecraft/NeoForge environment and put our JAR in that instance's `mods` folder. NeoForge reads `META-INF/neoforge.mods.toml` inside the JAR to find the mod's name, ID, version, and dependencies. See NeoForge's [getting-started guide](https://docs.neoforged.net/docs/1.21.1/gettingstarted/) and [mod files guide](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/).

| Metadata | Value |
| --- | --- |
| Mod ID / resource namespace | `livedin` |
| Display name | Lived-In |
| Java package | `dev.livedin` |
| Version | `0.1.0-dev` |
| Minecraft range | `[1.21.1]` — exactly 1.21.1 |
| NeoForge range | `[21.1.256,)` — at least 21.1.256 |
| Java FML loader range | `[4,)` |
| Temporary license | All Rights Reserved |

These version ranges are checks made by the loader. They do not mean every future NeoForge release has been tested. The Java FML loader version is separate from the NeoForge version.

## Where the mod starts

The entry point is [LivedIn.java](../src/main/java/dev/livedin/LivedIn.java):

```java
@Mod(LivedIn.MOD_ID)
public final class LivedIn {
    public static final String MOD_ID = "livedin";
    // Logger and setup methods omitted from this short example.

    public LivedIn(IEventBus modBus) {
        ModContent.register(modBus);
        modBus.addListener(LivedIn::registerAttributes);
        modBus.addListener(LivedIn::addCreativeItems);
        LOGGER.info("Lived-In loaded: ambient travelers are ready.");
    }
}
```

`@Mod` tells NeoForge that this class belongs to `livedin`. NeoForge constructs the class and supplies its **mod event bus**. A bus lets us subscribe methods to events that NeoForge sends at the correct time. There is no `main()` method in our mod.

The constructor registers entity, item, and sound definitions plus two setup listeners. It does not spawn travelers at startup. Players place individual travelers with an egg or command; the camp command creates a group.

## How the traveler connects to Minecraft

A **registry** is Minecraft's lookup table for content. For example, it maps `livedin:ambient_npc` to our entity type. An **entity** is a world object with its own position and lifetime, such as a player, villager, boat, or our traveler.

[ModContent.java](../src/main/java/dev/livedin/registry/ModContent.java) uses `DeferredRegister`. This queues registrations until NeoForge is ready. The references returned by registration become usable after that loading stage.

| Hook | What our mod supplies |
| --- | --- |
| Entity registry | `livedin:ambient_npc`, constructed as `AmbientNpc` |
| Item registry | `livedin:ambient_npc_spawn_egg` and `livedin:lute` |
| `EntityAttributeCreationEvent` | 20 health, movement speed 0.22, follow range 16 |
| `BuildCreativeModeTabContentsEvent` | Egg in Spawn Eggs; lute in Tools & Utilities |
| `EntityRenderersEvent.RegisterRenderers` | The client renderer for our traveler |
| Sound registry | `livedin:lute_note`, linked to a resource sound definition |

These setup events use the mod event bus. `ClientEvents` subscribes only on the physical client. Its renderer uses Minecraft's player model and vanilla Steve, Efe, Kai, Alex, and Noor skins. Walking, head movement, and passenger poses come from vanilla systems. `TravelerModel` adds lute and fishing poses, while `TravelerItemLayer` positions the activity items. Skins remain vanilla placeholders.

Our translation file gives the NPC the visible name **Traveler** and the egg the name **Traveler Spawn Egg**. The egg's item model uses Minecraft's spawn-egg template with brown and tan colors.

The separate **game event bus** (`NeoForge.EVENT_BUS`) can handle runtime events such as player actions. ModCommands uses that bus for RegisterCommandsEvent. The physical client also uses ClientTickEvent.Post for music cleanup and scheduling. Walking runs through the entity's own AI. `BoatRouteController` subscribes to `EntityTickEvent.Pre` and `.Post` on the game bus to steer only a boat whose first passenger is its assigned boat rider. NeoForge explains the distinction in its [events guide](https://docs.neoforged.net/docs/1.21.1/concepts/events/).

## Which parts of the mod live in code?

There are three useful categories:

- **Java code** decides what exists and what it does: entity creation, health, movement, AI, scene placement, saved fields, and when a musician performs.
- **Resource files** describe presentation: an item's shape, textures to use, visible names, subtitles, and which sound to play.
- **World save data** stores each placed NPC's current state: position, identity, health, equipment, role, and assigned spots. Save files are generated by Minecraft; they are not source code.

For example, our lute is registered by Java, shaped by a JSON model, and placed in a musician's hand by Java. Minecraft saves that equipped item with the NPC.

### Where an NPC is defined, and where it is created

[AmbientNpc.java](../src/main/java/dev/livedin/entity/AmbientNpc.java) defines our NPC **class**. It describes the behavior shared by every traveler. Extending `PathfinderMob` means we inherit Minecraft's basic mob systems and customize them.

[ModContent.java](../src/main/java/dev/livedin/registry/ModContent.java) defines the **entity type**:

```java
EntityType.Builder.of(AmbientNpc::new, MobCategory.CREATURE)
        .sized(0.6F, 1.8F)
        .eyeHeight(1.62F)
        .clientTrackingRange(8)
```

`AmbientNpc::new` is a constructor reference: it tells Minecraft which class to construct when this type is spawned. Registering the type creates a definition, not a living NPC in the world.

An actual NPC **instance** is created later. A spawn egg or `/summon` asks Minecraft to create one using that registered type. Our camp placement code does it explicitly in [CampfireScene.java](../src/main/java/dev/livedin/scene/CampfireScene.java):

```java
AmbientNpc npc = ModContent.AMBIENT_NPC.get().create(level);
npc.moveTo(location.x, location.y, location.z);
npc.assignCampRole(NpcRole.MUSICIAN, center, spot);
npc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModContent.LUTE.get()));
level.addFreshEntity(npc);
```

This shortened example omits the real method's null checks, validation, and rollback handling. `create(level)` constructs the object, `moveTo` sets its initial position, the role and equipment configure that individual, and `addFreshEntity` inserts it into the world. All of these steps run on the server.

The `AmbientNpc` constructor is another useful place to inspect: it calls the inherited constructor and marks the entity persistent. It does not choose the final position or create a camp.

### Where to change the NPC's characteristics

| What you want to change | File and method | Current setting / meaning |
| --- | --- | --- |
| Default health and walking speed | `AmbientNpc.createAttributes()` | `MAX_HEALTH = 20`, `MOVEMENT_SPEED = 0.22` |
| Default sensing range | `AmbientNpc.createAttributes()` | `FOLLOW_RANGE = 16`; individual goals can use their own range |
| Vehicle seat alignment | `ModContent.AMBIENT_NPC` builder | Uses `Player.DEFAULT_VEHICLE_ATTACHMENT` (0, 0.6, 0), matching the player model's seated hips |
| Collision width, height, and eye height | `ModContent.AMBIENT_NPC` builder | Width 0.6, height 1.8, eyes at 1.62 blocks |
| Whether distance makes it despawn | `AmbientNpc` constructor and `removeWhenFarAway()` | Persistent; distance does not remove it |
| Which behavior takes priority | `AmbientNpc.registerGoals()` | Lower priority numbers run first when goals compete |
| Where it belongs | `setActivitySpot()`, `assignCampRole()` | Per-instance block positions, saved with that NPC |
| How far it can wander | `activityRadius()`, `isWithinActivityArea()` | Ordinary traveler: 6; camp companion: 2; musician/fisher: 1 block |
| Walking speed during a goal | Goal constructors in `registerGoals()` | A multiplier applied to the movement-speed attribute |
| Walking frequency and chosen paths | `SceneStrollGoal` | Occasional short walks; paths must remain in bounds |
| Returning after being pushed | `ReturnToActivitySpotGoal` | Attempts navigation back; rejects unsafe camp paths |
| Scene roles | `NpcRole.java`, `AmbientNpc` assignment methods | Traveler 0, musician 1, companion 2, fisher 3, boat rider 4; saved IDs must remain stable |
| What it holds | `CampfireScene.place()` | Musician receives one lute in the offhand |
| Skin / appearance | `AmbientNpcRenderer.getTextureLocation()` | Vanilla Steve, Efe, Kai, Alex, or Noor skin selected by role/identity |
| Walking, holding, and strumming poses | `TravelerModel.setupAnim()` | Vanilla player animation plus our lute arm pose |
| Lute shape and player-held appearance | `assets/livedin/models/item/lute.json` | Cubes, texture references, and item display transforms |
| Lute placement against the musician | `TravelerItemLayer.renderArmWithItem()` | Torso attachment, outward-facing strings, and diagonal neck; other items use vanilla hand attachment |
| Visible names and subtitles | `assets/livedin/lang/en_us.json` | Translation keys shown to players |
| Which sample the lute uses | `assets/livedin/sounds.json` | References the existing Minecraft harp sound |
| Notes, rests, and tempo | `CampfireMelody.java` | Original phrase, ten game ticks per note slot |
| Loudness and lifetime of a note | `LuteNoteSound.java` | Quiet positional sound, stopped when performance ends |
| Camp layout and member positions | `CampfireScene.place()` | Fire, benches, supplies, one musician, two companions |
| How a developer places a camp | `ModCommands.register()` | Operator command `/livedin camp [center]` |
| What survives a world reload | `addAdditionalSaveData()`, `readAdditionalSaveData()` | Our activity/camp/water/pet spots, cat identity, and role; vanilla saves identity/equipment |
| Fishing cycle and conditions | `FishingCycle`, `AmbientNpc.canFish()` | Twelve-second cast/wait/retrieve cycle, paused when the scene is invalid |
| Fisher and cat creation | `FishingScene.place()` | One traveler and one vanilla cat with saved identity references |
| Boat route and speed | `BoatScene`, `BoatRouteController.beforeBoatTick()` | Short straight source-water route; 0.075 blocks per tick; pauses at obstacles |
| Boat loss recovery | `AmbientNpc.recoverFromLostBoat()` | One search for nearby safe shore, then ordinary traveler; never replaces the boat |
| Visible fishing line and bobber | `FishingSceneRenderer.render()` | Temporary client drawing; no separate bobber entity |

For a simple health change, edit the attribute builder:

```java
.add(Attributes.MAX_HEALTH, 30.0) // 15 hearts instead of 10
```

This sets a default for the type. To change just one specific NPC in code, set that instance's attribute using `npc.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0)`, then set its current health with `npc.setHealth(30.0F)`. Maximum health and current health are separate values.

For behavior changes, edit or add a goal and register it in `registerGoals()`. `canUse()` decides when a goal starts, `canContinueToUse()` decides whether it keeps running, `start()` prepares it, `tick()` performs repeated work, and `stop()` cleans up. Goal flags such as `MOVE` and `LOOK` tell Minecraft which behaviors compete.

After editing Java, rebuild and restart the development client. Spawn a fresh NPC when testing changed defaults; an existing saved NPC can retain old attribute or equipment values. Resource-only presentation changes can usually be reloaded with F3+T, but they should still be built into the final JAR.

## What the traveler does

[AmbientNpc.java](../src/main/java/dev/livedin/entity/AmbientNpc.java) extends Minecraft's `PathfinderMob`. This gives it normal entity collision, health, equipment, navigation, and AI goal support.

A **goal** is a small behavior, such as walking or looking at a player. Minecraft's goal selector chooses which goals can run together and gives higher-priority goals precedence.

Our priorities are:

1. Float if in water.
2. Perform at the campfire when the musician is ready, or hold the fishing position facing water.
3. Walk back if displaced outside the activity area.
4. Occasionally take a short walk within the activity area (companions and ordinary travelers).
5. Look at a player within eight blocks.
6. Occasionally look at the campfire.
7. Look around while idle.

The **activity spot** starts at the NPC's placement block. We capture it on the first server tick because the constructor runs before Minecraft sets the final spawn position. Scene code can later assign a different spot through `setActivitySpot(BlockPos)`.

Ordinary travelers use a six-block activity radius, companions use two, and musicians and fishers use one. Distances are measured in three dimensions using block positions. `SceneStrollGoal` chooses ground destinations and rejects a path if its destination or any path node leaves the area. `ReturnToActivitySpotGoal` tries to walk back into the area if the NPC is pushed outside it. It retries failed navigation at intervals and does not teleport the NPC home. Both walking goals pause while the NPC is a passenger.

This is a shared starting point for the scenes. Saved camp roles, lute posing, local music, fishing, and a sitting cat are implemented and verified. Short local boat routes are implemented; long-distance boat navigation remains planned. The NPC has no attack goal, dialogue, trading, or custom right-click action. Normal damage and death still apply.

## Server decisions, client display, and saved state

Even singleplayer uses two logical parts: an integrated **server** simulates the world, while the **client** draws it. NPC decisions run on the server. The client receives normal Minecraft entity updates and displays movement and looking. Roles and performance state use Minecraft's synchronized entity data. No custom packets are required.

Client rendering code lives under `dev.livedin.client` and is kept out of the common entry point. The GameTests also load the common mod on a server without a graphics client.

Minecraft saves each NPC as **NBT**, its structured world-data format. Vanilla saves the entity's UUID (its identity), position, health, name, and equipment. We additionally save packed block positions under `ActivitySpot`, `CampfireSpot`, `FishingWater`, and `PetSpot`, the numeric `NpcRole`, and the associated cat's UUID under `PetCat` when present. Boat riders also save `SceneBoat` (boat UUID), `BoatStart`, `BoatEnd` (water positions), and `BoatHeadingToEnd` (next destination). Vanilla saves a mounted rider inside the boat's `Passengers` list.

`addAdditionalSaveData` writes our field. `readAdditionalSaveData` restores it. A saved NPC keeps its original activity spot even if it walked elsewhere before saving. If the field is missing, its first server tick initializes it at its current position.

Travelers are marked persistent and do not despawn because a player moves far away. They still unload with their chunks. No scene manager respawns them on visits, and no chunk-loading tickets keep their area active.

## Place and inspect a traveler

Start the development client and use an isolated Creative world with commands enabled. Search for **Traveler Spawn Egg** in the creative inventory, or use:

```mcfunction
/give @s livedin:ambient_npc_spawn_egg
```

Right-click open ground to place one NPC. Each use places another NPC. Alternatively:

```mcfunction
/summon livedin:ambient_npc ~ ~ ~
```

Use flat, dry ground with room to walk for initial tests. Keep fire, cliffs, and deep water away from the basic prototype.

To inspect the nearest traveler:

```mcfunction
/data get entity @e[type=livedin:ambient_npc,sort=nearest,limit=1]
```

Look for `UUID`, `ActivitySpot`, and `PersistenceRequired: 1b`. To verify persistence manually, note the identity and spot, travel far enough to unload the scene, return, then save and reopen the world. Count the travelers each time.

## Milestone 2: campfire gathering

Milestone 2 is **complete**, with the final build, eleven server tests, and client fire/unload/save lifecycle checks passing on 2026-10-09. The lute attachment was corrected and inspected from the front and side.

### Placing a camp

Use a Creative development world with commands enabled. Stand on flat ground and run:

```mcfunction
/livedin camp
```

This places the center five blocks ahead of your facing direction. To choose an exact center at ground level:

```mcfunction
/livedin camp 30 -60 30
```

The command requires operator permission level 2. It checks a loaded 9 by 9 area for solid dry ground and two blocks of empty air. It refuses occupied terrain and existing nearby travelers. It places a lit campfire, three log benches, a barrel, hay supplies, one musician, and two companions. If adding the group fails, its newly placed blocks and members are rolled back.

This is a manual prototype command. There is no automatic world generation or group replacement on visits. The lute is also available in Tools & Utilities or with `/give @s livedin:lute`; it is currently a decorative item for players.

### Server behavior

`NpcRole` defines stable numeric role IDs: ordinary traveler 0, musician 1, companion 2. `AmbientNpc` saves each member's `NpcRole`, `ActivitySpot`, and `CampfireSpot`. Vanilla saves identity and equipment. Role and performance state use `SynchedEntityData`, Minecraft's built-in system for sending entity fields to clients.

The musician holds a lute in the offhand. Its activity area is restricted to its assigned block. `PerformAtCampfireGoal` keeps it still and facing the gathering. Companions can take small walks within two blocks of their own spots, look at players, and occasionally look toward the fire.

Camp walking paths reject positions less than two blocks horizontally from the fire center. The return goal also rejects paths through this clearance area. External pushing or changed terrain can still disrupt the scene; this is not complete obstacle recovery.

`canPerform()` requires a living musician, a lute in the offhand, a loaded and lit non-waterlogged campfire, the musician at its assigned activity area, and no passenger/water state. The server updates the synchronized `PERFORMING` flag each tick. Extinguishing/removing the fire or removing the lute stops that flag. It is recomputed after world load rather than saved as an active performance.

### Client model and music

`TravelerModel.setupAnim()` supports the lute with the left arm and moves the right arm across the strings. The pose holds still during phrase rests and when performance is inactive. `AmbientNpcRenderer` uses Efe for the musician and Steve/Kai for companions.

`TravelerItemLayer` is our drawing layer for equipped items. A **layer** draws something extra on a mob after its main body. We replace the renderer's default item layer with this one, so the lute is drawn exactly once. Other items still use vanilla hand attachment. For an offhand lute, `renderArmWithItem()` attaches it to the torso, turns its strings outward, and tilts its neck toward the supporting left hand. The right hand makes small strumming movements without dragging the entire instrument around. This corrects the generic held-item rotation that previously misaligned the lute. These transforms affect drawing only; the lute remains real saved offhand equipment.

`lute.json` is a custom blocky item model: a wooden body, dark neck, sound hole, pegs, and three strings. It references vanilla block textures. No modeling library or custom animation dependency is required.

`CampfireMelody` contains an original sequence of twelve notes followed by four rests. Each slot lasts ten game ticks (normally half a second), making an eight-second repeating phrase. Pitch changes reuse Minecraft's harp timbre. `sounds.json` defines `livedin:lute_note` by referencing the vanilla harp event, so we do not copy or distribute Minecraft audio files.

`ClientEvents.clientTick()` calls `TravelerMusic`. It keeps one voice per loaded, performing NPC UUID, with at most one current note per voice. A new note stops the previous one. Notes use `LuteNoteSound`, follow the NPC, have quiet volume and normal positional attenuation, and stop when the NPC is removed, dies, or stops performing.

The controller closes a voice when its musician becomes unavailable or is farther than 24 blocks from the player. The sound itself uses Minecraft's shorter normal attenuation range. Changing worlds clears all voices. The musical phase comes from world game time, so revisiting does not create another repeating loop. Audio is generated locally from synchronized performance state; no custom network packets are needed.

Optional local verification logging is enabled with JVM property `-Dlivedin.musicDiagnostics=true`. It reports voice creation/removal and confirmation that the sound engine is playing a note. Normal runs leave these logs disabled. The property changes logging only, not gameplay.

### Verification completed on 2026-10-09

- The final build passed and produced `build/libs/livedin-0.1.0-dev.jar`. The JAR includes the new drawing layer, lute model, and sound definition; it excludes GameTests, their arena, and local verification functions.
- All eleven headless GameTests passed: the original five NPC checks plus group placement/repeated-command rejection, preserving existing blocks, unsafe/unloaded-area rejection, 600 ticks of fire clearance without damage, performance response to fire/instrument removal, and restoring all three member identities and roles.
- In the existing isolated Creative world **Lived-In Milestone 1**, `/livedin camp 30 -60 30` successfully placed the group.
- The fire, benches, supplies, three travelers, and held lute were inspected in the client. After correcting the generic item rotation, front and side views showed the lute body in front of the musician, outward-facing strings, and hands aligned with the body and neck.
- Local diagnostics confirmed one musician voice and active playback in the Minecraft sound engine. No missing Lived-In model or sound errors were reported.
- Extinguishing the fire froze the strumming pose and closed the musician voice (total 0); relighting resumed a single voice (total 1) with active sound-engine playback.
- Moving to (2000, -60, 2000) closed the voice. After 30 seconds, vanilla `execute if loaded` checks confirmed all four camp chunks (1,1), (2,1), (1,2), and (2,2) were unloaded. Returning restored exactly three members with the same identities and resumed one voice.
- Saving to title cleared the voice on world change. Reopening the world restored exactly three members with the same UUIDs, roles, and `CampfireSpot: 8246337335236L` (30, -60, 30). One musician voice resumed; no duplicate member or voice appeared.
- Local verification functions live only in that ignored world. They are not mod resources or required by players. The original ordinary traveler near the origin was excluded from the camp count.

The saved group identities below matched before travel, after chunk reload, and after world reopen. Minecraft displays UUIDs as four signed integers in NBT:

| Member | UUID integer array | Saved role |
| --- | --- | --- |
| Musician | `[-683793725, 239749851, -2134674100, 566489376]` | 1 |
| Companion | `[1466737179, -1874245929, -1997048888, 363636456]` | 2 |
| Companion | `[515337971, 1143555132, -1745488881, 1129270781]` | 2 |

Audio verification used sound-engine playback and voice-lifecycle logs. The assistant did not capture or listen to desktop audio; perceived loudness and musical quality remain subjective polish to review during play.

## Milestone 3: fisher and sitting cat

Milestone 3 is **complete**, with the build, seventeen server tests, and client appearance/interruption/unload/save checks passing on 2026-10-10.

### Placing a fishing scene

Use an isolated Creative world with commands enabled. The command takes two block positions:

```mcfunction
/livedin fishing 80 -60 80 80 -61 84
```

The first position is the fisher's **feet on the bank**. The second is the **water block at the surface**, one block lower and three to five blocks away horizontally. This example faces south. The cat sits on the bank to the fisher's right. The command needs operator permission level 2.

Prepare a dry 3 by 3 bank with solid ground and two blocks of empty air, plus an exposed 3 by 3 area of source water around the target. The path of the cast must be clear and the whole area loaded. `FishingScene.place()` checks these conditions and refuses existing travelers or cats nearby. It creates the pair without digging a pond or changing blocks. Failed entity placement removes only the new pair.

The scene deliberately uses an existing shore. There is no natural spawning, shoreline searching, fishing loot, inventory, or economy in this milestone.

### Where the behavior lives

| Part | File / method | What it does |
| --- | --- | --- |
| Fisher role | `NpcRole.FISHER` | Saved role ID 3; uses the same `AmbientNpc` class |
| Pair creation and terrain checks | `FishingScene.place()`, `validFishingSpot()`, `safeShore()` | Creates a traveler and vanilla `Cat` only after checking bank, source water, and casting corridor |
| Scene assignment | `AmbientNpc.assignFishingScene()` | Stores bank, water target, cat identity, and cat spot; sets the fisher's role |
| Fishing conditions | `AmbientNpc.canFish()` | Requires living, dry, non-passenger fisher at its bank, with a rod and valid water/corridor |
| Staying and facing water | `FishAtWaterGoal.tick()` | Stops walking and faces the saved target; the fisher does not randomly stroll |
| Cycle timing | `FishingCycle` | 240 ticks: prepare 0–19, cast 20–39, wait 40–199, retrieve 200–219, rest 220–239 |
| Server progress | `AmbientNpc.tick()` | Advances synchronized `FISHING_TICK`, or sets it to -1 when fishing is invalid |
| Arm movement | `TravelerModel.setupAnim()` | Raises the rod, casts, waits, and retrieves using that tick value |
| Rod attachment | `TravelerItemLayer.renderArmWithItem()` | Draws the vanilla cast-rod model in the right hand |
| Rod model loading | `ClientEvents.registerModels()` | Registers the existing vanilla model through `ModelEvent.RegisterAdditional` |
| Line and bobber | `FishingSceneRenderer.render()` | Draws a line from the posed rod tip and a bobbing vanilla sprite at the water target |
| Keeping the pet nearby | `AmbientNpc.keepPetNearby()` | Checks the original loaded cat each second and returns a displaced cat to its saved safe spot |
| Saved fields | `AmbientNpc` save/load methods | Save `FishingWater`, `PetCat`, and `PetSpot`; vanilla saves both entity identities and equipment |

Twenty game ticks normally equal one second, so one cycle lasts twelve seconds. `FISHING_WATER` and `FISHING_TICK` use Minecraft's synchronized entity data. The server decides whether fishing is possible; the client displays that decision. No custom network messages or animation libraries are needed.

The bobber is a **temporary drawing**, not a `FishingHook` entity. Vanilla hooks require a player owner, while this activity belongs to an NPC. Tying the drawing directly to the fisher avoids a second entity lifecycle: there is no bobber to save, duplicate, forget to remove, or restore after an interrupted cast. The cast moves it outward in an arc, the wait gently bobs it, and retrieval brings it back. It disappears during rest, invalid conditions, death, and unloading.

An interrupted cycle resets to -1 and starts again from preparation when the bank, water, and rod become valid. The tick counter is not saved, so loading a world starts a fresh cycle rather than restoring a cast in midair. Fishing pauses if the water becomes blocked or missing, the cast corridor gains an obstacle, or the rod is removed.

### How the pet association works

The cat is a normal `minecraft:cat`, with vanilla appearance, sitting pose, looking, sounds, damage, and persistence. `FishingScene` orders it to sit and saves the fisher's UUID in its vanilla `Owner` field. The fisher stores the cat's UUID in `PetCat`. These two references identify the pair; `PetSpot` records the dry bank spot.

Vanilla `getOwner()` resolves **players only**, so an NPC UUID does not enable vanilla follow-owner behavior. Our code uses the saved identifiers directly. Once per second, a living fisher looks up its original cat in already loaded entities. If the associated living cat is more than 1.5 blocks from its pet spot, the fisher returns it there only if that spot remains safe. Normal behavior is sitting in place, with vanilla idle looking.

No lookup loads a missing chunk. A missing, dead, unloaded, or differently owned cat is never recreated. The association remains saved. If the fisher dies, it stops maintaining the pet; the cat remains a persistent vanilla cat. Terrain changes, attacks, external pushing, and deliberate player modification can still disrupt the scene. This is a prototype activity, not full pet navigation or hazard recovery.

### Verification

- The build passed and seventeen GameTests passed: the previous eleven plus six fishing checks.
- New tests cover pair creation/repeat rejection, blocked or unsafe placement without overwritten blocks, 600 ticks of cycles on dry ground, water/corridor/rod interruption and recovery, saving both identities and scene references, and displaced/missing pet behavior.
- In the isolated **Lived-In Milestone 1** world, the scene at bank (80, -60, 80) and water (80, -61, 84) showed an Alex fisher, held rod, moving arm, line, bobber, and sitting vanilla cat. The vanilla cast-rod model is explicitly registered as an additional model; the corrected lookup rendered without the missing-model cube.
- Replacing the target water stopped the activity and removed the line/bobber. Restoring the water recovered the cycle. Blocking the casting corridor also hid the line/bobber; clearing it resumed the activity.
- Traveling to (2000, -60, 2000) and waiting 30 seconds unloaded both the fisher's chunk (5,5) and the cat's neighboring chunk (4,5). Returning restored exactly one fisher and one cat and resumed fishing.
- Saving to title and reopening restored the same pair, `PetCat`/`Owner` references, `Sitting: 1b`, and water target. Casting restarted safely; no duplicate cat or fisher appeared. Vanilla entity counts confirmed there were no `minecraft:fishing_bobber` entities, as intended by the drawing-only design.
- The final JAR contains the fishing classes and no GameTest classes, test arena, tools, or worlds. The local pond and verification functions exist only in the ignored development save; players do not need those functions.

These identities matched before travel, after chunk reload, and after saving/reopening:

| Member | UUID integer array |
| --- | --- |
| Fisher | `[272345797, 1918389964, -2142742523, 166224139]` |
| Cat | `[-322669031, -9682173, -1951176919, -847005439]` |

The fisher's `PetCat` equals the cat's UUID, and the cat's `Owner` equals the fisher's UUID. `FishingWater: 21990232903619L` is the packed water position (80, -61, 84).

## Boat rider implementation

Milestone 4 is **complete**. The build, all twenty-five server GameTests, client presentation/obstacle checks, chunk unload/revisit, and world save/reopen checks passed on 2026-10-10.

### Place a boat scene

Use `/livedin boat <start> <end>` with two **water block positions**, not NPC feet. Example for a prepared pond:

```mcfunction
/livedin boat 130 -61 136 130 -61 148
```

The points must share a height and lie along X or Z, 4–12 blocks apart. Every point between them needs a 3 × 3 patch of source water and two air blocks above each water block. This leaves room for the hull, rider, and turns. Placement checks loaded chunks, rejects nearby travelers/boats, and never excavates or overwrites terrain. This operator command is a development placement tool; encounters do not generate naturally yet.

### Which code owns each part?

| Part | Code | Explanation |
| --- | --- | --- |
| Create the boat and NPC | `BoatScene.place()` | Constructs `minecraft:boat` and our existing traveler, adds both, then calls `rider.startRiding(boat)` |
| Boat rider characteristics | `NpcRole.BOAT_RIDER`, `AmbientNpc.assignBoatScene()` | Role ID 4, saved route endpoints and boat UUID; uses the same health and size as other travelers |
| Validate route terrain | `BoatScene.validRoute()`, `safeWater()` | Checks short straight routes, still exposed water, overhead space, and loaded chunks |
| Control movement | `BoatRouteController.beforeBoatTick()` | Sets horizontal velocity before the normal boat tick; preserves vanilla buoyancy, gravity, and collision |
| Show rowing | `BoatRouteController.afterBoatTick()` | Restores synchronized oar state after vanilla clears non-player rowing; keeps the rider facing the hull |
| Rider appearance | `AmbientNpcRenderer.getTextureLocation()` | Temporary vanilla Noor skin; vanilla player model supplies the seated passenger pose |
| Seat height | `ModContent.AMBIENT_NPC` registration | Uses the vanilla player vehicle attachment so the seated hips fit inside the hull |
| Recover after losing a boat | `AmbientNpc.recoverFromLostBoat()` | Clears the route, stops the original empty boat if loaded, and searches for safe nearby shore |
| Save and restore | `AmbientNpc` save/load methods plus vanilla boat save code | Saves boat UUID, endpoints, direction, and actual passenger relationship; never creates another boat on load |
| Verify behavior | `BoatSceneTests` | Eight checks exercise real vanilla boat physics and persistence |

A **tick** is one simulation step; Minecraft normally runs 20 per second. Our boat travels at about 1.5 blocks per second. Before vanilla's boat tick, the controller supplies a small horizontal velocity. Vanilla still floats and collides normally. After that tick, we set its left/right oar flags because vanilla normally clears them for mob passengers. Minecraft's existing entity synchronization sends the hull, passenger, and oar updates to the client. We register no new boat entity, boat renderer, item, or custom network packet.

The boat goes to the other endpoint, pauses for about 1.5 seconds, and returns. Heading turns gradually. The controller checks current/next water and swept hull/rider clearance before advancing. A new block or shoreline stops horizontal movement and rowing; clearing it lets the boat continue. Pushing the boat outside its narrow route also pauses movement. Only the assigned first passenger can drive this route; ordinary boats and boats controlled by players are left to vanilla.

### Persistence and recovery

The rider stores the original boat UUID and both endpoints. The boat saves its actual passenger in vanilla's `Passengers` list. Minecraft restores the whole tree before it ticks; our controller waits briefly and continues toward the saved destination. No visit, chunk load, or saved reference spawns replacements. Unloaded chunks are not forced to stay active.

If the NPC is dismounted, its boat is destroyed, or it is put in another vehicle, it becomes an ordinary traveler anchored at its current location. If it is no longer mounted, it searches loaded blocks within five blocks horizontally and from one below to two above for dry, unobstructed shore. It moves there once when a safe spot exists. If no shore exists, the ordinary floating behavior remains; this does not promise survival far offshore. Recovery is a small prototype safeguard, not a full swimming/pathfinding system. An intact empty boat is retained, so players can use it later.

### Verification

- `build` succeeded and all twenty-five GameTests passed: the previous seventeen and eight boat tests.
- New tests cover vanilla mounting and duplicate rejection; unsafe/diagonal/distant/unloaded placement; 600 ticks of bounded travel and repeated endpoint turns; obstacle stop/resume; shoreline and displacement stopping; boat/passenger tree save/load with unchanged UUIDs and route; boat removal with dry-shore recovery and no replacement; and dismount cleanup with the original empty boat retained.
- In the isolated **Lived-In Milestone 1** world, `/livedin boat 130 -61 136 130 -61 148` placed one vanilla oak boat and one visibly seated Noor traveler. The hull, rider, and animated oars moved together along the pond; the boat repeatedly paused and turned at the endpoints.
- A stone block at (130, -60, 142) stopped the approaching boat at Z = 144.075, before its hull touched the obstruction. Its X/Z motion was zero and its oars stopped. Clearing the block resumed movement with horizontal speed 0.075 blocks per tick. The rider remained aboard with 20 health.
- Traveling to (2000, -60, 2000) and waiting 30 seconds confirmed both route chunks (8,8) and (8,9) were unloaded. Returning restored exactly one boat and one rider, the same identities and route fields, and a real `Passengers` entry. Rowing resumed.
- Saving to title and reopening restored the same mounted pair and route, then movement resumed. No replacement or duplicate appeared. Boat removal and dismount recovery were exercised by server GameTests, not by destroying the client scene.
- The final JAR includes `BoatScene` and `BoatRouteController`, updated metadata, and the shared NPC changes. It excludes GameTests, the arena, local tools, worlds, and world-only verification functions. The local pond/helpers are not required by players.

These identities matched before unloading, after revisiting, and after reopening:

| Member | UUID integer array |
| --- | --- |
| Boat | `[-19497958, 1718110212, -1234976106, 170916965]` |
| Rider | `[971111413, -572634423, -1792685714, 1343982625]` |

The rider's `SceneBoat` equals the boat UUID. `BoatStart: 35734128463811L` and `BoatEnd: 35734128512963L` represent (130, -61, 136) and (130, -61, 148). The boat's saved `Passengers` list contains that rider with role 4. The direction changes during each journey, as intended.

Limits: short straight routes on prepared still water only; no automatic boarding, river navigation, terrain repair, interactions, or long journeys. External pushing, currents, attacks, and deliberate terrain changes can disrupt a scene. Removing water directly under a boat leaves vanilla gravity and submersion behavior in charge.

### Seat-height correction

The initial boat implementation mounted the NPC correctly, but its body floated above the seat. The player model bends its legs when riding, so its hips sit above its standing feet position. Our entity definition still used the generic mob vehicle attachment at its feet. Minecraft therefore aligned the wrong point with the boat seat.

`ModContent.AMBIENT_NPC` now calls `.vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)`. This is Minecraft's own player attachment: (0, 0.6, 0). When the boat positions a passenger, Minecraft subtracts that attachment from the boat seat position. The NPC's body is consequently 0.6 blocks lower and its hips sit inside the hull. It applies on both server and client, for any traveler riding a vehicle. Walking, the standing model, and saved boat identities need no special changes or migration. Existing riders use the corrected attachment after restarting with the new code.

The boat tests now check the actual seat height on mounting, throughout the 600-tick route, and after passenger-tree reload. In an oak boat, the NPC's feet are approximately 0.4125 blocks below the hull origin; the earlier incorrect position was 0.1875 blocks above it. The full build and all twenty-five server tests passed. The development client was restarted and the existing world reopened. The original rider now sits inside the hull while moving and turning; front and side views showed the corrected pose. The same boat and rider UUIDs remained, with exactly one of each. Saved-state inspection reported boat Y = -60.47673568725586 and rider Y = -60.88923568725586, a -0.4125 seat offset. This verifies the correction on the existing saved scene.

## Milestone 5 — Checking the scenes together

The combined QA setup uses a separate Creative flat save called **Lived-In Phase 1 QA**. It has three camps, three fishers with sitting cats, and three boat riders: **15 travelers, 3 scene cats, and 3 boats**. The fishing lines and bobbers are drawings, so there are zero saved fishing-hook entities. Nearby vanilla animals are separate from these scene counts.

### Test helpers

`scripts/prepare-phase1-qa.ps1` creates the local QA save and its datapack. It copies only `level.dat` from the development flat world as a generation/settings template; it does not copy its chunks, scene entities, or player files. Existing development saves are left intact. The helper assumes our local flat-world floor height of -61 and NPC feet height of -60. This is a test fixture, not a natural world-generation system.

In the QA world, run:

```text
/function phase1qa:setup
/function phase1qa:check
/function phase1qa:leave
```

`setup` prepares the terrain and places each scene through our normal mod commands. A saved initialization flag prevents that helper from placing everything again. `check` reports scene counts, entity UUIDs, pet references, and boat passenger references. A **UUID** is the saved identifier for one particular entity; comparing UUIDs catches replacements that a simple count might miss. Cats are counted by their sitting scene state, so unrelated wild cats do not distort the result.

`leave` moves the test player to (2000, -60, 2000), waits 35 seconds, checks that all eight chunks covering the scenes are unloaded, returns to the scenes, and records another snapshot after five seconds. Spawn-chunk retention and hostile spawning are disabled only in this QA save. Keep the game unpaused while scheduled checks run.

For timing, enter `/debug start` directly in chat, observe the loaded scenes for at least a minute, then enter `/debug stop`. These commands require a higher permission level than datapack functions. The optional Python 3 helper `scripts/check-phase1-qa.py <log-path>` compares the recorded UUIDs and associations and checks the music diagnostics for duplicate simultaneous voices. Python is a QA convenience, not a mod dependency.

### Packaged-JAR instance and verified results

The official NeoForge 21.1.256 client installer was downloaded with its SHA-256 checksum verified. It installed into `runs/package-smoke`, with a new local launcher-profile file in that folder. Existing launcher configuration and personal saves were not inspected or changed. The normal production `forgeclient` launch uses the installer libraries and the built JAR in that instance's `mods` folder. It does not use Gradle's development classes or GameTests. Cached official Minecraft assets are reused; the game directory, libraries, configuration, and save are separate.

This local singleplayer QA launch uses a fixed test identity. Account sign-in and multiplayer services are not being tested. To reopen the prepared instance on this PC:

```powershell
cd C:\Lived-in
. .\scripts\dev-env.ps1
Push-Location .\runs\package-smoke
java '@launch.args'
Pop-Location
```

The production mod list showed Minecraft 1.21.1, NeoForge 21.1.256, and Lived-In 0.1.0-dev. Client inspection showed the campfire musician and companions, the fisher's rod/line/bobber and sitting cat, and the seated moving boat rider together. All three musicians opened distinct music voices with sound-engine playback; leaving the area closed them to zero.

The build and all 25 server GameTests passed on 2026-10-10. With all nine scenes loaded, a vanilla tick profile measured **1,432 ticks in 71.57 seconds: 20.01 ticks per second**. Sampled F3 views showed approximately 97–100 FPS and 1.7–2.0 ms integrated-server ticks. Conditions: Ryzen 7 5800H, RTX 3070 Laptop GPU, Java 21, 3 GiB maximum heap, 854 × 480 render area, Fancy graphics/clouds, VSync enabled, 120 FPS limit, and render/simulation distances of 12 chunks. This is a short flat-world smoke test, not a hardware-independent benchmark or a modpack performance guarantee.

The tested JAR is `build/libs/livedin-0.1.0-dev.jar`, SHA-256 `13706baabd0f932dc58cb569cbfaddf6c452971cbb34c4eb59a2953bf6fcff4e`. The isolated instance contains a byte-identical copy. Local launch arguments, installed libraries, logs, QA worlds, and datapacks are ignored by Git and excluded from the JAR.

Three unload/revisit cycles passed. Each cycle confirmed all eight scene chunks unloaded before returning. Two save/reopen cycles passed, including quitting and restarting Minecraft for the second reload. Six snapshots preserved the same **21 UUIDs**, three unique cat-owner/pet associations, and three unique boat/passenger associations. Counts remained 15 travelers, three scene cats, three boats, and zero fishing-hook entities. The log-check helper passed against `build/phase1-qa-combined.log`, which combines the two client sessions.

Music diagnostics never exceeded three voices and never opened two simultaneous voices for the same musician. Moving away closed voices to zero. Each save to title cleared three voices; reopening restored exactly three voices with active sound-engine playback. Subjective listening review of loudness and the melody is still polish work.

The client sometimes logged `Received passengers for unknown entity` while scene chunks loaded. The server retained the real passenger trees, and a later client view confirmed the rider remained seated and moved with its boat. This observation remains worth checking in longer play and compatibility testing; it did not produce an enduring broken scene in this run. The restarted session had no QA function-loading errors.

## Project map

Paths are relative to `C:\Lived-in`.

| Path | Purpose |
| --- | --- |
| `src/main/java/dev/livedin/LivedIn.java` | Entry point and common setup listeners |
| `src/main/java/dev/livedin/registry/ModContent.java` | Entity and spawn-egg registration |
| `src/main/java/dev/livedin/entity/AmbientNpc.java` | Shared traveler behavior, roles, synchronized fields, and saved spots |
| `src/main/java/dev/livedin/entity/NpcRole.java` | Stable role IDs |
| `src/main/java/dev/livedin/scene` | Camp/fishing/boat placement, melody, fishing timing, and boat steering |
| `src/main/java/dev/livedin/command/ModCommands.java` | Camp, fishing, and boat command registration |
| `src/main/java/dev/livedin/entity/goal` | Stroll, return, campfire looking, and performance goals |
| `src/main/java/dev/livedin/client` | Renderer, traveler pose, event subscriptions, and sound controller |
| `src/main/resources/assets/livedin` | Translations, egg/lute models, and sound definition |
| `src/main/templates/META-INF/neoforge.mods.toml` | Template for packaged loader metadata |
| `src/gameTest/java/dev/livedin/gametest` | AmbientNpcTests, CampfireSceneTests, FishingSceneTests, and BoatSceneTests: twenty-five server checks |
| `src/gameTest/resources/data/livedin/structure/arena.nbt` | Empty 24 × 6 × 24 test arena; tests add a stone floor |
| `build.gradle` | Toolchain, registrations for development runs, metadata, packaging, test configuration |
| `gradle.properties` | Pinned versions and mod identity |
| `settings.gradle` | Plugin setup and project name |
| `gradlew / gradlew.bat` | Gradle wrapper launchers |
| `scripts/dev-env.ps1` | Selects our local Java 21 and Gradle cache |
| `scripts/open-idea.ps1` | Opens IntelliJ with the development environment |
| `scripts/prepare-phase1-qa.ps1` | Creates the isolated combined test save and world-only QA commands |
| `scripts/check-phase1-qa.py` | Reads QA logs and checks identities, associations, and music voice lifetime |
| `Documents` | Maintained roadmap and development guide |
| `build` | Generated output, including the mod JAR |
| `run` | Development client and its isolated saves |
| `runs/gametest` | Separate GameTest server directory |
| `runs/package-smoke` | Ignored production NeoForge installation, packaged JAR, and combined QA save |
| `.tools / .downloads / .gradle-user-home` | Ignored local tools, archives, and dependency cache |

The extra `gameTest` source set is available in development runs, but the release JAR contains only `main`. Changing local IDE folders or excluding caches from indexing does not affect packaging. Template demonstration content was removed.

## Toolchain and daily workflow

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.256 |
| Microsoft OpenJDK, Windows x64 | 21.0.12.1+1 LTS |
| IntelliJ IDEA | 2026.2.3, build 262.10968.63 |
| Gradle wrapper | 9.2.1 |
| ModDevGradle | 2.0.148 |
| Parchment | Minecraft 1.21.1 / mappings 2024.11.17 |
| Foojay resolver plugin | 1.0.0 |

Starting template: [NeoForge's 1.21.1 ModDevGradle MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle), commit `ad911709f06d3bdd5b1a500023a9156999216351`. Its wrapper and MIT template license are retained.

From PowerShell:

```powershell
cd C:\Lived-in
. .\scripts\dev-env.ps1
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

Dot-source `dev-env.ps1` in every new development terminal. It sets `JAVA_HOME` to `.tools/jdk-21.0.12.1+1` and `GRADLE_USER_HOME` to `.gradle-user-home`. The system terminal may otherwise select Java 8. Check `java -version`, `javac -version`, and `gradlew.bat --version` when diagnosing setup.

`build` compiles the mod and GameTests. `runGameTestServer` actually executes the twenty-five checks and exits with failure if a required test fails. It uses `runs/gametest`, separate from client worlds. See the official [GameTest guide](https://docs.neoforged.net/docs/1.21.1/misc/gametest/) for the underlying framework.

Open IntelliJ with `.\scripts\open-idea.ps1`. Both the project SDK and Gradle JVM are configured to our Microsoft JDK 21. IntelliJ itself uses its bundled runtime. Its unified distribution initially enabled an Ultimate trial; this project uses free Java/Gradle features. Reload the Gradle project after changing `build.gradle`.

For debugging, put a breakpoint in the `LivedIn` constructor and debug the generated Client configuration. For behavior debugging, use a breakpoint in an NPC goal or save method. Resume execution to continue playing. Minecraft sources are available through class navigation and External Libraries. If needed, attach `build/moddev/artifacts/neoforge-21.1.256-sources.jar`.

Tools are portable distributions under `.tools`; their download checksums were verified. On another machine, install a 64-bit JDK 21 and configure the IDE and terminal, then use the wrapper. Our convenience script assumes the local JDK path and is not required for other machines.

## Verification and limitations

Foundation checks completed on 2026-10-09: Java 21 and Gradle's 64-bit JDK confirmed; normal and clean builds passed; Lived-In appeared in the mod list; the original Smoke Test world saved and reopened; IntelliJ stopped at the entry-point breakpoint and resolved Villager.java sources.

Milestone 1 checks completed on 2026-10-09:

- The mod built successfully with the new entity, renderer, translations, and egg model.
- All five GameTests passed: actual placement initializes the anchor; save/load retains identity, anchor, name, health, equipment, and one entity; 600 ticks of real strolling remain in bounds; displacement recovers; passenger walking goals stay disabled.
- The headless GameTest server loaded the mod without client rendering classes.
- In the new Creative superflat world **Lived-In Milestone 1**, the egg successfully placed one visible traveler. Walking and looking were observed.
- Local test-world functions moved the player to (2000, -60, 2000), waited 30 seconds, and confirmed all four chunks surrounding the scene were unloaded using vanilla `execute if loaded` checks. Returning loaded exactly one traveler.
- Saving to title and reopening preserved the same UUID and `ActivitySpot: 12228L` (block 0, -60, 2). A second count found exactly one traveler.
- The release JAR was checked for common/client classes, metadata, translations, and the egg model, with no GameTest classes or arena.

The temporary verification datapack lives only in that ignored development world. It is not part of the mod or needed by players. Spawn-chunk retention was disabled only in that world to make the unload check possible.

Known limits: appearances use temporary vanilla skins; NPCs can take damage and die. Scene-specific fire, shore, and boat checks are implemented, but blocked routes, cliffs, external pushing, or terrain changes can still disrupt a scene. This is not a survival or terrain-safety guarantee. No natural spawning, dialogue, or deliberate NPC interactions exist yet. The manual scene commands are operator development tools.

A separate production instance using the packaged JAR passed milestone 5 on 2026-10-10. A normal multiplayer dedicated-server session, long survival sessions, and modpack compatibility remain untested. The 25 headless tests, individual development-client checks, and combined packaged-JAR checks cover the implemented foundation and three scenes. Subjective listening review remains polish work. Phase 1 is complete; the next phase's scope has not yet been chosen.

Git is local on `main`, with initial commit `cbf5832` and no remote. Tools, caches, IDE state, generated output, logs, and development worlds are ignored. Original work uses All Rights Reserved; upstream material keeps `TEMPLATE_LICENSE.txt`.

## Maintaining the documents

Update this guide whenever implementation, setup, resources, dependencies, verification, or limitations change. Update the gameplan when scope or milestone status changes. Keep proposed features separate from working ones and mark a milestone complete only after its acceptance checks pass.

This workflow is recorded in root `AGENTS.md`. It happens alongside development without a separate reminder or background automation.
