package com.not_noah.mistborn_metal_arts.datagen;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MetalArtsDataGenerators {
    private MetalArtsDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // Static JSON assets are included for the first pass. Provider-backed generation can be added here without changing gameplay code.
    }
}
