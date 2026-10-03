package com.hoghunter.item;

import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.net.HogNetworking;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Toggleable held light. The server player loop owns fuel and visibility effects. */
public final class FieldLanternItem extends Item {
    public FieldLanternItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        var data = HogAttachments.get(player);
        if (!data.lanternLit() && data.oil() <= 0) {
            player.displayClientMessage(Component.translatable("message.hoghunter.lantern_empty"), true);
            return InteractionResultHolder.fail(stack);
        }
        data.setLanternLit(!data.lanternLit());
        player.displayClientMessage(Component.translatable(data.lanternLit()
                ? "message.hoghunter.lantern_on" : "message.hoghunter.lantern_off"), true);
        level.playSound(null, player.blockPosition(), HogSounds.LANTERN_TOGGLE.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        player.getCooldowns().addCooldown(this, 10);
        if (player instanceof ServerPlayer serverPlayer) HogNetworking.sync(serverPlayer);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.hoghunter.field_lantern").withStyle(ChatFormatting.GRAY));
    }
}
