package com.hoghunter.entity;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;

/** Attribute builders shared by the registered corrupted hog entity types. */
public final class HogAttributes {
    private static final double FOLLOW_RANGE = 32.0D;

    private HogAttributes() {
    }

    public static AttributeSupplier.Builder create(double health, double armor, double damage, double speed) {
        return createWithKnockback(health, armor, damage, speed, 0.0D);
    }

    private static AttributeSupplier.Builder createWithKnockback(double health, double armor, double damage,
                                                                 double speed, double knockbackResistance) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    public static AttributeSupplier.Builder create(double health, double armor, double damage,
                                                   double speed, double knockbackResistance) {
        return createWithKnockback(health, armor, damage, speed, knockbackResistance);
    }
}
