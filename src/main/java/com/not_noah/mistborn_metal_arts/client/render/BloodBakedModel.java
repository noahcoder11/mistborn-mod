package com.not_noah.mistborn_metal_arts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Universal BakedModel wrapper that dynamically overlays a deep red blood stain on model quads
 * by directly manipulating the low-level vertex color buffers during the rendering stage.
 * Utilizes local-space coordinate classification to map progressive blood stains onto the
 * physical anatomy of the specific weapon class. Evaluates splatters uniformly at the quad level
 * to achieve crisp, pixelated organic splatters matching the weapon's texture pixels.
 */
public class BloodBakedModel implements BakedModel {

    public enum WeaponType {
        SWORD,
        AXE,
        PICKAXE,
        SHIELD,
        BOW,
        GENERIC_TOOL
    }

    public static class SplatterCenter {
        public final float x;
        public final float y;
        public final float maxSize;
        public final float onset;

        public SplatterCenter(float x, float y, float maxSize, float onset) {
            this.x = x;
            this.y = y;
            this.maxSize = maxSize;
            this.onset = onset;
        }
    }

    private static final SplatterCenter[] SWORD_SPLATTERS = {
        new SplatterCenter(0.85F, 0.85F, 0.22F, 0.05F), // Tip
        new SplatterCenter(0.70F, 0.70F, 0.18F, 0.15F), // Upper-mid blade
        new SplatterCenter(0.55F, 0.55F, 0.20F, 0.30F), // Mid blade
        new SplatterCenter(0.40F, 0.40F, 0.16F, 0.45F), // Lower blade
        new SplatterCenter(0.30F, 0.45F, 0.14F, 0.60F), // Guard splash
        new SplatterCenter(0.50F, 0.60F, 0.12F, 0.25F), // Side droplet
        new SplatterCenter(0.90F, 0.75F, 0.10F, 0.10F)  // Edge tip droplet
    };

    private static final SplatterCenter[] AXE_SPLATTERS = {
        new SplatterCenter(0.20F, 0.80F, 0.25F, 0.05F), // Left blade tip
        new SplatterCenter(0.80F, 0.80F, 0.25F, 0.10F), // Right blade tip (axes can face either way)
        new SplatterCenter(0.50F, 0.75F, 0.20F, 0.30F), // Upper center head
        new SplatterCenter(0.25F, 0.60F, 0.18F, 0.25F), // Lower left blade
        new SplatterCenter(0.75F, 0.60F, 0.18F, 0.35F), // Lower right blade
        new SplatterCenter(0.45F, 0.50F, 0.12F, 0.50F)  // Neck droplet
    };

    private static final SplatterCenter[] PICKAXE_SPLATTERS = {
        new SplatterCenter(0.15F, 0.75F, 0.22F, 0.05F), // Left pick tip
        new SplatterCenter(0.85F, 0.75F, 0.22F, 0.05F), // Right pick tip
        new SplatterCenter(0.50F, 0.70F, 0.18F, 0.30F), // Center pick head
        new SplatterCenter(0.30F, 0.65F, 0.15F, 0.25F), // Inner curve left
        new SplatterCenter(0.70F, 0.65F, 0.15F, 0.25F), // Inner curve right
        new SplatterCenter(0.50F, 0.55F, 0.10F, 0.50F)  // Shaft neck droplet
    };

    private static final SplatterCenter[] SHIELD_SPLATTERS = {
        new SplatterCenter(0.50F, 0.50F, 0.35F, 0.10F), // Center splash
        new SplatterCenter(0.75F, 0.75F, 0.22F, 0.20F), // Top right splatter
        new SplatterCenter(0.25F, 0.25F, 0.22F, 0.25F), // Bottom left splatter
        new SplatterCenter(0.25F, 0.75F, 0.20F, 0.40F), // Top left splatter
        new SplatterCenter(0.75F, 0.25F, 0.20F, 0.45F), // Bottom right splatter
        new SplatterCenter(0.50F, 0.85F, 0.15F, 0.30F), // Top edge droplet
        new SplatterCenter(0.15F, 0.50F, 0.15F, 0.35F)  // Left edge droplet
    };

