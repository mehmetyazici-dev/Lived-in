package dev.livedin.social;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = LivedIn.MOD_ID)
public final class SocialEvents {
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ProgressPayload.TYPE, ProgressPayload.STREAM_CODEC, (data, context) -> {
            context.enqueueWork(() -> {
                var entity = context.player().level().getEntity(data.entityId());
                if (entity instanceof AmbientNpc npc && npc.getUUID().equals(data.npc())) npc.setViewerProgress(data.count());
            });
        });
    }
    @SubscribeEvent public static void track(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof AmbientNpc npc && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            npc.getRelationships().sync(npc, player);
    }
}
