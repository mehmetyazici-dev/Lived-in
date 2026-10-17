package dev.livedin.gametest;

import com.mojang.logging.LogUtils;
import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.housing.Neighborhoods;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Development only: loads fresh normal terrain; production hooks choose and construct homes. */
@EventBusSubscriber(modid = LivedIn.MOD_ID)
public final class HousingSurvey {
    private static int regionIndex, ticks;
    private static ChunkPos center;
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (!Boolean.getBoolean("livedin.housingSurvey") || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (center == null) {
            if (regionIndex == 12) {
                LogUtils.getLogger().info("HOUSING_SURVEY complete seed={} homes={}", level.getSeed(), Neighborhoods.get(level).homeCount());
                if (Neighborhoods.get(level).homeCount() == 0) throw new IllegalStateException("No naturally generated homes in survey");
                level.getServer().halt(false); return;
            }
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer()); level.setDayTime(6000);
            center = new ChunkPos(Neighborhoods.center(regionIndex % 4 - 2, regionIndex / 4 - 1));
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) level.setChunkForced(center.x + x, center.z + z, true);
            ticks = 0; return;
        }
        if (++ticks < 180) return;
        LogUtils.getLogger().info("HOUSING_SURVEY region={} center={} homes={} processed={}", regionIndex, center,
                Neighborhoods.get(level).homeCount(), Neighborhoods.get(level).save(new net.minecraft.nbt.CompoundTag(), level.registryAccess()).getList("Processed", 8).size());
        for (AmbientNpc npc : level.getEntitiesOfClass(AmbientNpc.class, new net.minecraft.world.phys.AABB(center.getMiddleBlockX()-80, -64,
                center.getMiddleBlockZ()-80, center.getMiddleBlockX()+80, 320, center.getMiddleBlockZ()+80))) {
            var home = Neighborhoods.get(level).home(npc.getUUID());
            LogUtils.getLogger().info("HOUSING_SURVEY npc={} pos={} home={} request={}", npc.getUUID(), npc.blockPosition().toShortString(),
                    home == null ? "none" : home.origin().toShortString(), npc.getProfile().wantsNaturalHome());
        }
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) level.setChunkForced(center.x + x, center.z + z, false);
        regionIndex++; center = null;
    }
}
