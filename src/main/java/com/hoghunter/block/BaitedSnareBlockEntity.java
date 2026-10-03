package com.hoghunter.block;

import com.hoghunter.content.HogBlockEntities;
import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.entity.HogEntity;
import com.hoghunter.entity.RootmotherEntity;
import com.hoghunter.item.HogWeaponDamage;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Persistent bait with a single activation; no remote hit through walls. */
public final class BaitedSnareBlockEntity extends BlockEntity {
    private UUID owner;
    private int warmup = 20;
    public BaitedSnareBlockEntity(BlockPos pos, BlockState state) { super(HogBlockEntities.BAITED_SNARE.get(), pos, state); }
    public void setOwner(UUID value) { owner = value; setChanged(); }

    public static void tick(Level rawLevel, BlockPos pos, BlockState state, BaitedSnareBlockEntity snare) {
        if (!(rawLevel instanceof ServerLevel level)) return;
        if (snare.warmup > 0) { snare.warmup--; return; }
        if (level.getGameTime() % 10 != 0) return;
        Vec3 bait = Vec3.atBottomCenterOf(pos).add(0, 0.2D, 0);
        var hogs = level.getEntitiesOfClass(HogEntity.class, new AABB(pos).inflate(10),
                hog -> hog.isAlive() && !(hog instanceof RootmotherEntity) && hog.distanceToSqr(bait) <= 100.0D);
        hogs.sort(Comparator.comparingDouble(hog -> hog.distanceToSqr(bait)));
        for (HogEntity hog : hogs) {
            var sight = level.clip(new ClipContext(hog.getEyePosition(), bait, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, hog));
            if (sight.getType() != HitResult.Type.MISS) continue;
            if (hog.distanceToSqr(bait) <= 16.0D) {
                var player = snare.owner == null ? null : level.getPlayerByUUID(snare.owner);
                hog.root(100);
                HogWeaponDamage.apply(hog, player, 12.0F, 0.0F);
                if (player != null) {
                    var data = HogAttachments.get(player);
                    data.setNoise(data.noise() + 40);
                }
                level.playSound(null, pos, HogSounds.SNARE_TRIGGER.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
                level.removeBlock(pos, false);
                return;
            }
            hog.lureTo(bait, 20);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Warmup", warmup);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        warmup = Math.max(0, Math.min(20, tag.getInt("Warmup")));
    }
}
