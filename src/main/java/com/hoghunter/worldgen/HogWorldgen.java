package com.hoghunter.worldgen;

import com.hoghunter.HogHunterMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Runtime feature types; configured/placed features and biome modifiers live in server data. */
public final class HogWorldgen {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, HogHunterMod.MOD_ID);
    public static final DeferredHolder<Feature<?>, HogNestFeature> HOG_NEST = FEATURES.register("hog_nest", HogNestFeature::new);
    private HogWorldgen() {}
    public static void register(IEventBus bus) { FEATURES.register(bus); }
    public static boolean shouldPlaceCorruptedOre(BlockPos pos) { return HogBiomeModifiers.isCorruptedOreBand(pos.getY()); }
    public static boolean shouldPlaceBlackstoneSeam(BlockPos pos) { return HogBiomeModifiers.isBlackstoneSeam(pos.getY()); }
    public static boolean shouldPlaceRootmotherArena(BlockPos pos) { return HogBiomeModifiers.isRootmotherArenaBand(pos.getY()); }
}
