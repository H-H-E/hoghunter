package com.hoghunter.entity;

import com.hoghunter.content.HogEntities;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.core.HogHunterConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

import java.util.function.Predicate;

/** Natural-spawn rules; awareness radius must exceed vanilla's 24-block player exclusion. */
public final class HogSpawnPlacements {
    private static final double TRIGGER_RADIUS = 64.0D;
    private static final TagKey<Block> SPORE_AIR = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("hoghunter", "spore_air"));

    private HogSpawnPlacements() { }

    public static void register(RegisterSpawnPlacementsEvent event) {
        event.register(HogEntities.BOAR_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::boar, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HogEntities.SPORE_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::spore, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HogEntities.HOOK_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::hook, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // ON_GROUND rejects fluid at the spawn block. Mire has explicit water/mud checks below.
        event.register(HogEntities.MIRE_HOG.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::mire, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HogEntities.IRONBACK_HOG.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HogSpawnPlacements::ironback, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static boolean base(EntityType<?> type, ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        double density = HogHunterConfig.SPAWN_DENSITY_MULTIPLIER.get();
        if (density < 1.0D && random.nextDouble() >= density) return false;
        int cap = Math.max(1, (int) Math.round(8.0D * density));
        if (level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(48.0D), e -> e instanceof HogEntity && e.isAlive()).size() >= cap) return false;
        double halfWidth = type.getWidth() / 2.0D;
        AABB bounds = new AABB(pos.getX() + 0.5D - halfWidth, pos.getY(), pos.getZ() + 0.5D - halfWidth,
                pos.getX() + 0.5D + halfWidth, pos.getY() + type.getHeight(), pos.getZ() + 0.5D + halfWidth);
        return level.getWorldBorder().isWithinBounds(bounds) && level.noCollision(bounds);
    }

    private static boolean boar(EntityType<BoarHogEntity> type, ServerLevelAccessor level, MobSpawnType placement, BlockPos pos, RandomSource random) {
        return pos.getY() <= 48 && light(level, pos) <= 7 && base(type, level, pos, random)
                && hasPlayer(level, pos, player -> HogAttachments.get(player).noise() >= 25);
    }

    private static boolean spore(EntityType<SporeHogEntity> type, ServerLevelAccessor level, MobSpawnType placement, BlockPos pos, RandomSource random) {
        return pos.getY() <= 16 && light(level, pos) <= 8 && level.getBlockState(pos).is(SPORE_AIR)
                && base(type, level, pos, random);
    }

    private static boolean hook(EntityType<HookHogEntity> type, ServerLevelAccessor level, MobSpawnType placement, BlockPos pos, RandomSource random) {
        return pos.getY() >= -16 && pos.getY() <= 15 && light(level, pos) <= 8 && base(type, level, pos, random)
                && hasPlayer(level, pos, player -> Math.abs(player.getY() - pos.getY()) <= 8
                && HogSpawning.hasClearPath(player.level(), player.getEyePosition(), Vec3.atCenterOf(pos), player));
    }

    private static boolean ironback(EntityType<IronbackHogEntity> type, ServerLevelAccessor level, MobSpawnType placement, BlockPos pos, RandomSource random) {
        return pos.getY() <= -17 && light(level, pos) <= 5 && base(type, level, pos, random);
    }

    private static boolean mire(EntityType<MireHogEntity> type, ServerLevelAccessor level, MobSpawnType placement, BlockPos pos, RandomSource random) {
        boolean habitat = level.getFluidState(pos).is(FluidTags.WATER)
                || level.getFluidState(pos.below()).is(FluidTags.WATER)
                || level.getBlockState(pos.below()).is(Blocks.MUD);
        return pos.getY() <= -17 && habitat && light(level, pos) <= 8 && base(type, level, pos, random)
                && hasPlayer(level, pos, player -> player.isCrouching() || HogAttachments.get(player).heartRate() >= 120);
    }

    private static int light(ServerLevelAccessor level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos);
    }

    private static boolean hasPlayer(ServerLevelAccessor level, BlockPos pos, Predicate<Player> predicate) {
        Vec3 center = Vec3.atCenterOf(pos);
        return !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(TRIGGER_RADIUS),
                player -> HogEntity.isSurvivalPlayer(player) && player.distanceToSqr(center) <= TRIGGER_RADIUS * TRIGGER_RADIUS
                        && predicate.test(player)).isEmpty();
    }
}
