package dev.livedin.entity.activity;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import dev.livedin.scene.FishingScene;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Fishing target, cycle, bank behavior, and association with the original cat. */
public final class FishingActivity implements NpcActivity {
    private final FishingCast cast = new FishingCast();
    @Nullable private BlockPos water;
    @Nullable private UUID petCatId;
    @Nullable private BlockPos petSpot;
    private boolean checkLegacyBank;

    public FishingActivity() {}
    public FishingActivity(BlockPos water, UUID petCatId, BlockPos petSpot) {
        this.water = water.immutable(); this.petCatId = petCatId; this.petSpot = petSpot.immutable();
    }
    public static void initializeSynchedData() { FishingCast.initializeSynchedData(); }
    public static void defineSynchedData(SynchedEntityData.Builder builder) {
        FishingCast.defineSynchedData(builder);
    }
    @Nullable public static BlockPos water(AmbientNpc npc) { return FishingCast.water(npc); }
    public static int cycleTick(AmbientNpc npc) { return FishingCast.cycleTick(npc); }
    @Nullable public UUID petCatId() { return petCatId; }
    @Nullable public BlockPos petSpot() { return petSpot; }
    @Override public NpcRole role() { return NpcRole.FISHER; }
    @Override public int radius() { return 1; }
    @Override public boolean allowsStrolling() { return false; }
    @Override public boolean isSafePosition(AmbientNpc npc, BlockPos pos) { return FishingScene.safeShore(npc.level(), pos); }

    @Override public boolean canHoldPosition(AmbientNpc npc) {
        return water != null && !npc.isPassenger() && !npc.isInWater() && npc.isWithinActivityArea(npc.blockPosition());
    }
    @Override public void holdPosition(AmbientNpc npc) {
        if (water == null) return;
        npc.getNavigation().stop();
        float yaw = (float)(Mth.atan2(water.getZ() + 0.5 - npc.getZ(), water.getX() + 0.5 - npc.getX()) * 180 / Math.PI) - 90;
        npc.setYRot(yaw); npc.yBodyRot = yaw;
        npc.getLookControl().setLookAt(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 30, 30);
    }
    public boolean canFish(AmbientNpc npc) {
        return water != null && npc.getActivitySpot() != null && npc.isAlive() && !npc.isPassenger() && !npc.isInWater()
                && npc.isWithinActivityArea(npc.blockPosition()) && npc.getMainHandItem().is(Items.FISHING_ROD)
                && FishingScene.validFishingSpot(npc.level(), npc.getActivitySpot(), water);
    }
    @Override public void onAssigned(AmbientNpc npc) {
        cast.assign(npc, water);
    }
    @Override public void tick(AmbientNpc npc) {
        if (checkLegacyBank) { checkLegacyBank = false; repairLegacyBank(npc); }
        cast.advance(npc, canFish(npc));
        if (npc.tickCount % 20 == 0) keepPetNearby(npc);
    }
    @Override public void onStopped(AmbientNpc npc) {
        cast.clear(npc);
    }
    private void repairLegacyBank(AmbientNpc npc) {
        BlockPos origin = npc.getActivitySpot();
        if (origin == null || water == null || npc.isPassenger() || !npc.isAlive()
                || FishingScene.waterfront(npc.level(), origin, water)
                || npc.distanceToSqr(Vec3.atBottomCenterOf(origin)) > 64) return;
        // One bounded correction for older inland scenes. Read loaded terrain; create no replacements.
        for (int radius = 0; radius <= 6; radius++)
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                for (int dy : new int[] {0, -1, 1}) {
                    BlockPos bank = origin.offset(x, dy, z);
                    if (!FishingScene.safeShore(npc.level(), bank)
                            || !npc.level().noCollision(npc, npc.getBoundingBox()
                                    .move(Vec3.atBottomCenterOf(bank).subtract(npc.position())))) continue;
                    for (Direction direction : Direction.Plane.HORIZONTAL) for (int distance = 3; distance <= 5; distance++) {
                        BlockPos target = bank.relative(direction, distance).below();
                        if (!FishingScene.validFishingSpot(npc.level(), bank, target)) continue;
                        water = target; petSpot = FishingScene.findPetSpot(npc.level(), bank, target);
                        npc.getNavigation().stop();
                        npc.setActivitySpot(bank);
                        npc.teleportTo(bank.getX() + 0.5, bank.getY(), bank.getZ() + 0.5);
                        npc.setDeltaMovement(Vec3.ZERO);
                        cast.assign(npc, water);
                        keepPetNearby(npc);
                        return;
                    }
                }
            }
    }
    private void keepPetNearby(AmbientNpc npc) {
        if (!(npc.level() instanceof ServerLevel server) || petCatId == null || petSpot == null || !npc.isAlive()
                || !(server.getEntity(petCatId) instanceof Cat cat) || !cat.isAlive()
                || !npc.getUUID().equals(cat.getOwnerUUID())) return;
        cat.setOrderedToSit(true);
        if (cat.distanceToSqr(Vec3.atBottomCenterOf(petSpot)) > 2.25 && FishingScene.safeShore(npc.level(), petSpot)) {
            Vec3 spot = Vec3.atBottomCenterOf(petSpot);
            cat.teleportTo(spot.x, spot.y, spot.z); cat.setDeltaMovement(Vec3.ZERO); cat.getNavigation().stop();
        }
    }
    @Override public void save(CompoundTag tag) {
        if (water != null) tag.putLong("FishingWater", water.asLong());
        if (petCatId != null) tag.putUUID("PetCat", petCatId);
        if (petSpot != null) tag.putLong("PetSpot", petSpot.asLong());
        tag.putInt("FishingBankVersion", 1);
    }
    @Override public void load(CompoundTag tag) {
        water = tag.contains("FishingWater", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("FishingWater")) : null;
        petCatId = tag.hasUUID("PetCat") ? tag.getUUID("PetCat") : null;
        petSpot = tag.contains("PetSpot", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("PetSpot")) : null;
        checkLegacyBank = !tag.contains("FishingBankVersion");
    }
}
