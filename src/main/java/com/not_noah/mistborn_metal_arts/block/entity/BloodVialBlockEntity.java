package com.not_noah.mistborn_metal_arts.block.entity;

import com.not_noah.mistborn_metal_arts.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BloodVialBlockEntity extends BlockEntity {
    private ItemStack storedSpike = ItemStack.EMPTY;
    private long harvestTime = 0L;

    public BloodVialBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOD_VIAL.get(), pos, state);
    }

    public boolean hasSpike() {
        return !storedSpike.isEmpty();
    }

    public ItemStack getStoredSpike() {
        return storedSpike;
    }

    public void setStoredSpike(ItemStack spike) {
        this.storedSpike = spike;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public long getHarvestTime() {
        return harvestTime;
    }

    public void setHarvestTime(long harvestTime) {
        this.harvestTime = harvestTime;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!storedSpike.isEmpty()) {
            CompoundTag spikeTag = new CompoundTag();
            storedSpike.save(spikeTag);
            tag.put("StoredSpike", spikeTag);
        }
        tag.putLong("HarvestTime", harvestTime);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("StoredSpike")) {
            this.storedSpike = ItemStack.of(tag.getCompound("StoredSpike"));
        } else {
            this.storedSpike = ItemStack.EMPTY;
        }
        this.harvestTime = tag.getLong("HarvestTime");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
