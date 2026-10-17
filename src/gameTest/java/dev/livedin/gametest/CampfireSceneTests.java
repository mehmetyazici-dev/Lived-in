package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.CampActivity;
import dev.livedin.entity.NpcRole;
import dev.livedin.registry.ModContent;
import dev.livedin.scene.CampfireScene;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CampfireSceneTests {
    private static BlockPos floor(GameTestHelper helper) {
        for (int x = 0; x < 24; x++) {
            for (int z = 0; z < 24; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        }
        return helper.absolutePos(new BlockPos(12, 1, 12));
    }

    private static List<AmbientNpc> members(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(AmbientNpc.class, helper.getBounds());
    }

    private static AmbientNpc musician(GameTestHelper helper) {
        return members(helper).stream().filter(npc -> npc.getRole() == NpcRole.MUSICIAN).findFirst().orElseThrow();
    }

    private static BlockPos place(GameTestHelper helper) {
        BlockPos center = floor(helper);
        helper.assertTrue(CampfireScene.place(helper.getLevel(), center) == CampfireScene.Result.SUCCESS, "Camp must place");
        return center;
    }

    @GameTest(template = "arena")
    public static void createsOneGroupAndRejectsRepeat(GameTestHelper helper) {
        BlockPos center = place(helper);
        helper.assertTrue(members(helper).size() == 3, "Camp must contain three travelers");
        helper.assertTrue(members(helper).stream().filter(n -> n.getRole() == NpcRole.MUSICIAN).count() == 1,
                "Camp must contain one musician");
        helper.assertTrue(members(helper).stream().filter(n -> n.getRole() == NpcRole.CAMP_COMPANION).count() == 2,
                "Camp must contain two companions");
        helper.assertTrue(musician(helper).getOffhandItem().is(ModContent.LUTE.get()), "Musician must hold a lute");
        helper.assertTrue(CampfireScene.place(helper.getLevel(), center) == CampfireScene.Result.OCCUPIED,
                "Repeated placement must be rejected");
        helper.assertTrue(members(helper).size() == 3, "Repeated placement must not duplicate NPCs");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(CampActivity.isPerforming(musician(helper)), "Musician must perform near a lit fire");
            helper.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void blockedPlacementDoesNotOverwrite(GameTestHelper helper) {
        BlockPos center = floor(helper);
        helper.getLevel().setBlockAndUpdate(center.offset(4, 0, 4), Blocks.DIAMOND_BLOCK.defaultBlockState());
        helper.assertTrue(CampfireScene.place(helper.getLevel(), center) == CampfireScene.Result.OCCUPIED, "Blocked camp must fail");
        helper.assertTrue(helper.getLevel().getBlockState(center).isAir(), "Failed placement must not add fire");
        helper.assertTrue(helper.getLevel().getBlockState(center.offset(4, 0, 4)).is(Blocks.DIAMOND_BLOCK),
                "Existing blocks must not change");
        helper.assertTrue(members(helper).isEmpty(), "Failed placement must not create NPCs");
        helper.succeed();
    }

    @GameTest(template = "arena")
    public static void rejectsUnsafeGroundAndUnloadedArea(GameTestHelper helper) {
        BlockPos center = floor(helper);
        helper.getLevel().setBlockAndUpdate(center.offset(4, -1, 4), Blocks.AIR.defaultBlockState());
        helper.assertTrue(CampfireScene.place(helper.getLevel(), center) == CampfireScene.Result.UNSAFE_GROUND,
                "Missing floor must reject camp");
        helper.assertTrue(CampfireScene.place(helper.getLevel(), center.offset(10000, 0, 10000)) == CampfireScene.Result.UNLOADED,
                "Placement must not load distant chunks");
        helper.assertTrue(members(helper).isEmpty(), "Invalid terrain must not create NPCs");
        helper.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 650)
    public static void groupStaysClearOfFire(GameTestHelper helper) {
        BlockPos center = place(helper);
        List<AmbientNpc> group = members(helper);
        for (int tick = 2; tick < 600; tick++) {
            helper.runAtTickTime(tick, () -> {
                for (AmbientNpc npc : group) {
                    helper.assertTrue(npc.getActivity().isSafePosition(npc, npc.blockPosition()), "Camp member entered fire clearance");
                    helper.assertTrue(npc.isWithinActivityArea(npc.blockPosition()), "Camp member wandered from assigned spot");
                    helper.assertTrue(npc.getHealth() == npc.getMaxHealth() && !npc.isOnFire(), "Camp member must not take fire damage");
                }
            });
        }
        helper.runAfterDelay(600, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(center).getValue(CampfireBlock.LIT), "Fire must remain lit during test");
            helper.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void performanceFollowsFireAndInstrument(GameTestHelper helper) {
        BlockPos center = place(helper);
        AmbientNpc musician = musician(helper);
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(CampActivity.isPerforming(musician), "Lit fire and lute should start performance");
            helper.getLevel().setBlockAndUpdate(center, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(!CampActivity.isPerforming(musician), "Extinguishing fire must stop performance");
            helper.getLevel().setBlockAndUpdate(center, Blocks.CAMPFIRE.defaultBlockState());
        });
        helper.runAtTickTime(12, () -> {
            helper.assertTrue(CampActivity.isPerforming(musician), "Relighting fire should resume performance");
            musician.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        });
        helper.runAtTickTime(16, () -> {
            helper.assertTrue(!CampActivity.isPerforming(musician), "Removing lute must stop performance");
            musician.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModContent.LUTE.get()));
            helper.getLevel().setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        });
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(!CampActivity.isPerforming(musician), "Removing campfire must stop performance");
            helper.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void savesRolesAndWholeGroupWithoutDuplication(GameTestHelper helper) {
        BlockPos center = place(helper);
        List<AmbientNpc> group = new ArrayList<>(members(helper));
        var identities = new HashSet<java.util.UUID>();
        var savedMembers = new ArrayList<CompoundTag>();
        for (AmbientNpc npc : group) {
            identities.add(npc.getUUID());
            var tag = new CompoundTag();
            helper.assertTrue(npc.save(tag), "Camp member must be saveable");
            savedMembers.add(tag);
            npc.discard();
        }
        for (CompoundTag tag : savedMembers) {
            var npc = (AmbientNpc) EntityType.loadEntityRecursive(tag, helper.getLevel(), entity -> entity);
            helper.assertTrue(npc != null && center.equals(((CampActivity) npc.getActivity()).fire()), "Camp association must survive reload");
            helper.assertTrue(!CampActivity.isPerforming(npc), "Performance is recomputed rather than saved active");
            helper.getLevel().addFreshEntity(npc);
        }
        helper.runAfterDelay(3, () -> {
            var reloaded = members(helper);
            helper.assertTrue(reloaded.size() == 3, "Reload must contain exactly three NPCs");
            var restoredIds = new HashSet<java.util.UUID>();
            reloaded.forEach(npc -> restoredIds.add(npc.getUUID()));
            helper.assertTrue(restoredIds.equals(identities), "Reload must preserve all member identities");
            helper.assertTrue(CampActivity.isPerforming(musician(helper)) && musician(helper).getOffhandItem().is(ModContent.LUTE.get()),
                    "Reload must restore musician, instrument, and active behavior");
            helper.succeed();
        });
    }
}
