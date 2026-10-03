package com.hoghunter.block;

import com.hoghunter.core.HogHunterModCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hoghunter.core.HogHunterPlayerEvents;
import com.hoghunter.content.HogBlockEntities;

public final class HogNestBlockEntity extends BlockEntity {
    private int cooldown = 100;
    public HogNestBlockEntity(BlockPos pos, BlockState state) { super(HogBlockEntities.HOG_NEST.get(), pos, state); }
    public static void tick(net.minecraft.world.level.Level rawLevel, BlockPos pos, BlockState state, HogNestBlockEntity nest) {
        if (!(rawLevel instanceof ServerLevel level)) return;
        if (--nest.cooldown > 0) return;
        nest.cooldown = 600;
        long count = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(12), HogHunterPlayerEvents::isHog).size();
        if (count >= 4) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath(HogHunterModCompat.MOD_ID, "boar_hog"));
        if (type == null) return;
        Entity hog = type.create(level);
        if (hog == null) return;
        hog.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, level.random.nextFloat() * 360, 0);
        hog.getPersistentData().putBoolean("hoghunter_from_nest", true);
        level.addFreshEntity(hog);
        nest.setChanged();
    }
}