    private static final SplatterCenter[] BOW_SPLATTERS = {
        new SplatterCenter(0.75F, 0.75F, 0.20F, 0.05F), // Upper limb
        new SplatterCenter(0.25F, 0.25F, 0.20F, 0.10F), // Lower limb
        new SplatterCenter(0.50F, 0.50F, 0.15F, 0.30F), // Grip
        new SplatterCenter(0.60F, 0.60F, 0.15F, 0.25F), // Mid-upper limb
        new SplatterCenter(0.40F, 0.40F, 0.15F, 0.35F)  // Mid-lower limb
    };

    private static final SplatterCenter[] GENERIC_SPLATTERS = {
        new SplatterCenter(0.50F, 0.80F, 0.28F, 0.05F), // Top center
        new SplatterCenter(0.35F, 0.75F, 0.20F, 0.20F), // Top left
        new SplatterCenter(0.65F, 0.75F, 0.20F, 0.20F), // Top right
        new SplatterCenter(0.50F, 0.60F, 0.15F, 0.40F), // Neck
        new SplatterCenter(0.50F, 0.45F, 0.12F, 0.60F)  // Handle droplet
    };

    private final BakedModel originalModel;
    private final float bloodLevel;
    private final WeaponType weaponType;
    private final ItemOverrides overrides;

    // The scanned 3D local bounding box of the item model
    private float minX = 0.0F, maxX = 1.0F;
    private float minY = 0.0F, maxY = 1.0F;
    private float minZ = 0.0F, maxZ = 1.0F;

    public BloodBakedModel(BakedModel originalModel, float bloodLevel) {
        this(originalModel, bloodLevel, WeaponType.GENERIC_TOOL);
    }

    public BloodBakedModel(BakedModel originalModel, float bloodLevel, WeaponType weaponType) {
        this.originalModel = originalModel;
        this.bloodLevel = bloodLevel;
        this.weaponType = weaponType;
        
        // Only wrap the overrides once for the base model (when bloodLevel == 0.0F)
        if (bloodLevel == 0.0F) {
            this.overrides = new BloodItemOverrides(originalModel.getOverrides());
        } else {
            this.overrides = originalModel.getOverrides();
        }

        // Scan all quads in the original model to calculate the precise 3D local bounding box
        try {
            float x0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE;
            float y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
            float z0 = Float.MAX_VALUE, z1 = -Float.MAX_VALUE;
            boolean found = false;

            // Retrieve the standard quads of the item model
            List<BakedQuad> quads = originalModel.getQuads(null, null, RandomSource.create(42L));
            if (quads != null && !quads.isEmpty()) {
                for (BakedQuad quad : quads) {
                    int[] vertices = quad.getVertices();
                    int vertexStride = vertices.length / 4;
                    for (int i = 0; i < 4; i++) {
                        float vx = Float.intBitsToFloat(vertices[i * vertexStride + 0]);
                        float vy = Float.intBitsToFloat(vertices[i * vertexStride + 1]);
                        float vz = Float.intBitsToFloat(vertices[i * vertexStride + 2]);
                        
                        if (Float.isFinite(vx) && Float.isFinite(vy) && Float.isFinite(vz)) {
                            if (vx < x0) x0 = vx;
                            if (vx > x1) x1 = vx;
                            if (vy < y0) y0 = vy;
                            if (vy > y1) y1 = vy;
                            if (vz < z0) z0 = vz;
                            if (vz > z1) z1 = vz;
                            found = true;
                        }
                    }
                }
            }
            
            if (found) {
                this.minX = x0;
                this.maxX = Math.max(x1, x0 + 0.01F);
                this.minY = y0;
                this.maxY = Math.max(y1, y0 + 0.01F);
                this.minZ = z0;
                this.maxZ = Math.max(z1, z0 + 0.01F);
            }
        } catch (Exception e) {
            // Fallback to normalized defaults if scan fails
            this.minX = 0.0F; this.maxX = 1.0F;
            this.minY = 0.0F; this.maxY = 1.0F;
            this.minZ = 0.0F; this.maxZ = 1.0F;
        }
    }

