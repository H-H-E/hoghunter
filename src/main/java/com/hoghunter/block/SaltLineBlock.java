package com.hoghunter.block;

import com.hoghunter.core.HogHunterPlayerEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class SaltLineBlock extends Block {
    public SaltLineBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && HogHunterPlayerEvents.isHog(living)) {
            living.makeStuckInBlock(state, new Vec3(0.6, 1.0, 0.6));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
            living.getPersistentData().putLong("hoghunter_salt_until", level.getGameTime() + 60);
            // The entity lane has no ability-lock hook, so this authoritative marker is
            // the in-scope representation for special abilities to honor when it reads state.
            living.getPersistentData().putLong("hoghunter_salt_ability_until", level.getGameTime() + 60);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }
}
