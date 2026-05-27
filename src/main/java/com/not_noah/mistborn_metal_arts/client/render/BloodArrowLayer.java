package com.not_noah.mistborn_metal_arts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;

public class BloodArrowLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public BloodArrowLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        
        entity.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            java.util.List<com.not_noah.mistborn_metal_arts.capability.BloodSlash> slashes = data.getSlashes();
            if (slashes == null || slashes.isEmpty()) {
                return;
            }

            Arrow dummyArrow = new Arrow(entity.level(), entity.getX(), entity.getY(), entity.getZ());

            for (com.not_noah.mistborn_metal_arts.capability.BloodSlash slash : slashes) {
                if (!slash.isArrow()) {
                    continue;
                }

                poseStack.pushPose();

                try {
                    ModelPart part = getBestModelPart(this.getParentModel(), slash.getOx(), slash.getOy(), slash.getOz(), entity.getBbHeight());
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

                        if (this.getParentModel() instanceof net.minecraft.client.model.HumanoidModel<?> || this.getParentModel() instanceof net.minecraft.client.model.VillagerModel<?>) {
                            // Pivot in meters
                            float px = part.x / 16.0F;
                            float py = part.y / 16.0F;
                            float pz = part.z / 16.0F;

                            // Target-space coordinates scaled to the unscaled model space
                            float modelX = (float) (slash.getOx() / scale);
                            float modelY = (float) (1.5D - slash.getOy() / scale);
                            float modelZ = (float) (-slash.getOz() / scale);

                            System.out.printf("[BloodArrowLayer DEBUG] Entity: %s, ox: %.4f, oy: %.4f, oz: %.4f, scale: %.4f, px: %.4f, py: %.4f, pz: %.4f, modelX: %.4f, modelY: %.4f, modelZ: %.4f%n",
                                    entity.getType().toString(), slash.getOx(), slash.getOy(), slash.getOz(), scale, px, py, pz, modelX, modelY, modelZ);

                            // Transform to root space if hierarchical
                            float dx, dy, dz;
                            org.joml.Quaternionf rootQ = new org.joml.Quaternionf();
                            org.joml.Vector3f localProj = new org.joml.Vector3f(slash.getProjX(), slash.getProjY(), slash.getProjZ());

                            if (rootPart != null) {
                                float rx = modelX - rootPart.x / 16.0F;
                                float ry = modelY - rootPart.y / 16.0F;
                                float rz = modelZ - rootPart.z / 16.0F;

                                org.joml.Vector3f rootSpacePos = new org.joml.Vector3f(rx, ry, rz);
                                rootQ.rotateX(-rootPart.xRot);
                                rootQ.rotateY(-rootPart.yRot);
                                rootQ.rotateZ(-rootPart.zRot);
                                rootQ.transform(rootSpacePos);

                                dx = rootSpacePos.x() - px;
                                dy = rootSpacePos.y() - py;
                                dz = rootSpacePos.z() - pz;

                                rootQ.transform(localProj);
                            } else {
                                dx = modelX - px;
                                dy = modelY - py;
                                dz = modelZ - pz;
                            }

                            // Rotate the offset by the inverse of the part's rotation to get part-local coordinates.
                            org.joml.Vector3f localPos = new org.joml.Vector3f(dx, dy, dz);
                            org.joml.Quaternionf partQ = new org.joml.Quaternionf();
                            partQ.rotateX(-part.xRot);
                            partQ.rotateY(-part.yRot);
                            partQ.rotateZ(-part.zRot);
                            partQ.transform(localPos);

                            poseStack.translate(localPos.x(), localPos.y(), localPos.z());

                            partQ.transform(localProj);

                            float ldx = localProj.x();
                            float ldy = localProj.y();
                            float ldz = localProj.z();
                            float horizontalDist = Mth.sqrt(ldx * ldx + ldz * ldz);

                            if (horizontalDist > 0.001F || Math.abs(ldy) > 0.001F) {
                                dummyArrow.setYRot((float) (Math.atan2((double) ldx, (double) ldz) * (180.0D / Math.PI)));
                                dummyArrow.setXRot((float) (Math.atan2((double) ldy, (double) horizontalDist) * (180.0D / Math.PI)));
                                dummyArrow.yRotO = dummyArrow.getYRot();
                                dummyArrow.xRotO = dummyArrow.getXRot();
                            }
                        } else {
                            // Fallback for non-humanoid models (e.g. quadrupeds, custom models)
                            float[] localPos = getLocalPosForPart(this.getParentModel(), part, slash.getOx(), slash.getOy(), slash.getOz(), entity.getBbWidth(), entity.getBbHeight());
                            
                            org.joml.Quaternionf partQ = new org.joml.Quaternionf();
                            partQ.rotateX(-part.xRot);
                            partQ.rotateY(-part.yRot);
                            partQ.rotateZ(-part.zRot);

                            org.joml.Vector3f localPosVec = new org.joml.Vector3f(localPos[0], localPos[1], localPos[2]);
                            partQ.transform(localPosVec);
                            poseStack.translate(localPosVec.x(), localPosVec.y(), localPosVec.z());

                            org.joml.Vector3f localProj = new org.joml.Vector3f(slash.getProjX(), slash.getProjY(), slash.getProjZ());
                            if (rootPart != null) {
                                org.joml.Quaternionf rootQ = new org.joml.Quaternionf();
                                rootQ.rotateX(-rootPart.xRot);
                                rootQ.rotateY(-rootPart.yRot);
                                rootQ.rotateZ(-rootPart.zRot);
                                rootQ.transform(localProj);
                            }
                            partQ.transform(localProj);

                            float ldx = localProj.x();
                            float ldy = localProj.y();
                            float ldz = localProj.z();
                            float horizontalDist = Mth.sqrt(ldx * ldx + ldz * ldz);

                            if (horizontalDist > 0.001F || Math.abs(ldy) > 0.001F) {
                                dummyArrow.setYRot((float) (Math.atan2((double) ldx, (double) ldz) * (180.0D / Math.PI)));
                                dummyArrow.setXRot((float) (Math.atan2((double) ldy, (double) horizontalDist) * (180.0D / Math.PI)));
                                dummyArrow.yRotO = dummyArrow.getYRot();
                                dummyArrow.xRotO = dummyArrow.getXRot();
                            }
                        }

                        Minecraft.getInstance().getEntityRenderDispatcher().render(
                                dummyArrow,
                                0.0D, 0.0D, 0.0D,
                                0.0F, partialTicks,
                                poseStack,
                                bufferSource,
                                packedLight
                        );
                    }
                } catch (Exception e) {
                    // Safely handle custom model structures
                }

                poseStack.popPose();
            }
        });
    }

    private static double getEntityVisualScale(LivingEntity entity) {
        if (entity instanceof com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy enemy) {
            if (enemy.role() == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.STEEL_INQUISITOR) {
                return 1.35D;
            } else if (enemy.role() == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.KOLOSS) {
                return 1.85D;
            }
        } else if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
            double baseScale = 0.9375D;
            return villager.isBaby() ? baseScale * 0.5D : baseScale;
        } else if (entity instanceof net.minecraft.world.entity.npc.WanderingTrader) {
            return 0.9375D;
        }
        return 1.0D;
    }

    private ModelPart getSafeChild(ModelPart parent, String name) {
        try {
            return parent.getChild(name);
        } catch (Exception e) {
            return null;
        }
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
                ModelPart head = getSafeChild(root, "head");
                return head != null ? head : villagerModel.getHead();
            } else if (hPct < 0.35) {
                ModelPart leg = ox > 0 ? getSafeChild(root, "left_leg") : getSafeChild(root, "right_leg");
                return leg != null ? leg : getSafeChild(root, "body");
            } else {
                if (Math.abs(ox) > 0.22) {
                    ModelPart arms = getSafeChild(root, "arms");
                    if (arms != null) return arms;
                }
                ModelPart body = getSafeChild(root, "body");
                return body != null ? body : villagerModel.getHead();
            }
        }
        
        // Fallback for non-humanoid models (e.g. quadrupeds, custom models)
        RandomSource random = RandomSource.create(42L);
        return getRandomModelPart(model, random);
    }

    private float[] getLocalPosForPart(M model, ModelPart part, double ox, double oy, double oz, double entityWidth, double entityHeight) {
        float[] local = new float[3];
        if (model instanceof net.minecraft.client.model.HumanoidModel<?> humanoidModel) {
            double widthScale = entityWidth / 0.6D;
            double heightScale = entityHeight / 1.8D;

            float px = 0.0F;
            float py = 0.0F;

            if (part == humanoidModel.head) {
                px = 0.0F;
                py = 0.0F;
            } else if (part == humanoidModel.body) {
                px = 0.0F;
                py = 0.0F;
            } else if (part == humanoidModel.leftArm) {
                px = 0.3125F * (float) widthScale;
                py = 0.125F * (float) heightScale;
            } else if (part == humanoidModel.rightArm) {
                px = -0.3125F * (float) widthScale;
                py = 0.125F * (float) heightScale;
            } else if (part == humanoidModel.leftLeg) {
                px = 0.11875F * (float) widthScale;
                py = 0.75F * (float) heightScale;
            } else if (part == humanoidModel.rightLeg) {
                px = -0.11875F * (float) widthScale;
                py = 0.75F * (float) heightScale;
            }

            local[0] = (float) (ox - px);
            local[1] = (float) ((1.5D - py) * heightScale - oy);
            local[2] = (float) oz;
        } else {
            // Non-humanoid models: center slightly inside the part boundaries
            local[0] = 0.0F;
            local[1] = 0.0F;
            local[2] = 0.0F;
        }
        return local;
    }

    @SuppressWarnings("unchecked")
    private ModelPart getRandomModelPart(M model, RandomSource random) {
        java.util.List<ModelPart> parts = new java.util.ArrayList<>();
        
        if (model instanceof net.minecraft.client.model.HumanoidModel<?> humanoidModel) {
            if (humanoidModel.head != null && !humanoidModel.head.isEmpty()) parts.add(humanoidModel.head);
            if (humanoidModel.body != null && !humanoidModel.body.isEmpty()) parts.add(humanoidModel.body);
            if (humanoidModel.rightArm != null && !humanoidModel.rightArm.isEmpty()) parts.add(humanoidModel.rightArm);
            if (humanoidModel.leftArm != null && !humanoidModel.leftArm.isEmpty()) parts.add(humanoidModel.leftArm);
            if (humanoidModel.rightLeg != null && !humanoidModel.rightLeg.isEmpty()) parts.add(humanoidModel.rightLeg);
            if (humanoidModel.leftLeg != null && !humanoidModel.leftLeg.isEmpty()) parts.add(humanoidModel.leftLeg);
        } else if (model instanceof net.minecraft.client.model.HierarchicalModel<?> hierarchicalModel) {
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
