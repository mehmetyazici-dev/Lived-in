package dev.livedin.entity.activity;

import dev.livedin.scene.BoatScene;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

/** Finds short paths through already loaded, connected surface water. Never loads chunks. */
public final class BoatWaterNavigator {
    public static final int SEARCH_RADIUS = 24;
    public static final int MAX_VISITED = 2401;
    public static final int PREFERRED_DISTANCE = 10;
    private BoatWaterNavigator() {}

    public record Landing(BlockPos shore, List<BlockPos> route) {}

    /** One breadth-first search, including bends in the coast, for the nearest usable campsite. */
    @Nullable public static Landing findLanding(Boat boat, int waterY, java.util.function.Predicate<BlockPos> usableShore) {
        BlockPos origin = BlockPos.containing(boat.getX(), waterY, boat.getZ());
        if (!navigable(boat, origin)) return null;
        var parents = new HashMap<BlockPos, BlockPos>();
        var checked = new java.util.HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        parents.put(origin, origin); checked.add(origin); queue.add(origin);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos shore = current.relative(side, 2).above();
                if (usableShore.test(shore)) return new Landing(shore, path(parents, origin, current));
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(side);
                if (Math.abs(next.getX() - origin.getX()) > 64 || Math.abs(next.getZ() - origin.getZ()) > 64
                        || checked.size() >= 8192 || !checked.add(next)) continue;
                if (navigable(boat, next)) { parents.put(next, current); queue.addLast(next); }
            }
        }
        return null;
    }

    /** A null preferred target requests a random destination, favoring trips of at least ten blocks. */
    public static List<BlockPos> findPath(Boat boat, int waterY, @Nullable BlockPos preferred) {
        BlockPos origin = BlockPos.containing(boat.getX(), waterY, boat.getZ());
        if (!navigable(boat, origin)) return List.of();
        var parents = new HashMap<BlockPos, BlockPos>();
        var checked = new java.util.HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        var distant = new ArrayList<BlockPos>();
        var shorter = new ArrayList<BlockPos>();
        checked.add(origin); parents.put(origin, origin); queue.add(origin);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!current.equals(origin)) {
                double distance = Vec3.atCenterOf(current).subtract(boat.position()).horizontalDistanceSqr();
                if (distance >= PREFERRED_DISTANCE * PREFERRED_DISTANCE) distant.add(current);
                else if (distance >= 4) shorter.add(current);
            }
            if (current.equals(preferred)) return path(parents, origin, current);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);
                if (Math.abs(next.getX() - origin.getX()) > SEARCH_RADIUS
                        || Math.abs(next.getZ() - origin.getZ()) > SEARCH_RADIUS
                        || checked.contains(next) || checked.size() >= MAX_VISITED) continue;
                checked.add(next);
                if (navigable(boat, next)) { parents.put(next, current); queue.addLast(next); }
            }
        }
        if (preferred != null) return List.of();
        List<BlockPos> candidates = distant.isEmpty() ? shorter : distant;
        if (candidates.isEmpty()) return List.of();
        return path(parents, origin, candidates.get(boat.getRandom().nextInt(candidates.size())));
    }

    public static boolean navigable(Boat boat, BlockPos water) {
        if (!BoatScene.safeWater(boat.level(), water)) return false;
        Vec3 offset = new Vec3(water.getX() + 0.5 - boat.getX(), 0, water.getZ() + 0.5 - boat.getZ());
        return boat.level().noCollision(boat, boat.getBoundingBox().move(offset).inflate(0.1, 0, 0.1)
                .expandTowards(0, 1.8, 0));
    }

    private static List<BlockPos> path(HashMap<BlockPos, BlockPos> parents, BlockPos origin, BlockPos target) {
        var result = new ArrayList<BlockPos>();
        for (BlockPos at = target; !at.equals(origin); at = parents.get(at)) result.add(at);
        Collections.reverse(result);
        return result;
    }
}
