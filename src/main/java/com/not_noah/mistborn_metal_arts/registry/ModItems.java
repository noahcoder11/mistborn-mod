package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import com.not_noah.mistborn_metal_arts.item.LerasiumBeadItem;
import com.not_noah.mistborn_metal_arts.item.MetalVialItem;
import com.not_noah.mistborn_metal_arts.item.MetalmindItem;
import com.not_noah.mistborn_metal_arts.item.ObsidianAxeItem;
import com.not_noah.mistborn_metal_arts.item.SpikeRemovalToolItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MistbornMetalArts.MOD_ID);

    public static final EnumMap<Metal, RegistryObject<Item>> METAL_FLAKES = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_POWDERS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_BEADS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_INGOTS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_BLENDS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> RAW_ORES = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_VIALS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METALMINDS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> UNKEYED_METALMINDS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> SPIKE_BLANKS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> CHARGED_SPIKES = new EnumMap<>(Metal.class);
    public static final EnumMap<MetalbornRole, RegistryObject<Item>> METALBORN_SPAWN_EGGS = new EnumMap<>(MetalbornRole.class);

    public static final RegistryObject<Item> EMPTY_GLASS_VIAL = ITEMS.register("empty_glass_vial", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> MIXED_METAL_VIAL = ITEMS.register("mixed_metal_vial", () -> new MetalVialItem(null, true, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ALLOMANCER_TESTING_KIT = ITEMS.register("allomancer_testing_kit", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FERUCHEMIST_TESTING_KIT = ITEMS.register("feruchemist_testing_kit", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> METAL_ARTS_GUIDEBOOK = ITEMS.register("metal_arts_guidebook", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SPIKE_REMOVAL_TOOL = ITEMS.register("spike_removal_tool", () -> new SpikeRemovalToolItem(new Item.Properties().stacksTo(1).durability(64)));
    public static final RegistryObject<Item> OBSIDIAN_AXE = ITEMS.register("obsidian_axe", () -> new ObsidianAxeItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> GLASS_DAGGER = ITEMS.register("glass_dagger", () -> new com.not_noah.mistborn_metal_arts.item.GlassDaggerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> METALLURGY_TABLE = blockItem("metallurgy_table", ModBlocks.METALLURGY_TABLE);
    public static final RegistryObject<Item> METALWORKING_TABLE = blockItem("metalworking_table", ModBlocks.METALWORKING_TABLE);
    public static final RegistryObject<Item> ALLOY_FURNACE = blockItem("alloy_furnace", ModBlocks.ALLOY_FURNACE);
    public static final RegistryObject<Item> SPIKE_PRESS = blockItem("spike_press", ModBlocks.SPIKE_PRESS);
    public static final RegistryObject<Item> BIND_POINT_TABLE = blockItem("bind_point_table", ModBlocks.BIND_POINT_TABLE);
    public static final RegistryObject<Item> METALMIND_CHARGING_STAND = blockItem("metalmind_charging_stand", ModBlocks.METALMIND_CHARGING_STAND);
    public static final RegistryObject<Item> ATIUM_GEODE_CLUSTER = blockItem("atium_geode_cluster", ModBlocks.ATIUM_GEODE_CLUSTER);
    public static final RegistryObject<Item> LERASIUM_CACHE_BLOCK = blockItem("lerasium_cache_block", ModBlocks.LERASIUM_CACHE_BLOCK);
    public static final RegistryObject<Item> METAL_CACHE = blockItem("metal_cache", ModBlocks.METAL_CACHE);
    public static final RegistryObject<Item> WELL_OF_ASCENSION_BLOCK = blockItem("well_of_ascension_block", ModBlocks.WELL_OF_ASCENSION_BLOCK);
    public static final RegistryObject<Item> WELL_PULSE_CORE = blockItem("well_pulse_core", ModBlocks.WELL_PULSE_CORE);
    public static final RegistryObject<Item> SEALED_WELL_DOOR = blockItem("sealed_well_door", ModBlocks.SEALED_WELL_DOOR);
    public static final RegistryObject<Item> ANCIENT_METAL_FLOOR = blockItem("ancient_metal_floor", ModBlocks.ANCIENT_METAL_FLOOR);
    public static final RegistryObject<Item> ZINC_ORE = blockItem("zinc_ore", ModBlocks.ZINC_ORE);
    public static final RegistryObject<Item> DEEPSLATE_ZINC_ORE = blockItem("deepslate_zinc_ore", ModBlocks.DEEPSLATE_ZINC_ORE);
    public static final RegistryObject<Item> TIN_ORE = blockItem("tin_ore", ModBlocks.TIN_ORE);
    public static final RegistryObject<Item> DEEPSLATE_TIN_ORE = blockItem("deepslate_tin_ore", ModBlocks.DEEPSLATE_TIN_ORE);
    public static final RegistryObject<Item> ALUMINUM_ORE = blockItem("aluminum_ore", ModBlocks.ALUMINUM_ORE);
    public static final RegistryObject<Item> DEEPSLATE_ALUMINUM_ORE = blockItem("deepslate_aluminum_ore", ModBlocks.DEEPSLATE_ALUMINUM_ORE);
    public static final RegistryObject<Item> CHROMIUM_ORE = blockItem("chromium_ore", ModBlocks.CHROMIUM_ORE);
    public static final RegistryObject<Item> DEEPSLATE_CHROMIUM_ORE = blockItem("deepslate_chromium_ore", ModBlocks.DEEPSLATE_CHROMIUM_ORE);
    public static final RegistryObject<Item> CADMIUM_ORE = blockItem("cadmium_ore", ModBlocks.CADMIUM_ORE);
    public static final RegistryObject<Item> DEEPSLATE_CADMIUM_ORE = blockItem("deepslate_cadmium_ore", ModBlocks.DEEPSLATE_CADMIUM_ORE);
    public static final RegistryObject<Item> NICKEL_ORE = blockItem("nickel_ore", ModBlocks.NICKEL_ORE);
    public static final RegistryObject<Item> DEEPSLATE_NICKEL_ORE = blockItem("deepslate_nickel_ore", ModBlocks.DEEPSLATE_NICKEL_ORE);
    public static final RegistryObject<Item> SILVER_ORE = blockItem("silver_ore", ModBlocks.SILVER_ORE);
    public static final RegistryObject<Item> DEEPSLATE_SILVER_ORE = blockItem("deepslate_silver_ore", ModBlocks.DEEPSLATE_SILVER_ORE);
    public static final RegistryObject<Item> LEAD_ORE = blockItem("lead_ore", ModBlocks.LEAD_ORE);
    public static final RegistryObject<Item> DEEPSLATE_LEAD_ORE = blockItem("deepslate_lead_ore", ModBlocks.DEEPSLATE_LEAD_ORE);
    public static final RegistryObject<Item> ASH_DEPOSIT = blockItem("ash_deposit", ModBlocks.ASH_DEPOSIT);

    static {
        for (Metal metal : Metal.cachedValues()) {
            METAL_FLAKES.put(metal, ITEMS.register(metal.id() + "_flakes", () -> new Item(new Item.Properties())));
            METAL_POWDERS.put(metal, ITEMS.register(metal.id() + "_powder", () -> new Item(new Item.Properties())));
            if (metal == Metal.LERASIUM) {
                METAL_BEADS.put(metal, ITEMS.register("lerasium_bead", () -> new LerasiumBeadItem(new Item.Properties().stacksTo(16).fireResistant())));
            } else {
                METAL_BEADS.put(metal, ITEMS.register(metal.id() + "_bead", () -> new Item(new Item.Properties().stacksTo(32).fireResistant())));
            }
            METAL_INGOTS.put(metal, ITEMS.register(metal.id() + "_ingot", () -> new Item(new Item.Properties())));
            METAL_BLENDS.put(metal, ITEMS.register(metal.id() + "_blend", () -> new Item(new Item.Properties())));
            RAW_ORES.put(metal, ITEMS.register("raw_" + metal.id() + "_ore", () -> new Item(new Item.Properties())));
            if (metal.isAllomantic()) {
                METAL_VIALS.put(metal, ITEMS.register(metal.id() + "_vial", () -> new MetalVialItem(metal, false, new Item.Properties().stacksTo(16))));
            }
            if (metal.isFeruchemical()) {
                METALMINDS.put(metal, ITEMS.register(metal.id() + "_metalmind", () -> new MetalmindItem(metal, false, new Item.Properties().stacksTo(1))));
                UNKEYED_METALMINDS.put(metal, ITEMS.register("unkeyed_" + metal.id() + "_metalmind", () -> new MetalmindItem(metal, true, new Item.Properties().stacksTo(1))));
            }
            SPIKE_BLANKS.put(metal, ITEMS.register(metal.id() + "_spike", () -> new HemalurgicSpikeItem(metal, false, new Item.Properties().stacksTo(16))));
            CHARGED_SPIKES.put(metal, ITEMS.register("charged_" + metal.id() + "_spike", () -> new HemalurgicSpikeItem(metal, true, new Item.Properties().stacksTo(1).fireResistant())));
        }
        for (MetalbornRole role : MetalbornRole.cachedValues()) {
            METALBORN_SPAWN_EGGS.put(role, spawnEgg(role));
        }
    }

    private ModItems() {
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> spawnEgg(MetalbornRole role) {
        @SuppressWarnings("unchecked")
        RegistryObject<? extends EntityType<? extends Mob>> entityType = (RegistryObject<? extends EntityType<? extends Mob>>) (RegistryObject<?>) ModEntityTypes.METALBORN.get(role);
        return ITEMS.register(role.id() + "_spawn_egg", () -> new ForgeSpawnEggItem(entityType, role.eggBaseColor(), role.eggHighlightColor(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
