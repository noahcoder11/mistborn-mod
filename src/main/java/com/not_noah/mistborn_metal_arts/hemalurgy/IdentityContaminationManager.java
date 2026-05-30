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
        // 1. Passive Decay
        boolean isResting = player.getDeltaMovement().horizontalDistanceSqr() < 0.001
                && (player.tickCount - player.getLastHurtByMobTimestamp() > 100)
                && data.burningMetals().isEmpty();

        if (isResting) {
            double decay = ServerConfig.VALUES.contaminationDecayRate.get();
            // Aluminum Feruchemy: storing Identity accelerates decay x3 (handled if storing identity, but let's check basic for now)
            // If storing identity (feruchemy), we can scale it.
            // Let's check if player is storing Aluminum (we will check if tapping or storing)
            // For now, let's decay contamination passively:
            float finalDecay = (float) decay;
            data.reduceIdentityContamination(finalDecay);
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
                playWhisperSound(player);
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

    public static void playWhisperSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.SOUL_ESCAPE, SoundSource.AMBIENT, 0.25F, 0.4F + RANDOM.nextFloat() * 0.4F);
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
