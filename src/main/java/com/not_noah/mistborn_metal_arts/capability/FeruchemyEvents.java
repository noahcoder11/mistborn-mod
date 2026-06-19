package com.not_noah.mistborn_metal_arts.capability;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.feruchemy.FeruchemyManager;
import com.not_noah.mistborn_metal_arts.api.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MistbornMetalArts.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FeruchemyEvents {

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                event.setDamageMultiplier(FeruchemyManager.adjustFallDamage(player, data, event.getDamageMultiplier()));
            });
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            if (data.isTapping(Metal.PEWTER)) {
                int tapLevel = data.feruchemyMode(Metal.PEWTER);
                if (player.getMainHandItem().isEmpty()) {
                    BlockState state = event.getState();
                    BlockPos pos = event.getPosition().orElse(BlockPos.ZERO);
                    if (state.getDestroySpeed(player.level(), pos) > 0.0F) {
                        event.setNewSpeed(Math.max(event.getNewSpeed(), 8.0F * tapLevel));
                    }
                } else {
                    event.setNewSpeed(event.getNewSpeed() * (1.0F + 1.5F * tapLevel));
                }
            }
        });
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().isEmpty()) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isTapping(Metal.PEWTER) && data.feruchemyMode(Metal.PEWTER) >= 2) {
                    event.setCanHarvest(true);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        event.getEntity().getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            if (data.isTapping(Metal.CHROMIUM)) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
            }
        });
    }

    @SubscribeEvent
    public static void onLootingLevel(LootingLevelEvent event) {
        if (event.getDamageSource().getEntity() instanceof Player player) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isTapping(Metal.CHROMIUM)) {
                    int tapLevel = data.feruchemyMode(Metal.CHROMIUM);
                    event.setLootingLevel(event.getLootingLevel() + tapLevel * 2);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            if (data.isStoring(Metal.ZINC)) {
                ExperienceOrb orb = event.getOrb();
                try {
                    java.lang.reflect.Field valueField;
                    try {
                        valueField = ExperienceOrb.class.getDeclaredField("value");
                    } catch (NoSuchFieldException e) {
                        valueField = ExperienceOrb.class.getDeclaredField("f_20770_");
                    }
                    valueField.setAccessible(true);
                    int currentVal = valueField.getInt(orb);
                    valueField.setInt(orb, currentVal / 2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SubscribeEvent
    public static void onLivingKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isStoring(Metal.IRON)) {
                    event.setStrength(event.getStrength() * 1.8F);
                } else if (data.isTapping(Metal.IRON)) {
                    int tapLevel = data.feruchemyMode(Metal.IRON);
                    float factor = Math.max(0.0F, 1.0F - 0.22F * tapLevel);
                    event.setStrength(event.getStrength() * factor);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player != null && !player.level().isClientSide()) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                if (data.isTapping(Metal.CHROMIUM)) {
                    int tapLevel = data.feruchemyMode(Metal.CHROMIUM);
                    if (player.getRandom().nextFloat() < 0.15F * tapLevel) {
                        BlockState state = event.getState();
                        ItemStack drop = new ItemStack(state.getBlock().asItem());
                        if (!drop.isEmpty()) {
                            BlockPos pos = event.getPos();
                            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                                player.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop
                            );
                            player.level().addFreshEntity(itemEntity);
                        }
                    }
                }
            });
        }
    }
}
