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

public class AluminumCasingItem extends net.minecraft.world.item.BlockItem {
    public AluminumCasingItem(net.minecraft.world.level.block.Block block, Properties properties) {
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
            if (!beTag.isEmpty()) {
                tag.put("BlockEntityTag", beTag);
            }
        }
        
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack casingStack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(casingStack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(casingStack, level.isClientSide());
        }

        CompoundTag tag = casingStack.getOrCreateTag();
        boolean hasSpike = tag.contains("StoredSpike");
        
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);

        if (hasSpike) {
            if (otherStack.getItem() instanceof net.minecraft.world.item.ShearsItem) {
                CompoundTag spikeTag = tag.getCompound("StoredSpike");
                ItemStack spike = ItemStack.of(spikeTag);
                if (!spike.isEmpty()) {
                    spike = SpikeDecayManager.setStoredState(spike, "normal", level);
                    
                    if (!player.getInventory().add(spike)) {
                        player.drop(spike, false);
                    }
                    
                    tag.remove("StoredSpike");
                    
                    if (!player.getAbilities().instabuild) {
                        otherStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(otherHand));
                        if (otherStack.isEmpty()) {
                            player.setItemInHand(otherHand, ItemStack.EMPTY);
                        } else {
                            player.setItemInHand(otherHand, otherStack);
                        }
                    }
                    
                    level.playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.1F);
                    level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.4F, 1.6F);
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_retrieved_aluminum"), true);
                    player.containerMenu.broadcastChanges();
                    return InteractionResultHolder.success(casingStack);
                }
            } else {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.shears_required_extract"), true);
            }
        } else {
            if (otherStack.getItem() instanceof HemalurgicSpikeItem) {
                ItemStack inserted = otherStack.copy();
                inserted.setCount(1);
                
                inserted = SpikeDecayManager.setStoredState(inserted, "aluminum", level);
                
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
                
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_inserted_aluminum"), true);
                player.containerMenu.broadcastChanges();
                return InteractionResultHolder.success(casingStack);
            } else {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.aluminum_casing_empty_hint"), true);
            }
        }

        return InteractionResultHolder.pass(casingStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (level == null) return;
        
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("StoredSpike")) {
            CompoundTag spikeTag = tag.getCompound("StoredSpike");
            ItemStack spike = ItemStack.of(spikeTag);
            if (!spike.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.contains_spike", spike.getHoverName()).withStyle(ChatFormatting.RED));
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.decay_paused").withStyle(ChatFormatting.GREEN));
                
                if (SpikeDecayManager.updateDecay(spike, level)) {
                    spike = SpikeDecayManager.getExhaustedStack(spike);
                    CompoundTag updatedSpikeTag = new CompoundTag();
                    spike.save(updatedSpikeTag);
                    tag.put("StoredSpike", updatedSpikeTag);
                }
                
                float strength = spike.getOrCreateTag().getFloat("Strength");
                int percentage = Math.round(strength * 100);
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_charge", percentage).withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.aluminum_casing_empty").withStyle(ChatFormatting.GRAY));
        }
    }
}
