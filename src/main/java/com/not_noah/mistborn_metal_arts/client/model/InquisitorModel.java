package com.not_noah.mistborn_metal_arts.client.model;

import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/**
 * UV Guide for Steel Inquisitor:
 * 
 * - Head: (0, 0) region, Face at (8, 8) [YELLOW/ORANGE]
 * - Body: (16, 16) region, Chest at (20, 20) [RED/PINK]
 * - Right Arm: (40, 16) [BLUE]
 * - Left Arm: (32, 48) [CYAN]
 * - Right Leg: (0, 16) [GREEN]
 * - Left Leg: (16, 48) [LIME]
 * - Spikes: (40, 0) [GREY/SILVER]
 * - Cape: (0, 34) [overlay region — dark robe texture]
 */
public class InquisitorModel<T extends MetalbornEnemy> extends HumanoidModel<T> {
        public InquisitorModel(ModelPart root) {
                super(root);
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                // === Explicitly define all humanoid parts with standard skin UVs ===

                // Head — texOffs(0, 0), standard 8×8×8 cube
                PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                                PartPose.ZERO);

                // Hat & standard overlays — required by HumanoidModel constructor, empty to hide
                partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
                partdefinition.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.ZERO);
                partdefinition.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
                partdefinition.addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.ZERO);
                partdefinition.addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.ZERO);
                partdefinition.addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.ZERO);

                // Body — texOffs(16, 16)
                PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
                                .texOffs(16, 16)
                                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                                PartPose.ZERO);

                // Right Arm — texOffs(40, 16)
                partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                                .texOffs(40, 16)
                                .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                                PartPose.offset(-5.0F, 2.0F, 0.0F));

                // Left Arm — texOffs(32, 48)
                partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                                .texOffs(32, 48)
                                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                                PartPose.offset(5.0F, 2.0F, 0.0F));

                // Right Leg — texOffs(0, 16)
                partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
                                .texOffs(0, 16)
                                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                                PartPose.offset(-1.9F, 12.0F, 0.0F));

                // Left Leg — texOffs(16, 48)
                partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                                .texOffs(16, 48)
                                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                                PartPose.offset(1.9F, 12.0F, 0.0F));

                // === Custom children ===

                // Eye Spikes (Front) - 2x2 protruding 1 pixel from face [GREY/SILVER]
                head.addOrReplaceChild("left_eye_spike_front", CubeListBuilder.create()
                                .texOffs(40, 0)
                                .addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 1.0F),
                                PartPose.offset(-2.0F, -4.0F, 0.0F));

                head.addOrReplaceChild("right_eye_spike_front", CubeListBuilder.create()
                                .texOffs(40, 0)
                                .addBox(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 1.0F),
                                PartPose.offset(2.0F, -4.0F, 0.0F));

                // Eye Spikes (Back) - 1x1 protruding 2 pixels from back of head
                head.addOrReplaceChild("left_eye_spike_back", CubeListBuilder.create()
                                .texOffs(40, 0)
                                .addBox(-0.5F, -0.5F, 4.0F, 1.0F, 1.0F, 2.0F),
                                PartPose.offset(-2.0F, -4.0F, 0.0F));

                head.addOrReplaceChild("right_eye_spike_back", CubeListBuilder.create()
                                .texOffs(40, 0)
                                .addBox(-0.5F, -0.5F, 4.0F, 1.0F, 1.0F, 2.0F),
                                PartPose.offset(2.0F, -4.0F, 0.0F));

                // Body Spikes - Small metal studs [GREY/SILVER]
                body.addOrReplaceChild("shoulder_spike_l", CubeListBuilder.create()
                                .texOffs(40, 0)
                                .addBox(0.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                                PartPose.offset(4.0F, 1.0F, 0.0F));

                body.addOrReplaceChild("shoulder_spike_r", CubeListBuilder.create()
                                .texOffs(40, 0)
                                .addBox(-2.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                                PartPose.offset(-4.0F, 1.0F, 0.0F));

                body.addOrReplaceChild("back_spike_upper", CubeListBuilder.create()
                                .texOffs(40, 0)
                                .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F),
                                PartPose.offset(0.0F, 2.0F, 2.0F));

                body.addOrReplaceChild("back_spike_lower", CubeListBuilder.create()
                                .texOffs(40, 0)
                                .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                                PartPose.offset(0.0F, 7.0F, 2.0F));

                // Tattered Cape — (0, 34) dark robe overlay region. 10×16×1 cube.
                body.addOrReplaceChild("cape", CubeListBuilder.create()
                                .texOffs(0, 34)
                                .addBox(-5.0F, 0.0F, 2.1F, 10.0F, 16.0F, 1.0F),
                                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.15F, 0.0F, 0.0F));

                return LayerDefinition.create(meshdefinition, 64, 64);
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                        float headPitch) {
                super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }
}
