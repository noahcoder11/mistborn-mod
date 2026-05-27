package com.not_noah.mistborn_metal_arts.capability;

public class BloodSlash {
    private final double ox;
    private final double oy;
    private final double oz;
    private final int slashType;
    private final float scale;
    private final float roll;
    private final int maxLifetime;
    private final float projX;
    private final float projY;
    private final float projZ;
    private final boolean isArrow;
    private int age;

    public BloodSlash(double ox, double oy, double oz, int slashType, float scale, float roll, int maxLifetime) {
        this(ox, oy, oz, slashType, scale, roll, maxLifetime, 0.0F, 0.0F, 0.0F, false);
    }

    public BloodSlash(double ox, double oy, double oz, int slashType, float scale, float roll, int maxLifetime, float projX, float projY, float projZ, boolean isArrow) {
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.slashType = slashType;
        this.scale = scale;
        this.roll = roll;
        this.maxLifetime = maxLifetime;
        this.projX = projX;
        this.projY = projY;
        this.projZ = projZ;
        this.isArrow = isArrow;
        this.age = 0;
    }

    public double getOx() {
        return ox;
    }

    public double getOy() {
        return oy;
    }

    public double getOz() {
        return oz;
    }

    public int getSlashType() {
        return slashType;
    }

    public float getScale() {
        return scale;
    }

    public float getRoll() {
        return roll;
    }

    public int getMaxLifetime() {
        return maxLifetime;
    }

    public int getAge() {
        return age;
    }

    public float getProjX() {
        return projX;
    }

    public float getProjY() {
        return projY;
    }

    public float getProjZ() {
        return projZ;
    }

    public boolean isArrow() {
        return isArrow;
    }

    public void tick() {
        this.age++;
    }

    public boolean isExpired() {
        return this.age >= this.maxLifetime;
    }

    public float getAlpha() {
        float lifeRatio = (float) this.age / (float) this.maxLifetime;
        if (lifeRatio > 0.6F) {
            return 1.0F - ((lifeRatio - 0.6F) / 0.4F);
        }
        return 1.0F;
    }
}
