package com.hoghunter.client;

import com.hoghunter.net.HogPayloads;
import net.minecraft.util.Mth;

/**
 * Client-side mirror of the server-authoritative survival state. This is presentation only:
 * nothing here may decide gameplay outcomes.
 */
public final class HogClientState {

    private static volatile int heartRate = 60;
    private static volatile float oil = 100.0F;
    private static volatile int ammo = 0;
    private static volatile int wounds;
    private static volatile int fracture;
    private static volatile float sanity = 100.0F;
    private static volatile float noise;
    private static volatile boolean lanternLit;
    private static volatile int blackoutTicks;
    private static volatile int sprintLockTicks;
    private static volatile int unlockedTier;
    private static volatile boolean received;
    private static int ticksSinceSync;

    private HogClientState() {
    }

    public static void accept(HogPayloads.SyncPlayerState payload) {
        heartRate = Mth.clamp(payload.heartRate(), 45, 180);
        oil = percent(payload.oil());
        ammo = Math.max(0, payload.ammo());
        wounds = Mth.clamp(payload.wounds(), 0, 3);
        fracture = Mth.clamp(payload.fracture(), 0, 2);
        sanity = percent(payload.sanity());
        noise = percent(payload.noise());
        lanternLit = payload.lanternLit();
        blackoutTicks = Math.max(0, payload.blackoutTicks());
        sprintLockTicks = Math.max(0, payload.sprintLockTicks());
        unlockedTier = Mth.clamp(payload.unlockedTier(), 0, 5);
        ticksSinceSync = 0;
        received = true;
    }

    private static float percent(float value) { return Float.isFinite(value) ? Mth.clamp(value, 0, 100) : 0; }

    public static void tick() { if (received && ticksSinceSync < 1200) ticksSinceSync++; }

    public static void reset() {
        heartRate = 60;
        oil = sanity = 100;
        ammo = wounds = fracture = blackoutTicks = sprintLockTicks = ticksSinceSync = 0;
        noise = 0;
        lanternLit = received = false;
        unlockedTier = 0;
    }

    public static boolean received() { return received; }
    public static boolean lanternLit() { return lanternLit; }
    public static int blackoutTicks() { return Math.max(0, blackoutTicks - ticksSinceSync); }
    public static int sprintLockTicks() { return Math.max(0, sprintLockTicks - ticksSinceSync); }
    public static int unlockedTier() { return unlockedTier; }

    public static int heartRate() {
        return heartRate;
    }

    public static float oil() {
        return oil;
    }

    public static int ammo() {
        return ammo;
    }

    public static int wounds() {
        return wounds;
    }

    public static int fracture() {
        return fracture;
    }

    public static float sanity() {
        return sanity;
    }

    public static float noise() {
        return noise;
    }
}
