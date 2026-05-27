package com.not_noah.mistborn_metal_arts.item;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class MetalmindItem extends Item {
    private static final String CHARGE_TAG = "MetalbornCharge";
    private static final String CAPACITY_TAG = "MetalbornCapacity";
    private static final String OWNER_TAG = "MetalbornOwner";
    private static final String UNKEYED_TAG = "MetalbornUnkeyed";

    private final Metal metal;
    private final boolean unkeyed;

    public MetalmindItem(Metal metal, boolean unkeyed, Properties properties) {
        super(properties);
        this.metal = metal;
        this.unkeyed = unkeyed;
    }

    public Metal metal() {
        return metal;
    }

    public boolean builtInUnkeyed() {
        return unkeyed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                boolean canUse = data.hasFeruchemicalPower(metal) || (isUnkeyed(stack) && ServerConfig.VALUES.unkeyedMetalmindsEnabled.get());
                if (!canUse) {
                    serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.no_feruchemy", metal.displayName()), true);
                    return;
                }
                ensureOwner(stack, serverPlayer);
                int mode = data.cycleFeruchemyMode(metal);
                Component modeText = switch (mode) {
                    case -1 -> Component.translatable("message.mistborn_metal_arts.feruchemy_store", metal.displayName());
                    case 1 -> Component.translatable("message.mistborn_metal_arts.feruchemy_tap", metal.displayName());
                    default -> Component.translatable("message.mistborn_metal_arts.feruchemy_off", metal.displayName());
                };
                serverPlayer.displayClientMessage(modeText, true);
                MetalArtsNetwork.sync(serverPlayer);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(unkeyed ? "tooltip.mistborn_metal_arts.unkeyed_metalmind" : "tooltip.mistborn_metal_arts.metalmind", metal.displayName()).withStyle(unkeyed ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.metalmind_charge", Math.round(getCharge(stack)), Math.round(getCapacity(stack))).withStyle(ChatFormatting.DARK_AQUA));
        if (isUnkeyed(stack)) {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.metalmind_unkeyed").withStyle(ChatFormatting.AQUA));
        }
    }

    public static float getCharge(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getFloat(CHARGE_TAG) : 0F;
    }

    public static void setCharge(ItemStack stack, float amount) {
        float capacity = getCapacity(stack);
        stack.getOrCreateTag().putFloat(CHARGE_TAG, Math.max(0F, Math.min(capacity, amount)));
    }

    public static float getCapacity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CAPACITY_TAG)) {
            return ServerConfig.VALUES.metalmindCapacity.get().floatValue();
        }
        return Math.max(1F, tag.getFloat(CAPACITY_TAG));
    }

    public static boolean isUnkeyed(ItemStack stack) {
        if (!(stack.getItem() instanceof MetalmindItem item)) {
            return false;
        }
        if (item.builtInUnkeyed()) {
            return true;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(UNKEYED_TAG);
    }

    public static void setUnkeyed(ItemStack stack, boolean value) {
        if (value) {
            stack.getOrCreateTag().putBoolean(UNKEYED_TAG, true);
            stack.getOrCreateTag().remove(OWNER_TAG);
        } else {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(UNKEYED_TAG);
            }
        }
    }

    public static boolean canUse(ItemStack stack, ServerPlayer player) {
        if (!(stack.getItem() instanceof MetalmindItem)) {
            return false;
        }
        if (isUnkeyed(stack) && ServerConfig.VALUES.unkeyedMetalmindsEnabled.get()) {
            return true;
        }
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.hasUUID(OWNER_TAG) || player.getUUID().equals(tag.getUUID(OWNER_TAG));
    }

    public static void ensureOwner(ItemStack stack, ServerPlayer player) {
        if (!(stack.getItem() instanceof MetalmindItem) || isUnkeyed(stack)) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(OWNER_TAG)) {
            tag.putUUID(OWNER_TAG, player.getUUID());
        }
    }

    public static UUID owner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return (tag != null && tag.hasUUID(OWNER_TAG)) ? tag.getUUID(OWNER_TAG) : null;
    }
}
