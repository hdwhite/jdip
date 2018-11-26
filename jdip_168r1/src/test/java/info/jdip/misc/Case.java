package info.jdip.misc;

import info.jdip.order.DefineState;
import info.jdip.order.Move;
import info.jdip.order.Order;
import info.jdip.order.OrderException;
import info.jdip.order.OrderFactory;
import info.jdip.order.OrderParser;
import info.jdip.order.Orderable;
import info.jdip.order.result.ConvoyPathResult;
import info.jdip.order.result.OrderResult;
import info.jdip.world.Location;
import info.jdip.world.Phase;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import info.jdip.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a Case
 */
public final class Case {
    private static final Logger logger = LoggerFactory.getLogger(Case.class);
    private final String name;
    private List<Order> preState;
    private List<Order> postState;
    private List<Order> preDislodged;
    private List<Order> postDislodged;
    private List<Order> supplySCOwners;    // all types are 'army'
    private List<OrderResult> results = new ArrayList<>();
    private List<Order> orders;
    private Phase phase = null;
    private TurnState currentTS;
    private TurnState previousTS;
    private World world;


    // tsTemplate: template turnstate to create the current, and (if needed) previous
    // turnstates.
    public Case(String caseName, String phaseName, List<String> preState, List<String> orders,
                List<String> postState, List<String> preStateSupplyOwners, List<String> preStateDislodgedUnits,
                List<String> postStateDislodgedUnits, List<String> preStateResults, TurnState templateTurnState, World world, OrderParser orderParser) {
        this.name = caseName;
        this.world = world;

        // phase
        if (phaseName != null) {
            phase = Phase.parse(phaseName);
            if (phase == null) {
                logger.error("Case: {} Cannot parse phase {}.", phaseName);
                throw new IllegalStateException("cannot parse phase " + phaseName);
            }
        }

        // set phase to template phase, if no phase was assigned.
        phase = (phaseName == null) ? templateTurnState.getPhase() : phase;

        // setup current turnstate from template
        // use phase, if appropriate.
        currentTS = new TurnState(phase);
        currentTS.setPosition(templateTurnState.getPosition().cloneExceptUnits());
        currentTS.setWorld(world);

        // setup previous phase, in case we need it.
        previousTS = new TurnState(phase.getPrevious());
        previousTS.setPosition(templateTurnState.getPosition().cloneExceptUnits());
        previousTS.setWorld(world);

        this.preState = parseOrders(orderParser, preState, true);
        this.orders = parseOrders(orderParser, orders, false);
        this.postState = parseOrders(orderParser, postState, true);
        this.preDislodged = parseOrders(orderParser, preStateDislodgedUnits, true);
        this.postDislodged = parseOrders(orderParser, postStateDislodgedUnits, true);
        this.supplySCOwners = parseOrders(orderParser, preStateSupplyOwners, true);


        // OrderResults
        //
        // THE BEST way to do this would be to setup the case, and then run
        // the adjudicator to get the results, checking the ajudicator results
        // against the 'prestate' positions. This way we would have all the same
        // results that the adjudicator would normally generate.
        //
        if (preStateResults != null) {
            List<OrderResult> orderResults = new ArrayList<>();
            for (String line : preStateResults) {
                OrderResult.ResultType ordResultType = null;

                // success or failure??
                if (line.startsWith("success")) {
                    ordResultType = OrderResult.ResultType.SUCCESS;
                } else if (line.startsWith("failure")) {
                    ordResultType = OrderResult.ResultType.FAILURE;
                } else {
                    logger.error("case: {} line: {} PRESTATE_RESULTS: must prepend orders with \"SUCCESS:\" or \"FAILURE:\".", caseName, line);
                    throw new RuntimeException("PRESTATE_RESULTS: must prepend orders with \"SUCCESS:\" or \"FAILURE:\".");
                }

                // remove after first colon, and parse the order
                line = line.substring(line.indexOf(':') + 1);
                Order order = parseOrder(orderParser, line, previousTS, false);

                // was order a convoyed move? because then we have to add a
                // convoyed move result.
                //
                if (order instanceof Move) {
                    Move mv = (Move) order;
                    if (mv.isConvoying()) {
                        // NOTE: we cheat; path src/dest ok, middle is == src
                        Province[] path = new Province[3];
                        path[0] = mv.getSource().getProvince();
                        path[1] = path[0];
                        path[2] = mv.getDest().getProvince();
                        orderResults.add(new ConvoyPathResult(order, path));
                    }
                }


                // create/add order result
                orderResults.add(new OrderResult(order, ordResultType, " (prestate)"));
            }
            this.results = new ArrayList<>(orderResults);

            // add results to previous turnstate
            previousTS.setResultList(new ArrayList<>(orderResults));

            // add positions/ownership/orders to current turnstate
            //
            // add orders, first clearing any existing orders in the turnstate
            currentTS.clearAllOrders();
            for (Order order : this.orders) {
                List<Orderable> orderList = currentTS.getOrders(order.getPower());
                orderList.add(order);
                currentTS.setOrders(order.getPower(), orderList);
            }

            // get position
            Position position = currentTS.getPosition();

            // ensure all powers are active
            for (Power power : world.getMap().getPowers()) {
                position.setEliminated(power, false);
            }

            // Add non-dislodged units
            for (Order state : this.preState) {
                Unit unit = new Unit(state.getPower(), state.getSourceUnitType());
                unit.setCoast(state.getSource().getCoast());
                position.setUnit(state.getSource().getProvince(), unit);
            }

            // Add dislodged units
            for (Order state : preDislodged) {
                Unit unit = new Unit(state.getPower(), state.getSourceUnitType());
                unit.setCoast(state.getSource().getCoast());
                position.setDislodgedUnit(state.getSource().getProvince(), unit);
            }

            // Set supply center owners
            // if we have ANY supply center owners, we erase the template
            // if we do not have any, we assume the template is correct
            // no need to validate units
            if (supplySCOwners.size() > 0) {
                // first erase old info
                final Province[] provinces = position.getProvinces();

                for (Province province : provinces) {
                    if (position.hasSupplyCenterOwner(province)) {
                        position.setSupplyCenterOwner(province, null);
                    }
                }

                // add new info
                for (Order state : supplySCOwners) {
                    position.setSupplyCenterOwner(state.getSource().getProvince(), state.getPower());
                }
            }
        }

    }// Case()

