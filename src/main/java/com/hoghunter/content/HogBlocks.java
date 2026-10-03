package com.hoghunter.content;

import com.hoghunter.block.CorruptedOreBlock;
import com.hoghunter.block.DepthGateBlock;
import com.hoghunter.block.HogNestBlock;
import com.hoghunter.block.SaltLineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public final class HogBlocks {
    private HogBlocks() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("hoghunter");
    public static final DeferredBlock<Block> CORRUPTED_ORE = BLOCKS.register("corrupted_ore", () ->
            new CorruptedOreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 3.0f).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DEPTH_GATE = BLOCKS.register("depth_gate", () ->
            new DepthGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(5.0f, 1200.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> HOG_NEST = BLOCKS.register("hog_nest", () ->
            new HogNestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(1.5f).sound(SoundType.MUD)));
    public static final DeferredBlock<Block> SALT_LINE = BLOCKS.register("salt_line", () ->
            new SaltLineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.1f).sound(SoundType.SAND)));
    public static void register(IEventBus modBus) { BLOCKS.register(modBus); }
}
