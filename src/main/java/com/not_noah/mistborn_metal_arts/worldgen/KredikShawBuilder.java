package com.not_noah.mistborn_metal_arts.worldgen;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.registry.ModBlocks;
import com.not_noah.mistborn_metal_arts.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Procedural/template-style builder for the new Kredik Shaw: Hill of a Thousand Spires.
 *
 * <p>The structure keeps the public datapack id {@code mistborn_metal_arts:kredik_shaw}, and now builds as
 * seven circular terraces.  Each module below acts like a small build
 * template: rings, radial roads, gatehouses, halls, courtyards, ramps, spires, the central citadel, and the
 * Well chamber.  The builder is deliberately chunk-aware.  Forge calls {@link #placeAt} once per structure
 * chunk, so every loop clips to the supplied chunk bounding box before doing expensive placement.</p>
 */
public final class KredikShawBuilder {
    public static final int OUTER_RADIUS = 256;
    public static final int APPROACH_RADIUS = 286;
    public static final int NOMINAL_MAX_HEIGHT = 510;
    public static final int NOMINAL_CITADEL_TOP = 455;
    public static final int NOMINAL_WELL_DEPTH = 42;
    public static final int PIECE_HORIZONTAL_RADIUS = APPROACH_RADIUS + 32;
    public static final int PIECE_BELOW = 96;
    public static final int PIECE_ABOVE = NOMINAL_MAX_HEIGHT + 16;

    private static final ResourceLocation KREDIK_SHAW_LOOT = new ResourceLocation(MistbornMetalArts.MOD_ID, "chests/kredik_shaw");
    private static final Direction[] CARDINALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static final LayerSpec[] LAYERS = {
            new LayerSpec(1, 256, 0, 50, 10, 16, 10),
            new LayerSpec(2, 210, 50, 115, 12, 18, 12),
            new LayerSpec(3, 170, 115, 180, 12, 20, 14),
            new LayerSpec(4, 130, 180, 245, 14, 22, 16),
            new LayerSpec(5, 95, 245, 310, 13, 24, 18),
            new LayerSpec(6, 65, 310, 375, 12, 28, 22),
            new LayerSpec(7, 35, 375, 455, 10, 34, 28)
    };

    private KredikShawBuilder() {
    }

    public static void place(ServerPlayer player) {
        if (!ServerConfig.VALUES.kredikShawEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.kredik_disabled"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        BoundingBox bounds = fullBounds(origin);
        preloadDebugFootprint(level, bounds);
        placeAt(level, origin, ServerConfig.VALUES.kredikShawHasWell.get(), true, bounds, level.getRandom());
        player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.kredik_placed", origin.getX(), origin.getY(), origin.getZ()), false);
    }

    private static void preloadDebugFootprint(ServerLevel level, BoundingBox bounds) {
        int minChunkX = bounds.minX() >> 4;
        int maxChunkX = bounds.maxX() >> 4;
        int minChunkZ = bounds.minZ() >> 4;
        int maxChunkZ = bounds.maxZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
    }

    public static void placeAt(ServerLevelAccessor level, BlockPos origin, boolean withWell, boolean spawnBoss, BoundingBox chunkBounds, RandomSource random) {
        BuildContext ctx = BuildContext.create(level, origin, chunkBounds, 0L);

        buildFoundationAndApproaches(ctx);
        for (LayerSpec layer : LAYERS) {
            buildLayerTerrace(ctx, layer);
            buildLayerRoads(ctx, layer);
            buildLayerModules(ctx, layer);
            buildLayerCourtyards(ctx, layer);
            buildLayerSupports(ctx, layer);
        }

        buildGrandRamps(ctx);
        buildUpperConcourseConnections(ctx);
        buildSecondaryVerticalRoutes(ctx);
        buildSpireField(ctx);
        buildCitadel(ctx);

        if (withWell) {
            buildWellChamber(ctx);
        }
        if (spawnBoss) {
            spawnPalaceGuards(ctx);
        }
    }

    public static BoundingBox fullBounds(BlockPos origin) {
        return new BoundingBox(
                origin.getX() - PIECE_HORIZONTAL_RADIUS,
                origin.getY() - PIECE_BELOW,
                origin.getZ() - PIECE_HORIZONTAL_RADIUS,
                origin.getX() + PIECE_HORIZONTAL_RADIUS,
                origin.getY() + PIECE_ABOVE,
                origin.getZ() + PIECE_HORIZONTAL_RADIUS
        );
    }

    public static BoundingBox chunkBounds(ServerLevel level, net.minecraft.world.level.ChunkPos chunkPos) {
        return new BoundingBox(
                chunkPos.getMinBlockX(),
                level.getMinBuildHeight(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX(),
                level.getMaxBuildHeight() - 1,
                chunkPos.getMaxBlockZ()
        );
    }

    public static boolean chunkIntersectsFootprint(BlockPos origin, net.minecraft.world.level.ChunkPos chunkPos) {
        BoundingBox bounds = fullBounds(origin);
        return chunkPos.getMaxBlockX() >= bounds.minX()
                && chunkPos.getMinBlockX() <= bounds.maxX()
                && chunkPos.getMaxBlockZ() >= bounds.minZ()
                && chunkPos.getMinBlockZ() <= bounds.maxZ();
    }

    private static void buildFoundationAndApproaches(BuildContext ctx) {
        int baseY = ctx.y(0);
        fillCircle(ctx, ctx.origin.offset(0, baseY - 2, 0), OUTER_RADIUS + 3, Blocks.DEEPSLATE.defaultBlockState());
        ring(ctx, ctx.origin.offset(0, baseY - 1, 0), OUTER_RADIUS + 3, 8, Blocks.POLISHED_BLACKSTONE.defaultBlockState());

        for (Direction dir : CARDINALS) {
            for (int dist = OUTER_RADIUS - 5; dist <= APPROACH_RADIUS; dist++) {
                for (int w = -8; w <= 8; w++) {
                    BlockPos p = ctx.origin
                            .relative(dir, dist)
                            .relative(dir.getClockWise(), w)
                            .offset(0, baseY, 0);
                    setBlock(ctx, p, roadState(ctx, p));
                    if (Math.abs(w) == 8 && dist % 5 == 0) {
                        setBlock(ctx, p.above(), Blocks.CHAIN.defaultBlockState());
                        setBlock(ctx, p.above(2), Blocks.LANTERN.defaultBlockState());
                    }
                }
            }
        }

        buildOuterWallAndGates(ctx);
    }

    private static void buildOuterWallAndGates(BuildContext ctx) {
        int baseY = ctx.y(0);
        int wallTop = ctx.y(32);
        for (int y = baseY; y <= wallTop; y++) {
            int wallY = y;
            ring(ctx, ctx.origin.offset(0, y, 0), OUTER_RADIUS, 6, (x, z, pos) -> {
                if (isCardinalGateGap(x, z, OUTER_RADIUS, 18) && wallY < baseY + 18) {
                    return Blocks.AIR.defaultBlockState();
                }
                return fortressStone(ctx, pos, 0);
            });
        }
        for (Direction dir : CARDINALS) {
            BlockPos gateCenter = ctx.origin.relative(dir, OUTER_RADIUS).offset(0, baseY + 1, 0);
            buildGatehouse(ctx, gateCenter, dir);
        }
    }

    private static void buildGatehouse(BuildContext ctx, BlockPos center, Direction facing) {
        Direction side = facing.getClockWise();
        BlockPos a = center.relative(side, -13).relative(facing, -5);
        BlockPos b = center.relative(side, 13).relative(facing, 6).above(20);
        if (!intersectsBox(ctx, a, b)) {
            return;
        }
        boxShell(ctx, a, b, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.AIR.defaultBlockState());
        for (int w = -5; w <= 5; w++) {
            for (int h = 0; h <= 9; h++) {
                setBlock(ctx, center.relative(side, w).above(h), Blocks.AIR.defaultBlockState());
            }
        }
        for (int w = -4; w <= 4; w += 2) {
            setBlock(ctx, center.relative(side, w).above(10), Blocks.IRON_BARS.defaultBlockState());
        }
        buildSpire(ctx, center.relative(side, -11).above(21), scaled(ctx, 42), 0, 17);
        buildSpire(ctx, center.relative(side, 11).above(21), scaled(ctx, 45), 0, 18);
    }

    private static void buildLayerTerrace(BuildContext ctx, LayerSpec layer) {
        int floorY = ctx.y(layer.floor());
        int topY = ctx.y(layer.top());
        int radius = layer.radius();
        int wallThickness = layer.index() <= 2 ? 5 : layer.index() <= 5 ? 4 : 3;

        fillCircle(ctx, ctx.origin.offset(0, floorY, 0), radius, (x, z, pos) -> terraceFloor(ctx, pos, layer.index(), x, z));

        for (int y = floorY + 1; y <= topY; y++) {
            int localY = Math.max(0, y - floorY);
            boolean heavyBand = localY < 9 || localY % 13 == 0 || y > topY - 5;
            int thickness = heavyBand ? wallThickness : Math.max(2, wallThickness - 2);
            ring(ctx, ctx.origin.offset(0, y, 0), radius, thickness, (x, z, pos) -> {
                if (isAxisCorridor(x, z, layer.rampGapWidth(), radius) && localY < 14) {
                    return Blocks.AIR.defaultBlockState();
                }
                if (localY % 7 == 3 && Math.floorMod(x + z, 11) == 0) {
                    return Blocks.IRON_BARS.defaultBlockState();
                }
                return fortressStone(ctx, pos, layer.index());
            });
        }

        int parapetY = topY + 1;
        ring(ctx, ctx.origin.offset(0, parapetY, 0), radius, 2, (x, z, pos) -> {
            if (Math.floorMod(Math.abs(x) + Math.abs(z), 5) == 0) {
                return Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
            }
            return Blocks.AIR.defaultBlockState();
        });
    }

    private static void buildLayerRoads(BuildContext ctx, LayerSpec layer) {
        int y = ctx.y(layer.floor());
        int radius = layer.radius();
        int inner = layer.index() == 7 ? 0 : Math.max(18, LAYERS[layer.index()].radius() + 5);

        for (Direction dir : CARDINALS) {
            for (int dist = inner; dist <= radius - 6; dist++) {
                for (int w = -layer.rampGapWidth() / 2; w <= layer.rampGapWidth() / 2; w++) {
                    BlockPos p = ctx.origin
                            .relative(dir, dist)
                            .relative(dir.getClockWise(), w)
                            .offset(0, y, 0);
                    setBlock(ctx, p, roadState(ctx, p));
                }
            }
        }

        for (int ringRadius : circularRoads(layer)) {
            ring(ctx, ctx.origin.offset(0, y, 0), ringRadius, 3, (x, z, pos) -> roadState(ctx, pos));
        }
    }

    private static int[] circularRoads(LayerSpec layer) {
        int radius = layer.radius();
        return switch (layer.index()) {
            case 1 -> new int[]{70, 132, 198, 238};
            case 2 -> new int[]{62, 118, 172, 198};
            case 3 -> new int[]{55, 102, 145, 160};
            case 4 -> new int[]{42, 78, 112};
            case 5 -> new int[]{36, 62, 84};
            case 6 -> new int[]{28, 48, 59};
            default -> new int[]{16, 27};
        };
    }

    private static void buildLayerModules(BuildContext ctx, LayerSpec layer) {
        int floorY = ctx.y(layer.floor());
        if (layer.index() == 1) {
            buildFirstFloorBlueprint(ctx, layer);
            return;
        }
        int minR = switch (layer.index()) {
            case 1 -> 28;
            case 7 -> 8;
            default -> Math.max(18, LAYERS[layer.index()].radius() + 7);
        };
        int maxR = layer.radius() - 18;
        if (maxR <= minR) {
            return;
        }
        int radialStep = switch (layer.index()) {
            case 1, 2 -> 34;
            case 3, 4 -> 28;
            case 5 -> 23;
            default -> 18;
        };
        int angularStep = switch (layer.index()) {
            case 1 -> 16;
            case 2 -> 15;
            case 3 -> 14;
            case 4 -> 13;
            case 5 -> 12;
            case 6 -> 10;
            default -> 9;
        };

        for (int r = minR; r <= maxR; r += radialStep) {
            int offset = Math.floorMod(layer.index() * 11 + r / 3, angularStep);
            for (int angle = offset; angle < 360; angle += angularStep) {
                if (isCardinalAngle(angle, layer.rampGapWidth() + 3)) {
                    continue;
                }
                long seed = hash(ctx.seed, layer.index(), r, angle);
                RandomSource rand = RandomSource.create(seed);
                int jitterR = rand.nextInt(9) - 4;
                double radians = Math.toRadians(angle + rand.nextInt(7) - 3);
                int x = (int) Math.round(Math.cos(radians) * (r + jitterR));
                int z = (int) Math.round(Math.sin(radians) * (r + jitterR));
                if (isAxisCorridor(x, z, layer.rampGapWidth() + 6, layer.radius())) {
                    continue;
                }
                int width = layer.moduleBase() + rand.nextInt(8);
                int depth = layer.moduleBase() + rand.nextInt(10);
                int height = scaled(ctx, layer.moduleHeight() + rand.nextInt(14));
                ModuleKind kind = chooseModule(layer.index(), rand);
                BlockPos base = ctx.origin.offset(x, floorY + 1, z);
                buildModule(ctx, base, width, depth, Math.max(7, height), kind, layer.index(), rand);
            }
        }
    }

    private static ModuleKind chooseModule(int layer, RandomSource rand) {
        return switch (layer) {
            case 1 -> rand.nextBoolean() ? ModuleKind.WAREHOUSE : ModuleKind.MARKET;
            case 2 -> rand.nextInt(3) == 0 ? ModuleKind.PUBLIC_HALL : ModuleKind.WAREHOUSE;
            case 3 -> rand.nextBoolean() ? ModuleKind.FORGE : ModuleKind.BARRACKS;
            case 4 -> rand.nextInt(3) == 0 ? ModuleKind.RECORD_HALL : ModuleKind.ADMIN;
            case 5 -> rand.nextBoolean() ? ModuleKind.RESIDENCE : ModuleKind.TEMPLE;
            case 6 -> rand.nextBoolean() ? ModuleKind.ARCHIVE : ModuleKind.NOBLE_HALL;
            default -> ModuleKind.CITADEL_ANNEX;
        };
    }

    private static void buildFirstFloorBlueprint(BuildContext ctx, LayerSpec layer) {
        int y = ctx.y(layer.floor());
        buildFirstFloorDistricts(ctx, y);
        buildFirstFloorCentralBlock(ctx, y);
        buildFirstFloorCourtyards(ctx, y);
        buildFirstFloorCircularStreets(ctx, y);
        buildFirstFloorMainAvenues(ctx, y);
        buildFirstFloorSecondarySpokes(ctx, y);
        buildFirstFloorSpireBases(ctx, y);
    }

    private static void buildFirstFloorDistricts(BuildContext ctx, int y) {
        int[] centers = {-176, -132, -88, -44, 44, 88, 132, 176};
        for (int cx : centers) {
            for (int cz : centers) {
                if (cx * cx + cz * cz > 222 * 222 || Math.abs(cx) < 26 || Math.abs(cz) < 26) {
                    continue;
                }
                if (Math.abs(cx) < 58 && Math.abs(cz) < 58 || isFirstFloorCourtyardCell(cx, cz)) {
                    continue;
                }
                RandomSource rand = RandomSource.create(hash(ctx.seed, cx, cz, 0xF100));
                ModuleKind kind = Math.abs(cx) > 142 || Math.abs(cz) > 142
                        ? ModuleKind.WAREHOUSE
                        : rand.nextBoolean() ? ModuleKind.MARKET : ModuleKind.PUBLIC_HALL;
                int width = 22 + rand.nextInt(10);
                int depth = 22 + rand.nextInt(12);
                int height = Math.max(7, scaled(ctx, 8 + rand.nextInt(8)));
                buildModule(ctx, ctx.origin.offset(cx, y + 1, cz), width, depth, height, kind, 1, rand);
            }
        }
    }

    private static boolean isFirstFloorCourtyardCell(int x, int z) {
        return Math.abs(Math.abs(x) - 74) <= 38 && Math.abs(Math.abs(z) - 74) <= 38;
    }

    private static void buildFirstFloorCentralBlock(BuildContext ctx, int y) {
        fillRectInCircle(ctx, y, -54, 54, -54, 54, 78, Blocks.POLISHED_ANDESITE.defaultBlockState());
        rectFrame(ctx, y + 1, -56, 56, -56, 56, Blocks.STONE_BRICK_WALL.defaultBlockState());

        int[][] halls = {{-22, -22}, {-22, 22}, {22, -22}, {22, 22}};
        for (int[] hall : halls) {
            RandomSource rand = RandomSource.create(hash(ctx.seed, hall[0], hall[1], 0xC17ADE));
            buildModule(ctx, ctx.origin.offset(hall[0], y + 1, hall[1]), 22, 22, Math.max(8, scaled(ctx, 13)), ModuleKind.CITADEL_ANNEX, 1, rand);
        }

        fillCircle(ctx, ctx.origin.offset(0, y + 1, 0), 9, Blocks.CUT_COPPER.defaultBlockState());
        ring(ctx, ctx.origin.offset(0, y + 2, 0), 10, 1, Blocks.IRON_BARS.defaultBlockState());
        placeLootChest(ctx, ctx.origin.offset(0, y + 2, 0));
    }

    private static void buildFirstFloorCourtyards(BuildContext ctx, int y) {
        buildFirstFloorCourtyard(ctx, y, -74, -74, 34, 30);
        buildFirstFloorCourtyard(ctx, y, -74, 74, 34, 30);
        buildFirstFloorCourtyard(ctx, y, 74, -74, 34, 30);
        buildFirstFloorCourtyard(ctx, y, 74, 74, 34, 30);
    }

    private static void buildFirstFloorCourtyard(BuildContext ctx, int y, int cx, int cz, int halfX, int halfZ) {
        clearRectInCircle(ctx, y + 1, y + scaled(ctx, 14), cx - halfX, cx + halfX, cz - halfZ, cz + halfZ, OUTER_RADIUS);
        fillRectInCircle(ctx, y, cx - halfX, cx + halfX, cz - halfZ, cz + halfZ, OUTER_RADIUS, Blocks.MOSS_BLOCK.defaultBlockState());
        rectFrame(ctx, y + 1, cx - halfX, cx + halfX, cz - halfZ, cz + halfZ, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
        for (int px = cx - halfX + 10; px <= cx + halfX - 10; px += 18) {
            for (int pz = cz - halfZ + 8; pz <= cz + halfZ - 8; pz += 16) {
                BlockPos p = ctx.origin.offset(px, y + 1, pz);
                drawSmallColumn(ctx, p, 1, Blocks.STONE_BRICKS.defaultBlockState());
                setBlock(ctx, p.above(), Blocks.LANTERN.defaultBlockState());
            }
        }
    }

    private static void buildFirstFloorCircularStreets(BuildContext ctx, int y) {
        pavedRing(ctx, y, 236, 7);
        pavedRing(ctx, y, 198, 5);
        pavedRing(ctx, y, 132, 5);
        pavedRing(ctx, y, 70, 5);
    }

    private static void pavedRing(BuildContext ctx, int y, int radius, int thickness) {
        for (int h = 1; h <= scaled(ctx, 6); h++) {
            ring(ctx, ctx.origin.offset(0, y + h, 0), radius, thickness + 2, Blocks.AIR.defaultBlockState());
        }
        ring(ctx, ctx.origin.offset(0, y, 0), radius, thickness, (x, z, pos) -> roadState(ctx, pos));
    }

    private static void buildFirstFloorMainAvenues(BuildContext ctx, int y) {
        fillRectInCircle(ctx, y, -10, 10, -OUTER_RADIUS, OUTER_RADIUS, OUTER_RADIUS, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        fillRectInCircle(ctx, y, -OUTER_RADIUS, OUTER_RADIUS, -10, 10, OUTER_RADIUS, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        clearRectInCircle(ctx, y + 1, y + scaled(ctx, 14), -12, 12, -OUTER_RADIUS, OUTER_RADIUS, OUTER_RADIUS);
        clearRectInCircle(ctx, y + 1, y + scaled(ctx, 14), -OUTER_RADIUS, OUTER_RADIUS, -12, 12, OUTER_RADIUS);

        for (Direction dir : CARDINALS) {
            Direction side = dir.getClockWise();
            for (int dist = 18; dist <= OUTER_RADIUS + 6; dist += 16) {
                for (int sign : new int[]{-1, 1}) {
                    BlockPos p = ctx.origin.relative(dir, dist).relative(side, sign * 12).offset(0, y + 1, 0);
                    setBlock(ctx, p, Blocks.ANDESITE_WALL.defaultBlockState());
                    setBlock(ctx, p.above(), Blocks.LANTERN.defaultBlockState());
                }
            }
        }
    }

    private static void buildFirstFloorSecondarySpokes(BuildContext ctx, int y) {
        for (int angle = 30; angle < 360; angle += 30) {
            if (angle % 90 != 0) {
                drawAngledAvenue(ctx, y, angle, 66, angle % 45 == 0 ? OUTER_RADIUS + 4 : 232, angle % 45 == 0 ? 5 : 3);
            }
        }
    }

    private static void drawAngledAvenue(BuildContext ctx, int y, int angle, int startRadius, int endRadius, int halfWidth) {
        double radians = Math.toRadians(angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (int dist = startRadius; dist <= endRadius; dist++) {
            for (int w = -halfWidth; w <= halfWidth; w++) {
                int x = (int) Math.round(cos * dist - sin * w);
                int z = (int) Math.round(sin * dist + cos * w);
                if (x * x + z * z > (OUTER_RADIUS + 4) * (OUTER_RADIUS + 4)) {
                    continue;
                }
                BlockPos p = ctx.origin.offset(x, y, z);
                setBlock(ctx, p, roadState(ctx, p));
                for (int h = 1; h <= scaled(ctx, 6); h++) {
                    setBlock(ctx, p.above(h), Blocks.AIR.defaultBlockState());
                }
                if (dist % 28 == 0 && Math.abs(w) == halfWidth) {
                    setBlock(ctx, p.above(), Blocks.STONE_BRICK_WALL.defaultBlockState());
                    setBlock(ctx, p.above(2), Blocks.SOUL_LANTERN.defaultBlockState());
                }
            }
        }
    }

    private static void buildFirstFloorSpireBases(BuildContext ctx, int y) {
        for (int angle = 0; angle < 360; angle += 15) {
            double radians = Math.toRadians(angle);
            int radius = angle % 45 == 0 ? 214 : 238;
            int x = (int) Math.round(Math.cos(radians) * radius);
            int z = (int) Math.round(Math.sin(radians) * radius);
            BlockPos base = ctx.origin.offset(x, y + 1, z);
            fillCircle(ctx, base, 5, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            ring(ctx, base.above(), 5, 1, Blocks.CUT_COPPER.defaultBlockState());
            buildSpire(ctx, base.above(2), scaled(ctx, 18 + Math.floorMod(angle, 5) * 5), angle % 5, angle + 0xB45E);
        }

        for (int radius : new int[]{132, 198}) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                BlockPos p = ctx.origin.offset((int) Math.round(Math.cos(radians) * radius), y + 1, (int) Math.round(Math.sin(radians) * radius));
                for (int h = 0; h <= scaled(ctx, 10); h++) {
                    drawSmallColumn(ctx, p.above(h), h % 5 == 0 ? 2 : 1, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                }
                setBlock(ctx, p.above(scaled(ctx, 11)), Blocks.LANTERN.defaultBlockState());
            }
        }
    }

    private static void buildModule(BuildContext ctx, BlockPos base, int width, int depth, int height, ModuleKind kind, int layer, RandomSource rand) {
        BlockPos min = base.offset(-width / 2, 0, -depth / 2);
        BlockPos max = base.offset(width / 2, height, depth / 2);
        if (!intersectsBox(ctx, min, max)) {
            return;
        }

        BlockState wall = switch (kind) {
            case FORGE -> Blocks.BLACKSTONE.defaultBlockState();
            case TEMPLE, NOBLE_HALL -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case RECORD_HALL, ARCHIVE -> Blocks.STONE_BRICKS.defaultBlockState();
            default -> fortressStone(ctx, base, layer);
        };
        boxShell(ctx, min, max, wall, Blocks.AIR.defaultBlockState());

        for (int y = 4; y < height - 2; y += 6) {
            for (int x = -width / 2 + 2; x <= width / 2 - 2; x += 5) {
                setBlock(ctx, base.offset(x, y, -depth / 2), Blocks.IRON_BARS.defaultBlockState());
                setBlock(ctx, base.offset(x, y, depth / 2), Blocks.IRON_BARS.defaultBlockState());
            }
            for (int z = -depth / 2 + 2; z <= depth / 2 - 2; z += 5) {
                setBlock(ctx, base.offset(-width / 2, y, z), Blocks.IRON_BARS.defaultBlockState());
                setBlock(ctx, base.offset(width / 2, y, z), Blocks.IRON_BARS.defaultBlockState());
            }
        }

        for (int floor = 0; floor <= height; floor += 8) {
            fillRect(ctx, base.offset(-width / 2 + 1, floor, -depth / 2 + 1), base.offset(width / 2 - 1, floor, depth / 2 - 1), floorState(ctx, base, layer));
        }

        buildModuleRoof(ctx, base.above(height + 1), width, depth, kind, rand);
        decorateModule(ctx, base, width, depth, height, kind, rand);
    }

    private static void buildModuleRoof(BuildContext ctx, BlockPos center, int width, int depth, ModuleKind kind, RandomSource rand) {
        BlockState roof = switch (kind) {
            case TEMPLE, NOBLE_HALL, CITADEL_ANNEX -> Blocks.WEATHERED_CUT_COPPER.defaultBlockState();
            case FORGE -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
        };
        for (int y = 0; y < 4; y++) {
            int shrink = y * 2;
            if (width - shrink <= 2 || depth - shrink <= 2) {
                break;
            }
            fillRect(ctx, center.offset(-width / 2 + shrink, y, -depth / 2 + shrink), center.offset(width / 2 - shrink, y, depth / 2 - shrink), roof);
        }
        if (rand.nextFloat() < 0.38F) {
            buildSpire(ctx, center.above(4), scaled(ctx, 18 + rand.nextInt(34)), rand.nextInt(4), rand.nextInt(5000));
        }
    }

    private static void decorateModule(BuildContext ctx, BlockPos base, int width, int depth, int height, ModuleKind kind, RandomSource rand) {
        for (int i = 0; i < 3; i++) {
            int x = rand.nextInt(Math.max(2, width - 4)) - width / 2 + 2;
            int z = rand.nextInt(Math.max(2, depth - 4)) - depth / 2 + 2;
            BlockPos p = base.offset(x, 1, z);
            BlockState detail = switch (kind) {
                case FORGE -> Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
                case WAREHOUSE, MARKET -> Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
                case RECORD_HALL, ARCHIVE -> Blocks.LECTERN.defaultBlockState();
                default -> Blocks.LANTERN.defaultBlockState();
            };
            setBlock(ctx, p, detail);
        }
        if (kind == ModuleKind.WAREHOUSE || kind == ModuleKind.RECORD_HALL || kind == ModuleKind.ARCHIVE) {
            placeLootChest(ctx, base.offset(0, 1, 0));
        }
        for (int y = 5; y < height; y += 9) {
            setBlock(ctx, base.offset(-width / 2, y, 0), Blocks.LANTERN.defaultBlockState());
            setBlock(ctx, base.offset(width / 2, y, 0), Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void buildLayerCourtyards(BuildContext ctx, LayerSpec layer) {
        if (layer.index() == 1) {
            return;
        }
        int floorY = ctx.y(layer.floor());
        int radius = layer.radius();
        int courtRadius = switch (layer.index()) {
            case 1 -> 20;
            case 2, 3 -> 17;
            case 4 -> 18;
            case 5 -> 13;
            case 6 -> 10;
            default -> 8;
        };

        for (int angle = 45; angle < 360; angle += 90) {
            int r = Math.max(18, radius - 42);
            double radians = Math.toRadians(angle + layer.index() * 3);
            int x = (int) Math.round(Math.cos(radians) * r);
            int z = (int) Math.round(Math.sin(radians) * r);
            BlockPos center = ctx.origin.offset(x, floorY + 1, z);
            clearCylinder(ctx, center, courtRadius, scaled(ctx, 16));
            fillCircle(ctx, center.below(), courtRadius, Blocks.POLISHED_ANDESITE.defaultBlockState());
            ring(ctx, center.below(), courtRadius, 2, Blocks.STONE_BRICK_WALL.defaultBlockState());
            for (Direction dir : CARDINALS) {
                setBlock(ctx, center.relative(dir, courtRadius - 3), Blocks.LANTERN.defaultBlockState());
            }
        }

        if (layer.index() == 4) {
            BlockPos civic = ctx.origin.offset(0, floorY + 1, 0);
            clearCylinder(ctx, civic, 24, scaled(ctx, 20));
            fillCircle(ctx, civic.below(), 24, Blocks.POLISHED_ANDESITE.defaultBlockState());
            ring(ctx, civic.below(), 24, 2, Blocks.CUT_COPPER.defaultBlockState());
        }
    }

    private static void buildLayerSupports(BuildContext ctx, LayerSpec layer) {
        int floorY = ctx.y(layer.floor());
        if (floorY <= 0) {
            return;
        }
        int supportBottom = ctx.y(LAYERS[Math.max(0, layer.index() - 2)].floor());
        for (int angle = 0; angle < 360; angle += 30) {
            double radians = Math.toRadians(angle + layer.index() * 7);
            int r = layer.radius() - 10;
            int x = (int) Math.round(Math.cos(radians) * r);
            int z = (int) Math.round(Math.sin(radians) * r);
            BlockPos column = ctx.origin.offset(x, supportBottom, z);
            int h = Math.max(1, floorY - supportBottom);
            for (int y = 0; y < h; y++) {
                drawSmallColumn(ctx, column.above(y), y % 9 == 0 ? 2 : 1, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
    }

    private static void buildGrandRamps(BuildContext ctx) {
        for (Direction dir : CARDINALS) {
            Direction side = dir.getClockWise();
            for (int i = 0; i < LAYERS.length - 1; i++) {
                LayerSpec lower = LAYERS[i];
                LayerSpec upper = LAYERS[i + 1];
                int startR = lower.radius() - 20;
                int endR = upper.radius() + 12;
                int startY = ctx.y(lower.floor());
                int endY = ctx.y(upper.floor());
                int steps = Math.max(1, startR - endR);
                for (int step = 0; step <= steps; step++) {
                    int dist = startR - step;
                    int y = startY + (int) Math.round((endY - startY) * (step / (double) steps));
                    BlockPos center = ctx.origin.relative(dir, dist).offset(0, y, 0);
                    for (int w = -6; w <= 6; w++) {
                        BlockPos p = center.relative(side, w);
                        BlockState ramp = Math.floorMod(step, 3) == 0
                                ? Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dir.getOpposite())
                                : roadState(ctx, p);
                        setBlock(ctx, p, ramp);
                        if (Math.abs(w) == 6) {
                            setBlock(ctx, p.above(), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                        }
                        if (step % 11 == 0 && Math.abs(w) == 5) {
                            setBlock(ctx, p.above(2), Blocks.LANTERN.defaultBlockState());
                        }
                        for (int s = 1; s <= 6; s++) {
                            setBlock(ctx, p.below(s), Blocks.BLACKSTONE_WALL.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static void buildUpperConcourseConnections(BuildContext ctx) {
        for (int i = 0; i < LAYERS.length - 1; i++) {
            LayerSpec lower = LAYERS[i];
            LayerSpec upper = LAYERS[i + 1];
            int y = ctx.y(upper.floor());
            for (Direction dir : CARDINALS) {
                buildTopConcourse(ctx, lower, upper, dir, y);
            }
            for (int angle = 45; angle < 360; angle += 90) {
                buildAccessTower(ctx, lower, upper, angle);
            }
        }
    }

    private static void buildTopConcourse(BuildContext ctx, LayerSpec lower, LayerSpec upper, Direction dir, int y) {
        Direction side = dir.getClockWise();
        int start = upper.radius() + 5;
        int end = lower.radius() - 10;
        for (int dist = start; dist <= end; dist++) {
            for (int w = -4; w <= 4; w++) {
                BlockPos p = ctx.origin.relative(dir, dist).relative(side, w).offset(0, y, 0);
                setBlock(ctx, p, roadState(ctx, p));
                for (int h = 1; h <= 3; h++) {
                    setBlock(ctx, p.above(h), Blocks.AIR.defaultBlockState());
                }
                if (Math.abs(w) == 4) {
                    setBlock(ctx, p.above(), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                }
                if (dist % 18 == 0 && Math.abs(w) == 3) {
                    setBlock(ctx, p.above(2), Blocks.LANTERN.defaultBlockState());
                }
                if (dist % 24 == 0 && Math.abs(w) == 4) {
                    for (int s = 1; s <= Math.min(18, y - ctx.y(lower.floor())); s++) {
                        setBlock(ctx, p.below(s), Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void buildAccessTower(BuildContext ctx, LayerSpec lower, LayerSpec upper, int angle) {
        double radians = Math.toRadians(angle);
        int radius = (lower.radius() + upper.radius()) / 2;
        int x = (int) Math.round(Math.cos(radians) * radius);
        int z = (int) Math.round(Math.sin(radians) * radius);
        int bottomY = ctx.y(lower.floor()) + 1;
        int topY = ctx.y(upper.floor());
        if (topY <= bottomY) {
            return;
        }

        BlockPos base = ctx.origin.offset(x, bottomY, z);
        BlockPos top = ctx.origin.offset(x, topY, z);
        if (!intersectsBox(ctx, base.offset(-5, 0, -5), top.offset(5, 0, 5))) {
            return;
        }

        boxShell(ctx, base.offset(-4, 0, -4), top.offset(4, 0, 4), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.AIR.defaultBlockState());
        fillRect(ctx, base.offset(-5, -1, -5), base.offset(5, -1, 5), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        fillRect(ctx, top.offset(-5, 0, -5), top.offset(5, 0, 5), Blocks.POLISHED_BLACKSTONE.defaultBlockState());

        Direction ladderSide = Direction.NORTH;
        Direction[] stairLoop = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
        for (int worldY = bottomY; worldY <= topY + 2; worldY++) {
            BlockPos c = new BlockPos(base.getX(), worldY, base.getZ());
            setBlock(ctx, c, Blocks.AIR.defaultBlockState());
            setBlock(ctx, c.relative(ladderSide), Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, ladderSide.getOpposite()));

            Direction stepDir = stairLoop[Math.floorMod(worldY - bottomY, stairLoop.length)];
            BlockPos step = c.relative(stepDir, 2);
            setBlock(ctx, step, Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, stepDir.getClockWise()));
        }
        for (Direction dir : CARDINALS) {
            fillRect(ctx, top.relative(dir, 1).offset(-1, 0, -1), top.relative(dir, 5).offset(1, 0, 1), roadState(ctx, top));
        }
    }

    private static void buildSecondaryVerticalRoutes(BuildContext ctx) {
        for (int angle = 22; angle < 360; angle += 45) {
            double radians = Math.toRadians(angle);
            int r = 82;
            int x = (int) Math.round(Math.cos(radians) * r);
            int z = (int) Math.round(Math.sin(radians) * r);
            BlockPos shaft = ctx.origin.offset(x, ctx.y(0) + 1, z);
            int top = ctx.y(375);
            for (int y = shaft.getY(); y <= ctx.origin.getY() + top; y++) {
                BlockPos p = new BlockPos(shaft.getX(), y, shaft.getZ());
                setBlock(ctx, p, Blocks.AIR.defaultBlockState());
                setBlock(ctx, p.relative(Direction.NORTH), Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
                if (Math.floorMod(y, 18) == 0) {
                    drawSmallColumn(ctx, p, 2, Blocks.IRON_BARS.defaultBlockState());
                }
            }
        }
    }

    private static void buildSpireField(BuildContext ctx) {
        for (LayerSpec layer : LAYERS) {
            int y = ctx.y(layer.top()) + 1;
            int angleStep = switch (layer.index()) {
                case 1 -> 7;
                case 2 -> 6;
                case 3, 4 -> 5;
                case 5, 6 -> 4;
                default -> 3;
            };
            for (int angle = 0; angle < 360; angle += angleStep) {
                if (isCardinalAngle(angle, layer.rampGapWidth())) {
                    continue;
                }
                RandomSource rand = RandomSource.create(hash(ctx.seed, layer.index(), angle, 0x51A1));
                int radialOffset = 2 + rand.nextInt(layer.index() <= 2 ? 15 : 8);
                double radians = Math.toRadians(angle + rand.nextInt(7) - 3);
                int r = layer.radius() - radialOffset;
                int x = (int) Math.round(Math.cos(radians) * r);
                int z = (int) Math.round(Math.sin(radians) * r);
                int nominalHeight = switch (layer.index()) {
                    case 1 -> 28 + rand.nextInt(44);
                    case 2, 3 -> 36 + rand.nextInt(58);
                    case 4, 5 -> 46 + rand.nextInt(78);
                    case 6 -> 64 + rand.nextInt(92);
                    default -> 86 + rand.nextInt(120);
                };
                int type = rand.nextInt(5);
                BlockPos base = ctx.origin.offset(x, y, z);
                buildSpire(ctx, base, scaled(ctx, nominalHeight), type, angle + layer.index() * 1000);
                if (type == 4 || rand.nextFloat() < 0.16F) {
                    for (int i = 0; i < 2 + rand.nextInt(3); i++) {
                        BlockPos off = base.offset(rand.nextInt(7) - 3, 0, rand.nextInt(7) - 3);
                        buildSpire(ctx, off, scaled(ctx, nominalHeight / 2 + rand.nextInt(20)), 2 + rand.nextInt(2), angle + i);
                    }
                }
            }
        }
    }

    private static void buildCitadel(BuildContext ctx) {
        int baseY = ctx.y(375) + 1;
        int topY = ctx.y(NOMINAL_CITADEL_TOP);
        BlockPos center = ctx.origin.offset(0, baseY, 0);
        int height = Math.max(28, topY - baseY);

        for (int y = 0; y <= height; y++) {
            int radius = y < height * 0.55 ? 33 : y < height * 0.82 ? 26 : 19;
            ring(ctx, center.above(y), radius, 3, (x, z, pos) -> {
                if (Math.floorMod(pos.getY() + x - z, 17) == 0) {
                    return Blocks.IRON_BARS.defaultBlockState();
                }
                return fortressStone(ctx, pos, 7);
            });
            if (y % 10 == 0) {
                fillCircle(ctx, center.above(y), radius - 2, floorState(ctx, center, 7));
            }
        }

        fillCircle(ctx, center.above(height + 1), 24, Blocks.WEATHERED_CUT_COPPER.defaultBlockState());
        placeLootChest(ctx, center.offset(0, 2, 8));

        int peakHeight = Math.max(24, ctx.y(NOMINAL_MAX_HEIGHT) - (baseY + height + 2));
        buildSpire(ctx, center.above(height + 2), peakHeight, 4, 9001);
        for (int angle = 0; angle < 360; angle += 30) {
            double radians = Math.toRadians(angle);
            int r = angle % 60 == 0 ? 28 : 18;
            BlockPos p = center.offset((int) Math.round(Math.cos(radians) * r), height - 8, (int) Math.round(Math.sin(radians) * r));
            buildSpire(ctx, p, scaled(ctx, 70 + (angle % 90)), angle % 5, angle);
        }
    }

    private static void buildWellChamber(BuildContext ctx) {
        int chamberBaseY = ctx.y(-NOMINAL_WELL_DEPTH);
        BlockPos center = ctx.origin.offset(0, chamberBaseY, 0);
        int radius = 24;
        for (int y = -2; y <= 18; y++) {
            if (y == -2 || y == 18) {
                fillCircle(ctx, center.above(y), radius, ModBlocks.ANCIENT_METAL_FLOOR.get().defaultBlockState());
            } else {
                ring(ctx, center.above(y), radius, 3, (x, z, pos) -> Math.floorMod(pos.getY() + x + z, 8) == 0
                        ? Blocks.IRON_BARS.defaultBlockState()
                        : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
        fillCircle(ctx, center.below(), 8, ModBlocks.ANCIENT_METAL_FLOOR.get().defaultBlockState());
        setBlock(ctx, center, ModBlocks.WELL_OF_ASCENSION_BLOCK.get().defaultBlockState());
        setBlock(ctx, center.above(), ModBlocks.WELL_PULSE_CORE.get().defaultBlockState());
        for (Direction dir : CARDINALS) {
            for (int d = radius - 4; d <= radius + 8; d++) {
                BlockPos p = center.relative(dir, d);
                setBlock(ctx, p, Blocks.AIR.defaultBlockState());
                setBlock(ctx, p.below(), ModBlocks.ANCIENT_METAL_FLOOR.get().defaultBlockState());
                setBlock(ctx, p.above(3), Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }

        BlockPos shaft = center.offset(18, 1, 0);
        for (int y = shaft.getY(); y <= ctx.origin.getY() + ctx.y(180); y++) {
            BlockPos p = new BlockPos(shaft.getX(), y, shaft.getZ());
            setBlock(ctx, p, Blocks.AIR.defaultBlockState());
            setBlock(ctx, p.relative(Direction.WEST), Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        }
    }

    private static void buildSpire(BuildContext ctx, BlockPos base, int height, int type, int salt) {
        if (height <= 2 || !intersectsBox(ctx, base.offset(-5, 0, -5), base.offset(5, height + 4, 5))) {
            return;
        }
        switch (type) {
            case 0 -> {
                for (int y = 0; y < height; y++) {
                    int r = y < height * 0.22 ? 4 : y < height * 0.52 ? 3 : y < height * 0.78 ? 2 : 1;
                    fillCircle(ctx, base.above(y), r, spireStone(ctx, base.above(y), salt));
                    if (y % 9 == 0) {
                        ring(ctx, base.above(y), r + 1, 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
                    }
                }
            }
            case 1 -> {
                for (int y = 0; y < height; y++) {
                    int r = y < height * 0.62 ? 2 : 1;
                    fillCircle(ctx, base.above(y), r, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                    if (y % 7 == 0) {
                        for (Direction dir : CARDINALS) {
                            setBlock(ctx, base.above(y).relative(dir, r + 1), Blocks.CHAIN.defaultBlockState());
                        }
                    }
                }
            }
            case 2 -> {
                for (int y = 0; y < height; y++) {
                    setBlock(ctx, base.above(y), y < height * 0.68 ? Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState());
                }
            }
            case 3 -> {
                for (int y = 0; y < height; y++) {
                    setBlock(ctx, base.above(y), y % 5 == 0 ? Blocks.IRON_BARS.defaultBlockState() : Blocks.CHAIN.defaultBlockState());
                }
            }
            default -> {
                buildSpire(ctx, base, height, 1, salt);
                buildSpire(ctx, base.offset(2, 0, 1), Math.max(4, height * 2 / 3), 2, salt + 1);
                buildSpire(ctx, base.offset(-2, 0, -1), Math.max(4, height / 2), 3, salt + 2);
                buildSpire(ctx, base.offset(1, 0, -2), Math.max(4, height * 3 / 5), 2, salt + 3);
            }
        }
        setBlock(ctx, base.above(height), Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP));
        if (height > 16) {
            setBlock(ctx, base.above(height / 2).relative(Direction.NORTH), Blocks.LANTERN.defaultBlockState());
        }
    }

    private static void spawnPalaceGuards(BuildContext ctx) {
        spawnMetalborn(ctx, ModEntityTypes.STEEL_INQUISITOR.get(), ctx.origin.offset(0, ctx.y(382), 0), MetalbornRole.STEEL_INQUISITOR);
        for (int i = 0; i < 8; i++) {
            double radians = Math.toRadians(i * 45);
            int x = (int) Math.round(Math.cos(radians) * 24);
            int z = (int) Math.round(Math.sin(radians) * 24);
            spawnMetalborn(ctx, ModEntityTypes.MISTBORN_ASSASSIN.get(), ctx.origin.offset(x, ctx.y(375) + 2, z), MetalbornRole.MISTBORN_ASSASSIN);
        }
        for (LayerSpec layer : LAYERS) {
            if (layer.index() > 4) {
                continue;
            }
            for (int angle = 0; angle < 360; angle += 90) {
                double radians = Math.toRadians(angle + 18);
                int r = layer.radius() - 28;
                BlockPos p = ctx.origin.offset((int) Math.round(Math.cos(radians) * r), ctx.y(layer.floor()) + 2, (int) Math.round(Math.sin(radians) * r));
                MetalbornRole role = switch (layer.index()) {
                    case 1 -> MetalbornRole.LURCHER_GUARD;
                    case 2 -> MetalbornRole.SEEKER;
                    case 3 -> MetalbornRole.PEWTER_THUG;
                    default -> MetalbornRole.SMOKER;
                };
                var type = ModEntityTypes.METALBORN.get(role);
                if (type != null) {
                    spawnMetalborn(ctx, type.get(), p, role);
                }
            }
        }
    }

    private static void spawnMetalborn(BuildContext ctx, net.minecraft.world.entity.EntityType<MetalbornEnemy> type, BlockPos pos, MetalbornRole role) {
        if (!inside(ctx, pos)) {
            return;
        }
        MetalbornEnemy enemy = type.create(ctx.level.getLevel());
        if (enemy == null) {
            return;
        }
        enemy.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, ctx.level.getRandom().nextFloat() * 360F, 0F);
        enemy.finalizeSpawn(ctx.level, ctx.level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        enemy.setCustomName(Component.literal(role.displayName()));
        ctx.level.addFreshEntity(enemy);
    }

    private static void fillCircle(BuildContext ctx, BlockPos center, int radius, BlockState state) {
        fillCircle(ctx, center, radius, (x, z, pos) -> state);
    }

    private static void fillCircle(BuildContext ctx, BlockPos center, int radius, BlockSelector selector) {
        if (!intersectsBox(ctx, center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
            return;
        }
        int minX = minRelX(ctx, center, radius);
        int maxX = maxRelX(ctx, center, radius);
        int minZ = minRelZ(ctx, center, radius);
        int maxZ = maxRelZ(ctx, center, radius);
        int r2 = radius * radius;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (x * x + z * z <= r2) {
                    BlockPos pos = center.offset(x, 0, z);
                    setBlock(ctx, pos, selector.state(x, z, pos));
                }
            }
        }
    }

    private static void ring(BuildContext ctx, BlockPos center, int radius, int thickness, BlockState state) {
        ring(ctx, center, radius, thickness, (x, z, pos) -> state);
    }

    private static void ring(BuildContext ctx, BlockPos center, int radius, int thickness, BlockSelector selector) {
        if (!intersectsBox(ctx, center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
            return;
        }
        int minX = minRelX(ctx, center, radius);
        int maxX = maxRelX(ctx, center, radius);
        int minZ = minRelZ(ctx, center, radius);
        int maxZ = maxRelZ(ctx, center, radius);
        int outer = radius * radius;
        int innerRadius = Math.max(0, radius - thickness);
        int inner = innerRadius * innerRadius;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int d2 = x * x + z * z;
                if (d2 <= outer && d2 >= inner) {
                    BlockPos pos = center.offset(x, 0, z);
                    setBlock(ctx, pos, selector.state(x, z, pos));
                }
            }
        }
    }

    private static void drawSmallColumn(BuildContext ctx, BlockPos center, int radius, BlockState state) {
        fillCircle(ctx, center, radius, state);
    }

    private static void fillRect(BuildContext ctx, BlockPos a, BlockPos b, BlockState state) {
        if (!intersectsBox(ctx, a, b)) {
            return;
        }
        int minX = Math.max(Math.min(a.getX(), b.getX()), ctx.minX());
        int maxX = Math.min(Math.max(a.getX(), b.getX()), ctx.maxX());
        int minY = Math.max(Math.min(a.getY(), b.getY()), ctx.minY());
        int maxY = Math.min(Math.max(a.getY(), b.getY()), ctx.maxY());
        int minZ = Math.max(Math.min(a.getZ(), b.getZ()), ctx.minZ());
        int maxZ = Math.min(Math.max(a.getZ(), b.getZ()), ctx.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setBlock(ctx, new BlockPos(x, y, z), state);
                }
            }
        }
    }

    private static void boxShell(BuildContext ctx, BlockPos a, BlockPos b, BlockState shell, BlockState inside) {
        if (!intersectsBox(ctx, a, b)) {
            return;
        }
        int minX = Math.max(Math.min(a.getX(), b.getX()), ctx.minX());
        int maxX = Math.min(Math.max(a.getX(), b.getX()), ctx.maxX());
        int minY = Math.max(Math.min(a.getY(), b.getY()), ctx.minY());
        int maxY = Math.min(Math.max(a.getY(), b.getY()), ctx.maxY());
        int minZ = Math.max(Math.min(a.getZ(), b.getZ()), ctx.minZ());
        int maxZ = Math.min(Math.max(a.getZ(), b.getZ()), ctx.maxZ());
        int realMinX = Math.min(a.getX(), b.getX());
        int realMaxX = Math.max(a.getX(), b.getX());
        int realMinY = Math.min(a.getY(), b.getY());
        int realMaxY = Math.max(a.getY(), b.getY());
        int realMinZ = Math.min(a.getZ(), b.getZ());
        int realMaxZ = Math.max(a.getZ(), b.getZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == realMinX || x == realMaxX || y == realMinY || y == realMaxY || z == realMinZ || z == realMaxZ;
                    setBlock(ctx, new BlockPos(x, y, z), edge ? shell : inside);
                }
            }
        }
    }

    private static void clearCylinder(BuildContext ctx, BlockPos base, int radius, int height) {
        for (int y = 0; y <= height; y++) {
            fillCircle(ctx, base.above(y), radius, Blocks.AIR.defaultBlockState());
        }
    }

    private static void fillRectInCircle(BuildContext ctx, int y, int minRelX, int maxRelX, int minRelZ, int maxRelZ, int radius, BlockState state) {
        int minX = Math.max(ctx.origin.getX() + minRelX, ctx.minX());
        int maxX = Math.min(ctx.origin.getX() + maxRelX, ctx.maxX());
        int minZ = Math.max(ctx.origin.getZ() + minRelZ, ctx.minZ());
        int maxZ = Math.min(ctx.origin.getZ() + maxRelZ, ctx.maxZ());
        int r2 = radius * radius;
        for (int x = minX; x <= maxX; x++) {
            int relX = x - ctx.origin.getX();
            for (int z = minZ; z <= maxZ; z++) {
                int relZ = z - ctx.origin.getZ();
                if (relX * relX + relZ * relZ <= r2) {
                    setBlock(ctx, new BlockPos(x, ctx.origin.getY() + y, z), state);
                }
            }
        }
    }

    private static void clearRectInCircle(BuildContext ctx, int minY, int maxY, int minRelX, int maxRelX, int minRelZ, int maxRelZ, int radius) {
        for (int y = minY; y <= maxY; y++) {
            fillRectInCircle(ctx, y, minRelX, maxRelX, minRelZ, maxRelZ, radius, Blocks.AIR.defaultBlockState());
        }
    }

    private static void rectFrame(BuildContext ctx, int y, int minRelX, int maxRelX, int minRelZ, int maxRelZ, BlockState state) {
        fillRect(ctx, ctx.origin.offset(minRelX, y, minRelZ), ctx.origin.offset(maxRelX, y, minRelZ), state);
        fillRect(ctx, ctx.origin.offset(minRelX, y, maxRelZ), ctx.origin.offset(maxRelX, y, maxRelZ), state);
        fillRect(ctx, ctx.origin.offset(minRelX, y, minRelZ), ctx.origin.offset(minRelX, y, maxRelZ), state);
        fillRect(ctx, ctx.origin.offset(maxRelX, y, minRelZ), ctx.origin.offset(maxRelX, y, maxRelZ), state);
    }

    private static void placeLootChest(BuildContext ctx, BlockPos pos) {
        if (!inside(ctx, pos)) {
            return;
        }
        setBlock(ctx, pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        RandomizableContainerBlockEntity.setLootTable(ctx.level, RandomSource.create(hash(ctx.seed, pos.getX(), pos.getY(), pos.getZ())), pos, KREDIK_SHAW_LOOT);
    }

    private static BlockState terraceFloor(BuildContext ctx, BlockPos pos, int layer, int relX, int relZ) {
        if (isAxisCorridor(relX, relZ, layer <= 2 ? 18 : 12, OUTER_RADIUS)) {
            return roadState(ctx, pos);
        }
        return floorState(ctx, pos, layer);
    }

    private static BlockState floorState(BuildContext ctx, BlockPos pos, int layer) {
        int roll = hashPercent(ctx.seed, pos, layer);
        if (roll < 42) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (roll < 58) {
            return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (roll < 72) {
            return Blocks.POLISHED_ANDESITE.defaultBlockState();
        }
        if (roll < 86) {
            return Blocks.TUFF.defaultBlockState();
        }
        return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    }

    private static BlockState roadState(BuildContext ctx, BlockPos pos) {
        int roll = hashPercent(ctx.seed, pos, 901);
        if (roll < 50) {
            return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (roll < 72) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (roll < 88) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        return Blocks.CUT_COPPER.defaultBlockState();
    }

    private static BlockState fortressStone(BuildContext ctx, BlockPos pos, int layer) {
        int roll = hashPercent(ctx.seed, pos, layer * 23 + 5);
        if (roll < 34) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (roll < 51) {
            return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (roll < 68) {
            return layer <= 2 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (roll < 82) {
            return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (roll < 93) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        return Blocks.TUFF.defaultBlockState();
    }

    private static BlockState spireStone(BuildContext ctx, BlockPos pos, int salt) {
        int roll = hashPercent(ctx.seed, pos, salt);
        if (roll < 45) {
            return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        if (roll < 74) {
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        if (roll < 92) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        return Blocks.WEATHERED_COPPER.defaultBlockState();
    }

    private static boolean isAxisCorridor(int x, int z, int width, int radius) {
        return Math.abs(x) <= width && Math.abs(z) <= radius || Math.abs(z) <= width && Math.abs(x) <= radius;
    }

    private static boolean isCardinalGateGap(int x, int z, int radius, int width) {
        return Math.abs(x) <= width && Math.abs(Math.abs(z) - radius) <= 8
                || Math.abs(z) <= width && Math.abs(Math.abs(x) - radius) <= 8;
    }

    private static boolean isCardinalAngle(int angle, int widthDegrees) {
        int normalized = Math.floorMod(angle, 90);
        return normalized <= widthDegrees / 2 || normalized >= 90 - widthDegrees / 2;
    }

    private static int minRelX(BuildContext ctx, BlockPos center, int radius) {
        return Math.max(-radius, ctx.minX() - center.getX());
    }

    private static int maxRelX(BuildContext ctx, BlockPos center, int radius) {
        return Math.min(radius, ctx.maxX() - center.getX());
    }

    private static int minRelZ(BuildContext ctx, BlockPos center, int radius) {
        return Math.max(-radius, ctx.minZ() - center.getZ());
    }

    private static int maxRelZ(BuildContext ctx, BlockPos center, int radius) {
        return Math.min(radius, ctx.maxZ() - center.getZ());
    }

    private static boolean intersectsBox(BuildContext ctx, BlockPos a, BlockPos b) {
        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int maxY = Math.max(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxZ = Math.max(a.getZ(), b.getZ());
        return maxX >= ctx.minX() && minX <= ctx.maxX()
                && maxY >= ctx.minY() && minY <= ctx.maxY()
                && maxZ >= ctx.minZ() && minZ <= ctx.maxZ();
    }

    private static void setBlock(BuildContext ctx, BlockPos pos, BlockState state) {
        if (!inside(ctx, pos)) {
            return;
        }
        ctx.level.setBlock(pos, state, 2);
        if (!ctx.level.isClientSide() && (state.is(ModBlocks.WELL_PULSE_CORE.get()) || state.is(ModBlocks.WELL_OF_ASCENSION_BLOCK.get()))) {
            if (ctx.level instanceof ServerLevel serverLevel) {
                WellRegistry.register(serverLevel, pos);
            }
        }
    }

    private static boolean inside(BuildContext ctx, BlockPos pos) {
        return pos.getY() >= ctx.level.getMinBuildHeight()
                && pos.getY() < ctx.level.getMaxBuildHeight()
                && pos.getX() >= ctx.minX()
                && pos.getX() <= ctx.maxX()
                && pos.getZ() >= ctx.minZ()
                && pos.getZ() <= ctx.maxZ()
                && pos.getY() >= ctx.minY()
                && pos.getY() <= ctx.maxY();
    }

    private static int scaled(BuildContext ctx, int nominal) {
        return Math.max(1, (int) Math.round(nominal * ctx.verticalScale));
    }

    private static int hashPercent(long seed, BlockPos pos, int salt) {
        return (int) Math.floorMod(hash(seed, pos.getX(), pos.getY(), pos.getZ(), salt), 100);
    }

    private static long hash(long seed, int a, int b, int c) {
        return hash(seed, a, b, c, 0x9E3779B9);
    }

    private static long hash(long seed, int a, int b, int c, int d) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h ^= (long) a * 0xBF58476D1CE4E5B9L;
        h = Long.rotateLeft(h, 21);
        h ^= (long) b * 0x94D049BB133111EBL;
        h = Long.rotateLeft(h, 17);
        h ^= (long) c * 0xD6E8FEB86659FD93L;
        h = Long.rotateLeft(h, 29);
        h ^= (long) d * 0xA0761D6478BD642FL;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return h;
    }

    private record LayerSpec(int index, int radius, int floor, int top, int moduleBase, int moduleHeight, int rampGapWidth) {
    }

    private enum ModuleKind {
        MARKET,
        WAREHOUSE,
        PUBLIC_HALL,
        BARRACKS,
        FORGE,
        ADMIN,
        RECORD_HALL,
        RESIDENCE,
        TEMPLE,
        ARCHIVE,
        NOBLE_HALL,
        CITADEL_ANNEX
    }

    @FunctionalInterface
    private interface BlockSelector {
        BlockState state(int relX, int relZ, BlockPos pos);
    }

    private record BuildContext(ServerLevelAccessor level, BlockPos origin, BoundingBox bounds, double verticalScale, long seed) {
        static BuildContext create(ServerLevelAccessor level, BlockPos origin, BoundingBox bounds, long salt) {
            double available = Math.max(80, level.getMaxBuildHeight() - origin.getY() - 8);
            double scale = Math.min(1.0D, Math.max(0.42D, available / (double) NOMINAL_MAX_HEIGHT));
            long seed = hash(level.getLevel().getSeed() ^ salt, origin.getX(), origin.getY(), origin.getZ());
            BoundingBox effectiveBounds = bounds == null ? fullBounds(origin) : bounds;
            return new BuildContext(level, origin, effectiveBounds, scale, seed);
        }

        int y(int nominalY) {
            return (int) Math.round(nominalY * verticalScale);
        }

        int minX() {
            return bounds.minX();
        }

        int maxX() {
            return bounds.maxX();
        }

        int minY() {
            return Math.max(bounds.minY(), level.getMinBuildHeight());
        }

        int maxY() {
            return Math.min(bounds.maxY(), level.getMaxBuildHeight() - 1);
        }

        int minZ() {
            return bounds.minZ();
        }

        int maxZ() {
            return bounds.maxZ();
        }
    }
}
