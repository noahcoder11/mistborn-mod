package com.not_noah.mistborn_metal_arts.network;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class MetalArtsNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MistbornMetalArts.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private MetalArtsNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ServerboundMetalActionPacket.class, ServerboundMetalActionPacket::encode, ServerboundMetalActionPacket::decode, ServerboundMetalActionPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, ClientboundMetalArtsSyncPacket.class, ClientboundMetalArtsSyncPacket::encode, ClientboundMetalArtsSyncPacket::decode, ClientboundMetalArtsSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, SyncBloodLevelPacket.class, SyncBloodLevelPacket::toBytes, SyncBloodLevelPacket::new, SyncBloodLevelPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ClientboundBloodSlashPacket.class, ClientboundBloodSlashPacket::toBytes, ClientboundBloodSlashPacket::new, ClientboundBloodSlashPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, ServerboundSetFeruchemyModePacket.class, ServerboundSetFeruchemyModePacket::encode, ServerboundSetFeruchemyModePacket::decode, ServerboundSetFeruchemyModePacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToServer(ServerboundMetalActionPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(ServerboundSetFeruchemyModePacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sync(ServerPlayer player) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> sync(player, data.serializeNBT()));
    }

    public static void sync(ServerPlayer player, net.minecraft.nbt.CompoundTag tag) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundMetalArtsSyncPacket(tag));
    }

    public static void syncBloodLevel(Entity target, float bloodLevel) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), new SyncBloodLevelPacket(target.getId(), bloodLevel));
    }

    public static void sendBloodSlash(Entity target, double ox, double oy, double oz, int slashType, float scale, float roll, int lifetime) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), new ClientboundBloodSlashPacket(target.getId(), ox, oy, oz, slashType, scale, roll, lifetime));
    }

    public static void sendBloodSlash(Entity target, double ox, double oy, double oz, int slashType, float scale, float roll, int lifetime, float projX, float projY, float projZ, boolean isArrow) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), new ClientboundBloodSlashPacket(target.getId(), ox, oy, oz, slashType, scale, roll, lifetime, projX, projY, projZ, isArrow));
    }
}
