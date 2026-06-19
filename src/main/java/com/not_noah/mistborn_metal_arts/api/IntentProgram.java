package com.not_noah.mistborn_metal_arts.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;

public abstract class IntentProgram {
    public final String systemIdentifier;

    public IntentProgram(String systemIdentifier) {
        this.systemIdentifier = systemIdentifier;
    }

    public abstract void execute(LivingEntity user, Investiture fuel);

    public abstract void serializeToNBT(CompoundTag tag);
    public abstract void deserializeFromNBT(CompoundTag tag);
}
