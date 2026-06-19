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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EntityBlock;
import java.util.UUID;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

public final class AllomancyManager {
    public static final UUID PEWTER_SPEED_MODIFIER_UUID = UUID.fromString("d8ffe4f0-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_ATTACK_MODIFIER_UUID = UUID.fromString("d8ffe4f1-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_HEALTH_MODIFIER_UUID = UUID.fromString("d8ffe4f2-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_KNOCKBACK_MODIFIER_UUID = UUID.fromString("d8ffe4f3-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_DRAG_SPEED_MODIFIER_UUID = UUID.fromString("d8ffe4f4-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_DRAG_ATTACK_MODIFIER_UUID = UUID.fromString("d8ffe4f5-22a6-4c5c-9429-0d039b95bd90");
    public static final UUID PEWTER_DRAG_HEALTH_MODIFIER_UUID = UUID.fromString("d8ffe4f6-22a6-4c5c-9429-0d039b95bd90");

    private static final DustParticleOptions COPPER_DUST = new DustParticleOptions(new Vector3f(0.65F, 0.32F, 0.12F),
            0.8F);
    private static final DustParticleOptions ATIUM_DUST = new DustParticleOptions(new Vector3f(0.65F, 1.0F, 0.85F),
            0.9F);

    private AllomancyManager() {
    }

    public static void handleAction(ServerPlayer player, MetalAction action, Metal metal) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            switch (action) {
                case SELECT -> data.setSelectedMetal(metal);
                case START_BURN -> startBurn(player, data, metal);
                case STOP_BURN -> {
                    handleDeactivation(player, data, metal);
                    data.stopBurning(metal);
                }
                case TOGGLE_FLARE -> data.setFlaring(metal, !data.isFlaring(metal));
                case CYCLE -> data.setSelectedMetal(nextMetal(data, data.selectedMetal()));
                case PUSH_PULL -> handlePushPull(player, data, metal);
                case PUSH -> handlePush(player, data);
                case PULL -> handlePull(player, data);
                case PURGE -> purge(player, data, true);
                case TIME_BUBBLE -> activateTimeBubbleStub(player, data, metal);
                case TOGGLE_FERUCHEMY -> toggleFeruchemy(player, data, metal);
                case STOP_ALL -> {
                    for (Metal m : EnumSet.copyOf(data.burningMetals())) {
                        handleDeactivation(player, data, m);
                    }
                    data.stopAllBurning();
                }
            }
            MetalArtsNetwork.sync(player);
        });
    }

