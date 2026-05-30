package com.not_noah.mistborn_metal_arts.capability;

import com.not_noah.mistborn_metal_arts.api.Metal;
import net.minecraft.nbt.CompoundTag;

public class StuckSpike {
    private final Metal metal;
    private final boolean charged;
    private final String powerType;
    private final Metal powerMetal;
    private final float strength;
    private final double ox, oy, oz;
    private final float rx, ry, rz;

    public StuckSpike(Metal metal, boolean charged, String powerType, Metal powerMetal, float strength, double ox, double oy, double oz, float rx, float ry, float rz) {
        this.metal = metal;
        this.charged = charged;
        this.powerType = powerType != null ? powerType : "";
        this.powerMetal = powerMetal != null ? powerMetal : metal;
        this.strength = strength;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
    }

    public Metal getMetal() { return metal; }
    public boolean isCharged() { return charged; }
    public String getPowerType() { return powerType; }
    public Metal getPowerMetal() { return powerMetal; }
    public float getStrength() { return strength; }
    public double getOx() { return ox; }
    public double getOy() { return oy; }
    public double getOz() { return oz; }
    public float getRx() { return rx; }
    public float getRy() { return ry; }
    public float getRz() { return rz; }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Metal", metal.name());
        tag.putBoolean("Charged", charged);
        tag.putString("PowerType", powerType);
        tag.putString("PowerMetal", powerMetal.name());
        tag.putFloat("Strength", strength);
        tag.putDouble("ox", ox);
        tag.putDouble("oy", oy);
        tag.putDouble("oz", oz);
        tag.putFloat("rx", rx);
        tag.putFloat("ry", ry);
        tag.putFloat("rz", rz);
        return tag;
    }

    public static StuckSpike deserializeNBT(CompoundTag tag) {
        Metal metal = Metal.valueOf(tag.getString("Metal"));
        boolean charged = tag.getBoolean("Charged");
        String powerType = tag.getString("PowerType");
        Metal powerMetal = Metal.valueOf(tag.getString("PowerMetal"));
        float strength = tag.getFloat("Strength");
        double ox = tag.getDouble("ox");
        double oy = tag.getDouble("oy");
        double oz = tag.getDouble("oz");
        float rx = tag.getFloat("rx");
        float ry = tag.getFloat("ry");
        float rz = tag.getFloat("rz");
        return new StuckSpike(metal, charged, powerType, powerMetal, strength, ox, oy, oz, rx, ry, rz);
    }
}
