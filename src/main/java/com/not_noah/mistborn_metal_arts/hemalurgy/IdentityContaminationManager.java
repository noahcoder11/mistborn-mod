package com.not_noah.mistborn_metal_arts.hemalurgy;

import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

public class IdentityContaminationManager {

    private static final Random RANDOM = new Random();

    private static class ActiveSequence {
        int ticksRemaining;
        final double baseAngle;
        double currentDist;
        final net.minecraft.sounds.SoundEvent stepSound;
        
        ActiveSequence(double baseAngle, double startDist, net.minecraft.sounds.SoundEvent stepSound) {
            this.ticksRemaining = 0;
            this.baseAngle = baseAngle;
            this.currentDist = startDist;
            this.stepSound = stepSound;
        }
    }
    
    private static final java.util.Map<java.util.UUID, ActiveSequence> ACTIVE_SEQUENCES = new java.util.HashMap<>();

    private static final String[] DONOR_MEMORIES = {
        "You smell iron and ash. A forge you've never visited.",
        "A child's laughter echoes. Not your child.",
        "Your hands remember gripping a spear. You've never held one.",
        "A name surfaces: Kelsier. You don't know why.",
        "You taste copper on your tongue. Someone else's fear.",
        "The mists feel... welcoming. They shouldn't.",
        "You catch yourself humming a melody you don't recognize.",
        "For a moment, you see through two sets of eyes.",
        "A surge of rage that isn't yours. It passes.",
        "You know the layout of a building you've never entered.",
        "Someone else's grief washes over you like a wave.",
        "Your fingers twitch for a coin pouch that isn't there."
    };

    public static void tick(ServerPlayer player, MetalArtsData data) {
        tickActiveSequence(player);
        
        float oldContamination = data.identityContamination();

        // 1. Passive Decay
        boolean isResting = player.getDeltaMovement().horizontalDistanceSqr() < 0.001
                && (player.tickCount - player.getLastHurtByMobTimestamp() > 100)
                && data.burningMetals().isEmpty();

        if (isResting) {
            double decay = ServerConfig.VALUES.contaminationDecayRate.get();
            int installedCount = data.installedSpikes().size();
            int equippedCount = com.not_noah.mistborn_metal_arts.curios.CuriosIntegration.getEquippedSpikeCount(player);
            int totalSpikes = installedCount + equippedCount;
            
            float floor = totalSpikes * (float) ServerConfig.VALUES.contaminationPerSpike.get().doubleValue();
            
            if (data.identityContamination() > floor) {
                float finalDecay = (float) decay;
                float newContamination = Math.max(floor, data.identityContamination() - finalDecay);
                data.setIdentityContamination(newContamination);
            }
        }

        if (Math.abs(data.identityContamination() - oldContamination) > 0.01F) {
            com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork.sync(player);
        }

        float contamination = data.identityContamination();
        int stage = data.contaminationStage();

        if (contamination <= 0.0F) return;

        // 2. Whispers & Donor Memories by Stage
        if (stage >= 1) {
            int whisperInterval = 900; // Stage 1: 45s
            if (stage == 2) whisperInterval = 400; // Stage 2: 20s
            else if (stage >= 3) whisperInterval = 160; // Stage 3+: 8s

            if (player.tickCount % whisperInterval == 0 && RANDOM.nextFloat() < 0.8F) {
                // 15% chance to trigger a full auditory Dread Sequence instead of a one-off whisper!
                if (RANDOM.nextFloat() < 0.15F && !ACTIVE_SEQUENCES.containsKey(player.getUUID())) {
                    double angle = RANDOM.nextDouble() * 2 * Math.PI;
                    net.minecraft.sounds.SoundEvent stepSound = GHOSTLY_STEP_SOUNDS[RANDOM.nextInt(GHOSTLY_STEP_SOUNDS.length)];
                    ACTIVE_SEQUENCES.put(player.getUUID(), new ActiveSequence(angle, 12.0D, stepSound));
                } else {
                    playWhisperSound(player);
                }
            }

            // Chat Messages (Donor memories)
            int memoryChance = stage >= 3 ? 1200 : 2400; // Stage 3+: ~1 min, Stage 1-2: ~2 min
            if (player.tickCount % memoryChance == 0 && RANDOM.nextFloat() < 0.25F) {
                triggerDonorMemory(player);
            }
        }

        // 3. Stage Specific Effects
        if (stage == 2) {
            // 3% per minute chance of random power flaring (or starting to burn)
            // ~0.0025% per tick per active burn
            if (!data.burningMetals().isEmpty() && RANDOM.nextFloat() < 0.000025F * data.burningMetals().size()) {
                triggerRandomFlare(player, data);
            }
        }

        if (stage >= 3) {
            // Stage 3+: screen color distortion, metal consumption increased, etc.
            // (Increased metal consumption is handled in AllomancyManager consumption checks)
            // Powers randomly flare more frequently (Stage 3+: 6% per minute)
            if (!data.burningMetals().isEmpty() && RANDOM.nextFloat() < 0.00005F * data.burningMetals().size()) {
                triggerRandomFlare(player, data);
            }
        }

        if (stage == 4) {
            // Active Rejection: wither, existing spikes lose strength, berserk
            if (player.tickCount % 80 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, true, false));
            }

