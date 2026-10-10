package dev.livedin.entity;

import dev.livedin.entity.goal.LookAtCampfireGoal;
import dev.livedin.entity.goal.PerformAtCampfireGoal;
import dev.livedin.entity.goal.SceneStrollGoal;
import dev.livedin.entity.goal.ReturnToActivitySpotGoal;
import dev.livedin.entity.goal.FishAtWaterGoal;
import dev.livedin.scene.FishingScene;
import dev.livedin.scene.FishingCycle;
import java.util.Optional;
import java.util.UUID;
import dev.livedin.registry.ModContent;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.pathfinder.Path;

/** Shared passive traveler; saved roles add activities without duplicating the entity. */
public class AmbientNpc extends PathfinderMob {
    public static final int ACTIVITY_RADIUS = 6;
    private static final EntityDataAccessor<Integer> ROLE =
            SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PERFORMING =
            SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.BOOLEAN);
    @Nullable private BlockPos activitySpot;
    @Nullable private BlockPos campfireSpot;
    private static final EntityDataAccessor<Optional<BlockPos>> FISHING_WATER =
            SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> FISHING_TICK =
            SynchedEntityData.defineId(AmbientNpc.class, EntityDataSerializers.INT);
    @Nullable private UUID petCatId;
    @Nullable private BlockPos petSpot;
    @Nullable private UUID sceneBoatId;
    @Nullable private BlockPos boatStart;
    @Nullable private BlockPos boatEnd;
    private boolean boatHeadingToEnd = true;
    private int boatPauseTicks;
    private boolean boatRowing;

    public AmbientNpc(EntityType<? extends AmbientNpc> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, NpcRole.TRAVELER.id());
        builder.define(PERFORMING, false);
        builder.define(FISHING_WATER, Optional.empty());
        builder.define(FISHING_TICK, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PerformAtCampfireGoal(this));
        goalSelector.addGoal(1, new FishAtWaterGoal(this));
        goalSelector.addGoal(2, new ReturnToActivitySpotGoal(this, 1.0));
        goalSelector.addGoal(3, new SceneStrollGoal(this, 0.7));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new LookAtCampfireGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        // Constructors run before spawn positioning; capture the final location on the server.
        if (!level().isClientSide && activitySpot == null) {
            setActivitySpot(blockPosition());
        }
        super.tick();
        if (!level().isClientSide) {
            if (getRole() == NpcRole.BOAT_RIDER && (!isPassenger()
                    || !getVehicle().getUUID().equals(sceneBoatId))) recoverFromLostBoat();
            entityData.set(PERFORMING, canPerform());
            int fishingTick = getFishingTick();
            entityData.set(FISHING_TICK, canFish() ? (fishingTick + 1) % FishingCycle.LENGTH : -1);
            if (getRole() == NpcRole.FISHER && tickCount % 20 == 0) keepPetNearby();
        }
    }

    public NpcRole getRole() { return NpcRole.fromId(entityData.get(ROLE)); }
    public boolean isPerforming() { return entityData.get(PERFORMING); }
    @Nullable public BlockPos getActivitySpot() { return activitySpot; }
    @Nullable public BlockPos getCampfireSpot() { return campfireSpot; }
    @Nullable public BlockPos getFishingWater() { return entityData.get(FISHING_WATER).orElse(null); }
    public int getFishingTick() { return entityData.get(FISHING_TICK); }
    @Nullable public UUID getPetCatId() { return petCatId; }
    @Nullable public BlockPos getPetSpot() { return petSpot; }
    @Nullable public UUID getSceneBoatId() { return sceneBoatId; }
    @Nullable public BlockPos getBoatStart() { return boatStart; }
    @Nullable public BlockPos getBoatEnd() { return boatEnd; }
    public boolean isBoatHeadingToEnd() { return boatHeadingToEnd; }
    public void reverseBoatRoute() { boatHeadingToEnd = !boatHeadingToEnd; }
    public int getBoatPauseTicks() { return boatPauseTicks; }
    public void setBoatPauseTicks(int ticks) { boatPauseTicks = ticks; }
    public boolean isBoatRowing() { return boatRowing; }
    public void setBoatRowing(boolean rowing) { boatRowing = rowing; }

    public void assignBoatScene(BlockPos start, BlockPos end, UUID boatId) {
        entityData.set(ROLE, NpcRole.BOAT_RIDER.id());
        sceneBoatId = boatId;
        boatStart = start.immutable(); boatEnd = end.immutable();
        boatHeadingToEnd = true; boatPauseTicks = 0; boatRowing = false;
        setActivitySpot(start.above());
    }

    private void recoverFromLostBoat() {
        // Stop only our old, empty, already loaded boat. Never interfere with a player's vehicle.
        if (level() instanceof ServerLevel server && sceneBoatId != null
                && server.getEntity(sceneBoatId) instanceof net.minecraft.world.entity.vehicle.Boat boat
                && !boat.isVehicle()) {
            boat.setDeltaMovement(0, boat.getDeltaMovement().y, 0);
            boat.setPaddleState(false, false);
        }
        if (!isPassenger()) {
            BlockPos origin = blockPosition();
            // One-time local recovery, not pathfinding across a river or replacement spawning.
            outer: for (int radius = 0; radius <= 5; radius++)
                for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                    for (int y = -1; y <= 2; y++) {
                        BlockPos shore = origin.offset(x, y, z);
                        if (FishingScene.safeShore(level(), shore) && level().noCollision(this,
                                getBoundingBox().move(Vec3.atBottomCenterOf(shore).subtract(position())))) {
                            teleportTo(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5);
                            setDeltaMovement(Vec3.ZERO);
                            break outer;
                        }
                    }
        }
        entityData.set(ROLE, NpcRole.TRAVELER.id());
        sceneBoatId = null; boatStart = null; boatEnd = null; boatRowing = false; boatPauseTicks = 0;
        getNavigation().stop();
        setActivitySpot(blockPosition());
    }

    public void assignFishingScene(BlockPos shore, BlockPos water, UUID catId, BlockPos catSpot) {
        entityData.set(ROLE, NpcRole.FISHER.id());
        campfireSpot = null;
        entityData.set(FISHING_WATER, Optional.of(water.immutable()));
        entityData.set(FISHING_TICK, -1);
        petCatId = catId;
        petSpot = catSpot.immutable();
        setActivitySpot(shore);
    }

    public boolean canFish() {
        BlockPos water = getFishingWater();
        return getRole() == NpcRole.FISHER && water != null && activitySpot != null && isAlive()
                && !isPassenger() && !isInWater() && isWithinActivityArea(blockPosition())
                && getMainHandItem().is(Items.FISHING_ROD)
                && FishingScene.validFishingSpot(level(), activitySpot, water);
    }

    private void keepPetNearby() {
        // Only use the already loaded, original cat. Missing or dead pets are never recreated.
        if (!(level() instanceof ServerLevel server) || petCatId == null || petSpot == null || !isAlive()
                || !(server.getEntity(petCatId) instanceof Cat cat) || !cat.isAlive()
                || !getUUID().equals(cat.getOwnerUUID())) return;
        cat.setOrderedToSit(true);
        if (cat.distanceToSqr(Vec3.atBottomCenterOf(petSpot)) > 2.25 && FishingScene.safeShore(level(), petSpot)) {
            Vec3 spot = Vec3.atBottomCenterOf(petSpot);
            cat.teleportTo(spot.x, spot.y, spot.z);
            cat.setDeltaMovement(Vec3.ZERO);
            cat.getNavigation().stop();
        }
    }

    public void assignCampRole(NpcRole role, BlockPos fire, BlockPos spot) {
        entityData.set(ROLE, role.id());
        campfireSpot = fire.immutable();
        setActivitySpot(spot);
    }

    public void setActivitySpot(BlockPos pos) {
        activitySpot = pos.immutable();
        restrictTo(activitySpot, activityRadius());
        getNavigation().stop();
    }

    private int activityRadius() {
        if (getRole() == NpcRole.MUSICIAN || getRole() == NpcRole.FISHER) return 1;
        return campfireSpot == null ? ACTIVITY_RADIUS : 2;
    }

    public boolean isWithinActivityArea(BlockPos pos) {
        int radius = activityRadius();
        return activitySpot != null && activitySpot.distSqr(pos) < radius * radius && isSafeCampPosition(pos)
                && (getRole() != NpcRole.FISHER || FishingScene.safeShore(level(), pos));
    }

    public boolean isSafeCampPosition(BlockPos pos) {
        if (campfireSpot == null) return true;
        double dx = pos.getX() - campfireSpot.getX();
        double dz = pos.getZ() - campfireSpot.getZ();
        return dx * dx + dz * dz >= 4;
    }

    public boolean isSafeCampPath(Path path) {
        for (int i = 0; i < path.getNodeCount(); i++) {
            if (!isSafeCampPosition(path.getNode(i).asBlockPos())) return false;
            if (getRole() == NpcRole.FISHER && !FishingScene.safeShore(level(), path.getNode(i).asBlockPos())) return false;
        }
        return true;
    }

    /** Server evaluates the real fire and instrument; clients receive only the resulting flag. */
    public boolean canPerform() {
        if (getRole() != NpcRole.MUSICIAN || campfireSpot == null || !isAlive() || isPassenger()
                || isInWater() || !isWithinActivityArea(blockPosition())
                || !getOffhandItem().is(ModContent.LUTE.get()) || !level().hasChunkAt(campfireSpot)) {
            return false;
        }
        var fire = level().getBlockState(campfireSpot);
        return fire.is(BlockTags.CAMPFIRES) && fire.getValue(CampfireBlock.LIT)
                && !fire.getValue(CampfireBlock.WATERLOGGED);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (activitySpot != null) tag.putLong("ActivitySpot", activitySpot.asLong());
        if (campfireSpot != null) tag.putLong("CampfireSpot", campfireSpot.asLong());
        tag.putInt("NpcRole", getRole().id());
        if (getFishingWater() != null) tag.putLong("FishingWater", getFishingWater().asLong());
        if (petCatId != null) tag.putUUID("PetCat", petCatId);
        if (petSpot != null) tag.putLong("PetSpot", petSpot.asLong());
        if (sceneBoatId != null) tag.putUUID("SceneBoat", sceneBoatId);
        if (boatStart != null) tag.putLong("BoatStart", boatStart.asLong());
        if (boatEnd != null) tag.putLong("BoatEnd", boatEnd.asLong());
        tag.putBoolean("BoatHeadingToEnd", boatHeadingToEnd);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, NpcRole.fromId(tag.getInt("NpcRole")).id());
        entityData.set(PERFORMING, false);
        campfireSpot = tag.contains("CampfireSpot", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("CampfireSpot")) : null;
        entityData.set(FISHING_WATER, tag.contains("FishingWater", Tag.TAG_LONG)
                ? Optional.of(BlockPos.of(tag.getLong("FishingWater"))) : Optional.empty());
        entityData.set(FISHING_TICK, -1);
        petCatId = tag.hasUUID("PetCat") ? tag.getUUID("PetCat") : null;
        petSpot = tag.contains("PetSpot", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("PetSpot")) : null;
        sceneBoatId = tag.hasUUID("SceneBoat") ? tag.getUUID("SceneBoat") : null;
        boatStart = tag.contains("BoatStart", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("BoatStart")) : null;
        boatEnd = tag.contains("BoatEnd", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("BoatEnd")) : null;
        boatHeadingToEnd = !tag.contains("BoatHeadingToEnd") || tag.getBoolean("BoatHeadingToEnd");
        boatPauseTicks = 20; boatRowing = false;
        activitySpot = null;
        if (tag.contains("ActivitySpot", Tag.TAG_LONG)) setActivitySpot(BlockPos.of(tag.getLong("ActivitySpot")));
        // Equipment is restored by vanilla, not recreated, so removed instruments stay removed.
        setPersistenceRequired();
    }
}
