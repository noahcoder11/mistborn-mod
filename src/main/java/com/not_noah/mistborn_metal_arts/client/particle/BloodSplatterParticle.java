package com.not_noah.mistborn_metal_arts.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A flat, stationary blood splatter particle that sits on the ground surface
 * and slowly fades out over several seconds. Created when a BloodDropParticle
 * impacts a solid block.
 *
 * The particle is rendered flat (billboard facing UP) with no gravity,
 * giving the appearance of a ground decal that gradually disappears.
 */
@OnlyIn(Dist.CLIENT)
public class BloodSplatterParticle extends TextureSheetParticle {

    protected BloodSplatterParticle(ClientLevel level, double x, double y, double z, double sizeScale) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);

        this.gravity = 0.0F;
        this.hasPhysics = false;

        // Extremely long lifespan: 30–60 seconds (600–1200 ticks) for awesome blood tracking trails!
        this.lifetime = 600 + this.random.nextInt(600);

        // Splatter size (base 0.25 to 0.75, scaled by sizeScale)
        double scale = sizeScale > 0.001D ? sizeScale : 1.0D;
        this.quadSize = (float) ((0.25F + this.random.nextFloat() * 0.50F) * scale);

        // Extremely dark blood red
        this.rCol = 0.15F + this.random.nextFloat() * 0.15F;
        this.gCol = 0.01F + this.random.nextFloat() * 0.02F;
        this.bCol = 0.01F + this.random.nextFloat() * 0.02F;

        // No movement
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;

        // Random rotation for visual variety
        this.roll = this.random.nextFloat() * (float) Math.PI * 2.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 vec3 = camera.getPosition();
        float x = (float)(Mth.lerp((double)partialTicks, this.xo, this.x) - vec3.x());
        float y = (float)(Mth.lerp((double)partialTicks, this.yo, this.y) - vec3.y());
        float z = (float)(Mth.lerp((double)partialTicks, this.zo, this.z) - vec3.z());

        // Rotate 90 degrees around X to lay flat on the X-Z plane.
        // Then apply the random roll around Z (which acts as the Y axis after the first rotation).
        Quaternionf quaternion = new Quaternionf().rotationX((float) Math.PI / 2.0F);
        quaternion.rotateZ(this.roll);

        Vector3f[] avector3f = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };
        float f3 = this.getQuadSize(partialTicks);

        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.rotate(quaternion);
            vector3f.mul(f3);
            vector3f.add(x, y, z);
        }

        int light = this.getLightColor(partialTicks);
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        buffer.vertex((double) avector3f[0].x(), (double) avector3f[0].y(), (double) avector3f[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex((double) avector3f[1].x(), (double) avector3f[1].y(), (double) avector3f[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex((double) avector3f[2].x(), (double) avector3f[2].y(), (double) avector3f[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex((double) avector3f[3].x(), (double) avector3f[3].y(), (double) avector3f[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
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

        // Slow fade-out: fully opaque for first 60%, then linear fade
        float lifeRatio = (float) this.age / (float) this.lifetime;
        if (lifeRatio > 0.6F) {
            this.alpha = 1.0F - ((lifeRatio - 0.6F) / 0.4F);
        }
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
            BloodSplatterParticle particle = new BloodSplatterParticle(level, x, y, z, xSpeed);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
