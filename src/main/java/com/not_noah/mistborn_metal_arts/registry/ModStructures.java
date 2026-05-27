package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.structure.KredikShawPiece;
import com.not_noah.mistborn_metal_arts.structure.KredikShawStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MistbornMetalArts.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MistbornMetalArts.MOD_ID);

    public static final RegistryObject<StructureType<KredikShawStructure>> KREDIK_SHAW = STRUCTURE_TYPES.register("kredik_shaw", () -> (StructureType<KredikShawStructure>) () -> KredikShawStructure.CODEC);
    public static final RegistryObject<StructurePieceType> KREDIK_SHAW_PIECE = STRUCTURE_PIECES.register("kredik_shaw_piece", () -> (context, tag) -> new KredikShawPiece(context, tag));

    private ModStructures() {
    }

    public static void register(IEventBus bus) {
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PIECES.register(bus);
    }
}
