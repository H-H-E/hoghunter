package com.hoghunter.net;

import com.hoghunter.core.HogAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Objects;
import java.util.function.Consumer;

/** Explicit one-way synchronization. Item actions already use vanilla's validated interaction path. */
public final class HogNetworking {
    private static Consumer<HogPayloads.SyncPlayerState> clientStateConsumer = payload -> {};

    private HogNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // Version 2 adds lantern, blackout, sprint-lock and permanent progression fields.
        event.registrar("2").playToClient(HogPayloads.SyncPlayerState.TYPE,
                HogPayloads.SyncPlayerState.STREAM_CODEC,
                (payload, context) -> clientStateConsumer.accept(payload));
    }

    /** Installed during physical-client setup, without linking a client class from common code. */
    public static void setClientStateConsumer(Consumer<HogPayloads.SyncPlayerState> consumer) {
        clientStateConsumer = Objects.requireNonNull(consumer);
    }

    public static void sync(ServerPlayer player) {
        if (player.connection == null) return;
        PacketDistributor.sendToPlayer(player, HogPayloads.SyncPlayerState.from(HogAttachments.get(player)));
    }
}
