package com.not_noah.mistborn_metal_arts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.BloodSlash;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class BloodOverlayLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation BLOOD_TEXTURE = new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/entity/blood_overlay.png");

    private static final ResourceLocation[] SLASH_TEXTURES = new ResourceLocation[] {
            new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/particle/blood_slash_1.png"),
            new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/particle/blood_slash_2.png"),
            new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/particle/blood_slash_3.png")
    };

    public BloodOverlayLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        
        entity.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            float wetBlood = data.getBloodLevel();
            float healthPct = entity.getHealth() / entity.getMaxHealth();
            float missingHealth = 1.0F - healthPct;
            float displayBlood = Math.max(wetBlood, missingHealth);
            java.util.List<BloodSlash> slashes = data.getSlashes();
            boolean hasSlashes = slashes != null && !slashes.isEmpty();

            if (displayBlood > 0.01F || hasSlashes) {
                ResourceLocation baseTexture = this.getTextureLocation(entity);
                ResourceLocation dynamicOverlay = BloodTextureManager.getOrCreateBloodTexture(entity, baseTexture, displayBlood, slashes);

                if (dynamicOverlay != null) {
                    RenderType renderType = RenderType.entityTranslucentCull(dynamicOverlay);
                    VertexConsumer buffer = bufferSource.getBuffer(renderType);

                    boolean oldHatVisible = true;
                    boolean oldRimVisible = true;
                    net.minecraft.client.model.geom.ModelPart hatPart = null;
                    net.minecraft.client.model.geom.ModelPart rimPart = null;

                    if (this.getParentModel() instanceof net.minecraft.client.model.VillagerModel<?> villagerModel) {
                        try {
                            hatPart = villagerModel.getHead().getChild("hat");
                            rimPart = hatPart.getChild("hat_rim");
                            oldHatVisible = hatPart.visible;
                            oldRimVisible = rimPart.visible;
                            hatPart.visible = false;
                            rimPart.visible = false;
                        } catch (Exception e) {
                            // Ignore
                        }
                    }

                    // Render the model with the pre-baked blood overlay texture.
                    // All alpha and overlays are blended directly into the pixels of the dynamic overlay texture.
                    this.getParentModel().renderToBuffer(
                            poseStack,
                            buffer,
                            packedLight,
                            LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                            1.0F, 1.0F, 1.0F, 1.0F
                    );

                    // Restore hat visibility
                    if (hatPart != null) {
                        hatPart.visible = oldHatVisible;
                        rimPart.visible = oldRimVisible;
                    }
                }
            }
        });
    }
}
