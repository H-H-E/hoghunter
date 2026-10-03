package com.hoghunter.block;

import com.hoghunter.content.HogBlockEntities;
import com.hoghunter.content.HogEntities;
import com.hoghunter.entity.HogEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A local, player-activated hazard with a persistent delay and a hard population cap. */
public final class HogNestBlockEntity extends BlockEntity {
    private int cooldown = 100;
    public HogNestBlockEntity(BlockPos pos, BlockState state) { super(HogBlockEntities.HOG_NEST.get(), pos, state); }

    public static void tick(Level rawLevel, BlockPos pos, BlockState state, HogNestBlockEntity nest) {
        if (!(rawLevel instanceof ServerLevel level) || level.getGameTime() % 20 != 0 || level.getDifficulty() == Difficulty.PEACEFUL) return;
        boolean active = level.players().stream().anyMatch(player -> player.isAlive() && !player.isCreative()
                && !player.isSpectator() && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 256.0D);
        if (!active) return;
        nest.cooldown = Math.max(0, nest.cooldown - 20);
        nest.setChanged();
        if (nest.cooldown > 0) return;
        nest.cooldown = 600;
        if (level.getEntitiesOfClass(HogEntity.class, new AABB(pos).inflate(12), HogEntity::isAlive).size() >= 4) return;
        var hog = HogEntities.BOAR_HOG.get().create(level);
        if (hog == null) return;
        hog.moveTo(pos.getX() + 0.5D, pos.getY() + 1, pos.getZ() + 0.5D, level.random.nextFloat() * 360, 0);
        if (!level.noCollision(hog) || level.containsAnyLiquid(hog.getBoundingBox())) return;
        hog.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
        hog.getPersistentData().putBoolean("hoghunter_from_nest", true);
        level.addFreshEntity(hog);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("SpawnDelay", cooldown);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cooldown = tag.contains("SpawnDelay") ? Math.max(20, Math.min(600, tag.getInt("SpawnDelay"))) : 100;
    }
}
