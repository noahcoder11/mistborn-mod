package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import com.not_noah.mistborn_metal_arts.client.screen.MetalArtsMachineMenu;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MistbornMetalArts.MOD_ID);

    public static final RegistryObject<MenuType<MetalArtsMachineMenu>> METAL_ARTS_MACHINE = MENUS.register("metal_arts_machine", () -> IForgeMenuType.create(MetalArtsMachineMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
