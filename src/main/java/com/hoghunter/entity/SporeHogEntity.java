package com.hoghunter.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class SporeHogEntity extends HogEntity {
    private int cloudCooldown;
    private int cloudTicks;
    public SporeHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() { return HogAttributes.create(30, 3, 3, 0.22); }
    @Override protected void serverHogTick() {
        super.serverHogTick();
        if (cloudCooldown > 0) cloudCooldown--;
        if (cloudTicks > 0) {
            cloudTicks--;
            if (tickCount % 40 == 0) {
                for (var player : nearbyPlayers(4.0D)) {
                    if (distanceToSqr(player) <= 16.0D) {
                        addHeartRate(player, 12);
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
                    }
                }
            }
        }
        LivingEntity target = getTarget();
        if (target != null && distanceToSqr(target) <= 16.0D && cloudCooldown == 0) {
            cloudTicks = 120;
            cloudCooldown = 280;
        }
    }
}
