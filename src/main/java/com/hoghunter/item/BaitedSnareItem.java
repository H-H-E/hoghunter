package com.hoghunter.item;

import com.hoghunter.core.HogAttachments;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A placed bait marker implemented as a server-side attraction and trigger. */
public final class BaitedSnareItem extends Item {
    public BaitedSnareItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !level.getBlockState(context.getClickedPos()).isSolid()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // No snare block/entity may be registered in this lane, so useOn supplies the
        // same gameplay window directly: nearby hogs are attracted and a close hog is rooted.
        Vec3 bait = context.getClickedPos().relative(context.getClickedFace()).getCenter();
        var hogs = level.getEntitiesOfClass(LivingEntity.class, new AABB(bait, bait).inflate(10.0D),
                entity -> HogItemSupport.isHog(entity) && !HogItemSupport.isRootmother(entity));
        for (LivingEntity hog : hogs) {
            Vec3 toward = bait.subtract(hog.position());
            if (toward.lengthSqr() > 0.01D && hog.distanceToSqr(bait) > 16.0D) {
                hog.setDeltaMovement(toward.normalize().scale(0.22D).add(0, hog.getDeltaMovement().y, 0));
                hog.hurtMarked = true;
            } else {
                hog.setDeltaMovement(Vec3.ZERO);
                hog.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 255, false, false));
                hog.getPersistentData().putLong("hoghunter_snare_until", level.getGameTime() + 100);
                HogAttachments.get(player).setNoise(HogAttachments.get(player).noise() + 40);
                break;
            }
        }
        if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResult.SUCCESS;
    }
}
