package com.not_noah.mistborn_metal_arts.hemalurgy;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class SpiritualBloatManager {
    private SpiritualBloatManager() {
    }

    public static void tick(ServerPlayer player) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            float bloat = data.spiritualBloat();
            if (bloat <= 0.0F) {
                return;
            }

            // 1. Pewter / Tin Glitches (>35% Bloat)
            if (bloat > 35.0F) {
                // Tin Blindness: 0.1% chance per tick (approx once every 50 seconds)
                if (data.isBurning(Metal.TIN) && player.getRandom().nextFloat() < 0.001F) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false));
                    player.displayClientMessage(Component.literal("§cYour senses glitch as spiritual energy surges...§r"), true);
                }

                // Pewter Flicker: 0.1% chance per tick (approx once every 50 seconds)
                if (data.isBurning(Metal.PEWTER) && player.getRandom().nextFloat() < 0.001F) {
                    // We temporarily negate active burn benefits or give a weakness/slowness effect for 40 ticks (2 seconds)
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
                    player.displayClientMessage(Component.literal("§cYour pewter strength flickers unsteadily...§r"), true);
                }
            }

            // 2. Gold Feruchemy Healing Debuff & Numbness (>50% Bloat)
            // Tapping Gold is managed in FeruchemyManager.java. We handle the nauseous screen blur here if they are active tapping Gold.
            if (bloat > 50.0F && data.feruchemyMode(Metal.GOLD) > 0) {
                if (player.tickCount % 20 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
                    player.displayClientMessage(Component.literal("§cTapping goldmind with congested soul causes intense nausea...§r"), true);
                }
            }

            // 3. Supernova Beacon (>70% Bloat)
            if (bloat > 70.0F) {
                // Emits cosmic particles and glows
                if (player.tickCount % 5 == 0) {
                    ServerLevel level = player.serverLevel();
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            5, 0.3, 0.5, 0.3, 0.1);
                }
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, false, false));

                // Attracts all hostile mobs in 48 blocks
                if (player.tickCount % 20 == 0) {
                    AABB searchBox = player.getBoundingBox().inflate(48.0D);
                    List<Monster> list = player.level().getEntitiesOfClass(Monster.class, searchBox, e -> e.isAlive());
                    for (Monster monster : list) {
                        if (monster.getTarget() == null || monster.getTarget() != player) {
                            monster.setTarget(player);
                        }
                    }
                }
            }

            // 4. Spiritual Anomaly (100% Bloat)
            if (bloat >= 100.0F) {
                // Lightning strikes: 0.05% chance per tick (approx once every 100 seconds)
                if (player.getRandom().nextFloat() < 0.0005F) {
                    ServerLevel level = player.serverLevel();
                    LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
                    if (lightning != null) {
                        lightning.moveTo(player.position());
                        level.addFreshEntity(lightning);
                        player.displayClientMessage(Component.literal("§cYour overloaded spiritweb attracts lightning from the heavens!§r"), false);
                    }
                }

                // Spawns elite Metalborn Inquisitors (Steel Inquisitor / Pewter / Seeker) to hunt you down:
                // 0.01% chance per tick (approx once every 500 seconds)
                if (player.getRandom().nextFloat() < 0.0001F) {
                    ServerLevel level = player.serverLevel();
                    // Spawn random inquisitor near player
                    double angle = player.getRandom().nextDouble() * 2.0 * Math.PI;
                    double dist = 12.0D + player.getRandom().nextDouble() * 8.0D;
                    BlockPos spawnPos = new BlockPos(
                            (int) (player.getX() + Math.cos(angle) * dist),
                            (int) player.getY(),
                            (int) (player.getZ() + Math.sin(angle) * dist)
                    );
                    
                    MetalbornRole[] roles = {MetalbornRole.SEEKER, MetalbornRole.PEWTER_THUG, MetalbornRole.LURCHER_GUARD};
                    MetalbornRole role = roles[player.getRandom().nextInt(roles.length)];
                    net.minecraft.world.entity.EntityType<? extends Mob> type = ModEntityTypes.METALBORN.get(role).get();
                    Mob inquisitor = type.create(level);
                    if (inquisitor != null) {
                        inquisitor.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getRandom().nextFloat() * 360F, 0.0F);
                        inquisitor.setCustomName(Component.literal("Steel Inquisitor"));
                        inquisitor.setCustomNameVisible(true);
                        // Buff health and damage
                        var maxHealthAttr = inquisitor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            maxHealthAttr.setBaseValue(maxHealthAttr.getBaseValue() * 2.5D);
                            inquisitor.setHealth(inquisitor.getMaxHealth());
                        }
                        var attackDmgAttr = inquisitor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                        if (attackDmgAttr != null) {
                            attackDmgAttr.setBaseValue(attackDmgAttr.getBaseValue() * 2.0D);
                        }
                        
                        inquisitor.setTarget(player);
                        level.addFreshEntity(inquisitor);
                        
                        level.playSound(null, spawnPos, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1.0F, 0.8F);
                        player.displayClientMessage(Component.literal("§4A Steel Inquisitor has found you! Spiritual anomaly detected!§r"), false);
                    }
                }
            }
        });
    }
}
