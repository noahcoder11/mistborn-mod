package com.not_noah.mistborn_metal_arts.client.model;

import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class KolossModel<T extends MetalbornEnemy> extends HumanoidModel<T> {
    public KolossModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Koloss are bigger and have loose skin.
        // We use varied deformations to give a "wrong" look
        CubeDeformation headDeform = new CubeDeformation(0.8F);
        CubeDeformation bodyDeform = new CubeDeformation(1.8F);
        CubeDeformation armDeform = new CubeDeformation(2.2F);
        CubeDeformation legDeform = new CubeDeformation(1.5F);
        
        // Small head compared to body
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, headDeform), 
                PartPose.offset(0.0F, 2.0F, -4.0F)); // Hunched forward
        
        // Very broad body
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16)
                .addBox(-7.0F, 0.0F, -4.0F, 14.0F, 12.0F, 8.0F, bodyDeform), 
                PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Massive arms and fists
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16)
                .addBox(-5.0F, -2.0F, -3.0F, 6.0F, 14.0F, 6.0F, armDeform), 
                PartPose.offset(-7.0F, 2.0F, 0.0F));
        
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 16).mirror()
                .addBox(-1.0F, -2.0F, -3.0F, 6.0F, 14.0F, 6.0F, armDeform), 
                PartPose.offset(7.0F, 2.0F, 0.0F));
        
        // Thick legs
        partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 12.0F, 6.0F, legDeform), 
                PartPose.offset(-4.0F, 12.0F, 0.0F));
        
        partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror()
                .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 12.0F, 6.0F, legDeform), 
                PartPose.offset(4.0F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
