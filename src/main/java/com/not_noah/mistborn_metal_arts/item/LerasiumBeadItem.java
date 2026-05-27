package com.not_noah.mistborn_metal_arts.item;

import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
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

public class LerasiumBeadItem extends Item {
    public LerasiumBeadItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide && ServerConfig.VALUES.lerasiumExists.get() && ServerConfig.VALUES.lerasiumGrantsMistborn.get()) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                data.setMistborn();
                data.setAllomanticStrength(1.0F);
                data.setAllomancySnapped(true);
                if (player instanceof ServerPlayer serverPlayer) {
                    MetalArtsNetwork.sync(serverPlayer);
                }
            });
            level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.lerasium_mistborn"), true);
        }
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.lerasium").withStyle(ChatFormatting.AQUA));
    }
}
