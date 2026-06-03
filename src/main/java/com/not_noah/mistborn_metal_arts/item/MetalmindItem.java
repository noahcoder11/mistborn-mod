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
    public enum Type {
        RING("ring", 1.0F),
        BRACER("bracer", 3.0F),
        NECKLACE("necklace", 8.0F);

        private final String suffix;
        private final float capacityMultiplier;

        Type(String suffix, float capacityMultiplier) {
            this.suffix = suffix;
            this.capacityMultiplier = capacityMultiplier;
        }

        public String suffix() {
            return suffix;
        }

        public float capacityMultiplier() {
            return capacityMultiplier;
        }
    }

    private static final String CHARGE_TAG = "MetalbornCharge";
    private static final String CAPACITY_TAG = "MetalbornCapacity";
    private static final String OWNER_TAG = "MetalbornOwner";
    private static final String UNKEYED_TAG = "MetalbornUnkeyed";

    private final Metal metal;
    private final Type type;
    private final boolean unkeyed;

    public MetalmindItem(Metal metal, Type type, boolean unkeyed, Properties properties) {
        super(properties);
        this.metal = metal;
        this.type = type;
        this.unkeyed = unkeyed;
    }

    public MetalmindItem(Metal metal, boolean unkeyed, Properties properties) {
        this(metal, Type.RING, unkeyed, properties);
    }

    public Metal metal() {
        return metal;
    }

    public Type type() {
        return type;
    }

    public boolean builtInUnkeyed() {
        return unkeyed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String typeName = type.name().toLowerCase();
        tooltip.add(Component.translatable(unkeyed ? "tooltip.mistborn_metal_arts.unkeyed_metalmind" : "tooltip.mistborn_metal_arts.metalmind", metal.displayName()).append(" (" + typeName + ")").withStyle(unkeyed ? ChatFormatting.AQUA : ChatFormatting.GRAY));
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
            float base = ServerConfig.VALUES.metalmindCapacity.get().floatValue();
            if (stack.getItem() instanceof MetalmindItem item) {
                return base * item.type().capacityMultiplier();
            }
            return base;
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

    public static boolean canUse(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof MetalmindItem)) {
            return false;
        }
        if (isUnkeyed(stack) && ServerConfig.VALUES.unkeyedMetalmindsEnabled.get()) {
            return true;
        }
        CompoundTag tag = stack.getTag();
        return tag == null || !tag.hasUUID(OWNER_TAG) || player.getUUID().equals(tag.getUUID(OWNER_TAG));
    }

    public static void ensureOwner(ItemStack stack, Player player) {
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

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) > 0.0F;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float charge = getCharge(stack);
        float capacity = getCapacity(stack);
        if (capacity <= 0.0F) return 0;
        return Math.round(13.0F * charge / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00BEDC; // Light cyan/aqua
    }
}
