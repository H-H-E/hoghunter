package com.hoghunter.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Server-owned expedition state. Progression survives death; active effects do not. */
public final class HogHunterPlayerData implements INBTSerializable<CompoundTag> {
    private int heartRate = 60;
    private int oil = 100;
    private int reserveOil;
    private int ammo;
    private int wounds;
    private int fracture;
    private int sanity = 100;
    private int noise;
    private int evidence;
    private int unlockedTier;
    private int fractureTreatments;
    private boolean lanternLit;
    private double oilDrainProgress;
    private int blackoutTicks;
    private int sprintLockTicks;
    private int panicCooldownTicks;

    public int heartRate() { return heartRate; }
    public int oil() { return oil; }
    public int reserveOil() { return reserveOil; }
    /** A display count of inventory bolts, not a second source of ammunition. */
    public int ammo() { return ammo; }
    public int wounds() { return wounds; }
    public int fracture() { return fracture; }
    public int sanity() { return sanity; }
    public int noise() { return noise; }
    public int evidence() { return evidence; }
    public int unlockedTier() { return unlockedTier; }
    public int fractureTreatments() { return fractureTreatments; }
    public boolean lanternLit() { return lanternLit; }
    public int blackoutTicks() { return blackoutTicks; }
    public int sprintLockTicks() { return sprintLockTicks; }
    public boolean sprintLocked() { return sprintLockTicks > 0 || fracture >= 2; }
    public boolean lanternAvailable() { return lanternLit && oil > 0 && blackoutTicks == 0; }
    public void setHeartRate(int v) { heartRate = clamp(v, 45, 180); }
    public void setOil(int v) { oil = clamp(v, 0, 100); }
    public void setReserveOil(int v) { reserveOil = clamp(v, 0, 6400); }
    public void setAmmo(int v) { ammo = clamp(v, 0, 6400); }
    public void setWounds(int v) { wounds = clamp(v, 0, 3); }
    public void setFracture(int v) { fracture = clamp(v, 0, 2); }
    public void setSanity(int v) { sanity = clamp(v, 0, 100); }
    public void setNoise(int v) { noise = clamp(v, 0, 100); }
    public void setEvidence(int v) { evidence = clamp(v, 0, 1_000_000); }
    public void setUnlockedTier(int v) { unlockedTier = clamp(v, 0, 5); }
    public void recordFractureTreatment() { fractureTreatments = Math.min(1_000_000, fractureTreatments + 1); }
    public void setLanternLit(boolean lit) { lanternLit = lit; }
    public void blackoutLantern(int ticks) { blackoutTicks = Math.max(blackoutTicks, clamp(ticks, 0, 1200)); }
    public void lockSprint(int ticks) { sprintLockTicks = Math.max(sprintLockTicks, clamp(ticks, 0, 1200)); }

    public void tickTemporaryState() {
        if (blackoutTicks > 0) blackoutTicks--;
        if (sprintLockTicks > 0) sprintLockTicks--;
        if (panicCooldownTicks > 0) panicCooldownTicks--;
    }

    /** One oil point per 240 active ticks at the default rate; partial drain survives saving. */
    public void tickLanternOil(double multiplier) {
        if (!lanternAvailable() || !Double.isFinite(multiplier) || multiplier <= 0) return;
        oilDrainProgress += Math.min(3.0, multiplier);
        if (oilDrainProgress >= 240.0) {
            int used = (int) (oilDrainProgress / 240.0);
            setOil(oil - used);
            oilDrainProgress -= used * 240.0;
        }
    }

    public boolean beginPanic() {
        if (heartRate < 180 || panicCooldownTicks > 0) return false;
        lockSprint(60);
        setNoise(noise + 30);
        panicCooldownTicks = 200;
        return true;
    }

    public void resetAfterDeath() {
        heartRate = 60;
        oil = 100;
        reserveOil = 0;
        ammo = 0;
        wounds = 0;
        fracture = 0;
        sanity = 100;
        noise = 0;
        evidence = 0;
        lanternLit = false;
        oilDrainProgress = 0;
        blackoutTicks = sprintLockTicks = panicCooldownTicks = 0;
    }

    public void copyTo(HogHunterPlayerData other) {
        other.heartRate = heartRate;
        other.oil = oil;
        other.reserveOil = reserveOil;
        other.ammo = ammo;
        other.wounds = wounds;
        other.fracture = fracture;
        other.sanity = sanity;
        other.noise = noise;
        other.evidence = evidence;
        other.unlockedTier = unlockedTier;
        other.fractureTreatments = fractureTreatments;
        other.lanternLit = lanternLit;
        other.oilDrainProgress = oilDrainProgress;
        other.blackoutTicks = blackoutTicks;
        other.sprintLockTicks = sprintLockTicks;
        other.panicCooldownTicks = panicCooldownTicks;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", 1);
        tag.putInt("heartRate", heartRate);
        tag.putInt("oil", oil);
        tag.putInt("reserveOil", reserveOil);
        tag.putInt("ammo", ammo);
        tag.putInt("wounds", wounds);
        tag.putInt("fracture", fracture);
        tag.putInt("sanity", sanity);
        tag.putInt("noise", noise);
        tag.putInt("evidence", evidence);
        tag.putInt("unlockedTier", unlockedTier);
        tag.putInt("fractureTreatments", fractureTreatments);
        tag.putBoolean("lanternLit", lanternLit);
        tag.putDouble("oilDrainProgress", oilDrainProgress);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        setHeartRate(readInt(tag, "heartRate", 60));
        setOil(readInt(tag, "oil", 100));
        setReserveOil(readInt(tag, "reserveOil", 0));
        setAmmo(readInt(tag, "ammo", 0));
        setWounds(readInt(tag, "wounds", 0));
        setFracture(readInt(tag, "fracture", 0));
        setSanity(readInt(tag, "sanity", 100));
        setNoise(readInt(tag, "noise", 0));
        setEvidence(readInt(tag, "evidence", 0));
        setUnlockedTier(readInt(tag, "unlockedTier", 0));
        fractureTreatments = clamp(readInt(tag, "fractureTreatments", 0), 0, 1_000_000);
        lanternLit = tag.getBoolean("lanternLit");
        double progress = tag.getDouble("oilDrainProgress");
        oilDrainProgress = Double.isFinite(progress) ? Math.max(0, Math.min(239.999, progress)) : 0;
        // Timed effects must not strand a player after a restart or a respawn.
        blackoutTicks = sprintLockTicks = panicCooldownTicks = 0;
    }

    private static int readInt(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
