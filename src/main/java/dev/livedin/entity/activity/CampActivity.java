package dev.livedin.entity.activity;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import dev.livedin.registry.ModContent;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.CampfireBlock;

/** Fire association, clearance, looking, and musician performance for a camp member. */
public final class CampActivity implements NpcActivity {
    private static final EntityDataAccessor<Boolean> PERFORMING =
            AmbientNpc.activityDataKey(EntityDataSerializers.BOOLEAN);
    private final NpcRole role;
    @Nullable private BlockPos fire;

    public CampActivity(NpcRole role, @Nullable BlockPos fire) {
        if (role != NpcRole.MUSICIAN && role != NpcRole.CAMP_COMPANION)
            throw new IllegalArgumentException("Camp activity requires a camp role");
        this.role = role;
        this.fire = fire == null ? null : fire.immutable();
    }

    public static void initializeSynchedData() {} // Invoking this initializes the static key before entity construction.
    public static void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(PERFORMING, false); }
    public static boolean isPerforming(AmbientNpc npc) {
        return npc.getActivity() instanceof CampActivity && npc.getEntityData().get(PERFORMING);
    }
    @Override public NpcRole role() { return role; }
    @Nullable public BlockPos fire() { return fire; }
    @Override public int radius() { return role == NpcRole.MUSICIAN ? 1 : fire == null ? 6 : 2; }
    @Override public boolean allowsStrolling() { return role != NpcRole.MUSICIAN; }
    @Override public BlockPos idleLookTarget() { return fire; }

    @Override public boolean isSafePosition(AmbientNpc npc, BlockPos pos) {
        if (fire == null) return true;
        double dx = (double) pos.getX() - fire.getX(), dz = (double) pos.getZ() - fire.getZ();
        return dx * dx + dz * dz >= 4;
    }

    public boolean canPerform(AmbientNpc npc) {
        if (role != NpcRole.MUSICIAN || fire == null || !npc.isAlive() || npc.isPassenger() || npc.isInWater()
                || !npc.isWithinActivityArea(npc.blockPosition())
                || !npc.getOffhandItem().is(ModContent.LUTE.get()) || !npc.level().hasChunkAt(fire)) return false;
        var state = npc.level().getBlockState(fire);
        return state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT)
                && !state.getValue(CampfireBlock.WATERLOGGED);
    }

    @Override public boolean canHoldPosition(AmbientNpc npc) { return canPerform(npc); }
    @Override public void holdPosition(AmbientNpc npc) {
        if (fire != null)
            npc.getLookControl().setLookAt(fire.getX() + 0.5, npc.getEyeY() - 0.3, fire.getZ() + 0.5, 30, 20);
    }
    @Override public void onAssigned(AmbientNpc npc) { npc.getEntityData().set(PERFORMING, false); }
    @Override public void tick(AmbientNpc npc) { npc.getEntityData().set(PERFORMING, canPerform(npc)); }
    @Override public void onStopped(AmbientNpc npc) { npc.getEntityData().set(PERFORMING, false); }
    @Override public void save(CompoundTag tag) { if (fire != null) tag.putLong("CampfireSpot", fire.asLong()); }
    @Override public void load(CompoundTag tag) {
        fire = tag.contains("CampfireSpot", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("CampfireSpot")) : null;
    }
}
