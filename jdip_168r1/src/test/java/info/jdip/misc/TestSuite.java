//
//  @(#)TestSuite.java		4/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/
//
package info.jdip.misc;

import info.jdip.order.Order;
import info.jdip.order.OrderFactory;
import info.jdip.order.OrderParser;
import info.jdip.order.result.OrderResult;
import info.jdip.order.result.Result;
import info.jdip.process.StdAdjudicator;
import info.jdip.world.Position;
import info.jdip.world.Province;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import info.jdip.world.World;
import info.jdip.world.WorldFactory;
import info.jdip.world.variant.VariantManager;
import info.jdip.world.variant.data.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A very hastily-programmed Test harness..
 * <p>
 * This will read in a file of cases (1 or more). All cases must use the same
 * variant. The variant is then loaded, orders are parsed, and adjudication then
 * occurs. After adjudication, the positions of units are checked with that of
 * the case file for discrepancies. If no discrepancies exist, the case passes.
 * <p>
 * Note that when in performance-testing mode, all logging is disabled and
 * comparison-checking is not performed; the goal is testing adjudicator code
 * only.
 * <p>
 * All output is printed to stdout
 * <p>
 * <b>Case File Format Notes:</b>
 * <ul>
 * <li>
 * Any line prefixed by a # is a comment line. A # may be placed after a line,
 * to comment out part of a line or make a comment about a particular line.
 * </li>
 * <li>
 * Empty lines / whitespace-only lines are ignored. Whitespace before keywords
 * and lines are also ignored by the parser.
 * </li>
 * <li>
 * Single Line keywords are a keyword, followed by whitespace, followed by
 * text; that text is parsed and associated with that keyword. Some keywords
 * (such as END) do not have any text that follows them.
 * </li>
 * <li>
 * Block keywords begin a block; DO NOT put text on the same line as a block
 * keyword; start text on the next line. A block ends when another keyword
 * (block or single line) is detected.
 * </li>
 * </ul>
 *
 * <b>Case File Keywords:</b>
 * <ul>
 * <li><b>VARIANT_ALL: </b><i>Required</i>.
 * This must occur at the beginning of the case file. <i>All cases are
 * required to use the same variant</i>. Single line.
 * </li>
 * <li><b>CASE: (String)</b><i>Required</i>.
 * Begins a Case. The text following the case is the case name, and may
 * contain any printable character, including spaces, but must fit on
 * a single line.
 * </li>
 * <li><b>PRESTATE_SETPHASE: (phase)</b><i>Recommended</i>.
 * Set the phase (e.g., "Fall 1901, Movement" or "F1901M"). Single line.
 * </li>
 * <li><b>PRESTATE: </b><i>Recommended</i>.
 * Begins the non-dislodged unit setup block. Unit setups must consist of power, unit type,
 * and province, on the next line(s). e.g.: "England: F lon". Any orders to
 * non-dislodged units require a unit in the PRESTATE block.
 * </li>
 * <li><b>PRESTATE_DISLODGED: </b><i>Optional</i>.
 * If any dislodged units are to be positioned, set them in this block.
 * e.g.: "England: F lon" would create a dislodged Fleet in London.
 * </li>
 * <li><b>PRESTATE_RESULTS: </b><i>Optional</i>.
 * If a retreat phase is to be adjudicated, this sets up the "prior" phase.
 * Begins a block, where each order must be preceded by the keyword "SUCCESS:"
 * or "FAILURE:", followed by an order (i.e., Move, Hold, etc.).
 * </li>
 * <li><b>PRESTATE_SUPPLYCENTER_OWNERS: </b><i>Optional</i>.
 * Set owned, but not occupied, supply center owners in this block. If this is omitted,
 * the ownership is used from the initial variant settings. If it is supplied,
 * the variant information is erased and replaced with the given information.
 * <b>Note:</b> Currently you must use a unit too; e.g., "France: F lon" would set
 * the supply center in London to be owned by France. The unit type is required by
 * the parser but is ignored.
 * </li>
 * <li><b>ORDERS: </b><i>Recommended</i>.
 * One line, one order, in this block. e.g., "England: F lon-bel".
 * The orders are what will be adjudicated.
 * </li>
 * <li><b>POSTSTATE: </b><i>Recommended</i>.
 * A block of post-adjudication non-dislodged unit positions. The TestSuite tests
 * and make sure these match the post-adjudication state. Same format as PRESTATE.
 * </li>
 * <li><b>POSTSTATE_DISLODGED: </b><i>Recommended</i>.
 * A block of post-adjudication dislodged unit positions. The TestSuite tests
 * and make sure these match the post-adjudication state. Same format as PRESTATE
 * (or PRESTATE_DISLODGED for that matter).
 * </li>
 * <li><b>POSTSTATE_SAME: </b><i>Optional</i>.
 * If non-dislodged units do not change position, this may be used instead
 * of a POSTSTATE block and a list of non-dislodged unit positions.
 * </li>
 * <li><b>END: </b><i>Required</i>.
 * Ends a case. Must be the last line in a case.
 * </li>
 * </ul>
 * <p>
 * <b>An Example Case File:</b>
 * <pre>
 * VARIANT_ALL Standard
 * CASE Example Case 1 (illustrative example)
 * PRESTATE_SETPHASE Fall 1901, Movement
 * PRESTATE
 * Russia: F con
 * Russia: F bla
 * Turkey: F ank
 * ORDERS
 * Russia: F con S F bla-ank
 * Russia: F bla-ank
 * Turkey: F ank-con
 * POSTSTATE
 * Russia: F con
 * Russia: F ank
 * POSTSTATE_DISLODGED
 * Turkey: F ank
 * END
 * </pre>
 */
