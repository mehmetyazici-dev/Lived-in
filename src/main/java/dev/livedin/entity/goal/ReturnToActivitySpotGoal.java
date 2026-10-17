package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

/** Walks back if pushed out of the scene; never teleports or forces chunk loading. */
public final class ReturnToActivitySpotGoal extends Goal {
    private final AmbientNpc npc;
    private final double speed;
    private Path path;
    private long nextAttempt;

    public ReturnToActivitySpotGoal(AmbientNpc npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos spot = npc.getActivitySpot();
        if (!npc.allowsDayActivity() || spot == null || npc.isPassenger() || npc.isWithinActivityArea(npc.blockPosition())
                || npc.level().getGameTime() < nextAttempt || !npc.level().hasChunkAt(spot)) {
            return false;
        }
        nextAttempt = npc.level().getGameTime() + 40;
        path = npc.getNavigation().createPath(spot, 0);
        return path != null && npc.isSafeActivityPath(path);
    }

    @Override
    public void start() {
        npc.getNavigation().moveTo(path, speed);
    }

    @Override
    public boolean canContinueToUse() {
        return npc.allowsDayActivity() && !npc.isPassenger() && !npc.isWithinActivityArea(npc.blockPosition())
                && !npc.getNavigation().isDone();
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
    }
}
