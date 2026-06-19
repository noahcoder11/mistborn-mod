package com.not_noah.mistborn_metal_arts.capability;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.allomancy.AllomancyManager;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.compat.CuriosCompat;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.feruchemy.FeruchemyManager;
import com.not_noah.mistborn_metal_arts.hemalurgy.HemalurgyManager;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.network.SyncBloodLevelPacket;
import com.not_noah.mistborn_metal_arts.network.SyncStuckSpikesPacket;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
        event.register(com.not_noah.mistborn_metal_arts.api.SpiritWeb.class);
    }

    @Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof LivingEntity livingEntity) {
                event.addCapability(CAPABILITY_ID, new MetalArtsProvider(livingEntity));
                event.addCapability(new ResourceLocation(MistbornMetalArts.MOD_ID, "spirit_web"), new SpiritWebProvider());
                event.addCapability(new ResourceLocation(MistbornMetalArts.MOD_ID, "blood_data"), new BloodDataProvider());
            }
        }

        @SubscribeEvent
        public static void playerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            serverPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                serverPlayer.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
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
                        Direction facing = state.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
                        BlockPos headPos = pos.relative(facing);
                        double centerX = headPos.getX() + 0.5D;
                        double centerZ = headPos.getZ() + 0.5D;
                        double seatY = 0.5625D;
                        
                        if (serverPlayer.distanceToSqr(centerX, headPos.getY() + seatY, centerZ) > 1.0D) {
                            serverPlayer.setPos(centerX, headPos.getY() + seatY, centerZ);
                            serverPlayer.hurtMarked = true;
                        }
                        serverPlayer.setDeltaMovement(0, 0, 0);
                        
                        serverPlayer.setPose(net.minecraft.world.entity.Pose.STANDING);
                        serverPlayer.clearSleepingPos();
                    }
                }
                if (!web.allomancySnapped() && serverPlayer.getHealth() < 2.0F && serverPlayer.isAlive()) {
                    web.setAllomancySnapped(true);
                    
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
                    MetalArtsNetwork.syncSpiritWeb(serverPlayer);
                }

                refreshHemalurgicPowers(serverPlayer, data);
                
                float sightBonus = data.getPhysicalSightBonus();
                if (sightBonus > 0.0F) {
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 240, 0, false, false, false));
                }
                if (sightBonus >= 0.8F) {
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 240, 0, false, false, false));
                }
                if (sightBonus >= 1.5F) {
                    if (serverPlayer.tickCount % 20 == 0) {
                        java.util.List<LivingEntity> entities = serverPlayer.level().getEntitiesOfClass(LivingEntity.class, serverPlayer.getBoundingBox().inflate(16.0D * sightBonus));
                        for (LivingEntity e : entities) {
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
                com.not_noah.mistborn_metal_arts.hemalurgy.SpiritualBloatManager.tick(serverPlayer);
                });
            });
        }

        private static void refreshHemalurgicPowers(ServerPlayer player, MetalArtsData data) {
            if (data.needsPowerRefresh()) {
                data.refreshPowers(player);
                CuriosCompat.refreshEquippedHemalurgicSpikes(player, data);
                MetalArtsNetwork.sync(player);
                MetalArtsNetwork.syncSpiritWeb(player);
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
                    oldData.setPewterDragTicks(0);
                    oldData.setPewterBurnDuration(0);
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

            oldPlayer.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(oldWeb -> {
                newPlayer.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(newWeb -> {
                    newWeb.deserializeNBT(oldWeb.serializeNBT());
                    if (newPlayer instanceof ServerPlayer newServerPlayer) {
                        MetalArtsNetwork.syncSpiritWeb(newServerPlayer);
                    }
                });
            });

            oldPlayer.invalidateCaps();
        }

        @SubscribeEvent
        public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                player.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                    rollFirstJoinPowers(player, data, web);
                });
                data.refreshPowers(player);
                MetalArtsNetwork.sync(player);
                MetalArtsNetwork.syncSpiritWeb(player);
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
                MetalArtsNetwork.syncSpiritWeb(player);
            }
        }

        @SubscribeEvent
        public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MetalArtsNetwork.sync(player);
                MetalArtsNetwork.syncSpiritWeb(player);
            }
        }

        @SubscribeEvent
        public static void startTracking(PlayerEvent.StartTracking event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                MetalArtsNetwork.sync(player);
                MetalArtsNetwork.syncSpiritWeb(player);
                
                Entity target = event.getTarget();
                if (target instanceof LivingEntity living) {
                    living.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                        MetalArtsNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncStuckSpikesPacket(living, data.getStuckSpikes()));
                        MetalArtsNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncBloodLevelPacket(living.getId(), data.getBloodLevel()));
                    });
                }
            }
        }

        @SubscribeEvent
        public static void onLootTableLoad(net.minecraftforge.event.LootTableLoadEvent event) {
            if (event.getName().toString().equals("minecraft:chests/abandoned_mineshaft")) {
                net.minecraft.world.level.storage.loot.LootPool.Builder pool = net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                        .when(net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition.randomChance(0.05F))
                        .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(ModItems.METAL_BEADS.get(Metal.ATIUM).get()));
                event.getTable().addPool(pool.build());
            }
        }

        @SubscribeEvent
        public static void onWanderingTrades(net.minecraftforge.event.village.WandererTradesEvent event) {
            event.getRareTrades().add((merchant, random) -> new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 32),
                    new net.minecraft.world.item.ItemStack(ModItems.METAL_BEADS.get(Metal.ATIUM).get(), 1),
                    new net.minecraft.world.item.ItemStack(ModItems.METAL_BEADS.get(Metal.RAYSIUM).get(), 1),
                    1, 10, 0.05F
            ));

            event.getRareTrades().add((merchant, random) -> new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 24),
                    new net.minecraft.world.item.ItemStack(ModItems.METAL_BEADS.get(Metal.ALUMINUM).get(), 1),
                    new net.minecraft.world.item.ItemStack(ModItems.METAL_BEADS.get(Metal.TANAVASTIUM).get(), 1),
                    1, 10, 0.05F
            ));
        }

        private static void rollFirstJoinPowers(ServerPlayer player, MetalArtsData data, com.not_noah.mistborn_metal_arts.api.SpiritWeb web) {
            if (data.firstJoinRollComplete() || !ServerConfig.VALUES.randomPowersOnFirstJoin.get()) {
                return;
            }
            data.setFirstJoinRollComplete(true);
 
            double mistbornChance = ServerConfig.VALUES.mistbornChance.get();
            if (mistbornChance <= 0.0) {
                mistbornChance = 0.15;
            }
            double fullFeruchemistChance = ServerConfig.VALUES.fullFeruchemistChance.get();
            if (fullFeruchemistChance <= 0.0) {
                fullFeruchemistChance = 0.05;
            }
 
            com.not_noah.mistborn_metal_arts.api.Allomancy allomancy = (com.not_noah.mistborn_metal_arts.api.Allomancy) web.getInvestedSystems().get("allomancy");
            com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy = (com.not_noah.mistborn_metal_arts.api.Feruchemy) web.getInvestedSystems().get("feruchemy");
 
            float strength = 0.3F + RANDOM.nextFloat() * 0.4F;
 
            double roll = RANDOM.nextDouble();
            if (roll < mistbornChance) {
                for (Metal m : Metal.cachedValues()) {
                    if (m.isAllomantic()) {
                        allomancy.setPower(m, strength);
                    }
                }
                web.setAllomancySnapped(false);
            } else if (roll < mistbornChance + fullFeruchemistChance) {
                for (Metal m : Metal.cachedValues()) {
                    if (m.isFeruchemical()) {
                        feruchemy.setPower(m, 1.0F);
                    }
                }
                web.setAllomancySnapped(true);
            } else {
                Metal rawAllomantic = randomAllomanticMetal();
                allomancy.setPower(rawAllomantic, strength);
                 
                Metal randFeruchemy = randomFeruchemicalMetal();
                feruchemy.setPower(randFeruchemy, 1.0F);
                 
                web.setAllomancySnapped(false);
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

        private static void rollMobPowers(LivingEntity entity, MetalArtsData data, com.not_noah.mistborn_metal_arts.api.SpiritWeb web) {
            if (data.firstJoinRollComplete()) {
                return;
            }
            data.setFirstJoinRollComplete(true);

            boolean canHavePowers = entity instanceof net.minecraft.world.entity.npc.Villager 
                    || entity instanceof net.minecraft.world.entity.monster.AbstractIllager;

            if (!canHavePowers) {
                return;
            }

            double mistbornChance = ServerConfig.VALUES.mistbornChance.get();
            if (mistbornChance <= 0.0) {
                mistbornChance = 0.15;
            }
            double fullFeruchemistChance = ServerConfig.VALUES.fullFeruchemistChance.get();
            if (fullFeruchemistChance <= 0.0) {
                fullFeruchemistChance = 0.05;
            }

            com.not_noah.mistborn_metal_arts.api.Allomancy allomancy = (com.not_noah.mistborn_metal_arts.api.Allomancy) web.getInvestedSystems().get("allomancy");
            com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy = (com.not_noah.mistborn_metal_arts.api.Feruchemy) web.getInvestedSystems().get("feruchemy");

            float strength = 0.3F + RANDOM.nextFloat() * 0.4F;

            double roll = RANDOM.nextDouble();
            if (roll < mistbornChance * 0.5) {
                for (Metal m : Metal.cachedValues()) {
                    if (m.isAllomantic()) {
                        allomancy.setPower(m, strength);
                    }
                }
                web.setAllomancySnapped(true);
            } else if (roll < (mistbornChance + fullFeruchemistChance) * 0.5) {
                for (Metal m : Metal.cachedValues()) {
                    if (m.isFeruchemical()) {
                        feruchemy.setPower(m, 1.0F);
                    }
                }
                web.setAllomancySnapped(true);
            } else if (roll < 0.25) {
                if (RANDOM.nextBoolean()) {
                    Metal rawAllomantic = randomAllomanticMetal();
                    allomancy.setPower(rawAllomantic, strength);
                } else {
                    Metal randFeruchemy = randomFeruchemicalMetal();
                    feruchemy.setPower(randFeruchemy, 1.0F);
                }
                web.setAllomancySnapped(true);
            }
            data.markNeedsPowerRefresh();
        }

        @SubscribeEvent
        public static void onEntityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity livingEntity)) {
                return;
            }
            if (livingEntity instanceof Player) {
                return;
            }
            livingEntity.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                livingEntity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                    if (!web.baseAttributesInitialized) {
                        if (web.physicalAttributes == null) {
                            web.physicalAttributes = new com.not_noah.mistborn_metal_arts.api.PhysicalAttributes();
                        }
                        
                        net.minecraft.world.entity.ai.attributes.AttributeInstance attackAttr = livingEntity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        if (attackAttr != null) {
                            web.physicalAttributes.strength = (float) attackAttr.getBaseValue();
                        } else {
                            web.physicalAttributes.strength = 2.0F; // Cow/passive mob: 2.0F base strength
                        }
                        
                        net.minecraft.world.entity.ai.attributes.AttributeInstance healthAttr = livingEntity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                        if (healthAttr != null) {
                            web.physicalAttributes.health = (float) (healthAttr.getBaseValue() / 20.0D);
                        } else {
                            web.physicalAttributes.health = 0.5F;
                        }
                        
                        net.minecraft.world.entity.ai.attributes.AttributeInstance speedAttr = livingEntity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                        if (speedAttr != null) {
                            web.physicalAttributes.speed = (float) (speedAttr.getBaseValue() / 0.1D);
                        } else {
                            web.physicalAttributes.speed = 1.0F;
                        }

                        net.minecraft.world.entity.ai.attributes.AttributeInstance resAttr = livingEntity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
                        if (resAttr != null && resAttr.getBaseValue() > 0.0D) {
                            web.physicalAttributes.resistance = 1.0F + (float) (resAttr.getBaseValue() * 2.0D);
                        } else {
                            web.physicalAttributes.resistance = 1.0F;
                        }

                        web.baseAttributesInitialized = true;
                    }
                    rollMobPowers(livingEntity, data, web);
                    data.refreshPowers(livingEntity);
                });
            });
        }

        @SubscribeEvent
        public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide() || entity.tickCount % 20 != 0) {
                return;
            }
            entity.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.needsPowerRefresh()) {
                    data.refreshPowers(entity);
                }
                entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                    // 1. Attack Damage (Strength)
                    net.minecraft.world.entity.ai.attributes.AttributeInstance attack = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                    if (attack != null) {
                        java.util.UUID STRENGTH_UUID = java.util.UUID.fromString("7f382a1c-9b8d-4e5f-a0c1-3d2e1f0a9b8c");
                        attack.removeModifier(STRENGTH_UUID);
                        float bonus = web.getTotalStrength() - (float) attack.getBaseValue();
                        if (bonus != 0.0F) {
                            attack.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(STRENGTH_UUID, "Spiritweb Strength", bonus, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
                        }
                    }
                    // 2. Max Health (Health)
                    net.minecraft.world.entity.ai.attributes.AttributeInstance health = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                    if (health != null) {
                        java.util.UUID HEALTH_UUID = java.util.UUID.fromString("9b8d7f38-2a1c-4e5f-a0c1-3d2e1f0a9b8c");
                        health.removeModifier(HEALTH_UUID);
                        float bonus = (web.getTotalHealth() * 20.0F) - (float) health.getBaseValue();
                        if (bonus != 0.0F) {
                            float prevMax = entity.getMaxHealth();
                            health.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(HEALTH_UUID, "Spiritweb Health", bonus, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
                            if (entity.getHealth() > entity.getMaxHealth()) {
                                entity.setHealth(entity.getMaxHealth());
                            } else if (entity.getHealth() == prevMax && bonus > 0.0F) {
                                entity.setHealth(entity.getMaxHealth());
                            }
                        }
                    }
                    // 3. Movement Speed (Speed)
                    net.minecraft.world.entity.ai.attributes.AttributeInstance speed = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                    if (speed != null) {
                        java.util.UUID SPEED_UUID = java.util.UUID.fromString("2a1c7f38-9b8d-4e5f-a0c1-3d2e1f0a9b8c");
                        speed.removeModifier(SPEED_UUID);
                        float bonus = (web.getTotalSpeed() * 0.1F) - (float) speed.getBaseValue();
                        if (bonus != 0.0F) {
                            speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SPEED_UUID, "Spiritweb Speed", bonus, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
                        }
                    }
                    // 4. Knockback Resistance (Resistance)
                    net.minecraft.world.entity.ai.attributes.AttributeInstance resistance = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
                    if (resistance != null) {
                        java.util.UUID RESISTANCE_UUID = java.util.UUID.fromString("4e5f7f38-2a1c-9b8d-a0c1-3d2e1f0a9b8c");
                        resistance.removeModifier(RESISTANCE_UUID);
                        float bonus = (web.getTotalResistance() - 1.0F) * 0.5F - (float) resistance.getBaseValue();
                        if (bonus != 0.0F) {
                            resistance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RESISTANCE_UUID, "Spiritweb Resistance", bonus, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
                        }
                    }
                });
            });
        }
    }
}
