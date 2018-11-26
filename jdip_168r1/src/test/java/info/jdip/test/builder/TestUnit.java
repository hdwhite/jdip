package info.jdip.test.builder;

import org.apache.commons.lang3.StringUtils;

class TestUnit {
    private final TestPower testPower;
    private final TestLocation testLocation;
    private final TestUnitType testUnitType;

    TestUnit(TestPower testPower, TestLocation testLocation, TestUnitType testUnitType) {
        this.testPower = testPower;
        this.testLocation = testLocation;
        this.testUnitType = testUnitType;
    }

    /**
     * @return String containing the full description of this unit such as <em>England: A London</em>
     */
    public String getFullDescription(){
        return StringUtils.capitalize(testPower.getPowerName().toLowerCase())
                + ": "
                +getArmyDescription();
    }
    /**
     * @return String containing description of this unit without power such as <em>A London</em>
     */
    public String getArmyDescription() {
        return testUnitType.getUnitTypeLetter()
                + " "
                + testLocation.getLocationName();
    }
}
