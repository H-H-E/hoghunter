package com.hoghunter.entity;

import com.hoghunter.core.HogAttachments;
import com.hoghunter.core.HogHunterPlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Server-authoritative common behavior for the corrupted hog roster. */
public abstract class HogEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(HogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_INVISIBLE_PHASE =
            SynchedEntityData.defineId(HogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PLATED_FACING =
            SynchedEntityData.defineId(HogEntity.class, EntityDataSerializers.BOOLEAN);

    protected HogEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_INVISIBLE_PHASE, false);
        builder.define(DATA_PLATED_FACING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new HogAttackGoal(this, getAttackSpeed()));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HogTargeting(this));
    }

    protected double getAttackSpeed() {
        return 1.0D;
    }

    public final boolean isCharging() {
        return entityData.get(DATA_CHARGING);
    }

    public final void setCharging(boolean charging) {
        entityData.set(DATA_CHARGING, charging);
    }

    public final boolean isInvisiblePhase() {
        return entityData.get(DATA_INVISIBLE_PHASE);
    }

    protected final void setInvisiblePhase(boolean active) {
        entityData.set(DATA_INVISIBLE_PHASE, active);
        setInvisible(active);
    }

    public final boolean isPlatedFacing() {
        return entityData.get(DATA_PLATED_FACING);
    }

    protected final void setPlatedFacing(boolean active) {
        entityData.set(DATA_PLATED_FACING, active);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            serverHogTick();
        }
    }

    protected void serverHogTick() {
        LivingEntity target = getTarget();
        setPlatedFacing(false);
        if (target != null && target.isAlive()) {
            // The plates protect the front-facing damage profile while the hog's
            // vulnerable back is turned toward the attacker, per the design.
            setPlatedFacing(!isFacingTarget(target));
        }
    }

    protected final java.util.List<ServerPlayer> nearbyPlayers(double radius) {
        return level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(radius), Player::isAlive);
    }

    protected static void addHeartRate(ServerPlayer player, int amount) {
        HogHunterPlayerData data = HogAttachments.get(player);
        data.setHeartRate(data.heartRate() + amount);
        player.syncData(HogAttachments.PLAYER_DATA);
    }

    protected static void addNoise(ServerPlayer player, int amount) {
        HogHunterPlayerData data = HogAttachments.get(player);
        data.setNoise(data.noise() + amount);
        player.syncData(HogAttachments.PLAYER_DATA);
    }

    protected boolean isFacingTarget(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) {
            return true;
        }
        double facingX = -Math.sin(Math.toRadians(getYRot()));
        double facingZ = Math.cos(Math.toRadians(getYRot()));
        return (facingX * dx + facingZ * dz) / length > 0.35D;
    }

    protected void onAttackWindow(LivingEntity target) {
        // Subclasses use this hook for ability attacks; vanilla melee damage is applied by the goal.
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (isPlatedFacing() && getTarget() != null && source.getEntity() == getTarget()) {
            amount *= 0.4F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putBoolean("HogCharging", isCharging());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        setCharging(tag.getBoolean("HogCharging"));
    }
}
