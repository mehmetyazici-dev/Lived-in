package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
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
    public static void createsLinkedPairAndRejectsRepeat(GameTestHelper h) {
        BlockPos shore = place(h);
        AmbientNpc npc = fisher(h); Cat cat = cat(h);
        h.assertTrue(npc.getRole() == NpcRole.FISHER && npc.getMainHandItem().is(Items.FISHING_ROD), "Fisher role and rod");
        h.assertTrue(cat.getUUID().equals(npc.getPetCatId()) && npc.getUUID().equals(cat.getOwnerUUID()), "Saved reciprocal identity");
        h.assertTrue(FishingScene.place(h.getLevel(), shore, water(shore)) == FishingScene.Result.OCCUPIED, "No duplicate scene");
        h.runAfterDelay(5, () -> {
            h.assertTrue(cat.isInSittingPose() && npc.canFish(), "Cat sits while fisher is active");
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
            h.assertTrue(cat.isInSittingPose() && cat.distanceToSqr(npc) < 4 && !cat.isInWater(), "Cat stays dry and near");
            h.assertTrue(npc.getFishingTick() >= 0 && npc.getFishingTick() < FishingCycle.LENGTH, "Active cycle in bounds");
            h.assertTrue(h.getLevel().getEntitiesOfClass(FishingHook.class, h.getBounds()).isEmpty(), "No saved bobber entities");
        });
        h.runAtTickTime(60, () -> h.assertTrue(npc.getFishingTick() >= 40 && npc.getFishingTick() < 200, "Waiting phase"));
        h.runAtTickTime(210, () -> h.assertTrue(npc.getFishingTick() >= 200 && npc.getFishingTick() < 220, "Retrieve phase"));
        h.runAtTickTime(270, () -> h.assertTrue(npc.getFishingTick() >= 20 && npc.getFishingTick() < 40, "Next cast occurs"));
        h.runAtTickTime(600, h::succeed);
    }

    @GameTest(template = "arena")
    public static void pausesAndRecoversWhenWaterCorridorOrRodChanges(GameTestHelper h) {
        BlockPos shore = place(h), water = water(shore); AmbientNpc npc = fisher(h);
        h.runAtTickTime(5, () -> {
            h.assertTrue(npc.getFishingTick() >= 0, "Starts fishing");
            h.getLevel().setBlockAndUpdate(water, Blocks.STONE.defaultBlockState());
        });
        h.runAtTickTime(10, () -> {
            h.assertTrue(npc.getFishingTick() == -1, "Missing water hides bobber");
            h.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        });
        h.runAtTickTime(15, () -> {
            h.assertTrue(npc.getFishingTick() >= 0, "Water restoration recovers");
            h.getLevel().setBlockAndUpdate(shore.offset(0, 0, 2), Blocks.STONE.defaultBlockState());
        });
        h.runAtTickTime(20, () -> {
            h.assertTrue(npc.getFishingTick() == -1, "Obstructed cast pauses");
            h.getLevel().setBlockAndUpdate(shore.offset(0, 0, 2), Blocks.AIR.defaultBlockState());
        });
        h.runAtTickTime(25, () -> {
            h.assertTrue(npc.getFishingTick() >= 0, "Cleared corridor recovers");
            npc.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        });
        h.runAtTickTime(30, () -> {
            h.assertTrue(npc.getFishingTick() == -1, "No rod stops cycle");
            npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        });
        h.runAtTickTime(35, () -> { h.assertTrue(npc.getFishingTick() >= 0, "Rod restoration recovers"); h.succeed(); });
    }

    @GameTest(template = "arena")
    public static void savesPairAndAssociationWithoutBobbers(GameTestHelper h) {
        BlockPos shore = place(h); AmbientNpc npc = fisher(h); Cat cat = cat(h);
        var npcId = npc.getUUID(); var catId = cat.getUUID(); var petSpot = npc.getPetSpot();
        CompoundTag npcData = new CompoundTag(), catData = new CompoundTag();
        h.assertTrue(npc.save(npcData) && cat.save(catData), "Both entities must save");
        npc.discard(); cat.discard();
        var loadedCat = EntityType.loadEntityRecursive(catData, h.getLevel(), e -> e);
        var loadedNpc = (AmbientNpc)EntityType.loadEntityRecursive(npcData, h.getLevel(), e -> e);
        h.assertTrue(loadedNpc != null && loadedCat instanceof Cat, "Both entities reload");
        h.assertTrue(loadedNpc.getFishingTick() == -1, "Never restore a half-finished cast");
        h.getLevel().addFreshEntity(loadedCat); h.getLevel().addFreshEntity(loadedNpc);
        h.runAfterDelay(5, () -> {
            AmbientNpc restored = fisher(h); Cat restoredCat = cat(h);
            h.assertTrue(restored.getUUID().equals(npcId) && restoredCat.getUUID().equals(catId), "Same identities");
            h.assertTrue(catId.equals(restored.getPetCatId()) && npcId.equals(restoredCat.getOwnerUUID()), "Same association");
            h.assertTrue(shore.equals(restored.getActivitySpot()) && water(shore).equals(restored.getFishingWater())
                    && petSpot.equals(restored.getPetSpot()), "Saved scene positions");
            h.assertTrue(restored.getFishingTick() >= 0 && restoredCat.isInSittingPose(), "Activity resumes safely");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).size() == 1
                    && h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).size() == 1, "No replacement entities");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void recoversDisplacedCatButNeverReplacesMissingPet(GameTestHelper h) {
        place(h); AmbientNpc npc = fisher(h); Cat cat = cat(h); BlockPos spot = npc.getPetSpot();
        cat.teleportTo(spot.getX() + 3.5, spot.getY(), spot.getZ() + 0.5);
        h.runAtTickTime(25, () -> {
            h.assertTrue(cat.blockPosition().equals(spot), "Displaced cat returns to safe saved bank spot");
            cat.discard();
        });
        h.runAtTickTime(60, () -> {
            h.assertTrue(h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).isEmpty(), "Missing cat is not respawned");
            h.assertTrue(npc.getPetCatId() != null, "Association retained without creating replacement");
            h.succeed();
        });
    }
}
