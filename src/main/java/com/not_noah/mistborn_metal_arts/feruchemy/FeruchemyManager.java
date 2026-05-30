package com.not_noah.mistborn_metal_arts.feruchemy;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.item.MetalmindItem;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import java.util.UUID;

public final class FeruchemyManager {
    // Unique UUID constants for player attribute modifiers
    private static final UUID IRON_KB_MODIFIER_UUID = UUID.fromString("6d557ba1-7443-4b95-8854-8e3ff7cfdb6e");
    private static final UUID IRON_GRAVITY_MODIFIER_UUID = UUID.fromString("f48483b6-2007-4226-ae8f-8d26786c2a41");
    private static final UUID STEEL_SPEED_MODIFIER_UUID = UUID.fromString("0a3597c5-559d-4780-b2bc-618d4586db70");
    private static final UUID STEEL_ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("3d5483db-ca2e-4b47-ab87-0b1ad0153bb1");
    private static final UUID PEWTER_DAMAGE_MODIFIER_UUID = UUID.fromString("e574c8b8-2a12-4ee4-90a6-80be4063debb");
    private static final UUID GOLD_HEALTH_MODIFIER_UUID = UUID.fromString("52778da5-ccdf-4f4c-8ea5-0e70ab2fdb0b");
    private static final UUID CHROMIUM_LUCK_MODIFIER_UUID = UUID.fromString("490fffa1-a67b-402a-9e19-5d27b9db1a4d");

    private FeruchemyManager() {
    }

    public static void tick(ServerPlayer player, MetalArtsData data) {
        if (!ServerConfig.VALUES.feruchemyEnabled.get()) {
            return;
        }

        java.util.List<Metal> activeMetals = new java.util.ArrayList<>();
        for (Metal metal : Metal.cachedValues()) {
            if (metal.isFeruchemical() && data.feruchemyMode(metal) != 0) {
                activeMetals.add(metal);
            }
        }

        // Active cleanup for non-active metals
        for (Metal m : Metal.cachedValues()) {
            if (m.isFeruchemical()) {
                int mode = activeMetals.contains(m) ? data.feruchemyMode(m) : 0;
                cleanupModifiersAndEffects(player, m, mode);
            }
        }

        if (activeMetals.isEmpty()) {
            return;
        }

        boolean changed = false;
        java.util.Map<Metal, ItemStack> metalminds = findAllMetalminds(player, activeMetals);

        for (Metal metal : activeMetals) {
            int mode = data.feruchemyMode(metal);
            ItemStack metalmind = metalminds.get(metal);
            
            if (metalmind == null || metalmind.isEmpty()) {
                data.stopFeruchemy(metal);
                cleanupModifiersAndEffects(player, metal, 0);
                changed = true;
                continue;
            }

            if (!canUse(player, data, metalmind, metal)) {
                data.stopFeruchemy(metal);
                cleanupModifiersAndEffects(player, metal, 0);
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.metalmind_rejects", metal.displayName()), true);
                changed = true;
                continue;
            }
            changed |= mode < 0 ? store(player, data, metalmind, metal) : tap(player, data, metalmind, metal);
        }

        if (changed && player.tickCount % 5 == 0) {
            MetalArtsNetwork.sync(player, data.serializeReservesNBT());
        }
    }

