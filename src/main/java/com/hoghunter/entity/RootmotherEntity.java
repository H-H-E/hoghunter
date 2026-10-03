package com.hoghunter.entity;

import com.hoghunter.content.HogEntities;
import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.net.HogNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class RootmotherEntity extends HogEntity {
    private static final EntityDataAccessor<Boolean> DATA_BLACKOUT_PHASE =
            SynchedEntityData.defineId(RootmotherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int ROOT_CALL_COOLDOWN = 500;
    private static final int SWEEP_COOLDOWN = 140;
    private static final int SWEEP_WINDUP = 20;
    private static final int BLACKOUT_TICKS = 160;
    private static final int REINFORCEMENT_CAP = 8;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private int rootCallCooldown = ROOT_CALL_COOLDOWN;
    private int sweepCooldown = SWEEP_COOLDOWN;
    private int blackoutTicks;
    private float sweepYaw;

    public RootmotherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 50;
        setPersistenceRequired();
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return HogAttributes.create(260, 10, 8, 0.16, 1.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BLACKOUT_PHASE, false);
    }

    public boolean isBlackoutPhase() { return entityData.get(DATA_BLACKOUT_PHASE); }

    @Override public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override public void setCustomName(Component name) {
        super.setCustomName(name);
        if (bossEvent != null) bossEvent.setName(getDisplayName());
    }

    @Override protected boolean canPerformMelee() {
        return super.canPerformMelee() && sweepCooldown > SWEEP_WINDUP;
    }

    @Override protected void serverHogTick() {
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (!areAbilitiesSuppressed() && !isBlackoutPhase() && getHealth() <= getMaxHealth() * 0.5F) {
            entityData.set(DATA_BLACKOUT_PHASE, true);
            blackoutTicks = BLACKOUT_TICKS;
            abilitySound(HogSounds.ROOTMOTHER_BLACKOUT.get(), 0.65F);
        }
        if (blackoutTicks > 0) {
            for (ServerPlayer player : nearbyPlayers(16.0D)) {
                // A temporary light lock cannot consume fuel or overwrite a refill on death/reconnect.
                var data = HogAttachments.get(player);
                if (data.blackoutTicks() < blackoutTicks) {
                    data.blackoutLantern(blackoutTicks);
                    HogNetworking.sync(player);
                }
            }
            blackoutTicks--;
        }
        if (!hasCombatTarget() || areAbilitiesSuppressed()) {
            if (getAbilityState() == AbilityState.WINDUP) {
                sweepCooldown = SWEEP_COOLDOWN;
                setAbilityState(AbilityState.IDLE);
            }
            return;
        }
        if (--rootCallCooldown <= 0 && level() instanceof ServerLevel server) {
            summonReinforcements(server);
            rootCallCooldown = ROOT_CALL_COOLDOWN;
        }
        if (sweepCooldown > 0) sweepCooldown--;
        if (sweepCooldown == SWEEP_WINDUP) {
            sweepYaw = getYRot();
            setAbilityState(AbilityState.WINDUP);
            abilitySound(HogSounds.ROOTMOTHER_WINDUP.get(), 0.7F);
        }
        if (sweepCooldown <= SWEEP_WINDUP) {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            // Commit to the telegraphed arc so circling behind the boss is real counterplay.
            setYRot(sweepYaw);
            setYBodyRot(sweepYaw);
            setYHeadRot(sweepYaw);
        }
        if (sweepCooldown == 0) {
            for (ServerPlayer player : nearbyPlayers(4.0D)) {
                if (isInSweepArc(player) && hasLineOfSight(player)) {
                    player.hurt(damageSources().mobAttack(this), abilityDamage(8.0F));
                }
            }
            sweepCooldown = SWEEP_COOLDOWN;
            showAbilityState(AbilityState.ACTIVE, 10);
            abilitySound(HogSounds.ROOTMOTHER_SWEEP.get(), 0.7F);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.0D, getZ(), 8, 1.5D, 0.3D, 1.5D, 0.0D);
            }
        }
    }

    private void summonReinforcements(ServerLevel server) {
        int count = server.getEntitiesOfClass(HogEntity.class, getBoundingBox().inflate(24.0D),
                hog -> hog != this && hog.isAlive()).size();
        for (int i = 0; i < 3 && count < REINFORCEMENT_CAP; i++) {
            HogEntity hog = i < 2
                    ? HogSpawning.spawnNear(server, HogEntities.BOAR_HOG.get(), blockPosition(), getTarget(), 3, 6)
                    : HogSpawning.spawnNear(server, HogEntities.SPORE_HOG.get(), blockPosition(), getTarget(), 3, 6);
            if (hog != null) count++;
        }
        abilitySound(HogSounds.ROOTMOTHER_SUMMON.get(), 0.65F);
    }

    private boolean isInSweepArc(LivingEntity target) {
        // cos(60 degrees) gives a 120-degree full arc. -0.5 would hit a 240-degree arc.
        return isFacingPosition(target.position(), 0.5D);
    }

    @Override protected void cancelAbilities() {
        if (sweepCooldown <= SWEEP_WINDUP) sweepCooldown = SWEEP_COOLDOWN;
        super.cancelAbilities();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("BlackoutPhase", isBlackoutPhase());
        tag.putInt("BlackoutTicks", blackoutTicks);
        tag.putInt("RootCallCooldown", rootCallCooldown);
        tag.putInt("SweepCooldown", Math.max(SWEEP_WINDUP + 1, sweepCooldown));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPersistenceRequired();
        entityData.set(DATA_BLACKOUT_PHASE, tag.getBoolean("BlackoutPhase"));
        blackoutTicks = Math.max(0, Math.min(BLACKOUT_TICKS, tag.getInt("BlackoutTicks")));
        rootCallCooldown = tag.contains("RootCallCooldown") ? Math.max(1, tag.getInt("RootCallCooldown")) : ROOT_CALL_COOLDOWN;
        sweepCooldown = tag.contains("SweepCooldown") ? Math.max(SWEEP_WINDUP + 1, tag.getInt("SweepCooldown")) : SWEEP_COOLDOWN;
        bossEvent.setName(getDisplayName());
    }
}
