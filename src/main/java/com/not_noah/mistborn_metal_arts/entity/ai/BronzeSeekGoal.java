package com.not_noah.mistborn_metal_arts.entity.ai;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

public class BronzeSeekGoal extends Goal {
    private final MetalbornEnemy mob;
    private int cooldown = 0;
    private int reinforcementCooldown = 220;

    public BronzeSeekGoal(MetalbornEnemy mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (reinforcementCooldown > 0) {
            reinforcementCooldown--;
        }
        return mob.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(Metal.BRONZE) && data.getReserve(Metal.BRONZE) >= 10.0F)
                .orElse(false);
    }

    @Override
    public void start() {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        double range = mob.role().isBoss() ? 48.0D : 28.0D;
        Optional<Player> targetPlayer = level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(range),
                        player -> player.isAlive() && !player.isCreative() && !player.isSpectator())
                .stream()
                .filter(player -> isDetectable(player))
                .min(Comparator.comparingDouble(mob::distanceToSqr));

        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.consumeReserve(Metal.BRONZE, 10.0F);

            if (targetPlayer.isPresent()) {
                Player player = targetPlayer.get();
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));

                boolean isHostile = mob.role() != MetalbornRole.KANDRA || mob.getLastHurtByMob() == player;
                if (isHostile) {
                    mob.setTarget(player);
                    if (reinforcementCooldown <= 0 && mob.role() == MetalbornRole.SEEKER) {
                        spawnReinforcement(level, player.blockPosition().offset(mob.getRandom().nextInt(5) - 2, 0, mob.getRandom().nextInt(5) - 2));
                        reinforcementCooldown = 420;
                    }
                }

                drawLine(level, mob.getEyePosition(), player.getEyePosition());
            }

            level.sendParticles(ModParticles.BRONZE_PULSE.get(), mob.getX(), mob.getY() + 1.5, mob.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
        });

        cooldown = mob.role().isBoss() ? 70 : 110;
    }

    private boolean isDetectable(Player player) {
        boolean trelliumShielded = player.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(Metal.TRELLIUM) || data.installedSpikes().stream().anyMatch(s -> s.spikeMetal() == Metal.TRELLIUM))
                .orElse(false);
        if (trelliumShielded) {
            return false;
        }

        boolean copperShielded = player.hasEffect(com.not_noah.mistborn_metal_arts.registry.ModEffects.COPPERCLOUD.get())
                || player.getCapability(MetalArtsCapabilities.METAL_ARTS).map(d -> d.isCopperclouded()).orElse(false);
        if (copperShielded) {
            if (!mob.role().isBoss()) {
                return false;
            }
        }

        return player.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> !data.burningMetals().isEmpty())
                .orElse(false);
    }

    private void spawnReinforcement(ServerLevel level, BlockPos pos) {
        net.minecraft.world.entity.EntityType<MetalbornEnemy> type = mob.getRandom().nextBoolean()
                ? com.not_noah.mistborn_metal_arts.registry.ModEntityTypes.PEWTER_THUG.get()
                : com.not_noah.mistborn_metal_arts.registry.ModEntityTypes.LURCHER_GUARD.get();
        MetalbornEnemy guard = type.create(level);
        if (guard == null) {
            return;
        }
        guard.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, mob.getRandom().nextFloat() * 360.0F, 0F);
        guard.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.REINFORCEMENT, null, null);
        level.addFreshEntity(guard);
    }

    private void drawLine(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(4, Math.min(24, (int) (delta.length() * 2.0D)));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            level.sendParticles(ModParticles.BRONZE_PULSE.get(), point.x, point.y, point.z, 1, 0D, 0D, 0D, 0D);
        }
    }
}
