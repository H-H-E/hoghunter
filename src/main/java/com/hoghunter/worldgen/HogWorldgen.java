package com.hoghunter.worldgen;

import net.minecraft.core.BlockPos;

/** Server/data-side worldgen helpers. Actual placement is supplied by generated datapack data. */
public final class HogWorldgen {
    private HogWorldgen() {}
    public static boolean shouldPlaceCorruptedOre(BlockPos pos) { return HogBiomeModifiers.isCorruptedOreBand(pos.getY()); }
    public static boolean shouldPlaceBlackstoneSeam(BlockPos pos) { return HogBiomeModifiers.isBlackstoneSeam(pos.getY()); }
    public static boolean shouldPlaceRootmotherArena(BlockPos pos) { return HogBiomeModifiers.isRootmotherArenaBand(pos.getY()); }
}
