package com.not_noah.mistborn_metal_arts.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BloodDataProvider implements ICapabilitySerializable<CompoundTag> {
    private final BloodData bloodData = new BloodData();
    private final LazyOptional<IBloodData> optional = LazyOptional.of(() -> bloodData);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == MetalArtsCapabilities.BLOOD_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("BloodLevel", bloodData.getBloodLevel());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        bloodData.setBloodLevel(nbt.getFloat("BloodLevel"));
    }
}
