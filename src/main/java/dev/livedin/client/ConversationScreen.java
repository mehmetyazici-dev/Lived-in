package dev.livedin.client;

import dev.livedin.social.ConversationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ConversationScreen extends AbstractContainerScreen<ConversationMenu> {
    public ConversationScreen(ConversationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 300; imageHeight = 218;
        inventoryLabelY = -1000;
    }
    @Override protected void init() {
        super.init();
        var invite = addRenderableWidget(Button.builder(Component.literal(menu.description.getBoolean("Issued") ? "Invitation already issued" : "Join my village"), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 12, topPos + 173, 276, 20).build());
        invite.active = !menu.description.getBoolean("Issued");
        addRenderableWidget(Button.builder(Component.literal("Goodbye"), button -> onClose()).bounds(leftPos + 12, topPos + 196, 276, 20).build());
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, 0xFF82735B);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF252C29);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, menu.description.getString("Name"), 12, 10, 0xFFF1DCAB, false);
        String text = "I'm a " + menu.description.getString("Occupation") + ". " + menu.description.getString("Background") + " " + menu.description.getString("Personality");
        int y = 29;
        for (var line : font.split(Component.literal(text), 276)) { g.drawString(font, line, 12, y, 0xFFE6E8DA, false); y += 11; }
        var stats = menu.description.getCompound("Stats");
        String[] lines = {"Health: " + stats.getInt("Health") + " points (player: 20)",
                "Strength: " + stats.getInt("Strength") + " base damage (player: 1)",
                "Walking pace: " + stats.getInt("WalkingPace") + "% (player: 100%)",
                "Marksmanship: " + stats.getInt("Marksmanship") + "/100",
                "Craftsmanship: " + stats.getInt("Craftsmanship") + "/100"};
        y = Math.max(y + 7, 76);
        for (String line : lines) { g.drawString(font, line, 12, y, 0xFFD3DABB, false); y += 12; }
        g.drawString(font, "Trust: 20/20", 12, 146, 0xFF8ED39C, false);
        g.drawString(font, "Keep the invitation safe; it cannot be replaced.", 12, 159, 0xFFD8BE84, false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float partialTick) {
        super.render(g, x, y, partialTick); renderTooltip(g, x, y);
    }
}
