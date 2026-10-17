package dev.livedin.entity.life;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.BoatActivity;
import dev.livedin.entity.activity.BoatWaterNavigator;
import dev.livedin.entity.activity.FishingActivity;
import dev.livedin.housing.Neighborhoods;
import dev.livedin.scene.FishingScene;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Dusk/dawn overrides pause the activity, keeping its original cat, boat, and daytime anchor. */
@net.neoforged.fml.common.EventBusSubscriber(modid = dev.livedin.LivedIn.MOD_ID)
public final class NpcRoutine {
    @net.neoforged.bus.api.SubscribeEvent public static void campDrops(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (event.getState().is(Blocks.CAMPFIRE) && event.getBlockEntity() != null
                && event.getBlockEntity().getPersistentData().hasUUID("LivedInCampOwner")) event.setCanceled(true);
    }
    private boolean active;
    private boolean returning;
    private boolean fallback;
    private int stalled;
    private BlockPos fire;
    private BlockPos campSpot;
    private BlockPos dock;
    private List<BlockPos> waterPath = List.of();
    public boolean active() { return active || returning; }
    public static boolean night(long dayTime) { return Math.floorMod(dayTime, 24000) >= 12000; }
    public boolean needsWeapon(AmbientNpc npc) {
        return active && (npc.getActivity() instanceof BoatActivity || fallback
                || Neighborhoods.get((ServerLevel)npc.level()).home(npc.getUUID()) == null);
    }
    public boolean allowsPursuit(BlockPos target) {
        return fire == null || fire.distSqr(target) <= 36;
    }
    public boolean safeDefensePath(AmbientNpc npc, net.minecraft.world.level.pathfinder.Path path) {
        for (int i = 0; i < path.getNodeCount(); i++) {
            BlockPos pos = path.getNode(i).asBlockPos();
            if (!allowsPursuit(pos) || !FishingScene.safeShore(npc.level(), pos)
                    || (fire != null && fire.distSqr(pos) < 4)
                    || (!active() && !npc.getActivity().isSafePosition(npc, pos))) return false;
        }
        return true;
    }
    public void prepare(AmbientNpc npc, long dayTime) {
        // Retry a saved cleanup even after the return journey finished while the fire chunk was unloaded.
        if (!night(dayTime)) cleanupFire(npc);
        if (!npc.getProfile().eligibility().nightlyCamp() && !npc.getProfile().eligibility().naturalHome()) return;
        boolean night = night(dayTime);
        if (night && !active) {
            active = true; returning = false; fallback = false; stalled = 0;
            dock = null; waterPath = List.of();
            npc.getActivity().onPaused(npc); npc.getNavigation().stop(); npc.clearRestriction();
        } else if (!night && active) {
            active = false; returning = true; stalled = 0;
            if (npc.isSleeping()) npc.stopSleeping();
            cleanupFire(npc); npc.getNavigation().stop();
        }
        if (returning) cleanupFire(npc);
        if (active() && npc.getActivity() instanceof FishingActivity fishing && fishing.petCatId() != null
                && ((ServerLevel)npc.level()).getEntity(fishing.petCatId()) instanceof Cat cat
                && npc.getUUID().equals(cat.getOwnerUUID())) {
            cat.setOrderedToSit(false);
            if (npc.tickCount % 20 == 0 && cat.distanceToSqr(npc) > 4) cat.getNavigation().moveTo(npc, 1.2);
        }
    }
    public void move(AmbientNpc npc) {
        if (npc.getTarget() != null) { if (npc.isSleeping()) npc.stopSleeping(); return; }
        if (npc.isSleeping()) return;
        ServerLevel level = (ServerLevel)npc.level();
        if (npc.getVehicle() instanceof Boat) { if (returning) finish(npc); return; }
        BlockPos target;
        if (returning) {
            if (npc.getActivity() instanceof BoatActivity boatActivity && boatActivity.boatId() != null) {
                if (!(level.getEntity(boatActivity.boatId()) instanceof Boat boat)) return;
                target = dock;
                if (target != null && npc.distanceToSqr(boat) < 12 && !boat.isVehicle()) {
                    npc.startRiding(boat, true); finish(npc); return;
                }
            } else target = npc.getActivitySpot();
            if (target == null) { finish(npc); return; }
            if (npc.blockPosition().distSqr(target) < 1) { finish(npc); return; }
            walk(npc, target); return;
        }
        var home = Neighborhoods.get(level).home(npc.getUUID());
        if (home != null && !fallback && !(npc.getActivity() instanceof BoatActivity)) {
            target = home.bed();
            if (level.hasChunkAt(target) && npc.distanceToSqr(Vec3.atCenterOf(target)) < 4
                    && level.getBlockState(target).getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                npc.getNavigation().stop(); npc.startSleeping(target); return;
            }
            if (walk(npc, target)) { stalled = 0; return; }
            if (++stalled < 200) return;
            fallback = true;
        }
        if (fire == null && npc.getProfile().eligibility().nightlyCamp()) placeFire(npc);
        if (fire != null && campSpot != null && npc.blockPosition().distSqr(campSpot) > 4) {
            walk(npc, campSpot); return;
        }
        npc.getNavigation().stop();
        if (fire != null) npc.getLookControl().setLookAt(fire.getX() + .5, npc.getEyeY(), fire.getZ() + .5);
    }
    private boolean walk(AmbientNpc npc, BlockPos goal) {
        if (!npc.getNavigation().isDone()) return true;
        if (npc.tickCount % 20 != 0) return false;
        Vec3 delta = Vec3.atBottomCenterOf(goal).subtract(npc.position());
        BlockPos waypoint = delta.horizontalDistanceSqr() > 24 * 24
                ? BlockPos.containing(npc.position().add(delta.normalize().scale(24))) : goal;
        // Vanilla pathfinding gets a small fully loaded neighborhood; never request remote chunks.
        for (int x = (Math.min(npc.getBlockX(), waypoint.getX()) - 8) >> 4; x <= (Math.max(npc.getBlockX(), waypoint.getX()) + 8) >> 4; x++)
            for (int z = (Math.min(npc.getBlockZ(), waypoint.getZ()) - 8) >> 4; z <= (Math.max(npc.getBlockZ(), waypoint.getZ()) + 8) >> 4; z++)
                if (npc.level().getChunkSource().getChunk(x, z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) == null) return false;
        var path = npc.getNavigation().createPath(waypoint, 0);
        if (path == null || path.getNodeCount() < 2) return false;
        return npc.getNavigation().moveTo(path, 1.0);
    }
    private void finish(AmbientNpc npc) {
        returning = false; stalled = 0;
        if (npc.getActivitySpot() != null) npc.restrictTo(npc.getActivitySpot(), npc.getActivity().radius());
        npc.getNavigation().stop(); npc.getActivity().onResumed(npc);
    }
    private static BlockPos fireSite(net.minecraft.world.level.Level level, BlockPos standing) {
        for (int distance = 3; distance >= 2; distance--) for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = standing.relative(direction, distance);
            if (FishingScene.safeShore(level, pos) && level.getBlockState(pos).isAir()) return pos;
        }
        return null;
    }
    private void placeFire(AmbientNpc npc) {
            BlockPos pos = fireSite(npc.level(), npc.blockPosition());
            if (pos != null) {
                npc.level().setBlockAndUpdate(pos, Blocks.CAMPFIRE.defaultBlockState());
                var be = npc.level().getBlockEntity(pos);
                if (be != null) { be.getPersistentData().putUUID("LivedInCampOwner", npc.getUUID()); be.setChanged(); }
                fire = pos; campSpot = npc.blockPosition().immutable();
            }
    }
    public void cleanupFire(AmbientNpc npc) {
        if (fire == null || !npc.level().hasChunkAt(fire)) return;
        var be = npc.level().getBlockEntity(fire);
        if (npc.level().getBlockState(fire).is(Blocks.CAMPFIRE) && be != null
                && be.getPersistentData().hasUUID("LivedInCampOwner")
                && npc.getUUID().equals(be.getPersistentData().getUUID("LivedInCampOwner"))) npc.level().removeBlock(fire, false);
        fire = null; campSpot = null;
    }
    /** Boat bridge calls this instead of daytime steering while a schedule or defense overrides it. */
    public void boatTick(AmbientNpc npc, Boat boat) {
        boat.setDeltaMovement(0, boat.getDeltaMovement().y, 0); boat.setPaddleState(false, false);
        if (!active || !(npc.getActivity() instanceof BoatActivity activity) || activity.start() == null) return;
        int y = activity.start().getY();
        BlockPos water = BlockPos.containing(boat.getX(), y, boat.getZ());
        if (dock != null && npc.distanceToSqr(Vec3.atBottomCenterOf(dock)) < 9 && FishingScene.safeShore(npc.level(), dock)) {
            npc.stopRiding(); npc.teleportTo(dock.getX() + .5, dock.getY(), dock.getZ() + .5); npc.setDeltaMovement(Vec3.ZERO); return;
        }
        if (waterPath.isEmpty()) {
            if (npc.tickCount % 40 != 0) return;
            var landing = BoatWaterNavigator.findLanding(boat, y,
                    shore -> FishingScene.safeShore(npc.level(), shore) && fireSite(npc.level(), shore) != null);
            if (landing == null) return; // No loaded safe campsite: hold aboard and retry, never create another boat.
            dock = landing.shore(); waterPath = landing.route();
            if (waterPath.isEmpty()) return; // Already at the landing; dismount on the next tick.
        }
        Vec3 delta = Vec3.atCenterOf(waterPath.getFirst()).subtract(boat.position()).multiply(1, 0, 1);
        if (delta.lengthSqr() < .04) { waterPath = waterPath.subList(1, waterPath.size()); return; }
        Vec3 step = delta.normalize().scale(.075);
        if (!BoatWaterNavigator.navigable(boat, waterPath.getFirst()) || !boat.level().noCollision(boat, boat.getBoundingBox().expandTowards(step))) {
            waterPath = List.of(); dock = null; return;
        }
        boat.setYRot((float)(net.minecraft.util.Mth.atan2(-delta.x, delta.z) * 180 / Math.PI));
        boat.setDeltaMovement(step.x / .9, boat.getDeltaMovement().y, step.z / .9); boat.setPaddleState(true, true);
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putBoolean("Night", active); tag.putBoolean("Returning", returning);
        tag.putBoolean("Fallback", fallback);
        if (fire != null) tag.putLong("Fire", fire.asLong()); if (dock != null) tag.putLong("Dock", dock.asLong());
        if (campSpot != null) tag.putLong("CampSpot", campSpot.asLong());
        return tag;
    }
    public void load(CompoundTag tag) {
        active = tag.getBoolean("Night"); returning = tag.getBoolean("Returning"); fallback = tag.getBoolean("Fallback");
        fire = tag.contains("Fire", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("Fire")) : null;
        dock = tag.contains("Dock", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("Dock")) : null;
        campSpot = tag.contains("CampSpot", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("CampSpot")) : null;
        waterPath = List.of(); stalled = 0;
    }
}
