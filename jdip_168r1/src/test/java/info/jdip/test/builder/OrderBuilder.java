package info.jdip.test.builder;

import java.util.Map;

public class OrderBuilder<L extends TestLocation,P extends TestPower> {

    private final TestCaseBuilder<L, P> testCaseBuilder;
    private final Map<L, TestUnit> preStateUnitMap;
    private final TestUnit orderingUnit;


    OrderBuilder(TestCaseBuilder<L, P> testCaseBuilder, Map<L, TestUnit> preStateUnitMap, TestUnit orderingUnit) {
        this.testCaseBuilder = testCaseBuilder;
        this.preStateUnitMap = preStateUnitMap;
        this.orderingUnit = orderingUnit;
    }

    public TestCaseBuilder<L, P> supportHold(L holdingLocation) {
        testCaseBuilder.addOrder(new HoldSupportTestOrder(orderingUnit, preStateUnitMap.get(holdingLocation)));
        return testCaseBuilder;
    }

    public TestCaseBuilder<L, P> hold() {
        testCaseBuilder.addOrder(new HoldTestOrder(orderingUnit));
        return testCaseBuilder;
    }

    public TestCaseBuilder<L, P> moveTo(L to) {
        testCaseBuilder.addOrder(new MoveTestOrder(orderingUnit, to));
        return testCaseBuilder;
    }

    public TestCaseBuilder<L, P> supportMove(L from, L to) {
        testCaseBuilder.addOrder(new MoveSupportTestOrder(orderingUnit, preStateUnitMap.get(from), to));
        return testCaseBuilder;
    }

    public TestCaseBuilder<L, P> convoy(L from, L to) {
        testCaseBuilder.addOrder(new ConvoyTestOrder(orderingUnit, preStateUnitMap.get(from), to));
        return testCaseBuilder;
    }

    class HoldTestOrder implements TestOrder {

        private TestUnit testUnit;

        HoldTestOrder(TestUnit testUnit) {
            this.testUnit = testUnit;
        }

        @Override
        public String getOrder() {
            return testUnit.getFullDescription() + " H";
        }
    }

    class MoveTestOrder implements TestOrder {

        private final TestUnit testUnit;
        private final L to;

        MoveTestOrder(TestUnit testUnit, L to) {
            this.testUnit = testUnit;
            this.to = to;
        }

        @Override
        public String getOrder() {
            return testUnit.getFullDescription() + "-" + to.getLocationName();
        }
    }

    class HoldSupportTestOrder implements TestOrder {

        private final TestUnit supportingUnit;
        private final TestUnit supportedUnit;

        HoldSupportTestOrder(TestUnit supportingUnit, TestUnit supportedUnit) {

            this.supportingUnit = supportingUnit;
            this.supportedUnit = supportedUnit;
        }

        @Override
        public String getOrder() {
            return supportingUnit.getFullDescription() + " S " + supportedUnit.getArmyDescription();
        }
    }

    class MoveSupportTestOrder implements TestOrder {

        private final TestUnit supportingUnit;
        private final TestUnit supportedUnit;
        private final L to;

        MoveSupportTestOrder(TestUnit supportingUnit, TestUnit supportedUnit, L to) {

            this.supportingUnit = supportingUnit;
            this.supportedUnit = supportedUnit;
            this.to = to;
        }

        @Override
        public String getOrder() {
            return supportingUnit.getFullDescription() + " S " + supportedUnit.getFullDescription() + "-" + to.getLocationName();
        }
    }

    class ConvoyTestOrder implements TestOrder {

        private final TestUnit convoyingUnit;
        private final TestUnit convoyedUnit;
        private final L to;

        ConvoyTestOrder(TestUnit convoyingUnit, TestUnit convoyedUnit, L to) {

            this.convoyingUnit = convoyingUnit;
            this.convoyedUnit = convoyedUnit;
            this.to = to;
        }

        @Override
        public String getOrder() {
            return convoyingUnit.getFullDescription() + " C " + convoyedUnit.getFullDescription() + "-" + to.getLocationName();
        }
    }
}
