package com.hoghunter.core;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public final class HogAttachments {
    private HogAttachments() {}
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, HogHunterModCompat.MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HogHunterPlayerData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register("player_data", () -> AttachmentType.serializable(HogHunterPlayerData::new).copyOnDeath().build());
    public static HogHunterPlayerData get(net.minecraft.world.entity.player.Player player) {
        return player.getData(PLAYER_DATA);
    }
    public static void register(IEventBus modBus) { ATTACHMENT_TYPES.register(modBus); }
}
