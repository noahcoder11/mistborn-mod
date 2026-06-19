package com.not_noah.mistborn_metal_arts.capability;

import com.not_noah.mistborn_metal_arts.api.SpiritWeb;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpiritWebProvider implements ICapabilitySerializable<CompoundTag> {
    private final SpiritWeb spiritWeb = new SpiritWeb();
    private final LazyOptional<SpiritWeb> optional = LazyOptional.of(() -> spiritWeb);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == MetalArtsCapabilities.SPIRIT_WEB ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return spiritWeb.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        spiritWeb.deserializeNBT(nbt);
    }
}
