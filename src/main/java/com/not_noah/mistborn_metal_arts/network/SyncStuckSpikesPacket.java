package com.not_noah.mistborn_metal_arts.network;

import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.StuckSpike;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncStuckSpikesPacket {
    private final int entityId;
    private final List<StuckSpike> spikes;
    private final boolean restrained;
    private final BlockPos altarPos;

    public SyncStuckSpikesPacket(int entityId, List<StuckSpike> spikes, boolean restrained, BlockPos altarPos) {
        this.entityId = entityId;
        this.spikes = spikes;
        this.restrained = restrained;
        this.altarPos = altarPos;
    }

    public SyncStuckSpikesPacket(LivingEntity living, List<StuckSpike> spikes) {
        this.entityId = living.getId();
        this.spikes = spikes;
        boolean isRestrained = false;
        BlockPos pos = null;
        if (living instanceof Player player) {
            var cap = player.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
            if (cap != null && cap.isRestrained()) {
                isRestrained = true;
                pos = cap.getRestrainedAltarPos();
            }
        } else {
            CompoundTag nbt = living.getPersistentData();
            if (nbt.getBoolean("RestrainedAltar")) {
                isRestrained = true;
                pos = BlockPos.of(nbt.getLong("RestrainedAltarPos"));
            }
        }
        this.restrained = isRestrained;
        this.altarPos = pos;
    }

    public SyncStuckSpikesPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.spikes = new ArrayList<>();
        CompoundTag tag = buf.readNbt();
        boolean isRestrained = false;
        BlockPos pos = null;
        if (tag != null) {
            if (tag.contains("Spikes")) {
                ListTag list = tag.getList("Spikes", 10);
                for (int i = 0; i < list.size(); i++) {
                    spikes.add(StuckSpike.deserializeNBT(list.getCompound(i)));
                }
            }
            isRestrained = tag.getBoolean("Restrained");
            if (tag.contains("AltarPos")) {
                pos = BlockPos.of(tag.getLong("AltarPos"));
            }
        }
        this.restrained = isRestrained;
        this.altarPos = pos;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (StuckSpike spike : spikes) {
            list.add(spike.serializeNBT());
        }
        tag.put("Spikes", list);
        tag.putBoolean("Restrained", restrained);
        if (altarPos != null) {
            tag.putLong("AltarPos", altarPos.asLong());
        }
        buf.writeNbt(tag);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                Entity entity = Minecraft.getInstance().level.getEntity(entityId);
                if (entity instanceof LivingEntity living) {
                    living.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                        data.setStuckSpikes(spikes);
                    });
                    
                    CompoundTag nbt = living.getPersistentData();
                    nbt.putBoolean("ClientRestrained", restrained);
                    if (restrained && altarPos != null) {
                        nbt.putLong("ClientAltarPos", altarPos.asLong());
                    } else {
                        nbt.remove("ClientAltarPos");
                    }
                }
            }
        });
        return true;
    }
}
