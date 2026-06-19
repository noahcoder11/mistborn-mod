package com.not_noah.mistborn_metal_arts;

import com.not_noah.mistborn_metal_arts.capability.MetalArtsEvents;
import com.not_noah.mistborn_metal_arts.command.MetalArtsCommand;
import com.not_noah.mistborn_metal_arts.compat.CuriosCompat;
import com.not_noah.mistborn_metal_arts.config.CommonConfig;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.registry.ModBlockEntities;
import com.not_noah.mistborn_metal_arts.registry.ModBlocks;
import com.not_noah.mistborn_metal_arts.registry.ModCreativeTabs;
import com.not_noah.mistborn_metal_arts.registry.ModEffects;
import com.not_noah.mistborn_metal_arts.registry.ModEntityTypes;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import com.not_noah.mistborn_metal_arts.registry.ModMenus;
import com.not_noah.mistborn_metal_arts.registry.ModParticles;
import com.not_noah.mistborn_metal_arts.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MistbornMetalArts.MOD_ID)
public class MistbornMetalArts {
    public static final String MOD_ID = "mistborn_metal_arts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MistbornMetalArts() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModEffects.register(modBus);
        ModSounds.register(modBus);
        ModParticles.register(modBus);
        ModMenus.register(modBus);
        ModBlockEntities.register(modBus);
        ModEntityTypes.register(modBus);
        ModCreativeTabs.register(modBus);
        modBus.addListener(ModEntityTypes::registerAttributes);
        CuriosCompat.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        MetalArtsNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        MetalArtsCommand.register(event.getDispatcher());
    }
}
