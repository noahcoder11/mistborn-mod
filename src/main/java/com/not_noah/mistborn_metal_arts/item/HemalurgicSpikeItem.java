package com.not_noah.mistborn_metal_arts.item;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.compat.CuriosCompat;
import com.not_noah.mistborn_metal_arts.hemalurgy.HemalurgyManager;
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

public class HemalurgicSpikeItem extends Item {
    private final Metal metal;
    private final boolean charged;

    public HemalurgicSpikeItem(Metal metal, boolean charged, Properties properties) {
        super(properties);
        this.metal = metal;
        this.charged = charged;
    }

    public Metal metal() {
        return metal;
    }

    public boolean charged() {
        return charged;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!charged) {
                serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.blank_spike"), true);
                return InteractionResultHolder.fail(stack);
            }

            if (CuriosCompat.equipSpikeFromUse(serverPlayer, stack)) {
                serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_equipped"), true);
                return InteractionResultHolder.success(stack);
            }

            // Fallback to permanent install if shifting
            if (serverPlayer.isShiftKeyDown()) {
                serverPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    CompoundTag tag = stack.getOrCreateTag();
                    String powerType = tag.getString("PowerType");
                    if (powerType.isBlank()) {
                        powerType = metal.isFeruchemical() ? "feruchemy" : "allomancy";
                    }
                    Metal powerMetal = Metal.byName(tag.getString("PowerMetal")).orElse(metal);
                    float strength = tag.contains("Strength") ? tag.getFloat("Strength") : 1.0F;
                    String identityKey = tag.getString("SpikeIdentity");
                    CompoundTag stolenSpiritWeb = tag.contains("StolenSpiritWeb") ? tag.getCompound("StolenSpiritWeb") : new CompoundTag();
                    if (HemalurgyManager.installSpike(serverPlayer, data, metal, powerType, powerMetal, strength, identityKey, stolenSpiritWeb) && !serverPlayer.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                });
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.no_spike_slots"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return charged || super.isFoil(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && charged) {
            CompoundTag tag = stack.getOrCreateTag();
            String state = tag.getString("StoredState");
            if ("equipped".equals(state) || "blood".equals(state) || "aluminum".equals(state)) {
                tag.putString("StoredState", "normal");
                tag.putLong("LastUpdateTime", level.getGameTime());
            }
            if (com.not_noah.mistborn_metal_arts.hemalurgy.SpikeDecayManager.updateDecay(stack, level)) {
                // Charge exhausted! Revert to blank spike
                if (entity instanceof Player player) {
                    ItemStack newStack = com.not_noah.mistborn_metal_arts.hemalurgy.SpikeDecayManager.getExhaustedStack(stack);
                    if (slotId >= 0 && slotId < player.getInventory().getContainerSize()) {
                        player.getInventory().setItem(slotId, newStack);
                    } else {
                        com.not_noah.mistborn_metal_arts.compat.CuriosCompat.replaceCurioStack(player, stack, newStack);
                    }
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.2F);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(charged ? "tooltip.mistborn_metal_arts.charged_spike" : "tooltip.mistborn_metal_arts.spike", metal.displayName()).withStyle(charged ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (charged) {
            CompoundTag tag = stack.getOrCreateTag();
            String type = tag.getString("PowerType");
            if (type.isBlank()) {
                type = "allomancy";
            }
            String powerMetalStr = tag.getString("PowerMetal");
            String powerMetalNameStr = powerMetalStr.isBlank() ? metal.displayName() : Metal.byName(powerMetalStr).map(Metal::displayName).orElse(powerMetalStr);
            Component powerMetalName = Component.literal(powerMetalNameStr);

            if ("physical_strength".equals(type)) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_attribute_strength").withStyle(ChatFormatting.DARK_RED));
            } else if ("physical_sight".equals(type)) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_attribute_sight").withStyle(ChatFormatting.DARK_RED));
            } else if ("emotional_fortitude".equals(type)) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_attribute_emotional").withStyle(ChatFormatting.DARK_RED));
            } else if ("mental_fortitude".equals(type)) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_attribute_mental").withStyle(ChatFormatting.DARK_RED));
            } else if ("investiture".equals(type)) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_attribute_investiture").withStyle(ChatFormatting.DARK_RED));
            } else if ("destiny".equals(type)) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_attribute_destiny").withStyle(ChatFormatting.DARK_RED));
            } else if ("connection".equals(type)) {
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_attribute_connection").withStyle(ChatFormatting.DARK_RED));
            } else {
                Component typeComp = Component.translatable("power_type.mistborn_metal_arts." + type);
                tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_power", typeComp, powerMetalName).withStyle(ChatFormatting.DARK_RED));
            }
            
            float strength = tag.contains("Strength") ? tag.getFloat("Strength") : 1.0F;

            // Client-side decay prediction for tooltips so it's always perfectly synced and responsive in real time!
            if (level != null && tag.contains("LastUpdateTime")) {
                long elapsed = level.getGameTime() - tag.getLong("LastUpdateTime");
                if (elapsed > 0) {
                    String state = tag.getString("StoredState");
                    double decayRate = com.not_noah.mistborn_metal_arts.hemalurgy.SpikeDecayManager.getDecayRateForState(state);
                    strength = (float) Math.max(0.0F, strength - (decayRate * elapsed));
                }
            }

            // Curio slot recommendation based on spike metal
            String recommendedSlot;
            if (metal == Metal.IRON || metal == Metal.STEEL || metal == Metal.TIN || metal == Metal.PEWTER) {
                recommendedSlot = "§9• Recommended Slot: Physical Quadrant";
            } else if (metal == Metal.ZINC || metal == Metal.BRASS || metal == Metal.COPPER || metal == Metal.BRONZE) {
                recommendedSlot = "§6• Recommended Slot: Mental Quadrant";
            } else if (metal == Metal.GOLD || metal == Metal.ELECTRUM || metal == Metal.CHROMIUM || metal == Metal.NICROSIL) {
                recommendedSlot = "§d• Recommended Slot: Spiritual Quadrant";
            } else {
                recommendedSlot = "§a• Recommended Slot: Temporal Quadrant";
            }
            tooltip.add(Component.literal(recommendedSlot));

            int percentage = Math.round(strength * 100);
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_charge", percentage).withStyle(ChatFormatting.GOLD));

            // Render decay context info
            String state = tag.getString("StoredState");
            if ("equipped".equals(state)) {
                tooltip.add(Component.literal("§a• Spiritweb Anchored (Decay Paused)"));
            } else if ("aluminum".equals(state)) {
                tooltip.add(Component.literal("§e• Sealed in Aluminum Casing (Decay Paused)"));
            } else if ("blood".equals(state)) {
                tooltip.add(Component.literal("§c• Preserved in Blood Vial (5x slower decay)"));
            } else if ("tank".equals(state)) {
                tooltip.add(Component.literal("§b• Preserved in Altar Tank (10x slower decay)"));
            } else {
                tooltip.add(Component.literal("§7• Decaying in Air (No Preservation)"));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.blank_spike_warning").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
