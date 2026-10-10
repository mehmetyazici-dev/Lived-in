package dev.livedin.client;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** Vanilla skins distinguish scene roles for this prototype. */
public final class AmbientNpcRenderer extends HumanoidMobRenderer<AmbientNpc, TravelerModel> {
    private static final ResourceLocation STEVE = skin("steve");
    private static final ResourceLocation MUSICIAN = skin("efe");
    private static final ResourceLocation COMPANION = skin("kai");
    private static final ResourceLocation FISHER = skin("alex");
    private static final ResourceLocation BOAT_RIDER = skin("noor");

    private static ResourceLocation skin(String name) {
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/" + name + ".png");
    }

    public AmbientNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new TravelerModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        // Replace the generic held-item layer: the lute needs a playing attachment.
        layers.removeIf(layer -> layer instanceof ItemInHandLayer<?, ?>);
        addLayer(new TravelerItemLayer(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(AmbientNpc npc) {
        if (npc.getRole() == NpcRole.BOAT_RIDER) return BOAT_RIDER;
        if (npc.getRole() == NpcRole.FISHER) return FISHER;
        if (npc.getRole() == NpcRole.MUSICIAN) return MUSICIAN;
        if (npc.getRole() == NpcRole.CAMP_COMPANION && (npc.getUUID().getLeastSignificantBits() & 1) == 0) return COMPANION;
        return STEVE;
    }

    @Override
    public void render(AmbientNpc npc, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffer, int light) {
        super.render(npc, yaw, partialTick, poses, buffer, light);
        FishingSceneRenderer.render(npc, getModel(), partialTick, poses, buffer, light,
                entityRenderDispatcher.cameraOrientation());
    }
}
