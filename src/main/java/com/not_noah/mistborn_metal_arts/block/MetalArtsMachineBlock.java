package com.not_noah.mistborn_metal_arts.block;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.hemalurgy.HemalurgyManager;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import com.not_noah.mistborn_metal_arts.item.MetalmindItem;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Optional;

import com.not_noah.mistborn_metal_arts.block.entity.MetalArtsMachineBlockEntity;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class MetalArtsMachineBlock extends Block implements EntityBlock {
    private final String machineId;
    private final String tooltipKey;

    public MetalArtsMachineBlock(Properties properties, String machineId, String tooltipKey) {
        super(properties);
        this.machineId = machineId;
        this.tooltipKey = tooltipKey;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MetalArtsMachineBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MetalArtsMachineBlockEntity machineBe) {
                // Shift-right click for direct interaction (old behavior)
                if (player.isSecondaryUseActive()) {
                    if (handleInteraction(level, pos, serverPlayer, hand)) {
                        return InteractionResult.CONSUME;
                    }
                }
                NetworkHooks.openScreen(serverPlayer, machineBe, pos);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MetalArtsMachineBlockEntity machineBe) {
                machineBe.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), handler.getStackInSlot(i));
                    }
                });
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    private boolean handleInteraction(Level level, BlockPos pos, ServerPlayer player, InteractionHand hand) {
        return switch (machineId) {
            case "metallurgy_table" -> createFlakes(level, pos, player, hand);
            case "metalworking_table" -> craftMetalmind(level, pos, player, hand);
            case "alloy_furnace" -> craftAlloy(level, pos, player);
            case "spike_press" -> pressSpike(level, pos, player, hand);
            case "bind_point_table" -> bindSpike(level, pos, player, hand);
            case "metalmind_charging_stand" -> inspectOrToggleMetalmind(level, pos, player, hand);
            default -> false;
        };
    }

    private boolean createFlakes(Level level, BlockPos pos, ServerPlayer player, InteractionHand hand) {
        Optional<Metal> metal = metalFrom(player.getItemInHand(hand), ModItems.METAL_INGOTS);
        if (metal.isEmpty()) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        giveOrDrop(player, new net.minecraft.world.item.ItemStack(ModItems.METAL_FLAKES.get(metal.get()).get(), 6));
        play(level, pos, SoundEvents.GRINDSTONE_USE, 0.8F, 1.15F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.machine_flakes", metal.get().displayName()), true);
        return true;
    }

    private boolean craftMetalmind(Level level, BlockPos pos, ServerPlayer player, InteractionHand hand) {
        Optional<Metal> metal = metalFrom(player.getItemInHand(hand), ModItems.METAL_INGOTS);
        if (metal.isEmpty() || !metal.get().isFeruchemical()) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        giveOrDrop(player, new net.minecraft.world.item.ItemStack(ModItems.METALMINDS.get(metal.get()).get()));
        play(level, pos, SoundEvents.CHAIN_PLACE, 0.7F, 1.4F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.machine_metalmind", metal.get().displayName()), true);
        return true;
    }

    private boolean craftAlloy(Level level, BlockPos pos, ServerPlayer player) {
        AlloyRecipe recipe = findAlloyRecipe(player);
        if (recipe == null) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            consume(player, recipe.first(), 1);
            consume(player, recipe.second(), 1);
        }
        giveOrDrop(player, new net.minecraft.world.item.ItemStack(ModItems.METAL_INGOTS.get(recipe.result()).get()));
        play(level, pos, SoundEvents.FURNACE_FIRE_CRACKLE, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.machine_alloy", recipe.result().displayName()), true);
        return true;
    }

    private AlloyRecipe findAlloyRecipe(ServerPlayer player) {
        AlloyRecipe[] recipes = {
                new AlloyRecipe(Items.IRON_INGOT, Items.COAL, Metal.STEEL),
                new AlloyRecipe(ModItems.METAL_INGOTS.get(Metal.TIN).get(), ModItems.METAL_INGOTS.get(Metal.COPPER).get(), Metal.PEWTER),
                new AlloyRecipe(ModItems.METAL_INGOTS.get(Metal.COPPER).get(), ModItems.METAL_INGOTS.get(Metal.TIN).get(), Metal.BRONZE),
                new AlloyRecipe(ModItems.METAL_INGOTS.get(Metal.COPPER).get(), ModItems.METAL_INGOTS.get(Metal.ZINC).get(), Metal.BRASS),
                new AlloyRecipe(Items.GOLD_INGOT, ModItems.METAL_INGOTS.get(Metal.SILVER).get(), Metal.ELECTRUM),
                new AlloyRecipe(ModItems.METAL_INGOTS.get(Metal.ALUMINUM).get(), ModItems.METAL_INGOTS.get(Metal.COPPER).get(), Metal.DURALUMIN),
                new AlloyRecipe(ModItems.METAL_INGOTS.get(Metal.CHROMIUM).get(), ModItems.METAL_INGOTS.get(Metal.NICKEL).get(), Metal.NICROSIL),
                new AlloyRecipe(ModItems.METAL_INGOTS.get(Metal.CADMIUM).get(), ModItems.METAL_INGOTS.get(Metal.TIN).get(), Metal.BENDALLOY)
        };
        for (AlloyRecipe recipe : recipes) {
            if (has(player, recipe.first(), 1) && has(player, recipe.second(), 1)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean pressSpike(Level level, BlockPos pos, ServerPlayer player, InteractionHand hand) {
        Optional<Metal> metal = metalFrom(player.getItemInHand(hand), ModItems.METAL_INGOTS);
        if (metal.isEmpty()) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        giveOrDrop(player, new net.minecraft.world.item.ItemStack(ModItems.SPIKE_BLANKS.get(metal.get()).get()));
        play(level, pos, SoundEvents.ANVIL_USE, 0.8F, 0.75F);
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.machine_spike", metal.get().displayName()), true);
        return true;
    }

    private boolean bindSpike(Level level, BlockPos pos, ServerPlayer player, InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof HemalurgicSpikeItem spike) || !spike.charged()) {
            return false;
        }
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            String type = stack.getOrCreateTag().getString("PowerType");
            Metal powerMetal = Metal.byName(stack.getOrCreateTag().getString("PowerMetal")).orElse(spike.metal());
            float strength = stack.getOrCreateTag().contains("Strength") ? stack.getOrCreateTag().getFloat("Strength") : 1.0F;
            if (HemalurgyManager.installSpike(player, data, spike.metal(), type.isBlank() ? "allomancy" : type, powerMetal, strength) && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        });
        play(level, pos, SoundEvents.ANVIL_LAND, 0.75F, 0.55F);
        return true;
    }

    private boolean inspectOrToggleMetalmind(Level level, BlockPos pos, ServerPlayer player, InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof MetalmindItem metalmind)) {
            return false;
        }
        MetalmindItem.ensureOwner(stack, player);
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            int mode = data.cycleFeruchemyMode(metalmind.metal());
            Component modeText = switch (mode) {
                case -1 -> Component.translatable("message.mistborn_metal_arts.feruchemy_store", metalmind.metal().displayName());
                case 1 -> Component.translatable("message.mistborn_metal_arts.feruchemy_tap", metalmind.metal().displayName());
                default -> Component.translatable("message.mistborn_metal_arts.feruchemy_off", metalmind.metal().displayName());
            };
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.machine_metalmind_charge", Math.round(MetalmindItem.getCharge(stack)), Math.round(MetalmindItem.getCapacity(stack))).append(" ").append(modeText), true);
        });
        play(level, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.5F);
        return true;
    }

    private Optional<Metal> metalFrom(net.minecraft.world.item.ItemStack stack, EnumMap<Metal, RegistryObject<Item>> items) {
        for (var entry : items.entrySet()) {
            if (stack.is(entry.getValue().get())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private boolean has(ServerPlayer player, Item item, int count) {
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    private void consume(ServerPlayer player, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
    }

    private void giveOrDrop(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void play(Level level, BlockPos pos, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }

    private record AlloyRecipe(Item first, Item second, Metal result) {
    }
}
