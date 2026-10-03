package com.hoghunter.worldgen;

import com.hoghunter.entity.RootmotherEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/** Locates a prepared arena beside an altar without replacing terrain or player builds. */
public final class RootmotherArenaGenerator {
    private RootmotherArenaGenerator() {}

    public static boolean positionInPreparedArena(ServerLevel level, BlockPos altar, RootmotherEntity boss) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos center = altar.relative(direction, 4);
            if (!level.hasChunksAt(center.offset(-2, -1, -2), center.offset(2, 3, 2))) continue;
            boss.moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D, 0, 0);
            AABB box = boss.getBoundingBox();
            if (!level.getWorldBorder().isWithinBounds(box) || !level.noCollision(boss) || level.containsAnyLiquid(box)) continue;
            boolean floor = true;
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos support = center.offset(x, -1, z);
                if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) floor = false;
            }
            if (floor) return true;
        }
        return false;
    }
}
