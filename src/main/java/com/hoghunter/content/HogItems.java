package com.hoghunter.content;

import com.hoghunter.HogHunterMod;
import com.hoghunter.item.HogConsumableItem;
import com.hoghunter.item.HogGunItem;
import com.hoghunter.item.BaitedSnareItem;
import com.hoghunter.item.FieldLanternItem;
import com.hoghunter.item.MineHarpoonItem;
import com.hoghunter.item.OilCanItem;
import com.hoghunter.item.SaltShakerItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item registry and stable handles used by the other common lanes. */
public final class HogItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HogHunterMod.MOD_ID);

    public static final DeferredItem<Item> IRON_BOLT = ITEMS.registerItem("iron_bolt",
            Item::new, new Item.Properties().stacksTo(32));
    public static final DeferredItem<Item> BOLT_GUN = ITEMS.registerItem("bolt_gun",
            p -> new HogGunItem(p.durability(128), 9.0F, 2.0F, 24.0D, 20, 10, false), new Item.Properties());
    public static final DeferredItem<Item> SILVER_BOLT_GUN = ITEMS.registerItem("silver_bolt_gun",
            p -> new HogGunItem(p.durability(192), 14.0F, 5.0F, 28.0D, 24, 30, true), new Item.Properties());
    public static final DeferredItem<Item> BAITED_SNARE = ITEMS.registerItem("baited_snare",
            p -> new BaitedSnareItem(p.stacksTo(16)), new Item.Properties());
    public static final DeferredItem<Item> FIELD_LANTERN = ITEMS.registerItem("field_lantern",
            p -> new FieldLanternItem(p.durability(256)), new Item.Properties());
    public static final DeferredItem<Item> MINE_HARPOON = ITEMS.registerItem("mine_harpoon",
            p -> new MineHarpoonItem(p.durability(96)), new Item.Properties());
    public static final DeferredItem<Item> SALT_SHAKER = ITEMS.registerItem("salt_shaker",
            p -> new SaltShakerItem(p.durability(32)), new Item.Properties());

    public static final DeferredItem<Item> OIL_CAN = ITEMS.registerItem("oil_can",
            p -> new OilCanItem(p.stacksTo(4)), new Item.Properties());
    public static final DeferredItem<Item> BANDAGE = ITEMS.registerItem("bandage",
            p -> new HogConsumableItem(p.stacksTo(8), 60), new Item.Properties());
    public static final DeferredItem<Item> SPLINT = ITEMS.registerItem("splint",
            p -> new HogConsumableItem(p.stacksTo(4), 100), new Item.Properties());
    public static final DeferredItem<Item> FIELD_MEDKIT = ITEMS.registerItem("field_medkit",
            p -> new HogConsumableItem(p.stacksTo(1), 80), new Item.Properties());
    public static final DeferredItem<Item> SALT_RATION = ITEMS.registerItem("salt_ration",
            p -> new HogConsumableItem(p.stacksTo(16), 32), new Item.Properties());

    public static final DeferredItem<ArmorItem> HOGHIDE_HELMET = armor("hoghide_helmet", ArmorItem.Type.HELMET, 165);
    public static final DeferredItem<ArmorItem> HOGHIDE_CHESTPLATE = armor("hoghide_chestplate", ArmorItem.Type.CHESTPLATE, 240);
    public static final DeferredItem<ArmorItem> HOGHIDE_LEGGINGS = armor("hoghide_leggings", ArmorItem.Type.LEGGINGS, 225);
    public static final DeferredItem<ArmorItem> HOGHIDE_BOOTS = armor("hoghide_boots", ArmorItem.Type.BOOTS, 195);
    public static final DeferredItem<ArmorItem> HUNTER_COAT_HELMET = HOGHIDE_HELMET;
    public static final DeferredItem<ArmorItem> HUNTER_COAT_CHESTPLATE = HOGHIDE_CHESTPLATE;
    public static final DeferredItem<ArmorItem> HUNTER_COAT_LEGGINGS = HOGHIDE_LEGGINGS;
    public static final DeferredItem<ArmorItem> HUNTER_COAT_BOOTS = HOGHIDE_BOOTS;
    public static final DeferredItem<ArmorItem> IRONBACK_HARNESS = armor("ironback_harness", ArmorItem.Type.CHESTPLATE, 240);

    private static DeferredItem<ArmorItem> armor(String id, ArmorItem.Type type, int durability) {
        return ITEMS.registerItem(id, p -> new ArmorItem(ArmorMaterials.LEATHER, type, p.durability(durability)), new Item.Properties());
    }

    // Block items. Without these the blocks register but can never be placed, and the
    // data/hoghunter/loot_table/blocks/*.json tables have no item id to resolve.
    public static final DeferredItem<Item> CORRUPTED_ORE_BLOCK =
            ITEMS.registerItem("corrupted_ore", p -> new BlockItem(HogBlocks.CORRUPTED_ORE.get(), p), new Item.Properties());
    public static final DeferredItem<Item> DEPTH_GATE_BLOCK =
            ITEMS.registerItem("depth_gate", p -> new BlockItem(HogBlocks.DEPTH_GATE.get(), p), new Item.Properties());
    public static final DeferredItem<Item> HOG_NEST_BLOCK =
            ITEMS.registerItem("hog_nest", p -> new BlockItem(HogBlocks.HOG_NEST.get(), p), new Item.Properties());
    public static final DeferredItem<Item> SALT_LINE_BLOCK =
            ITEMS.registerItem("salt_line", p -> new BlockItem(HogBlocks.SALT_LINE.get(), p), new Item.Properties());

    /** Called once by the mod's common registry bootstrap. */
    public static void register(IEventBus modBus) { ITEMS.register(modBus); }

    /** Registers this lane's complete content surface in one call. */
    public static void registerAll(IEventBus modBus) {
        ITEMS.register(modBus);
        HogSounds.register(modBus);
        HogCreativeTabs.register(modBus);
    }

    private HogItems() {}
}
