package com.hoghunter.content;

import com.hoghunter.block.HogNestBlockEntity;
import com.hoghunter.block.BaitedSnareBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public final class HogBlockEntities {
    private HogBlockEntities() {}
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "hoghunter");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HogNestBlockEntity>> HOG_NEST = BLOCK_ENTITY_TYPES.register("hog_nest", () -> BlockEntityType.Builder.of(HogNestBlockEntity::new, HogBlocks.HOG_NEST.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BaitedSnareBlockEntity>> BAITED_SNARE = BLOCK_ENTITY_TYPES.register("baited_snare", () -> BlockEntityType.Builder.of(BaitedSnareBlockEntity::new, HogBlocks.BAITED_SNARE.get()).build(null));
    public static void register(IEventBus modBus) { BLOCK_ENTITY_TYPES.register(modBus); }
}
