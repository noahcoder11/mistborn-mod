package com.not_noah.mistborn_metal_arts.feruchemy;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsCapabilities;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import com.not_noah.mistborn_metal_arts.item.MetalmindItem;
import com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem;
import net.minecraft.nbt.CompoundTag;
import com.not_noah.mistborn_metal_arts.network.MetalArtsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import java.util.UUID;

public final class FeruchemyManager {
    // Unique UUID constants for player attribute modifiers
    private static final UUID IRON_KB_MODIFIER_UUID = UUID.fromString("6d557ba1-7443-4b95-8854-8e3ff7cfdb6e");
    private static final UUID IRON_GRAVITY_MODIFIER_UUID = UUID.fromString("f48483b6-2007-4226-ae8f-8d26786c2a41");
    private static final UUID STEEL_SPEED_MODIFIER_UUID = UUID.fromString("0a3597c5-559d-4780-b2bc-618d4586db70");
    private static final UUID STEEL_ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("3d5483db-ca2e-4b47-ab87-0b1ad0153bb1");
    private static final UUID PEWTER_DAMAGE_MODIFIER_UUID = UUID.fromString("e574c8b8-2a12-4ee4-90a6-80be4063debb");
    private static final UUID GOLD_HEALTH_MODIFIER_UUID = UUID.fromString("52778da5-ccdf-4f4c-8ea5-0e70ab2fdb0b");
    private static final UUID CHROMIUM_LUCK_MODIFIER_UUID = UUID.fromString("490fffa1-a67b-402a-9e19-5d27b9db1a4d");

    private FeruchemyManager() {
    }

    public static void tick(ServerPlayer player, MetalArtsData data) {
        if (!ServerConfig.VALUES.feruchemyEnabled.get()) {
            return;
        }
        boolean changed = tick((LivingEntity) player, data);
        if (changed && player.tickCount % 5 == 0) {
            MetalArtsNetwork.sync(player, data.serializeReservesNBT());
        }
    }

    public static boolean tick(LivingEntity entity, MetalArtsData data) {
        if (!ServerConfig.VALUES.feruchemyEnabled.get()) {
            return false;
        }

        java.util.List<Metal> activeMetals = new java.util.ArrayList<>();
        for (Metal metal : Metal.cachedValues()) {
            if (metal.isFeruchemical() && data.feruchemyMode(metal) != 0) {
                activeMetals.add(metal);
            }
        }

        // Active cleanup for non-active metals
        for (Metal m : Metal.cachedValues()) {
            if (m.isFeruchemical()) {
                int mode = activeMetals.contains(m) ? data.feruchemyMode(m) : 0;
                cleanupModifiersAndEffects(entity, m, mode);
            }
        }

        if (activeMetals.isEmpty()) {
            return false;
        }

        boolean changed = false;

        for (Metal metal : activeMetals) {
            int mode = data.feruchemyMode(metal);
            
            if (entity instanceof ServerPlayer player) {
                java.util.List<IMetalSource> sources = findMetalSources(player, data, metal);
                
                if (sources.isEmpty()) {
                    data.stopFeruchemy(metal);
                    cleanupModifiersAndEffects(player, metal, 0);
                    changed = true;
                    continue;
                }

                sources.removeIf(source -> {
                    if (source instanceof ItemStackSource itemSource) {
                        ItemStack stack = itemSource.getStack();
                        if (stack.getItem() instanceof MetalmindItem) {
                            return !canUse(player, data, stack, metal);
                        }
                    }
                    return false;
                });

                if (sources.isEmpty()) {
                    data.stopFeruchemy(metal);
                    cleanupModifiersAndEffects(player, metal, 0);
                    player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.metalmind_rejects", metal.displayName()), true);
                    changed = true;
                    continue;
                }

                changed |= mode < 0 ? store(player, data, sources, metal) : tap(player, data, sources, metal);
            } else {
                // For mobs, we only tap (mode > 0)
                if (mode > 0) {
                    float charge = data.getMetalmindCharge(metal);
                    if (charge <= 0.0F) {
                        data.stopFeruchemy(metal);
                        cleanupModifiersAndEffects(entity, metal, 0);
                        changed = true;
                        continue;
                    }
                    float baseRate = ServerConfig.VALUES.feruchemyTapRate.get().floatValue();
                    float drainMultiplier = (float) (mode * Math.pow(1.5D, mode - 1));
                    float rate = baseRate * drainMultiplier;
                    float toDrain = rate;
                    float drained = Math.min(toDrain, charge);
                    
                    data.setMetalmindCharge(metal, charge - drained);
                    applyTapBenefit(entity, data, null, metal, (float) mode);
                    changed = true;
                } else {
                    data.stopFeruchemy(metal);
                    cleanupModifiersAndEffects(entity, metal, 0);
                    changed = true;
                }
            }
        }

        return changed;
    }

