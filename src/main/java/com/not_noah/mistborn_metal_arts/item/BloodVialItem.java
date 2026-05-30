package com.not_noah.mistborn_metal_arts.item;

import com.not_noah.mistborn_metal_arts.hemalurgy.SpikeDecayManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public class BloodVialItem extends net.minecraft.world.item.BlockItem {
    public BloodVialItem(net.minecraft.world.level.block.Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            net.minecraft.world.InteractionResultHolder<ItemStack> res = use(context.getLevel(), context.getPlayer(), context.getHand());
            if (res.getResult().consumesAction()) {
                return net.minecraft.world.InteractionResult.sidedSuccess(context.getLevel().isClientSide());
            }
            return net.minecraft.world.InteractionResult.PASS;
        }
        
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            CompoundTag beTag = new CompoundTag();
            if (tag.contains("StoredSpike")) {
                beTag.put("StoredSpike", tag.getCompound("StoredSpike"));
            }
            if (tag.contains("HarvestTime")) {
                beTag.putLong("HarvestTime", tag.getLong("HarvestTime"));
            }
            if (!beTag.isEmpty()) {
                tag.put("BlockEntityTag", beTag);
            }
        }
        
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack vialStack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(vialStack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(vialStack, level.isClientSide());
        }

        CompoundTag tag = vialStack.getOrCreateTag();
        boolean hasSpike = tag.contains("StoredSpike");

        if (hasSpike) {
            CompoundTag spikeTag = tag.getCompound("StoredSpike");
            ItemStack spike = ItemStack.of(spikeTag);
            if (!spike.isEmpty()) {
                spike = SpikeDecayManager.setStoredState(spike, "normal", level);
                
                if (!player.getInventory().add(spike)) {
                    player.drop(spike, false);
                }
                
                tag.remove("StoredSpike");
                level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.3F);
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_retrieved_blood"), true);
                player.containerMenu.broadcastChanges();
                return InteractionResultHolder.success(vialStack);
            }
        } else {
            InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack otherStack = player.getItemInHand(otherHand);
            if (otherStack.getItem() instanceof HemalurgicSpikeItem) {
                if (isSpoiled(vialStack, level)) {
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.blood_spoiled_cannot_insert").withStyle(ChatFormatting.GRAY), true);
                    return InteractionResultHolder.fail(vialStack);
                }

                ItemStack inserted = otherStack.copy();
                inserted.setCount(1);
                
                inserted = SpikeDecayManager.setStoredState(inserted, "blood", level);
                
                CompoundTag spikeTag = new CompoundTag();
                inserted.save(spikeTag);
                tag.put("StoredSpike", spikeTag);
                
                if (!player.getAbilities().instabuild) {
                    otherStack.shrink(1);
                    if (otherStack.isEmpty()) {
                        player.setItemInHand(otherHand, ItemStack.EMPTY);
                    } else {
                        player.setItemInHand(otherHand, otherStack);
                    }
                }
                
                level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8F, 1.1F);
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_inserted_blood"), true);
                player.containerMenu.broadcastChanges();
                return InteractionResultHolder.success(vialStack);
            } else {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.blood_vial_empty_hint"), true);
            }
        }

        return InteractionResultHolder.pass(vialStack);
    }

    public static boolean isSpoiled(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("HarvestTime")) {
            return false;
        }
        long harvest = tag.getLong("HarvestTime");
        if (harvest <= 0L) {
            return false;
        }
        long age = level.getGameTime() - harvest;
        return age > 72000L;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (level == null) return;
        
        boolean spoiled = isSpoiled(stack, level);
        if (spoiled) {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.blood_spoiled").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.blood_fresh").withStyle(ChatFormatting.DARK_RED));
        }

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("StoredSpike")) {
            CompoundTag spikeTag = tag.getCompound("StoredSpike");
            ItemStack spike = ItemStack.of(spikeTag);
            if (!spike.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.contains_spike", spike.getHoverName()).withStyle(ChatFormatting.RED));
                
                // If blood is spoiled, we should switch the storage state to normal so it decays at normal speed!
                if (spoiled && "blood".equals(spike.getOrCreateTag().getString("StoredState"))) {
                    spike = SpikeDecayManager.setStoredState(spike, "normal", level);
                    // Resave to tag
                    CompoundTag updatedSpikeTag = new CompoundTag();
                    spike.save(updatedSpikeTag);
                    tag.put("StoredSpike", updatedSpikeTag);
                } else {
                    if (SpikeDecayManager.updateDecay(spike, level)) {
                        spike = SpikeDecayManager.getExhaustedStack(spike);
                        CompoundTag updatedSpikeTag = new CompoundTag();
                        spike.save(updatedSpikeTag);
                        tag.put("StoredSpike", updatedSpikeTag);
                    }
                }
                
                float strength = spike.getOrCreateTag().getFloat("Strength");
                int percentage = Math.round(strength * 100);
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_charge", percentage).withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.blood_vial_empty").withStyle(ChatFormatting.GRAY));
        }
    }
}
