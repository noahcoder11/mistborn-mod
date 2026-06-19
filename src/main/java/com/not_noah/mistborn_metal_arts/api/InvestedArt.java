package com.not_noah.mistborn_metal_arts.api;

import java.util.Map;

public class InvestedArt {
    public Shard[] shardicAlignment;
    public Map<String, Float> connections = new java.util.HashMap<>();

    public InvestedArt(Shard[] shardicAlignment) {
        this.shardicAlignment = shardicAlignment;
    }

    public net.minecraft.nbt.CompoundTag serializeNBT() {
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        if (shardicAlignment != null) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (Shard shard : shardicAlignment) {
                list.add(net.minecraft.nbt.StringTag.valueOf(shard.name()));
            }
            nbt.put("ShardicAlignment", list);
        }
        if (connections != null) {
            net.minecraft.nbt.CompoundTag connNbt = new net.minecraft.nbt.CompoundTag();
            connections.forEach(connNbt::putFloat);
            nbt.put("Connections", connNbt);
        }
        return nbt;
    }

    public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
        if (nbt.contains("ShardicAlignment", 9)) { // 9 is ListTag
            net.minecraft.nbt.ListTag list = nbt.getList("ShardicAlignment", 8); // 8 is StringTag
            shardicAlignment = new Shard[list.size()];
            for (int i = 0; i < list.size(); i++) {
                try {
                    shardicAlignment[i] = Shard.valueOf(list.getString(i));
                } catch (IllegalArgumentException e) {
                    shardicAlignment[i] = Shard.NONE;
                }
            }
        }
        if (nbt.contains("Connections", 10)) { // 10 is CompoundTag
            net.minecraft.nbt.CompoundTag connNbt = nbt.getCompound("Connections");
            connections = new java.util.HashMap<>();
            for (String key : connNbt.getAllKeys()) {
                connections.put(key, connNbt.getFloat(key));
            }
        }
    }
}
