package com.not_noah.mistborn_metal_arts.client.render;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.client.model.MistbornModel;
import com.not_noah.mistborn_metal_arts.client.model.ModModelLayers;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MetalbornEnemyRenderer extends HumanoidMobRenderer<MetalbornEnemy, MistbornModel<MetalbornEnemy>> {
    private final MetalbornRole role;

    public MetalbornEnemyRenderer(EntityRendererProvider.Context context, MetalbornRole role) {
        super(context, new MistbornModel<>(context.bakeLayer(ModModelLayers.METALBORN)), 0.5F);
        this.role = role;
    }

    @Override
    public ResourceLocation getTextureLocation(MetalbornEnemy entity) {
        return new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/entity/" + role.id() + ".png");
    }
}
