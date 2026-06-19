package com.not_noah.mistborn_metal_arts.api;

import java.util.Map;
import java.util.EnumMap;
import net.minecraft.nbt.CompoundTag;

public class Feruchemy extends InvestedArt {
    private final Map<Metal, Float> powers = new EnumMap<>(Metal.class);

    public Feruchemy() {
        super(new Shard[] { Shard.RUIN, Shard.PRESERVATION });

        // Initialize maps
        for (Metal metal : Metal.values()) {
            powers.put(metal, 0F);
        }
    }

    public float getPower(Metal metal) {
        return powers.getOrDefault(metal, 0F);
    }

    public void setPower(Metal metal, float level) {
        powers.put(metal, level);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = super.serializeNBT();
        
        CompoundTag powersNbt = new CompoundTag();
        powers.forEach((k, v) -> powersNbt.putFloat(k.name(), v));
        nbt.put("Powers", powersNbt);
        
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        
        if (nbt.contains("Powers", 10)) {
            CompoundTag powersNbt = nbt.getCompound("Powers");
            for (String key : powersNbt.getAllKeys()) {
                try {
                    Metal metal = Metal.valueOf(key);
                    powers.put(metal, powersNbt.getFloat(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
