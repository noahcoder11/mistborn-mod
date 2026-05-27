package com.not_noah.mistborn_metal_arts.structure;

import com.not_noah.mistborn_metal_arts.registry.ModStructures;
import com.not_noah.mistborn_metal_arts.worldgen.KredikShawBuilder;
import com.not_noah.mistborn_metal_arts.worldgen.MetalArtsWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class KredikShawPiece extends StructurePiece {
    private static final int HORIZONTAL_RADIUS = KredikShawBuilder.PIECE_HORIZONTAL_RADIUS;
    private static final int BELOW = KredikShawBuilder.PIECE_BELOW;
    private static final int ABOVE = KredikShawBuilder.PIECE_ABOVE;

    private final BlockPos origin;
    private final boolean withWell;
    private final boolean spawnBoss;

    public KredikShawPiece(BlockPos origin, boolean withWell, boolean spawnBoss) {
        super(ModStructures.KREDIK_SHAW_PIECE.get(), 0, bounds(origin));
        this.origin = origin;
        this.withWell = withWell;
        this.spawnBoss = spawnBoss;
    }

    public KredikShawPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.KREDIK_SHAW_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.withWell = tag.getBoolean("WithWell");
        this.spawnBoss = tag.getBoolean("SpawnBoss");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OriginX", origin.getX());
        tag.putInt("OriginY", origin.getY());
        tag.putInt("OriginZ", origin.getZ());
        tag.putBoolean("WithWell", withWell);
        tag.putBoolean("SpawnBoss", spawnBoss);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        KredikShawBuilder.placeAt(level, origin, withWell, spawnBoss, box, random);
        ServerLevel serverLevel = level.getLevel();
        serverLevel.getServer().execute(() -> MetalArtsWorldgen.registerKredikShawOrigin(serverLevel, origin, withWell, spawnBoss, chunkPos));
    }

    public BlockPos origin() {
        return origin;
    }

    public boolean withWell() {
        return withWell;
    }

    public boolean spawnBoss() {
        return spawnBoss;
    }

    private static BoundingBox bounds(BlockPos origin) {
        return new BoundingBox(
                origin.getX() - HORIZONTAL_RADIUS,
                origin.getY() - BELOW,
                origin.getZ() - HORIZONTAL_RADIUS,
                origin.getX() + HORIZONTAL_RADIUS,
                origin.getY() + ABOVE,
                origin.getZ() + HORIZONTAL_RADIUS
        );
    }
}
