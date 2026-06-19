package com.not_noah.mistborn_metal_arts.capability;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.allomancy.AllomancyManager;
import com.not_noah.mistborn_metal_arts.api.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AllomancyEvents {

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isBurning(Metal.STEEL) && data.savantStage(Metal.STEEL) >= 3) {
                    if (player.isShiftKeyDown()) {
                        net.minecraft.nbt.CompoundTag persist = player.getPersistentData();
                        long lastDashTime = persist.getLong("LastSteelDashTime");
                        long currentTime = player.level().getGameTime();
                        if (currentTime - lastDashTime >= 60L) {
                            persist.putLong("LastSteelDashTime", currentTime);
                            
                            Vec3 look = player.getLookAngle();
                            float strength = data.getEffectiveStrength(Metal.STEEL);
                            float flareMult = data.isFlaring(Metal.STEEL) ? 1.8F : 1.0F;
                            double speed = 1.2D * strength * flareMult;
                            
                            player.setDeltaMovement(look.x * speed, Math.max(0.4D, look.y * speed), look.z * speed);
                            player.hurtMarked = true;
                            
                            player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8F, 1.2F);
                            if (player.level() instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.05D);
                            }
                        }
                    }
                }
            });
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
            Vec3 damagePos = source.getSourcePosition() != null ? source.getSourcePosition() : (source.getEntity() != null ? source.getEntity().position() : player.position());
            Vec3 playerPos = player.position();
            
            Vec3 incoming = playerPos.subtract(damagePos).normalize();
            Vec3 lateral = new Vec3(-incoming.z, 0D, incoming.x).normalize();
            double dodgeDist = 1.6D;
            
            if (player.getRandom().nextBoolean()) {
                lateral = lateral.scale(-1D);
            }
            
            double tx = playerPos.x + lateral.x * dodgeDist;
            double ty = playerPos.y;
            double tz = playerPos.z + lateral.z * dodgeDist;
            
            if (player.level().getBlockState(new BlockPos((int) tx, (int) ty, (int) tz)).isAir()) {
                player.teleportTo(tx, ty, tz);
                player.level().playSound(null, player.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.3F);
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0D, player.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
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

        // Raysium siphoning
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            attacker.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(attackerData -> {
                if (attackerData.isBurning(Metal.RAYSIUM)) {
                    event.setAmount(event.getAmount() + 2.0F);
                    attacker.heal(2.0F);
                    if (event.getEntity() instanceof ServerPlayer targetPlayer) {
                        targetPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(targetData -> {
                            for (Metal m : targetData.burningMetals()) {
                                targetData.consumeReserve(m, 10.0F);
                            }
                        });
                    }
                    for (Metal m : attackerData.burningMetals()) {
                        attackerData.fillReserve(m, 5.0F);
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
    public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && AllomancyManager.isBurning(player, Metal.STEEL)) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.25F);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isBurning(Metal.PEWTER) && data.savantStage(Metal.PEWTER) >= 3) {
                    event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        if (sender.hasEffect(com.not_noah.mistborn_metal_arts.registry.ModEffects.EMOTIONAL_RIOT.get())) {
            String rawText = event.getRawText();
            String angryMessage = rawText.toUpperCase(Locale.ROOT);
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
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            AllomancyManager.tickWorldBubbles(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onCropGrowPre(BlockEvent.CropGrowEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BlockPos pos = event.getPos();
            double slowFactor = AllomancyManager.getSlowFactorAt(level, pos);
            if (slowFactor > 1.0D) {
                if (level.getRandom().nextDouble() >= 1.0D / slowFactor) {
                    event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                }
            }
        }
    }
}