    public static void cleanupModifiersAndEffects(ServerPlayer player, Metal metal, int mode) {
        switch (metal) {
            case IRON -> {
                AttributeInstance kbInstance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kbInstance != null) {
                    kbInstance.removeModifier(IRON_KB_MODIFIER_UUID);
                }
                AttributeInstance gravityInstance = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                if (gravityInstance != null) {
                    gravityInstance.removeModifier(IRON_GRAVITY_MODIFIER_UUID);
                }
            }
            case STEEL -> {
                AttributeInstance speedInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedInstance != null) {
                    speedInstance.removeModifier(STEEL_SPEED_MODIFIER_UUID);
                }
                AttributeInstance attackSpeedInstance = player.getAttribute(Attributes.ATTACK_SPEED);
                if (attackSpeedInstance != null) {
                    attackSpeedInstance.removeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID);
                }
            }
            case PEWTER -> {
                AttributeInstance damageInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (damageInstance != null) {
                    damageInstance.removeModifier(PEWTER_DAMAGE_MODIFIER_UUID);
                }
            }
            case GOLD -> {
                if (mode >= 0) { // Safely restore max health if not storing
                    AttributeInstance healthInstance = player.getAttribute(Attributes.MAX_HEALTH);
                    if (healthInstance != null && healthInstance.getModifier(GOLD_HEALTH_MODIFIER_UUID) != null) {
                        double healthPct = player.getHealth() / player.getMaxHealth();
                        healthInstance.removeModifier(GOLD_HEALTH_MODIFIER_UUID);
                        player.setHealth((float) (player.getMaxHealth() * healthPct));
                    }
                }
            }
            case CHROMIUM -> {
                AttributeInstance luckInstance = player.getAttribute(Attributes.LUCK);
                if (luckInstance != null) {
                    luckInstance.removeModifier(CHROMIUM_LUCK_MODIFIER_UUID);
                }
            }
            default -> {}
        }
    }

    private static java.util.Map<Metal, ItemStack> findAllMetalminds(ServerPlayer player, java.util.List<Metal> metals) {
        java.util.Map<Metal, ItemStack> result = new java.util.EnumMap<>(Metal.class);
        java.util.Set<Metal> toFind = new java.util.HashSet<>(metals);

        // Check Curios first
        for (Metal metal : metals) {
            ItemStack curio = com.not_noah.mistborn_metal_arts.compat.CuriosCompat.findMetalmind(player, metal);
            if (!curio.isEmpty()) {
                result.put(metal, curio);
                toFind.remove(metal);
            }
        }

        if (toFind.isEmpty()) return result;

        // Check inventory
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof MetalmindItem item) {
                Metal metal = item.metal();
                if (toFind.contains(metal)) {
                    result.putIfAbsent(metal, stack);
                }
            }
        }
        
        if (result.size() < metals.size()) {
            for (ItemStack stack : player.getInventory().offhand) {
                if (stack.getItem() instanceof MetalmindItem item) {
                    Metal metal = item.metal();
                    if (toFind.contains(metal)) {
                        result.putIfAbsent(metal, stack);
                    }
                }
            }
        }

        return result;
    }

    public static float adjustFallDamage(ServerPlayer player, MetalArtsData data, float damageMultiplier) {
        if (data.isStoring(Metal.IRON)) {
            return damageMultiplier * 0.15F; // Light weight reduces fall damage dramatically
        }
        if (data.isTapping(Metal.IRON)) {
            int tapLevel = data.feruchemyMode(Metal.IRON);
            return damageMultiplier * (1.0F + 0.35F * tapLevel); // Heavy weight increases fall damage
        }
        return damageMultiplier;
    }

    public static void handleFallGroundSlam(ServerPlayer player, MetalArtsData data, LivingFallEvent event) {
        if (!data.isTapping(Metal.IRON) || event.getDistance() < 2.5F) {
            return;
        }
        int tapLevel = data.feruchemyMode(Metal.IRON);
        float radius = 1.5F + 0.9F * tapLevel;
        ServerLevel level = player.serverLevel();
        
        // Dynamic explosion/dust particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 8 + tapLevel * 4, radius * 0.5D, 0.15D, radius * 0.5D, 0.05D);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F + 0.1F * tapLevel, 0.6F);
        
        // Shockwave damage & knockback
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
            double dist = player.position().distanceTo(target.position());
            float damage = (float) Math.max(1.0F, (radius - dist) * 2.5F * tapLevel);
            target.hurt(level.damageSources().fall(), damage);
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(1.2D * tapLevel / Math.max(1.0D, dist));
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.25D * tapLevel, knockback.z));
        }
        
        // Break weak blocks or crack stone
        BlockPos center = player.blockPosition().below();
        int bRadius = Math.round(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-bRadius, -1, -bRadius), center.offset(bRadius, 1, bRadius))) {
            if (pos.distSqr(center) <= radius * radius) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)) {
                    level.destroyBlock(pos, true, player);
                } else if (state.is(Blocks.STONE)) {
                    level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
    }

    private static boolean store(ServerPlayer player, MetalArtsData data, ItemStack stack, Metal metal) {
        float charge = MetalmindItem.getCharge(stack);
        float capacity = MetalmindItem.getCapacity(stack) * (1.0F + 0.15F * data.getLerasatiumAlloyBonus(metal));
        if (charge >= capacity) {
            data.stopFeruchemy(metal);
            cleanupModifiersAndEffects(player, metal, 0);
            return true;
        }
        
        float baseRate = ServerConfig.VALUES.feruchemyStoreRate.get().floatValue();
        float rate = baseRate;

        boolean stored = switch (metal) {
            case COPPER -> storeCopper(player, stack, rate);
            case BENDALLOY -> storeBendalloy(player, stack, rate);
            default -> {
                MetalmindItem.setCharge(stack, charge + rate);
                yield true;
            }
        };
        if (stored) {
            data.setMetalmindCharge(metal, MetalmindItem.getCharge(stack));
            applyStorePenalty(player, data, stack, metal);
        }
        return stored;
    }

    private static boolean tap(ServerPlayer player, MetalArtsData data, ItemStack stack, Metal metal) {
        float charge = MetalmindItem.getCharge(stack);
        if (charge <= 0F) {
            data.stopFeruchemy(metal);
            data.setMetalmindCharge(metal, 0F);
            cleanupModifiersAndEffects(player, metal, 0);
            return true;
        }

        float baseRate = ServerConfig.VALUES.feruchemyTapRate.get().floatValue();
        int tapLevel = data.feruchemyMode(metal);
        
        // Exponential NBT drain scaling based on tap multiplier
        float drainMultiplier = (float) (tapLevel * Math.pow(1.5D, tapLevel - 1));
        float rate = baseRate * drainMultiplier;

        float NbtRate = rate / (1.0F + 0.15F * data.getLerasatiumAlloyBonus(metal));
        if (charge < NbtRate) {
            NbtRate = charge;
        }

        boolean tapped = switch (metal) {
            case COPPER -> tapCopper(player, stack, NbtRate);
            case BENDALLOY -> tapBendalloy(player, stack, NbtRate);
            default -> {
                MetalmindItem.setCharge(stack, charge - NbtRate);
                yield true;
            }
        };
        if (tapped) {
            data.setMetalmindCharge(metal, MetalmindItem.getCharge(stack));
            applyTapBenefit(player, data, stack, metal, tapLevel);
        }
        return tapped;
    }

    // Precise Lossless Minecraft XP Points Math
    public static int getPlayerXP(Player player) {
        return (int) (getXPForLevel(player.experienceLevel) + (player.experienceProgress * getXPForNextLevel(player.experienceLevel)));
    }

    public static int getXPForLevel(int level) {
        if (level <= 15) {
            return level * level + 6 * level;
        } else if (level <= 30) {
            return (int) (2.5D * level * level - 40.5D * level + 360D);
        } else {
            return (int) (4.5D * level * level - 162.5D * level + 2220D);
        }
    }

    public static int getXPForNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    public static void setPlayerXP(Player player, int totalXP) {
        player.totalExperience = Math.max(0, totalXP);
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        int xp = totalXP;
        while (xp >= getXPForNextLevel(player.experienceLevel)) {
            xp -= getXPForNextLevel(player.experienceLevel);
            player.experienceLevel++;
        }
        player.experienceProgress = (float) xp / (float) getXPForNextLevel(player.experienceLevel);
    }

    private static boolean storeCopper(ServerPlayer player, ItemStack stack, float rate) {
        int currentXP = getPlayerXP(player);
        if (currentXP <= 0) {
            return false;
        }
        int amountToStore = Math.min(currentXP, Math.round(rate * 12));
        if (amountToStore <= 0) amountToStore = 1;
        
        setPlayerXP(player, currentXP - amountToStore);
        MetalmindItem.setCharge(stack, MetalmindItem.getCharge(stack) + amountToStore);
        return true;
    }

    private static boolean tapCopper(ServerPlayer player, ItemStack stack, float rate) {
        float currentCharge = MetalmindItem.getCharge(stack);
        if (currentCharge <= 0F) {
            return false;
        }
        int amountToTap = Math.min(Math.round(rate * 12), Math.round(currentCharge));
        if (amountToTap <= 0) amountToTap = 1;
        
        setPlayerXP(player, getPlayerXP(player) + amountToTap);
        MetalmindItem.setCharge(stack, currentCharge - amountToTap);
        return true;
    }

    private static boolean storeBendalloy(ServerPlayer player, ItemStack stack, float rate) {
        if (player.getFoodData().getFoodLevel() <= 1 && player.getFoodData().getSaturationLevel() <= 0F) {
            return false;
        }
        if (player.tickCount % 20 == 0 && player.getFoodData().getFoodLevel() > 1) {
            player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
        }
        player.causeFoodExhaustion(0.2F);
        MetalmindItem.setCharge(stack, MetalmindItem.getCharge(stack) + rate);
        return true;
    }

    private static boolean tapBendalloy(ServerPlayer player, ItemStack stack, float rate) {
        if (player.tickCount % 10 != 0) {
            return false;
        }
        if (player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(Math.min(20, player.getFoodData().getFoodLevel() + 1));
            MetalmindItem.setCharge(stack, MetalmindItem.getCharge(stack) - Math.max(1F, rate));
            return true;
        }
        return false;
    }

    private static void applyStorePenalty(ServerPlayer player, MetalArtsData data, ItemStack stack, Metal metal) {
        switch (metal) {
            case IRON -> {
                AttributeInstance kbInstance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kbInstance != null && kbInstance.getModifier(IRON_KB_MODIFIER_UUID) == null) {
                    kbInstance.addTransientModifier(new AttributeModifier(IRON_KB_MODIFIER_UUID, "Iron Feruchemy Weight Storing", -0.6D, AttributeModifier.Operation.ADDITION));
                }
                AttributeInstance gravityInstance = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                if (gravityInstance != null && gravityInstance.getModifier(IRON_GRAVITY_MODIFIER_UUID) == null) {
                    gravityInstance.addTransientModifier(new AttributeModifier(IRON_GRAVITY_MODIFIER_UUID, "Iron Feruchemy Gravity Storing", -0.65D, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                if (player.isInWater()) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, 0.04D, 0));
                }
            }
            case STEEL -> {
                AttributeInstance speedInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedInstance != null && speedInstance.getModifier(STEEL_SPEED_MODIFIER_UUID) == null) {
                    speedInstance.addTransientModifier(new AttributeModifier(STEEL_SPEED_MODIFIER_UUID, "Steel Feruchemy Speed Storing", -0.3D, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                AttributeInstance attackSpeedInstance = player.getAttribute(Attributes.ATTACK_SPEED);
                if (attackSpeedInstance != null && attackSpeedInstance.getModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID) == null) {
                    attackSpeedInstance.addTransientModifier(new AttributeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID, "Steel Feruchemy Attack Speed Storing", -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
            case TIN -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }
            case PEWTER -> {
                AttributeInstance damageInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (damageInstance != null && damageInstance.getModifier(PEWTER_DAMAGE_MODIFIER_UUID) == null) {
                    damageInstance.addTransientModifier(new AttributeModifier(PEWTER_DAMAGE_MODIFIER_UUID, "Pewter Feruchemy Strength Storing", -3.0D, AttributeModifier.Operation.ADDITION));
                }
            }
            case GOLD -> {
                AttributeInstance healthInstance = player.getAttribute(Attributes.MAX_HEALTH);
                if (healthInstance != null && healthInstance.getModifier(GOLD_HEALTH_MODIFIER_UUID) == null) {
                    double healthPct = player.getHealth() / player.getMaxHealth();
                    healthInstance.addTransientModifier(new AttributeModifier(GOLD_HEALTH_MODIFIER_UUID, "Gold Feruchemy Health Storing", -14.0D, AttributeModifier.Operation.ADDITION));
                    player.setHealth((float) Math.min(player.getMaxHealth(), player.getMaxHealth() * healthPct));
                }
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0, false, false));
            }
            case BRASS -> {
                player.setTicksFrozen(Math.min(player.getTicksFrozen() + 8, 140));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
            }
            case ZINC -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            }
            case BRONZE -> {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
                if (player.tickCount % 60 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false));
                }
            }
            case ELECTRUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 40, 1, false, false));
            }
            case CHROMIUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 40, 2, false, false));
            }
            case NICROSIL -> {
                if (!data.burningMetals().isEmpty()) {
                    data.stopAllBurning();
                    player.displayClientMessage(Component.literal("Allomancy locked out while storing Investiture!").withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            }
            case TRELLIUM -> {
                // Storing spiritual presence — become stealthy but slow
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
            }
            case RAYSIUM -> {
                // Storing energy — drain other active metal reserves to charge metalmind
                if (player.tickCount % 20 == 0) {
                    data.burningMetals().stream()
                        .filter(m -> m != Metal.RAYSIUM)
                        .forEach(m -> data.consumeReserve(m, 5.0F));
                }
            }
            case TANAVASTIUM -> {
                // No storing penalty for Tanavastium — pure spiritual integrity
            }
            default -> {}
        }
    }

    private static void applyTapBenefit(ServerPlayer player, MetalArtsData data, ItemStack stack, Metal metal, int tapLevel) {
        switch (metal) {
            case IRON -> {
                AttributeInstance kbInstance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kbInstance != null) {
                    kbInstance.removeModifier(IRON_KB_MODIFIER_UUID);
                    kbInstance.addTransientModifier(new AttributeModifier(IRON_KB_MODIFIER_UUID, "Iron Feruchemy Weight Tapping", 0.25D * tapLevel, AttributeModifier.Operation.ADDITION));
                }
                AttributeInstance gravityInstance = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                if (gravityInstance != null) {
                    gravityInstance.removeModifier(IRON_GRAVITY_MODIFIER_UUID);
                    gravityInstance.addTransientModifier(new AttributeModifier(IRON_GRAVITY_MODIFIER_UUID, "Iron Feruchemy Gravity Tapping", 0.45D * tapLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                if (player.isInWater()) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, -0.15D * tapLevel, 0));
                }
            }
            case STEEL -> {
                AttributeInstance speedInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedInstance != null) {
                    speedInstance.removeModifier(STEEL_SPEED_MODIFIER_UUID);
                    speedInstance.addTransientModifier(new AttributeModifier(STEEL_SPEED_MODIFIER_UUID, "Steel Feruchemy Speed Tapping", 0.12D * tapLevel * tapLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                AttributeInstance attackSpeedInstance = player.getAttribute(Attributes.ATTACK_SPEED);
                if (attackSpeedInstance != null) {
                    attackSpeedInstance.removeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID);
                    attackSpeedInstance.addTransientModifier(new AttributeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID, "Steel Feruchemy Attack Speed Tapping", 0.35D * tapLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, tapLevel - 1, false, false));
            }
            case TIN -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
                if (player.tickCount % 10 == 0) {
                    double soundRadius = 8.0D + 4.0D * tapLevel;
                    AABB area = player.getBoundingBox().inflate(soundRadius);
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                        if (target.getDeltaMovement().lengthSqr() > 0.001D) {
                            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                        }
                    }
                }
            }
            case PEWTER -> {
                AttributeInstance damageInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (damageInstance != null) {
                    damageInstance.removeModifier(PEWTER_DAMAGE_MODIFIER_UUID);
                    damageInstance.addTransientModifier(new AttributeModifier(PEWTER_DAMAGE_MODIFIER_UUID, "Pewter Feruchemy Strength Tapping", 2.0D * tapLevel, AttributeModifier.Operation.ADDITION));
                }
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, tapLevel - 1, false, false));
            }
            case GOLD -> {
                float healAmount = 0.5F * tapLevel;
                float bloatVal = data.spiritualBloat();
                if (bloatVal > 50.0F) {
                    healAmount *= 0.7F; // 30% reduction in healing efficiency
                }
                player.heal(healAmount);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, bloatVal > 50.0F ? Math.max(0, (int)(tapLevel * 0.7F)) : tapLevel, false, false));
                if (player.tickCount % 5 == 0) {
                    player.removeEffect(MobEffects.POISON);
                    player.removeEffect(MobEffects.WITHER);
                }
            }
            case BRASS -> {
                player.setTicksFrozen(0);
                AABB area = player.getBoundingBox().inflate(2.5D + 0.5D * tapLevel);
                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                    if (!(target instanceof Player p && !p.canHarmPlayer(player))) {
                        target.setSecondsOnFire(2 * tapLevel);
                    }
                }
                if (player.tickCount % 10 == 0) {
                    BlockPos feet = player.blockPosition();
                    for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 1, 2))) {
                        BlockState state = player.level().getBlockState(pos);
                        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                            player.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        } else if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)) {
                            player.level().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                        }
                    }
                }
            }
            case ZINC -> {
                double zincRadius = 6.0D + 2.0D * tapLevel;
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(zincRadius))) {
                    if (entity instanceof Projectile) {
                        Vec3 motion = entity.getDeltaMovement();
                        entity.setDeltaMovement(motion.scale(1.0D - (0.08D * tapLevel)));
                    } else if (entity instanceof Monster monster) {
                        monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, true));
                    }
                }
                if (player.tickCount % 2 == 0) {
                    BlockPos origin = player.blockPosition();
                    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                            for (int i = 0; i < tapLevel * 2; i++) {
                                net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(
                                    (ServerLevel) player.level(), pos, player.level().getBlockState(pos), furnace
                                );
                            }
                        } else if (be instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity brewing) {
                            for (int i = 0; i < tapLevel * 2; i++) {
                                net.minecraft.world.level.block.entity.BrewingStandBlockEntity.serverTick(
                                    player.level(), pos, player.level().getBlockState(pos), brewing
                                );
                            }
                        } else if (be instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire) {
                            for (int i = 0; i < tapLevel * 2; i++) {
                                net.minecraft.world.level.block.entity.CampfireBlockEntity.cookTick(
                                    player.level(), pos, player.level().getBlockState(pos), campfire
                                );
                            }
                        }
                    }
                }
            }
            case BRONZE -> {
                AABB area = player.getBoundingBox().inflate(32.0D);
                for (Phantom phantom : player.level().getEntitiesOfClass(Phantom.class, area)) {
                    if (phantom.getTarget() == player) {
                        phantom.setTarget(null);
                    }
                }
                player.getStats().setValue(player, net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TIME_SINCE_REST), 0);
                
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                player.removeEffect(MobEffects.WEAKNESS);
            }
            case ELECTRUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, tapLevel - 1, false, false));
                if (player.tickCount % 20 == 0 && player.getAbsorptionAmount() < 4.0F * tapLevel) {
                    player.setAbsorptionAmount(Math.min(4.0F * tapLevel, player.getAbsorptionAmount() + 2.0F));
                }
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                player.removeEffect(MobEffects.BAD_OMEN);
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.DARKNESS);
                player.removeEffect(MobEffects.UNLUCK);
            }
            case CHROMIUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 40, tapLevel, false, false));
            }
            case NICROSIL -> {
                if (player.tickCount % 5 == 0) {
                    for (Metal activeBurn : data.burningMetals()) {
                        data.fillReserve(activeBurn, 0.05F * tapLevel);
                    }
                }
            }
            case TRELLIUM -> {
                // Tapping spiritual presence — glow all entities through walls
                if (player.tickCount % 20 == 0) {
                    double radius = 16.0D + 4.0D * tapLevel;
                    AABB area = player.getBoundingBox().inflate(radius);
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
                    }
                }
            }
            case RAYSIUM -> {
                // Tapping siphoning energy — deal magic damage and heal
                if (player.tickCount % 20 == 0) {
                    double radius = 4.0D + 2.0D * tapLevel;
                    AABB area = player.getBoundingBox().inflate(radius);
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                        if (target instanceof Monster || (target instanceof Player p && p != player)) {
                            float damage = 1.5F * tapLevel;
                            target.hurt(player.damageSources().magic(), damage);
                            player.heal(damage * 0.5F);
                        }
                    }
                }
            }
            case TANAVASTIUM -> {
                // Tapping spiritual integrity — massive Soul Stability bonus
                // Actual stability bonus is handled in SoulStabilityManager
                // Here we remove negative effects
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.WEAKNESS);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, tapLevel - 1, false, false));
            }
            default -> {}
        }
    }

    private static boolean canUse(ServerPlayer player, MetalArtsData data, ItemStack stack, Metal metal) {
        return (data.hasFeruchemicalPower(metal) || (MetalmindItem.isUnkeyed(stack) && ServerConfig.VALUES.unkeyedMetalmindsEnabled.get())) && MetalmindItem.canUse(stack, player);
    }
}
