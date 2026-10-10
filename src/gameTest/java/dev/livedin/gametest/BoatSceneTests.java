package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
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
        for (int x = 7; x <= 13; x++) for (int z = 5; z <= 17; z++) {
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
        h.assertTrue(npc.getVehicle() == boat && boat.getFirstPassenger() == npc && boat.getUUID().equals(npc.getSceneBoatId()), "Real linked passenger");
        h.assertTrue(BoatScene.place(h.getLevel(), start, end(start)) == BoatScene.Result.OCCUPIED, "Repeated placement rejects");
        h.runAfterDelay(10, () -> {
            h.assertTrue(npc.isPassenger() && npc.distanceToSqr(boat) < 2, "Passenger stays seated");
            assertSeatHeight(h, boat, npc);
            h.assertTrue(boat.getPaddleState(0) && boat.getPaddleState(1), "Moving oars synchronize");
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

    @GameTest(template = "arena", timeoutTicks = 650)
    public static void travelsAndReversesWithinBounds(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        for (int tick = 5; tick < 600; tick++) h.runAtTickTime(tick, () -> {
            h.assertTrue(boat.getX() > start.getX() + 0.15 && boat.getX() < start.getX() + 0.85, "No lateral wandering");
            h.assertTrue(boat.getZ() >= start.getZ() + 0.3 && boat.getZ() <= end(start).getZ() + 0.7, "Bounded local route");
            h.assertTrue(npc.getVehicle() == boat && npc.distanceToSqr(boat) < 2, "Passenger moves with boat");
            assertSeatHeight(h, boat, npc);
            h.assertTrue(!boat.horizontalCollision && !npc.isInWater(), "Not pushing a bank or dunking rider");
        });
        h.runAtTickTime(70, () -> h.assertTrue(boat.getZ() > start.getZ() + 3, "Actually travels"));
        h.runAtTickTime(135, () -> h.assertTrue(!npc.isBoatHeadingToEnd(), "Reverses at far endpoint"));
        h.runAtTickTime(260, () -> h.assertTrue(npc.isBoatHeadingToEnd(), "Returns and reverses again"));
        h.runAtTickTime(600, h::succeed);
    }

    @GameTest(template = "arena", timeoutTicks = 250)
    public static void stopsAtObstacleAndResumesAfterClearing(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h);
        h.getLevel().setBlockAndUpdate(start.offset(0, 1, 4), Blocks.STONE.defaultBlockState());
        final Vec3[] stopped = new Vec3[1];
        h.runAtTickTime(65, () -> {
            h.assertTrue(boat.getZ() < start.getZ() + 3 && !boat.getPaddleState(0), "Stops before obstruction");
            stopped[0] = boat.position();
        });
        h.runAtTickTime(110, () -> {
            h.assertTrue(boat.position().subtract(stopped[0]).horizontalDistanceSqr() < 0.001, "No repeated pushing");
            h.getLevel().setBlockAndUpdate(start.offset(0, 1, 4), Blocks.AIR.defaultBlockState());
        });
        h.runAtTickTime(160, () -> { h.assertTrue(boat.getZ() > stopped[0].z + 2 && boat.getPaddleState(0), "Clearing resumes route"); h.succeed(); });
    }

    @GameTest(template = "arena", timeoutTicks = 200)
    public static void stopsBeforeRemovedWaterOrOutsideRoute(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h);
        h.getLevel().setBlockAndUpdate(start.offset(0, 0, 4), Blocks.STONE.defaultBlockState());
        h.runAtTickTime(80, () -> {
            h.assertTrue(boat.getZ() < start.getZ() + 3 && !boat.getPaddleState(0), "Shoreline stops route");
            boat.setPos(start.getX() + 2.5, boat.getY(), start.getZ() + 0.5);
        });
        h.runAtTickTime(90, () -> {
            h.assertTrue(boat.getDeltaMovement().horizontalDistanceSqr() < 0.001 && !boat.getPaddleState(0), "Displacement pauses outside route");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 200)
    public static void savesAndReloadsPassengerTreeAndRoute(GameTestHelper h) {
        BlockPos start = place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        var boatId = boat.getUUID(); var npcId = npc.getUUID();
        npc.reverseBoatRoute();
        CompoundTag data = new CompoundTag();
        h.assertTrue(boat.save(data) && data.getList("Passengers", 10).size() == 1, "Vanilla saves passenger inside boat");
        npc.discard(); boat.discard();
        var restored = EntityType.loadEntityRecursive(data, h.getLevel(), e -> e);
        h.assertTrue(restored instanceof Boat && restored.getPassengers().size() == 1, "Passenger tree reloads");
        h.getLevel().addFreshEntityWithPassengers(restored);
        h.runAfterDelay(5, () -> {
            Boat b = boat(h); AmbientNpc n = rider(h);
            h.assertTrue(b.getUUID().equals(boatId) && n.getUUID().equals(npcId) && n.getVehicle() == b, "Same identities and real mount");
            assertSeatHeight(h, b, n);
            h.assertTrue(boatId.equals(n.getSceneBoatId()) && start.equals(n.getBoatStart()) && end(start).equals(n.getBoatEnd())
                    && !n.isBoatHeadingToEnd(), "Route and direction saved");
        });
        h.runAfterDelay(65, () -> {
            h.assertTrue(rider(h).isBoatHeadingToEnd() && boat(h).getZ() > start.getZ() + 1, "Reloaded route resumes");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Boat.class, h.getBounds()).size() == 1
                    && h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).size() == 1, "No duplicate entities");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 100)
    public static void removedBoatRecoversToShoreWithoutReplacement(GameTestHelper h) {
        place(h); Boat boat = boat(h); AmbientNpc npc = rider(h);
        h.runAtTickTime(10, boat::discard);
        h.runAtTickTime(20, () -> {
            h.assertTrue(!npc.isPassenger() && npc.getRole() == NpcRole.TRAVELER && npc.getSceneBoatId() == null, "Loses route cleanly");
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
