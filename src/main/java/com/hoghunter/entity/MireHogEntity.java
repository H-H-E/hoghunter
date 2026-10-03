package com.hoghunter.entity;

import com.hoghunter.content.HogSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class MireHogEntity extends HogEntity {
    private static final int VANISH_TICKS = 80;
    private static final int COOLDOWN_TICKS = 320;
    private static final ResourceLocation WATER_SPEED = ResourceLocation.fromNamespaceAndPath("hoghunter", "mire_water_speed");
    private int vanishCooldown;
    private int vanishTicks;

    public MireHogEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0F);
    }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return HogAttributes.create(34, 2, 5, 0.24);
    }

    @Override public boolean checkSpawnObstruction(LevelReader level) {
        // Mob's default rejects every liquid even after a valid placement predicate.
        return level.isUnobstructed(this) && level.noCollision(this)
                && BlockPos.betweenClosedStream(getBoundingBox()).allMatch(pos -> {
                    var fluid = level.getFluidState(pos);
                    return fluid.isEmpty() || fluid.is(FluidTags.WATER);
                });
    }

    @Override protected boolean canPerformMelee() {
        return super.canPerformMelee() && vanishTicks == 0;
    }

    @Override protected void serverHogTick() {
        if (vanishCooldown > 0) vanishCooldown--;
        if (areAbilitiesSuppressed()) return;
        if (vanishTicks > 0) {
            if (isRevealed() || !hasCombatTarget()) {
                cancelAbilities();
                return;
            }
            setWaterSpeed(isInWater());
            if (--vanishTicks == 0) reappear();
            return;
        }
        setWaterSpeed(false);
        boolean inHabitat = isInWater() || level().getBlockState(blockPosition().below()).is(Blocks.MUD);
        if (hasCombatTarget() && inHabitat && vanishCooldown == 0 && !isRevealed()) {
            vanishTicks = VANISH_TICKS;
            vanishCooldown = COOLDOWN_TICKS;
            setInvisiblePhase(true);
            setWaterSpeed(isInWater());
            setAbilityState(AbilityState.ACTIVE);
            abilitySound(HogSounds.MIRE_SHIFT.get(), 0.75F);
        }
    }

    private void reappear() {
        ServerPlayer nearest = nearbyPlayers(8.0D).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (nearest != null) {
            Vec3 look = nearest.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0.0D, look.z).normalize();
            if (horizontal.lengthSqr() < 0.01D) horizontal = new Vec3(0.0D, 0.0D, 1.0D);
            Vec3 sideways = new Vec3(-horizontal.z, 0.0D, horizontal.x);
            boolean moved = false;
            for (double distance : new double[]{2.0D, 2.75D, 3.5D}) {
                for (double side : new double[]{0.0D, 1.0D, -1.0D}) {
                    Vec3 desired = nearest.position().subtract(horizontal.scale(distance)).add(sideways.scale(side));
                    Vec3 safe = HogSpawning.findSafePosition(this, desired, 2, true);
                    if (safe == null) continue;
                    teleportTo(safe.x, safe.y, safe.z);
                    getNavigation().stop();
                    setDeltaMovement(Vec3.ZERO);
                    moved = true;
                    break;
                }
                if (moved) break;
            }
        }
        setInvisiblePhase(false);
        setWaterSpeed(false);
        showAbilityState(AbilityState.RECOVERY, 12);
        abilitySound(HogSounds.MIRE_SHIFT.get(), 1.05F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5D, getZ(), 24, 0.6D, 0.3D, 0.6D, 0.1D);
        }
    }

    private void setWaterSpeed(boolean active) {
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (active && speed.getModifier(WATER_SPEED) == null) {
            speed.addTransientModifier(new AttributeModifier(WATER_SPEED, 0.38D / 0.24D - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!active) {
            speed.removeModifier(WATER_SPEED);
        }
    }

    @Override protected void cancelAbilities() {
        if (vanishTicks > 0) vanishCooldown = Math.max(vanishCooldown, COOLDOWN_TICKS);
        vanishTicks = 0;
        setWaterSpeed(false);
        super.cancelAbilities();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VanishCooldown", vanishTicks > 0 ? COOLDOWN_TICKS : vanishCooldown);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        vanishCooldown = Math.max(0, tag.getInt("VanishCooldown"));
    }
}
