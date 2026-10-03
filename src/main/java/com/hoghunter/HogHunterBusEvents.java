package com.hoghunter;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = HogHunterMod.MOD_ID)
public final class HogHunterBusEvents {

    private HogHunterBusEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        HogHunterMod.LOGGER.info("Hog Hunter server hook OK.");
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        com.hoghunter.net.HogNetworking.register(event);
    }
}