public final class TestSuite {
    private static final Logger logger = LoggerFactory.getLogger(TestSuite.class);
    // constants
    private static final String VARIANT_ALL = "variant_all";
    private static final String CASE = "case";
    private static final String PRESTATE = "prestate";
    private static final String ORDERS = "orders";
    private static final String POSTSTATE = "poststate";
    private static final String POSTSTATE_SAME = "poststate_same";
    private static final String END = "end";
    private static final String PRESTATE_SETPHASE = "prestate_setphase";
    private static final String PRESTATE_SUPPLYCENTER_OWNERS = "prestate_supplycenter_owners";
    private static final String PRESTATE_DISLODGED = "prestate_dislodged";
    private static final String POSTSTATE_DISLODGED = "poststate_dislodged";
    private static final String PRESTATE_RESULTS = "prestate_results";

    // warning: POSTSTATE_SAME MUST come before POSTSTATE (since we use startsWith())
    // "other" == not CASE (begin) or END
    private static final String[] KEY_TYPES_OTHER = {ORDERS, POSTSTATE_SAME, PRESTATE_SETPHASE, PRESTATE_RESULTS,
            PRESTATE_SUPPLYCENTER_OWNERS, PRESTATE_DISLODGED,
            POSTSTATE_DISLODGED, PRESTATE, POSTSTATE, VARIANT_ALL};

    private static final String[] KEY_TYPES_WITH_LIST = {ORDERS, PRESTATE_SUPPLYCENTER_OWNERS, PRESTATE_RESULTS,
            PRESTATE_DISLODGED, POSTSTATE_DISLODGED, POSTSTATE, PRESTATE};
    private static final String VARIANT_DIR = "variants";
    private static boolean isAdjudicatorLogged = true;
    private static boolean isLogging = true;
    private static boolean isPerfTest = false;
    private static boolean isRegression = false;
    private static String inFileName = null;
    private static int benchTimes = 1;
    private final List<Case> cases = new ArrayList<>(10);
    private final List<String> failedCaseNames = new ArrayList<>(10);
    private Map<String, List<String>> keyMap = null;
    private World world = null;
    private TurnState templateTurnState;
    private StdAdjudicator stdJudge = null;
    // VARIANT_ALL name
    private String variantName = null;
    private File variantsDir;

    public TestSuite(File variantsDir) {
        this.variantsDir = variantsDir;
    }

    public TestSuite() {

    }


