package info.jdip.test.builder.ancient;

import info.jdip.test.builder.TestLocation;

public enum AncientMediterranenanLocation implements TestLocation {
    AEGEAN_SEA, SPARTA, CRETE;

    @Override
    public String getLocationName() {
        return this.name().toLowerCase();
    }
}
