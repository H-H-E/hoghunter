package com.hoghunter.block;

import com.hoghunter.core.HogAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A reusable gate controlled by the player's extracted evidence tier. */
public final class DepthGateBlock extends Block {
    public static final int REQUIRED_TIER = 1;
    private static final VoxelShape OPEN_FRAME = Shapes.or(Block.box(0, 0, 0, 2, 16, 16), Block.box(14, 0, 0, 16, 16, 16), Block.box(2, 14, 0, 14, 16, 16));
    public DepthGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.OPEN, false));
    }
    public static int requiredTier(BlockPos pos) { return pos.getY() <= -49 ? 3 : pos.getY() <= -17 ? 2 : REQUIRED_TIER; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.OPEN); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BlockStateProperties.OPEN) ? OPEN_FRAME : Shapes.block();
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BlockStateProperties.OPEN) ? Shapes.empty() : Shapes.block();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!player.getAbilities().mayBuild || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        int required = requiredTier(pos);
        if (!player.getAbilities().instabuild && HogAttachments.get(player).unlockedTier() < required) {
            player.displayClientMessage(Component.translatable("message.hoghunter.depth_gate_locked", required), true);
            return InteractionResult.FAIL;
        }
        boolean open = state.getValue(BlockStateProperties.OPEN);
        if (open && !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), entity -> !entity.isSpectator()).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.hoghunter.depth_gate_occupied"), true);
            return InteractionResult.FAIL;
        }
        level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, !open), Block.UPDATE_ALL);
        level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.7F, 0.8F);
        return InteractionResult.CONSUME;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useWithoutItem(state, level, pos, player, hit);
        return result == InteractionResult.FAIL ? ItemInteractionResult.FAIL
                : level.isClientSide() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }
}
