package com.not_noah.mistborn_metal_arts.config;

import com.not_noah.mistborn_metal_arts.api.Metal;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;

public final class ServerConfig {
    public static final ServerConfig VALUES;
    public static final ForgeConfigSpec SPEC;

    public final ForgeConfigSpec.BooleanValue randomPowersOnFirstJoin;
    public final ForgeConfigSpec.DoubleValue mistingChance;
    public final ForgeConfigSpec.DoubleValue mistbornChance;
    public final ForgeConfigSpec.DoubleValue ferringChance;
    public final ForgeConfigSpec.DoubleValue fullFeruchemistChance;
    public final ForgeConfigSpec.DoubleValue twinbornChance;
    public final ForgeConfigSpec.BooleanValue powersPersistThroughRespawn;
    public final ForgeConfigSpec.BooleanValue powersLostOnDeath;

    public final ForgeConfigSpec.BooleanValue pvpPowerEffects;
    public final ForgeConfigSpec.BooleanValue lerasiumExists;
    public final ForgeConfigSpec.BooleanValue atiumExists;
    public final ForgeConfigSpec.BooleanValue lerasiumGrantsMistborn;
    public final ForgeConfigSpec.BooleanValue lerasiumCraftable;
    public final ForgeConfigSpec.BooleanValue lerasiumLoot;
    public final ForgeConfigSpec.BooleanValue lerasiumAlloysGrantMistings;

    public final ForgeConfigSpec.BooleanValue hemalurgyEnabled;
    public final ForgeConfigSpec.BooleanValue hemalurgyMobStealing;
    public final ForgeConfigSpec.DoubleValue spikeDecayRate;
    public final ForgeConfigSpec.IntValue maxSpikesBeforeCorruption;
    public final ForgeConfigSpec.IntValue maxInstalledSpikes;
    public final ForgeConfigSpec.BooleanValue spikeRemovalPossible;
    public final ForgeConfigSpec.BooleanValue removedSpikesRetainCharge;

    public final ForgeConfigSpec.DoubleValue soulStabilityBaseMax;
    public final ForgeConfigSpec.DoubleValue linchpinStabilityBonus;
    public final ForgeConfigSpec.DoubleValue stabilityLossPerSpike;
    public final ForgeConfigSpec.DoubleValue stabilityLossPerDuplicate;

    public final ForgeConfigSpec.DoubleValue contaminationPerSpike;
    public final ForgeConfigSpec.DoubleValue contaminationDecayRate;

    public final ForgeConfigSpec.DoubleValue savantGainPerBurnTick;
    public final ForgeConfigSpec.DoubleValue savantGainPerFlareTick;
    public final ForgeConfigSpec.DoubleValue savantGainPerSpikeStack;
    public final ForgeConfigSpec.DoubleValue savantDecayRate;

    public final ForgeConfigSpec.DoubleValue bloatPerForcedSystem;

    public final ForgeConfigSpec.IntValue maxDuplicateSpikesPerPower;

    public final ForgeConfigSpec.DoubleValue spikeDecayRateOutside;
    public final ForgeConfigSpec.DoubleValue spikeDecayRateBlood;
    public final ForgeConfigSpec.DoubleValue spikeDecayRateAluminum;
    public final ForgeConfigSpec.IntValue instantTransferWindow;
    public final ForgeConfigSpec.DoubleValue instantTransferRetention;

    public final ForgeConfigSpec.BooleanValue feruchemyEnabled;
    public final ForgeConfigSpec.DoubleValue metalmindCapacity;
    public final ForgeConfigSpec.DoubleValue feruchemyStoreRate;
    public final ForgeConfigSpec.DoubleValue feruchemyTapRate;
    public final ForgeConfigSpec.BooleanValue unkeyedMetalmindsEnabled;

    public final ForgeConfigSpec.DoubleValue maxPushPullRange;
    public final ForgeConfigSpec.DoubleValue maxPushPullForce;
    public final ForgeConfigSpec.DoubleValue pushPullStrength;
    public final ForgeConfigSpec.BooleanValue allowBlockMovement;
    public final ForgeConfigSpec.IntValue maxActiveBubblesPerPlayer;
    public final ForgeConfigSpec.IntValue maxActiveBubblesServer;
    public final ForgeConfigSpec.DoubleValue timeBubbleRadius;
    public final ForgeConfigSpec.IntValue timeBubbleDuration;

