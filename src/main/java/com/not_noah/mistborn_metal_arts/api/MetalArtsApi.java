package com.not_noah.mistborn_metal_arts.api;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraft.resources.ResourceLocation;

public final class MetalArtsApi {
    public static final ResourceLocation METALLIC_BLOCKS = id("metallic_blocks");
    public static final ResourceLocation METALLIC_ITEMS = id("metallic_items");
    public static final ResourceLocation PUSHABLE_ENTITIES = id("pushable_entities");
    public static final ResourceLocation PULLABLE_ENTITIES = id("pullable_entities");
    public static final ResourceLocation METALMINDS = id("metalminds");
    public static final ResourceLocation HEMALURGIC_SPIKES = id("hemalurgic_spikes");
    public static final ResourceLocation GOD_METALS = id("god_metals");

    private MetalArtsApi() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MistbornMetalArts.MOD_ID, path);
    }
}
