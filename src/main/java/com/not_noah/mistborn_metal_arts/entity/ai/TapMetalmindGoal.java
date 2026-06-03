package com.not_noah.mistborn_metal_arts.entity.ai;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class TapMetalmindGoal extends Goal {
    private final MetalbornEnemy mob;

    public TapMetalmindGoal(MetalbornEnemy mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!mob.isAlive() || mob.getTarget() == null) {
            return false;
        }
        return mob.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> {
            for (Metal metal : Metal.cachedValues()) {
                if (data.hasFeruchemicalPower(metal) && data.getMetalmindCharge(metal) > 0.0F) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    @Override
    public void start() {
        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            int tapLevel = mob.role().isBoss() ? 2 : 1;
            for (Metal metal : Metal.cachedValues()) {
                if (data.hasFeruchemicalPower(metal) && data.getMetalmindCharge(metal) > 0.0F) {
                    data.setFeruchemyMode(metal, tapLevel);
                }
            }
        });
    }

    @Override
    public void tick() {
        if (mob.getTarget() == null) {
            stop();
            return;
        }
        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            for (Metal metal : Metal.cachedValues()) {
                if (data.feruchemyMode(metal) > 0 && data.getMetalmindCharge(metal) <= 0.0F) {
                    data.setFeruchemyMode(metal, 0);
                }
            }
        });
    }

    @Override
    public void stop() {
        mob.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            for (Metal metal : Metal.cachedValues()) {
                data.setFeruchemyMode(metal, 0);
            }
        });
    }
}
