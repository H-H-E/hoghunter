package com.hoghunter.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.hoghunter.content.HogEntities;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

public final class RootmotherEntity extends HogEntity {
    private boolean blackoutPhase;
    private int rootCallCooldown = 500;
    private int sweepCooldown = 140;
    private int blackoutTicks;
    private final Map<ServerPlayer, Integer> blackoutOil = new HashMap<>();
    public RootmotherEntity(EntityType<? extends Monster> type, Level level) { super(type, level); this.xpReward = 50; }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() { return HogAttributes.create(260, 10, 8, 0.16); }
    @Override protected void serverHogTick() {
        super.serverHogTick();
        boolean phase = getHealth() <= getMaxHealth() * 0.5F;
        if (phase && !blackoutPhase) blackoutTicks = 160;
        blackoutPhase = phase;
        setCharging(phase && getTarget() != null);
        if (rootCallCooldown-- <= 0 && level() instanceof ServerLevel server) {
            spawnHog(server, HogEntities.BOAR_HOG.get()); spawnHog(server, HogEntities.BOAR_HOG.get()); spawnHog(server, HogEntities.SPORE_HOG.get());
            rootCallCooldown = 500;
        }
        if (sweepCooldown-- <= 0) {
            for (Player player : nearbyPlayers(4.0D)) {
                if (isInSweepArc(player)) player.hurt(damageSources().mobAttack(this), 8.0F);
            }
            sweepCooldown = 140;
        }
        if (blackoutTicks > 0) {
            blackoutTicks--;
            for (ServerPlayer player : nearbyPlayers(16.0D)) {
                blackoutOil.putIfAbsent(player, com.hoghunter.core.HogAttachments.get(player).oil());
                com.hoghunter.core.HogAttachments.get(player).setOil(0);
                player.syncData(com.hoghunter.core.HogAttachments.PLAYER_DATA);
            }
        } else if (!blackoutOil.isEmpty()) {
            for (var entry : blackoutOil.entrySet()) {
                if (entry.getKey().isAlive()) {
                    com.hoghunter.core.HogAttachments.get(entry.getKey()).setOil(entry.getValue());
                    entry.getKey().syncData(com.hoghunter.core.HogAttachments.PLAYER_DATA);
                }
            }
            blackoutOil.clear();
        }
    }

    private void spawnHog(ServerLevel server, net.minecraft.world.entity.EntityType<? extends Monster> type) {
        var hog = type.create(server);
        if (hog != null) { hog.moveTo(getX(), getY(), getZ(), getYRot(), 0); server.addFreshEntity(hog); }
    }

    private boolean isInSweepArc(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(position()).normalize();
        Vec3 facing = getLookAngle().normalize();
        return facing.dot(toTarget) >= -0.5D;
    }
}
