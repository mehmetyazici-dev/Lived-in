package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.FishingActivity;
import dev.livedin.entity.NpcRole;
import dev.livedin.scene.FishingCycle;
import dev.livedin.scene.FishingScene;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FishingSceneTests {
    private static BlockPos bank(GameTestHelper h) {
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        BlockPos shore = h.absolutePos(new BlockPos(10, 1, 10)), water = water(shore);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            h.getLevel().setBlockAndUpdate(water.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(water.offset(x, 0, z), Blocks.WATER.defaultBlockState());
        }
        for (int z = 1; z <= 2; z++) {
            h.getLevel().setBlockAndUpdate(shore.offset(0, -2, z), Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(shore.offset(0, -1, z), Blocks.WATER.defaultBlockState());
        }
        return shore;
    }
    private static BlockPos water(BlockPos shore) { return shore.offset(0, -1, 4); }
    private static AmbientNpc fisher(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).getFirst();
    }
    private static Cat cat(GameTestHelper h) { return h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).getFirst(); }
    private static BlockPos place(GameTestHelper h) {
        BlockPos shore = bank(h);
        h.assertTrue(FishingScene.place(h.getLevel(), shore, water(shore)) == FishingScene.Result.SUCCESS, "Scene must place");
        return shore;
    }

    @GameTest(template = "arena")
    public static void acceptsNarrowVegetatedBankAndRaisedCatFooting(GameTestHelper h) {
        BlockPos shore = bank(h), target = water(shore);
        h.getLevel().setBlockAndUpdate(shore.offset(1, -1, 1), Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(shore.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        h.getLevel().setBlockAndUpdate(shore, Blocks.SHORT_GRASS.defaultBlockState());
        h.getLevel().setBlockAndUpdate(shore.offset(-1, 0, 0), Blocks.STONE.defaultBlockState());
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            if (x != 0 || z != 0) h.getLevel().setBlockAndUpdate(target.offset(x, 0, z), Blocks.STONE.defaultBlockState());
        h.assertTrue(FishingScene.place(h.getLevel(), shore, target) == FishingScene.Result.SUCCESS,
                "Safe actual feet and cast are enough; no rectangular clearings required");
        h.assertTrue(cat(h).blockPosition().equals(shore.offset(-1, 1, 0)), "Cat uses safe local bank height");
        h.assertTrue(h.getLevel().getBlockState(shore).is(Blocks.SHORT_GRASS), "Vegetation is preserved");
        h.runAfterDelay(5, () -> {
            h.assertTrue(((FishingActivity) fisher(h).getActivity()).canFish(fisher(h)), "Narrow bank remains a valid activity");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void rejectsInlandAndHighBanksDespiteValidDistantBobber(GameTestHelper h) {
        BlockPos shore = bank(h), target = water(shore);
        h.assertTrue(FishingScene.validFishingSpot(h.getLevel(), shore, target), "Adjacent bank accepts a cast farther into water");
        h.getLevel().setBlockAndUpdate(shore.offset(0, -1, 1), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(shore.offset(0, -1, 2), Blocks.STONE.defaultBlockState());
        h.assertTrue(FishingScene.clearCast(h.getLevel(), net.minecraft.world.phys.Vec3.atBottomCenterOf(shore).add(0, 1.5, 0), target), "Old inland layout still has an unobstructed distant cast");
        h.assertTrue(FishingScene.place(h.getLevel(), shore, target) == FishingScene.Result.UNSAFE, "Dry gap before water rejects the entire pair");
        h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).isEmpty()
                && h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).isEmpty(), "No partial inland scene");
        h.getLevel().setBlockAndUpdate(shore.offset(0, -1, 1), Blocks.WATER.defaultBlockState());
        h.getLevel().setBlockAndUpdate(shore, Blocks.STONE.defaultBlockState());
        h.assertTrue(!FishingScene.validFishingSpot(h.getLevel(), shore.above(), target), "Two-block-high perch rejects even beside water");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void removingEdgeWaterPausesAndRestorationResumesFishing(GameTestHelper h) {
        BlockPos shore = place(h); AmbientNpc npc = fisher(h);
        BlockPos edge = shore.offset(0, -1, 1);
        h.runAtTickTime(5, () -> h.getLevel().setBlockAndUpdate(edge, Blocks.STONE.defaultBlockState()));
        h.runAtTickTime(10, () -> {
            h.assertTrue(FishingActivity.cycleTick(npc) == -1, "Distant bobber alone cannot keep an inland fisher active");
            h.getLevel().setBlockAndUpdate(edge, Blocks.WATER.defaultBlockState());
        });
        h.runAtTickTime(15, () -> { h.assertTrue(FishingActivity.cycleTick(npc) >= 0, "Water beside feet restores activity"); h.succeed(); });
    }

    @GameTest(template = "arena")
    public static void olderInlandPairMovesToWaterfrontWithoutReplacement(GameTestHelper h) {
        BlockPos oldShore = place(h); AmbientNpc oldNpc = fisher(h); Cat pet = cat(h);
        var npcId = oldNpc.getUUID(); var catId = pet.getUUID();
        CompoundTag data = new CompoundTag(); oldNpc.save(data);
        data.remove("FishingBankVersion");
        oldNpc.discard();
        h.getLevel().setBlockAndUpdate(oldShore.offset(0, -1, 1), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(oldShore.offset(0, -1, 2), Blocks.STONE.defaultBlockState());
        AmbientNpc loaded = (AmbientNpc) EntityType.loadEntityRecursive(data, h.getLevel(), e -> e);
        h.getLevel().addFreshEntity(loaded);
        h.runAtTickTime(25, () -> {
            h.assertTrue(!oldShore.equals(loaded.getActivitySpot()) && loaded.blockPosition().equals(loaded.getActivitySpot()), "Older inland fisher moved to nearby dry edge");
            h.assertTrue(FishingScene.waterfront(h.getLevel(), loaded.blockPosition(), FishingActivity.water(loaded))
                    && ((FishingActivity) loaded.getActivity()).canFish(loaded), "Corrected pair actively fishes beside water");
            h.assertTrue(loaded.getUUID().equals(npcId) && catId.equals(((FishingActivity) loaded.getActivity()).petCatId())
                    && npcId.equals(pet.getOwnerUUID()) && pet.distanceToSqr(loaded) < 4 && !pet.isInWater(), "Original fisher and dry linked cat retained");
            CompoundTag saved = new CompoundTag(); loaded.save(saved);
            h.assertTrue(saved.getInt("FishingBankVersion") == 1, "Correction saved once");
            h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).size() == 1
                    && h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).size() == 1, "No duplicate or replacement pair");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void createsLinkedPairAndRejectsRepeat(GameTestHelper h) {
        BlockPos shore = place(h);
        AmbientNpc npc = fisher(h); Cat cat = cat(h);
        h.assertTrue(npc.getRole() == NpcRole.FISHER && npc.getMainHandItem().is(Items.FISHING_ROD), "Fisher role and rod");
        h.assertTrue(cat.getUUID().equals(((FishingActivity) npc.getActivity()).petCatId()) && npc.getUUID().equals(cat.getOwnerUUID()), "Saved reciprocal identity");
        h.assertTrue(FishingScene.place(h.getLevel(), shore, water(shore)) == FishingScene.Result.OCCUPIED, "No duplicate scene");
        h.runAfterDelay(5, () -> {
            h.assertTrue(cat.isInSittingPose() && ((FishingActivity) npc.getActivity()).canFish(npc), "Cat sits while fisher is active");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).size() == 1, "Exactly one cat");
            h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).size() == 1, "Exactly one fisher");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void rejectsUnsafeOrBlockedTerrainWithoutChanges(GameTestHelper h) {
        BlockPos shore = bank(h), water = water(shore);
        h.getLevel().setBlockAndUpdate(shore, Blocks.DIAMOND_BLOCK.defaultBlockState());
        h.assertTrue(FishingScene.place(h.getLevel(), shore, water) == FishingScene.Result.UNSAFE, "Occupied bank rejects placement");
        h.assertTrue(h.getLevel().getBlockState(shore).is(Blocks.DIAMOND_BLOCK), "No overwritten builds");
        h.getLevel().setBlockAndUpdate(shore, Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(water, Blocks.STONE.defaultBlockState());
        h.assertTrue(FishingScene.place(h.getLevel(), shore, water) == FishingScene.Result.UNSAFE, "Missing water rejects placement");
        h.assertTrue(FishingScene.place(h.getLevel(), shore.offset(10000, 0, 10000), water.offset(10000, 0, 10000))
                == FishingScene.Result.UNSAFE, "Distant terrain must not be loaded");
        h.assertTrue(!FishingScene.validFishingSpot(h.getLevel(), shore, shore.offset(65536, -1, 4)),
                "Large coordinates must not overflow the short casting-distance check");
        h.assertTrue(h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).isEmpty()
                && h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).isEmpty(), "Invalid scene creates nothing");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 650)
    public static void cyclesOnDryBankAndKeepsCatNearby(GameTestHelper h) {
        place(h); AmbientNpc npc = fisher(h); Cat cat = cat(h);
        for (int tick = 5; tick < 600; tick++) h.runAtTickTime(tick, () -> {
            h.assertTrue(npc.isWithinActivityArea(npc.blockPosition()) && !npc.isInWater(), "Fisher stays on dry bank");
            h.assertTrue(FishingScene.waterfront(h.getLevel(), npc.blockPosition(), FishingActivity.water(npc)), "Fisher remains directly beside water");
            h.assertTrue(cat.isInSittingPose() && cat.distanceToSqr(npc) < 4 && !cat.isInWater(), "Cat stays dry and near");
            h.assertTrue(FishingActivity.cycleTick(npc) >= 0 && FishingActivity.cycleTick(npc) < FishingCycle.LENGTH, "Active cycle in bounds");
            h.assertTrue(h.getLevel().getEntitiesOfClass(FishingHook.class, h.getBounds()).isEmpty(), "No saved bobber entities");
        });
        h.runAtTickTime(60, () -> h.assertTrue(FishingActivity.cycleTick(npc) >= 40 && FishingActivity.cycleTick(npc) < 200, "Waiting phase"));
        h.runAtTickTime(210, () -> h.assertTrue(FishingActivity.cycleTick(npc) >= 200 && FishingActivity.cycleTick(npc) < 220, "Retrieve phase"));
        h.runAtTickTime(270, () -> h.assertTrue(FishingActivity.cycleTick(npc) >= 20 && FishingActivity.cycleTick(npc) < 40, "Next cast occurs"));
        h.runAtTickTime(600, h::succeed);
    }

    @GameTest(template = "arena")
    public static void pausesAndRecoversWhenWaterCorridorOrRodChanges(GameTestHelper h) {
        BlockPos shore = place(h), water = water(shore); AmbientNpc npc = fisher(h);
        h.runAtTickTime(5, () -> {
            h.assertTrue(FishingActivity.cycleTick(npc) >= 0, "Starts fishing");
            h.getLevel().setBlockAndUpdate(water, Blocks.STONE.defaultBlockState());
        });
        h.runAtTickTime(10, () -> {
            h.assertTrue(FishingActivity.cycleTick(npc) == -1, "Missing water hides bobber");
            h.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        });
        h.runAtTickTime(15, () -> {
            h.assertTrue(FishingActivity.cycleTick(npc) >= 0, "Water restoration recovers");
            h.getLevel().setBlockAndUpdate(shore.offset(0, 0, 2), Blocks.STONE.defaultBlockState());
        });
        h.runAtTickTime(20, () -> {
            h.assertTrue(FishingActivity.cycleTick(npc) == -1, "Obstructed cast pauses");
            h.getLevel().setBlockAndUpdate(shore.offset(0, 0, 2), Blocks.AIR.defaultBlockState());
        });
        h.runAtTickTime(25, () -> {
            h.assertTrue(FishingActivity.cycleTick(npc) >= 0, "Cleared corridor recovers");
            npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        });
        h.runAtTickTime(30, () -> {
            h.assertTrue(FishingActivity.cycleTick(npc) == -1, "No rod stops cycle");
            npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        });
        h.runAtTickTime(35, () -> { h.assertTrue(FishingActivity.cycleTick(npc) >= 0, "Rod restoration recovers"); h.succeed(); });
    }

    @GameTest(template = "arena")
    public static void savesPairAndAssociationWithoutBobbers(GameTestHelper h) {
        BlockPos shore = place(h); AmbientNpc npc = fisher(h); Cat cat = cat(h);
        var npcId = npc.getUUID(); var catId = cat.getUUID(); var petSpot = ((FishingActivity) npc.getActivity()).petSpot();
        CompoundTag npcData = new CompoundTag(), catData = new CompoundTag();
        h.assertTrue(npc.save(npcData) && cat.save(catData), "Both entities must save");
        npc.discard(); cat.discard();
        var loadedCat = EntityType.loadEntityRecursive(catData, h.getLevel(), e -> e);
        var loadedNpc = (AmbientNpc)EntityType.loadEntityRecursive(npcData, h.getLevel(), e -> e);
        h.assertTrue(loadedNpc != null && loadedCat instanceof Cat, "Both entities reload");
        h.assertTrue(FishingActivity.cycleTick(loadedNpc) == -1, "Never restore a half-finished cast");
        h.getLevel().addFreshEntity(loadedCat); h.getLevel().addFreshEntity(loadedNpc);
        h.runAfterDelay(5, () -> {
            AmbientNpc restored = fisher(h); Cat restoredCat = cat(h);
            h.assertTrue(restored.getUUID().equals(npcId) && restoredCat.getUUID().equals(catId), "Same identities");
            h.assertTrue(catId.equals(((FishingActivity) restored.getActivity()).petCatId()) && npcId.equals(restoredCat.getOwnerUUID()), "Same association");
            h.assertTrue(shore.equals(restored.getActivitySpot()) && water(shore).equals(FishingActivity.water(restored))
                    && petSpot.equals(((FishingActivity) restored.getActivity()).petSpot()), "Saved scene positions");
            h.assertTrue(FishingActivity.cycleTick(restored) >= 0 && restoredCat.isInSittingPose(), "Activity resumes safely");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).size() == 1
                    && h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).size() == 1, "No replacement entities");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void recoversDisplacedCatButNeverReplacesMissingPet(GameTestHelper h) {
        place(h); AmbientNpc npc = fisher(h); Cat cat = cat(h); BlockPos spot = ((FishingActivity) npc.getActivity()).petSpot();
        cat.teleportTo(spot.getX() + 3.5, spot.getY(), spot.getZ() + 0.5);
        h.runAtTickTime(25, () -> {
            h.assertTrue(cat.blockPosition().equals(spot), "Displaced cat returns to safe saved bank spot");
            cat.discard();
        });
        h.runAtTickTime(60, () -> {
            h.assertTrue(h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).isEmpty(), "Missing cat is not respawned");
            h.assertTrue(((FishingActivity) npc.getActivity()).petCatId() != null, "Association retained without creating replacement");
            h.succeed();
        });
    }
}
