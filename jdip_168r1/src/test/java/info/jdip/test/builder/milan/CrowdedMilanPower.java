package info.jdip.test.builder.milan;

import info.jdip.test.builder.TestPower;

public enum CrowdedMilanPower implements TestPower {
    AUSTRIA, RUSSIA, GERMANY;

    @Override
    public String getPowerName() {
        return this.name().toLowerCase();
    }
}
