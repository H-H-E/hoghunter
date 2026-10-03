package com.hoghunter.net;

import com.hoghunter.HogHunterMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** Server -> client sync of the player's authoritative horror-survival state. */
public final class HogPayloads {

    private HogPayloads() {
    }

    public record SyncPlayerState(
            int heartRate,
            float oil,
            int ammo,
            int wounds,
            int fracture,
            float sanity,
            float noise
    ) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncPlayerState> TYPE =
                new CustomPacketPayload.Type<>(HogHunterMod.id("state_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerState> STREAM_CODEC =
                StreamCodec.of(
                        (buf, value) -> {
                            buf.writeVarInt(value.heartRate());
                            buf.writeFloat(value.oil());
                            buf.writeVarInt(value.ammo());
                            buf.writeVarInt(value.wounds());
                            buf.writeVarInt(value.fracture());
                            buf.writeFloat(value.sanity());
                            buf.writeFloat(value.noise());
                        },
                        buf -> new SyncPlayerState(
                                buf.readVarInt(),
                                buf.readFloat(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readFloat(),
                                buf.readFloat()));

        @Override
        public CustomPacketPayload.Type<SyncPlayerState> type() {
            return TYPE;
        }

        public List<Float> asList() {
            List<Float> out = new ArrayList<>(7);
            out.add((float) heartRate);
            out.add(oil);
            out.add((float) ammo);
            out.add((float) wounds);
            out.add((float) fracture);
            out.add(sanity);
            out.add(noise);
            return out;
        }
    }

    /** Client -> server request to spend a bolt / oil / a snare charge. */
    public record ActionRequest(String action, int value) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ActionRequest> TYPE =
                new CustomPacketPayload.Type<>(HogHunterMod.id("action_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ActionRequest> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, ActionRequest::action,
                        ByteBufCodecs.VAR_INT, ActionRequest::value,
                        ActionRequest::new);

        @Override
        public CustomPacketPayload.Type<ActionRequest> type() {
            return TYPE;
        }
    }
}
