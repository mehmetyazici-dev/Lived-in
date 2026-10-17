package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Uses vanilla ground navigation, but rejects paths that leave the scene. */
public final class SceneStrollGoal extends RandomStrollGoal {
    private final AmbientNpc npc;
    private Path path;

    public SceneStrollGoal(AmbientNpc npc, double speed) {
        super(npc, speed, 120, false);
        this.npc = npc;
    }

    @Override
    public boolean canUse() {
        if (!npc.allowsDayActivity() || !npc.getActivity().allowsStrolling()
                || npc.isPassenger() || !npc.isWithinActivityArea(npc.blockPosition()) || !super.canUse()) {
            return false;
        }
        BlockPos destination = BlockPos.containing(wantedX, wantedY, wantedZ);
        if (!npc.isWithinActivityArea(destination) || !npc.level().hasChunkAt(destination)) {
            return false;
        }
        path = npc.getNavigation().createPath(destination, 0);
        if (path == null || !path.canReach()) {
            return false;
        }
        for (int i = 0; i < path.getNodeCount(); i++) {
            if (!npc.isWithinActivityArea(path.getNode(i).asBlockPos())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected Vec3 getPosition() {
        return LandRandomPos.getPos(npc, 4, 2);
    }

    @Override
    public void start() {
        npc.getNavigation().moveTo(path, speedModifier);
    }

    @Override
    public boolean canContinueToUse() {
        return npc.allowsDayActivity() && !npc.isPassenger() && npc.isWithinActivityArea(npc.blockPosition()) && super.canContinueToUse();
    }
}
