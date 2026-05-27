package com.not_noah.mistborn_metal_arts.api;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Metal {
    IRON("iron", "Iron", false, true, true),
    STEEL("steel", "Steel", false, true, true),
    TIN("tin", "Tin", false, true, true),
    PEWTER("pewter", "Pewter", false, true, true),
    ZINC("zinc", "Zinc", false, true, true),
    BRASS("brass", "Brass", false, true, true),
    COPPER("copper", "Copper", false, true, true),
    BRONZE("bronze", "Bronze", false, true, true),
    GOLD("gold", "Gold", false, true, true),
    ELECTRUM("electrum", "Electrum", false, true, true),
    CADMIUM("cadmium", "Cadmium", false, true, true),
    BENDALLOY("bendalloy", "Bendalloy", false, true, true),
    ALUMINUM("aluminum", "Aluminum", false, true, true),
    DURALUMIN("duralumin", "Duralumin", false, true, true),
    CHROMIUM("chromium", "Chromium", false, true, true),
    NICROSIL("nicrosil", "Nicrosil", false, true, true),
    ATIUM("atium", "Atium", true, true, false),
    LERASIUM("lerasium", "Lerasium", true, false, false),
    LEAD("lead", "Lead", false, true, true),
    SILVER("silver", "Silver", false, true, true),
    NICKEL("nickel", "Nickel", false, true, true),
    HARMONIUM("harmonium", "Harmonium", true, true, false),
    MALATIUM("malatium", "Malatium", true, true, false),
    LERASATIUM("lerasatium", "Lerasatium", true, false, false);

    private final String id;
    private final String displayName;
    private final boolean godMetal;
    private final boolean allomantic;
    private final boolean feruchemical;

    private static final java.util.Map<String, Metal> BY_ID = new java.util.HashMap<>();
    private static final Metal[] CACHED_VALUES = values();

    static {
        for (Metal metal : CACHED_VALUES) {
            BY_ID.put(metal.id, metal);
        }
    }

    Metal(String id, String displayName, boolean godMetal, boolean allomantic, boolean feruchemical) {
        this.id = id;
        this.displayName = displayName;
        this.godMetal = godMetal;
        this.allomantic = allomantic;
        this.feruchemical = feruchemical;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isGodMetal() {
        return godMetal;
    }

    public boolean isAllomantic() {
        return allomantic;
    }

    public boolean isFeruchemical() {
        return feruchemical;
    }

    public String translationKey(String prefix) {
        return prefix + "." + id;
    }

    public static Metal[] cachedValues() {
        return CACHED_VALUES;
    }

    public static Optional<Metal> byName(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(name.toLowerCase(Locale.ROOT)));
    }
}
