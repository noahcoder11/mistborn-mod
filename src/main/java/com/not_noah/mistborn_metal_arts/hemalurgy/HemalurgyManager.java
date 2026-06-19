package com.not_noah.mistborn_metal_arts.hemalurgy;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.compat.CuriosCompat;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class HemalurgyManager {
    private HemalurgyManager() {
    }

    public static void tick(ServerPlayer player, MetalArtsData data) {
        // Removed old hemalurgic corruption logic in favor of soul stability & scarring consequences
    }

    public static boolean installSpike(ServerPlayer player, MetalArtsData data, Metal spikeMetal, String powerType, Metal powerMetal, float strength) {
        return installSpike(player, data, spikeMetal, powerType, powerMetal, strength, com.not_noah.mistborn_metal_arts.api.SpiritualAttributes.generateIdentity(), new net.minecraft.nbt.CompoundTag());
    }

    public static boolean installSpike(ServerPlayer player, MetalArtsData data, Metal spikeMetal, String powerType, Metal powerMetal, float strength, String identityKey, net.minecraft.nbt.CompoundTag stolenSpiritWeb) {
        if (!ServerConfig.VALUES.hemalurgyEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.hemalurgy_disabled"), true);
            return false;
        }
        if (!data.installSpike(spikeMetal, powerType, powerMetal, strength, identityKey, stolenSpiritWeb)) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.too_many_spikes"), true);
            return false;
        }
        player.hurt(player.damageSources().magic(), 2.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.65F, 0.55F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_installed", powerMetal.displayName()), true);
        MetalArtsNetwork.sync(player);
        return true;
    }

    public static boolean removeLastSpike(ServerPlayer player, MetalArtsData data) {
        if (!ServerConfig.VALUES.spikeRemovalPossible.get()) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_removal_disabled"), true);
            return false;
        }
        int lastIndex = data.installedSpikes().size() - 1;
        MetalArtsData.InstalledSpike spike = lastIndex >= 0 ? data.installedSpikes().get(lastIndex) : null;

        boolean removedPermanent = data.removeLastSpike();
        boolean removedCurio = false;
        if (!removedPermanent) {
            removedCurio = CuriosCompat.removeEquippedSpike(player);
        }
        if (!removedPermanent && !removedCurio) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.no_spikes"), true);
            return false;
        }
        
        if (removedPermanent && spike != null) {
            net.minecraft.world.item.ItemStack spikeStack = com.not_noah.mistborn_metal_arts.hemalurgy.SoulStabilityManager.createSpikeItem(spike);
            if (!spikeStack.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
                    player.level(), player.getX(), player.getY() + 1.0D, player.getZ(), spikeStack
                );
                entity.setDeltaMovement(
                    (player.getRandom().nextFloat() - 0.5) * 0.5,
                    0.3 + player.getRandom().nextFloat() * 0.2,
                    (player.getRandom().nextFloat() - 0.5) * 0.5
                );
                player.level().addFreshEntity(entity);
            }
        }

        player.hurt(player.damageSources().magic(), 4.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.7F, 0.7F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_removed"), true);
        MetalArtsNetwork.sync(player);
        return true;
    }
}
