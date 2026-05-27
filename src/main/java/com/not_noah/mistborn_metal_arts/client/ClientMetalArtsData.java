package com.not_noah.mistborn_metal_arts.client;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import net.minecraft.nbt.CompoundTag;

public final class ClientMetalArtsData {
    private static final MetalArtsData DATA = new MetalArtsData();
    private static Metal localSelected = Metal.IRON;

    private ClientMetalArtsData() {
    }

    public static void read(CompoundTag tag) {
        DATA.deserializeNBT(tag, false);
        localSelected = DATA.selectedMetal();
        if (!DATA.hasAllomanticPower(localSelected) && !DATA.hasFeruchemicalPower(localSelected)) {
            for (Metal m : Metal.cachedValues()) {
                if (DATA.hasAllomanticPower(m) || DATA.hasFeruchemicalPower(m)) {
                    localSelected = m;
                    break;
                }
            }
        }
    }

    public static MetalArtsData data() {
        return DATA;
    }

    public static Metal selectedMetal() {
        return localSelected;
    }

    public static void setLocalSelected(Metal metal) {
        localSelected = metal;
    }
}