    public BakedModel getOriginalModel() {
        return this.originalModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        List<BakedQuad> originalQuads = this.originalModel.getQuads(state, side, rand);
        if (this.bloodLevel <= 0.01F || originalQuads.isEmpty()) {
            return originalQuads;
        }

        List<BakedQuad> bloodyQuads = new ArrayList<>(originalQuads.size());
        for (BakedQuad quad : originalQuads) {
            bloodyQuads.add(makeBloodyQuad(quad, this.bloodLevel));
        }
        return bloodyQuads;
    }

    /**
     * Clones the given quad and modifies the color components of its 4 vertices to apply a red blood tint.
     * Evaluates calculations uniformly at the quad center to render crisp, pixel-aligned splatters.
     */
    private BakedQuad makeBloodyQuad(BakedQuad quad, float bloodLevel) {
        int[] originalVertices = quad.getVertices();
        int[] vertices = Arrays.copyOf(originalVertices, originalVertices.length);
        int vertexStride = vertices.length / 4;
        int colorOffset = 3; // Standard for DefaultVertexFormat.BLOCK and ITEM

        // 1. Calculate quad center by averaging the 4 vertices to perform uniform quad-level evaluation
        float cx = 0.0F;
        float cy = 0.0F;
        float cz = 0.0F;
        for (int i = 0; i < 4; i++) {
            cx += Float.intBitsToFloat(vertices[i * vertexStride + 0]);
            cy += Float.intBitsToFloat(vertices[i * vertexStride + 1]);
            cz += Float.intBitsToFloat(vertices[i * vertexStride + 2]);
        }
        cx /= 4.0F;
        cy /= 4.0F;
        cz /= 4.0F;

        // 2. High-fidelity dual-octave organic noise using quad center coordinates
        // Low-frequency organic patches (blood pools)
        double nl1 = Math.sin(cx * 15.13 + cy * 21.79 + cz * 18.43);
        double nl2 = Math.cos(cx * 29.41 - cy * 13.87 + cz * 25.19);
        float noiseLow = (float) ((nl1 * 0.6 + nl2 * 0.4 + 1.0) * 0.5);

        // High-frequency splatter details (fine droplets)
        double nh1 = Math.sin(cx * 73.19 + cy * 91.53 + cz * 83.27);
        double nh2 = Math.cos(cx * 143.41 - cy * 57.19 + cz * 111.87);
        float noiseHigh = (float) ((nh1 * 0.6 + nh2 * 0.4 + 1.0) * 0.5);

        // Combined organic noise
        float noiseVal = noiseLow * 0.6F + noiseHigh * 0.4F;

        // 3. Compute normalized relative coordinates [0.0..1.0] in local bounding space
        float relX = (cx - minX) / (maxX - minX);
        float relY = (cy - minY) / (maxY - minY);
        float relZ = (cz - minZ) / (maxZ - minZ);

        relX = Math.max(0.0F, Math.min(1.0F, relX));
        relY = Math.max(0.0F, Math.min(1.0F, relY));
        relZ = Math.max(0.0F, Math.min(1.0F, relZ));

        // 4. Select the active splatter preset array for the weapon class
        SplatterCenter[] splatters;
        switch (this.weaponType) {
            case SWORD: splatters = SWORD_SPLATTERS; break;
            case AXE: splatters = AXE_SPLATTERS; break;
            case PICKAXE: splatters = PICKAXE_SPLATTERS; break;
            case SHIELD: splatters = SHIELD_SPLATTERS; break;
            case BOW: splatters = BOW_SPLATTERS; break;
            case GENERIC_TOOL:
            default:
                splatters = GENERIC_SPLATTERS;
                break;
        }

        // 5. Evaluate combined distance field from active splatter centers
        float maxField = 0.0F;
        for (SplatterCenter sc : splatters) {
            if (bloodLevel >= sc.onset) {
                // Growth progress of this specific splatter from its onset to 1.0 blood level
                float progress = (bloodLevel - sc.onset) / (1.0F - sc.onset);
                // Splatter grows organically as bloodLevel increases (starting at 20% size)
                float currentSize = sc.maxSize * (0.20F + 0.80F * progress);

                float dx = relX - sc.x;
                float dy = relY - sc.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                // Perturb the distance calculation with dual-octave noise to achieve organic, jagged edges
                float perturbedDist = dist + (1.0F - noiseVal) * 0.08F;

                if (perturbedDist < currentSize) {
                    float fieldVal = 1.0F - (perturbedDist / currentSize);
                    if (fieldVal > maxField) {
                        maxField = fieldVal;
                    }
                }
            }
        }

        // 6. Crisp, organic step function with a narrow transition zone to avoid being too discrete
        float localBlend = 0.0F;
        if (maxField > 0.0F) {
            if (maxField >= 0.10F) {
                localBlend = 1.0F;
            } else {
                localBlend = maxField / 0.10F;
            }
        }

        // Determine target color and thickness based on noise (rich coagulated clotted blood vs vibrant fresh blood)
        int localTargetR;
        int localTargetG;
        int localTargetB;
        float bloodThickness;

        if (noiseVal > 0.65F) {
            // Thick, coagulated clotted blood (gorgeous crimson)
            localTargetR = 140 + (int) (noiseVal * 20); // 140 to 160
            localTargetG = 4;
            localTargetB = 4;
            bloodThickness = 0.95F;
        } else if (noiseVal > 0.30F) {
            // Vibrant fresh blood (bright arterial red)
            localTargetR = 190 + (int) (noiseVal * 35); // 190 to 225
            localTargetG = 10 + (int) (noiseVal * 5);
            localTargetB = 10 + (int) (noiseVal * 5);
            bloodThickness = 0.85F;
        } else {
            // Thin glaze/splatter fadeout (vibrant translucent red)
            localTargetR = 160 + (int) (noiseVal * 30); // 160 to 190
            localTargetG = 8;
            localTargetB = 8;
            bloodThickness = 0.65F;
        }

        localBlend = Math.max(0.0F, Math.min(1.0F, localBlend * bloodThickness));

        // 6. Loop over the 4 vertices to apply the quad-level blood overlay
        for (int i = 0; i < 4; i++) {
            int colorIdx = i * vertexStride + colorOffset;
            if (colorIdx < vertices.length) {
                int col = vertices[colorIdx];
                int r = col & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = (col >> 16) & 0xFF;
                int a = (col >> 24) & 0xFF; // Respect transparency

                // Calculate the luminance of the original texture pixel to scale target colors.
                // This ensures blood respects highlights, shadows, and base texture details organically.
                float luminance = (r * 0.299F + g * 0.587F + b * 0.114F) / 255.0F;
                // High-visibility minimum floor of 0.60 to make blood pop beautifully on dark items like Netherite
                float brightnessScale = 0.60F + luminance * 0.40F;

                int finalTargetR = (int) (localTargetR * brightnessScale);
                int finalTargetG = (int) (localTargetG * brightnessScale);
                int finalTargetB = (int) (localTargetB * brightnessScale);

                int newR = (int) (r * (1.0F - localBlend) + finalTargetR * localBlend);
                int newG = (int) (g * (1.0F - localBlend) + finalTargetG * localBlend);
                int newB = (int) (b * (1.0F - localBlend) + finalTargetB * localBlend);

                // ABGR format representation
                vertices[colorIdx] = (a << 24) | (newB << 16) | (newG << 8) | newR;
            }
        }

        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.originalModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.originalModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.originalModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return this.originalModel.isCustomRenderer();
    }

    @Override
    public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() {
        return this.originalModel.getParticleIcon();
    }

    @Override
    public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
        return this.originalModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    @Override
    public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        BakedModel transformedModel = this.originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
        if (transformedModel == this.originalModel) {
            return this;
        }
        return new BloodBakedModel(transformedModel, this.bloodLevel, this.weaponType);
    }
}
