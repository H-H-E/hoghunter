package com.hoghunter.entity;

import com.hoghunter.content.HogEntities;
import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.core.HogHunterConfig;
import com.hoghunter.net.HogNetworking;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Server-authoritative combat, counterplay and presentation shared by the hog roster. */
public abstract class HogEntity extends Monster {
    public enum AbilityState { IDLE, WINDUP, ACTIVE, RECOVERY }

    private static final EntityDataAccessor<Integer> DATA_ABILITY_STATE =
            SynchedEntityData.defineId(HogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(HogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_INVISIBLE_PHASE =
            SynchedEntityData.defineId(HogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PLATED_FACING =
            SynchedEntityData.defineId(HogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation HEALTH_DIFFICULTY = ResourceLocation.fromNamespaceAndPath("hoghunter", "health_difficulty");
    private static final ResourceLocation DAMAGE_DIFFICULTY = ResourceLocation.fromNamespaceAndPath("hoghunter", "damage_difficulty");
    private static final ResourceLocation SPEED_DIFFICULTY = ResourceLocation.fromNamespaceAndPath("hoghunter", "speed_difficulty");

    private int suppressedTicks;
    private int rootedTicks;
    private int revealedTicks;
    private int presentationTicks;
    private int lureTicks;
    private Vec3 baitPosition;
    private boolean difficultyApplied;
    private boolean loadedFromSave;

    protected HogEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ABILITY_STATE, AbilityState.IDLE.ordinal());
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_INVISIBLE_PHASE, false);
        builder.define(DATA_PLATED_FACING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BaitGoal(this));
        goalSelector.addGoal(2, new HogAttackGoal(this, getAttackSpeed()));
        // Goal speeds are multipliers; passing the base attribute slowed wandering twice.
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new HogTargeting(this));
    }

    protected double getAttackSpeed() { return 1.0D; }

    public final AbilityState getAbilityState() {
        return AbilityState.values()[entityData.get(DATA_ABILITY_STATE)];
    }

    protected final void setAbilityState(AbilityState state) {
        entityData.set(DATA_ABILITY_STATE, state.ordinal());
        presentationTicks = 0;
    }

    protected final void showAbilityState(AbilityState state, int ticks) {
        setAbilityState(state);
        presentationTicks = ticks;
    }

    public final boolean isCharging() { return entityData.get(DATA_CHARGING); }
    public final void setCharging(boolean charging) { entityData.set(DATA_CHARGING, charging); }
    public final boolean isInvisiblePhase() { return entityData.get(DATA_INVISIBLE_PHASE); }

    protected final void setInvisiblePhase(boolean active) {
        boolean invisible = active && revealedTicks == 0;
        entityData.set(DATA_INVISIBLE_PHASE, invisible);
        setInvisible(invisible);
    }

    public final boolean isPlatedFacing() { return entityData.get(DATA_PLATED_FACING); }
    protected final void setPlatedFacing(boolean active) { entityData.set(DATA_PLATED_FACING, active); }
    public final boolean areAbilitiesSuppressed() { return suppressedTicks > 0; }
    public final boolean isRooted() { return rootedTicks > 0; }
    public final boolean isRevealed() { return revealedTicks > 0; }
    public final boolean isLured() { return lureTicks > 0 && baitPosition != null; }

    /** A live snare refreshes this short lure. Its goal takes precedence over chasing a player. */
    public final void lureTo(Vec3 position, int ticks) {
        if (level().isClientSide() || this instanceof RootmotherEntity || isRooted() || ticks <= 0) return;
        if (!isLured()) cancelAbilities();
        baitPosition = position;
        lureTicks = Math.max(lureTicks, ticks);
        setTarget(null);
    }

    /** Salt interrupts a tell or active special attack without disabling ordinary melee. */
    public final void suppressAbilities(int ticks) {
        if (level().isClientSide() || ticks <= 0) return;
        suppressedTicks = Math.max(suppressedTicks, ticks);
        cancelAbilities();
    }

