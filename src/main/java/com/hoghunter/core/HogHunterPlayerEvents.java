package com.hoghunter.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Server-only heartbeat calculation. Other lanes can call calculateTargetBpm directly. */
@EventBusSubscriber(modid = HogHunterModCompat.MOD_ID)
public final class HogHunterPlayerEvents {
    private HogHunterPlayerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        tickServerState(player);
    }

    public static void tickServerState(ServerPlayer player) {
        HogHunterPlayerData data = HogAttachments.get(player);
        int target = calculateTargetBpm(player, data);
        int current = data.heartRate();
        data.setHeartRate(current == target ? current : current + Integer.signum(target - current) * Math.min(4, Math.abs(target - current)));
        data.setNoise(Math.max(0, data.noise() - 1));
        if (player.getY() <= 16 && data.oil() <= 10 && player.tickCount % 200 == 0) data.setSanity(data.sanity() - 1);
        player.syncData(HogAttachments.PLAYER_DATA);
    }

    public static int calculateTargetBpm(ServerPlayer player, HogHunterPlayerData data) {
        int stress = 0;
        int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
        if (light < 8) stress += 10;
        if (data.oil() == 0) stress += 20;
        int nearby = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(12), e -> isHog(e) && e.hasLineOfSight(player)).size();
        if (nearby > 0) stress += 15;
        if (!player.level().getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(6), HogHunterPlayerEvents::isHog).isEmpty()) stress += 25;
        if (player.isSprinting()) stress += 8;
        stress += data.wounds() * 8;
        if (data.fracture() >= 2) stress += 6;
        if (data.sanity() < 35) stress += 10;
        if (data.sanity() < 15) stress += 15;
        return Math.max(45, Math.min(180, 60 + (int) (stress * HogHunterConfig.HEARTBEAT_STRESS_MULTIPLIER.get())));
    }

    public static int bpmBand(int bpm) { return bpm >= 180 ? 180 : (bpm / 20) * 20; }
    public static boolean isHog(LivingEntity entity) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && HogHunterModCompat.MOD_ID.equals(key.getNamespace()) && key.getPath().endsWith("hog");
    }
}
