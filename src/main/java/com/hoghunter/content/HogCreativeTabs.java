package com.hoghunter.content;

import com.hoghunter.HogHunterMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HogCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, HogHunterMod.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HUNTER_TAB = TABS.register("hunter", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup.hoghunter.hunter"))
                    .icon(() -> new ItemStack(HogItems.BOLT_GUN.get()))
                    .displayItems((params, output) -> {
                        output.accept(HogItems.BOLT_GUN.get()); output.accept(HogItems.SILVER_BOLT_GUN.get());
                        output.accept(HogItems.IRON_BOLT.get()); output.accept(HogItems.BAITED_SNARE.get());
                        output.accept(HogItems.FIELD_LANTERN.get()); output.accept(HogItems.MINE_HARPOON.get());
                        output.accept(HogItems.SALT_SHAKER.get()); output.accept(HogItems.OIL_CAN.get());
                        output.accept(HogItems.BANDAGE.get()); output.accept(HogItems.SPLINT.get());
                        output.accept(HogItems.FIELD_MEDKIT.get()); output.accept(HogItems.SALT_RATION.get());
                        output.accept(HogItems.HUNTER_COAT_HELMET.get()); output.accept(HogItems.HUNTER_COAT_CHESTPLATE.get());
                        output.accept(HogItems.HUNTER_COAT_LEGGINGS.get()); output.accept(HogItems.HUNTER_COAT_BOOTS.get());
                        output.accept(HogItems.IRONBACK_HARNESS.get());
                    }).build());

    public static void register(IEventBus modBus) { TABS.register(modBus); }
    private HogCreativeTabs() {}
}
