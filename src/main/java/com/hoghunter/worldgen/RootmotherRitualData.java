package com.hoghunter.worldgen;

import com.hoghunter.HogHunterMod;
import com.hoghunter.entity.RootmotherEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Persists the active encounter; unloaded bosses keep their reservation. */
@EventBusSubscriber(modid = HogHunterMod.MOD_ID)
public final class RootmotherRitualData extends SavedData {
    private UUID activeBoss;
    private BlockPos lastKnownPos = BlockPos.ZERO;
    private ResourceKey<Level> activeDimension = Level.OVERWORLD;
    private long missingSince = -1;

    public static RootmotherRitualData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(RootmotherRitualData::new, RootmotherRitualData::load), "hoghunter_rootmother");
    }
    public BlockPos lastKnownPos() { return lastKnownPos; }
    public void begin(UUID boss, BlockPos pos) {
        activeBoss = boss;
        lastKnownPos = pos.immutable();
        activeDimension = Level.OVERWORLD;
        missingSince = -1;
        setDirty();
    }
    public boolean hasActiveBoss(ServerLevel level) {
        if (activeBoss == null) return false;
        for (ServerLevel dimension : level.getServer().getAllLevels()) {
            Entity active = dimension.getEntity(activeBoss);
            if (active != null && !active.isRemoved()) {
                lastKnownPos = active.blockPosition();
                activeDimension = dimension.dimension();
                missingSince = -1;
                setDirty();
                return true;
            }
        }
        // Recover a stale reservation only after its entity-ticking chunk has been observed
        // empty for five seconds. Merely unloading the chunk is never evidence of death.
        ServerLevel bossLevel = level.getServer().getLevel(activeDimension);
        if (bossLevel != null && bossLevel.isPositionEntityTicking(lastKnownPos)) {
            if (missingSince < 0) missingSince = bossLevel.getGameTime();
            if (bossLevel.getGameTime() - missingSince >= 100) {
                activeBoss = null;
                missingSince = -1;
                setDirty();
                return false;
            }
        } else missingSince = -1;
        return true;
    }
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof RootmotherEntity boss) || !(event.getLevel() instanceof ServerLevel level)) return;
        RootmotherRitualData data = get(level);
        if (!boss.getUUID().equals(data.activeBoss)) return;
        data.lastKnownPos = boss.blockPosition();
        data.activeDimension = level.dimension();
        data.missingSince = -1;
        data.setDirty();
    }
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof RootmotherEntity boss) || !(event.getLevel() instanceof ServerLevel level)) return;
        RootmotherRitualData data = get(level);
        if (!boss.getUUID().equals(data.activeBoss)) return;
        data.lastKnownPos = boss.blockPosition();
        data.activeDimension = level.dimension();
        Entity.RemovalReason reason = boss.getRemovalReason();
        if (reason != null && reason.shouldDestroy()) data.activeBoss = null;
        data.missingSince = -1;
        data.setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        if (activeBoss != null) tag.putUUID("ActiveBoss", activeBoss);
        tag.putLong("LastKnownPos", lastKnownPos.asLong());
        tag.putString("Dimension", activeDimension.location().toString());
        return tag;
    }
    private static RootmotherRitualData load(CompoundTag tag, HolderLookup.Provider provider) {
        var data = new RootmotherRitualData();
        data.activeBoss = tag.hasUUID("ActiveBoss") ? tag.getUUID("ActiveBoss") : null;
        data.lastKnownPos = BlockPos.of(tag.getLong("LastKnownPos"));
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimension != null) data.activeDimension = ResourceKey.create(Registries.DIMENSION, dimension);
        return data;
    }
}
