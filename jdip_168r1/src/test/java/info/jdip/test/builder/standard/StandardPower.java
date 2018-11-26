package info.jdip.test.builder.standard;

import info.jdip.test.builder.TestPower;

public enum StandardPower implements TestPower {
    AUSTRIA, RUSSIA, GERMANY, TURKEY;

    @Override
    public String getPowerName() {
        return this.name().toLowerCase();
    }
}
