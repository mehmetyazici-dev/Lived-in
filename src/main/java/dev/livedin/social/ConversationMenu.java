package dev.livedin.social;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Server validates invite buttons; the client receives only this person's introduction/stat snapshot. */
public final class ConversationMenu extends AbstractContainerMenu {
    private final AmbientNpc npc;
    public final CompoundTag description;
    public ConversationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        super(ModContent.CONVERSATION.get(), id);
        npc = null; description = buf.readNbt();
    }
    private ConversationMenu(int id, AmbientNpc npc, CompoundTag description) {
        super(ModContent.CONVERSATION.get(), id); this.npc = npc; this.description = description;
    }
    public static ConversationMenu forNpc(int id, AmbientNpc npc, ServerPlayer player) {
        CompoundTag data = npc.getProfile().save(); data.putString("Name", npc.getName().getString());
        data.putBoolean("Issued", Invitations.get(player.serverLevel()).issued(npc.getUUID()));
        data.putString("Occupation", switch (npc.getRole()) { case FISHER -> "shoreline fisher"; case BOAT_RIDER -> "boat fisher"; default -> "traveler"; });
        return new ConversationMenu(id, npc, data);
    }
    public static void open(AmbientNpc npc, ServerPlayer player) {
        if (!npc.getProfile().eligibility().recruitment() || npc.getRelationships().progress(player.getUUID()) < 20 || !npc.isAlive()) return;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> forNpc(id, npc, player), Component.literal(npc.getName().getString())),
                buf -> buf.writeNbt(forNpc(0, npc, player).description));
    }
    @Override public boolean stillValid(Player player) {
        return npc == null ? player.level().isClientSide : npc.isAlive() && npc.level() == player.level() && npc.distanceToSqr(player) <= 16
                && npc.getProfile().eligibility().recruitment() && npc.getRelationships().progress(player.getUUID()) >= 20;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || npc == null || !(player instanceof ServerPlayer server) || !stillValid(player)) return false;
        if (Invitations.get(server.serverLevel()).issue(npc, server)) { server.closeContainer(); return true; }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
