package com.hoghunter.item;

import com.hoghunter.content.HogBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Places a three-block, timed salt line using the already registered salt_line block. */
public final class SaltShakerItem extends Item {
    public SaltShakerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Direction.Axis axis = context.getHorizontalDirection().getAxis();
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos start = context.getClickedPos().relative(context.getClickedFace());
        int placed = 0;
        for (int i = -1; i <= 1; i++) {
            BlockPos pos = start.relative(along, i);
            if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.below()).isSolid()) {
                level.setBlock(pos, HogBlocks.SALT_LINE.get().defaultBlockState(), Block.UPDATE_ALL);
                level.scheduleTick(pos, HogBlocks.SALT_LINE.get(), 240);
                placed++;
            }
        }
        if (placed > 0 && !context.getPlayer().getAbilities().instabuild) context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
        return placed > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
