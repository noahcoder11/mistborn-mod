package com.not_noah.mistborn_metal_arts.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WellRegistry {
    private static final Map<Level, Set<BlockPos>> WELLS = new ConcurrentHashMap<>();

    private WellRegistry() {
    }

    public static void register(Level level, BlockPos pos) {
        WELLS.computeIfAbsent(level, k -> new HashSet<>()).add(pos.immutable());
    }

    public static void unregister(Level level, BlockPos pos) {
        Set<BlockPos> positions = WELLS.get(level);
        if (positions != null) {
            positions.remove(pos);
        }
    }

    public static Set<BlockPos> getWells(Level level) {
        return WELLS.getOrDefault(level, Set.of());
    }
}
