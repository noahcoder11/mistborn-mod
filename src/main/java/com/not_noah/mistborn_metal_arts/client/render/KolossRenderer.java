package com.not_noah.mistborn_metal_arts.client.render;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.client.model.KolossModel;
import com.not_noah.mistborn_metal_arts.client.model.ModModelLayers;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class KolossRenderer extends HumanoidMobRenderer<MetalbornEnemy, KolossModel<MetalbornEnemy>> {
    public KolossRenderer(EntityRendererProvider.Context context) {
        super(context, new KolossModel<>(context.bakeLayer(ModModelLayers.KOLOSS)), 1.1F);
    }

    @Override
    protected void scale(MetalbornEnemy entity, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        poseStack.scale(1.85F, 1.85F, 1.85F); // Huge brute
    }

    @Override
    public ResourceLocation getTextureLocation(MetalbornEnemy entity) {
        return new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/entity/koloss.png");
    }
}