    private List<Order> parseOrders(OrderParser orderParser, List<String> orderLines, boolean isDefineState) {
        List<Order> orders = new ArrayList<>(orderLines.size());
        for (String line : orderLines) {
            Order order = parseOrder(orderParser, line, currentTS, isDefineState);
            orders.add(order);
        }
        return orders;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public List<Order> getPreState() {
        return preState;
    }

    public List<Order> getPostState() {
        return postState;
    }

    public List<Order> getPreDislodged() {
        return preDislodged;
    }

    public List<Order> getPostDislodged() {
        return postDislodged;
    }

    public List<Order> getSCOwners() {
        return supplySCOwners;
    }

    public Phase getPhase() {
        return phase;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public List<OrderResult> getResults() {
        return results;
    }

    public TurnState getCurrentTurnState() {
        return currentTS;
    }

    public TurnState getPreviousTurnState() {
        return previousTS;
    }

    private Order parseOrder(OrderParser orderParser, String orderString, TurnState ts, boolean isDefineState) {
        try {
            // no guessing (but not locked); we must ALWAYS specify the power.
            Order o = orderParser.parse(OrderFactory.getDefault(), orderString, null, ts, false, false);

            if (isDefineState) {
                if (o instanceof DefineState) {
                    // we just want to check if the DefineState order does not have
                    // an undefined coast for a fleet unit.
                    Location newLoc = o.getSource().getValidatedSetup(o.getSourceUnitType());

                    // create a new DefineState with a validated loc
                    o = OrderFactory.getDefault().createDefineState(o.getPower(),
                            newLoc, o.getSourceUnitType());
                } else {
                    throw new OrderException("A DefineState order is required here.");
                }
            }

            return o;
        } catch (OrderException e) {
            System.out.println("ERROR");
            System.out.println("parseOrder() OrderException: " + e);
            System.out.println("Case: " + name);
            System.out.println("failure line: " + orderString);
            throw new RuntimeException(e);
        }
    }// parseOrder()

    public World getWorld() {
        return world;
    }
}// class Case
