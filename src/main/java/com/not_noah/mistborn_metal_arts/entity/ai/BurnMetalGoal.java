package com.not_noah.mistborn_metal_arts.entity.ai;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BurnMetalGoal extends Goal {
    private final MetalbornEnemy mob;
    private int noTargetTicks = 0;

    public BurnMetalGoal(MetalbornEnemy mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return mob.isAlive() && mob.getTarget() != null;
    }

    @Override
    public void start() {
        noTargetTicks = 0;
        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            for (Metal metal : Metal.cachedValues()) {
                if (data.hasAllomanticPower(metal) && data.getReserve(metal) > 0F) {
                    data.startBurning(metal);
                }
            }
        });
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            noTargetTicks++;
            if (noTargetTicks > 40) {
                stop();
            }
            return;
        }
        noTargetTicks = 0;

        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            boolean shouldFlare = mob.getHealth() < mob.getMaxHealth() * 0.5F || mob.distanceToSqr(target) < 36.0D;
            for (Metal metal : Metal.cachedValues()) {
                if (data.isBurning(metal)) {
                    if (data.getReserve(metal) <= 0F) {
                        data.stopBurning(metal);
                    } else {
                        data.setFlaring(metal, shouldFlare);
                    }
                } else if (data.hasAllomanticPower(metal) && data.getReserve(metal) > 0F) {
                    data.startBurning(metal);
                }
            }
        });
    }

    @Override
    public void stop() {
        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            for (Metal metal : Metal.cachedValues()) {
                data.stopBurning(metal);
                data.setFlaring(metal, false);
            }
        });
    }
}
