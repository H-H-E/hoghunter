package com.hoghunter.entity;

import com.hoghunter.core.HogAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/** Sight, close scent and noise acquisition, with a bounded search after contact is lost. */
public final class HogTargeting extends NearestAttackableTargetGoal<Player> {
    private final HogEntity hog;
    private int lastContactTick;

    public HogTargeting(HogEntity hog) {
        super(hog, Player.class, 10, false, false,
                entity -> entity instanceof Player player && canSense(hog, player));
        this.hog = hog;
    }

    @Override
    public void start() {
        lastContactTick = hog.tickCount;
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        if (hog.isLured() || !super.canContinueToUse()) return false;
        LivingEntity target = hog.getTarget();
        if (!(target instanceof Player player) || !HogEntity.isSurvivalPlayer(player)) return false;
        if (canSense(hog, player)) lastContactTick = hog.tickCount;
        return hog.tickCount - lastContactTick <= 60;
    }

    private static boolean canSense(HogEntity hog, Player player) {
        if (hog.isLured() || !HogEntity.isSurvivalPlayer(player)) return false;
        double range = hog.getAttributeValue(Attributes.FOLLOW_RANGE);
        double distance = hog.distanceToSqr(player);
        if (distance > range * range) return false;
        if (hog.hasLineOfSight(player)) return true;
        var data = HogAttachments.get(player);
        double scentRange = data.wounds() > 0 ? 12.0D : 6.0D;
        double hearingRange = data.noise() >= 25 ? Math.min(range, 8.0D + data.noise() * 0.24D) : 0.0D;
        return distance <= Math.max(scentRange * scentRange, hearingRange * hearingRange);
    }
}
