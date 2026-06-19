package com.not_noah.mistborn_metal_arts.api;

import java.util.Map;
import java.util.HashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import com.not_noah.mistborn_metal_arts.capability.MetalArtsData;
import com.not_noah.mistborn_metal_arts.compat.CuriosCompat;

public class SpiritWeb {
    private boolean allomancySnapped = true;
    // Entity Linking
    public String entityUUID;

    // Properties that the SpiritWeb dictates (sort of sDNA)
    public PhysicalAttributes physicalAttributes;
    public CognitiveAttributes cognitiveAttributes;
    public SpiritualAttributes spiritualAttributes;
    public boolean baseAttributesInitialized = false;

    // Soul Fragment Storage
    public Map<String, SpiritWeb> soulFragments = new HashMap<>();

    // What Invested Arts the SpiritWeb has access to
    private Map<String, InvestedArt> investedSystems = new HashMap<>();

    public SpiritWeb() {
        this.physicalAttributes = new PhysicalAttributes();
        this.cognitiveAttributes = new CognitiveAttributes();
        this.spiritualAttributes = new SpiritualAttributes();
        this.spiritualAttributes.identity = SpiritualAttributes.generateIdentity();
        this.investedSystems.put("allomancy", new Allomancy());
        this.investedSystems.put("feruchemy", new Feruchemy());
        this.investedSystems.put("hemalurgy", new Hemalurgy());
    }

    public Map<String, InvestedArt> getInvestedSystems() {
        return investedSystems;
    }

    private void mergeSoulFragments(SpiritWeb a, SpiritWeb b) {
        if (a.physicalAttributes != null && b.physicalAttributes != null) {
            a.physicalAttributes.merge(b.physicalAttributes);
        }
        if (a.cognitiveAttributes != null && b.cognitiveAttributes != null) {
            a.cognitiveAttributes.merge(b.cognitiveAttributes);
        }
        if (a.spiritualAttributes != null && b.spiritualAttributes != null) {
            a.spiritualAttributes.merge(b.spiritualAttributes);
        }
    }

    public String getIdentityKey() {
        if (this.spiritualAttributes != null)
            return this.spiritualAttributes.identity;
        return null;
    }

    public void imposeForeignSoulFragment(SpiritWeb fragment) {
        if (fragment == null || fragment.spiritualAttributes == null)
            return;

        String key = fragment.getIdentityKey();
        if (this.soulFragments.containsKey(key)) {
            // Update the soul fragment with the new soul pieces
            SpiritWeb old = this.soulFragments.get(key);
            mergeSoulFragments(old, fragment);
        } else {
            this.soulFragments.put(key, fragment);
        }
        this.calculateNewIdentityContamination();
    }

    public void removeForeignSoulFragment(String identityKey) {
        if (this.soulFragments.containsKey(identityKey)) {
            this.soulFragments.remove(identityKey);
            this.calculateNewIdentityContamination();
        }
    }

