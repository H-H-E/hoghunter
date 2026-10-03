package com.hoghunter.client;

import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogHunterConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Quiet, tick-driven local feedback; respects both mod and native volume controls. */
final class HogClientAudio {
    private static int nextBeat;

    private HogClientAudio() {}

    static void reset() { nextBeat = 0; }

    static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !HogClientState.received()) {
            reset();
            return;
        }
        if (minecraft.isPaused()) return;
        if (!minecraft.player.isAlive() || minecraft.player.isSpectator() || minecraft.player.isCreative()
                || HogClientState.heartRate() < 90) {
            reset();
            return;
        }
        float intensity = HogHunterConfig.HORROR_AUDIO_INTENSITY.get().floatValue();
        if (intensity <= 0) { reset(); return; }
        if (nextBeat-- > 0) return;
        nextBeat = HogClientState.heartRate() >= 140 ? 15 : 29;
        float volume = intensity * (HogClientState.heartRate() >= 140 ? 0.3F : 0.18F);
        minecraft.getSoundManager().play(new SimpleSoundInstance(
                HogSounds.HEARTBEAT.getId(), SoundSource.PLAYERS, volume, 1.0F,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE,
                0, 0, 0, true));
    }
}
