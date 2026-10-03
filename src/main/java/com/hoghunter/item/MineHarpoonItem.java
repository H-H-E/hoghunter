package com.hoghunter.item;

import com.hoghunter.core.HogAttachments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Hitscan harpoon that pulls the first hog hit by the unobstructed line. */
public final class MineHarpoonItem extends Item {
    public MineHarpoonItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(24.0D));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double limit = block.getType() == BlockHitResult.Type.MISS ? 24.0D : start.distanceTo(block.getLocation());
        AABB search = player.getBoundingBox().expandTowards(direction.scale(limit)).inflate(1.0D);
        LivingEntity hit = null;
        double nearest = limit * limit;
        for (Entity candidate : level.getEntities(player, search, e -> e instanceof LivingEntity living
                && HogItemSupport.isHog(living) && e.isPickable())) {
            var intercept = candidate.getBoundingBox().inflate(0.3D).clip(start, end);
            if (intercept.isPresent()) {
                double distance = start.distanceToSqr(intercept.get());
                if (distance < nearest) { nearest = distance; hit = (LivingEntity) candidate; }
            }
        }
        if (hit != null) {
            Vec3 pull = player.position().subtract(hit.position());
            if (pull.lengthSqr() > 0.01D) {
                Vec3 destination = hit.position().add(pull.normalize().scale(Math.min(6.0D, pull.length())));
                BlockHitResult pullBlock = level.clip(new ClipContext(hit.getEyePosition(), destination,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, hit));
                if (pullBlock.getType() == BlockHitResult.Type.MISS) hit.teleportTo(destination.x, destination.y, destination.z);
                HogWeaponDamage.apply(hit, player, 10.0F, 0.0F);
                HogAttachments.get(player).setNoise(HogAttachments.get(player).noise() + 20);
            } else {
                HogWeaponDamage.apply(hit, player, 16.0F, 0.0F);
            }
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
