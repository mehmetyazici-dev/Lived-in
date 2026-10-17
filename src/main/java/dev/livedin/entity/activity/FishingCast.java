package dev.livedin.entity.activity;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.scene.FishingCycle;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

/** Shared cast presentation for bank and boat activities; owns no pet or movement behavior. */
public final class FishingCast {
    private static final EntityDataAccessor<Optional<BlockPos>> WATER =
            AmbientNpc.activityDataKey(EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> CYCLE_TICK =
            AmbientNpc.activityDataKey(EntityDataSerializers.INT);

    public static void initializeSynchedData() {}
    public static void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WATER, Optional.empty()); builder.define(CYCLE_TICK, -1);
    }
    @Nullable public static BlockPos water(AmbientNpc npc) { return npc.getEntityData().get(WATER).orElse(null); }
    public static int cycleTick(AmbientNpc npc) { return npc.getEntityData().get(CYCLE_TICK); }
    public void assign(AmbientNpc npc, @Nullable BlockPos water) {
        npc.getEntityData().set(WATER, Optional.ofNullable(water)); npc.getEntityData().set(CYCLE_TICK, -1);
    }
    public void advance(AmbientNpc npc, boolean active) {
        npc.getEntityData().set(CYCLE_TICK, active ? (cycleTick(npc) + 1) % FishingCycle.LENGTH : -1);
    }
    /** Lets an activity hold the waiting pose longer without changing the shared animation. */
    public void showFrame(AmbientNpc npc, int frame) { npc.getEntityData().set(CYCLE_TICK, frame); }
    public void clear(AmbientNpc npc) { assign(npc, null); }
}
