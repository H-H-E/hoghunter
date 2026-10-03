package com.hoghunter.block;

import com.hoghunter.content.HogEntities;
import com.hoghunter.content.HogItems;
import com.hoghunter.content.HogSounds;
import com.hoghunter.core.HogAttachments;
import com.hoghunter.net.HogNetworking;
import com.hoghunter.worldgen.RootmotherArenaGenerator;
import com.hoghunter.worldgen.RootmotherRitualData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A camp evidence station on the surface and a three-tusk ritual in the deep Overworld. */
public final class RootAltarBlock extends Block {
    public RootAltarBlock(Properties properties) { super(properties); }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !player.getAbilities().mayBuild || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (level.dimension() != Level.OVERWORLD) return fail(player, "message.hoghunter.altar_overworld");
        if (pos.getY() >= 48 && level.canSeeSky(pos.above())) return extract(server, pos, player);
        if (pos.getY() > -49) return fail(player, "message.hoghunter.altar_location");
        return summon(server, pos, player);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useWithoutItem(state, level, pos, player, hit);
        return result == InteractionResult.FAIL ? ItemInteractionResult.FAIL
                : level.isClientSide() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    private static InteractionResult extract(ServerLevel level, BlockPos pos, Player player) {
        var data = HogAttachments.get(player);
        int tier = data.unlockedTier();
        if (tier == 0) {
            if (count(player, HogItems.CORRUPTED_TISSUE.get()) < 3) return fail(player, "message.hoghunter.altar_need_tissue");
            consume(player, HogItems.CORRUPTED_TISSUE.get(), 3);
            data.setEvidence(data.evidence() + 3);
            data.setUnlockedTier(1);
        } else if (tier == 1) {
            if (count(player, HogItems.HOOK_TOOTH.get()) < 1 || count(player, HogItems.SPORE_SAC.get()) < 1)
                return fail(player, "message.hoghunter.altar_need_samples");
            consume(player, HogItems.HOOK_TOOTH.get(), 1);
            consume(player, HogItems.SPORE_SAC.get(), 1);
            data.setEvidence(data.evidence() + 2);
            data.setUnlockedTier(2);
        } else if (tier == 2) {
            if (count(player, HogItems.IRON_PLATE.get()) < 1 || data.fractureTreatments() < 1)
                return fail(player, "message.hoghunter.altar_need_plate");
            consume(player, HogItems.IRON_PLATE.get(), 1);
            data.setEvidence(data.evidence() + 1);
            data.setUnlockedTier(3);
        } else if (tier < 5) {
            if (count(player, HogItems.ROOT_HEART.get()) < 1) return fail(player, "message.hoghunter.altar_need_heart");
            // The extracted heart remains a trophy. The permanent tier prevents repeated rewards.
            data.setUnlockedTier(5);
            data.setEvidence(data.evidence() + 1);
        } else return fail(player, "message.hoghunter.altar_complete");
        data.setSanity(data.sanity() + 20);
        data.setHeartRate(60);
        player.displayClientMessage(Component.translatable("message.hoghunter.altar_extracted", data.unlockedTier()), false);
        level.playSound(null, pos, HogSounds.ROOT_ALTAR_ACTIVATE.get(), SoundSource.BLOCKS, 0.7F, 1.1F);
        if (player instanceof ServerPlayer serverPlayer) HogNetworking.sync(serverPlayer);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult summon(ServerLevel level, BlockPos pos, Player player) {
        var data = HogAttachments.get(player);
        if (level.getDifficulty() == Difficulty.PEACEFUL) return fail(player, "message.hoghunter.altar_peaceful");
        if (!player.getAbilities().instabuild && data.unlockedTier() < 3) return fail(player, "message.hoghunter.altar_need_tier");
        if (!player.getAbilities().instabuild && count(player, HogItems.MARKED_TUSK.get()) < 3)
            return fail(player, "message.hoghunter.altar_need_tusks");
        var ritual = RootmotherRitualData.get(level);
        if (ritual.hasActiveBoss(level)) {
            BlockPos active = ritual.lastKnownPos();
            player.displayClientMessage(Component.translatable("message.hoghunter.altar_active", active.getX(), active.getY(), active.getZ()), false);
            return InteractionResult.FAIL;
        }
        var boss = HogEntities.ROOTMOTHER.get().create(level);
        if (boss == null || !RootmotherArenaGenerator.positionInPreparedArena(level, pos, boss))
            return fail(player, "message.hoghunter.altar_no_space");
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.TRIGGERED, null);
        boss.setPersistenceRequired();
        if (!player.isCreative() && !player.isSpectator()) boss.setTarget(player);
        if (!level.addFreshEntity(boss)) return fail(player, "message.hoghunter.altar_no_space");
        ritual.begin(boss.getUUID(), boss.blockPosition());
        if (!player.getAbilities().instabuild) consume(player, HogItems.MARKED_TUSK.get(), 3);
        data.setUnlockedTier(Math.max(4, data.unlockedTier()));
        data.setNoise(data.noise() + 40);
        level.playSound(null, pos, HogSounds.ROOT_ALTAR_ACTIVATE.get(), SoundSource.BLOCKS, 1.0F, 0.75F);
        player.displayClientMessage(Component.translatable("message.hoghunter.altar_awakened"), false);
        if (player instanceof ServerPlayer serverPlayer) HogNetworking.sync(serverPlayer);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult fail(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        return InteractionResult.FAIL;
    }
    private static int count(Player player, Item item) {
        int amount = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) amount += stack.getCount();
        }
        return amount;
    }
    private static void consume(Player player, Item item, int amount) {
        for (int slot = 0; slot < player.getInventory().getContainerSize() && amount > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int used = Math.min(amount, stack.getCount());
            stack.shrink(used);
            amount -= used;
        }
    }
}
