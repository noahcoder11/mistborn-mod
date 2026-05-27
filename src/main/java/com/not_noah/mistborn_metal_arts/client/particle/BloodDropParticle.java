package com.not_noah.mistborn_metal_arts.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A gravity-affected blood droplet particle that arcs outward and falls.
 * When it collides with a solid block below, it spawns a BloodSplatterParticle
 * and removes itself — creating a convincing ground-stain effect.
 *
 * Color: deep crimson / dark blood red (0.35–0.55 R, 0.02–0.06 G, 0.02–0.05 B)
 */
@OnlyIn(Dist.CLIENT)
public class BloodDropParticle extends TextureSheetParticle {

    protected BloodDropParticle(ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        // Gravity — arcs and falls
        this.gravity = 0.85F;
        this.hasPhysics = true;

        // Short–medium lifespan: 15–35 ticks (0.75–1.75 seconds)
        this.lifetime = 15 + this.random.nextInt(20);

        // Determine if this is a combat drop or a trail drip based on initial velocity.
        // Combat drops spray upward and outward (ySpeed > 0 or horizontal velocity), while passive drips fall straight down (ySpeed <= 0, no horizontal speed).
        boolean isCombat = ySpeed > 0.0D || Math.abs(xSpeed) > 0.001D || Math.abs(zSpeed) > 0.001D;

        if (isCombat) {
            // Combat drops: large, splashing clumps
            this.quadSize = 0.08F + this.random.nextFloat() * 0.08F;
        } else {
            // Trail drips: tiny, delicate droplets
            this.quadSize = 0.02F + this.random.nextFloat() * 0.02F;
        }

        // Dark red with higher brightness variation
        this.rCol = 0.1F + this.random.nextFloat() * 0.3F;
        this.gCol = 0.01F + this.random.nextFloat() * 0.02F;
        this.bCol = 0.01F + this.random.nextFloat() * 0.02F;

        // Slight outward velocity (passed in) + downward kick
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
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

        // Apply gravity
        this.yd -= 0.04D * (double) this.gravity;
        this.move(this.xd, this.yd, this.zd);

        // Air drag
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;

        // Ground collision — spawn a splatter and die
        if (this.onGround || (this.yd == 0.0D && this.yo != this.y)) {
            // Spawn a flat splatter particle at impact point
            BlockPos below = BlockPos.containing(this.x, this.y - 0.05D, this.z);
            if (this.level.getBlockState(below).isSolidRender(this.level, below)) {
                this.level.addParticle(
                        com.not_noah.mistborn_metal_arts.registry.ModParticles.BLOOD_SPLATTER.get(),
                        this.x, below.getY() + 1.01D + (this.random.nextDouble() * 0.02D), this.z,
                        (double) (this.quadSize * 8.0F), 0.0D, 0.0D
                );
            }
            this.remove();
        }

        // Fade out near end of life
        float lifeRatio = (float) this.age / (float) this.lifetime;
        this.alpha = 1.0F - (lifeRatio * lifeRatio * 0.5F);
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
            BloodDropParticle particle = new BloodDropParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
