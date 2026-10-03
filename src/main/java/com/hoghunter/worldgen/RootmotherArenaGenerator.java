package com.hoghunter.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

/** Deterministic server-side fallback arena when no authored structure template is available. */
public final class RootmotherArenaGenerator {
    private RootmotherArenaGenerator() {}
    public static boolean placeArena(WorldGenLevel level, BlockPos origin) {
        if (!HogBiomeModifiers.isRootmotherArenaBand(origin.getY())) return false;
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            int distance = Math.max(Math.abs(x), Math.abs(z));
            if (distance <= 5) level.setBlock(origin.offset(x, 0, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
            if (distance == 6) level.setBlock(origin.offset(x, 0, z), Blocks.BLACKSTONE.defaultBlockState(), 2);
        }
        for (int y = 1; y <= 3; y++) for (int side = -6; side <= 6; side++) {
            level.setBlock(origin.offset(-6, y, side), Blocks.BLACKSTONE.defaultBlockState(), 2);
            level.setBlock(origin.offset(6, y, side), Blocks.BLACKSTONE.defaultBlockState(), 2);
            level.setBlock(origin.offset(side, y, -6), Blocks.BLACKSTONE.defaultBlockState(), 2);
            level.setBlock(origin.offset(side, y, 6), Blocks.BLACKSTONE.defaultBlockState(), 2);
        }
        return true;
    }
}
