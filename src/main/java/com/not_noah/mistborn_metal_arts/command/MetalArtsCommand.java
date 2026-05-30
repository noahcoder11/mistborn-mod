package com.not_noah.mistborn_metal_arts.command;

import com.not_noah.mistborn_metal_arts.allomancy.AllomancyManager;
import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.entity.MetalbornEnemy;
import com.not_noah.mistborn_metal_arts.entity.MetalbornRole;
import com.not_noah.mistborn_metal_arts.network.MetalAction;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import com.not_noah.mistborn_metal_arts.registry.ModEntityTypes;
import com.not_noah.mistborn_metal_arts.registry.ModItems;
import com.not_noah.mistborn_metal_arts.worldgen.KredikShawBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

public final class MetalArtsCommand {
    private static final ResourceKey<Registry<Structure>> STRUCTURE_REGISTRY = ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "worldgen/structure"));
    private static TagKey<Structure> KREDIK_SHAW_STRUCTURES;

    private static TagKey<Structure> getKredikTag() {
        if (KREDIK_SHAW_STRUCTURES == null) {
            KREDIK_SHAW_STRUCTURES = TagKey.create(STRUCTURE_REGISTRY, new ResourceLocation("mistborn_metal_arts", "kredik_shaw"));
        }
        return KREDIK_SHAW_STRUCTURES;
    }

    private MetalArtsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("metalarts")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("power")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> powerGet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("setallomancy")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(allomancyChoices(), builder))
                                                .executes(ctx -> setAllomancy(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"))))))
                        .then(Commands.literal("setferuchemy")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(feruchemyChoices(), builder))
                                                .executes(ctx -> setFeruchemy(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"))))))
                        .then(Commands.literal("setfullborn")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> setFullborn(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("snap")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                                .executes(ctx -> setSnap(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value")))))))
                .then(Commands.literal("reserve")
                        .then(Commands.literal("fill")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(metalChoices(), builder))
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0D))
                                                        .executes(ctx -> reserveFill(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"), DoubleArgumentType.getDouble(ctx, "amount")))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> reserveClear(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("burn")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(allomancyChoices(), builder))
                                        .executes(ctx -> burn(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"))))))
                .then(Commands.literal("stopburning")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Stream.concat(Stream.of("all"), metalChoices()), builder))
                                        .executes(ctx -> stopBurning(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"))))))
                .then(Commands.literal("metalmind")
                        .then(Commands.literal("fill")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(feruchemyChoices(), builder))
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0D))
                                                        .executes(ctx -> metalmindFill(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"), DoubleArgumentType.getDouble(ctx, "amount"))))))))
                .then(Commands.literal("corruption")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> corruptionGet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> corruptionSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("godmetal")
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Stream.of("atium", "lerasium"), builder))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> giveGodMetal(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"), IntegerArgumentType.getInteger(ctx, "amount"))))))))
                .then(Commands.literal("hemalurgy")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> hemalurgyGet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("addspike")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(metalChoices(), builder))
                                                .then(Commands.argument("power", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(metalChoices(), builder))
                                                        .executes(ctx -> hemalurgyAddSpike(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"), StringArgumentType.getString(ctx, "power"), "allomancy"))))))
                        .then(Commands.literal("removespike")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .executes(ctx -> hemalurgyRemoveSpike(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "slot"))))))
                        .then(Commands.literal("chargespike")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(metalChoices(), builder))
                                                .then(Commands.argument("power", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(metalChoices(), builder))
                                                        .executes(ctx -> hemalurgyChargeSpike(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"), StringArgumentType.getString(ctx, "power"), "allomancy"))))))
                        .then(Commands.literal("corruption")
                                .then(Commands.literal("get")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> corruptionGet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> corruptionSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(ctx -> corruptionAdd(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("bronze").executes(ctx -> debug(ctx.getSource(), "Bronze debug: active seeker pulse data is shown on each player's HUD.")))
                        .then(Commands.literal("anchors").executes(ctx -> debug(ctx.getSource(), "Anchor debug: tagged metallic blocks are valid Iron/Steel anchors.")))
                        .then(Commands.literal("bubbles").executes(ctx -> debug(ctx.getSource(), "Bubble debug: first pass uses stable status-effect bubbles, no global tick-rate changes.")))
                        .then(Commands.literal("capability")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> debugCapability(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("setstability")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0D, 100D))
                                                .executes(ctx -> setStability(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "value"))))))
                        .then(Commands.literal("setcontamination")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0D, 100D))
                                                .executes(ctx -> setContamination(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "value"))))))
                        .then(Commands.literal("setsavant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("metal", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(allomancyChoices(), builder))
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0D, 4.0D))
                                                        .executes(ctx -> setSavant(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "metal"), DoubleArgumentType.getDouble(ctx, "value")))))))
                        .then(Commands.literal("setbloat")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0D, 100D))
                                                .executes(ctx -> setBloat(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), DoubleArgumentType.getDouble(ctx, "value"))))))
                        .then(Commands.literal("place_kredik_shaw")
                                .executes(ctx -> placeKredikShaw(ctx.getSource())))
                        .then(Commands.literal("locate_kredik_shaw")
                                .executes(ctx -> locateKredikShaw(ctx.getSource())))
                        .then(Commands.literal("spawn_metalborn")
                                .then(Commands.argument("role", StringArgumentType.word()).suggests((ctx, builder) -> SharedSuggestionProvider.suggest(metalbornChoices(), builder))
                                        .executes(ctx -> spawnMetalborn(ctx.getSource(), StringArgumentType.getString(ctx, "role")))))));
    }

    private static int powerGet(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> source.sendSuccess(() -> Component.literal(player.getGameProfile().getName()
                + " Allomancy=" + data.allomanticPowers()
                + " Feruchemy=" + data.feruchemicalPowers()
                + " Burning=" + data.burningMetals()
                + " Corruption=" + data.totalCorruption()), false));
        return 1;
    }

    private static int setAllomancy(CommandSourceStack source, ServerPlayer player, String choice) {
        String normalized = choice.toLowerCase(Locale.ROOT);
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            if ("mistborn".equals(normalized)) {
                data.setMistborn();
                data.setAllomancySnapped(false);
            } else if ("none".equals(normalized)) {
                data.clearAllomancy();
                data.setAllomancySnapped(true);
            } else {
                Metal.byName(normalized).ifPresent(metal -> {
                    data.setMisting(metal);
                    data.setAllomancySnapped(false);
                });
            }
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Updated Allomancy for " + player.getGameProfile().getName() + " (dormant until snapping)"), true);
        return 1;
    }

    private static int setSnap(CommandSourceStack source, ServerPlayer player, boolean value) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setAllomancySnapped(value);
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Set snapping status to " + value + " for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int setFeruchemy(CommandSourceStack source, ServerPlayer player, String choice) {
        String normalized = choice.toLowerCase(Locale.ROOT);
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            if ("full".equals(normalized)) {
                data.setFullFeruchemist();
            } else if ("none".equals(normalized)) {
                data.clearFeruchemy();
            } else {
                Metal.byName(normalized).ifPresent(data::setFerring);
            }
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Updated Feruchemy for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int setFullborn(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setFullborn();
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + " is now Fullborn."), true);
        return 1;
    }

    private static int reserveFill(CommandSourceStack source, ServerPlayer player, String metalName, double amount) {
        Metal.byName(metalName).ifPresent(metal -> player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.fillReserve(metal, (float) amount);
            MetalArtsNetwork.sync(player);
        }));
        source.sendSuccess(() -> Component.literal("Filled reserve."), true);
        return 1;
    }

    private static int reserveClear(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.clearReserves();
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Cleared reserves."), true);
        return 1;
    }

    private static int burn(CommandSourceStack source, ServerPlayer player, String metalName) {
        Metal.byName(metalName).ifPresent(metal -> AllomancyManager.handleAction(player, MetalAction.START_BURN, metal));
        source.sendSuccess(() -> Component.literal("Burn command sent."), true);
        return 1;
    }

    private static int stopBurning(CommandSourceStack source, ServerPlayer player, String metalName) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            if ("all".equalsIgnoreCase(metalName)) {
                data.stopAllBurning();
            } else {
                Metal.byName(metalName).ifPresent(data::stopBurning);
            }
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Updated burning metals."), true);
        return 1;
    }

    private static int metalmindFill(CommandSourceStack source, ServerPlayer player, String metalName, double amount) {
        Metal.byName(metalName).ifPresent(metal -> player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setMetalmindCharge(metal, (float) amount);
            MetalArtsNetwork.sync(player);
        }));
        source.sendSuccess(() -> Component.literal("Filled metalmind data slot."), true);
        return 1;
    }

    private static int corruptionGet(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> source.sendSuccess(() -> Component.literal(player.getGameProfile().getName()
                + " corruption=" + data.corruption()
                + " equipped=" + data.equippedSpikeCorruption()
                + " total=" + data.totalCorruption()), false));
        return 1;
    }

    private static int corruptionSet(CommandSourceStack source, ServerPlayer player, int amount) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setCorruption(amount);
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Set corruption."), true);
        return 1;
    }

    private static int corruptionAdd(CommandSourceStack source, ServerPlayer player, int amount) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setCorruption(data.corruption() + amount);
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Adjusted corruption."), true);
        return 1;
    }

    private static int giveGodMetal(CommandSourceStack source, ServerPlayer player, String metalName, int amount) {
        Metal metal = "lerasium".equalsIgnoreCase(metalName) ? Metal.LERASIUM : Metal.ATIUM;
        player.getInventory().add(new ItemStack(ModItems.METAL_BEADS.get(metal).get(), amount));
        source.sendSuccess(() -> Component.literal("Gave " + amount + " " + metal.displayName() + " bead(s)."), true);
        return 1;
    }

    private static int hemalurgyGet(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> source.sendSuccess(() -> Component.literal(player.getGameProfile().getName()
                + " corruption=" + data.corruption()
                + " equipped=" + data.equippedSpikeCorruption()
                + " spikes=" + data.installedSpikes()), false));
        return 1;
    }

    private static int hemalurgyAddSpike(CommandSourceStack source, ServerPlayer player, String spikeMetalName, String powerMetalName, String powerType) {
        Metal.byName(spikeMetalName).ifPresent(spikeMetal -> Metal.byName(powerMetalName).ifPresent(powerMetal -> player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.installSpike(spikeMetal, powerType, powerMetal, 1.0F);
            MetalArtsNetwork.sync(player);
        })));
        source.sendSuccess(() -> Component.literal("Installed spike through command."), true);
        return 1;
    }

    private static int hemalurgyRemoveSpike(CommandSourceStack source, ServerPlayer player, int slot) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.removeSpike(slot);
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Removed spike slot " + slot + "."), true);
        return 1;
    }

    private static int hemalurgyChargeSpike(CommandSourceStack source, ServerPlayer player, String spikeMetalName, String powerMetalName, String powerType) {
        Metal spikeMetal = Metal.byName(spikeMetalName).orElse(Metal.IRON);
        Metal powerMetal = Metal.byName(powerMetalName).orElse(spikeMetal);
        ItemStack stack = new ItemStack(ModItems.CHARGED_SPIKES.get(spikeMetal).get());
        stack.getOrCreateTag().putString("PowerType", powerType);
        stack.getOrCreateTag().putString("PowerMetal", powerMetal.id());
        stack.getOrCreateTag().putFloat("Strength", 1.0F);
        player.getInventory().add(stack);
        source.sendSuccess(() -> Component.literal("Gave charged " + spikeMetal.displayName() + " spike for " + powerMetal.displayName() + "."), true);
        return 1;
    }

    private static int debugCapability(CommandSourceStack source, ServerPlayer player) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> source.sendSuccess(() -> Component.literal(data.serializeNBT().toString()), false));
        return 1;
    }

    private static int placeKredikShaw(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            KredikShawBuilder.place(player);
            return 1;
        }
        source.sendFailure(Component.literal("This debug placement command must be run by a player."));
        return 0;
    }

    private static int locateKredikShaw(CommandSourceStack source) {
        BlockPos origin = BlockPos.containing(source.getPosition());
        BlockPos located = source.getLevel().findNearestMapStructure(getKredikTag(), origin, 128, false);
        if (located != null) {
            source.sendSuccess(() -> Component.literal("Nearest registered Kredik Shaw is near "
                    + located.getX() + ", " + located.getY() + ", " + located.getZ()
                    + ". Vanilla /locate should also work as /locate structure mistborn_metal_arts:kredik_shaw."), false);
            return 1;
        }

        source.sendFailure(Component.literal("No registered Kredik Shaw found nearby. Try vanilla /locate structure mistborn_metal_arts:kredik_shaw after generating/reloading the world."));
        return 0;
    }

    private static int spawnMetalborn(CommandSourceStack source, String roleName) {
        MetalbornRole role = Arrays.stream(MetalbornRole.cachedValues())
                .filter(value -> value.id().equalsIgnoreCase(roleName))
                .findFirst()
                .orElse(MetalbornRole.COINSHOT_BANDIT);
        var type = ModEntityTypes.METALBORN.get(role);
        if (type == null) {
            source.sendFailure(Component.literal("Unknown Metalborn role: " + roleName));
            return 0;
        }
        MetalbornEnemy enemy = type.get().create(source.getLevel());
        if (enemy == null) {
            source.sendFailure(Component.literal("Could not create Metalborn entity."));
            return 0;
        }
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(source.getPosition());
        enemy.moveTo(source.getPosition().x, source.getPosition().y, source.getPosition().z, source.getRotation().y, 0F);
        enemy.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(pos), MobSpawnType.COMMAND, null, null);
        source.getLevel().addFreshEntity(enemy);
        source.sendSuccess(() -> Component.literal("Spawned " + role.displayName() + "."), true);
        return 1;
    }

    private static int debug(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int setStability(CommandSourceStack source, ServerPlayer player, double value) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setSoulStability((float) value);
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Set Soul Stability of " + player.getGameProfile().getName() + " to " + value), true);
        return 1;
    }

    private static int setContamination(CommandSourceStack source, ServerPlayer player, double value) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setIdentityContamination((float) value);
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Set Identity Contamination of " + player.getGameProfile().getName() + " to " + value), true);
        return 1;
    }

    private static int setSavant(CommandSourceStack source, ServerPlayer player, String metalChoice, double value) {
        Metal.byName(metalChoice).ifPresent(metal -> player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            float progressValue = (float) value;
            if (value > 1.0D) {
                if (value == 2.0D) progressValue = 0.50F;
                else if (value == 3.0D) progressValue = 0.75F;
                else if (value == 4.0D) progressValue = 0.95F;
                else progressValue = 1.0F;
            }
            data.setSavantProgress(metal, progressValue);
            MetalArtsNetwork.sync(player);
        }));
        source.sendSuccess(() -> Component.literal("Set Savant progress for " + metalChoice + " of " + player.getGameProfile().getName() + " to " + value), true);
        return 1;
    }

    private static int setBloat(CommandSourceStack source, ServerPlayer player, double value) {
        player.getCapability(MetalArtsCapabilities.METAL_ARTS).ifPresent(data -> {
            data.setSpiritualBloat((float) value);
            MetalArtsNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.literal("Set Spiritual Bloat of " + player.getGameProfile().getName() + " to " + value), true);
        return 1;
    }

    private static Stream<String> metalChoices() {
        return Arrays.stream(Metal.cachedValues()).map(Metal::id);
    }

    private static Stream<String> allomancyChoices() {
        return Stream.concat(Stream.of("mistborn", "none"), Arrays.stream(Metal.cachedValues()).filter(Metal::isAllomantic).map(Metal::id));
    }

    private static Stream<String> feruchemyChoices() {
        return Stream.concat(Stream.of("full", "none"), Arrays.stream(Metal.cachedValues()).filter(Metal::isFeruchemical).map(Metal::id));
    }

    private static java.util.stream.Stream<String> metalbornChoices() {
        return java.util.Arrays.stream(MetalbornRole.cachedValues()).map(MetalbornRole::id);
    }
}
