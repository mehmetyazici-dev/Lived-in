package dev.livedin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.FishingCast;
import dev.livedin.scene.FishingCycle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A temporary visual tied to its NPC, with no separate entity to save, respawn, or clean up. */
public final class FishingSceneRenderer {
    public static final net.minecraft.client.resources.model.ModelResourceLocation ROD_MODEL =
            net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                    ResourceLocation.withDefaultNamespace("item/fishing_rod_cast"));
    private static final RenderType BOBBER = RenderType.entityCutout(
            ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png"));
    private FishingSceneRenderer() {}

    public static void render(AmbientNpc npc, TravelerModel model, float partial, PoseStack poses,
                              MultiBufferSource buffer, int light, Quaternionf camera) {
        int serverTick = FishingCast.cycleTick(npc);
        var water = FishingCast.water(npc);
        if (!npc.isAlive() || npc.isRemoved() || water == null || !FishingCycle.hasBobber(serverTick)) return;
        float tick = serverTick + partial;
        // The same arm transform and rod endpoint used by TravelerItemLayer.
        PoseStack arm = new PoseStack();
        model.rightArm.translateAndRotate(arm);
        Vector3f local = arm.last().pose().transformPosition(new Vector3f(0, 0.625F, -0.938F));
        float yaw = Mth.rotLerp(partial, npc.yBodyRotO, npc.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 tip = new Vec3(local.x * Mth.cos(yaw) + local.z * Mth.sin(yaw),
                1.501 - local.y, local.x * Mth.sin(yaw) - local.z * Mth.cos(yaw));
        Vec3 target = Vec3.atCenterOf(water).add(0, 0.43, 0).subtract(npc.getPosition(partial));
        float extension = FishingCycle.extension(tick);
        Vec3 bobber = tip.lerp(target, extension);
        if (serverTick < FishingCycle.WAIT_START || serverTick >= FishingCycle.RETRIEVE_START)
            bobber = bobber.add(0, Mth.sin(extension * Mth.PI) * 0.8, 0);
        else bobber = bobber.add(0, Mth.sin((npc.tickCount + partial) * 0.15F) * 0.025, 0);

        VertexConsumer line = buffer.getBuffer(RenderType.lineStrip());
        for (int i = 0; i <= 24; i++) {
            float t = i / 24F, next = (i + 1) / 24F;
            Vec3 point = tip.lerp(bobber, t).add(0, -0.18 * Mth.sin(t * Mth.PI) * extension, 0);
            Vec3 after = tip.lerp(bobber, next).add(0, -0.18 * Mth.sin(next * Mth.PI) * extension, 0);
            Vec3 normal = after.subtract(point).normalize();
            line.addVertex(poses.last(), (float)point.x, (float)point.y, (float)point.z)
                    .setColor(0xFF292321).setNormal(poses.last(), (float)normal.x, (float)normal.y, (float)normal.z);
        }
        poses.pushPose();
        poses.translate(bobber.x, bobber.y + 0.10, bobber.z);
        poses.mulPose(camera);
        var quad = buffer.getBuffer(BOBBER);
        vertex(quad, poses.last(), light, -0.125F, -0.125F, 0, 1);
        vertex(quad, poses.last(), light, 0.125F, -0.125F, 1, 1);
        vertex(quad, poses.last(), light, 0.125F, 0.125F, 1, 0);
        vertex(quad, poses.last(), light, -0.125F, 0.125F, 0, 0);
        poses.popPose();
    }
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, int light, float x, float y, float u, float v) {
        out.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, 0, 1, 0);
    }
}
