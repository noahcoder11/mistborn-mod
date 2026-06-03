package com.not_noah.mistborn_metal_arts.worldgen;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.registry.ModBlocks;
import com.not_noah.mistborn_metal_arts.registry.ModEntityTypes;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SmallStructureBuilder {
    private SmallStructureBuilder() {
    }

    public static void placeSteelMinistryOutpost(ServerLevel level, BlockPos origin) {
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState trim = Blocks.IRON_BLOCK.defaultBlockState();
        fill(level, origin.offset(-5, -1, -5), origin.offset(5, -1, 5), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        hollowBox(level, origin.offset(-5, 0, -5), origin.offset(5, 5, 5), wall);
        fill(level, origin.offset(-2, 1, -5), origin.offset(2, 3, -5), Blocks.AIR.defaultBlockState());
        fill(level, origin.offset(-1, 5, -1), origin.offset(1, 8, 1), trim);
        placeLoot(level, origin.offset(3, 0, 3), LootProfile.MINISTRY);
        spawn(level, ModEntityTypes.SEEKER.get(), origin.offset(-3, 0, 2), MetalbornRole.SEEKER);
        spawn(level, ModEntityTypes.SMOKER.get(), origin.offset(3, 0, -2), MetalbornRole.SMOKER);
        spawn(level, ModEntityTypes.LURCHER_GUARD.get(), origin.offset(0, 0, 0), MetalbornRole.LURCHER_GUARD);
    }

    public static void placeSkaaHideout(ServerLevel level, BlockPos origin) {
        hollowBox(level, origin.offset(-4, -3, -4), origin.offset(4, 1, 4), Blocks.OAK_PLANKS.defaultBlockState());
        fill(level, origin.offset(-3, -2, -3), origin.offset(3, 0, 3), Blocks.AIR.defaultBlockState());
        level.setBlock(origin.offset(0, 1, 0), Blocks.OAK_TRAPDOOR.defaultBlockState(), 3);
        placeLoot(level, origin.offset(2, -2, 2), LootProfile.HIDEOUT);
    }

    public static void placeCanalRuin(ServerLevel level, BlockPos origin) {
        BlockState stone = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        fill(level, origin.offset(-7, -1, -3), origin.offset(7, -1, 3), stone);
        for (int x = -7; x <= 7; x++) {
            level.setBlock(origin.offset(x, 0, -3), stone, 3);
            level.setBlock(origin.offset(x, 0, 3), stone, 3);
        }
        fill(level, origin.offset(-6, 0, -1), origin.offset(6, 0, 1), Blocks.WATER.defaultBlockState());
        placeLoot(level, origin.offset(6, 0, 3), LootProfile.CANAL);
    }

    public static void placeNobleKeep(ServerLevel level, BlockPos origin) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState roof = Blocks.DEEPSLATE_TILES.defaultBlockState();
        fill(level, origin.offset(-6, -1, -6), origin.offset(6, -1, 6), Blocks.SMOOTH_STONE.defaultBlockState());
        hollowBox(level, origin.offset(-6, 0, -6), origin.offset(6, 5, 6), wall);
        fill(level, origin.offset(-7, 6, -7), origin.offset(7, 6, 7), roof);
        fill(level, origin.offset(-2, 1, -6), origin.offset(2, 3, -6), Blocks.AIR.defaultBlockState());
        placeLoot(level, origin.offset(4, 0, 4), LootProfile.NOBLE);
        spawn(level, ModEntityTypes.COINSHOT_BANDIT.get(), origin.offset(-3, 0, 0), MetalbornRole.COINSHOT_BANDIT);
        spawn(level, ModEntityTypes.SOOTHER.get(), origin.offset(3, 0, 0), MetalbornRole.SOOTHER);
    }

    public static void placeAtiumCavern(ServerLevel level, BlockPos origin) {
        int radius = 7;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double d = Math.sqrt(x * x + z * z + y * y * 0.7D);
                    BlockPos pos = origin.offset(x, y, z);
                    if (d < radius - 1) {
                        level.setBlock(pos, Blocks.CAVE_AIR.defaultBlockState(), 3);
                    } else if (d < radius + 0.8D) {
                        level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 3);
                    }
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            BlockPos crystal = origin.offset(level.random.nextInt(9) - 4, level.random.nextInt(3) - 2, level.random.nextInt(9) - 4);
            level.setBlock(crystal, ModBlocks.ATIUM_CLUSTER.get().defaultBlockState(), 3);
        }
        placeLoot(level, origin.offset(4, -2, 4), LootProfile.ATIUM);
        spawn(level, ModEntityTypes.ATIUM_SEER.get(), origin.offset(0, -2, 0), MetalbornRole.ATIUM_SEER);
    }

    public static void placeKolossCamp(ServerLevel level, BlockPos origin) {
        fill(level, origin.offset(-6, -1, -6), origin.offset(6, -1, 6), Blocks.COARSE_DIRT.defaultBlockState());
        for (int dx : new int[]{-6, 6}) {
            for (int dz : new int[]{-6, 6}) {
                fill(level, origin.offset(dx, 0, dz), origin.offset(dx, 3, dz), Blocks.SPRUCE_FENCE.defaultBlockState());
            }
        }
        level.setBlock(origin, Blocks.CAMPFIRE.defaultBlockState(), 3);
        placeLoot(level, origin.offset(4, 0, 0), LootProfile.KOLOSS);
        spawn(level, ModEntityTypes.KOLOSS.get(), origin.offset(-3, 0, -2), MetalbornRole.KOLOSS);
        spawn(level, ModEntityTypes.KOLOSS.get(), origin.offset(3, 0, 2), MetalbornRole.KOLOSS);
    }

    public static void placeKandraDen(ServerLevel level, BlockPos origin) {
        hollowBox(level, origin.offset(-4, -2, -4), origin.offset(4, 2, 4), Blocks.CALCITE.defaultBlockState());
        fill(level, origin.offset(-3, -1, -3), origin.offset(3, 1, 3), Blocks.AIR.defaultBlockState());
        placeLoot(level, origin.offset(3, -1, 3), LootProfile.KANDRA);
        spawn(level, ModEntityTypes.KANDRA.get(), origin.offset(0, -1, 0), MetalbornRole.KANDRA);
    }

    private static void spawn(ServerLevel level, net.minecraft.world.entity.EntityType<MetalbornEnemy> type, BlockPos pos, MetalbornRole role) {
        MetalbornEnemy entity = type.create(level);
        if (entity == null) {
            return;
        }
        entity.moveTo(pos.getX() + 0.5D, pos.getY() + 0.02D, pos.getZ() + 0.5D, level.random.nextFloat() * 360F, 0F);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        entity.setCustomName(Component.literal(role.displayName()));
        level.addFreshEntity(entity);
    }

    private static void placeLoot(ServerLevel level, BlockPos pos, LootProfile profile) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 3);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            return;
        }
        switch (profile) {
            case MINISTRY -> {
                chest.setItem(0, new ItemStack(ModItems.METAL_VIALS.get(Metal.BRONZE).get(), 2));
                chest.setItem(1, new ItemStack(ModItems.METAL_VIALS.get(Metal.COPPER).get(), 2));
                chest.setItem(2, new ItemStack(ModItems.SPIKE_BLANKS.get(Metal.IRON).get(), 2));
            }
            case HIDEOUT -> {
                chest.setItem(0, new ItemStack(ModItems.METAL_VIALS.get(Metal.STEEL).get(), 1));
                chest.setItem(1, new ItemStack(ModItems.METAL_FLAKES.get(Metal.IRON).get(), 6));
                chest.setItem(2, new ItemStack(net.minecraft.world.item.Items.BREAD, 4));
            }
            case CANAL -> {
                chest.setItem(0, new ItemStack(ModItems.METAL_FLAKES.get(Metal.COPPER).get(), 6));
                chest.setItem(1, new ItemStack(ModItems.METAL_FLAKES.get(Metal.BRASS).get(), 3));
            }
            case NOBLE -> {
                chest.setItem(0, new ItemStack(ModItems.METAL_VIALS.get(Metal.GOLD).get(), 2));
                chest.setItem(1, new ItemStack(ModItems.METALMIND_RINGS.get(Metal.GOLD).get(), 1));
                chest.setItem(2, new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 5));
            }
            case ATIUM -> {
                chest.setItem(0, new ItemStack(ModItems.METAL_BEADS.get(Metal.ATIUM).get(), 2));
                chest.setItem(1, new ItemStack(ModItems.METAL_VIALS.get(Metal.ATIUM).get(), 1));
            }
            case KOLOSS -> {
                chest.setItem(0, new ItemStack(ModItems.CHARGED_SPIKES.get(Metal.PEWTER).get(), 1));
                chest.setItem(1, new ItemStack(ModItems.METAL_INGOTS.get(Metal.IRON).get(), 8));
            }
            case KANDRA -> {
                chest.setItem(0, new ItemStack(ModItems.METALMIND_RINGS.get(Metal.COPPER).get(), 1));
                if (ServerConfig.VALUES.lerasiumExists.get() && ServerConfig.VALUES.lerasiumLoot.get() && level.random.nextFloat() < ServerConfig.VALUES.lerasiumRarity.get() * 10.0F) {
                    chest.setItem(1, new ItemStack(ModItems.METAL_BEADS.get(Metal.LERASIUM).get(), 1));
                }
            }
        }
    }

    private static void hollowBox(ServerLevel level, BlockPos min, BlockPos max, BlockState state) {
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean shell = x == min.getX() || x == max.getX() || y == min.getY() || y == max.getY() || z == min.getZ() || z == max.getZ();
                    level.setBlock(new BlockPos(x, y, z), shell ? state : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void fill(ServerLevel level, BlockPos min, BlockPos max, BlockState state) {
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    level.setBlock(new BlockPos(x, y, z), state, 3);
                }
            }
        }
    }

    private enum LootProfile {
        MINISTRY,
        HIDEOUT,
        CANAL,
        NOBLE,
        ATIUM,
        KOLOSS,
        KANDRA
    }
}
