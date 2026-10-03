package com.hoghunter.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Mutable authoritative expedition state. All mutation happens on the server. */
public final class HogHunterPlayerData implements INBTSerializable<CompoundTag> {
    private int heartRate = 60;
    private int oil = 100;
    private int reserveOil;
    private int ammo = 12;
    private int wounds;
    private int fracture;
    private int sanity = 100;
    private int noise;
    private int evidence;

    public int heartRate() { return heartRate; }
    public int oil() { return oil; }
    public int reserveOil() { return reserveOil; }
    public int ammo() { return ammo; }
    public int wounds() { return wounds; }
    public int fracture() { return fracture; }
    public int sanity() { return sanity; }
    public int noise() { return noise; }
    public int evidence() { return evidence; }
    public void setHeartRate(int v) { heartRate = clamp(v, 45, 180); }
    public void setOil(int v) { oil = clamp(v, 0, 100); }
    public void setReserveOil(int v) { reserveOil = Math.max(0, v); }
    public void setAmmo(int v) { ammo = Math.max(0, v); }
    public void setWounds(int v) { wounds = clamp(v, 0, 3); }
    public void setFracture(int v) { fracture = clamp(v, 0, 2); }
    public void setSanity(int v) { sanity = clamp(v, 0, 100); }
    public void setNoise(int v) { noise = clamp(v, 0, 100); }
    public void setEvidence(int v) { evidence = Math.max(0, v); }

    public void copyTo(HogHunterPlayerData other) {
        other.heartRate = heartRate; other.oil = oil; other.reserveOil = reserveOil;
        other.ammo = ammo; other.wounds = wounds; other.fracture = fracture;
        other.sanity = sanity; other.noise = noise; other.evidence = evidence;
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("heartRate", heartRate); tag.putInt("oil", oil); tag.putInt("reserveOil", reserveOil);
        tag.putInt("ammo", ammo); tag.putInt("wounds", wounds); tag.putInt("fracture", fracture);
        tag.putInt("sanity", sanity); tag.putInt("noise", noise); tag.putInt("evidence", evidence);
        return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        heartRate = clamp(tag.getInt("heartRate"), 45, 180); oil = clamp(tag.getInt("oil"), 0, 100);
        reserveOil = Math.max(0, tag.getInt("reserveOil")); ammo = Math.max(0, tag.getInt("ammo"));
        wounds = clamp(tag.getInt("wounds"), 0, 3); fracture = clamp(tag.getInt("fracture"), 0, 2);
        sanity = clamp(tag.getInt("sanity"), 0, 100); noise = clamp(tag.getInt("noise"), 0, 100);
        evidence = Math.max(0, tag.getInt("evidence"));
    }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
