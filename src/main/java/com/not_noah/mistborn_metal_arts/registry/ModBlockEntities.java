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
            ModBlocks.METALLURGY_TABLE.get(),
            ModBlocks.METALWORKING_TABLE.get(),
            ModBlocks.ALLOY_FURNACE.get(),
            ModBlocks.SPIKE_PRESS.get(),
            ModBlocks.BIND_POINT_TABLE.get(),
            ModBlocks.METALMIND_CHARGING_STAND.get()
    ).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