    public final ForgeConfigSpec.BooleanValue skillProgressionEnabled;
    public final EnumMap<Metal, ForgeConfigSpec.BooleanValue> metalEnabled = new EnumMap<>(Metal.class);
    public final EnumMap<Metal, ForgeConfigSpec.DoubleValue> reserveCapacities = new EnumMap<>(Metal.class);
    public final EnumMap<Metal, ForgeConfigSpec.DoubleValue> burnRates = new EnumMap<>(Metal.class);
    public final EnumMap<Metal, ForgeConfigSpec.DoubleValue> vialValues = new EnumMap<>(Metal.class);
    public final EnumMap<Metal, ForgeConfigSpec.DoubleValue> powerStrengths = new EnumMap<>(Metal.class);

    public final ForgeConfigSpec.DoubleValue atiumRarity;
    public final ForgeConfigSpec.DoubleValue lerasiumRarity;
    public final ForgeConfigSpec.DoubleValue structureSpawnRate;
    public final ForgeConfigSpec.DoubleValue mobSpawnRate;
    public final ForgeConfigSpec.BooleanValue kredikShawEnabled;
    public final ForgeConfigSpec.IntValue kredikShawSpacing;
    public final ForgeConfigSpec.IntValue kredikShawSeparation;
    public final ForgeConfigSpec.IntValue kredikShawSalt;
    public final ForgeConfigSpec.DoubleValue kredikShawLootMultiplier;
    public final ForgeConfigSpec.DoubleValue kredikShawMobDensity;
    public final ForgeConfigSpec.BooleanValue kredikShawHasWell;
    public final ForgeConfigSpec.BooleanValue kredikShawRequiresKeyForWell;
    public final ForgeConfigSpec.BooleanValue wellEnabled;
    public final ForgeConfigSpec.BooleanValue wellRequiresKredikShaw;
    public final ForgeConfigSpec.BooleanValue wellCanGrantLerasium;
    public final ForgeConfigSpec.BooleanValue wellCanGrantMistborn;
    public final ForgeConfigSpec.IntValue wellEventCooldown;
    public final ForgeConfigSpec.BooleanValue wellOneUsePerPlayer;
    public final ForgeConfigSpec.BooleanValue wellOneUsePerWorld;
    public final ForgeConfigSpec.BooleanValue wellSummonsBoss;
    public final ForgeConfigSpec.DoubleValue wellBronzePulseRange;

    static {
        Pair<ServerConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        VALUES = pair.getLeft();
        SPEC = pair.getRight();
    }

