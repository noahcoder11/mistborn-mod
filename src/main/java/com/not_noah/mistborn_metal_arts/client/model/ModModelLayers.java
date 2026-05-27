package com.not_noah.mistborn_metal_arts.client.model;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class ModModelLayers {
    public static final ModelLayerLocation METALBORN = new ModelLayerLocation(new ResourceLocation(MistbornMetalArts.MOD_ID, "metalborn"), "main");
    public static final ModelLayerLocation STEEL_INQUISITOR = new ModelLayerLocation(new ResourceLocation(MistbornMetalArts.MOD_ID, "steel_inquisitor"), "main");
    public static final ModelLayerLocation KOLOSS = new ModelLayerLocation(new ResourceLocation(MistbornMetalArts.MOD_ID, "koloss"), "main");
    public static final ModelLayerLocation KANDRA = new ModelLayerLocation(new ResourceLocation(MistbornMetalArts.MOD_ID, "kandra"), "main");

    private ModModelLayers() {
    }
}
