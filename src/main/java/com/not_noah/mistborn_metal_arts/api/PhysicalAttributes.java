package com.not_noah.mistborn_metal_arts.api;

public class PhysicalAttributes {
    public float strength = 1.0F;
    public float sight = 1.0F;
    public float zoom = 1.0F;
    public float speed = 1.0F;
    public float resistance = 1.0F;
    public float weight = 1.0F;
    public float health = 1.0F;

    public void merge(PhysicalAttributes other) {
        if (other == null)
            return;
        this.strength += other.strength;
        this.sight += other.sight;
        this.zoom += other.zoom;
        this.speed += other.speed;
        this.resistance += other.resistance;
        this.weight += other.weight;
        this.health += other.health;
    }
}
