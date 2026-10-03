package com.hoghunter.item;

import com.hoghunter.HogHunterMod;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.entity.RootmotherEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A wall-clipped tether; motion uses vanilla collision instead of teleporting through geometry. */
public final class MineHarpoonItem extends Item {
    public static final TagKey<Block> ANCHORS = TagKey.create(Registries.BLOCK, HogHunterMod.id("harpoon_anchors"));
    public MineHarpoonItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        LivingEntity hit = HogItemSupport.firstTarget(player, 24.0D, HogItemSupport::isHog);
        if (hit != null) {
            HogWeaponDamage.apply(hit, player, 16.0F, 0.0F);
            if (!(hit instanceof RootmotherEntity)) {
                Vec3 pull = player.position().subtract(hit.position());
                if (pull.lengthSqr() > 1.0D) {
                    hit.setDeltaMovement(pull.normalize().scale(Math.min(1.2D, pull.length() / 5.0D)).add(0, 0.15D, 0));
                    hit.hurtMarked = true;
                }
            }
        } else {
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getLookAngle().scale(24.0D));
            var block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (block.getType() == HitResult.Type.MISS || !level.getBlockState(block.getBlockPos()).is(ANCHORS))
                return InteractionResultHolder.fail(stack);
            Vec3 pull = block.getLocation().subtract(player.position());
            if (pull.lengthSqr() <= 1.0D) return InteractionResultHolder.fail(stack);
            player.setDeltaMovement(pull.normalize().scale(Math.min(1.25D, pull.length() / 8.0D)));
            player.hurtMarked = true;
            player.fallDistance = 0;
        }
        var data = HogAttachments.get(player);
        data.setNoise(data.noise() + 20);
        player.getCooldowns().addCooldown(this, 30);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.7F, 0.8F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.hoghunter.mine_harpoon").withStyle(ChatFormatting.GRAY));
    }
}
