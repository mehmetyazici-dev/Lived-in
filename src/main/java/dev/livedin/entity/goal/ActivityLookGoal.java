package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.NpcActivity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Occasional idle looking at the current activity's target, without knowing any role. */
public final class ActivityLookGoal extends Goal {
    private final AmbientNpc npc;
    private NpcActivity running;
    private int remainingTicks;
    public ActivityLookGoal(AmbientNpc npc) { this.npc = npc; setFlags(EnumSet.of(Flag.LOOK)); }
    @Override public boolean canUse() {
        return npc.allowsDayActivity() && npc.getActivity().idleLookTarget() != null && !npc.isPassenger()
                && npc.getNavigation().isDone() && npc.getRandom().nextInt(40) == 0;
    }
    @Override public boolean canContinueToUse() {
        return npc.allowsDayActivity() && running == npc.getActivity() && remainingTicks > 0 && !npc.isPassenger();
    }
    @Override public void start() { running = npc.getActivity(); remainingTicks = 40; }
    @Override public void tick() {
        remainingTicks--;
        var target = npc.getActivity().idleLookTarget();
        if (target != null)
            npc.getLookControl().setLookAt(target.getX() + 0.5, npc.getEyeY() - 0.3, target.getZ() + 0.5, 20, 20);
    }
    @Override public void stop() { running = null; remainingTicks = 0; }
}
