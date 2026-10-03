package com.hoghunter.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/** Melee goal that also exposes the attack/charge animation state to clients. */
public final class HogAttackGoal extends MeleeAttackGoal {
    private final HogEntity hog;

    public HogAttackGoal(HogEntity hog, double speedModifier) {
        super(hog, speedModifier, true);
        this.hog = hog;
    }

    @Override
    public void stop() {
        super.stop();
        hog.setCharging(false);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        super.checkAndPerformAttack(target);
        if (hog.isWithinMeleeAttackRange(target)) {
            hog.onAttackWindow(target);
        }
    }
}
