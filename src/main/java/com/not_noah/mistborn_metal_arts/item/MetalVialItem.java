package com.not_noah.mistborn_metal_arts.item;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class MetalVialItem extends Item {
    private final Metal metal;
    private final boolean mixed;

    public MetalVialItem(Metal metal, boolean mixed, Properties properties) {
        super(properties);
        this.metal = metal;
        this.mixed = mixed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (mixed) {
                    for (Metal value : Metal.cachedValues()) {
                        if (value.isAllomantic() && value != Metal.ATIUM) {
                            data.fillReserve(value, ServerConfig.vialValue(value));
                        }
                    }
                } else if (metal != null) {
                    data.fillReserve(metal, ServerConfig.vialValue(metal));
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    MetalArtsNetwork.sync(serverPlayer);
                }
            });
            level.playSound(null, entity.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.7F, 1.15F);
        }
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        ItemStack empty = new ItemStack(ModItems.EMPTY_GLASS_VIAL.get());
        if (stack.isEmpty()) {
            return empty;
        }
        if (entity instanceof Player player && !player.getInventory().add(empty)) {
            player.drop(empty, false);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 24;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (mixed) {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.mixed_vial").withStyle(ChatFormatting.GRAY));
        } else if (metal != null) {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.metal_vial", metal.displayName()).withStyle(ChatFormatting.GRAY));
        }
    }
}
