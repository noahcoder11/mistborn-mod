package com.not_noah.mistborn_metal_arts.allomancy;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.network.MetalAction;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.registry.ModBlocks;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.not_noah.mistborn_metal_arts.allomancy.MetalForceHelper.ForceTarget;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.joml.Vector3f;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.UUID;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public final class AllomancyManager {
    public static final UUID PEWTER_SPEED_MODIFIER_UUID = UUID.fromString("d8ffe4f0-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_ATTACK_MODIFIER_UUID = UUID.fromString("d8ffe4f1-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_HEALTH_MODIFIER_UUID = UUID.fromString("d8ffe4f2-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_KNOCKBACK_MODIFIER_UUID = UUID.fromString("d8ffe4f3-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_DRAG_SPEED_MODIFIER_UUID = UUID.fromString("d8ffe4f4-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_DRAG_ATTACK_MODIFIER_UUID = UUID.fromString("d8ffe4f5-22a6-4c5c-9429-0d039b95bd90");

    private static final DustParticleOptions COPPER_DUST = new DustParticleOptions(new Vector3f(0.65F, 0.32F, 0.12F), 0.8F);
    private static final DustParticleOptions ATIUM_DUST = new DustParticleOptions(new Vector3f(0.65F, 1.0F, 0.85F), 0.9F);

    private AllomancyManager() {
    }

    public static void handleAction(ServerPlayer player, MetalAction action, Metal metal) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            switch (action) {
                case SELECT -> data.setSelectedMetal(metal);
                case START_BURN -> startBurn(player, data, metal);
                case STOP_BURN -> data.stopBurning(metal);
                case TOGGLE_FLARE -> data.setFlaring(metal, !data.isFlaring(metal));
                case CYCLE -> data.setSelectedMetal(nextMetal(data, data.selectedMetal()));
                case PUSH_PULL -> handlePushPull(player, data, metal);
                case PUSH -> handlePush(player, data);
                case PULL -> handlePull(player, data);
                case PURGE -> purge(player, data, true);
                case TIME_BUBBLE -> activateTimeBubbleStub(player, data, metal);
                case TOGGLE_FERUCHEMY -> toggleFeruchemy(player, data, metal);
                case STOP_ALL -> data.stopAllBurning();
            }
            MetalArtsNetwork.sync(player);
        });
    }

    public static void tick(ServerPlayer player, MetalArtsData data) {
        data.tickCooldowns();
        boolean changed = false;

        // 1. Pewter burn duration and drag tracking
        boolean isBurningPewter = data.isBurning(Metal.PEWTER) && data.getReserve(Metal.PEWTER) > 0F;
        if (isBurningPewter) {
            data.setPewterBurnDuration(data.pewterBurnDuration() + 1);
        } else {
            if (data.pewterBurnDuration() > 0) {
                // Trigger Pewter Drag on turn-off if burned continuously for 2+ minutes (2400 ticks)
                if (data.pewterBurnDuration() > 2400) {
                    data.setPewterDragTicks(data.pewterBurnDuration() * 2);
                    player.addEffect(new MobEffectInstance(ModEffects.PEWTER_DRAG.get(), data.pewterBurnDuration() * 2, 0, false, true));
                }
                data.setPewterBurnDuration(0);
                changed = true;
            }
        }

        if (data.pewterDragTicks() > 0) {
            data.setPewterDragTicks(data.pewterDragTicks() - 1);
            player.getFoodData().setFoodLevel(0); // drain hunger
            if (player.tickCount % 20 == 0) {
                if (!player.isSleeping()) {
                    player.hurt(player.damageSources().generic(), 1.0F); // 1 half-heart damage
                }
            }
        }

        // Apply/update pewter attribute modifiers
        updatePewterAttributes(player, data);
        for (Metal metal : EnumSet.copyOf(data.burningMetals())) {
            if (!data.hasAllomanticPower(metal) || data.getReserve(metal) <= 0F || !ServerConfig.isMetalEnabled(metal)) {
                data.stopBurning(metal);
                changed = true;
                continue;
            }
            if (metal == Metal.ALUMINUM || metal == Metal.LERASIUM) {
                continue;
            }
            float multiplier = data.isFlaring(metal) ? 2.75F : 1.0F;
            data.consumeReserve(metal, ServerConfig.burnRate(metal) * multiplier);
            applyContinuousEffect(player, data, metal);
            if (data.getReserve(metal) <= 0F) {
                onReserveEmpty(player, data, metal);
                changed = true;
            }
        }
        if (data.isBurning(Metal.BRONZE) && player.tickCount % 10 == 0) {
            updateBronzePulse(player, data);
            changed = true;
        } else if (!data.isBurning(Metal.BRONZE) && data.bronzePulseStrength() > 0F) {
            data.setBronzePulse(0F, 0F, 0F, "");
            changed = true;
        }
        if (player.tickCount % 10 == 0 || changed) {
            MetalArtsNetwork.sync(player, data.serializeReservesNBT());
        }
    }

    public static boolean isProtectedByPewter(Player player) {
        return player.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(Metal.PEWTER) && data.getReserve(Metal.PEWTER) > 0F)
                .orElse(false);
    }

    public static boolean tryPewterSurvival(Player player, float incomingDamage) {
        return player.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> {
            if (!data.isBurning(Metal.PEWTER) || data.getReserve(Metal.PEWTER) < 25F || incomingDamage < player.getHealth()) {
                return false;
            }
            data.consumeReserve(Metal.PEWTER, 25F);
            data.stopBurning(Metal.PEWTER);
            data.setPewterDragTicks(260);
            player.addEffect(new MobEffectInstance(ModEffects.PEWTER_DRAG.get(), 260, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 260, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 260, 1, false, true));
            if (player instanceof ServerPlayer serverPlayer) {
                MetalArtsNetwork.sync(serverPlayer);
            }
            return true;
        }).orElse(false);
    }

    public static boolean isBurning(Player player, Metal metal) {
        return player.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> data.isBurning(metal)).orElse(false);
    }

    private static void startBurn(ServerPlayer player, MetalArtsData data, Metal metal) {
        if (metal == Metal.LERASIUM) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.lerasium_item_only"), true);
            return;
        }
        if (metal == Metal.ALUMINUM) {
            if (data.hasAllomanticPower(Metal.ALUMINUM) && data.getReserve(Metal.ALUMINUM) > 0F) {
                data.consumeReserve(Metal.ALUMINUM, Math.max(1F, data.getReserve(Metal.ALUMINUM)));
                purge(player, data, false);
            }
            return;
        }
        if (metal == Metal.DURALUMIN) {
            performDuraluminBurst(player, data);
            return;
        }
        if (data.startBurning(metal)) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.25F, 1.6F);
        } else {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.cannot_burn", metal.displayName()), true);
        }
    }

    private static void purge(ServerPlayer player, MetalArtsData data, boolean requirePower) {
        if (requirePower && !data.hasAllomanticPower(Metal.ALUMINUM) && data.getReserve(Metal.ALUMINUM) <= 0F) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.no_aluminum"), true);
            return;
        }
        data.clearReserves();
        player.removeEffect(ModEffects.EMOTIONAL_PRESSURE.get());
        player.removeEffect(ModEffects.SENSORY_OVERLOAD.get());
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.55F, 1.4F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.aluminum_purge"), true);
    }

    private static void performDuraluminBurst(ServerPlayer player, MetalArtsData data) {
        if (!data.hasAllomanticPower(Metal.DURALUMIN) || data.getReserve(Metal.DURALUMIN) <= 0F || data.duraluminCooldown() > 0) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.duralumin_not_ready"), true);
            return;
        }
        EnumSet<Metal> active = EnumSet.copyOf(data.burningMetals());
        active.remove(Metal.DURALUMIN);
        active.remove(Metal.ALUMINUM);
        if (active.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.duralumin_no_targets"), true);
            return;
        }
        data.consumeReserve(Metal.DURALUMIN, Math.max(1F, data.getReserve(Metal.DURALUMIN)));
        for (Metal metal : active) {
            float consumed = data.getReserve(metal);
            data.consumeReserve(metal, consumed);
            data.stopBurning(metal);
            switch (metal) {
                case STEEL -> MetalForceHelper.applyRadialForce(player, false, consumed / 30F + 2.5F);
                case IRON -> MetalForceHelper.applyRadialForce(player, true, consumed / 30F + 2.5F);
                case PEWTER -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 3, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2, false, true));
                    player.addEffect(new MobEffectInstance(ModEffects.PEWTER_DRAG.get(), 260, 1, false, true));
                }
                case TIN -> {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 180, 0, false, true));
                    player.addEffect(new MobEffectInstance(ModEffects.SENSORY_OVERLOAD.get(), 160, 1, false, true));
                }
                case COPPER -> player.addEffect(new MobEffectInstance(ModEffects.COPPERCLOUD.get(), 220, 1, false, true));
                case BRONZE -> wideBronzePing(player, data);
                case ATIUM -> player.addEffect(new MobEffectInstance(ModEffects.ATIUM_SIGHT.get(), 80, 2, false, true));
                default -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, true));
            }
        }
        data.setDuraluminCooldown(20 * 12);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    private static void handlePushPull(ServerPlayer player, MetalArtsData data, Metal metal) {
        if (metal == Metal.IRON && data.isBurning(Metal.IRON)) {
            MetalForceHelper.applyTargetedForce(player, true, data.isFlaring(Metal.IRON), false);
        } else if (metal == Metal.STEEL && data.isBurning(Metal.STEEL)) {
            MetalForceHelper.applyTargetedForce(player, false, data.isFlaring(Metal.STEEL), false);
        } else {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.need_push_pull_burn"), true);
        }
    }

    private static void handlePush(ServerPlayer player, MetalArtsData data) {
        if (data.isBurning(Metal.STEEL) && data.getReserve(Metal.STEEL) > 0F) {
            ItemStack hand = player.getMainHandItem();
            if (MetalForceHelper.isMetallicStack(hand)) {
                shootMetalItem(player, data, hand);
            } else {
                MetalForceHelper.applyTargetedForce(player, false, data.isFlaring(Metal.STEEL), false);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.need_steel_burn"), true);
        }
    }

    private static void shootMetalItem(ServerPlayer player, MetalArtsData data, ItemStack stack) {
        float strength = data.getEffectiveStrength();
        float flareFactor = data.isFlaring(Metal.STEEL) ? 1.85F : 1.0F;

        ItemStack shot = stack.copy();
        shot.setCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        net.minecraft.world.entity.projectile.Snowball projectile = new net.minecraft.world.entity.projectile.Snowball(player.level(), player);
        projectile.setItem(shot);
        
        float speed = 2.5F * strength * flareFactor;
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 0.5F);
        
        projectile.getPersistentData().putBoolean("MistbornMetalArtsShot", true);
        projectile.getPersistentData().putFloat("ShotStrength", strength);
        projectile.getPersistentData().putFloat("ShotFlare", flareFactor);
        
        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.9F, 1.6F);
        data.consumeReserve(Metal.STEEL, 15F);
    }

    private static void handlePull(ServerPlayer player, MetalArtsData data) {
        if (data.isBurning(Metal.IRON) && data.getReserve(Metal.IRON) > 0F) {
            MetalForceHelper.applyTargetedForce(player, true, data.isFlaring(Metal.IRON), false);
        } else {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.need_iron_burn"), true);
        }
    }

    private static void toggleFeruchemy(ServerPlayer player, MetalArtsData data, Metal metal) {
        if (!metal.isFeruchemical() || !data.hasFeruchemicalPower(metal)) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.no_feruchemy", metal.displayName()), true);
            return;
        }
        int mode = data.cycleFeruchemyMode(metal);
        Component message = switch (mode) {
            case -1 -> Component.translatable("message.mistborn_metal_arts.feruchemy_store", metal.displayName());
            case 1 -> Component.translatable("message.mistborn_metal_arts.feruchemy_tap", metal.displayName());
            default -> Component.translatable("message.mistborn_metal_arts.feruchemy_off", metal.displayName());
        };
        player.displayClientMessage(message, true);
    }

    private static void applyContinuousEffect(ServerPlayer player, MetalArtsData data, Metal metal) {
        int amplifier = data.isFlaring(metal) ? 1 : 0;
        switch (metal) {
            case PEWTER -> applyPewter(player, data, amplifier);
            case TIN -> applyTin(player, data, amplifier);
            case COPPER -> applyCopper(player, data, amplifier);
            case BRONZE -> player.addEffect(new MobEffectInstance(ModEffects.BRONZE_SEEKING.get(), 40, amplifier, false, true));
            case ATIUM -> applyAtium(player, data, amplifier);
            case CADMIUM -> applySlowBubbleEffects(player);
            case BENDALLOY -> applySpeedBubbleEffects(player, data);
            case ZINC -> applyZinc(player, data, amplifier);
            case BRASS -> applyBrass(player, data, amplifier);
            case GOLD -> player.addEffect(new MobEffectInstance(ModEffects.GOLD_SIGHT.get(), 40, amplifier, false, true));
            case ELECTRUM -> player.addEffect(new MobEffectInstance(ModEffects.ELECTRUM_SIGHT.get(), 40, amplifier, false, true));
            default -> {
            }
        }
    }

    private static boolean isCopperclouded(LivingEntity entity) {
        return entity.hasEffect(ModEffects.COPPERCLOUD.get());
    }

    private static void applyZinc(ServerPlayer player, MetalArtsData data, int amplifier) {
        double radius = 8.0D + (amplifier * 6.0D);
        AABB area = player.getBoundingBox().inflate(radius);

        // A. Look-Targeting focus override
        LivingEntity lookTarget = null;
        Optional<ForceTarget> precision = MetalForceHelper.findPrecisionTarget(player, radius * 1.5D);
        if (precision.isPresent() && precision.get().entity() instanceof LivingEntity livingLook) {
            lookTarget = livingLook;
        }
        final LivingEntity finalTarget = lookTarget;

        // 1. Affect nearby Mobs (Riot their emotions, making them attack each other or the look target)
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, area, entity -> entity.isAlive());
        for (Mob mob : mobs) {
            mob.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_RIOT.get(), 40, amplifier, false, true));

            if (finalTarget != null) {
                if (mob != finalTarget && !isCopperclouded(finalTarget)) {
                    mob.setTarget(finalTarget);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, amplifier + 1, false, false));
                }
            } else {
                // Periodically force them to target another nearby mob
                if (mob.tickCount % 20 == 0 && mob.getRandom().nextFloat() < 0.45F) {
                    List<LivingEntity> potentialTargets = mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(10D), 
                        target -> target != player && target != mob && target.isAlive() && !isCopperclouded(target));
                    if (!potentialTargets.isEmpty()) {
                        LivingEntity target = potentialTargets.get(mob.getRandom().nextInt(potentialTargets.size()));
                        mob.setTarget(target);
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, amplifier, false, false));
                        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, amplifier, false, false));
                    }
                }
            }
        }

        // 2. Affect nearby Players (cause food exhaustion and confusion if flared)
        List<ServerPlayer> players = player.level().getEntitiesOfClass(ServerPlayer.class, area, entity -> entity != player && entity.isAlive() && !isCopperclouded(entity));
        for (ServerPlayer other : players) {
            other.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_RIOT.get(), 40, amplifier, false, true));
            if (other.tickCount % 20 == 0) {
                other.causeFoodExhaustion(amplifier > 0 ? 1.5F : 0.5F);
            }
            if (amplifier > 0) {
                other.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
            }
        }
    }

    private static void applyBrass(ServerPlayer player, MetalArtsData data, int amplifier) {
        double radius = 8.0D + (amplifier * 6.0D);
        AABB area = player.getBoundingBox().inflate(radius);

        // 1. Affect nearby Mobs (Soothe/pacify them, clearing targeting and slowing them)
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, area, entity -> entity.isAlive());
        for (Mob mob : mobs) {
            mob.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_SOOTHE.get(), 40, amplifier, false, true));

            if (mob.getTarget() != null) {
                if (amplifier > 0 || mob.getRandom().nextFloat() < 0.35F) {
                    mob.setTarget(null);
                }
            }
            if (mob instanceof net.minecraft.world.entity.NeutralMob neutralMob) {
                neutralMob.setPersistentAngerTarget(null);
                neutralMob.setRemainingPersistentAngerTime(0);
            }
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, amplifier), false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, Math.max(0, amplifier), false, false));
        }

        // Villager Gossip discount soothe injection
        List<net.minecraft.world.entity.npc.Villager> villagers = player.level().getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, area, entity -> entity.isAlive());
        for (net.minecraft.world.entity.npc.Villager villager : villagers) {
            if (villager.tickCount % 20 == 0) {
                villager.getGossips().add(player.getUUID(), net.minecraft.world.entity.ai.gossip.GossipType.MINOR_POSITIVE, Math.round(5 * data.getEffectiveStrength()));
            }
        }

        // 2. Affect nearby Players (decrease speed, mining speed, and sprinting)
        List<ServerPlayer> players = player.level().getEntitiesOfClass(ServerPlayer.class, area, entity -> entity != player && entity.isAlive() && !isCopperclouded(entity));
        for (ServerPlayer other : players) {
            other.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_SOOTHE.get(), 40, amplifier, false, true));
            other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, amplifier, false, false));
            other.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, amplifier, false, false));
            other.setSprinting(false);
        }
    }

    private static void applyPewter(ServerPlayer player, MetalArtsData data, int amplifier) {
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, amplifier, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, amplifier, false, false));
        if (player.tickCount % 40 == 0) {
            player.causeFoodExhaustion(data.isFlaring(Metal.PEWTER) ? 0.85F : 0.35F);
        }
    }

    public static void updatePewterAttributes(ServerPlayer player, MetalArtsData data) {
        AttributeInstance speedInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance healthInstance = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance kbInstance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);

        boolean burningPewter = data.isBurning(Metal.PEWTER) && data.getReserve(Metal.PEWTER) > 0F;

        // 1. Remove standard pewter modifiers if not burning
        if (!burningPewter) {
            if (speedInstance != null && speedInstance.getModifier(PEWTER_SPEED_MODIFIER_UUID) != null) {
                speedInstance.removeModifier(PEWTER_SPEED_MODIFIER_UUID);
            }
            if (attackInstance != null && attackInstance.getModifier(PEWTER_ATTACK_MODIFIER_UUID) != null) {
                attackInstance.removeModifier(PEWTER_ATTACK_MODIFIER_UUID);
            }
            if (healthInstance != null && healthInstance.getModifier(PEWTER_HEALTH_MODIFIER_UUID) != null) {
                float currentHealthPercent = player.getHealth() / player.getMaxHealth();
                healthInstance.removeModifier(PEWTER_HEALTH_MODIFIER_UUID);
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(currentHealthPercent * player.getMaxHealth());
                }
            }
            if (kbInstance != null && kbInstance.getModifier(PEWTER_KNOCKBACK_MODIFIER_UUID) != null) {
                kbInstance.removeModifier(PEWTER_KNOCKBACK_MODIFIER_UUID);
            }
        } else {
            // Add or update modifiers based on allomantic strength
            float strength = data.getEffectiveStrength();
            float flareMult = data.isFlaring(Metal.PEWTER) ? 1.8F : 1.0F;

            // Speed: Multiply base by (1.0 + 0.25 * strength * flareMult)
            double speedAmt = 0.25D * strength * flareMult;
            if (speedInstance != null) {
                AttributeModifier mod = speedInstance.getModifier(PEWTER_SPEED_MODIFIER_UUID);
                if (mod == null || Math.abs(mod.getAmount() - speedAmt) > 0.001D) {
                    speedInstance.removeModifier(PEWTER_SPEED_MODIFIER_UUID);
                    speedInstance.addTransientModifier(new AttributeModifier(
                        PEWTER_SPEED_MODIFIER_UUID, "Pewter Speed", speedAmt, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // Attack Damage: Add flat (4.0 * strength * flareMult)
            double attackAmt = 4.0D * strength * flareMult;
            if (attackInstance != null) {
                AttributeModifier mod = attackInstance.getModifier(PEWTER_ATTACK_MODIFIER_UUID);
                if (mod == null || Math.abs(mod.getAmount() - attackAmt) > 0.001D) {
                    attackInstance.removeModifier(PEWTER_ATTACK_MODIFIER_UUID);
                    attackInstance.addTransientModifier(new AttributeModifier(
                        PEWTER_ATTACK_MODIFIER_UUID, "Pewter Attack Damage", attackAmt, AttributeModifier.Operation.ADDITION));
                }
            }

            // Max Health: Add flat (8.0 * strength * flareMult)
            double healthAmt = 8.0D * strength * flareMult;
            if (healthInstance != null) {
                AttributeModifier mod = healthInstance.getModifier(PEWTER_HEALTH_MODIFIER_UUID);
                if (mod == null || Math.abs(mod.getAmount() - healthAmt) > 0.001D) {
                    float currentHealthPercent = player.getHealth() / player.getMaxHealth();
                    healthInstance.removeModifier(PEWTER_HEALTH_MODIFIER_UUID);
                    healthInstance.addTransientModifier(new AttributeModifier(
                        PEWTER_HEALTH_MODIFIER_UUID, "Pewter Max Health", healthAmt, AttributeModifier.Operation.ADDITION));
                    player.setHealth(currentHealthPercent * player.getMaxHealth());
                }
            }

            // Knockback Resistance: Add flat (0.5 * strength * flareMult)
            double kbAmt = 0.5D * strength * flareMult;
            if (kbInstance != null) {
                AttributeModifier mod = kbInstance.getModifier(PEWTER_KNOCKBACK_MODIFIER_UUID);
                if (mod == null || Math.abs(mod.getAmount() - kbAmt) > 0.001D) {
                    kbInstance.removeModifier(PEWTER_KNOCKBACK_MODIFIER_UUID);
                    kbInstance.addTransientModifier(new AttributeModifier(
                        PEWTER_KNOCKBACK_MODIFIER_UUID, "Pewter Knockback Resistance", kbAmt, AttributeModifier.Operation.ADDITION));
                }
            }
        }

        // 2. Handle Pewter Drag negative modifiers
        boolean inPewterDrag = data.pewterDragTicks() > 0;
        if (!inPewterDrag) {
            if (speedInstance != null && speedInstance.getModifier(PEWTER_DRAG_SPEED_MODIFIER_UUID) != null) {
                speedInstance.removeModifier(PEWTER_DRAG_SPEED_MODIFIER_UUID);
            }
            if (attackInstance != null && attackInstance.getModifier(PEWTER_DRAG_ATTACK_MODIFIER_UUID) != null) {
                attackInstance.removeModifier(PEWTER_DRAG_ATTACK_MODIFIER_UUID);
            }
        } else {
            // Speed: Multiply base by (1.0 - 0.90) = 90% reduction
            if (speedInstance != null && speedInstance.getModifier(PEWTER_DRAG_SPEED_MODIFIER_UUID) == null) {
                speedInstance.addTransientModifier(new AttributeModifier(
                    PEWTER_DRAG_SPEED_MODIFIER_UUID, "Pewter Drag Slowness", -0.90D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            // Attack Damage: Multiply base by (1.0 - 0.75) = 75% reduction
            if (attackInstance != null && attackInstance.getModifier(PEWTER_DRAG_ATTACK_MODIFIER_UUID) == null) {
                attackInstance.addTransientModifier(new AttributeModifier(
                    PEWTER_DRAG_ATTACK_MODIFIER_UUID, "Pewter Drag Weakness", -0.75D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    private static void applyTin(ServerPlayer player, MetalArtsData data, int amplifier) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
        
        // Tin Sound Waves Visualizer: Spawn beautiful sculk-vibration sound particles traveling to the player's ears!
        if (player.level() instanceof ServerLevel serverLevel && player.tickCount % 4 == 0) {
            double range = data.isFlaring(Metal.TIN) ? 32.0D : 16.0D;
            List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), 
                entity -> entity != player && entity.isAlive() && !isCopperclouded(entity));
            
            for (LivingEntity entity : nearby) {
                // Determine if entity is moving, swinging arms (attacking), or hurt (making noise)
                double speedSqr = entity.getDeltaMovement().horizontalDistanceSqr();
                boolean isMoving = speedSqr > 0.0008D;
                boolean isAttacking = entity.swinging;
                boolean isHurt = entity.hurtTime > 0;
                boolean isAmbient = entity.getRandom().nextFloat() < 0.03F; // occasional ambient sounds
                
                if (isMoving || isAttacking || isHurt || isAmbient) {
                    double ex = entity.getX();
                    double ey = entity.getY() + entity.getBbHeight() * 0.5D;
                    double ez = entity.getZ();
                    
                    // 1. Spawn Sculk sound vibration wave traveling from mob to seeker's head
                    EntityPositionSource source = new EntityPositionSource(player, (float) player.getEyeHeight());
                    VibrationParticleOption vibration = new VibrationParticleOption(source, 15);
                    serverLevel.sendParticles(player, vibration, false, ex, ey, ez, 1, 0D, 0D, 0D, 0D);
                    
                    // 2. Spawn Sculk Charge Pop sound bubble at the source
                    serverLevel.sendParticles(player, ParticleTypes.SCULK_CHARGE_POP, false, ex, ey, ez, isHurt ? 3 : 1, 0.05D, 0.05D, 0.05D, 0.01D);
                }
            }
        }
    }

    private static void applyCopper(ServerPlayer player, MetalArtsData data, int amplifier) {
        data.setCopperclouded(true);
        player.addEffect(new MobEffectInstance(ModEffects.COPPERCLOUD.get(), 50, amplifier, false, true));
        double radius = amplifier > 0 ? 10D : 7D;
        if (player.level() instanceof ServerLevel serverLevel && player.tickCount % 12 == 0) {
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2D / 24D) * i;
                serverLevel.sendParticles(COPPER_DUST, player.getX() + Math.cos(angle) * radius, player.getY() + 1.0D, player.getZ() + Math.sin(angle) * radius, 1, 0D, 0D, 0D, 0D);
            }
        }
    }

    private static void applyAtium(ServerPlayer player, MetalArtsData data, int amplifier) {
        player.addEffect(new MobEffectInstance(ModEffects.ATIUM_SIGHT.get(), 35, amplifier, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 35, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 35, amplifier, false, false));

        if (player.tickCount % 2 == 0) {
            double range = 16D + (amplifier * 8D);
            AABB box = player.getBoundingBox().inflate(range);
            for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));

                if (player.level() instanceof ServerLevel serverLevel) {
                    double x = mob.getX();
                    double y = mob.getY();
                    double z = mob.getZ();

                    // Calculate actual velocity including AI movement
                    double vx = x - mob.xo;
                    double vy = y - mob.yo;
                    double vz = z - mob.zo;

                    // If they are standing still, show shadows in a slight pulse instead
                    if (Math.abs(vx) < 0.01 && Math.abs(vz) < 0.01) {
                        double time = (player.tickCount + mob.getId()) * 0.2;
                        vx = Math.sin(time) * 0.05;
                        vz = Math.cos(time) * 0.05;
                    }

                    // Extrapolate forward. Level 1 (Burning) = ~0.6s, Level 2 (Flaring) = ~1.0s
                    float predictionTicks = 12.0F + (amplifier * 8.0F);

                    // Draw 3 shadow points along the predicted path
                    for (int i = 1; i <= 3; i++) {
                        float scale = (float) i / 3.0F;
                        double px = x + (vx * predictionTicks * scale);
                        double py = y + (vy * predictionTicks * scale);
                        double pz = z + (vz * predictionTicks * scale);

                        serverLevel.sendParticles(ModParticles.ATIUM_SHADOW.get(), px, py + mob.getBbHeight() * 0.5, pz, 1, 0.1D, 0.2D, 0.1D, 0D);
                    }

                    // Current position dust
                    serverLevel.sendParticles(ATIUM_DUST, x, y + mob.getBbHeight() + 0.1D, z, 1, 0.1D, 0.1D, 0.1D, 0D);
                }
            }
        }
    }

    private static void applySlowBubbleEffects(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(ServerConfig.VALUES.timeBubbleRadius.get());
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive())) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 0, false, true));
        }
    }

    private static void applySpeedBubbleEffects(ServerPlayer player, MetalArtsData data) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, data.isFlaring(Metal.BENDALLOY) ? 2 : 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30, data.isFlaring(Metal.BENDALLOY) ? 2 : 1, false, true));
        if (player.tickCount % 20 == 0) {
            player.causeFoodExhaustion(0.75F);
        }
    }

    private static void onReserveEmpty(ServerPlayer player, MetalArtsData data, Metal metal) {
        data.stopBurning(metal);
        if (metal == Metal.PEWTER) {
            data.setPewterDragTicks(220);
            player.addEffect(new MobEffectInstance(ModEffects.PEWTER_DRAG.get(), 220, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 0, false, true));
        }
    }

    private static void updateBronzePulse(ServerPlayer seeker, MetalArtsData seekerData) {
        double range = seekerData.isFlaring(Metal.BRONZE) ? 42D : 28D;
        Optional<LivingEntity> target = seeker.level().getEntitiesOfClass(LivingEntity.class, seeker.getBoundingBox().inflate(range), entity -> entity != seeker && entity.isAlive())
                .stream()
                .filter(entity -> isDetectable(seeker, entity))
                .min(Comparator.comparingDouble(seeker::distanceToSqr));
        if (target.isEmpty()) {
            if (!updateWellPulse(seeker, seekerData)) {
                seekerData.setBronzePulse(0F, 0F, 0F, "");
            }
            return;
        }
        LivingEntity entity = target.get();
        Vec3 offset = entity.position().subtract(seeker.position());
        float yaw = (float) Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90F;
        float distance = (float) Math.sqrt(seeker.distanceToSqr(entity));
        float strength = (float) Math.max(0.05D, 1D - distance / range);
        
        Optional<Metal> activeMetal = detectableMetal(entity);
        String metalName = activeMetal.map(Metal::displayName).orElse("Allomancy");
        seekerData.setBronzePulse(yaw, strength, distance, metalName);

        // Seeker Melodic Pulses: Play periodic rhythmic heartbeat sound waves representing the target's burning metal type
        net.minecraft.sounds.SoundEvent sound = SoundEvents.NOTE_BLOCK_BASS.get();
        float pitch = 0.55F;
        if (activeMetal.isPresent()) {
            Metal m = activeMetal.get();
            if (m == Metal.IRON || m == Metal.STEEL || m == Metal.TIN || m == Metal.PEWTER) {
                sound = SoundEvents.NOTE_BLOCK_BASS.get();
                pitch = 0.55F;
            } else if (m == Metal.ZINC || m == Metal.BRASS || m == Metal.COPPER || m == Metal.BRONZE) {
                sound = SoundEvents.NOTE_BLOCK_FLUTE.get();
                pitch = 0.95F;
            } else if (m == Metal.GOLD || m == Metal.ELECTRUM || m == Metal.CADMIUM || m == Metal.BENDALLOY) {
                sound = SoundEvents.NOTE_BLOCK_CHIME.get();
                pitch = 1.25F;
            } else {
                sound = SoundEvents.NOTE_BLOCK_DIDGERIDOO.get();
                pitch = 0.65F;
            }
        }
        seeker.level().playSound(null, seeker.getX(), seeker.getY(), seeker.getZ(), sound, SoundSource.PLAYERS, strength * 0.60F, pitch);
    }

    private static boolean isDetectable(ServerPlayer seeker, LivingEntity entity) {
        if (entity.hasEffect(ModEffects.COPPERCLOUD.get())) {
            return false;
        }
        return entity.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> !data.isCopperclouded() && (!data.burningMetals().isEmpty() || data.totalCorruption() > 0))
                .orElse(entity.hasEffect(ModEffects.ATIUM_SIGHT.get()) || entity.hasEffect(ModEffects.BRONZE_SEEKING.get()));
    }

    private static Optional<Metal> detectableMetal(LivingEntity entity) {
        Metal[] detected = new Metal[1];
        entity.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> detected[0] = data.burningMetals().stream().findFirst().orElse(null));
        return Optional.ofNullable(detected[0]);
    }

    private static void wideBronzePing(ServerPlayer player, MetalArtsData data) {
        updateBronzePulse(player, data);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.bronze_ping"), true);
    }

    private static boolean updateWellPulse(ServerPlayer seeker, MetalArtsData seekerData) {
        if (!ServerConfig.VALUES.wellEnabled.get()) {
            return false;
        }
        double range = Math.min(ServerConfig.VALUES.wellBronzePulseRange.get(), seekerData.isFlaring(Metal.BRONZE) ? 128D : 96D);
        BlockPos origin = seeker.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        // Optimization: Use the O(1)/O(N) WellRegistry instead of scanning hundreds of thousands of blocks.
        for (BlockPos pos : com.not_noah.mistborn_metal_arts.worldgen.WellRegistry.getWells(seeker.level())) {
            double distance = pos.distSqr(origin);
            if (distance < bestDistance && distance <= range * range) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }

        if (best == null) {
            return false;
        }
        Vec3 offset = Vec3.atCenterOf(best).subtract(seeker.position());
        float yaw = (float) Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90F;
        float distance = (float) Math.sqrt(bestDistance);
        float strength = (float) Math.max(0.08D, 1D - distance / range);
        seekerData.setBronzePulse(yaw, strength, distance, "Well of Ascension");
        return true;
    }

    private static Metal nextMetal(MetalArtsData data, Metal current) {
        java.util.List<Metal> unlocked = new java.util.ArrayList<>();
        for (Metal m : Metal.cachedValues()) {
            if (data.hasAllomanticPower(m) || data.hasFeruchemicalPower(m)) {
                unlocked.add(m);
            }
        }
        if (unlocked.isEmpty()) {
            return current;
        }
        int currentIndex = unlocked.indexOf(current);
        int nextIndex;
        if (currentIndex == -1) {
            nextIndex = 0;
        } else {
            nextIndex = (currentIndex + 1) % unlocked.size();
        }
        return unlocked.get(nextIndex);
    }

    public static class TimeBubble {
        public final BlockPos center;
        public final double radius;
        public final Metal type;
        public final ServerPlayer owner;
        public int ticksLeft;

        public TimeBubble(BlockPos center, double radius, Metal type, ServerPlayer owner, int ticksLeft) {
            this.center = center;
            this.radius = radius;
            this.type = type;
            this.owner = owner;
            this.ticksLeft = ticksLeft;
        }

        public AABB getArea() {
            return new AABB(center).inflate(radius);
        }
    }

    public static final java.util.List<TimeBubble> ACTIVE_BUBBLES = new java.util.ArrayList<>();

    public static void tickWorldBubbles(ServerLevel level) {
        ACTIVE_BUBBLES.removeIf(bubble -> {
            bubble.ticksLeft--;
            return bubble.ticksLeft <= 0 || !bubble.owner.isAlive() || !bubble.owner.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> data.isBurning(bubble.type)).orElse(false);
        });

        for (TimeBubble bubble : ACTIVE_BUBBLES) {
            if (bubble.owner.level() != level) continue;
            
            AABB area = bubble.getArea();
            List<Entity> entities = level.getEntitiesOfClass(Entity.class, area, entity -> entity.isAlive() && !(entity instanceof Player));
            
            if (bubble.type == Metal.BENDALLOY) {
                for (Entity entity : entities) {
                    for (int i = 0; i < 4; i++) {
                        entity.tick();
                    }
                }
            } else if (bubble.type == Metal.CADMIUM) {
                if (level.getGameTime() % 5 != 0) {
                    for (Entity entity : entities) {
                        entity.setDeltaMovement(Vec3.ZERO);
                    }
                }
            }

            // Spawn spherical boundary particles to show bubble volume visually
            if (level.getGameTime() % 3 == 0) {
                double cx = bubble.center.getX() + 0.5D;
                double cy = bubble.center.getY() + 0.5D;
                double cz = bubble.center.getZ() + 0.5D;
                double r = bubble.radius;
                
                for (int i = 0; i < 20; i++) {
                    double theta = level.random.nextDouble() * Math.PI * 2.0D;
                    double phi = Math.acos(2.0D * level.random.nextDouble() - 1.0D);
                    double x = cx + r * Math.sin(phi) * Math.cos(theta);
                    double y = cy + r * Math.sin(phi) * Math.sin(theta);
                    double z = cz + r * Math.cos(phi);
                    
                    net.minecraft.core.particles.SimpleParticleType pType = bubble.type == Metal.BENDALLOY ? 
                        net.minecraft.core.particles.ParticleTypes.END_ROD : net.minecraft.core.particles.ParticleTypes.PORTAL;
                    
                    level.sendParticles(pType, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private static void activateTimeBubbleStub(ServerPlayer player, MetalArtsData data, Metal metal) {
        if (data.bubbleCooldown() > 0) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.bubble_cooldown"), true);
            return;
        }
        
        double radius = ServerConfig.VALUES.timeBubbleRadius.get();
        ACTIVE_BUBBLES.removeIf(b -> b.owner == player);
        
        int duration = 400; // 20 seconds
        ACTIVE_BUBBLES.add(new TimeBubble(player.blockPosition(), radius, metal, player, duration));
        data.setBubbleCooldown(80);
        
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.3F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts." + metal.id() + "_bubble"), true);
    }
}
