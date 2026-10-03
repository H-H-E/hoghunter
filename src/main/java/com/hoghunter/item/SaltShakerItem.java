package com.hoghunter.item;

import com.hoghunter.content.HogBlocks;
import com.hoghunter.content.HogSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Lays a short barrier perpendicular to the player; each successful use costs one charge. */
public final class SaltShakerItem extends Item {
    public SaltShakerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        var player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP || player.getCooldowns().isOnCooldown(this))
            return InteractionResult.FAIL;
        BlockPos start = context.getClickedPos().above();
        Direction along = context.getHorizontalDirection().getClockWise();
        int placed = 0;
        for (int i = -1; i <= 1; i++) {
            BlockPos pos = start.relative(along, i);
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, context.getItemInHand())) continue;
            var salt = HogBlocks.SALT_LINE.get().defaultBlockState();
            if (level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty() && salt.canSurvive(level, pos)) {
                if (level.isClientSide() || level.setBlock(pos, salt, Block.UPDATE_ALL)) placed++;
            }
        }
        if (placed == 0) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            player.getCooldowns().addCooldown(this, 15);
            level.playSound(null, start, HogSounds.SALT_PLACE.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.hoghunter.salt_shaker").withStyle(ChatFormatting.GRAY));
    }
}
