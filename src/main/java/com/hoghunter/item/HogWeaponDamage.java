package com.hoghunter.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Server-side damage entry point shared by weapons and the entity lane. */
public final class HogWeaponDamage {
    private HogWeaponDamage() {}

    /**
     * Applies weapon damage. The armor-pierce value is carried here so entity
     * code has one stable integration point when a custom damage type is added.
     */
    public static boolean apply(LivingEntity target, Entity attacker, float damage, float armorPierce) {
        if (target.level().isClientSide()) return false;
        DamageSource source = attacker instanceof LivingEntity living
                ? living.damageSources().mobAttack(living)
                : target.damageSources().generic();
        return target.hurt(source, damage);
    }
}
