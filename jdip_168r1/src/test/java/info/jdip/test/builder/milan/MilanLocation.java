package info.jdip.test.builder.milan;

import info.jdip.test.builder.TestLocation;

public enum MilanLocation implements TestLocation {
    TRIESTE, BUDAPEST, GALACIA, DENMARK, RUMANIA, NORWAY, SWEDEN, HOLLAND, NORTH_SEA, NORWEGIAN_SEA, PRUSSIA, SILESIA,
    WARSAW;

    @Override
    public String getLocationName() {
        return this.name().toLowerCase();
    }
}
