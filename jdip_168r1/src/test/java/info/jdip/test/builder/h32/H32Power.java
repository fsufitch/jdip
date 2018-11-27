package info.jdip.test.builder.h32;

import info.jdip.test.builder.TestPower;

public enum H32Power implements TestPower {
    ENGLAND, BURGUNDY, FRANCE;

    @Override
    public String getPowerName() {
        return this.name().toLowerCase();
    }
}
