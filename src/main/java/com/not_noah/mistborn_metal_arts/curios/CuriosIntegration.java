package com.not_noah.mistborn_metal_arts.curios;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import com.not_noah.mistborn_metal_arts.item.MetalmindItem;

public final class CuriosIntegration {
    private static final String[] PREFERRED_SLOTS = {
        "physical_quadrant", "mental_quadrant", "spiritual_quadrant", "temporal_quadrant",
        "head", "necklace", "back", "body", "belt", "ring", "hands", "bracelet", "charm"
    };
    private static final String[] METALMIND_SLOTS = {
        "ring", "hands", "bracelet", "necklace", "belt", "back", "head",
        "metalmind_ring", "metalmind_bracer", "metalmind_necklace"
    };

    private CuriosIntegration() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        if (event.getObject().getItem() instanceof HemalurgicSpikeItem) {
            event.addCapability(new ResourceLocation(MistbornMetalArts.MOD_ID, "curio"), new ICapabilityProvider() {
                private final LazyOptional<ICurio> curio = LazyOptional.of(() -> new SpikeCurio(event.getObject()));

                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                    return CuriosCapability.ITEM.orEmpty(cap, curio);
                }
            });
        } else if (event.getObject().getItem() instanceof MetalmindItem) {
            event.addCapability(new ResourceLocation(MistbornMetalArts.MOD_ID, "curio"), new ICapabilityProvider() {
                private final LazyOptional<ICurio> curio = LazyOptional.of(() -> new MetalmindCurio(event.getObject()));

                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                    return CuriosCapability.ITEM.orEmpty(cap, curio);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onCurioChange(CurioChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(MetalArtsData::markNeedsPowerRefresh);
        }
    }

    public static ItemStack findMetalmind(ServerPlayer player, Metal metal) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player).resolve();
        if (optional.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ICuriosItemHandler handler = optional.get();
        for (String slotType : METALMIND_SLOTS) {
            Optional<ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotType);
            if (stacksHandler.isEmpty()) {
                continue;
            }
            IItemHandlerModifiable stacks = stacksHandler.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.getItem() instanceof MetalmindItem item && item.metal() == metal) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean equipSpikeFromUse(ServerPlayer player, ItemStack sourceStack) {
        if (!isChargedSpike(sourceStack)) {
            return false;
        }
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player).resolve();
        if (optional.isEmpty()) {
            return false;
        }
        ICuriosItemHandler handler = optional.get();
        if (!canAddAnotherSpike(player, handler, sourceStack)) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.too_many_spikes"), true);
            return false;
        }
        for (String slotType : PREFERRED_SLOTS) {
            Optional<ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotType);
            if (stacksHandler.isEmpty()) {
                continue;
            }
            ICurioStacksHandler stacks = stacksHandler.get();
            for (int i = 0; i < stacks.getStacks().getSlots(); i++) {
                if (!stacks.getStacks().getStackInSlot(i).isEmpty()) {
                    continue;
                }
                ItemStack equipped = sourceStack.copy();
                equipped.setCount(1);
                SlotContext context = new SlotContext(slotType, player, i, false, true);
                if (!CuriosApi.isStackValid(context, equipped)) {
                    continue;
                }
                handler.setEquippedCurio(slotType, i, equipped);
                if (!player.getAbilities().instabuild) {
                    sourceStack.shrink(1);
                }
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    data.refreshPowers();
                    refreshEquippedHemalurgicSpikes(player, data);
                });
                return true;
            }
        }
        return false;
    }

    public static boolean removeEquippedSpike(ServerPlayer player) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player).resolve();
        if (optional.isEmpty()) {
            return false;
        }
        ICuriosItemHandler handler = optional.get();
        for (int slotIndex = PREFERRED_SLOTS.length - 1; slotIndex >= 0; slotIndex--) {
            String slotType = PREFERRED_SLOTS[slotIndex];
            Optional<ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotType);
            if (stacksHandler.isEmpty()) {
                continue;
            }
            ICurioStacksHandler stacks = stacksHandler.get();
            for (int i = stacks.getStacks().getSlots() - 1; i >= 0; i--) {
                ItemStack stack = stacks.getStacks().getStackInSlot(i);
                if (!isChargedSpike(stack)) {
                    continue;
                }
                handler.setEquippedCurio(slotType, i, ItemStack.EMPTY);
                if (!player.getInventory().add(stack.copy())) {
                    player.drop(stack.copy(), false);
                }
                player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                    data.refreshPowers();
                    refreshEquippedHemalurgicSpikes(player, data);
                });
                return true;
            }
        }
        return false;
    }

    public static void refreshEquippedHemalurgicSpikes(ServerPlayer player, MetalArtsData data) {
        data.setEquippedSpikeCorruption(0);
        data.clearEquippedStrengths();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            int corruption = 0;
            for (String slotType : PREFERRED_SLOTS) {
                Optional<ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotType);
                if (stacksHandler.isEmpty()) {
                    continue;
                }
                ICurioStacksHandler stacks = stacksHandler.get();
                for (int i = 0; i < stacks.getStacks().getSlots(); i++) {
                    ItemStack stack = stacks.getStacks().getStackInSlot(i);
                    if (stack.getItem() instanceof HemalurgicSpikeItem spike && spike.charged()) {
                        CompoundTag tag = stack.getOrCreateTag();
                        String powerType = normalizedPowerType(tag.getString("PowerType"), spike.metal());
                        Metal powerMetal = Metal.byName(tag.getString("PowerMetal")).orElse(spike.metal());
                        float baseStrength = tag.contains("Strength") ? tag.getFloat("Strength") : 1.0F;
                        float efficiency = isMatchingQuadrant(slotType, spike.metal()) ? 1.0F : 0.2F;
                        float strength = baseStrength * efficiency;

                        data.addSpikePower(powerMetal, powerType);
                        if ("allomancy".equals(powerType)) {
                            data.addEquippedStrength(powerMetal, strength);
                        }
                        corruption += Math.max(1, Math.round(2F * Math.max(0.05F, strength)));
                    }
                }
            }
            data.setEquippedSpikeCorruption(corruption);
        });
    }

    public static java.util.List<ItemStack> getEquippedSpikes(ServerPlayer player) {
        java.util.List<ItemStack> spikes = new java.util.ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (String slotType : PREFERRED_SLOTS) {
                handler.getStacksHandler(slotType).ifPresent(stacksHandler -> {
                    var stacks = stacksHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStackInSlot(i);
                        if (stack.getItem() instanceof HemalurgicSpikeItem spike && spike.charged()) {
                            spikes.add(stack);
                        }
                    }
                });
            }
        });
        return spikes;
    }

    private static boolean isMatchingQuadrant(String slotType, Metal metal) {
        return switch (slotType) {
            case "physical_quadrant" -> metal == Metal.STEEL || metal == Metal.IRON || metal == Metal.TIN || metal == Metal.PEWTER;
            case "mental_quadrant" -> metal == Metal.ZINC || metal == Metal.BRASS || metal == Metal.COPPER || metal == Metal.BRONZE;
            case "spiritual_quadrant" -> metal == Metal.GOLD || metal == Metal.ELECTRUM || metal == Metal.CHROMIUM || metal == Metal.NICROSIL;
            case "temporal_quadrant" -> metal == Metal.CADMIUM || metal == Metal.BENDALLOY || metal == Metal.ALUMINUM || metal == Metal.DURALUMIN || metal == Metal.ATIUM || metal == Metal.LERASIUM;
            default -> false;
        };
    }

    private static boolean canAddAnotherSpike(Player player, ICuriosItemHandler handler, ItemStack currentStack) {
        int equipped = 0;
        for (String slotType : PREFERRED_SLOTS) {
            Optional<ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotType);
            if (stacksHandler.isEmpty()) {
                continue;
            }
            IItemHandlerModifiable stacks = stacksHandler.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack s = stacks.getStackInSlot(i);
                if (s != currentStack && isChargedSpike(s)) {
                    equipped++;
                }
            }
        }
        int installed = player.getCapability(MetalArtsCapabilities.METAL_ARTS).map(data -> data.installedSpikes().size()).orElse(0);
        return installed + equipped < ServerConfig.VALUES.maxInstalledSpikes.get();
    }

    private static boolean isChargedSpike(ItemStack stack) {
        return stack.getItem() instanceof HemalurgicSpikeItem spike && spike.charged();
    }

    public static int getEquippedSpikeCount(Player player) {
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            int equipped = 0;
            for (String slotType : PREFERRED_SLOTS) {
                var stacksHandler = handler.getStacksHandler(slotType);
                if (stacksHandler.isEmpty()) continue;
                var stacks = stacksHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (isChargedSpike(stacks.getStackInSlot(i))) {
                        equipped++;
                    }
                }
            }
            return equipped;
        }).orElse(0);
    }

    public static boolean replaceCurioStack(Player player, ItemStack original, ItemStack replacement) {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player).resolve();
        if (optional.isEmpty()) {
            return false;
        }
        ICuriosItemHandler handler = optional.get();
        for (String slotType : PREFERRED_SLOTS) {
            Optional<ICurioStacksHandler> stacksHandler = handler.getStacksHandler(slotType);
            if (stacksHandler.isEmpty()) {
                continue;
            }
            ICurioStacksHandler stacks = stacksHandler.get();
            for (int i = 0; i < stacks.getStacks().getSlots(); i++) {
                if (stacks.getStacks().getStackInSlot(i) == original) {
                    stacks.getStacks().setStackInSlot(i, replacement);
                    if (player instanceof ServerPlayer serverPlayer) {
                        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                            data.refreshPowers();
                            refreshEquippedHemalurgicSpikes(serverPlayer, data);
                        });
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static String normalizedPowerType(String type, Metal fallbackMetal) {
        if (!type.isBlank()) {
            return type;
        }
        return fallbackMetal.isFeruchemical() ? "feruchemy" : "allomancy";
    }

    private static class SpikeCurio implements ICurio {
        private final ItemStack stack;

        public SpikeCurio(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack getStack() {
            return stack;
        }

        @Override
        public void onEquip(SlotContext slotContext, ItemStack prevStack) {
            LivingEntity entity = slotContext.entity();
            if (!entity.level().isClientSide()) {
                stack.getOrCreateTag().putString("StoredState", "equipped");
                stack.getOrCreateTag().putLong("LastUpdateTime", entity.level().getGameTime());
                if (prevStack.getItem() != stack.getItem()) {
                    entity.level().playSound(null, entity.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 0.65F);
                    entity.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                        data.addIdentityContamination((float) ServerConfig.VALUES.contaminationPerSpike.get().doubleValue());
                        if (entity instanceof ServerPlayer serverPlayer) {
                            MetalArtsNetwork.sync(serverPlayer);
                        }
                    });
                }
            }
        }

        @Override
        public void onUnequip(SlotContext slotContext, ItemStack newStack) {
            LivingEntity entity = slotContext.entity();
            if (!entity.level().isClientSide()) {
                stack.getOrCreateTag().putString("StoredState", "normal");
                stack.getOrCreateTag().putLong("LastUpdateTime", entity.level().getGameTime());
                if (newStack.getItem() != stack.getItem()) {
                    entity.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
                        data.reduceIdentityContamination((float) ServerConfig.VALUES.contaminationPerSpike.get().doubleValue());
                        data.setSpiritualScarring(data.spiritualScarring() + 2.0F); // Unequipping a spike curios slot inflicts +2.0% spiritual scarring
                        if (entity instanceof ServerPlayer serverPlayer) {
                            MetalArtsNetwork.sync(serverPlayer);
                        }
                    });
                }
            }
        }

        @Override
        public boolean canEquip(SlotContext slotContext) {
            if (!isChargedSpike(stack)) {
                return false;
            }
            if (slotContext.entity() instanceof ServerPlayer serverPlayer) {
                return CuriosApi.getCuriosInventory(serverPlayer).resolve().map(handler -> canAddAnotherSpike(serverPlayer, handler, stack)).orElse(true);
            }
            return slotContext.entity().level().isClientSide;
        }

        @Override
        public boolean canUnequip(SlotContext slotContext) {
            return true;
        }

        @Override
        public boolean canEquipFromUse(SlotContext slotContext) {
            return isChargedSpike(stack);
        }
    }

    private static class MetalmindCurio implements ICurio {
        private final ItemStack stack;

        public MetalmindCurio(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack getStack() {
            return stack;
        }

        @Override
        public void onEquip(SlotContext slotContext, ItemStack prevStack) {
            LivingEntity entity = slotContext.entity();
            if (!entity.level().isClientSide() && prevStack.getItem() != stack.getItem()) {
                entity.level().playSound(null, entity.blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 0.55F, 1.25F);
            }
        }

        @Override
        public boolean canEquipFromUse(SlotContext slotContext) {
            return true;
        }
    }
}
