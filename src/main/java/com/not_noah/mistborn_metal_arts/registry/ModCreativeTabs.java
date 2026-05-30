package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final ResourceKey<Registry<CreativeModeTab>> CREATIVE_MODE_TAB_REGISTRY = ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "creative_mode_tab"));

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(CREATIVE_MODE_TAB_REGISTRY, MistbornMetalArts.MOD_ID);

    public static final RegistryObject<CreativeModeTab> METAL_ARTS = TABS.register("metal_arts", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mistborn_metal_arts.metal_arts"))
            .icon(() -> new ItemStack(ModItems.MIXED_METAL_VIAL.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.EMPTY_GLASS_VIAL.get());
                output.accept(ModItems.MIXED_METAL_VIAL.get());
                output.accept(ModItems.BLOOD_VIAL.get());
                output.accept(ModItems.ALUMINUM_CASING.get());
                output.accept(ModItems.BLOOD_PRESERVATION_TANK.get());
                output.accept(ModItems.HEMALURGIC_MANUSCRIPT.get());
                output.accept(ModItems.METAL_ARTS_GUIDEBOOK.get());
                output.accept(ModItems.SPIKE_REMOVAL_TOOL.get());
                output.accept(ModItems.SPIKE_PRESS.get());
                output.accept(ModItems.HEMALURGIC_ALTAR.get());
                output.accept(ModItems.ATIUM_GEODE_CLUSTER.get());
                output.accept(ModItems.LERASIUM_CACHE_BLOCK.get());
                output.accept(ModItems.METAL_CACHE.get());
                output.accept(ModItems.WELL_OF_ASCENSION_BLOCK.get());
                output.accept(ModItems.WELL_PULSE_CORE.get());
                output.accept(ModItems.SEALED_WELL_DOOR.get());
                output.accept(ModItems.ANCIENT_METAL_FLOOR.get());
                output.accept(ModItems.ZINC_ORE.get());
                output.accept(ModItems.DEEPSLATE_ZINC_ORE.get());
                output.accept(ModItems.TIN_ORE.get());
                output.accept(ModItems.DEEPSLATE_TIN_ORE.get());
                output.accept(ModItems.ALUMINUM_ORE.get());
                output.accept(ModItems.DEEPSLATE_ALUMINUM_ORE.get());
                output.accept(ModItems.CHROMIUM_ORE.get());
                output.accept(ModItems.DEEPSLATE_CHROMIUM_ORE.get());
                output.accept(ModItems.CADMIUM_ORE.get());
                output.accept(ModItems.DEEPSLATE_CADMIUM_ORE.get());
                output.accept(ModItems.NICKEL_ORE.get());
                output.accept(ModItems.DEEPSLATE_NICKEL_ORE.get());
                output.accept(ModItems.SILVER_ORE.get());
                output.accept(ModItems.DEEPSLATE_SILVER_ORE.get());
                output.accept(ModItems.LEAD_ORE.get());
                output.accept(ModItems.DEEPSLATE_LEAD_ORE.get());
                output.accept(ModItems.ASH_DEPOSIT.get());
                for (MetalbornRole role : MetalbornRole.cachedValues()) {
                    output.accept(ModItems.METALBORN_SPAWN_EGGS.get(role).get());
                }
                for (Metal metal : Metal.cachedValues()) {
                    output.accept(ModItems.METAL_FLAKES.get(metal).get());
                    output.accept(ModItems.METAL_POWDERS.get(metal).get());
                    output.accept(ModItems.METAL_BLENDS.get(metal).get());
                    output.accept(ModItems.METAL_BEADS.get(metal).get());
                    output.accept(ModItems.METAL_INGOTS.get(metal).get());
                    output.accept(ModItems.RAW_ORES.get(metal).get());
                    if (ModItems.METAL_VIALS.containsKey(metal)) {
                        output.accept(ModItems.METAL_VIALS.get(metal).get());
                    }
                    if (ModItems.METALMINDS.containsKey(metal)) {
                        output.accept(ModItems.METALMINDS.get(metal).get());
                        output.accept(ModItems.UNKEYED_METALMINDS.get(metal).get());
                    }
                    output.accept(ModItems.SPIKE_BLANKS.get(metal).get());
                    output.accept(ModItems.CHARGED_SPIKES.get(metal).get());
                }
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
