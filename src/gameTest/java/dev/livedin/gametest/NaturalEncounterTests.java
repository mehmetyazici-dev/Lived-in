package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.scene.CampfireScene;
import dev.livedin.world.EncounterPlacement;
import dev.livedin.world.NaturalEncounters;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NaturalEncounterTests {
    @GameTest(template = "arena")
    public static void candidatesAreStableSparseAndHandleNegativeCoordinates(GameTestHelper h) {
        for (long seed : new long[] {42, 20261013, 8675309}) {
            var positions = new HashSet<ChunkPos>();
            for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
                ChunkPos p = NaturalEncounters.candidate(seed, x, z);
                h.assertTrue(positions.add(p) && NaturalEncounters.isCandidate(seed, p), "One stable candidate per region");
                h.assertTrue(Math.floorDiv(p.x, 8) == x && Math.floorDiv(p.z, 8) == z, "Correct negative region");
                ChunkPos next = NaturalEncounters.candidate(seed, x + 1, z);
                h.assertTrue(next.x - p.x >= 5, "Neighbor candidates at least five chunks apart on separated axis");
                int count = 0;
                for (int cx = x * 8; cx < x * 8 + 8; cx++) for (int cz = z * 8; cz < z * 8 + 8; cz++)
                    if (NaturalEncounters.isBoatCandidate(seed, new ChunkPos(cx, cz))) count++;
                h.assertTrue(count == 1, "Exactly one candidate among 64 chunks");
                int landCount = 0;
                for (int cx = x * 8; cx < x * 8 + 8; cx++) for (int cz = z * 8; cz < z * 8 + 8; cz++)
                    if (NaturalEncounters.isLandCandidate(seed, new ChunkPos(cx, cz))) landCount++;
                h.assertTrue(landCount == 4, "Four additional land opportunities without increasing boat opportunities");
                var land = NaturalEncounters.landCandidate(seed, x, z);
                h.assertTrue(NaturalEncounters.isCandidate(seed, land)
                        && Math.floorDiv(land.x, 4) == x && Math.floorDiv(land.z, 4) == z, "Stable negative land coordinates");
                h.assertTrue(NaturalEncounters.landCandidate(seed, x + 1, z).x - land.x >= 3, "Land candidate spacing");
            }
        }
        h.succeed();
    }

    private static BlockPos ground(GameTestHelper h, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++)
            h.setBlock(new BlockPos(x, 0, z), block);
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) for (int y = 6; y <= 9; y++)
            h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        return h.absolutePos(new BlockPos(12, 1, 12));
    }

    @GameTest(template = "arena")
    public static void naturalCampAcceptsGrassButRejectsBuildingsAndCanopies(GameTestHelper h) {
        BlockPos center = ground(h, Blocks.GRASS_BLOCK);
        h.getLevel().setBlockAndUpdate(center, Blocks.SHORT_GRASS.defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.above(3), Blocks.OAK_LEAVES.defaultBlockState());
        h.assertTrue(CampfireScene.placeNatural(h.getLevel(), center) == CampfireScene.Result.OCCUPIED, "Reject canopy above fire");
        h.getLevel().setBlockAndUpdate(center.above(3), Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.offset(2, -1, 2), Blocks.OAK_PLANKS.defaultBlockState());
        h.assertTrue(CampfireScene.placeNatural(h.getLevel(), center) == CampfireScene.Result.UNSAFE_GROUND, "Reject built floor");
        h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.SHORT_GRASS), "Rejected scene preserves original plants");
        h.getLevel().setBlockAndUpdate(center.offset(2, -1, 2), Blocks.GRASS_BLOCK.defaultBlockState());
        h.assertTrue(CampfireScene.placeNatural(h.getLevel(), center) == CampfireScene.Result.SUCCESS, "Accept open natural ground");
        h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).size() == 3, "Complete group");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void rejectsSlopeWithoutChangingBlocks(GameTestHelper h) {
        BlockPos center = ground(h, Blocks.GRASS_BLOCK);
        h.getLevel().setBlockAndUpdate(center.offset(3, -1, 3), Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(center.offset(3, -2, 3), Blocks.AIR.defaultBlockState());
        h.assertTrue(CampfireScene.placeNatural(h.getLevel(), center) != CampfireScene.Result.SUCCESS, "Reject hole/slope");
        h.assertTrue(h.getLevel().getBlockState(center).isAir(), "No partial fire");
        h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).isEmpty(), "No partial group");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void naturalCampFitsOneBlockRiseWithoutFlattening(GameTestHelper h) {
        BlockPos center = ground(h, Blocks.GRASS_BLOCK);
        for (int x = 14; x < 24; x++) for (int z = 0; z < 24; z++)
            h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
        h.assertTrue(CampfireScene.placeNatural(h.getLevel(), center) == CampfireScene.Result.SUCCESS, "Gentle slope accepted");
        h.assertTrue(h.getLevel().getBlockState(center.offset(3, 0, 0)).is(Blocks.GRASS_BLOCK), "Raised ground preserved");
        h.assertTrue(h.getLevel().getBlockState(center.offset(2, 1, 1)).is(Blocks.OAK_LOG), "Bench rests on raised ground");
        var raised = h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).stream()
                .filter(n -> n.getActivitySpot().getX() == center.getX() + 2).findFirst().orElseThrow();
        h.assertTrue(raised.getActivitySpot().getY() == center.getY() + 1, "Traveler stands at local ground height");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void ownerChunkContainsWholeFootprint(GameTestHelper h) {
        ChunkPos chunk = new ChunkPos(-3, 2);
        h.assertTrue(EncounterPlacement.inside(chunk, chunk.getBlockAt(4, 70, 4), 4), "Camp inside negative chunk");
        h.assertTrue(!EncounterPlacement.inside(chunk, chunk.getBlockAt(3, 70, 4), 4), "Camp crossing edge rejected");
        h.assertTrue(!EncounterPlacement.inside(chunk, chunk.getBlockAt(15, 62, 8), 1), "Water route edge rejected");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void processedMarkerSurvivesAttachmentSerialization(GameTestHelper h) {
        LevelChunk chunk = h.getLevel().getChunkAt(h.absolutePos(BlockPos.ZERO));
        var prior = chunk.getExistingDataOrNull(NaturalEncounters.STATE.get());
        chunk.setData(NaturalEncounters.STATE.get(), NaturalEncounters.PROCESSED);
        var saved = chunk.writeAttachmentsToNBT(h.getLevel().registryAccess());
        LevelChunk restored = new LevelChunk(h.getLevel(), chunk.getPos());
        restored.readAttachmentsFromNBT(h.getLevel().registryAccess(), saved);
        h.assertTrue(restored.getData(NaturalEncounters.STATE.get()) == NaturalEncounters.PROCESSED, "Marker round trip");
        h.assertTrue(!NaturalEncounters.process(h.getLevel(), restored), "Reload does not initialize again");
        if (prior == null) chunk.removeData(NaturalEncounters.STATE.get()); else chunk.setData(NaturalEncounters.STATE.get(), prior);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void oldUnmarkedChunksAndFlatWorldStayUnchanged(GameTestHelper h) {
        LevelChunk old = new LevelChunk(h.getLevel(), new ChunkPos(h.absolutePos(BlockPos.ZERO)));
        h.assertTrue(!NaturalEncounters.process(h.getLevel(), old), "Old chunk without pending marker ignored");
        h.assertTrue(!old.hasData(NaturalEncounters.STATE.get()), "No marker allocated by checking old chunks");
        h.assertTrue(!NaturalEncounters.supported(h.getLevel()), "Flat GameTest world excluded from event generation");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void consumedCandidateDoesNotReplaceRemovedMembers(GameTestHelper h) {
        BlockPos center = ground(h, Blocks.GRASS_BLOCK);
        // GameTest surrounds its arena with a barrier roof; a surface search must see open sky.
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) for (int y = 6; y <= 8; y++)
            h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        LevelChunk chunk = h.getLevel().getChunkAt(center);
        var prior = chunk.getExistingDataOrNull(NaturalEncounters.STATE.get());
        chunk.setData(NaturalEncounters.STATE.get(), NaturalEncounters.PENDING);
        h.assertTrue(NaturalEncounters.process(h.getLevel(), chunk, true), "Real candidate search finds natural camp; center="
                + center + " height=" + chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                center.getX() & 15, center.getZ() & 15) + " refs=" + chunk.getAllReferences());
        var members = h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds());
        h.assertTrue(members.size() == 3, "One group from candidate search");
        members.forEach(AmbientNpc::discard);
        h.assertTrue(!NaturalEncounters.process(h.getLevel(), chunk), "Removed group never recreated");
        h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).isEmpty(), "No replacements");
        if (prior == null) chunk.removeData(NaturalEncounters.STATE.get()); else chunk.setData(NaturalEncounters.STATE.get(), prior);
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void failedSpawnsRestoreNaturalPlantsAndBlocks(GameTestHelper h) {
        BlockPos center = ground(h, Blocks.GRASS_BLOCK);
        h.getLevel().setBlockAndUpdate(center, Blocks.SHORT_GRASS.defaultBlockState());
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> cancel = event -> {
            if (event.getEntity() instanceof AmbientNpc && h.getBounds().contains(event.getEntity().position()))
                event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try {
            h.assertTrue(CampfireScene.placeNatural(h.getLevel(), center) == CampfireScene.Result.SPAWN_FAILED, "Injected entity insertion failure");
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);
        }
        h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.SHORT_GRASS), "Original grass restored");
        h.assertTrue(h.getLevel().getBlockState(center.offset(0, 0, 3)).isAir(), "Bench rolled back");
        h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).isEmpty(), "No orphan members");
        h.succeed();
    }
}
