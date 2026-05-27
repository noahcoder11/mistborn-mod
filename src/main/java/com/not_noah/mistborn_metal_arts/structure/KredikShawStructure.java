package com.not_noah.mistborn_metal_arts.structure;

import com.not_noah.mistborn_metal_arts.registry.ModStructures;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class KredikShawStructure extends Structure {
    public static final Codec<KredikShawStructure> CODEC = simpleCodec(KredikShawStructure::new);

    public KredikShawStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!ServerConfig.VALUES.kredikShawEnabled.get()) {
            return Optional.empty();
        }
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (y <= context.heightAccessor().getMinBuildHeight() + 8 || y >= context.heightAccessor().getMaxBuildHeight() - 24) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x, y, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new KredikShawPiece(origin, true, true))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.KREDIK_SHAW.get();
    }
}
