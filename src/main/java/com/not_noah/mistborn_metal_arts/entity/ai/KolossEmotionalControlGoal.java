package com.not_noah.mistborn_metal_arts.entity.ai;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

public class KolossEmotionalControlGoal extends Goal {
    private final MetalbornEnemy mob;
    private Player controller = null;

    public KolossEmotionalControlGoal(MetalbornEnemy mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (mob.role() != MetalbornRole.KOLOSS || !mob.isAlive()) {
            return false;
        }
        boolean hasEffect = mob.hasEffect(ModEffects.EMOTIONAL_RIOT.get()) || mob.hasEffect(ModEffects.EMOTIONAL_SOOTHE.get());
        if (!hasEffect) {
            return false;
        }
        
        Optional<Player> nearestSeeker = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(16.0D),
            p -> p.isAlive() && p.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> 
                data.isBurning(Metal.ZINC) || data.isBurning(Metal.BRASS)
            ).orElse(false)
        ).stream().min(Comparator.comparingDouble(mob::distanceToSqr));

        if (nearestSeeker.isPresent()) {
            controller = nearestSeeker.get();
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        if (controller != null) {
            mob.setTarget(null);
        }
    }

    @Override
    public void tick() {
        if (controller == null || !controller.isAlive()) {
            stop();
            return;
        }

        LivingEntity commandTarget = controller.getLastHurtMob();
        if (commandTarget == null || !commandTarget.isAlive() || commandTarget == mob) {
            commandTarget = controller.getLastHurtByMob();
        }

        if (commandTarget != null && commandTarget.isAlive() && commandTarget != mob) {
            mob.setTarget(commandTarget);
        } else {
            mob.getNavigation().moveTo(controller, 1.25D);
        }

        if (mob.getTarget() == controller) {
            mob.setTarget(null);
        }
    }

    @Override
    public void stop() {
        controller = null;
    }
}
