package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import dev.livedin.entity.activity.NpcActivity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Minecraft goal lifecycle for whichever activity currently holds position and looking. */
public final class ActivityGoal extends Goal {
    private final AmbientNpc npc;
    private NpcActivity running;
    public ActivityGoal(AmbientNpc npc) { this.npc = npc; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    @Override public boolean canUse() { return npc.allowsDayActivity() && npc.getActivity().canHoldPosition(npc); }
    @Override public boolean canContinueToUse() {
        return npc.allowsDayActivity() && running == npc.getActivity() && running.canHoldPosition(npc);
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { running = npc.getActivity(); npc.getNavigation().stop(); }
    @Override public void tick() { if (running == npc.getActivity()) running.holdPosition(npc); }
    @Override public void stop() { running = null; }
}