    /**
     * Start the TestSuite
     */
    public static void main(String args[]) {
        if (args.length < 1 || args.length > 2) {
            printUsageAndExit();
        }

        if (args.length == 2) {
            inFileName = args[1];

            String firstArg = args[0].trim().toLowerCase();
            if (firstArg.startsWith("-perftest")) {
                isLogging = false;
                isAdjudicatorLogged = false;
                isPerfTest = true;
                if (firstArg.contains(":")) {
                    benchTimes = getTimes(firstArg);
                } else {
                    printUsageAndExit();
                }
            } else if (firstArg.equals("-brief")) {
                isAdjudicatorLogged = false;
            } else if (firstArg.equals("-statsonly")) {
                isAdjudicatorLogged = false;
                isPerfTest = false;
                isLogging = false;
            } else if (firstArg.equals("-regress")) {
                isAdjudicatorLogged = false;
                isPerfTest = false;
                isLogging = false;
                isRegression = true;
            } else {
                printUsageAndExit();
            }
        } else {
            inFileName = args[0];
        }


        TestSuite ts = new TestSuite();

        logger.info("TestSuite Results: ({})", new Date());
        logger.info("Test case file: {}", inFileName);


        File file = new File(inFileName);
        TestStatistics testStatistics = new TestStatistics(System.currentTimeMillis(),benchTimes);

        ts.parseCases(file);
        testStatistics.parsingFinished(System.currentTimeMillis());
        logger.info("  initialization complete.");
        ts.evaluate(testStatistics);
    }// main()

    private static void printUsageAndExit() {
        System.out.println("USAGE: TestSuite [-statsonly | -perftest | -brief] <test-input-file>");
        System.out.println("  All log output to stdout");
        System.out.println("  -statsonly      disable all logging; only show statistics");
        System.out.println("  -perftest:n     no logging or statistics; repeat all cases n times");
        System.out.println("  -brief          disable internal adjudicator logging");
        System.out.println("  -regress        run test cases in infinite loop; no logging or stats.");
        System.out.println();
        System.out.println("  Examples:");
        System.out.println("      java info.jdip.misc.TestSuite datc.txt >out");
        System.out.println("      java info.jdip.misc.TestSuite -brief datc.txt >out");
        System.out.println("      java info.jdip.misc.TestSuite -perftest:1000 case.txt >out");
        System.exit(1);
    }


    private static int getTimes(String in) {
        String s = in.substring(in.indexOf(':') + 1);
        int n = -1;
        try {
            n = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: invalid argument: " + in);
            printUsageAndExit();
        }

        if (n <= 0) {
            System.err.println("Benchmark repitition out of range; must be greater than 0");
            printUsageAndExit();
        }

        return n;
    }// getTimes()


    private void initVariant() {
        try {

            // parse variants
            VariantManager.init(new File[]{variantsDir}, false);

            // load the default variant (Standard)
            // error if it cannot be found!!
            Variant variant = VariantManager.getVariant(variantName, VariantManager.VERSION_NEWEST);
            if (variant == null) {
                throw new Exception("Cannot find variant " + variantName);
            }

            // create the world
            world = WorldFactory.getInstance().createWorld(variant);
            templateTurnState = world.getLastTurnState();
            world.removeTurnState(templateTurnState);

            // set the RuleOptions in the World (this is normally done
            // by the GUI)
            world.setRuleOptions(RuleOptions.createFromVariant(variant));
        } catch (Exception e) {
            logger.error("Init error: ", e);
            throw new RuntimeException(e);
        }
    }// init()

    public void evaluate(TestStatistics statistics) {

        // all cases in an array
        final Case[] allCases = cases.toArray(new Case[cases.size()]);

        if (isRegression) {
            regression(allCases);
            return;
        } else if (isPerfTest) {
            performance(statistics, allCases);
        } else {
            normalMode(statistics, allCases);
        }


        // print stats
        //
        statistics.endTime(System.currentTimeMillis());

        statistics.printStatistics(isPerfTest);


        // exit
        System.exit(statistics.failed());
    }// evaluate()

