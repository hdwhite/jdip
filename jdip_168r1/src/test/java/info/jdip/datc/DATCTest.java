package info.jdip.datc;

import dip.misc.TestSuite;
import dip.order.DefineState;
import dip.order.OrderFactory;
import dip.process.StdAdjudicator;
import dip.world.Position;
import dip.world.Province;
import dip.world.TurnState;
import dip.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DATCTest {


    protected static TestSuite testSuite;
    protected static String testCaseLocation;
    final List<String> unRezParadoxes = new LinkedList<>();


    private static List<TestSuite.Case> sourceOfCases() {
        return testSuite.getAllCases();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("DATC parametrized cases")
    @MethodSource("sourceOfCases")
    void withValueSource(TestSuite.Case testCase) {
        assertTimeoutPreemptively(ofSeconds(2), () -> {
            World world = testSuite.getWorld();
            world.setTurnState(testCase.getCurrentTurnState());
            world.setTurnState(testCase.getPreviousTurnState());
            StdAdjudicator stdJudge = new StdAdjudicator(OrderFactory.getDefault(), testCase.getCurrentTurnState());
            stdJudge.process();

            if(stdJudge.isUnresolvedParadox())
            {
                unRezParadoxes.add(testCase.getName());
            }

            assertTrue(compareState(testCase, stdJudge.getNextTurnState()));
        });

    }



    /**
     *	compareState: checks to see if resolved state matches,
     *	unit for unit, the Case POSTSTATEs. Units that match
     *	are prepended with an '='. Units that are not found in the
     *	case POSTSTATE/POSTSTATE_DISLODGED are prepended with a '+',
     *	and units in POSTSTATE/POSTSTATE_DISLODGED not found in
     *	the resolved turnstate are prepended with a '-'.
     *	<p>
     *	This is a more strict comparison than the old compareState,
     *	w.r.t. dislodged units and coast checking. The implementation
     *	is fairly simple and is not optimized for performance.
     *	<p>
     *	If no POSTSTATE or POSTSTATE_DISLODGED results are given,
     *	it is assumed that there are no units for the omitted section.
     *	<p>
     *	Returns true if the states match (or game has been won);
     *	otherwise, returns false.
     */
    private boolean compareState(TestSuite.Case c, TurnState resolvedTS)
    {
        if(resolvedTS == null)
        {
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


        resolvedUnits = Collections.unmodifiableSet(resolvedUnits);	// for safety


        // create set of caseUnits
        //
        Set<TestSuite.UnitPos> caseUnits = new HashSet<>();

        DefineState[] dsOrds = c.getPostState();
        for (DefineState dsOrd : dsOrds) {
            if (!caseUnits.add(new TestSuite.UnitPos(dsOrd, false))) {
//                println("ERROR: duplicate POSTSTATE position: "+dsOrds[i]);
                return false;
            }
        }

        dsOrds = c.getPostDislodged();
        for (DefineState dsOrd : dsOrds) {
            if (!caseUnits.add(new TestSuite.UnitPos(dsOrd, true))) {
//                println("ERROR: duplicate POSTSTATE_DISLODGED position: "+dsOrds[i]);
                return false;
            }
        }
        caseUnits = Collections.unmodifiableSet(caseUnits);	// for safety

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