    private ServerConfig(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        randomPowersOnFirstJoin = builder.define("randomPowersOnFirstJoin", true);
        powersPersistThroughRespawn = builder.define("powersPersistThroughRespawn", true);
        powersLostOnDeath = builder.define("powersLostOnDeath", false);
        skillProgressionEnabled = builder.define("skillProgressionEnabled", true);
        builder.pop();

        builder.push("power_assignment");
        mistingChance = builder.defineInRange("mistingChance", 0.08D, 0D, 1D);
        mistbornChance = builder.defineInRange("mistbornChance", 0.002D, 0D, 1D);
        ferringChance = builder.defineInRange("ferringChance", 0.08D, 0D, 1D);
        fullFeruchemistChance = builder.defineInRange("fullFeruchemistChance", 0.001D, 0D, 1D);
        twinbornChance = builder.defineInRange("twinbornChance", 0.01D, 0D, 1D);
        builder.pop();

        builder.push("pvp");
        pvpPowerEffects = builder.comment("PvP effects default conservative. No direct player mind control is implemented.").define("pvpPowerEffects", false);
        builder.pop();

        builder.push("god_metals");
        atiumExists = builder.define("atiumExists", true);
        lerasiumExists = builder.define("lerasiumExists", true);
        lerasiumGrantsMistborn = builder.define("lerasiumGrantsMistborn", true);
        lerasiumCraftable = builder.define("lerasiumCraftable", false);
        lerasiumLoot = builder.define("lerasiumLoot", true);
        lerasiumAlloysGrantMistings = builder.define("lerasiumAlloysGrantMistings", false);
        atiumRarity = builder.defineInRange("atiumRarity", 0.01D, 0D, 1D);
        lerasiumRarity = builder.defineInRange("lerasiumRarity", 0.001D, 0D, 1D);
        builder.pop();

        builder.push("allomancy");
        maxPushPullRange = builder.defineInRange("maxPushPullRange", 18D, 2D, 96D);
        maxPushPullForce = builder.defineInRange("maxPushPullForce", 1.35D, 0.05D, 12D);
        pushPullStrength = builder.defineInRange("pushPullStrength", 1.0D, 0.05D, 10D);
        allowBlockMovement = builder.comment("Block movement is intentionally disabled by default to avoid griefing.").define("allowBlockMovement", false);
        timeBubbleRadius = builder.defineInRange("timeBubbleRadius", 7.5D, 1D, 32D);
        timeBubbleDuration = builder.defineInRange("timeBubbleDuration", 160, 20, 20 * 60 * 5);
        maxActiveBubblesPerPlayer = builder.defineInRange("maxActiveBubblesPerPlayer", 1, 0, 8);
        maxActiveBubblesServer = builder.defineInRange("maxActiveBubblesServer", 16, 0, 128);
        for (Metal metal : Metal.cachedValues()) {
            if (!metal.isAllomantic()) {
                continue;
            }
            builder.push(metal.id());
            metalEnabled.put(metal, builder.define("enabled", true));
            reserveCapacities.put(metal, builder.defineInRange("reserveCapacity", defaultReserveCapacity(metal), 0D, 10000D));
            burnRates.put(metal, builder.defineInRange("burnRatePerTick", defaultBurnRate(metal), 0D, 100D));
            vialValues.put(metal, builder.defineInRange("vialValue", defaultVialValue(metal), 0D, 10000D));
            powerStrengths.put(metal, builder.defineInRange("powerStrength", defaultPowerStrength(metal), 0D, 100D));
            builder.pop();
        }
        builder.pop();

        builder.push("feruchemy");
        feruchemyEnabled = builder.define("enabled", true);
        metalmindCapacity = builder.defineInRange("metalmindCapacity", 1000D, 1D, 100000D);
        feruchemyStoreRate = builder.defineInRange("storeRatePerTick", 1.0D, 0.01D, 100D);
        feruchemyTapRate = builder.defineInRange("tapRatePerTick", 1.0D, 0.01D, 100D);
        unkeyedMetalmindsEnabled = builder.define("unkeyedMetalmindsEnabled", true);
        builder.pop();

        builder.push("hemalurgy");
        hemalurgyEnabled = builder.define("hemalurgyEnabled", true);
        hemalurgyMobStealing = builder.define("allowMobPowerStealing", true);
        spikeDecayRate = builder.defineInRange("spikeDecayRate", 0.02D, 0D, 100D);
        maxSpikesBeforeCorruption = builder.defineInRange("maxSpikesBeforeCorruption", 3, 0, 32);
        maxInstalledSpikes = builder.defineInRange("maxInstalledSpikes", 32, 0, 64);
        spikeRemovalPossible = builder.define("spikeRemovalPossible", true);
        removedSpikesRetainCharge = builder.define("removedSpikesRetainCharge", false);
        builder.pop();

        builder.push("soul_stability");
        soulStabilityBaseMax = builder.comment("Maximum soul stability before any modifiers").defineInRange("soulStabilityBaseMax", 100.0D, 10D, 200D);
        linchpinStabilityBonus = builder.comment("Soul stability bonus from having a linchpin spike").defineInRange("linchpinStabilityBonus", 35.0D, 0D, 100D);
        stabilityLossPerSpike = builder.comment("Soul stability lost per installed spike").defineInRange("stabilityLossPerSpike", 6.0D, 1D, 25D);
        stabilityLossPerDuplicate = builder.comment("Extra soul stability lost per duplicate power spike").defineInRange("stabilityLossPerDuplicate", 4.0D, 0D, 15D);
        builder.pop();

        builder.push("identity_contamination");
        contaminationPerSpike = builder.comment("Identity contamination gained per spike").defineInRange("contaminationPerSpike", 5.0D, 1D, 20D);
        contaminationDecayRate = builder.comment("Identity contamination passive decay per tick when resting").defineInRange("contaminationDecayRate", 0.001D, 0D, 0.1D);
        builder.pop();

        builder.push("savantism");
        savantGainPerBurnTick = builder.comment("Savantism progress gained per tick of normal burning").defineInRange("savantGainPerBurnTick", 0.00002D, 0D, 0.001D);
        savantGainPerFlareTick = builder.comment("Savantism progress gained per tick of flaring").defineInRange("savantGainPerFlareTick", 0.00008D, 0D, 0.005D);
        savantGainPerSpikeStack = builder.comment("Savantism progress multiplier per duplicate spike").defineInRange("savantGainPerSpikeStack", 0.15D, 0D, 1.0D);
        savantDecayRate = builder.comment("Savantism progress decay per tick when not burning").defineInRange("savantDecayRate", 0.000005D, 0D, 0.001D);
        builder.pop();

        builder.push("spiritual_bloat");
        bloatPerForcedSystem = builder.comment("Spiritual bloat gained per forced system acquisition").defineInRange("bloatPerForcedSystem", 12.0D, 1D, 50D);
        builder.pop();

        builder.push("stacking");
        maxDuplicateSpikesPerPower = builder.comment("Maximum duplicate spikes for a single power").defineInRange("maxDuplicateSpikesPerPower", 6, 1, 16);
        builder.pop();

        builder.push("spike_decay");
        spikeDecayRateOutside = builder.comment("Spike charge decay per tick when outside a body (in world/inventory)").defineInRange("spikeDecayRateOutside", 0.0005D, 0D, 0.01D);
        spikeDecayRateBlood = builder.comment("Spike charge decay per tick when stored in blood").defineInRange("spikeDecayRateBlood", 0.0001D, 0D, 0.01D);
        spikeDecayRateAluminum = builder.comment("Spike charge decay per tick when sealed in aluminum (0 = paused)").defineInRange("spikeDecayRateAluminum", 0.0D, 0D, 0.01D);
        instantTransferWindow = builder.comment("Ticks after spike creation for instant heart-to-heart transfer bonus").defineInRange("instantTransferWindow", 60, 0, 200);
        instantTransferRetention = builder.comment("Strength retained during instant transfer").defineInRange("instantTransferRetention", 0.98D, 0D, 1.0D);
        builder.pop();

        builder.push("worldgen");
        builder.define("zincOre", true);
        builder.define("tinOre", true);
        builder.define("aluminumOre", true);
        builder.define("chromiumOre", true);
        builder.define("cadmiumOre", true);
        builder.define("atiumGeodes", true);
        builder.define("lerasiumCaches", true);
        builder.define("ashDeposits", true);
        builder.pop();

        builder.push("structures");
        structureSpawnRate = builder.defineInRange("structureSpawnRate", 1.0D, 0D, 10D);
        builder.define("abandonedKeeps", true);
        builder.define("undergroundHideouts", true);
        builder.define("canalRuins", true);
        builder.define("ancientMetalVaults", true);
        builder.define("atiumCaverns", true);
        builder.define("lerasiumShrineRooms", true);
        builder.pop();

        builder.push("kredik_shaw");
        kredikShawEnabled = builder.define("kredikShawEnabled", true);
        kredikShawSpacing = builder.defineInRange("kredikShawSpacing", 48, 16, 4096);
        kredikShawSeparation = builder.defineInRange("kredikShawSeparation", 16, 8, 2048);
        kredikShawSalt = builder.defineInRange("kredikShawSalt", 610527, 1, Integer.MAX_VALUE);
        kredikShawLootMultiplier = builder.defineInRange("kredikShawLootMultiplier", 1.0D, 0D, 16D);
        kredikShawMobDensity = builder.defineInRange("kredikShawMobDensity", 1.0D, 0D, 16D);
        kredikShawHasWell = builder.define("kredikShawHasWell", true);
        kredikShawRequiresKeyForWell = builder.define("kredikShawRequiresKeyForWell", false);
        builder.pop();

        builder.push("well_of_ascension");
        wellEnabled = builder.define("wellEnabled", true);
        wellRequiresKredikShaw = builder.define("wellRequiresKredikShaw", true);
        wellCanGrantLerasium = builder.define("wellCanGrantLerasium", true);
        wellCanGrantMistborn = builder.define("wellCanGrantMistborn", false);
        wellEventCooldown = builder.defineInRange("wellEventCooldownTicks", 20 * 60 * 20, 0, 20 * 60 * 60 * 24);
        wellOneUsePerPlayer = builder.define("wellOneUsePerPlayer", true);
        wellOneUsePerWorld = builder.define("wellOneUsePerWorld", false);
        wellSummonsBoss = builder.define("wellSummonsBoss", false);
        wellBronzePulseRange = builder.defineInRange("wellBronzePulseRange", 96D, 8D, 512D);
        builder.pop();

        builder.push("mobs");
        mobSpawnRate = builder.defineInRange("mobSpawnRate", 0.35D, 0D, 10D);
        builder.define("coinshotBandits", true);
        builder.define("lurcherGuards", true);
        builder.define("pewterThugs", true);
        builder.define("tineyeScouts", true);
        builder.define("rioters", true);
        builder.define("soothers", true);
        builder.define("seekers", true);
        builder.define("smokers", true);
        builder.define("atiumSeers", true);
        builder.define("mistbornAssassins", true);
        builder.define("koloss", true);
        builder.define("kandra", true);
        builder.define("steelInquisitorBoss", true);
        builder.pop();
    }

