package dev.livedin.client;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.registry.ModContent;
import dev.livedin.scene.CampfireMelody;
import dev.livedin.entity.NpcRole;
import dev.livedin.scene.FishingCycle;
import net.minecraft.world.item.Items;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public final class TravelerModel extends PlayerModel<AmbientNpc> {
    public TravelerModel(ModelPart root) { super(root, false); }

    @Override
    public void setupAnim(AmbientNpc npc, float swing, float swingAmount, float age, float yaw, float pitch) {
        super.setupAnim(npc, swing, swingAmount, age, yaw, pitch);
        if (npc.getRole() == NpcRole.FISHER && npc.getMainHandItem().is(Items.FISHING_ROD)) {
            float tick = npc.getFishingTick();
            if (tick >= 0) tick += age - npc.tickCount;
            rightArm.xRot = FishingCycle.armPitch(tick);
            rightArm.yRot = 0;
            rightArm.zRot = 0;
            leftArm.xRot = -0.25F;
            leftArm.yRot = 0;
            leftArm.zRot = 0;
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
        }
        if (npc.getOffhandItem().is(ModContent.LUTE.get())) {
            // Left hand supports the neck; right hand moves over the strings.
            leftArm.xRot = -1.25F;
            leftArm.yRot = 0.25F;
            leftArm.zRot = 0;
            rightArm.xRot = -0.85F;
            rightArm.yRot = -0.35F;
            rightArm.zRot = 0;
            if (npc.isPerforming() && CampfireMelody.noteAt(npc.level().getGameTime()) >= 0) {
                float partialTick = age - npc.tickCount;
                float phase = (npc.level().getGameTime() % CampfireMelody.TICKS_PER_NOTE + partialTick)
                        * Mth.TWO_PI / CampfireMelody.TICKS_PER_NOTE;
                rightArm.xRot += 0.06F * Mth.sin(phase);
                rightArm.zRot += 0.04F * Mth.cos(phase);
            }
            leftSleeve.copyFrom(leftArm);
            rightSleeve.copyFrom(rightArm);
        }
    }
}