    public static void tick(ServerPlayer player, MetalArtsData data) {
        boolean changed = tick((LivingEntity) player, data);
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

    public static boolean tick(LivingEntity entity, MetalArtsData data) {
        data.tickCooldowns();
        boolean changed = false;

        // 1. Pewter burn duration and drag tracking
        boolean isBurningPewter = data.isBurning(Metal.PEWTER) && data.getReserve(Metal.PEWTER) > 0F;
        if (isBurningPewter) {
            data.setPewterBurnDuration(data.pewterBurnDuration() + 1);
            if (data.pewterDragTicks() > 0) {
                data.setPewterDragTicks(0);
                entity.removeEffect(ModEffects.PEWTER_DRAG.get());
                entity.removeEffect(MobEffects.WEAKNESS);
                entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                entity.removeEffect(MobEffects.DIG_SLOWDOWN);
                changed = true;
            }
        }

        if (data.pewterDragTicks() > 0) {
            if (!entity.hasEffect(ModEffects.PEWTER_DRAG.get())) {
                data.setPewterDragTicks(0);
                changed = true;
            } else {
                data.setPewterDragTicks(data.pewterDragTicks() - 1);
                if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                    player.getFoodData().setFoodLevel(0); // drain hunger
                    if (player.tickCount % 20 == 0) {
                        if (!player.isSleeping()) {
                            player.hurt(player.damageSources().generic(), 1.0F); // 1 half-heart damage
                        }
                    }
                }
            }
        }

        // Apply/update pewter attribute modifiers
        updatePewterAttributes(entity, data);
        for (Metal metal : EnumSet.copyOf(data.burningMetals())) {
            if (!data.hasAllomanticPower(metal) || data.getReserve(metal) <= 0F
                    || !ServerConfig.isMetalEnabled(metal)) {
                handleDeactivation(entity, data, metal);
                data.stopBurning(metal);
                changed = true;
                continue;
            }
            if (metal == Metal.ALUMINUM || metal == Metal.LERASIUM) {
                continue;
            }
            float multiplier = data.isFlaring(metal) ? 2.75F : 1.0F;
            if (data.hasFeruchemicalPower(metal) && data.feruchemyMode(metal) < 0) {
                multiplier *= 2.0F;
            }
            data.consumeReserve(metal, ServerConfig.burnRate(metal) * multiplier);
            applyContinuousEffect(entity, data, metal);
            if (data.getReserve(metal) <= 0F) {
                handleDeactivation(entity, data, metal);
                onReserveEmpty(entity, data, metal);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean isProtectedByPewter(Player player) {
        return player.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(Metal.PEWTER) && data.getReserve(Metal.PEWTER) > 0F)
                .orElse(false);
    }

    public static boolean tryPewterSurvival(Player player, float incomingDamage) {
        return player.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> {
            if (!data.isBurning(Metal.PEWTER) || data.getReserve(Metal.PEWTER) < 25F
                    || incomingDamage < player.getHealth()) {
                return false;
            }
            data.consumeReserve(Metal.PEWTER, 25F);
            data.stopBurning(Metal.PEWTER);
            data.setPewterDragTicks(260);
            data.setPewterBurnDuration(0);
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
            player.level().playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS,
                    0.25F, 1.6F);
            if (metal == Metal.BENDALLOY || metal == Metal.CADMIUM) {
                activateTimeBubbleStub(player, data, metal);
            }
        } else {
            player.displayClientMessage(
                    Component.translatable("message.mistborn_metal_arts.cannot_burn", metal.displayName()), true);
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
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.55F,
                1.4F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.aluminum_purge"), true);
    }

    private static void performDuraluminBurst(ServerPlayer player, MetalArtsData data) {
        if (!data.hasAllomanticPower(Metal.DURALUMIN) || data.getReserve(Metal.DURALUMIN) <= 0F
                || data.duraluminCooldown() > 0) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.duralumin_not_ready"),
                    true);
            return;
        }
        EnumSet<Metal> active = EnumSet.copyOf(data.burningMetals());
        active.remove(Metal.DURALUMIN);
        active.remove(Metal.ALUMINUM);
        if (active.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.duralumin_no_targets"),
                    true);
            return;
        }
        data.consumeReserve(Metal.DURALUMIN, Math.max(1F, data.getReserve(Metal.DURALUMIN)));
        for (Metal metal : active) {
            float consumed = data.getReserve(metal);
            data.consumeReserve(metal, consumed);
            handleDeactivation(player, data, metal);
            data.stopBurning(metal);
            switch (metal) {
                case STEEL -> MetalForceHelper.applyRadialForce(player, false, consumed / 30F + 2.5F);
                case IRON -> MetalForceHelper.applyRadialForce(player, true, consumed / 30F + 2.5F);
                case PEWTER -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 3, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2, false, true));
                    player.addEffect(new MobEffectInstance(ModEffects.PEWTER_DRAG.get(), 260, 1, false, true));
                    data.setPewterDragTicks(260);
                    data.setPewterBurnDuration(0);
                }
                case TIN -> {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 180, 0, false, true));
                    player.addEffect(new MobEffectInstance(ModEffects.SENSORY_OVERLOAD.get(), 160, 1, false, true));
                }
                case COPPER ->
                    player.addEffect(new MobEffectInstance(ModEffects.COPPERCLOUD.get(), 220, 1, false, true));
                case BRONZE -> wideBronzePing(player, data);
                case ATIUM -> player.addEffect(new MobEffectInstance(ModEffects.ATIUM_SIGHT.get(), 80, 2, false, true));
                case TRELLIUM -> {
                    // Absolute spiritual invisibility for 15 seconds
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 300, 1, false, true));
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.trellium_burst"),
                            true);
                }
                case RAYSIUM -> {
                    // Siphon 30% max health from all entities in 12-block radius
                    double radius = 12.0D;
                    AABB area = player.getBoundingBox().inflate(radius);
                    float totalHealed = 0F;
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area,
                            e -> e != player && e.isAlive())) {
                        float siphon = target.getMaxHealth() * 0.3F;
                        target.hurt(player.damageSources().magic(), siphon);
                        totalHealed += siphon * 0.5F;
                    }
                    player.heal(Math.min(totalHealed, player.getMaxHealth()));
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.raysium_burst"),
                            true);
                }
                case TANAVASTIUM -> {
                    // Instantly repair Soul Stability to 100% (capped by spiritual scarring) and
                    // grant temporary protection
                    player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(d -> {
                        d.setSoulStability(
                                ServerConfig.VALUES.soulStabilityBaseMax.get().floatValue() - d.spiritualScarring());
                    });
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 3, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 4, false, true));
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.tanavastium_burst"),
                            true);
                }
                default -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, true));
            }
        }
        data.setDuraluminCooldown(20 * 12);
        player.level().playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
                0.7F, 1.6F);
    }

    private static void handlePushPull(ServerPlayer player, MetalArtsData data, Metal metal) {
        if (metal == Metal.IRON && data.isBurning(Metal.IRON)) {
            MetalForceHelper.applyTargetedForce(player, true, data.isFlaring(Metal.IRON), false);
        } else if (metal == Metal.STEEL && data.isBurning(Metal.STEEL)) {
            MetalForceHelper.applyTargetedForce(player, false, data.isFlaring(Metal.STEEL), false);
        } else {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.need_push_pull_burn"),
                    true);
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
        float strength = data.getEffectiveStrength(Metal.STEEL);
        float flareFactor = data.isFlaring(Metal.STEEL) ? 1.85F : 1.0F;

        ItemStack shot = stack.copy();
        shot.setCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        net.minecraft.world.entity.projectile.Snowball projectile = new net.minecraft.world.entity.projectile.Snowball(
                player.level(), player);
        projectile.setItem(shot);

        float speed = 2.5F * strength * flareFactor;
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 0.5F);

        projectile.getPersistentData().putBoolean("MistbornMetalArtsShot", true);
        projectile.getPersistentData().putFloat("ShotStrength", strength);
        projectile.getPersistentData().putFloat("ShotFlare", flareFactor);

        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.9F,
                1.6F);
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
            player.displayClientMessage(
                    Component.translatable("message.mistborn_metal_arts.no_feruchemy", metal.displayName()), true);
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

    private static void applyContinuousEffect(LivingEntity entity, MetalArtsData data, Metal metal) {
        int amplifier = data.isFlaring(metal) ? 1 : 0;
        switch (metal) {
            case PEWTER -> applyPewter(entity, data, amplifier);
            case TIN -> applyTin(entity, data, amplifier);
            case COPPER -> applyCopper(entity, data, amplifier);
            case BRONZE ->
                entity.addEffect(new MobEffectInstance(ModEffects.BRONZE_SEEKING.get(), 40, amplifier, false, true));
            case ATIUM -> applyAtium(entity, data, amplifier);
            case CADMIUM -> {
            }
            case BENDALLOY -> {
            }
            case ZINC -> applyZinc(entity, data, amplifier);
            case BRASS -> applyBrass(entity, data, amplifier);
            case GOLD ->
                entity.addEffect(new MobEffectInstance(ModEffects.GOLD_SIGHT.get(), 40, amplifier, false, true));
            case ELECTRUM ->
                entity.addEffect(new MobEffectInstance(ModEffects.ELECTRUM_SIGHT.get(), 40, amplifier, false, true));
            case TRELLIUM -> {
                // Spiritual stealth — apply invisibility effect while burning
                entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
                // Mark as copperclouded to hide from bronze seekers
                data.setCopperclouded(true);
            }
            case RAYSIUM -> {
                // Passive siphoning aura — heal slightly and damage nearby hostiles
                if (entity.tickCount % 40 == 0) {
                    double radius = 6.0D;
                    AABB area = entity.getBoundingBox().inflate(radius);
                    for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class, area,
                            e -> e != entity && e.isAlive())) {
                        if (target instanceof net.minecraft.world.entity.monster.Monster) {
                            target.hurt(entity.damageSources().magic(), 1.0F + amplifier);
                            entity.heal(0.5F + amplifier * 0.5F);
                        }
                    }
                }
            }
            case TANAVASTIUM -> {
                // Soul Stability active burn bonus — recalculate stability with bonus
                // The actual bonus is handled in SoulStabilityManager.recalculateStability()
                // Here we just apply a visual/protective effect and slowly heal spiritual
                // scarring
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, amplifier, false, false));
                if (data.spiritualScarring() > 0.0F) {
                    data.setSpiritualScarring(data.spiritualScarring() - 0.005F);
                }
            }
            default -> {
            }
        }
    }

    private static boolean isCopperclouded(LivingEntity entity) {
        return entity.hasEffect(ModEffects.COPPERCLOUD.get());
    }

    private static void applyZinc(LivingEntity entity, MetalArtsData data, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        double radius = 8.0D + (amplifier * 6.0D);
        AABB area = player.getBoundingBox().inflate(radius);

        // A. Look-Targeting focus override
        LivingEntity lookTarget = null;
        Optional<ForceTarget> precision = MetalForceHelper.findPrecisionTarget(player, radius * 1.5D);
        if (precision.isPresent() && precision.get().entity() instanceof LivingEntity livingLook) {
            lookTarget = livingLook;
        }
        final LivingEntity finalTarget = lookTarget;

        // 1. Affect nearby Mobs (Riot their emotions, making them attack each other or
        // the look target)
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive());
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
                    List<LivingEntity> potentialTargets = mob.level().getEntitiesOfClass(LivingEntity.class,
                            mob.getBoundingBox().inflate(10D),
                            target -> target != player && target != mob && target.isAlive()
                                    && !isCopperclouded(target));
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
        List<ServerPlayer> players = player.level().getEntitiesOfClass(ServerPlayer.class, area,
                other -> other != player && other.isAlive() && !isCopperclouded(other));
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

    private static void applyBrass(LivingEntity entity, MetalArtsData data, int amplifier) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        double radius = 8.0D + (amplifier * 6.0D);
        AABB area = player.getBoundingBox().inflate(radius);

        // 1. Affect nearby Mobs (Soothe/pacify them, clearing targeting and slowing
        // them)
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive());
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
            mob.addEffect(
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, amplifier), false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, Math.max(0, amplifier), false, false));
        }

        // Villager Gossip discount soothe injection
        List<net.minecraft.world.entity.npc.Villager> villagers = player.level().getEntitiesOfClass(
                net.minecraft.world.entity.npc.Villager.class, area, villager -> villager.isAlive());
        for (net.minecraft.world.entity.npc.Villager villager : villagers) {
            if (villager.tickCount % 20 == 0) {
                villager.getGossips().add(player.getUUID(),
                        net.minecraft.world.entity.ai.gossip.GossipType.MINOR_POSITIVE,
                        Math.round(5 * data.getEffectiveStrength(Metal.BRASS)));
            }
        }

        // 2. Affect nearby Players (decrease speed, mining speed, and sprinting)
        List<ServerPlayer> players = player.level().getEntitiesOfClass(ServerPlayer.class, area,
                other -> other != player && other.isAlive() && !isCopperclouded(other));
        for (ServerPlayer other : players) {
            other.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_SOOTHE.get(), 40, amplifier, false, true));
            other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, amplifier, false, false));
            other.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, amplifier, false, false));
            other.setSprinting(false);
        }
    }

    private static void applyPewter(LivingEntity entity, MetalArtsData data, int amplifier) {
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, amplifier, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, amplifier, false, false));
        if (entity instanceof Player player) {
            if (player.tickCount % 40 == 0) {
                player.causeFoodExhaustion(data.isFlaring(Metal.PEWTER) ? 0.85F : 0.35F);
            }
        }
    }

    public static void updatePewterAttributes(LivingEntity player, MetalArtsData data) {
        AttributeInstance speedInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance healthInstance = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance kbInstance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);

        boolean burningPewter = data.isBurning(Metal.PEWTER) && data.getReserve(Metal.PEWTER) > 0F;
        int stage = data.savantStage(Metal.PEWTER);

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
                player.setHealth(Math.max(1.0F, currentHealthPercent * player.getMaxHealth()));
            }
            if (kbInstance != null && kbInstance.getModifier(PEWTER_KNOCKBACK_MODIFIER_UUID) != null) {
                kbInstance.removeModifier(PEWTER_KNOCKBACK_MODIFIER_UUID);
            }
        } else {
            // Add or update modifiers based on allomantic strength
            float strength = data.getEffectiveStrength(Metal.PEWTER);
            float flareMult = data.isFlaring(Metal.PEWTER) ? 1.8F : 1.0F;

            // Pewter Savant Stage 2+: passive healing
            if (stage >= 2 && player.getHealth() < player.getMaxHealth()) {
                player.heal(0.25F);
            }

            // Speed: Multiply base by (1.0 + 0.25 * strength * flareMult)
            double speedAmt = 0.25D * strength * flareMult;
            if (stage == 4) {
                speedAmt *= 2.0D; // double speed boost for extreme savant
            }
            if (speedInstance != null) {
                AttributeModifier mod = speedInstance.getModifier(PEWTER_SPEED_MODIFIER_UUID);
                if (mod == null || Math.abs(mod.getAmount() - speedAmt) > 0.001D) {
                    speedInstance.removeModifier(PEWTER_SPEED_MODIFIER_UUID);
                    speedInstance.addTransientModifier(new AttributeModifier(
                            PEWTER_SPEED_MODIFIER_UUID, "Pewter Speed", speedAmt,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }

            // Attack Damage: Add flat (4.0 * strength * flareMult)
            double attackAmt = 4.0D * strength * flareMult;
            if (stage == 4 && player.getMainHandItem().isEmpty()) {
                attackAmt += 7.0D; // diamond sword equivalent unarmed
            }
            if (attackInstance != null) {
                AttributeModifier mod = attackInstance.getModifier(PEWTER_ATTACK_MODIFIER_UUID);
                if (mod == null || Math.abs(mod.getAmount() - attackAmt) > 0.001D) {
                    attackInstance.removeModifier(PEWTER_ATTACK_MODIFIER_UUID);
                    attackInstance.addTransientModifier(new AttributeModifier(
                            PEWTER_ATTACK_MODIFIER_UUID, "Pewter Attack Damage", attackAmt,
                            AttributeModifier.Operation.ADDITION));
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
                            PEWTER_HEALTH_MODIFIER_UUID, "Pewter Max Health", healthAmt,
                            AttributeModifier.Operation.ADDITION));
                    player.setHealth(currentHealthPercent * player.getMaxHealth());
                }
            }

            // Knockback Resistance: Add flat (0.5 * strength * flareMult)
            double kbAmt = 0.5D * strength * flareMult;
            if (stage >= 3) {
                kbAmt = 1.0D; // 100% knockback resistance for true savants
            }
            if (kbInstance != null) {
                AttributeModifier mod = kbInstance.getModifier(PEWTER_KNOCKBACK_MODIFIER_UUID);
                if (mod == null || Math.abs(mod.getAmount() - kbAmt) > 0.001D) {
                    kbInstance.removeModifier(PEWTER_KNOCKBACK_MODIFIER_UUID);
                    kbInstance.addTransientModifier(new AttributeModifier(
                            PEWTER_KNOCKBACK_MODIFIER_UUID, "Pewter Knockback Resistance", kbAmt,
                            AttributeModifier.Operation.ADDITION));
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
            if (healthInstance != null && healthInstance.getModifier(PEWTER_DRAG_HEALTH_MODIFIER_UUID) != null) {
                float currentHealthPercent = player.getHealth() / player.getMaxHealth();
                healthInstance.removeModifier(PEWTER_DRAG_HEALTH_MODIFIER_UUID);
                player.setHealth(Math.max(1.0F, currentHealthPercent * player.getMaxHealth()));
            }
        } else {
            // Speed: Multiply base by (1.0 - 0.90) = 90% reduction
            if (speedInstance != null && speedInstance.getModifier(PEWTER_DRAG_SPEED_MODIFIER_UUID) == null) {
                speedInstance.addTransientModifier(new AttributeModifier(
                        PEWTER_DRAG_SPEED_MODIFIER_UUID, "Pewter Drag Slowness", -0.90D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            // Attack Damage: Multiply base by (1.0 - 0.75) = 75% reduction
            if (attackInstance != null && attackInstance.getModifier(PEWTER_DRAG_ATTACK_MODIFIER_UUID) == null) {
                attackInstance.addTransientModifier(new AttributeModifier(
                        PEWTER_DRAG_ATTACK_MODIFIER_UUID, "Pewter Drag Weakness", -0.75D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }

            // Savant Drag Penalties: severe mining fatigue, slowness, and max health
            // reduction
            if (stage >= 2) {
                int fatigueLevel = stage - 2; // stage 2 -> level 0 (I), stage 3 -> level 1 (II), stage 4 -> level 2
                                              // (III)
                int slownessLevel = stage - 1; // stage 2 -> level 1 (II), stage 3 -> level 2 (III), stage 4 -> level 3
                                               // (IV)
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, fatigueLevel, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slownessLevel, true, false));

                double dragHealthPenalty = -2.0D * stage; // Stage 2: -4, Stage 3: -6, Stage 4: -8 max health
                if (healthInstance != null && healthInstance.getModifier(PEWTER_DRAG_HEALTH_MODIFIER_UUID) == null) {
                    float currentHealthPercent = player.getHealth() / player.getMaxHealth();
                    healthInstance.addTransientModifier(new AttributeModifier(
                            PEWTER_DRAG_HEALTH_MODIFIER_UUID, "Pewter Drag Health Penalty", dragHealthPenalty,
                            AttributeModifier.Operation.ADDITION));

                    float newHealth = currentHealthPercent * player.getMaxHealth();
                    if (newHealth <= 0.0F) {
                        player.hurt(player.damageSources().magic(), 1000.0F); // Lethal drag collapse!
                    } else {
                        player.setHealth(newHealth);
                    }
                }
            }
        }
    }

    private static void applyTin(LivingEntity entity, MetalArtsData data, int amplifier) {
        entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));

        int stage = data.savantStage(Metal.TIN);

        if (entity instanceof ServerPlayer player) {
            // Tin Savant Stage 2+ Penalty: bright light sensitivity
            if (stage >= 2) {
                boolean isSunlit = player.level().isDay() && player.level().canSeeSky(player.blockPosition());
                boolean nearLava = player.level().getBlockState(player.blockPosition())
                        .is(net.minecraft.world.level.block.Blocks.LAVA)
                        || player.level().getBlockState(player.blockPosition().below())
                                .is(net.minecraft.world.level.block.Blocks.LAVA);
                if (isSunlit || nearLava) {
                    if (player.tickCount % 60 == 0) {
                        player.hurt(player.damageSources().magic(), 0.5F);
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false));
                    }
                }
            }

            // Tin Savant Stage 3: Heartbeat detection (red hearts)
            if (stage >= 3 && player.level() instanceof ServerLevel serverLevel && player.tickCount % 20 == 0) {
                double hRange = 16.0D;
                List<LivingEntity> nearbyHidden = player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(hRange),
                        e -> e != player && e.isAlive() && !isCopperclouded(e));
                for (LivingEntity e : nearbyHidden) {
                    double ex = e.getX();
                    double ey = e.getY() + e.getBbHeight() * 0.5D;
                    double ez = e.getZ();
                    serverLevel.sendParticles(player, net.minecraft.core.particles.ParticleTypes.HEART, false, ex, ey,
                            ez, 1, 0.1D, 0.1D, 0.1D, 0D);
                }
            }

            // Tin Savant Stage 4: Echolocation (periodically glows entities through walls)
            if (stage == 4 && player.tickCount % 60 == 0) {
                double eRange = 24.0D;
                List<LivingEntity> nearbyGlow = player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(eRange),
                        e -> e != player && e.isAlive() && !isCopperclouded(e));
                for (LivingEntity e : nearbyGlow) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, false));
                }
            }

            // Tin Sound Waves Visualizer: Spawn beautiful sculk-vibration sound particles
            // traveling to the player's ears!
            if (player.level() instanceof ServerLevel serverLevel && player.tickCount % 4 == 0) {
                double range = data.isFlaring(Metal.TIN) ? 32.0D : 16.0D;
                List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(range),
                        e -> e != player && e.isAlive() && !isCopperclouded(e));

                for (LivingEntity e : nearby) {
                    double speedSqr = e.getDeltaMovement().horizontalDistanceSqr();
                    boolean isMoving = speedSqr > 0.0008D;
                    boolean isAttacking = e.swinging;
                    boolean isHurt = e.hurtTime > 0;
                    boolean isAmbient = e.getRandom().nextFloat() < 0.03F; // occasional ambient sounds

                    if (isMoving || isAttacking || isHurt || isAmbient) {
                        double ex = e.getX();
                        double ey = e.getY() + e.getBbHeight() * 0.5D;
                        double ez = e.getZ();

                        EntityPositionSource source = new EntityPositionSource(player, (float) player.getEyeHeight());
                        VibrationParticleOption vibration = new VibrationParticleOption(source, 15);
                        serverLevel.sendParticles(player, vibration, false, ex, ey, ez, 1, 0D, 0D, 0D, 0D);

                        serverLevel.sendParticles(player, ParticleTypes.SCULK_CHARGE_POP, false, ex, ey, ez,
                                isHurt ? 3 : 1, 0.05D, 0.05D, 0.05D, 0.01D);
                    }
                }
            }
        }
    }

    private static void applyCopper(LivingEntity entity, MetalArtsData data, int amplifier) {
        data.setCopperclouded(true);
        entity.addEffect(new MobEffectInstance(ModEffects.COPPERCLOUD.get(), 50, amplifier, false, true));
        double radius = amplifier > 0 ? 10D : 7D;
        if (entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 12 == 0) {
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2D / 24D) * i;
                serverLevel.sendParticles(COPPER_DUST, entity.getX() + Math.cos(angle) * radius, entity.getY() + 1.0D,
                        entity.getZ() + Math.sin(angle) * radius, 1, 0D, 0D, 0D, 0D);
            }
        }
    }

    private static Vec3 predictFuturePosition(Entity entity, float ticks, ServerLevel level) {
        if (entity instanceof Mob mob) {
            Path path = mob.getNavigation().getPath();
            if (path != null && !path.isDone() && mob.onGround()) {
                // Path-based prediction
                double speed = new Vec3(mob.getX() - mob.xo, 0.0D, mob.getZ() - mob.zo).length();
                if (speed < 0.01D) {
                    // Use base movement speed attribute as fallback if they are about to move
                    speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.25D;
                }
                double targetDistance = speed * ticks;
                
                Vec3 currentPos = mob.position();
                int nextIndex = path.getNextNodeIndex();
                int count = path.getNodeCount();
                
                Vec3 lastPos = currentPos;
                double accumulatedDistance = 0.0D;
                
                for (int i = nextIndex; i < count; i++) {
                    Node node = path.getNode(i);
                    Vec3 nodePos = new Vec3(node.x + 0.5D, node.y, node.z + 0.5D);
                    double dist = lastPos.distanceTo(nodePos);
                    if (accumulatedDistance + dist >= targetDistance) {
                        double remaining = targetDistance - accumulatedDistance;
                        double ratio = remaining / dist;
                        return lastPos.lerp(nodePos, ratio);
                    }
                    accumulatedDistance += dist;
                    lastPos = nodePos;
                }
                return lastPos; // return end of path if target distance is beyond the path
            }
        }

        // Physics-based prediction (airborne, no active path, or projectile/other entity)
        Vec3 simPos = entity.position();
        
        // Retrieve original unscaled velocity if entity is currently slowed down inside a dilation
        Vec3 simVel = entity.getDeltaMovement();
        net.minecraft.nbt.CompoundTag tag = entity.getPersistentData();
        if (tag.getBoolean("MA_InsideDilation")) {
            simVel = new Vec3(
                    tag.getDouble("MA_OrigVelX"),
                    tag.getDouble("MA_OrigVelY"),
                    tag.getDouble("MA_OrigVelZ")
            );
        } else if (tag.getBoolean("MA_HasDilationData")) {
            simVel = new Vec3(
                    tag.getDouble("MA_DilationVelX"),
                    tag.getDouble("MA_DilationVelY"),
                    tag.getDouble("MA_DilationVelZ")
            );
        }
        
        // If they are standing still on the ground, return current position (no path trail)
        if (entity.onGround() && simVel.horizontalDistanceSqr() < 0.0001D) {
            return simPos;
        }
        
        double gravity = 0.08D;
        double drag = 0.98D;
        if (entity instanceof Projectile) {
            gravity = getProjectileGravity(entity);
            drag = getProjectileDrag(entity);
        } else if (entity instanceof net.minecraft.world.entity.animal.FlyingAnimal || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            gravity = 0.0D;
        }
        
        int steps = Math.round(ticks);
        AABB baseBox = entity.getBoundingBox();
        
        for (int step = 0; step < steps; step++) {
            Vec3 nextPos = simPos.add(simVel);
            // Check collision
            AABB movedBox = baseBox.move(nextPos.subtract(entity.position()));
            if (!level.noCollision(entity, movedBox)) {
                // Collision occurred, stop prediction here
                break;
            }
            simPos = nextPos;
            // Drag is multiplied by velocity first, then gravity is subtracted, matching Minecraft Vanilla physics exactly
            simVel = new Vec3(simVel.x * drag, simVel.y * drag - gravity, simVel.z * drag);
        }
        return simPos;
    }

    private static void applyAtium(LivingEntity entity, MetalArtsData data, int amplifier) {
        entity.addEffect(new MobEffectInstance(ModEffects.ATIUM_SIGHT.get(), 35, amplifier, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 35, 0, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 35, amplifier, false, false));

        if (entity.tickCount % 2 == 0) {
            double range = 16D + (amplifier * 8D);
            AABB box = entity.getBoundingBox().inflate(range);
            for (Entity target : entity.level().getEntitiesOfClass(Entity.class, box, e -> (e instanceof LivingEntity le && le.isAlive() && le != entity) || e instanceof Projectile)) {
                if (target == entity)
                    continue;

                if (target instanceof LivingEntity livingTarget) {
                    livingTarget.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                }

                if (entity instanceof ServerPlayer serverPlayer) {
                    // Precompute future positions at 2 steps: 50% and 100% of prediction ticks
                    float predictionTicks = 12.0F + (amplifier * 8.0F);
                    Vec3[] positions = new Vec3[2];
                    positions[0] = predictFuturePosition(target, predictionTicks * 0.5F, (ServerLevel) entity.level());
                    positions[1] = predictFuturePosition(target, predictionTicks, (ServerLevel) entity.level());
                    
                    // Send sync packet to client
                    com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork.sendAtiumShadows(serverPlayer, target.getId(), positions);
                }
            }
        }
    }

    private static void onReserveEmpty(LivingEntity entity, MetalArtsData data, Metal metal) {
        data.stopBurning(metal);
    }

    private static void updateBronzePulse(ServerPlayer seeker, MetalArtsData seekerData) {
        double range = seekerData.isFlaring(Metal.BRONZE) ? 42D : 28D;
        Optional<LivingEntity> target = seeker.level()
                .getEntitiesOfClass(LivingEntity.class, seeker.getBoundingBox().inflate(range),
                        entity -> entity != seeker && entity.isAlive())
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

        // Seeker Melodic Pulses: Play periodic rhythmic heartbeat sound waves
        // representing the target's burning metal type
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
        seeker.level().playSound(null, seeker.getX(), seeker.getY(), seeker.getZ(), sound, SoundSource.PLAYERS,
                strength * 0.60F, pitch);
    }

    private static boolean isDetectable(ServerPlayer seeker, LivingEntity entity) {
        // Trellium spiritual stealth — blocks ALL detection
        boolean trelliumShielded = entity.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(Metal.TRELLIUM)
                        || data.installedSpikes().stream().anyMatch(s -> s.spikeMetal() == Metal.TRELLIUM))
                .orElse(false);
        if (trelliumShielded) {
            return false;
        }

        int seekerBronzeSavant = seeker.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(d -> d.savantStage(Metal.BRONZE))
                .orElse(0);
        int targetCopperSavant = entity.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(d -> d.savantStage(Metal.COPPER))
                .orElse(0);

        boolean copperShielded = entity.hasEffect(ModEffects.COPPERCLOUD.get());
        if (!copperShielded) {
            copperShielded = entity.getCapability(MetalArtsCapabilities.METAL_ARTS)
                    .map(data -> data.isCopperclouded())
                    .orElse(false);
        }

        if (copperShielded) {
            if (seekerBronzeSavant >= 3 && targetCopperSavant < 3) {
                // Pierced the coppercloud!
            } else {
                return false;
            }
        }

        return entity.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> !data.burningMetals().isEmpty() || data.totalCorruption() > 0)
                .orElse(entity.hasEffect(ModEffects.ATIUM_SIGHT.get())
                        || entity.hasEffect(ModEffects.BRONZE_SEEKING.get()));
    }

    private static Optional<Metal> detectableMetal(LivingEntity entity) {
        Metal[] detected = new Metal[1];
        entity.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .ifPresent(data -> detected[0] = data.burningMetals().stream().findFirst().orElse(null));
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
        double range = Math.min(ServerConfig.VALUES.wellBronzePulseRange.get(),
                seekerData.isFlaring(Metal.BRONZE) ? 128D : 96D);
        BlockPos origin = seeker.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        // Optimization: Use the O(1)/O(N) WellRegistry instead of scanning hundreds of
        // thousands of blocks.
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
        public double baseRadius;
        public final Metal type;
        public final ServerPlayer owner;

        public TimeBubble(BlockPos center, double radius, Metal type, ServerPlayer owner) {
            this.center = center;
            this.baseRadius = radius;
            this.type = type;
            this.owner = owner;
        }

        public double getRadius() {
            boolean isFlaring = owner.getCapability(MetalArtsCapabilities.METAL_ARTS)
                    .map(d -> d.isFlaring(type)).orElse(false);
            return isFlaring ? baseRadius * 1.25D : baseRadius;
        }

        public AABB getArea() {
            return new AABB(center).inflate(getRadius());
        }
    }

    public static final java.util.List<TimeBubble> ACTIVE_BUBBLES = new java.util.ArrayList<>();
    public static final java.util.Set<Integer> DILATED_ENTITIES = new java.util.HashSet<>();
    public static final java.util.Map<BlockPos, BlockEntityDilationData> DILATED_BLOCK_ENTITIES = new java.util.HashMap<>();

    private static java.lang.reflect.Field itemAgeField = null;
    private static java.lang.reflect.Field itemPickupDelayField = null;

    static {
        try {
            itemAgeField = net.minecraft.world.entity.item.ItemEntity.class.getDeclaredField("age");
            itemAgeField.setAccessible(true);
        } catch (Exception e) {
            try {
                itemAgeField = net.minecraft.world.entity.item.ItemEntity.class.getDeclaredField("f_32001_");
                itemAgeField.setAccessible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        try {
            itemPickupDelayField = net.minecraft.world.entity.item.ItemEntity.class.getDeclaredField("pickupDelay");
            itemPickupDelayField.setAccessible(true);
        } catch (Exception e) {
            try {
                itemPickupDelayField = net.minecraft.world.entity.item.ItemEntity.class.getDeclaredField("f_32002_");
                itemPickupDelayField.setAccessible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static class BlockEntityDilationData {
        public double tickCredit = 0.0D;
        public int lastBurnTime = -1;
        public int lastCookTime = -1;
        public int lastBrewTime = -1;
        public int lastSpawnerDelay = -1;
        public int lastHopperCooldown = -1;
        public boolean hasState = false;

        public void saveState(BlockEntity be) {
            net.minecraft.nbt.CompoundTag tag = be.saveWithoutMetadata();
            if (tag.contains("BurnTime")) {
                lastBurnTime = tag.getInt("BurnTime");
            }
            if (tag.contains("CookTime")) {
                lastCookTime = tag.getInt("CookTime");
            }
            if (tag.contains("BrewTime")) {
                lastBrewTime = tag.getInt("BrewTime");
            }
            if (tag.contains("Delay")) {
                lastSpawnerDelay = tag.getInt("Delay");
            }
            if (tag.contains("TransferCooldown")) {
                lastHopperCooldown = tag.getInt("TransferCooldown");
            }
            hasState = true;
        }

        public void restoreState(BlockEntity be) {
            if (!hasState) {
                saveState(be);
                return;
            }
            net.minecraft.nbt.CompoundTag tag = be.saveWithoutMetadata();
            boolean changed = false;
            if (tag.contains("BurnTime") && lastBurnTime != -1) {
                tag.putInt("BurnTime", lastBurnTime);
                changed = true;
            }
            if (tag.contains("CookTime") && lastCookTime != -1) {
                tag.putInt("CookTime", lastCookTime);
                changed = true;
            }
            if (tag.contains("BrewTime") && lastBrewTime != -1) {
                tag.putInt("BrewTime", lastBrewTime);
                changed = true;
            }
            if (tag.contains("Delay") && lastSpawnerDelay != -1) {
                tag.putInt("Delay", lastSpawnerDelay);
                changed = true;
            }
            if (tag.contains("TransferCooldown") && lastHopperCooldown != -1) {
                tag.putInt("TransferCooldown", lastHopperCooldown);
                changed = true;
            }
            if (changed) {
                be.load(tag);
                be.setChanged();
            }
        }
    }

    private static void tickSlowedBlockEntity(BlockEntity be, double slowFactor, ServerLevel level) {
        BlockPos pos = be.getBlockPos();
        BlockEntityDilationData data = DILATED_BLOCK_ENTITIES.computeIfAbsent(pos, p -> new BlockEntityDilationData());
        data.tickCredit += 1.0D / slowFactor;
        if (data.tickCredit >= 1.0D) {
            data.tickCredit -= 1.0D;
            data.saveState(be);
        } else {
            data.restoreState(be);
        }
    }

    public static double getSlowFactorAt(ServerLevel level, BlockPos pos) {
        double maxSlowFactor = 1.0D;
        boolean insideAnyBendalloy = false;
        
        for (TimeBubble bubble : ACTIVE_BUBBLES) {
            if (bubble.owner.level() != level || bubble.type != Metal.BENDALLOY)
                continue;
            double cx = bubble.center.getX() + 0.5D;
            double cy = bubble.center.getY() + 0.5D;
            double cz = bubble.center.getZ() + 0.5D;
            double rSqr = bubble.getRadius() * bubble.getRadius();
            if (pos.distToCenterSqr(cx, cy, cz) <= rSqr) {
                insideAnyBendalloy = true;
                break;
            }
        }
        
        if (insideAnyBendalloy) {
            return 1.0D;
        }

        for (TimeBubble bubble : ACTIVE_BUBBLES) {
            if (bubble.owner.level() != level || bubble.type != Metal.BENDALLOY)
                continue;

            double cx = bubble.center.getX() + 0.5D;
            double cy = bubble.center.getY() + 0.5D;
            double cz = bubble.center.getZ() + 0.5D;
            double scanRange = bubble.getRadius() + ServerConfig.VALUES.maxAffectedDistanceOutsideBubble.get();
            double distSqr = pos.distToCenterSqr(cx, cy, cz);
            if (distSqr > bubble.getRadius() * bubble.getRadius() && distSqr <= scanRange * scanRange) {
                float strength = bubble.owner.getCapability(MetalArtsCapabilities.METAL_ARTS)
                        .map(d -> d.getEffectiveStrength(bubble.type)).orElse(1.0F);
                boolean isFlaring = bubble.owner.getCapability(MetalArtsCapabilities.METAL_ARTS)
                        .map(d -> d.isFlaring(bubble.type)).orElse(false);
                
                double baseFactor = ServerConfig.VALUES.baseBendalloySlowFactor.get();
                double flareMultiplier = isFlaring ? ServerConfig.VALUES.bendalloyFlareMultiplier.get() : 1.0D;
                double maxSlow = ServerConfig.VALUES.maxBendalloySlowFactor.get();
                double slowFactor = baseFactor * strength * flareMultiplier;
                slowFactor = Math.max(1.0D, Math.min(maxSlow, slowFactor));
                
                if (slowFactor > maxSlowFactor) {
                    maxSlowFactor = slowFactor;
                }
            }
        }
        return maxSlowFactor;
    }

    private static void saveDilationState(Entity entity) {
        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        Vec3 pos = entity.position();
        Vec3 vel = entity.getDeltaMovement();
        data.putDouble("MA_DilationPosX", pos.x);
        data.putDouble("MA_DilationPosY", pos.y);
        data.putDouble("MA_DilationPosZ", pos.z);
        data.putDouble("MA_DilationVelX", vel.x);
        data.putDouble("MA_DilationVelY", vel.y);
        data.putDouble("MA_DilationVelZ", vel.z);
        data.putBoolean("MA_HasDilationData", true);
    }

    private static void restoreDilationState(Entity entity, double slowFactor) {
        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean("MA_HasDilationData")) {
            saveDilationState(entity);
            return;
        }
        double px = data.getDouble("MA_DilationPosX");
        double py = data.getDouble("MA_DilationPosY");
        double pz = data.getDouble("MA_DilationPosZ");
        double vx = data.getDouble("MA_DilationVelX");
        double vy = data.getDouble("MA_DilationVelY");
        double vz = data.getDouble("MA_DilationVelZ");

        double speedFactor = 1.0D / slowFactor;

        entity.setPos(px, py, pz);
        entity.setDeltaMovement(new Vec3(vx * speedFactor, vy * speedFactor, vz * speedFactor));
        entity.fallDistance = 0.0F;

        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().broadcast(entity,
                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
        }
    }

    public static void tickWorldBubbles(ServerLevel level) {
        ACTIVE_BUBBLES.removeIf(bubble -> {
            return !bubble.owner.isAlive() || !bubble.owner.getCapability(MetalArtsCapabilities.METAL_ARTS)
                    .map(data -> data.isBurning(bubble.type)).orElse(false);
        });

        Set<LivingEntity> insideBendalloy = new HashSet<>();
        Set<LivingEntity> insideCadmium = new HashSet<>();

        // Generic maps to track dilation factors per entity
        Map<Entity, Double> entitySlowFactors = new HashMap<>();
        Map<Entity, Integer> entityExtraTicks = new HashMap<>();

        // Map to track block entity speedups
        Map<BlockEntity, Integer> blockEntityExtraTicks = new HashMap<>();

        for (TimeBubble bubble : ACTIVE_BUBBLES) {
            if (bubble.owner.level() != level)
                continue;
            double cx = bubble.center.getX() + 0.5D;
            double cy = bubble.center.getY() + 0.5D;
            double cz = bubble.center.getZ() + 0.5D;
            double rSqr = bubble.getRadius() * bubble.getRadius();

            // Calculate dilation factors based on owner strength and flaring state
            float strength = bubble.owner.getCapability(MetalArtsCapabilities.METAL_ARTS)
                    .map(d -> d.getEffectiveStrength(bubble.type)).orElse(1.0F);
            boolean isFlaring = bubble.owner.getCapability(MetalArtsCapabilities.METAL_ARTS)
                    .map(d -> d.isFlaring(bubble.type)).orElse(false);
            float flareMult = isFlaring ? 1.5F : 1.0F;

            double slowFactor = 1.0D;
            if (bubble.type == Metal.BENDALLOY) {
                double baseFactor = ServerConfig.VALUES.baseBendalloySlowFactor.get();
                double flareMultiplier = isFlaring ? ServerConfig.VALUES.bendalloyFlareMultiplier.get() : 1.0D;
                double maxSlowFactor = ServerConfig.VALUES.maxBendalloySlowFactor.get();
                slowFactor = baseFactor * strength * flareMultiplier;
                slowFactor = Math.max(1.0D, Math.min(maxSlowFactor, slowFactor));
            }
            int cadmiumExtraTicks = Math.max(1, Math.round(3.0F * strength * flareMult));

            double scanRange = bubble.type == Metal.BENDALLOY 
                    ? bubble.getRadius() + ServerConfig.VALUES.maxAffectedDistanceOutsideBubble.get()
                    : 64.0D;
            AABB scanArea = new AABB(cx - scanRange, cy - scanRange, cz - scanRange, cx + scanRange, cy + scanRange, cz + scanRange);
            List<Entity> entities = level.getEntities((Entity) null, scanArea, Entity::isAlive);

            for (Entity entity : entities) {
                double distSqr = entity.distanceToSqr(cx, cy, cz);
                if (entity instanceof LivingEntity living) {
                    if (bubble.type == Metal.BENDALLOY) {
                        if (distSqr <= rSqr) {
                            insideBendalloy.add(living);
                        } else if (distSqr <= scanRange * scanRange) {
                            entitySlowFactors.merge(living, slowFactor, Math::max);
                        }
                    } else if (bubble.type == Metal.CADMIUM) {
                        if (distSqr <= rSqr) {
                            insideCadmium.add(living);
                        } else if (distSqr <= 64.0D * 64.0D) {
                            entityExtraTicks.merge(living, cadmiumExtraTicks, Math::max);
                        }
                    }
                } else {
                    if (bubble.type == Metal.BENDALLOY) {
                        if (distSqr > rSqr && distSqr <= scanRange * scanRange) {
                            entitySlowFactors.merge(entity, slowFactor, Math::max);
                        }
                    } else if (bubble.type == Metal.CADMIUM) {
                        if (distSqr > rSqr && distSqr <= 64.0D * 64.0D) {
                            entityExtraTicks.merge(entity, cadmiumExtraTicks, Math::max);
                        }
                    }
                }
            }

            // Spawn spherical boundary particles to show bubble volume visually
            if (level.getGameTime() % 2 == 0) {
                double r = bubble.getRadius();
                int particleCount = (int) (120 * (r / 7.5));
                for (int i = 0; i < particleCount; i++) {
                    double theta = level.random.nextDouble() * Math.PI * 2.0D;
                    double phi = Math.acos(2.0D * level.random.nextDouble() - 1.0D);
                    double x = cx + r * Math.sin(phi) * Math.cos(theta);
                    double y = cy + r * Math.cos(phi);
                    double z = cz + r * Math.sin(phi) * Math.sin(theta);

                    if (bubble.type == Metal.BENDALLOY) {
                        if (level.random.nextBoolean()) {
                            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 0, 0.0D, 0.02D, 0.0D, 0.5D);
                        } else {
                            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        }
                    } else if (bubble.type == Metal.CADMIUM) {
                        if (level.random.nextBoolean()) {
                            level.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        } else {
                            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        }
                    }
                }
            }
        }

        // Global Atium Dilation: scan for active Atium burners in the level
        Set<Player> atiumBurners = new HashSet<>();
        double maxAtiumSlowFactor = 1.0D;
        for (ServerPlayer player : level.players()) {
            boolean isBurning = player.getCapability(MetalArtsCapabilities.METAL_ARTS)
                    .map(d -> d.isBurning(Metal.ATIUM) && d.getReserve(Metal.ATIUM) > 0F)
                    .orElse(false);
            if (isBurning) {
                atiumBurners.add(player);
                float strength = player.getCapability(MetalArtsCapabilities.METAL_ARTS)
                        .map(d -> d.getEffectiveStrength(Metal.ATIUM)).orElse(1.0F);
                boolean isFlaring = player.getCapability(MetalArtsCapabilities.METAL_ARTS)
                        .map(d -> d.isFlaring(Metal.ATIUM)).orElse(false);
                double slowFactor = 1.35D + (strength * 0.1D) + (isFlaring ? 0.3D : 0.0D);
                if (slowFactor > maxAtiumSlowFactor) {
                    maxAtiumSlowFactor = slowFactor;
                }
            }
        }

        if (maxAtiumSlowFactor > 1.0D) {
            Set<Entity> levelEntities = new HashSet<>();
            for (ServerPlayer player : level.players()) {
                AABB area = player.getBoundingBox().inflate(128.0D);
                levelEntities.addAll(level.getEntities((Entity) null, area, Entity::isAlive));
            }
            
            for (Entity entity : levelEntities) {
                if (atiumBurners.contains(entity)) {
                    continue; // Atium burners are immune to global Atium slowdown
                }
                
                if (entity instanceof LivingEntity living) {
                    entitySlowFactors.merge(living, maxAtiumSlowFactor, Math::max);
                } else {
                    entitySlowFactors.merge(entity, maxAtiumSlowFactor, Math::max);
                }
            }
        }

        // Clean up: entities inside a bubble cannot be considered outside one of the same type
        for (LivingEntity living : insideBendalloy) {
            entitySlowFactors.remove(living);
        }
        for (LivingEntity living : insideCadmium) {
            entityExtraTicks.remove(living);
        }

        // Cancellation: if an entity is affected by both slowdown and speedup, they cancel out
        Set<Entity> overlap = new HashSet<>(entitySlowFactors.keySet());
        overlap.retainAll(entityExtraTicks.keySet());
        for (Entity entity : overlap) {
            entitySlowFactors.remove(entity);
            entityExtraTicks.remove(entity);
        }

        Set<LivingEntity> toAccelerate = new HashSet<>();
        Set<LivingEntity> toFreeze = new HashSet<>();
        Set<Entity> slowedDownEntities = new HashSet<>();

        // Populate acceleration list
        for (Entity entity : entityExtraTicks.keySet()) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 10, 1, false, false));
                if (!(living instanceof Player)) {
                    toAccelerate.add(living);
                }
            }
        }

        // Populate freeze list
        for (Entity entity : entitySlowFactors.keySet()) {
            if (entity instanceof LivingEntity living) {
                if (living instanceof Player && !ServerConfig.VALUES.affectOtherPlayers.get()) {
                    continue;
                }
                living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10, 3, false, false));
                toFreeze.add(living);
            }
        }

        // Perform entity tick acceleration safely
        for (LivingEntity entity : toAccelerate) {
            int extraTicks = entityExtraTicks.getOrDefault(entity, 3);
            for (int i = 0; i < extraTicks; i++) {
                entity.tick();
            }
        }

        // Perform entity slowness freezing safely
        for (LivingEntity entity : toFreeze) {
            slowedDownEntities.add(entity);
            DILATED_ENTITIES.add(entity.getId());
            double slowFactor = entitySlowFactors.getOrDefault(entity, 8.0D);
            net.minecraft.nbt.CompoundTag tag = entity.getPersistentData();
            
            if (!tag.getBoolean("MA_HasDilationData")) {
                saveDilationState(entity);
                tag.putDouble("MA_DilationTickCredit", 0.0D);
                tag.putBoolean("MA_WasSkipped", false);
            }
            
            double tickCredit = tag.getDouble("MA_DilationTickCredit");
            tickCredit += 1.0D / slowFactor;
            
            boolean isHurt = entity.hurtTime > 0;
            boolean savedHurt = tag.getBoolean("MA_SavedHurt");
            if (isHurt && !savedHurt) {
                saveDilationState(entity);
                tag.putBoolean("MA_SavedHurt", true);
            } else if (!isHurt && savedHurt) {
                tag.putBoolean("MA_SavedHurt", false);
            }
            
            boolean wasSkipped = tag.getBoolean("MA_WasSkipped");
            
            if (tickCredit >= 1.0D) {
                // Next tick is active
                tickCredit -= 1.0D;
                tag.putDouble("MA_DilationTickCredit", tickCredit);
                tag.putBoolean("MA_WasSkipped", false);
                
                if (wasSkipped) {
                    // Current tick was skipped, so we need to restore the original unscaled velocity for the next (active) tick
                    double vx = tag.getDouble("MA_DilationVelX");
                    double vy = tag.getDouble("MA_DilationVelY");
                    double vz = tag.getDouble("MA_DilationVelZ");
                    entity.setDeltaMovement(new Vec3(vx, vy, vz));
                } else {
                    // Current tick was active, so we save the new position and velocity as the reference
                    saveDilationState(entity);
                }
            } else {
                // Next tick is skipped
                tag.putDouble("MA_DilationTickCredit", tickCredit);
                tag.putBoolean("MA_WasSkipped", true);
                
                if (wasSkipped) {
                    // Current tick was skipped. We restore the reference position, and keep the velocity scaled down.
                    restoreDilationState(entity, slowFactor);
                } else {
                    // Current tick was active. We save the reference position/velocity, and then scale down the velocity.
                    saveDilationState(entity);
                    double vx = tag.getDouble("MA_DilationVelX");
                    double vy = tag.getDouble("MA_DilationVelY");
                    double vz = tag.getDouble("MA_DilationVelZ");
                    double speedFactor = 1.0D / slowFactor;
                    entity.setDeltaMovement(new Vec3(vx * speedFactor, vy * speedFactor, vz * speedFactor));
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        serverLevel.getChunkSource().broadcast(entity,
                                new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
                    }
                }
            }
        }

        // Handle Non-Living Entities (arrows, items, projectiles) smoothly via velocity scaling:
        for (Map.Entry<Entity, Double> entry : entitySlowFactors.entrySet()) {
            Entity entity = entry.getKey();
            if (entity instanceof LivingEntity)
                continue; // already handled above

            slowedDownEntities.add(entity);
            DILATED_ENTITIES.add(entity.getId());
            double slowFactor = entry.getValue();
            double speedFactor = 1.0D / slowFactor;

            net.minecraft.nbt.CompoundTag tag = entity.getPersistentData();
            
            // Get or initialize original unscaled velocity
            Vec3 originalVel;
            if (!tag.getBoolean("MA_InsideDilation")) {
                originalVel = entity.getDeltaMovement();
                tag.putDouble("MA_OrigVelX", originalVel.x);
                tag.putDouble("MA_OrigVelY", originalVel.y);
                tag.putDouble("MA_OrigVelZ", originalVel.z);
                tag.putBoolean("MA_InsideDilation", true);
                tag.putDouble("MA_AppliedSpeedFactor", speedFactor);
                tag.putDouble("MA_DilationTickCredit", 0.0D);

                // Pull back position to prevent transition jump/pop
                if (entity.tickCount > 0 && (entity.xo != 0.0D || entity.yo != 0.0D || entity.zo != 0.0D)) {
                    double dx = entity.getX() - entity.xo;
                    double dy = entity.getY() - entity.yo;
                    double dz = entity.getZ() - entity.zo;
                    entity.setPos(entity.xo + dx * speedFactor, entity.yo + dy * speedFactor, entity.zo + dz * speedFactor);
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        serverLevel.getChunkSource().broadcast(entity,
                                new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(entity));
                    }
                }
            } else {
                originalVel = new Vec3(
                        tag.getDouble("MA_OrigVelX"),
                        tag.getDouble("MA_OrigVelY"),
                        tag.getDouble("MA_OrigVelZ")
                );
            }

            // Apply smooth fractional gravity and drag step on every tick
            double gravity = getProjectileGravity(entity);
            double drag = getProjectileDrag(entity);
            double dragFactor = 1.0D - (1.0D - drag) * speedFactor;
            double newY = (originalVel.y - gravity * speedFactor) * dragFactor;
            originalVel = new Vec3(originalVel.x * dragFactor, newY, originalVel.z * dragFactor);
            tag.putDouble("MA_OrigVelX", originalVel.x);
            tag.putDouble("MA_OrigVelY", originalVel.y);
            tag.putDouble("MA_OrigVelZ", originalVel.z);

            // Update tick credit accumulator for other timers (like TNT fuse, Item age)
            double credit = tag.getDouble("MA_DilationTickCredit");
            credit += speedFactor;
            boolean didPhysicsTick = false;
            if (credit >= 1.0D) {
                credit -= 1.0D;
                didPhysicsTick = true;
            }
            tag.putDouble("MA_DilationTickCredit", credit);

            // Set actual scaled velocity for the entity to move by during next tick
            Vec3 scaledVel = originalVel.scale(speedFactor);
            entity.setDeltaMovement(scaledVel);

            // Sync to client
            if (entity.level() instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(entity,
                        new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
            }

            // Apply stateless timer adjustments for ItemEntity and PrimedTnt based on tick credit
            if (!didPhysicsTick) {
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) {
                    try {
                        if (itemAgeField != null) {
                            int age = itemAgeField.getInt(item);
                            itemAgeField.setInt(item, age - 1);
                        }
                        if (itemPickupDelayField != null) {
                            int delay = itemPickupDelayField.getInt(item);
                            if (delay > 0) {
                                itemPickupDelayField.setInt(item, delay + 1);
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                } else if (entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt) {
                    tnt.setFuse(tnt.getFuse() + 1);
                }
            }
        }

        for (Map.Entry<Entity, Integer> entry : entityExtraTicks.entrySet()) {
            Entity entity = entry.getKey();
            if (entity instanceof LivingEntity)
                continue; // already handled above

            int extraTicks = entry.getValue();
            for (int i = 0; i < extraTicks; i++) {
                entity.tick();
            }
            if (entity.level() instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(entity,
                        new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
                serverLevel.getChunkSource().broadcast(entity,
                        new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(entity));
            }
        }

        // Clean up bubble tag for entities that are no longer affected (even if the bubble was deactivated)
        java.util.Iterator<Integer> iterator = DILATED_ENTITIES.iterator();
        while (iterator.hasNext()) {
            int entityId = iterator.next();
            Entity entity = level.getEntity(entityId);
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                continue;
            }
            if (!slowedDownEntities.contains(entity)) {
                net.minecraft.nbt.CompoundTag tag = entity.getPersistentData();
                if (tag.getBoolean("MA_InsideDilation")) {
                    // Restore original unscaled velocity upon leaving
                    Vec3 originalVel = new Vec3(
                            tag.getDouble("MA_OrigVelX"),
                            tag.getDouble("MA_OrigVelY"),
                            tag.getDouble("MA_OrigVelZ")
                    );
                    if (originalVel.lengthSqr() >= 0.0001D) {
                        entity.setDeltaMovement(originalVel);
                        if (entity.level() instanceof ServerLevel serverLevel) {
                            serverLevel.getChunkSource().broadcast(entity,
                                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
                        }

                        // Push forward position to compensate for the last slowed tick
                        double appliedFactor = tag.getDouble("MA_AppliedSpeedFactor");
                        if (appliedFactor > 0.0D && entity.tickCount > 0 && (entity.xo != 0.0D || entity.yo != 0.0D || entity.zo != 0.0D)) {
                            double dx = entity.getX() - entity.xo;
                            double dy = entity.getY() - entity.yo;
                            double dz = entity.getZ() - entity.zo;
                            double factor = 1.0D / appliedFactor;
                            double maxPush = 5.0D;
                            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (distance > 0.001D) {
                                double pushFactor = Math.min(factor, maxPush / distance);
                                entity.setPos(entity.xo + dx * pushFactor, entity.yo + dy * pushFactor, entity.zo + dz * pushFactor);
                            }
                            if (entity.level() instanceof ServerLevel serverLevel) {
                                serverLevel.getChunkSource().broadcast(entity,
                                        new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(entity));
                            }
                        }
                    }
                    tag.remove("MA_InsideDilation");
                    tag.remove("MA_OrigVelX");
                    tag.remove("MA_OrigVelY");
                    tag.remove("MA_OrigVelZ");
                    tag.remove("MA_AppliedSpeedFactor");
                    tag.remove("MA_DilationTickCredit");
                }
                
                // Living entity cleanup
                if (tag.getBoolean("MA_HasDilationData")) {
                    if (tag.getBoolean("MA_WasSkipped")) {
                        double vx = tag.getDouble("MA_DilationVelX");
                        double vy = tag.getDouble("MA_DilationVelY");
                        double vz = tag.getDouble("MA_DilationVelZ");
                        entity.setDeltaMovement(new Vec3(vx, vy, vz));
                        if (entity.level() instanceof ServerLevel serverLevel) {
                            serverLevel.getChunkSource().broadcast(entity,
                                    new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
                        }
                    }
                    tag.remove("MA_HasDilationData");
                    tag.remove("MA_WasSkipped");
                    tag.remove("MA_DilationTickCredit");
                    tag.remove("MA_DilationPosX");
                    tag.remove("MA_DilationPosY");
                    tag.remove("MA_DilationPosZ");
                    tag.remove("MA_DilationVelX");
                    tag.remove("MA_DilationVelY");
                    tag.remove("MA_DilationVelZ");
                    tag.remove("MA_SavedHurt");
                }
                iterator.remove();
            }
        }

        // Find and handle Block Entities outside bubbles
        Set<BlockEntity> insideCadmiumBlocks = new HashSet<>();
        Set<BlockEntity> outsideCadmiumBlocks = new HashSet<>();
        Set<BlockEntity> insideBendalloyBlocks = new HashSet<>();
        Set<BlockEntity> outsideBendalloyBlocks = new HashSet<>();
        Map<BlockEntity, Double> blockEntitySlowFactors = new HashMap<>();

        for (TimeBubble bubble : ACTIVE_BUBBLES) {
            if (bubble.owner.level() != level)
                continue;

            double cx = bubble.center.getX() + 0.5D;
            double cy = bubble.center.getY() + 0.5D;
            double cz = bubble.center.getZ() + 0.5D;
            double rSqr = bubble.getRadius() * bubble.getRadius();

            // Calculate factors
            float strength = bubble.owner.getCapability(MetalArtsCapabilities.METAL_ARTS)
                    .map(d -> d.getEffectiveStrength(bubble.type)).orElse(1.0F);
            boolean isFlaring = bubble.owner.getCapability(MetalArtsCapabilities.METAL_ARTS)
                    .map(d -> d.isFlaring(bubble.type)).orElse(false);
            float flareMult = isFlaring ? 1.5F : 1.0F;

            double slowFactor = 1.0D;
            if (bubble.type == Metal.BENDALLOY) {
                double baseFactor = ServerConfig.VALUES.baseBendalloySlowFactor.get();
                double flareMultiplier = isFlaring ? ServerConfig.VALUES.bendalloyFlareMultiplier.get() : 1.0D;
                double maxSlowFactor = ServerConfig.VALUES.maxBendalloySlowFactor.get();
                slowFactor = baseFactor * strength * flareMultiplier;
                slowFactor = Math.max(1.0D, Math.min(maxSlowFactor, slowFactor));
            }
            int cadmiumExtraTicks = Math.max(1, Math.round(3.0F * strength * flareMult));

            double scanRange = bubble.type == Metal.BENDALLOY 
                    ? bubble.getRadius() + ServerConfig.VALUES.maxAffectedDistanceOutsideBubble.get()
                    : 32.0D;

            int minChunkX = ((int) cx - (int) scanRange) >> 4;
            int maxChunkX = ((int) cx + (int) scanRange) >> 4;
            int minChunkZ = ((int) cz - (int) scanRange) >> 4;
            int maxChunkZ = ((int) cz + (int) scanRange) >> 4;

            for (int cxChunk = minChunkX; cxChunk <= maxChunkX; cxChunk++) {
                for (int czChunk = minChunkZ; czChunk <= maxChunkZ; czChunk++) {
                    if (level.hasChunk(cxChunk, czChunk)) {
                        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunk(cxChunk, czChunk);
                        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                            BlockPos pos = entry.getKey();
                            BlockEntity be = entry.getValue();
                            if (be.isRemoved())
                                continue;

                            double distSqr = pos.distToCenterSqr(cx, cy, cz);
                            if (bubble.type == Metal.CADMIUM) {
                                if (distSqr <= rSqr) {
                                    insideCadmiumBlocks.add(be);
                                } else if (distSqr <= (double) (scanRange * scanRange)) {
                                    outsideCadmiumBlocks.add(be);
                                    blockEntityExtraTicks.merge(be, cadmiumExtraTicks, Math::max);
                                }
                            } else if (bubble.type == Metal.BENDALLOY) {
                                if (distSqr <= rSqr) {
                                    insideBendalloyBlocks.add(be);
                                } else if (distSqr <= (double) (scanRange * scanRange)) {
                                    outsideBendalloyBlocks.add(be);
                                    blockEntitySlowFactors.merge(be, slowFactor, Math::max);
                                }
                            }
                        }
                    }
                }
            }
        }

        outsideCadmiumBlocks.removeAll(insideCadmiumBlocks);
        for (BlockEntity be : insideCadmiumBlocks) {
            blockEntityExtraTicks.remove(be);
        }

        outsideBendalloyBlocks.removeAll(insideBendalloyBlocks);
        for (BlockEntity be : insideBendalloyBlocks) {
            blockEntitySlowFactors.remove(be);
        }

        // Apply slowdown logic for Bendalloy block entities
        for (BlockEntity be : outsideBendalloyBlocks) {
            if (be.isRemoved())
                continue;
            try {
                double slowFactor = blockEntitySlowFactors.getOrDefault(be, 8.0D);
                tickSlowedBlockEntity(be, slowFactor, level);
            } catch (Exception e) {
                // Prevent any block-specific tick crashes from taking down the server
            }
        }

        // Clean up block entity dilation data map for positions no longer slowed
        java.util.Iterator<BlockPos> blockIterator = DILATED_BLOCK_ENTITIES.keySet().iterator();
        while (blockIterator.hasNext()) {
            BlockPos pos = blockIterator.next();
            boolean stillSlowed = false;
            for (BlockEntity be : outsideBendalloyBlocks) {
                if (be.getBlockPos().equals(pos)) {
                    stillSlowed = true;
                    break;
                }
            }
            if (!stillSlowed) {
                blockIterator.remove();
            }
        }

        // Process remaining Cadmium block entities
        for (BlockEntity be : outsideCadmiumBlocks) {
            if (be.isRemoved())
                continue;
            try {
                BlockPos pos = be.getBlockPos();
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof EntityBlock entityBlock) {
                    BlockEntityTicker<BlockEntity> ticker = (BlockEntityTicker<BlockEntity>) entityBlock
                            .getTicker(level, state, (BlockEntityType<BlockEntity>) be.getType());
                    if (ticker != null) {
                        int extraTicks = blockEntityExtraTicks.getOrDefault(be, 3);
                        for (int i = 0; i < extraTicks; i++) {
                            ticker.tick(level, pos, state, be);
                        }
                    }
                }
            } catch (Exception e) {
                // Prevent any block-specific tick crashes from taking down the server
            }
        }
    }

    public static void handleDeactivation(LivingEntity entity, MetalArtsData data, Metal metal) {
        if (entity instanceof ServerPlayer player) {
            if (metal == Metal.TIN) {
                int stage = data.savantStage(Metal.TIN);
                if (stage >= 2) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true));
                }
            } else if (metal == Metal.PEWTER) {
                int stage = data.savantStage(Metal.PEWTER);
                int burnDuration = data.pewterBurnDuration();
                if (burnDuration > 0) {
                    if (stage >= 2) {
                        int dragDuration = 1000 + (burnDuration * 2);
                        data.setPewterDragTicks(dragDuration);
                        player.addEffect(
                                new MobEffectInstance(ModEffects.PEWTER_DRAG.get(), dragDuration, 0, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dragDuration, 0, false, true));
                        player.addEffect(
                                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dragDuration, 0, false, true));
                    } else {
                        // Standard Pewter Drag: only if burned continuously for 2+ minutes (2400 ticks)
                        // OR if reserve is empty
                        boolean emptyReserve = data.getReserve(Metal.PEWTER) <= 0.01F;

                        // TODO: Only if flaring for that long maybe??
                        if (burnDuration > 2400 || emptyReserve) {
                            int dragDuration = Math.max(220, burnDuration * 2);
                            data.setPewterDragTicks(dragDuration);
                            player.addEffect(
                                    new MobEffectInstance(ModEffects.PEWTER_DRAG.get(), dragDuration, 0, false, true));
                            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dragDuration, 0, false, true));
                            player.addEffect(
                                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dragDuration, 0, false, true));
                        }
                    }
                    data.setPewterBurnDuration(0);
                }
            }
        }
    }

    private static void activateTimeBubbleStub(ServerPlayer player, MetalArtsData data, Metal metal) {
        if (data.bubbleCooldown() > 0) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.bubble_cooldown"), true);
            return;
        }

        double radius = (metal == Metal.CADMIUM) ? 6.4D : 3.2D;
        ACTIVE_BUBBLES.removeIf(b -> b.owner == player);

        ACTIVE_BUBBLES.add(new TimeBubble(player.blockPosition(), radius, metal, player));
        data.setBubbleCooldown(80);

        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F,
                1.3F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts." + metal.id() + "_bubble"),
                true);
    }

    public static double getProjectileGravity(Entity entity) {
        if (entity.isNoGravity()) {
            return 0.0D;
        }
        if (entity instanceof net.minecraft.world.entity.projectile.AbstractArrow) {
            return 0.05D;
        }
        if (entity instanceof net.minecraft.world.entity.projectile.ThrowableProjectile) {
            return 0.03D;
        }
        if (entity instanceof net.minecraft.world.entity.projectile.LlamaSpit) {
            return 0.06D;
        }
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
            return 0.04D;
        }
        if (entity instanceof net.minecraft.world.entity.item.FallingBlockEntity) {
            return 0.04D;
        }
        if (entity instanceof net.minecraft.world.entity.item.PrimedTnt) {
            return 0.04D;
        }
        return 0.0D;
    }

    public static double getProjectileDrag(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.projectile.AbstractArrow) {
            return 0.99D;
        }
        if (entity instanceof net.minecraft.world.entity.projectile.ThrowableProjectile) {
            return 0.99D;
        }
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
            return 0.98D;
        }
        if (entity instanceof net.minecraft.world.entity.item.FallingBlockEntity) {
            return 0.98D;
        }
        if (entity instanceof net.minecraft.world.entity.item.PrimedTnt) {
            return 0.98D;
        }
        return 0.99D;
    }
}
