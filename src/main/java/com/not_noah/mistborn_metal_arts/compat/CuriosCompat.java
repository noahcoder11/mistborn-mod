package com.not_noah.mistborn_metal_arts.compat;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class CuriosCompat {
    private static final String CURIOS_MOD_ID = "curios";
    private static final String COMMON_INTEGRATION = "com.not_noah.mistborn_metal_arts.curios.CuriosIntegration";
    private static final String CLIENT_INTEGRATION = "com.not_noah.mistborn_metal_arts.curios.CuriosClientIntegration";

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }

    public static void register(IEventBus modBus) {
        if (!isLoaded()) {
            MistbornMetalArts.LOGGER.info("Curios not detected; Hemalurgic spike slots will use inventory/bind-table fallback.");
            return;
        }
        try {
            MinecraftForge.EVENT_BUS.register(Class.forName(COMMON_INTEGRATION));
            if (FMLEnvironment.dist.isClient()) {
                modBus.register(Class.forName(CLIENT_INTEGRATION));
            }
            MistbornMetalArts.LOGGER.info("Curios detected; registered Hemalurgic spike curio integration.");
        } catch (ClassNotFoundException exception) {
            MistbornMetalArts.LOGGER.warn("Curios is present, but Mistborn Curios integration classes could not be loaded.", exception);
        }
    }

    public static boolean equipSpikeFromUse(ServerPlayer player, ItemStack stack) {
        if (!isLoaded()) {
            return false;
        }
        return invokeBoolean("equipSpikeFromUse", new Class<?>[]{ServerPlayer.class, ItemStack.class}, player, stack);
    }

    public static boolean removeEquippedSpike(ServerPlayer player) {
        if (!isLoaded()) {
            return false;
        }
        return invokeBoolean("removeEquippedSpike", new Class<?>[]{ServerPlayer.class}, player);
    }

    public static void refreshEquippedHemalurgicSpikes(ServerPlayer player, MetalArtsData data) {
        data.setEquippedSpikeCorruption(0);
        if (!isLoaded()) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(COMMON_INTEGRATION);
            Method method = clazz.getMethod("refreshEquippedHemalurgicSpikes", ServerPlayer.class, MetalArtsData.class);
            method.invoke(null, player, data);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            data.setEquippedSpikeCorruption(0);
            MistbornMetalArts.LOGGER.warn("Failed to refresh Hemalurgic Curios data; treating Curios spikes as inactive this tick.", exception);
        }
    }

    public static ItemStack findMetalmind(ServerPlayer player, Metal metal) {
        if (!isLoaded()) {
            return ItemStack.EMPTY;
        }
        try {
            Class<?> clazz = Class.forName(COMMON_INTEGRATION);
            Method method = clazz.getMethod("findMetalmind", ServerPlayer.class, Metal.class);
            Object result = method.invoke(null, player, metal);
            return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return ItemStack.EMPTY;
        }
    }

    public static void replaceCurioStack(Player player, ItemStack original, ItemStack replacement) {
        if (!isLoaded()) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(COMMON_INTEGRATION);
            Method method = clazz.getMethod("replaceCurioStack", Player.class, ItemStack.class, ItemStack.class);
            method.invoke(null, player, original, replacement);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            MistbornMetalArts.LOGGER.warn("Failed to replace Curios stack.", exception);
        }
    }

    private static boolean invokeBoolean(String methodName, Class<?>[] parameters, Object... args) {
        try {
            Class<?> clazz = Class.forName(COMMON_INTEGRATION);
            Method method = clazz.getMethod(methodName, parameters);
            Object result = method.invoke(null, args);
            return result instanceof Boolean value && value;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            MistbornMetalArts.LOGGER.warn("Curios compatibility call {} failed.", methodName, exception);
            return false;
        }
    }
}
