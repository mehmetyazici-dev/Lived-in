package dev.livedin.social;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

/** Each player earns their own twenty points. Food and progress are awarded by the same server action. */
public final class NpcRelationships {
    public static final TagKey<Item> VALUABLES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(LivedIn.MOD_ID, "barter_valuables"));
    public static final TagKey<Item> FOODS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(LivedIn.MOD_ID, "barter_foods"));
    private final Map<UUID, Integer> progress = new HashMap<>();
    public int progress(UUID player) { return progress.getOrDefault(player, 0); }
    public boolean offer(AmbientNpc npc, ServerPlayer player, InteractionHand hand) {
        ItemStack offered = player.getItemInHand(hand);
        if (!npc.isAlive() || player.isSpectator() || npc.distanceToSqr(player) > 16 || !npc.getProfile().eligibility().barter()
                || offered.isEmpty() || offered.getItem() instanceof BlockItem || !offered.is(VALUABLES)) return false;
        var foods = npc.registryAccess().registryOrThrow(Registries.ITEM).getTag(FOODS);
        if (foods.isEmpty()) return false;
        var choices = foods.get().stream().filter(item -> item.value().components().has(DataComponents.FOOD)).toList();
        if (choices.isEmpty()) return false;
        ItemStack reward = new ItemStack(choices.get(npc.getRandom().nextInt(choices.size())));
        ItemEntity drop = new ItemEntity(npc.level(), npc.getX(), npc.getY() + .7, npc.getZ(), reward);
        drop.setTarget(player.getUUID()); drop.setDefaultPickUpDelay();
        if (!npc.level().addFreshEntity(drop)) return false;
        offered.shrink(1); progress.put(player.getUUID(), Math.min(20, progress(player.getUUID()) + 1));
        player.getInventory().setChanged(); sync(npc, player);
        npc.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, .7F, 1);
        return true;
    }
    public void sync(AmbientNpc npc, ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new ProgressPayload(npc.getId(), npc.getUUID(), progress(player.getUUID())));
    }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); ListTag list = new ListTag();
        progress.forEach((id, count) -> { CompoundTag t = new CompoundTag(); t.putUUID("Player", id); t.putInt("Count", count); list.add(t); });
        tag.put("Players", list); return tag;
    }
    public void load(CompoundTag tag) {
        progress.clear();
        for (Tag entry : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)entry;
            if (t.hasUUID("Player")) progress.put(t.getUUID("Player"), Math.clamp(t.getInt("Count"), 0, 20));
        }
    }
}
