package com.hoghunter.item;

import com.hoghunter.content.HogBlocks;
import com.hoghunter.content.HogSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/** Uses vanilla's placement pipeline so protection hooks, support checks, and consumption agree. */
public final class BaitedSnareItem extends BlockItem {
    public BaitedSnareItem(Properties properties) { super(HogBlocks.BAITED_SNARE.get(), properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP || player.getCooldowns().isOnCooldown(this))
            return InteractionResult.FAIL;
        InteractionResult result = super.useOn(context);
        if (result.consumesAction() && !context.getLevel().isClientSide()) {
            player.getCooldowns().addCooldown(this, 10);
            context.getLevel().playSound(null, context.getClickedPos(), HogSounds.SNARE_SET.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.hoghunter.baited_snare").withStyle(ChatFormatting.GRAY));
    }
}
