package com.not_noah.mistborn_metal_arts.api;

public class Investiture {
    public int quantity; // In BEU (BioChromatic Breath Equivalent Units)
    public StateOfMatter physicalState;
    public Shard[] shardicAlignment;

    public String identityKey; // For Spiritual identity
    public IntentProgram intentProgram; // For Feruchemical intent

    public Investiture(int quantity, Shard[] shardicAlignment) {
        this.quantity = quantity;
        this.shardicAlignment = shardicAlignment;
        this.identityKey = null;
        this.intentProgram = null;
        this.physicalState = null;
    }

    public Boolean canBeAccessedBy(SpiritWeb user) {
        if (this.isRawInvestiture())
            return true;

        return this.identityKey.equals(user.getIdentityKey());
    }

    public Boolean isRawInvestiture() {
        return this.identityKey == null;
    }
}
