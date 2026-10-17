package dev.livedin.entity.activity;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import dev.livedin.scene.BoatScene;
import dev.livedin.scene.FishingScene;
import dev.livedin.scene.FishingCycle;
import java.util.UUID;
import java.util.List;
import net.minecraft.core.Direction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Owns one boat and its fish, retrieve, random-water-trip cycle. */
public final class BoatActivity implements NpcActivity {
    public static final double SPEED = 0.075;
    @Nullable private UUID boatId;
    @Nullable private BlockPos start;
    @Nullable private BlockPos end;
    private boolean headingToEnd = true;
    public static final int FISH_TICKS = 25 * 20;
    public static final int RETRIEVE_TICKS = 20;
    private int retryTicks;
    private boolean traveling;
    private List<BlockPos> path = List.of();
    private int waypoint;
    @Nullable private BlockPos travelTarget;
    private boolean rowing;
    private final FishingCast cast = new FishingCast();
    private int fishingTicks = -1;
    @Nullable private BlockPos fishingWater;
    private boolean upgradeLegacyEquipment;

    public BoatActivity() {}
    public BoatActivity(BlockPos start, BlockPos end, UUID boatId) {
        this.start = start.immutable(); this.end = end.immutable(); this.boatId = boatId;
    }
    @Override public NpcRole role() { return NpcRole.BOAT_RIDER; }
    @Override public boolean allowsStrolling() { return false; }
    @Nullable public UUID boatId() { return boatId; }
    @Nullable public BlockPos start() { return start; }
    @Nullable public BlockPos end() { return end; }
    public boolean headingToEnd() { return headingToEnd; }
    @Nullable public BlockPos travelTarget() { return travelTarget; }
    public boolean traveling() { return traveling; }
    public boolean controls(Boat boat) { return boat.getUUID().equals(boatId); }

    @Override public void tick(AmbientNpc npc) {
        if (!npc.isPassenger() || !npc.getVehicle().getUUID().equals(boatId)) { recover(npc); return; }
        if (fishingTicks >= 0) {
            if (!npc.isAlive() || !npc.getMainHandItem().is(Items.FISHING_ROD) || fishingWater == null
                    || !FishingScene.clearCast(npc.level(), npc.position().add(0, 1.2, 0), fishingWater)) {
                finishFishing(npc);
                return;
            }
            // Keep the existing client animation: prepare/cast once, hold, then visibly retrieve.
            int frame = fishingTicks < FishingCycle.WAIT_START ? fishingTicks
                    : fishingTicks < FISH_TICKS ? FishingCycle.WAIT_START
                    : FishingCycle.RETRIEVE_START + fishingTicks - FISH_TICKS;
            cast.showFrame(npc, frame);
            if (++fishingTicks >= FISH_TICKS + RETRIEVE_TICKS) finishFishing(npc);
        }
    }
    private void finishFishing(AmbientNpc npc) {
        fishingTicks = -1; fishingWater = null; cast.clear(npc);
        traveling = true; path = List.of(); waypoint = 0; travelTarget = null;
    }
    @Override public void onAssigned(AmbientNpc npc) {
        cast.clear(npc);
        if (upgradeLegacyEquipment && npc.getMainHandItem().isEmpty()) {
            npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
            npc.setDropChance(EquipmentSlot.MAINHAND, 0);
        }
        upgradeLegacyEquipment = false;
    }
    @Override public void onStopped(AmbientNpc npc) {
        // Release only our own mount; leave unrelated/player-controlled vehicles alone.
        if (npc.getVehicle() instanceof Boat boat && controls(boat)) npc.stopRiding();
        if (npc.level() instanceof ServerLevel server && boatId != null
                && server.getEntity(boatId) instanceof Boat boat && !boat.isVehicle()) {
            boat.setDeltaMovement(0, boat.getDeltaMovement().y, 0);
            boat.setPaddleState(false, false);
        }
        rowing = false;
        fishingTicks = -1; fishingWater = null; cast.clear(npc);
        traveling = false; path = List.of(); travelTarget = null;
    }
    @Override public void onPaused(AmbientNpc npc) {
        rowing = false; fishingTicks = -1; fishingWater = null; cast.clear(npc);
        path = List.of(); travelTarget = null; traveling = false;
    }
    private void recover(AmbientNpc npc) {
        if (!npc.isPassenger()) {
            BlockPos origin = npc.blockPosition();
            outer: for (int radius = 0; radius <= 5; radius++)
                for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                    for (int y = -1; y <= 2; y++) {
                        BlockPos shore = origin.offset(x, y, z);
                        if (FishingScene.safeShore(npc.level(), shore) && npc.level().noCollision(npc,
                                npc.getBoundingBox().move(Vec3.atBottomCenterOf(shore).subtract(npc.position())))) {
                            npc.teleportTo(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5);
                            npc.setDeltaMovement(Vec3.ZERO);
                            break outer;
                        }
                    }
        }
        npc.setActivity(new TravelerActivity(), npc.blockPosition());
    }

