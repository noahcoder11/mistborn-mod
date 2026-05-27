package com.not_noah.mistborn_metal_arts.network;

import com.not_noah.mistborn_metal_arts.allomancy.AllomancyManager;
import com.not_noah.mistborn_metal_arts.api.Metal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerboundMetalActionPacket(MetalAction action, Metal metal) {
    public static void encode(ServerboundMetalActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeInt(packet.metal == null ? -1 : packet.metal.ordinal());
    }

    public static ServerboundMetalActionPacket decode(FriendlyByteBuf buffer) {
        MetalAction action = buffer.readEnum(MetalAction.class);
        int ordinal = buffer.readInt();
        Metal metal = ordinal >= 0 && ordinal < Metal.cachedValues().length ? Metal.cachedValues()[ordinal] : Metal.IRON;
        return new ServerboundMetalActionPacket(action, metal);
    }

    public static void handle(ServerboundMetalActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                AllomancyManager.handleAction(player, packet.action, packet.metal);
            }
        });
        context.setPacketHandled(true);
    }
}
