package com.not_noah.mistborn_metal_arts.block;

import com.not_noah.mistborn_metal_arts.block.entity.AluminumCasingBlockEntity;
import com.not_noah.mistborn_metal_arts.hemalurgy.SpikeDecayManager;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
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

public class AluminumCasingBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 14.0D, 12.0D);

    public AluminumCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AluminumCasingBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AluminumCasingBlockEntity casingEntity)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);

        if (casingEntity.hasSpike()) {
            // Check if player has shears to extract
            boolean hasShears = held.getItem() instanceof ShearsItem || otherStack.getItem() instanceof ShearsItem;
            InteractionHand shearsHand = held.getItem() instanceof ShearsItem ? hand : (otherStack.getItem() instanceof ShearsItem ? otherHand : null);

            if (hasShears && shearsHand != null) {
                ItemStack shears = player.getItemInHand(shearsHand);
                ItemStack spike = casingEntity.getStoredSpike().copy();
                
                if (!spike.isEmpty()) {
                    spike = SpikeDecayManager.setStoredState(spike, "normal", level);
                    
                    if (!player.getInventory().add(spike)) {
                        player.drop(spike, false);
                    }
                    
                    casingEntity.setStoredSpike(ItemStack.EMPTY);
                    
                    if (!player.getAbilities().instabuild) {
                        shears.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(shearsHand));
                        if (shears.isEmpty()) {
                            player.setItemInHand(shearsHand, ItemStack.EMPTY);
                        } else {
                            player.setItemInHand(shearsHand, shears);
                        }
                    }

                    level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.1F);
                    level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.4F, 1.6F);
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_retrieved_aluminum"), true);
                    player.containerMenu.broadcastChanges();
                    return InteractionResult.CONSUME;
                }
            } else {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.shears_required_extract"), true);
            }
        } else {
            // Insert spike
            if (held.getItem() instanceof HemalurgicSpikeItem) {
                ItemStack inserted = held.copy();
                inserted.setCount(1);
                
                inserted = SpikeDecayManager.setStoredState(inserted, "aluminum", level);
                casingEntity.setStoredSpike(inserted);

                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                    if (held.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                    } else {
                        player.setItemInHand(hand, held);
                    }
                }

                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.4F);
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.spike_inserted_aluminum"), true);
                player.containerMenu.broadcastChanges();
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.aluminum_casing_empty_hint"), true);
            }
        }

        return InteractionResult.PASS;
    }
}
