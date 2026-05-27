package com.not_noah.mistborn_metal_arts.registry;

import com.not_noah.mistborn_metal_arts.MistbornMetalArts;
import com.not_noah.mistborn_metal_arts.effect.SimpleMetalEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MistbornMetalArts.MOD_ID);

    public static final RegistryObject<MobEffect> PEWTER_DRAG = EFFECTS.register("pewter_drag", () -> new SimpleMetalEffect(MobEffectCategory.HARMFUL, 0x5f6d76));
    public static final RegistryObject<MobEffect> SENSORY_OVERLOAD = EFFECTS.register("sensory_overload", () -> new SimpleMetalEffect(MobEffectCategory.HARMFUL, 0xf2e8c9));
    public static final RegistryObject<MobEffect> COPPERCLOUD = EFFECTS.register("coppercloud", () -> new SimpleMetalEffect(MobEffectCategory.BENEFICIAL, 0xb97739));
    public static final RegistryObject<MobEffect> BRONZE_SEEKING = EFFECTS.register("bronze_seeking", () -> new SimpleMetalEffect(MobEffectCategory.BENEFICIAL, 0x6c4a28));
    public static final RegistryObject<MobEffect> ATIUM_SIGHT = EFFECTS.register("atium_sight", () -> new SimpleMetalEffect(MobEffectCategory.BENEFICIAL, 0xc7ffe8));
    public static final RegistryObject<MobEffect> EMOTIONAL_PRESSURE = EFFECTS.register("emotional_pressure", () -> new SimpleMetalEffect(MobEffectCategory.HARMFUL, 0xd24d7a));
    public static final RegistryObject<MobEffect> HEMALURGIC_CORRUPTION = EFFECTS.register("hemalurgic_corruption", () -> new SimpleMetalEffect(MobEffectCategory.HARMFUL, 0x9c1d2a));
    public static final RegistryObject<MobEffect> EMOTIONAL_RIOT = EFFECTS.register("emotional_riot", () -> new SimpleMetalEffect(MobEffectCategory.HARMFUL, 0xc95442));
    public static final RegistryObject<MobEffect> EMOTIONAL_SOOTHE = EFFECTS.register("emotional_soothe", () -> new SimpleMetalEffect(MobEffectCategory.BENEFICIAL, 0xd2bb72));
    public static final RegistryObject<MobEffect> GOLD_SIGHT = EFFECTS.register("gold_sight", () -> new SimpleMetalEffect(MobEffectCategory.BENEFICIAL, 0xffd700));
    public static final RegistryObject<MobEffect> ELECTRUM_SIGHT = EFFECTS.register("electrum_sight", () -> new SimpleMetalEffect(MobEffectCategory.BENEFICIAL, 0xe2e2e2));

    private ModEffects() {
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
