package com.not_noah.mistborn_metal_arts.client.render;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.client.model.InquisitorModel;
import com.not_noah.mistborn_metal_arts.client.model.ModModelLayers;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SteelInquisitorRenderer extends HumanoidMobRenderer<MetalbornEnemy, InquisitorModel<MetalbornEnemy>> {
    public SteelInquisitorRenderer(EntityRendererProvider.Context context) {
        super(context, new InquisitorModel<>(context.bakeLayer(ModModelLayers.STEEL_INQUISITOR)), 0.6F);
    }

    @Override
    protected void scale(MetalbornEnemy entity, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        poseStack.scale(1.35F, 1.35F, 1.35F); // Taller than player
    }

    @Override
    public ResourceLocation getTextureLocation(MetalbornEnemy entity) {
        return new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/entity/steel_inquisitor.png");
    }
}
