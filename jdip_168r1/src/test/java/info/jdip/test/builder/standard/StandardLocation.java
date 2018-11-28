package info.jdip.test.builder.standard;

import info.jdip.test.builder.TestLocation;

public enum StandardLocation implements TestLocation {
    TRIESTE, BUDAPEST, GALACIA, DENMARK, RUMANIA, NORWAY, SWEDEN, HOLLAND, NORTH_SEA, NORWEGIAN_SEA, SERBIA,BULGARIA,
    GREECE, CONSTANTINOPLE, BLACK_SEA, AEGEAN_SEA;

    @Override
    public String getLocationName() {
        return this.name().toLowerCase();
    }
}