    private void calculateNewIdentityContamination() {
        float identitySum = 0;
        int fragmentCount = 0;

        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment != null && fragment.spiritualAttributes != null) {
                identitySum += fragment.spiritualAttributes.contamination;
                fragmentCount++;
            }
        }

        if (fragmentCount > 0 && this.spiritualAttributes != null) {
            this.spiritualAttributes.contamination = identitySum / fragmentCount;
        }
    }

    public net.minecraft.nbt.CompoundTag serializeNBT() {
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        nbt.putBoolean("AllomancySnapped", allomancySnapped);
        nbt.putBoolean("BaseAttributesInitialized", baseAttributesInitialized);
        if (entityUUID != null) {
            nbt.putString("EntityUUID", entityUUID);
        }
        
        if (physicalAttributes != null) {
            net.minecraft.nbt.CompoundTag phys = new net.minecraft.nbt.CompoundTag();
            phys.putFloat("strength", physicalAttributes.strength);
            phys.putFloat("sight", physicalAttributes.sight);
            phys.putFloat("zoom", physicalAttributes.zoom);
            phys.putFloat("speed", physicalAttributes.speed);
            phys.putFloat("resistance", physicalAttributes.resistance);
            phys.putFloat("weight", physicalAttributes.weight);
            phys.putFloat("health", physicalAttributes.health);
            nbt.put("PhysicalAttributes", phys);
        }
        
        if (cognitiveAttributes != null) {
            net.minecraft.nbt.CompoundTag cogn = new net.minecraft.nbt.CompoundTag();
            cogn.putFloat("mentalSpeed", cognitiveAttributes.mentalSpeed);
            cogn.putFloat("wakefulness", cognitiveAttributes.wakefulness);
            cogn.putFloat("determination", cognitiveAttributes.determination);
            cogn.putFloat("intelligence", cognitiveAttributes.intelligence);
            nbt.put("CognitiveAttributes", cogn);
        }
        
        if (spiritualAttributes != null) {
            net.minecraft.nbt.CompoundTag spir = new net.minecraft.nbt.CompoundTag();
            if (spiritualAttributes.identity != null) {
                spir.putString("identity", spiritualAttributes.identity);
            }
            spir.putFloat("stability", spiritualAttributes.stability);
            spir.putFloat("contamination", spiritualAttributes.contamination);
            spir.putFloat("scarring", spiritualAttributes.scarring);
            
            net.minecraft.nbt.CompoundTag conns = new net.minecraft.nbt.CompoundTag();
            if (spiritualAttributes.getConnections() != null) {
                spiritualAttributes.getConnections().forEach(conns::putFloat);
            }
            spir.put("connections", conns);
            nbt.put("SpiritualAttributes", spir);
        }
        
        // Soul fragments Map<String, SpiritWeb>
        net.minecraft.nbt.CompoundTag fragments = new net.minecraft.nbt.CompoundTag();
        soulFragments.forEach((k, v) -> {
            if (v != null) {
                fragments.put(k, v.serializeNBT());
            }
        });
        nbt.put("SoulFragments", fragments);
        
        // Map<String, InvestedArt> investedSystems
        net.minecraft.nbt.CompoundTag systems = new net.minecraft.nbt.CompoundTag();
        investedSystems.forEach((k, v) -> {
            if (v != null) {
                systems.put(k, v.serializeNBT());
            }
        });
        nbt.put("InvestedSystems", systems);
        
        return nbt;
    }

    public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
        if (nbt.contains("AllomancySnapped")) {
            allomancySnapped = nbt.getBoolean("AllomancySnapped");
        } else {
            allomancySnapped = true;
        }
        if (nbt.contains("BaseAttributesInitialized")) {
            baseAttributesInitialized = nbt.getBoolean("BaseAttributesInitialized");
        }
        if (nbt.contains("EntityUUID", 8)) {
            entityUUID = nbt.getString("EntityUUID");
        }
        
        if (nbt.contains("PhysicalAttributes", 10)) {
            net.minecraft.nbt.CompoundTag phys = nbt.getCompound("PhysicalAttributes");
            if (physicalAttributes == null) physicalAttributes = new PhysicalAttributes();
            if (phys.contains("strength")) physicalAttributes.strength = phys.getFloat("strength");
            if (phys.contains("sight")) physicalAttributes.sight = phys.getFloat("sight");
            if (phys.contains("zoom")) physicalAttributes.zoom = phys.getFloat("zoom");
            if (phys.contains("speed")) physicalAttributes.speed = phys.getFloat("speed");
            if (phys.contains("resistance")) physicalAttributes.resistance = phys.getFloat("resistance");
            if (phys.contains("weight")) physicalAttributes.weight = phys.getFloat("weight");
            if (phys.contains("health")) physicalAttributes.health = phys.getFloat("health");
        }
        
        if (nbt.contains("CognitiveAttributes", 10)) {
            net.minecraft.nbt.CompoundTag cogn = nbt.getCompound("CognitiveAttributes");
            if (cognitiveAttributes == null) cognitiveAttributes = new CognitiveAttributes();
            cognitiveAttributes.mentalSpeed = cogn.getFloat("mentalSpeed");
            cognitiveAttributes.wakefulness = cogn.getFloat("wakefulness");
            cognitiveAttributes.determination = cogn.getFloat("determination");
            cognitiveAttributes.intelligence = cogn.getFloat("intelligence");
        }
        
        if (nbt.contains("SpiritualAttributes", 10)) {
            net.minecraft.nbt.CompoundTag spir = nbt.getCompound("SpiritualAttributes");
            if (spiritualAttributes == null) spiritualAttributes = new SpiritualAttributes();
            if (spir.contains("identity", 8)) {
                spiritualAttributes.identity = spir.getString("identity");
            }
            spiritualAttributes.stability = spir.getFloat("stability");
            spiritualAttributes.contamination = spir.getFloat("contamination");
            spiritualAttributes.scarring = spir.getFloat("scarring");
            
            if (spir.contains("connections", 10)) {
                net.minecraft.nbt.CompoundTag conns = spir.getCompound("connections");
                spiritualAttributes.getConnections().clear();
                for (String key : conns.getAllKeys()) {
                    spiritualAttributes.getConnections().put(key, conns.getFloat(key));
                }
            }
        }
        
        if (nbt.contains("SoulFragments", 10)) {
            net.minecraft.nbt.CompoundTag fragments = nbt.getCompound("SoulFragments");
            soulFragments.clear();
            for (String key : fragments.getAllKeys()) {
                SpiritWeb fragment = new SpiritWeb();
                fragment.deserializeNBT(fragments.getCompound(key));
                soulFragments.put(key, fragment);
            }
        }
        
        if (nbt.contains("InvestedSystems", 10)) {
            net.minecraft.nbt.CompoundTag systems = nbt.getCompound("InvestedSystems");
            investedSystems.clear();
            for (String key : systems.getAllKeys()) {
                net.minecraft.nbt.CompoundTag sysNbt = systems.getCompound(key);
                if (key.equals("allomancy")) {
                    Allomancy allomancy = new Allomancy();
                    allomancy.deserializeNBT(sysNbt);
                    investedSystems.put(key, allomancy);
                } else if (key.equals("feruchemy")) {
                    Feruchemy feruchemy = new Feruchemy();
                    feruchemy.deserializeNBT(sysNbt);
                    investedSystems.put(key, feruchemy);
                } else if (key.equals("hemalurgy")) {
                    Hemalurgy hemalurgy = new Hemalurgy();
                    hemalurgy.deserializeNBT(sysNbt);
                    investedSystems.put(key, hemalurgy);
                } else {
                    InvestedArt art = new InvestedArt(new Shard[0]);
                    art.deserializeNBT(sysNbt);
                    investedSystems.put(key, art);
                }
            }
        }
        investedSystems.putIfAbsent("allomancy", new Allomancy());
        investedSystems.putIfAbsent("feruchemy", new Feruchemy());
        investedSystems.putIfAbsent("hemalurgy", new Hemalurgy());
    }

    public float getAllomanticStrength(Metal metal) {
        float total = 0F;
        if (allomancySnapped()) {
            InvestedArt naturalArt = investedSystems.get("allomancy");
            if (naturalArt instanceof Allomancy allomancy) {
                total += allomancy.getPower(metal);
            }
        }
        for (SpiritWeb fragment : soulFragments.values()) {
            InvestedArt fragArt = fragment.getInvestedSystems().get("allomancy");
            if (fragArt instanceof Allomancy allomancy) {
                total += allomancy.getPower(metal);
            }
        }
        return total;
    }

    public float getFeruchemicalStrength(Metal metal) {
        float total = 0F;
        InvestedArt naturalArt = investedSystems.get("feruchemy");
        if (naturalArt instanceof Feruchemy feruchemy) {
            total += feruchemy.getPower(metal);
        }
        for (SpiritWeb fragment : soulFragments.values()) {
            InvestedArt fragArt = fragment.getInvestedSystems().get("feruchemy");
            if (fragArt instanceof Feruchemy feruchemy) {
                total += feruchemy.getPower(metal);
            }
        }
        return total;
    }

    public float getTotalStrength() {
        float total = physicalAttributes != null ? physicalAttributes.strength : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.physicalAttributes != null) {
                total += fragment.physicalAttributes.strength;
            }
        }
        return total;
    }

    public float getTotalSight() {
        float total = physicalAttributes != null ? physicalAttributes.sight : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.physicalAttributes != null) {
                total += fragment.physicalAttributes.sight;
            }
        }
        return total;
    }

    public float getTotalZoom() {
        float total = physicalAttributes != null ? physicalAttributes.zoom : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.physicalAttributes != null) {
                total += fragment.physicalAttributes.zoom;
            }
        }
        return total;
    }

    public float getTotalSpeed() {
        float total = physicalAttributes != null ? physicalAttributes.speed : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.physicalAttributes != null) {
                total += fragment.physicalAttributes.speed;
            }
        }
        return total;
    }

    public float getTotalResistance() {
        float total = physicalAttributes != null ? physicalAttributes.resistance : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.physicalAttributes != null) {
                total += fragment.physicalAttributes.resistance;
            }
        }
        return total;
    }

    public float getTotalWeight() {
        float total = physicalAttributes != null ? physicalAttributes.weight : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.physicalAttributes != null) {
                total += fragment.physicalAttributes.weight;
            }
        }
        return total;
    }

    public float getTotalHealth() {
        float total = physicalAttributes != null ? physicalAttributes.health : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.physicalAttributes != null) {
                total += fragment.physicalAttributes.health;
            }
        }
        return total;
    }

    public float getTotalMentalSpeed() {
        float total = cognitiveAttributes != null ? cognitiveAttributes.mentalSpeed : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.cognitiveAttributes != null) {
                total += fragment.cognitiveAttributes.mentalSpeed;
            }
        }
        return total;
    }

    public float getTotalWakefulness() {
        float total = cognitiveAttributes != null ? cognitiveAttributes.wakefulness : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.cognitiveAttributes != null) {
                total += fragment.cognitiveAttributes.wakefulness;
            }
        }
        return total;
    }

    public float getTotalDetermination() {
        float total = cognitiveAttributes != null ? cognitiveAttributes.determination : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.cognitiveAttributes != null) {
                total += fragment.cognitiveAttributes.determination;
            }
        }
        return total;
    }

    public float getTotalIntelligence() {
        float total = cognitiveAttributes != null ? cognitiveAttributes.intelligence : 1.0F;
        for (SpiritWeb fragment : soulFragments.values()) {
            if (fragment.cognitiveAttributes != null) {
                total += fragment.cognitiveAttributes.intelligence;
            }
        }
        return total;
    }

    public void scaleAttributesAndPowers(float factor) {
        if (physicalAttributes != null) {
            physicalAttributes.strength *= factor;
            physicalAttributes.sight *= factor;
            physicalAttributes.zoom *= factor;
            physicalAttributes.speed *= factor;
            physicalAttributes.resistance *= factor;
            physicalAttributes.weight *= factor;
            physicalAttributes.health *= factor;
        }
        if (cognitiveAttributes != null) {
            cognitiveAttributes.mentalSpeed *= factor;
            cognitiveAttributes.wakefulness *= factor;
            cognitiveAttributes.determination *= factor;
            cognitiveAttributes.intelligence *= factor;
        }
        if (spiritualAttributes != null) {
            spiritualAttributes.stability *= factor;
            spiritualAttributes.contamination *= factor;
            spiritualAttributes.scarring *= factor;
        }
        InvestedArt allomancy = investedSystems.get("allomancy");
        if (allomancy instanceof Allomancy allom) {
            for (Metal m : Metal.cachedValues()) {
                if (m.isAllomantic()) {
                    allom.setPower(m, allom.getPower(m) * factor);
                }
            }
        }
        InvestedArt feruchemy = investedSystems.get("feruchemy");
        if (feruchemy instanceof Feruchemy feru) {
            for (Metal m : Metal.cachedValues()) {
                if (m.isFeruchemical()) {
                    feru.setPower(m, feru.getPower(m) * factor);
                }
            }
        }
    }

    public boolean allomancySnapped() {
        return allomancySnapped;
    }

    public void setAllomancySnapped(boolean snapped) {
        this.allomancySnapped = snapped;
    }

    public boolean hasAllomanticPower(Metal metal) {
        if (metal == Metal.LERASIUM) {
            return true;
        }
        return getAllomanticStrength(metal) > 0F;
    }

    public boolean hasNaturalAllomanticPower(Metal metal) {
        InvestedArt naturalArt = investedSystems.get("allomancy");
        if (naturalArt instanceof Allomancy allomancy) {
            return allomancy.getPower(metal) > 0F;
        }
        return false;
    }

    public boolean hasForeignAllomanticPower(Metal metal) {
        for (SpiritWeb fragment : soulFragments.values()) {
            InvestedArt fragArt = fragment.getInvestedSystems().get("allomancy");
            if (fragArt instanceof Allomancy allomancy) {
                if (allomancy.getPower(metal) > 0F) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasFeruchemicalPower(Metal metal) {
        return hasNaturalFeruchemicalPower(metal) || hasForeignFeruchemicalPower(metal);
    }

    public boolean hasNaturalFeruchemicalPower(Metal metal) {
        InvestedArt naturalArt = investedSystems.get("feruchemy");
        if (naturalArt instanceof Feruchemy feruchemy) {
            return feruchemy.getPower(metal) > 0F;
        }
        return false;
    }

    public boolean hasForeignFeruchemicalPower(Metal metal) {
        for (SpiritWeb fragment : soulFragments.values()) {
            InvestedArt fragArt = fragment.getInvestedSystems().get("feruchemy");
            if (fragArt instanceof Feruchemy feruchemy) {
                if (feruchemy.getPower(metal) > 0F) {
                    return true;
                }
            }
        }
        return false;
    }

    public void refreshSpikedFragments(LivingEntity entity, MetalArtsData data) {
        this.soulFragments.clear();
        
        // 1. Permanently installed spikes
        for (MetalArtsData.InstalledSpike spike : data.installedSpikes()) {
            if (spike.stolenSpiritWeb() != null && !spike.stolenSpiritWeb().isEmpty()) {
                SpiritWeb fragment = new SpiritWeb();
                fragment.deserializeNBT(spike.stolenSpiritWeb());
                fragment.scaleAttributesAndPowers(spike.strength());
                String key = fragment.getIdentityKey();
                if (key == null || key.isBlank()) {
                    key = spike.identityKey();
                    fragment.spiritualAttributes.identity = key;
                }
                this.soulFragments.put(key, fragment);
            } else {
                // Fallback for legacy spikes
                addFragmentFromSpike(spike.spikeMetal(), spike.powerType(), spike.powerMetal(), spike.strength(), spike.identityKey());
            }
        }

        // 2. Equipped Curios spikes
        if (entity instanceof ServerPlayer player) {
            for (ItemStack stack : CuriosCompat.getEquippedSpikes(player)) {
                CompoundTag tag = stack.getOrCreateTag();
                float strength = tag.contains("Strength") ? tag.getFloat("Strength") : 1.0F;
                String identity = tag.getString("SpikeIdentity");
                if (identity.isBlank()) {
                    identity = SpiritualAttributes.generateIdentity();
                    tag.putString("SpikeIdentity", identity);
                }

                if (tag.contains("StolenSpiritWeb", 10)) {
                    SpiritWeb fragment = new SpiritWeb();
                    fragment.deserializeNBT(tag.getCompound("StolenSpiritWeb"));
                    fragment.scaleAttributesAndPowers(strength);
                    String key = fragment.getIdentityKey();
                    if (key == null || key.isBlank()) {
                        key = identity;
                        fragment.spiritualAttributes.identity = key;
                    }
                    this.soulFragments.put(key, fragment);
                } else {
                    // Fallback for legacy curios
                    String powerType = tag.getString("PowerType");
                    if (powerType.isBlank()) {
                        powerType = ((com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem) stack.getItem()).metal().isFeruchemical() ? "feruchemy" : "allomancy";
                    }
                    Metal powerMetal = Metal.byName(tag.getString("PowerMetal")).orElse(((com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem) stack.getItem()).metal());
                    addFragmentFromSpike(((com.not_noah.mistborn_metal_arts.item.HemalurgicSpikeItem) stack.getItem()).metal(), powerType, powerMetal, strength, identity);
                }
            }
        }
        this.calculateNewIdentityContamination();
    }

    private void addFragmentFromSpike(Metal spikeMetal, String powerType, Metal powerMetal, float strength, String identity) {
        String key = (identity == null || identity.isBlank()) ? SpiritualAttributes.generateIdentity() : identity;
        
        SpiritWeb fragment = this.soulFragments.get(key);
        if (fragment == null) {
            fragment = new SpiritWeb();
            fragment.spiritualAttributes.identity = key;
            this.soulFragments.put(key, fragment);
        }

        // Clear default arts initialized in SpiritWeb constructor so we only have the spiked ones
        Allomancy allomancy = (Allomancy) fragment.getInvestedSystems().get("allomancy");
        for (Metal m : Metal.cachedValues()) {
            if (m.isAllomantic()) {
                allomancy.setPower(m, 0.0F);
            }
        }
        Feruchemy feruchemy = (Feruchemy) fragment.getInvestedSystems().get("feruchemy");
        for (Metal m : Metal.cachedValues()) {
            if (m.isFeruchemical()) {
                feruchemy.setPower(m, 0.0F);
            }
        }
        
        if ("allomancy".equals(powerType)) {
            allomancy.setPower(powerMetal, strength);
        } else if ("feruchemy".equals(powerType)) {
            feruchemy.setPower(powerMetal, strength);
        } else if ("physical_strength".equals(powerType)) {
            fragment.physicalAttributes.strength = strength;
        } else if ("physical_sight".equals(powerType)) {
            fragment.physicalAttributes.sight = strength;
            fragment.physicalAttributes.zoom = strength;
        } else if ("emotional_fortitude".equals(powerType)) {
            fragment.cognitiveAttributes.determination = strength;
            fragment.spiritualAttributes.stability = strength;
        } else if ("mental_fortitude".equals(powerType)) {
            fragment.cognitiveAttributes.intelligence = strength;
        } else if ("investiture".equals(powerType)) {
            fragment.spiritualAttributes.contamination = strength;
        } else if ("destiny".equals(powerType)) {
            fragment.spiritualAttributes.scarring = strength;
        } else if ("connection".equals(powerType)) {
            fragment.spiritualAttributes.identity = key;
        }
    }

    public boolean canCompound(Metal metal) {
        boolean hasNaturalAllomancy = hasNaturalAllomanticPower(metal) && allomancySnapped();
        boolean hasNaturalFeruchemy = hasNaturalFeruchemicalPower(metal);
        if (hasNaturalAllomancy && hasNaturalFeruchemy) {
            return true;
        }
        
        for (Map.Entry<String, SpiritWeb> entry : soulFragments.entrySet()) {
            SpiritWeb fragment = entry.getValue();
            boolean hasFragAllomancy = fragment.hasNaturalAllomanticPower(metal);
            boolean hasFragFeruchemy = fragment.hasNaturalFeruchemicalPower(metal);
            if (hasFragAllomancy && hasFragFeruchemy) {
                return true;
            }
        }
        
        return false;
    }
}
