package com.not_noah.mistborn_metal_arts.hemalurgy;

import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

public class SoulStabilityManager {

    private static final Random RANDOM = new Random();

    public static void tick(ServerPlayer player, MetalArtsData data) {
        // Recalculate stability dynamically based on actual player state (including Curios slots!)
        float oldStability = data.soulStability();
        recalculateStability(player, data);
        if (Math.abs(data.soulStability() - oldStability) > 0.01F) {
            com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork.sync(player);
        }

        float stability = data.soulStability();

        // 1. Stage Transition Warnings
        int stage;
        if (stability >= 80.0F) stage = 0;
        else if (stability >= 60.0F) stage = 1;
        else if (stability >= 40.0F) stage = 2;
        else if (stability >= 20.0F) stage = 3;
        else stage = 4;

        CompoundTag persistentData = player.getPersistentData();
        int lastStage = persistentData.contains("LastStabilityStage") ? persistentData.getInt("LastStabilityStage") : 0;
        if (stage != lastStage) {
            persistentData.putInt("LastStabilityStage", stage);
            switch (stage) {
                case 0 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Stability] Your soul has stabilized.§r"));
                case 1 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e[Stability] You feel a slight spiritual tremor in your soul (Stability: " + String.format("%.1f", stability) + "%).§r"));
                case 2 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6[Stability] Eerie whispers echo in your mind as your powers begin to fluctuate (Stability: " + String.format("%.1f", stability) + "%).§r"));
                case 3 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Stability] CRITICAL! Your spiritweb is fracturing; spikes are resisting your soul (Stability: " + String.format("%.1f", stability) + "%).§r"));
                case 4 -> player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§4[Collapse] COLLAPSE IMMINENT! Your spiritweb is tearing apart! Install a linchpin spike immediately (Stability: " + String.format("%.1f", stability) + "%).§r"));
            }
        }

        // 2. Stage Specific Tick Loops
        // Minor Instability (60–79): occasional power flicker
        if (stability < 80.0F && stability >= 60.0F) {
            if (RANDOM.nextFloat() < 0.00008F) { // ~5% per minute
                flickerRandomPower(player, data, 20);
            }
            if (player.tickCount % 1200 == 0) {
                playWhisperSound(player);
            }
        }

        // Major Instability (40–59): stronger flicker, weakness, whispers
        if (stability < 60.0F && stability >= 40.0F) {
            if (RANDOM.nextFloat() < 0.00016F) { // ~10% per minute
                flickerRandomPower(player, data, 20);
            }
            if (player.tickCount % 600 == 0) {
                playWhisperSound(player);
            }
            if (player.tickCount % 200 == 0 && !data.isBurning(com.not_noah.mistborn_metal_arts.api.Metal.PEWTER)) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 210, 0, true, false));
            }
        }

        // Critical (20–39): constant whispers, spike ejection risk, weakness II
        if (stability < 40.0F && stability >= 20.0F) {
            if (RANDOM.nextFloat() < 0.00033F) { // ~20% per minute
                flickerRandomPower(player, data, 20);
            }
            if (RANDOM.nextFloat() < 0.001F) { // 0.1% per tick spike ejection
                ejectRandomSpike(player, data);
            }
            if (player.tickCount % 300 == 0) {
                playWhisperSound(player);
            }
            if (player.tickCount % 100 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 110, 1, true, false));
            }
        }

        // Collapse (0–19): spikes fighting, wither, active ejection
        if (stability < 20.0F) {
            if (RANDOM.nextFloat() < 0.005F) { // 0.5% per tick
                ejectRandomSpike(player, data);
            }
            if (player.tickCount % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 2, true, false));
            }
            if (player.tickCount % 100 == 0) {
                playWhisperSound(player);
            }
        }
    }

    public static void recalculateStability(MetalArtsData data) {
        float baseMax = ServerConfig.VALUES.soulStabilityBaseMax.get().floatValue();
        float lossPerSpike = ServerConfig.VALUES.stabilityLossPerSpike.get().floatValue();
        float lossPerDuplicate = ServerConfig.VALUES.stabilityLossPerDuplicate.get().floatValue();
        float linchpinBonus = ServerConfig.VALUES.linchpinStabilityBonus.get().floatValue();

        int spikeCount = data.installedSpikes().size();
        int duplicateCount = countDuplicatePowers(data);
        int godMetalSpikeCount = countGodMetalSpikes(data);

        // Count Tanavastium spikes for stability BONUS
        int tanavastiumSpikeCount = 0;
        for (MetalArtsData.InstalledSpike spike : data.installedSpikes()) {
            if (spike.spikeMetal() == com.not_noah.mistborn_metal_arts.api.Metal.TANAVASTIUM) {
                tanavastiumSpikeCount++;
            }
        }

        float maxStability = baseMax - data.spiritualScarring();
        float stability = maxStability
                - (spikeCount * lossPerSpike)
                - (duplicateCount * lossPerDuplicate)
                - (godMetalSpikeCount * 8.0F)
                - (data.identityContamination() * 0.3F)
                - (data.spiritualBloat() * 0.2F)
                + (data.hasLinchpinSpike() ? linchpinBonus : 0.0F)
                + (tanavastiumSpikeCount * 25.0F);  // Tanavastium spike stability bonus!

        // Active Tanavastium burn bonus
        if (data.isBurning(com.not_noah.mistborn_metal_arts.api.Metal.TANAVASTIUM)) {
            stability += 40.0F;
        }

        // Active Tanavastium tapping bonus
        int tanavastiumTapLevel = data.feruchemyMode(com.not_noah.mistborn_metal_arts.api.Metal.TANAVASTIUM);
        if (tanavastiumTapLevel > 0) {
            stability += 15.0F * tanavastiumTapLevel;
        }

        data.setSoulStability(Math.max(0.0F, Math.min(maxStability, stability)));
    }

    public static void recalculateStability(net.minecraft.world.entity.player.Player player, MetalArtsData data) {
        float baseMax = ServerConfig.VALUES.soulStabilityBaseMax.get().floatValue();
        float lossPerSpike = ServerConfig.VALUES.stabilityLossPerSpike.get().floatValue();
        float lossPerDuplicate = ServerConfig.VALUES.stabilityLossPerDuplicate.get().floatValue();
        float linchpinBonus = ServerConfig.VALUES.linchpinStabilityBonus.get().floatValue();

        java.util.List<SpikeInfo> allSpikes = new java.util.ArrayList<>();
        
        // 1. Installed Spikes
        for (MetalArtsData.InstalledSpike spike : data.installedSpikes()) {
            allSpikes.add(new SpikeInfo(spike.spikeMetal(), spike.powerType(), spike.powerMetal()));
        }

        // 2. Curio Equipped Spikes
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            String[] preferredSlots = {
                "physical_quadrant", "mental_quadrant", "spiritual_quadrant", "temporal_quadrant",
                "head", "necklace", "back", "body", "belt", "ring", "hands", "bracelet", "charm"
            };
            for (String slotType : preferredSlots) {
                java.util.Optional<top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotType);
                if (stacksHandler.isEmpty()) continue;
                net.minecraftforge.items.IItemHandlerModifiable stacks = stacksHandler.get().getStacks();
                for (int j = 0; j < stacks.getSlots(); j++) {
                    ItemStack stack = stacks.getStackInSlot(j);
                    if (stack.getItem() instanceof com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem spikeItem && spikeItem.charged()) {
                        CompoundTag tag = stack.getOrCreateTag();
                        String powerType = tag.getString("PowerType");
                        if (powerType.isBlank()) {
                            powerType = spikeItem.metal().isFeruchemical() ? "feruchemy" : "allomancy";
                        }
                        com.not_noah.mistborn_metal_arts.api.Metal powerMetal = com.not_noah.mistborn_metal_arts.api.Metal.byName(tag.getString("PowerMetal")).orElse(spikeItem.metal());
                        allSpikes.add(new SpikeInfo(spikeItem.metal(), powerType, powerMetal));
                    }
                }
            }
        });

        int spikeCount = allSpikes.size();
        
        // Count duplicate powers across ALL spikes (installed + equipped)
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        int tanavastiumSpikeCount = 0;
        int godMetalSpikeCount = 0;
        
        for (SpikeInfo spike : allSpikes) {
            String key = spike.powerType + ":" + spike.powerMetal.id();
            counts.put(key, counts.getOrDefault(key, 0) + 1);
            
            if (spike.metal == com.not_noah.mistborn_metal_arts.api.Metal.TANAVASTIUM) {
                tanavastiumSpikeCount++;
            } else if (spike.metal.isGodMetal()) {
                godMetalSpikeCount++;
            }
        }
        
        int duplicateCount = 0;
        for (int count : counts.values()) {
            if (count > 1) {
                duplicateCount += (count - 1);
            }
        }

        float maxStability = baseMax - data.spiritualScarring();
        float stability = maxStability
                - (spikeCount * lossPerSpike)
                - (duplicateCount * lossPerDuplicate)
                - (godMetalSpikeCount * 8.0F)
                - (data.identityContamination() * 0.3F)
                - (data.spiritualBloat() * 0.2F)
                + (data.hasLinchpinSpike() ? linchpinBonus : 0.0F)
                + (tanavastiumSpikeCount * 25.0F);

        // Active Tanavastium burn bonus
        if (data.isBurning(com.not_noah.mistborn_metal_arts.api.Metal.TANAVASTIUM)) {
            stability += 40.0F;
        }

        // Active Tanavastium tapping bonus
        int tanavastiumTapLevel = data.feruchemyMode(com.not_noah.mistborn_metal_arts.api.Metal.TANAVASTIUM);
        if (tanavastiumTapLevel > 0) {
            stability += 15.0F * tanavastiumTapLevel;
        }

        data.setSoulStability(Math.max(0.0F, Math.min(maxStability, stability)));
    }

    private static class SpikeInfo {
        final com.not_noah.mistborn_metal_arts.api.Metal metal;
        final String powerType;
        final com.not_noah.mistborn_metal_arts.api.Metal powerMetal;
        SpikeInfo(com.not_noah.mistborn_metal_arts.api.Metal metal, String powerType, com.not_noah.mistborn_metal_arts.api.Metal powerMetal) {
            this.metal = metal;
            this.powerType = powerType;
            this.powerMetal = powerMetal;
        }
    }

    public static void onLinchpinRemoved(ServerPlayer player, MetalArtsData data) {
        data.clearLinchpinSpike();
        float linchpinBonus = ServerConfig.VALUES.linchpinStabilityBonus.get().floatValue();
        float stabilityDrop = linchpinBonus * 1.5F;
        data.setSoulStability(data.soulStability() - stabilityDrop);

        // Cascade eject until stability > 20
        int ejected = 0;
        while (data.soulStability() < 20.0F && !data.installedSpikes().isEmpty() && ejected < 8) {
            ejectRandomSpike(player, data);
            recalculateStability(data);
            ejected++;
        }

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 1));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2));

        data.addIdentityContamination(25.0F);
    }

    public static void ejectRandomSpike(ServerPlayer player, MetalArtsData data) {
        if (data.installedSpikes().isEmpty()) return;

        int index = RANDOM.nextInt(data.installedSpikes().size());

        // Don't eject the linchpin
        if (data.hasLinchpinSpike() && index == data.linchpinSpikeIndex()) {
            index = (index + 1) % data.installedSpikes().size();
            if (index == data.linchpinSpikeIndex()) return;
        }

        MetalArtsData.InstalledSpike spike = data.installedSpikes().get(index);

        // Create item to drop
        ItemStack spikeItem = createSpikeItem(spike);
        if (!spikeItem.isEmpty()) {
            ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 1, player.getZ(), spikeItem);
            entity.setDeltaMovement(
                    (RANDOM.nextFloat() - 0.5) * 0.5,
                    0.3 + RANDOM.nextFloat() * 0.2,
                    (RANDOM.nextFloat() - 0.5) * 0.5
            );
            player.level().addFreshEntity(entity);
        }

        data.removeSpike(index);
        recalculateStability(data);
        player.hurt(player.damageSources().magic(), 4.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.5F);
    }

    public static ItemStack createSpikeItem(MetalArtsData.InstalledSpike spike) {
        // Try to find the charged spike item from the registry
        var key = new net.minecraft.resources.ResourceLocation("mistborn_metal_arts",
                "charged_" + spike.spikeMetal().id() + "_spike");
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(key);
        if (item == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString("PowerType", spike.powerType());
        stack.getOrCreateTag().putString("PowerMetal", spike.powerMetal().id());
        stack.getOrCreateTag().putFloat("Strength", spike.strength() * 0.8F); // some loss on ejection
        return stack;
    }

    private static void flickerRandomPower(ServerPlayer player, MetalArtsData data, int ticks) {
        var burning = data.burningMetals();
        if (burning.isEmpty()) return;
        var metals = burning.toArray(new com.not_noah.mistborn_metal_arts.api.Metal[0]);
        var metal = metals[RANDOM.nextInt(metals.length)];
        com.not_noah.mistborn_metal_arts.allomancy.AllomancyManager.handleDeactivation(player, data, metal);
        data.stopBurning(metal);
        // Power resumes naturally when player re-engages
    }

    private static void playWhisperSound(ServerPlayer player) {
        com.not_noah.mistborn_metal_arts.hemalurgy.IdentityContaminationManager.playWhisperSound(player);
    }

    private static int countDuplicatePowers(MetalArtsData data) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (MetalArtsData.InstalledSpike spike : data.installedSpikes()) {
            String key = spike.powerType() + ":" + spike.powerMetal().id();
            counts.merge(key, 1, Integer::sum);
        }
        int duplicates = 0;
        for (int count : counts.values()) {
            if (count > 1) duplicates += count - 1;
        }
        return duplicates;
    }

    private static int countGodMetalSpikes(MetalArtsData data) {
        int count = 0;
        for (MetalArtsData.InstalledSpike spike : data.installedSpikes()) {
            // Tanavastium spikes are excluded — they HELP stability, not hurt it
            if (spike.spikeMetal().isGodMetal() && spike.spikeMetal() != com.not_noah.mistborn_metal_arts.api.Metal.TANAVASTIUM) count++;
        }
        return count;
    }
}
