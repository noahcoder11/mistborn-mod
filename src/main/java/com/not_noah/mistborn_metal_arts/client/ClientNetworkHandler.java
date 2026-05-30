package com.not_noah.mistborn_metal_arts.client;

import net.minecraft.nbt.CompoundTag;

public final class ClientNetworkHandler {
    private ClientNetworkHandler() {
    }

    public static void handleMetalArtsSync(CompoundTag tag) {
        ClientMetalArtsData.read(tag);
        net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                data.deserializeNBT(tag, false);
            });
        }
    }
}
