package com.not_noah.mistborn_metal_arts.entity;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;

public class MetalbornEnemy extends Monster {
    private final MetalbornRole role;
    @Nullable
    private final ServerBossEvent bossEvent;
    private int powerCooldown;
    private int reinforcementCooldown = 220;

    public MetalbornEnemy(EntityType<? extends MetalbornEnemy> type, Level level, MetalbornRole role) {
        super(type, level);
        this.role = role;
        this.xpReward = role.isBoss() ? 80 : (role == MetalbornRole.KOLOSS ? 20 : 8);
        this.bossEvent = role.isBoss()
                ? new ServerBossEvent(Component.literal(role.displayName()), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)
                : null;
    }

    public MetalbornRole role() {
        return role;
    }

    public static AttributeSupplier.Builder createAttributes(MetalbornRole role) {
        double followRange = switch (role) {
            case TINEYE_SCOUT -> 40.0D;
            case SEEKER, STEEL_INQUISITOR -> 48.0D;
            case KANDRA -> 18.0D;
            default -> 28.0D;
        };
        double armor = switch (role) {
            case LURCHER_GUARD -> 7.0D;
            case KOLOSS -> 10.0D;
            case STEEL_INQUISITOR -> 12.0D;
            default -> 2.0D;
        };
        double knockbackResistance = switch (role) {
            case PEWTER_THUG -> 0.35D;
            case KOLOSS -> 0.65D;
            case STEEL_INQUISITOR -> 0.55D;
            default -> 0.05D;
        };
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, role.health())
                .add(Attributes.MOVEMENT_SPEED, role.speed())
                .add(Attributes.ATTACK_DAMAGE, role.attackDamage())
                .add(Attributes.FOLLOW_RANGE, followRange)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, role == MetalbornRole.KANDRA ? 0.7D : 1.0D, false));
        goalSelector.addGoal(6, new RandomStrollGoal(this, role == MetalbornRole.KANDRA ? 0.75D : 0.9D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, player -> role != MetalbornRole.KANDRA));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable net.minecraft.nbt.CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        setCustomName(Component.literal(role.displayName()));
        setPersistenceRequired();
        populateLoadout();
        if (role == MetalbornRole.KANDRA) {
            setTarget(null);
        }
        return data;
    }

    private void populateLoadout() {
        switch (role) {
            case COINSHOT_BANDIT -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_NUGGET));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }
            case LURCHER_GUARD -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            }
            case PEWTER_THUG, KOLOSS -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            case TINEYE_SCOUT, RIOTER, SOOTHER, SEEKER, SMOKER, ATIUM_SEER -> setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_NUGGET));
            case MISTBORN_ASSASSIN -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_NUGGET));
            }
            case STEEL_INQUISITOR -> {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.OBSIDIAN_AXE.get()));
                setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.OBSIDIAN_AXE.get()));
            }
            case KANDRA -> setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel serverLevel) || !isAlive()) {
            return;
        }
        if (role == MetalbornRole.KANDRA) {
            setTarget(null);
            addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false));
            return;
        }

        updateBossBar();
        if (powerCooldown > 0) {
            powerCooldown--;
        }
        if (reinforcementCooldown > 0) {
            reinforcementCooldown--;
        }

        applyPassiveEffects();
        if (powerCooldown <= 0) {
            useMetalbornPower(serverLevel);
        }
    }

    private void applyPassiveEffects() {
        switch (role) {
            case PEWTER_THUG -> {
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false));
            }
            case KOLOSS -> addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 1, true, false));
            case ATIUM_SEER -> addEffect(new MobEffectInstance(ModEffects.ATIUM_SIGHT.get(), 60, 0, true, true));
            case MISTBORN_ASSASSIN -> {
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false));
            }
            case STEEL_INQUISITOR -> {
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, getHealth() < getMaxHealth() * 0.45F ? 1 : 0, true, false));
                addEffect(new MobEffectInstance(ModEffects.HEMALURGIC_CORRUPTION.get(), 100, 2, true, true));
            }
            default -> {
            }
        }
    }

    private void useMetalbornPower(ServerLevel serverLevel) {
        Optional<ServerPlayer> target = nearestValidPlayer(serverLevel, role == MetalbornRole.TINEYE_SCOUT || role == MetalbornRole.SEEKER || role.isBoss() ? 28D : 16D);
        if (target.isEmpty()) {
            powerCooldown = 30;
            return;
        }
        ServerPlayer player = target.get();

        switch (role) {
            case COINSHOT_BANDIT -> steelPush(serverLevel, player, 0.82D);
            case LURCHER_GUARD -> ironPull(serverLevel, player, 0.55D);
            case TINEYE_SCOUT -> {
                setTarget(player);
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
                powerCooldown = 80;
            }
            case RIOTER -> riot(serverLevel, player);
            case SOOTHER -> soothe(player);
            case SEEKER -> seek(serverLevel, player);
            case SMOKER -> coppercloud(serverLevel);
            case ATIUM_SEER -> atiumFeint(serverLevel, player);
            case MISTBORN_ASSASSIN -> {
                if (tickCount % 2 == 0) {
                    steelPush(serverLevel, player, 0.92D);
                } else {
                    ironPull(serverLevel, player, 0.62D);
                }
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            }
            case STEEL_INQUISITOR -> {
                if (getHealth() < getMaxHealth() * 0.5F) {
                    atiumFeint(serverLevel, player);
                }
                if (tickCount % 3 == 0) {
                    steelPush(serverLevel, player, 1.12D);
                } else {
                    ironPull(serverLevel, player, 0.82D);
                }
                seek(serverLevel, player);
            }
            case PEWTER_THUG, KOLOSS, KANDRA -> powerCooldown = 55;
        }
    }

    private Optional<ServerPlayer> nearestValidPlayer(ServerLevel level, double range) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(blockPosition()).inflate(range),
                        player -> player.isAlive() && !player.isCreative() && !player.isSpectator())
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr));
    }

    private void steelPush(ServerLevel level, ServerPlayer player, double strength) {
        Vec3 direction = player.position().subtract(position());
        if (direction.lengthSqr() < 0.01D) {
            return;
        }
        Vec3 push = direction.normalize().scale(strength).add(0D, 0.16D, 0D);
        player.setDeltaMovement(player.getDeltaMovement().add(push));
        player.hurtMarked = true;
        drawLine(level, getEyePosition(), player.getEyePosition(), ModParticles.METAL_LINE.get());
        level.playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 0.55F, 1.45F);
        powerCooldown = role.isBoss() ? 35 : 65;
    }

    private void ironPull(ServerLevel level, ServerPlayer player, double strength) {
        Vec3 direction = position().subtract(player.position());
        if (direction.lengthSqr() < 0.01D) {
            return;
        }
        Vec3 pull = direction.normalize().scale(strength).add(0D, 0.05D, 0D);
        player.setDeltaMovement(player.getDeltaMovement().add(pull));
        player.hurtMarked = true;
        drawLine(level, getEyePosition(), player.getEyePosition(), ModParticles.METAL_LINE.get());
        level.playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.6F, 1.25F);
        powerCooldown = role.isBoss() ? 34 : 70;
    }

    private void riot(ServerLevel level, ServerPlayer player) {
        for (Monster monster : level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(12D), mob -> mob != this && mob.isAlive())) {
            monster.setTarget(player);
            monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
        }
        player.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_PRESSURE.get(), 100, 1));
        level.sendParticles(ModParticles.EMOTIONAL_WAVE.get(), player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.5, 0.5, 0.5, 0.01);
        drawLine(level, getEyePosition(), player.getEyePosition(), ModParticles.EMOTIONAL_WAVE.get());
        powerCooldown = 100;
    }

    private void soothe(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_PRESSURE.get(), 120, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        player.serverLevel().sendParticles(ModParticles.EMOTIONAL_WAVE.get(), player.getX(), player.getY() + 1.0, player.getZ(), 5, 0.4, 0.4, 0.4, 0.01);
        powerCooldown = 120;
    }

    private void seek(ServerLevel level, ServerPlayer player) {
        if (isBurningAnyMetal(player) || role.isBoss()) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
            setTarget(player);
            if (reinforcementCooldown <= 0 && role == MetalbornRole.SEEKER) {
                spawnReinforcement(level, player.blockPosition().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2));
                reinforcementCooldown = 420;
            }
        }
        level.sendParticles(ModParticles.BRONZE_PULSE.get(), getX(), getY() + 1.5, getZ(), 3, 0.1, 0.1, 0.1, 0.01);
        drawLine(level, getEyePosition(), player.getEyePosition(), ModParticles.BRONZE_PULSE.get());
        powerCooldown = role.isBoss() ? 70 : 110;
    }

    private boolean isBurningAnyMetal(ServerPlayer player) {
        return player.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> {
            for (Metal metal : Metal.cachedValues()) {
                if (data.isBurning(metal)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    private void coppercloud(ServerLevel level) {
        List<MetalbornEnemy> allies = level.getEntitiesOfClass(MetalbornEnemy.class, getBoundingBox().inflate(8D), LivingEntity::isAlive);
        for (MetalbornEnemy ally : allies) {
            ally.addEffect(new MobEffectInstance(ModEffects.COPPERCLOUD.get(), 120, 0, true, true));
        }
        for (int i = 0; i < 28; i++) {
            double ox = (random.nextDouble() - 0.5D) * 8.0D;
            double oy = random.nextDouble() * 2.6D;
            double oz = (random.nextDouble() - 0.5D) * 8.0D;
            level.sendParticles(ModParticles.COPPERCLOUD.get(), getX() + ox, getY() + oy, getZ() + oz, 1, 0D, 0D, 0D, 0D);
        }
        powerCooldown = 130;
    }

    private void atiumFeint(ServerLevel level, ServerPlayer player) {
        addEffect(new MobEffectInstance(ModEffects.ATIUM_SIGHT.get(), 120, 1, true, true));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
        drawLine(level, getEyePosition(), player.getEyePosition(), ModParticles.ATIUM_SHADOW.get());
        powerCooldown = role.isBoss() ? 45 : 95;
    }

    private void spawnReinforcement(ServerLevel level, net.minecraft.core.BlockPos pos) {
        MetalbornEnemy guard = (MetalbornEnemy) getType().create(level);
        if (guard == null) {
            return;
        }
        guard.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0F);
        guard.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.REINFORCEMENT, null, null);
        level.addFreshEntity(guard);
    }

    private void drawLine(ServerLevel level, Vec3 start, Vec3 end, net.minecraft.core.particles.ParticleOptions particle) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(4, Math.min(24, (int) (delta.length() * 2.0D)));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0D, 0D, 0D, 0D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        RandomSource randomSource = getRandom();
        if ((role == MetalbornRole.ATIUM_SEER || role == MetalbornRole.MISTBORN_ASSASSIN || role.isBoss()) && randomSource.nextFloat() < (role.isBoss() ? 0.22F : 0.14F)) {
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ModParticles.ATIUM_SHADOW.get(), getX(), getY() + getBbHeight() * 0.5D, getZ(), 12, 0.45D, 0.65D, 0.45D, 0.02D);
                level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.45F, 0.65F);
            }
            return false;
        }
        if (role.isBoss()) {
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ModParticles.HEMALURGIC_SPARK.get(), getX(), getY() + getBbHeight() * 0.5D, getZ(), 5, 0.2D, 0.4D, 0.2D, 0.05D);
            }
            if (amount > 18.0F) {
                amount = 18.0F;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (role.isBoss()) {
            spawnAtLocation(new ItemStack(ModItems.CHARGED_SPIKES.get(Metal.ATIUM).get()));
            spawnAtLocation(new ItemStack(ModItems.METAL_BEADS.get(Metal.ATIUM).get(), 2 + looting));
            spawnAtLocation(new ItemStack(ModItems.METAL_BEADS.get(Metal.LERASIUM).get()));
        }
        if (role == MetalbornRole.STEEL_INQUISITOR) {
            // Always drop one obsidian axe, chance for a second
            spawnAtLocation(new ItemStack(ModItems.OBSIDIAN_AXE.get()));
            if (random.nextFloat() < 0.35F + looting * 0.1F) {
                spawnAtLocation(new ItemStack(ModItems.OBSIDIAN_AXE.get()));
            }
        } else if (role == MetalbornRole.KOLOSS) {
            spawnAtLocation(new ItemStack(ModItems.CHARGED_SPIKES.get(Metal.PEWTER).get()));
        } else if (random.nextFloat() < 0.35F + looting * 0.1F) {
            spawnAtLocation(new ItemStack(ModItems.METAL_VIALS.get(Metal.PEWTER).get()));
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bossEvent != null) {
            bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossEvent != null) {
            bossEvent.removePlayer(player);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
        }
        super.remove(reason);
    }

    private void updateBossBar() {
        if (bossEvent != null) {
            bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !role.isBoss() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public void aiStep() {
        updateSwingTime();
        super.aiStep();
        if (!level().isClientSide && role.isBoss() && tickCount % 40 == 0) {
            gameEvent(GameEvent.ENTITY_ROAR);
        }
    }
}
