package com.hoghunter.entity;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/** Shared player targeting rule for every hostile hog. */
public final class HogTargeting extends NearestAttackableTargetGoal<Player> {
    public HogTargeting(HogEntity hog) {
        super(hog, Player.class, 10, true, false,
                entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator());
    }
}
