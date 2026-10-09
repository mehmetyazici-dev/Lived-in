# Lived-In: travelers at dusk

Status: concept proposal. No NPCs, structures, or routines are implemented in the starter mod.

## The experience

You notice smoke above the trees as the light fades. In a clearing, three travelers are settling down for the night. One tends a fire, another sorts supplies, and a third watches the edge of the woods. As you approach, someone turns toward you and offers a short greeting. You can linger or continue your journey. Their lives appear to continue either way.

The aim is to make singleplayer exploration feel inhabited without making every encounter a task, reward screen, or fight.

## The first camp

A small clearing with two simple shelters, three sleeping places, a campfire, log seats, and a modest supply area. Use vanilla blocks and a restrained palette first. Paths between beds, activity spots, and the fire must be clear enough for NPC navigation. Supplies are scenery initially; there is no generated reward chest or economy.

Start with three human adventurers using one shared NPC implementation and visual variations. Their working roles suggest activity rather than separate gameplay systems:

| Traveler | Daytime activity | Evening activity | Night |
| --- | --- | --- | --- |
| Trail scout | Walk a short route around the camp | Return and watch arrivals | Rest at their assigned bed |
| Camp cook | Move between supplies and the fire | Tend the fire and gather with the others | Rest at their assigned bed |
| Mender | Work near shelter and supplies | Join the group by the fire | Rest at their assigned bed |

The first prototype should use simple movement, looking, and idle poses. Cooking and mending initially communicate intent; they do not require crafting, inventory simulation, or custom animations.

## Interaction and atmosphere

- Residents acknowledge a nearby player through a glance and a brief written greeting. Repeat greetings need a cooldown so the camp stays quiet.
- Direct interaction can offer a short authored line, such as "There's room by the fire." No dialogue tree, online AI, quest requirement, or trade UI in the first prototype.
- Keep the camp stationary initially. "Travelers" describes its inhabitants; migration is a later possibility.
- Let fire, shelter, movement, and routine carry the atmosphere. Avoid constant chatter.
- When world generation arrives, camps should be occasional discoveries with substantial wilderness between them. Determine spacing through exploration playtests.

## Design priorities

1. Visible purpose: a few believable activities matter more than a crowd.
2. Minecraft fit: blocky silhouettes, vanilla-scale structures, and readable behavior.
3. Spacious wilderness: population should make exploration warmer without crowding the world.
4. Player freedom: encounters are optional and do not interrupt ordinary survival play.
5. Bounded simulation: run routines for loaded NPCs; avoid forced chunks and world-wide pathfinding.

## Milestones

1. **Foundation (this phase):** working NeoForge starter, repeatable build/run workflow, and this brief.
2. **One camp, one traveler:** manually place a test camp and spawn one NPC; verify navigation, interaction, persistence, and save/reload behavior.
3. **A lived-in evening:** three travelers sharing one behavior system, assigned activity and sleep spots, time-of-day transitions, and restrained greetings. Test blocked paths and missing beds.
4. **Discovery:** add natural camp placement in suitable terrain and newly generated chunks. Test terrain fit, resident duplication on chunk reload, encounter density, and performance.
5. **A wider world:** vanilla-style hamlets, more human adventurers, and fantasy communities with distinct architecture and customs. Reuse proven behavior before expanding it.

## Later prototype acceptance targets

At dusk the player can approach the camp, see residents converge on the fire, receive a greeting once, and leave without taking a quest. Residents return to their activities and rest at night. Saving and reloading preserves residents without duplication. Unloaded camps do not keep chunks active.

These targets guide the next planning discussion; they are not claims about the current starter.

## Deferred

Quests, factions, reputation, combat roles, companions, trading economies, settlement growth, long-distance travel, voice acting, fantasy species assets, custom animation libraries, and Forge/multi-version ports.

Before implementing the prototype, refine the travelers' appearance, threat response, and exact daily timings together. These choices do not block the environment setup.
