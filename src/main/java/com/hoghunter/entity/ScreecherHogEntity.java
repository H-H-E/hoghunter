package com.hoghunter.entity;

import com.hoghunter.content.HogEntities;
import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.net.HogNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class ScreecherHogEntity extends HogEntity {
    private static final int WINDUP_TICKS = 20;
    private static final int COOLDOWN_TICKS = 360;
    private int screechCooldown;
    private int windupTicks;

    public ScreecherHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return HogAttributes.create(18, 0, 2, 0.31);
    }

    @Override protected void serverHogTick() {
        if (screechCooldown > 0) screechCooldown--;
        if (areAbilitiesSuppressed()) return;
        if (!hasCombatTarget() || distanceToSqr(getTarget()) > 100.0D) {
            if (windupTicks > 0) cancelAbilities();
            return;
        }
        if (windupTicks > 0) {
            if (--windupTicks == 0) screech();
            return;
        }
        if (screechCooldown == 0) {
            windupTicks = WINDUP_TICKS;
            setAbilityState(AbilityState.WINDUP);
            abilitySound(HogSounds.SCREECHER_WINDUP.get(), 1.1F);
        }
    }

    private void screech() {
        for (var player : nearbyPlayers(10.0D)) {
            addHeartRate(player, 25);
            var data = HogAttachments.get(player);
            data.lockSprint(40);
            data.setSanity(data.sanity() - 5);
            player.setSprinting(false);
            HogNetworking.sync(player);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 32, 2.5D, 0.4D, 2.5D, 0.1D);
            if (server.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(16.0D), LivingEntity::isAlive).size() < 5) {
                HogSpawning.spawnNear(server, HogEntities.BOAR_HOG.get(), blockPosition(), getTarget(), 2, 5);
            }
        }
        screechCooldown = COOLDOWN_TICKS;
        showAbilityState(AbilityState.ACTIVE, 12);
        abilitySound(HogSounds.SCREECH.get(), 1.1F);
    }

    @Override protected void cancelAbilities() {
        if (windupTicks > 0) screechCooldown = COOLDOWN_TICKS;
        windupTicks = 0;
        super.cancelAbilities();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ScreechCooldown", windupTicks > 0 ? COOLDOWN_TICKS : screechCooldown);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        screechCooldown = Math.max(0, tag.getInt("ScreechCooldown"));
    }
}
