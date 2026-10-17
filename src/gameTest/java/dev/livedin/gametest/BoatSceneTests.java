package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.BoatActivity;
import dev.livedin.entity.activity.BoatWaterNavigator;
import dev.livedin.entity.activity.FishingCast;
import dev.livedin.scene.FishingCycle;
import dev.livedin.entity.NpcRole;
import dev.livedin.scene.BoatScene;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BoatSceneTests {
    private static BlockPos pond(GameTestHelper h) {
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        for (int x = 3; x <= 20; x++) for (int z = 3; z <= 20; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.WATER);
            h.setBlock(new BlockPos(x, -1, z), Blocks.STONE);
        }
        return h.absolutePos(new BlockPos(10, 0, 7));
    }
    private static BlockPos end(BlockPos start) { return start.offset(0, 0, 8); }
    private static Boat boat(GameTestHelper h) { return h.getLevel().getEntitiesOfClass(Boat.class, h.getBounds()).getFirst(); }
    private static AmbientNpc rider(GameTestHelper h) { return h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).getFirst(); }
    private static BlockPos place(GameTestHelper h) {
        BlockPos start = pond(h);
        h.assertTrue(BoatScene.place(h.getLevel(), start, end(start)) == BoatScene.Result.SUCCESS, "Scene places");
        return start;
    }

    private static void assertSeatHeight(GameTestHelper h, Boat boat, AmbientNpc npc) {
        // A normal player-shaped adult sits with its feet below the seat and its hips inside the hull.
        double feetAboveHull = npc.getY() - boat.getY();
        h.assertTrue(feetAboveHull > -0.45 && feetAboveHull < -0.38,
                "Player-shaped rider must sit inside the boat, not float above it; feet offset=" + feetAboveHull);
    }

    @GameTest(template = "arena")
    public static void mountsRealBoatAndRejectsDuplicate(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        h.assertTrue(boat.getType() == EntityType.BOAT && npc.getRole() == NpcRole.BOAT_RIDER, "Vanilla boat and shared NPC");
        h.assertTrue(npc.getVehicle() == boat && boat.getFirstPassenger() == npc && boat.getUUID().equals(((BoatActivity) npc.getActivity()).boatId()), "Real linked passenger");
        h.assertTrue(BoatScene.place(h.getLevel(), start, end(start)) == BoatScene.Result.OCCUPIED, "Repeated placement rejects");
        h.runAfterDelay(10, () -> {
            h.assertTrue(npc.isPassenger() && npc.distanceToSqr(boat) < 2, "Passenger stays seated");
            assertSeatHeight(h, boat, npc);
            h.assertTrue(!boat.getPaddleState(0) && FishingCast.water(npc) != null, "Starts by fishing with stationary oars");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void rejectsBadRoutesWithoutTerrainChanges(GameTestHelper h) {
        BlockPos start = pond(h);
        h.getLevel().setBlockAndUpdate(start.offset(0, 1, 4), Blocks.DIAMOND_BLOCK.defaultBlockState());
        h.assertTrue(BoatScene.place(h.getLevel(), start, end(start)) == BoatScene.Result.UNSAFE, "Obstacle rejects");
        h.assertTrue(h.getLevel().getBlockState(start.offset(0, 1, 4)).is(Blocks.DIAMOND_BLOCK), "Build preserved");
        h.getLevel().setBlockAndUpdate(start.offset(0, 1, 4), Blocks.AIR.defaultBlockState());
        h.assertTrue(!BoatScene.validRoute(h.getLevel(), start, start.offset(1, 0, 8)), "Diagonal rejects");
        h.assertTrue(!BoatScene.validRoute(h.getLevel(), start, start.offset(0, 0, 65536)), "Huge route rejects without overflow");
        h.assertTrue(!BoatScene.validRoute(h.getLevel(), start.offset(10000, 0, 10000), end(start).offset(10000, 0, 10000)), "Unloaded route rejects");
        h.assertTrue(h.getLevel().getEntitiesOfClass(Boat.class, h.getBounds()).isEmpty(), "Invalid scene creates no boat");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 1400)
    public static void randomTripsStayOnWaterAndRepeatFishing(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        final boolean[] traveled = {false}, returnedToFishing = {false};
        final BlockPos[] firstTarget = {null};
        for (int tick = 10; tick < 1300; tick += 5) h.runAtTickTime(tick, () -> {
            h.assertTrue(BoatScene.safeWater(h.getLevel(), BlockPos.containing(boat.getX(), start.getY(), boat.getZ())), "Stays on safe water");
            h.assertTrue(npc.getVehicle() == boat && !boat.horizontalCollision && !npc.isInWater(), "Real seated passenger; no bank pushing");
            assertSeatHeight(h, boat, npc);
            BoatActivity activity = (BoatActivity) npc.getActivity();
            if (activity.travelTarget() != null && firstTarget[0] == null) {
                firstTarget[0] = activity.travelTarget();
                h.assertTrue(Vec3.atCenterOf(firstTarget[0]).subtract(Vec3.atCenterOf(start)).horizontalDistanceSqr() >= 100,
                        "Open pond destination is at least ten blocks away");
                h.assertTrue(!firstTarget[0].equals(end(start)), "Not restricted to former endpoint");
            }
            if (boat.position().subtract(Vec3.atCenterOf(start)).horizontalDistanceSqr() >= 81) traveled[0] = true;
            if (traveled[0] && FishingCast.water(npc) != null) returnedToFishing[0] = true;
        });
        h.runAtTickTime(1300, () -> {
            h.assertTrue(traveled[0] && returnedToFishing[0], "Travels then fishes again");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 700)
    public static void blockedDepartureStopsAndRetriesAfterClearing(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h);
        // A newly built obstruction prevents departure without removing the fishing water.
        h.runAtTickTime(480, () -> h.getLevel().setBlockAndUpdate(start.above(2), Blocks.STONE.defaultBlockState()));
        h.runAtTickTime(550, () -> {
            h.assertTrue(!boat.getPaddleState(0) && boat.getDeltaMovement().horizontalDistanceSqr() < 0.001, "Blocked hull stays still");
            h.getLevel().setBlockAndUpdate(start.above(2), Blocks.AIR.defaultBlockState());
        });
        h.runAtTickTime(650, () -> {
            h.assertTrue(boat.position().subtract(Vec3.atCenterOf(start)).horizontalDistanceSqr() > 4, "Cleared water allows a new trip");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 650)
    public static void stopsWhenDisplacedOntoUnsafeWater(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        h.runAtTickTime(540, () -> boat.setPos(start.getX() - 7, boat.getY(), start.getZ() + 0.5));
        h.runAtTickTime(560, () -> {
            h.assertTrue(!boat.getPaddleState(0) && boat.getDeltaMovement().horizontalDistanceSqr() < 0.001, "No steering into bank after displacement");
            h.assertTrue(npc.getVehicle() == boat, "Displacement does not replace the mount");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 650)
    public static void savesAndReloadsPassengerTreeAndDestination(GameTestHelper h) {
        BlockPos start = place(h);
        h.runAtTickTime(560, () -> {
            Boat old = boat(h); AmbientNpc npc = rider(h);
            var boatId = old.getUUID(); var npcId = npc.getUUID();
            BlockPos target = ((BoatActivity) npc.getActivity()).travelTarget();
            h.assertTrue(target != null, "Trip has a destination");
            CompoundTag data = new CompoundTag(); old.save(data); npc.discard(); old.discard();
            var restored = EntityType.loadEntityRecursive(data, h.getLevel(), e -> e);
            h.getLevel().addFreshEntityWithPassengers(restored);
            AmbientNpc loaded = (AmbientNpc) restored.getFirstPassenger();
            BoatActivity activity = (BoatActivity) loaded.getActivity();
            h.assertTrue(restored.getUUID().equals(boatId) && loaded.getUUID().equals(npcId) && loaded.getVehicle() == restored, "Same passenger identities");
            h.assertTrue(target.equals(activity.travelTarget()) && start.equals(activity.start()) && end(start).equals(activity.end()), "Destination and legacy placement data saved");
        });
        h.runAtTickTime(600, () -> {
            assertSeatHeight(h, boat(h), rider(h));
            h.assertTrue(boat(h).getPaddleState(0), "Replans and resumes saved trip");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Boat.class, h.getBounds()).size() == 1
                    && h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).size() == 1, "No duplicates");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 650)
    public static void fishesForwardFor25SecondsRetrievesThenRows(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        h.runAtTickTime(80, () -> {
            BlockPos water = FishingCast.water(npc);
            h.assertTrue(water != null && FishingCycle.hasBobber(FishingCast.cycleTick(npc)), "Visible cast");
            Vec3 offset = Vec3.atCenterOf(water).subtract(boat.position());
            Vec3 forward = Vec3.directionFromRotation(0, boat.getYRot());
            h.assertTrue(offset.dot(forward) > 1.5 && Math.abs(offset.x * forward.z - offset.z * forward.x) < 0.1, "Bobber is directly in front of hull");
            h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(npc.getYHeadRot() - boat.getYRot())) < 1, "Rider faces cast");
        });
        for (int tick : new int[] {100, 250, 495}) h.runAtTickTime(tick, () -> {
            h.assertTrue(!boat.getPaddleState(0) && boat.position().subtract(Vec3.atCenterOf(start)).horizontalDistanceSqr() < 0.001, "Holds still for full 25 seconds");
            h.assertTrue(FishingCast.cycleTick(npc) == FishingCycle.WAIT_START, "One prolonged wait, no repeated casting");
            assertSeatHeight(h, boat, npc);
        });
        h.runAtTickTime(510, () -> {
            h.assertTrue(FishingCast.cycleTick(npc) >= FishingCycle.RETRIEVE_START && !boat.getPaddleState(0), "Visibly retracts before moving");
        });
        h.runAtTickTime(570, () -> {
            h.assertTrue(boat.getPaddleState(0) && boat.position().subtract(Vec3.atCenterOf(start)).horizontalDistanceSqr() > 1, "Rows after retraction");
            h.assertTrue(FishingCast.water(npc) == null && FishingCast.cycleTick(npc) == -1, "No line during travel");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 240)
    public static void removingRodCancelsBoatCastWithoutStoppingRoute(GameTestHelper h) {
        BlockPos start = place(h); AmbientNpc npc = rider(h); Boat boat = boat(h);
        h.runAtTickTime(150, () -> npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                net.minecraft.world.item.ItemStack.EMPTY));
        h.runAtTickTime(160, () -> {
            h.assertTrue(dev.livedin.entity.activity.FishingCast.cycleTick(npc) == -1
                    && dev.livedin.entity.activity.FishingCast.water(npc) == null, "Removed rod clears cast");
        });
        h.runAtTickTime(215, () -> {
            h.assertTrue(boat.position().subtract(Vec3.atCenterOf(start)).horizontalDistanceSqr() > 4 && npc.getVehicle() == boat, "Mounted random trip continues");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 220)
    public static void removedWaterCancelsBoatCast(GameTestHelper h) {
        place(h); AmbientNpc npc = rider(h);
        h.runAtTickTime(150, () -> {
            BlockPos target = dev.livedin.entity.activity.FishingCast.water(npc);
            h.assertTrue(target != null, "Casting target exists");
            h.getLevel().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        });
        h.runAtTickTime(160, () -> {
            h.assertTrue(dev.livedin.entity.activity.FishingCast.cycleTick(npc) == -1
                    && dev.livedin.entity.activity.FishingCast.water(npc) == null, "Invalid water clears visuals");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 220)
    public static void savingDuringCastRestoresPassengerWithoutStaleBobber(GameTestHelper h) {
        place(h);
        h.runAtTickTime(150, () -> {
            Boat old = boat(h); AmbientNpc npc = rider(h);
            var boatId = old.getUUID(); var npcId = npc.getUUID();
            CompoundTag tag = new CompoundTag(); old.save(tag); npc.discard(); old.discard();
            var restored = EntityType.loadEntityRecursive(tag, h.getLevel(), e -> e);
            h.getLevel().addFreshEntityWithPassengers(restored);
            AmbientNpc loaded = (AmbientNpc) restored.getFirstPassenger();
            h.assertTrue(loaded.getUUID().equals(npcId) && restored.getUUID().equals(boatId), "Same passenger tree");
            h.assertTrue(dev.livedin.entity.activity.FishingCast.cycleTick(loaded) == -1, "Half cast resets on load");
            h.assertTrue(loaded.getMainHandItem().is(net.minecraft.world.item.Items.FISHING_ROD), "Rod survives reload");
        });
        h.runAtTickTime(200, () -> {
            assertSeatHeight(h, boat(h), rider(h));
            h.assertTrue(!boat(h).getPaddleState(0) && FishingCast.water(rider(h)) != null, "Reload starts a fresh fishing stop");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void legacyBoatGetsRodOnceButRemovedEquipmentStaysRemoved(GameTestHelper h) {
        place(h); AmbientNpc npc = rider(h);
        npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
        CompoundTag tree = new CompoundTag(); boat(h).save(tree);
        CompoundTag current = tree.getList("Passengers", 10).getCompound(0);
        var restored = (AmbientNpc) EntityType.loadEntityRecursive(current, h.getLevel(), e -> e);
        h.assertTrue(restored.getMainHandItem().isEmpty(), "Current save preserves intentionally removed rod");
        current.remove("BoatFishingVersion");
        var legacy = (AmbientNpc) EntityType.loadEntityRecursive(current, h.getLevel(), e -> e);
        h.assertTrue(legacy.getMainHandItem().is(net.minecraft.world.item.Items.FISHING_ROD), "Old empty-handed boat rider gets its first rod");
        h.assertTrue(legacy.getUUID().equals(npc.getUUID()), "Upgrade preserves identity");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void navigatorChoosesVariedDistantPointsAndAvoidsIsland(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h);
        for (int z = 9; z <= 14; z++) h.setBlock(new BlockPos(12, 0, z), Blocks.STONE);
        var destinations = new java.util.HashSet<BlockPos>();
        for (int seed = 0; seed < 12; seed++) {
            boat.getRandom().setSeed(seed);
            var path = BoatWaterNavigator.findPath(boat, start.getY(), null);
            h.assertTrue(!path.isEmpty(), "Finds route around island");
            destinations.add(path.getLast());
            h.assertTrue(Vec3.atCenterOf(path.getLast()).subtract(boat.position()).horizontalDistanceSqr() >= 100, "Chooses ten-block destination when available");
            BlockPos previous = BlockPos.containing(boat.getX(), start.getY(), boat.getZ());
            for (BlockPos point : path) {
                h.assertTrue(BoatWaterNavigator.navigable(boat, point) && previous.distManhattan(point) == 1, "Connected cardinal path with hull clearance; no corner cutting");
                previous = point;
            }
        }
        h.assertTrue(destinations.size() >= 4, "Different random choices, rather than two endpoints");
        h.assertTrue(BoatWaterNavigator.findPath(boat, start.getY(), start.offset(10000, 0, 10000)).isEmpty(), "Unloaded destination is never forced");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void navigatorRejectsDisconnectedWaterAndUsesShorterSmallPondTrip(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h);
        for (int x = 3; x <= 20; x++) for (int z = 11; z <= 12; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        h.assertTrue(BoatWaterNavigator.findPath(boat, start.getY(), h.absolutePos(new BlockPos(10, 0, 17))).isEmpty(), "Does not cross land into a separate pool");
        for (int x = 3; x <= 20; x++) for (int z = 3; z <= 10; z++)
            if (x < 7 || x > 13 || z < 4 || z > 10) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        var path = BoatWaterNavigator.findPath(boat, start.getY(), null);
        h.assertTrue(!path.isEmpty() && Vec3.atCenterOf(path.getLast()).subtract(boat.position()).horizontalDistanceSqr() < 100, "Small pond permits a shorter reachable trip");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100)
    public static void removedBoatRecoversToShoreWithoutReplacement(GameTestHelper h) {
        place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        h.runAtTickTime(10, boat::discard);
        h.runAtTickTime(20, () -> {
            h.assertTrue(!npc.isPassenger() && npc.getRole() == NpcRole.TRAVELER && !(npc.getActivity() instanceof BoatActivity), "Loses route cleanly");
            h.assertTrue(!npc.isInWater() && dev.livedin.scene.FishingScene.safeShore(h.getLevel(), npc.blockPosition()), "Nearby dry shore recovery");
        });
        h.runAtTickTime(70, () -> {
            h.assertTrue(npc.isAlive() && h.getLevel().getEntitiesOfClass(Boat.class, h.getBounds()).isEmpty(), "No replacement boats");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void dismountStopsEmptyBoatAndLeavesNoController(GameTestHelper h) {
        place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        h.runAtTickTime(15, npc::stopRiding);
        h.runAtTickTime(25, () -> {
            h.assertTrue(npc.getRole() == NpcRole.TRAVELER && !boat.isVehicle(), "Dismount clears activity");
            h.assertTrue(!boat.getPaddleState(0) && boat.getDeltaMovement().horizontalDistanceSqr() < 0.001, "Empty boat no longer driven");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Boat.class, h.getBounds()).size() == 1, "Original boat retained, no replacement");
            h.succeed();
        });
    }
}
