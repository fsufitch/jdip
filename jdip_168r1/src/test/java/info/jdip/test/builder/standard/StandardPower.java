package info.jdip.test.builder.standard;

import info.jdip.test.builder.TestPower;

public enum StandardPower implements TestPower {
    ENGLAND, FRANCE, AUSTRIA, RUSSIA, GERMANY;

    @Override
    public String getPowerName() {
        return this.name().toLowerCase();
    }
}
