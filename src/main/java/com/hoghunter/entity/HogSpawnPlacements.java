package com.hoghunter.entity;

import com.hoghunter.content.HogEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Natural-spawn rules for the non-scripted hog roster. */
public final class HogSpawnPlacements {
    private HogSpawnPlacements() {}

    public static void register(RegisterSpawnPlacementsEvent event) {
        event.register(HogEntities.BOAR_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::boar, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HogEntities.SPORE_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::spore, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HogEntities.HOOK_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::hook, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HogEntities.MIRE_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::mire, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HogEntities.IRONBACK_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::ironback, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static boolean base(EntityType<?> type, ServerLevelAccessor level, BlockPos pos) {
        return level.getDifficulty() != Difficulty.PEACEFUL;
    }

    private static boolean boar(EntityType<BoarHogEntity> type, ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType placement, BlockPos pos, net.minecraft.util.RandomSource random) {
        return base(type, level, pos) && pos.getY() <= 48 && light(level, pos) <= 7
                && hasPlayer(level, pos, 16, player -> com.hoghunter.core.HogAttachments.get(player).noise() >= 25);
    }

    private static boolean spore(EntityType<SporeHogEntity> type, ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType placement, BlockPos pos, net.minecraft.util.RandomSource random) {
        return base(type, level, pos) && pos.getY() <= 16 && light(level, pos) <= 8 && level.getBlockState(pos.above()).isAir();
    }

    private static boolean hook(EntityType<HookHogEntity> type, ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType placement, BlockPos pos, net.minecraft.util.RandomSource random) {
        return base(type, level, pos) && pos.getY() >= -16 && pos.getY() <= 15
                && hasPlayer(level, pos, 14, player -> Math.abs(player.getY() - pos.getY()) <= 8
                && level.clip(new ClipContext(player.getEyePosition(), new Vec3(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS);
    }

    private static boolean ironback(EntityType<IronbackHogEntity> type, ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType placement, BlockPos pos, net.minecraft.util.RandomSource random) {
        return base(type, level, pos) && pos.getY() <= -17 && light(level, pos) <= 5;
    }

    private static boolean mire(EntityType<MireHogEntity> type, ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType placement, BlockPos pos, net.minecraft.util.RandomSource random) {
        return base(type, level, pos) && pos.getY() <= -17
                && (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.MUD))
                && hasPlayer(level, pos, 16, player -> player.isCrouching() || com.hoghunter.core.HogAttachments.get(player).heartRate() >= 120);
    }

    private static int light(ServerLevelAccessor level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos);
    }

    private static boolean hasPlayer(ServerLevelAccessor level, BlockPos pos, double radius, java.util.function.Predicate<Player> predicate) {
        var box = new net.minecraft.world.phys.AABB(pos).inflate(radius);
        return !level.getEntitiesOfClass(Player.class, box, predicate).isEmpty();
    }
}
