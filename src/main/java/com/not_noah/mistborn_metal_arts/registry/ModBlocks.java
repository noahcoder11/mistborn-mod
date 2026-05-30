package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.block.MetalArtsMachineBlock;
import com.not_noah.mistborn_metal_arts.block.WellOfAscensionBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MistbornMetalArts.MOD_ID);

    public static final RegistryObject<Block> SPIKE_PRESS = machine("spike_press", 5.0F);
    public static final RegistryObject<Block> HEMALURGIC_ALTAR = BLOCKS.register("hemalurgic_altar", () -> new com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock(BlockBehaviour.Properties.of().strength(5.0F, 12.0F).sound(SoundType.METAL).noOcclusion().isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false).lightLevel(state -> 12)));
    public static final RegistryObject<Block> ATIUM_GEODE_CLUSTER = BLOCKS.register("atium_geode_cluster", () -> new Block(BlockBehaviour.Properties.of().strength(4.0F, 8.0F).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> ATIUM_GEODE = BLOCKS.register("atium_geode", () -> new Block(BlockBehaviour.Properties.of().strength(4.0F, 8.0F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> BUDDING_ATIUM = BLOCKS.register("budding_atium", () -> new net.minecraft.world.level.block.BuddingAmethystBlock(BlockBehaviour.Properties.of().strength(4.0F, 8.0F).sound(SoundType.AMETHYST).randomTicks().requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> ATIUM_CLUSTER = BLOCKS.register("atium_cluster", () -> new net.minecraft.world.level.block.AmethystClusterBlock(7, 3, BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.AMETHYST_CLUSTER).noOcclusion().lightLevel(state -> 5).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> NETHER_TRELLIUM_ORE = BLOCKS.register("nether_trellium_ore", () -> new Block(BlockBehaviour.Properties.of().strength(3.0F, 3.0F).sound(SoundType.NETHER_ORE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> LERASIUM_CACHE_BLOCK = BLOCKS.register("lerasium_cache_block", () -> new Block(BlockBehaviour.Properties.of().strength(8.0F, 24.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> METAL_CACHE = BLOCKS.register("metal_cache", () -> new Block(BlockBehaviour.Properties.of().strength(4.0F, 12.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> WELL_OF_ASCENSION_BLOCK = BLOCKS.register("well_of_ascension_block", () -> new WellOfAscensionBlock(BlockBehaviour.Properties.of().strength(12.0F, 1200.0F).lightLevel(state -> 10).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> WELL_PULSE_CORE = BLOCKS.register("well_pulse_core", () -> new WellOfAscensionBlock(BlockBehaviour.Properties.of().strength(16.0F, 1200.0F).lightLevel(state -> 14).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> SEALED_WELL_DOOR = BLOCKS.register("sealed_well_door", () -> new Block(BlockBehaviour.Properties.of().strength(18.0F, 1200.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> ANCIENT_METAL_FLOOR = BLOCKS.register("ancient_metal_floor", () -> new Block(BlockBehaviour.Properties.of().strength(8.0F, 36.0F).sound(SoundType.METAL)));
    public static final RegistryObject<Block> BLOOD_PRESERVATION_TANK = BLOCKS.register("blood_preservation_tank", () -> new com.not_noah.mistborn_metal_arts.block.BloodPreservationTankBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion().isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));
    public static final RegistryObject<Block> BLOOD_VIAL = BLOCKS.register("blood_vial", () -> new com.not_noah.mistborn_metal_arts.block.BloodVialBlock(BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GLASS).noOcclusion().isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));
    public static final RegistryObject<Block> ALUMINUM_CASING = BLOCKS.register("aluminum_casing", () -> new com.not_noah.mistborn_metal_arts.block.AluminumCasingBlock(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.METAL).noOcclusion().isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));

    public static final RegistryObject<Block> ZINC_ORE = ore("zinc_ore");
    public static final RegistryObject<Block> DEEPSLATE_ZINC_ORE = ore("deepslate_zinc_ore");
    public static final RegistryObject<Block> TIN_ORE = ore("tin_ore");
    public static final RegistryObject<Block> DEEPSLATE_TIN_ORE = ore("deepslate_tin_ore");
    public static final RegistryObject<Block> ALUMINUM_ORE = ore("aluminum_ore");
    public static final RegistryObject<Block> DEEPSLATE_ALUMINUM_ORE = ore("deepslate_aluminum_ore");
    public static final RegistryObject<Block> CHROMIUM_ORE = ore("chromium_ore");
    public static final RegistryObject<Block> DEEPSLATE_CHROMIUM_ORE = ore("deepslate_chromium_ore");
    public static final RegistryObject<Block> CADMIUM_ORE = ore("cadmium_ore");
    public static final RegistryObject<Block> DEEPSLATE_CADMIUM_ORE = ore("deepslate_cadmium_ore");
    public static final RegistryObject<Block> NICKEL_ORE = ore("nickel_ore");
    public static final RegistryObject<Block> DEEPSLATE_NICKEL_ORE = ore("deepslate_nickel_ore");
    public static final RegistryObject<Block> SILVER_ORE = ore("silver_ore");
    public static final RegistryObject<Block> DEEPSLATE_SILVER_ORE = ore("deepslate_silver_ore");
    public static final RegistryObject<Block> LEAD_ORE = ore("lead_ore");
    public static final RegistryObject<Block> DEEPSLATE_LEAD_ORE = ore("deepslate_lead_ore");
    public static final RegistryObject<Block> ASH_DEPOSIT = BLOCKS.register("ash_deposit", () -> new Block(BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.SAND)));

    private ModBlocks() {
    }

    private static RegistryObject<Block> machine(String name, float strength) {
        return BLOCKS.register(name, () -> new MetalArtsMachineBlock(BlockBehaviour.Properties.of().strength(strength, strength * 4.0F).sound(SoundType.METAL), name, "block.mistborn_metal_arts." + name + ".hint"));
    }

    private static RegistryObject<Block> ore(String name) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
