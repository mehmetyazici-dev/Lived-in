package dev.livedin.gametest;

import com.mojang.authlib.GameProfile;
import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import dev.livedin.entity.activity.CampActivity;
import dev.livedin.entity.activity.TravelerActivity;
import dev.livedin.housing.HouseCatalog;
import dev.livedin.housing.Neighborhoods;
import dev.livedin.registry.ModContent;
import dev.livedin.social.ConversationMenu;
import dev.livedin.social.Invitations;
import dev.livedin.social.NpcRelationships;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhaseTwoLifeTests {
    @GameTest(template = "arena")
    public static void breakingTemporaryCampCannotFarmMaterials(GameTestHelper h) {
        var npc = npc(h); npc.getRoutine().prepare(npc, 13000); npc.getRoutine().move(npc);
        var fire = BlockPos.of(npc.getRoutine().save().getLong("Fire"));
        h.assertTrue(h.getLevel().getBlockState(fire).is(Blocks.CAMPFIRE), "Owned temporary fire exists");
        h.getLevel().destroyBlock(fire, true);
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds()).isEmpty(), "Player/explosion break yields no free campfire materials");
        h.succeed();
    }
    @GameTest(template = "arena", timeoutTicks = 120)
    public static void defendersFireRealArrowsWithoutLootOrFriendlyDamage(GameTestHelper h) {
        var npc = npc(h); useWeapon(npc, "BOW"); var p = player(h, npc); npc.getDefense().attacked(npc, p);
        h.runAtTickTime(80, () -> {
            var arrows = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class, h.getBounds());
            h.assertTrue(!arrows.isEmpty() && arrows.stream().allMatch(a -> a.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED),
                    "Actual defensive shots exist and cannot be farmed for arrows");
            var ally = h.spawn(ModContent.AMBIENT_NPC.get(), new BlockPos(15,1,12)); ally.getProfile(); float health = ally.getHealth();
            ally.hurt(npc.damageSources().arrow(arrows.getFirst(), npc), 8);
            h.assertTrue(ally.getHealth() == health && npc.getMainHandItem().getDamageValue() == 0, "Friendly travelers stay safe and bow has no wear"); h.succeed();
        });
    }
    @GameTest(template = "arena")
    public static void cancelledNaturalHomeSpawnLeavesTerrainIntact(GameTestHelper h) {
        var original = npc(h); original.discard();
        for (int x=0;x<24;x++) for(int z=0;z<24;z++) for(int y=6;y<=9;y++) h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        var origin = h.absolutePos(new BlockPos(2,1,2));
        int rx=Neighborhoods.region(origin.getX()), rz=Neighborhoods.region(origin.getZ());
        CompoundTag tag = new CompoundTag(), center = new CompoundTag(); var centers = new net.minecraft.nbt.ListTag();
        center.putString("Region",rx+":"+rz); center.putLong("Position",origin.offset(34,0,22).atY(0).asLong()); centers.add(center); tag.put("Centers",centers);
        var data=Neighborhoods.load(tag,h.getLevel().registryAccess()); var fresh=new java.util.HashMap<net.minecraft.world.level.ChunkPos,Long>();
        for(int x=origin.getX()>>4;x<=(origin.getX()+19)>>4;x++) for(int z=origin.getZ()>>4;z<=(origin.getZ()+19)>>4;z++) fresh.put(new net.minecraft.world.level.ChunkPos(x,z),h.getLevel().getGameTime());
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> cancel=e->{if(e.getEntity() instanceof AmbientNpc) e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try { h.assertTrue(data.tryRegion(h.getLevel(),rx,rz,fresh),"Fresh opportunity is handled"); }
        finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
        h.assertTrue(data.homeCount()==0 && h.getLevel().getBlockState(origin).isAir() && h.getLevel().getBlockState(origin.below()).is(Blocks.GRASS_BLOCK),
                "Cancelled NPC join does not leave a house, foundation, or claim"); h.succeed();
    }
    private static AmbientNpc npc(GameTestHelper h) {
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
        var npc = h.spawn(ModContent.AMBIENT_NPC.get(), new BlockPos(12, 1, 12));
        npc.setActivitySpot(npc.blockPosition()); return npc;
    }
    private static void useWeapon(AmbientNpc npc, String weapon) {
        CompoundTag tag = new CompoundTag(); tag.putString("Weapon", weapon); npc.getDefense().load(tag, npc);
    }
    private static ServerPlayer player(GameTestHelper h, AmbientNpc npc) {
        var p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "barter-test"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(npc.getX() + 1, npc.getY(), npc.getZ()); return p;
    }
    private static void unlock(GameTestHelper h, AmbientNpc npc, ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 20));
        for (int i = 0; i < 20; i++) h.assertTrue(npc.getRelationships().offer(npc, player, InteractionHand.MAIN_HAND), "Valued item accepted");
    }
    @GameTest(template = "arena")
    public static void savedHomeClaimsAndNegativeRegions(GameTestHelper h) {
        var npc = npc(h);
        var data = new Neighborhoods();
        BlockPos origin = h.absolutePos(new BlockPos(2, 1, 2));
        var home = new Neighborhoods.Home(origin, Rotation.CLOCKWISE_90, DyeColor.BLUE);
        data.assign(npc.getUUID(), home); data.assign(npc.getUUID(), new Neighborhoods.Home(origin.offset(100, 0, 0), Rotation.NONE, DyeColor.WHITE));
        var saved = data.save(new CompoundTag(), h.getLevel().registryAccess());
        var loaded = Neighborhoods.load(saved, h.getLevel().registryAccess());
        h.assertTrue(home.equals(loaded.home(npc.getUUID())), "One original home across duplicate assignment/reload");
        h.assertTrue(Neighborhoods.region(-1) == -1 && Neighborhoods.region(-700) == -1 && Neighborhoods.region(-701) == -2,
                "Negative coordinates use correct 700-block cells");
        h.assertTrue(Neighborhoods.center(1, 0).getX() - Neighborhoods.center(0, 0).getX() == 700, "Center spacing");
        h.assertTrue(HouseCatalog.designs().size() == 1, "One starter design is sufficient and remains extensible");
        h.succeed();
    }
    @GameTest(template = "arena")
    public static void houseSitesRejectBuildingsWaterAndHoles(GameTestHelper h) {
        npc(h).discard();
        var origin = h.absolutePos(new BlockPos(2, 1, 2));
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++) for (int y = 6; y <= 9; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        h.assertTrue(Neighborhoods.safeSite(h.getLevel(), origin), "Level natural site accepted");
        h.setBlock(new BlockPos(12, 1, 12), Blocks.CHEST);
        h.assertTrue(!Neighborhoods.safeSite(h.getLevel(), origin), "Reject existing build without overwriting it");
        h.setBlock(new BlockPos(12, 1, 12), Blocks.AIR); h.setBlock(new BlockPos(12, 0, 12), Blocks.WATER);
        h.assertTrue(!Neighborhoods.safeSite(h.getLevel(), origin), "Reject water foundation");
        for (int y = 0; y >= -4; y--) h.setBlock(new BlockPos(12, y, 12), Blocks.AIR);
        h.assertTrue(!Neighborhoods.safeSite(h.getLevel(), origin), "Reject deep hole"); h.succeed();
    }
    @GameTest(template = "arena")
    public static void homelessCampAndEquipmentSurviveReload(GameTestHelper h) {
        var npc = npc(h); useWeapon(npc, "BOW"); npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        npc.getRoutine().prepare(npc, 13000); npc.getRoutine().move(npc); npc.getDefense().tick(npc);
        h.assertTrue(npc.getMainHandItem().is(Items.BOW) && npc.getMainHandItem().has(DataComponents.UNBREAKABLE), "Night bow never wears out");
        var registry = h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.POWER), npc.getMainHandItem()) == 3
                && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.PUNCH), npc.getMainHandItem()) == 1, "Exact defensive enchantments");
        CompoundTag saved = new CompoundTag(); npc.save(saved); UUID id = npc.getUUID(); npc.discard();
        var restored = (AmbientNpc) EntityType.loadEntityRecursive(saved, h.getLevel(), e -> e);
        h.getLevel().addFreshEntity(restored);
        restored.getRoutine().prepare(restored, 1000); restored.getRoutine().move(restored); restored.getDefense().tick(restored);
        h.assertTrue(id.equals(restored.getUUID()) && restored.getMainHandItem().is(Items.FISHING_ROD), "Dawn after reload restores original rod and person");
        var fire = BlockPos.of(saved.getCompound("LivedInRoutine").getLong("Fire"));
        h.assertTrue(!h.getLevel().getBlockState(fire).is(Blocks.CAMPFIRE), "Owned temporary camp removed without drops"); h.succeed();
    }
    @GameTest(template = "arena")
    public static void neverRemovesPlayerCampfireAndCampsStayExcluded(GameTestHelper h) {
        var npc = npc(h); npc.getRoutine().prepare(npc, 13000); npc.getRoutine().move(npc);
        var saved = npc.getRoutine().save(); var fire = BlockPos.of(saved.getLong("Fire"));
        h.getLevel().removeBlock(fire, false); h.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState());
        npc.getRoutine().cleanupFire(npc);
        h.assertTrue(h.getLevel().getBlockState(fire).is(Blocks.CAMPFIRE), "Player replacement has no NPC owner tag and remains");
        var camp = ModContent.AMBIENT_NPC.get().create(h.getLevel());
        camp.moveTo(npc.getX() + 4, npc.getY(), npc.getZ()); camp.setActivity(new CampActivity(NpcRole.CAMP_COMPANION, fire), camp.blockPosition());
        camp.getRoutine().prepare(camp, 13000);
        h.assertTrue(!camp.getRoutine().active() && camp.getProfile().eligibility().defense(), "Lute companion defends without nightly relocation"); h.succeed();
    }
    @GameTest(template = "arena")
    public static void housedNpcSleepsThenReturnsToDaytimeAnchor(GameTestHelper h) {
        var npc = npc(h); var origin = h.absolutePos(new BlockPos(2, 1, 2));
        h.assertTrue(HouseCatalog.STARTER.place(h.getLevel(), origin, Rotation.NONE, DyeColor.WHITE), "House places");
        var home = new Neighborhoods.Home(origin, Rotation.NONE, DyeColor.WHITE); Neighborhoods.get(h.getLevel()).assign(npc.getUUID(), home);
        var bed = home.bed(); npc.teleportTo(bed.getX() - .5, bed.getY(), bed.getZ() + .5);
        npc.getRoutine().prepare(npc, 13000); npc.getRoutine().move(npc);
        h.assertTrue(npc.isSleeping(), "At home NPC uses real bed");
        npc.getRoutine().prepare(npc, 0);
        h.assertTrue(!npc.isSleeping(), "Sleep-skipped dawn wakes NPC");
        var anchor = npc.getActivitySpot(); npc.teleportTo(anchor.getX()+.5, anchor.getY(), anchor.getZ()+.5); npc.getRoutine().move(npc);
        h.assertTrue(npc.allowsDayActivity(), "Original daytime activity resumes at original anchor"); h.succeed();
    }
    @GameTest(template = "arena")
    public static void barterCountsMixedUnitsAndRejectsBlocks(GameTestHelper h) {
        var npc = npc(h); var p = player(h, npc); var other = player(h, npc);
        for (var item : new net.minecraft.world.item.Item[] {Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT}) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 3));
            h.assertTrue(npc.getRelationships().offer(npc, p, InteractionHand.MAIN_HAND) && p.getMainHandItem().getCount() == 2, "One accepted unit per offer");
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_BLOCK, 3));
        h.assertTrue(!npc.getRelationships().offer(npc, p, InteractionHand.MAIN_HAND) && p.getMainHandItem().getCount() == 3, "Building blocks rejected intact");
        h.assertTrue(npc.getRelationships().progress(p.getUUID()) == 4 && npc.getRelationships().progress(other.getUUID()) == 0, "Private per-player counts");
        var food = h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds());
        h.assertTrue(food.size() == 4 && food.stream().allMatch(e -> e.getItem().getCount() == 1 && e.getItem().has(DataComponents.FOOD)), "One consumable food per accepted unit");
        var loaded = new NpcRelationships(); loaded.load(npc.getRelationships().save());
        h.assertTrue(loaded.progress(p.getUUID()) == 4 && loaded.progress(other.getUUID()) == 0, "Private progress survives save"); h.succeed();
    }
    @GameTest(template = "arena")
    public static void invitationIsUniqueOwnedAndDeathInvalidates(GameTestHelper h) {
        var npc = npc(h); var p = player(h, npc); var other = player(h, npc); unlock(h, npc, p); unlock(h, npc, other);
        var registry = Invitations.get(h.getLevel());
        h.assertTrue(registry.issue(npc, p), "Unlocked player receives unique invite");
        ItemStack invite = p.getInventory().items.stream().filter(i -> i.is(ModContent.INVITATION.get())).findFirst().orElseThrow().copy();
        h.assertTrue(registry.valid(invite, p.getUUID()) && !registry.valid(invite, other.getUUID()), "Token belongs to NPC and inviting player");
        p.getInventory().clearContent();
        h.assertTrue(!registry.issue(npc, p) && !registry.issue(npc, other), "Lost or destroyed invitation cannot be reissued to either player");
        var reloaded = Invitations.load(registry.save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess());
        h.assertTrue(reloaded.valid(invite, p.getUUID()), "Registry survives world save");
        npc.kill(); h.assertTrue(!registry.valid(invite, p.getUUID()), "Death before joining invalidates invitation permanently"); h.succeed();
    }
    @GameTest(template = "arena")
    public static void fullInventoryCannotBurnInvitationAndMenusValidate(GameTestHelper h) {
        var npc = npc(h); var p = player(h, npc); unlock(h, npc, p);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        var registry = Invitations.get(h.getLevel());
        h.assertTrue(!registry.issue(npc, p) && !registry.issued(npc.getUUID()), "Full inventory does not consume one issuance");
        p.getInventory().setItem(10, ItemStack.EMPTY);
        var menu = ConversationMenu.forNpc(1, npc, p);
        h.assertTrue(menu.description.getCompound("Stats").equals(npc.getProfile().stats().save()) && menu.stillValid(p), "Conversation displays stable real stats");
        p.setPos(npc.getX() + 20, npc.getY(), npc.getZ());
        h.assertTrue(!menu.clickMenuButton(p, 0) && !registry.issued(npc.getUUID()), "Out-of-range forged menu action rejected");
        p.setPos(npc.getX() + 1, npc.getY(), npc.getZ());
        h.assertTrue(menu.clickMenuButton(p, 0) && registry.issued(npc.getUUID()), "Authorized menu action issues invite"); h.succeed();
    }
    @GameTest(template = "arena")
    public static void lockedOrDeadNpcCannotInviteAndDeathDropsNothing(GameTestHelper h) {
        var npc = npc(h); var p = player(h, npc);
        h.assertTrue(!Invitations.get(h.getLevel()).issue(npc, p), "No invitation before twenty items");
        npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW)); npc.setDropChance(EquipmentSlot.MAINHAND, 1);
        npc.kill();
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds()).isEmpty(), "No equipment or death loot even with forced drop chance");
        h.assertTrue(!Invitations.get(h.getLevel()).issue(npc, p), "Dead NPC cannot issue"); h.succeed();
    }
    @GameTest(template = "arena")
    public static void defendersRetaliateButRespectResidentOwner(GameTestHelper h) {
        var npc = npc(h); var p = player(h, npc);
        npc.getDefense().attacked(npc, p);
        h.assertTrue(npc.getTarget() == p, "Retaliates against attacking survival player");
        npc.setTarget(null); npc.getDefense().setVillageOwner(p.getUUID()); npc.getDefense().attacked(npc, p);
        h.assertTrue(npc.getTarget() == null, "Future resident owner is never a retaliation target");
        var ally = h.spawn(ModContent.AMBIENT_NPC.get(), new BlockPos(15,1,12));
        h.assertTrue(!npc.getDefense().valid(npc, ally), "Travelers are allies"); h.succeed();
    }
}
