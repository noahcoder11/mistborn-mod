package com.not_noah.mistborn_metal_arts.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.capability.BloodSlash;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BloodTextureManager {
    private static final ResourceLocation BLOOD_TEXTURE = new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/entity/blood_overlay.png");
    private static final ResourceLocation[] SLASH_TEXTURES = new ResourceLocation[]{
            new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/particle/blood_slash_1.png"),
            new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/particle/blood_slash_2.png"),
            new ResourceLocation(MistbornMetalArts.MOD_ID, "textures/particle/blood_slash_3.png")
    };

    private static final Map<Integer, DynamicBloodSkin> CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, NativeImage> TEMPLATE_IMAGES = new ConcurrentHashMap<>();

    public static ResourceLocation getOrCreateBloodTexture(LivingEntity entity, ResourceLocation baseTexture, float bloodLevel, List<BloodSlash> slashes) {
        int entityId = entity.getId();
        String currentFingerprint = getBloodFingerprint(bloodLevel, slashes);

        DynamicBloodSkin skin = CACHE.get(entityId);
        if (skin != null) {
            // If the base skin texture changed, close and recreate
            if (!skin.baseTexture.equals(baseTexture)) {
                skin.close();
                CACHE.remove(entityId);
                skin = null;
            }
        }

        if (skin == null) {
            // Query base texture dimensions
            int width = 64;
            int height = 64;
            try {
                var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(baseTexture);
                if (resourceOpt.isPresent()) {
                    try (var stream = resourceOpt.get().open()) {
                        try (NativeImage baseImage = NativeImage.read(stream)) {
                            width = baseImage.getWidth();
                            height = baseImage.getHeight();
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to 64x64
            }

            NativeImage overlayImage = new NativeImage(width, height, true);
            DynamicTexture dynamicTexture = new DynamicTexture(overlayImage);
            ResourceLocation dynamicLocation = new ResourceLocation(MistbornMetalArts.MOD_ID, "blood_skins/" + entityId + "_" + System.currentTimeMillis());
            
            Minecraft.getInstance().getTextureManager().register(dynamicLocation, dynamicTexture);

            skin = new DynamicBloodSkin(baseTexture, dynamicLocation, dynamicTexture, overlayImage);
            CACHE.put(entityId, skin);
        }

        // Check if anything has changed compared to last drawn fingerprint
        if (!currentFingerprint.equals(skin.lastFingerprint)) {
            skin.redraw(entity.getBbWidth(), entity.getBbHeight(), bloodLevel, slashes);
            skin.lastFingerprint = currentFingerprint;
        }

        return skin.dynamicTextureLocation;
    }

    public static void clearCache() {
        CACHE.forEach((id, skin) -> skin.close());
        CACHE.clear();
    }

    public static void cleanExpiredEntities(Minecraft mc) {
        if (mc.level == null) {
            clearCache();
            return;
        }
        CACHE.entrySet().removeIf(entry -> {
            var entity = mc.level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive()) {
                entry.getValue().close();
                return true;
            }
            return false;
        });
    }

    private static NativeImage getTemplate(ResourceLocation loc) {
        return TEMPLATE_IMAGES.computeIfAbsent(loc, r -> {
            try {
                var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(r);
                if (resourceOpt.isPresent()) {
                    try (var stream = resourceOpt.get().open()) {
                        return NativeImage.read(stream);
                    }
                }
            } catch (Exception e) {
                MistbornMetalArts.LOGGER.error("Failed to load blood template texture: " + r, e);
            }
            return null;
        });
    }

    private static String getBloodFingerprint(float bloodLevel, List<BloodSlash> slashes) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.3f", bloodLevel));
        if (slashes != null) {
            for (BloodSlash slash : slashes) {
                sb.append("|")
                  .append(slash.getSlashType())
                  .append(",")
                  .append(String.format("%.3f", slash.getOx()))
                  .append(",")
                  .append(String.format("%.3f", slash.getOy()))
                  .append(",")
                  .append(String.format("%.3f", slash.getOz()))
                  .append(",")
                  .append(String.format("%.3f", slash.getScale()))
                  .append(",")
                  .append(String.format("%.3f", slash.getRoll()))
                  .append(",")
                  .append(String.format("%.3f", slash.getAlpha()));
            }
        }
        return sb.toString();
    }

    private static class DynamicBloodSkin {
        public final ResourceLocation baseTexture;
        public final ResourceLocation dynamicTextureLocation;
        public final DynamicTexture dynamicTexture;
        public final NativeImage overlayImage;
        public String lastFingerprint = "";

        public DynamicBloodSkin(ResourceLocation baseTexture, ResourceLocation dynamicTextureLocation, DynamicTexture dynamicTexture, NativeImage overlayImage) {
            this.baseTexture = baseTexture;
            this.dynamicTextureLocation = dynamicTextureLocation;
            this.dynamicTexture = dynamicTexture;
            this.overlayImage = overlayImage;
        }

        public void redraw(double entityWidth, double entityHeight, float bloodLevel, List<BloodSlash> slashes) {
            int oW = overlayImage.getWidth();
            int oH = overlayImage.getHeight();

            // 1. Clear with completely transparent pixels
            for (int y = 0; y < oH; y++) {
                for (int x = 0; x < oW; x++) {
                    overlayImage.setPixelRGBA(x, y, 0);
                }
            }

            // 2. Draw general blood level splatters with dynamic multi-layering based on missing health / blood level!
            // As they get closer to dying, we progressively render more layered splatters (standard, mirrored, offset, diagonal).
            if (bloodLevel > 0.01F) {
                NativeImage bloodTemplate = getTemplate(BLOOD_TEXTURE);
                if (bloodTemplate != null) {
                    int tW = bloodTemplate.getWidth();
                    int tH = bloodTemplate.getHeight();

                    // Smooth continuous scaling of opacity for each layer to avoid sudden popping in!
                    float l1Factor = Math.min(1.0f, bloodLevel * 1.5f);
                    float l2Factor = Math.max(0.0f, Math.min(1.0f, (bloodLevel - 0.2f) * 1.5f));
                    float l3Factor = Math.max(0.0f, Math.min(1.0f, (bloodLevel - 0.4f) * 2.0f));
                    float l4Factor = Math.max(0.0f, Math.min(1.0f, (bloodLevel - 0.6f) * 2.5f));
                    float l5Factor = Math.max(0.0f, Math.min(1.0f, (bloodLevel - 0.8f) * 5.0f));

                    for (int y = 0; y < oH; y++) {
                        for (int x = 0; x < oW; x++) {
                            int tx = (x * tW) / oW;
                            int ty = (y * tH) / oH;

                            // Layer 1: Standard mapping
                            if (l1Factor > 0.0f) {
                                drawTemplatePixel(bloodTemplate, tx, ty, x, y, l1Factor, tW, tH);
                            }
                            // Layer 2: Mirrored (x & y) mapping
                            if (l2Factor > 0.0f) {
                                drawTemplatePixel(bloodTemplate, tW - 1 - tx, tH - 1 - ty, x, y, l2Factor, tW, tH);
                            }
                            // Layer 3: Shifted offset mapping
                            if (l3Factor > 0.0f) {
                                drawTemplatePixel(bloodTemplate, (tx + tW / 3) % tW, (ty + tH / 4) % tH, x, y, l3Factor, tW, tH);
                            }
                            // Layer 4: Diagonal mirrored & offset mapping
                            if (l4Factor > 0.0f) {
                                drawTemplatePixel(bloodTemplate, tW - 1 - ((tx + tW / 2) % tW), (ty + tH / 3) % tH, x, y, l4Factor, tW, tH);
                            }
                            // Layer 5: Extreme Drenched mapping (offset and flipped)
                            if (l5Factor > 0.0f) {
                                drawTemplatePixel(bloodTemplate, (tx + tW / 5) % tW, tH - 1 - ((ty + tH / 5) % tH), x, y, l5Factor, tW, tH);
                            }
                        }
                    }
                }
            }

            // 3. Draw active high-res slash decals mapped lock-in to limbs UV coordinates!
            if (slashes != null && !slashes.isEmpty()) {
                for (BloodSlash slash : slashes) {
                    TargetFace face = getHitFace(slash.getOx(), slash.getOy(), slash.getOz(), entityWidth, entityHeight, oW, oH);
                    if (face == null) continue;

                    double cx = face.minU + face.fractionX * (face.maxU - face.minU);
                    double cy = face.minV + face.fractionY * (face.maxV - face.minV);

                    int slashType = Math.min(2, Math.max(0, slash.getSlashType()));
                    NativeImage slashTemplate = getTemplate(SLASH_TEXTURES[slashType]);
                    if (slashTemplate == null) continue;

                    int sW = slashTemplate.getWidth();
                    int sH = slashTemplate.getHeight();

                    // Map slash scale dynamically
                    double baseSize = 8.0 * (oW / 64.0);
                    double destSize = baseSize * slash.getScale();

                    float roll = slash.getRoll();
                    double cos = Math.cos(-roll);
                    double sin = Math.sin(-roll);
                    float alpha = slash.getAlpha();

                    // Paint slash bounded by target face only to prevent visual UV bleeding/wrapping issues!
                    int minX = (int) Math.max(face.minU, Math.floor(cx - destSize));
                    int maxX = (int) Math.min(face.maxU, Math.ceil(cx + destSize));
                    int minY = (int) Math.max(face.minV, Math.floor(cy - destSize));
                    int maxY = (int) Math.min(face.maxV, Math.ceil(cy + destSize));

                    for (int dy = minY; dy < maxY; dy++) {
                        for (int dx = minX; dx < maxX; dx++) {
                            double rx = dx - cx;
                            double ry = dy - cy;

                            double rotX = rx * cos - ry * sin;
                            double rotY = rx * sin + ry * cos;

                            double sx = sW / 2.0 + rotX * (sW / (2.0 * destSize));
                            double sy = sH / 2.0 + rotY * (sH / (2.0 * destSize));

                            int isx = (int) sx;
                            int isy = (int) sy;
                            if (isx >= 0 && isx < sW && isy >= 0 && isy < sH) {
                                int srcCol = slashTemplate.getPixelRGBA(isx, isy);
                                int srcA = (srcCol >> 24) & 0xFF;
                                if (srcA > 0) {
                                    int srcB = (srcCol >> 16) & 0xFF;
                                    int srcG = (srcCol >> 8) & 0xFF;
                                    int srcR = srcCol & 0xFF;

                                    int effectiveA = (int) (srcA * alpha);
                                    if (effectiveA > 0) {
                                        blendPixel(overlayImage, dx, dy, srcR, srcG, srcB, effectiveA);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Upload to GPU
            dynamicTexture.upload();
        }

        private void drawTemplatePixel(NativeImage template, int tx, int ty, int destX, int destY, float layerFactor, int tW, int tH) {
            if (tx >= 0 && tx < tW && ty >= 0 && ty < tH) {
                int srcCol = template.getPixelRGBA(tx, ty);
                int srcA = (srcCol >> 24) & 0xFF;
                if (srcA > 0) {
                    int srcB = (srcCol >> 16) & 0xFF;
                    int srcG = (srcCol >> 8) & 0xFF;
                    int srcR = srcCol & 0xFF;

                    int alpha = (int) (srcA * layerFactor);
                    if (alpha > 0) {
                        blendPixel(overlayImage, destX, destY, srcR, srcG, srcB, alpha);
                    }
                }
            }
        }

        private void blendPixel(NativeImage image, int x, int y, int srcR, int srcG, int srcB, int srcA) {
            int destCol = image.getPixelRGBA(x, y);
            int destA = (destCol >> 24) & 0xFF;
            int destB = (destCol >> 16) & 0xFF;
            int destG = (destCol >> 8) & 0xFF;
            int destR = destCol & 0xFF;

            int outA = srcA + destA * (255 - srcA) / 255;
            if (outA > 0) {
                int outR = (srcR * srcA + destR * destA * (255 - srcA) / 255) / outA;
                int outG = (srcG * srcA + destG * destA * (255 - srcA) / 255) / outA;
                int outB = (srcB * srcA + destB * destA * (255 - srcA) / 255) / outA;

                image.setPixelRGBA(x, y, (outA << 24) | (outB << 16) | (outG << 8) | outR);
            }
        }

        public void close() {
            try {
                dynamicTexture.close();
                overlayImage.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private static class TargetFace {
        public final int minU, maxU, minV, maxV;
        public final double fractionX, fractionY;

        public TargetFace(int minU, int maxU, int minV, int maxV, double fractionX, double fractionY) {
            this.minU = minU;
            this.maxU = maxU;
            this.minV = minV;
            this.maxV = maxV;
            this.fractionX = fractionX;
            this.fractionY = fractionY;
        }
    }

    private static TargetFace getHitFace(double ox, double oy, double oz, double entityWidth, double entityHeight, int texWidth, int texHeight) {
        double scaleU = texWidth / 64.0;
        double scaleV = texHeight / 64.0;

        // Scale coordinates to standard player dimensions (width 0.6, height 1.8)
        double widthScale = entityWidth / 0.6D;
        double heightScale = entityHeight / 1.8D;
        if (widthScale <= 0.01D) widthScale = 1.0D;
        if (heightScale <= 0.01D) heightScale = 1.0D;

        double sx = ox / widthScale;
        double sy = oy / heightScale;
        double sz = oz / widthScale;

        double hPct = Math.max(0.0, Math.min(1.0, sy / 1.8D));

        int minU = 20, maxU = 28, minV = 20, maxV = 32;
        double fx = 0.5, fy = 0.5;

        if (hPct >= 0.72) {
            // --- Head ---
            double angle = Math.atan2(sx, sz);
            if (angle >= -Math.PI / 4 && angle <= Math.PI / 4) { // Front
                minU = 8; maxU = 16; minV = 8; maxV = 16;
                fx = (sx + 0.25) / 0.5;
            } else if (angle > Math.PI / 4 && angle <= 3 * Math.PI / 4) { // Left Side
                minU = 16; maxU = 24; minV = 8; maxV = 16;
                fx = (sz + 0.25) / 0.5;
            } else if (angle >= -3 * Math.PI / 4 && angle < -Math.PI / 4) { // Right Side
                minU = 0; maxU = 8; minV = 8; maxV = 16;
                fx = (0.25 - sz) / 0.5;
            } else { // Back
                minU = 24; maxU = 32; minV = 8; maxV = 16;
                fx = (0.25 - sx) / 0.5;
            }
            fy = (sy - 0.72 * 1.8D) / (0.28 * 1.8D);
        } else if (hPct < 0.35) {
            // --- Legs ---
            if (sx > 0) { // Left Leg
                minU = 20; maxU = 24; minV = 52; maxV = 64;
                if (sz > 0) { // Front
                    fx = sx / 0.25;
                } else { // Back
                    minU = 28; maxU = 32;
                    fx = (0.25 - sx) / 0.25;
                }
            } else { // Right Leg
                minU = 4; maxU = 8; minV = 20; maxV = 32;
                if (sz > 0) { // Front
                    fx = (sx + 0.25) / 0.25;
                } else { // Back
                    minU = 12; maxU = 16;
                    fx = -sx / 0.25;
                }
            }
            fy = sy / (0.35 * 1.8D);
        } else {
            // --- Torso & Arms ---
            if (sx > 0.22) { // Left Arm
                double armAngle = Math.atan2(sx - 0.3, sz);
                minU = 36; maxU = 40; minV = 52; maxV = 64;
                if (armAngle >= -Math.PI / 4 && armAngle <= Math.PI / 4) { // Front
                    fx = (sx - 0.22) / 0.16;
                } else if (armAngle > Math.PI / 4 && armAngle <= 3 * Math.PI / 4) { // Left (outer)
                    minU = 32; maxU = 36;
                    fx = (sz + 0.2) / 0.4;
                } else if (armAngle >= -3 * Math.PI / 4 && armAngle < -Math.PI / 4) { // Right (inner)
                    minU = 40; maxU = 44;
                    fx = (0.2 - sz) / 0.4;
                } else { // Back
                    minU = 44; maxU = 48;
                    fx = (0.38 - sx) / 0.16;
                }
            } else if (sx < -0.22) { // Right Arm
                double armAngle = Math.atan2(sx + 0.3, sz);
                minU = 44; maxU = 48; minV = 20; maxV = 32;
                if (armAngle >= -Math.PI / 4 && armAngle <= Math.PI / 4) { // Front
                    fx = (sx + 0.38) / 0.16;
                } else if (armAngle >= -3 * Math.PI / 4 && armAngle < -Math.PI / 4) { // Right (outer)
                    minU = 40; maxU = 44;
                    fx = (0.2 - sz) / 0.4;
                } else if (armAngle > Math.PI / 4 && armAngle <= 3 * Math.PI / 4) { // Left (inner)
                    minU = 48; maxU = 52;
                    fx = (sz + 0.2) / 0.4;
                } else { // Back
                    minU = 52; maxU = 56;
                    fx = (-0.22 - sx) / 0.16;
                }
            } else { // Torso
                double angle = Math.atan2(sx, sz);
                minU = 20; maxU = 28; minV = 20; maxV = 32;
                if (angle >= -Math.PI / 4 && angle <= Math.PI / 4) { // Front
                    fx = (sx + 0.22) / 0.44;
                } else if (angle > Math.PI / 4 && angle <= 3 * Math.PI / 4) { // Left
                    minU = 28; maxU = 32;
                    fx = (sz + 0.18) / 0.36;
                } else if (angle >= -3 * Math.PI / 4 && angle < -Math.PI / 4) { // Right
                    minU = 16; maxU = 20;
                    fx = (0.18 - sz) / 0.36;
                } else { // Back
                    minU = 32; maxU = 40;
                    fx = (0.22 - ox) / 0.44;
                }
            }
            fy = (sy - 0.35 * 1.8D) / (0.37 * 1.8D);
        }

        // Apply 64x32 mapping mirroring for downscaled older layouts
        if (texHeight == 32) {
            if (minU == 36 && minV == 52) { minU = 44; maxU = 48; minV = 20; maxV = 32; }
            else if (minU == 44 && minV == 52) { minU = 52; maxU = 56; minV = 20; maxV = 32; }
            else if (minU == 32 && minV == 52) { minU = 40; maxU = 44; minV = 20; maxV = 32; }
            else if (minU == 40 && minV == 52) { minU = 48; maxU = 52; minV = 20; maxV = 32; }
            else if (minU == 20 && minV == 52) { minU = 4; maxU = 8; minV = 20; maxV = 32; }
            else if (minU == 28 && minV == 52) { minU = 12; maxU = 16; minV = 20; maxV = 32; }
            else if (minU == 16 && minV == 52) { minU = 0; maxU = 4; minV = 20; maxV = 32; }
            else if (minU == 24 && minV == 52) { minU = 8; maxU = 12; minV = 20; maxV = 32; }
        }

        int finalMinU = (int) Math.max(0, Math.min(texWidth, Math.round(minU * scaleU)));
        int finalMaxU = (int) Math.max(0, Math.min(texWidth, Math.round(maxU * scaleU)));
        int finalMinV = (int) Math.max(0, Math.min(texHeight, Math.round(minV * scaleV)));
        int finalMaxV = (int) Math.max(0, Math.min(texHeight, Math.round(maxV * scaleV)));

        double clampedFx = Math.max(0.0, Math.min(1.0, fx));
        double clampedFy = Math.max(0.0, Math.min(1.0, fy));

        return new TargetFace(finalMinU, finalMaxU, finalMinV, finalMaxV, clampedFx, clampedFy);
    }
}
