package com.hoghunter.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class CorruptedOreBlock extends DropExperienceBlock {
    public CorruptedOreBlock(BlockBehaviour.Properties properties) {
        super(UniformInt.of(1, 3), properties.requiresCorrectToolForDrops());
    }
}
