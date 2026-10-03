package com.hoghunter.worldgen;

/** Worldgen contract and depth predicates shared by datapack/bootstrap code. */
public final class HogBiomeModifiers {
    public static final int SHALLOW_ORE_MAX_Y = 48;
    public static final int BLACKSTONE_SEAM_MAX_Y = -17;
    public static final int ROOTMOTHER_ARENA_MAX_Y = -49;
    private HogBiomeModifiers() {}
    public static boolean isCorruptedOreBand(int y) { return y <= SHALLOW_ORE_MAX_Y && y >= -64; }
    public static boolean isBlackstoneSeam(int y) { return y <= BLACKSTONE_SEAM_MAX_Y; }
    public static boolean isRootmotherArenaBand(int y) { return y <= ROOTMOTHER_ARENA_MAX_Y; }
}
