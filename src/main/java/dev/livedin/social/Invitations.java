package dev.livedin.social;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.saveddata.SavedData;

/** One issuance per person, shared across players and dimensions. Destroyed items are never reissued. */
public final class Invitations extends SavedData {
    public record Claim(UUID token, UUID owner, boolean alive) { }
    private final Map<UUID, Claim> claims = new HashMap<>();
    public static Invitations get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(Invitations::new, Invitations::load), "livedin_invitations");
    }
    public boolean issued(UUID npc) { return claims.containsKey(npc); }
    public Claim claim(UUID npc) { return claims.get(npc); }
    public boolean valid(ItemStack item, UUID player) {
        if (!item.is(ModContent.INVITATION.get())) return false;
        CompoundTag data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.hasUUID("Npc") || !data.hasUUID("Token") || !data.hasUUID("Owner")) return false;
        Claim claim = claims.get(data.getUUID("Npc"));
        return claim != null && claim.alive && claim.token.equals(data.getUUID("Token"))
                && claim.owner.equals(player) && claim.owner.equals(data.getUUID("Owner"));
    }
    public boolean issue(AmbientNpc npc, ServerPlayer player) {
        if (!npc.isAlive() || !npc.getProfile().eligibility().recruitment() || npc.getRelationships().progress(player.getUUID()) < 20
                || npc.distanceToSqr(player) > 16 || player.isSpectator()) return false;
        if (issued(npc.getUUID())) { player.displayClientMessage(Component.literal("This traveler has already issued their one invitation."), false); return false; }
        int slot = player.getInventory().getFreeSlot();
        if (slot < 0) { player.displayClientMessage(Component.literal("Make room in your inventory before asking for an invitation."), false); return false; }
        Claim claim = new Claim(UUID.randomUUID(), player.getUUID(), true);
        ItemStack item = new ItemStack(ModContent.INVITATION.get());
        CompoundTag data = new CompoundTag(); data.putUUID("Npc", npc.getUUID()); data.putUUID("Token", claim.token); data.putUUID("Owner", claim.owner);
        data.putString("NpcName", npc.getName().getString());
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        item.set(DataComponents.CUSTOM_NAME, Component.literal(npc.getName().getString() + "'s invite"));
        item.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(npc.getRole().id()));
        claims.put(npc.getUUID(), claim); setDirty();
        player.getInventory().setItem(slot, item); player.getInventory().setChanged();
        player.displayClientMessage(Component.literal("I'll join when you have a home ready. Keep my invitation safe."), false);
        return true;
    }
    public void died(UUID npc) {
        Claim claim = claims.get(npc);
        if (claim != null) { claims.put(npc, new Claim(claim.token, claim.owner, false)); setDirty(); }
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        claims.forEach((npc, claim) -> {
            CompoundTag t = new CompoundTag(); t.putUUID("Npc", npc); t.putUUID("Token", claim.token); t.putUUID("Owner", claim.owner);
            t.putBoolean("Alive", claim.alive); list.add(t);
        });
        tag.put("Claims", list); return tag;
    }
    public static Invitations load(CompoundTag tag, HolderLookup.Provider registries) {
        Invitations data = new Invitations();
        for (Tag v : tag.getList("Claims", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)v; data.claims.put(t.getUUID("Npc"), new Claim(t.getUUID("Token"), t.getUUID("Owner"), t.getBoolean("Alive")));
        }
        return data;
    }
}
