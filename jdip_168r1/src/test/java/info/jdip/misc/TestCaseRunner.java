package info.jdip.misc;

import info.jdip.order.Order;
import info.jdip.order.OrderFactory;
import info.jdip.process.StdAdjudicator;
import info.jdip.world.Position;
import info.jdip.world.Province;
import info.jdip.world.TurnState;
import info.jdip.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCaseRunner {
    private static final Logger logger = LoggerFactory.getLogger(TestCaseRunner.class);
    public static void runCase(Case testCase){
        World world = testCase.getWorld();
        world.setTurnState(testCase.getCurrentTurnState());
        world.setTurnState(testCase.getPreviousTurnState());
        StdAdjudicator stdJudge = new StdAdjudicator(OrderFactory.getDefault(), testCase.getCurrentTurnState());
        stdJudge.process();

        if (stdJudge.isUnresolvedParadox()) {
            throw new RuntimeException("Unresolved paradox...");
        }

        assertTrue(compareState(testCase, stdJudge.getNextTurnState()));
    }
    /**
     * compareState: checks to see if resolved state matches,
     * unit for unit, the Case POSTSTATEs. Units that match
     * are prepended with an '='. Units that are not found in the
     * case POSTSTATE/POSTSTATE_DISLODGED are prepended with a '+',
     * and units in POSTSTATE/POSTSTATE_DISLODGED not found in
     * the resolved turnstate are prepended with a '-'.
     * <p>
     * This is a more strict comparison than the old compareState,
     * w.r.t. dislodged units and coast checking. The implementation
     * is fairly simple and is not optimized for performance.
     * <p>
     * If no POSTSTATE or POSTSTATE_DISLODGED results are given,
     * it is assumed that there are no units for the omitted section.
     * <p>
     * Returns true if the states match (or game has been won);
     * otherwise, returns false.
     */
    private static boolean compareState(Case c, TurnState resolvedTS) {
        if (resolvedTS == null) {
            //position is won
            return true;
        }

        final Position pos = resolvedTS.getPosition();

        // create set of resolvedUnits
        //
        Set<TestSuite.UnitPos> resolvedUnits = new HashSet<>();

        Province[] provs = pos.getUnitProvinces();
        for (Province province : provs) {
            if (!resolvedUnits.add(new TestSuite.UnitPos(pos, province, false))) {
                throw new IllegalStateException("CompareState: Internal error (non dislodged)");
            }
        }

        provs = pos.getDislodgedUnitProvinces();
        for (Province province : provs) {
            if (!resolvedUnits.add(new TestSuite.UnitPos(pos, province, true))) {
                throw new IllegalStateException("CompareState: Internal error (dislodged)");
            }
        }


        resolvedUnits = Collections.unmodifiableSet(resolvedUnits);    // for safety


        // create set of caseUnits
        //
        Set<TestSuite.UnitPos> caseUnits = new HashSet<>();

        for (Order dsOrd : c.getPostState()) {
            if (!caseUnits.add(new TestSuite.UnitPos(dsOrd, false))) {
                logger.error("duplicate POSTSTATE position: ", dsOrd);
                return false;
            }
        }

        for (Order dsOrd : c.getPostDislodged()) {
            if (!caseUnits.add(new TestSuite.UnitPos(dsOrd, true))) {
                logger.error("duplicate POSTSTATE_DISLODGED position: ", dsOrd);
                return false;
            }
        }
        caseUnits = Collections.unmodifiableSet(caseUnits);    // for safety

        // compare sets.
        //
        // first, we must make a duplicate of one set.
        // these are the units that are in the correct position (intersection)
        //
        Set<TestSuite.UnitPos> intersection = new HashSet<>(caseUnits);
        intersection.retainAll(resolvedUnits);

        // now, create subtraction sets
        Set<TestSuite.UnitPos> added = new HashSet<>(resolvedUnits);
        added.removeAll(caseUnits);

        Set<TestSuite.UnitPos> missing = new HashSet<>(caseUnits);
        missing.removeAll(resolvedUnits);

        // if subtraction sets have no units, we are done. Otherwise, we must print
        // the differences.
        //
        return missing.isEmpty() && added.isEmpty();
    }// compareState()

}
