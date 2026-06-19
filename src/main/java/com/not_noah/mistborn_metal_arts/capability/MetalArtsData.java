package com.not_noah.mistborn_metal_arts.capability;

import com.not_noah.mistborn_metal_arts.api.Metal;
import com.not_noah.mistborn_metal_arts.config.ServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class MetalArtsData {
    private final EnumSet<Metal> naturalAllomanticPowers = EnumSet.noneOf(Metal.class);
    private final EnumSet<Metal> naturalFeruchemicalPowers = EnumSet.noneOf(Metal.class);
    private final EnumSet<Metal> allomanticPowers = EnumSet.noneOf(Metal.class);
    private final EnumSet<Metal> spikedAllomanticPowers = EnumSet.noneOf(Metal.class);
    private final EnumSet<Metal> feruchemicalPowers = EnumSet.noneOf(Metal.class);
    private final EnumSet<Metal> burningMetals = EnumSet.noneOf(Metal.class);
    private final EnumSet<Metal> flaringMetals = EnumSet.noneOf(Metal.class);
    private final EnumMap<Metal, Float> reserves = new EnumMap<>(Metal.class);
    private final EnumMap<Metal, Float> metalmindCharges = new EnumMap<>(Metal.class);
    private final EnumMap<Metal, Integer> feruchemyModes = new EnumMap<>(Metal.class);
    private final List<InstalledSpike> installedSpikes = new ArrayList<>();
    private Metal selectedMetal = Metal.IRON;
    private int corruption;
    private int equippedSpikeCorruption;
    private int duraluminCooldown;
    private int bubbleCooldown;
    private boolean wellTouched;
    private boolean firstJoinRollComplete;
    private float bronzePulseYaw;
    private float bronzePulseStrength;
    private float bronzePulseDistance;
    private String bronzePulseMetal = "";
    private boolean copperclouded;
    private boolean needsPowerRefresh = true;
    private boolean allomancySnapped = true;
    private float allomanticStrength = 0.5F;
    private int pewterDragTicks = 0;
    private int pewterBurnDuration = 0;
    private boolean restrained;
    private net.minecraft.core.BlockPos restrainedAltarPos = net.minecraft.core.BlockPos.ZERO;
    private int altarSeatIndex;
    private final EnumMap<Metal, Float> equippedAllomanticStrengths = new EnumMap<>(Metal.class);
    private boolean restrainedByOthers;
    private final EnumMap<Metal, Float> naturalAllomanticStrengths = new EnumMap<>(Metal.class);
    private net.minecraft.world.entity.LivingEntity entity = null;

    public MetalArtsData(net.minecraft.world.entity.LivingEntity entity) {
        this();
        this.entity = entity;
    }

    // ── Soul Stability ──
    private float soulStability = 100.0F;
    private boolean hasLinchpinSpike = false;
    private int linchpinSpikeIndex = -1;
    private float spiritualScarring = 0.0F;

    // ── Identity Contamination ──
    private float identityContamination = 0.0F;
    private int contaminationStage = 0;

    // ── Savantism (per-metal) ──
    private final EnumMap<Metal, Float> savantProgress = new EnumMap<>(Metal.class);
    private final EnumMap<Metal, Integer> savantStage = new EnumMap<>(Metal.class);
    private final EnumMap<Metal, Long> savantLastBurned = new EnumMap<>(Metal.class);

    // ── Spiritual Bloat ──
    private float spiritualBloat = 0.0F;
    private int forcedSystemCount = 0;

    // ── Lerasium Enhancement ──
    private int lerasiumEnhancements = 0;
    private float lerasiumBonus = 0.0F;
    private final EnumMap<Metal, Float> lerasiumAlloyBonuses = new EnumMap<>(Metal.class);
    private final EnumMap<Metal, Float> lerasatiumAlloyBonuses = new EnumMap<>(Metal.class);

    public MetalArtsData() {
        for (Metal metal : Metal.cachedValues()) {
            reserves.put(metal, 0F);
            metalmindCharges.put(metal, 0F);
            feruchemyModes.put(metal, 0);
            equippedAllomanticStrengths.put(metal, 0F);
            naturalAllomanticStrengths.put(metal, 0F);
            savantProgress.put(metal, 0.0F);
            savantStage.put(metal, 0);
            savantLastBurned.put(metal, 0L);
            lerasiumAlloyBonuses.put(metal, 0.0F);
            lerasatiumAlloyBonuses.put(metal, 0.0F);
        }
        java.util.Random rand = new java.util.Random();
        this.allomanticStrength = 0.3F + rand.nextFloat() * 0.4F;
    }

    public void clearEquippedStrengths() {
        equippedAllomanticStrengths.replaceAll((m, v) -> 0F);
    }

    public void addEquippedStrength(Metal metal, float strength) {
        equippedAllomanticStrengths.put(metal, equippedAllomanticStrengths.getOrDefault(metal, 0F) + strength);
    }

    public boolean needsPowerRefresh() {
        return needsPowerRefresh;
    }

    public void markNeedsPowerRefresh() {
        this.needsPowerRefresh = true;
    }

    public Set<Metal> allomanticPowers() {
        if (needsPowerRefresh) refreshPowers();
        java.util.EnumSet<Metal> active = java.util.EnumSet.noneOf(Metal.class);
        for (Metal m : allomanticPowers) {
            if (hasAllomanticPower(m)) {
                active.add(m);
            }
        }
        return active;
    }

    public Set<Metal> allomanticPowersRaw() {
        if (needsPowerRefresh) refreshPowers();
        return EnumSet.copyOf(allomanticPowers);
    }

    public Set<Metal> feruchemicalPowers() {
        if (needsPowerRefresh) refreshPowers();
        return EnumSet.copyOf(feruchemicalPowers);
    }

    public Set<Metal> burningMetals() {
        return EnumSet.copyOf(burningMetals);
    }

    public Set<Metal> flaringMetals() {
        return EnumSet.copyOf(flaringMetals);
    }

    public boolean hasAllomanticPower(Metal metal) {
        if (needsPowerRefresh) refreshPowers();
        if (!metal.isAllomantic() || !allomanticPowers.contains(metal) || !ServerConfig.isMetalEnabled(metal)) {
            return false;
        }
        if (spikedAllomanticPowers.contains(metal)) {
            return true;
        }
        return allomancySnapped;
    }

    public boolean hasFeruchemicalPower(Metal metal) {
        if (needsPowerRefresh) refreshPowers();
        return metal.isFeruchemical() && feruchemicalPowers.contains(metal);
    }

    public void setMisting(Metal metal) {
        naturalAllomanticPowers.clear();
        if (metal.isAllomantic()) {
            naturalAllomanticPowers.add(metal);
        }
        if (entity != null) {
            entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("allomancy");
                if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Allomancy allomancy) {
                    for (Metal m : Metal.cachedValues()) {
                        allomancy.setPower(m, 0.0F);
                    }
                    allomancy.setPower(metal, 0.5F);
                }
            });
        }
        markNeedsPowerRefresh();
    }

    public void setMistborn() {
        naturalAllomanticPowers.clear();
        for (Metal metal : Metal.cachedValues()) {
            if (metal.isAllomantic() || metal == Metal.LERASIUM) {
                naturalAllomanticPowers.add(metal);
            }
        }
        if (entity != null) {
            entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("allomancy");
                if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Allomancy allomancy) {
                    for (Metal m : Metal.cachedValues()) {
                        if (m.isAllomantic()) {
                            if (allomancy.getPower(m) <= 0F) {
                                allomancy.setPower(m, 0.5F);
                            }
                        }
                    }
                }
            });
        }
        markNeedsPowerRefresh();
    }

    public void clearAllomancy() {
        naturalAllomanticPowers.clear();
        burningMetals.clear();
        flaringMetals.clear();
        if (entity != null) {
            entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("allomancy");
                if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Allomancy allomancy) {
                    for (Metal m : Metal.cachedValues()) {
                        allomancy.setPower(m, 0.0F);
                    }
                }
            });
        }
        markNeedsPowerRefresh();
    }

    public void setFerring(Metal metal) {
        naturalFeruchemicalPowers.clear();
        if (metal.isFeruchemical()) {
            naturalFeruchemicalPowers.add(metal);
        }
        if (entity != null) {
            entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("feruchemy");
                if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy) {
                    for (Metal m : Metal.cachedValues()) {
                        if (m.isFeruchemical()) {
                            feruchemy.setPower(m, 0.0F);
                        }
                    }
                    feruchemy.setPower(metal, 1.0F);
                }
            });
        }
        markNeedsPowerRefresh();
    }

    public void setFullFeruchemist() {
        naturalFeruchemicalPowers.clear();
        for (Metal metal : Metal.cachedValues()) {
            if (metal.isFeruchemical()) {
                naturalFeruchemicalPowers.add(metal);
            }
        }
        if (entity != null) {
            entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("feruchemy");
                if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy) {
                    for (Metal m : Metal.cachedValues()) {
                        if (m.isFeruchemical()) {
                            feruchemy.setPower(m, 1.0F);
                        }
                    }
                }
            });
        }
        markNeedsPowerRefresh();
    }

    public void clearFeruchemy() {
        naturalFeruchemicalPowers.clear();
        feruchemyModes.replaceAll((metal, mode) -> 0);
        if (entity != null) {
            entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("feruchemy");
                if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy) {
                    for (Metal m : Metal.cachedValues()) {
                        if (m.isFeruchemical()) {
                            feruchemy.setPower(m, 0.0F);
                        }
                    }
                }
            });
        }
        markNeedsPowerRefresh();
    }

    public void setFullborn() {
        setMistborn();
        setFullFeruchemist();
    }

    public void refreshPowers() {
        if (entity != null) {
            refreshPowers(entity);
        } else {
            needsPowerRefresh = false;
            stopInvalidBurns();
        }
    }

    public void refreshPowers(net.minecraft.world.entity.LivingEntity entity) {
        allomanticPowers.clear();
        feruchemicalPowers.clear();
        spikedAllomanticPowers.clear();
        
        entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
            web.refreshSpikedFragments(entity, this);
            for (Metal m : Metal.cachedValues()) {
                if (web.hasAllomanticPower(m)) {
                    allomanticPowers.add(m);
                }
                if (web.hasNaturalAllomanticPower(m)) {
                    naturalAllomanticPowers.add(m);
                } else {
                    naturalAllomanticPowers.remove(m);
                }
                if (web.hasForeignAllomanticPower(m)) {
                    spikedAllomanticPowers.add(m);
                }
                if (web.hasFeruchemicalPower(m)) {
                    feruchemicalPowers.add(m);
                }
                if (web.hasNaturalFeruchemicalPower(m)) {
                    naturalFeruchemicalPowers.add(m);
                } else {
                    naturalFeruchemicalPowers.remove(m);
                }
            }
            this.allomancySnapped = web.allomancySnapped();
            
            com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("allomancy");
            if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Allomancy allomancy) {
                for (Metal m : Metal.cachedValues()) {
                    naturalAllomanticStrengths.put(m, allomancy.getPower(m));
                }
            }
        });
        needsPowerRefresh = false;
        stopInvalidBurns();
    }

    public float getPhysicalStrengthBonus() {
        if (entity != null) {
            return entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).map(web -> {
                return (web.getTotalStrength() - 1.0F) * 4.0F;
            }).orElse(0.0F);
        }
        return 0.0F;
    }

    public float getPhysicalSightBonus() {
        if (entity != null) {
            return entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).map(web -> {
                return web.getTotalSight() - 1.0F;
            }).orElse(0.0F);
        }
        return 0.0F;
    }

    public void addSpikePower(Metal metal, String type) {
        if ("feruchemy".equals(type) && metal.isFeruchemical()) {
            feruchemicalPowers.add(metal);
        } else if (metal.isAllomantic()) {
            allomanticPowers.add(metal);
            spikedAllomanticPowers.add(metal);
        }
    }

    public boolean startBurning(Metal metal) {
        if (!hasAllomanticPower(metal) || metal == Metal.LERASIUM || getReserve(metal) <= 0F) {
            return false;
        }
        burningMetals.add(metal);
        return true;
    }

    public void stopBurning(Metal metal) {
        burningMetals.remove(metal);
        flaringMetals.remove(metal);
    }

    public void stopAllBurning() {
        burningMetals.clear();
        flaringMetals.clear();
    }

    public boolean isBurning(Metal metal) {
        return burningMetals.contains(metal);
    }

    public void setFlaring(Metal metal, boolean flaring) {
        if (flaring && isBurning(metal)) {
            flaringMetals.add(metal);
        } else {
            flaringMetals.remove(metal);
        }
    }

    public boolean isFlaring(Metal metal) {
        return flaringMetals.contains(metal);
    }

    public float getReserve(Metal metal) {
        return reserves.getOrDefault(metal, 0F);
    }

    public float fillReserve(Metal metal, float amount) {
        float cap = ServerConfig.reserveCapacity(metal);
        float value = Math.min(cap, getReserve(metal) + amount);
        reserves.put(metal, value);
        return value;
    }

    public float setReserve(Metal metal, float amount) {
        float cap = ServerConfig.reserveCapacity(metal);
        float value = Math.max(0F, Math.min(cap, amount));
        reserves.put(metal, value);
        if (value <= 0F) {
            stopBurning(metal);
        }
        return value;
    }

    public float consumeReserve(Metal metal, float amount) {
        return setReserve(metal, getReserve(metal) - amount);
    }

    public void clearReserves() {
        for (Metal metal : Metal.cachedValues()) {
            reserves.put(metal, 0F);
        }
        burningMetals.clear();
        flaringMetals.clear();
    }

    public float getMetalmindCharge(Metal metal) {
        return metalmindCharges.getOrDefault(metal, 0F);
    }

    public void setMetalmindCharge(Metal metal, float amount) {
        metalmindCharges.put(metal, Math.max(0F, amount));
    }

    public int feruchemyMode(Metal metal) {
        return feruchemyModes.getOrDefault(metal, 0);
    }

    public boolean isStoring(Metal metal) {
        return feruchemyMode(metal) < 0;
    }

    public boolean isTapping(Metal metal) {
        return feruchemyMode(metal) > 0;
    }

    public void setFeruchemyMode(Metal metal, int mode) {
        if (!metal.isFeruchemical() || !hasFeruchemicalPower(metal)) {
            feruchemyModes.put(metal, 0);
            return;
        }
        feruchemyModes.put(metal, mode);
    }

    public int cycleFeruchemyMode(Metal metal) {
        int current = feruchemyMode(metal);
        int next = switch (current) {
            case 0 -> -1;
            case -1 -> 1;
            case 1 -> 2;
            case 2 -> 4;
            case 4 -> 8;
            case 8 -> 10;
            case 10 -> 10; // Don't reset, clamp!
            default -> 0;
        };
        setFeruchemyMode(metal, next);
        return feruchemyMode(metal);
    }

    public void stopFeruchemy(Metal metal) {
        feruchemyModes.put(metal, 0);
    }

    public List<InstalledSpike> installedSpikes() {
        return Collections.unmodifiableList(installedSpikes);
    }

    public boolean installSpike(Metal spikeMetal, String powerType, Metal powerMetal, float strength) {
        return installSpike(spikeMetal, powerType, powerMetal, strength, com.not_noah.mistborn_metal_arts.api.SpiritualAttributes.generateIdentity(), new CompoundTag());
    }

    public boolean installSpike(Metal spikeMetal, String powerType, Metal powerMetal, float strength, String identityKey) {
        return installSpike(spikeMetal, powerType, powerMetal, strength, identityKey, new CompoundTag());
    }

    public boolean installSpike(Metal spikeMetal, String powerType, Metal powerMetal, float strength, String identityKey, CompoundTag stolenSpiritWeb) {
        if (installedSpikes.size() >= ServerConfig.VALUES.maxInstalledSpikes.get()) {
            return false;
        }
        String idKey = (identityKey == null || identityKey.isBlank()) ? com.not_noah.mistborn_metal_arts.api.SpiritualAttributes.generateIdentity() : identityKey;
        InstalledSpike spike = new InstalledSpike(spikeMetal, powerType, powerMetal, Math.max(0.05F, strength), 0, 0.0F, idKey, stolenSpiritWeb);
        installedSpikes.add(spike);
        setCorruption(corruption + Math.max(1, Math.round(2F * spike.strength())));
        if ("feruchemy".equals(spike.powerType()) && spike.powerMetal().isFeruchemical()) {
            feruchemicalPowers.add(spike.powerMetal());
        } else if (spike.powerMetal().isAllomantic()) {
            allomanticPowers.add(spike.powerMetal());
            spikedAllomanticPowers.add(spike.powerMetal());
        }
        addIdentityContamination((float) ServerConfig.VALUES.contaminationPerSpike.get().doubleValue());
        com.not_noah.mistborn_metal_arts.hemalurgy.SoulStabilityManager.recalculateStability(this);
        markNeedsPowerRefresh();
        return true;
    }

    public boolean removeSpike(int index) {
        if (index < 0 || index >= installedSpikes.size()) {
            return false;
        }
        installedSpikes.remove(index);
        setCorruption(Math.max(0, corruption - 1));
        reduceIdentityContamination((float) ServerConfig.VALUES.contaminationPerSpike.get().doubleValue());
        setSpiritualScarring(spiritualScarring + 2.0F); // Uninstalling a spike inflicts +2.0% spiritual scarring
        com.not_noah.mistborn_metal_arts.hemalurgy.SoulStabilityManager.recalculateStability(this);
        markNeedsPowerRefresh();
        return true;
    }

    public boolean removeLastSpike() {
        return removeSpike(installedSpikes.size() - 1);
    }

    public void updateSpikeStrength(int index, float strength) {
        if (index >= 0 && index < installedSpikes.size()) {
            InstalledSpike old = installedSpikes.get(index);
            installedSpikes.set(index, new InstalledSpike(old.spikeMetal(), old.powerType(), old.powerMetal(), Math.max(0.0F, strength), old.decayTicks(), old.feruchemicalCharge(), old.identityKey(), old.stolenSpiritWeb()));
            markNeedsPowerRefresh();
        }
    }

    public void updateSpikeFeruchemicalCharge(int index, float charge) {
        if (index >= 0 && index < installedSpikes.size()) {
            InstalledSpike old = installedSpikes.get(index);
            installedSpikes.set(index, new InstalledSpike(old.spikeMetal(), old.powerType(), old.powerMetal(), old.strength(), old.decayTicks(), Math.max(0.0F, charge), old.identityKey(), old.stolenSpiritWeb()));
            markNeedsPowerRefresh();
        }
    }

    public Metal selectedMetal() {
        return selectedMetal;
    }

    public void setSelectedMetal(Metal selectedMetal) {
        this.selectedMetal = selectedMetal;
    }

    public int corruption() {
        return corruption;
    }

    public int equippedSpikeCorruption() {
        return equippedSpikeCorruption;
    }

    public int totalCorruption() {
        return corruption + equippedSpikeCorruption;
    }

    public void setEquippedSpikeCorruption(int equippedSpikeCorruption) {
        this.equippedSpikeCorruption = Math.max(0, equippedSpikeCorruption);
    }

    public void setCorruption(int corruption) {
        this.corruption = Math.max(0, corruption);
    }

    public int duraluminCooldown() {
        return duraluminCooldown;
    }

    public void setDuraluminCooldown(int duraluminCooldown) {
        this.duraluminCooldown = Math.max(0, duraluminCooldown);
    }

    public int bubbleCooldown() {
        return bubbleCooldown;
    }

    public void setBubbleCooldown(int bubbleCooldown) {
        this.bubbleCooldown = Math.max(0, bubbleCooldown);
    }

    public void tickCooldowns() {
        if (duraluminCooldown > 0) {
            duraluminCooldown--;
        }
        if (bubbleCooldown > 0) {
            bubbleCooldown--;
        }
    }

    public boolean wellTouched() {
        return wellTouched;
    }

    public void setWellTouched(boolean wellTouched) {
        this.wellTouched = wellTouched;
    }

    public boolean allomancySnapped() {
        return allomancySnapped;
    }

    public void setAllomancySnapped(boolean allomancySnapped) {
        this.allomancySnapped = allomancySnapped;
        if (entity != null) {
            entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                web.setAllomancySnapped(allomancySnapped);
            });
        }
        this.markNeedsPowerRefresh();
    }

    public boolean firstJoinRollComplete() {
        return firstJoinRollComplete;
    }

    public void setFirstJoinRollComplete(boolean firstJoinRollComplete) {
        this.firstJoinRollComplete = firstJoinRollComplete;
    }

    public void setBronzePulse(float yaw, float strength, float distance, String metal) {
        this.bronzePulseYaw = yaw;
        this.bronzePulseStrength = strength;
        this.bronzePulseDistance = distance;
        this.bronzePulseMetal = metal == null ? "" : metal;
    }

    public float bronzePulseYaw() {
        return bronzePulseYaw;
    }

    public float bronzePulseStrength() {
        return bronzePulseStrength;
    }

    public float bronzePulseDistance() {
        return bronzePulseDistance;
    }

    public String bronzePulseMetal() {
        return bronzePulseMetal;
    }

    public boolean isCopperclouded() {
        return copperclouded;
    }

    public void setCopperclouded(boolean copperclouded) {
        this.copperclouded = copperclouded;
    }

    public CompoundTag serializeReservesNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag reserveTag = new CompoundTag();
        for (Metal metal : Metal.cachedValues()) {
            float reserve = getReserve(metal);
            if (reserve > 0F) {
                reserveTag.putFloat(metal.id(), reserve);
            }
        }
        tag.put("Reserves", reserveTag);
        CompoundTag metalmindTag = new CompoundTag();
        for (Metal metal : Metal.cachedValues()) {
            float charge = getMetalmindCharge(metal);
            if (charge > 0F) {
                metalmindTag.putFloat(metal.id(), charge);
            }
        }
        tag.put("Metalminds", metalmindTag);
        tag.put("Burning", metalSet(burningMetals));
        tag.put("Flaring", metalSet(flaringMetals));
        tag.putFloat("BronzePulseYaw", bronzePulseYaw);
        tag.putFloat("BronzePulseStrength", bronzePulseStrength);
        tag.putFloat("BronzePulseDistance", bronzePulseDistance);
        tag.putString("BronzePulseMetal", bronzePulseMetal);
        return tag;
    }

    public CompoundTag serializeNBT() {
        if (needsPowerRefresh) refreshPowers();
        CompoundTag tag = new CompoundTag();
        tag.put("NaturalAllomanticPowers", metalSet(naturalAllomanticPowers));
        tag.put("NaturalFeruchemicalPowers", metalSet(naturalFeruchemicalPowers));
        tag.put("AllomanticPowers", metalSet(allomanticPowers));
        tag.put("FeruchemicalPowers", metalSet(feruchemicalPowers));
        tag.put("SpikedAllomanticPowers", metalSet(spikedAllomanticPowers));
        tag.put("Burning", metalSet(burningMetals));
        tag.put("Flaring", metalSet(flaringMetals));
        CompoundTag reserveTag = new CompoundTag();
        CompoundTag metalmindTag = new CompoundTag();
        CompoundTag feruchemyModeTag = new CompoundTag();
        for (Metal metal : Metal.cachedValues()) {
            reserveTag.putFloat(metal.id(), getReserve(metal));
            metalmindTag.putFloat(metal.id(), getMetalmindCharge(metal));
            feruchemyModeTag.putInt(metal.id(), feruchemyMode(metal));
        }
        tag.put("Reserves", reserveTag);
        tag.put("Metalminds", metalmindTag);
        tag.put("FeruchemyModes", feruchemyModeTag);

        CompoundTag equippedStrengthTag = new CompoundTag();
        equippedAllomanticStrengths.forEach((metal, strength) -> {
            if (strength > 0) {
                equippedStrengthTag.putFloat(metal.id(), strength);
            }
        });
        tag.put("EquippedStrengths", equippedStrengthTag);

        CompoundTag naturalAllomanticStrengthsTag = new CompoundTag();
        naturalAllomanticStrengths.forEach((metal, strength) -> {
            if (strength > 0) {
                naturalAllomanticStrengthsTag.putFloat(metal.id(), strength);
            }
        });
        tag.put("NaturalAllomanticStrengths", naturalAllomanticStrengthsTag);

        ListTag spikeList = new ListTag();
        for (InstalledSpike spike : installedSpikes) {
            spikeList.add(spike.serializeNBT());
        }
        tag.put("InstalledSpikes", spikeList);
        tag.putString("SelectedMetal", selectedMetal.id());
        tag.putInt("Corruption", corruption);
        tag.putInt("EquippedSpikeCorruption", equippedSpikeCorruption);
        tag.putInt("DuraluminCooldown", duraluminCooldown);
        tag.putInt("BubbleCooldown", bubbleCooldown);
        tag.putBoolean("WellTouched", wellTouched);
        tag.putBoolean("FirstJoinRollComplete", firstJoinRollComplete);
        tag.putFloat("BronzePulseYaw", bronzePulseYaw);
        tag.putFloat("BronzePulseStrength", bronzePulseStrength);
        tag.putFloat("BronzePulseDistance", bronzePulseDistance);
        tag.putString("BronzePulseMetal", bronzePulseMetal);
        tag.putBoolean("Copperclouded", copperclouded);
        tag.putFloat("AllomanticStrength", allomanticStrength);
        tag.putInt("PewterDragTicks", pewterDragTicks);
        tag.putInt("PewterBurnDuration", pewterBurnDuration);
        tag.putBoolean("AllomancySnapped", allomancySnapped);
        tag.putBoolean("Restrained", restrained);
        tag.putLong("RestrainedAltarPos", restrainedAltarPos.asLong());
        tag.putInt("AltarSeatIndex", altarSeatIndex);
        tag.putBoolean("RestrainedByOthers", restrainedByOthers);

        // ── Expanded Systems ──
        tag.putFloat("SoulStability", soulStability);
        tag.putBoolean("HasLinchpinSpike", hasLinchpinSpike);
        tag.putInt("LinchpinSpikeIndex", linchpinSpikeIndex);
        tag.putFloat("SpiritualScarring", spiritualScarring);
        tag.putFloat("IdentityContamination", identityContamination);
        tag.putInt("ContaminationStage", contaminationStage);
        tag.putFloat("SpiritualBloat", spiritualBloat);
        tag.putInt("ForcedSystemCount", forcedSystemCount);
        tag.putInt("LerasiumEnhancements", lerasiumEnhancements);
        tag.putFloat("LerasiumBonus", lerasiumBonus);

        CompoundTag savantProgressTag = new CompoundTag();
        CompoundTag savantStageTag = new CompoundTag();
        CompoundTag alloyBonusTag = new CompoundTag();
        for (Metal metal : Metal.cachedValues()) {
            float sp = savantProgress.getOrDefault(metal, 0.0F);
            if (sp > 0) savantProgressTag.putFloat(metal.id(), sp);
            int ss = savantStage.getOrDefault(metal, 0);
            if (ss > 0) savantStageTag.putInt(metal.id(), ss);
            float ab = lerasiumAlloyBonuses.getOrDefault(metal, 0.0F);
            if (ab > 0) alloyBonusTag.putFloat(metal.id(), ab);
        }
        tag.put("SavantProgress", savantProgressTag);
        tag.put("SavantStage", savantStageTag);
        tag.put("LerasiumAlloyBonuses", alloyBonusTag);

        CompoundTag savantLastBurnedTag = new CompoundTag();
        for (Metal metal : Metal.cachedValues()) {
            long lb = savantLastBurned.getOrDefault(metal, 0L);
            if (lb > 0) savantLastBurnedTag.putLong(metal.id(), lb);
        }
        tag.put("SavantLastBurned", savantLastBurnedTag);

        CompoundTag lerasatiumAlloyBonusTag = new CompoundTag();
        for (Metal metal : Metal.cachedValues()) {
            float lab = lerasatiumAlloyBonuses.getOrDefault(metal, 0.0F);
            if (lab > 0) lerasatiumAlloyBonusTag.putFloat(metal.id(), lab);
        }
        tag.put("LerasatiumAlloyBonuses", lerasatiumAlloyBonusTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        deserializeNBT(tag, true);
    }

    public void deserializeNBT(CompoundTag tag, boolean full) {
        if (full) {
            naturalAllomanticPowers.clear();
            naturalFeruchemicalPowers.clear();
            allomanticPowers.clear();
            feruchemicalPowers.clear();
            burningMetals.clear();
            flaringMetals.clear();
            installedSpikes.clear();
            spikedAllomanticPowers.clear();
            reserves.replaceAll((m, v) -> 0F);
            metalmindCharges.replaceAll((m, v) -> 0F);
            feruchemyModes.replaceAll((m, v) -> 0);
        }

        if (tag.contains("NaturalAllomanticPowers")) {
            naturalAllomanticPowers.clear();
            readMetalSet(tag.getList("NaturalAllomanticPowers", 8), naturalAllomanticPowers);
        }
        if (tag.contains("NaturalFeruchemicalPowers")) {
            naturalFeruchemicalPowers.clear();
            readMetalSet(tag.getList("NaturalFeruchemicalPowers", 8), naturalFeruchemicalPowers);
        }
        if (tag.contains("AllomanticPowers")) {
            allomanticPowers.clear();
            readMetalSet(tag.getList("AllomanticPowers", 8), allomanticPowers);
        }
        if (tag.contains("FeruchemicalPowers")) {
            feruchemicalPowers.clear();
            readMetalSet(tag.getList("FeruchemicalPowers", 8), feruchemicalPowers);
        }
        if (tag.contains("SpikedAllomanticPowers")) {
            spikedAllomanticPowers.clear();
            readMetalSet(tag.getList("SpikedAllomanticPowers", 8), spikedAllomanticPowers);
        }
        if (tag.contains("Burning")) {
            burningMetals.clear();
            readMetalSet(tag.getList("Burning", 8), burningMetals);
        }
        if (tag.contains("Flaring")) {
            flaringMetals.clear();
            readMetalSet(tag.getList("Flaring", 8), flaringMetals);
        }

        if (tag.contains("Reserves")) {
            CompoundTag reserveTag = tag.getCompound("Reserves");
            for (String key : reserveTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> reserves.put(m, reserveTag.getFloat(key)));
            }
        }
        if (tag.contains("Metalminds")) {
            CompoundTag metalmindTag = tag.getCompound("Metalminds");
            for (String key : metalmindTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> metalmindCharges.put(m, metalmindTag.getFloat(key)));
            }
        }
        if (tag.contains("FeruchemyModes")) {
            CompoundTag modeTag = tag.getCompound("FeruchemyModes");
            for (String key : modeTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> feruchemyModes.put(m, modeTag.getInt(key)));
            }
        }

        if (tag.contains("InstalledSpikes")) {
            installedSpikes.clear();
            ListTag spikeList = tag.getList("InstalledSpikes", 10);
            for (int i = 0; i < spikeList.size(); i++) {
                InstalledSpike.deserializeNBT(spikeList.getCompound(i)).ifPresent(installedSpikes::add);
            }
        }

        if (tag.contains("EquippedStrengths")) {
            CompoundTag strengthTag = tag.getCompound("EquippedStrengths");
            equippedAllomanticStrengths.replaceAll((m, v) -> 0F);
            for (String key : strengthTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> equippedAllomanticStrengths.put(m, strengthTag.getFloat(key)));
            }
        }

        if (tag.contains("NaturalAllomanticStrengths")) {
            CompoundTag naturalStrengthTag = tag.getCompound("NaturalAllomanticStrengths");
            naturalAllomanticStrengths.replaceAll((m, v) -> 0F);
            for (String key : naturalStrengthTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> naturalAllomanticStrengths.put(m, naturalStrengthTag.getFloat(key)));
            }
        }

        if (tag.contains("SelectedMetal")) Metal.byName(tag.getString("SelectedMetal")).ifPresent(value -> selectedMetal = value);
        if (tag.contains("Corruption")) corruption = tag.getInt("Corruption");
        if (tag.contains("EquippedSpikeCorruption")) equippedSpikeCorruption = tag.getInt("EquippedSpikeCorruption");
        if (tag.contains("DuraluminCooldown")) duraluminCooldown = tag.getInt("DuraluminCooldown");
        if (tag.contains("BubbleCooldown")) bubbleCooldown = tag.getInt("BubbleCooldown");
        if (tag.contains("WellTouched")) wellTouched = tag.getBoolean("WellTouched");
        if (tag.contains("FirstJoinRollComplete")) firstJoinRollComplete = tag.getBoolean("FirstJoinRollComplete");
        if (tag.contains("BronzePulseYaw")) bronzePulseYaw = tag.getFloat("BronzePulseYaw");
        if (tag.contains("BronzePulseStrength")) bronzePulseStrength = tag.getFloat("BronzePulseStrength");
        if (tag.contains("BronzePulseDistance")) bronzePulseDistance = tag.getFloat("BronzePulseDistance");
        if (tag.contains("BronzePulseMetal")) bronzePulseMetal = tag.getString("BronzePulseMetal");
        if (tag.contains("Copperclouded")) copperclouded = tag.getBoolean("Copperclouded");
        if (tag.contains("AllomanticStrength")) allomanticStrength = tag.getFloat("AllomanticStrength");
        if (tag.contains("PewterDragTicks")) pewterDragTicks = tag.getInt("PewterDragTicks");
        if (tag.contains("PewterBurnDuration")) pewterBurnDuration = tag.getInt("PewterBurnDuration");
        if (tag.contains("AllomancySnapped")) {
            allomancySnapped = tag.getBoolean("AllomancySnapped");
        } else {
            allomancySnapped = true;
        }
        if (tag.contains("Restrained")) restrained = tag.getBoolean("Restrained");
        if (tag.contains("RestrainedAltarPos")) restrainedAltarPos = net.minecraft.core.BlockPos.of(tag.getLong("RestrainedAltarPos"));
        if (tag.contains("AltarSeatIndex")) altarSeatIndex = tag.getInt("AltarSeatIndex");
        if (tag.contains("RestrainedByOthers")) restrainedByOthers = tag.getBoolean("RestrainedByOthers");

        // ── Expanded Systems ──
        if (tag.contains("SoulStability")) soulStability = tag.getFloat("SoulStability");
        if (tag.contains("HasLinchpinSpike")) hasLinchpinSpike = tag.getBoolean("HasLinchpinSpike");
        if (tag.contains("LinchpinSpikeIndex")) linchpinSpikeIndex = tag.getInt("LinchpinSpikeIndex");
        if (tag.contains("SpiritualScarring")) spiritualScarring = tag.getFloat("SpiritualScarring");
        if (tag.contains("IdentityContamination")) identityContamination = tag.getFloat("IdentityContamination");
        if (tag.contains("ContaminationStage")) contaminationStage = tag.getInt("ContaminationStage");
        if (tag.contains("SpiritualBloat")) spiritualBloat = tag.getFloat("SpiritualBloat");
        if (tag.contains("ForcedSystemCount")) forcedSystemCount = tag.getInt("ForcedSystemCount");
        if (tag.contains("LerasiumEnhancements")) lerasiumEnhancements = tag.getInt("LerasiumEnhancements");
        if (tag.contains("LerasiumBonus")) lerasiumBonus = tag.getFloat("LerasiumBonus");

        if (tag.contains("SavantProgress")) {
            CompoundTag spTag = tag.getCompound("SavantProgress");
            for (String key : spTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> savantProgress.put(m, spTag.getFloat(key)));
            }
        }
        if (tag.contains("SavantStage")) {
            CompoundTag ssTag = tag.getCompound("SavantStage");
            for (String key : ssTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> savantStage.put(m, ssTag.getInt(key)));
            }
        }
        if (tag.contains("SavantLastBurned")) {
            CompoundTag lbTag = tag.getCompound("SavantLastBurned");
            for (String key : lbTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> savantLastBurned.put(m, lbTag.getLong(key)));
            }
        }
        if (tag.contains("LerasiumAlloyBonuses")) {
            CompoundTag abTag = tag.getCompound("LerasiumAlloyBonuses");
            for (String key : abTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> lerasiumAlloyBonuses.put(m, abTag.getFloat(key)));
            }
        }
        if (tag.contains("LerasatiumAlloyBonuses")) {
            CompoundTag labTag = tag.getCompound("LerasatiumAlloyBonuses");
            for (String key : labTag.getAllKeys()) {
                Metal.byName(key).ifPresent(m -> lerasatiumAlloyBonuses.put(m, labTag.getFloat(key)));
            }
        }

        stopInvalidBurns();
        feruchemyModes.replaceAll((metal, mode) -> hasFeruchemicalPower(metal) ? mode : 0);

        if (!full) {
            this.needsPowerRefresh = false;
        }
    }

    public float allomanticStrength() {
        return allomanticStrength;
    }

    public void setAllomanticStrength(float allomanticStrength) {
        this.allomanticStrength = Math.min(1.0F, Math.max(0.0F, allomanticStrength));
    }

    public float getEffectiveStrength(Metal metal) {
        boolean hasNatural = naturalAllomanticPowers.contains(metal);
        float base = 0.0F;

        if (hasNatural) {
            base = naturalAllomanticStrengths.getOrDefault(metal, 0.0F) + lerasiumBonus
                    + lerasiumAlloyBonuses.getOrDefault(metal, 0.0F);
        }

        // Collect all duplicate spike strengths for this metal
        List<Float> duplicateStrengths = new ArrayList<>();
        for (InstalledSpike spike : installedSpikes) {
            if ("allomancy".equals(spike.powerType()) && spike.powerMetal() == metal) {
                duplicateStrengths.add(spike.strength());
            }
        }
        float equippedStr = equippedAllomanticStrengths.getOrDefault(metal, 0F);
        if (equippedStr > 0) duplicateStrengths.add(equippedStr);

        // Sort descending so strongest spikes are processed first
        duplicateStrengths.sort(Collections.reverseOrder());

        if (hasNatural) {
            // All spikes are additions to the natural base (100%, 80%, 60%, 40%...)
            float[] factors = {1.00F, 0.80F, 0.60F, 0.40F, 0.25F, 0.15F};
            for (int i = 0; i < duplicateStrengths.size(); i++) {
                float factor = i < factors.length ? factors[i] : 0.08F;
                base += duplicateStrengths.get(i) * factor;
            }
        } else {
            // First/strongest spike acts as the full base power (100% efficiency)
            // Subsequent spikes are additions (80%, 60%, 40%...)
            float[] factors = {1.00F, 0.80F, 0.60F, 0.40F, 0.25F, 0.15F};
            for (int i = 0; i < duplicateStrengths.size(); i++) {
                float factor = i < factors.length ? factors[i] : 0.08F;
                base += duplicateStrengths.get(i) * factor;
            }
        }

        // Savant bonus
        base += getSavantBonus(metal);

        return base;
    }

    public float getEffectiveStrength() {
        return allomanticStrength;
    }

    public int pewterDragTicks() {
        return pewterDragTicks;
    }

    public void setPewterDragTicks(int pewterDragTicks) {
        this.pewterDragTicks = Math.max(0, pewterDragTicks);
    }

    public int pewterBurnDuration() {
        return pewterBurnDuration;
    }

    public void setPewterBurnDuration(int pewterBurnDuration) {
        this.pewterBurnDuration = Math.max(0, pewterBurnDuration);
    }

    public void copyFrom(MetalArtsData other) {
        deserializeNBT(other.serializeNBT());
    }

    private void stopInvalidBurns() {
        burningMetals.removeIf(metal -> !allomanticPowers.contains(metal) || getReserve(metal) <= 0F || !ServerConfig.isMetalEnabled(metal));
        flaringMetals.removeIf(metal -> !burningMetals.contains(metal));
    }

    private ListTag metalSet(Set<Metal> metals) {
        ListTag list = new ListTag();
        for (Metal metal : metals) {
            list.add(StringTag.valueOf(metal.id()));
        }
        return list;
    }

    private void readMetalSet(ListTag list, EnumSet<Metal> target) {
        for (int i = 0; i < list.size(); i++) {
            Metal.byName(list.getString(i)).ifPresent(target::add);
        }
    }

    public record InstalledSpike(Metal spikeMetal, String powerType, Metal powerMetal, float strength, int decayTicks, float feruchemicalCharge, String identityKey, CompoundTag stolenSpiritWeb) {
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("SpikeMetal", spikeMetal.id());
            tag.putString("PowerType", powerType == null ? "allomancy" : powerType);
            tag.putString("PowerMetal", powerMetal.id());
            tag.putFloat("Strength", strength);
            tag.putInt("DecayTicks", decayTicks);
            tag.putFloat("FeruchemicalCharge", feruchemicalCharge);
            tag.putString("IdentityKey", identityKey == null ? "" : identityKey);
            tag.put("StolenSpiritWeb", stolenSpiritWeb == null ? new CompoundTag() : stolenSpiritWeb);
            return tag;
        }

        public static java.util.Optional<InstalledSpike> deserializeNBT(CompoundTag tag) {
            java.util.Optional<Metal> spikeMetal = Metal.byName(tag.getString("SpikeMetal"));
            java.util.Optional<Metal> powerMetal = Metal.byName(tag.getString("PowerMetal"));
            if (spikeMetal.isEmpty() || powerMetal.isEmpty()) {
                return java.util.Optional.empty();
            }
            String type = tag.getString("PowerType");
            if (type.isBlank()) {
                type = "allomancy";
            }
            float charge = tag.contains("FeruchemicalCharge") ? tag.getFloat("FeruchemicalCharge") : 0.0F;
            String identityKey = tag.getString("IdentityKey");
            if (identityKey.isBlank()) {
                identityKey = com.not_noah.mistborn_metal_arts.api.SpiritualAttributes.generateIdentity();
            }
            CompoundTag stolenSpiritWeb = tag.contains("StolenSpiritWeb") ? tag.getCompound("StolenSpiritWeb") : new CompoundTag();
            return java.util.Optional.of(new InstalledSpike(spikeMetal.get(), type, powerMetal.get(), tag.getFloat("Strength"), tag.getInt("DecayTicks"), charge, identityKey, stolenSpiritWeb));
        }
    }

    public boolean isRestrained() {
        return restrained;
    }

    public void setRestrained(boolean restrained, net.minecraft.core.BlockPos pos, int seatIndex) {
        this.restrained = restrained;
        this.restrainedAltarPos = pos != null ? pos : net.minecraft.core.BlockPos.ZERO;
        this.altarSeatIndex = seatIndex;
        if (!restrained) {
            this.restrainedByOthers = false;
        }
    }

    public boolean isRestrainedByOthers() {
        return restrainedByOthers;
    }

    public void setRestrainedByOthers(boolean restrainedByOthers) {
        this.restrainedByOthers = restrainedByOthers;
    }

    public net.minecraft.core.BlockPos getRestrainedAltarPos() {
        return restrainedAltarPos;
    }

    public int getAltarSeatIndex() {
        return altarSeatIndex;
    }

    public boolean hasPowerToSteal(Metal metal, String type) {
        if ("feruchemy".equals(type)) {
            return naturalFeruchemicalPowers.contains(metal);
        } else {
            return naturalAllomanticPowers.contains(metal);
        }
    }

    public boolean stealSpecificPower(Metal metal, String type) {
        if ("feruchemy".equals(type)) {
            if (naturalFeruchemicalPowers.remove(metal)) {
                if (entity != null) {
                    entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                        com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("feruchemy");
                        if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy) {
                            feruchemy.setPower(metal, 0.0F);
                        }
                    });
                }
                markNeedsPowerRefresh();
                return true;
            }
        } else {
            if (naturalAllomanticPowers.remove(metal)) {
                if (entity != null) {
                    entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                        com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("allomancy");
                        if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Allomancy allomancy) {
                            allomancy.setPower(metal, 0.0F);
                        }
                    });
                }
                markNeedsPowerRefresh();
                return true;
            }
        }
        return false;
    }

    // ═══════════════════════════════════════════════
    // ── Soul Stability ──
    // ═══════════════════════════════════════════════

    public float soulStability() {
        return soulStability;
    }

    public void setSoulStability(float value) {
        this.soulStability = Math.max(0.0F, Math.min(100.0F, value));
    }

    public float spiritualScarring() {
        return spiritualScarring;
    }

    public void setSpiritualScarring(float value) {
        this.spiritualScarring = Math.max(0.0F, value);
    }

    public boolean hasLinchpinSpike() {
        return hasLinchpinSpike;
    }

    public int linchpinSpikeIndex() {
        return linchpinSpikeIndex;
    }

    public void setLinchpinSpike(int index) {
        if (index >= 0 && index < installedSpikes.size()) {
            this.hasLinchpinSpike = true;
            this.linchpinSpikeIndex = index;
            com.not_noah.mistborn_metal_arts.hemalurgy.SoulStabilityManager.recalculateStability(this);
        }
    }

    public void clearLinchpinSpike() {
        this.hasLinchpinSpike = false;
        this.linchpinSpikeIndex = -1;
        com.not_noah.mistborn_metal_arts.hemalurgy.SoulStabilityManager.recalculateStability(this);
    }

    // ═══════════════════════════════════════════════
    // ── Identity Contamination ──
    // ═══════════════════════════════════════════════

    public float identityContamination() {
        return identityContamination;
    }

    public void setIdentityContamination(float value) {
        this.identityContamination = Math.max(0.0F, Math.min(100.0F, value));
        updateContaminationStage();
        com.not_noah.mistborn_metal_arts.hemalurgy.SoulStabilityManager.recalculateStability(this);
    }

    public void addIdentityContamination(float amount) {
        setIdentityContamination(identityContamination + amount);
    }

    public void reduceIdentityContamination(float amount) {
        setIdentityContamination(identityContamination - amount);
    }

    public int contaminationStage() {
        return contaminationStage;
    }

    private void updateContaminationStage() {
        if (identityContamination >= 85.0F) contaminationStage = 4;
        else if (identityContamination >= 60.0F) contaminationStage = 3;
        else if (identityContamination >= 40.0F) contaminationStage = 2;
        else if (identityContamination >= 20.0F) contaminationStage = 1;
        else contaminationStage = 0;
    }

    // ═══════════════════════════════════════════════
    // ── Savantism ──
    // ═══════════════════════════════════════════════

    public float savantProgress(Metal metal) {
        return savantProgress.getOrDefault(metal, 0.0F);
    }

    public void setSavantProgress(Metal metal, float value) {
        savantProgress.put(metal, Math.max(0.0F, Math.min(1.0F, value)));
        updateSavantStage(metal);
    }

    public void addSavantProgress(Metal metal, float amount) {
        setSavantProgress(metal, savantProgress(metal) + amount);
    }

    public int savantStage(Metal metal) {
        return savantStage.getOrDefault(metal, 0);
    }

    public long savantLastBurned(Metal metal) {
        return savantLastBurned.getOrDefault(metal, 0L);
    }

    public void setSavantLastBurned(Metal metal, long value) {
        savantLastBurned.put(metal, value);
    }

    private void updateSavantStage(Metal metal) {
        float progress = savantProgress(metal);
        int stage;
        if (progress >= 0.95F) stage = 4;
        else if (progress >= 0.75F) stage = 3;
        else if (progress >= 0.50F) stage = 2;
        else if (progress >= 0.25F) stage = 1;
        else stage = 0;
        savantStage.put(metal, stage);
    }

    public float getSavantBonus(Metal metal) {
        return switch (savantStage(metal)) {
            case 1 -> 0.05F;
            case 2 -> 0.12F;
            case 3 -> 0.25F;
            case 4 -> 0.45F;
            default -> 0.0F;
        };
    }

    public float getSavantEfficiency(Metal metal) {
        return switch (savantStage(metal)) {
            case 1 -> 0.05F;
            case 2 -> 0.10F;
            case 3 -> 0.20F;
            case 4 -> 0.30F;
            default -> 0.0F;
        };
    }

    public int getHighestSavantStage() {
        int max = 0;
        for (int stage : savantStage.values()) {
            if (stage > max) max = stage;
        }
        return max;
    }

    public Metal getHighestSavantMetal() {
        Metal best = null;
        int maxStage = 0;
        float maxProgress = 0;
        for (Metal m : Metal.cachedValues()) {
            int stage = savantStage(m);
            float progress = savantProgress(m);
            if (stage > maxStage || (stage == maxStage && progress > maxProgress)) {
                maxStage = stage;
                maxProgress = progress;
                best = m;
            }
        }
        return best;
    }

    // ═══════════════════════════════════════════════
    // ── Spiritual Bloat ──
    // ═══════════════════════════════════════════════

    public float spiritualBloat() {
        return spiritualBloat;
    }

    public void setSpiritualBloat(float value) {
        this.spiritualBloat = Math.max(0.0F, Math.min(100.0F, value));
        com.not_noah.mistborn_metal_arts.hemalurgy.SoulStabilityManager.recalculateStability(this);
    }

    public void addSpiritualBloat(float amount) {
        setSpiritualBloat(spiritualBloat + amount);
    }

    public int forcedSystemCount() {
        return forcedSystemCount;
    }

    public void setForcedSystemCount(int count) {
        this.forcedSystemCount = Math.max(0, count);
    }

    // ═══════════════════════════════════════════════
    // ── Lerasium Enhancement ──
    // ═══════════════════════════════════════════════

    public int lerasiumEnhancements() {
        return lerasiumEnhancements;
    }

    public void setLerasiumEnhancements(int value) {
        this.lerasiumEnhancements = Math.max(0, value);
    }

    public float lerasiumBonus() {
        return lerasiumBonus;
    }

    public void setLerasiumBonus(float value) {
        this.lerasiumBonus = Math.max(0.0F, Math.min(2.0F, value));
    }

    public float getLerasiumAlloyBonus(Metal metal) {
        return lerasiumAlloyBonuses.getOrDefault(metal, 0.0F);
    }

    public void addLerasiumAlloyBonus(Metal metal, float amount) {
        lerasiumAlloyBonuses.put(metal,
                lerasiumAlloyBonuses.getOrDefault(metal, 0.0F) + amount);
    }

    public float getLerasatiumAlloyBonus(Metal metal) {
        return lerasatiumAlloyBonuses.getOrDefault(metal, 0.0F);
    }

    public void addLerasatiumAlloyBonus(Metal metal, float amount) {
        lerasatiumAlloyBonuses.put(metal,
                lerasatiumAlloyBonuses.getOrDefault(metal, 0.0F) + amount);
    }

    public void addNaturalFeruchemicalPower(Metal metal) {
        if (metal.isFeruchemical()) {
            naturalFeruchemicalPowers.add(metal);
            if (entity != null) {
                entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                    com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("feruchemy");
                    if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Feruchemy feruchemy) {
                        feruchemy.setPower(metal, 1.0F);
                    }
                });
            }
            markNeedsPowerRefresh();
        }
    }

    public void addNaturalAllomanticPower(Metal metal) {
        if (metal.isAllomantic()) {
            naturalAllomanticPowers.add(metal);
            if (entity != null) {
                entity.getCapability(MetalArtsCapabilities.SPIRIT_WEB).ifPresent(web -> {
                    com.not_noah.mistborn_metal_arts.api.InvestedArt naturalArt = web.getInvestedSystems().get("allomancy");
                    if (naturalArt instanceof com.not_noah.mistborn_metal_arts.api.Allomancy allomancy) {
                        if (allomancy.getPower(metal) <= 0F) {
                            allomancy.setPower(metal, 0.5F);
                        }
                    }
                });
            }
            markNeedsPowerRefresh();
        }
    }

    public int countDuplicateSpikes(Metal metal, String powerType) {
        int count = 0;
        for (InstalledSpike spike : installedSpikes) {
            if (powerType.equals(spike.powerType()) && spike.powerMetal() == metal) {
                count++;
            }
        }
        return count;
    }
}
