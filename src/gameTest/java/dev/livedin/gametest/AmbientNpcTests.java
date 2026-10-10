package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.goal.ReturnToActivitySpotGoal;
import dev.livedin.entity.goal.SceneStrollGoal;
import dev.livedin.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AmbientNpcTests {
    private static AmbientNpc spawn(GameTestHelper helper) {
        for (int x = 0; x < 24; x++) {
            for (int z = 0; z < 24; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        return helper.spawn(ModContent.AMBIENT_NPC.get(), new BlockPos(12, 1, 12));
    }

    @GameTest(template = "arena")
    public static void capturesSpawnPosition(GameTestHelper helper) {
        AmbientNpc npc = spawn(helper);
        BlockPos placed = npc.blockPosition();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(placed.equals(npc.getActivitySpot()), "Anchor must be actual placement, not constructor origin");
            helper.assertTrue(npc.isPersistenceRequired() && !npc.removeWhenFarAway(10000), "NPC must not despawn at distance");
            helper.succeed();
        });
    }

    @GameTest(template = "arena")
    public static void retainsIdentityAndAnchorOnReload(GameTestHelper helper) {
        AmbientNpc original = spawn(helper);
        BlockPos anchor = original.blockPosition();
        original.setActivitySpot(anchor);
        original.setCustomName(Component.literal("Persistence Check"));
        original.setHealth(13);
        original.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        original.moveTo(original.getX() + 3, original.getY(), original.getZ());
        CompoundTag saved = new CompoundTag();
        helper.assertTrue(original.save(saved), "Entity must be saveable");
        var uuid = original.getUUID();
        original.discard();
        AmbientNpc restored = (AmbientNpc) EntityType.loadEntityRecursive(saved, helper.getLevel(), entity -> entity);
        helper.assertTrue(restored != null, "Entity registry must reconstruct saved NPC");
        helper.getLevel().addFreshEntity(restored);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(uuid.equals(restored.getUUID()), "Reload must preserve identity");
            helper.assertTrue(anchor.equals(restored.getActivitySpot()), "Reload must not reset anchor to current position");
            helper.assertTrue(restored.getHealth() == 13 && restored.getMainHandItem().is(Items.STICK),
                    "Vanilla health and equipment must survive reload");
            helper.assertTrue("Persistence Check".equals(restored.getCustomName().getString()), "Name must survive reload");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(AmbientNpc.class, helper.getBounds()).size() == 1,
                    "Reload must leave exactly one NPC");
            helper.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 650)
    public static void strollsWithoutLeavingScene(GameTestHelper helper) {
        AmbientNpc npc = spawn(helper);
        var initialPosition = npc.position();
        boolean[] moved = { false };
        for (int tick = 2; tick < 600; tick++) {
            helper.runAtTickTime(tick, () -> {
                helper.assertTrue(npc.isAlive(), "Traveler must stay alive on safe ground");
                helper.assertTrue(npc.isWithinActivityArea(npc.blockPosition()), "Voluntary movement must stay in scene");
                moved[0] |= npc.position().distanceToSqr(initialPosition) > 0.25;
            });
        }
        helper.runAfterDelay(600, () -> {
            helper.assertTrue(moved[0], "Traveler must actually stroll rather than pass by standing still");
            helper.succeed();
        });
    }

    @GameTest(template = "arena", timeoutTicks = 250)
    public static void returnsAfterDisplacement(GameTestHelper helper) {
        AmbientNpc npc = spawn(helper);
        npc.setActivitySpot(npc.blockPosition());
        npc.moveTo(npc.getX() + 8, npc.getY(), npc.getZ());
        helper.succeedWhen(() -> helper.assertTrue(npc.isWithinActivityArea(npc.blockPosition()),
                "NPC must walk back into scene after displacement"));
    }

    @GameTest(template = "arena")
    public static void passengerDoesNotWalk(GameTestHelper helper) {
        AmbientNpc npc = spawn(helper);
        npc.setActivitySpot(npc.blockPosition());
        var boat = helper.spawn(EntityType.BOAT, new BlockPos(20, 1, 12));
        helper.assertTrue(npc.startRiding(boat, true), "NPC must support vanilla passenger attachment");
        helper.assertTrue(!new SceneStrollGoal(npc, 0.7).canUse(), "Passenger must not stroll");
        helper.assertTrue(!new ReturnToActivitySpotGoal(npc, 1).canUse(), "Passenger must not walk home");
        helper.succeed();
    }
}