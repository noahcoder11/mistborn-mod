package com.not_noah.mistborn_metal_arts.block;

import com.not_noah.mistborn_metal_arts.block.entity.BloodVialBlockEntity;
import com.not_noah.mistborn_metal_arts.hemalurgy.SpikeDecayManager;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BloodVialBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 12.0D, 11.0D);

    public BloodVialBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloodVialBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BloodVialBlockEntity vialEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (vialEntity.hasSpike()) {
            // Retrieve spike
            ItemStack spike = vialEntity.getStoredSpike().copy();
            if (!spike.isEmpty()) {
                spike = SpikeDecayManager.setStoredState(spike, "normal", level);
                
                if (!player.getInventory().add(spike)) {
                    player.drop(spike, false);
                }
                
                vialEntity.setStoredSpike(ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8F, 1.3F);
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_retrieved_blood"), true);
                player.containerMenu.broadcastChanges();
                return InteractionResult.CONSUME;
            }
        } else {
            // Insert spike
            if (held.getItem() instanceof HemalurgicSpikeItem) {
                // Check if blood is spoiled
                long harvest = vialEntity.getHarvestTime();
                if (harvest > 0) {
                    long age = level.getGameTime() - harvest;
                    if (age > 72000L) {
                        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.blood_spoiled_cannot_insert").withStyle(ChatFormatting.GRAY), true);
                        return InteractionResult.FAIL;
                    }
                }

                if (vialEntity.getHarvestTime() <= 0L) {
                    vialEntity.setHarvestTime(level.getGameTime());
                }

                ItemStack inserted = held.copy();
                inserted.setCount(1);
                
                inserted = SpikeDecayManager.setStoredState(inserted, "blood", level);
                vialEntity.setStoredSpike(inserted);

                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    if (held.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                    } else {
                        player.setItemInHand(hand, held);
                    }
                }

                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.1F);
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_inserted_blood"), true);
                player.containerMenu.broadcastChanges();
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.blood_vial_empty_hint"), true);
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BloodVialBlockEntity vialEntity) {
            if (vialEntity.getHarvestTime() <= 0L) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("HarvestTime")) {
                    vialEntity.setHarvestTime(tag.getLong("HarvestTime"));
                }
            }
        }
    }
}
