package dev.livedin.scene;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** A manually placed, straight route on exposed still water; never changes terrain. */
public final class BoatScene {
    private BoatScene() {}
    public enum Result {
        SUCCESS("Placed one boat rider on a short return route."),
        UNSAFE("Needs two source-water positions at the same height, 4 to 12 blocks apart along X or Z, with a clear 3-wide water corridor and two air blocks above it."),
        OCCUPIED("This route already contains a traveler or boat."),
        SPAWN_FAILED("Boat scene placement was cancelled; its new entities were removed.");
        private final String message;
        Result(String message) { this.message = message; }
        public String message() { return message; }
    }

    public static boolean validEndpoints(BlockPos start, BlockPos end) {
        long dx = (long)end.getX() - start.getX(), dz = (long)end.getZ() - start.getZ();
        long distance = Math.abs(dx) + Math.abs(dz);
        return start.getY() == end.getY() && (dx == 0 || dz == 0) && distance >= 4 && distance <= 12;
    }

    public static boolean safeWater(Level level, BlockPos center) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = center.offset(x, 0, z);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(Blocks.WATER)
                    || !level.getFluidState(pos).isSource() || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos.above(2)).isAir()) return false;
        }
        return true;
    }

    public static boolean validRoute(Level level, BlockPos start, BlockPos end) {
        if (!validEndpoints(start, end)) return false;
        int dx = Integer.signum(end.getX() - start.getX()), dz = Integer.signum(end.getZ() - start.getZ());
        int distance = Math.abs(end.getX() - start.getX()) + Math.abs(end.getZ() - start.getZ());
        for (int step = 0; step <= distance; step++) if (!safeWater(level, start.offset(dx * step, 0, dz * step))) return false;
        return true;
    }

    public static Result place(ServerLevel level, BlockPos start, BlockPos end) {
        if (!validRoute(level, start, end)) return Result.UNSAFE;
        AABB area = new AABB(net.minecraft.world.phys.Vec3.atCenterOf(start), net.minecraft.world.phys.Vec3.atCenterOf(end)).inflate(2, 3, 2);
        if (!level.getEntitiesOfClass(AmbientNpc.class, area).isEmpty()
                || !level.getEntitiesOfClass(Boat.class, area).isEmpty()) return Result.OCCUPIED;
        Boat boat = EntityType.BOAT.create(level);
        AmbientNpc rider = ModContent.AMBIENT_NPC.get().create(level);
        if (boat == null || rider == null) return Result.SPAWN_FAILED;
        float yaw = end.getX() > start.getX() ? -90 : end.getX() < start.getX() ? 90 : end.getZ() > start.getZ() ? 0 : 180;
        boat.moveTo(start.getX() + 0.5, start.getY() + 0.8, start.getZ() + 0.5, yaw, 0);
        boat.setVariant(Boat.Type.OAK);
        rider.moveTo(boat.getX(), boat.getY() + 0.5, boat.getZ(), yaw, 0);
        rider.assignBoatScene(start, end, boat.getUUID());
        if (!level.addFreshEntity(boat) || !level.addFreshEntity(rider) || !rider.startRiding(boat)) {
            rider.discard(); boat.discard(); return Result.SPAWN_FAILED;
        }
        return Result.SUCCESS;
    }
}
