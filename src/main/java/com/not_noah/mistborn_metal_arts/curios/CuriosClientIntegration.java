package com.not_noah.mistborn_metal_arts.curios;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.client.render.SpikeCurioRenderer;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public final class CuriosClientIntegration {
    private CuriosClientIntegration() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        for (Metal metal : Metal.cachedValues()) {
            RegistryObject<net.minecraft.world.item.Item> blank = ModItems.SPIKE_BLANKS.get(metal);
            if (blank != null) {
                CuriosRendererRegistry.register(blank.get(), SpikeCurioRenderer::new);
            }
            RegistryObject<net.minecraft.world.item.Item> charged = ModItems.CHARGED_SPIKES.get(metal);
            if (charged != null) {
                CuriosRendererRegistry.register(charged.get(), SpikeCurioRenderer::new);
            }
        }
    }
}
