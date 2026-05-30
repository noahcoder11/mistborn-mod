package com.not_noah.mistborn_metal_arts.hemalurgy;

import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SpikeDecayManager {
    private SpikeDecayManager() {
    }

    public static boolean updateDecay(ItemStack stack, Level level) {
        if (level.isClientSide() || stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Strength")) {
            return false;
        }

        // Tanavastium spikes never decay — divine spiritual integrity
        if (stack.getItem() instanceof com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem spikeItem
                && spikeItem.metal() == com.not_noah.mistborn_metal_arts.api.Metal.TANAVASTIUM) {
            return false;
        }

        long currentTime = level.getGameTime();
        if (!tag.contains("LastUpdateTime")) {
            tag.putLong("LastUpdateTime", currentTime);
            if (!tag.contains("CreationTime")) {
                tag.putLong("CreationTime", currentTime);
            }
            return false;
        }

        long lastUpdate = tag.getLong("LastUpdateTime");
        if (currentTime - lastUpdate < 20L) {
            return false;
        }

        long elapsedTicks = currentTime - lastUpdate;
        float currentStrength = tag.getFloat("Strength");

        String state = tag.getString("StoredState");
        if (state.isBlank()) {
            state = "normal";
        }

        double decayRate = getDecayRateForState(state);
        float newStrength = (float) (currentStrength - (decayRate * elapsedTicks));
        newStrength = Math.max(0.0F, newStrength);

        tag.putFloat("Strength", newStrength);
        tag.putLong("LastUpdateTime", currentTime);
        
        return newStrength <= 0.001F;
    }

    public static ItemStack getExhaustedStack(ItemStack original) {
        if (original.getItem() instanceof com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem spike) {
            net.minecraft.world.item.Item blank = com.not_noah.mistborn_metal_arts.registry.ModItems.SPIKE_BLANKS.get(spike.metal()).get();
            return new ItemStack(blank, original.getCount());
        }
        return original;
    }

    public static double getDecayRateForState(String state) {
        return switch (state) {
            case "aluminum" -> ServerConfig.VALUES.spikeDecayRateAluminum.get();
            case "blood" -> ServerConfig.VALUES.spikeDecayRateBlood.get();
            case "equipped" -> 0.0D;
            default -> ServerConfig.VALUES.spikeDecayRateOutside.get();
        };
    }

    public static ItemStack setStoredState(ItemStack stack, String state, Level level) {
        if (updateDecay(stack, level)) {
            return getExhaustedStack(stack);
        }
        stack.getOrCreateTag().putString("StoredState", state);
        stack.getOrCreateTag().putLong("LastUpdateTime", level.getGameTime());
        return stack;
    }

    public static boolean isWithinInstantWindow(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("CreationTime")) {
            return false;
        }
        long elapsed = level.getGameTime() - tag.getLong("CreationTime");
        return elapsed >= 0 && elapsed <= ServerConfig.VALUES.instantTransferWindow.get();
    }
}
