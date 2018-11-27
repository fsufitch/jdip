package info.jdip.test.builder.ancient;

import info.jdip.test.builder.TestPower;

public enum AncientMediterranenanPower implements TestPower {
    CARTHAGE, EGYPT, GREECE, PERSIA, ROME;

    @Override
    public String getPowerName() {
        return this.name().toLowerCase();
    }
}
