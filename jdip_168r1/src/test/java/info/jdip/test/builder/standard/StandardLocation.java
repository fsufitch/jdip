package info.jdip.test.builder.standard;

import info.jdip.test.builder.TestLocation;

public enum StandardLocation implements TestLocation {
    TRIESTE, BUDAPEST, BULGARIA, GREECE, ANKARA, CONSTANTINOPLE, GALACIA, DENMARK, RUMANIA, NORWAY, SWEDEN, HOLLAND,
    NORTH_SEA, NORWEGIAN_SEA, EDINBURGH,AEGEAN_SEA;

    @Override
    public String getLocationName() {
        return this.name().toLowerCase();
    }
}
