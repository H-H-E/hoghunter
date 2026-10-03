package com.hoghunter.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class MireHogEntity extends HogEntity {
    private int vanishCooldown;
    private int vanishTicks;
    public MireHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() { return HogAttributes.create(34, 2, 5, 0.24); }
    @Override protected void serverHogTick() {
        super.serverHogTick();
        if (vanishCooldown > 0) vanishCooldown--;
        if (vanishTicks > 0 && --vanishTicks == 0) setInvisiblePhase(false);
        LivingEntity target = getTarget();
        if (target != null && isInWater() && vanishCooldown == 0) {
            setInvisiblePhase(true);
            getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.38D);
            vanishTicks = 80;
            vanishCooldown = 320;
        }
        if (vanishTicks == 0) getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0.24D);
        if (vanishTicks > 0 && tickCount % 10 == 0) {
            ServerPlayer nearest = nearbyPlayers(8.0D).stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (nearest != null) {
                Vec3 behind = nearest.position().subtract(nearest.getLookAngle().normalize().scale(1.5D));
                teleportTo(behind.x, behind.y, behind.z);
                setInvisiblePhase(false);
                vanishTicks = 0;
            }
        }
    }
}
