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
    public static final DeferredHolder<SoundEvent, SoundEvent> SNARE_SET = sound("block.baited_snare.set");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNARE_TRIGGER = sound("block.baited_snare.trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALT_PLACE = sound("block.salt_line.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROOT_ALTAR_ACTIVATE = sound("block.root_altar.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = sound("player.heartbeat");

    public static final DeferredHolder<SoundEvent, SoundEvent> HOG_AMBIENT = sound("entity.boar_hog.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOG_HURT = sound("entity.hog.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOG_DEATH = sound("entity.hog.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOAR_WINDUP = sound("entity.boar_hog.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOAR_ATTACK = sound("entity.boar_hog.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_WINDUP = sound("entity.spore_hog.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_RELEASE = sound("entity.spore_hog.release");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_WINDUP = sound("entity.hook_hog.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_PULL = sound("entity.hook_hog.pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREECHER_WINDUP = sound("entity.screecher_hog.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREECH = sound("entity.screecher_hog.screech");
    public static final DeferredHolder<SoundEvent, SoundEvent> IRONBACK_DEFLECT = sound("entity.ironback_hog.deflect");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIRE_SHIFT = sound("entity.mire_hog.shift");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROOTMOTHER_WINDUP = sound("entity.rootmother.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROOTMOTHER_SWEEP = sound("entity.rootmother.sweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROOTMOTHER_SUMMON = sound("entity.rootmother.summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROOTMOTHER_BLACKOUT = sound("entity.rootmother.blackout");

    // Keep the original authored event ids available to resource packs and /playsound.
    public static final DeferredHolder<SoundEvent, SoundEvent> HOG_SQUEAL = sound("entity.boar_hog.squeal");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_ACTIVATE = sound("block.ritual_altar.activate");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HogHunterMod.MOD_ID, id)));
    }

    public static void register(IEventBus modBus) { SOUNDS.register(modBus); }
    private HogSounds() {}
}
