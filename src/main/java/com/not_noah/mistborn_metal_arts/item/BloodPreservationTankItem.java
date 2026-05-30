package com.not_noah.mistborn_metal_arts.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BloodPreservationTankItem extends BlockItem {
    public BloodPreservationTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("BlockEntityTag")) {
            CompoundTag beTag = tag.getCompound("BlockEntityTag");
            if (beTag.contains("BloodLevel")) {
                float blood = beTag.getFloat("BloodLevel");
                int percent = Math.round(blood * 100);
                tooltip.add(Component.literal("Blood Level: " + percent + "%").withStyle(ChatFormatting.DARK_RED));
            }
            if (beTag.contains("Inventory")) {
                CompoundTag invTag = beTag.getCompound("Inventory");
                if (invTag.contains("Items")) {
                    net.minecraft.nbt.ListTag itemsList = invTag.getList("Items", 10);
                    int count = 0;
                    int listIndex = tooltip.size();
                    for (int i = 0; i < itemsList.size(); i++) {
                        CompoundTag itemTag = itemsList.getCompound(i);
                        ItemStack spikeStack = ItemStack.of(itemTag);
                        if (!spikeStack.isEmpty()) {
                            count++;
                            tooltip.add(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                                    .append(spikeStack.getHoverName().copy().withStyle(ChatFormatting.RED)));
                        }
                    }
                    if (count > 0) {
                        tooltip.add(listIndex, Component.literal("Stored Spikes (" + count + "/4):").withStyle(ChatFormatting.RED));
                    }
                }
            }
        }
    }
}
