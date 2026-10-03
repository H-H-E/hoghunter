package com.hoghunter.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class IronbackHogEntity extends HogEntity {
    private int turnTicks;
    public IronbackHogEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() { return HogAttributes.create(48, 8, 6, 0.19, 0.8); }
    @Override protected void serverHogTick() {
        super.serverHogTick();
        if (getTarget() != null && turnTicks < 12) turnTicks++;
        if (getTarget() != null && turnTicks >= 12) lookAt(getTarget(), 30.0F, 30.0F);
    }
}
