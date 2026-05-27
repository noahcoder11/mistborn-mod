package com.not_noah.mistborn_metal_arts.allomancy;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.Optional;

public final class MetalForceHelper {
    private static final DustParticleOptions BLUE_LINE = new DustParticleOptions(new Vector3f(0.15F, 0.55F, 1.0F), 0.85F);

    private MetalForceHelper() {
    }

    public static void applyTargetedForce(ServerPlayer player, boolean pulling, boolean flaring, boolean duralumin) {
        double range = ServerConfig.VALUES.maxPushPullRange.get();
        // Always try precision target first now, for a better feel
        Optional<ForceTarget> target = findPrecisionTarget(player, range);
        if (target.isEmpty()) {
            // Fallback to auto-targeting if looking into empty air, but only if not shifting
            if (!player.isShiftKeyDown()) {
                target = findBestTarget(player, range);
            }
        }

        if (target.isEmpty()) {
            return;
        }
        applyForce(player, target.get(), pulling, flaring, duralumin ? 3.5D : 1D);
    }

    public static void applyRadialForce(ServerPlayer player, boolean pulling, double burstMultiplier) {
        double range = Math.min(ServerConfig.VALUES.maxPushPullRange.get() * 1.35D, 36D);
        Level level = player.level();
        AABB box = player.getBoundingBox().inflate(range);
        for (Entity entity : level.getEntities(player, box, entity -> entity.isAlive() && isMetallicEntity(entity))) {
            applyForce(player, ForceTarget.entity(entity), pulling, true, burstMultiplier);
        }
        findNearestMetalBlock(player, range).ifPresent(target -> applyForce(player, target, pulling, true, burstMultiplier));
    }

    private static void applyForce(ServerPlayer player, ForceTarget target, boolean pulling, boolean flaring, double burstMultiplier) {
        Vec3 playerCenter = player.position().add(0D, player.getBbHeight() * 0.5D, 0D);
        Vec3 targetCenter = target.position();
        Vec3 fromPlayerToTarget = targetCenter.subtract(playerCenter);
        double distance = Math.max(1.0D, fromPlayerToTarget.length());
        Vec3 direction = fromPlayerToTarget.normalize();
        
        float allomanticStrength = player.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.METAL_ARTS)
                .map(com.not_noah.mistborn_metal_arts.capability.MetalArtsData::getEffectiveStrength)
                .orElse(0.5F);

        double burnStrength = ServerConfig.powerStrength(pulling ? Metal.IRON : Metal.STEEL);
        double flareMultiplier = flaring ? 1.85D : 1D;
        double totalForce = ServerConfig.VALUES.pushPullStrength.get() * burnStrength * flareMultiplier * burstMultiplier * allomanticStrength / Math.sqrt(distance);
        totalForce = Math.min(totalForce, ServerConfig.VALUES.maxPushPullForce.get() * burstMultiplier);

