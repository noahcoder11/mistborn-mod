package com.not_noah.mistborn_metal_arts.api;

import java.util.Map;
import java.util.EnumMap;

public class Allomancy extends InvestedArt {
    private final Map<Metal, Float> powers = new EnumMap<>(Metal.class);
    private final Map<Metal, Float> reserves = new EnumMap<>(Metal.class);

    public Allomancy() {
        super(new Shard[] { Shard.PRESERVATION });

        // Initialize maps
        for (Metal metal : Metal.cachedValues()) {
            if (metal.isAllomantic()) {
                powers.put(metal, 0F);
                reserves.put(metal, 0F);
            }
        }
    }

    public float getPower(Metal metal) {
        return powers.getOrDefault(metal, 0F);
    }

    public void setPower(Metal metal, float level) {
        if (metal.isAllomantic()) {
            powers.put(metal, level);
        }
    }

    public float getReserve(Metal metal) {
        return reserves.getOrDefault(metal, 0F);
    }

    public void setReserve(Metal metal, float amount) {
        if (metal.isAllomantic()) {
            reserves.put(metal, amount);
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag serializeNBT() {
        net.minecraft.nbt.CompoundTag nbt = super.serializeNBT();
        
        net.minecraft.nbt.CompoundTag powersNbt = new net.minecraft.nbt.CompoundTag();
        powers.forEach((k, v) -> powersNbt.putFloat(k.name(), v));
        nbt.put("Powers", powersNbt);
        
        net.minecraft.nbt.CompoundTag reservesNbt = new net.minecraft.nbt.CompoundTag();
        reserves.forEach((k, v) -> reservesNbt.putFloat(k.name(), v));
        nbt.put("Reserves", reservesNbt);
        
        return nbt;
    }

    @Override
    public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
        super.deserializeNBT(nbt);
        
        if (nbt.contains("Powers", 10)) {
            net.minecraft.nbt.CompoundTag powersNbt = nbt.getCompound("Powers");
            for (String key : powersNbt.getAllKeys()) {
                try {
                    Metal metal = Metal.valueOf(key);
                    if (metal.isAllomantic()) {
                        powers.put(metal, powersNbt.getFloat(key));
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        if (nbt.contains("Reserves", 10)) {
            net.minecraft.nbt.CompoundTag reservesNbt = nbt.getCompound("Reserves");
            for (String key : reservesNbt.getAllKeys()) {
                try {
                    Metal metal = Metal.valueOf(key);
                    if (metal.isAllomantic()) {
                        reserves.put(metal, reservesNbt.getFloat(key));
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        // Backwards compatibility with separate GodMetal tags
        if (nbt.contains("GodMetalPowers", 10)) {
            net.minecraft.nbt.CompoundTag godPowersNbt = nbt.getCompound("GodMetalPowers");
            for (String key : godPowersNbt.getAllKeys()) {
                try {
                    Metal metal = Metal.valueOf(key);
                    if (metal.isAllomantic()) {
                        powers.put(metal, godPowersNbt.getFloat(key));
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        if (nbt.contains("GodMetalReserves", 10)) {
            net.minecraft.nbt.CompoundTag godMetalReservesNbt = nbt.getCompound("GodMetalReserves");
            for (String key : godMetalReservesNbt.getAllKeys()) {
                try {
                    Metal metal = Metal.valueOf(key);
                    if (metal.isAllomantic()) {
                        reserves.put(metal, godMetalReservesNbt.getFloat(key));
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
