package dev.livedin.entity.goal;

import dev.livedin.entity.AmbientNpc;
import java.util.EnumSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Items;

/** Bow aim or bounded melee pursuit. Never abandon a boat or chase away from a night camp. */
public final class DefendGoal extends Goal {
    private final AmbientNpc npc;
    private int cooldown;
    public DefendGoal(AmbientNpc npc) { this.npc = npc; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    @Override public boolean canUse() { return npc.getTarget() != null && npc.getDefense().armed(npc); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void tick() {
        var target = npc.getTarget(); if (target == null) return;
        npc.getMoveControl().strafe(0, 0); npc.getLookControl().setLookAt(target, 30, 30);
        if (cooldown > 0) cooldown--;
        if (!npc.getMainHandItem().is(Items.BOW)) {
            npc.stopUsingItem();
            if (!npc.isPassenger() && npc.getRoutine().allowsPursuit(target.blockPosition())) {
                if (npc.tickCount % 10 == 0) {
                    var path = npc.getNavigation().createPath(target, 0);
                    if (path != null && npc.getRoutine().safeDefensePath(npc, path)) npc.getNavigation().moveTo(path, 1.15);
                    else npc.getNavigation().stop();
                }
            } else npc.getNavigation().stop();
            if (cooldown == 0 && npc.isWithinMeleeAttackRange(target) && npc.getSensing().hasLineOfSight(target)) {
                npc.swing(InteractionHand.MAIN_HAND); npc.doHurtTarget(target); cooldown = 20;
            }
            return;
        }
        npc.getNavigation().stop();
        if (!npc.getSensing().hasLineOfSight(target) || npc.distanceToSqr(target) > 225) { npc.stopUsingItem(); return; }
        if (cooldown == 0 && !npc.isUsingItem()) npc.startUsingItem(InteractionHand.MAIN_HAND);
        if (npc.isUsingItem() && npc.getTicksUsingItem() >= 20) {
            npc.stopUsingItem(); npc.performRangedAttack(target, 1); cooldown = 40;
        }
    }
    @Override public void stop() { npc.getNavigation().stop(); npc.stopUsingItem(); cooldown = 0; }
}