    private void normalMode(TestStatistics counters , Case[] allCases) {
        // 'typical' mode (testing).
        // we keep stats and may or may not have logging
        //
        for (Case currentCase : allCases) {
            // world: setup
            world.setTurnState(currentCase.getCurrentTurnState());
            world.setTurnState(currentCase.getPreviousTurnState());

            logger.info("Case: {}", currentCase.getName());

            printState(currentCase);

            printOrders(currentCase);
            counters.addOrders(currentCase.getOrders().size());

            stdJudge = new StdAdjudicator(OrderFactory.getDefault(), currentCase.getCurrentTurnState());
            stdJudge.process();

            // add unresolved paradoxes to list, so we know which cases they are
            if (stdJudge.isUnresolvedParadox()) {
                counters.addUnresolved(currentCase.getName());
            }

            logger.info("Adjudications results:");
            if (stdJudge.getNextTurnState() == null) {
                logger.info("Next Phase: NONE. Game has been won.");
            } else {
                logger.info("Next Phase: {} ", stdJudge.getNextTurnState().getPhase());
            }
            List<Result> resultList = stdJudge.getTurnState().getResultList();
            if (isLogging) {
                for (Result r : resultList) {
                    logger.info("Result: {}", r);
                }
            }

            logger.info("Post state");
            if (compareState(currentCase, stdJudge.getNextTurnState())) {
                counters.pass();
            } else {
                counters.fail(currentCase.getName());
                failedCaseNames.add(currentCase.getName());
            }
            counters.newCase();

            // cleanup: remove turnstates from world
            world.removeAllTurnStates();

            // cleanup: clear results in currentTurnSTate
            // this is absolutely essential!!
            currentCase.getCurrentTurnState().getResultList().clear();
        }
    }

    private void performance(TestStatistics counters, Case[] allCases) {
        // performance mode. We need to track stats here,
        // but there is no logging or output except stats.
        //
        for (int i = 0; i < benchTimes; i++) {
            for (Case currentCase : allCases) {
                // world: setup
                world.setTurnState(currentCase.getCurrentTurnState());
                world.setTurnState(currentCase.getPreviousTurnState());

                counters.addOrders(currentCase.getOrders().size());

                // adjudicate
                // we don't check results when in performance mode.
                //
                stdJudge = new StdAdjudicator(OrderFactory.getDefault(), currentCase.getCurrentTurnState());
                stdJudge.process();

                counters.newCase();

                // cleanup: remove turnstates from world
                world.removeAllTurnStates();

                // cleanup: clear results in currentTurnSTate
                // this is absolutely essential!!
                currentCase.getCurrentTurnState().getResultList().clear();
            }
        }
    }

    private void regression(Case[] allCases) {
        // no stats are kept in regression mode, because we're in an
        // infinite loop.
        //
        int rCount = 0;
        System.out.print("Running cases in an infinite loop");

        while (true) {
            for (Case currentCase : allCases) {
                // world: setup
                world.setTurnState(currentCase.getCurrentTurnState());
                world.setTurnState(currentCase.getPreviousTurnState());

                stdJudge = new StdAdjudicator(OrderFactory.getDefault(), currentCase.getCurrentTurnState());
                stdJudge.process();

                // cleanup: remove turnstates from world
                world.removeAllTurnStates();

                // cleanup: clear results in currentTurnSTate
                // this is absolutely essential!!
                currentCase.getCurrentTurnState().getResultList().clear();

                // print a '.' every 1000 iterations
                rCount++;
                if (rCount == 1000) {
                    System.out.print('.');
                    rCount = 0;
                }
            }
        }
    }



    // prints state settings...
    private void printState(Case c) {
        if (!isLogging) {
            return;
        }

        TurnState turnState = c.getCurrentTurnState();
        //Position position = turnState.getPosition();

        logger.info("Phase: {}", turnState.getPhase());

        // if we have some results to display, for prior state, do that now.
        if (isLogging && c.getResults().size() > 0) {
            // print
            logger.info("Pre state results from {}:", c.getPreviousTurnState().getPhase());
            for (OrderResult anOr : c.getResults()) {
                logger.info("Order result: {}", anOr);
            }
        }


        // print non-dislodged units
        if (c.getPreState().size() > 0) {
            logger.info("Pre state:");
            for (Order dsOrd : c.getPreState()) {
                logger.info("Define state: {}", dsOrd);
            }
        }

        // print dislodged units
        if (c.getPreDislodged().size() > 0) {
            logger.info("Pre state dislodged:");
            for (Order dsOrd : c.getPreDislodged()) {
                logger.info("Define state: {}", dsOrd);
            }
        }
    }// printState()

