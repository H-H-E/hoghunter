package com.hoghunter.item;

import com.hoghunter.core.HogAttachments;
import com.hoghunter.core.HogHunterPlayerEvents;
import com.hoghunter.net.HogNetworking;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Timed medical treatment; completion and consumption are one server-side transaction. */
public final class HogConsumableItem extends Item {
    public enum Treatment { BANDAGE, SPLINT, MEDKIT, RATION }
    private final int useTicks;
    private final Treatment treatment;

    public HogConsumableItem(Properties properties, int useTicks, Treatment treatment) {
        super(properties);
        this.useTicks = useTicks;
        this.treatment = treatment;
    }

    public Treatment treatment() { return treatment; }
    public boolean isMedical() { return treatment != Treatment.RATION; }

    public boolean canTreat(Player player) {
        var data = HogAttachments.get(player);
        if (player.isSprinting()) return false;
        return switch (treatment) {
            case BANDAGE -> data.wounds() > 0 && player.getHealth() >= 8.0F;
            case SPLINT -> data.fracture() > 0 && player.onGround()
                    && player.getDeltaMovement().horizontalDistanceSqr() < 0.0001D;
            case MEDKIT -> data.wounds() > 0 || data.fracture() > 0 || player.getHealth() < player.getMaxHealth();
            case RATION -> player.getFoodData().needsFood() || data.heartRate() > 60;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && !canTreat(player)) {
            player.displayClientMessage(Component.translatable("message.hoghunter.treatment_unavailable"), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return useTicks; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof Player player) || !canTreat(player)) return stack;
        var data = HogAttachments.get(player);
        switch (treatment) {
            case BANDAGE -> data.setWounds(data.wounds() - 1);
            case SPLINT -> {
                data.setFracture(data.fracture() - 1);
                data.recordFractureTreatment();
            }
            case MEDKIT -> {
                data.setWounds(data.wounds() - 1);
                if (data.fracture() > 0) {
                    data.setFracture(data.fracture() - 1);
                    data.recordFractureTreatment();
                }
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
            }
            case RATION -> {
                player.getFoodData().eat(3, 0.6F);
                data.setHeartRate(Math.max(60, data.heartRate() - 5));
            }
        }
        if (player instanceof ServerPlayer serverPlayer)
            HogHunterPlayerEvents.updateInjuryModifiers(serverPlayer, data);
        if (treatment == Treatment.MEDKIT) player.heal(4.0F);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (player instanceof ServerPlayer serverPlayer) HogNetworking.sync(serverPlayer);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.hoghunter.treatment." + treatment.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
    }
}
