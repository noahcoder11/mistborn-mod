package com.not_noah.mistborn_metal_arts.entity.ai;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class EmotionalAllomancyGoal extends Goal {
    private final MetalbornEnemy mob;
    private int cooldown = 0;

    public EmotionalAllomancyGoal(MetalbornEnemy mob) {
        this.mob = mob;
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
        boolean isRioter = mob.role() == MetalbornRole.RIOTER;
        Metal metal = isRioter ? Metal.ZINC : Metal.BRASS;

        return mob.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(metal) && data.getReserve(metal) >= 20.0F)
                .orElse(false);
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null || !(mob.level() instanceof ServerLevel level)) {
            return;
        }

        boolean isRioter = mob.role() == MetalbornRole.RIOTER;
        Metal metal = isRioter ? Metal.ZINC : Metal.BRASS;

        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.consumeReserve(metal, 20.0F);

            if (isRioter) {
                for (Monster monster : level.getEntitiesOfClass(Monster.class, mob.getBoundingBox().inflate(12.0D), m -> m != mob && m.isAlive())) {
                    monster.setTarget(target);
                    monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
                }
                target.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_PRESSURE.get(), 100, 1));
                level.sendParticles(ModParticles.EMOTIONAL_WAVE.get(), target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.5, 0.5, 0.5, 0.01);
                drawLine(level, mob.getEyePosition(), target.getEyePosition(), ModParticles.EMOTIONAL_WAVE.get());
                cooldown = 100;
            } else {
                target.addEffect(new MobEffectInstance(ModEffects.EMOTIONAL_PRESSURE.get(), 120, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                level.sendParticles(ModParticles.EMOTIONAL_WAVE.get(), target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.4, 0.4, 0.4, 0.01);
                drawLine(level, mob.getEyePosition(), target.getEyePosition(), ModParticles.EMOTIONAL_WAVE.get());
                cooldown = 120;
            }
        });
    }

    private void drawLine(ServerLevel level, Vec3 start, Vec3 end, net.minecraft.core.particles.SimpleParticleType particle) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(4, Math.min(24, (int) (delta.length() * 2.0D)));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(delta.scale(i / (double) steps));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0D, 0D, 0D, 0D);
        }
    }
}
