package dev.livedin.entity.activity;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** One traveler's current activity. Minecraft hooks stay in AmbientNpc and delegate here. */
public interface NpcActivity {
    NpcRole role();
    default int radius() { return AmbientNpc.ACTIVITY_RADIUS; }
    default boolean allowsStrolling() { return true; }
    default boolean isSafePosition(AmbientNpc npc, BlockPos pos) { return true; }
    default boolean canHoldPosition(AmbientNpc npc) { return false; }
    default void holdPosition(AmbientNpc npc) {}
    @Nullable default BlockPos idleLookTarget() { return null; }
    default void onAssigned(AmbientNpc npc) {}
    default void tick(AmbientNpc npc) {}
    default void onStopped(AmbientNpc npc) {}
    default void onPaused(AmbientNpc npc) { onStopped(npc); }
    default void onResumed(AmbientNpc npc) { onAssigned(npc); }
    default void save(CompoundTag tag) {}
    default void load(CompoundTag tag) {}
}
