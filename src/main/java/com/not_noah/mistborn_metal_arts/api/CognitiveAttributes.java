package com.not_noah.mistborn_metal_arts.api;

public class CognitiveAttributes {
    public float mentalSpeed;
    public float wakefulness;
    public float determination;
    public float intelligence;

    public void merge(CognitiveAttributes other) {
        if (other == null)
            return;
        this.mentalSpeed += other.mentalSpeed;
        this.wakefulness += other.wakefulness;
        this.determination += other.determination;
        this.intelligence += other.intelligence;
    }
}
