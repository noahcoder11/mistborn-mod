package com.not_noah.mistborn_metal_arts.client.model;

import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class MistbornModel<T extends MetalbornEnemy> extends HumanoidModel<T> {
    private final ModelPart cloak;
    private final ModelPart hood;

    public MistbornModel(ModelPart root) {
        super(root);
        this.cloak = root.getChild("cloak");
        this.hood = root.getChild("head").getChild("hood");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Mistcloak strips
        PartDefinition cloak = partdefinition.addOrReplaceChild("cloak", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 2.1F));

        // Create tassels for the mistcloak - more dense for better visual
        for (int i = 0; i < 12; i++) {
            float angle = (float) (i * Math.PI / 6.0);
            float x = Mth.cos(angle) * 3.8F;
            float z = Mth.sin(angle) * 1.5F; // Flattened circle to wrap back better
            if (z < -0.5F) continue; // Only on back and sides
            
            cloak.addOrReplaceChild("tassel_" + i, CubeListBuilder.create()
                    .texOffs(32, 0)
                    .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 22.0F, 0.5F), 
                    PartPose.offset(x, 0.0F, z));
        }
        
        // Add a hood to the head
        PartDefinition head = partdefinition.getChild("head");
        head.addOrReplaceChild("hood", CubeListBuilder.create()
                .texOffs(0, 32)
                .addBox(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.5F)),
                PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        
        boolean isMistborn = entity.role() == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.MISTBORN_ASSASSIN;
        this.cloak.visible = isMistborn;
        this.hood.visible = isMistborn || entity.role().id().contains("scout") || entity.role().id().contains("bandit");

        if (this.cloak.visible) {
            float wave = Mth.sin(ageInTicks * 0.1F) * 0.05F;
            float speedWave = limbSwingAmount * 0.5F;
            
            // Access tassels safely
            for (int i = 0; i < 12; i++) {
                try {
                    ModelPart tassel = cloak.getChild("tassel_" + i);
                    tassel.xRot = 0.15F + wave + speedWave;
                    tassel.zRot = (i % 2 == 0 ? 0.08F : -0.08F) * (1.0F + speedWave);
                } catch (Exception ignored) {}
            }
        }
    }
}
