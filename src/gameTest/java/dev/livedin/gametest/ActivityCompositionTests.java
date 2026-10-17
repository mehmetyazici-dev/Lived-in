package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import dev.livedin.entity.activity.BoatActivity;
import dev.livedin.entity.activity.FishingCast;
import dev.livedin.entity.activity.CampActivity;
import dev.livedin.entity.activity.FishingActivity;
import dev.livedin.entity.activity.TravelerActivity;
import dev.livedin.entity.goal.ActivityGoal;
import dev.livedin.registry.ModContent;
import dev.livedin.scene.BoatScene;
import dev.livedin.scene.FishingScene;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ActivityCompositionTests {
    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }
    private static AmbientNpc npc(GameTestHelper h) {
        floor(h);
        return h.spawn(ModContent.AMBIENT_NPC.get(), new BlockPos(12, 1, 12));
    }
    private static AmbientNpc restoreLegacy(GameTestHelper h, AmbientNpc original, CompoundTag tag) {
        UUID id = original.getUUID();
        original.discard();
        AmbientNpc loaded = (AmbientNpc) EntityType.loadEntityRecursive(tag, h.getLevel(), e -> e);
        h.assertTrue(loaded != null && loaded.getUUID().equals(id), "Earlier flat NBT preserves registered type and identity");
        h.getLevel().addFreshEntity(loaded);
        return loaded;
    }
    private static CompoundTag vanillaSave(GameTestHelper h, AmbientNpc npc) {
        CompoundTag tag = new CompoundTag();
        h.assertTrue(npc.save(tag), "Entity saves");
        return tag;
    }

    @GameTest(template = "arena")
    public static void loadsLegacyCampRolesAndRemovedInstrument(GameTestHelper h) {
        AmbientNpc original = npc(h);
        original.setHealth(13);
        BlockPos fire = original.blockPosition().offset(0, 0, -3), spot = original.blockPosition();
        h.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        CompoundTag tag = vanillaSave(h, original);
        // Explicit pre-composition format, independent of the new activity serializer.
        tag.putInt("NpcRole", 1); tag.putLong("CampfireSpot", fire.asLong()); tag.putLong("ActivitySpot", spot.asLong());
        AmbientNpc loaded = restoreLegacy(h, original, tag);
        h.assertTrue(loaded.getActivity() instanceof CampActivity camp && camp.role() == NpcRole.MUSICIAN
                && fire.equals(camp.fire()) && spot.equals(loaded.getActivitySpot()), "Earlier musician fields restored");
        h.assertTrue(loaded.getHealth() == 13 && loaded.getOffhandItem().isEmpty(), "No replacement lute or reset health");
        tag.putInt("NpcRole", 2);
        CompoundTag companionTag = tag.copy(); companionTag.putUUID("UUID", UUID.randomUUID());
        AmbientNpc companion = (AmbientNpc) EntityType.loadEntityRecursive(companionTag, h.getLevel(), e -> e);
        h.assertTrue(companion.getActivity() instanceof CampActivity camp && camp.role() == NpcRole.CAMP_COMPANION
                && camp.radius() == 2, "Earlier companion role retains its activity rules");
        h.runAfterDelay(3, () -> { h.assertTrue(!CampActivity.isPerforming(loaded), "Removed instrument stays removed"); h.succeed(); });
    }

    @GameTest(template = "arena")
    public static void loadsLegacyFishingFieldsAndStartsFreshCycle(GameTestHelper h) {
        AmbientNpc original = npc(h);
        original.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        BlockPos spot = original.blockPosition(), water = spot.offset(0, -1, 4), pet = spot.offset(-1, 0, 0);
        UUID catId = UUID.randomUUID();
        CompoundTag tag = vanillaSave(h, original);
        tag.putInt("NpcRole", 3); tag.putLong("ActivitySpot", spot.asLong()); tag.putLong("FishingWater", water.asLong());
        tag.putUUID("PetCat", catId); tag.putLong("PetSpot", pet.asLong());
        AmbientNpc loaded = restoreLegacy(h, original, tag);
        FishingActivity activity = (FishingActivity) loaded.getActivity();
        h.assertTrue(catId.equals(activity.petCatId()) && pet.equals(activity.petSpot()) && water.equals(FishingActivity.water(loaded)),
                "Earlier fishing target and original pet association restored");
        h.assertTrue(FishingActivity.cycleTick(loaded) == -1 && loaded.getMainHandItem().is(Items.FISHING_ROD),
                "Restores equipment but not a half-finished cast");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void loadsLegacyBoatFieldsAndUnknownRoleFallback(GameTestHelper h) {
        AmbientNpc original = npc(h);
        BlockPos start = original.blockPosition().below(), end = start.offset(0, 0, 8);
        UUID boatId = UUID.randomUUID();
        CompoundTag tag = vanillaSave(h, original);
        tag.putInt("NpcRole", 4); tag.putUUID("SceneBoat", boatId); tag.putLong("BoatStart", start.asLong());
        tag.putLong("BoatEnd", end.asLong()); tag.putBoolean("BoatHeadingToEnd", false);
        AmbientNpc loaded = restoreLegacy(h, original, tag);
        BoatActivity activity = (BoatActivity) loaded.getActivity();
        h.assertTrue(boatId.equals(activity.boatId()) && start.equals(activity.start()) && end.equals(activity.end())
                && !activity.headingToEnd(), "Earlier boat identity, endpoints, and direction restored");
        tag.putInt("NpcRole", 999); tag.putUUID("UUID", UUID.randomUUID());
        AmbientNpc fallback = (AmbientNpc) EntityType.loadEntityRecursive(tag, h.getLevel(), e -> e);
        h.assertTrue(fallback.getActivity() instanceof TravelerActivity && fallback.getRole() == NpcRole.TRAVELER,
                "Unknown old role falls back safely without creating scene members");
        h.succeed();
    }

    @GameTest(template = "arena")
    public static void switchingCampToFishingReleasesPerformanceAndGoal(GameTestHelper h) {
        AmbientNpc npc = npc(h);
        BlockPos spot = npc.blockPosition(), fire = spot.offset(0, 0, -3);
        h.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        npc.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModContent.LUTE.get()));
        npc.setActivity(new CampActivity(NpcRole.MUSICIAN, fire), spot);
        h.runAfterDelay(3, () -> {
            h.assertTrue(CampActivity.isPerforming(npc), "Original activity performs before transition");
            ActivityGoal goal = new ActivityGoal(npc); goal.start();
            npc.setActivity(new FishingActivity(spot.offset(0, -1, 4), UUID.randomUUID(), spot.offset(1, 0, 0)), spot);
            h.assertTrue(!CampActivity.isPerforming(npc) && !goal.canContinueToUse(), "Old performance and running goal released");
            CompoundTag saved = vanillaSave(h, npc);
            h.assertTrue(saved.getInt("NpcRole") == 3 && !saved.contains("CampfireSpot"), "Only the current activity writes its state");
            h.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 100)
    public static void switchingFishingStopsCycleAndPetMaintenance(GameTestHelper h) {
        floor(h);
        BlockPos shore = h.absolutePos(new BlockPos(12, 1, 8)), water = shore.offset(0, -1, 4);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            h.getLevel().setBlockAndUpdate(water.offset(x, 0, z), Blocks.WATER.defaultBlockState());
        for (int z = 1; z <= 2; z++) h.getLevel().setBlockAndUpdate(shore.offset(0, -1, z), Blocks.WATER.defaultBlockState());
        h.assertTrue(FishingScene.place(h.getLevel(), shore, water) == FishingScene.Result.SUCCESS, "Fishing scene places");
        AmbientNpc npc = h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).getFirst();
        Cat cat = h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).getFirst();
        h.runAtTickTime(5, () -> {
            h.assertTrue(FishingActivity.cycleTick(npc) >= 0, "Fishing runs before transition");
            npc.setActivity(new TravelerActivity(), shore);
            cat.teleportTo(shore.getX() + 4.5, shore.getY(), shore.getZ() + 0.5);
            h.assertTrue(FishingActivity.water(npc) == null && FishingActivity.cycleTick(npc) == -1, "Old client water/cycle cleared");
            CompoundTag saved = vanillaSave(h, npc);
            h.assertTrue(!saved.contains("PetCat") && !saved.contains("FishingWater"), "Fishing state is absent from traveler save");
        });
        h.runAtTickTime(45, () -> {
            h.assertTrue(cat.getX() > shore.getX() + 3, "Old activity no longer teleports its pet");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Cat.class, h.getBounds()).size() == 1, "Original cat retained without replacement");
            h.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void switchingWithinSameRoleReplacesState(GameTestHelper h) {
        AmbientNpc npc = npc(h);
        BlockPos spot = npc.blockPosition(), firstWater = spot.offset(0, -1, 4), secondWater = spot.offset(4, -1, 0);
        UUID firstPet = UUID.randomUUID(), secondPet = UUID.randomUUID(), id = npc.getUUID();
        FishingActivity first = new FishingActivity(firstWater, firstPet, spot.offset(-1, 0, 0));
        npc.setActivity(first, spot);
        npc.setActivity(new FishingActivity(secondWater, secondPet, spot.offset(0, 0, 1)), spot);
        h.assertTrue(npc.getActivity() != first && id.equals(npc.getUUID()) && secondWater.equals(FishingActivity.water(npc)),
                "Same role replacement changes synchronized target without replacing entity");
        CompoundTag tag = vanillaSave(h, npc);
        h.assertTrue(secondPet.equals(tag.getUUID("PetCat")) && tag.getLong("FishingWater") == secondWater.asLong(),
                "No stale association saved after same-role replacement");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100)
    public static void switchingBoatReleasesMountAndStopsOriginalBoat(GameTestHelper h) {
        floor(h);
        for (int x = 7; x <= 13; x++) for (int z = 5; z <= 17; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.WATER); h.setBlock(new BlockPos(x, -1, z), Blocks.STONE);
        }
        BlockPos start = h.absolutePos(new BlockPos(10, 0, 7));
        h.assertTrue(BoatScene.place(h.getLevel(), start, start.offset(0, 0, 8)) == BoatScene.Result.SUCCESS, "Boat scene places");
        AmbientNpc npc = h.getLevel().getEntitiesOfClass(AmbientNpc.class, h.getBounds()).getFirst();
        Boat boat = (Boat) npc.getVehicle();
        h.runAtTickTime(10, () -> {
            h.assertTrue(FishingCast.water(npc) != null, "Boat fishes before transition");
            BlockPos shore = h.absolutePos(new BlockPos(15, 1, 8));
            npc.setActivity(new TravelerActivity(), shore);
            npc.teleportTo(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5);
            h.assertTrue(!npc.isPassenger() && !boat.isVehicle() && !boat.getPaddleState(0), "Releases old mount and oars immediately");
        });
        h.runAtTickTime(30, () -> {
            h.assertTrue(boat.getDeltaMovement().horizontalDistanceSqr() < 0.001, "Old boat has no leftover steering");
            h.assertTrue(h.getLevel().getEntitiesOfClass(Boat.class, h.getBounds()).size() == 1, "Retains only original boat");
            h.assertTrue(!vanillaSave(h, npc).contains("SceneBoat"), "Traveler no longer saves a boat association");
            h.succeed();
        });
    }
}
