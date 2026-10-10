package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Holds the musician in place and facing the gathering while playing. */
public final class PerformAtCampfireGoal extends Goal {
    private final AmbientNpc npc;

    public PerformAtCampfireGoal(AmbientNpc npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() { return npc.canPerform(); }

    @Override
    public boolean canContinueToUse() { return npc.canPerform(); }

    @Override
    public void start() { npc.getNavigation().stop(); }

    @Override
    public void tick() {
        var fire = npc.getCampfireSpot();
        if (fire != null) {
            npc.getLookControl().setLookAt(fire.getX() + 0.5, npc.getEyeY() - 0.3, fire.getZ() + 0.5, 30, 20);
        }
    }
}