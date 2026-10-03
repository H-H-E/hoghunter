package com.hoghunter.client;

import com.hoghunter.client.model.HogModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "hoghunter", value = Dist.CLIENT, bus = Bus.MOD)
public final class HogClient {
    private static final String[] HOGS = {"boar_hog", "spore_hog", "hook_hog", "screecher_hog", "ironback_hog", "mire_hog", "rootmother"};
    private HogClient() {}

    public static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("hoghunter", id), "main");
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (String id : HOGS) event.registerLayerDefinition(layer(id), HogModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (String id : HOGS) registerIfPresent(event, id, scaleFor(id));
    }

    private static float scaleFor(String id) {
        return switch (id) { case "rootmother" -> 1.8f; case "ironback_hog" -> 1.15f; case "screecher_hog", "hook_hog" -> 1.05f; default -> 1.0f; };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Entity> void registerIfPresent(EntityRenderersEvent.RegisterRenderers event, String id, float scale) {
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath("hoghunter", id);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
        if (type != null) event.registerEntityRenderer((EntityType) type, context -> new HogRenderer(context, id, scale));
    }
}
