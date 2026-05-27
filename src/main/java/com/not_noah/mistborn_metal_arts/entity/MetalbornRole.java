package com.not_noah.mistborn_metal_arts.entity;

public enum MetalbornRole {
    COINSHOT_BANDIT("coinshot_bandit", "Coinshot Bandit", 24.0D, 0.24D, 4.0D, 0x3f4448, 0x5fb8ff),
    LURCHER_GUARD("lurcher_guard", "Lurcher Guard", 30.0D, 0.21D, 5.0D, 0x2d3134, 0x8d9499),
    PEWTER_THUG("pewter_thug", "Pewter Thug", 38.0D, 0.23D, 7.0D, 0x625b51, 0xb7b1a8),
    TINEYE_SCOUT("tineye_scout", "Tineye Scout", 20.0D, 0.31D, 3.0D, 0x2e2f34, 0xdfe8e8),
    RIOTER("rioter", "Rioter", 24.0D, 0.24D, 3.0D, 0x3a2d2d, 0xc95442),
    SOOTHER("soother", "Soother", 24.0D, 0.23D, 3.0D, 0x31343b, 0xd2bb72),
    SEEKER("seeker", "Seeker", 26.0D, 0.25D, 4.0D, 0x3c3227, 0xb26f36),
    SMOKER("smoker", "Smoker", 28.0D, 0.22D, 4.0D, 0x2f332d, 0xb56e3d),
    ATIUM_SEER("atium_seer", "Atium Seer", 30.0D, 0.28D, 5.0D, 0x25272a, 0xb7c0c4),
    MISTBORN_ASSASSIN("mistborn_assassin", "Mistborn Assassin", 44.0D, 0.31D, 7.0D, 0x1f2228, 0x79cfff),
    KOLOSS("koloss", "Koloss", 70.0D, 0.20D, 10.0D, 0x40566f, 0xa7bfd0),
    KANDRA("kandra", "Kandra", 28.0D, 0.20D, 2.0D, 0xb7ada4, 0xd9d2c9),
    STEEL_INQUISITOR("steel_inquisitor", "Steel Inquisitor", 160.0D, 0.29D, 11.0D, 0xffffff, 0xffffff);

    private final String id;
    private final String displayName;
    private final double health;
    private final double speed;
    private final double attackDamage;
    private final int eggBaseColor;
    private final int eggHighlightColor;

    private static final java.util.Map<String, MetalbornRole> BY_ID = new java.util.HashMap<>();
    private static final MetalbornRole[] CACHED_VALUES = values();

    static {
        for (MetalbornRole role : CACHED_VALUES) {
            BY_ID.put(role.id, role);
        }
    }

    MetalbornRole(String id, String displayName, double health, double speed, double attackDamage, int eggBaseColor, int eggHighlightColor) {
        this.id = id;
        this.displayName = displayName;
        this.health = health;
        this.speed = speed;
        this.attackDamage = attackDamage;
        this.eggBaseColor = eggBaseColor;
        this.eggHighlightColor = eggHighlightColor;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public double health() {
        return health;
    }

    public double speed() {
        return speed;
    }

    public double attackDamage() {
        return attackDamage;
    }

    public int eggBaseColor() {
        return eggBaseColor;
    }

    public int eggHighlightColor() {
        return eggHighlightColor;
    }

    public boolean isBoss() {
        return this == STEEL_INQUISITOR;
    }

    public static MetalbornRole[] cachedValues() {
        return CACHED_VALUES;
    }

    public static java.util.Optional<MetalbornRole> byId(String id) {
        if (id == null || id.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(BY_ID.get(id.toLowerCase(java.util.Locale.ROOT)));
    }
}
