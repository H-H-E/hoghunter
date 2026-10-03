package com.hoghunter.worldgen;

import com.hoghunter.content.HogBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Rare, surface-safe nests placed only in existing dry underground cave space. */
public final class HogNestFeature extends Feature<NoneFeatureConfiguration> {
    public HogNestFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        BlockPos origin = context.origin();
        int top = Math.min(48, level.getMaxBuildHeight() - 4);
        int bottom = Math.max(-48, level.getMinBuildHeight() + 1);
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());
            if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above()) || !level.isEmptyBlock(pos.above(2))
                    || !level.isEmptyBlock(pos.above(3)) || level.canSeeSky(pos)) continue;
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) continue;
            level.setBlock(pos, HogBlocks.HOG_NEST.get().defaultBlockState(), 2);
            return true;
        }
        return false;
    }
}
