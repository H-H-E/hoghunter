package com.hoghunter.item;

import com.hoghunter.entity.HogEntity;
import com.hoghunter.entity.RootmotherEntity;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared server-safe predicates and wall-clipped weapon targeting. */
final class HogItemSupport {
    private HogItemSupport() {}
    static boolean isHog(LivingEntity entity) { return entity instanceof HogEntity; }
    static boolean isRootmother(LivingEntity entity) { return entity instanceof RootmotherEntity; }

    static LivingEntity firstTarget(Player player, double range, Predicate<LivingEntity> predicate) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();
        Vec3 end = start.add(direction.scale(range));
        var wall = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        double nearest = start.distanceToSqr(end);
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        LivingEntity hit = null;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != player && entity.isAlive() && entity.isPickable() && !entity.isSpectator() && predicate.test(entity))) {
            var intercept = candidate.getBoundingBox().inflate(0.15D).clip(start, end);
            if (candidate.getBoundingBox().contains(start)) {
                nearest = 0;
                hit = candidate;
            } else if (intercept.isPresent()) {
                double distance = start.distanceToSqr(intercept.get());
                if (distance < nearest) {
                    nearest = distance;
                    hit = candidate;
                }
            }
        }
        return hit;
    }
}
