package com.not_noah.mistborn_metal_arts.network;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundSetFeruchemyModePacket(Metal metal, int mode) {
    public static void encode(ServerboundSetFeruchemyModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.metal);
        buffer.writeInt(packet.mode);
    }

    public static ServerboundSetFeruchemyModePacket decode(FriendlyByteBuf buffer) {
        Metal metal = buffer.readEnum(Metal.class);
        int mode = buffer.readInt();
        return new ServerboundSetFeruchemyModePacket(metal, mode);
    }

    public static void handle(ServerboundSetFeruchemyModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    if (packet.metal.isFeruchemical() && data.hasFeruchemicalPower(packet.metal)) {
                        data.setFeruchemyMode(packet.metal, packet.mode);
                        // Make sure to sync back to the player
                        MetalArtsNetwork.sync(player);
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
}
