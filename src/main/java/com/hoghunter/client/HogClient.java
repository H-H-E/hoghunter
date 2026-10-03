package com.hoghunter.client;

import com.hoghunter.client.model.HogModel;
import com.hoghunter.HogHunterMod;
import com.hoghunter.content.HogEntities;
import com.hoghunter.content.HogItems;
import com.hoghunter.net.HogNetworking;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;

/** NeoForge 21.1.181+ routes each subscriber to its event's bus automatically. */
@EventBusSubscriber(modid = HogHunterMod.MOD_ID, value = Dist.CLIENT)
public final class HogClient {
    private static final String[] HOGS = {"boar_hog", "spore_hog", "hook_hog", "screecher_hog", "ironback_hog", "mire_hog", "rootmother"};
    private HogClient() {}

    public static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("hoghunter", id), "main");
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (String id : HOGS) event.registerLayerDefinition(layer(id), () -> HogModel.createBodyLayer(id));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Typed holders deliberately fail registration if a roster entry is broken.
        event.registerEntityRenderer(HogEntities.BOAR_HOG.get(), context -> new HogRenderer<>(context, "boar_hog", 1.0F));
        event.registerEntityRenderer(HogEntities.SPORE_HOG.get(), context -> new HogRenderer<>(context, "spore_hog", 1.0F));
        event.registerEntityRenderer(HogEntities.HOOK_HOG.get(), context -> new HogRenderer<>(context, "hook_hog", 1.05F));
        event.registerEntityRenderer(HogEntities.SCREECHER_HOG.get(), context -> new HogRenderer<>(context, "screecher_hog", 1.05F));
        event.registerEntityRenderer(HogEntities.IRONBACK_HOG.get(), context -> new HogRenderer<>(context, "ironback_hog", 1.15F));
        event.registerEntityRenderer(HogEntities.MIRE_HOG.get(), context -> new HogRenderer<>(context, "mire_hog", 1.0F));
        event.registerEntityRenderer(HogEntities.ROOTMOTHER.get(), context -> new HogRenderer<>(context, "rootmother", 1.8F));
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            HogNetworking.setClientStateConsumer(HogClientState::accept);
            ItemProperties.register(HogItems.FIELD_LANTERN.get(), HogHunterMod.id("lit"),
                    (stack, level, entity, seed) -> entity != null && entity == Minecraft.getInstance().player
                            && HogClientState.received() && HogClientState.lanternLit()
                            && HogClientState.oil() > 0 && HogClientState.blackoutTicks() == 0 ? 1.0F : 0.0F);
            HogHunterMod.LOGGER.info("Hog Hunter client presentation initialized.");
        });
    }

    @SubscribeEvent
    public static void registerGui(RegisterGuiLayersEvent event) {
        event.registerBelowAll(HogHunterMod.id("horror_effects"), HogHud::renderEffects);
        event.registerAboveAll(HogHunterMod.id("survival_status"), HogHud::renderStatus);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void bossBar(CustomizeGuiOverlayEvent.BossEventProgress event) { HogHud.trackBossBar(event); }

    @SubscribeEvent
    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) { resetSession(); }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) { resetSession(); }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !minecraft.isPaused()) {
            HogClientState.tick();
        }
        HogClientAudio.tick();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void beforePlayerTick(PlayerTickEvent.Pre event) { HogClientInput.beforePlayerTick(event.getEntity()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void afterPlayerTick(PlayerTickEvent.Post event) { HogClientInput.afterPlayerTick(event.getEntity()); }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity().level().isClientSide() && event.getEntity() == Minecraft.getInstance().player
                && HogClientState.received() && HogClientState.heartRate() >= 120)
            event.setNewSpeed(event.getNewSpeed() * 0.9F);
    }

    private static void resetSession() {
        HogClientInput.reset();
        HogClientState.reset();
        HogClientAudio.reset();
    }
}
