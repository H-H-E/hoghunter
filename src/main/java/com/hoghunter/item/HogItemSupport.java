package com.hoghunter.item;

import com.hoghunter.core.HogHunterPlayerEvents;
import net.minecraft.world.entity.LivingEntity;

/** Small common predicates for item mechanics; the entity lane remains untouched. */
final class HogItemSupport {
    private HogItemSupport() {}

    static boolean isHog(LivingEntity entity) {
        return HogHunterPlayerEvents.isHog(entity);
    }

    static boolean isRootmother(LivingEntity entity) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && "hoghunter".equals(key.getNamespace()) && "rootmother".equals(key.getPath());
    }
}
