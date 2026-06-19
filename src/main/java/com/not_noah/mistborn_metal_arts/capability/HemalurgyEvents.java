package com.not_noah.mistborn_metal_arts.capability;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.compat.CuriosCompat;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.network.SyncBloodLevelPacket;
import com.not_noah.mistborn_metal_arts.network.SyncStuckSpikesPacket;
import com.not_noah.mistborn_metal_arts.registry.ModBlocks;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HemalurgyEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }

        if (entity instanceof Player) {
            return;
        }

        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.getBoolean("RestrainedAltar")) {
            BlockPos pos = BlockPos.of(persistentData.getLong("RestrainedAltarPos"));
            BlockState state = entity.level().getBlockState(pos);

            if (!state.is(ModBlocks.HEMALURGIC_ALTAR.get())) {
                persistentData.putBoolean("RestrainedAltar", false);
                entity.clearSleepingPos();
                entity.setPose(Pose.STANDING);
                MetalArtsNetwork.syncStuckSpikes(entity);
            } else {
                Direction facing = state.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
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
                
                entity.setPose(Pose.STANDING);
                entity.clearSleepingPos();
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.is(Items.GLASS_BOTTLE)) {
            Entity target = event.getTarget();
            if (target instanceof LivingEntity living) {
                living.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(bloodData -> {
                    if (bloodData.getBloodLevel() > 0.0F) {
                        if (!event.getLevel().isClientSide) {
                            bloodData.setBloodLevel(Math.max(0.0F, bloodData.getBloodLevel() - 0.25F));
                            
                            ItemStack bloodVial = new ItemStack(ModItems.BLOOD_VIAL.get());
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
                            
                            player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                            MetalArtsNetwork.syncBloodLevel(living, bloodData.getBloodLevel());
                        }
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        entity.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            List<StuckSpike> spikes = data.getStuckSpikes();
            if (spikes != null && !spikes.isEmpty()) {
                for (StuckSpike spike : spikes) {
                    ItemStack spikeStack;
                    if (spike.isCharged()) {
                        spikeStack = new ItemStack(ModItems.CHARGED_SPIKES.get(spike.getMetal()).get());
                        spikeStack.getOrCreateTag().putString("PowerType", spike.getPowerType());
                        spikeStack.getOrCreateTag().putString("PowerMetal", spike.getPowerMetal().id());
                        spikeStack.getOrCreateTag().putFloat("Strength", spike.getStrength());
                        spikeStack.getOrCreateTag().putString("SpikeIdentity", spike.getIdentityKey());
                        if (spike.getStolenSpiritWeb() != null && !spike.getStolenSpiritWeb().isEmpty()) {
                            spikeStack.getOrCreateTag().put("StolenSpiritWeb", spike.getStolenSpiritWeb().copy());
                        }
                    } else {
                        spikeStack = new ItemStack(ModItems.SPIKE_BLANKS.get(spike.getMetal()).get());
                    }
                    ItemEntity itemEntity = new ItemEntity(
                        entity.level(), entity.getX(), entity.getY() + 0.5D, entity.getZ(), spikeStack
                    );
                    event.getDrops().add(itemEntity);
                }
                data.setStuckSpikes(new ArrayList<>());
            }
        });
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
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
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player != null && !player.level().isClientSide()) {
            BlockState brokenState = event.getState();
            net.minecraft.world.level.block.Block brokenBlock = brokenState.getBlock();
            if (brokenBlock == ModBlocks.ATIUM_CLUSTER.get() || brokenBlock == ModBlocks.BUDDING_ATIUM.get() || brokenBlock == ModBlocks.ATIUM_GEODE.get()) {
                player.hurt(player.damageSources().generic(), 2.0F);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.displayClientMessage(Component.literal("§cYour hands are scarred by the sharp, jagged Atium crystals...§r"), true);
            }
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            attacker.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(attackerData -> {
                boolean hasRaysiumSpike = attackerData.installedSpikes().stream()
                        .anyMatch(s -> s.spikeMetal() == Metal.RAYSIUM);
                if (hasRaysiumSpike && !attackerData.installedSpikes().isEmpty()) {
                    List<MetalArtsData.InstalledSpike> spikes = attackerData.installedSpikes();
                    if (attacker.getRandom().nextFloat() < 0.15F) {
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
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Entity target = event.getTarget();
        Player attacker = event.getEntity();
        ItemStack stack = event.getItemStack();
        InteractionHand hand = event.getHand();
        net.minecraft.world.level.Level level = event.getLevel();

        if (stack.getItem() instanceof com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem spike) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));

            if (!level.isClientSide && attacker instanceof ServerPlayer serverAttacker && target instanceof LivingEntity victim) {
                Vec3 eyePos = attacker.getEyePosition(1.0F);
                Vec3 lookVec = attacker.getLookAngle();
                Vec3 reachEnd = eyePos.add(lookVec.scale(6.0D));
                AABB targetBB = victim.getBoundingBox().inflate(0.1D);
                Optional<Vec3> clipResult = targetBB.clip(eyePos, reachEnd);
                Vec3 hitPos = clipResult.orElseGet(event::getLocalPos).subtract(victim.position());

                performSpikingRitual(serverAttacker, victim, spike, stack, hand, victim.blockPosition(), hitPos);
            }
            return;
        }

        if (level.isClientSide || !(attacker instanceof ServerPlayer serverAttacker)) {
            return;
        }

        if (!(target instanceof LivingEntity victim)) {
            return;
        }

        // stuck spikes retrieve or bind/unbind
        boolean[] retrieved = {false};
        victim.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            List<StuckSpike> spikes = data.getStuckSpikes();
            if (spikes != null && !spikes.isEmpty()) {
                Vec3 clickPos = event.getLocalPos();
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
                        returnedSpike = new ItemStack(ModItems.CHARGED_SPIKES.get(closest.getMetal()).get());
                        returnedSpike.getOrCreateTag().putString("PowerType", closest.getPowerType());
                        returnedSpike.getOrCreateTag().putString("PowerMetal", closest.getPowerMetal().id());
                        returnedSpike.getOrCreateTag().putFloat("Strength", closest.getStrength());
                        returnedSpike.getOrCreateTag().putString("SpikeIdentity", closest.getIdentityKey());
                        returnedSpike.getOrCreateTag().put("StolenSpiritWeb", closest.getStolenSpiritWeb());
                    } else {
                        returnedSpike = new ItemStack(ModItems.SPIKE_BLANKS.get(closest.getMetal()).get());
                    }
                    
                    if (!serverAttacker.getInventory().add(returnedSpike)) {
                        serverAttacker.drop(returnedSpike, false);
                    }
                    
                    if (closest.isCharged() && !(victim instanceof Player)) {
                        victim.discard();
                        level.playSound(null, victim.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.4F, 0.7F);
                    } else {
                        level.playSound(null, victim.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                    retrieved[0] = true;
                }
            }
        });

        if (retrieved[0]) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        // Altar bind/unbind
        boolean isRestrained = false;
        BlockPos restrainedAltar = null;
        
        if (victim instanceof Player victimPlayer) {
            var vCap = victimPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
            if (vCap != null && vCap.isRestrained()) {
                isRestrained = true;
                restrainedAltar = vCap.getRestrainedAltarPos();
            }
        } else {
            CompoundTag vNbt = victim.getPersistentData();
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
                        victimPlayer.setPose(Pose.STANDING);
                        attacker.displayClientMessage(Component.translatable("message.mistborn_metal_arts.released_mob"), true);
                        victimPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.released_restraint"), true);
                        MetalArtsNetwork.sync(serverVictim);
                        MetalArtsNetwork.syncStuckSpikes(serverVictim);
                    }
                } else {
                    CompoundTag vNbt = victim.getPersistentData();
                    vNbt.putBoolean("RestrainedAltar", false);
                    victim.clearSleepingPos();
                    victim.setPose(Pose.STANDING);
                    attacker.displayClientMessage(Component.translatable("message.mistborn_metal_arts.released_mob"), true);
                    MetalArtsNetwork.syncStuckSpikes(victim);
                }
                level.playSound(null, restrainedAltar, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.8F);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
                return;
            }
        } else {
            BlockPos victimPos = victim.blockPosition();
            BlockPos foundAltarFoot = null;
            BlockState altarState = null;
            if (level.getBlockState(victimPos).is(ModBlocks.HEMALURGIC_ALTAR.get())) {
                foundAltarFoot = com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.getFootPos(level.getBlockState(victimPos), victimPos);
                altarState = level.getBlockState(foundAltarFoot);
            } else if (level.getBlockState(victimPos.below()).is(ModBlocks.HEMALURGIC_ALTAR.get())) {
                foundAltarFoot = com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.getFootPos(level.getBlockState(victimPos.below()), victimPos.below());
                altarState = level.getBlockState(foundAltarFoot);
            }

            if (foundAltarFoot != null) {
                boolean occupied = false;
                List<LivingEntity> occupants = level.getEntitiesOfClass(LivingEntity.class, new AABB(foundAltarFoot).expandTowards(altarState.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING).getStepX(), 0.0D, altarState.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING).getStepZ()).inflate(0.5D));
                for (LivingEntity e : occupants) {
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
                            Direction facing = altarState.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
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

                            attacker.displayClientMessage(Component.translatable("message.mistborn_metal_arts.mob_restrained"), true);
                            victimPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.restrained_altar"), true);
                            MetalArtsNetwork.sync(serverVictim);
                            MetalArtsNetwork.syncStuckSpikes(serverVictim);
                        }
                    } else {
                        CompoundTag vNbt = victim.getPersistentData();
                        vNbt.putBoolean("RestrainedAltar", true);
                        vNbt.putLong("RestrainedAltarPos", foundAltarFoot.asLong());
                        vNbt.putInt("RestrainedAltarSeat", 0);
                        attacker.displayClientMessage(Component.translatable("message.mistborn_metal_arts.mob_restrained"), true);
                        MetalArtsNetwork.syncStuckSpikes(victim);
                    }

                    level.playSound(null, foundAltarFoot, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.CONSUME);
                } else {
                    attacker.displayClientMessage(Component.translatable("message.mistborn_metal_arts.altar_full"), true);
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

    public static void performSpikingRitual(ServerPlayer attacker, LivingEntity victim, com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem spikeItem, ItemStack spikeStack, InteractionHand hand, BlockPos altarPos, Vec3 hitPos) {
        ServerLevel serverLevel = (ServerLevel) victim.level();
        Metal spikeMetal = spikeItem.metal();

        if (spikeItem.charged()) {
            victim.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(vData -> {
                CompoundTag tag = spikeStack.getOrCreateTag();
                String powerType = tag.getString("PowerType");
                if (powerType.isBlank()) {
                    powerType = spikeMetal.isFeruchemical() ? "feruchemy" : "allomancy";
                }
                Metal powerMetal = Metal.byName(tag.getString("PowerMetal")).orElse(spikeMetal);
                float strength = tag.contains("Strength") ? tag.getFloat("Strength") : 1.0F;

                String identityKey = tag.getString("SpikeIdentity");
                CompoundTag stolenSpiritWeb = tag.contains("StolenSpiritWeb") ? tag.getCompound("StolenSpiritWeb") : new CompoundTag();
                if (vData.installSpike(spikeMetal, powerType, powerMetal, strength, identityKey, stolenSpiritWeb)) {
                    if (!attacker.getAbilities().instabuild) {
                        spikeStack.shrink(1);
                    }
                    victim.hurt(victim.damageSources().magic(), 4.0F);
                    serverLevel.playSound(null, victim.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.65F, 0.55F);
                    attacker.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_installed_other", powerMetal.displayName(), victim.getDisplayName()), true);
                    if (victim instanceof ServerPlayer serverVictim) {
                        serverVictim.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_installed_self", powerMetal.displayName()), true);
                        MetalArtsNetwork.sync(serverVictim);
                    }
                }
            });
            return;
        }

        List<BindPoint> points = new ArrayList<>();
        boolean isAnimal = victim instanceof net.minecraft.world.entity.animal.Animal || victim instanceof net.minecraft.world.entity.ambient.AmbientCreature || victim instanceof net.minecraft.world.entity.monster.Spider;

        if (spikeMetal.isGodMetal() && spikeMetal != Metal.LERASIUM) {
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

        boolean isRestrained = false;
        if (victim instanceof Player victimPlayer) {
            var vCap = victimPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
            if (vCap != null && vCap.isRestrained() && altarPos != null && altarPos.equals(vCap.getRestrainedAltarPos())) {
                isRestrained = true;
            }
        } else {
            CompoundTag vNbt = victim.getPersistentData();
            if (vNbt.getBoolean("RestrainedAltar") && altarPos != null && BlockPos.of(vNbt.getLong("RestrainedAltarPos")).equals(altarPos)) {
                isRestrained = true;
            }
        }

        double rx = hitPos.x;
        double rz = hitPos.z;
        float yawRad = (float) Math.toRadians(-victim.yBodyRot);
        double ox = rx * Math.cos(yawRad) - rz * Math.sin(yawRad);
        double oy = Math.max(0.05D, Math.min(victim.getBoundingBox().getYsize() - 0.05D, hitPos.y));
        double oz = rx * Math.sin(yawRad) + rz * Math.cos(yawRad);

        Vec3 localCoords;
        if (isRestrained) {
            Direction facing = Direction.NORTH;
            BlockState altarState = serverLevel.getBlockState(altarPos);
            if (altarState.is(ModBlocks.HEMALURGIC_ALTAR.get())) {
                facing = altarState.getValue(com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.FACING);
            }

            Vec3 worldClickPos = victim.position().add(hitPos);
            BlockPos headPos = com.not_noah.mistborn_metal_arts.block.HemalurgicAltarBlock.getFootPos(altarState, altarPos).relative(facing);

            double dx = worldClickPos.x - (headPos.getX() + 0.5D);
            double dz = worldClickPos.z - (headPos.getZ() + 0.5D);

            double projY = dx * facing.getStepX() + dz * facing.getStepZ();
            double standingY = 1.45D + projY;

            Direction perp = facing.getClockWise();
            double standingX = dx * perp.getStepX() + dz * perp.getStepZ();

            double heightAboveMattress = worldClickPos.y - (headPos.getY() + 0.5625D);
            double standingZ = 0.22D - heightAboveMattress;

            localCoords = new Vec3(standingX, standingY, standingZ);
        } else {
            localCoords = new Vec3(ox, oy, oz);
        }

        List<BindPoint> validPoints = new ArrayList<>();
        for (BindPoint bp : points) {
            boolean hasPower = false;
            if ("physical_strength".equals(bp.powerType) || "physical_sight".equals(bp.powerType) || "emotional_fortitude".equals(bp.powerType) || "mental_fortitude".equals(bp.powerType) || "investiture".equals(bp.powerType) || "destiny".equals(bp.powerType) || "connection".equals(bp.powerType)) {
                hasPower = true;
            } else {
                var cap = victim.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.SPIRIT_WEB).orElse(null);
                if (cap != null) {
                    if ("feruchemy".equals(bp.powerType)) {
                        hasPower = cap.getFeruchemicalStrength(bp.powerMetal) > 0.0F;
                    } else if ("allomancy".equals(bp.powerType)) {
                        hasPower = cap.getAllomanticStrength(bp.powerMetal) > 0.0F;
                    }
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
                float accuracy = (float) Math.max(0.05D, 1.0D - (distance / 2.0D));
                strength = 0.8F * accuracy;
                stolenSuccessfully = true;
            }
        }

        final String[] victimIdentity = {""};
        victim.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
            victimIdentity[0] = web.getIdentityKey();
        });
        if (victimIdentity[0].isEmpty()) {
            victimIdentity[0] = com.not_noah.mistborn_metal_arts.api.SpiritualAttributes.generateIdentity();
        }

        CompoundTag stolenSpiritWebNBT = new CompoundTag();
        if (stolenSuccessfully) {
            com.not_noah.mistborn_metal_arts.api.SpiritWeb stolenFragment = createStolenSpiritWeb(victim, best, strength, victimIdentity[0]);
            stolenSpiritWebNBT = stolenFragment.serializeNBT();
            
            damageVictimSpiritWeb(victim, best, strength);

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
            0,
            victimIdentity[0],
            stolenSpiritWebNBT
        );

        victim.getCapability(MetalArtsCapabilities.BLOOD_DATA).ifPresent(data -> {
            data.addStuckSpike(stuck);
            MetalArtsNetwork.syncStuckSpikes(victim);
        });

        if (!attacker.getAbilities().instabuild) {
            spikeStack.shrink(1);
        }

        victim.hurt(victim.damageSources().magic(), 4.0F);
        serverLevel.playSound(null, victim.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 0.9F);

        if (stolenSuccessfully) {
            String stolenPowerName = switch (best.powerType) {
                case "physical_strength" -> "Physical Strength";
                case "physical_sight" -> "Physical Senses";
                case "emotional_fortitude" -> "Emotional Fortitude";
                case "mental_fortitude" -> "Mental Fortitude";
                case "investiture" -> "Investiture";
                case "destiny" -> "Destiny";
                case "connection" -> "Connection/Identity";
                case "allomancy" -> best.powerMetal.displayName() + " Allomancy";
                case "feruchemy" -> best.powerMetal.displayName() + " Feruchemy";
                default -> "Unknown Power";
            };
            attacker.displayClientMessage(Component.literal(
                "You drove a " + spikeMetal.displayName() + " spike into " + victim.getDisplayName().getString() + 
                " (Stole " + stolenPowerName + " with " + Math.round(strength * 100) + "% efficiency)!"
            ), true);
        } else {
            attacker.displayClientMessage(Component.literal(
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

        serverLevel.sendParticles(
                ModParticles.BLOOD_DROP.get(),
                victim.getX() + hitPos.x, victim.getY() + hitPos.y, victim.getZ() + hitPos.z,
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

    public static com.not_noah.mistborn_metal_arts.api.SpiritWeb createStolenSpiritWeb(LivingEntity victim, BindPoint best, float strength, String identityKey) {
        com.not_noah.mistborn_metal_arts.api.SpiritWeb fragment = new com.not_noah.mistborn_metal_arts.api.SpiritWeb();
        fragment.spiritualAttributes.identity = identityKey;

        // Clear default physical attributes in fragment so we only have the stolen ones
        if (fragment.physicalAttributes != null) {
            fragment.physicalAttributes.strength = 0.0F;
            fragment.physicalAttributes.sight = 0.0F;
            fragment.physicalAttributes.zoom = 0.0F;
            fragment.physicalAttributes.speed = 0.0F;
            fragment.physicalAttributes.resistance = 0.0F;
            fragment.physicalAttributes.weight = 0.0F;
            fragment.physicalAttributes.health = 0.0F;
        }

        // Clear default arts in fragment constructor so we only have the stolen ones
        com.not_noah.mistborn_metal_arts.api.Allomancy allomancy = (com.not_noah.mistborn_metal_arts.api.Allomancy) fragment.getInvestedSystems().get("allomancy");
        for (Metal m : Metal.cachedValues()) {
            if (m.isAllomantic()) {
                allomancy.setPower(m, 0.0F);
            }
        }
        com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy = (com.not_noah.mistborn_metal_arts.api.Feruchemy) fragment.getInvestedSystems().get("feruchemy");
        for (Metal m : Metal.cachedValues()) {
            if (m.isFeruchemical()) {
                feruchemy.setPower(m, 0.0F);
            }
        }

        // Determine victim's original value scaled by efficiency
        final float[] victimValue = {1.0F};
        victim.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
            switch (best.powerType) {
                case "physical_strength" -> {
                    if (web.physicalAttributes != null) {
                        victimValue[0] = web.physicalAttributes.strength;
                    }
                }
                case "physical_sight" -> {
                    if (web.physicalAttributes != null) {
                        victimValue[0] = web.physicalAttributes.sight;
                    }
                }
                case "emotional_fortitude" -> {
                    if (web.cognitiveAttributes != null) {
                        victimValue[0] = web.cognitiveAttributes.determination;
                    }
                }
                case "mental_fortitude" -> {
                    if (web.cognitiveAttributes != null) {
                        victimValue[0] = web.cognitiveAttributes.intelligence;
                    }
                }
                case "allomancy" -> {
                    victimValue[0] = web.getAllomanticStrength(best.powerMetal);
                }
                case "feruchemy" -> {
                    victimValue[0] = web.getFeruchemicalStrength(best.powerMetal);
                }
            }
        });

        float finalStolenValue = victimValue[0] <= 0.0F ? 1.0F : victimValue[0];

        switch (best.powerType) {
            case "physical_strength" -> {
                fragment.physicalAttributes.strength = finalStolenValue;
            }
            case "physical_sight" -> {
                fragment.physicalAttributes.sight = finalStolenValue;
                fragment.physicalAttributes.zoom = finalStolenValue;
            }
            case "emotional_fortitude" -> {
                fragment.cognitiveAttributes.determination = finalStolenValue;
                fragment.spiritualAttributes.stability = finalStolenValue;
            }
            case "mental_fortitude" -> {
                fragment.cognitiveAttributes.intelligence = finalStolenValue;
            }
            case "investiture" -> {
                fragment.spiritualAttributes.contamination = finalStolenValue;
            }
            case "destiny" -> {
                fragment.spiritualAttributes.scarring = finalStolenValue;
            }
            case "connection" -> {
                fragment.spiritualAttributes.identity = identityKey;
            }
            case "allomancy" -> {
                allomancy.setPower(best.powerMetal, finalStolenValue);
            }
            case "feruchemy" -> {
                feruchemy.setPower(best.powerMetal, finalStolenValue);
            }
        }

        return fragment;
    }

    private static void damageVictimSpiritWeb(LivingEntity victim, BindPoint best, float strength) {
        victim.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
            float factor = Math.max(0.0F, 1.0F - strength);
            switch (best.powerType) {
                case "physical_strength" -> {
                    if (web.physicalAttributes != null) {
                        web.physicalAttributes.strength *= factor;
                    }
                }
                case "physical_sight" -> {
                    if (web.physicalAttributes != null) {
                        web.physicalAttributes.sight *= factor;
                        web.physicalAttributes.zoom *= factor;
                    }
                }
                case "emotional_fortitude" -> {
                    if (web.cognitiveAttributes != null) {
                        web.cognitiveAttributes.determination *= factor;
                    }
                    if (web.spiritualAttributes != null) {
                        web.spiritualAttributes.stability *= factor;
                    }
                }
                case "mental_fortitude" -> {
                    if (web.cognitiveAttributes != null) {
                        web.cognitiveAttributes.intelligence *= factor;
                    }
                }
                case "investiture" -> {
                    if (web.spiritualAttributes != null) {
                        web.spiritualAttributes.contamination *= factor;
                    }
                }
                case "destiny" -> {
                    if (web.spiritualAttributes != null) {
                        web.spiritualAttributes.scarring *= factor;
                    }
                }
                case "allomancy" -> {
                    com.not_noah.mistborn_metal_arts.api.InvestedArt art = web.getInvestedSystems().get("allomancy");
                    if (art instanceof com.not_noah.mistborn_metal_arts.api.Allomancy allomancy) {
                        allomancy.setPower(best.powerMetal, allomancy.getPower(best.powerMetal) * factor);
                    }
                }
                case "feruchemy" -> {
                    com.not_noah.mistborn_metal_arts.api.InvestedArt art = web.getInvestedSystems().get("feruchemy");
                    if (art instanceof com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy) {
                        feruchemy.setPower(best.powerMetal, feruchemy.getPower(best.powerMetal) * factor);
                    }
                }
            }
        });
    }
}
