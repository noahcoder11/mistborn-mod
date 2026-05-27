package com.not_noah.mistborn_metal_arts.event;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-side event handler that spawns blood particles whenever a living entity
 * takes melee/slashing damage. Particle count scales with the damage dealt.
 *
 * Runs on the server and uses {@link ServerLevel#sendParticles} to broadcast to
 * all nearby clients — ensuring multiplayer visibility.
 */
@Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID)
public class BloodParticleEventHandler {

    /** Minimum particles per hit. */
    private static final int MIN_PARTICLES = 15;
    /** Particles per point of damage dealt. */
    private static final float PARTICLES_PER_DAMAGE = 8.0F;
    /** Maximum particles per single hit (performance cap). */
    private static final int MAX_PARTICLES = 150;

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float damage = event.getAmount();

        if (target.level().isClientSide) {
            return;
        }

        AABB bb = target.getBoundingBox();
        Vec3 attackDir = Vec3.ZERO;
        if (source.getEntity() != null) {
            attackDir = target.position().subtract(source.getEntity().position()).normalize();
        }

        // --- General Damage Blood Update (Runs for ALL damage sources on the server) ---
        target.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            float oldLevel = data.getBloodLevel();
            data.addBlood(damage * 0.05f); // 20 damage = full blood
            
            if (data.getBloodLevel() != oldLevel) {
                com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork.syncBloodLevel(target, data.getBloodLevel());
            }
        });

        // Also add blood to the attacker (their hands get bloody)
        if (source.getDirectEntity() instanceof LivingEntity attacker) {
            attacker.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                float oldLevel = data.getBloodLevel();
                data.addBlood(damage * 0.02f); // 50 damage dealt = full blood on hands/weapon
                if (data.getBloodLevel() != oldLevel) {
                    com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork.syncBloodLevel(attacker, data.getBloodLevel());
                }
            });

            // Also add blood directly to the attacker's held weapon ItemStack
            net.minecraft.world.item.ItemStack mainHandStack = attacker.getMainHandItem();
            if (isBloodableWeapon(mainHandStack)) {
                float currentBlood = 0.0F;
                if (mainHandStack.hasTag() && mainHandStack.getTag().contains("BloodLevel")) {
                    currentBlood = mainHandStack.getTag().getFloat("BloodLevel");
                }
                float addedBlood = damage * 0.04f; // 25 damage dealt = full blood on weapon
                mainHandStack.getOrCreateTag().putFloat("BloodLevel", Math.min(1.0F, currentBlood + addedBlood));
            }
        }

        // --- General Damage Blood Drop Particles (Runs for ALL damage sources on the server) ---
        if (target.level() instanceof ServerLevel serverLevel) {
            int count = Math.min(MAX_PARTICLES,
                    Math.max(MIN_PARTICLES, (int) (damage * PARTICLES_PER_DAMAGE)));

            // Spawn at the target's bounding box center
            double cx = bb.getCenter().x;
            double cy = bb.getCenter().y;
            double cz = bb.getCenter().z;

            for (int i = 0; i < count; i++) {
                // Randomize spawn within the bounding box
                double px = cx + (target.getRandom().nextDouble() - 0.5D) * bb.getXsize();
                double py = cy + (target.getRandom().nextDouble() - 0.5D) * bb.getYsize() * 0.6D;
                double pz = cz + (target.getRandom().nextDouble() - 0.5D) * bb.getZsize();

                // Velocity: spray outward from attacker direction + random spread + upward arc
                double vx = attackDir.x * 0.15D + (target.getRandom().nextDouble() - 0.5D) * 0.12D;
                double vy = 0.05D + target.getRandom().nextDouble() * 0.15D;
                double vz = attackDir.z * 0.15D + (target.getRandom().nextDouble() - 0.5D) * 0.12D;

                serverLevel.sendParticles(
                        ModParticles.BLOOD_DROP.get(),
                        px, py, pz,
                        0,    // count (0 to send exact velocity to client)
                        vx, vy, vz,
                        1.0D  // speed multiplier (1.0 to keep exact velocity)
                );
            }
        }

        // Trigger slashes and stuck arrows only for direct melee hits or projectiles.
        // Excludes environmental damage (fire, fall, drowning, etc.) from spawning 3D slashes/arrows.
        boolean isMelee = source.getDirectEntity() != null && source.getDirectEntity().equals(source.getEntity());
        boolean isProjectile = source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
        if (!isMelee && !isProjectile) {
            return; 
        }

        double ox, oy, oz;
        int lifetime;
        net.minecraft.world.entity.Entity directSource = source.getDirectEntity();
        final float projX;
        final float projY;
        final float projZ;
        final boolean isArrowVal;

        if (isProjectile && directSource != null) {
            // Projectile hits (arrows, etc.) - clip to bounding box surface!
            Vec3 hitPos = directSource.position();
            java.util.Optional<Vec3> clipped = bb.clip(hitPos, bb.getCenter());
            Vec3 exactHitPos = clipped.orElse(hitPos);
            Vec3 relativePos = exactHitPos.subtract(target.position());

            double rx = relativePos.x;
            double rz = relativePos.z;
            float yawRad = (float) Math.toRadians(-target.yBodyRot);
            ox = rx * Math.cos(yawRad) - rz * Math.sin(yawRad);
            oy = Math.max(0.05D, Math.min(bb.getYsize() - 0.05D, relativePos.y));
            oz = rx * Math.sin(yawRad) + rz * Math.cos(yawRad);

            // Longer lifetime matching vanilla arrow stuck duration (~15 seconds / 300 ticks)
            lifetime = 280 + target.getRandom().nextInt(40);

            // Compute the trajectory look vector in world space
            float yaw = directSource.getYRot();
            float pitch = directSource.getXRot();
            double yawRadVal = Math.toRadians(yaw);
            double pitchRadVal = Math.toRadians(pitch);
            double lookX = -Math.sin(yawRadVal) * Math.cos(pitchRadVal);
            double lookY = -Math.sin(pitchRadVal);
            double lookZ = Math.cos(yawRadVal) * Math.cos(pitchRadVal);

            // Rotate direction vector to target-relative space
            double cosYaw = Math.cos(yawRad);
            double sinYaw = Math.sin(yawRad);
            projX = (float) (lookX * cosYaw - lookZ * sinYaw);
            projY = (float) (-lookY); // Invert Y to match model-space (Y is downwards)
            projZ = (float) (lookX * sinYaw + lookZ * cosYaw);

            isArrowVal = true;
        } else {
            // Melee hits - calculate target-relative coordinates on bounding box surface
            double localX = -attackDir.x * (bb.getXsize() * 0.5D + 0.015D);
            double localZ = -attackDir.z * (bb.getZsize() * 0.5D + 0.015D);

            // Transform to target rotated space
            float yawRad = (float) Math.toRadians(-target.yBodyRot);
            ox = localX * Math.cos(yawRad) - localZ * Math.sin(yawRad);
            oy = (target.getRandom().nextDouble() - 0.5D) * bb.getYsize() * 0.4D + (bb.getYsize() * 0.5D);
            oz = localX * Math.sin(yawRad) + localZ * Math.cos(yawRad);

            // Standard melee wound lifetime (~3.5 seconds / 70 ticks)
            lifetime = 60 + target.getRandom().nextInt(30);

            projX = 0.0F;
            projY = 0.0F;
            projZ = 0.0F;
            isArrowVal = false;
        }

        int slashType = target.getRandom().nextInt(3);
        float scale = 0.65F + (float) Math.min(1.1D, damage * 0.065D);
        float roll = target.getRandom().nextFloat() * (float) Math.PI * 2.0F;

        // Send packet to tracking clients and add to server side list for logical parity
        com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork.sendBloodSlash(target, ox, oy, oz, slashType, scale, roll, lifetime, projX, projY, projZ, isArrowVal);
        target.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            data.addSlash(ox, oy, oz, slashType, scale, roll, lifetime, projX, projY, projZ, isArrowVal);
        });
    }

    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // Always tick slash lifetimes on both client and server sides
        entity.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            if (entity.level().isClientSide) {
                // On client, run decay logic every tick to process smooth slash fadeout
                data.tickDecay(entity);
            } else {
                // On server, tick slashes every tick for logical parity
                if (data.getSlashes() != null && !data.getSlashes().isEmpty()) {
                    data.getSlashes().forEach(com.not_noah.mistborn_metal_arts.capability.BloodSlash::tick);
                    data.getSlashes().removeIf(com.not_noah.mistborn_metal_arts.capability.BloodSlash::isExpired);
                }
            }
        });

        if (entity.level().isClientSide) return;

        // Server-side decay/wash-off of weapons held by the entity
        if (entity.tickCount % 10 == 0 || entity.isInWater() || entity.isInWaterRainOrBubble()) {
            for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                net.minecraft.world.item.ItemStack stack = entity.getItemInHand(hand);
                if (!stack.isEmpty() && isBloodableWeapon(stack)) {
                    if (stack.hasTag() && stack.getTag().contains("BloodLevel")) {
                        float blood = stack.getTag().getFloat("BloodLevel");
                        if (blood > 0.0F) {
                            if (entity.isInWater() || entity.isInWaterRainOrBubble()) {
                                // Rapidly wash off weapons in water or rain
                                blood = Math.max(0.0F, blood - 0.15F);
                            } else {
                                // Slow weapon decay (takes ~200 seconds / 4000 ticks from 1.0 to 0.0)
                                blood = Math.max(0.0F, blood - 0.0025F);
                            }
                            
                            if (blood <= 0.01F) {
                                stack.getTag().remove("BloodLevel");
                                if (stack.getTag().isEmpty()) {
                                    stack.setTag(null);
                                }
                            } else {
                                stack.getTag().putFloat("BloodLevel", blood);
                            }
                        }
                    }
                }
            }
        }

        // Run decay every few ticks to save performance, unless in water
        if (entity.tickCount % 5 == 0 || entity.isInWater() || entity.isInWaterRainOrBubble()) {
            entity.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                float oldLevel = data.getBloodLevel();
                data.tickDecay(entity);
                
                // Sync if significant change (or cleaned completely)
                float newLevel = data.getBloodLevel();
                if (Math.abs(oldLevel - newLevel) > 0.01f || (oldLevel > 0 && newLevel == 0)) {
                    com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork.syncBloodLevel(entity, newLevel);
                }
            });
        }

        // Bleeding effect: Wither II causes constant blood drip
        if (entity.hasEffect(net.minecraft.world.effect.MobEffects.WITHER) && entity.tickCount % 4 == 0) {
            if (entity.level() instanceof ServerLevel serverLevel) {
                net.minecraft.world.phys.AABB bb = entity.getBoundingBox();
                double px = bb.getCenter().x + (entity.getRandom().nextDouble() - 0.5D) * bb.getXsize() * 0.8D;
                double py = bb.getCenter().y + (entity.getRandom().nextDouble() - 0.5D) * bb.getYsize() * 0.8D;
                double pz = bb.getCenter().z + (entity.getRandom().nextDouble() - 0.5D) * bb.getZsize() * 0.8D;
                
                // Spawn a few slow dripping drops
                serverLevel.sendParticles(
                        com.not_noah.mistborn_metal_arts.registry.ModParticles.BLOOD_DROP.get(),
                        px, py, pz,
                        0,
                        0.0D, -0.02D, 0.0D,
                        1.0D
                );
            }
        }
        
        // General blood dripping from being covered in blood (creates dynamic tracking trails)
        entity.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            float bloodLevel = data.getBloodLevel();
            if (bloodLevel > 0.1f && entity.level() instanceof ServerLevel serverLevel) {
                boolean isMoving = entity.getDeltaMovement().horizontalDistanceSqr() > 0.002D;
                // Base check frequency is higher if moving to leave continuous trails
                int baseInterval = isMoving ? 5 : 15;
                // Higher blood level makes drops more frequent (interval scales down to 2 ticks when moving, or 5 ticks when standing)
                int interval = Math.max(isMoving ? 2 : 5, (int) (baseInterval / (bloodLevel * 1.5f + 0.5f)));

                if (entity.tickCount % interval == 0) {
                    net.minecraft.world.phys.AABB bb = entity.getBoundingBox();
                    // Spawn near feet/lower body to make them hit the ground quickly and form tight trails
                    double px = bb.getCenter().x + (entity.getRandom().nextDouble() - 0.5D) * bb.getXsize() * 0.8D;
                    double py = bb.minY + bb.getYsize() * (0.05D + entity.getRandom().nextDouble() * 0.25D); 
                    double pz = bb.getCenter().z + (entity.getRandom().nextDouble() - 0.5D) * bb.getZsize() * 0.8D;
                    
                    serverLevel.sendParticles(
                            com.not_noah.mistborn_metal_arts.registry.ModParticles.BLOOD_DROP.get(),
                            px, py, pz,
                            0,
                            0.0D, -0.05D, 0.0D,
                            1.0D
                    );
                }
            }
        });
        
        // Gameplay: Severe blood loss (critically low health) causes Nausea and Darkness
        if (entity.tickCount % 40 == 0) {
            float healthPercent = entity.getHealth() / entity.getMaxHealth();
            if (healthPercent <= 0.3f) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 80, 0, false, false, true));
            }
            if (healthPercent <= 0.15f) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 80, 0, false, false, true));
            }
        }
        
        // Symptoms also apply while actively bleeding (Wither II effect)
        if (entity.hasEffect(net.minecraft.world.effect.MobEffects.WITHER) && entity.tickCount % 40 == 0) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 80, 0, false, false, true));
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 80, 0, false, false, true));
        }
    }

    /**
     * Server-safe helper that determines whether the given item stack is a weapon or tool capable of accumulating blood.
     */
    public static boolean isBloodableWeapon(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        
        // Base standard weapon types
        if (item instanceof net.minecraft.world.item.SwordItem) return true;
        if (item instanceof net.minecraft.world.item.AxeItem) return true;
        if (item instanceof net.minecraft.world.item.TridentItem) return true;
        if (item instanceof net.minecraft.world.item.BowItem || item instanceof net.minecraft.world.item.CrossbowItem) return true;
        if (item instanceof net.minecraft.world.item.ShieldItem) return true;
        
        // Check by class name patterns to catch modded weapons / custom tools
        String className = item.getClass().getSimpleName().toLowerCase();
        return className.contains("sword") || className.contains("axe") || className.contains("scythe") 
                || className.contains("weapon") || className.contains("spear") || className.contains("dagger") 
                || className.contains("halberd") || className.contains("cleaver") || className.contains("hammer")
                || className.contains("pickaxe") || className.contains("shovel") || className.contains("hoe");
    }
}
