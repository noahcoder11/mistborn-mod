package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.not_noah.mistborn_metal_arts.block.entity.MetalArtsMachineBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MistbornMetalArts.MOD_ID);

    public static final RegistryObject<BlockEntityType<MetalArtsMachineBlockEntity>> METAL_ARTS_MACHINE = BLOCK_ENTITIES.register("metal_arts_machine", () -> BlockEntityType.Builder.of(MetalArtsMachineBlockEntity::new, 
            ModBlocks.SPIKE_PRESS.get()
    ).build(null));

    public static final RegistryObject<BlockEntityType<com.not_noah.mistborn_metal_arts.block.entity.BloodPreservationTankBlockEntity>> BLOOD_PRESERVATION_TANK = BLOCK_ENTITIES.register("blood_preservation_tank", () -> BlockEntityType.Builder.of(com.not_noah.mistborn_metal_arts.block.entity.BloodPreservationTankBlockEntity::new, 
            ModBlocks.BLOOD_PRESERVATION_TANK.get()
    ).build(null));

    public static final RegistryObject<BlockEntityType<com.not_noah.mistborn_metal_arts.block.entity.BloodVialBlockEntity>> BLOOD_VIAL = BLOCK_ENTITIES.register("blood_vial", () -> BlockEntityType.Builder.of(com.not_noah.mistborn_metal_arts.block.entity.BloodVialBlockEntity::new, 
            ModBlocks.BLOOD_VIAL.get()
    ).build(null));

    public static final RegistryObject<BlockEntityType<com.not_noah.mistborn_metal_arts.block.entity.AluminumCasingBlockEntity>> ALUMINUM_CASING = BLOCK_ENTITIES.register("aluminum_casing", () -> BlockEntityType.Builder.of(com.not_noah.mistborn_metal_arts.block.entity.AluminumCasingBlockEntity::new, 
            ModBlocks.ALUMINUM_CASING.get()
    ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
