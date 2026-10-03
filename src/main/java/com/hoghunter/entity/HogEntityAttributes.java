package com.hoghunter.entity;

import com.hoghunter.content.HogEntities;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * Registers the attribute supplier for every corrupted hog.
 *
 * <p>{@code EntityType.Builder} in 1.21.1 has no {@code attributes(...)} hook, so the supplier
 * has to be supplied through the mod-bus {@link EntityAttributeCreationEvent}. Without this the
 * server logs "Entity hoghunter:... has no attributes" and the mob cannot be spawned at all.
 *
 * <p>Values mirror {@code design/02-gameplay.md} section 3.
 */
public final class HogEntityAttributes {

    private HogEntityAttributes() {
    }

    public static void register(EntityAttributeCreationEvent event) {
        // health, armor, attack damage, movement speed [, knockback resistance]
        event.put(HogEntities.BOAR_HOG.get(),
                HogAttributes.create(24.0D, 2.0D, 4.0D, 0.27D).build());
        event.put(HogEntities.SPORE_HOG.get(),
                HogAttributes.create(30.0D, 3.0D, 3.0D, 0.22D).build());
        event.put(HogEntities.HOOK_HOG.get(),
                HogAttributes.create(20.0D, 1.0D, 2.0D, 0.25D).build());
        event.put(HogEntities.SCREECHER_HOG.get(),
                HogAttributes.create(18.0D, 0.0D, 2.0D, 0.31D).build());
        event.put(HogEntities.IRONBACK_HOG.get(),
                HogAttributes.create(48.0D, 8.0D, 6.0D, 0.19D, 0.8D).build());
        event.put(HogEntities.MIRE_HOG.get(),
                HogAttributes.create(34.0D, 2.0D, 5.0D, 0.24D).build());
        event.put(HogEntities.ROOTMOTHER.get(),
                HogAttributes.create(260.0D, 10.0D, 8.0D, 0.16D, 1.0D).build());
    }
}