            // Berserk state: 2% per minute chance (approx 0.0016% per tick)
            if (RANDOM.nextFloat() < 0.000016F) {
                triggerBerserk(player);
            }

            // Decay installed spikes strength: 1% per minute (approx 0.016% per 20 ticks)
            if (player.tickCount % 1200 == 0) {
                decaySpikesStrength(data);
            }
        }
    }

    private static final net.minecraft.sounds.SoundEvent[] GHOSTLY_STEP_SOUNDS = {
        SoundEvents.STONE_STEP, SoundEvents.WOOD_STEP, SoundEvents.GRASS_STEP, SoundEvents.GRAVEL_STEP
    };

    private static final net.minecraft.sounds.SoundEvent[] GHOSTLY_ACTION_SOUNDS = {
        SoundEvents.STONE_BREAK, SoundEvents.WOOD_BREAK, SoundEvents.ITEM_PICKUP,
        SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, SoundEvents.ARROW_SHOOT
    };

    public static void playWhisperSound(net.minecraft.world.entity.player.Player player) {
        Random rand = new Random();
        double angle = rand.nextDouble() * 2 * Math.PI;
        double dist = 4.0D + rand.nextDouble() * 5.0D; // 4 to 9 blocks away
        double x = player.getX() + Math.cos(angle) * dist;
        double y = player.getY() + (rand.nextDouble() - 0.5) * 1.5D;
        double z = player.getZ() + Math.sin(angle) * dist;

        int type = rand.nextInt(4); // 0 = standard soul escape, 1 = footstep sequence, 2 = mining, 3 = actions
        if (type == 0) {
            // Eerie soul escape - slightly louder and more distinct!
            player.level().playSound(null, x, y, z,
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.5F, 0.35F + rand.nextFloat() * 0.4F);
        } else if (type == 1) {
            // Ghostly footsteps sequence: play two soft steps close together in space
            net.minecraft.sounds.SoundEvent stepSound = GHOSTLY_STEP_SOUNDS[rand.nextInt(GHOSTLY_STEP_SOUNDS.length)];
            player.level().playSound(null, x, y, z, stepSound, SoundSource.PLAYERS, 0.45F, 0.65F + rand.nextFloat() * 0.25F);
            
            double x2 = x + (rand.nextDouble() - 0.5) * 1.2;
            double z2 = z + (rand.nextDouble() - 0.5) * 1.2;
            player.level().playSound(null, x2, y, z2, stepSound, SoundSource.PLAYERS, 0.4F, 0.55F + rand.nextFloat() * 0.25F);
        } else if (type == 2) {
            // Ghostly mining (pickaxe impact or block breaking)
            net.minecraft.sounds.SoundEvent mineSound = rand.nextBoolean() ? SoundEvents.STONE_HIT : SoundEvents.STONE_BREAK;
            player.level().playSound(null, x, y, z, mineSound, SoundSource.PLAYERS, 0.5F, 0.45F + rand.nextFloat() * 0.35F);
        } else {
            // Ghostly interaction
            net.minecraft.sounds.SoundEvent actionSound = GHOSTLY_ACTION_SOUNDS[rand.nextInt(GHOSTLY_ACTION_SOUNDS.length)];
            float pitch = actionSound == SoundEvents.ITEM_PICKUP ? 0.45F + rand.nextFloat() * 0.4F : 0.65F + rand.nextFloat() * 0.3F;
            player.level().playSound(null, x, y, z, actionSound, SoundSource.PLAYERS, 0.45F, pitch);
        }
    }

    public static void tickActiveSequence(ServerPlayer player) {
        java.util.UUID uuid = player.getUUID();
        if (!ACTIVE_SEQUENCES.containsKey(uuid)) return;
        
        ActiveSequence seq = ACTIVE_SEQUENCES.get(uuid);
        seq.ticksRemaining++;
        
        int tick = seq.ticksRemaining;
        net.minecraft.util.RandomSource rand = player.getRandom();
        
        // FOOTSTEPS APPROACHING: ticks 10, 20, 30, 40
        if (tick == 10 || tick == 20 || tick == 30 || tick == 40) {
            double dist = 12.0D - (tick / 10.0D) * 2.2D; // walk closer
            double x = player.getX() + Math.cos(seq.baseAngle) * dist;
            double y = player.getY() + (rand.nextDouble() - 0.5) * 0.5D;
            double z = player.getZ() + Math.sin(seq.baseAngle) * dist;
            
            float volume = 0.25F + (tick / 40.0F) * 0.3F;
            float pitch = 0.65F + rand.nextFloat() * 0.25F;
            player.level().playSound(null, x, y, z, seq.stepSound, SoundSource.PLAYERS, volume, pitch);
        }
        
        // GHOSTLY MINING: tick 55 and 65
        else if (tick == 55 || tick == 65) {
            double dist = 3.0D;
            double x = player.getX() + Math.cos(seq.baseAngle) * dist;
            double y = player.getY() + (rand.nextDouble() - 0.5) * 0.5D;
            double z = player.getZ() + Math.sin(seq.baseAngle) * dist;
            
            net.minecraft.sounds.SoundEvent mineSound = tick == 55 ? SoundEvents.STONE_HIT : SoundEvents.STONE_BREAK;
            player.level().playSound(null, x, y, z, mineSound, SoundSource.PLAYERS, 0.55F, 0.45F + rand.nextFloat() * 0.3F);
        }
        
        // CHEST OPEN: tick 80
        else if (tick == 80) {
            double dist = 3.0D;
            double x = player.getX() + Math.cos(seq.baseAngle) * dist;
            double y = player.getY();
            double z = player.getZ() + Math.sin(seq.baseAngle) * dist;
            
            player.level().playSound(null, x, y, z, SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.45F, 0.75F + rand.nextFloat() * 0.2F);
        }
        
        // ITEM PICKUP / RUSTLE: tick 95
        else if (tick == 95) {
            double dist = 2.5D;
            double x = player.getX() + Math.cos(seq.baseAngle) * dist;
            double y = player.getY();
            double z = player.getZ() + Math.sin(seq.baseAngle) * dist;
            
            player.level().playSound(null, x, y, z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.45F, 0.55F + rand.nextFloat() * 0.3F);
        }
        
        // CHEST CLOSE: tick 110
        else if (tick == 110) {
            double dist = 3.0D;
            double x = player.getX() + Math.cos(seq.baseAngle) * dist;
            double y = player.getY();
            double z = player.getZ() + Math.sin(seq.baseAngle) * dist;
            
            player.level().playSound(null, x, y, z, SoundEvents.CHEST_CLOSE, SoundSource.PLAYERS, 0.45F, 0.75F + rand.nextFloat() * 0.2F);
        }
        
        // FOOTSTEPS RETREATING: ticks 125, 135, 145
        else if (tick == 125 || tick == 135 || tick == 145) {
            double dist = 3.0D + ((tick - 120.0D) / 30.0D) * 7.0D; // walk away
            double x = player.getX() + Math.cos(seq.baseAngle) * dist;
            double y = player.getY() + (rand.nextDouble() - 0.5) * 0.5D;
            double z = player.getZ() + Math.sin(seq.baseAngle) * dist;
            
            float volume = 0.45F - ((tick - 120.0F) / 30.0F) * 0.25F;
            float pitch = 0.55F + rand.nextFloat() * 0.25F;
            player.level().playSound(null, x, y, z, seq.stepSound, SoundSource.PLAYERS, volume, pitch);
        }
        
        // EXPIRATION: tick 150
        else if (tick >= 150) {
            ACTIVE_SEQUENCES.remove(uuid);
        }
    }

    public static void triggerDonorMemory(ServerPlayer player) {
        String msg = DONOR_MEMORIES[RANDOM.nextInt(DONOR_MEMORIES.length)];
        player.sendSystemMessage(Component.literal("§d[Memory] " + msg));
    }

    private static void triggerRandomFlare(ServerPlayer player, MetalArtsData data) {
        var burning = data.burningMetals();
        if (burning.isEmpty()) return;
        var metals = burning.toArray(new com.not_noah.mistborn_metal_arts.api.Metal[0]);
        var metal = metals[RANDOM.nextInt(metals.length)];
        
        // Flare it for 60 ticks (3 seconds) if not already flaring
        if (!data.isFlaring(metal)) {
            data.setFlaring(metal, true);
            player.sendSystemMessage(Component.translatable("message.mistborn_metal_arts.power_surge", metal.displayName()));
            // We can schedule turning it off or just let the player handle it, but flaring increases burn rate
        }
    }

    public static void triggerBerserk(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§c[Rage] A foreign will forces your hands to strike!"));
        
        AABB box = player.getBoundingBox().inflate(6.0D);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        if (!targets.isEmpty()) {
            LivingEntity nearest = targets.get(0);
            double dist = player.distanceToSqr(nearest);
            for (LivingEntity e : targets) {
                if (player.distanceToSqr(e) < dist) {
                    nearest = e;
                    dist = player.distanceToSqr(e);
                }
            }

            // Force attack nearest
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, nearest.getEyePosition());
            player.swing(InteractionHand.MAIN_HAND);
            player.attack(nearest);
            
            // Add a short strength boost and speed
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
        }
    }

    private static void decaySpikesStrength(MetalArtsData data) {
        for (int i = 0; i < data.installedSpikes().size(); i++) {
            MetalArtsData.InstalledSpike spike = data.installedSpikes().get(i);
            float newStrength = spike.strength() * 0.99F; // Lose 1% strength
            data.updateSpikeStrength(i, newStrength);
        }
    }
}
