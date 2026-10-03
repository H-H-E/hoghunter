package com.hoghunter.item;

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

/** Shared hitscan and charging behavior for the two bolt guns. */
public class HogGunItem extends Item {
    private final float damage;
    private final float armorPierce;
    private final double range;
    private final int cooldown;
    private final int maxUse;
    private final boolean charged;

    public HogGunItem(Properties properties, float damage, float armorPierce, double range,
                      int cooldown, int maxUse, boolean charged) {
        super(properties);
        this.damage = damage;
        this.armorPierce = armorPierce;
        this.range = range;
        this.cooldown = cooldown;
        this.maxUse = maxUse;
        this.charged = charged;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (charged || player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        fire(level, player, stack, 0);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return maxUse; }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || level.isClientSide()) return;
        int used = maxUse - timeLeft;
        if (charged && used < maxUse) return;
        fire(level, player, stack, used);
    }

    private void fire(Level level, Player player, ItemStack stack, int chargeTicks) {
        if (level.isClientSide() || player.getCooldowns().isOnCooldown(this)) return;
        boolean chargedShot = charged && chargeTicks >= maxUse;
        boolean steadyAim = !charged && chargeTicks >= maxUse;
        int ammoNeeded = chargedShot ? 2 : 1;
        if (!consumeAmmo(player, ammoNeeded)) return;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(range));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double limit = block.getType() == BlockHitResult.Type.MISS ? range : start.distanceTo(block.getLocation());
        AABB search = player.getBoundingBox().expandTowards(direction.scale(limit)).inflate(1.0D);
        Entity hit = null;
        double nearest = limit * limit;
        for (Entity candidate : level.getEntities(player, search, e -> e instanceof LivingEntity && e.isPickable())) {
            AABB box = candidate.getBoundingBox().inflate(0.3D);
            var intercept = box.clip(start, end);
            if (intercept.isPresent()) {
                double distance = start.distanceToSqr(intercept.get());
                if (distance < nearest) { nearest = distance; hit = candidate; }
            }
        }
        if (hit instanceof LivingEntity living) {
            float shotDamage = steadyAim ? damage * 1.25F : damage;
            HogWeaponDamage.apply(living, player, shotDamage, armorPierce);
        }
        player.getCooldowns().addCooldown(this, cooldown);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
    }

    private boolean consumeAmmo(Player player, int amount) {
        if (player.getAbilities().instabuild) return true;
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(com.hoghunter.content.HogItems.IRON_BOLT.get())) {
                found += player.getInventory().getItem(slot).getCount();
                if (found >= amount) break;
            }
        }
        if (found < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack ammo = player.getInventory().getItem(slot);
            if (!ammo.is(com.hoghunter.content.HogItems.IRON_BOLT.get())) continue;
            int used = Math.min(remaining, ammo.getCount());
            ammo.shrink(used);
            remaining -= used;
        }
        return true;
    }
}
