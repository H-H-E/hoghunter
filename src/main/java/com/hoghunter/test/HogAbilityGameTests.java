package com.hoghunter.test;

import com.hoghunter.content.HogEntities;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.entity.HogEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Deterministic ability contracts with real server entities and observable effects. */
@GameTestHolder("hoghunter")
@PrefixGameTestTemplate(false)
public final class HogAbilityGameTests {
    private HogAbilityGameTests() {}

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void boarTelegraphsHitsOnceAndRecovers(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 17.0);
        var boar = HogTestSupport.hog(helper, HogEntities.BOAR_HOG.get(), 16, 2, 16);
        try {
            boar.setTarget(player);
            float before = player.getHealth();
            HogTestSupport.stationaryTicks(boar, 10);
            helper.assertValueEqual(player.getHealth(), before, "boar damaged before its charge tell finished");
            for (int tick = 0; tick < 30 && player.getHealth() == before; tick++) HogTestSupport.stationaryTicks(boar, 1);
            helper.assertTrue(player.getHealth() < before - 2.0F, "boar charge lost its normal damage component; before="
                    + before + ", after=" + player.getHealth() + ", " + combatState(boar, player));
            float after = player.getHealth();
            for (int tick = 0; tick < 100; tick++) {
                player.invulnerableTime = 0;
                HogTestSupport.stationaryTicks(boar, 1);
            }
            helper.assertValueEqual(player.getHealth(), after, "charge hit more than once during cooldown");
            for (int tick = 0; tick < 120 && player.getHealth() == after; tick++) {
                player.invulnerableTime = 0;
                HogTestSupport.stationaryTicks(boar, 1);
            }
            helper.assertTrue(player.getHealth() < after, "boar never became able to charge again");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void sporeCloudPulsesAndHonorsCooldown(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 19);
        var spore = HogTestSupport.hog(helper, HogEntities.SPORE_HOG.get(), 16, 2, 16);
        try {
            var data = HogAttachments.get(player);
            data.setHeartRate(60);
            spore.setTarget(player);
            HogTestSupport.stationaryTicks(spore, 10);
            helper.assertTrue(!player.hasEffect(MobEffects.BLINDNESS), "spores hit during their tell");
            for (int tick = 0; tick < 40 && data.heartRate() == 60; tick++) HogTestSupport.stationaryTicks(spore, 1);
            helper.assertValueEqual(data.heartRate(), 72, "first spore pulse heart rate; " + combatState(spore, player));
            helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "spore pulse did not blind its target");
            HogTestSupport.stationaryTicks(spore, 20);
            helper.assertValueEqual(data.heartRate(), 72, "spore cloud pulses too frequently");
            HogTestSupport.stationaryTicks(spore, 210);
            helper.assertValueEqual(data.heartRate(), 96, "cloud should deliver three pulses before recovery");
            HogTestSupport.stationaryTicks(spore, 120);
            helper.assertTrue(data.heartRate() > 96, "spore cloud never recovered after cooldown");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void hookAppliesPullImpulseOnceThenWaits(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 24);
        var hook = HogTestSupport.hog(helper, HogEntities.HOOK_HOG.get(), 16, 2, 16);
        try {
            var data = HogAttachments.get(player);
            data.setNoise(0);
            hook.setTarget(player);
            HogTestSupport.stationaryTicks(hook, 6);
            helper.assertValueEqual(data.noise(), 0, "hook fired before its tell");
            HogTestSupport.stationaryTicks(hook, 30);
            helper.assertValueEqual(data.noise(), 20, "hook must add noise once per grapple");
            helper.assertTrue(player.getDeltaMovement().dot(hook.position().subtract(player.position())) > 0.0D,
                    "grapple impulse does not pull toward the Hook Hog");
            helper.assertTrue(player.hurtMarked, "grapple did not request player motion synchronization");
            HogTestSupport.stationaryTicks(hook, 70);
            helper.assertValueEqual(data.noise(), 20, "hook repeated before cooldown expired");
            HogTestSupport.stationaryTicks(hook, 100);
            helper.assertTrue(data.noise() >= 40, "hook did not recover after cooldown");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void hookCannotGrappleThroughStone(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 23);
        var hook = HogTestSupport.hog(helper, HogEntities.HOOK_HOG.get(), 16, 2, 16);
        try {
            for (int x = 13; x <= 19; x++) for (int y = 1; y <= 5; y++) helper.setBlock(x, y, 19, Blocks.STONE);
            hook.setTarget(player);
            Vec3 before = player.position();
            HogTestSupport.stationaryTicks(hook, 180);
            helper.assertValueEqual(HogAttachments.get(player).noise(), 0, "hook emitted a successful hit through stone");
            helper.assertTrue(player.position().distanceToSqr(before) < 0.001D, "hook moved its occluded target");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void screecherLocksSprintAndSummonsOnce(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 22);
        var screecher = HogTestSupport.hog(helper, HogEntities.SCREECHER_HOG.get(), 16, 2, 16);
        try {
            var data = HogAttachments.get(player);
            data.setHeartRate(60);
            player.setSprinting(true);
            screecher.setTarget(player);
            HogTestSupport.stationaryTicks(screecher, 10);
            helper.assertValueEqual(data.heartRate(), 60, "screecher skipped its tell");
            for (int tick = 0; tick < 40 && data.heartRate() == 60; tick++) HogTestSupport.stationaryTicks(screecher, 1);
            helper.assertValueEqual(data.heartRate(), 85, "screecher pulse heart rate");
            helper.assertTrue(data.sprintLockTicks() > 0, "screecher did not install a persistent sprint lock");
            helper.assertTrue(!player.isSprinting(), "screech left its victim sprinting");
            helper.assertValueEqual(helper.getEntities(HogEntities.BOAR_HOG.get()).size(), 1, "screech reinforcement count");
            HogTestSupport.stationaryTicks(screecher, 200);
            helper.assertValueEqual(data.heartRate(), 85, "screecher pulsed during cooldown");
            helper.assertValueEqual(helper.getEntities(HogEntities.BOAR_HOG.get()).size(), 1, "screecher summoned during cooldown");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void screecherRespectsLocalPopulationCap(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 22);
        var screecher = HogTestSupport.hog(helper, HogEntities.SCREECHER_HOG.get(), 16, 2, 16);
        try {
            for (int index = 0; index < 4; index++) HogTestSupport.hog(helper, HogEntities.BOAR_HOG.get(), 11 + index * 3, 2, 12);
            screecher.setTarget(player);
            HogTestSupport.stationaryTicks(screecher, 50);
            helper.assertValueEqual(HogAttachments.get(player).heartRate(), 85, "cap should not suppress the screech itself");
            helper.assertValueEqual(helper.getEntities(HogEntities.BOAR_HOG.get()).size(), 4, "screech exceeded local population cap");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void onlyIronbackHasDirectionalPlating(GameTestHelper helper) {
        var attacker = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var ironback = HogTestSupport.hog(helper, HogEntities.IRONBACK_HOG.get(), 16, 2, 16);
        float front = hitFrom(helper, ironback, attacker, 16, 20);
        float back = hitFrom(helper, ironback, attacker, 16, 12);
        helper.assertTrue(front > 0 && back > front * 1.8F, "Ironback plating did not protect its front while exposing its rear");
        var boar = HogTestSupport.hog(helper, HogEntities.BOAR_HOG.get(), 12, 2, 16);
        float boarFront = hitFrom(helper, boar, attacker, 12, 20);
        float boarBack = hitFrom(helper, boar, attacker, 12, 12);
        helper.assertTrue(Math.abs(boarFront - boarBack) < 0.001F, "directional plating leaked onto ordinary hogs");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void mireVanishLastsAndReappearsSafelyBehindTarget(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 22);
        for (int x = 14; x <= 18; x++) for (int z = 14; z <= 18; z++) helper.setBlock(x, 2, z, Blocks.WATER);
        var mire = HogTestSupport.hog(helper, HogEntities.MIRE_HOG.get(), 16, 2, 16);
        try {
            player.setYRot(0);
            player.setXRot(0);
            mire.setTarget(player);
            for (int tick = 0; tick < 10 && !mire.isInvisiblePhase(); tick++) mire.tick();
            helper.assertTrue(mire.isInvisiblePhase(), "Mire Hog did not vanish in water");
            helper.assertTrue(mire.canBreatheUnderwater(), "Mire Hog cannot survive its own water habitat");
            helper.assertTrue(mire.checkSpawnObstruction(helper.getLevel()), "Mire Hog's native spawn obstruction check rejects clear water");
            for (int tick = 0; tick < 50; tick++) { mire.setDeltaMovement(Vec3.ZERO); mire.tick(); }
            helper.assertTrue(mire.isInvisiblePhase(), "Mire Hog reappeared before its four-second vanish completed");
            for (int tick = 0; tick < 50 && mire.isInvisiblePhase(); tick++) { mire.setDeltaMovement(Vec3.ZERO); mire.tick(); }
            helper.assertTrue(!mire.isInvisiblePhase() && !mire.isInvisible(), "Mire Hog stayed invisible after its ability");
            Vec3 offset = mire.position().subtract(player.position());
            helper.assertTrue(offset.dot(player.getLookAngle()) < 0, "Mire Hog did not reappear behind its target");
            helper.assertTrue(offset.lengthSqr() < 20, "Mire Hog never reached its target");
            helper.assertTrue(helper.getLevel().noCollision(mire), "Mire Hog teleported inside a solid block or entity");
            HogTestSupport.position(helper, mire, 16, 2, 16);
            HogTestSupport.stationaryTicks(mire, 120);
            helper.assertTrue(!mire.isInvisiblePhase(), "Mire Hog vanished again during cooldown");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void rootmotherSweepExcludesSideAndRear(GameTestHelper helper) {
        var front = HogTestSupport.player(helper, 16, 2, 19);
        var side = HogTestSupport.player(helper, 19, 2, 16);
        var rear = HogTestSupport.player(helper, 16, 2, 13);
        var boss = HogTestSupport.hog(helper, HogEntities.ROOTMOTHER.get(), 16, 2, 16);
        try {
            boss.setTarget(front);
            for (int tick = 0; tick < 200 && front.getHealth() == front.getMaxHealth(); tick++) {
                boss.setYRot(0);
                boss.setXRot(0);
                HogTestSupport.stationaryTicks(boss, 1);
            }
            helper.assertTrue(front.getHealth() < front.getMaxHealth(), "Rootmother sweep missed a player directly ahead");
            helper.assertValueEqual(side.getHealth(), side.getMaxHealth(), "120-degree sweep hit a player at 90 degrees");
            helper.assertValueEqual(rear.getHealth(), rear.getMaxHealth(), "Rootmother sweep hit a player behind it");
            float after = front.getHealth();
            for (int tick = 0; tick < 80; tick++) {
                front.invulnerableTime = 0;
                boss.setYRot(0);
                HogTestSupport.stationaryTicks(boss, 1);
            }
            helper.assertValueEqual(front.getHealth(), after, "Rootmother swept again during cooldown");
            helper.succeed();
        } finally {
            front.discard(); side.discard(); rear.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void rootmotherBlackoutConservesOilAndLatchesPhase(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 24);
        var boss = HogTestSupport.hog(helper, HogEntities.ROOTMOTHER.get(), 16, 2, 16);
        try {
            var data = HogAttachments.get(player);
            data.setOil(71);
            data.setLanternLit(true);
            boss.setTarget(player);
            boss.setHealth(boss.getMaxHealth() * 0.49F);
            HogTestSupport.stationaryTicks(boss, 1);
            helper.assertTrue(data.blackoutTicks() > 0, "half-health Rootmother did not trigger blackout");
            helper.assertValueEqual(data.oil(), 71, "blackout destroyed stored lantern oil");
            data.setOil(38);
            for (int tick = 0; tick < 180; tick++) {
                HogTestSupport.stationaryTicks(boss, 1);
                data.tickTemporaryState();
            }
            boss.setHealth(boss.getMaxHealth());
            HogTestSupport.stationaryTicks(boss, 1);
            boss.setHealth(boss.getMaxHealth() * 0.49F);
            HogTestSupport.stationaryTicks(boss, 1);
            helper.assertValueEqual(data.blackoutTicks(), 0, "healing across half health retriggered the one-shot blackout");
            helper.assertValueEqual(data.oil(), 38, "blackout restored stale oil after the player spent fuel");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void rootmotherSummonsOnlyInCombatAndWaits(GameTestHelper helper) {
        var boss = HogTestSupport.hog(helper, HogEntities.ROOTMOTHER.get(), 16, 2, 16);
        HogTestSupport.stationaryTicks(boss, 600);
        helper.assertValueEqual(helper.getEntities(HogEntities.BOAR_HOG.get()).size(), 0, "idle boss spawned reinforcements");
        var player = HogTestSupport.player(helper, 16, 2, 27);
        try {
            boss.setTarget(player);
            HogTestSupport.stationaryTicks(boss, 540);
            helper.assertValueEqual(helper.getEntities(HogEntities.BOAR_HOG.get()).size(), 2, "root call boar count");
            helper.assertValueEqual(helper.getEntities(HogEntities.SPORE_HOG.get()).size(), 1, "root call spore count");
            HogTestSupport.stationaryTicks(boss, 100);
            helper.assertValueEqual(helper.getEntities(HogEntities.BOAR_HOG.get()).size(), 2, "root call ignored its cooldown");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void hogSaveRestoresVanillaHealthAndPersistence(GameTestHelper helper) {
        List<EntityType<? extends HogEntity>> types = List.of(HogEntities.BOAR_HOG.get(), HogEntities.SPORE_HOG.get(),
                HogEntities.HOOK_HOG.get(), HogEntities.SCREECHER_HOG.get(), HogEntities.IRONBACK_HOG.get(),
                HogEntities.MIRE_HOG.get(), HogEntities.ROOTMOTHER.get());
        for (var type : types) {
            var original = type.create(helper.getLevel());
            original.setHealth(7.0F);
            original.setPersistenceRequired();
            original.setNoAi(true);
            var saved = original.saveWithoutId(new CompoundTag());
            var restored = type.create(helper.getLevel());
            restored.load(saved);
            helper.assertValueEqual(restored.getHealth(), 7.0F, type + " lost vanilla health on save/load");
            helper.assertTrue(restored.isPersistenceRequired(), type + " lost persistence on save/load");
            helper.assertTrue(restored.isNoAi(), type + " lost vanilla NoAI on save/load");
            helper.assertTrue(!restored.isCharging() && !restored.isInvisiblePhase(), type + " resumed a half-saved attack");
        }
        helper.succeed();
    }

    private static float hitFrom(GameTestHelper helper, HogEntity hog, net.minecraft.world.entity.player.Player player,
                                 double x, double z) {
        HogTestSupport.position(helper, player, x, 2, z);
        hog.setYRot(0);
        hog.setXRot(0);
        hog.setTarget(player);
        hog.setHealth(hog.getMaxHealth());
        hog.invulnerableTime = 0;
        float before = hog.getHealth();
        helper.assertTrue(hog.hurt(hog.damageSources().playerAttack(player), 10.0F), "test attack failed to damage " + hog.getType());
        return before - hog.getHealth();
    }

    private static String combatState(HogEntity hog, LivingEntity player) {
        return "state=" + hog.getAbilityState() + ", distance=" + hog.distanceTo(player)
                + ", lineOfSight=" + hog.hasLineOfSight(player) + ", hog=" + hog.position() + ", player=" + player.position()
                + ", feetBlock=" + hog.level().getBlockState(hog.blockPosition())
                + ", eyeBlock=" + hog.level().getBlockState(BlockPos.containing(hog.getEyePosition()));
    }
}
