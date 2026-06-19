package com.not_noah.mistborn_metal_arts.network;

import com.not_noah.mistborn_metal_arts.client.ClientNetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundSpiritWebSyncPacket(CompoundTag tag) {
    public static void encode(ClientboundSpiritWebSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.tag);
    }

    public static ClientboundSpiritWebSyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new ClientboundSpiritWebSyncPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(ClientboundSpiritWebSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientNetworkHandler.handleSpiritWebSync(packet.tag)));
        context.setPacketHandled(true);
    }
}
