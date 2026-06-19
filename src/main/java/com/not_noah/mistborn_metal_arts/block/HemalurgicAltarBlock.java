package com.not_noah.mistborn_metal_arts.block;

import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class HemalurgicAltarBlock extends BedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D);

    public HemalurgicAltarBlock(Properties properties) {
        super(DyeColor.GRAY, properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT).setValue(BlockStateProperties.OCCUPIED, false));
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext entityContext) {
            net.minecraft.world.entity.Entity entity = entityContext.getEntity();
            if (entity instanceof LivingEntity living) {
                BlockPos footPos = getFootPos(state, pos);
                Direction facing = state.getValue(FACING);
                BlockPos headPos = footPos.relative(facing);

                // Safe check for sleeping/laying poses that are synchronized to the client
                boolean looksSleeping = living.isSleeping() || living.getPose() == net.minecraft.world.entity.Pose.SLEEPING;
                if (looksSleeping) {
                    var sleepingPosOpt = living.getSleepingPos();
                    if (sleepingPosOpt.isPresent() && sleepingPosOpt.get().equals(headPos)) {
                        return net.minecraft.world.phys.shapes.Shapes.empty();
                    }
                }

                // Fallback check on server-side using capability/NBT state
                if (!living.level().isClientSide) {
                    if (living instanceof Player player) {
                        var cap = player.getCapability(com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities.METAL_ARTS).orElse(null);
                        if (cap != null && cap.isRestrained() && footPos.equals(cap.getRestrainedAltarPos())) {
                            return net.minecraft.world.phys.shapes.Shapes.empty();
                        }
                    } else {
                        var nbt = living.getPersistentData();
                        if (nbt.getBoolean("RestrainedAltar") && footPos.equals(BlockPos.of(nbt.getLong("RestrainedAltarPos")))) {
                            return net.minecraft.world.phys.shapes.Shapes.empty();
                        }
                    }
                }
            }
        }
        return SHAPE;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos();
        BlockPos headPos = pos.relative(direction);
        Level level = context.getLevel();
        if (level.getBlockState(headPos).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(headPos)) {
            return this.defaultBlockState().setValue(FACING, direction).setValue(PART, BedPart.FOOT);
        } else {
            return null;
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockPos headPos = pos.relative(state.getValue(FACING));
            level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), 3);
            level.blockUpdated(pos, net.minecraft.world.level.block.Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            BedPart part = state.getValue(PART);
            if (part == BedPart.FOOT) {
                BlockPos headPos = pos.relative(state.getValue(FACING));
                BlockState headState = level.getBlockState(headPos);
                if (headState.is(this) && headState.getValue(PART) == BedPart.HEAD) {
                    level.setBlock(headPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
                    level.levelEvent(player, 2001, headPos, Block.getId(headState));
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
            if (!level.isClientSide) {
                Direction direction = state.getValue(FACING);
                BedPart part = state.getValue(PART);
                BlockPos otherPos = part == BedPart.FOOT ? pos.relative(direction) : pos.relative(direction.getOpposite());
                BlockState otherState = level.getBlockState(otherPos);
                if (otherState.is(this) && otherState.getValue(PART) != part) {
                    level.removeBlock(otherPos, false);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    public static BlockPos getFootPos(BlockState state, BlockPos pos) {
        return state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(state.getValue(FACING).getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);

            // Get the footprint of the altar
            BlockPos footPos = getFootPos(state, pos);
            Direction facing = state.getValue(FACING);

            // Find all living entities restrained at this altar position
            List<LivingEntity> restrainedEntities = level.getEntitiesOfClass(LivingEntity.class, new AABB(footPos).expandTowards(facing.getStepX(), 0.0D, facing.getStepZ()).inflate(0.5D));
            LivingEntity occupant = null;

            for (LivingEntity entity : restrainedEntities) {
                if (entity instanceof Player p) {
                    var cap = p.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                    if (cap != null && cap.isRestrained() && footPos.equals(cap.getRestrainedAltarPos())) {
                        occupant = p;
                        break;
                    }
                } else {
                    var nbt = entity.getPersistentData();
                    if (nbt.getBoolean("RestrainedAltar") && footPos.equals(BlockPos.of(nbt.getLong("RestrainedAltarPos")))) {
                        occupant = entity;
                        break;
                    }
                }
            }

            // If they are holding a Hemalurgic spike, perform the spiking ritual directly on the occupant!
            if (stack.getItem() instanceof com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem spike) {
                if (occupant != null) {
                    net.minecraft.world.phys.Vec3 hitPos = hit.getLocation().subtract(occupant.position());
                    com.not_noah.mistborn_metal_arts.capability.HemalurgyEvents.performSpikingRitual(serverPlayer, occupant, spike, stack, hand, footPos, hitPos);
                    return InteractionResult.CONSUME;
                }
                return InteractionResult.PASS;
            }

            if (occupant instanceof Player playerOccupant) {
                var occupantCap = playerOccupant.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                if (occupantCap != null && serverPlayer.isShiftKeyDown() && stack.isEmpty()) {
                    var spikes = occupantCap.installedSpikes();
                    if (!spikes.isEmpty()) {
                        int currentIndex = occupantCap.linchpinSpikeIndex();
                        int nextIndex = (currentIndex + 1) % spikes.size();
                        occupantCap.setLinchpinSpike(nextIndex);
                        
                        BlockPos headPos = footPos.relative(facing);
                        double px = headPos.getX() + 0.5D;
                        double py = headPos.getY() + 1.0D;
                        double pz = headPos.getZ() + 0.5D;
                        
                        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, px, py, pz, 15, 0.25D, 0.25D, 0.25D, 0.05D);
                        }
                        level.playSound(null, footPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.75F, 0.85F);
                        
                        serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.linchpin_promoted", spikes.get(nextIndex).powerMetal().displayName()), true);
                        if (playerOccupant != serverPlayer) {
                            ((ServerPlayer) playerOccupant).sendSystemMessage(Component.translatable("message.mistborn_metal_arts.linchpin_promoted_by_other", spikes.get(nextIndex).powerMetal().displayName(), serverPlayer.getDisplayName().getString()));
                        }
                        MetalArtsNetwork.sync((ServerPlayer) playerOccupant);
                        return InteractionResult.CONSUME;
                    } else {
                        serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.no_spikes_to_promote"), true);
                        return InteractionResult.CONSUME;
                    }
                }
            }

            // If clicker is already restrained here, do nothing
            var clickerCap = serverPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
            if (clickerCap != null && clickerCap.isRestrained() && footPos.equals(clickerCap.getRestrainedAltarPos())) {
                return InteractionResult.PASS;
            }

            // Restrain the player!
            if (clickerCap != null) {
                if (occupant != null) {
                    if (serverPlayer.isShiftKeyDown()) {
                        if (occupant instanceof Player occupantPlayer) {
                            var occupantCap = occupantPlayer.getCapability(MetalArtsCapabilities.METAL_ARTS).orElse(null);
                            if (occupantCap != null && occupantPlayer instanceof ServerPlayer serverOccupant) {
                                occupantCap.setRestrained(false, null, 0);
                                occupantPlayer.clearSleepingPos();
                                occupantPlayer.setPose(net.minecraft.world.entity.Pose.STANDING);
                                serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.released_mob"), true);
                                occupantPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.released_restraint"), true);
                                MetalArtsNetwork.sync(serverOccupant);
                            }
                        } else {
                            var nbt = occupant.getPersistentData();
                            nbt.putBoolean("RestrainedAltar", false);
                            occupant.clearSleepingPos();
                            occupant.setPose(net.minecraft.world.entity.Pose.STANDING);
                            serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.released_mob"), true);
                        }
                        level.playSound(null, footPos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.8F);
                        return InteractionResult.CONSUME;
                    } else {
                        serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.altar_full"), true);
                        return InteractionResult.CONSUME;
                    }
                }

                BlockPos headPos = footPos.relative(facing);
                double centerX = headPos.getX() + 0.5D;
                double centerZ = headPos.getZ() + 0.5D;

                clickerCap.setRestrained(true, footPos, 0);
                clickerCap.setRestrainedByOthers(false);
                serverPlayer.setPos(centerX, headPos.getY() + 0.5625D, centerZ);
                serverPlayer.setDeltaMovement(0, 0, 0);
                serverPlayer.hurtMarked = true;
                
                // Align yaw to altar direction
                float yaw = facing.toYRot();
                serverPlayer.setYRot(yaw);
                serverPlayer.setXRot(0.0F);
                serverPlayer.setYBodyRot(yaw);
                serverPlayer.setYHeadRot(yaw);

                level.playSound(null, footPos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
                serverPlayer.displayClientMessage(Component.translatable("message.mistborn_metal_arts.restrained_altar"), true);
                MetalArtsNetwork.sync(serverPlayer);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        Direction facing = state.getValue(FACING);
        BedPart part = state.getValue(PART);

        // Candle Y offset is 21 pixels / 16 = 1.3125
        double ly = 1.3125D;

        if (part == BedPart.FOOT) {
            spawnCandleFlame(level, pos, facing, -0.09375D, ly, 0.09375D, random);
            spawnCandleFlame(level, pos, facing, 1.09375D, ly, 0.09375D, random);
        } else {
            spawnCandleFlame(level, pos, facing, -0.09375D, ly, 0.90625D, random);
            spawnCandleFlame(level, pos, facing, 1.09375D, ly, 0.90625D, random);
        }
    }

    private void spawnCandleFlame(Level level, BlockPos pos, Direction facing, double lx, double ly, double lz, net.minecraft.util.RandomSource random) {
        double rx, rz;
        switch (facing) {
            case NORTH:
                rx = 1.0D - lx;
                rz = 1.0D - lz;
                break;
            case EAST:
                rx = 1.0D - lz;
                rz = lx;
                break;
            case WEST:
                rx = lz;
                rz = 1.0D - lx;
                break;
            case SOUTH:
            default:
                rx = lx;
                rz = lz;
                break;
        }

        double px = pos.getX() + rx;
        double py = pos.getY() + ly;
        double pz = pos.getZ() + rz;

        level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, px, py, pz, 0.0D, 0.0D, 0.0D);
        if (random.nextFloat() < 0.15F) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, px, py, pz, 0.0D, 0.0D, 0.0D);
        }
    }
}