    public static void cleanupModifiersAndEffects(LivingEntity player, Metal metal, int mode) {
        switch (metal) {
            case IRON -> {
                AttributeInstance kbInstance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kbInstance != null) {
                    kbInstance.removeModifier(IRON_KB_MODIFIER_UUID);
                }
                AttributeInstance gravityInstance = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                if (gravityInstance != null) {
                    gravityInstance.removeModifier(IRON_GRAVITY_MODIFIER_UUID);
                }
            }
            case STEEL -> {
                AttributeInstance speedInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedInstance != null) {
                    speedInstance.removeModifier(STEEL_SPEED_MODIFIER_UUID);
                }
                AttributeInstance attackSpeedInstance = player.getAttribute(Attributes.ATTACK_SPEED);
                if (attackSpeedInstance != null) {
                    attackSpeedInstance.removeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID);
                }
            }
            case PEWTER -> {
                AttributeInstance damageInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (damageInstance != null) {
                    damageInstance.removeModifier(PEWTER_DAMAGE_MODIFIER_UUID);
                }
            }
            case GOLD -> {
                if (mode >= 0) { // Safely restore max health if not storing
                    AttributeInstance healthInstance = player.getAttribute(Attributes.MAX_HEALTH);
                    if (healthInstance != null && healthInstance.getModifier(GOLD_HEALTH_MODIFIER_UUID) != null) {
                        double healthPct = player.getHealth() / player.getMaxHealth();
                        healthInstance.removeModifier(GOLD_HEALTH_MODIFIER_UUID);
                        player.setHealth((float) (player.getMaxHealth() * healthPct));
                    }
                }
            }
            case CHROMIUM -> {
                AttributeInstance luckInstance = player.getAttribute(Attributes.LUCK);
                if (luckInstance != null) {
                    luckInstance.removeModifier(CHROMIUM_LUCK_MODIFIER_UUID);
                }
            }
            default -> {}
        }
    }

    public interface IMetalSource {
        Metal metal();
        float getCharge();
        void setCharge(float amount);
        float getCapacity();
    }

    public static class InstalledSpikeSource implements IMetalSource {
        private final MetalArtsData data;
        private final int index;
        private final MetalArtsData.InstalledSpike spike;

        public InstalledSpikeSource(MetalArtsData data, int index, MetalArtsData.InstalledSpike spike) {
            this.data = data;
            this.index = index;
            this.spike = spike;
        }

        @Override
        public Metal metal() {
            return spike.spikeMetal();
        }

        @Override
        public float getCharge() {
            return spike.feruchemicalCharge();
        }

        @Override
        public void setCharge(float amount) {
            data.updateSpikeFeruchemicalCharge(index, amount);
        }

        @Override
        public float getCapacity() {
            return 150.0F;
        }
    }

    public static class ItemStackSource implements IMetalSource {
        private final ItemStack stack;
        private final Metal metal;
        private final float capacity;

        public ItemStackSource(ItemStack stack, Metal metal, float capacity) {
            this.stack = stack;
            this.metal = metal;
            this.capacity = capacity;
        }

        @Override
        public Metal metal() {
            return metal;
        }

        @Override
        public float getCharge() {
            return getChargeFromStack(stack);
        }

        @Override
        public void setCharge(float amount) {
            setChargeToStack(stack, amount, capacity);
        }

        @Override
        public float getCapacity() {
            return capacity;
        }

        public ItemStack getStack() {
            return stack;
        }
    }

    public static float getCapacityForSource(ItemStack stack, Metal metal) {
        if (stack.getItem() instanceof MetalmindItem) {
            return MetalmindItem.getCapacity(stack);
        }
        if (stack.getItem() instanceof HemalurgicSpikeItem spike) {
            return spike.charged() ? 150.0F : 500.0F;
        }
        String regName = stack.getItem().builtInRegistryHolder().key().location().getPath().toLowerCase();
        if (regName.contains("chestplate")) return 4000.0F;
        if (regName.contains("leggings")) return 3000.0F;
        if (regName.contains("helmet")) return 2000.0F;
        if (regName.contains("boots")) return 1500.0F;
        if (regName.contains("block")) return 9000.0F;
        if (regName.contains("ingot")) return 1000.0F;
        if (regName.contains("nugget")) return 110.0F;
        return 500.0F;
    }

    public static float getChargeFromStack(ItemStack stack) {
        if (stack.getItem() instanceof MetalmindItem) {
            return MetalmindItem.getCharge(stack);
        }
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getFloat("FeruchemicalCharge") : 0.0F;
    }

    public static void setChargeToStack(ItemStack stack, float amount, float capacity) {
        if (stack.getItem() instanceof MetalmindItem) {
            MetalmindItem.setCharge(stack, amount);
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putFloat("FeruchemicalCharge", Math.max(0.0F, Math.min(capacity, amount)));
    }

    public static boolean isMadeOfMetal(ItemStack stack, Metal metal) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof MetalmindItem item) {
            return item.metal() == metal;
        }
        if (stack.getItem() instanceof HemalurgicSpikeItem spike) {
            return spike.metal() == metal;
        }
        var resource = stack.getItem().builtInRegistryHolder().key().location();
        String path = resource.getPath().toLowerCase();
        String metalName = metal.id().toLowerCase();
        return path.contains(metalName);
    }

    private static final String[] ALL_CURIOS_SLOTS = {
        "physical_quadrant", "mental_quadrant", "spiritual_quadrant", "temporal_quadrant",
        "head", "necklace", "back", "body", "belt", "ring", "hands", "bracelet", "charm",
        "metalmind_ring", "metalmind_bracer", "metalmind_necklace"
    };

    public static java.util.List<IMetalSource> findMetalSources(Player player, MetalArtsData data, Metal metal) {
        java.util.List<IMetalSource> sources = new java.util.ArrayList<>();
        
        var spikes = data.installedSpikes();
        for (int i = 0; i < spikes.size(); i++) {
            var spike = spikes.get(i);
            if (spike.spikeMetal() == metal) {
                sources.add(new InstalledSpikeSource(data, i, spike));
            }
        }

        if (com.not_noah.mistborn_metal_arts.compat.CuriosCompat.isLoaded()) {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                for (String slotType : ALL_CURIOS_SLOTS) {
                    handler.getStacksHandler(slotType).ifPresent(stacksHandler -> {
                        var stacks = stacksHandler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            ItemStack stack = stacks.getStackInSlot(i);
                            if (isMadeOfMetal(stack, metal)) {
                                sources.add(new ItemStackSource(stack, metal, getCapacityForSource(stack, metal)));
                            }
                        }
                    });
                }
            });
        }

        ItemStack mainHand = player.getMainHandItem();
        if (isMadeOfMetal(mainHand, metal)) {
            sources.add(new ItemStackSource(mainHand, metal, getCapacityForSource(mainHand, metal)));
        }
        ItemStack offHand = player.getOffhandItem();
        if (isMadeOfMetal(offHand, metal)) {
            sources.add(new ItemStackSource(offHand, metal, getCapacityForSource(offHand, metal)));
        }

        for (ItemStack armorStack : player.getArmorSlots()) {
            if (isMadeOfMetal(armorStack, metal)) {
                sources.add(new ItemStackSource(armorStack, metal, getCapacityForSource(armorStack, metal)));
            }
        }

        var inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (stack == mainHand || stack == offHand) continue;
            boolean isArmor = false;
            for (ItemStack armor : player.getArmorSlots()) {
                if (stack == armor) {
                    isArmor = true;
                    break;
                }
            }
            if (isArmor) continue;
            
            if (isMadeOfMetal(stack, metal)) {
                sources.add(new ItemStackSource(stack, metal, getCapacityForSource(stack, metal)));
            }
        }

        return sources;
    }

    public static boolean isCompounding(LivingEntity entity, MetalArtsData data, Metal metal) {
        if (entity == null) return false;
        boolean[] compound = {false};
        entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
            compound[0] = web.canCompound(metal);
        });
        return data.hasAllomanticPower(metal) 
            && data.hasFeruchemicalPower(metal) 
            && data.isBurning(metal) 
            && data.feruchemyMode(metal) < 0
            && compound[0];
    }

    public static float adjustFallDamage(ServerPlayer player, MetalArtsData data, float damageMultiplier) {
        if (data.isStoring(Metal.IRON)) {
            return damageMultiplier * 0.15F; // Light weight reduces fall damage dramatically
        }
        if (data.isTapping(Metal.IRON)) {
            int tapLevel = data.feruchemyMode(Metal.IRON);
            return damageMultiplier * (1.0F + 0.35F * tapLevel); // Heavy weight increases fall damage
        }
        return damageMultiplier;
    }

    public static void handleFallGroundSlam(ServerPlayer player, MetalArtsData data, LivingFallEvent event) {
        if (!data.isTapping(Metal.IRON) || event.getDistance() < 2.5F) {
            return;
        }
        int tapLevel = data.feruchemyMode(Metal.IRON);
        float radius = 1.5F + 0.9F * tapLevel;
        ServerLevel level = player.serverLevel();
        
        // Dynamic explosion/dust particles
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 8 + tapLevel * 4, radius * 0.5D, 0.15D, radius * 0.5D, 0.05D);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F + 0.1F * tapLevel, 0.6F);
        
        // Shockwave damage & knockback
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
            double dist = player.position().distanceTo(target.position());
            float damage = (float) Math.max(1.0F, (radius - dist) * 2.5F * tapLevel);
            target.hurt(level.damageSources().fall(), damage);
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(1.2D * tapLevel / Math.max(1.0D, dist));
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.25D * tapLevel, knockback.z));
        }
        
        // Break weak blocks or crack stone
        BlockPos center = player.blockPosition().below();
        int bRadius = Math.round(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-bRadius, -1, -bRadius), center.offset(bRadius, 1, bRadius))) {
            if (pos.distSqr(center) <= radius * radius) {
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)) {
                    level.destroyBlock(pos, true, player);
                } else if (state.is(Blocks.STONE)) {
                    level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
    }

    private static boolean store(ServerPlayer player, MetalArtsData data, java.util.List<IMetalSource> sources, Metal metal) {
        if (sources.isEmpty()) {
            data.stopFeruchemy(metal);
            cleanupModifiersAndEffects(player, metal, 0);
            return true;
        }

        int mode = data.feruchemyMode(metal);
        int storeLevel = -mode;
        if (storeLevel <= 0) return false;

        float baseRate = ServerConfig.VALUES.feruchemyStoreRate.get().floatValue();
        float rate = baseRate * storeLevel;
        boolean compounding = isCompounding(player, data, metal);
        if (compounding) {
            rate *= 10.0F;
        }

        float toStore = rate;
        boolean storedAny = false;

        for (IMetalSource source : sources) {
            float charge = source.getCharge();
            float capacity = source.getCapacity() * (1.0F + 0.15F * data.getLerasatiumAlloyBonus(metal));
            if (charge >= capacity) {
                continue;
            }
            float room = capacity - charge;
            float added = Math.min(toStore, room);

            boolean storedInThis = false;
            if (metal == Metal.COPPER) {
                storedInThis = storeCopper(player, source, added);
            } else if (metal == Metal.BENDALLOY) {
                storedInThis = storeBendalloy(player, source, added);
            } else {
                source.setCharge(charge + added);
                storedInThis = true;
            }

            if (storedInThis) {
                storedAny = true;
                toStore -= added;
                if (toStore <= 0) {
                    break;
                }
            }
        }

        if (storedAny) {
            if (!sources.isEmpty()) {
                data.setMetalmindCharge(metal, sources.get(0).getCharge());
            }
            if (compounding && player.tickCount % 5 == 0) {
                ServerLevel level = player.serverLevel();
                double x = player.getX();
                double y = player.getY() + player.getBbHeight() * 0.5;
                double z = player.getZ();
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 5, 0.5, 0.5, 0.5, 0.05);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW, x, y, z, 5, 0.5, 0.5, 0.5, 0.05);
            }
            applyStorePenalty(player, data, metal, storeLevel);
        } else {
            data.stopFeruchemy(metal);
            cleanupModifiersAndEffects(player, metal, 0);
            player.displayClientMessage(Component.translatable("message.mistborn_metal_arts.metalminds_full", metal.displayName()), true);
        }

        return storedAny;
    }

    private static boolean tap(ServerPlayer player, MetalArtsData data, java.util.List<IMetalSource> sources, Metal metal) {
        if (sources.isEmpty()) {
            data.stopFeruchemy(metal);
            cleanupModifiersAndEffects(player, metal, 0);
            return true;
        }

        int tapLevel = data.feruchemyMode(metal);
        if (tapLevel <= 0) return false;

        float baseRate = ServerConfig.VALUES.feruchemyTapRate.get().floatValue();
        
        float drainMultiplier = (float) (tapLevel * Math.pow(1.5D, tapLevel - 1));
        float rate = baseRate * drainMultiplier;

        float toDrain = rate / (1.0F + 0.15F * data.getLerasatiumAlloyBonus(metal));
        float remainingToDrain = toDrain;
        boolean tappedAny = false;

        for (IMetalSource source : sources) {
            float charge = source.getCharge();
            if (charge <= 0.0F) {
                continue;
            }
            float drained = Math.min(remainingToDrain, charge);

            boolean tappedInThis = false;
            if (metal == Metal.COPPER) {
                tappedInThis = tapCopper(player, source, drained);
            } else if (metal == Metal.BENDALLOY) {
                tappedInThis = tapBendalloy(player, source, drained);
            } else {
                source.setCharge(charge - drained);
                tappedInThis = true;
            }

            if (tappedInThis) {
                tappedAny = true;
                remainingToDrain -= drained;
                if (remainingToDrain <= 0.0F) {
                    break;
                }
            }
        }

        if (tappedAny) {
            if (!sources.isEmpty()) {
                data.setMetalmindCharge(metal, sources.get(0).getCharge());
            }
            float effectiveTap = (float) tapLevel;
            applyTapBenefit(player, data, null, metal, effectiveTap);
        } else {
            data.stopFeruchemy(metal);
            data.setMetalmindCharge(metal, 0F);
            cleanupModifiersAndEffects(player, metal, 0);
        }

        return tappedAny;
    }

    private static boolean storeCopper(ServerPlayer player, IMetalSource source, float rate) {
        int currentXP = getPlayerXP(player);
        if (currentXP <= 0) {
            return false;
        }
        int amountToStore = Math.min(currentXP, Math.round(rate * 12));
        if (amountToStore <= 0) amountToStore = 1;
        
        setPlayerXP(player, currentXP - amountToStore);
        source.setCharge(source.getCharge() + amountToStore);
        return true;
    }

    private static boolean tapCopper(ServerPlayer player, IMetalSource source, float rate) {
        float currentCharge = source.getCharge();
        if (currentCharge <= 0F) {
            return false;
        }
        int amountToTap = Math.min(Math.round(rate * 12), Math.round(currentCharge));
        if (amountToTap <= 0) amountToTap = 1;
        
        setPlayerXP(player, getPlayerXP(player) + amountToTap);
        source.setCharge(currentCharge - amountToTap);
        return true;
    }

    private static boolean storeBendalloy(ServerPlayer player, IMetalSource source, float rate) {
        if (player.getFoodData().getFoodLevel() <= 1 && player.getFoodData().getSaturationLevel() <= 0F) {
            return false;
        }
        if (player.tickCount % 20 == 0 && player.getFoodData().getFoodLevel() > 1) {
            player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
        }
        player.causeFoodExhaustion(0.2F);
        source.setCharge(source.getCharge() + rate);
        return true;
    }

    private static boolean tapBendalloy(ServerPlayer player, IMetalSource source, float rate) {
        if (player.tickCount % 10 != 0) {
            return false;
        }
        if (player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(Math.min(20, player.getFoodData().getFoodLevel() + 1));
            source.setCharge(source.getCharge() - Math.max(1F, rate));
            return true;
        }
        return false;
    }

    private static void applyStorePenalty(ServerPlayer player, MetalArtsData data, Metal metal, int storeLevel) {
        if (isCompounding(player, data, metal)) {
            cleanupModifiersAndEffects(player, metal, 1);
            return;
        }
        switch (metal) {
            case IRON -> {
                AttributeInstance kbInstance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kbInstance != null) {
                    kbInstance.removeModifier(IRON_KB_MODIFIER_UUID);
                    kbInstance.addTransientModifier(new AttributeModifier(IRON_KB_MODIFIER_UUID, "Iron Feruchemy Weight Storing", -0.1D * storeLevel, AttributeModifier.Operation.ADDITION));
                }
                AttributeInstance gravityInstance = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                if (gravityInstance != null) {
                    gravityInstance.removeModifier(IRON_GRAVITY_MODIFIER_UUID);
                    gravityInstance.addTransientModifier(new AttributeModifier(IRON_GRAVITY_MODIFIER_UUID, "Iron Feruchemy Gravity Storing", -0.09D * storeLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                if (player.isInWater()) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, 0.01D * storeLevel, 0));
                }
            }
            case STEEL -> {
                AttributeInstance speedInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedInstance != null) {
                    speedInstance.removeModifier(STEEL_SPEED_MODIFIER_UUID);
                    speedInstance.addTransientModifier(new AttributeModifier(STEEL_SPEED_MODIFIER_UUID, "Steel Feruchemy Speed Storing", -0.05D * storeLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                AttributeInstance attackSpeedInstance = player.getAttribute(Attributes.ATTACK_SPEED);
                if (attackSpeedInstance != null) {
                    attackSpeedInstance.removeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID);
                    attackSpeedInstance.addTransientModifier(new AttributeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID, "Steel Feruchemy Attack Speed Storing", -0.08D * storeLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
            case TIN -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
            }
            case PEWTER -> {
                AttributeInstance damageInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (damageInstance != null) {
                    damageInstance.removeModifier(PEWTER_DAMAGE_MODIFIER_UUID);
                    damageInstance.addTransientModifier(new AttributeModifier(PEWTER_DAMAGE_MODIFIER_UUID, "Pewter Feruchemy Strength Storing", -0.5D * storeLevel, AttributeModifier.Operation.ADDITION));
                }
            }
            case GOLD -> {
                AttributeInstance healthInstance = player.getAttribute(Attributes.MAX_HEALTH);
                if (healthInstance != null) {
                    healthInstance.removeModifier(GOLD_HEALTH_MODIFIER_UUID);
                    double healthPct = player.getHealth() / player.getMaxHealth();
                    healthInstance.addTransientModifier(new AttributeModifier(GOLD_HEALTH_MODIFIER_UUID, "Gold Feruchemy Health Storing", -2.0D * storeLevel, AttributeModifier.Operation.ADDITION));
                    player.setHealth((float) Math.min(player.getMaxHealth(), player.getMaxHealth() * healthPct));
                }
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0, false, false));
            }
            case BRASS -> {
                player.setTicksFrozen(Math.min(player.getTicksFrozen() + storeLevel, 140));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
            }
            case ZINC -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, storeLevel / 2), false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, Math.max(0, storeLevel / 3), false, false));
            }
            case BRONZE -> {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, storeLevel - 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, Math.max(0, storeLevel / 3), false, false));
                if (player.tickCount % 60 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false));
                }
            }
            case ELECTRUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, Math.max(0, storeLevel / 2), false, false));
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 40, Math.max(0, storeLevel / 2), false, false));
            }
            case CHROMIUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 40, Math.max(0, storeLevel / 2), false, false));
            }
            case NICROSIL -> {
                if (!data.burningMetals().isEmpty()) {
                    data.stopAllBurning();
                    player.displayClientMessage(Component.literal("Allomancy locked out while storing Investiture!").withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            }
            case TRELLIUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, storeLevel - 1, false, false));
            }
            case RAYSIUM -> {
                if (player.tickCount % 20 == 0) {
                    data.burningMetals().stream()
                        .filter(m -> m != Metal.RAYSIUM)
                        .forEach(m -> data.consumeReserve(m, 5.0F * storeLevel));
                }
            }
            case TANAVASTIUM -> {
            }
            case ATIUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, storeLevel - 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, storeLevel - 1, false, false));
            }
            default -> {}
        }
    }

    private static void applyTapBenefit(LivingEntity player, MetalArtsData data, ItemStack stack, Metal metal, float tapLevel) {
        switch (metal) {
            case IRON -> {
                AttributeInstance kbInstance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (kbInstance != null) {
                    kbInstance.removeModifier(IRON_KB_MODIFIER_UUID);
                    kbInstance.addTransientModifier(new AttributeModifier(IRON_KB_MODIFIER_UUID, "Iron Feruchemy Weight Tapping", 0.25D * tapLevel, AttributeModifier.Operation.ADDITION));
                }
                AttributeInstance gravityInstance = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
                if (gravityInstance != null) {
                    gravityInstance.removeModifier(IRON_GRAVITY_MODIFIER_UUID);
                    gravityInstance.addTransientModifier(new AttributeModifier(IRON_GRAVITY_MODIFIER_UUID, "Iron Feruchemy Gravity Tapping", 0.45D * tapLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                if (player.isInWater()) {
                    player.setDeltaMovement(player.getDeltaMovement().add(0, -0.15D * tapLevel, 0));
                }
            }
            case STEEL -> {
                AttributeInstance speedInstance = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedInstance != null) {
                    speedInstance.removeModifier(STEEL_SPEED_MODIFIER_UUID);
                    speedInstance.addTransientModifier(new AttributeModifier(STEEL_SPEED_MODIFIER_UUID, "Steel Feruchemy Speed Tapping", 0.12D * tapLevel * tapLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                AttributeInstance attackSpeedInstance = player.getAttribute(Attributes.ATTACK_SPEED);
                if (attackSpeedInstance != null) {
                    attackSpeedInstance.removeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID);
                    attackSpeedInstance.addTransientModifier(new AttributeModifier(STEEL_ATTACK_SPEED_MODIFIER_UUID, "Steel Feruchemy Attack Speed Tapping", 0.35D * tapLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, (int) tapLevel - 1, false, false));
            }
            case TIN -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
                if (player.tickCount % 10 == 0) {
                    double soundRadius = 8.0D + 4.0D * tapLevel;
                    AABB area = player.getBoundingBox().inflate(soundRadius);
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                        if (target.getDeltaMovement().lengthSqr() > 0.001D) {
                            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                        }
                    }
                }
            }
            case PEWTER -> {
                AttributeInstance damageInstance = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (damageInstance != null) {
                    damageInstance.removeModifier(PEWTER_DAMAGE_MODIFIER_UUID);
                    damageInstance.addTransientModifier(new AttributeModifier(PEWTER_DAMAGE_MODIFIER_UUID, "Pewter Feruchemy Strength Tapping", 2.0D * tapLevel, AttributeModifier.Operation.ADDITION));
                }
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, (int) tapLevel - 1, false, false));
            }
            case GOLD -> {
                float healAmount = 0.5F * tapLevel;
                float bloatVal = data.spiritualBloat();
                if (bloatVal > 50.0F) {
                    healAmount *= 0.7F; // 30% reduction in healing efficiency
                }
                player.heal(healAmount);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, bloatVal > 50.0F ? Math.max(0, (int)(tapLevel * 0.7F)) : (int) tapLevel, false, false));
                if (player.tickCount % 5 == 0) {
                    player.removeEffect(MobEffects.POISON);
                    player.removeEffect(MobEffects.WITHER);
                }
                if (data.spiritualScarring() > 0.0F) {
                    data.setSpiritualScarring(data.spiritualScarring() - (0.005F * tapLevel));
                }
            }
            case BRASS -> {
                player.setTicksFrozen(0);
                AABB area = player.getBoundingBox().inflate(2.5D + 0.5D * tapLevel);
                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                    if (!(target instanceof Player p && player instanceof Player sourcePlayer && !p.canHarmPlayer(sourcePlayer))) {
                        target.setSecondsOnFire((int) (2 * tapLevel));
                    }
                }
                if (player.tickCount % 10 == 0) {
                    BlockPos feet = player.blockPosition();
                    for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, 1, 2))) {
                        BlockState state = player.level().getBlockState(pos);
                        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                            player.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        } else if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)) {
                            player.level().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                        }
                    }
                }
            }
            case ZINC -> {
                double zincRadius = 6.0D + 2.0D * tapLevel;
                for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(zincRadius))) {
                    if (entity instanceof Projectile) {
                        Vec3 motion = entity.getDeltaMovement();
                        entity.setDeltaMovement(motion.scale(1.0D - (0.08D * tapLevel)));
                    } else if (entity instanceof Monster monster) {
                        monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, true));
                    }
                }
                if (player.tickCount % 2 == 0) {
                    BlockPos origin = player.blockPosition();
                    for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -2, -3), origin.offset(3, 2, 3))) {
                        net.minecraft.world.level.block.entity.BlockEntity be = player.level().getBlockEntity(pos);
                        if (be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
                            for (int i = 0; i < (int) (tapLevel * 2); i++) {
                                net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(
                                    (ServerLevel) player.level(), pos, player.level().getBlockState(pos), furnace
                                );
                            }
                        } else if (be instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity brewing) {
                            for (int i = 0; i < (int) (tapLevel * 2); i++) {
                                net.minecraft.world.level.block.entity.BrewingStandBlockEntity.serverTick(
                                    player.level(), pos, player.level().getBlockState(pos), brewing
                                );
                            }
                        } else if (be instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity campfire) {
                            for (int i = 0; i < (int) (tapLevel * 2); i++) {
                                net.minecraft.world.level.block.entity.CampfireBlockEntity.cookTick(
                                    player.level(), pos, player.level().getBlockState(pos), campfire
                                );
                            }
                        }
                    }
                }
            }
            case BRONZE -> {
                AABB area = player.getBoundingBox().inflate(32.0D);
                for (Phantom phantom : player.level().getEntitiesOfClass(Phantom.class, area)) {
                    if (phantom.getTarget() == player) {
                        phantom.setTarget(null);
                    }
                }
                if (player instanceof ServerPlayer p) {
                    p.getStats().setValue(p, net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TIME_SINCE_REST), 0);
                }
                
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                player.removeEffect(MobEffects.WEAKNESS);
            }
            case ELECTRUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, (int) tapLevel - 1, false, false));
                if (player.tickCount % 20 == 0 && player.getAbsorptionAmount() < 4.0F * tapLevel) {
                    player.setAbsorptionAmount(Math.min(4.0F * tapLevel, player.getAbsorptionAmount() + 2.0F));
                }
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                player.removeEffect(MobEffects.BAD_OMEN);
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.DARKNESS);
                player.removeEffect(MobEffects.UNLUCK);
            }
            case CHROMIUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 40, (int) tapLevel, false, false));
            }
            case NICROSIL -> {
                if (player.tickCount % 5 == 0) {
                    for (Metal activeBurn : data.burningMetals()) {
                        data.fillReserve(activeBurn, 0.05F * tapLevel);
                    }
                }
            }
            case TRELLIUM -> {
                if (player.tickCount % 20 == 0) {
                    double radius = 16.0D + 4.0D * tapLevel;
                    AABB area = player.getBoundingBox().inflate(radius);
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
                    }
                }
            }
            case RAYSIUM -> {
                if (player.tickCount % 20 == 0) {
                    double radius = 4.0D + 2.0D * tapLevel;
                    AABB area = player.getBoundingBox().inflate(radius);
                    for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
                        if (target instanceof Monster || (target instanceof Player p && p != player)) {
                            float damage = 1.5F * tapLevel;
                            target.hurt(player.damageSources().magic(), damage);
                            player.heal(damage * 0.5F);
                        }
                    }
                }
            }
            case TANAVASTIUM -> {
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.WEAKNESS);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, (int) tapLevel - 1, false, false));
            }
            case ATIUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, (int) tapLevel - 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, (int) tapLevel - 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, (int) tapLevel - 1, false, false));
            }
            default -> {}
        }
    }

    public static int getPlayerXP(Player player) {
        return (int) (getXPForLevel(player.experienceLevel) + (player.experienceProgress * getXPForNextLevel(player.experienceLevel)));
    }

    public static int getXPForLevel(int level) {
        if (level <= 15) {
            return level * level + 6 * level;
        } else if (level <= 30) {
            return (int) (2.5D * level * level - 40.5D * level + 360D);
        } else {
            return (int) (4.5D * level * level - 162.5D * level + 2220D);
        }
    }

    public static int getXPForNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    public static void setPlayerXP(Player player, int totalXP) {
        player.totalExperience = Math.max(0, totalXP);
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        int xp = totalXP;
        while (xp >= getXPForNextLevel(player.experienceLevel)) {
            xp -= getXPForNextLevel(player.experienceLevel);
            player.experienceLevel++;
        }
        player.experienceProgress = (float) xp / (float) getXPForNextLevel(player.experienceLevel);
    }

    private static boolean canUse(Player player, MetalArtsData data, ItemStack stack, Metal metal) {
        return (data.hasFeruchemicalPower(metal) || (MetalmindItem.isUnkeyed(stack) && ServerConfig.VALUES.unkeyedMetalmindsEnabled.get())) && MetalmindItem.canUse(stack, player);
    }
}
