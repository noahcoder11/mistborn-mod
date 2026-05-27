package com.not_noah.mistborn_metal_arts.network;

import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncBloodLevelPacket {
    private final int entityId;
    private final float bloodLevel;

    public SyncBloodLevelPacket(int entityId, float bloodLevel) {
        this.entityId = entityId;
        this.bloodLevel = bloodLevel;
    }

    public SyncBloodLevelPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.bloodLevel = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeFloat(bloodLevel);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client-side execution
            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(entityId);
                if (entity instanceof LivingEntity living) {
                    living.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                        data.setBloodLevel(bloodLevel);
                    });
                }
            }
        });
        return true;
    }
}
