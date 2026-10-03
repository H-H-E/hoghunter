package com.hoghunter.core;

import com.hoghunter.HogHunterMod;
import com.hoghunter.content.HogItems;
import com.hoghunter.entity.HogEntity;
import com.hoghunter.entity.MireHogEntity;
import com.hoghunter.item.HogConsumableItem;
import com.hoghunter.net.HogNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** Server authority for physiology, injuries, lamp fuel, progression persistence and HUD snapshots. */
@EventBusSubscriber(modid = HogHunterModCompat.MOD_ID)
public final class HogHunterPlayerEvents {
    private static final ResourceLocation WOUND_HEALTH = HogHunterMod.id("wound_health");
    private static final ResourceLocation FRACTURE_SPEED = HogHunterMod.id("fracture_speed");
    private static final int LANTERN_VISION_TICKS = 240;
    private static final Map<ServerPlayer, MobEffectInstance> LANTERN_VISION = new WeakHashMap<>();
    private static final Map<ServerPlayer, MovementSample> MOVEMENT = new WeakHashMap<>();

    private HogHunterPlayerEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        MovementSample movement = MOVEMENT.computeIfAbsent(player, p -> new MovementSample(p.position()));
        movement.observe(player.position());
        HogHunterPlayerData data = HogAttachments.get(player);
        data.tickTemporaryState();
        if (player.isSpectator() || player.isCreative()) {
            movement.movedSinceSync = false;
            releaseLanternVision(player);
            if (player.tickCount % 10 == 0) {
                updateInventoryAmmo(player, data);
                HogNetworking.sync(player);
            }
            return;
        }
        if (data.sprintLocked()) player.setSprinting(false);
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof HogConsumableItem treatment
                && treatment.isMedical() && (!treatment.canTreat(player)
                || (player.getUseItem().is(HogItems.SPLINT.get()) && horizontalMovement(player) > 0.0001))) {
            player.stopUsingItem();
        }
        if (isLanternHeld(player)) data.tickLanternOil(HogHunterConfig.OIL_DRAIN_MULTIPLIER.get());
        if (data.beginPanic()) HogNetworking.sync(player);
        updateInjuryModifiers(player, data);
        if (player.tickCount % 10 == 0) tickServerState(player);
        if (player.tickCount % 240 == 0 && data.wounds() > 0 && player.getHealth() > 2.0F
                && !(player.isUsingItem() && player.getUseItem().getItem() instanceof HogConsumableItem)) {
            player.hurt(player.damageSources().magic(), Math.min(1.0F, player.getHealth() - 2.0F));
        }
    }

    /** Called every ten ticks. Timed locks and fuel are updated by the per-tick entry point. */
    public static void tickServerState(ServerPlayer player) {
        HogHunterPlayerData data = HogAttachments.get(player);
        boolean lit = isLanternHeld(player) && data.lanternAvailable();
        updateLanternVision(player, lit);
        if (lit) {
            for (MireHogEntity mire : player.level().getEntitiesOfClass(MireHogEntity.class,
                    player.getBoundingBox().inflate(8), LivingEntity::isAlive)) {
                if (player.hasLineOfSight(mire)) mire.reveal(20);
            }
        }
        int target = calculateTargetBpm(player, data);
        int current = data.heartRate();
        // Two BPM per half-second is the intended four BPM per second.
        data.setHeartRate(current + Integer.signum(target - current) * Math.min(2, Math.abs(target - current)));
        data.setNoise(data.noise() - 2);
        MovementSample motion = MOVEMENT.get(player);
        boolean moved = motion != null && motion.movedSinceSync;
        if (motion != null) motion.movedSinceSync = false;
        if (moved && player.isSprinting()) {
            data.setNoise(data.noise() + (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                    .is(HogItems.IRONBACK_HARNESS.get()) ? 10 : 5));
            if (data.heartRate() >= 140 && player.tickCount % 40 == 0) player.causeFoodExhaustion(4.0F);
        } else if (moved && !player.isCrouching()) {
            data.setNoise(data.noise() + 2);
        }
        if (player.tickCount % 200 == 0) {
            if (player.getY() <= 16 && data.oil() <= 10) data.setSanity(data.sanity() - 1);
            if (player.getY() >= 48 && player.level().canSeeSky(player.blockPosition())
                    && player.level().getMaxLocalRawBrightness(player.blockPosition()) >= 8) {
                data.setSanity(data.sanity() + 2);
            }
        }
        if (data.sanity() == 0) {
            if (HogHunterConfig.SANITY_EFFECTS_ENABLED.get()) data.blackoutLantern(60);
            data.setHeartRate(data.heartRate() + 20);
            data.setSanity(15);
        }
        updateInventoryAmmo(player, data);
        HogNetworking.sync(player);
    }

    public static int calculateTargetBpm(ServerPlayer player, HogHunterPlayerData data) {
        int stress = 0;
        boolean lampAvailable = isLanternHeld(player) && data.lanternAvailable();
        int light = player.level().getMaxLocalRawBrightness(player.blockPosition());
        if (light < 8 && !lampAvailable) stress += 10;
        if (data.oil() == 0 && player.getY() <= 48) stress += 20;
        var hogs = player.level().getEntitiesOfClass(HogEntity.class,
                player.getBoundingBox().inflate(12), LivingEntity::isAlive);
        if (hogs.stream().anyMatch(hog -> hog.hasLineOfSight(player))) stress += 15;
        if (hogs.stream().anyMatch(hog -> hog.distanceToSqr(player) <= 36.0)) stress += 25;
        if (player.isSprinting()) stress += 8;
        stress += data.wounds() * 8;
        if (data.fracture() >= 2) stress += 6;
        if (data.sanity() < 35) stress += 10;
        if (data.sanity() < 15) stress += 15;
        return Math.max(45, Math.min(180, 60 + (int) (stress * HogHunterConfig.HEARTBEAT_STRESS_MULTIPLIER.get())));
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()
                || player.isCreative() || player.isSpectator() || event.getNewDamage() <= 0) return;
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof HogConsumableItem treatment
                && treatment.isMedical()) {
            player.stopUsingItem();
        }
        HogHunterPlayerData data = HogAttachments.get(player);
        if (event.getNewDamage() >= 8.0F) data.setWounds(data.wounds() + 1);
        boolean heavyHogHit = event.getSource().getEntity() instanceof HogEntity && event.getNewDamage() >= 12.0F;
        boolean severeFall = event.getSource().is(DamageTypes.FALL) && player.fallDistance >= 6.0F;
        if (heavyHogHit || severeFall) data.setFracture(data.fracture() + 1);
        updateInjuryModifiers(player, data);
        HogNetworking.sync(player);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        HogHunterPlayerData data = HogAttachments.get(player);
        data.setNoise(data.noise() + 10);
        HogNetworking.sync(player);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer player && HogAttachments.get(player).heartRate() >= 120) {
            event.setNewSpeed(event.getNewSpeed() * 0.9F);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        HogAttachments.get(event.getOriginal()).copyTo(HogAttachments.get(event.getEntity()));
        if (event.isWasDeath()) HogAttachments.get(event.getEntity()).resetAfterDeath();
        if (event.getOriginal() instanceof ServerPlayer oldPlayer) {
            releaseLanternVision(oldPlayer);
            MOVEMENT.remove(oldPlayer);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Migrate the only progression key read by the early prototype without lowering a new save.
            var data = HogAttachments.get(player);
            data.setUnlockedTier(Math.max(data.unlockedTier(), player.getPersistentData().getInt("hoghunter_unlocked_tier")));
            updateInventoryAmmo(player, data);
            updateInjuryModifiers(player, data);
            HogNetworking.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!event.isEndConquered()) HogAttachments.get(player).resetAfterDeath();
            updateInjuryModifiers(player, HogAttachments.get(player));
            updateInventoryAmmo(player, HogAttachments.get(player));
            HogNetworking.sync(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseLanternVision(player);
            MOVEMENT.remove(player);
            HogNetworking.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseLanternVision(player);
            MOVEMENT.remove(player);
        }
    }

    public static boolean isLanternHeld(ServerPlayer player) {
        return player.getMainHandItem().is(HogItems.FIELD_LANTERN.get())
                || player.getOffhandItem().is(HogItems.FIELD_LANTERN.get());
    }

    private static double horizontalMovement(ServerPlayer player) {
        MovementSample motion = MOVEMENT.get(player);
        return motion == null ? 0 : motion.distanceSquared;
    }

    private static final class MovementSample {
        private Vec3 lastPosition;
        private double distanceSquared;
        private boolean movedSinceSync;

        private MovementSample(Vec3 position) { lastPosition = position; }

        private void observe(Vec3 position) {
            // Vanilla resets xo/zo immediately before PlayerTickEvent; they cannot measure
            // received player movement here. Keep our own sample and ignore teleports.
            distanceSquared = position.subtract(lastPosition).horizontalDistanceSqr();
            if (distanceSquared > 0.0025 && distanceSquared <= 16.0) movedSinceSync = true;
            lastPosition = position;
        }
    }

    private static void updateInventoryAmmo(ServerPlayer player, HogHunterPlayerData data) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(HogItems.IRON_BOLT.get())) total += stack.getCount();
        }
        data.setAmmo(total);
    }

    public static void updateInjuryModifiers(ServerPlayer player, HogHunterPlayerData data) {
        updateModifier(player.getAttribute(Attributes.MAX_HEALTH), WOUND_HEALTH,
                -2.0 * data.wounds(), AttributeModifier.Operation.ADD_VALUE);
        updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), FRACTURE_SPEED,
                -0.1 * data.fracture(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount,
                                       AttributeModifier.Operation operation) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && old.amount() == amount) return;
        if (old != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    private static void updateLanternVision(ServerPlayer player, boolean active) {
        if (!active) {
            releaseLanternVision(player);
            return;
        }
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        MobEffectInstance owned = LANTERN_VISION.get(player);
        // Never shorten or remove a potion supplied by vanilla or another mod.
        if (current != null && (current != owned || current.getDuration() > LANTERN_VISION_TICKS)) {
            LANTERN_VISION.remove(player);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, LANTERN_VISION_TICKS, 0, true, false, false));
        LANTERN_VISION.put(player, player.getEffect(MobEffects.NIGHT_VISION));
    }

    private static void releaseLanternVision(ServerPlayer player) {
        MobEffectInstance owned = LANTERN_VISION.remove(player);
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (owned != null && owned == current && current.getDuration() <= LANTERN_VISION_TICKS) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    public static int bpmBand(int bpm) { return bpm >= 180 ? 180 : (bpm / 20) * 20; }
    public static boolean isHog(LivingEntity entity) { return entity instanceof HogEntity; }
}
