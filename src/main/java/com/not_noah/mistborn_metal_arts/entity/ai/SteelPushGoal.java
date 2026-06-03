package com.not_noah.mistborn_metal_arts.entity.ai;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import com.not_noah.mistborn_metal_arts.allomancy.MetalForceHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SteelPushGoal extends Goal {
    private final MetalbornEnemy mob;
    private int cooldown = 0;
    private final double baseStrength;
    private int duration = 0;

    public SteelPushGoal(MetalbornEnemy mob, double baseStrength) {
        this.mob = mob;
        this.baseStrength = baseStrength;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || mob.distanceToSqr(target) > 256.0D) {
            return false;
        }
        if (!MetalForceHelper.isMetallicEntity(target)) {
            return false;
        }
        return mob.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(Metal.STEEL) && data.getReserve(Metal.STEEL) >= 5.0F)
                .orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        if (duration <= 0 || !mob.isAlive()) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || mob.distanceToSqr(target) > 256.0D) {
            return false;
        }
        return mob.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(Metal.STEEL) && data.getReserve(Metal.STEEL) >= 2.0F)
                .orElse(false);
    }

    @Override
    public void start() {
        duration = 20 + mob.getRandom().nextInt(15);
    }

    @Override
    public void tick() {
        duration--;
        LivingEntity target = mob.getTarget();
        if (target == null || !(mob.level() instanceof ServerLevel level)) {
            return;
        }

        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            float strengthMult = data.getEffectiveStrength(Metal.STEEL);
            float flareFactor = data.isFlaring(Metal.STEEL) ? 1.85F : 1.0F;
            double distance = Math.max(1.0D, mob.distanceTo(target));
            double force = (this.baseStrength * strengthMult * flareFactor) / Math.sqrt(distance);

            Vec3 direction = target.position().subtract(mob.position());
            if (direction.lengthSqr() >= 0.01D) {
                Vec3 push = direction.normalize().scale(force * 0.15D);
                if (target.onGround()) {
                    push = push.add(0D, 0.12D, 0D);
                }
                MetalForceHelper.pushEntity(target, push);

                double mobShare = 0.5D;
                Vec3 mobPush = direction.normalize().scale(-force * 0.15D * mobShare);
                MetalForceHelper.pushEntity(mob, mobPush);

                if (duration % 4 == 0) {
                    drawLine(level, mob.getEyePosition(), target.getEyePosition());
                    level.playSound(null, mob.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 0.35F, 1.45F);
                }

                data.consumeReserve(Metal.STEEL, 1.2F);
            }
        });
    }

    @Override
    public void stop() {
        duration = 0;
        cooldown = 40 + mob.getRandom().nextInt(40);
    }

    private void drawLine(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(4, Math.min(24, (int) (delta.length() * 2.0D)));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            level.sendParticles(ModParticles.METAL_LINE.get(), point.x, point.y, point.z, 1, 0D, 0D, 0D, 0D);
        }
    }
}
