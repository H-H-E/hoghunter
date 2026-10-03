package com.hoghunter;

import com.mojang.logging.LogUtils;
import com.hoghunter.content.HogBlockEntities;
import com.hoghunter.content.HogBlocks;
import com.hoghunter.content.HogCreativeTabs;
import com.hoghunter.content.HogEntities;
import com.hoghunter.content.HogItems;
import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

@Mod(HogHunterMod.MOD_ID)
public class HogHunterMod {

    public static final String MOD_ID = "hoghunter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public HogHunterMod(IEventBus modBus, ModContainer container) {
        HogItems.register(modBus);
        HogBlocks.register(modBus);
        HogBlockEntities.register(modBus);
        HogEntities.register(modBus);
        HogSounds.register(modBus);
        HogCreativeTabs.register(modBus);
        HogAttachments.register(modBus);

        com.hoghunter.core.HogHunterConfig.register(container);

        modBus.addListener(com.hoghunter.entity.HogEntityAttributes::register);
        modBus.addListener(com.hoghunter.entity.HogSpawnPlacements::register);

        LOGGER.info("Hog Hunter loaded: items, blocks, entities, sounds, attachments.");
    }
}
