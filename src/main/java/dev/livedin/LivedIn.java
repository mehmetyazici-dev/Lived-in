package dev.livedin;

import com.mojang.logging.LogUtils;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

/** NeoForge constructs this entry point on both the client and dedicated server. */
@Mod(LivedIn.MOD_ID)
public final class LivedIn {
    public static final String MOD_ID = "livedin";
    private static final Logger LOGGER = LogUtils.getLogger();

    public LivedIn(IEventBus modBus) {
        ModContent.register(modBus);
        modBus.addListener(LivedIn::registerAttributes);
        modBus.addListener(LivedIn::addCreativeItems);
        LOGGER.info("Lived-In loaded: ambient travelers are ready.");
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModContent.AMBIENT_NPC.get(), AmbientNpc.createAttributes().build());
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModContent.AMBIENT_NPC_SPAWN_EGG.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModContent.LUTE.get());
        }
    }
}
