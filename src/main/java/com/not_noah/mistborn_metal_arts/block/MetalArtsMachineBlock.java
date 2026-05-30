package com.not_noah.mistborn_metal_arts.block;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.hemalurgy.HemalurgyManager;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import com.not_noah.mistborn_metal_arts.item.MetalmindItem;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Optional;

import com.not_noah.mistborn_metal_arts.block.entity.MetalArtsMachineBlockEntity;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class MetalArtsMachineBlock extends Block implements EntityBlock {
    private final String machineId;
    private final String tooltipKey;

    public MetalArtsMachineBlock(Properties properties, String machineId, String tooltipKey) {
        super(properties);
        this.machineId = machineId;
        this.tooltipKey = tooltipKey;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MetalArtsMachineBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MetalArtsMachineBlockEntity machineBe) {
                // Shift-right click for direct interaction (old behavior)
                if (player.isSecondaryUseActive()) {
                    if (handleInteraction(level, pos, serverPlayer, hand)) {
                        return InteractionResult.CONSUME;
                    }
                }
                NetworkHooks.openScreen(serverPlayer, machineBe, pos);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MetalArtsMachineBlockEntity machineBe) {
                machineBe.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    private boolean handleInteraction(Level level, BlockPos pos, ServerPlayer player, InteractionHand hand) {
        return "spike_press".equals(machineId) && pressSpike(level, pos, player, hand);
    }

    private boolean pressSpike(Level level, BlockPos pos, ServerPlayer player, InteractionHand hand) {
        Optional<Metal> metal = metalFrom(player.getItemInHand(hand), ModItems.METAL_INGOTS);
        if (metal.isEmpty()) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        giveOrDrop(player, new net.minecraft.world.item.ItemStack(ModItems.SPIKE_BLANKS.get(metal.get()).get()));
        play(level, pos, SoundEvents.ANVIL_USE, 0.8F, 0.75F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.machine_spike", metal.get().displayName()), true);
        return true;
    }

    private Optional<Metal> metalFrom(net.minecraft.world.item.ItemStack stack, EnumMap<Metal, RegistryObject<Item>> items) {
        for (var entry : items.entrySet()) {
            if (stack.is(entry.getValue().get())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private void giveOrDrop(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void play(Level level, BlockPos pos, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }
}
