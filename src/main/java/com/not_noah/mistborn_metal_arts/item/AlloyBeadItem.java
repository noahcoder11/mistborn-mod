package com.not_noah.mistborn_metal_arts.item;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
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

public class AlloyBeadItem extends Item {
    private final Metal targetMetal;
    private final boolean feruchemical; // true for Lerasatium alloy, false for Lerasium alloy

    public AlloyBeadItem(Metal targetMetal, boolean feruchemical, Properties properties) {
        super(properties);
        this.feruchemical = feruchemical;
        this.targetMetal = targetMetal;
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
                if (feruchemical) {
                    // Lerasatium alloy bead: grants permanent Ferring of target metal.
                    // If already a Feruchemist or Ferring of that metal, it increases storage capacity & efficiency by +15%.
                    boolean alreadyHas = data.hasFeruchemicalPower(targetMetal);
                    if (!alreadyHas) {
                        data.addNaturalFeruchemicalPower(targetMetal);
                        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.lerasatium_ferring", targetMetal.displayName()), true);
                    } else {
                        data.addLerasatiumAlloyBonus(targetMetal, 1.0F);
                        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.lerasatium_boost", targetMetal.displayName()), true);
                    }
                } else {
                    // Lerasium alloy bead: grants permanent Misting of target metal.
                    // If already a Misting/Mistborn, increases Allomantic strength for that specific metal by +0.1F (up to a cap of 1.5F).
                    boolean alreadyHas = data.hasAllomanticPower(targetMetal);
                    if (!alreadyHas) {
                        data.addNaturalAllomanticPower(targetMetal);
                        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.lerasium_misting", targetMetal.displayName()), true);
                    } else {
                        float currentBonus = data.getLerasiumAlloyBonus(targetMetal);
                        // limit total bonus to 0.5F (so base 1.0F + 0.5F = 1.5F cap)
                        if (currentBonus < 0.5F) {
                            data.addLerasiumAlloyBonus(targetMetal, 0.1F);
                            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.lerasium_boost", targetMetal.displayName()), true);
                        } else {
                            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.lerasium_boost_max", targetMetal.displayName()), true);
                        }
                    }
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    MetalArtsNetwork.sync(serverPlayer);
                }
            });
            level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.4F);
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
        if (feruchemical) {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.lerasatium_alloy", targetMetal.displayName()).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.mistborn_metal_arts.lerasium_alloy", targetMetal.displayName()).withStyle(ChatFormatting.AQUA));
        }
    }
}
