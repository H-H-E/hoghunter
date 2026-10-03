package com.hoghunter.test;

import com.hoghunter.core.HogAttachments;
import com.hoghunter.core.HogHunterPlayerData;
import com.hoghunter.net.HogPayloads;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Persistence and protocol regressions run with the real server registry provider. */
@GameTestHolder("hoghunter")
@PrefixGameTestTemplate(false)
public final class HogDataGameTests {
    private HogDataGameTests() {}

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 40)
    public static void missingNbtKeepsFreshPlayerDefaults(GameTestHelper helper) {
        var data = new HogHunterPlayerData();
        data.deserializeNBT(helper.getLevel().registryAccess(), new CompoundTag());
        helper.assertValueEqual(data.heartRate(), 60, "new player heart rate after empty save");
        helper.assertValueEqual(data.oil(), 100, "new player oil after empty save");
        helper.assertValueEqual(data.sanity(), 100, "new player sanity after empty save");
        helper.assertValueEqual(data.wounds(), 0, "new player wounds");
        helper.assertValueEqual(data.unlockedTier(), 0, "new player progression");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 40)
    public static void malformedNbtCannotEscapeSurvivalBounds(GameTestHelper helper) {
        var tag = new CompoundTag();
        tag.putInt("heartRate", Integer.MAX_VALUE);
        tag.putInt("oil", -100);
        tag.putInt("reserveOil", -1);
        tag.putInt("ammo", -1);
        tag.putInt("wounds", 100);
        tag.putInt("fracture", 100);
        tag.putInt("sanity", -1);
        tag.putInt("noise", 999);
        tag.putInt("evidence", -1);
        tag.putInt("unlockedTier", 999);
        var data = new HogHunterPlayerData();
        data.deserializeNBT(helper.getLevel().registryAccess(), tag);
        helper.assertValueEqual(data.heartRate(), 180, "heart rate upper limit");
        helper.assertValueEqual(data.oil(), 0, "oil lower limit");
        helper.assertValueEqual(data.reserveOil(), 0, "reserve oil lower limit");
        helper.assertValueEqual(data.ammo(), 0, "ammo lower limit");
        helper.assertValueEqual(data.wounds(), 3, "wound upper limit");
        helper.assertValueEqual(data.fracture(), 2, "fracture upper limit");
        helper.assertValueEqual(data.sanity(), 0, "sanity lower limit");
        helper.assertValueEqual(data.noise(), 100, "noise upper limit");
        helper.assertValueEqual(data.evidence(), 0, "evidence lower limit");
        helper.assertValueEqual(data.unlockedTier(), 5, "progression upper limit");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 40)
    public static void expeditionStateRoundTripsWithoutCopyAliasing(GameTestHelper helper) {
        var original = filledData();
        var restored = new HogHunterPlayerData();
        var provider = helper.getLevel().registryAccess();
        CompoundTag saved = original.serializeNBT(provider);
        restored.deserializeNBT(provider, saved);
        helper.assertTrue(saved.equals(restored.serializeNBT(provider)), "NBT round trip lost expedition state");
        var copy = new HogHunterPlayerData();
        original.copyTo(copy);
        helper.assertTrue(saved.equals(copy.serializeNBT(provider)), "copyTo lost expedition state");
        copy.setOil(0);
        copy.setUnlockedTier(5);
        helper.assertValueEqual(original.oil(), 37, "copy mutation changed original oil");
        helper.assertValueEqual(original.unlockedTier(), 3, "copy mutation changed original progression");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 40)
    public static void playerSaveLoadsRegisteredAttachment(GameTestHelper helper) {
        // Vanilla's basic mock is sufficient here: no packets, login, or player-list
        // behavior is claimed. Entity save/load still exercises the registered attachment.
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        filledData().copyTo(HogAttachments.get(player));
        var saved = player.saveWithoutId(new CompoundTag());
        var restored = helper.makeMockPlayer(GameType.SURVIVAL);
        restored.load(saved);
        var provider = helper.getLevel().registryAccess();
        helper.assertTrue(HogAttachments.get(player).serializeNBT(provider)
                        .equals(HogAttachments.get(restored).serializeNBT(provider)),
                "registered player attachment did not survive entity save/load");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 40)
    public static void deathResetsInjuriesAndKeepsEarnedProgression(GameTestHelper helper) {
        var data = filledData();
        data.blackoutLantern(160);
        data.lockSprint(40);
        data.resetAfterDeath();
        helper.assertValueEqual(data.unlockedTier(), 3, "death erased earned unlocks");
        helper.assertValueEqual(data.fractureTreatments(), 2, "death erased treatment evidence");
        helper.assertValueEqual(data.wounds(), 0, "wounds survived death");
        helper.assertValueEqual(data.fracture(), 0, "fractures survived death");
        helper.assertValueEqual(data.evidence(), 0, "expedition evidence survived death");
        helper.assertValueEqual(data.blackoutTicks(), 0, "blackout survived death");
        helper.assertValueEqual(data.sprintLockTicks(), 0, "sprint lock survived death");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 40)
    public static void overlappingTemporaryEffectsDoNotRestoreSpentOil(GameTestHelper helper) {
        var data = new HogHunterPlayerData();
        data.setOil(71);
        data.blackoutLantern(160);
        data.blackoutLantern(20);
        data.lockSprint(40);
        data.lockSprint(5);
        helper.assertValueEqual(data.blackoutTicks(), 160, "short blackout shortened existing blackout");
        helper.assertValueEqual(data.sprintLockTicks(), 40, "short lock shortened existing sprint lock");
        data.setOil(38);
        for (int tick = 0; tick < 200; tick++) data.tickTemporaryState();
        helper.assertValueEqual(data.blackoutTicks(), 0, "blackout did not expire");
        helper.assertValueEqual(data.sprintLockTicks(), 0, "sprint lock did not expire");
        helper.assertValueEqual(data.oil(), 38, "blackout restored a stale oil snapshot");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 40)
    public static void statePayloadCodecPreservesEveryField(GameTestHelper helper) {
        var value = new HogPayloads.SyncPlayerState(147, 37.0F, 9, 2, 1, 43.0F, 56.0F, true, 133, 22, 3);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            HogPayloads.SyncPlayerState.STREAM_CODEC.encode(buffer, value);
            var decoded = HogPayloads.SyncPlayerState.STREAM_CODEC.decode(buffer);
            helper.assertTrue(value.equals(decoded), "state payload codec lost or reordered fields");
            helper.assertValueEqual(buffer.readableBytes(), 0, "state codec left unread trailing fields");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 40)
    public static void lanternDrainSurvivesSaveAndPausesWhileBlackout(GameTestHelper helper) {
        var original = new HogHunterPlayerData();
        original.setOil(80);
        original.setLanternLit(true);
        for (int tick = 0; tick < 120; tick++) original.tickLanternOil(1.0D);
        helper.assertValueEqual(original.oil(), 80, "lantern burned a whole oil point too early");
        var restored = new HogHunterPlayerData();
        restored.deserializeNBT(helper.getLevel().registryAccess(), original.serializeNBT(helper.getLevel().registryAccess()));
        restored.blackoutLantern(160);
        for (int tick = 0; tick < 160; tick++) {
            restored.tickLanternOil(1.0D);
            restored.tickTemporaryState();
        }
        helper.assertValueEqual(restored.oil(), 80, "blackout consumed oil");
        for (int tick = 0; tick < 120; tick++) restored.tickLanternOil(1.0D);
        helper.assertValueEqual(restored.oil(), 79, "save/load lost the partial oil drain interval");
        restored.setLanternLit(false);
        for (int tick = 0; tick < 480; tick++) restored.tickLanternOil(1.0D);
        restored.tickLanternOil(Double.NaN);
        helper.assertValueEqual(restored.oil(), 79, "extinguished lantern consumed fuel");
        helper.succeed();
    }

    private static HogHunterPlayerData filledData() {
        var data = new HogHunterPlayerData();
        data.setHeartRate(147);
        data.setOil(37);
        data.setReserveOil(2);
        data.setAmmo(9);
        data.setWounds(2);
        data.setFracture(1);
        data.setSanity(43);
        data.setNoise(56);
        data.setEvidence(7);
        data.setLanternLit(false);
        data.setUnlockedTier(3);
        data.recordFractureTreatment();
        data.recordFractureTreatment();
        return data;
    }
}
