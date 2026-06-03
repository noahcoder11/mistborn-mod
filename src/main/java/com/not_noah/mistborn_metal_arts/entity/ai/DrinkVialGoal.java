package com.not_noah.mistborn_metal_arts.entity.ai;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

public class DrinkVialGoal extends Goal {
    private final MetalbornEnemy mob;
    private Metal lowMetal = null;
    private int drinkTicks = 0;
    private int cooldown = 0;

    public DrinkVialGoal(MetalbornEnemy mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!mob.isAlive()) {
            return false;
        }

        lowMetal = null;
        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            for (Metal metal : Metal.cachedValues()) {
                if (data.hasAllomanticPower(metal) && data.getReserve(metal) < 40.0F) {
                    for (ItemStack stack : mob.getMobInventory()) {
                        if (!stack.isEmpty() && stack.is(ModItems.METAL_VIALS.get(metal).get())) {
                            lowMetal = metal;
                            return;
                        }
                    }
                }
            }
        });

        return lowMetal != null;
    }

    @Override
    public boolean canContinueToUse() {
        return drinkTicks > 0 && lowMetal != null && mob.isAlive();
    }

    @Override
    public void start() {
        drinkTicks = 20;
        mob.getNavigation().stop();
        mob.startUsingItem(InteractionHand.MAIN_HAND);
    }

    @Override
    public void tick() {
        drinkTicks--;
        if (drinkTicks % 4 == 0) {
            mob.level().playSound(null, mob.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 0.5F, 0.9F + mob.getRandom().nextFloat() * 0.2F);
        }
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.METAL_VIALS.get(lowMetal).get())), mob.getX(), mob.getY() + mob.getBbHeight() * 0.8, mob.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.05D);
        }

        if (drinkTicks <= 0 && lowMetal != null) {
            mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                ItemStack vialStack = ItemStack.EMPTY;
                for (ItemStack stack : mob.getMobInventory()) {
                    if (!stack.isEmpty() && stack.is(ModItems.METAL_VIALS.get(lowMetal).get())) {
                        vialStack = stack;
                        break;
                    }
                }

                if (!vialStack.isEmpty()) {
                    vialStack.shrink(1);
                    data.fillReserve(lowMetal, 150.0F);
                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 0.5F, 1.2F);
                }
            });
            lowMetal = null;
        }
    }

    @Override
    public void stop() {
        drinkTicks = 0;
        lowMetal = null;
        mob.stopUsingItem();
        cooldown = 100;
    }
}
