package dev.livedin.registry;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, LivedIn.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LivedIn.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, LivedIn.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<dev.livedin.social.ConversationMenu>> CONVERSATION =
            MENUS.register("conversation", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(dev.livedin.social.ConversationMenu::new));
    public static final DeferredHolder<Item, Item> INVITATION = ITEMS.register("invitation", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<EntityType<?>, EntityType<AmbientNpc>> AMBIENT_NPC =
            ENTITIES.register("ambient_npc", () -> EntityType.Builder.of(AmbientNpc::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(8)
                    // The player model sits at its hips; the default mob attachment is at its feet.
                    .vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
                    .build(LivedIn.MOD_ID + ":ambient_npc"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> AMBIENT_NPC_SPAWN_EGG =
            ITEMS.register("ambient_npc_spawn_egg",
                    () -> new DeferredSpawnEggItem(AMBIENT_NPC, 0x806044, 0xD6B879, new Item.Properties()));

    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, LivedIn.MOD_ID);
    public static final DeferredHolder<Item, Item> LUTE =
            ITEMS.register("lute", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> LUTE_NOTE =
            SOUNDS.register("lute_note", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(LivedIn.MOD_ID, "lute_note")));

    private ModContent() {}

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        MENUS.register(modBus);
    }
}
