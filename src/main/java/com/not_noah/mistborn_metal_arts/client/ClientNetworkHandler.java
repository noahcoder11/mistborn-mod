package com.not_noah.mistborn_metal_arts.client;

import net.minecraft.nbt.CompoundTag;

public final class ClientNetworkHandler {
    private ClientNetworkHandler() {
    }

    public static void handleMetalArtsSync(CompoundTag tag) {
        ClientMetalArtsData.read(tag);
    }
}
