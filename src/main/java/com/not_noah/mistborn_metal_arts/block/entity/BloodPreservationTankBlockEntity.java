package com.not_noah.mistborn_metal_arts.block.entity;

import com.not_noah.mistborn_metal_arts.block.BloodPreservationTankBlock;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.hemalurgy.SpikeDecayManager;
import com.not_noah.mistborn_metal_arts.item.BloodVialItem;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import com.not_noah.mistborn_metal_arts.registry.ModBlockEntities;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BloodPreservationTankBlockEntity extends BlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (!(stack.getItem() instanceof HemalurgicSpikeItem)) {
                return false;
            }
            int currentSpikes = 0;
            for (int i = 0; i < 4; i++) {
                if (!getStackInSlot(i).isEmpty()) {
                    currentSpikes++;
                }
            }
            int maxSpikes = (int) Math.ceil(bloodLevel * 4.0F);
            return currentSpikes < maxSpikes;
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private float bloodLevel = 0.0F;

    public BloodPreservationTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOD_PRESERVATION_TANK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BloodPreservationTankBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        boolean hasSpikes = false;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = blockEntity.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof HemalurgicSpikeItem spikeItem && spikeItem.charged()) {
                hasSpikes = true;
                break;
            }
        }

        if (hasSpikes && blockEntity.bloodLevel > 0.0F) {
            blockEntity.bloodLevel = Math.max(0.0F, blockEntity.bloodLevel - (1.0F / 240000.0F));
            blockEntity.setChanged();
        }

        int expectedVisualLevel = (int) Math.ceil(blockEntity.bloodLevel * 4.0F);
        if (state.hasProperty(BloodPreservationTankBlock.LEVEL) && state.getValue(BloodPreservationTankBlock.LEVEL) != expectedVisualLevel) {
            level.setBlock(pos, state.setValue(BloodPreservationTankBlock.LEVEL, expectedVisualLevel), 3);
        }

        double decayRate = blockEntity.bloodLevel > 0.0F
                ? ServerConfig.VALUES.spikeDecayRateBlood.get() * 0.5D
                : ServerConfig.VALUES.spikeDecayRateOutside.get();
        boolean changed = false;

        for (int i = 0; i < 4; i++) {
            ItemStack stack = blockEntity.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof HemalurgicSpikeItem spikeItem && spikeItem.charged()) {
                CompoundTag tag = stack.getOrCreateTag();
                if (tag.contains("Strength")) {
                    float currentStrength = tag.getFloat("Strength");
                    float newStrength = (float) Math.max(0.0D, currentStrength - decayRate);
                    tag.putFloat("Strength", newStrength);
                    tag.putLong("LastUpdateTime", level.getGameTime());
                    
                    if (newStrength <= 0.001F) {
                        blockEntity.itemHandler.setStackInSlot(i, SpikeDecayManager.getExhaustedStack(stack));
                    }
                    
                    changed = true;
                }
            }
        }

        if (changed) {
            blockEntity.setChanged();
        }
    }

    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        
        if (held.getItem() == ModItems.BLOOD_VIAL.get()) {
            if (held.getOrCreateTag().contains("StoredSpike")) {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.remove_spike_first").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            if (BloodVialItem.isSpoiled(held, level)) {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.cannot_pour_spoiled_blood").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            if (bloodLevel >= 1.0F) {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.tank_full_blood"), true);
                return;
            }

            bloodLevel = Math.min(1.0F, bloodLevel + 0.25F);
            
            BlockState currentState = level.getBlockState(worldPosition);
            int expectedVisualLevel = (int) Math.ceil(bloodLevel * 4.0F);
            if (currentState.hasProperty(BloodPreservationTankBlock.LEVEL)) {
                level.setBlock(worldPosition, currentState.setValue(BloodPreservationTankBlock.LEVEL, expectedVisualLevel), 3);
            }

            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (held.isEmpty()) {
                    player.setItemInHand(hand, bottle);
                } else {
                    if (!player.getInventory().add(bottle)) {
                        player.drop(bottle, false);
                    }
                }
            }
            
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
            displayStatus(player);
            setChanged();
            player.containerMenu.broadcastChanges();
            return;
        }

        if (held.getItem() == Items.GLASS_BOTTLE) {
            if (bloodLevel < 0.25F) {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.not_enough_blood_in_tank").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            int currentSpikes = 0;
            for (int i = 0; i < 4; i++) {
                if (!itemHandler.getStackInSlot(i).isEmpty()) {
                    currentSpikes++;
                }
            }
            int newMaxSpikes = (int) Math.ceil(Math.max(0.0F, bloodLevel - 0.25F) * 4.0F);
            if (currentSpikes > newMaxSpikes) {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.cannot_draw_blood_spikes_limit").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            bloodLevel = Math.max(0.0F, bloodLevel - 0.25F);

            BlockState currentState = level.getBlockState(worldPosition);
            int expectedVisualLevel = (int) Math.ceil(bloodLevel * 4.0F);
            if (currentState.hasProperty(BloodPreservationTankBlock.LEVEL)) {
                level.setBlock(worldPosition, currentState.setValue(BloodPreservationTankBlock.LEVEL, expectedVisualLevel), 3);
            }

            ItemStack freshVial = new ItemStack(ModItems.BLOOD_VIAL.get());
            freshVial.getOrCreateTag().putLong("HarvestTime", level.getGameTime());

            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (held.isEmpty()) {
                    player.setItemInHand(hand, freshVial);
                } else {
                    if (!player.getInventory().add(freshVial)) {
                        player.drop(freshVial, false);
                    }
                }
            } else {
                if (!player.getInventory().add(freshVial)) {
                    player.drop(freshVial, false);
                }
            }

            level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            displayStatus(player);
            setChanged();
            player.containerMenu.broadcastChanges();
            return;
        }

        if (held.getItem() instanceof HemalurgicSpikeItem) {
            int emptySlot = -1;
            for (int i = 0; i < 4; i++) {
                if (itemHandler.getStackInSlot(i).isEmpty()) {
                    emptySlot = i;
                    break;
                }
            }

            if (emptySlot != -1) {
                int currentSpikes = 0;
                for (int i = 0; i < 4; i++) {
                    if (!itemHandler.getStackInSlot(i).isEmpty()) {
                        currentSpikes++;
                    }
                }
                int maxSpikes = (int) Math.ceil(bloodLevel * 4.0F);
                if (currentSpikes >= maxSpikes) {
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.tank_needs_more_blood"), true);
                    return;
                }

                ItemStack inserted = held.copy();
                inserted.setCount(1);
                
                inserted = SpikeDecayManager.setStoredState(inserted, "blood", level);
                
                itemHandler.setStackInSlot(emptySlot, inserted);
                
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    if (held.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                    } else {
                        player.setItemInHand(hand, held);
                    }
                }
                
                level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.8F, 1.3F);
                displayStatus(player);
                setChanged();
                player.containerMenu.broadcastChanges();
            } else {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.tank_full_spikes"), true);
            }
            return;
        }

        int filledSlot = -1;
        for (int i = 3; i >= 0; i--) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                filledSlot = i;
                break;
            }
        }

        if (filledSlot != -1) {
            ItemStack spike = itemHandler.getStackInSlot(filledSlot);
            
            spike = SpikeDecayManager.setStoredState(spike, "normal", level);
            
            if (!player.getInventory().add(spike)) {
                player.drop(spike, false);
            }
            
            itemHandler.setStackInSlot(filledSlot, ItemStack.EMPTY);
            
            level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.2F);
            displayStatus(player);
            setChanged();
            player.containerMenu.broadcastChanges();
        } else {
            displayStatus(player);
        }
    }

    private void displayStatus(Player player) {
        int spikesCount = 0;
        for (int i = 0; i < 4; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                spikesCount++;
            }
        }
        
        int percent = Math.round(bloodLevel * 100);
        String bloodBar = "[";
        int filledBars = Math.round(bloodLevel * 10);
        for (int i = 0; i < 10; i++) {
            if (i < filledBars) {
                bloodBar += "=";
            } else {
                bloodBar += " ";
            }
        }
        bloodBar += "]";

        player.displayClientMessage(Component.literal("Tank Status: ")
                .append(Component.literal(spikesCount + "/4 Spikes").withStyle(ChatFormatting.RED))
                .append(Component.literal(" | Blood Level: " + percent + "% ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal(bloodBar).withStyle(ChatFormatting.RED)), true);
    }

    public void drops() {
        if (level != null) {
            for (int i = 0; i < 4; i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    stack = SpikeDecayManager.setStoredState(stack, "normal", level);
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                }
            }
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putFloat("BloodLevel", bloodLevel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        bloodLevel = tag.getFloat("BloodLevel");
    }
}
