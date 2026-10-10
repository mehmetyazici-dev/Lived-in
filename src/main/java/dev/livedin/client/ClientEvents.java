package dev.livedin.client;

import dev.livedin.LivedIn;
import dev.livedin.client.sound.TravelerMusic;
import dev.livedin.registry.ModContent;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Only the physical client loads this renderer and sound controller. */
@EventBusSubscriber(modid = LivedIn.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent.AMBIENT_NPC.get(), AmbientNpcRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(FishingSceneRenderer.ROD_MODEL);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        TravelerMusic.tick(Minecraft.getInstance());
    }
}
