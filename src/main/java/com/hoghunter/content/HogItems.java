package com.hoghunter.content;

import com.hoghunter.HogHunterMod;
import com.hoghunter.item.HogConsumableItem;
import com.hoghunter.item.HogGunItem;
import com.hoghunter.item.BaitedSnareItem;
import com.hoghunter.item.FieldLanternItem;
import com.hoghunter.item.MineHarpoonItem;
import com.hoghunter.item.OilCanItem;
import com.hoghunter.item.SaltShakerItem;
import com.hoghunter.item.HogMaterialItem;
import com.hoghunter.item.HogBlockItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;
import java.util.Map;

/** Item registry and stable handles used by the other common lanes. */
public final class HogItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HogHunterMod.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, HogHunterMod.MOD_ID);
    public static final Holder<ArmorMaterial> HOGHIDE_MATERIAL = ARMOR_MATERIALS.register("hoghide", () ->
            material(3, 0.0F, () -> Ingredient.of(Items.LEATHER), ArmorMaterials.LEATHER.value().layers()));
    public static final Holder<ArmorMaterial> IRONBACK_MATERIAL = ARMOR_MATERIALS.register("ironback", () ->
            material(5, 0.1F, () -> Ingredient.of(HogItems.IRON_PLATE.get()), ArmorMaterials.IRON.value().layers()));

    public static final DeferredItem<Item> SALT = materialItem("salt");
    public static final DeferredItem<Item> PURIFIED_TUSK = materialItem("purified_tusk");
    public static final DeferredItem<Item> CORRUPTED_TISSUE = materialItem("corrupted_tissue");
    public static final DeferredItem<Item> HOOK_TOOTH = materialItem("hook_tooth");
    public static final DeferredItem<Item> SPORE_SAC = materialItem("spore_sac");
    public static final DeferredItem<Item> IRON_PLATE = materialItem("iron_plate");
    public static final DeferredItem<Item> MARKED_TUSK = materialItem("marked_tusk");
    public static final DeferredItem<Item> ROOT_HEART = materialItem("root_heart");

    public static final DeferredItem<Item> IRON_BOLT = ITEMS.registerItem("iron_bolt",
            Item::new, new Item.Properties().stacksTo(32));
    public static final DeferredItem<Item> BOLT_GUN = ITEMS.registerItem("bolt_gun",
            p -> new HogGunItem(p.durability(128), 9.0F, 2.0F, 24.0D, 20, 10, false), new Item.Properties());
    public static final DeferredItem<Item> SILVER_BOLT_GUN = ITEMS.registerItem("silver_bolt_gun",
            p -> new HogGunItem(p.durability(192), 14.0F, 5.0F, 28.0D, 24, 30, true), new Item.Properties());
    public static final DeferredItem<Item> BAITED_SNARE = ITEMS.registerItem("baited_snare",
            p -> new BaitedSnareItem(p.stacksTo(16)), new Item.Properties());
    public static final DeferredItem<Item> FIELD_LANTERN = ITEMS.registerItem("field_lantern",
            p -> new FieldLanternItem(p.stacksTo(1)), new Item.Properties());
    public static final DeferredItem<Item> MINE_HARPOON = ITEMS.registerItem("mine_harpoon",
            p -> new MineHarpoonItem(p.durability(96)), new Item.Properties());
    public static final DeferredItem<Item> SALT_SHAKER = ITEMS.registerItem("salt_shaker",
            p -> new SaltShakerItem(p.durability(32)), new Item.Properties());

    public static final DeferredItem<Item> OIL_CAN = ITEMS.registerItem("oil_can",
            p -> new OilCanItem(p.stacksTo(4)), new Item.Properties());
    public static final DeferredItem<Item> BANDAGE = ITEMS.registerItem("bandage",
            p -> new HogConsumableItem(p.stacksTo(8), 60, HogConsumableItem.Treatment.BANDAGE), new Item.Properties());
    public static final DeferredItem<Item> SPLINT = ITEMS.registerItem("splint",
            p -> new HogConsumableItem(p.stacksTo(4), 100, HogConsumableItem.Treatment.SPLINT), new Item.Properties());
    public static final DeferredItem<Item> FIELD_MEDKIT = ITEMS.registerItem("field_medkit",
            p -> new HogConsumableItem(p.stacksTo(1), 80, HogConsumableItem.Treatment.MEDKIT), new Item.Properties());
    public static final DeferredItem<Item> SALT_RATION = ITEMS.registerItem("salt_ration",
            p -> new HogConsumableItem(p.stacksTo(16), 32, HogConsumableItem.Treatment.RATION), new Item.Properties());

    public static final DeferredItem<ArmorItem> HOGHIDE_HELMET = armor("hoghide_helmet", ArmorItem.Type.HELMET, 165);
    public static final DeferredItem<ArmorItem> HOGHIDE_CHESTPLATE = armor("hoghide_chestplate", ArmorItem.Type.CHESTPLATE, 240);
    public static final DeferredItem<ArmorItem> HOGHIDE_LEGGINGS = armor("hoghide_leggings", ArmorItem.Type.LEGGINGS, 225);
    public static final DeferredItem<ArmorItem> HOGHIDE_BOOTS = armor("hoghide_boots", ArmorItem.Type.BOOTS, 195);
    public static final DeferredItem<ArmorItem> HUNTER_COAT_HELMET = HOGHIDE_HELMET;
    public static final DeferredItem<ArmorItem> HUNTER_COAT_CHESTPLATE = HOGHIDE_CHESTPLATE;
    public static final DeferredItem<ArmorItem> HUNTER_COAT_LEGGINGS = HOGHIDE_LEGGINGS;
    public static final DeferredItem<ArmorItem> HUNTER_COAT_BOOTS = HOGHIDE_BOOTS;
    public static final DeferredItem<ArmorItem> IRONBACK_HARNESS = ITEMS.registerItem("ironback_harness",
            p -> new ArmorItem(IRONBACK_MATERIAL, ArmorItem.Type.CHESTPLATE, p.durability(320)), new Item.Properties());

    private static DeferredItem<ArmorItem> armor(String id, ArmorItem.Type type, int durability) {
        return ITEMS.registerItem(id, p -> new ArmorItem(HOGHIDE_MATERIAL, type, p.durability(durability)), new Item.Properties());
    }

    private static ArmorMaterial material(int chestArmor, float knockback,
                                           java.util.function.Supplier<Ingredient> repair, List<ArmorMaterial.Layer> layers) {
        return new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, chestArmor,
                ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1, ArmorItem.Type.BODY, chestArmor),
                15, SoundEvents.ARMOR_EQUIP_LEATHER, repair,
                layers, 0.0F, knockback);
    }

    private static DeferredItem<Item> materialItem(String id) {
        return ITEMS.registerItem(id, p -> new HogMaterialItem(p, "tooltip.hoghunter." + id), new Item.Properties());
    }

    // Block items. Without these the blocks register but can never be placed, and the
    // data/hoghunter/loot_table/blocks/*.json tables have no item id to resolve.
    public static final DeferredItem<Item> CORRUPTED_ORE_BLOCK =
            ITEMS.registerItem("corrupted_ore", p -> new BlockItem(HogBlocks.CORRUPTED_ORE.get(), p), new Item.Properties());
    public static final DeferredItem<Item> DEPTH_GATE_BLOCK =
            ITEMS.registerItem("depth_gate", p -> new HogBlockItem(HogBlocks.DEPTH_GATE.get(), p,
                    "tooltip.hoghunter.depth_gate"), new Item.Properties());
    public static final DeferredItem<Item> HOG_NEST_BLOCK =
            ITEMS.registerItem("hog_nest", p -> new BlockItem(HogBlocks.HOG_NEST.get(), p), new Item.Properties());
    public static final DeferredItem<Item> SALT_LINE_BLOCK =
            ITEMS.registerItem("salt_line", p -> new BlockItem(HogBlocks.SALT_LINE.get(), p), new Item.Properties());
    public static final DeferredItem<Item> ROOT_ALTAR_BLOCK =
            ITEMS.registerItem("root_altar", p -> new HogBlockItem(HogBlocks.ROOT_ALTAR.get(), p,
                    "tooltip.hoghunter.root_altar.surface", "tooltip.hoghunter.root_altar.depths"), new Item.Properties());

    /** Called once by the mod's common registry bootstrap. */
    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
        ITEMS.register(modBus);
    }

    /** Registers this lane's complete content surface in one call. */
    public static void registerAll(IEventBus modBus) {
        register(modBus);
        HogSounds.register(modBus);
        HogCreativeTabs.register(modBus);
    }

    private HogItems() {}
}
