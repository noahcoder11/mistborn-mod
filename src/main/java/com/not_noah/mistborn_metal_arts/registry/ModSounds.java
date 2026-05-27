package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MistbornMetalArts.MOD_ID);

    public static final RegistryObject<SoundEvent> METAL_BURN = register("metal_burn");
    public static final RegistryObject<SoundEvent> METAL_PUSH = register("metal_push");
    public static final RegistryObject<SoundEvent> METAL_PULL = register("metal_pull");
    public static final RegistryObject<SoundEvent> BRONZE_PULSE = register("bronze_pulse");
    public static final RegistryObject<SoundEvent> LERASIUM_CONSUME = register("lerasium_consume");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MistbornMetalArts.MOD_ID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
