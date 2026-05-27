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
                serverPlayer.level().playSound(null, serverPlayer.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 0.65F, 0.55F);
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
                    if (HemalurgyManager.installSpike(serverPlayer, data, metal, powerType, powerMetal, strength) && !serverPlayer.getAbilities().instabuild) {
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
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(charged ? "tooltip.mistborn_metal_arts.charged_spike" : "tooltip.mistborn_metal_arts.spike", metal.displayName()).withStyle(charged ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (charged) {
            CompoundTag tag = stack.getOrCreateTag();
            String type = tag.getString("PowerType");
            String powerMetal = tag.getString("PowerMetal");
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.spike_power", type.isBlank() ? "allomancy" : type, powerMetal.isBlank() ? metal.displayName() : powerMetal).withStyle(ChatFormatting.DARK_RED));
        } else {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.blank_spike_warning").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
