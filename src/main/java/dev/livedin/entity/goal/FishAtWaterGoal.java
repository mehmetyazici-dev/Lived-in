package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.NpcRole;
import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;

/** Keep a fisher on the dry bank, facing the saved water target even while casting is paused. */
public final class FishAtWaterGoal extends Goal {
    private final AmbientNpc npc;
    public FishAtWaterGoal(AmbientNpc npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() {
        return npc.getRole() == NpcRole.FISHER && npc.getFishingWater() != null
                && !npc.isPassenger() && !npc.isInWater() && npc.isWithinActivityArea(npc.blockPosition());
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        var water = npc.getFishingWater();
        npc.getNavigation().stop();
        float yaw = (float)(Mth.atan2(water.getZ() + 0.5 - npc.getZ(), water.getX() + 0.5 - npc.getX())
                * 180 / Math.PI) - 90;
        npc.setYRot(yaw);
        npc.yBodyRot = yaw;
        npc.getLookControl().setLookAt(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 30, 30);
    }
}