    public static boolean isMetalEnabled(Metal metal) {
        ForgeConfigSpec.BooleanValue value = VALUES.metalEnabled.get(metal);
        return value == null || value.get();
    }

    public static float reserveCapacity(Metal metal) {
        ForgeConfigSpec.DoubleValue value = VALUES.reserveCapacities.get(metal);
        return value == null ? 0F : value.get().floatValue();
    }

    public static float burnRate(Metal metal) {
        ForgeConfigSpec.DoubleValue value = VALUES.burnRates.get(metal);
        return value == null ? 0F : value.get().floatValue();
    }

    public static float vialValue(Metal metal) {
        ForgeConfigSpec.DoubleValue value = VALUES.vialValues.get(metal);
        return value == null ? 0F : value.get().floatValue();
    }

    public static float powerStrength(Metal metal) {
        ForgeConfigSpec.DoubleValue value = VALUES.powerStrengths.get(metal);
        return value == null ? 1F : value.get().floatValue();
    }

    private static double defaultReserveCapacity(Metal metal) {
        return switch (metal) {
            case ATIUM -> 100D;
            case DURALUMIN, ALUMINUM -> 80D;
            case PEWTER, STEEL, IRON, BENDALLOY, CADMIUM -> 160D;
            default -> 120D;
        };
    }

    private static double defaultBurnRate(Metal metal) {
        return switch (metal) {
            case ATIUM -> 0.25D;
            case PEWTER -> 0.08D;
            case STEEL, IRON -> 0.055D;
            case TIN, COPPER, BRONZE -> 0.04D;
            case BENDALLOY, CADMIUM -> 0.08D;
            case ALUMINUM -> 0D;
            case DURALUMIN -> 0.2D;
            default -> 0.06D;
        };
    }

    private static double defaultVialValue(Metal metal) {
        return switch (metal) {
            case ATIUM -> 20D;
            case DURALUMIN, ALUMINUM -> 40D;
            default -> 80D;
        };
    }

    private static double defaultPowerStrength(Metal metal) {
        return switch (metal) {
            case PEWTER -> 1.15D;
            case STEEL, IRON -> 1.0D;
            case ATIUM -> 1.35D;
            default -> 1.0D;
        };
    }
}