    /**
     * Prints the orders in a case
     */
    private void printOrders(Case currentCase) {
        if (isLogging) {
            logger.info("Orders:");
            for (Order order : currentCase.getOrders()) {
                logger.info("Order: {}", order);
            }

            if (currentCase.getOrders().size() == 0) {
                logger.info("No orders");
            }
        }
    }// printOrders()

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
    private boolean compareState(Case c, TurnState resolvedTS) {
        // special case: check for a win.
        if (resolvedTS == null) {
            logger.info("The game has been won. No new TurnState object is created.");
            return true;
        }

        final Position pos = resolvedTS.getPosition();

        // create set of resolvedUnits
        //
        Set<UnitPos> resolvedUnits = new HashSet<>();

        Province[] provs = pos.getUnitProvinces();
        for (Province prov : provs) {
            if (!resolvedUnits.add(new UnitPos(pos, prov, false))) {
                throw new IllegalStateException("CompareState: Internal error (non dislodged)");
            }
        }

        provs = pos.getDislodgedUnitProvinces();
        for (Province prov : provs) {
            if (!resolvedUnits.add(new UnitPos(pos, prov, true))) {
                throw new IllegalStateException("CompareState: Internal error (dislodged)");
            }
        }


        resolvedUnits = Collections.unmodifiableSet(resolvedUnits);    // for safety


        // create set of caseUnits
        //
        Set<UnitPos> caseUnits = new HashSet<>();

        for (Order dsOrd : c.getPostState()) {
            if (!caseUnits.add(new UnitPos(dsOrd, false))) {
                logger.error("Duplicate POSTSTATE position: {}", dsOrd);
                return false;
            }
        }

        for (Order dsOrd : c.getPostDislodged()) {
            if (!caseUnits.add(new UnitPos(dsOrd, true))) {
                logger.error("Duplicate POSTSTATE_DISLODGED position: {}", dsOrd);
                return false;
            }
        }
        caseUnits = Collections.unmodifiableSet(caseUnits);    // for safety

        // compare sets.
        //
        // first, we must make a duplicate of one set.
        // these are the units that are in the correct position (intersection)
        //
        Set<UnitPos> intersection = new HashSet<>(caseUnits);
        intersection.retainAll(resolvedUnits);

        // now, create subtraction sets
        Set<UnitPos> added = new HashSet<>(resolvedUnits);
        added.removeAll(caseUnits);

        Set<UnitPos> missing = new HashSet<>(caseUnits);
        missing.removeAll(resolvedUnits);

        // if subtraction sets have no units, we are done. Otherwise, we must print
        // the differences.
        //
        if (!missing.isEmpty() || !added.isEmpty()) {
            logger.info("CompareState: FAILED: unit positions follow.");

            // print adds
            printSet(added, "+");

            // print subtracts
            printSet(missing, "-");

            // print units in correct position
            printSet(intersection, "=");

            return false;
        } else {
            logger.info("  CompareState: PASSED");
        }

        return true;
    }// compareState()

    /**
     * Print all the UnitPos objects from a Set; prefixing with the given prefix
     */
    private void printSet(Set<UnitPos> set, String prefix) {
        for (UnitPos up : set) {

            StringBuilder sb = new StringBuilder(64);
            sb.append("  ");    // spacer
            sb.append(prefix);
            sb.append(" ");
            sb.append(up);

            logger.info(sb.toString());
        }
    }// printSet()

    public int getNumberOfCases() {
        return cases.size();
    }

    public Case getTestCase(int testCase) {
        return cases.get(testCase);
    }

    public World getWorld() {
        return world;
    }

    public List<Case> getAllCases() {
        return cases;
    }