    /** Pins stop navigation and melee. The boss is deliberately immune. */
    public final void root(int ticks) {
        if (level().isClientSide() || this instanceof RootmotherEntity || ticks <= 0) return;
        rootedTicks = Math.max(rootedTicks, ticks);
        lureTicks = 0;
        baitPosition = null;
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        cancelAbilities();
    }

    /** A lantern keeps a Mire Hog visible for the entire reveal, not only this tick. */
    public final void reveal(int ticks) {
        if (level().isClientSide() || ticks <= 0) return;
        revealedTicks = Math.max(revealedTicks, ticks);
        setInvisiblePhase(false);
    }

    protected void cancelAbilities() {
        setCharging(false);
        setInvisiblePhase(false);
        setAbilityState(AbilityState.IDLE);
    }

    protected boolean canPerformMelee() { return !isRooted(); }

    public static boolean isSurvivalPlayer(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    protected final boolean hasCombatTarget() {
        LivingEntity target = getTarget();
        return target != null && target.isAlive() && target.level() == level()
                && (!(target instanceof Player player) || isSurvivalPlayer(player));
    }

    @Override
    public void tick() {
        // Stop carry-over motion before vanilla physics, then stop new goal motion afterward.
        if (!level().isClientSide() && isRooted()) {
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        }
        super.tick();
        if (level().isClientSide()) return;
        if (!isAlive() || isRemoved()) {
            cancelAbilities();
            return;
        }
        if (!difficultyApplied || tickCount % 20 == 0) applyDifficulty();
        if (suppressedTicks > 0) suppressedTicks--;
        if (revealedTicks > 0) revealedTicks--;
        if (presentationTicks > 0 && --presentationTicks == 0) setAbilityState(AbilityState.IDLE);
        if (getTarget() != null && !hasCombatTarget()) {
            setTarget(null);
            getNavigation().stop();
            cancelAbilities();
        }
        if (isRooted()) {
            rootedTicks--;
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            return;
        }
        if (isLured()) {
            setTarget(null);
            if (--lureTicks == 0) baitPosition = null;
            return;
        }
        serverHogTick();
    }

    protected void serverHogTick() { }

    protected final List<ServerPlayer> nearbyPlayers(double radius) {
        return playersAround(position(), radius);
    }

    protected final List<ServerPlayer> playersAround(Vec3 center, double radius) {
        return level().getEntitiesOfClass(ServerPlayer.class, new AABB(center, center).inflate(radius),
                player -> isSurvivalPlayer(player) && player.distanceToSqr(center) <= radius * radius);
    }

    protected static void addHeartRate(ServerPlayer player, int amount) {
        var data = HogAttachments.get(player);
        data.setHeartRate(data.heartRate() + (int) Math.round(amount * HogHunterConfig.HEARTBEAT_STRESS_MULTIPLIER.get()));
        HogNetworking.sync(player);
    }

    protected static void addNoise(ServerPlayer player, int amount) {
        var data = HogAttachments.get(player);
        data.setNoise(data.noise() + amount);
        HogNetworking.sync(player);
    }

    protected final float abilityDamage(float baseDamage) {
        return (float) (baseDamage * HogHunterConfig.ENEMY_DAMAGE_MULTIPLIER.get());
    }

    protected final boolean isFacingPosition(Vec3 point, double minimumDot) {
        double dx = point.x - getX();
        double dz = point.z - getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) return true;
        double facingX = -Math.sin(Math.toRadians(getYRot()));
        double facingZ = Math.cos(Math.toRadians(getYRot()));
        return (facingX * dx + facingZ * dz) / length >= minimumDot;
    }

    protected final void abilitySound(SoundEvent event, float pitch) {
        playSound(event, (float) (0.9D * HogHunterConfig.HORROR_AUDIO_INTENSITY.get()), pitch);
    }

