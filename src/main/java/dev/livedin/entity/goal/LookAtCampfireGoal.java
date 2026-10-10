package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

public final class LookAtCampfireGoal extends Goal {
    private final AmbientNpc npc;
    private int remainingTicks;

    public LookAtCampfireGoal(AmbientNpc npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return npc.getCampfireSpot() != null && !npc.isPassenger()
                && npc.getNavigation().isDone() && npc.getRandom().nextInt(40) == 0;
    }

    @Override
    public boolean canContinueToUse() { return remainingTicks > 0 && !npc.isPassenger(); }

    @Override
    public void start() { remainingTicks = 40; }

    @Override
    public void tick() {
        remainingTicks--;
        var fire = npc.getCampfireSpot();
        if (fire != null) {
            npc.getLookControl().setLookAt(fire.getX() + 0.5, npc.getEyeY() - 0.3, fire.getZ() + 0.5, 20, 20);
        }
    }
}