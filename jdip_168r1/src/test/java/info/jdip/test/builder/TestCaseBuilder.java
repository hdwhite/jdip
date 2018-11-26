package info.jdip.test.builder;

import info.jdip.misc.Case;
import info.jdip.order.OrderParser;
import info.jdip.test.builder.standard.StandardLocation;
import info.jdip.test.builder.standard.StandardPower;
import info.jdip.test.builder.standard.UnitType;
import info.jdip.world.InvalidWorldException;
import info.jdip.world.Phase;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.World;
import info.jdip.world.WorldFactory;
import info.jdip.world.variant.NoVariantsException;
import info.jdip.world.variant.VariantManager;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestCaseBuilder<L extends TestLocation, P extends TestPower> {

    private final Phase phase;
    private final Variant variant;
    private List<TestOrder> orders = new ArrayList<>();
    private Map<L, TestUnit> preStateUnitMap = new HashMap<>();
    private List<TestUnit> postStateUnits = new ArrayList<>();
    private List<TestUnit> preStateDislodgedUnits = new ArrayList<>();
    private List<TestUnit> postStateDislodgedUnits = new ArrayList<>();


    private TestCaseBuilder(Variant variant, Phase phase) {
        this.variant = variant;
        this.phase = phase;
    }

    public static TestCaseBuilder<StandardLocation, StandardPower> standard(Phase.SeasonType seasonType, int year, Phase.PhaseType phaseType) {
        return new TestCaseBuilder<>(Variant.STANDARD, new Phase(seasonType, year, phaseType));
    }

    void addOrder(TestOrder order){
        orders.add(order);
    }

    public TestCaseBuilder<L, P> fleet(P power, L location) {
        preStateUnitMap.put(location, new TestUnit(power, location, UnitType.FLEET));
        return this;
    }

    public TestCaseBuilder<L, P> army(P power, L location) {
        preStateUnitMap.put(location, new TestUnit(power, location, UnitType.ARMY));
        return this;
    }

    public OrderBuilder<L, P> order(L from) {
        TestUnit orderingUnit = preStateUnitMap.get(from);
        if (orderingUnit == null) {
            throw new IllegalArgumentException("There is no unit standing on location " + from.getLocationName() + ". Did you forget to define it first?");
        }
        return new OrderBuilder<>(this, preStateUnitMap, orderingUnit);
    }

    public TestCaseBuilder<L, P> expectFleet(P power, L location) {
        postStateUnits.add(new TestUnit(power, location, UnitType.FLEET));
        return this;
    }

    public TestCaseBuilder<L, P> expectArmy(P power, L location) {
        postStateUnits.add(new TestUnit(power, location, UnitType.ARMY));
        return this;
    }

    public TestCaseBuilder<L, P> expectDislodgedArmy(P power, L location) {
        postStateDislodgedUnits.add(new TestUnit(power, location, UnitType.ARMY));
        return this;
    }

    public TestCaseBuilder<L, P> expectDislodgedFleet(P power, L location) {
        postStateDislodgedUnits.add(new TestUnit(power, location, UnitType.FLEET));
        return this;
    }


    public Case build() throws NoVariantsException, ParserConfigurationException, InvalidWorldException {
        VariantManager.init(new File[]{new File("build/tmp/variants")}, false);
        info.jdip.world.variant.data.Variant variant = VariantManager.getVariant(this.variant.name(), VariantManager.VERSION_NEWEST);
        World world = WorldFactory.getInstance().createWorld(variant);
        TurnState templateTurnState = world.getLastTurnState();
        world.removeTurnState(templateTurnState);
        world.setRuleOptions(RuleOptions.createFromVariant(variant));

        return new Case(
                "nothing yet, sorry",
                phase.toString(),
                preState(),
                orders(),
                postState(),
                preStateSupplyOwners(),
                preStateDislodgedUnits(),
                postStateDislodgedUnits(),
                preStateResults(),
                templateTurnState,
                world,
                OrderParser.getInstance()
        );
    }

    private List<String> preStateResults() {
        List<String> result = new ArrayList<>();
        return result;

    }

    private List<String> postStateDislodgedUnits() {
        return postStateDislodgedUnits.stream().map(TestUnit::getFullDescription).collect(Collectors.toList());
    }

    private List<String> preStateDislodgedUnits() {
        return preStateDislodgedUnits.stream().map(TestUnit::getFullDescription).collect(Collectors.toList());
    }

    private List<String> preStateSupplyOwners() {
        List<String> result = new ArrayList<>();
        return result;

    }

    private List<String> postState() {
        return postStateUnits.stream().map(TestUnit::getFullDescription).collect(Collectors.toList());
    }

    private List<String> orders() {
        return orders.stream().map(TestOrder::getOrder).collect(Collectors.toList());

    }

    private List<String> preState() {
        return preStateUnitMap.values().stream().map(TestUnit::getFullDescription).collect(Collectors.toList());
    }

    enum Variant {
        STANDARD
    }


}
