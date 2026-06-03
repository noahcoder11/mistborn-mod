package com.not_noah.mistborn_metal_arts.entity.ai;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;

public class CoppercloudGoal extends Goal {
    private final MetalbornEnemy mob;
    private int cooldown = 0;

    public CoppercloudGoal(MetalbornEnemy mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        return mob.getCapability(MetalArtsCapabilities.METAL_ARTS)
                .map(data -> data.isBurning(Metal.COPPER) && data.getReserve(Metal.COPPER) >= 15.0F)
                .orElse(false);
    }

    @Override
    public void start() {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.consumeReserve(Metal.COPPER, 15.0F);

            List<MetalbornEnemy> allies = level.getEntitiesOfClass(MetalbornEnemy.class, mob.getBoundingBox().inflate(8.0D), LivingEntity::isAlive);
            for (MetalbornEnemy ally : allies) {
                ally.addEffect(new MobEffectInstance(ModEffects.COPPERCLOUD.get(), 120, 0, true, true));
            }

            for (int i = 0; i < 28; i++) {
                double ox = (mob.getRandom().nextDouble() - 0.5D) * 8.0D;
                double oy = mob.getRandom().nextDouble() * 2.6D;
                double oz = (mob.getRandom().nextDouble() - 0.5D) * 8.0D;
                level.sendParticles(ModParticles.COPPERCLOUD.get(), mob.getX() + ox, mob.getY() + oy, mob.getZ() + oz, 1, 0D, 0D, 0D, 0D);
            }
        });

        cooldown = 130;
    }
}
