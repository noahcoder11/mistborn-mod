package com.not_noah.mistborn_metal_arts.client.model;

import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class KandraModel<T extends MetalbornEnemy> extends HumanoidModel<T> {
    private final ModelPart boneRidges;

    public KandraModel(ModelPart root) {
        super(root);
        this.boneRidges = root.getChild("body").getChild("bone_ridges");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Slightly hunched posture - head offset forward
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(0.0F, 0.5F, -1.0F));

        PartDefinition body = partdefinition.getChild("body");
        
        // Elongated limbs
        partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 14.0F, 3.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 3.0F, 14.0F, 3.0F), PartPose.offset(5.0F, 2.0F, 0.0F));

        // Bone-like ridges for revealed form
        body.addOrReplaceChild("bone_ridges", CubeListBuilder.create()
                .texOffs(0, 48)
                .addBox(-1.0F, 1.0F, 2.0F, 2.0F, 10.0F, 2.0F) // Spine ridge
                .addBox(-4.0F, 2.0F, 2.0F, 8.0F, 1.0F, 1.0F) // Rib ridge 1
                .addBox(-4.0F, 5.0F, 2.0F, 8.0F, 1.0F, 1.0F) // Rib ridge 2
                .addBox(-4.0F, 8.0F, 2.0F, 8.0F, 1.0F, 1.0F), // Rib ridge 3
                PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        
        // Hide/show ridges based on entity state (revealed form)
        // We'll assume revealed if health is low or it's attacking, for now
        this.boneRidges.visible = entity.getHealth() < entity.getMaxHealth() * 0.5F || entity.isAggressive();
        
        if (this.boneRidges.visible) {
            this.head.xRot += 0.2F; // More hunched when revealed
        }
    }
}
