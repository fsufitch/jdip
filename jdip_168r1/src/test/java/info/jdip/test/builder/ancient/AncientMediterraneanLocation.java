package info.jdip.test.builder.ancient;

import info.jdip.test.builder.TestLocation;

public enum AncientMediterraneanLocation implements TestLocation {
    AEGEAN_SEA, SPARTA, CRETE, SYRIAN_SEA, SIDON, CYPRUS;

    @Override
    public String getLocationName() {
        return this.name().toLowerCase();
    }
}
