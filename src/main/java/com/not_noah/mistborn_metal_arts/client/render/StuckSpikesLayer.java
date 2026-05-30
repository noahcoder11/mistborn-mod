package com.not_noah.mistborn_metal_arts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.StuckSpike;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

public class StuckSpikesLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(com.not_noah.mistborn_metal_arts.MistbornMetalArts.MOD_ID, "textures/entity/white.png");

    public StuckSpikesLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        entity.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            java.util.List<StuckSpike> spikes = data.getStuckSpikes();
            if (spikes == null || spikes.isEmpty()) {
                return;
            }

            for (StuckSpike spike : spikes) {
                poseStack.pushPose();

                try {
                    ModelPart part = getBestModelPart(this.getParentModel(), spike.getOx(), spike.getOy(), spike.getOz(), entity.getBbHeight());
                    if (part != null && !part.isEmpty()) {
                        ModelPart rootPart = null;
                        if (this.getParentModel() instanceof net.minecraft.client.model.HierarchicalModel<?> hm) {
                            rootPart = hm.root();
                        }
                        if (rootPart != null) {
                            rootPart.translateAndRotate(poseStack);
                        }
                        part.translateAndRotate(poseStack);

                        double scale = getEntityVisualScale(entity);

                        org.joml.Vector3f localPos;
                        float cx = 0.0F;
                        float cy = 0.0F;
                        float cz = 0.0F;
                        float hx = 0.125F;
                        float hy = 0.125F;
                        float hz = 0.125F;

                        double hPct = spike.getOy() / entity.getBbHeight();

                        if (this.getParentModel() instanceof net.minecraft.client.model.HumanoidModel<?>) {
                            float px = part.x / 16.0F;
                            float py = part.y / 16.0F;
                            float pz = part.z / 16.0F;

                            float modelX = (float) (spike.getOx() / scale);
                            float modelY = (float) (1.5D - spike.getOy() / scale);
                            float modelZ = (float) (spike.getOz() / scale);

                            float dx = modelX - px;
                            float dy = modelY - py;
                            float dz = modelZ - pz;

                            localPos = new org.joml.Vector3f(dx, dy, dz);

                            if (hPct >= 0.72) {
                                // Head
                                cx = 0.0F;
                                cy = -0.25F;
                                cz = 0.0F;
                                hx = 0.25F;
                                hy = 0.25F;
                                hz = 0.25F;
                            } else if (hPct < 0.35) {
                                // Legs
                                cx = 0.0F;
                                cy = 0.375F;
                                cz = 0.0F;
                                hx = 0.125F;
                                hy = 0.375F;
                                hz = 0.125F;
                            } else {
                                if (spike.getOx() > 0.22 || spike.getOx() < -0.22) {
                                    // Arms
                                    cx = 0.0F;
                                    cy = 0.375F;
                                    cz = 0.0F;
                                    hx = 0.125F;
                                    hy = 0.375F;
                                    hz = 0.125F;
                                } else {
                                    // Body
                                    cx = 0.0F;
                                    cy = 0.375F;
                                    cz = 0.0F;
                                    hx = 0.25F;
                                    hy = 0.375F;
                                    hz = 0.125F;
                                }
                            }
                        } else {
                            // Non-humanoids (e.g. quadrupeds like Cows): center on part pivot
                            localPos = new org.joml.Vector3f(0.0F, 0.0F, 0.0F);
                        }

                        org.joml.Quaternionf partQ = new org.joml.Quaternionf();
                        partQ.rotateX(-part.xRot);
                        partQ.rotateY(-part.yRot);
                        partQ.rotateZ(-part.zRot);
                        partQ.transform(localPos);

                        poseStack.translate(localPos.x(), localPos.y(), localPos.z());

                        // DYNAMIC PERPENDICULAR ROTATION:
                        // Find dominant axis of the offset vector from the box's center
                        float ox_off = localPos.x() - cx;
                        float oy_off = localPos.y() - cy;
                        float oz_off = localPos.z() - cz;

                        float nx = ox_off / hx;
                        float ny = oy_off / hy;
                        float nz = oz_off / hz;

                        float absX = Math.abs(nx);
                        float absY = Math.abs(ny);
                        float absZ = Math.abs(nz);

                        if (absX >= absY && absX >= absZ) {
                            if (nx > 0.0F) {
                                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                            } else {
                                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                            }
                        } else if (absY >= absX && absY >= absZ) {
                            if (ny > 0.0F) {
                                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                            } else {
                                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                            }
                        } else {
                            if (nz > 0.0F) {
                                // Back Face - no rotation needed
                            } else {
                                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                            }
                        }

                        // Define dimensions based on spike location
                        float xSize, ySize, zSize, zOffset;
                        boolean isEyeSpike = hPct >= 0.72 && spike.getOz() <= 0.0;
                        boolean isBackSpike = spike.getOz() > 0.1;

                        if (isEyeSpike) {
                            // Eye spike: 2x2 pixels, pokes out 1 pixel
                            xSize = 2.0F / 16.0F;
                            ySize = 2.0F / 16.0F;
                            zSize = 4.0F / 16.0F;
                            zOffset = 1.0F / 16.0F;
                        } else if (isBackSpike) {
                            // Back spike: 1x1 pixel, sticks out 2 pixels
                            xSize = 1.0F / 16.0F;
                            ySize = 1.0F / 16.0F;
                            zSize = 4.0F / 16.0F;
                            zOffset = 2.0F / 16.0F;
                        } else {
                            // General body spike: 1x1 pixel, sticks out 1 pixel
                            xSize = 1.0F / 16.0F;
                            ySize = 1.0F / 16.0F;
                            zSize = 4.0F / 16.0F;
                            zOffset = 1.0F / 16.0F;
                        }

                        int[] color = getMetalColor(spike.getMetal());
                        renderBlockySpike(poseStack, bufferSource, packedLight, net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(entity, 0.0F), xSize, ySize, zSize, zOffset, color[0], color[1], color[2]);
                    }
                } catch (Exception e) {
                    // Safe fallback
                }

                poseStack.popPose();
            }
        });
    }

    private int[] getMetalColor(com.not_noah.mistborn_metal_arts.api.Metal metal) {
        switch (metal) {
            case STEEL: return new int[]{180, 190, 200};      // steel gray
            case IRON: return new int[]{120, 120, 120};       // dark iron gray
            case PEWTER: return new int[]{150, 160, 165};     // dull pewter gray
            case TIN: return new int[]{220, 220, 225};        // tin silver
            case ZINC: return new int[]{160, 180, 190};       // bluish zinc
            case BRASS: return new int[]{215, 175, 55};       // brass gold
            case COPPER: return new int[]{200, 110, 75};      // copper red-brown
            case BRONZE: return new int[]{165, 125, 80};      // bronze brown
            case GOLD: return new int[]{255, 215, 0};         // gold yellow
            case ELECTRUM: return new int[]{240, 235, 175};   // pale gold
            case CADMIUM: return new int[]{135, 175, 165};    // green-gray cadmium
            case BENDALLOY: return new int[]{170, 200, 190};  // pale bendalloy
            case CHROMIUM: return new int[]{195, 200, 190};   // silver chrome
            case NICROSIL: return new int[]{215, 205, 180};   // soft nicrosil
            case ALUMINUM: return new int[]{210, 210, 215};   // light aluminum
            case DURALUMIN: return new int[]{185, 185, 190};  // dull duralumin
            case ATIUM: return new int[]{55, 55, 55};         // dark atium
            case LERASIUM: return new int[]{175, 235, 175};   // green lerasium
            default: return new int[]{200, 200, 200};
        }
    }

    private void renderBlockySpike(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float xSize, float ySize, float zSize, float zOffset, int r, int g, int b) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEXTURE));
        
        float x1 = -xSize / 2.0F;
        float x2 = xSize / 2.0F;
        float y1 = -ySize / 2.0F;
        float y2 = ySize / 2.0F;
        
        // Z-axis points outward. Go deep inside (negative Z) and stick out (up to zOffset).
        float z1 = -zSize + zOffset;
        float z2 = zOffset;

        PoseStack.Pose entry = poseStack.last();
        org.joml.Matrix4f m = entry.pose();
        org.joml.Matrix3f n = entry.normal();

        // Front Face (Z2)
        addVertex(m, n, consumer, x1, y1, z2, r, g, b, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y1, z2, r, g, b, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y2, z2, r, g, b, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x1, y2, z2, r, g, b, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, packedLight, packedOverlay);

        // Back Face (Z1)
        addVertex(m, n, consumer, x1, y1, z1, r, g, b, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x1, y2, z1, r, g, b, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y2, z1, r, g, b, 1.0F, 1.0F, 0.0F, 0.0F, -1.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y1, z1, r, g, b, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, packedLight, packedOverlay);

        // Left Face (X1)
        addVertex(m, n, consumer, x1, y1, z1, r, g, b, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x1, y1, z2, r, g, b, 1.0F, 0.0F, -1.0F, 0.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x1, y2, z2, r, g, b, 1.0F, 1.0F, -1.0F, 0.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x1, y2, z1, r, g, b, 0.0F, 1.0F, -1.0F, 0.0F, 0.0F, packedLight, packedOverlay);

        // Right Face (X2)
        addVertex(m, n, consumer, x2, y1, z1, r, g, b, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y2, z1, r, g, b, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y2, z2, r, g, b, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y1, z2, r, g, b, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, packedLight, packedOverlay);

        // Top Face (Y2)
        addVertex(m, n, consumer, x1, y2, z1, r, g, b, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x1, y2, z2, r, g, b, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y2, z2, r, g, b, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y2, z1, r, g, b, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, packedLight, packedOverlay);

        // Bottom Face (Y1)
        addVertex(m, n, consumer, x1, y1, z1, r, g, b, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y1, z1, r, g, b, 1.0F, 0.0F, 0.0F, -1.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x2, y1, z2, r, g, b, 1.0F, 1.0F, 0.0F, -1.0F, 0.0F, packedLight, packedOverlay);
        addVertex(m, n, consumer, x1, y1, z2, r, g, b, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F, packedLight, packedOverlay);
    }

    private void addVertex(org.joml.Matrix4f matrix, org.joml.Matrix3f normalMatrix, VertexConsumer consumer, float x, float y, float z, int r, int g, int b, float u, float v, float nx, float ny, float nz, int light, int overlay) {
        consumer.vertex(matrix, x, y, z)
                .color(r, g, b, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }

    private static double getEntityVisualScale(LivingEntity entity) {
        if (entity instanceof com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy enemy) {
            if (enemy.role() == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.STEEL_INQUISITOR) {
                return 1.35D;
            } else if (enemy.role() == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.KOLOSS) {
                return 1.85D;
            }
        }
        return 1.0D;
    }

    @SuppressWarnings("unchecked")
    private ModelPart getBestModelPart(M model, double ox, double oy, double oz, double entityHeight) {
        if (model instanceof net.minecraft.client.model.HumanoidModel<?> humanoidModel) {
            double hPct = oy / entityHeight;
            if (hPct >= 0.72) {
                return humanoidModel.head;
            } else if (hPct < 0.35) {
                return ox > 0 ? humanoidModel.leftLeg : humanoidModel.rightLeg;
            } else {
                if (ox > 0.22) return humanoidModel.leftArm;
                if (ox < -0.22) return humanoidModel.rightArm;
                return humanoidModel.body;
            }
        } else if (model instanceof net.minecraft.client.model.VillagerModel<?> villagerModel) {
            double hPct = oy / entityHeight;
            ModelPart root = villagerModel.root();
            if (hPct >= 0.72) {
                try { return root.getChild("head"); } catch (Exception e) { return villagerModel.getHead(); }
            } else {
                try { return root.getChild("body"); } catch (Exception e) { return villagerModel.getHead(); }
            }
        }
        
        RandomSource random = RandomSource.create(42L);
        return getRandomModelPart(model, random);
    }

    @SuppressWarnings("unchecked")
    private ModelPart getRandomModelPart(M model, RandomSource random) {
        java.util.List<ModelPart> parts = new java.util.ArrayList<>();
        
        if (model instanceof net.minecraft.client.model.HierarchicalModel<?> hierarchicalModel) {
            ModelPart root = hierarchicalModel.root();
            if (root != null) {
                java.util.Map<String, ModelPart> childrenMap = null;
                try {
                    java.lang.reflect.Field field = ModelPart.class.getDeclaredField("children");
                    field.setAccessible(true);
                    childrenMap = (java.util.Map<String, ModelPart>) field.get(root);
                } catch (Exception e) {
                    try {
                        java.lang.reflect.Field field = ModelPart.class.getDeclaredField("f_104213_");
                        field.setAccessible(true);
                        childrenMap = (java.util.Map<String, ModelPart>) field.get(root);
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                if (childrenMap != null && !childrenMap.isEmpty()) {
                    for (ModelPart child : childrenMap.values()) {
                        if (child != null && !child.isEmpty()) {
                            parts.add(child);
                        }
                    }
                } else if (!root.isEmpty()) {
                    parts.add(root);
                }
            }
        } else if (model instanceof net.minecraft.client.model.AgeableListModel<?> ageableListModel) {
            java.lang.Iterable<ModelPart> headParts = null;
            java.lang.Iterable<ModelPart> bodyParts = null;
            try {
                java.lang.reflect.Method method = net.minecraft.client.model.AgeableListModel.class.getDeclaredMethod("headParts");
                method.setAccessible(true);
                headParts = (java.lang.Iterable<ModelPart>) method.invoke(ageableListModel);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Method method = net.minecraft.client.model.AgeableListModel.class.getDeclaredMethod("m_5607_");
                    method.setAccessible(true);
                    headParts = (java.lang.Iterable<ModelPart>) method.invoke(ageableListModel);
                } catch (Exception ex) {
                    // ignore
                }
            }
            try {
                java.lang.reflect.Method method = net.minecraft.client.model.AgeableListModel.class.getDeclaredMethod("bodyParts");
                method.setAccessible(true);
                bodyParts = (java.lang.Iterable<ModelPart>) method.invoke(ageableListModel);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Method method = net.minecraft.client.model.AgeableListModel.class.getDeclaredMethod("m_5608_");
                    method.setAccessible(true);
                    bodyParts = (java.lang.Iterable<ModelPart>) method.invoke(ageableListModel);
                } catch (Exception ex) {
                    // ignore
                }
            }

            if (headParts != null) {
                for (ModelPart part : headParts) {
                    if (part != null && !part.isEmpty()) {
                        parts.add(part);
                    }
                }
            }
            if (bodyParts != null) {
                for (ModelPart part : bodyParts) {
                    if (part != null && !part.isEmpty()) {
                        parts.add(part);
                    }
                }
            }
        }
        if (!parts.isEmpty()) {
            return parts.get(random.nextInt(parts.size()));
        }
        return null;
    }
}
