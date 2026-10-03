package com.hoghunter.core;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

/** Server-side pressure settings. Values are bounded by ModConfigSpec. */
public final class HogHunterConfig {
    private HogHunterConfig() {}
    public static void register(ModContainer container) { container.registerConfig(ModConfig.Type.SERVER, SPEC); }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue ENEMY_HEALTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ENEMY_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ENEMY_SPEED_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SPAWN_DENSITY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue OIL_DRAIN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HEARTBEAT_STRESS_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue SANITY_EFFECTS_ENABLED;
    public static final ModConfigSpec.DoubleValue HORROR_VISUAL_INTENSITY;
    public static final ModConfigSpec.DoubleValue HORROR_AUDIO_INTENSITY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENEMY_HEALTH_MULTIPLIER = b.defineInRange("enemyHealthMultiplier", 1.0, 0.5, 3.0);
        ENEMY_DAMAGE_MULTIPLIER = b.defineInRange("enemyDamageMultiplier", 1.0, 0.5, 2.5);
        ENEMY_SPEED_MULTIPLIER = b.defineInRange("enemySpeedMultiplier", 1.0, 0.75, 1.35);
        SPAWN_DENSITY_MULTIPLIER = b.defineInRange("spawnDensityMultiplier", 1.0, 0.25, 2.0);
        OIL_DRAIN_MULTIPLIER = b.defineInRange("oilDrainMultiplier", 1.0, 0.25, 3.0);
        HEARTBEAT_STRESS_MULTIPLIER = b.defineInRange("heartbeatStressMultiplier", 1.0, 0.0, 2.0);
        SANITY_EFFECTS_ENABLED = b.define("sanityEffectsEnabled", true);
        HORROR_VISUAL_INTENSITY = b.defineInRange("horrorVisualIntensity", 1.0, 0.0, 1.0);
        HORROR_AUDIO_INTENSITY = b.defineInRange("horrorAudioIntensity", 1.0, 0.0, 1.0);
        SPEC = b.build();
    }
}
