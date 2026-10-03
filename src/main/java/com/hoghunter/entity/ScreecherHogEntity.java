package com.hoghunter.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.server.level.ServerLevel;
import com.hoghunter.content.HogEntities;
import net.minecraft.world.level.Level;

public final class ScreecherHogEntity extends HogEntity {
    private int screechCooldown;
    private int sprintLockTicks;
    public ScreecherHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() { return HogAttributes.create(18, 0, 2, 0.31); }
    @Override protected void serverHogTick() {
        super.serverHogTick();
        if (screechCooldown > 0) screechCooldown--;
        if (sprintLockTicks > 0) {
            sprintLockTicks--;
            for (var player : nearbyPlayers(10.0D)) player.setSprinting(false);
        }
        if (screechCooldown == 0 && getTarget() != null && distanceToSqr(getTarget()) <= 100.0D) {
            for (var player : nearbyPlayers(10.0D)) addHeartRate(player, 25);
            if (level() instanceof ServerLevel server && server.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(16.0D)).size() < 5) {
                var boar = HogEntities.BOAR_HOG.get().create(server);
                if (boar != null) { boar.moveTo(getX(), getY(), getZ(), getYRot(), 0); server.addFreshEntity(boar); }
            }
            sprintLockTicks = 40;
            screechCooldown = 360;
        }
    }
}
