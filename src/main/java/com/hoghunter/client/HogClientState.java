package com.hoghunter.client;

import com.hoghunter.net.HogPayloads;

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

    private HogClientState() {
    }

    public static void accept(HogPayloads.SyncPlayerState payload) {
        heartRate = payload.heartRate();
        oil = payload.oil();
        ammo = payload.ammo();
        wounds = payload.wounds();
        fracture = payload.fracture();
        sanity = payload.sanity();
        noise = payload.noise();
    }

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
