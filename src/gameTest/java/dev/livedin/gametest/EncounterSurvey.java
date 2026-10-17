package dev.livedin.gametest;

import com.mojang.logging.LogUtils;
import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.world.NaturalEncounters;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Opt-in development run: asks Minecraft to generate real terrain; never calls scene placement. */
@EventBusSubscriber(modid = LivedIn.MOD_ID)
public final class EncounterSurvey {
    private static final List<ChunkPos> SAMPLES = new ArrayList<>();
    private static int index, waitTicks;
    private static ChunkPos current;
    private static long started;

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!Boolean.getBoolean("livedin.encounterSurvey") || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) return;
        if (SAMPLES.isEmpty()) {
            started = System.nanoTime();
            level.getGameRules().getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
            level.setDayTime(6000);
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
            int radius = Integer.getInteger("livedin.surveyRadius", 5);
            var selected = new java.util.LinkedHashSet<ChunkPos>();
            for (int x = -radius; x < radius; x++) for (int z = -radius; z < radius; z++) {
                selected.add(NaturalEncounters.candidate(level.getSeed(), x, z));
                for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++)
                    selected.add(NaturalEncounters.landCandidate(level.getSeed(), x * 2 + dx, z * 2 + dz));
            }
            SAMPLES.addAll(selected);
        }
        if (current == null) {
            if (index == SAMPLES.size()) {
                LogUtils.getLogger().info("ENCOUNTER_SURVEY finished seed={} samples={} seconds={}",
                        level.getSeed(), index, (System.nanoTime() - started) / 1e9);
                level.getServer().halt(false);
                return;
            }
            current = SAMPLES.get(index++);
            level.setChunkForced(current.x, current.z, true);
            waitTicks = 0;
            return;
        }
        var chunk = level.getChunkSource().getChunkNow(current.x, current.z);
        if (++waitTicks > 1200) throw new IllegalStateException("Survey chunk timed out: " + current);
        if (chunk == null || !chunk.hasData(NaturalEncounters.STATE.get())
                || chunk.getData(NaturalEncounters.STATE.get()) != NaturalEncounters.PROCESSED
                || !level.areEntitiesLoaded(current.toLong()) || waitTicks < 15) return;
        var bounds = new AABB(current.getMinBlockX(), level.getMinBuildHeight(), current.getMinBlockZ(),
                current.getMaxBlockX() + 1, level.getMaxBuildHeight(), current.getMaxBlockZ() + 1);
        var npcs = level.getEntitiesOfClass(AmbientNpc.class, bounds);
        if (NaturalEncounters.process(level, chunk)) throw new IllegalStateException("Repeated initialization");
        for (AmbientNpc npc : npcs) LogUtils.getLogger().info("ENCOUNTER_SURVEY npc seed={} uuid={} role={} pos={} biome={}",
                level.getSeed(), npc.getUUID(), npc.getRole(), npc.blockPosition().toShortString(),
                level.getBiome(npc.blockPosition()).unwrapKey().orElseThrow().location());
        LogUtils.getLogger().info("ENCOUNTER_SURVEY sample seed={} chunk={} members={} referencedStructure={}",
                level.getSeed(), current, npcs.size(), chunk.getAllReferences().values().stream().anyMatch(s -> !s.isEmpty()));
        level.setChunkForced(current.x, current.z, false);
        current = null;
    }
}
