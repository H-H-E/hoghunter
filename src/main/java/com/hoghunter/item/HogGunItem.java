package com.hoghunter.item;

import com.hoghunter.content.HogItems;
import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.entity.HogEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Inventory-fed hitscan guns; an interrupted charge and a completed charge fire once each. */
public final class HogGunItem extends Item {
    private final float damage;
    private final float armorPierce;
    private final double range;
    private final int cooldown;
    private final int chargeTime;
    private final boolean silver;

    public HogGunItem(Properties properties, float damage, float armorPierce, double range,
                      int cooldown, int chargeTime, boolean silver) {
        super(properties);
        this.damage = damage;
        this.armorPierce = armorPierce;
        this.range = range;
        this.cooldown = cooldown;
        this.chargeTime = chargeTime;
        this.silver = silver;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide() && !player.getAbilities().instabuild && countAmmo(player) < 1) {
            player.displayClientMessage(Component.translatable("message.hoghunter.no_ammo"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (silver || player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        fire(level, player, stack, hand, 0);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return chargeTime; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        int used = chargeTime - timeLeft;
        if (used <= 0) return;
        fire(level, player, stack, player.getUsedItemHand(), used);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) fire(level, player, stack, player.getUsedItemHand(), chargeTime);
        return stack;
    }

    private void fire(Level level, Player player, ItemStack stack, InteractionHand hand, int chargeTicks) {
        if (level.isClientSide() || player.getCooldowns().isOnCooldown(this)) return;
        boolean chargedShot = silver && chargeTicks >= chargeTime;
        boolean steadyAim = !silver && chargeTicks >= chargeTime;
        if (!consumeAmmo(player, chargedShot ? 2 : 1)) {
            player.displayClientMessage(Component.translatable("message.hoghunter.no_ammo"), true);
            return;
        }
        LivingEntity hit = HogItemSupport.firstTarget(player, range, entity -> true);
        if (hit != null && HogWeaponDamage.apply(hit, player, steadyAim ? damage * 1.25F : damage, armorPierce)
                && chargedShot && hit instanceof HogEntity hog) hog.root(40);
        var data = HogAttachments.get(player);
        data.setNoise(data.noise() + 10);
        data.setAmmo(countAmmo(player));
        level.playSound(null, player.blockPosition(), HogSounds.BOLT_FIRE.get(), SoundSource.PLAYERS,
                0.8F, silver ? 0.85F : 1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, cooldown);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
    }

    public static int countAmmo(Player player) {
        int amount = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(HogItems.IRON_BOLT.get())) amount += stack.getCount();
        }
        return amount;
    }

    private static boolean consumeAmmo(Player player, int amount) {
        if (player.getAbilities().instabuild) return true;
        if (countAmmo(player) < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack ammo = player.getInventory().getItem(slot);
            if (!ammo.is(HogItems.IRON_BOLT.get())) continue;
            int used = Math.min(remaining, ammo.getCount());
            ammo.shrink(used);
            remaining -= used;
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(silver ? "tooltip.hoghunter.silver_bolt_gun" : "tooltip.hoghunter.bolt_gun")
                .withStyle(ChatFormatting.GRAY));
    }
}