    @Override protected SoundEvent getAmbientSound() { return HogSounds.HOG_AMBIENT.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return HogSounds.HOG_HURT.get(); }
    @Override protected SoundEvent getDeathSound() { return HogSounds.HOG_DEATH.get(); }
    @Override protected float getSoundVolume() { return HogHunterConfig.HORROR_AUDIO_INTENSITY.get().floatValue(); }

    @Override public float getVoicePitch() {
        float pitch = this instanceof RootmotherEntity ? 0.55F
                : this instanceof IronbackHogEntity ? 0.70F
                : this instanceof ScreecherHogEntity ? 1.25F
                : this instanceof SporeHogEntity ? 0.80F
                : this instanceof MireHogEntity ? 0.85F
                : this instanceof HookHogEntity ? 1.05F : 0.95F;
        return pitch + (random.nextFloat() - 0.5F) * 0.1F;
    }

    @Override
    public void die(DamageSource source) {
        boolean wasAlive = !dead;
        cancelAbilities();
        super.die(source);
        if (!wasAlive || !dead || !(level() instanceof ServerLevel server) || this instanceof RootmotherEntity
                || random.nextFloat() >= 0.30F) return;
        ServerPlayer witness = nearbyPlayers(24.0D).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (witness == null || server.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(24.0D), LivingEntity::isAlive).size() >= 5) return;
        // Death alarms are the Screecher's survival entry point; never spawn inside the corpse.
        HogSpawning.spawnNear(server, HogEntities.SCREECHER_HOG.get(), blockPosition(), witness, 4, 12);
    }

    private void applyDifficulty() {
        float previousHealth = getHealth();
        float previousMaxHealth = getMaxHealth();
        replaceMultiplier(Attributes.MAX_HEALTH, HEALTH_DIFFICULTY, HogHunterConfig.ENEMY_HEALTH_MULTIPLIER.get());
        replaceMultiplier(Attributes.ATTACK_DAMAGE, DAMAGE_DIFFICULTY, HogHunterConfig.ENEMY_DAMAGE_MULTIPLIER.get());
        replaceMultiplier(Attributes.MOVEMENT_SPEED, SPEED_DIFFICULTY, HogHunterConfig.ENEMY_SPEED_MULTIPLIER.get());
        if (!difficultyApplied && !loadedFromSave) {
            setHealth(previousHealth / previousMaxHealth * getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
        difficultyApplied = true;
    }

    private void replaceMultiplier(Holder<Attribute> attribute, ResourceLocation id, double multiplier) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        double amount = multiplier - 1.0D;
        if (current != null && current.amount() == amount) return;
        instance.removeModifier(id);
        if (amount != 0.0D) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HogSuppressedTicks", suppressedTicks);
        tag.putInt("HogRootedTicks", rootedTicks);
        tag.putInt("HogRevealedTicks", revealedTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadedFromSave = true;
        difficultyApplied = false;
        suppressedTicks = Math.max(0, tag.getInt("HogSuppressedTicks"));
        rootedTicks = this instanceof RootmotherEntity ? 0 : Math.max(0, tag.getInt("HogRootedTicks"));
        revealedTicks = Math.max(0, tag.getInt("HogRevealedTicks"));
        lureTicks = 0;
        baitPosition = null;
        // Attacks have transient aiming data. Resume their cooldown, never a half attack.
        cancelAbilities();
    }

    private static final class BaitGoal extends Goal {
        private final HogEntity hog;

        private BaitGoal(HogEntity hog) {
            this.hog = hog;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() { return hog.isLured() && !hog.isRooted(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { followBait(); }
        @Override public void stop() { hog.getNavigation().stop(); }
        @Override public void tick() {
            if (hog.tickCount % 10 == 0) followBait();
        }

        private void followBait() {
            Vec3 bait = hog.baitPosition;
            if (bait == null) return;
            hog.getNavigation().moveTo(bait.x, bait.y, bait.z, 1.1D);
            hog.getLookControl().setLookAt(bait.x, bait.y, bait.z);
        }
    }
}
