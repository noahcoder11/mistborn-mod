package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MistbornMetalArts.MOD_ID);

    public static final RegistryObject<SimpleParticleType> METAL_LINE = PARTICLES.register("metal_line", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> COPPERCLOUD = PARTICLES.register("coppercloud", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ATIUM_SHADOW = PARTICLES.register("atium_shadow", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PEWTER_FLARE = PARTICLES.register("pewter_flare", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIN_GLINT = PARTICLES.register("tin_glint", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> EMOTIONAL_WAVE = PARTICLES.register("emotional_wave", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BRONZE_PULSE = PARTICLES.register("bronze_pulse", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> HEMALURGIC_SPARK = PARTICLES.register("hemalurgic_spark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TIME_BUBBLE_EDGE = PARTICLES.register("time_bubble_edge", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_DROP = PARTICLES.register("blood_drop", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_SPLATTER = PARTICLES.register("blood_splatter", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BLOOD_SLASH = PARTICLES.register("blood_slash", () -> new SimpleParticleType(false));

    private ModParticles() {
    }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
