package com.hoghunter.entity;

import com.hoghunter.content.HogSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class HookHogEntity extends HogEntity {
    private static final int WINDUP_TICKS = 12;
    private static final int PULL_TICKS = 10;
    private static final int COOLDOWN_TICKS = 120;
    private int grappleCooldown;
    private int windupTicks;
    private int pullTicks;
    private ServerPlayer hookedPlayer;

    public HookHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return HogAttributes.create(20, 1, 2, 0.25);
    }

    @Override protected void serverHogTick() {
        if (grappleCooldown > 0) grappleCooldown--;
        if (areAbilitiesSuppressed()) return;
        if (pullTicks > 0) {
            if (hookedPlayer == null || !isSurvivalPlayer(hookedPlayer) || hookedPlayer.level() != level()
                    || !hasLineOfSight(hookedPlayer) || distanceToSqr(hookedPlayer) > 169.0D) {
                cancelAbilities();
                return;
            }
            Vec3 offset = position().subtract(hookedPlayer.position());
            if (offset.horizontalDistanceSqr() < 2.25D) {
                finishPull();
                return;
            }
            Vec3 pull = offset.normalize().scale(0.55D);
            hookedPlayer.setDeltaMovement(pull.x, Math.max(-0.3D, Math.min(0.3D, pull.y + 0.10D)), pull.z);
            // A ServerPlayer needs the motion packet; addDeltaMovement alone is not authoritative on its client.
            hookedPlayer.hurtMarked = true;
            if (level() instanceof ServerLevel server) {
                Vec3 start = getEyePosition();
                Vec3 end = hookedPlayer.getEyePosition();
                for (int step = 1; step <= 8; step++) {
                    Vec3 point = start.lerp(end, step / 8.0D);
                    server.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            if (--pullTicks == 0) finishPull();
            return;
        }
        if (!hasCombatTarget() || !(getTarget() instanceof ServerPlayer player)
                || distanceToSqr(player) > 144.0D || !hasLineOfSight(player)) {
            if (windupTicks > 0) cancelAbilities();
            return;
        }
        if (windupTicks > 0) {
            if (--windupTicks == 0) {
                hookedPlayer = player;
                pullTicks = PULL_TICKS;
                grappleCooldown = COOLDOWN_TICKS;
                addNoise(player, 20);
                setAbilityState(AbilityState.ACTIVE);
                abilitySound(HogSounds.HOOK_PULL.get(), 0.9F);
            }
            return;
        }
        if (grappleCooldown == 0 && distanceToSqr(player) > 2.25D) {
            windupTicks = WINDUP_TICKS;
            setAbilityState(AbilityState.WINDUP);
            abilitySound(HogSounds.HOOK_WINDUP.get(), 1.0F);
        }
    }

    private void finishPull() {
        pullTicks = 0;
        hookedPlayer = null;
        showAbilityState(AbilityState.RECOVERY, 10);
    }

    @Override protected void cancelAbilities() {
        if (windupTicks > 0 || pullTicks > 0) grappleCooldown = COOLDOWN_TICKS;
        windupTicks = 0;
        pullTicks = 0;
        hookedPlayer = null;
        super.cancelAbilities();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GrappleCooldown", windupTicks > 0 || pullTicks > 0 ? COOLDOWN_TICKS : grappleCooldown);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        grappleCooldown = Math.max(0, tag.getInt("GrappleCooldown"));
    }
}
