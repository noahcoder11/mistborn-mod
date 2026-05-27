package com.not_noah.mistborn_metal_arts.block;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.not_noah.mistborn_metal_arts.worldgen.WellRegistry;
import net.minecraft.world.entity.LivingEntity;

public class WellOfAscensionBlock extends Block {
    public WellOfAscensionBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            WellRegistry.register(level, pos);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            WellRegistry.register(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            WellRegistry.unregister(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!ServerConfig.VALUES.wellEnabled.get()) {
            serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.well_silent"), true);
            return InteractionResult.CONSUME;
        }
        serverPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            if (ServerConfig.VALUES.wellOneUsePerPlayer.get() && data.wellTouched()) {
                serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.well_already_touched"), true);
                return;
            }
            data.setWellTouched(true);
            data.fillReserve(Metal.ATIUM, 20F);
            data.fillReserve(Metal.PEWTER, 40F);
            data.fillReserve(Metal.TIN, 40F);
            serverPlayer.addEffect(new MobEffectInstance(ModEffects.ATIUM_SIGHT.get(), 120, 0, false, true));
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, true));
            if (ServerConfig.VALUES.wellCanGrantMistborn.get()) {
                data.setMistborn();
            }
            if (ServerConfig.VALUES.wellCanGrantLerasium.get() && ServerConfig.VALUES.lerasiumExists.get()) {
                serverPlayer.getInventory().add(new ItemStack(ModItems.METAL_BEADS.get(Metal.LERASIUM).get()));
            }
            serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.well_touched"), true);
            level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.35F);
            MetalArtsNetwork.sync(serverPlayer);
        });
        return InteractionResult.CONSUME;
    }
}
