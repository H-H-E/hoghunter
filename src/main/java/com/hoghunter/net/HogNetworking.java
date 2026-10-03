package com.hoghunter.net;

import com.hoghunter.HogHunterMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Registers every Hog Hunter network payload. */
public final class HogNetworking {

    private HogNetworking() {
    }

    public static void register(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(
                HogPayloads.SyncPlayerState.TYPE,
                HogPayloads.SyncPlayerState.STREAM_CODEC,
                HogNetworking::handleSyncClient);

        registrar.playToServer(
                HogPayloads.ActionRequest.TYPE,
                HogPayloads.ActionRequest.STREAM_CODEC,
                HogNetworking::handleActionRequest);
    }

    private static void handleSyncClient(HogPayloads.SyncPlayerState payload, IPayloadContext context) {
        // Rendering/HUD reads the mirrored client state. Gameplay authority stays server-side.
        context.enqueueWork(() -> com.hoghunter.client.HogClientState.accept(payload));
    }

    private static void handleActionRequest(HogPayloads.ActionRequest payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() ->
                HogHunterMod.LOGGER.debug("action {} x{} from {}", payload.action(), payload.value(), player.getName().getString()));
    }
}
