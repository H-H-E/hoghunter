package com.hoghunter.entity;

import com.hoghunter.content.HogSounds;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class IronbackHogEntity extends HogEntity {
    private static final float MAX_TURN_DEGREES_PER_TICK = 15.0F;
    private float yawBeforeTick;

    public IronbackHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return HogAttributes.create(48, 8, 6, 0.19, 0.8);
    }

    @Override public void tick() {
        yawBeforeTick = getYRot();
        super.tick();
    }

    @Override protected void serverHogTick() {
        if (hasCombatTarget()) {
            double dx = getTarget().getX() - getX();
            double dz = getTarget().getZ() - getZ();
            float desiredYaw = (float) (Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
            float yaw = Mth.approachDegrees(yawBeforeTick, desiredYaw, MAX_TURN_DEGREES_PER_TICK);
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
            setPlatedFacing(isFacingPosition(getTarget().position(), 0.0D));
        } else {
            setPlatedFacing(false);
        }
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        Vec3 origin = source.getSourcePosition();
        boolean deflected = origin != null && !source.is(DamageTypeTags.BYPASSES_ARMOR)
                && isFacingPosition(origin, 0.0D);
        boolean damaged = super.hurt(source, deflected ? amount * 0.4F : amount);
        if (damaged && deflected && !level().isClientSide()) {
            setPlatedFacing(true);
            showAbilityState(AbilityState.ACTIVE, 6);
            abilitySound(HogSounds.IRONBACK_DEFLECT.get(), 0.9F);
        }
        return damaged;
    }
}
