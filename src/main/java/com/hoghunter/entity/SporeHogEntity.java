package com.hoghunter.entity;

import com.hoghunter.content.HogSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SporeHogEntity extends HogEntity {
    private static final int WINDUP_TICKS = 20;
    private static final int CLOUD_TICKS = 120;
    private static final int COOLDOWN_TICKS = 280;
    private int cloudCooldown;
    private int windupTicks;
    private int cloudTicks;
    private Vec3 cloudCenter = Vec3.ZERO;

    public SporeHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return HogAttributes.create(30, 3, 3, 0.22);
    }

    @Override protected void serverHogTick() {
        if (cloudCooldown > 0) cloudCooldown--;
        if (areAbilitiesSuppressed()) return;
        if (cloudTicks > 0) {
            if (level() instanceof ServerLevel server && cloudTicks % 4 == 0) {
                server.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, cloudCenter.x, cloudCenter.y + 0.8D,
                        cloudCenter.z, 12, 2.0D, 0.65D, 2.0D, 0.01D);
            }
            if (cloudTicks % 40 == 0) {
                for (var player : playersAround(cloudCenter, 4.0D)) {
                    if (HogSpawning.hasClearPath(level(), cloudCenter.add(0.0D, 0.7D, 0.0D), player.getEyePosition(), this)) {
                        addHeartRate(player, 12);
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
                    }
                }
            }
            if (--cloudTicks == 0) showAbilityState(AbilityState.RECOVERY, 10);
            return;
        }
        if (!hasCombatTarget()) {
            if (windupTicks > 0) cancelAbilities();
            return;
        }
        if (windupTicks > 0) {
            if (--windupTicks == 0) {
                cloudCenter = position();
                cloudTicks = CLOUD_TICKS;
                cloudCooldown = COOLDOWN_TICKS;
                setAbilityState(AbilityState.ACTIVE);
                abilitySound(HogSounds.SPORE_RELEASE.get(), 0.9F);
            }
            return;
        }
        if (cloudCooldown == 0 && distanceToSqr(getTarget()) <= 16.0D && hasLineOfSight(getTarget())) {
            windupTicks = WINDUP_TICKS;
            setAbilityState(AbilityState.WINDUP);
            abilitySound(HogSounds.SPORE_WINDUP.get(), 0.95F);
        }
    }

    @Override protected void cancelAbilities() {
        if (cloudTicks > 0 || windupTicks > 0) cloudCooldown = COOLDOWN_TICKS;
        cloudTicks = 0;
        windupTicks = 0;
        super.cancelAbilities();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CloudCooldown", cloudTicks > 0 || windupTicks > 0 ? COOLDOWN_TICKS : cloudCooldown);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cloudCooldown = Math.max(0, tag.getInt("CloudCooldown"));
    }
}
