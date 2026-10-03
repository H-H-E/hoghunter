package com.hoghunter.test;

import com.hoghunter.block.DepthGateBlock;
import com.hoghunter.content.HogBlocks;
import com.hoghunter.content.HogEntities;
import com.hoghunter.content.HogItems;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.core.HogHunterPlayerEvents;
import com.hoghunter.item.HogGunItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Item transactions, real use completion, block interactions and timed world effects. */
@GameTestHolder("hoghunter")
@PrefixGameTestTemplate(false)
public final class HogItemGameTests {
    private HogItemGameTests() {}

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void boltGunConsumesInventoryAmmoExactlyOnce(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        HogTestSupport.position(helper, player, 16, 2, 16);
        var boar = HogTestSupport.hog(helper, HogEntities.BOAR_HOG.get(), 16, 2, 22);
        aimAt(player, boar);
        var gun = new ItemStack(HogItems.BOLT_GUN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().setItem(1, new ItemStack(HogItems.IRON_BOLT.get(), 3));
        HogAttachments.get(player).setAmmo(999); // A stale HUD count cannot authorize free bullets.
        float before = boar.getHealth();
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(boar.getHealth() < before, "bolt gun did not hit the aimed hog");
        helper.assertValueEqual(HogGunItem.countAmmo(player), 2, "bolt gun ammo consumption");
        helper.assertValueEqual(HogAttachments.get(player).ammo(), 2, "HUD ammo diverged from inventory");
        helper.assertValueEqual(gun.getDamageValue(), 1, "bolt gun durability per shot");
        float after = boar.getHealth();
        boar.invulnerableTime = 0;
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(HogGunItem.countAmmo(player), 2, "cooldown allowed a second paid shot");
        helper.assertValueEqual(boar.getHealth(), after, "cooldown allowed a second hit");
        player.getInventory().setItem(1, ItemStack.EMPTY);
        player.getCooldowns().removeCooldown(gun.getItem());
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(boar.getHealth(), after, "cached attachment ammo allowed a shot with empty inventory");
        helper.assertValueEqual(gun.getDamageValue(), 1, "empty gun lost durability");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void chargedSilverShotCompletesOnceAndConsumesTwoBolts(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        HogTestSupport.position(helper, player, 16, 2, 16);
        var boar = HogTestSupport.hog(helper, HogEntities.BOAR_HOG.get(), 16, 2, 22);
        aimAt(player, boar);
        var gun = new ItemStack(HogItems.SILVER_BOLT_GUN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().setItem(1, new ItemStack(HogItems.IRON_BOLT.get()));
        player.getInventory().setItem(2, new ItemStack(HogItems.IRON_BOLT.get()));
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        // Exercise the vanilla full-use callback that the original gun omitted.
        gun.getItem().finishUsingItem(gun, helper.getLevel(), player);
        helper.assertValueEqual(HogGunItem.countAmmo(player), 0, "charged silver shot did not consume both split stacks");
        helper.assertTrue(boar.getHealth() < boar.getMaxHealth(), "completed silver charge never fired");
        helper.assertTrue(boar.isRooted(), "charged silver shot did not pin a non-boss hog");
        helper.assertValueEqual(gun.getDamageValue(), 1, "charged shot durability");
        gun.getItem().releaseUsing(gun, helper.getLevel(), player, 0);
        helper.assertValueEqual(gun.getDamageValue(), 1, "release after completion fired twice");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void insufficientChargedAmmoIsNotPartiallyConsumed(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var gun = new ItemStack(HogItems.SILVER_BOLT_GUN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        player.getInventory().setItem(1, new ItemStack(HogItems.IRON_BOLT.get()));
        gun.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        gun.getItem().finishUsingItem(gun, helper.getLevel(), player);
        helper.assertValueEqual(HogGunItem.countAmmo(player), 1, "failed two-bolt shot consumed its only bolt");
        helper.assertValueEqual(gun.getDamageValue(), 0, "failed charge damaged the gun");
        helper.assertValueEqual(HogAttachments.get(player).noise(), 0, "failed charge emitted firing noise");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void bandageUsesVanillaCompletionAndDamageCancelsTreatment(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 16);
        try {
            var data = HogAttachments.get(player);
            data.setWounds(2);
            HogHunterPlayerEvents.updateInjuryModifiers(player, data);
            var bandages = new ItemStack(HogItems.BANDAGE.get(), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, bandages);
            bandages.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            playerTicks(player, 30);
            helper.assertValueEqual(data.wounds(), 2, "bandage healed before the use duration completed");
            helper.assertValueEqual(bandages.getCount(), 3, "bandage consumed before completing");
            playerTicks(player, 40);
            helper.assertValueEqual(data.wounds(), 1, "vanilla item-use completion did not remove one wound");
            helper.assertValueEqual(bandages.getCount(), 2, "one completed bandage must consume exactly one item");
            bandages.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            playerTicks(player, 10);
            player.invulnerableTime = 0;
            helper.assertTrue(player.hurt(player.damageSources().generic(), 1.0F), "cancellation test damage did not land");
            helper.assertTrue(!player.isUsingItem(), "damage did not interrupt medical use");
            playerTicks(player, 80);
            helper.assertValueEqual(data.wounds(), 1, "interrupted treatment healed a wound");
            helper.assertValueEqual(bandages.getCount(), 2, "interrupted treatment consumed an item");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void medkitRestoresHealthCapBeforeHealingAndRecordsTreatment(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 16);
        try {
            var data = HogAttachments.get(player);
            data.setWounds(1);
            data.setFracture(1);
            HogHunterPlayerEvents.updateInjuryModifiers(player, data);
            player.setHealth(player.getMaxHealth());
            helper.assertValueEqual(player.getMaxHealth(), 18.0F, "wound health penalty fixture");
            var medkit = new ItemStack(HogItems.FIELD_MEDKIT.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, medkit);
            medkit.getItem().finishUsingItem(medkit, helper.getLevel(), player);
            helper.assertValueEqual(data.wounds(), 0, "medkit did not treat wound");
            helper.assertValueEqual(data.fracture(), 0, "medkit did not treat fracture");
            helper.assertValueEqual(data.fractureTreatments(), 1, "medkit did not record permanent treatment evidence");
            helper.assertValueEqual(player.getMaxHealth(), 20.0F, "medkit left stale wound modifier");
            helper.assertValueEqual(player.getHealth(), 20.0F, "medkit healing was truncated by the old wound health cap");
            helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS), "medkit did not apply recovery weakness");
            helper.assertTrue(medkit.isEmpty(), "completed medkit was not consumed");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void oilRefillAndLanternToggleDoNotWasteFuel(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var data = HogAttachments.get(player);
        var cans = new ItemStack(HogItems.OIL_CAN.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, cans);
        var rejected = cans.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(rejected.getResult() == InteractionResult.FAIL, "full tank accepted unnecessary oil use");
        cans.getItem().finishUsingItem(cans, helper.getLevel(), player);
        helper.assertValueEqual(cans.getCount(), 2, "full tank consumed an oil can");
        data.setOil(60);
        cans.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        cans.getItem().finishUsingItem(cans, helper.getLevel(), player);
        helper.assertValueEqual(data.oil(), 100, "oil can did not cap its 50-point refill at 100");
        helper.assertValueEqual(cans.getCount(), 1, "refill consumed wrong item count");
        var lantern = new ItemStack(HogItems.FIELD_LANTERN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, lantern);
        lantern.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(data.lanternLit(), "lantern did not switch on");
        helper.assertValueEqual(data.oil(), 100, "toggling lantern consumed fuel immediately");
        lantern.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(data.lanternLit(), "toggle cooldown was ignored");
        player.getCooldowns().removeCooldown(lantern.getItem());
        lantern.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!data.lanternLit(), "lantern did not switch off");
        player.getCooldowns().removeCooldown(lantern.getItem());
        data.setOil(0);
        lantern.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!data.lanternLit(), "empty lantern switched on");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void harpoonCannotDamageHogsBehindWalls(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        HogTestSupport.position(helper, player, 16, 2, 16);
        var boar = HogTestSupport.hog(helper, HogEntities.BOAR_HOG.get(), 16, 2, 23);
        aimAt(player, boar);
        var harpoon = new ItemStack(HogItems.MINE_HARPOON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, harpoon);
        for (int x = 14; x <= 18; x++) for (int y = 1; y <= 4; y++) helper.setBlock(x, y, 19, Blocks.GLASS);
        harpoon.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(boar.getHealth(), boar.getMaxHealth(), "harpoon hit a hog through a wall");
        helper.assertValueEqual(harpoon.getDamageValue(), 0, "invalid glass anchor spent durability");
        for (int x = 14; x <= 18; x++) for (int y = 1; y <= 4; y++) helper.setBlock(x, y, 19, Blocks.AIR);
        harpoon.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(boar.getHealth() < boar.getMaxHealth(), "unobstructed harpoon failed to damage hog");
        helper.assertTrue(boar.getDeltaMovement().dot(player.position().subtract(boar.position())) > 0,
                "harpoon did not pull its hit hog toward the player");
        helper.assertValueEqual(harpoon.getDamageValue(), 1, "successful harpoon durability");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 300)
    public static void saltPlacesAcrossTunnelSuppressesAbilityAndExpires(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        HogTestSupport.position(helper, player, 16, 2, 13);
        player.setYRot(0);
        var shaker = new ItemStack(HogItems.SALT_SHAKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, shaker);
        var floor = new BlockPos(16, 1, 16);
        helper.assertTrue(shaker.useOn(context(helper, player, floor)).consumesAction(), "salt shaker placement failed");
        for (int x = 15; x <= 17; x++) helper.assertBlockPresent(HogBlocks.SALT_LINE.get(), new BlockPos(x, 2, 16));
        helper.assertValueEqual(shaker.getDamageValue(), 1, "three salt blocks should cost one shaker charge");
        var boar = HogTestSupport.hog(helper, HogEntities.BOAR_HOG.get(), 16.5, 2, 16.5);
        boar.setTarget(player);
        var absolute = helper.absolutePos(new BlockPos(16, 2, 16));
        var state = helper.getLevel().getBlockState(absolute);
        state.entityInside(helper.getLevel(), absolute, boar);
        helper.assertTrue(boar.areAbilitiesSuppressed(), "salt contact did not suppress hog abilities");
        helper.assertTrue(boar.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "salt contact did not slow the hog");
        float after = boar.getHealth();
        boar.invulnerableTime = 0;
        state.entityInside(helper.getLevel(), absolute, boar);
        helper.assertValueEqual(boar.getHealth(), after, "salt repeated damage without its damage cooldown");
        HogTestSupport.stationaryTicks(boar, 25);
        helper.assertTrue(!boar.isCharging(), "salt did not interrupt the boar's charge");
        boar.discard();
        helper.runAfterDelay(250, () -> {
            for (int x = 15; x <= 17; x++) helper.assertBlockNotPresent(HogBlocks.SALT_LINE.get(), new BlockPos(x, 2, 16));
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 100)
    public static void placedSnarePersistsAndTripsOnLaterHog(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 12, 2, 16);
        var snares = new ItemStack(HogItems.BAITED_SNARE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, snares);
        var floor = new BlockPos(16, 1, 16);
        helper.assertTrue(snares.useOn(context(helper, player, floor)).consumesAction(), "snare placement failed");
        var placed = floor.above();
        helper.assertValueEqual(snares.getCount(), 1, "snare placement consumption");
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(placed)) != null, "snare has no persistent block entity");
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(HogBlocks.BAITED_SNARE.get(), placed);
            var boar = HogTestSupport.hog(helper, HogEntities.BOAR_HOG.get(), 16.5, 2, 19.5);
            helper.succeedWhen(() -> {
                helper.assertBlockNotPresent(HogBlocks.BAITED_SNARE.get(), placed);
                helper.assertTrue(boar.isRooted(), "snare failed to pin a hog arriving after placement");
                helper.assertTrue(boar.getHealth() < boar.getMaxHealth(), "snare did not damage its victim");
                player.discard();
            });
        });
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void gateRequiresProgressionAndReallyOpensCollision(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 13, 2, 16);
        var relative = new BlockPos(16, 2, 16);
        var absolute = helper.absolutePos(relative);
        helper.setBlock(relative, HogBlocks.DEPTH_GATE.get());
        try {
            helper.useBlock(relative, player);
            var closed = helper.getBlockState(relative);
            helper.assertTrue(!closed.getValue(BlockStateProperties.OPEN), "locked gate opened without evidence");
            helper.assertTrue(!closed.getCollisionShape(helper.getLevel(), absolute).isEmpty(), "closed gate has no collision");
            HogAttachments.get(player).setUnlockedTier(DepthGateBlock.requiredTier(absolute));
            helper.useBlock(relative, player);
            var opened = helper.getBlockState(relative);
            helper.assertTrue(opened.getValue(BlockStateProperties.OPEN), "unlocked gate did not open");
            helper.assertTrue(opened.getCollisionShape(helper.getLevel(), absolute).isEmpty(), "visually open gate still blocks movement");
            HogTestSupport.position(helper, player, 16.5, 2, 16.5);
            helper.useBlock(relative, player);
            helper.assertTrue(helper.getBlockState(relative).getValue(BlockStateProperties.OPEN), "gate closed through an occupant");
            HogTestSupport.position(helper, player, 13, 2, 16);
            helper.useBlock(relative, player);
            helper.assertTrue(!helper.getBlockState(relative).getValue(BlockStateProperties.OPEN), "clear gate did not close");
            helper.succeed();
        } finally {
            player.discard();
        }
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void surfaceAltarConsumesEvidenceAtomicallyAndKeepsTrophy(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 16);
        BlockPos base = helper.absolutePos(new BlockPos(16, 2, 16));
        BlockPos altar = new BlockPos(base.getX(), Math.max(64, base.getY()), base.getZ());
        var previous = helper.getLevel().getBlockState(altar);
        helper.getLevel().setBlockAndUpdate(altar, HogBlocks.ROOT_ALTAR.get().defaultBlockState());
        player.setPos(altar.getX() + 2, altar.getY() + 1, altar.getZ() + 0.5D);
        try {
            var data = HogAttachments.get(player);
            useAltar(helper, player, altar);
            helper.assertValueEqual(data.unlockedTier(), 0, "empty altar granted progression");
            var tissue = new ItemStack(HogItems.CORRUPTED_TISSUE.get(), 3);
            player.getInventory().setItem(1, tissue);
            useAltar(helper, player, altar);
            helper.assertValueEqual(data.unlockedTier(), 1, "three tissue did not unlock first tier");
            helper.assertTrue(tissue.isEmpty(), "first extraction did not spend tissue");
            var tooth = new ItemStack(HogItems.HOOK_TOOTH.get());
            player.getInventory().setItem(1, tooth);
            useAltar(helper, player, altar);
            helper.assertValueEqual(tooth.getCount(), 1, "incomplete sample pair was partially consumed");
            helper.assertValueEqual(data.unlockedTier(), 1, "incomplete sample pair granted progression");
            var sac = new ItemStack(HogItems.SPORE_SAC.get());
            player.getInventory().setItem(2, sac);
            useAltar(helper, player, altar);
            helper.assertValueEqual(data.unlockedTier(), 2, "complete samples did not unlock tier two");
            helper.assertTrue(tooth.isEmpty() && sac.isEmpty(), "sample extraction did not spend both items");
            var plate = new ItemStack(HogItems.IRON_PLATE.get());
            player.getInventory().setItem(1, plate);
            useAltar(helper, player, altar);
            helper.assertValueEqual(plate.getCount(), 1, "untreated-fracture gate spent its plate");
            data.recordFractureTreatment();
            useAltar(helper, player, altar);
            helper.assertValueEqual(data.unlockedTier(), 3, "plate plus treatment did not unlock tier three");
            var heart = new ItemStack(HogItems.ROOT_HEART.get());
            player.getInventory().setItem(1, heart);
            useAltar(helper, player, altar);
            helper.assertValueEqual(data.unlockedTier(), 5, "extracted root heart did not complete progression");
            helper.assertValueEqual(heart.getCount(), 1, "extraction destroyed the root heart trophy");
            int evidence = data.evidence();
            useAltar(helper, player, altar);
            helper.assertValueEqual(data.evidence(), evidence, "completed altar repeatedly awarded evidence");
            helper.succeed();
        } finally {
            helper.getLevel().setBlockAndUpdate(altar, previous);
            player.discard();
        }
    }

    private static UseOnContext context(GameTestHelper helper, Player player, BlockPos relative) {
        var absolute = helper.absolutePos(relative);
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5D, 0), Direction.UP, absolute, false));
    }

    private static void aimAt(Player player, LivingEntity entity) {
        Vec3 difference = entity.getBoundingBox().getCenter().subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-difference.x, difference.z)));
        player.setXRot((float) -Math.toDegrees(Math.atan2(difference.y, difference.horizontalDistance())));
    }

    private static void playerTicks(ServerPlayer player, int count) {
        for (int tick = 0; tick < count; tick++) {
            player.setOnGround(true);
            player.setDeltaMovement(Vec3.ZERO);
            player.tick();
            player.doTick();
        }
    }

    private static void useAltar(GameTestHelper helper, Player player, BlockPos absolute) {
        var state = helper.getLevel().getBlockState(absolute);
        state.useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }
}
