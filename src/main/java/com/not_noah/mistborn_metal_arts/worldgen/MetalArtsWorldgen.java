package com.not_noah.mistborn_metal_arts.worldgen;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.structure.KredikShawPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID)
public final class MetalArtsWorldgen {
    private static final int KREDIK_SLICES_PER_TICK = 2;
    private static final ConcurrentHashMap<ResourceKey<Level>, ConcurrentLinkedQueue<Long>> KREDIK_QUEUES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ResourceKey<Level>, Set<Long>> KREDIK_QUEUED_CHUNKS = new ConcurrentHashMap<>();

    private MetalArtsWorldgen() {
    }

    public static void bootstrapSecondPassHook() {
        // Forge JSON/datapack structures can layer on top of this event fallback later.
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        discoverLoadedKredikStarts(level, chunk);

        KredikShawSavedData data = KredikShawSavedData.getIfPresent(level);
        if (data == null || data.isEmpty()) {
            return;
        }
        queueKredikChunk(level, chunk.getPos());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        ConcurrentLinkedQueue<Long> queue = KREDIK_QUEUES.get(level.dimension());
        if (queue == null || queue.isEmpty()) {
            return;
        }

        Set<Long> queued = KREDIK_QUEUED_CHUNKS.get(level.dimension());
        for (int i = 0; i < KREDIK_SLICES_PER_TICK && event.haveTime(); i++) {
            Long packed = queue.poll();
            if (packed == null) {
                return;
            }
            if (queued != null) {
                queued.remove(packed);
            }

            int chunkX = ChunkPos.getX(packed);
            int chunkZ = ChunkPos.getZ(packed);
            if (!level.hasChunk(chunkX, chunkZ)) {
                continue;
            }
            placeLoadedKredikSlices(level, new ChunkPos(packed));
        }
    }

    private static void discoverLoadedKredikStarts(ServerLevel level, LevelChunk chunk) {
        for (StructureStart start : chunk.getAllStarts().values()) {
            if (!start.isValid()) {
                continue;
            }
            for (var piece : start.getPieces()) {
                if (piece instanceof KredikShawPiece kredikPiece) {
                    registerKredikShawOrigin(level, kredikPiece.origin(), kredikPiece.withWell(), kredikPiece.spawnBoss(), chunk.getPos());
                }
            }
        }
    }

    public static void registerKredikShawOrigin(ServerLevel level, BlockPos origin, boolean withWell, boolean spawnBoss, ChunkPos generatedChunk) {
        KredikShawSavedData data = KredikShawSavedData.get(level);
        boolean added = data.addOrigin(origin, withWell, spawnBoss);
        data.markChunkComplete(origin, generatedChunk);
        if (added) {
            queueKredikFootprint(level, origin);
        }
    }

    public static void queueKredikFootprint(ServerLevel level, BlockPos origin) {
        BoundingBox bounds = KredikShawBuilder.fullBounds(origin);
        int minChunkX = bounds.minX() >> 4;
        int maxChunkX = bounds.maxX() >> 4;
        int minChunkZ = bounds.minZ() >> 4;
        int maxChunkZ = bounds.maxZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                queueKredikChunk(level, new ChunkPos(chunkX, chunkZ));
            }
        }
    }

    private static void queueKredikChunk(ServerLevel level, ChunkPos chunkPos) {
        ResourceKey<Level> dimension = level.dimension();
        Set<Long> queued = KREDIK_QUEUED_CHUNKS.computeIfAbsent(dimension, key -> ConcurrentHashMap.newKeySet());
        long packed = chunkPos.toLong();
        if (queued.add(packed)) {
            KREDIK_QUEUES.computeIfAbsent(dimension, key -> new ConcurrentLinkedQueue<>()).add(packed);
        }
    }

    private static void placeLoadedKredikSlices(ServerLevel level, ChunkPos chunkPos) {
        KredikShawSavedData data = KredikShawSavedData.getIfPresent(level);
        if (data == null || data.isEmpty()) {
            return;
        }

        BoundingBox chunkBounds = KredikShawBuilder.chunkBounds(level, chunkPos);
        for (KredikShawSavedData.OriginRecord record : data.origins()) {
            if (!KredikShawBuilder.chunkIntersectsFootprint(record.origin(), chunkPos)) {
                continue;
            }
            if (data.isChunkComplete(record.origin(), chunkPos)) {
                continue;
            }

            RandomSource random = RandomSource.create(record.origin().asLong() ^ chunkPos.toLong() ^ level.getSeed());
            KredikShawBuilder.placeAt(level, record.origin(), record.withWell(), record.spawnBoss(), chunkBounds, random);
            data.markChunkComplete(record.origin(), chunkPos);
        }
    }
}
