package info.jdip.test.builder.standard;

import info.jdip.test.builder.TestUnitType;

public enum UnitType implements TestUnitType {
    ARMY,
    FLEET;

    @Override
    public String getUnitTypeLetter() {
        return name().substring(0, 1);
    }
}
