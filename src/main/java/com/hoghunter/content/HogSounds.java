package com.hoghunter.content;

import com.hoghunter.HogHunterMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HogSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, HogHunterMod.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_FIRE = sound("bolt_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> LANTERN_TOGGLE = sound("lantern_toggle");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HogHunterMod.MOD_ID, id)));
    }

    public static void register(IEventBus modBus) { SOUNDS.register(modBus); }
    private HogSounds() {}
}
