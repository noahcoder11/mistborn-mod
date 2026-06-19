package com.not_noah.mistborn_metal_arts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class ClientboundAtiumShadowsPacket {
    private final int targetId;
    private final double[] px;
    private final double[] py;
    private final double[] pz;

    public ClientboundAtiumShadowsPacket(int targetId, Vec3[] positions) {
        this.targetId = targetId;
        this.px = new double[positions.length];
        this.py = new double[positions.length];
        this.pz = new double[positions.length];
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] != null) {
                this.px[i] = positions[i].x;
                this.py[i] = positions[i].y;
                this.pz[i] = positions[i].z;
            }
        }
    }

    public ClientboundAtiumShadowsPacket(FriendlyByteBuf buf) {
        this.targetId = buf.readInt();
        int len = buf.readInt();
        this.px = new double[len];
        this.py = new double[len];
        this.pz = new double[len];
        for (int i = 0; i < len; i++) {
            this.px[i] = buf.readDouble();
            this.py[i] = buf.readDouble();
            this.pz[i] = buf.readDouble();
        }
    }

    public static ClientboundAtiumShadowsPacket decode(FriendlyByteBuf buf) {
        return new ClientboundAtiumShadowsPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.targetId);
        buf.writeInt(this.px.length);
        for (int i = 0; i < this.px.length; i++) {
            buf.writeDouble(this.px[i]);
            buf.writeDouble(this.py[i]);
            buf.writeDouble(this.pz[i]);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            com.not_noah.mistborn_metal_arts.client.MetalArtsClientEvents.handleAtiumShadowsSync(this.targetId, this.px, this.py, this.pz);
        });
        context.setPacketHandled(true);
    }
}
