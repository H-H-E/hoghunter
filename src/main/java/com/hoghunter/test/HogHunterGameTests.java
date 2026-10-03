package com.hoghunter.test;

import com.hoghunter.content.HogBlockEntities;
import com.hoghunter.content.HogBlocks;
import com.hoghunter.content.HogEntities;
import com.hoghunter.entity.HogEntity;
import com.hoghunter.item.HogWeaponDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Runtime proof for the registered hog entities and blocks. */
@GameTestHolder("hoghunter")
@PrefixGameTestTemplate(false)
public final class HogHunterGameTests {
    private HogHunterGameTests() {
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 100, setupTicks = 1, required = true)
    public static void hogRegistryAndAttributes(GameTestHelper helper) {
        assertHog(helper, "boar_hog", HogEntities.BOAR_HOG, 24.0D);
        assertHog(helper, "spore_hog", HogEntities.SPORE_HOG, 30.0D);
        assertHog(helper, "hook_hog", HogEntities.HOOK_HOG, 20.0D);
        assertHog(helper, "screecher_hog", HogEntities.SCREECHER_HOG, 18.0D);
        assertHog(helper, "ironback_hog", HogEntities.IRONBACK_HOG, 48.0D);
        assertHog(helper, "mire_hog", HogEntities.MIRE_HOG, 34.0D);
        assertHog(helper, "rootmother", HogEntities.ROOTMOTHER, 260.0D);
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 100, setupTicks = 1, required = true)
    public static void hogAiTicks(GameTestHelper helper) {
        HogEntity hog = helper.spawn(HogEntities.BOAR_HOG.get(), new BlockPos(0, 2, 0));
        helper.assertTrue(hog.isAlive(), "boar_hog was not alive after spawning");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(hog.isAlive(), "boar_hog died while ticking its AI");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 100, setupTicks = 1, required = true)
    public static void hogWeaponDamageHurtsBoar(GameTestHelper helper) {
        HogEntity boar = helper.spawn(HogEntities.BOAR_HOG.get(), new BlockPos(0, 2, 0));
        Player attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        float before = boar.getHealth();
        helper.assertTrue(HogWeaponDamage.apply(boar, attacker, 1.0F, 0.0F),
                "HogWeaponDamage did not apply to boar_hog");
        helper.assertTrue(boar.getHealth() < before,
                "boar_hog health did not decrease after HogWeaponDamage");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 100, setupTicks = 1, required = true)
    public static void hogBlocksPlaceAndRemove(GameTestHelper helper) {
        assertPlaceAndRemove(helper, HogBlocks.CORRUPTED_ORE.get(), new BlockPos(0, 2, 0));
        assertPlaceAndRemove(helper, HogBlocks.DEPTH_GATE.get(), new BlockPos(1, 2, 0));
        assertPlaceAndRemove(helper, HogBlocks.HOG_NEST.get(), new BlockPos(2, 2, 0));
        assertPlaceAndRemove(helper, HogBlocks.SALT_LINE.get(), new BlockPos(3, 2, 0));
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 100, setupTicks = 1, required = true)
    public static void hogBlockEntitiesInstantiate(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(0, 2, 0);
        helper.setBlock(relativePos, HogBlocks.HOG_NEST.get());
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(relativePos));
        helper.assertTrue(entity != null, "hog_nest block entity was not created in the level");
        helper.assertTrue(entity.getType() == HogBlockEntities.HOG_NEST.get(),
                "hog_nest created the wrong block entity type");
        helper.succeed();
    }

    private static <T extends LivingEntity> void assertHog(
            GameTestHelper helper, String name, DeferredHolder<EntityType<?>, EntityType<T>> holder, double expectedHealth) {
        EntityType<T> type = holder.get();
        helper.assertTrue(type != null, name + " registry holder resolved to null");
        AttributeSupplier supplier = DefaultAttributes.getSupplier(type);
        helper.assertTrue(supplier != null, name + " has no registered attribute supplier");
        helper.assertTrue(supplier.hasAttribute(Attributes.MAX_HEALTH), name + " has no max-health attribute");
        helper.assertValueEqual(supplier.getBaseValue(Attributes.MAX_HEALTH), expectedHealth,
                name + " max health");
        T entity = type.create(helper.getLevel());
        helper.assertTrue(entity != null, name + " entity factory returned null");
        helper.assertValueEqual((double) entity.getMaxHealth(), expectedHealth, name + " entity max health");
    }

    private static void assertPlaceAndRemove(GameTestHelper helper, Block block, BlockPos relativePos) {
        helper.setBlock(relativePos, block);
        helper.assertBlockPresent(block, relativePos);
        helper.getLevel().destroyBlock(helper.absolutePos(relativePos), false);
        helper.assertBlockNotPresent(block, relativePos);
    }
}
