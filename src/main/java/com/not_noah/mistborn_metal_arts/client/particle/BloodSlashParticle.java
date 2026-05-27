package com.not_noah.mistborn_metal_arts.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A client-side blood slash decal particle that aligns flat against an entity's
 * surface and tracks its position in real-time.
 */
@OnlyIn(Dist.CLIENT)
public class BloodSlashParticle extends TextureSheetParticle {
    private final int entityId;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final float rollAngle;
    private final float woundScale;
    private final SpriteSet spriteSet;

    protected BloodSlashParticle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);

        this.entityId = (int) xSpeed;
        this.spriteSet = spriteSet;
        this.woundScale = (float) zSpeed;
        
        // Pick specific texture variant based on ySpeed (slashType)
        int slashType = Math.min(2, Math.max(0, (int) ySpeed));
        this.setSprite(this.spriteSet.get(slashType, 2));

        this.gravity = 0.0F;
        this.hasPhysics = false;

        // Visual lifespan: ~3 to 4.5 seconds
        this.lifetime = 60 + this.random.nextInt(30);
        
        // Base quad size
        this.quadSize = 0.55F + this.random.nextFloat() * 0.15F;

        // Full brightness for transparent premium texture
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;

        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;

        // Random roll angle for visual cut variation
        this.rollAngle = this.random.nextFloat() * (float) Math.PI * 2.0F;

        // Resolve parent entity relative position
        Entity entity = level.getEntity(this.entityId);
        if (entity != null) {
            this.offsetX = x - entity.getX();
            this.offsetY = y - entity.getY();
            this.offsetZ = z - entity.getZ();
        } else {
            this.offsetX = 0;
            this.offsetY = 0;
            this.offsetZ = 0;
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        Entity entity = this.level.getEntity(this.entityId);
        if (entity == null || !entity.isAlive()) {
            // Rapid fade-out if parent entity is dead/removed
            this.alpha *= 0.85F;
            if (this.alpha < 0.05F) {
                this.remove();
            }
            return;
        }

        // Dynamically bind to the moving entity
        this.x = entity.getX() + this.offsetX;
        this.y = entity.getY() + this.offsetY;
        this.z = entity.getZ() + this.offsetZ;

        // Linear fade-out in final 40% of life
        float lifeRatio = (float) this.age / (float) this.lifetime;
        if (lifeRatio > 0.6F) {
            this.alpha = 1.0F - ((lifeRatio - 0.6F) / 0.4F);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        float px = (float) (Mth.lerp((double) partialTicks, this.xo, this.x) - cameraPos.x());
        float py = (float) (Mth.lerp((double) partialTicks, this.yo, this.y) - cameraPos.y());
        float pz = (float) (Mth.lerp((double) partialTicks, this.zo, this.z) - cameraPos.z());

        // 1. Calculate Outward Surface Normal Vector
        double hDist = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        Vec3 normalVec;
        if (hDist > 0.001D) {
            normalVec = new Vec3(offsetX / hDist, 0, offsetZ / hDist);
        } else {
            normalVec = new Vec3(1, 0, 0);
        }

        // 2. Generate Orthonormal Coordinate Frame (Right & Up vectors)
        Vec3 rightVec = normalVec.cross(new Vec3(0, 1, 0)).normalize();
        
        Vector3f right = new Vector3f((float) rightVec.x, (float) rightVec.y, (float) rightVec.z);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f normal = new Vector3f((float) normalVec.x, (float) normalVec.y, (float) normalVec.z);

        // 3. Rotate Orthonormal Frame around Normal by Roll tilt
        Quaternionf rotation = new Quaternionf().rotationAxis(this.rollAngle, normal);
        right.rotate(rotation);
        up.rotate(rotation);

        // 4. Offset slightly outward along the normal to prevent model z-fighting
        float bx = px + (float) normalVec.x * 0.012F;
        float by = py;
        float bz = pz + (float) normalVec.z * 0.012F;

        // 5. Construct scaled oriented vertices
        float size = this.quadSize * this.woundScale;
        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        for (Vector3f v : vertices) {
            float vx = (v.x() * right.x() + v.y() * up.x()) * size;
            float vy = (v.x() * right.y() + v.y() * up.y()) * size;
            float vz = (v.x() * right.z() + v.y() * up.z()) * size;
            v.set(bx + vx, by + vy, bz + vz);
        }

        // 6. Draw quad with translucent render settings
        int light = this.getLightColor(partialTicks);
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        buffer.vertex((double) vertices[0].x(), (double) vertices[0].y(), (double) vertices[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex((double) vertices[1].x(), (double) vertices[1].y(), (double) vertices[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex((double) vertices[2].x(), (double) vertices[2].y(), (double) vertices[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex((double) vertices[3].x(), (double) vertices[3].y(), (double) vertices[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            BloodSlashParticle particle = new BloodSlashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
            return particle;
        }
    }
}
