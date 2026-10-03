package com.hoghunter.entity;

import com.hoghunter.content.HogSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BoarHogEntity extends HogEntity {
    private static final int WINDUP_TICKS = 20;
    private static final int CHARGE_TICKS = 10;
    private static final int COOLDOWN_TICKS = 160;
    private int sightTicks;
    private int chargeCooldown;
    private int chargeTicks;
    private Vec3 chargeDirection = Vec3.ZERO;
    private float chargeYaw;

    public BoarHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return HogAttributes.create(24, 2, 4, 0.27);
    }

    @Override protected boolean canPerformMelee() {
        return super.canPerformMelee() && sightTicks == 0 && chargeTicks == 0;
    }

    @Override protected void serverHogTick() {
        if (chargeCooldown > 0) chargeCooldown--;
        if (areAbilitiesSuppressed()) return;
        LivingEntity target = getTarget();
        if (!hasCombatTarget()) {
            if (chargeTicks > 0 || sightTicks > 0) cancelAbilities();
            return;
        }
        if (chargeTicks > 0) {
            getNavigation().stop();
            setYRot(chargeYaw);
            setYBodyRot(chargeYaw);
            setYHeadRot(chargeYaw);
            setDeltaMovement(chargeDirection.x * 0.8D, getDeltaMovement().y, chargeDirection.z * 0.8D);
            if (hasLineOfSight(target) && getBoundingBox().inflate(0.3D).intersects(target.getBoundingBox())) {
                // One hit contains both normal damage and the bonus; separate hits lose to hurt immunity.
                boolean hit = target.hurt(damageSources().mobAttack(this),
                        (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + abilityDamage(2.0F));
                if (hit) target.knockback(0.6D, getX() - target.getX(), getZ() - target.getZ());
                finishCharge();
            } else if (--chargeTicks == 0 || horizontalCollision) {
                finishCharge();
            }
            return;
        }
        if (chargeCooldown > 0 || !hasLineOfSight(target) || distanceToSqr(target) > 144.0D) {
            if (sightTicks > 0) {
                sightTicks = 0;
                setAbilityState(AbilityState.IDLE);
            }
            return;
        }
        if (sightTicks++ == 0) {
            setAbilityState(AbilityState.WINDUP);
            abilitySound(HogSounds.BOAR_WINDUP.get(), 0.9F);
        }
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        Vec3 offset = target.position().subtract(position());
        float aimYaw = (float) (Mth.atan2(offset.z, offset.x) * 180.0D / Math.PI) - 90.0F;
        setYRot(Mth.approachDegrees(getYRot(), aimYaw, 30.0F));
        setYBodyRot(getYRot());
        if (sightTicks >= WINDUP_TICKS) {
            chargeDirection = new Vec3(offset.x, 0.0D, offset.z).normalize();
            chargeYaw = aimYaw;
            chargeTicks = CHARGE_TICKS;
            sightTicks = 0;
            setCharging(true);
            setAbilityState(AbilityState.ACTIVE);
            abilitySound(HogSounds.BOAR_ATTACK.get(), 0.85F);
        }
    }

    private void finishCharge() {
        chargeTicks = 0;
        sightTicks = 0;
        chargeCooldown = COOLDOWN_TICKS;
        setCharging(false);
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        showAbilityState(AbilityState.RECOVERY, 12);
    }

    @Override protected void cancelAbilities() {
        if (chargeTicks > 0 || sightTicks > 0) chargeCooldown = COOLDOWN_TICKS;
        chargeTicks = 0;
        sightTicks = 0;
        chargeDirection = Vec3.ZERO;
        super.cancelAbilities();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChargeCooldown", chargeTicks > 0 || sightTicks > 0 ? COOLDOWN_TICKS : chargeCooldown);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        chargeCooldown = Math.max(0, tag.getInt("ChargeCooldown"));
    }
}
