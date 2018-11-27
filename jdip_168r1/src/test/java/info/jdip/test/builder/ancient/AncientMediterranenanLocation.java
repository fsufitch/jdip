package info.jdip.test.builder.ancient;

import info.jdip.test.builder.TestLocation;

public enum AncientMediterranenanLocation implements TestLocation {
    TRIESTE, BUDAPEST, GALACIA, DENMARK, RUMANIA, NORWAY, SWEDEN, HOLLAND, NORTH_SEA, NORWEGIAN_SEA;

    @Override
    public String getLocationName() {
        return this.name().toLowerCase();
    }
}
