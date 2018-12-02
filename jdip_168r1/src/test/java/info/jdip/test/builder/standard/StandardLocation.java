package info.jdip.test.builder.standard;

import info.jdip.test.builder.TestLocation;

public enum StandardLocation implements TestLocation {
    TRIESTE, BUDAPEST, GALACIA, DENMARK, RUMANIA, NORWAY, SWEDEN, HOLLAND, NORTH_SEA, NORWEGIAN_SEA, BELGIUM, RUHR, KIEL;

    @Override
    public String getLocationName() {
        return this.name().toLowerCase();
    }
}
