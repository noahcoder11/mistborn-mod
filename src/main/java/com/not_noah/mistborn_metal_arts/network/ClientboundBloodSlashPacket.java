package com.not_noah.mistborn_metal_arts.network;

import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundBloodSlashPacket {
    private final int entityId;
    private final double ox;
    private final double oy;
    private final double oz;
    private final int slashType;
    private final float scale;
    private final float roll;
    private final int lifetime;
    private final float projX;
    private final float projY;
    private final float projZ;
    private final boolean isArrow;

    public ClientboundBloodSlashPacket(int entityId, double ox, double oy, double oz, int slashType, float scale, float roll, int lifetime) {
        this(entityId, ox, oy, oz, slashType, scale, roll, lifetime, 0.0F, 0.0F, 0.0F, false);
    }

    public ClientboundBloodSlashPacket(int entityId, double ox, double oy, double oz, int slashType, float scale, float roll, int lifetime, float projX, float projY, float projZ, boolean isArrow) {
        this.entityId = entityId;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.slashType = slashType;
        this.scale = scale;
        this.roll = roll;
        this.lifetime = lifetime;
        this.projX = projX;
        this.projY = projY;
        this.projZ = projZ;
        this.isArrow = isArrow;
    }

    public ClientboundBloodSlashPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.ox = buf.readDouble();
        this.oy = buf.readDouble();
        this.oz = buf.readDouble();
        this.slashType = buf.readInt();
        this.scale = buf.readFloat();
        this.roll = buf.readFloat();
        this.lifetime = buf.readInt();
        this.projX = buf.readFloat();
        this.projY = buf.readFloat();
        this.projZ = buf.readFloat();
        this.isArrow = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(ox);
        buf.writeDouble(oy);
        buf.writeDouble(oz);
        buf.writeInt(slashType);
        buf.writeFloat(scale);
        buf.writeFloat(roll);
        buf.writeInt(lifetime);
        buf.writeFloat(projX);
        buf.writeFloat(projY);
        buf.writeFloat(projZ);
        buf.writeBoolean(isArrow);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client-side execution
            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(entityId);
                if (entity instanceof LivingEntity living) {
                    living.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                        data.addSlash(ox, oy, oz, slashType, scale, roll, lifetime, projX, projY, projZ, isArrow);
                    });
                }
            }
        });
        return true;
    }
}
