package com.not_noah.mistborn_metal_arts.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class CommonConfig {
    public static final CommonConfig VALUES;
    public static final ForgeConfigSpec SPEC;

    public final ForgeConfigSpec.BooleanValue showHud;
    public final ForgeConfigSpec.IntValue hudX;
    public final ForgeConfigSpec.IntValue hudY;
    public final ForgeConfigSpec.BooleanValue showBronzePulse;
    public final ForgeConfigSpec.BooleanValue suppressMagicParticlesInsideCoppercloud;
    public final ForgeConfigSpec.BooleanValue debugLogging;

    static {
        Pair<CommonConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        VALUES = pair.getLeft();
        SPEC = pair.getRight();
    }

    private CommonConfig(ForgeConfigSpec.Builder builder) {
        builder.push("hud");
        showHud = builder.comment("Show the Metal Arts reserve HUD.").define("showHud", true);
        hudX = builder.defineInRange("hudX", 8, 0, 10000);
        hudY = builder.defineInRange("hudY", 28, 0, 10000);
        showBronzePulse = builder.comment("Show Bronze seeking pulse hints on the HUD.").define("showBronzePulse", true);
        builder.pop();

        builder.push("visuals");
        suppressMagicParticlesInsideCoppercloud = builder.comment("Client hint for future coppercloud particle suppression.").define("suppressMagicParticlesInsideCoppercloud", true);
        builder.pop();

        builder.push("debug");
        debugLogging = builder.define("debugLogging", false);
        builder.pop();
    }
}
