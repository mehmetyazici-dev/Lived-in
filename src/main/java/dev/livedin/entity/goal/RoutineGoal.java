package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

public final class RoutineGoal extends Goal {
    private final AmbientNpc npc;
    public RoutineGoal(AmbientNpc npc) { this.npc = npc; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    @Override public boolean canUse() { return npc.getRoutine().active() && npc.getTarget() == null; }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() { npc.getRoutine().move(npc); }
    @Override public void stop() { npc.getNavigation().stop(); }
}
