package com.hoghunter.test;

import com.hoghunter.HogHunterMod;
import com.hoghunter.content.HogBlocks;
import com.hoghunter.content.HogItems;
import com.hoghunter.item.MineHarpoonItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Real datapack loading: these tests catch resources silently ignored by old directory names. */
@GameTestHolder("hoghunter")
@PrefixGameTestTemplate(false)
public final class HogResourceGameTests {
    private HogResourceGameTests() {}

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void survivalRecipesLoadAndCraftWithRealTags(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : List.of("bolt_gun", "silver_bolt_gun", "iron_bolt", "field_lantern", "mine_harpoon",
                "baited_snare", "salt_shaker", "oil_can", "bandage", "splint", "field_medkit", "salt_ration",
                "hunter_coat_helmet", "hunter_coat_chestplate", "hunter_coat_leggings", "hunter_coat_boots",
                "ironback_harness", "salt_from_kelp", "purified_tusk", "root_altar", "depth_gate")) {
            var recipe = recipes.byKey(HogHunterMod.id(id));
            helper.assertTrue(recipe.isPresent(), "server did not load survival recipe " + id);
            helper.assertTrue(!recipe.orElseThrow().value().getResultItem(helper.getLevel().registryAccess()).isEmpty(),
                    "recipe " + id + " resolves to an empty output");
        }
        var gunGrid = CraftingInput.of(3, 3, List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.STRING), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.STICK),
                new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY));
        var gun = recipes.getRecipeFor(RecipeType.CRAFTING, gunGrid, helper.getLevel());
        helper.assertTrue(gun.isPresent(), "bolt gun crafting grid did not match any loaded recipe");
        helper.assertTrue(gun.orElseThrow().value().assemble(gunGrid, helper.getLevel().registryAccess()).is(HogItems.BOLT_GUN.get()),
                "bolt gun grid crafted the wrong item");
        var purification = CraftingInput.of(3, 1, List.of(new ItemStack(HogItems.HOOK_TOOTH.get()),
                new ItemStack(HogItems.SALT.get()), new ItemStack(Items.AMETHYST_SHARD)));
        var purified = recipes.getRecipeFor(RecipeType.CRAFTING, purification, helper.getLevel());
        helper.assertTrue(purified.isPresent(), "real salt tag does not complete purified tusk recipe");
        helper.assertTrue(purified.orElseThrow().value().assemble(purification, helper.getLevel().registryAccess()).is(HogItems.PURIFIED_TUSK.get()),
                "purified tusk recipe output is incorrect");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void runtimeTagsAndWorldgenResolveRealContent(GameTestHelper helper) {
        var salt = TagKey.create(Registries.ITEM, HogHunterMod.id("salt"));
        var tusk = TagKey.create(Registries.ITEM, HogHunterMod.id("purified_tusk"));
        helper.assertTrue(new ItemStack(HogItems.SALT.get()).is(salt), "real salt is absent from its runtime ingredient tag");
        helper.assertTrue(!new ItemStack(Items.SUGAR).is(salt), "salt tag still accepts placeholder sugar");
        helper.assertTrue(new ItemStack(HogItems.PURIFIED_TUSK.get()).is(tusk), "real purified tusk is absent from its ingredient tag");
        helper.assertTrue(!new ItemStack(Items.BONE).is(tusk), "purified tusk tag still accepts placeholder bone");
        helper.assertTrue(Blocks.IRON_BLOCK.defaultBlockState().is(MineHarpoonItem.ANCHORS), "harpoon anchor tag did not load");
        var registry = helper.getLevel().registryAccess();
        helper.assertTrue(registry.registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(HogHunterMod.id("corrupted_ore")),
                "configured corrupted ore feature did not load");
        helper.assertTrue(registry.registryOrThrow(Registries.PLACED_FEATURE).containsKey(HogHunterMod.id("corrupted_ore")),
                "placed corrupted ore feature did not load");
        helper.succeed();
    }

    @GameTest(templateNamespace = "hoghunter", template = "empty", timeoutTicks = 80)
    public static void miningOreDropsTissueAndSilkTouchKeepsOre(GameTestHelper helper) {
        var player = HogTestSupport.player(helper, 16, 2, 16);
        var ordinary = new BlockPos(17, 2, 16);
        var silk = new BlockPos(18, 2, 16);
        helper.setBlock(ordinary, HogBlocks.CORRUPTED_ORE.get());
        helper.setBlock(silk, HogBlocks.CORRUPTED_ORE.get());
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE_PICKAXE));
            helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(ordinary)), "survival stone pickaxe failed to mine ore");
            int tissue = helper.getEntities(EntityType.ITEM).stream().map(entity -> entity.getItem())
                    .filter(stack -> stack.is(HogItems.CORRUPTED_TISSUE.get())).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(tissue >= 1 && tissue <= 2, "ordinary ore mining did not drop one or two tissue");
            var pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, pick);
            helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(silk)), "silk touch pickaxe failed to mine ore");
            helper.assertTrue(helper.getEntities(EntityType.ITEM).stream().anyMatch(entity -> entity.getItem().is(HogItems.CORRUPTED_ORE_BLOCK.get())),
                    "silk touch did not preserve the corrupted ore block");
            helper.succeed();
        } finally {
            player.discard();
        }
    }
}
