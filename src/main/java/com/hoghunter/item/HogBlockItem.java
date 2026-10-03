package com.hoghunter.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Placement remains vanilla; inventory hints explain the server-side interaction. */
public final class HogBlockItem extends BlockItem {
    private final String[] tooltips;

    public HogBlockItem(Block block, Properties properties, String... tooltips) {
        super(block, properties);
        this.tooltips = tooltips.clone();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        for (String tooltip : tooltips) lines.add(Component.translatable(tooltip).withStyle(ChatFormatting.GRAY));
    }
}
