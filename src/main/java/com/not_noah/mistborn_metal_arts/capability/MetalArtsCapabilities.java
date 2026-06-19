package com.not_noah.mistborn_metal_arts.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class MetalArtsCapabilities {
    public static final Capability<MetalArtsData> METAL_ARTS = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<IBloodData> BLOOD_DATA = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final Capability<com.not_noah.mistborn_metal_arts.api.SpiritWeb> SPIRIT_WEB = CapabilityManager.get(new CapabilityToken<>() {
    });

    private MetalArtsCapabilities() {
    }
}