    // NEW case parser
    public void parseCases(File caseFile) {
        BufferedReader br = null;

        // per case data that is NOT in List format
        String caseName = null;
        String phaseName = null;
        boolean inCase = false;        // we are in a CASE

        // setup reader
        try {
            br = new BufferedReader(new FileReader(caseFile));
        } catch (IOException e) {
            System.out.println("ERROR: I/O error opening case file \"" + caseFile + "\"");
            System.out.println("EXCEPTION: " + e);
            throw new RuntimeException(e);
        }

        try {
            String rawLine = br.readLine();
            String currentKey = null;
            int lineCount = 1;

            while (rawLine != null) {
                String line = filterLine(rawLine);
                String key = getKeyType(line);

                if (key != null) {
                    currentKey = key;
                }

                // only process non-null (after filtering)
                if (line != null) {
                    if (currentKey == null) {
                        // this can occur if a key is missing.
                        System.out.println("ERROR: missing a required key");
                        System.out.println("Line " + lineCount + ": " + rawLine);
                        throw new IllegalStateException("Missing required key");
                    } else if (currentKey.equals(VARIANT_ALL)) {
                        // make sure nothing is defined yet
                        if (variantName == null) {
                            variantName = getAfterKeyword(line);
                        } else {
                            System.out.println("ERROR: before cases are defined, the variant must");
                            System.out.println("       be set with the VARIANT_ALL flag.");
                            throw new IllegalStateException("ERROR: before cases are defined, the variant must be set with the VARIANT_ALL flag.");
                        }

                        // make sure we are not in a case!
                        if (inCase) {
                            System.out.println("ERROR: VARIANT_ALL cannot be used within a CASE.");
                            throw new IllegalStateException("ERROR: VARIANT_ALL cannot be used within a CASE.");
                        }

                        // attempt to initialize the variant
                        initVariant();
                    } else if (currentKey.equals(CASE)) {
                        // begin a case; case name appears after keyword
                        //
                        // clear data
                        inCase = true;
                        clearAndSetupKeyMap();
                        caseName = null;
                        phaseName = null;
                        currentKey = null;

                        // set case name
                        caseName = getAfterKeyword(line);

                        // make sure we have defined a variant!
                        if (variantName == null) {
                            System.out.println("ERROR: before cases are defined, the variant must");
                            System.out.println("       be set with the VARIANT_ALL flag.");
                            throw new IllegalStateException("ERROR: before cases are defined, the variant must be set with the VARIANT_ALL flag.");
                        }
                    } else if (currentKey.equals(END)) {
                        // end a case
                        inCase = false;

                        // create the case
                        Case aCase = new Case(caseName, phaseName,
                                getListForKeyType(PRESTATE),        // prestate
                                getListForKeyType(ORDERS),            // orders
                                getListForKeyType(POSTSTATE),        // poststate
                                getListForKeyType(PRESTATE_SUPPLYCENTER_OWNERS),        // pre-state: sc owners
                                getListForKeyType(PRESTATE_DISLODGED),        // pre-dislodged
                                getListForKeyType(POSTSTATE_DISLODGED),        // post-dislodged
                                getListForKeyType(PRESTATE_RESULTS) // results (of prior phase)
                                , templateTurnState
                                , world, OrderParser.getInstance()
                        );
                        cases.add(aCase);
                    } else {
                        if (inCase) {
                            if (currentKey.equals(POSTSTATE_SAME)) {
                                // just copy prestate data
                                List<String> list = getListForKeyType(POSTSTATE);
                                list.addAll(getListForKeyType(PRESTATE));
                            } else if (currentKey.equals(PRESTATE_SETPHASE)) {
                                // phase appears after keyword
                                phaseName = getAfterKeyword(line);
                            } else if (key == null) // important: we don't want to add key lines to the lists
                            {
                                // we need to get a list.
                                List<String> list = getListForKeyType(currentKey);
                                list.add(line);
                            }
                        } else {
                            System.out.println("ERROR: line not enclosed within a CASE.");
                            System.out.println("Line " + lineCount + ": " + rawLine);
                            throw new IllegalStateException("ERROR: line not enclosed within a CASE. " + "Line " + lineCount + ": " + rawLine);
                        }
                    }
                }

                rawLine = br.readLine();
                lineCount++;
            }// while()
        } catch (IOException e) {
            logger.error("I/O error reading case file "+ caseFile,e);
            throw new RuntimeException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e2) {
                }
            }
        }
        logger.info("Parsed {} cases.", cases.size());
    }// parseCases()

    // returns null if string is a comment line.
    private String filterLine(String in) {
        // remove whitespace
        String out = in.trim();

        // find comment-character index, if it exists
        int ccIdx = out.indexOf('#');

        // if entire line is a comment, or empty, return COMMENT_LINE now
        if (ccIdx == 0 || out.length() < 1) {
            return null;
        }

        // remove 'trailing' comments, if any
        // otherwise, it could interfere with order processing.
        if (ccIdx > 0) {
            out = out.substring(0, ccIdx);
        }

        // convert to lower case();
        out = out.toLowerCase();

        return out;
    }// filterLine

    // find first space this works, because the
    // preceding whitespace before a keyword has already been trimmed
    private String getAfterKeyword(String in) {
        int idxSpace = in.indexOf(' ');
        int idxTab = in.indexOf('\t');

        if (idxSpace == -1 && idxTab == -1) {
            return null;
        }

        int idx = 0;

        if (idxSpace == -1 || idxTab == -1) {
            idx = (idxSpace > idxTab) ? idxSpace : idxTab;        // return greater
        } else {
            idx = (idxSpace < idxTab) ? idxSpace : idxTab;        // return lesser
        }

        return in.substring(idx + 1);
    }// getAfterKeyword()

    private void clearAndSetupKeyMap() {
        if (keyMap == null) {
            keyMap = new HashMap<>(23);
        }

        keyMap.clear();

        for (String keyType : KEY_TYPES_WITH_LIST) {
            keyMap.put(keyType, new LinkedList<>());
        }
    }// setupKeyMap()

    /*
        returns:
            true key type type
    */
    private String getKeyType(String line) {
        if (line == null) {
            return null;
        }

        if (line.startsWith(CASE)) {
            return CASE;
        } else if (line.startsWith(END)) {
            return END;
        } else {
            for (String keyType : KEY_TYPES_OTHER) {
                if (line.startsWith(keyType)) {
                    return keyType;
                }
            }
        }

        return null;
    }// getKeyType()

    private List<String> getListForKeyType(String keyType) {
        return keyMap.get(keyType);
    }// getListForKeyType()

    /**
     * Private inner class, usually contained in Sets, that
     * is comparable, for determining if the end-state is
     * in fact correct.
     */
    public static class UnitPos {
        private final Unit unit;            // owner/type/coast
        private final Province province;        // position
        private final boolean isDislodged;    // dislodged?

        /**
         * Create a UnitPos
         */
        public UnitPos(Order ds, boolean isDislodged) {
            this.unit = new Unit(ds.getPower(), ds.getSourceUnitType());
            unit.setCoast(ds.getSource().getCoast());
            this.province = ds.getSource().getProvince();
            this.isDislodged = isDislodged;
        }// UnitPos()

        /**
         * Create a UnitPos
         */
        public UnitPos(Position pos, Province prov, boolean isDislodged) {
            this.province = prov;
            this.isDislodged = isDislodged;
            this.unit = (isDislodged) ? pos.getDislodgedUnit(prov) : pos.getUnit(prov);
            if (this.unit == null) {
                throw new IllegalArgumentException();
            }
        }// UnitPos()

        /**
         * Print
         */
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append(unit.getPower().getName());
            sb.append(' ');
            sb.append(unit.getType().getShortName());
            sb.append(' ');
            sb.append(province.getShortName());
            sb.append('/');
            sb.append(unit.getCoast().getAbbreviation());
            if (isDislodged) {
                sb.append(" [DISLODGED]");
            }
            return sb.toString();
        }// toString()

        /**
         * Compare
         */
        public boolean equals(Object obj) {
            if (obj instanceof UnitPos) {
                UnitPos up = (UnitPos) obj;
                return isDislodged == up.isDislodged
                        && province == up.province
                        && unit.equals(up.unit);
            }

            return false;
        }// equals()

        /**
         * Force all hashes to be the same, so equals() is used
         */
        public int hashCode() {
            return 0;    // very very bad! just an easy shortcut
        }
    }// inner class UnitPos

}// class TestSuite