    public void beforeBoatTick(AmbientNpc npc, Boat boat) {
        rowing = false;
        npc.getNavigation().stop();
        boat.setDeltaMovement(0, boat.getDeltaMovement().y, 0);
        if (start == null || boat.getY() < start.getY() + 0.2 || boat.getY() > start.getY() + 1.2) return;
        if (fishingTicks >= 0) return;
        if (retryTicks > 0) { retryTicks--; return; }
        if (!traveling) {
            if (beginFishing(npc, boat)) return;
            traveling = true;
        }
        if (path.isEmpty()) {
            path = BoatWaterNavigator.findPath(boat, start.getY(), travelTarget);
            if (path.isEmpty() && travelTarget != null)
                path = BoatWaterNavigator.findPath(boat, start.getY(), null);
            waypoint = 0;
            if (path.isEmpty()) {
                travelTarget = null;
                // A temporarily blocked hull retries departure; a tiny safe pond can fish again.
                traveling = !BoatWaterNavigator.navigable(boat, BlockPos.containing(boat.getX(), start.getY(), boat.getZ()));
                retryTicks = 40;
                return;
            }
            travelTarget = path.getLast();
        }
        Vec3 at = boat.position();
        Vec3 target = Vec3.atCenterOf(path.get(waypoint));
        Vec3 offset = new Vec3(target.x - at.x, 0, target.z - at.z);
        if (offset.lengthSqr() <= 0.01) {
            if (++waypoint == path.size()) {
                path = List.of(); travelTarget = null; traveling = false;
                beginFishing(npc, boat);
            }
            return;
        }
        Vec3 step = offset.normalize().scale(Math.min(SPEED, offset.length()));
        BlockPos current = BlockPos.containing(at.x, start.getY(), at.z);
        BlockPos next = BlockPos.containing(at.x + step.x, start.getY(), at.z + step.z);
        if (!BoatWaterNavigator.navigable(boat, current) || !BoatWaterNavigator.navigable(boat, next)
                || !boat.level().noCollision(boat, boat.getBoundingBox().expandTowards(step).inflate(0.1, 0, 0.1)
                        .expandTowards(0, 1.8, 0))) {
            path = List.of(); travelTarget = null; retryTicks = 20;
            return;
        }
        float desiredYaw = (float)(Mth.atan2(-offset.x, offset.z) * Mth.RAD_TO_DEG);
        boat.setYRot(Mth.rotLerp(0.18F, boat.getYRot(), desiredYaw));
        // Vanilla still-water boat physics multiplies horizontal speed by 0.9.
        boat.setDeltaMovement(step.x / 0.9, boat.getDeltaMovement().y, step.z / 0.9);
        rowing = true;
    }
    private boolean beginFishing(AmbientNpc npc, Boat boat) {
        if (!npc.getMainHandItem().is(Items.FISHING_ROD) || start == null) return false;
        BlockPos origin = BlockPos.containing(boat.getX(), start.getY(), boat.getZ());
        Direction forward = Direction.fromYRot(boat.getYRot());
        // Prefer the current heading; if it faces a bank, turn the entire hull before casting.
        for (Direction direction : new Direction[] {forward, forward.getClockWise(), forward.getCounterClockWise(), forward.getOpposite()}) {
            for (int distance = 3; distance >= 2; distance--) {
                BlockPos water = origin.relative(direction, distance);
                if (FishingScene.clearCast(npc.level(), npc.position().add(0, 1.2, 0), water)) {
                    boat.setYRot(direction.toYRot());
                    fishingWater = water; fishingTicks = 0;
                    cast.assign(npc, water);
                    return true;
                }
            }
        }
        return false;
    }
    public void afterBoatTick(AmbientNpc npc, Boat boat) {
        boat.setPaddleState(rowing, rowing);
        npc.setYRot(boat.getYRot()); npc.setYHeadRot(boat.getYRot()); npc.yBodyRot = boat.getYRot();
    }
    @Override public void save(CompoundTag tag) {
        if (boatId != null) tag.putUUID("SceneBoat", boatId);
        if (start != null) tag.putLong("BoatStart", start.asLong());
        if (end != null) tag.putLong("BoatEnd", end.asLong());
        tag.putBoolean("BoatHeadingToEnd", headingToEnd);
        tag.putInt("BoatFishingVersion", 1);
        if (traveling && travelTarget != null) tag.putLong("BoatTravelTarget", travelTarget.asLong());
    }
    @Override public void load(CompoundTag tag) {
        boatId = tag.hasUUID("SceneBoat") ? tag.getUUID("SceneBoat") : null;
        start = tag.contains("BoatStart", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("BoatStart")) : null;
        end = tag.contains("BoatEnd", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("BoatEnd")) : null;
        headingToEnd = !tag.contains("BoatHeadingToEnd") || tag.getBoolean("BoatHeadingToEnd");
        retryTicks = 0; rowing = false;
        fishingTicks = -1; fishingWater = null;
        path = List.of(); waypoint = 0;
        travelTarget = tag.contains("BoatTravelTarget", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("BoatTravelTarget")) : null;
        traveling = travelTarget != null;
        upgradeLegacyEquipment = !tag.contains("BoatFishingVersion");
    }
}
