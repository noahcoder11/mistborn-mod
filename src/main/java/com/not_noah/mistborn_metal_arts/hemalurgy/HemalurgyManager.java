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
        int corruption = data.totalCorruption();
        if (!ServerConfig.VALUES.hemalurgyEnabled.get() || corruption <= 0) {
            return;
        }
        int threshold = Math.max(1, ServerConfig.VALUES.maxSpikesBeforeCorruption.get());
        player.addEffect(new MobEffectInstance(ModEffects.HEMALURGIC_CORRUPTION.get(), 60, Math.min(3, corruption / threshold), false, true));
        if (corruption >= threshold * 2) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false));
        }
        if (corruption >= threshold * 3) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, false));
        }
        if (player.tickCount % 240 == 0 && corruption >= threshold) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.25F, 1.45F);
        }
    }

    public static boolean installSpike(ServerPlayer player, MetalArtsData data, Metal spikeMetal, String powerType, Metal powerMetal, float strength) {
        if (!ServerConfig.VALUES.hemalurgyEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.hemalurgy_disabled"), true);
            return false;
        }
        if (!data.installSpike(spikeMetal, powerType, powerMetal, strength)) {
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
        boolean removedPermanent = data.removeLastSpike();
        boolean removedCurio = false;
        if (!removedPermanent) {
            removedCurio = CuriosCompat.removeEquippedSpike(player);
        }
        if (!removedPermanent && !removedCurio) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.no_spikes"), true);
            return false;
        }
        player.hurt(player.damageSources().magic(), 4.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.7F, 0.7F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_removed"), true);
        MetalArtsNetwork.sync(player);
        return true;
    }
}
