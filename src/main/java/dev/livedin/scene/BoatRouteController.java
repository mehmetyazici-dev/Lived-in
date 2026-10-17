package dev.livedin.scene;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.BoatActivity;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Event bridge only: the assigned passenger's activity owns steering and rowing. */
@EventBusSubscriber(modid = LivedIn.MOD_ID)
public final class BoatRouteController {
    private BoatRouteController() {}
    @SubscribeEvent public static void beforeBoatTick(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Boat boat)) return;
        if (boat.getFirstPassenger() instanceof AmbientNpc npc && npc.getActivity() instanceof BoatActivity activity
                && activity.controls(boat)) {
            if (npc.allowsDayActivity()) activity.beforeBoatTick(npc, boat); else npc.getRoutine().boatTick(npc, boat);
        }
    }
    @SubscribeEvent public static void afterBoatTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Boat boat)) return;
        if (boat.getFirstPassenger() instanceof AmbientNpc npc && npc.getActivity() instanceof BoatActivity activity
                && activity.controls(boat) && npc.allowsDayActivity()) activity.afterBoatTick(npc, boat);
    }
}
