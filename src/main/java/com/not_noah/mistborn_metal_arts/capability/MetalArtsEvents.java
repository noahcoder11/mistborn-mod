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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
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
                AllomancyManager.tick(serverPlayer, data);
                FeruchemyManager.tick(serverPlayer, data);
                HemalurgyManager.tick(serverPlayer, data);
            });
        }

        private static void refreshHemalurgicPowers(ServerPlayer player, MetalArtsData data) {
            if (data.needsPowerRefresh()) {
                data.refreshPowers();
                CuriosCompat.refreshEquippedHemalurgicSpikes(player, data);
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
            }
        }

        @SubscribeEvent
        public static void hurt(LivingHurtEvent event) {
            if (event.getEntity() instanceof Player player && AllomancyManager.isProtectedByPewter(player)) {
                float factor = player.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> {
                    float strength = data.getEffectiveStrength();
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
        }

        @SubscribeEvent
        public static void damage(LivingDamageEvent event) {
            if (event.getEntity() instanceof Player player && AllomancyManager.tryPewterSurvival(player, event.getAmount())) {
                event.setAmount(Math.max(0F, player.getHealth() - 1F));
            }
        }

        @SubscribeEvent
        public static void fall(LivingFallEvent event) {
            if (event.getEntity() instanceof Player player && AllomancyManager.isBurning(player, Metal.STEEL)) {
                event.setDamageMultiplier(event.getDamageMultiplier() * 0.25F);
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> event.setDamageMultiplier(FeruchemyManager.adjustFallDamage(player, data, event.getDamageMultiplier())));
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
    }
}
