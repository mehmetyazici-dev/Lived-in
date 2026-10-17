package dev.livedin.entity.life;

import dev.livedin.entity.AmbientNpc;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;

/** Temporary defensive loadout; daytime equipment is reserved rather than recreated. */
@net.neoforged.fml.common.EventBusSubscriber(modid = dev.livedin.LivedIn.MOD_ID)
public final class NpcDefense {
    public enum Weapon { SWORD, AXE, BOW }
    private ItemStack reserved = ItemStack.EMPTY;
    private boolean equipped;
    private Weapon weapon;
    private UUID villageOwner;
    /** Stable, equally likely groups; independent of the daytime activity and equipment. */
    public Weapon weapon(AmbientNpc npc) {
        if (weapon == null) weapon = Weapon.values()[Math.floorMod(npc.getUUID().hashCode(), Weapon.values().length)];
        return weapon;
    }
    private net.minecraft.world.item.Item weaponItem(AmbientNpc npc) {
        return switch (weapon(npc)) { case SWORD -> Items.IRON_SWORD; case AXE -> Items.IRON_AXE; case BOW -> Items.BOW; };
    }
    public boolean armed(AmbientNpc npc) { return equipped && npc.getMainHandItem().is(weaponItem(npc)); }
    private ItemStack createWeapon(AmbientNpc npc) {
        ItemStack stack = new ItemStack(weaponItem(npc));
        var registry = npc.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        switch (weapon(npc)) {
            case SWORD -> {
                stack.enchant(registry.getHolderOrThrow(Enchantments.SHARPNESS), 3);
                stack.enchant(registry.getHolderOrThrow(Enchantments.KNOCKBACK), 1);
            }
            case AXE -> stack.enchant(registry.getHolderOrThrow(Enchantments.SHARPNESS), 4);
            case BOW -> {
                stack.enchant(registry.getHolderOrThrow(Enchantments.POWER), 3);
                stack.enchant(registry.getHolderOrThrow(Enchantments.PUNCH), 1);
            }
        }
        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        return stack;
    }
    @net.neoforged.bus.api.SubscribeEvent public static void friendlyFire(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof AmbientNpc
                && (event.getEntity() instanceof AmbientNpc || event.getEntity() instanceof net.minecraft.world.entity.animal.Cat)) event.setCanceled(true);
    }
    public void setVillageOwner(UUID owner) { villageOwner = owner; }
    public boolean protects(Player player) { return player.getUUID().equals(villageOwner); }
    public boolean valid(AmbientNpc npc, LivingEntity target) {
        return target != null && target.isAlive() && !(target instanceof AmbientNpc)
                && !(target instanceof net.minecraft.world.entity.animal.Cat)
                && (!(target instanceof Player p) || (!p.isCreative() && !p.isSpectator() && !protects(p)))
                && npc.distanceToSqr(target) <= 24 * 24;
    }
    public void attacked(AmbientNpc npc, LivingEntity attacker) {
        if (npc.getProfile().eligibility().defense() && valid(npc, attacker)) npc.setTarget(attacker);
        // Nearby companions defend their group and its musician.
        for (AmbientNpc ally : npc.level().getEntitiesOfClass(AmbientNpc.class, npc.getBoundingBox().inflate(12)))
            if (ally != npc && ally.getProfile().eligibility().defense() && ally.getDefense().valid(ally, attacker)) ally.setTarget(attacker);
    }
    public void tick(AmbientNpc npc) {
        if (npc.getTarget() != null && !valid(npc, npc.getTarget())) npc.setTarget(null);
        if (npc.getProfile().eligibility().defense() && npc.getTarget() == null && npc.tickCount % 20 == 0) {
            var threats = npc.level().getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(12),
                    e -> e instanceof Enemy && valid(npc, e) && npc.getSensing().hasLineOfSight(e));
            if (!threats.isEmpty()) npc.setTarget(threats.stream().min(java.util.Comparator.comparingDouble(npc::distanceToSqr)).orElseThrow());
        }
        boolean needed = npc.getProfile().eligibility().defense()
                && (npc.getTarget() != null || npc.getRoutine().needsWeapon(npc)
                    || npc.getRole() == dev.livedin.entity.NpcRole.CAMP_COMPANION);
        if (needed && !equipped) {
            reserved = npc.getMainHandItem().copy(); equipped = true;
            npc.setItemSlot(EquipmentSlot.MAINHAND, createWeapon(npc)); npc.setDropChance(EquipmentSlot.MAINHAND, 0);
        } else if (needed && !armed(npc)) {
            // Migrate an already equipped old bow without overwriting the reserved fishing rod.
            npc.stopUsingItem(); npc.setItemSlot(EquipmentSlot.MAINHAND, createWeapon(npc));
            npc.setDropChance(EquipmentSlot.MAINHAND, 0);
        } else if (!needed && equipped) {
            npc.stopUsingItem(); npc.setItemSlot(EquipmentSlot.MAINHAND, reserved); reserved = ItemStack.EMPTY; equipped = false;
        }
    }
    public CompoundTag save(AmbientNpc npc) {
        CompoundTag tag = new CompoundTag(); tag.putBoolean("Equipped", equipped);
        tag.putString("Weapon", weapon(npc).name());
        if (!reserved.isEmpty()) tag.put("Reserved", reserved.save(npc.registryAccess()));
        if (villageOwner != null) tag.putUUID("VillageOwner", villageOwner);
        return tag;
    }
    public void load(CompoundTag tag, AmbientNpc npc) {
        try { weapon = Weapon.valueOf(tag.getString("Weapon")); }
        catch (IllegalArgumentException ignored) { weapon = null; }
        equipped = tag.getBoolean("Equipped"); reserved = ItemStack.parseOptional(npc.registryAccess(), tag.getCompound("Reserved"));
        villageOwner = tag.hasUUID("VillageOwner") ? tag.getUUID("VillageOwner") : null;
    }
}
