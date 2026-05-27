package com.not_noah.mistborn_metal_arts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class SpikeCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entity = slotContext.entity();
        M model = renderLayerParent.getModel();

        if (!(model instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        poseStack.pushPose();

        // Attach to specific parts based on slot type
        String identifier = slotContext.identifier();
        int index = slotContext.index();

        switch (identifier) {
            case "hemalurgic_eye" -> {
                humanoidModel.head.translateAndRotate(poseStack);
                if (index == 0) { // Right Eye
                    poseStack.translate(0.125D, -0.25D, -0.42D);
                    poseStack.mulPose(Axis.YP.rotationDegrees(12.0F));
                } else { // Left Eye
                    poseStack.translate(-0.125D, -0.25D, -0.42D);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-12.0F));
                }
            }
            case "hemalurgic_heart" -> {
                humanoidModel.body.translateAndRotate(poseStack);
                poseStack.translate(0.14D, 0.32D, -0.25D);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(5.0F));
            }
            case "hemalurgic_shoulder" -> {
                if (index == 0) { // Right Shoulder
                    humanoidModel.rightArm.translateAndRotate(poseStack);
                    poseStack.translate(-0.05D, 0.05D, 0.0D);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
                } else { // Left Shoulder
                    humanoidModel.leftArm.translateAndRotate(poseStack);
                    poseStack.translate(0.05D, 0.05D, 0.0D);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
                }
            }
            case "hemalurgic_spine" -> {
                humanoidModel.body.translateAndRotate(poseStack);
                // Spread out along the spine
                double yOffset = 0.1D + (index * 0.25D);
                poseStack.translate(0.0D, yOffset, 0.25D);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
            }
            default -> {
                humanoidModel.body.translateAndRotate(poseStack);
                poseStack.translate(0.0D, 0.5D, 0.22D);
            }
        }

        // Stick the spike in!
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, 0, poseStack, multiBufferSource, entity.level(), entity.getId());

        poseStack.popPose();
    }
}
