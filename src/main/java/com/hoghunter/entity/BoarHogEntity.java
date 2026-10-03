package com.hoghunter.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class BoarHogEntity extends HogEntity {
    private int sightTicks;
    private int chargeCooldown;
    private int chargeTicks;
    private boolean chargeHit;

    public BoarHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() { return HogAttributes.create(24, 2, 4, 0.27); }

    @Override protected void serverHogTick() {
        super.serverHogTick();
        if (chargeCooldown > 0) chargeCooldown--;
        LivingEntity target = getTarget();
        if (target != null && hasLineOfSight(target)) sightTicks++; else sightTicks = 0;
        if (chargeTicks > 0) {
            chargeTicks--;
            setCharging(true);
            if (target != null && target.isAlive()) {
                var direction = target.position().subtract(position()).normalize();
                setDeltaMovement(direction.x * 0.8D, getDeltaMovement().y, direction.z * 0.8D);
                if (!chargeHit && distanceToSqr(target) <= 3.0D) {
                    target.hurt(damageSources().mobAttack(this), 2.0F);
                    target.knockback(0.6D, getX() - target.getX(), getZ() - target.getZ());
                    chargeHit = true;
                    chargeTicks = 0;
                    chargeCooldown = 160;
                }
            }
            if (chargeTicks == 0 && !chargeHit) chargeCooldown = 160;
        }
        boolean charging = chargeTicks > 0 || (sightTicks >= 20 && chargeCooldown == 0);
        if (sightTicks >= 20 && chargeCooldown == 0 && chargeTicks == 0) {
            chargeTicks = 10;
            chargeHit = false;
        }
        setCharging(charging);
    }
}
