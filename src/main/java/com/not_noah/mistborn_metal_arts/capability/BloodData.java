package com.not_noah.mistborn_metal_arts.capability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BloodData implements IBloodData {
    private float bloodLevel = 0.0f;
    private final List<BloodSlash> slashes = new CopyOnWriteArrayList<>();

    @Override
    public float getBloodLevel() {
        return bloodLevel;
    }

    @Override
    public void setBloodLevel(float level) {
        this.bloodLevel = Mth.clamp(level, 0.0f, 1.0f);
    }

    @Override
    public void addBlood(float amount) {
        setBloodLevel(this.bloodLevel + amount);
    }

    @Override
    public List<BloodSlash> getSlashes() {
        return slashes;
    }

    @Override
    public void addSlash(double ox, double oy, double oz, int slashType, float scale, float roll, int lifetime) {
        this.slashes.add(new BloodSlash(ox, oy, oz, slashType, scale, roll, lifetime));
    }

    @Override
    public void addSlash(double ox, double oy, double oz, int slashType, float scale, float roll, int lifetime, float projX, float projY, float projZ, boolean isArrow) {
        this.slashes.add(new BloodSlash(ox, oy, oz, slashType, scale, roll, lifetime, projX, projY, projZ, isArrow));
    }

    @Override
    public void setSlashes(List<BloodSlash> slashes) {
        this.slashes.clear();
        if (slashes != null) {
            this.slashes.addAll(slashes);
        }
    }

    @Override
    public void tickDecay(LivingEntity entity) {
        // Tick and prune expired slashes (runs on both client and server)
        if (!slashes.isEmpty()) {
            slashes.forEach(BloodSlash::tick);
            slashes.removeIf(BloodSlash::isExpired);
        }

        if (bloodLevel <= 0.0f) return;

        if (entity.isInWater() || entity.isInWaterRainOrBubble()) {
            // Rapid wash off active wet blood completely in water or rain
            setBloodLevel(Math.max(0.0f, bloodLevel - 0.05f));
            // Rapidly wash off slashes in water too!
            if (!slashes.isEmpty() && entity.getRandom().nextFloat() < 0.2f) {
                slashes.clear();
            }
        } else {
            // Slow decay (takes ~100 seconds to clear from 1.0)
            setBloodLevel(Math.max(0.0f, bloodLevel - 0.0005f));
        }
    }
}

