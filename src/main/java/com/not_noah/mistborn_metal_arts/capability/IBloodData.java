package com.not_noah.mistborn_metal_arts.capability;

import net.minecraft.world.entity.LivingEntity;
import java.util.List;

public interface IBloodData {
    float getBloodLevel();
    void setBloodLevel(float level);
    void addBlood(float amount);
    void tickDecay(LivingEntity entity);
    
    List<BloodSlash> getSlashes();
    void addSlash(double ox, double oy, double oz, int slashType, float scale, float roll, int lifetime);
    void addSlash(double ox, double oy, double oz, int slashType, float scale, float roll, int lifetime, float projX, float projY, float projZ, boolean isArrow);
    void setSlashes(List<BloodSlash> slashes);

    List<StuckSpike> getStuckSpikes();
    void addStuckSpike(StuckSpike spike);
    void setStuckSpikes(List<StuckSpike> spikes);
}

