package dev.livedin.gametest;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import dev.livedin.entity.activity.CampActivity;
import dev.livedin.entity.life.NpcDefense;
import dev.livedin.registry.ModContent;
import dev.livedin.world.EncounterPlacement;
import dev.livedin.world.NaturalEncounters;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivedIn.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DefenseAndCampTests {
    private static void floor(GameTestHelper h) {
        for (int x=0; x<24; x++) for (int z=0; z<24; z++) h.setBlock(new BlockPos(x,0,z), Blocks.GRASS_BLOCK);
    }
    private static AmbientNpc companion(GameTestHelper h, String weapon, int x) {
        var npc = ModContent.AMBIENT_NPC.get().create(h.getLevel());
        var pos = h.absolutePos(new BlockPos(x,1,12));
        npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        npc.setActivity(new CampActivity(NpcRole.CAMP_COMPANION, null), pos);
        CompoundTag state = new CompoundTag(); state.putString("Weapon", weapon); npc.getDefense().load(state,npc);
        h.getLevel().addFreshEntity(npc); npc.getDefense().tick(npc); return npc;
    }
    @GameTest(template="arena")
    public static void companionsAlwaysCarryExactIndestructibleLoadoutsAndRememberThem(GameTestHelper h) {
        floor(h);
        var enchantments = h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (var weapon : NpcDefense.Weapon.values()) {
            var npc = companion(h,weapon.name(),4+weapon.ordinal()*6);
            var stack = npc.getMainHandItem();
            h.assertTrue(npc.getTarget()==null && npc.getDefense().armed(npc) && stack.has(DataComponents.UNBREAKABLE),
                    "Unthreatened companion visibly carries an indestructible "+weapon);
            var first = switch(weapon) { case SWORD, AXE -> Enchantments.SHARPNESS; case BOW -> Enchantments.POWER; };
            h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(enchantments.getHolderOrThrow(first),stack)
                    == (weapon==NpcDefense.Weapon.AXE ? 4 : 3), "Exact damage enchantment");
            if (weapon!=NpcDefense.Weapon.AXE) h.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(
                    enchantments.getHolderOrThrow(weapon==NpcDefense.Weapon.BOW ? Enchantments.PUNCH : Enchantments.KNOCKBACK),stack)==1,
                    "Exact knockback enchantment");
            CompoundTag saved = new CompoundTag(); npc.save(saved); npc.discard();
            var restored = (AmbientNpc)EntityType.loadEntityRecursive(saved,h.getLevel(),e->e);
            restored.getDefense().tick(restored);
            h.assertTrue(restored.getDefense().weapon(restored)==weapon && ItemStack.isSameItemSameComponents(stack,restored.getMainHandItem()),
                    "Reload keeps companion weapon and enchantments");
        }
        h.succeed();
    }
    @GameTest(template="arena",timeoutTicks=100)
    public static void swordAndAxeGoalsHitEnemiesWithoutWearingWeapons(GameTestHelper h) {
        floor(h);
        var defenders = new java.util.ArrayList<AmbientNpc>();
        var enemies = new java.util.ArrayList<net.minecraft.world.entity.monster.Husk>();
        for (var weapon : new String[]{"SWORD","AXE"}) {
            // More than the 12-block ally alert distance, so each defender retains its own target.
            int x = weapon.equals("SWORD") ? 3 : 20;
            var npc = companion(h,weapon,x);
            var enemy = h.spawn(EntityType.HUSK,new BlockPos(x,1,14)); enemy.setNoAi(true);
            enemy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100); enemy.setHealth(100);
            npc.getDefense().attacked(npc,enemy); defenders.add(npc); enemies.add(enemy);
        }
        h.runAtTickTime(60,()->{
            for(int i=0;i<2;i++) h.assertTrue(enemies.get(i).getHealth()<100 && defenders.get(i).getMainHandItem().getDamageValue()==0,
                    "Actual melee goal approaches and hits with "+defenders.get(i).getDefense().weapon(defenders.get(i))
                    +" health="+enemies.get(i).getHealth()+" npc="+defenders.get(i).position()+" enemy="+enemies.get(i).position()
                    +" target="+defenders.get(i).getTarget()+" wear="+defenders.get(i).getMainHandItem().getDamageValue()
                    +" navigationDone="+defenders.get(i).getNavigation().isDone());
            var ally=h.spawn(ModContent.AMBIENT_NPC.get(),new BlockPos(12,1,12)); ally.getProfile(); float health=ally.getHealth();
            defenders.getFirst().doHurtTarget(ally);
            h.assertTrue(ally.getHealth()==health,"Melee friendly fire also suppressed"); h.succeed();
        });
    }
    @GameTest(template="arena")
    public static void oldEquippedBowMigratesWithoutLosingReservedRod(GameTestHelper h) {
        floor(h); var npc=h.spawn(ModContent.AMBIENT_NPC.get(),new BlockPos(12,1,12)); npc.setActivitySpot(npc.blockPosition());
        npc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        CompoundTag old=new CompoundTag(); old.putBoolean("Equipped",true);
        old.put("Reserved",new ItemStack(Items.FISHING_ROD).save(npc.registryAccess()));
        npc.getDefense().load(old,npc); npc.getRoutine().prepare(npc,13000); npc.getDefense().tick(npc);
        h.assertTrue(npc.getDefense().armed(npc),"Old bow upgraded to stable assigned loadout");
        npc.getRoutine().prepare(npc,0); npc.getDefense().tick(npc);
        h.assertTrue(npc.getMainHandItem().is(Items.FISHING_ROD),"Migration preserves reserved daytime rod"); h.succeed();
    }
    @GameTest(template="arena")
    public static void savedDaytimeFireIsCleanedEvenWithoutAnActiveRoutine(GameTestHelper h) {
        floor(h); var npc=h.spawn(ModContent.AMBIENT_NPC.get(),new BlockPos(12,1,12)); npc.setActivitySpot(npc.blockPosition());
        npc.getRoutine().prepare(npc,13000); npc.getRoutine().move(npc);
        var saved=npc.getRoutine().save(); var fire=BlockPos.of(saved.getLong("Fire"));
        saved.putBoolean("Night",false); saved.putBoolean("Returning",false); npc.getRoutine().load(saved);
        npc.getRoutine().prepare(npc,1000);
        h.assertTrue(!h.getLevel().getBlockState(fire).is(Blocks.CAMPFIRE) && !npc.getRoutine().save().contains("Fire"),
                "Pending saved cleanup is retried even after routine finished"); h.succeed();
    }
    @GameTest(template="arena")
    public static void campRateIsOneTenthOfPreviousRateWithoutRemovingOtherOpportunities(GameTestHelper h) {
        int allowed=0, previous=0;
        for(int x=-64;x<64;x++) for(int z=-64;z<64;z++) {
            ChunkPos pos=NaturalEncounters.landCandidate(42,x,z);
            boolean formerlyAllowed = net.minecraft.util.RandomSource.create(42L ^ (pos.x * 341873128712L)
                    ^ (pos.z * 132897987541L) ^ 0x43414D5052415445L).nextInt(4)==0;
            if (formerlyAllowed) previous++;
            if(NaturalEncounters.allowsCamp(42,pos)) {
                h.assertTrue(formerlyAllowed,"New camp opportunities are a subset of the previous rate"); allowed++;
            }
            h.assertTrue(NaturalEncounters.isLandCandidate(42,pos),"All fisher opportunity cells remain");
            h.assertTrue(NaturalEncounters.isBoatCandidate(42,NaturalEncounters.candidate(42,x,z)),"Boat spacing unchanged");
        }
        h.assertTrue(allowed>previous*.08 && allowed<previous*.12,
                "About one tenth of the previous camp opportunities; got "+allowed+"/"+previous);
        floor(h);
        for(int x=0;x<24;x++) for(int z=0;z<24;z++) for(int y=6;y<=9;y++) h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        var chunk=h.getLevel().getChunkAt(h.absolutePos(new BlockPos(12,1,12)));
        h.assertTrue(EncounterPlacement.tryPlace(h.getLevel(),chunk,false,false)==null,"Disallowed camp leaves dry clearing empty");
        var placed=EncounterPlacement.tryPlace(h.getLevel(),chunk,false,true);
        h.assertTrue(placed!=null && placed.kind().equals("camp"),"Same terrain still supports the allowed camp"); h.succeed();
    }
    @GameTest(template="arena")
    public static void weaponPopulationHasThreeStableEqualChanceGroups(GameTestHelper h) {
        int[] counts=new int[3]; var random=new java.util.Random(28119);
        for(int i=0;i<900;i++) {
            var npc=ModContent.AMBIENT_NPC.get().create(h.getLevel()); npc.setUUID(new UUID(random.nextLong(),random.nextLong()));
            var selected=npc.getDefense().weapon(npc); counts[selected.ordinal()]++;
            var saved=npc.getDefense().save(npc); var restored=new NpcDefense(); restored.load(saved,npc);
            h.assertTrue(restored.weapon(npc)==selected,"Saved weapon does not reroll");
        }
        for(int count:counts) h.assertTrue(count>=250 && count<=350,"Each group near one third of 900; count="+count); h.succeed();
    }
}
