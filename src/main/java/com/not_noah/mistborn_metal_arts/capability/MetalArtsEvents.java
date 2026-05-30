package com.not_noah.mistborn_metal_arts.capability;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.allomancy.AllomancyManager;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.compat.CuriosCompat;
import com.not_noah.mistborn_metal_arts.feruchemy.FeruchemyManager;
import com.not_noah.mistborn_metal_arts.hemalurgy.HemalurgyManager;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import com.not_noah.mistborn_metal_arts.network.SyncStuckSpikesPacket;
import com.not_noah.mistborn_metal_arts.network.SyncBloodLevelPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import java.util.List;
import net.minecraft.world.phys.AABB;

import java.util.Random;

@Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MetalArtsEvents {
    private static final ResourceLocation CAPABILITY_ID = new ResourceLocation(MistbornMetalArts.MOD_ID, "metal_arts");
    private static final Random RANDOM = new Random();

    private MetalArtsEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MetalArtsData.class);
        event.register(IBloodData.class);
    }

    @Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(CAPABILITY_ID, new MetalArtsProvider());
            }
            if (event.getObject() instanceof net.minecraft.world.entity.LivingEntity) {
                event.addCapability(new ResourceLocation(MistbornMetalArts.MOD_ID, "blood_data"), new BloodDataProvider());
            }
        }

        @SubscribeEvent
        public static void playerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            serverPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isRestrained()) {
                    BlockPos pos = data.getRestrainedAltarPos();
                    BlockState state = serverPlayer.level().getBlockState(pos);
                    if (!data.isRestrainedByOthers() && serverPlayer.isShiftKeyDown()) {
                        data.setRestrained(false, null, 0);
                        serverPlayer.clearSleepingPos();
                        serverPlayer.setPose(net.minecraft.world.entity.Pose.STANDING);
                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.released_restraint"), true);
                        MetalArtsNetwork.sync(serverPlayer);
                        MetalArtsNetwork.syncStuckSpikes(serverPlayer);
                    } else if (!state.is(com.not_noah.mistborn_metal_arts.registry.ModBlocks.HEMALURGIC_ALTAR.get())) {
                        data.setRestrained(false, null, 0);
                        serverPlayer.clearSleepingPos();
                        serverPlayer.setPose(net.minecraft.world.entity.Pose.STANDING);
                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.released_restraint"), true);
                        MetalArtsNetwork.sync(serverPlayer);
                        MetalArtsNetwork.syncStuckSpikes(serverPlayer);
                    } else {
                        net.minecraft.core.Direction facing = state.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
                        BlockPos headPos = pos.relative(facing);
                        double centerX = headPos.getX() + 0.5D;
                        double centerZ = headPos.getZ() + 0.5D;
                        double seatY = 0.5625D;
                        
                        if (serverPlayer.distanceToSqr(centerX, headPos.getY() + seatY, centerZ) > 1.0D) {
                            serverPlayer.setPos(centerX, headPos.getY() + seatY, centerZ);
                            serverPlayer.hurtMarked = true;
                        }
                        serverPlayer.setDeltaMovement(0, 0, 0);
                        
                        // Keep player in standard standing pose on server to completely bypass vanilla's daytime bed check!
                        serverPlayer.setPose(net.minecraft.world.entity.Pose.STANDING);
                        serverPlayer.clearSleepingPos();
                    }
                }
                if (!data.allomancySnapped() && serverPlayer.getHealth() < 2.0F && serverPlayer.isAlive()) {
                    data.setAllomancySnapped(true);
                    
                    serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), 
                        SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.6F);
                    serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), 
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
                    
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Your spirit snaps under intense physical trauma! You have awakened your Allomantic powers!"
                    ).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.BOLD));
                    
                    serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                        net.minecraft.network.chat.Component.literal("Snapped!").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.BOLD)
                    ));
                    serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                        net.minecraft.network.chat.Component.literal("Your Allomantic powers awaken...").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                    ));
                    
                    if (serverPlayer.getServer() != null) {
                        net.minecraft.advancements.Advancement adv = serverPlayer.getServer().getAdvancements().getAdvancement(
                            new ResourceLocation("mistborn_metal_arts", "snap")
                        );
                        if (adv != null) {
                            serverPlayer.getAdvancements().award(adv, "snapped");
                        }
                    }
                    
                    MetalArtsNetwork.sync(serverPlayer);
                }

                refreshHemalurgicPowers(serverPlayer, data);
                
                // Apply physical Hemalurgic attributes
                float strengthBonus = data.getPhysicalStrengthBonus();
                net.minecraft.world.entity.ai.attributes.AttributeInstance attackInstance = serverPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                if (attackInstance != null) {
                    java.util.UUID STRENGTH_UUID = java.util.UUID.fromString("7f382a1c-9b8d-4e5f-a0c1-3d2e1f0a9b8c");
                    attackInstance.removeModifier(STRENGTH_UUID);
                    if (strengthBonus > 0) {
                        attackInstance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            STRENGTH_UUID, "Hemalurgic Strength", strengthBonus, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
                    }
                }
                
                float sightBonus = data.getPhysicalSightBonus();
                if (sightBonus > 0.0F) {
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 240, 0, false, false, false));
                }
                if (sightBonus >= 0.8F) {
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 240, 0, false, false, false));
                }
                if (sightBonus >= 1.5F) {
                    if (serverPlayer.tickCount % 20 == 0) {
                        java.util.List<net.minecraft.world.entity.LivingEntity> entities = serverPlayer.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, serverPlayer.getBoundingBox().inflate(16.0D * sightBonus));
                        for (net.minecraft.world.entity.LivingEntity e : entities) {
                            if (e != serverPlayer && !e.isAlliedTo(serverPlayer)) {
                                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 30, 0, false, false, false));
                            }
                        }
                    }
                }

                AllomancyManager.tick(serverPlayer, data);
                FeruchemyManager.tick(serverPlayer, data);
                HemalurgyManager.tick(serverPlayer, data);
                com.not_noah.mistborn_metal_arts.hemalurgy.SoulStabilityManager.tick(serverPlayer, data);
                com.not_noah.mistborn_metal_arts.hemalurgy.IdentityContaminationManager.tick(serverPlayer, data);
                com.not_noah.mistborn_metal_arts.hemalurgy.SavantismManager.tick(serverPlayer, data);
            });
        }

        @SubscribeEvent
        public static void onLivingJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    if (data.isBurning(Metal.STEEL) && data.savantStage(Metal.STEEL) >= 3) {
                        if (player.isShiftKeyDown()) {
                            net.minecraft.nbt.CompoundTag persist = player.getPersistentData();
                            long lastDashTime = persist.getLong("LastSteelDashTime");
                            long currentTime = player.level().getGameTime();
                            if (currentTime - lastDashTime >= 60L) {
                                persist.putLong("LastSteelDashTime", currentTime);
                                
                                net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                                float strength = data.getEffectiveStrength(Metal.STEEL);
                                float flareMult = data.isFlaring(Metal.STEEL) ? 1.8F : 1.0F;
                                double speed = 1.2D * strength * flareMult;
                                
                                player.setDeltaMovement(look.x * speed, Math.max(0.4D, look.y * speed), look.z * speed);
                                player.hurtMarked = true;
                                
                                player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
                                if (player.level() instanceof ServerLevel serverLevel) {
                                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.05D);
                                }
                            }
                        }
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
            net.minecraft.world.entity.LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide || entity instanceof Player) {
                return;
            }

            net.minecraft.nbt.CompoundTag persistentData = entity.getPersistentData();
            if (persistentData.getBoolean("RestrainedAltar")) {
                BlockPos pos = BlockPos.of(persistentData.getLong("RestrainedAltarPos"));
                BlockState state = entity.level().getBlockState(pos);

                if (!state.is(com.not_noah.mistborn_metal_arts.registry.ModBlocks.HEMALURGIC_ALTAR.get())) {
                    persistentData.putBoolean("RestrainedAltar", false);
                    entity.clearSleepingPos();
                    entity.setPose(net.minecraft.world.entity.Pose.STANDING);
                    MetalArtsNetwork.syncStuckSpikes(entity);
                } else {
                    net.minecraft.core.Direction facing = state.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
                    BlockPos headPos = pos.relative(facing);
                    double centerX = headPos.getX() + 0.5D;
                    double centerZ = headPos.getZ() + 0.5D;
                    double seatY = 0.85D;

                    if (entity.distanceToSqr(centerX, headPos.getY() + seatY, centerZ) > 0.01D) {
                        entity.setPos(centerX, headPos.getY() + seatY, centerZ);
                        entity.hurtMarked = true;
                    }
                    entity.setDeltaMovement(0, 0, 0);

                    float yaw = facing.toYRot();
                    entity.setYRot(yaw);
                    entity.setXRot(0.0F);
                    entity.yRotO = yaw;
                    entity.xRotO = 0.0F;
                    entity.setYBodyRot(yaw);
                    entity.setYHeadRot(yaw);
                    
                    // Keep mob in standard standing pose on server as well for a completely unified, bug-free client render experience
                    entity.setPose(net.minecraft.world.entity.Pose.STANDING);
                    entity.clearSleepingPos();
                }
            }
        }

        private static void refreshHemalurgicPowers(ServerPlayer player, MetalArtsData data) {
            if (data.needsPowerRefresh()) {
                data.refreshPowers();
                CuriosCompat.refreshEquippedHemalurgicSpikes(player, data);
                MetalArtsNetwork.sync(player);
            }
        }

        @SubscribeEvent
        public static void clone(PlayerEvent.Clone event) {
            Player oldPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();
            
            oldPlayer.reviveCaps();
            oldPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(oldData -> {
                if (event.isWasDeath()) {
                    oldData.stopAllBurning();
                    for (Metal metal : Metal.cachedValues()) {
                        oldData.stopFeruchemy(metal);
                    }
                    oldData.setRestrained(false, null, 0);
                }

                newPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                });
                
                if (event.isWasDeath() && newPlayer instanceof ServerPlayer newServerPlayer) {
                    for (Metal metal : Metal.cachedValues()) {
                        FeruchemyManager.cleanupModifiersAndEffects(newServerPlayer, metal, 0);
                    }
                }
            });
            oldPlayer.invalidateCaps();
        }

        @SubscribeEvent
        public static void onEntityInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
            Player player = event.getEntity();
            ItemStack stack = event.getItemStack();
            if (stack.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
                Entity target = event.getTarget();
                if (target instanceof LivingEntity living) {
                    living.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(bloodData -> {
                        if (bloodData.getBloodLevel() > 0.0F) {
                            if (!event.getLevel().isClientSide) {
                                bloodData.setBloodLevel(Math.max(0.0F, bloodData.getBloodLevel() - 0.25F));
                                
                                ItemStack bloodVial = new ItemStack(com.not_noah.mistborn_metal_arts.registry.ModItems.BLOOD_VIAL.get());
                                bloodVial.getOrCreateTag().putLong("HarvestTime", event.getLevel().getGameTime());
                                
                                if (!player.getAbilities().instabuild) {
                                    stack.shrink(1);
                                }
                                
                                if (stack.isEmpty()) {
                                    player.setItemInHand(event.getHand(), bloodVial);
                                } else {
                                    if (!player.getInventory().add(bloodVial)) {
                                        player.drop(bloodVial, false);
                                    }
                                }
                                
                                player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                                com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork.syncBloodLevel(living, bloodData.getBloodLevel());
                            }
                            event.setCanceled(true);
                            event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(event.getLevel().isClientSide));
                        }
                    });
                }
            }
        }

        @SubscribeEvent
        public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                rollFirstJoinPowers(data);
                MetalArtsNetwork.sync(player);
                // Purge active modifiers just in case to avoid ghost values on login
                for (Metal metal : Metal.cachedValues()) {
                    if (metal.isFeruchemical() && data.feruchemyMode(metal) == 0) {
                        FeruchemyManager.cleanupModifiersAndEffects(player, metal, 0);
                    }
                }
            });
        }

        @SubscribeEvent
        public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MetalArtsNetwork.sync(player);
            }
        }

        @SubscribeEvent
        public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MetalArtsNetwork.sync(player);
            }
        }

        @SubscribeEvent
        public static void startTracking(PlayerEvent.StartTracking event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MetalArtsNetwork.sync(player);
                
                Entity target = event.getTarget();
                if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                    living.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                        MetalArtsNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncStuckSpikesPacket(living, data.getStuckSpikes()));
                        MetalArtsNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncBloodLevelPacket(living.getId(), data.getBloodLevel()));
                    });
                }
            }
        }

        @SubscribeEvent
        public static void hurt(LivingHurtEvent event) {
            if (event.getEntity() instanceof Player player && AllomancyManager.isProtectedByPewter(player)) {
                float factor = player.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> {
                    float strength = data.getEffectiveStrength(Metal.PEWTER);
                    float flareMult = data.isFlaring(Metal.PEWTER) ? 1.5F : 1.0F;
                    float reduction = 0.9F - (0.3F * strength * flareMult);
                    return Math.max(0.35F, reduction);
                }).orElse(0.75F);
                event.setAmount(event.getAmount() * factor);
            }
            if (event.getEntity() instanceof Player player && AllomancyManager.isBurning(player, Metal.ATIUM)) {
                event.setCanceled(true);
                
                net.minecraft.world.damagesource.DamageSource source = event.getSource();
                net.minecraft.world.phys.Vec3 damagePos = source.getSourcePosition() != null ? source.getSourcePosition() : (source.getEntity() != null ? source.getEntity().position() : player.position());
                net.minecraft.world.phys.Vec3 playerPos = player.position();
                
                net.minecraft.world.phys.Vec3 incoming = playerPos.subtract(damagePos).normalize();
                net.minecraft.world.phys.Vec3 lateral = new net.minecraft.world.phys.Vec3(-incoming.z, 0D, incoming.x).normalize();
                double dodgeDist = 1.6D;
                
                if (player.getRandom().nextBoolean()) {
                    lateral = lateral.scale(-1D);
                }
                
                double tx = playerPos.x + lateral.x * dodgeDist;
                double ty = playerPos.y;
                double tz = playerPos.z + lateral.z * dodgeDist;
                
                if (player.level().getBlockState(new BlockPos((int) tx, (int) ty, (int) tz)).isAir()) {
                    player.teleportTo(tx, ty, tz);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.3F);
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
                    }
                }
                return;
            }
            if (event.getSource().getDirectEntity() != null) {
                net.minecraft.world.entity.Entity proj = event.getSource().getDirectEntity();
                if (proj.getPersistentData().contains("MistbornMetalArtsShot")) {
                    float strength = proj.getPersistentData().getFloat("ShotStrength");
                    float flare = proj.getPersistentData().getFloat("ShotFlare");
                    float customDmg = 4.0F + (6.0F * strength * flare);
                    event.setAmount(customDmg);
                }
            }

            // Raysium Allomantic siphoning on hit
            if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
                attacker.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(attackerData -> {
                    // Raysium burn siphoning
                    if (attackerData.isBurning(Metal.RAYSIUM)) {
                        event.setAmount(event.getAmount() + 2.0F);
                        attacker.heal(2.0F);
                        // Drain target's reserves if they have metal arts
                        if (event.getEntity() instanceof ServerPlayer targetPlayer) {
                            targetPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(targetData -> {
                                for (Metal m : targetData.burningMetals()) {
                                    targetData.consumeReserve(m, 10.0F);
                                }
                            });
                        }
                        // Restore attacker's reserves
                        for (Metal m : attackerData.burningMetals()) {
                            attackerData.fillReserve(m, 5.0F);
                        }
                    }

                    // Raysium spike siphoning — restore strength to other spikes
                    boolean hasRaysiumSpike = attackerData.installedSpikes().stream()
                            .anyMatch(s -> s.spikeMetal() == Metal.RAYSIUM);
                    if (hasRaysiumSpike && !attackerData.installedSpikes().isEmpty()) {
                        java.util.List<MetalArtsData.InstalledSpike> spikes = attackerData.installedSpikes();
                        if (attacker.getRandom().nextFloat() < 0.15F) {
                            // 15% chance to restore spike strength on hit
                            int idx = attacker.getRandom().nextInt(spikes.size());
                            MetalArtsData.InstalledSpike spike = spikes.get(idx);
                            if (spike.strength() < 1.0F) {
                                attackerData.updateSpikeStrength(idx, Math.min(1.0F, spike.strength() + 0.01F));
                            }
                        }
                    }
                });
            }
        }

        @SubscribeEvent
        public static void damage(LivingDamageEvent event) {
            if (event.getEntity() instanceof Player player && AllomancyManager.tryPewterSurvival(player, event.getAmount())) {
                event.setAmount(Math.max(0F, player.getHealth() - 1F));
            }
        }

        @SubscribeEvent
        public static void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) {
                return;
            }
            entity.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                java.util.List<StuckSpike> spikes = data.getStuckSpikes();
                if (spikes != null && !spikes.isEmpty()) {
                    for (StuckSpike spike : spikes) {
                        ItemStack spikeStack;
                        if (spike.isCharged()) {
                            spikeStack = new ItemStack(com.not_noah.mistborn_metal_arts.registry.ModItems.CHARGED_SPIKES.get(spike.getMetal()).get());
                            spikeStack.getOrCreateTag().putString("PowerType", spike.getPowerType());
                            spikeStack.getOrCreateTag().putString("PowerMetal", spike.getPowerMetal().id());
                            spikeStack.getOrCreateTag().putFloat("Strength", spike.getStrength());
                        } else {
                            spikeStack = new ItemStack(com.not_noah.mistborn_metal_arts.registry.ModItems.SPIKE_BLANKS.get(spike.getMetal()).get());
                        }
                        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                            entity.level(), entity.getX(), entity.getY() + 0.5D, entity.getZ(), spikeStack
                        );
                        event.getDrops().add(itemEntity);
                    }
                    data.setStuckSpikes(new java.util.ArrayList<>());
                }
            });
        }

        @SubscribeEvent
        public static void onLivingAttack(LivingAttackEvent event) {
            net.minecraft.world.entity.LivingEntity entity = event.getEntity();
            if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
                boolean restrained = false;
                if (entity instanceof Player player) {
                    var cap = player.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                    if (cap != null && cap.isRestrained()) {
                        restrained = true;
                    }
                } else {
                    if (entity.getPersistentData().getBoolean("RestrainedAltar")) {
                        restrained = true;
                    }
                }
                if (restrained) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void fall(LivingFallEvent event) {
            if (event.getEntity() instanceof Player player && AllomancyManager.isBurning(player, Metal.STEEL)) {
                event.setDamageMultiplier(event.getDamageMultiplier() * 0.25F);
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    if (data.isBurning(com.not_noah.mistborn_metal_arts.api.Metal.PEWTER) && data.savantStage(com.not_noah.mistborn_metal_arts.api.Metal.PEWTER) >= 3) {
                        event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
                    }
                    event.setDamageMultiplier(FeruchemyManager.adjustFallDamage(player, data, event.getDamageMultiplier()));
                });
            }
        }

        @SubscribeEvent
        public static void onServerChat(ServerChatEvent event) {
            ServerPlayer sender = event.getPlayer();
            if (sender.hasEffect(ModEffects.EMOTIONAL_RIOT.get())) {
                String rawText = event.getRawText();
                String angryMessage = rawText.toUpperCase(java.util.Locale.ROOT);
                if (!angryMessage.endsWith("!!!")) {
                    angryMessage += "!!!";
                }
                String[] angryTags = {
                    " BY THE LORD RULER!",
                    " IN THE NAME OF RUIN!",
                    " RUSTS AND RUINS!",
                    " YOU WORTHLESS PIECE OF SPENT METAL!"
                };
                angryMessage += angryTags[sender.getRandom().nextInt(angryTags.length)];
                event.setMessage(net.minecraft.network.chat.Component.literal("<" + sender.getDisplayName().getString() + "> " + angryMessage));
            }
        }

        @SubscribeEvent
        public static void breakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();
            if (AllomancyManager.isBurning(player, Metal.PEWTER)) {
                event.setNewSpeed(event.getNewSpeed() * 1.25F);
            }
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isTapping(Metal.PEWTER)) {
                    int tapLevel = data.feruchemyMode(Metal.PEWTER);
                    if (player.getMainHandItem().isEmpty()) {
                        BlockState state = event.getState();
                        BlockPos pos = event.getPosition().orElse(BlockPos.ZERO);
                        if (state.getDestroySpeed(player.level(), pos) > 0.0F) {
                            event.setNewSpeed(Math.max(event.getNewSpeed(), 8.0F * tapLevel));
                        }
                    } else {
                        event.setNewSpeed(event.getNewSpeed() * (1.0F + 1.5F * tapLevel));
                    }
                }
            });
        }

        @SubscribeEvent
        public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
            Player player = event.getEntity();
            if (player.getMainHandItem().isEmpty()) {
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    if (data.isTapping(Metal.PEWTER) && data.feruchemyMode(Metal.PEWTER) >= 2) {
                        event.setCanHarvest(true);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onCriticalHit(CriticalHitEvent event) {
            event.getEntity().getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isTapping(Metal.CHROMIUM)) {
                    event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
                }
            });
        }

        @SubscribeEvent
        public static void onLootingLevel(LootingLevelEvent event) {
            if (event.getDamageSource().getEntity() instanceof Player player) {
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    if (data.isTapping(Metal.CHROMIUM)) {
                        int tapLevel = data.feruchemyMode(Metal.CHROMIUM);
                        event.setLootingLevel(event.getLootingLevel() + tapLevel * 2);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onPickupXp(PlayerXpEvent.PickupXp event) {
            Player player = event.getEntity();
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isStoring(Metal.ZINC)) {
                    net.minecraft.world.entity.ExperienceOrb orb = event.getOrb();
                    try {
                        java.lang.reflect.Field valueField;
                        try {
                            valueField = net.minecraft.world.entity.ExperienceOrb.class.getDeclaredField("value");
                        } catch (NoSuchFieldException e) {
                            valueField = net.minecraft.world.entity.ExperienceOrb.class.getDeclaredField("f_20770_");
                        }
                        valueField.setAccessible(true);
                        int currentVal = valueField.getInt(orb);
                        valueField.setInt(orb, currentVal / 2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @SubscribeEvent
        public static void onLivingKnockback(LivingKnockBackEvent event) {
            if (event.getEntity() instanceof Player player) {
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    if (data.isStoring(Metal.IRON)) {
                        event.setStrength(event.getStrength() * 1.8F);
                    } else if (data.isTapping(Metal.IRON)) {
                        int tapLevel = data.feruchemyMode(Metal.IRON);
                        float factor = Math.max(0.0F, 1.0F - 0.22F * tapLevel);
                        event.setStrength(event.getStrength() * factor);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                AllomancyManager.tickWorldBubbles(serverLevel);
            }
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            Player player = event.getPlayer();
            if (player != null && !player.level().isClientSide()) {
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    if (data.isTapping(Metal.CHROMIUM)) {
                        // High luck block break: small chance to drop extra item
                        int tapLevel = data.feruchemyMode(Metal.CHROMIUM);
                        if (player.getRandom().nextFloat() < 0.15F * tapLevel) {
                            BlockState state = event.getState();
                            // Spawn duplicate block item as a fun bonus drop
                            net.minecraft.world.item.ItemStack drop = new net.minecraft.world.item.ItemStack(state.getBlock().asItem());
                            if (!drop.isEmpty()) {
                                BlockPos pos = event.getPos();
                                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                                    player.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop
                                );
                                player.level().addFreshEntity(itemEntity);
                            }
                        }
                    }
                });
            }
        }

        private static void rollFirstJoinPowers(MetalArtsData data) {
            if (data.firstJoinRollComplete() || !ServerConfig.VALUES.randomPowersOnFirstJoin.get()) {
                return;
            }
            data.setFirstJoinRollComplete(true);

            double mistbornChance = ServerConfig.VALUES.mistbornChance.get();
            if (mistbornChance <= 0.0) {
                mistbornChance = 0.15; // Default to 15% if set to 0 or disabled
            }
            double fullFeruchemistChance = ServerConfig.VALUES.fullFeruchemistChance.get();
            if (fullFeruchemistChance <= 0.0) {
                fullFeruchemistChance = 0.05; // Default to 5% if set to 0 or disabled
            }

            double roll = RANDOM.nextDouble();
            if (roll < mistbornChance) {
                // Spawn as a Mistborn with reduced power (30% to 70%)
                data.setMistborn();
                float strength = 0.3F + RANDOM.nextFloat() * 0.4F;
                data.setAllomanticStrength(strength);
                data.setAllomancySnapped(false);
            } else if (roll < mistbornChance + fullFeruchemistChance) {
                // Spawn as a Full Feruchemist with reduced power (30% to 70%)
                data.clearAllomancy();
                data.setFullFeruchemist();
                float strength = 0.3F + RANDOM.nextFloat() * 0.4F;
                data.setAllomanticStrength(strength);
                data.setAllomancySnapped(true);
            } else {
                // Spawn as a Twinborn of random powers (1 Misting allomantic + 1 Ferring feruchemical) with reduced power (30% to 70%)
                data.clearAllomancy();
                data.clearFeruchemy();
                data.setMisting(randomAllomanticMetal());
                data.setFerring(randomFeruchemicalMetal());
                float strength = 0.3F + RANDOM.nextFloat() * 0.4F;
                data.setAllomanticStrength(strength);
                data.setAllomancySnapped(false);
            }
        }

        private static Metal randomAllomanticMetal() {
            Metal[] values = Metal.cachedValues();
            Metal metal;
            do {
                metal = values[RANDOM.nextInt(values.length)];
            } while (!metal.isAllomantic() || metal == Metal.ATIUM);
            return metal;
        }

        private static Metal randomFeruchemicalMetal() {
            Metal[] values = Metal.cachedValues();
            Metal metal;
            do {
                metal = values[RANDOM.nextInt(values.length)];
            } while (!metal.isFeruchemical());
            return metal;
        }

        @SubscribeEvent
        public static void onEntityInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific event) {
            Entity target = event.getTarget();
            Player attacker = event.getEntity();
            ItemStack stack = event.getItemStack();
            net.minecraft.world.InteractionHand hand = event.getHand();
            net.minecraft.world.level.Level level = event.getLevel();

            if (stack.getItem() instanceof com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem spike) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide));

                if (!level.isClientSide && attacker instanceof ServerPlayer serverAttacker && target instanceof net.minecraft.world.entity.LivingEntity victim) {
                    // Precise raycast hit calculation to prevent spikes always landing at (0,0,0)/feet
                    net.minecraft.world.phys.Vec3 eyePos = attacker.getEyePosition(1.0F);
                    net.minecraft.world.phys.Vec3 lookVec = attacker.getLookAngle();
                    net.minecraft.world.phys.Vec3 reachEnd = eyePos.add(lookVec.scale(6.0D));
                    net.minecraft.world.phys.AABB targetBB = victim.getBoundingBox().inflate(0.1D);
                    java.util.Optional<net.minecraft.world.phys.Vec3> clipResult = targetBB.clip(eyePos, reachEnd);
                    net.minecraft.world.phys.Vec3 hitPos;
                    if (clipResult.isPresent()) {
                        hitPos = clipResult.get().subtract(victim.position());
                    } else {
                        hitPos = event.getLocalPos();
                    }

                    performSpikingRitual(serverAttacker, victim, spike, stack, hand, victim.blockPosition(), hitPos);
                }
                return;
            }

            if (level.isClientSide || !(attacker instanceof ServerPlayer serverAttacker)) {
                return;
            }

            if (!(target instanceof net.minecraft.world.entity.LivingEntity victim)) {
                return;
            }

            // Handle empty-hand or non-spike right clicks to retrieve stuck spikes OR bind/unbind mobs and players
            if (true) {
                boolean[] retrieved = {false};
                victim.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                    java.util.List<StuckSpike> spikes = data.getStuckSpikes();
                    if (spikes != null && !spikes.isEmpty()) {
                        net.minecraft.world.phys.Vec3 clickPos = event.getLocalPos();
                        double rx = clickPos.x;
                        double rz = clickPos.z;
                        float yawRad = (float) Math.toRadians(-victim.yBodyRot);
                        double cx = rx * Math.cos(yawRad) - rz * Math.sin(yawRad);
                        double cy = clickPos.y;
                        double cz = rx * Math.sin(yawRad) + rz * Math.cos(yawRad);

                        StuckSpike closest = null;
                        double minDist = Double.MAX_VALUE;
                        for (StuckSpike s : spikes) {
                            double ds = (s.getOx() - cx) * (s.getOx() - cx) + 
                                        (s.getOy() - cy) * (s.getOy() - cy) + 
                                        (s.getOz() - cz) * (s.getOz() - cz);
                            if (ds < minDist) {
                                minDist = ds;
                                closest = s;
                            }
                        }
                        
                        if (closest != null) {
                            data.getStuckSpikes().remove(closest);
                            MetalArtsNetwork.syncStuckSpikes(victim);
                            
                            ItemStack returnedSpike;
                            if (closest.isCharged()) {
                                returnedSpike = new ItemStack(com.not_noah.mistborn_metal_arts.registry.ModItems.CHARGED_SPIKES.get(closest.getMetal()).get());
                                returnedSpike.getOrCreateTag().putString("PowerType", closest.getPowerType());
                                returnedSpike.getOrCreateTag().putString("PowerMetal", closest.getPowerMetal().id());
                                returnedSpike.getOrCreateTag().putFloat("Strength", closest.getStrength());
                            } else {
                                returnedSpike = new ItemStack(com.not_noah.mistborn_metal_arts.registry.ModItems.SPIKE_BLANKS.get(closest.getMetal()).get());
                            }
                            
                            if (!serverAttacker.getInventory().add(returnedSpike)) {
                                serverAttacker.drop(returnedSpike, false);
                            }
                            
                            if (closest.isCharged() && !(victim instanceof Player)) {
                                victim.discard();
                                level.playSound(null, victim.blockPosition(), SoundEvents.WITHER_DEATH, net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 0.7F);
                            } else {
                                level.playSound(null, victim.blockPosition(), SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                            }
                            retrieved[0] = true;
                        }
                    }
                });

                if (retrieved[0]) {
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
                    return;
                }

                // Fall back to original altar bind/unbind logic
                boolean isRestrained = false;
                BlockPos restrainedAltar = null;
                
                if (victim instanceof Player victimPlayer) {
                    var vCap = victimPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                    if (vCap != null && vCap.isRestrained()) {
                        isRestrained = true;
                        restrainedAltar = vCap.getRestrainedAltarPos();
                    }
                } else {
                    net.minecraft.nbt.CompoundTag vNbt = victim.getPersistentData();
                    if (vNbt.getBoolean("RestrainedAltar")) {
                        isRestrained = true;
                        restrainedAltar = BlockPos.of(vNbt.getLong("RestrainedAltarPos"));
                    }
                }

                if (isRestrained) {
                    if (attacker.isShiftKeyDown()) {
                        if (victim instanceof Player victimPlayer) {
                            var vCap = victimPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                            if (vCap != null && victimPlayer instanceof ServerPlayer serverVictim) {
                                vCap.setRestrained(false, null, 0);
                                victimPlayer.clearSleepingPos();
                                victimPlayer.setPose(net.minecraft.world.entity.Pose.STANDING);
                                attacker.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.released_mob"), true);
                                victimPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.released_restraint"), true);
                                MetalArtsNetwork.sync(serverVictim);
                                MetalArtsNetwork.syncStuckSpikes(serverVictim);
                            }
                        } else {
                            net.minecraft.nbt.CompoundTag vNbt = victim.getPersistentData();
                            vNbt.putBoolean("RestrainedAltar", false);
                            victim.clearSleepingPos();
                            victim.setPose(net.minecraft.world.entity.Pose.STANDING);
                            attacker.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.released_mob"), true);
                            MetalArtsNetwork.syncStuckSpikes(victim);
                        }
                        level.playSound(null, restrainedAltar, SoundEvents.IRON_TRAPDOOR_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.8F);
                        event.setCanceled(true);
                        event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
                        return;
                    }
                } else {
                    BlockPos victimPos = victim.blockPosition();
                    BlockPos foundAltarFoot = null;
                    BlockState altarState = null;
                    if (level.getBlockState(victimPos).is(com.not_noah.mistborn_metal_arts.registry.ModBlocks.HEMALURGIC_ALTAR.get())) {
                        foundAltarFoot = com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.getFootPos(level.getBlockState(victimPos), victimPos);
                        altarState = level.getBlockState(foundAltarFoot);
                    } else if (level.getBlockState(victimPos.below()).is(com.not_noah.mistborn_metal_arts.registry.ModBlocks.HEMALURGIC_ALTAR.get())) {
                        foundAltarFoot = com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.getFootPos(level.getBlockState(victimPos.below()), victimPos.below());
                        altarState = level.getBlockState(foundAltarFoot);
                    }

                    if (foundAltarFoot != null) {
                        boolean occupied = false;
                        List<net.minecraft.world.entity.LivingEntity> occupants = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new AABB(foundAltarFoot).expandTowards(altarState.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING).getStepX(), 0.0D, altarState.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING).getStepZ()).inflate(0.5D));
                        for (net.minecraft.world.entity.LivingEntity e : occupants) {
                            if (e instanceof Player p) {
                                var cap = p.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                                if (cap != null && cap.isRestrained() && foundAltarFoot.equals(cap.getRestrainedAltarPos())) {
                                    occupied = true;
                                    break;
                                }
                            } else {
                                var nbt = e.getPersistentData();
                                if (nbt.getBoolean("RestrainedAltar") && BlockPos.of(nbt.getLong("RestrainedAltarPos")).equals(foundAltarFoot)) {
                                    occupied = true;
                                    break;
                                }
                            }
                        }

                        if (!occupied) {
                            if (victim instanceof Player victimPlayer) {
                                var vCap = victimPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                                if (vCap != null && victimPlayer instanceof ServerPlayer serverVictim) {
                                    net.minecraft.core.Direction facing = altarState.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
                                    BlockPos headPos = foundAltarFoot.relative(facing);
                                    double centerX = headPos.getX() + 0.5D;
                                    double centerZ = headPos.getZ() + 0.5D;

                                    vCap.setRestrained(true, foundAltarFoot, 0);
                                    vCap.setRestrainedByOthers(true);

                                    victimPlayer.setPos(centerX, headPos.getY() + 0.5625D, centerZ);
                                    victimPlayer.setDeltaMovement(0, 0, 0);
                                    victimPlayer.hurtMarked = true;

                                    float yaw = facing.toYRot();
                                    victimPlayer.setYRot(yaw);
                                    victimPlayer.setXRot(0.0F);
                                    victimPlayer.setYBodyRot(yaw);
                                    victimPlayer.setYHeadRot(yaw);

                                    attacker.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.mob_restrained"), true);
                                    victimPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.restrained_altar"), true);
                                    MetalArtsNetwork.sync(serverVictim);
                                    MetalArtsNetwork.syncStuckSpikes(serverVictim);
                                }
                            } else {
                                net.minecraft.nbt.CompoundTag vNbt = victim.getPersistentData();
                                vNbt.putBoolean("RestrainedAltar", true);
                                vNbt.putLong("RestrainedAltarPos", foundAltarFoot.asLong());
                                vNbt.putInt("RestrainedAltarSeat", 0);
                                attacker.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.mob_restrained"), true);
                                MetalArtsNetwork.syncStuckSpikes(victim);
                            }

                            level.playSound(null, foundAltarFoot, SoundEvents.WOOD_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.8F);
                            event.setCanceled(true);
                            event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
                            return;
                        } else {
                            attacker.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.altar_full"), true);
                        }
                    }
                }
            }
        }

        private static class BindPoint {
            final String name;
            final Metal powerMetal;
            final String powerType; // "allomancy" or "feruchemy"
            final double x, y, z;

            BindPoint(String name, Metal powerMetal, String powerType, double x, double y, double z) {
                this.name = name;
                this.powerMetal = powerMetal;
                this.powerType = powerType;
                this.x = x;
                this.y = y;
                this.z = z;
            }

            double distanceSq(double px, double py, double pz) {
                return (px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z);
            }
        }

        public static void performSpikingRitual(ServerPlayer attacker, net.minecraft.world.entity.LivingEntity victim, com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem spikeItem, ItemStack spikeStack, net.minecraft.world.InteractionHand hand, BlockPos altarPos, net.minecraft.world.phys.Vec3 hitPos) {
            ServerLevel serverLevel = (ServerLevel) victim.level();
            Metal spikeMetal = spikeItem.metal();

            // 1. Spiking with a CHARGED spike -> Insert it into the victim's body
            if (spikeItem.charged()) {
                victim.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(vData -> {
                    CompoundTag tag = spikeStack.getOrCreateTag();
                    String powerType = tag.getString("PowerType");
                    if (powerType.isBlank()) {
                        powerType = spikeMetal.isFeruchemical() ? "feruchemy" : "allomancy";
                    }
                    Metal powerMetal = Metal.byName(tag.getString("PowerMetal")).orElse(spikeMetal);
                    float strength = tag.contains("Strength") ? tag.getFloat("Strength") : 1.0F;

                    if (vData.installSpike(spikeMetal, powerType, powerMetal, strength)) {
                        if (!attacker.getAbilities().instabuild) {
                            spikeStack.shrink(1);
                        }
                        victim.hurt(victim.damageSources().magic(), 4.0F);
                        serverLevel.playSound(null, victim.blockPosition(), SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 0.65F, 0.55F);
                        attacker.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.spike_installed_other", powerMetal.displayName(), victim.getDisplayName()), true);
                        if (victim instanceof ServerPlayer serverVictim) {
                            serverVictim.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mistborn_metal_arts.spike_installed_self", powerMetal.displayName()), true);
                            MetalArtsNetwork.sync(serverVictim);
                        }
                    }
                });
                return;
            }

            // 2. Spiking with a BLANK spike -> Extract power and place stuck spike physically
            java.util.List<BindPoint> points = new java.util.ArrayList<>();
            boolean isAnimal = victim instanceof net.minecraft.world.entity.animal.Animal || victim instanceof net.minecraft.world.entity.ambient.AmbientCreature || victim instanceof net.minecraft.world.entity.monster.Spider;

            if (spikeMetal.isGodMetal() && spikeMetal != Metal.LERASIUM) {
                // Wildcard spike — can target and steal ANY power depending on aimed coordinates!
                points.add(new BindPoint("Physical Strength", Metal.IRON, "physical_strength", 0.0D, 1.2D, -0.1D));
                points.add(new BindPoint("Physical Senses", Metal.TIN, "physical_sight", 0.0D, 1.6D, 0.0D));
                if (!isAnimal) {
                    points.add(new BindPoint("Allomantic Steel", Metal.STEEL, "allomancy", 0.25D, 1.35D, 0.0D));
                    points.add(new BindPoint("Allomantic Iron", Metal.IRON, "allomancy", -0.25D, 1.35D, 0.0D));
                    points.add(new BindPoint("Allomantic Pewter", Metal.PEWTER, "allomancy", 0.0D, 1.45D, 0.22D));
                    points.add(new BindPoint("Allomantic Tin", Metal.TIN, "allomancy", 0.0D, 1.0D, 0.22D));
                    points.add(new BindPoint("Allomantic Zinc", Metal.ZINC, "allomancy", 0.12D, 1.62D, -0.12D));
                    points.add(new BindPoint("Allomantic Brass", Metal.BRASS, "allomancy", -0.12D, 1.62D, -0.12D));
                    points.add(new BindPoint("Allomantic Copper", Metal.COPPER, "allomancy", 0.0D, 1.68D, -0.15D));
                    points.add(new BindPoint("Allomantic Bronze", Metal.BRONZE, "allomancy", 0.0D, 1.75D, 0.0D));
                    points.add(new BindPoint("Allomantic Aluminum", Metal.ALUMINUM, "allomancy", 0.0D, 1.2D, 0.0D));
                    points.add(new BindPoint("Allomantic Duralumin", Metal.DURALUMIN, "allomancy", 0.0D, 1.1D, -0.1D));
                    points.add(new BindPoint("Allomantic Chromium", Metal.CHROMIUM, "allomancy", 0.15D, 1.35D, -0.1D));
                    points.add(new BindPoint("Allomantic Nicrosil", Metal.NICROSIL, "allomancy", -0.15D, 1.35D, -0.1D));
                    points.add(new BindPoint("Allomantic Cadmium", Metal.CADMIUM, "allomancy", 0.1D, 1.05D, -0.1D));
                    points.add(new BindPoint("Allomantic Bendalloy", Metal.BENDALLOY, "allomancy", -0.1D, 1.05D, -0.1D));
                    points.add(new BindPoint("Allomantic Gold", Metal.GOLD, "allomancy", 0.1D, 0.95D, 0.1D));
                    points.add(new BindPoint("Allomantic Electrum", Metal.ELECTRUM, "allomancy", -0.1D, 0.95D, 0.1D));
                    
                    points.add(new BindPoint("Feruchemical Pewter", Metal.PEWTER, "feruchemy", 0.28D, 0.85D, -0.1D));
                    points.add(new BindPoint("Feruchemical Tin", Metal.TIN, "feruchemy", -0.28D, 0.85D, -0.1D));
                    points.add(new BindPoint("Feruchemical Iron", Metal.IRON, "feruchemy", 0.15D, 0.95D, -0.15D));
                    points.add(new BindPoint("Feruchemical Steel", Metal.STEEL, "feruchemy", -0.15D, 0.95D, -0.15D));
                    points.add(new BindPoint("Feruchemical Zinc", Metal.ZINC, "feruchemy", 0.2D, 1.5D, -0.1D));
                    points.add(new BindPoint("Feruchemical Brass", Metal.BRASS, "feruchemy", -0.2D, 1.5D, -0.1D));
                    points.add(new BindPoint("Feruchemical Copper", Metal.COPPER, "feruchemy", 0.1D, 1.45D, 0.1D));
                    points.add(new BindPoint("Feruchemical Bronze", Metal.BRONZE, "feruchemy", -0.1D, 1.45D, 0.1D));
                    points.add(new BindPoint("Feruchemical Gold", Metal.GOLD, "feruchemy", -0.1D, 1.25D, -0.12D));
                    points.add(new BindPoint("Feruchemical Electrum", Metal.ELECTRUM, "feruchemy", 0.1D, 1.25D, -0.12D));
                    points.add(new BindPoint("Feruchemical Cadmium", Metal.CADMIUM, "feruchemy", -0.1D, 1.15D, 0.1D));
                    points.add(new BindPoint("Feruchemical Bendalloy", Metal.BENDALLOY, "feruchemy", 0.1D, 1.15D, 0.1D));
                    points.add(new BindPoint("Feruchemical Chromium", Metal.CHROMIUM, "feruchemy", 0.15D, 1.1D, -0.1D));
                    points.add(new BindPoint("Feruchemical Nicrosil", Metal.NICROSIL, "feruchemy", -0.15D, 1.1D, -0.1D));
                    points.add(new BindPoint("Feruchemical Duralumin", Metal.DURALUMIN, "feruchemy", 0.0D, 1.0D, 0.1D));
                    points.add(new BindPoint("Feruchemical Aluminum", Metal.ALUMINUM, "feruchemy", 0.0D, 1.0D, -0.1D));
                    
                    points.add(new BindPoint("Emotional Fortitude", Metal.ZINC, "emotional_fortitude", 0.0D, 1.5D, 0.0D));
                    points.add(new BindPoint("Mental Fortitude", Metal.COPPER, "mental_fortitude", 0.0D, 1.6D, -0.15D));
                    points.add(new BindPoint("Investiture", Metal.NICROSIL, "investiture", 0.0D, 1.0D, 0.0D));
                    points.add(new BindPoint("Destiny", Metal.CHROMIUM, "destiny", 0.0D, 0.9D, 0.0D));
                    points.add(new BindPoint("Power Removal", Metal.ALUMINUM, "allomancy", 0.0D, 1.2D, 0.0D));
                    points.add(new BindPoint("Connection/Identity", Metal.DURALUMIN, "connection", 0.0D, 1.1D, 0.0D));
                }
            } else if (spikeMetal == Metal.IRON) {
                points.add(new BindPoint("Physical Strength", Metal.IRON, "physical_strength", 0.0D, 1.2D, -0.1D));
            } else if (spikeMetal == Metal.TIN) {
                points.add(new BindPoint("Physical Senses", Metal.TIN, "physical_sight", 0.0D, 1.6D, 0.0D));
            } else if (!isAnimal) {
                if (spikeMetal == Metal.STEEL) {
                    points.add(new BindPoint("Allomantic Steel", Metal.STEEL, "allomancy", 0.25D, 1.35D, 0.0D));
                    points.add(new BindPoint("Allomantic Iron", Metal.IRON, "allomancy", -0.25D, 1.35D, 0.0D));
                    points.add(new BindPoint("Allomantic Pewter", Metal.PEWTER, "allomancy", 0.0D, 1.45D, 0.22D));
                    points.add(new BindPoint("Allomantic Tin", Metal.TIN, "allomancy", 0.0D, 1.0D, 0.22D));
                } else if (spikeMetal == Metal.BRONZE) {
                    points.add(new BindPoint("Allomantic Zinc", Metal.ZINC, "allomancy", 0.12D, 1.62D, -0.12D));
                    points.add(new BindPoint("Allomantic Brass", Metal.BRASS, "allomancy", -0.12D, 1.62D, -0.12D));
                    points.add(new BindPoint("Allomantic Copper", Metal.COPPER, "allomancy", 0.0D, 1.68D, -0.15D));
                    points.add(new BindPoint("Allomantic Bronze", Metal.BRONZE, "allomancy", 0.0D, 1.75D, 0.0D));
                } else if (spikeMetal == Metal.ELECTRUM) {
                    points.add(new BindPoint("Allomantic Aluminum", Metal.ALUMINUM, "allomancy", 0.0D, 1.2D, 0.0D));
                    points.add(new BindPoint("Allomantic Duralumin", Metal.DURALUMIN, "allomancy", 0.0D, 1.1D, -0.1D));
                    points.add(new BindPoint("Allomantic Chromium", Metal.CHROMIUM, "allomancy", 0.15D, 1.35D, -0.1D));
                    points.add(new BindPoint("Allomantic Nicrosil", Metal.NICROSIL, "allomancy", -0.15D, 1.35D, -0.1D));
                } else if (spikeMetal == Metal.CADMIUM) {
                    points.add(new BindPoint("Allomantic Cadmium", Metal.CADMIUM, "allomancy", 0.1D, 1.05D, -0.1D));
                    points.add(new BindPoint("Allomantic Bendalloy", Metal.BENDALLOY, "allomancy", -0.1D, 1.05D, -0.1D));
                    points.add(new BindPoint("Allomantic Gold", Metal.GOLD, "allomancy", 0.1D, 0.95D, 0.1D));
                    points.add(new BindPoint("Allomantic Electrum", Metal.ELECTRUM, "allomancy", -0.1D, 0.95D, 0.1D));
                } else if (spikeMetal == Metal.PEWTER) {
                    points.add(new BindPoint("Feruchemical Pewter", Metal.PEWTER, "feruchemy", 0.28D, 0.85D, -0.1D));
                    points.add(new BindPoint("Feruchemical Tin", Metal.TIN, "feruchemy", -0.28D, 0.85D, -0.1D));
                    points.add(new BindPoint("Feruchemical Iron", Metal.IRON, "feruchemy", 0.15D, 0.95D, -0.15D));
                    points.add(new BindPoint("Feruchemical Steel", Metal.STEEL, "feruchemy", -0.15D, 0.95D, -0.15D));
                } else if (spikeMetal == Metal.BRASS) {
                    points.add(new BindPoint("Feruchemical Zinc", Metal.ZINC, "feruchemy", 0.2D, 1.5D, -0.1D));
                    points.add(new BindPoint("Feruchemical Brass", Metal.BRASS, "feruchemy", -0.2D, 1.5D, -0.1D));
                    points.add(new BindPoint("Feruchemical Copper", Metal.COPPER, "feruchemy", 0.1D, 1.45D, 0.1D));
                    points.add(new BindPoint("Feruchemical Bronze", Metal.BRONZE, "feruchemy", -0.1D, 1.45D, 0.1D));
                } else if (spikeMetal == Metal.GOLD) {
                    points.add(new BindPoint("Feruchemical Gold", Metal.GOLD, "feruchemy", -0.1D, 1.25D, -0.12D));
                    points.add(new BindPoint("Feruchemical Electrum", Metal.ELECTRUM, "feruchemy", 0.1D, 1.25D, -0.12D));
                    points.add(new BindPoint("Feruchemical Cadmium", Metal.CADMIUM, "feruchemy", -0.1D, 1.15D, 0.1D));
                    points.add(new BindPoint("Feruchemical Bendalloy", Metal.BENDALLOY, "feruchemy", 0.1D, 1.15D, 0.1D));
                } else if (spikeMetal == Metal.BENDALLOY) {
                    points.add(new BindPoint("Feruchemical Chromium", Metal.CHROMIUM, "feruchemy", 0.15D, 1.1D, -0.1D));
                    points.add(new BindPoint("Feruchemical Nicrosil", Metal.NICROSIL, "feruchemy", -0.15D, 1.1D, -0.1D));
                    points.add(new BindPoint("Feruchemical Duralumin", Metal.DURALUMIN, "feruchemy", 0.0D, 1.0D, 0.1D));
                    points.add(new BindPoint("Feruchemical Aluminum", Metal.ALUMINUM, "feruchemy", 0.0D, 1.0D, -0.1D));
                } else if (spikeMetal == Metal.ZINC) {
                    points.add(new BindPoint("Emotional Fortitude", Metal.ZINC, "emotional_fortitude", 0.0D, 1.5D, 0.0D));
                } else if (spikeMetal == Metal.COPPER) {
                    points.add(new BindPoint("Mental Fortitude", Metal.COPPER, "mental_fortitude", 0.0D, 1.6D, -0.15D));
                } else if (spikeMetal == Metal.NICROSIL) {
                    points.add(new BindPoint("Investiture", Metal.NICROSIL, "investiture", 0.0D, 1.0D, 0.0D));
                } else if (spikeMetal == Metal.CHROMIUM) {
                    points.add(new BindPoint("Destiny", Metal.CHROMIUM, "destiny", 0.0D, 0.9D, 0.0D));
                } else if (spikeMetal == Metal.ALUMINUM) {
                    points.add(new BindPoint("Power Removal", Metal.ALUMINUM, "allomancy", 0.0D, 1.2D, 0.0D));
                } else if (spikeMetal == Metal.DURALUMIN) {
                    points.add(new BindPoint("Connection/Identity", Metal.DURALUMIN, "connection", 0.0D, 1.1D, 0.0D));
                } else if (spikeMetal == Metal.LERASIUM) {
                    points.add(new BindPoint("Lerasium Plexus", Metal.LERASIUM, "allomancy", 0.0D, 1.2D, 0.0D));
                }
            }

            // 3D coordinate transformation: lying flat on altar -> standing locally
            boolean isRestrained = false;
            if (victim instanceof Player victimPlayer) {
                var vCap = victimPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                if (vCap != null && vCap.isRestrained() && altarPos != null && altarPos.equals(vCap.getRestrainedAltarPos())) {
                    isRestrained = true;
                }
            } else {
                net.minecraft.nbt.CompoundTag vNbt = victim.getPersistentData();
                if (vNbt.getBoolean("RestrainedAltar") && altarPos != null && BlockPos.of(vNbt.getLong("RestrainedAltarPos")).equals(altarPos)) {
                    isRestrained = true;
                }
            }

            // Rotate the raw hit coordinate by negative yaw to get local rotated coordinates ox, oy, oz
            double rx = hitPos.x;
            double rz = hitPos.z;
            float yawRad = (float) Math.toRadians(-victim.yBodyRot);
            double ox = rx * Math.cos(yawRad) - rz * Math.sin(yawRad);
            double oy = Math.max(0.05D, Math.min(victim.getBoundingBox().getYsize() - 0.05D, hitPos.y));
            double oz = rx * Math.sin(yawRad) + rz * Math.cos(yawRad);

            net.minecraft.world.phys.Vec3 localCoords;
            if (isRestrained) {
                net.minecraft.core.Direction facing = net.minecraft.core.Direction.NORTH;
                BlockState altarState = serverLevel.getBlockState(altarPos);
                if (altarState.is(com.not_noah.mistborn_metal_arts.registry.ModBlocks.HEMALURGIC_ALTAR.get())) {
                    facing = altarState.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
                }

                net.minecraft.world.phys.Vec3 worldClickPos = victim.position().add(hitPos);
                BlockPos headPos = com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.getFootPos(altarState, altarPos).relative(facing);

                double dx = worldClickPos.x - (headPos.getX() + 0.5D);
                double dz = worldClickPos.z - (headPos.getZ() + 0.5D);

                // Project along the head-to-toe body axis (facing direction)
                double projY = dx * facing.getStepX() + dz * facing.getStepZ();
                double standingY = 1.45D + projY;

                // Project along the left-right axis (clockwise direction)
                net.minecraft.core.Direction perp = facing.getClockWise();
                double standingX = dx * perp.getStepX() + dz * perp.getStepZ();

                // Project along the chest-back axis (vertical world Y relative to mattress top)
                double heightAboveMattress = worldClickPos.y - (headPos.getY() + 0.5625D);
                double standingZ = 0.22D - heightAboveMattress;

                localCoords = new net.minecraft.world.phys.Vec3(standingX, standingY, standingZ);
            } else {
                localCoords = new net.minecraft.world.phys.Vec3(ox, oy, oz);
            }

            // Filter bind points based on what powers the victim possesses!
            java.util.List<BindPoint> validPoints = new java.util.ArrayList<>();
            for (BindPoint bp : points) {
                boolean hasPower = false;
                if ("physical_strength".equals(bp.powerType) || "physical_sight".equals(bp.powerType)) {
                    hasPower = true; // Any mob has physical attributes
                } else if (victim instanceof Player victimPlayer) {
                    var vData = victimPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                    if (vData != null && vData.hasPowerToSteal(bp.powerMetal, bp.powerType)) {
                        hasPower = true;
                    }
                } else if (victim instanceof com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy enemy) {
                    if (metalbornHasPower(enemy, bp.powerMetal, bp.powerType)) {
                        hasPower = true;
                    }
                } else {
                    String mobName = victim.getType().toString().toLowerCase();
                    if (mobHasPower(mobName, bp.powerMetal, bp.powerType)) {
                        hasPower = true;
                    }
                }
                
                if (hasPower) {
                    validPoints.add(bp);
                }
            }

            boolean stolenSuccessfully = false;
            BindPoint best = null;
            float strength = 0.0F;

            if (!validPoints.isEmpty()) {
                double bestDistSq = Double.MAX_VALUE;
                for (BindPoint bp : validPoints) {
                    double ds = bp.distanceSq(localCoords.x, localCoords.y, localCoords.z);
                    if (ds < bestDistSq) {
                        bestDistSq = ds;
                        best = bp;
                    }
                }

                if (best != null) {
                    double distance = Math.sqrt(bestDistSq);
                    // Extraction power is better closer to the corresponding bind point!
                    // Ranges linearly from 1.0 (exact hit) down to 0.05 (2.0 meters away)
                    float accuracy = (float) Math.max(0.05D, 1.0D - (distance / 2.0D));
                    strength = 0.8F * accuracy;
                    stolenSuccessfully = true;

                    // Steal the power immediately from the victim if it's a player
                    if (victim instanceof Player victimPlayer) {
                        var vData = victimPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                        if (vData != null) {
                            if (best.powerMetal == Metal.ALUMINUM || best.powerMetal == Metal.LERASIUM) {
                                vData.clearAllomancy();
                                vData.clearFeruchemy();
                                vData.markNeedsPowerRefresh();
                                MetalArtsNetwork.sync((ServerPlayer) victimPlayer);
                            } else {
                                if (vData.stealSpecificPower(best.powerMetal, best.powerType)) {
                                    MetalArtsNetwork.sync((ServerPlayer) victimPlayer);
                                }
                            }
                        }
                    }
                }
            }

            // Always drove the spike physically into the victim using rotated local coordinates ox, oy, oz!
            StuckSpike stuck = new StuckSpike(
                spikeMetal, 
                stolenSuccessfully, 
                stolenSuccessfully ? best.powerType : "", 
                stolenSuccessfully ? best.powerMetal : spikeMetal, 
                strength, 
                ox, 
                oy, 
                oz, 
                0, 
                0, 
                0
            );

            victim.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                data.addStuckSpike(stuck);
                MetalArtsNetwork.syncStuckSpikes(victim);
            });

            if (!attacker.getAbilities().instabuild) {
                spikeStack.shrink(1);
            }

            victim.hurt(victim.damageSources().magic(), 4.0F);
            serverLevel.playSound(null, victim.blockPosition(), SoundEvents.TRIDENT_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F);

            if (stolenSuccessfully) {
                attacker.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "You drove a " + spikeMetal.displayName() + " spike into " + victim.getDisplayName().getString() + 
                    " (Stole " + best.powerMetal.displayName() + " " + (best.powerType.equals("allomancy") ? "Allomancy" : "Feruchemy") + 
                    " with " + Math.round(strength * 100) + "% efficiency)!"
                ), true);
            } else {
                attacker.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "You drove a blank " + spikeMetal.displayName() + " spike into " + victim.getDisplayName().getString() + "."
                ), true);
            }

            int lifetime = 140 + serverLevel.random.nextInt(60);
            MetalArtsNetwork.sendBloodSlash(victim, ox, oy, oz, 0, 1.2F, (float) Math.PI / 4F, lifetime, 0, 0, 0, false);
            victim.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(bdata -> {
                bdata.addSlash(ox, oy, oz, 0, 1.2F, (float) Math.PI / 4F, lifetime, 0, 0, 0, false);
                bdata.addBlood(0.6f);
            });
            attacker.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(bdata -> bdata.addBlood(0.2f));

            // Massive burst of 50 blood drop particles at exact hitPos
            net.minecraft.world.phys.Vec3 worldHitPos = victim.position().add(hitPos);
            serverLevel.sendParticles(
                    com.not_noah.mistborn_metal_arts.registry.ModParticles.BLOOD_DROP.get(),
                    worldHitPos.x, worldHitPos.y, worldHitPos.z,
                    50,
                    0.05D, 0.05D, 0.05D,
                    0.12D
            );
        }

        private static boolean mobHasPower(String mobName, Metal metal, String powerType) {
            boolean isPewter = mobName.contains("zombie") || mobName.contains("golem") || mobName.contains("ravager");
            boolean isTin = mobName.contains("bat") || mobName.contains("phantom");
            boolean isSteel = mobName.contains("enderman");
            boolean isZinc = mobName.contains("creeper");
            boolean isIron = mobName.contains("cow") || mobName.contains("pig") || mobName.contains("sheep");
            
            boolean isMental = mobName.contains("creeper") || mobName.contains("blaze") || mobName.contains("ghast") || mobName.contains("witch");
            boolean isTemporal = mobName.contains("piglin") || mobName.contains("wither") || mobName.contains("shulker") || mobName.contains("endermite");
            boolean isSpiritual = mobName.contains("evoker") || mobName.contains("illusioner") || mobName.contains("guardian") || mobName.contains("witch");
            boolean isBoss = mobName.contains("wither") || mobName.contains("dragon") || mobName.contains("warden") || mobName.contains("guardian");

            if (metal == Metal.PEWTER) {
                return isPewter || isSteel || isMental || isTemporal || isSpiritual || isBoss;
            }
            if (metal == Metal.TIN) {
                return isTin || isPewter || isSteel || isMental || isTemporal || isSpiritual || isBoss;
            }
            if (metal == Metal.STEEL) {
                return isSteel || isPewter || isIron || isMental || isTemporal || isSpiritual || isBoss;
            }
            if (metal == Metal.IRON) {
                return isIron || isSteel || isPewter || isMental || isTemporal || isSpiritual || isBoss;
            }
            if (metal == Metal.ZINC || metal == Metal.BRASS || metal == Metal.BRONZE || metal == Metal.COPPER) {
                return isMental || isSpiritual || isBoss;
            }
            if (metal == Metal.GOLD || metal == Metal.ELECTRUM || metal == Metal.CADMIUM || metal == Metal.BENDALLOY) {
                return isTemporal || isBoss;
            }
            if (metal == Metal.CHROMIUM || metal == Metal.NICROSIL || metal == Metal.DURALUMIN || metal == Metal.ALUMINUM) {
                return isSpiritual || isBoss;
            }
            if (metal.isGodMetal()) {
                return isBoss;
            }
            return false;
        }

        private static boolean metalbornHasPower(com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy enemy, Metal metal, String powerType) {
            com.not_noah.mistborn_metal_arts.entity.MetalbornRole role = enemy.role();
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.MISTBORN_ASSASSIN) {
                return "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.STEEL_INQUISITOR) {
                return true;
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.COINSHOT_BANDIT) {
                return metal == Metal.STEEL && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.LURCHER_GUARD) {
                return metal == Metal.IRON && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.PEWTER_THUG) {
                return metal == Metal.PEWTER && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.TINEYE_SCOUT) {
                return metal == Metal.TIN && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.RIOTER) {
                return metal == Metal.ZINC && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.SOOTHER) {
                return metal == Metal.BRASS && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.SEEKER) {
                return metal == Metal.BRONZE && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.SMOKER) {
                return metal == Metal.COPPER && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.ATIUM_SEER) {
                return metal == Metal.ATIUM && "allomancy".equals(powerType);
            }
            if (role == com.not_noah.mistborn_metal_arts.entity.MetalbornRole.KOLOSS) {
                return (metal == Metal.PEWTER || metal == Metal.IRON || metal == Metal.STEEL);
            }
            return false;
        }

        private static java.lang.reflect.Field sleepCounterField = null;

        private static void resetSleepCounter(Player player) {
            if (sleepCounterField == null) {
                String[] names = {"sleepCounter", "sleepTimer", "f_36113_", "f_36125_"};
                for (String name : names) {
                    try {
                        sleepCounterField = net.minecraft.world.entity.player.Player.class.getDeclaredField(name);
                        sleepCounterField.setAccessible(true);
                        break;
                    } catch (Exception e) {
                        // ignore and try next
                    }
                }
            }
            if (sleepCounterField != null) {
                try {
                    sleepCounterField.setInt(player, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
