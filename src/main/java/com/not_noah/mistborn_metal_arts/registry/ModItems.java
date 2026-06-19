package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import com.not_noah.mistborn_metal_arts.item.LerasiumBeadItem;
import com.not_noah.mistborn_metal_arts.item.LerasatiumBeadItem;
import com.not_noah.mistborn_metal_arts.item.AlloyBeadItem;
import com.not_noah.mistborn_metal_arts.item.SpiritualCleansingTalismanItem;
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
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            MistbornMetalArts.MOD_ID);

    public static final EnumMap<Metal, RegistryObject<Item>> METAL_FLAKES = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_POWDERS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_BEADS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_INGOTS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_BLENDS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> RAW_ORES = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METAL_VIALS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METALMIND_RINGS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> UNKEYED_METALMIND_RINGS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METALMIND_BRACERS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> UNKEYED_METALMIND_BRACERS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> METALMIND_NECKLACES = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> UNKEYED_METALMIND_NECKLACES = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> SPIKE_BLANKS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> CHARGED_SPIKES = new EnumMap<>(Metal.class);
    public static final EnumMap<MetalbornRole, RegistryObject<Item>> METALBORN_SPAWN_EGGS = new EnumMap<>(
            MetalbornRole.class);
    public static final EnumMap<Metal, RegistryObject<Item>> LERASIUM_ALLOY_BEADS = new EnumMap<>(Metal.class);
    public static final EnumMap<Metal, RegistryObject<Item>> LERASATIUM_ALLOY_BEADS = new EnumMap<>(Metal.class);

    public static final RegistryObject<Item> EMPTY_GLASS_VIAL = ITEMS.register("empty_glass_vial",
            () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> MIXED_METAL_VIAL = ITEMS.register("mixed_metal_vial",
            () -> new MetalVialItem(null, true, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> HEMALURGIC_MANUSCRIPT = ITEMS.register("hemalurgic_manuscript",
            () -> new com.not_noah.mistborn_metal_arts.item.HemalurgicManuscriptItem(
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SPIKE_REMOVAL_TOOL = ITEMS.register("spike_removal_tool",
            () -> new SpikeRemovalToolItem(new Item.Properties().stacksTo(1).durability(64)));
    public static final RegistryObject<Item> OBSIDIAN_AXE = ITEMS.register("obsidian_axe",
            () -> new ObsidianAxeItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> GLASS_DAGGER = ITEMS.register("glass_dagger",
            () -> new com.not_noah.mistborn_metal_arts.item.GlassDaggerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BLOOD_VIAL = ITEMS.register("blood_vial",
            () -> new com.not_noah.mistborn_metal_arts.item.BloodVialItem(ModBlocks.BLOOD_VIAL.get(),
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ALUMINUM_CASING = ITEMS.register("aluminum_casing",
            () -> new com.not_noah.mistborn_metal_arts.item.AluminumCasingItem(ModBlocks.ALUMINUM_CASING.get(),
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SPIKE_PRESS = blockItem("spike_press", ModBlocks.SPIKE_PRESS);
    public static final RegistryObject<Item> HEMALURGIC_ALTAR = blockItem("hemalurgic_altar",
            ModBlocks.HEMALURGIC_ALTAR);
    public static final RegistryObject<Item> BLOOD_PRESERVATION_TANK = ITEMS.register("blood_preservation_tank",
            () -> new com.not_noah.mistborn_metal_arts.item.BloodPreservationTankItem(
                    ModBlocks.BLOOD_PRESERVATION_TANK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ATIUM_GEODE = blockItem("atium_geode", ModBlocks.ATIUM_GEODE);
    public static final RegistryObject<Item> BUDDING_ATIUM = blockItem("budding_atium", ModBlocks.BUDDING_ATIUM);
    public static final RegistryObject<Item> ATIUM_CLUSTER = blockItem("atium_cluster", ModBlocks.ATIUM_CLUSTER);
    public static final RegistryObject<Item> NETHER_TRELLIUM_ORE = blockItem("nether_trellium_ore",
            ModBlocks.NETHER_TRELLIUM_ORE);
    public static final RegistryObject<Item> ZINC_ORE = blockItem("zinc_ore", ModBlocks.ZINC_ORE);
    public static final RegistryObject<Item> DEEPSLATE_ZINC_ORE = blockItem("deepslate_zinc_ore",
            ModBlocks.DEEPSLATE_ZINC_ORE);
    public static final RegistryObject<Item> TIN_ORE = blockItem("tin_ore", ModBlocks.TIN_ORE);
    public static final RegistryObject<Item> DEEPSLATE_TIN_ORE = blockItem("deepslate_tin_ore",
            ModBlocks.DEEPSLATE_TIN_ORE);
    public static final RegistryObject<Item> ALUMINUM_ORE = blockItem("aluminum_ore", ModBlocks.ALUMINUM_ORE);
    public static final RegistryObject<Item> DEEPSLATE_ALUMINUM_ORE = blockItem("deepslate_aluminum_ore",
            ModBlocks.DEEPSLATE_ALUMINUM_ORE);
    public static final RegistryObject<Item> CHROMIUM_ORE = blockItem("chromium_ore", ModBlocks.CHROMIUM_ORE);
    public static final RegistryObject<Item> DEEPSLATE_CHROMIUM_ORE = blockItem("deepslate_chromium_ore",
            ModBlocks.DEEPSLATE_CHROMIUM_ORE);
    public static final RegistryObject<Item> CADMIUM_ORE = blockItem("cadmium_ore", ModBlocks.CADMIUM_ORE);
    public static final RegistryObject<Item> DEEPSLATE_CADMIUM_ORE = blockItem("deepslate_cadmium_ore",
            ModBlocks.DEEPSLATE_CADMIUM_ORE);
    public static final RegistryObject<Item> NICKEL_ORE = blockItem("nickel_ore", ModBlocks.NICKEL_ORE);
    public static final RegistryObject<Item> DEEPSLATE_NICKEL_ORE = blockItem("deepslate_nickel_ore",
            ModBlocks.DEEPSLATE_NICKEL_ORE);
    public static final RegistryObject<Item> SILVER_ORE = blockItem("silver_ore", ModBlocks.SILVER_ORE);
    public static final RegistryObject<Item> DEEPSLATE_SILVER_ORE = blockItem("deepslate_silver_ore",
            ModBlocks.DEEPSLATE_SILVER_ORE);
    public static final RegistryObject<Item> LEAD_ORE = blockItem("lead_ore", ModBlocks.LEAD_ORE);
    public static final RegistryObject<Item> DEEPSLATE_LEAD_ORE = blockItem("deepslate_lead_ore",
            ModBlocks.DEEPSLATE_LEAD_ORE);

    static {
        for (Metal metal : Metal.cachedValues()) {
            METAL_FLAKES.put(metal, ITEMS.register(metal.id() + "_flakes", () -> new Item(new Item.Properties())));
            METAL_POWDERS.put(metal, ITEMS.register(metal.id() + "_powder", () -> new Item(new Item.Properties())));
            if (metal == Metal.LERASIUM) {
                METAL_BEADS.put(metal, ITEMS.register("lerasium_bead",
                        () -> new LerasiumBeadItem(new Item.Properties().stacksTo(16).fireResistant())));
            } else if (metal == Metal.LERASATIUM) {
                METAL_BEADS.put(metal, ITEMS.register("lerasatium_bead",
                        () -> new LerasatiumBeadItem(new Item.Properties().stacksTo(16).fireResistant())));
            } else {
                METAL_BEADS.put(metal, ITEMS.register(metal.id() + "_bead",
                        () -> new Item(new Item.Properties().stacksTo(32).fireResistant())));
            }
            if (!metal.isGodMetal()) {
                LERASIUM_ALLOY_BEADS.put(metal, ITEMS.register("lerasium_" + metal.id() + "_bead",
                        () -> new AlloyBeadItem(metal, false, new Item.Properties().stacksTo(16).fireResistant())));
                LERASATIUM_ALLOY_BEADS.put(metal, ITEMS.register("lerasatium_" + metal.id() + "_bead",
                        () -> new AlloyBeadItem(metal, true, new Item.Properties().stacksTo(16).fireResistant())));
            }
            METAL_INGOTS.put(metal, ITEMS.register(metal.id() + "_ingot", () -> new Item(new Item.Properties())));
            METAL_BLENDS.put(metal, ITEMS.register(metal.id() + "_blend", () -> new Item(new Item.Properties())));
            RAW_ORES.put(metal, ITEMS.register("raw_" + metal.id() + "_ore", () -> new Item(new Item.Properties())));
            if (metal.isAllomantic()) {
                METAL_VIALS.put(metal, ITEMS.register(metal.id() + "_vial",
                        () -> new MetalVialItem(metal, false, new Item.Properties().stacksTo(16))));
            }
            if (metal.isFeruchemical()) {
                METALMIND_RINGS.put(metal, ITEMS.register(metal.id() + "_ring", () -> new MetalmindItem(metal,
                        MetalmindItem.Type.RING, false, new Item.Properties().stacksTo(1))));
                UNKEYED_METALMIND_RINGS.put(metal,
                        ITEMS.register("unkeyed_" + metal.id() + "_ring", () -> new MetalmindItem(metal,
                                MetalmindItem.Type.RING, true, new Item.Properties().stacksTo(1))));

                METALMIND_BRACERS.put(metal, ITEMS.register(metal.id() + "_bracer", () -> new MetalmindItem(metal,
                        MetalmindItem.Type.BRACER, false, new Item.Properties().stacksTo(1))));
                UNKEYED_METALMIND_BRACERS.put(metal,
                        ITEMS.register("unkeyed_" + metal.id() + "_bracer", () -> new MetalmindItem(metal,
                                MetalmindItem.Type.BRACER, true, new Item.Properties().stacksTo(1))));

                METALMIND_NECKLACES.put(metal, ITEMS.register(metal.id() + "_necklace", () -> new MetalmindItem(metal,
                        MetalmindItem.Type.NECKLACE, false, new Item.Properties().stacksTo(1))));
                UNKEYED_METALMIND_NECKLACES.put(metal,
                        ITEMS.register("unkeyed_" + metal.id() + "_necklace", () -> new MetalmindItem(metal,
                                MetalmindItem.Type.NECKLACE, true, new Item.Properties().stacksTo(1))));
            }
            SPIKE_BLANKS.put(metal, ITEMS.register(metal.id() + "_spike",
                    () -> new HemalurgicSpikeItem(metal, false, new Item.Properties().stacksTo(16))));
            CHARGED_SPIKES.put(metal, ITEMS.register("charged_" + metal.id() + "_spike",
                    () -> new HemalurgicSpikeItem(metal, true, new Item.Properties().stacksTo(1).fireResistant())));
        }
        for (MetalbornRole role : MetalbornRole.cachedValues()) {
            METALBORN_SPAWN_EGGS.put(role, spawnEgg(role));
        }
    }

    private ModItems() {
    }

    private static RegistryObject<Item> blockItem(String name,
            RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> spawnEgg(MetalbornRole role) {
        @SuppressWarnings("unchecked")
        RegistryObject<? extends EntityType<? extends Mob>> entityType = (RegistryObject<? extends EntityType<? extends Mob>>) (RegistryObject<?>) ModEntityTypes.METALBORN
                .get(role);
        return ITEMS.register(role.id() + "_spawn_egg", () -> new ForgeSpawnEggItem(entityType, role.eggBaseColor(),
                role.eggHighlightColor(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
