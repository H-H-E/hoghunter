package com.hoghunter.item;

import com.hoghunter.core.HogAttachments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Oil-powered lantern activation with a short-lived server-authoritative reveal pulse. */
public final class FieldLanternItem extends Item {
    public FieldLanternItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        var data = HogAttachments.get(player);
        if (data.oil() <= 0 || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        data.setOil(data.oil() - 1);
        // A dynamic light/projectile entity is outside this lane; an aimed pulse provides
        // the throwable interaction, usable light, and spoor reveal without new registration.
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0, false, false));
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(12.0D));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 revealPoint = hit.getType() == BlockHitResult.Type.MISS ? end : hit.getLocation();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(revealPoint, revealPoint).inflate(4.0D), HogItemSupport::isHog)) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.consume(stack);
    }
}
