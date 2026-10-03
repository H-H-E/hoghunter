package com.hoghunter.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class HookHogEntity extends HogEntity {
    private int grappleCooldown;
    public HookHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() { return HogAttributes.create(20, 1, 2, 0.25); }
    @Override protected void serverHogTick() {
        super.serverHogTick();
        if (grappleCooldown > 0) grappleCooldown--;
        LivingEntity target = getTarget();
        if (target instanceof ServerPlayer player && hasLineOfSight(player) && distanceToSqr(player) <= 144.0D && grappleCooldown == 0) {
            Vec3 pull = position().subtract(player.position()).normalize().scale(0.55D);
            player.addDeltaMovement(pull.add(0, 0.12D, 0));
            addNoise(player, 20);
            grappleCooldown = 120;
        }
    }
}
