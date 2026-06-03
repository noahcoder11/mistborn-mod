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
    private final java.util.List<ItemStack> inventory = new java.util.ArrayList<>();
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

    public java.util.List<ItemStack> getMobInventory() {
        return inventory;
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
        
        goalSelector.addGoal(1, new com.not_noah.mistborn_metal_arts.entity.ai.DrinkVialGoal(this));
        goalSelector.addGoal(1, new com.not_noah.mistborn_metal_arts.entity.ai.BurnMetalGoal(this));
        goalSelector.addGoal(1, new com.not_noah.mistborn_metal_arts.entity.ai.TapMetalmindGoal(this));
        
        if (role == MetalbornRole.COINSHOT_BANDIT || role == MetalbornRole.MISTBORN_ASSASSIN || role == MetalbornRole.STEEL_INQUISITOR) {
            goalSelector.addGoal(2, new com.not_noah.mistborn_metal_arts.entity.ai.SteelPushGoal(this, role == MetalbornRole.STEEL_INQUISITOR ? 1.12D : 0.82D));
        }
        if (role == MetalbornRole.LURCHER_GUARD || role == MetalbornRole.MISTBORN_ASSASSIN || role == MetalbornRole.STEEL_INQUISITOR) {
            goalSelector.addGoal(2, new com.not_noah.mistborn_metal_arts.entity.ai.IronPullGoal(this, role == MetalbornRole.STEEL_INQUISITOR ? 0.82D : 0.55D));
        }
        if (role == MetalbornRole.RIOTER || role == MetalbornRole.SOOTHER) {
            goalSelector.addGoal(2, new com.not_noah.mistborn_metal_arts.entity.ai.EmotionalAllomancyGoal(this));
        }
        if (role == MetalbornRole.SEEKER || role == MetalbornRole.STEEL_INQUISITOR) {
            goalSelector.addGoal(2, new com.not_noah.mistborn_metal_arts.entity.ai.BronzeSeekGoal(this));
        }
        if (role == MetalbornRole.SMOKER) {
            goalSelector.addGoal(2, new com.not_noah.mistborn_metal_arts.entity.ai.CoppercloudGoal(this));
        }
        
        goalSelector.addGoal(3, new MeleeAttackGoal(this, role == MetalbornRole.KANDRA ? 0.7D : 1.0D, false));
        goalSelector.addGoal(6, new RandomStrollGoal(this, role == MetalbornRole.KANDRA ? 0.75D : 0.9D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, player -> role != MetalbornRole.KANDRA));
        
        if (role == MetalbornRole.KOLOSS) {
            targetSelector.addGoal(0, new com.not_noah.mistborn_metal_arts.entity.ai.KolossEmotionalControlGoal(this));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable net.minecraft.nbt.CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        setCustomName(Component.literal(role.displayName()));
        setPersistenceRequired();
        populateLoadout();
        initializePowers();
        if (role == MetalbornRole.KANDRA) {
            setTarget(null);
        }
        return data;
    }

    private void initializePowers() {
        getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setAllomancySnapped(true);
            data.clearAllomancy();
            data.clearFeruchemy();
            
            for (Metal m : Metal.cachedValues()) {
                data.setReserve(m, 0F);
                data.setMetalmindCharge(m, 0F);
            }

            switch (role) {
                case COINSHOT_BANDIT -> {
                    data.addNaturalAllomanticPower(Metal.STEEL);
                    data.setReserve(Metal.STEEL, 200F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.STEEL).get(), 2));
                }
                case LURCHER_GUARD -> {
                    data.addNaturalAllomanticPower(Metal.IRON);
                    data.setReserve(Metal.IRON, 250F);
                    data.addNaturalFeruchemicalPower(Metal.IRON);
                    data.setMetalmindCharge(Metal.IRON, 100F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.IRON).get(), 2));
                }
                case PEWTER_THUG -> {
                    data.addNaturalAllomanticPower(Metal.PEWTER);
                    data.setReserve(Metal.PEWTER, 300F);
                    data.addNaturalFeruchemicalPower(Metal.PEWTER);
                    data.setMetalmindCharge(Metal.PEWTER, 200F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.PEWTER).get(), 2));
                }
                case TINEYE_SCOUT -> {
                    data.addNaturalAllomanticPower(Metal.TIN);
                    data.setReserve(Metal.TIN, 150F);
                    data.addNaturalFeruchemicalPower(Metal.TIN);
                    data.setMetalmindCharge(Metal.TIN, 100F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.TIN).get(), 2));
                }
                case RIOTER -> {
                    data.addNaturalAllomanticPower(Metal.ZINC);
                    data.setReserve(Metal.ZINC, 200F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.ZINC).get(), 2));
                }
                case SOOTHER -> {
                    data.addNaturalAllomanticPower(Metal.BRASS);
                    data.setReserve(Metal.BRASS, 200F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.BRASS).get(), 2));
                }
                case SEEKER -> {
                    data.addNaturalAllomanticPower(Metal.BRONZE);
                    data.setReserve(Metal.BRONZE, 200F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.BRONZE).get(), 2));
                }
                case SMOKER -> {
                    data.addNaturalAllomanticPower(Metal.COPPER);
                    data.setReserve(Metal.COPPER, 250F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.COPPER).get(), 2));
                }
                case ATIUM_SEER -> {
                    data.addNaturalAllomanticPower(Metal.ATIUM);
                    data.setReserve(Metal.ATIUM, 100F);
                    data.addNaturalFeruchemicalPower(Metal.ATIUM);
                    data.setMetalmindCharge(Metal.ATIUM, 50F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.ATIUM).get(), 1));
                }
                case MISTBORN_ASSASSIN -> {
                    for (Metal m : Metal.cachedValues()) {
                        if (m.isAllomantic()) {
                            data.addNaturalAllomanticPower(m);
                            data.setReserve(m, 400F);
                        }
                    }
                    data.addNaturalFeruchemicalPower(Metal.PEWTER);
                    data.setMetalmindCharge(Metal.PEWTER, 200F);
                    data.addNaturalFeruchemicalPower(Metal.STEEL);
                    data.setMetalmindCharge(Metal.STEEL, 150F);
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.PEWTER).get(), 1));
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.STEEL).get(), 1));
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.IRON).get(), 1));
                }
                case STEEL_INQUISITOR -> {
                    for (Metal m : Metal.cachedValues()) {
                        if (m.isAllomantic()) {
                            data.addNaturalAllomanticPower(m);
                            data.setReserve(m, 800F);
                        }
                        if (m.isFeruchemical()) {
                            data.addNaturalFeruchemicalPower(m);
                            data.setMetalmindCharge(m, 300F);
                        }
                    }
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.PEWTER).get(), 2));
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.STEEL).get(), 2));
                    this.inventory.add(new ItemStack(ModItems.METAL_VIALS.get(Metal.ATIUM).get(), 1));
                }
                default -> {}
            }
        });
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
        if (reinforcementCooldown > 0) {
            reinforcementCooldown--;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean isBurningAtium = getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.METAL_ARTS).map(data -> {
            if (data.isBurning(Metal.ATIUM) && data.getReserve(Metal.ATIUM) > 0.0F) {
                data.consumeReserve(Metal.ATIUM, 5.0F);
                return true;
            }
            return false;
        }).orElse(false);

        if (isBurningAtium && getRandom().nextFloat() < (role.isBoss() ? 0.35F : 0.20F)) {
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
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.SHEARS) && player.isShiftKeyDown() && (role == MetalbornRole.KOLOSS || role == MetalbornRole.KANDRA)) {
            if (!level().isClientSide) {
                if (random.nextFloat() < 0.35F) {
                    level().playSound(null, blockPosition(), SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                    if (level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + getBbHeight() * 0.5D, getZ(), 8, 0.2D, 0.2D, 0.2D, 0.1D);
                    }
                    
                    ItemStack dropStack;
                    if (role == MetalbornRole.KOLOSS) {
                        dropStack = new ItemStack(ModItems.CHARGED_SPIKES.get(Metal.PEWTER).get());
                        hurt(damageSources().generic(), 30.0F);
                        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 3));
                        addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 3));
                    } else {
                        dropStack = new ItemStack(ModItems.CHARGED_SPIKES.get(Metal.TIN).get());
                        hurt(damageSources().generic(), 15.0F);
                        removeEffect(MobEffects.INVISIBILITY);
                    }
                    
                    spawnAtLocation(dropStack);
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                    player.displayClientMessage(Component.literal("You successfully extracted a Hemalurgic spike!"), true);
                } else {
                    level().playSound(null, blockPosition(), SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.6F);
                    player.displayClientMessage(Component.literal("You failed to grab the spike! Try again."), true);
                }
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        net.minecraft.nbt.ListTag invTag = new net.minecraft.nbt.ListTag();
        for (ItemStack stack : this.inventory) {
            invTag.add(stack.save(new net.minecraft.nbt.CompoundTag()));
        }
        tag.put("MobInventory", invTag);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.inventory.clear();
        if (tag.contains("MobInventory", 9)) {
            net.minecraft.nbt.ListTag invTag = tag.getList("MobInventory", 10);
            for (int i = 0; i < invTag.size(); i++) {
                ItemStack stack = ItemStack.of(invTag.getCompound(i));
                if (!stack.isEmpty()) {
                    this.inventory.add(stack);
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
            }
        }

        if (role.isBoss()) {
            spawnAtLocation(new ItemStack(ModItems.CHARGED_SPIKES.get(Metal.ATIUM).get()));
            spawnAtLocation(new ItemStack(ModItems.METAL_BEADS.get(Metal.ATIUM).get(), 2 + looting));
            spawnAtLocation(new ItemStack(ModItems.METAL_BEADS.get(Metal.LERASIUM).get()));
        }
        if (role == MetalbornRole.STEEL_INQUISITOR) {
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
