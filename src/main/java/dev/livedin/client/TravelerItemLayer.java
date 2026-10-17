package dev.livedin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import dev.livedin.entity.NpcRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Activity items get deliberate attachments; other held items keep vanilla rendering. */
public final class TravelerItemLayer extends ItemInHandLayer<AmbientNpc, TravelerModel> {
    private final ItemInHandRenderer itemRenderer;

    public TravelerItemLayer(RenderLayerParent<AmbientNpc, TravelerModel> parent, ItemInHandRenderer itemRenderer) {
        super(parent, itemRenderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    protected void renderArmWithItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                    HumanoidArm arm, PoseStack poses, MultiBufferSource buffer, int light) {
        if (arm == HumanoidArm.RIGHT && entity instanceof AmbientNpc npc
                && (npc.getRole() == NpcRole.FISHER || npc.getRole() == NpcRole.BOAT_RIDER)
                && stack.is(Items.FISHING_ROD)) {
            poses.pushPose();
            getParentModel().translateToHand(arm, poses);
            poses.translate(0, 0.625F, 0);
            poses.mulPose(Axis.XP.rotationDegrees(90));
            poses.translate(0, -0.45F, 0);
            poses.mulPose(Axis.ZP.rotationDegrees(-45));
            poses.scale(0.85F, -0.85F, -0.85F);
            var model = Minecraft.getInstance().getModelManager().getModel(FishingSceneRenderer.ROD_MODEL);
            Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poses, buffer,
                    light, OverlayTexture.NO_OVERLAY, model);
            poses.popPose();
            return;
        }
        if (arm != HumanoidArm.LEFT || !stack.is(ModContent.LUTE.get())) {
            super.renderArmWithItem(entity, stack, context, arm, poses, buffer, light);
            return;
        }
        poses.pushPose();
        getParentModel().body.translateAndRotate(poses);
        // Model space has downward Y and forward -Z. The item has upward Y and strings on +Z.
        poses.translate(-0.10F, 0.50F, -0.50F);
        poses.mulPose(Axis.ZP.rotationDegrees(55));
        poses.scale(0.65F, -0.65F, -0.65F);
        itemRenderer.renderItem(entity, stack, ItemDisplayContext.NONE, false, poses, buffer, light);
        poses.popPose();
    }
}
