package com.not_noah.mistborn_metal_arts.hemalurgy;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Random;

public class SavantismManager {

    private static final Random RANDOM = new Random();

    public static void tick(ServerPlayer player, MetalArtsData data) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag lastBurnedTag = persistentData.getCompound("SavantLastBurned");
        boolean changedTag = false;

        for (Metal metal : Metal.cachedValues()) {
            if (!metal.isAllomantic()) continue;

            boolean isBurning = data.isBurning(metal);

            if (isBurning) {
                // 1. Calculate Savantism progress gain
                double baseGain = data.isFlaring(metal)
                        ? ServerConfig.VALUES.savantGainPerFlareTick.get()
                        : ServerConfig.VALUES.savantGainPerBurnTick.get();

                float spikeMultiplier = 1.0F + (float) (double) ServerConfig.VALUES.savantGainPerSpikeStack.get()
                        * data.countDuplicateSpikes(metal, "allomancy");

                float totalGain = (float) baseGain * spikeMultiplier;
                data.addSavantProgress(metal, totalGain);

                // Update last burned timestamp
                lastBurnedTag.putInt(metal.id(), player.tickCount);
                changedTag = true;
            } else {
                // 2. Passive Decay when not burning
                double decay = ServerConfig.VALUES.savantDecayRate.get();
                data.addSavantProgress(metal, -(float) decay);

                // 3. Withdrawal and Involuntary Activation checks
                int stage = data.savantStage(metal);
                if (stage >= 1) {
                    int lastBurned = lastBurnedTag.contains(metal.id()) ? lastBurnedTag.getInt(metal.id()) : 0;
                    int ticksSinceBurn = player.tickCount - lastBurned;

                    // 5 minutes (6000 ticks) for Stage 1 urge messages
                    if (stage == 1 && ticksSinceBurn > 6000 && ticksSinceBurn % 2400 == 0 && RANDOM.nextFloat() < 0.15F) {
                        player.sendSystemMessage(Component.literal("§d[Savant] You feel a deep, gnawing craving to burn " + metal.displayName() + "..."));
                    }

                    // 10 minutes (12000 ticks) for Stage 2+ withdrawal effects
                    if (stage >= 2 && ticksSinceBurn > 12000) {
                        applyWithdrawal(player, metal, stage);
                    }

                    // Stage 4 Involuntary Activation check (0.01% chance per tick when not burning)
                    if (stage == 4 && ticksSinceBurn > 2400 && RANDOM.nextFloat() < 0.0001F) {
                        triggerInvoluntaryActivation(player, data, metal);
                    }
                }
            }
        }

        if (changedTag) {
            persistentData.put("SavantLastBurned", lastBurnedTag);
        }
    }

    public static void applyWithdrawal(ServerPlayer player, Metal metal, int stage) {
        // Apply once every 30 seconds if they don't already have the main withdrawal effect active
        if (!player.hasEffect(MobEffects.WEAKNESS)) {
            player.sendSystemMessage(Component.literal("§c[Withdrawal] Lacking " + metal.displayName() + " leaves your spirit fractured and weak.§r"));
            
            switch (stage) {
                case 2:
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0, true, true));
                    break;
                case 3:
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1, true, true));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0, true, true));
                    break;
                case 4:
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2400, 2, true, true));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2400, 1, true, true));
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, true, true));
                    break;
            }
        }
    }

    private static void triggerInvoluntaryActivation(ServerPlayer player, MetalArtsData data, Metal metal) {
        data.startBurning(metal);
        player.sendSystemMessage(Component.literal("§c[Savant] Your spirit-web is so deformed that " + metal.displayName() + " ignites involuntarily!§r"));
    }
}
