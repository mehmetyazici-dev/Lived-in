package dev.livedin.world;

import dev.livedin.scene.BoatScene;
import dev.livedin.scene.CampfireScene;
import dev.livedin.scene.FishingScene;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded surface search. Every read and modified position stays inside the ready owner chunk. */
public final class EncounterPlacement {
    public record Placed(String kind, BlockPos position) {}
    private EncounterPlacement() {}

    public static Placed tryPlace(ServerLevel level, LevelChunk chunk) {
        return tryPlace(level, chunk, true);
    }

    public static Placed tryPlace(ServerLevel level, LevelChunk chunk, boolean allowBoat) {
        return tryPlace(level, chunk, allowBoat, true);
    }

    public static Placed tryPlace(ServerLevel level, LevelChunk chunk, boolean allowBoat, boolean allowCamp) {
        ChunkPos cp = chunk.getPos();
        // Reserve usable clearings for camps before considering their nearby banks.
        if (allowCamp) for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++) {
            BlockPos center = surface(chunk, x, z);
            if (CampfireScene.placeNatural(level, center) == CampfireScene.Result.SUCCESS)
                return new Placed("camp", center);
        }
        for (int x = 1; x <= 14; x++) for (int z = 1; z <= 14; z++) {
            BlockPos shore = surface(chunk, x, z);
            if (!naturalBank(level, shore)) continue;
            for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
                int distance = dx * dx + dz * dz;
                if (distance < 9 || distance > 25) continue;
                int wx = x + dx, wz = z + dz;
                if (wx < 1 || wx > 14 || wz < 1 || wz > 14) continue;
                BlockPos water = surface(chunk, wx, wz).below();
                if (!level.getBlockState(water).is(Blocks.WATER)) continue;
                if (!inside(cp, water, 1)) continue;
                if (FishingScene.place(level, shore, water) == FishingScene.Result.SUCCESS)
                    return new Placed("fishing", shore);
            }
        }
        if (!allowBoat) return null;
        for (int x = 1; x <= 14; x++) for (int z = 1; z <= 14; z++) {
            BlockPos start = surface(chunk, x, z).below();
            if (!level.getBlockState(start).is(Blocks.WATER)) continue;
            for (Direction direction : new Direction[] {Direction.EAST, Direction.SOUTH}) {
                BlockPos end = start.relative(direction, 8);
                if (inside(cp, end, 1) && BoatScene.place(level, start, end) == BoatScene.Result.SUCCESS)
                    return new Placed("boat", start);
            }
        }
        return null;
    }

    private static BlockPos surface(LevelChunk chunk, int x, int z) {
        return new BlockPos(chunk.getPos().getMinBlockX() + x,
                chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1,
                chunk.getPos().getMinBlockZ() + z);
    }

    public static boolean inside(ChunkPos chunk, BlockPos pos, int margin) {
        return pos.getX() >= chunk.getMinBlockX() + margin && pos.getX() <= chunk.getMaxBlockX() - margin
                && pos.getZ() >= chunk.getMinBlockZ() + margin && pos.getZ() <= chunk.getMaxBlockZ() - margin;
    }

    public static boolean naturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.STONE);
    }

    private static boolean naturalBank(ServerLevel level, BlockPos shore) {
        return naturalGround(level.getBlockState(shore.below())) && FishingScene.safeShore(level, shore);
    }
}
