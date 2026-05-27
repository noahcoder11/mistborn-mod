package com.not_noah.mistborn_metal_arts.network;

import com.not_noah.mistborn_metal_arts.client.ClientNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundMetalArtsSyncPacket(CompoundTag tag) {
    public static void encode(ClientboundMetalArtsSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.tag);
    }

    public static ClientboundMetalArtsSyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new ClientboundMetalArtsSyncPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(ClientboundMetalArtsSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkHandler.handleMetalArtsSync(packet.tag)));
        context.setPacketHandled(true);
    }
}
