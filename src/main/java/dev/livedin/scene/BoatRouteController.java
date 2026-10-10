package dev.livedin.scene;

import dev.livedin.LivedIn;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Supplies horizontal steering before vanilla boat physics, then synchronizes the oars. */
@EventBusSubscriber(modid = LivedIn.MOD_ID)
public final class BoatRouteController {
    public static final double SPEED = 0.075;
    private BoatRouteController() {}

    private static AmbientNpc rider(Boat boat) {
        return boat.getFirstPassenger() instanceof AmbientNpc npc && npc.getRole() == NpcRole.BOAT_RIDER
                && boat.getUUID().equals(npc.getSceneBoatId()) ? npc : null;
    }

    @SubscribeEvent
    public static void beforeBoatTick(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Boat boat)) return;
        AmbientNpc npc = rider(boat);
        if (npc == null) return;
        npc.setBoatRowing(false);
        npc.getNavigation().stop();
        Vec3 velocity = Vec3.ZERO;
        BlockPos start = npc.getBoatStart(), end = npc.getBoatEnd();
        if (start != null && end != null && BoatScene.validEndpoints(start, end)) {
            Vec3 at = boat.position(), a = Vec3.atCenterOf(start), b = Vec3.atCenterOf(end);
            boolean inBounds = at.x >= Math.min(a.x, b.x) - 0.35 && at.x <= Math.max(a.x, b.x) + 0.35
                    && at.z >= Math.min(a.z, b.z) - 0.35 && at.z <= Math.max(a.z, b.z) + 0.35
                    && at.y >= start.getY() + 0.2 && at.y <= start.getY() + 1.2;
            if (inBounds && npc.getBoatPauseTicks() == 0) {
                Vec3 target = npc.isBoatHeadingToEnd() ? b : a;
                Vec3 offset = new Vec3(target.x - at.x, 0, target.z - at.z);
                if (offset.lengthSqr() <= 0.04) {
                    npc.reverseBoatRoute();
                    npc.setBoatPauseTicks(30);
                } else {
                    Vec3 step = offset.normalize().scale(Math.min(SPEED, offset.length()));
                    BlockPos current = BlockPos.containing(at.x, start.getY(), at.z);
                    BlockPos next = BlockPos.containing(at.x + step.x, start.getY(), at.z + step.z);
                    if (BoatScene.safeWater(boat.level(), current) && BoatScene.safeWater(boat.level(), next)
                            && boat.level().noCollision(boat, boat.getBoundingBox().expandTowards(step).inflate(0.1, 0, 0.1)
                                    .expandTowards(0, 1.8, 0))) {
                        float desiredYaw = (float)(Mth.atan2(-offset.x, offset.z) * Mth.RAD_TO_DEG);
                        boat.setYRot(Mth.rotLerp(0.18F, boat.getYRot(), desiredYaw));
                        // Vanilla still-water boat physics multiplies horizontal speed by 0.9.
                        velocity = step.scale(1 / 0.9);
                        npc.setBoatRowing(true);
                    }
                }
            }
        }
        if (npc.getBoatPauseTicks() > 0) npc.setBoatPauseTicks(npc.getBoatPauseTicks() - 1);
        boat.setDeltaMovement(velocity.x, boat.getDeltaMovement().y, velocity.z);
    }

    @SubscribeEvent
    public static void afterBoatTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Boat boat)) return;
        AmbientNpc npc = rider(boat);
        if (npc != null) {
            // Vanilla clears oars for non-player passengers; restore our server-owned rowing state.
            boat.setPaddleState(npc.isBoatRowing(), npc.isBoatRowing());
            npc.setYRot(boat.getYRot());
            npc.setYHeadRot(boat.getYRot());
            npc.yBodyRot = boat.getYRot();
        }
    }
}
