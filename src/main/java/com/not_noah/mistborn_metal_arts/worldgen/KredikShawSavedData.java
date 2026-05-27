package com.not_noah.mistborn_metal_arts.worldgen;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tracks naturally discovered Kredik Shaw origins and the chunks already filled by the procedural builder.
 *
 * <p>Kredik Shaw is larger than vanilla's normal structure reference search square.  The registered structure
 * still gives /locate a real center, then this data lets loaded outer chunks finish their own slice exactly once.</p>
 */
public final class KredikShawSavedData extends SavedData {
    private static final String NAME = MistbornMetalArts.MOD_ID + "_kredik_shaw";

    private final Map<Long, OriginRecord> origins = new LinkedHashMap<>();
    private final Set<String> completedSlices = new HashSet<>();

    public static KredikShawSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(KredikShawSavedData::load, KredikShawSavedData::new, NAME);
    }

    public static KredikShawSavedData getIfPresent(ServerLevel level) {
        return level.getDataStorage().get(KredikShawSavedData::load, NAME);
    }

    private static KredikShawSavedData load(CompoundTag tag) {
        KredikShawSavedData data = new KredikShawSavedData();

        ListTag originTags = tag.getList("Origins", Tag.TAG_COMPOUND);
        for (int i = 0; i < originTags.size(); i++) {
            CompoundTag originTag = originTags.getCompound(i);
            BlockPos origin = BlockPos.of(originTag.getLong("Pos"));
            boolean withWell = originTag.getBoolean("WithWell");
            boolean spawnBoss = originTag.getBoolean("SpawnBoss");
            data.origins.put(origin.asLong(), new OriginRecord(origin, withWell, spawnBoss));
        }

        ListTag completedTags = tag.getList("CompletedSlices", Tag.TAG_STRING);
        for (int i = 0; i < completedTags.size(); i++) {
            data.completedSlices.add(completedTags.getString(i));
        }
        return data;
    }

    public boolean addOrigin(BlockPos origin, boolean withWell, boolean spawnBoss) {
        long key = origin.asLong();
        OriginRecord existing = origins.get(key);
        if (existing == null) {
            origins.put(key, new OriginRecord(origin.immutable(), withWell, spawnBoss));
            setDirty();
            return true;
        }

        boolean mergedWithWell = existing.withWell() || withWell;
        boolean mergedSpawnBoss = existing.spawnBoss() || spawnBoss;
        if (mergedWithWell != existing.withWell() || mergedSpawnBoss != existing.spawnBoss()) {
            origins.put(key, new OriginRecord(existing.origin(), mergedWithWell, mergedSpawnBoss));
            setDirty();
        }
        return false;
    }

    public boolean isEmpty() {
        return origins.isEmpty();
    }

    public Collection<OriginRecord> origins() {
        return origins.values();
    }

    public boolean isChunkComplete(BlockPos origin, ChunkPos chunkPos) {
        return completedSlices.contains(sliceKey(origin, chunkPos));
    }

    public void markChunkComplete(BlockPos origin, ChunkPos chunkPos) {
        if (completedSlices.add(sliceKey(origin, chunkPos))) {
            setDirty();
        }
    }

    private static String sliceKey(BlockPos origin, ChunkPos chunkPos) {
        return origin.asLong() + ":" + chunkPos.toLong();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag originTags = new ListTag();
        for (OriginRecord record : origins.values()) {
            CompoundTag originTag = new CompoundTag();
            originTag.putLong("Pos", record.origin().asLong());
            originTag.putBoolean("WithWell", record.withWell());
            originTag.putBoolean("SpawnBoss", record.spawnBoss());
            originTags.add(originTag);
        }
        tag.put("Origins", originTags);

        ListTag completedTags = new ListTag();
        for (String key : completedSlices) {
            completedTags.add(StringTag.valueOf(key));
        }
        tag.put("CompletedSlices", completedTags);
        return tag;
    }

    public record OriginRecord(BlockPos origin, boolean withWell, boolean spawnBoss) {
    }
}
