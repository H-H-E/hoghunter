package com.hoghunter.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bounded, loaded-chunk placement shared by summoned hogs and Mire teleportation. */
public final class HogSpawning {
    private HogSpawning() { }

    public static <T extends HogEntity> T spawnNear(ServerLevel server, EntityType<T> type,
                                                    BlockPos center, LivingEntity target,
                                                    int minimumRadius, int maximumRadius) {
        if (server.getDifficulty() == Difficulty.PEACEFUL) return null;
        T hog = type.create(server);
        if (hog == null) return null;
        for (int attempt = 0; attempt < 32; attempt++) {
            double angle = server.random.nextDouble() * Math.PI * 2.0D;
            double radius = minimumRadius + server.random.nextDouble() * (maximumRadius - minimumRadius);
            Vec3 desired = Vec3.atBottomCenterOf(center).add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            Vec3 safe = findSafePosition(hog, desired, 2, false);
            if (safe == null) continue;
            hog.moveTo(safe.x, safe.y, safe.z, server.random.nextFloat() * 360.0F, 0.0F);
            hog.finalizeSpawn(server, server.getCurrentDifficultyAt(hog.blockPosition()), MobSpawnType.REINFORCEMENT, null);
            if (target != null && target.isAlive() && target.level() == server) hog.setTarget(target);
            return server.addFreshEntity(hog) ? hog : null;
        }
        return null;
    }

    public static Vec3 findSafePosition(Entity entity, Vec3 desired, int verticalRange, boolean allowWater) {
        Level level = entity.level();
        BlockPos origin = BlockPos.containing(desired);
        for (int step = 0; step <= verticalRange * 2; step++) {
            int dy = step == 0 ? 0 : (step + 1) / 2 * (step % 2 == 1 ? 1 : -1);
            BlockPos feet = origin.offset(0, dy, 0);
            if (feet.getY() <= level.getMinBuildHeight() || feet.getY() + entity.getBbHeight() >= level.getMaxBuildHeight()
                    || !level.hasChunkAt(feet)) continue;
            Vec3 candidate = new Vec3(desired.x, feet.getY(), desired.z);
            AABB bounds = entity.getBoundingBox().move(candidate.subtract(entity.position()));
            if (!level.getWorldBorder().isWithinBounds(bounds) || !level.noCollision(entity, bounds)) continue;
            boolean water = level.getFluidState(feet).is(FluidTags.WATER);
            if ((!allowWater || !water) && !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) continue;
            if (!allowWater && level.containsAnyLiquid(bounds)) continue;
            boolean lava = BlockPos.betweenClosedStream(bounds).anyMatch(pos -> level.getFluidState(pos).is(FluidTags.LAVA));
            if (lava || !level.getEntities(entity, bounds, other -> other.isAlive() && !other.isSpectator()).isEmpty()) continue;
            return candidate;
        }
        return null;
    }

    public static boolean hasClearPath(Level level, Vec3 from, Vec3 to, Entity context) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context))
                .getType() == HitResult.Type.MISS;
    }
}
