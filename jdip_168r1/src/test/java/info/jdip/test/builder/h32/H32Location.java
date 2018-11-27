package info.jdip.test.builder.h32;

import info.jdip.test.builder.TestLocation;

public enum H32Location implements TestLocation {
    LONDON,PARIS, ENGLISH_CHANNEL, NORMANDY;

    @Override
    public String getLocationName() {
        return this.name().toLowerCase().replace("_"," ");
    }
}