        // Handle Iron Pulling custom disarm and catching actions
        if (pulling && target.entity() != null) {
            Entity targetEnt = target.entity();

            // A. Disarm Check
            if (targetEnt instanceof LivingEntity living && isMetallicStack(living.getMainHandItem())) {
                float flareFactor = flaring ? 1.85F : 1.0F;
                float healthRatio = living.getHealth() / living.getMaxHealth();
                float targetArmor = living.getArmorValue();
                
                float disarmChance = (allomanticStrength * flareFactor) / (healthRatio + (targetArmor / 20.0F));
                if (player.getRandom().nextFloat() < disarmChance) {
                    ItemStack stack = living.getMainHandItem();
                    living.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    
                    ItemEntity itemEntity = new ItemEntity(living.level(), living.getX(), living.getEyeY(), living.getZ(), stack);
                    Vec3 pullVec = playerCenter.subtract(living.getEyePosition()).normalize().scale(1.2D * allomanticStrength * flareFactor);
                    itemEntity.setDeltaMovement(pullVec);
                    living.level().addFreshEntity(itemEntity);
                    
                    living.level().playSound(null, living.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.4F);
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.disarmed", stack.getHoverName()), true);
                    return;
                }
            }

            // B. Kinetic Catch / Collision
            if (targetEnt instanceof ItemEntity itemEntity && distance < 2.0D) {
                if (player.getMainHandItem().isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, itemEntity.getItem());
                    itemEntity.discard();
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
                    return;
                } else {
                    float agilityChance = allomanticStrength * 0.7F;
                    if (player.getRandom().nextFloat() >= agilityChance) {
                        double velocityMag = itemEntity.getDeltaMovement().length();
                        float dmg = (float) (velocityMag * 3.5D * (1.0F - allomanticStrength));
                        if (dmg > 0.5F) {
                            player.hurt(player.damageSources().generic(), dmg);
                            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.6F, 1.2F);
                        }
                    }
                    itemEntity.setDeltaMovement(Vec3.ZERO);
                }
            }
        }

        double playerMass = 1.8D; // Base player mass
        // Add mass for player's metal equipment
        for (ItemStack stack : player.getArmorSlots()) if (isMetallicStack(stack)) playerMass += 0.4D;
        if (isMetallicStack(player.getMainHandItem())) playerMass += 0.25D;
        if (isMetallicStack(player.getOffhandItem())) playerMass += 0.25D;

        double targetMass = target.mass();
        
        // Newton's Third Law: Forces are equal and opposite. Acceleration = Force / Mass.
        // If anchored (block), targetMass is effectively infinite.
        if (target.anchored()) {
            Vec3 playerDelta = pulling ? direction.scale(totalForce) : direction.scale(-totalForce);
            pushEntity(player, playerDelta);
        } else {
            // Proportional movement: heavier moves less
            double totalMass = playerMass + targetMass;
            double playerShare = targetMass / totalMass; // If target is heavier, player moves more
            double targetShare = playerMass / totalMass; // If player is heavier, target moves more
            
            Vec3 playerDelta = pulling ? direction.scale(totalForce * playerShare) : direction.scale(-totalForce * playerShare);
            Vec3 targetDelta = pulling ? direction.scale(-totalForce * targetShare) : direction.scale(totalForce * targetShare);
            
            pushEntity(player, playerDelta);
            if (target.entity() != null) {
                pushEntity(target.entity(), targetDelta);
            }
        }
        drawLine(player, playerCenter, targetCenter);
    }

    private static Optional<ForceTarget> findBestTarget(ServerPlayer player, double range) {
        Optional<ForceTarget> entityTarget = player.level().getEntities(player, player.getBoundingBox().inflate(range), entity -> entity.isAlive() && isMetallicEntity(entity))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .map(ForceTarget::entity);
        if (entityTarget.isPresent()) {
            return entityTarget;
        }
        Optional<ForceTarget> below = findMetalBlockBelow(player, range);
        return below.isPresent() ? below : findNearestMetalBlock(player, Math.min(range, 10D));
    }

    public static Optional<ForceTarget> findPrecisionTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1F).scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK && isMetallicBlock(player.level(), hit.getBlockPos())) {
            return Optional.of(ForceTarget.block(hit.getBlockPos()));
        }
        return player.level().getEntities(player, player.getBoundingBox().inflate(range), entity -> entity.isAlive() && isMetallicEntity(entity) && isInViewCone(player, entity, 0.975D))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .map(ForceTarget::entity);
    }

    private static Optional<ForceTarget> findMetalBlockBelow(ServerPlayer player, double range) {
        BlockPos origin = player.blockPosition();
        int max = (int) Math.min(range, 12D);
        for (int dy = 1; dy <= max; dy++) {
            BlockPos pos = origin.below(dy);
            if (isMetallicBlock(player.level(), pos)) {
                return Optional.of(ForceTarget.block(pos));
            }
        }
        return Optional.empty();
    }

    private static Optional<ForceTarget> findNearestMetalBlock(ServerPlayer player, double range) {
        BlockPos origin = player.blockPosition();
        int radius = (int) Math.min(range, 8D);
        Optional<BlockPos> best = BlockPos.betweenClosedStream(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))
                .filter(pos -> isMetallicBlock(player.level(), pos))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(origin)));
        return best.map(ForceTarget::block);
    }

    private static boolean isInViewCone(ServerPlayer player, Entity entity, double threshold) {
        Vec3 look = player.getViewVector(1F).normalize();
        Vec3 target = entity.position().add(0D, entity.getBbHeight() * 0.5D, 0D).subtract(player.getEyePosition()).normalize();
        return look.dot(target) > threshold;
    }

    public static boolean isMetallicBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModTags.Blocks.METALLIC_BLOCKS) || state.is(ModTags.Blocks.PUSH_PULL_ANCHORS);
    }

    public static boolean isMetallicEntity(Entity entity) {
        // 1. Entities made of metal
        if (entity instanceof net.minecraft.world.entity.animal.IronGolem || 
            entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) {
            return true;
        }

        // 2. Entities tagged as pushable/pullable
        if (entity.getType().is(ModTags.EntityTypes.pushable()) || entity.getType().is(ModTags.EntityTypes.pullable())) {
            return true;
        }

        // 3. Spiked Inquisitors and other spiked metalborn
        if (entity instanceof com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy) {
            return true;
        }

        // 4. Items dropped on the ground
        if (entity instanceof ItemEntity itemEntity) {
            return isMetallicStack(itemEntity.getItem());
        }

        // 5. Metallic projectiles (Arrows, Tridents) — excluding snowballs, eggs, ender pearls
        if (entity instanceof Projectile) {
            return entity instanceof net.minecraft.world.entity.projectile.AbstractArrow || 
                   entity instanceof net.minecraft.world.entity.projectile.ThrownTrident;
        }

        // 6. Living entities holding or wearing actual metallic armor or tools
        if (entity instanceof LivingEntity living) {
            for (ItemStack stack : living.getArmorSlots()) {
                if (isMetallicStack(stack)) {
                    return true;
                }
            }
            return isMetallicStack(living.getMainHandItem()) || isMetallicStack(living.getOffhandItem());
        }

        return false;
    }

    public static boolean isMetallicStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // 1. Direct tag checks
        if (stack.is(ModTags.Items.METALLIC_ITEMS) || 
            stack.is(ModTags.Items.METAL_ARMOR) || 
            stack.is(ModTags.Items.METALMINDS) || 
            stack.is(ModTags.Items.HEMALURGIC_SPIKES) || 
            stack.is(ModTags.Items.GOD_METALS)) {
            return true;
        }

        String name = stack.getItem().getDescriptionId().toLowerCase();
        // 2. Direct string match for common metallic material names (Vanilla & Modded compatibility)
        if (name.contains("iron") || name.contains("gold") || name.contains("steel") || name.contains("copper") || 
            name.contains("bronze") || name.contains("tin") || name.contains("pewter") || name.contains("brass") || 
            name.contains("zinc") || name.contains("nickel") || name.contains("lead") || name.contains("silver") || 
            name.contains("platinum") || name.contains("electrum") || name.contains("aluminum") || name.contains("duralumin") || 
            name.contains("chromium") || name.contains("nicrosil") || name.contains("cadmium") || name.contains("bendalloy") || 
            name.contains("atium") || name.contains("metal") || name.contains("alloy") || name.contains("chainmail") || 
            name.contains("netherite") || name.contains("hemalurgic") || name.contains("spike")) {
            return true;
        }

        // 3. Fallback checks for vanilla ArmorItem materials
        if (stack.getItem() instanceof ArmorItem armor) {
            net.minecraft.world.item.ArmorMaterial mat = armor.getMaterial();
            return mat == net.minecraft.world.item.ArmorMaterials.IRON || 
                   mat == net.minecraft.world.item.ArmorMaterials.GOLD || 
                   mat == net.minecraft.world.item.ArmorMaterials.CHAIN || 
                   mat == net.minecraft.world.item.ArmorMaterials.NETHERITE;
        }

        // 4. Fallback checks for vanilla TieredItem tiers (wood, stone, diamond tools are NOT metallic!)
        if (stack.getItem() instanceof TieredItem tiered) {
            net.minecraft.world.item.Tier tier = tiered.getTier();
            return tier == net.minecraft.world.item.Tiers.IRON || 
                   tier == net.minecraft.world.item.Tiers.GOLD || 
                   tier == net.minecraft.world.item.Tiers.NETHERITE;
        }

        return false;
    }

    private static void pushEntity(Entity entity, Vec3 delta) {
        double max = ServerConfig.VALUES.maxPushPullForce.get() * 1.5D;
        Vec3 clamped = clamp(delta, max);
        entity.setDeltaMovement(clamp(entity.getDeltaMovement().add(clamped), max));
        entity.hasImpulse = true;
        entity.hurtMarked = true;
    }

    private static Vec3 clamp(Vec3 vec, double max) {
        if (vec.lengthSqr() <= max * max) {
            return vec;
        }
        return vec.normalize().scale(max);
    }

    private static void drawLine(ServerPlayer player, Vec3 start, Vec3 end) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 delta = end.subtract(start);
        int steps = Math.max(3, Math.min(30, (int) (delta.length() * 2.0D)));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(BLUE_LINE, point.x, point.y, point.z, 1, 0D, 0D, 0D, 0D);
        }
    }

    public record ForceTarget(Entity entity, BlockPos blockPos, Vec3 position, double mass, boolean anchored) {
        static ForceTarget entity(Entity entity) {
            double mass = 1.0D;
            if (entity instanceof ItemEntity) mass = 0.2D;
            else if (entity instanceof Projectile) mass = 0.15D;
            else if (entity instanceof ServerPlayer) mass = 1.8D;
            else if (entity.getType().toShortString().contains("iron_golem")) mass = 12.0D;
            else if (entity instanceof LivingEntity) mass = 1.1D;
            
            if (entity instanceof LivingEntity living) {
                for (ItemStack stack : living.getArmorSlots()) {
                    if (!stack.isEmpty()) {
                        mass += 0.15D;
                        if (isMetallicStack(stack)) mass += 0.35D;
                    }
                }
                if (isMetallicStack(living.getMainHandItem())) mass += 0.3D;
                if (isMetallicStack(living.getOffhandItem())) mass += 0.3D;
            }
            Vec3 center = entity.position().add(0D, entity.getBbHeight() * 0.5D, 0D);
            return new ForceTarget(entity, null, center, mass, false);
        }

        static ForceTarget block(BlockPos pos) {
            return new ForceTarget(null, pos, Vec3.atCenterOf(pos), 1000D, true);
        }
    }
}
