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
        // Keep each entity's builder as the single source of its baseline combat values.
        event.put(HogEntities.BOAR_HOG.get(), BoarHogEntity.createAttributes().build());
        event.put(HogEntities.SPORE_HOG.get(), SporeHogEntity.createAttributes().build());
        event.put(HogEntities.HOOK_HOG.get(), HookHogEntity.createAttributes().build());
        event.put(HogEntities.SCREECHER_HOG.get(), ScreecherHogEntity.createAttributes().build());
        event.put(HogEntities.IRONBACK_HOG.get(), IronbackHogEntity.createAttributes().build());
        event.put(HogEntities.MIRE_HOG.get(), MireHogEntity.createAttributes().build());
        event.put(HogEntities.ROOTMOTHER.get(), RootmotherEntity.createAttributes().build());
    }
}
