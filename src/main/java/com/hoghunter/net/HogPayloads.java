package com.hoghunter.net;

import com.hoghunter.HogHunterMod;
import com.hoghunter.core.HogHunterPlayerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Common-side payload definitions; none of these classes depend on the Minecraft client. */
public final class HogPayloads {
    private HogPayloads() {}

    public record SyncPlayerState(int heartRate, float oil, int ammo, int wounds, int fracture,
                                  float sanity, float noise, boolean lanternLit, int blackoutTicks,
                                  int sprintLockTicks, int unlockedTier) implements CustomPacketPayload {
        public static final Type<SyncPlayerState> TYPE = new Type<>(HogHunterMod.id("state_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerState> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeVarInt(value.heartRate());
                    buf.writeFloat(value.oil());
                    buf.writeVarInt(value.ammo());
                    buf.writeVarInt(value.wounds());
                    buf.writeVarInt(value.fracture());
                    buf.writeFloat(value.sanity());
                    buf.writeFloat(value.noise());
                    buf.writeBoolean(value.lanternLit());
                    buf.writeVarInt(value.blackoutTicks());
                    buf.writeVarInt(value.sprintLockTicks());
                    buf.writeVarInt(value.unlockedTier());
                },
                buf -> new SyncPlayerState(buf.readVarInt(), buf.readFloat(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                        buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

        public static SyncPlayerState from(HogHunterPlayerData data) {
            return new SyncPlayerState(data.heartRate(), data.oil(), data.ammo(), data.wounds(), data.fracture(),
                    data.sanity(), data.noise(), data.lanternLit(), data.blackoutTicks(),
                    data.sprintLockTicks(), data.unlockedTier());
        }

        @Override public Type<SyncPlayerState> type() { return TYPE; }
    }
}
