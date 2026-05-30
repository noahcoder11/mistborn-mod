package com.not_noah.mistborn_metal_arts.client;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.client.hud.MetalArtsHudOverlay;
import com.not_noah.mistborn_metal_arts.allomancy.MetalForceHelper;
import com.not_noah.mistborn_metal_arts.client.keybind.MetalArtsKeyMappings;
import com.not_noah.mistborn_metal_arts.client.render.MetalbornEnemyRenderer;
import com.not_noah.mistborn_metal_arts.client.screen.MetalArtsRadialScreen;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.network.MetalAction;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.network.ServerboundMetalActionPacket;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import com.not_noah.mistborn_metal_arts.registry.ModEntityTypes;
import com.not_noah.mistborn_metal_arts.util.ModTags;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.not_noah.mistborn_metal_arts.client.model.*;
import com.not_noah.mistborn_metal_arts.client.render.KolossRenderer;
import com.not_noah.mistborn_metal_arts.client.render.SteelInquisitorRenderer;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.client.screen.MetalArtsMachineScreen;
import com.not_noah.mistborn_metal_arts.registry.ModBlocks;
import com.not_noah.mistborn_metal_arts.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MetalArtsClientEvents {
    private MetalArtsClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.METAL_ARTS_MACHINE.get(), MetalArtsMachineScreen::new);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.ATIUM_CLUSTER.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.ATIUM_GEODE_CLUSTER.get(), RenderType.cutout());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLOOD_VIAL.get(), RenderType.translucent());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.ALUMINUM_CASING.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MetalArtsKeyMappings.OPEN_MENU);
        event.register(MetalArtsKeyMappings.BURN_SELECTED);
        event.register(MetalArtsKeyMappings.STOP_BURNING);
        event.register(MetalArtsKeyMappings.FLARE_SELECTED);
        event.register(MetalArtsKeyMappings.ALLOMANCY_PUSH);
        event.register(MetalArtsKeyMappings.ALLOMANCY_PULL);
        event.register(MetalArtsKeyMappings.CYCLE_SELECTED);
        event.register(MetalArtsKeyMappings.ALUMINUM_PURGE);
        event.register(MetalArtsKeyMappings.TOGGLE_FERUCHEMY);
        event.register(MetalArtsKeyMappings.TIME_BUBBLE);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("metal_arts_hud", MetalArtsHudOverlay::render);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.METALBORN, MistbornModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.STEEL_INQUISITOR, InquisitorModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.KOLOSS, KolossModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.KANDRA, KandraModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerParticleProviders(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.not_noah.mistborn_metal_arts.registry.ModParticles.BLOOD_DROP.get(), com.not_noah.mistborn_metal_arts.client.particle.BloodDropParticle.Provider::new);
        event.registerSpriteSet(com.not_noah.mistborn_metal_arts.registry.ModParticles.BLOOD_SPLATTER.get(), com.not_noah.mistborn_metal_arts.client.particle.BloodSplatterParticle.Provider::new);
        event.registerSpriteSet(com.not_noah.mistborn_metal_arts.registry.ModParticles.BLOOD_SLASH.get(), com.not_noah.mistborn_metal_arts.client.particle.BloodSlashParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var entry : ModEntityTypes.METALBORN.entrySet()) {
            MetalbornRole role = entry.getKey();
            if (role == MetalbornRole.STEEL_INQUISITOR) {
                event.registerEntityRenderer(entry.getValue().get(), SteelInquisitorRenderer::new);
            } else if (role == MetalbornRole.KOLOSS) {
                event.registerEntityRenderer(entry.getValue().get(), KolossRenderer::new);
            } else if (role == MetalbornRole.KANDRA) {
                event.registerEntityRenderer(entry.getValue().get(), context -> new net.minecraft.client.renderer.entity.HumanoidMobRenderer<>(context, new KandraModel<>(context.bakeLayer(ModModelLayers.KANDRA)), 0.5F) {
                    private static final net.minecraft.resources.ResourceLocation NEUTRAL = new net.minecraft.resources.ResourceLocation(MistbornMetalArts.MOD_ID, "textures/entity/kandra.png");
                    private static final net.minecraft.resources.ResourceLocation REVEALED = new net.minecraft.resources.ResourceLocation(MistbornMetalArts.MOD_ID, "textures/entity/kandra_true.png");

                    @Override
                    public net.minecraft.resources.ResourceLocation getTextureLocation(com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy entity) {
                        return (entity.getHealth() < entity.getMaxHealth() * 0.5F || entity.isAggressive()) ? REVEALED : NEUTRAL;
                    }
                });
            } else {
                event.registerEntityRenderer(entry.getValue().get(), context -> new MetalbornEnemyRenderer(context, role));
            }
        }
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.AddLayers event) {
        // Add BloodOverlayLayer, BloodArrowLayer, and StuckSpikesLayer to all LivingEntity renderers (players, mobs, armor stands)
        for (String skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer) {
                LivingEntityRenderer livingRenderer = (LivingEntityRenderer) renderer;
                livingRenderer.addLayer(new com.not_noah.mistborn_metal_arts.client.render.BloodOverlayLayer(livingRenderer));
                livingRenderer.addLayer(new com.not_noah.mistborn_metal_arts.client.render.BloodArrowLayer(livingRenderer));
                livingRenderer.addLayer(new com.not_noah.mistborn_metal_arts.client.render.StuckSpikesLayer(livingRenderer));
            }
        }
        for (net.minecraft.world.entity.EntityType<?> type : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES) {
            try {
                net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = event.getRenderer((net.minecraft.world.entity.EntityType) type);
                if (renderer instanceof LivingEntityRenderer) {
                    LivingEntityRenderer livingRenderer = (LivingEntityRenderer) renderer;
                    livingRenderer.addLayer(new com.not_noah.mistborn_metal_arts.client.render.BloodOverlayLayer(livingRenderer));
                    livingRenderer.addLayer(new com.not_noah.mistborn_metal_arts.client.render.BloodArrowLayer(livingRenderer));
                    livingRenderer.addLayer(new com.not_noah.mistborn_metal_arts.client.render.StuckSpikesLayer(livingRenderer));
                }
            } catch (Exception e) {
                // Ignore entities that don't have registered renderers yet or fail
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> models = event.getModels();
        for (java.util.Map.Entry<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> entry : models.entrySet()) {
            net.minecraft.resources.ResourceLocation loc = entry.getKey();
            if (loc instanceof net.minecraft.client.resources.model.ModelResourceLocation mrl) {
                if (mrl.getVariant().equals("inventory")) {
                    entry.setValue(new com.not_noah.mistborn_metal_arts.client.render.BloodBakedModel(entry.getValue(), 0.0F));
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeClientEvents {
        private ForgeClientEvents() {
        }

        public record PathFrame(Vec3 pos, float yRot, float xRot) {}
        public static final java.util.LinkedList<PathFrame> PAST_FRAMES = new java.util.LinkedList<>();
        private static boolean lastTickHazard = false;

        private static class EntityRenderState {
            final net.minecraft.world.entity.Pose pose;
            final java.util.Optional<net.minecraft.core.BlockPos> sleepingPos;
            EntityRenderState(net.minecraft.world.entity.Pose pose, java.util.Optional<net.minecraft.core.BlockPos> sleepingPos) {
                this.pose = pose;
                this.sleepingPos = sleepingPos;
            }
        }
        private static final java.util.Map<Integer, EntityRenderState> originalStates = new java.util.HashMap<>();

        private static java.lang.reflect.Field mainHandItemField = null;
        private static java.lang.reflect.Field offHandItemField = null;
        private static boolean reflectionFailed = false;

        private static void syncHeldItemTags(Minecraft mc) {
            if (reflectionFailed || mc.player == null) return;
            try {
                net.minecraft.client.renderer.ItemInHandRenderer renderer = mc.getEntityRenderDispatcher().getItemInHandRenderer();
                if (renderer == null) return;

                if (mainHandItemField == null) {
                    try {
                        mainHandItemField = net.minecraft.client.renderer.ItemInHandRenderer.class.getDeclaredField("mainHandItem");
                    } catch (NoSuchFieldException e) {
                        mainHandItemField = net.minecraft.client.renderer.ItemInHandRenderer.class.getDeclaredField("f_109300_");
                    }
                    mainHandItemField.setAccessible(true);
                }

                if (offHandItemField == null) {
                    try {
                        offHandItemField = net.minecraft.client.renderer.ItemInHandRenderer.class.getDeclaredField("offHandItem");
                    } catch (NoSuchFieldException e) {
                        offHandItemField = net.minecraft.client.renderer.ItemInHandRenderer.class.getDeclaredField("f_109301_");
                    }
                    offHandItemField.setAccessible(true);
                }

                net.minecraft.world.item.ItemStack cachedMain = (net.minecraft.world.item.ItemStack) mainHandItemField.get(renderer);
                net.minecraft.world.item.ItemStack cachedOff = (net.minecraft.world.item.ItemStack) offHandItemField.get(renderer);

                net.minecraft.world.item.ItemStack playerMain = mc.player.getMainHandItem();
                net.minecraft.world.item.ItemStack playerOff = mc.player.getOffhandItem();

                if (cachedMain != null && !cachedMain.isEmpty() && playerMain != null && !playerMain.isEmpty()) {
                    if (cachedMain.is(playerMain.getItem())) {
                        if (playerMain.hasTag() && playerMain.getTag().contains("BloodLevel")) {
                            float val = playerMain.getTag().getFloat("BloodLevel");
                            cachedMain.getOrCreateTag().putFloat("BloodLevel", val);
                        } else if (cachedMain.hasTag() && cachedMain.getTag().contains("BloodLevel")) {
                            cachedMain.removeTagKey("BloodLevel");
                        }
                    }
                }

                if (cachedOff != null && !cachedOff.isEmpty() && playerOff != null && !playerOff.isEmpty()) {
                    if (cachedOff.is(playerOff.getItem())) {
                        if (playerOff.hasTag() && playerOff.getTag().contains("BloodLevel")) {
                            float val = playerOff.getTag().getFloat("BloodLevel");
                            cachedOff.getOrCreateTag().putFloat("BloodLevel", val);
                        } else if (cachedOff.hasTag() && cachedOff.getTag().contains("BloodLevel")) {
                            cachedOff.removeTagKey("BloodLevel");
                        }
                    }
                }
            } catch (Exception e) {
                reflectionFailed = true;
            }
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (Minecraft.getInstance().player == null) {
                return;
            }

            if (event.phase == TickEvent.Phase.START) {
                syncHeldItemTags(Minecraft.getInstance());
            }

            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            if (Minecraft.getInstance().player.tickCount % 20 == 0) {
                com.not_noah.mistborn_metal_arts.client.render.BloodTextureManager.cleanExpiredEntities(Minecraft.getInstance());
            }

            while (MetalArtsKeyMappings.OPEN_MENU.consumeClick()) {
                MetalArtsData data = ClientMetalArtsData.data();
                if (!data.allomanticPowersRaw().isEmpty() || !data.feruchemicalPowers().isEmpty()) {
                    Minecraft.getInstance().setScreen(new MetalArtsRadialScreen());
                }
            }
            while (MetalArtsKeyMappings.CYCLE_SELECTED.consumeClick()) {
                cycle(1);
            }
            while (MetalArtsKeyMappings.BURN_SELECTED.consumeClick()) {
                send(MetalAction.START_BURN);
            }
            while (MetalArtsKeyMappings.STOP_BURNING.consumeClick()) {
                send(MetalAction.STOP_BURN);
            }
            while (MetalArtsKeyMappings.FLARE_SELECTED.consumeClick()) {
                send(MetalAction.TOGGLE_FLARE);
            }
            if (MetalArtsKeyMappings.ALLOMANCY_PUSH.isDown()) {
                send(MetalAction.PUSH);
            }
            if (MetalArtsKeyMappings.ALLOMANCY_PULL.isDown()) {
                send(MetalAction.PULL);
            }
            while (MetalArtsKeyMappings.ALUMINUM_PURGE.consumeClick()) {
                send(MetalAction.PURGE);
            }
            while (MetalArtsKeyMappings.TOGGLE_FERUCHEMY.consumeClick()) {
                send(MetalAction.TOGGLE_FERUCHEMY);
            }
            while (MetalArtsKeyMappings.TIME_BUBBLE.consumeClick()) {
                send(MetalAction.TIME_BUBBLE);
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                // Record path for Gold Chrono Ghost
                PAST_FRAMES.addFirst(new PathFrame(player.position(), player.getYRot(), player.getXRot()));
                while (PAST_FRAMES.size() > 120) {
                    PAST_FRAMES.removeLast();
                }

                // Suppress sprint if emotionally soothed by Brass
                if (player.hasEffect(ModEffects.EMOTIONAL_SOOTHE.get())) {
                    player.setSprinting(false);
                    Minecraft.getInstance().options.keySprint.setDown(false);
                }
            }
        }

        @SubscribeEvent
        public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
            LivingEntity entity = event.getEntity();
            net.minecraft.nbt.CompoundTag nbt = entity.getPersistentData();
            if (nbt.getBoolean("ClientRestrained")) {
                net.minecraft.core.BlockPos altarPos = net.minecraft.core.BlockPos.of(nbt.getLong("ClientAltarPos"));
                net.minecraft.world.level.block.state.BlockState state = entity.level().getBlockState(altarPos);
                if (state.is(com.not_noah.mistborn_metal_arts.registry.ModBlocks.HEMALURGIC_ALTAR.get())) {
                    originalStates.put(entity.getId(), new EntityRenderState(entity.getPose(), entity.getSleepingPos()));

                    net.minecraft.core.Direction facing = state.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
                    net.minecraft.core.BlockPos headPos = altarPos.relative(facing);
                    
                    if (entity.getSleepingPos().isEmpty() || !entity.getSleepingPos().get().equals(headPos)) {
                        entity.setSleepingPos(headPos);
                    }
                    entity.setPose(net.minecraft.world.entity.Pose.SLEEPING);
                    
                    // Force entity alignment rotations on the client
                    float yaw = facing.toYRot();
                    entity.setYRot(yaw);
                    entity.setXRot(0.0F);
                    entity.yRotO = yaw;
                    entity.xRotO = 0.0F;
                    entity.setYBodyRot(yaw);
                    entity.setYHeadRot(yaw);
                }
            } else {
                if (entity.getPose() == net.minecraft.world.entity.Pose.SLEEPING) {
                    boolean isVanillaSleeping = entity.getSleepingPos().isPresent() && 
                                                !(entity.level().getBlockState(entity.getSleepingPos().get()).is(com.not_noah.mistborn_metal_arts.registry.ModBlocks.HEMALURGIC_ALTAR.get()));
                    if (!isVanillaSleeping) {
                        entity.setPose(net.minecraft.world.entity.Pose.STANDING);
                        entity.clearSleepingPos();
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
            LivingEntity entity = event.getEntity();
            EntityRenderState originalState = originalStates.remove(entity.getId());
            if (originalState != null) {
                entity.setPose(originalState.pose);
                if (originalState.sleepingPos.isPresent()) {
                    entity.setSleepingPos(originalState.sleepingPos.get());
                } else {
                    entity.clearSleepingPos();
                }
            }
        }

        @SubscribeEvent
        public static void renderAtiumShadows(RenderLivingEvent.Post<?, ?> event) {
            LocalPlayer localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null || !localPlayer.hasEffect(ModEffects.ATIUM_SIGHT.get())) {
                return;
            }

            LivingEntity entity = event.getEntity();
            if (entity == localPlayer || !entity.isAlive()) {
                return;
            }

            int amplifier = localPlayer.getEffect(ModEffects.ATIUM_SIGHT.get()).getAmplifier();

            // Extrapolation (Client-side)
            double vx = entity.getX() - entity.xo;
            double vy = entity.getY() - entity.yo;
            double vz = entity.getZ() - entity.zo;

            if (Math.abs(vx) < 0.01 && Math.abs(vz) < 0.01) {
                double time = (localPlayer.tickCount + entity.getId()) * 0.2;
                vx = Math.sin(time) * 0.03;
                vz = Math.cos(time) * 0.03;
            }

            float basePrediction = 10.0F + (amplifier * 6.0F);

            // A. Action Warning Indicator: Check if opponent is about to attack the player
            boolean isAboutToAttack = false;
            if (entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == localPlayer) {
                double distanceSqr = mob.distanceToSqr(localPlayer);
                if (distanceSqr <= 16.0D) {
                    isAboutToAttack = true;
                }
            } else if (entity instanceof Player opponent) {
                double distanceSqr = opponent.distanceToSqr(localPlayer);
                if (distanceSqr <= 25.0D) {
                    Vec3 lookDir = opponent.getLookAngle();
                    Vec3 toPlayer = localPlayer.position().subtract(opponent.position()).normalize();
                    if (lookDir.dot(toPlayer) > 0.92D) {
                        isAboutToAttack = true;
                    }
                }
            }

            // B. Atium-Nullification check: is the opponent burning Electrum?
            boolean splitShadows = entity.hasEffect(ModEffects.ELECTRUM_SIGHT.get());

            if (event.getRenderer() instanceof LivingEntityRenderer) {
                LivingEntityRenderer lr = (LivingEntityRenderer) event.getRenderer();
                RenderType shadowType = RenderType.entityTranslucent(lr.getTextureLocation(entity));
                VertexConsumer buffer = event.getMultiBufferSource().getBuffer(shadowType);
                PoseStack poseStack = event.getPoseStack();

                if (splitShadows) {
                    // Render 8 chaotic fanning ghosts!
                    int numFanning = 8;
                    for (int s = 0; s < numFanning; s++) {
                        double angle = (2 * Math.PI / numFanning) * s;
                        double speed = Math.sqrt(vx * vx + vz * vz);
                        if (speed < 0.05D) {
                            speed = 0.15D;
                        }
                        double fanningVx = Math.cos(angle) * speed;
                        double fanningVz = Math.sin(angle) * speed;

                        poseStack.pushPose();
                        poseStack.translate(fanningVx * basePrediction, vy * basePrediction, fanningVz * basePrediction);
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
                        poseStack.translate(0, -entity.getBbHeight(), 0);

                        lr.getModel().renderToBuffer(
                                poseStack,
                                buffer,
                                event.getPackedLight(),
                                LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                                0.35F, 0.35F, 0.4F, 0.18F
                        );
                        poseStack.popPose();
                    }
                } else {
                    // Render 3 standard future shadows showing 5, 10, 15 tick trajectories!
                    int[] predictionSteps = {5, 10, 15};
                    for (int ticks : predictionSteps) {
                        poseStack.pushPose();
                        poseStack.translate(vx * ticks, vy * ticks, vz * ticks);
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
                        poseStack.translate(0, -entity.getBbHeight(), 0);

                        float opacity = 0.45F - (ticks / 45.0F);
                        float r = 0.05F, g = 0.1F, b = 0.12F;
                        if (isAboutToAttack) {
                            // Turn red-orange warning outline glow 10 ticks before hitting
                            r = 1.0F;
                            g = 0.22F;
                            b = 0.08F;
                            opacity = 0.55F - (ticks / 45.0F);
                        }

                        lr.getModel().renderToBuffer(
                                poseStack,
                                buffer,
                                event.getPackedLight(),
                                LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                                r, g, b, opacity
                        );
                        poseStack.popPose();
                    }
                }
            }
        }



        private record AllomanticLine(Vec3 start, Vec3 end, float massScale, float alphaFactor) {}

        private static float getEntityMass(Entity entity) {
            float mass = 1.0F;
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                mass = 0.2F;
            } else if (entity instanceof net.minecraft.world.entity.projectile.Projectile) {
                mass = 0.15F;
            } else if (entity instanceof net.minecraft.world.entity.player.Player) {
                mass = 1.8F;
            } else if (entity.getType().toShortString().contains("iron_golem")) {
                mass = 12.0F;
            } else if (entity instanceof LivingEntity living) {
                mass = 1.1F;
                for (net.minecraft.world.item.ItemStack stack : living.getArmorSlots()) {
                    if (!stack.isEmpty()) {
                        mass += 0.15F;
                        if (isMetallicStack(stack)) {
                            mass += 0.35F;
                        }
                    }
                }
                if (isMetallicStack(living.getMainHandItem())) mass += 0.3F;
                if (isMetallicStack(living.getOffhandItem())) mass += 0.3F;
            }
            return mass;
        }

        private static boolean isMetallicStack(net.minecraft.world.item.ItemStack stack) {
            return !stack.isEmpty() && (stack.is(ModTags.Items.METALLIC_ITEMS) || stack.is(ModTags.Items.METAL_ARMOR) || stack.getItem() instanceof net.minecraft.world.item.ArmorItem || stack.getItem() instanceof net.minecraft.world.item.TieredItem);
        }

        @SubscribeEvent
        public static void renderTinHealthBars(RenderLivingEvent.Post<?, ?> event) {
            LocalPlayer localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;

            MetalArtsData data = ClientMetalArtsData.data();
            if (data.isBurning(Metal.TIN) && data.savantStage(Metal.TIN) >= 2) {
                LivingEntity target = event.getEntity();
                if (target == localPlayer || !target.isAlive() || target.isInvisibleTo(localPlayer)) {
                    return;
                }

                double distSqr = Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(target);
                if (distSqr > 4096.0D) {
                    return;
                }

                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                
                float heightOffset = target.getNameTagOffsetY();
                if (target.hasCustomName() || target instanceof Player) {
                    heightOffset += 0.3F;
                }
                
                poseStack.translate(0.0D, heightOffset, 0.0D);
                poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                poseStack.scale(-0.025F, -0.025F, 0.025F);
                
                org.joml.Matrix4f matrix = poseStack.last().pose();
                net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
                
                float health = target.getHealth();
                float maxHealth = target.getMaxHealth();
                
                String healthText = String.format("%.1f / %.1f", health, maxHealth);
                net.minecraft.network.chat.Component component = net.minecraft.network.chat.Component.literal("[")
                        .append(net.minecraft.network.chat.Component.literal(healthText).withStyle(net.minecraft.ChatFormatting.RED))
                        .append(net.minecraft.network.chat.Component.literal("]"));
                
                float width = font.width(component);
                float x = -width / 2.0F;
                
                float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
                int bgColor = (int)(bgOpacity * 255.0F) << 24;
                
                font.drawInBatch(component, x, 0.0F, -1, false, matrix, event.getMultiBufferSource(), net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, bgColor, event.getPackedLight());
                font.drawInBatch(component, x, 0.0F, -1, false, matrix, event.getMultiBufferSource(), net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, event.getPackedLight());
                
                poseStack.popPose();
            }
        }

        @SubscribeEvent
        public static void renderSteelSavantOutlines(RenderLivingEvent.Post<?, ?> event) {
            LocalPlayer localPlayer = Minecraft.getInstance().player;
            if (localPlayer == null) return;
            
            MetalArtsData data = ClientMetalArtsData.data();
            if (data.isBurning(Metal.STEEL) && data.savantStage(Metal.STEEL) >= 4) {
                LivingEntity entity = event.getEntity();
                if (entity == localPlayer || !entity.isAlive()) return;
                
                boolean hasMetal = false;
                for (net.minecraft.world.item.ItemStack stack : entity.getArmorSlots()) {
                    if (isMetallicStack(stack)) {
                        hasMetal = true;
                        break;
                    }
                }
                if (isMetallicStack(entity.getMainHandItem()) || isMetallicStack(entity.getOffhandItem())) {
                    hasMetal = true;
                }
                
                if (hasMetal) {
                    if (event.getRenderer() instanceof LivingEntityRenderer) {
                        LivingEntityRenderer lr = (LivingEntityRenderer) event.getRenderer();
                        RenderType type = RenderType.entityTranslucent(lr.getTextureLocation(entity));
                        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(type);
                        PoseStack poseStack = event.getPoseStack();
                        
                        poseStack.pushPose();
                        float r = 0.15F, g = 0.65F, b = 1.0F, alpha = 0.35F;
                        
                        lr.getModel().renderToBuffer(
                            poseStack,
                            buffer,
                            15728880,
                            LivingEntityRenderer.getOverlayCoords(entity, 0.0F),
                            r, g, b, alpha
                        );
                        poseStack.popPose();
                    }
                }
            }
        }

        private static void drawGlowingBlockBox(BufferBuilder buffer, PoseStack poseStack, BlockPos pos, Vec3 camPos, float r, float g, float b, float alpha) {
            double x = pos.getX() - camPos.x;
            double y = pos.getY() - camPos.y;
            double z = pos.getZ() - camPos.z;
            
            double min = -0.01D;
            double max = 1.01D;
            
            float faceAlpha = alpha * 0.15F;
            
            // South Face
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + min), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + min), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + max), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + max), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            
            // North Face
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + min), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + max), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + max), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + min), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            
            // East Face
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + min), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + max), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + max), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + min), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            
            // West Face
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + min), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + min), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + max), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + max), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            
            // Top Face
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + max), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + max), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + max), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + max), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            
            // Bottom Face
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + min), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + min), (float)(z + min)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + max), (float)(y + min), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float)(x + min), (float)(y + min), (float)(z + max)).color(r, g, b, faceAlpha).endVertex();
        }

        @SubscribeEvent
        public static void renderAllomanticLines(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                return;
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            PoseStack poseStack = event.getPoseStack();
            Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

            // Render Gold Chrono Ghosts and Death Beacon
            renderGoldChronoGhosts(event, player, camPos, poseStack);
            renderGoldDeathBeacon(event, player, camPos, poseStack);

            // Render Electrum Future Shadow
            renderElectrumShadow(event, player, camPos, poseStack);

            boolean iron = ClientMetalArtsData.data().isBurning(Metal.IRON);
            boolean steel = ClientMetalArtsData.data().isBurning(Metal.STEEL);
            if (!iron && !steel) {
                return;
            }

            double range = ServerConfig.VALUES.maxPushPullRange.get();
            Vec3 chestPos = player.getEyePosition(event.getPartialTick()).subtract(0, 0.45D, 0);

            java.util.List<AllomanticLine> lines = new java.util.ArrayList<>();
            java.util.List<BlockPos> outlinedBlocks = new java.util.ArrayList<>();
            boolean steelSavant4 = ClientMetalArtsData.data().isBurning(Metal.STEEL) && ClientMetalArtsData.data().savantStage(Metal.STEEL) >= 4;

            // Metallic Entities
            for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(range), e -> e.isAlive() && MetalForceHelper.isMetallicEntity(e))) {
                Vec3 targetPos = entity.getPosition(event.getPartialTick()).add(0, entity.getBbHeight() * 0.5D, 0);
                double dist = chestPos.distanceTo(targetPos);
                if (dist <= range) {
                    float alphaFactor = (float) (1.0 - (dist / range));
                    float mass = getEntityMass(entity);
                    float massScale = (float) Math.min(2.5f, 0.5f + Math.sqrt(mass) * 0.5f);
                    lines.add(new AllomanticLine(chestPos, targetPos, massScale, alphaFactor));
                }
            }

            // Optimized Metallic Blocks Scan
            BlockPos origin = player.blockPosition();
            int bRange = (int) Math.min(range, 24D); // Increased visual range for blocks
            int linesDrawn = 0;
            int maxLines = 256; // Limit lines for performance in dense areas

            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-bRange, -bRange, -bRange), origin.offset(bRange, bRange, bRange))) {
                if (player.level().getBlockState(pos).is(ModTags.Blocks.METALLIC_BLOCKS)) {
                    Vec3 targetPos = Vec3.atCenterOf(pos);
                    double dist = chestPos.distanceTo(targetPos);
                    if (dist <= range) {
                        float alphaFactor = (float) (1.0 - (dist / range));
                        // Blocks are extremely heavy anchors, give them a high mass scale
                        float massScale = 2.2F;
                        lines.add(new AllomanticLine(chestPos, targetPos, massScale, alphaFactor));
                        linesDrawn++;
                        
                        if (steelSavant4 && dist <= 16.0D) {
                            outlinedBlocks.add(pos.immutable());
                        }
                    }
                }
                if (linesDrawn >= maxLines) break;
            }

            if (lines.isEmpty() && outlinedBlocks.isEmpty()) {
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            boolean seeThrough = ClientMetalArtsData.data().isBurning(Metal.TIN) || 
                                ClientMetalArtsData.data().isBurning(Metal.BRONZE) || 
                                ClientMetalArtsData.data().isBurning(Metal.ATIUM);
            if (seeThrough) {
                RenderSystem.disableDepthTest();
            }

            poseStack.pushPose();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            float r = 0.15F, g = 0.65F, b = 1.0F;
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.getBuilder();

            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            for (AllomanticLine line : lines) {
                Vec3 dir = line.end.subtract(line.start);
                double len = dir.length();
                if (len > 0.001D) {
                    float dist = (float) len;
                    // Gentler decay over distance to keep them thick and highly visible at long distances
                    float distThicknessScale = (float) Math.max(0.5, 1.0 - 0.3 * (dist / range));
                    float baseThickness = line.massScale * distThicknessScale;

                    // 1. Draw Outer Halo Bloom (wide billboarded volumetric glow ribbon)
                    double wBloom = 0.009D * baseThickness;
                    float bloomAlpha = 0.25F * line.alphaFactor;
                    addBillboardedBeam(bufferBuilder, poseStack, line.start, line.end, camPos, wBloom, r, g, b, bloomAlpha);

                    // 2. Draw Inner Core (thick, intense billboarded core ribbon)
                    double wCore = 0.003D * baseThickness;
                    float coreAlpha = 0.85F * line.alphaFactor;
                    addBillboardedBeam(bufferBuilder, poseStack, line.start, line.end, camPos, wCore, r, g, b, coreAlpha);
                }
            }

            for (BlockPos pos : outlinedBlocks) {
                double dist = chestPos.distanceTo(Vec3.atCenterOf(pos));
                float alphaFactor = (float) (1.0 - (dist / range));
                drawGlowingBlockBox(bufferBuilder, poseStack, pos, camPos, r, g, b, 0.40F * alphaFactor);
            }

            tesselator.end();
            poseStack.popPose();

            if (seeThrough) {
                RenderSystem.enableDepthTest();
            }
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }

        private static void renderGoldChronoGhosts(RenderLevelStageEvent event, LocalPlayer player, Vec3 camPos, PoseStack poseStack) {
            if (!ClientMetalArtsData.data().isBurning(Metal.GOLD)) {
                return;
            }
            net.minecraft.client.renderer.entity.EntityRenderer<? super LocalPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                LivingEntityRenderer lr = (LivingEntityRenderer) renderer;
                int[] intervals = {20, 40, 60, 80};
                for (int idx : intervals) {
                    if (idx < PAST_FRAMES.size()) {
                        PathFrame frame = PAST_FRAMES.get(idx);
                        
                        poseStack.pushPose();
                        double rx = frame.pos.x - camPos.x;
                        double ry = frame.pos.y - camPos.y;
                        double rz = frame.pos.z - camPos.z;
                        poseStack.translate(rx, ry, rz);
                        
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - frame.yRot));
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
                        poseStack.translate(0, -player.getBbHeight(), 0);
                        
                        RenderType type = RenderType.entityTranslucent(lr.getTextureLocation(player));
                        VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(type);
                        
                        lr.getModel().renderToBuffer(
                            poseStack,
                            buffer,
                            15728880,
                            LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                            1.0F, 0.82F, 0.25F, 0.3F
                        );
                        poseStack.popPose();
                    }
                }
            }
        }

        private static void renderGoldDeathBeacon(RenderLevelStageEvent event, LocalPlayer player, Vec3 camPos, PoseStack poseStack) {
            if (!ClientMetalArtsData.data().isBurning(Metal.GOLD)) {
                return;
            }
            player.getLastDeathLocation().ifPresent(globalPos -> {
                if (globalPos.dimension() == player.level().dimension()) {
                    BlockPos pos = globalPos.pos();
                    double rx = pos.getX() + 0.5D - camPos.x;
                    double ry = pos.getY() - camPos.y;
                    double rz = pos.getZ() + 0.5D - camPos.z;

                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableCull();
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    RenderSystem.disableDepthTest();
                    
                    poseStack.pushPose();
                    poseStack.translate(rx, ry, rz);
                    
                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder buffer = tesselator.getBuilder();
                    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    
                    float r = 1.0F, g = 0.82F, b = 0.2F, alpha = 0.35F;
                    double w = 0.3D, h = 256.0D;
                    
                    buffer.vertex(poseStack.last().pose(), (float)-w, 0f, (float)-w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)w, 0f, (float)-w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)w, (float)h, (float)-w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)-w, (float)h, (float)-w).color(r, g, b, alpha).endVertex();
                    
                    buffer.vertex(poseStack.last().pose(), (float)w, 0f, (float)-w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)w, 0f, (float)w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)w, (float)h, (float)w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)w, (float)h, (float)-w).color(r, g, b, alpha).endVertex();
                    
                    buffer.vertex(poseStack.last().pose(), (float)w, 0f, (float)w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)-w, 0f, (float)w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)-w, (float)h, (float)w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)w, (float)h, (float)w).color(r, g, b, alpha).endVertex();
                    
                    buffer.vertex(poseStack.last().pose(), (float)-w, 0f, (float)w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)-w, 0f, (float)-w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)-w, (float)h, (float)-w).color(r, g, b, alpha).endVertex();
                    buffer.vertex(poseStack.last().pose(), (float)-w, (float)h, (float)w).color(r, g, b, alpha).endVertex();
                    
                    tesselator.end();
                    poseStack.popPose();
                    
                    RenderSystem.enableDepthTest();
                    RenderSystem.enableCull();
                    RenderSystem.disableBlend();
                }
            });
        }

        private static void renderElectrumShadow(RenderLevelStageEvent event, LocalPlayer player, Vec3 camPos, PoseStack poseStack) {
            if (!ClientMetalArtsData.data().isBurning(Metal.ELECTRUM)) {
                lastTickHazard = false;
                return;
            }
            
            double vx = player.getX() - player.xo;
            double vy = player.getY() - player.yo;
            double vz = player.getZ() - player.zo;
            
            double px = vx * 10.0D;
            double py = vy * 10.0D;
            double pz = vz * 10.0D;
            
            Vec3 futurePos = player.position().add(px, py, pz);
            BlockPos futureBlockPos = BlockPos.containing(futurePos);
            
            boolean isLava = player.level().getBlockState(futureBlockPos).is(net.minecraft.world.level.block.Blocks.LAVA) || 
                             player.level().getBlockState(futureBlockPos.below()).is(net.minecraft.world.level.block.Blocks.LAVA);
            boolean isFire = player.level().getBlockState(futureBlockPos).is(net.minecraft.world.level.block.Blocks.FIRE) || 
                             player.level().getBlockState(futureBlockPos.below()).is(net.minecraft.world.level.block.Blocks.FIRE);
            
            boolean isFallDanger = false;
            if (player.level().getBlockState(futureBlockPos).isAir() && player.level().getBlockState(futureBlockPos.below()).isAir()) {
                int drop = 0;
                BlockPos check = futureBlockPos.below();
                while (drop < 5 && player.level().getBlockState(check).isAir()) {
                    check = check.below();
                    drop++;
                }
                if (drop >= 4 && !player.level().getBlockState(check).isAir()) {
                    isFallDanger = true;
                }
            }
            
            boolean hazard = isLava || isFire || isFallDanger;
            
            if (hazard && !lastTickHazard) {
                player.level().playSound(player, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.4F);
            }
            lastTickHazard = hazard;
            
            net.minecraft.client.renderer.entity.EntityRenderer<? super LocalPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                LivingEntityRenderer lr = (LivingEntityRenderer) livingRenderer;
                poseStack.pushPose();
                
                double rx = futurePos.x - camPos.x;
                double ry = futurePos.y - camPos.y;
                double rz = futurePos.z - camPos.z;
                poseStack.translate(rx, ry, rz);
                
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - player.getYRot()));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
                poseStack.translate(0, -player.getBbHeight(), 0);
                
                RenderType type = RenderType.entityTranslucent(lr.getTextureLocation(player));
                VertexConsumer buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(type);
                
                float r = hazard ? 1.0F : 0.9F;
                float g = hazard ? 0.2F : 0.95F;
                float b = hazard ? 0.2F : 1.0F;
                float alpha = 0.4F;
                
                lr.getModel().renderToBuffer(
                    poseStack,
                    buffer,
                    15728880,
                    LivingEntityRenderer.getOverlayCoords(player, 0.0F),
                    r, g, b, alpha
                );
                
                poseStack.popPose();
            }
        }

        private static void addBillboardedBeam(BufferBuilder buffer, PoseStack poseStack, Vec3 start, Vec3 end, Vec3 camPos, double baseWidth, float r, float g, float b, float alpha) {
            Vec3 dir = end.subtract(start);
            if (dir.lengthSqr() < 0.0001D) return;
            Vec3 lineDir = dir.normalize();

            // Midpoint to camera vector
            Vec3 mid = start.add(end).scale(0.5D);
            Vec3 toCam = camPos.subtract(mid);
            if (toCam.lengthSqr() < 0.0001D) return;
            toCam = toCam.normalize();

            // Perpendicular vector facing the camera
            Vec3 u = lineDir.cross(toCam);
            if (u.lengthSqr() < 0.0001D) return;
            u = u.normalize();

            // Distance from camera to start and end
            double distStart = camPos.distanceTo(start);
            double distEnd = camPos.distanceTo(end);

            // Scale world-space width by distance to keep screen-space pixel width constant
            double wStart = baseWidth * distStart;
            double wEnd = baseWidth * distEnd;

            Vec3 uStart = u.scale(wStart);
            Vec3 uEnd = u.scale(wEnd);

            // Draw perfectly billboarded quad
            addQuad(buffer, poseStack,
                start.subtract(uStart),
                start.add(uStart),
                end.add(uEnd),
                end.subtract(uEnd),
                r, g, b, alpha
            );
        }

        private static void addQuad(BufferBuilder buffer, PoseStack poseStack, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, float r, float g, float b, float alpha) {
            buffer.vertex(poseStack.last().pose(), (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, alpha).endVertex();
            buffer.vertex(poseStack.last().pose(), (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, alpha).endVertex();
        }

        private static final net.minecraft.resources.ResourceLocation VIGNETTE_LOCATION = new net.minecraft.resources.ResourceLocation(MistbornMetalArts.MOD_ID, "textures/gui/blood_vignette.png");

        @SubscribeEvent
        public static void onComputeFov(net.minecraftforge.client.event.ViewportEvent.ComputeFov event) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                if (player.hasEffect(ModEffects.EMOTIONAL_SOOTHE.get())) {
                    int amp = player.getEffect(ModEffects.EMOTIONAL_SOOTHE.get()).getAmplifier();
                    double targetFov = event.getFOV() - (10.0D + amp * 5.0D); // Zoom/narrowing FOV
                    event.setFOV(targetFov);
                } else if (player.hasEffect(ModEffects.ATIUM_SIGHT.get())) {
                    // Broaden FOV slightly for high-speed Chronos slowing perception!
                    event.setFOV(event.getFOV() + 8.0D);
                }
            }
        }

        @SubscribeEvent
        public static void renderBloodVignette(net.minecraftforge.client.event.RenderGuiOverlayEvent.Post event) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            // Only render our vignette once per frame (after the "all" elements)
            if (!event.getOverlay().id().getPath().equals("all")) return;

            // Draw cyan Atium sight Chronos Matrix vignette if active
            if (player.hasEffect(ModEffects.ATIUM_SIGHT.get())) {
                int width = event.getWindow().getGuiScaledWidth();
                int height = event.getWindow().getGuiScaledHeight();
                
                com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                
                // Color filter for a beautiful cyan/blue temporal bubble vignette!
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(0.2F, 0.7F, 1.0F, 0.4F);
                
                event.getGuiGraphics().blit(VIGNETTE_LOCATION, 0, 0, 0, 0.0F, 0.0F, width, height, 1920, 1080);
                
                com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
                com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            player.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
                float wetBlood = data.getBloodLevel();
                float healthPct = player.getHealth() / player.getMaxHealth();
                float missingHealth = 1.0F - healthPct;
                float displayBlood = Math.max(wetBlood, missingHealth);
                if (displayBlood > 0.01F) {
                    int width = event.getWindow().getGuiScaledWidth();
                    int height = event.getWindow().getGuiScaledHeight();
                    
                    com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                    com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                    
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, displayBlood);
                    
                    // Draw vignette stretched to the screen size
                    event.getGuiGraphics().blit(VIGNETTE_LOCATION, 0, 0, 0, 0.0F, 0.0F, width, height, 1920, 1080);
                    
                    com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
                    com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            });
        }

        private static void cycle(int direction) {
            var data = ClientMetalArtsData.data();
            java.util.List<Metal> unlocked = new java.util.ArrayList<>();
            for (Metal m : Metal.cachedValues()) {
                if (data.hasAllomanticPower(m) || data.hasFeruchemicalPower(m)) {
                    unlocked.add(m);
                }
            }
            if (unlocked.isEmpty()) {
                return;
            }
            Metal current = ClientMetalArtsData.selectedMetal();
            int currentIndex = unlocked.indexOf(current);
            int nextIndex;
            if (currentIndex == -1) {
                nextIndex = 0;
            } else {
                nextIndex = (currentIndex + direction + unlocked.size()) % unlocked.size();
            }
            Metal metal = unlocked.get(nextIndex);
            ClientMetalArtsData.setLocalSelected(metal);
            MetalArtsNetwork.sendToServer(new ServerboundMetalActionPacket(MetalAction.SELECT, metal));
        }

        private static void send(MetalAction action) {
            MetalArtsNetwork.sendToServer(new ServerboundMetalActionPacket(action, ClientMetalArtsData.selectedMetal()));
        }
    }
}
