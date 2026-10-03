package com.hoghunter.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Vanilla melee cadence, with roots and species attack windows respected. */
public final class HogAttackGoal extends MeleeAttackGoal {
    private final HogEntity hog;

    public HogAttackGoal(HogEntity hog, double speedModifier) {
        super(hog, speedModifier, true);
        this.hog = hog;
    }

    @Override public boolean canUse() { return !hog.isRooted() && super.canUse(); }
    @Override public boolean canContinueToUse() { return !hog.isRooted() && super.canContinueToUse(); }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (hog.canPerformMelee()) super.checkAndPerformAttack(target);
    }
}
