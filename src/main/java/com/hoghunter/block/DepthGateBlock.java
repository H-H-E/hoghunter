package com.hoghunter.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/** Progression gate. The level and persistent player data are checked on the server. */
public final class DepthGateBlock extends Block {
    public static final int REQUIRED_TIER = 1;
    public DepthGateBlock(BlockBehaviour.Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.OPEN); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        int tier = player.getPersistentData().getInt("hoghunter_unlocked_tier");
        if (tier < REQUIRED_TIER) {
            player.displayClientMessage(Component.translatable("message.hoghunter.depth_gate_locked"), true);
            return InteractionResult.FAIL;
        }
        level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, !state.getValue(BlockStateProperties.OPEN)), Block.UPDATE_ALL);
        return InteractionResult.CONSUME;
    }
}
