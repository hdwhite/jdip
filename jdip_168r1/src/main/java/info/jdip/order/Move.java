// 	
//  @(#)Move.java	4/2002
// 	
//  Copyright 2002-2004 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
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
//  Or from http://www.gnu.org/package info.jdip.order.result;
//
package info.jdip.order;

import info.jdip.misc.Utils;
import info.jdip.order.result.ConvoyPathResult;
import info.jdip.order.result.DependentMoveFailedResult;
import info.jdip.order.result.OrderResult.ResultType;
import info.jdip.process.Adjudicator;
import info.jdip.process.OrderState;
import info.jdip.process.Tristate;
import info.jdip.world.Border;
import info.jdip.world.Location;
import info.jdip.world.Path;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Implementation of the Move order.
 * <p>
 * This has been updated to support the 2003-Dec-16 DATC, specifically,
 * section 4.A.3.
 */
public class Move extends Order {
    private static final Logger logger = LoggerFactory.getLogger(Move.class);
    // il8n constants
    private static final String MOVE_VAL_SRC_EQ_DEST = "MOVE_VAL_SRC_EQ_DEST";
    private static final String MOVE_VAL_UNIT_ADJ = "MOVE_VAL_UNIT_ADJ";
    private static final String MOVE_VAL_ADJ_UNLESS_CONVOY = "MOVE_VAL_ADJ_UNLESS_CONVOY";
    private static final String MOVE_VAL_BAD_ROUTE_SRCDEST = "MOVE_VAL_BAD_ROUTE_SRCDEST";
    private static final String MOVE_VAL_BAD_ROUTE = "MOVE_VAL_BAD_ROUTE";
    private static final String MOVE_VER_NO_ROUTE = "MOVE_VER_NO_ROUTE";
    private static final String MOVE_VER_CONVOY_INTENT = "MOVE_VER_CONVOY_INTENT";
    private static final String MOVE_EVAL_BAD_ROUTE = "MOVE_EVAL_BAD_ROUTE";
    private static final String MOVE_FAILED = "MOVE_FAILED";
    private static final String MOVE_FAILED_NO_SELF_DISLODGE = "MOVE_FAILED_NO_SELF_DISLODGE";
	private static final String MOVE_FAILED_IMPASSABLE = "MOVE_FAILED_IMPASSABLE";
    private static final String MOVE_FORMAT = "MOVE_FORMAT";
    private static final String MOVE_FORMAT_EXPLICIT_CONVOY = "MOVE_FORMAT_EXPLICIT_CONVOY";
    private static final String CONVOY_PATH_MUST_BE_EXPLICIT = "CONVOY_PATH_MUST_BE_EXPLICIT";
    private static final String CONVOY_PATH_MUST_BE_IMPLICIT = "CONVOY_PATH_MUST_BE_IMPLICIT";


    // constants: names
    private static final String ORDER_NAME_BRIEF = "M";
    private static final String ORDER_NAME_FULL = "Move";
    private static final transient String ORDER_FORMAT_STRING = Utils.getLocalString(MOVE_FORMAT);
    private static final transient String ORDER_FORMAT_EX_CON = Utils.getLocalString(MOVE_FORMAT_EXPLICIT_CONVOY);    // explicit convoy format

    // instance variables
    protected Location dest = null;
    protected ArrayList<Province[]> convoyRoutes = null;    // contains *defined* convoy routes; null if none.
    protected boolean isViaConvoy = false;                    // 'true' if army was explicitly ordered to convoy.
    protected boolean isConvoyIntent = false;                // 'true' if we determine that intent is to convoy. MUST be set to same initial value as _isViaConvoy
    protected boolean isAdjWithPossibleConvoy = false;        // 'true' if an army with an adjacent move has a possible convoy route move too
    protected boolean fmtIsAdjWithConvoy = false;            // for OrderFormat ONLY. 'true' if explicit convoy AND has land route.
    protected boolean hasLandRoute = false;                    // 'true' if move has an overland route.

    /**
     * Creates a Move order
     */
    protected Move() {
        super();
    }// Move()

    /**
     * Creates a Move order
     */
    protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest) {
        this(power, src, srcUnitType, dest, false);
    }// Move()

    /**
     * Creates a Move order, with optional convoy preference.
     */
    protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, boolean isConvoying) {
        super(power, src, srcUnitType);

        if (dest == null) {
            throw new IllegalArgumentException("null argument(s)");
        }

        this.dest = dest;
        this.isViaConvoy = isConvoying;
        this.isConvoyIntent = this.isViaConvoy;        // intent: same initial value as _isViaConvoy
    }// Move()

    /**
     * Creates a Move order with an explicit convoy route.
     * The convoyRoute array must have a length of 3 or more, and not be null.
     */
    protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, Province[] convoyRoute) {
        this(power, src, srcUnitType, dest, true);

        if (convoyRoute == null || convoyRoute.length < 3) {
            throw new IllegalArgumentException("bad or missing route");
        }

        convoyRoutes = new ArrayList<>(1);
        convoyRoutes.add(convoyRoute);
    }// Move()


    /**
     * Creates a Move order with multiple explicit convoy routes.
     * Each entry in routes must be a single-dimensional Province array.
     */
    protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, List<Province[]> routes) {
        this(power, src, srcUnitType, dest, true);

        if (routes == null) {
            throw new IllegalArgumentException("null routes");
        }

        // TODO: we don't check the routes very strictly.
        convoyRoutes = new ArrayList<>(routes);
    }// Move()


    /**
     * Returns the destination Location of this Move
     */
    public Location getDest() {
        return dest;
    }


    /**
     * Returns true if this Move was explicitly ordered to be by convoy,
     * either by specifying "by convoy" or "via convoy" after the move
     * order, or, by giving an explicit convoy path.
     * <p>
     * Note that this is <b>not</b> always true for all convoyed moves;
     * to check if a move is convoyed, see isConvoying().
     * <p>
     * Note that explicitly ordering a convoy doesn't really matter
     * unless there are <b>both</b> a land route and a convoy route. See
     * Dec-16-2003 DATC 6.G.8.
     */
    public boolean isViaConvoy() {
        return isViaConvoy;
    }// isExplicitConvoy()


    /**
     * Returns true if an Army can possibly Move to its destination with a convoy,
     * even though it is adjacent to its destination. This is only really important
     * when a Move to an adjacent province could occur by land or by convoy.
     * <p>
     * <b>Important Note:</b> This value will not be properly determined
     * until <code>validate()</code> has been called.
     */
    public boolean isAdjWithPossibleConvoy() {
        return isAdjWithPossibleConvoy;
    }// isAdjWithPossibleConvoy()


    /**
     * Returns true if the Intent of this Move order is to Convoy.
     * This is true when:
     * <ul>
     * <li>isViaConvoy() is false, Source and Dest are not adjacent,
     * both coastal, and there is
     * a theoretical convoy path between them. <b>Note:</b> this can
     * only be determined after <code>validate()</code> has been
     * called.</li>
     * <li>hasDualRoute() is true, isViaConvoy() is false, and there is a
     * matching convoy path between source and dest with at least one
     * Convoying Fleet of the same Power as this Move (thus signalling
     * "intent to Convoy"). <b>Note:</b> this can only be determined
     * after <code>verify()</code> has been called.</li>
     * <li>isViaConvoy is true, and hasDualRoute() are true. This also can
     * only be determined after <code>verify()</code> has been called.</li>
     * </ul>
     * <b>Note:</b> if this method (or isConvoying()) is to be used during
     * the verify() stage by other orders, they <b>absolutely</b> must check that
     * the Move has already been verified, since move verification can change
     * the value of this method.
     */
    public boolean isConvoyIntent() {
        return isConvoyIntent;
    }// isConvoyIntent()


    /**
     * This is implemented for compatibility; it is no different than
     * <code>isConvoyIntent()</code>.
     */
    public boolean isConvoying() {
        return isConvoyIntent();
    }// isConvoying()


    /**
     * Returns, if set, an explicit convoy route (or the first explicit
     * route if there are multiple routes). Returns null if not convoying
     * or no explicit route was defined.
     */
    public Province[] getConvoyRoute() {
        return (convoyRoutes != null) ? convoyRoutes.get(0) : null;
    }// getConvoyRoute()

    /**
     * Returns, if set, all explicit convoy routes as an unmodifiable List.
     * Returns null if not convoying or no explicit route(s) were defined.
     */
    public List<Province[]> getConvoyRoutes() {
        return (convoyRoutes != null) ? Collections.unmodifiableList(convoyRoutes) : null;
    }// getConvoyRoute()


    public String getFullName() {
        return ORDER_NAME_FULL;
    }// getFullName()

    public String getBriefName() {
        return ORDER_NAME_BRIEF;
    }// getBriefName()


    // order formatting
    public String getDefaultFormat() {
        return (convoyRoutes == null) ? ORDER_FORMAT_STRING : ORDER_FORMAT_EX_CON;
    }// getDefaultFormat()


    public String toBriefString() {
        StringBuilder sb = new StringBuilder(64);


        if (convoyRoutes != null) {
            // print all explicit routes
            sb.append(power);
            sb.append(": ");
            sb.append(srcUnitType.getShortName());
            sb.append(' ');
            final int size = convoyRoutes.size();
            for (int i = 0; i < size; i++) {
                final Province[] path = convoyRoutes.get(i);
                formatConvoyRoute(sb, path, true, true);

                // prepare for next path
                if (i < (size - 1)) {
                    sb.append(", ");
                }
            }
        } else {
            super.appendBrief(sb);
            sb.append('-');
            dest.appendBrief(sb);

            if (isViaConvoy()) {
                sb.append(" by convoy");
            }
        }

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuilder sb = new StringBuilder(128);

        if (convoyRoutes != null) {
            // print all explicit routes
            sb.append(power);
            sb.append(": ");
            sb.append(srcUnitType.getFullName());
            sb.append(' ');
            final int size = convoyRoutes.size();
            for (int i = 0; i < size; i++) {
                final Province[] path = convoyRoutes.get(i);
                formatConvoyRoute(sb, path, false, true);

                // prepare for next path
                if (i < (size - 1)) {
                    sb.append(", ");
                }
            }
        } else {
            super.appendFull(sb);
            sb.append(" -> ");
            dest.appendFull(sb);

            if (isViaConvoy()) {
                sb.append(" by convoy");
            }
        }

        return sb.toString();
    }// toFullString()


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Move) {
            Move move = (Move) obj;
            return super.equals(move)
                    && this.dest.equals(move.dest)
                    && this.isViaConvoy() == move.isViaConvoy();
        }
        return false;
    }// equals()


    @Override
    public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
            throws OrderException {
        // NOTE: the first time we validate(), _isViaConvoy == _isConvoyIntent.
        // if we re-validate, that assertion may not be true.

        // basic checks
        //
        checkSeasonMovement(state, ORDER_NAME_FULL);
        checkPower(power, state, true);
        super.validate(state, valOpts, ruleOpts);

        // first, validate the unit type and destination, if we are
        // using strict validation.
        //
        if (valOpts.getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE)) {
            return;
        }

        final Position position = state.getPosition();

        // a.1
        if (src.isProvinceEqual(dest)) {
            throw new OrderException(Utils.getLocalString(MOVE_VAL_SRC_EQ_DEST));
        }

        // validate Borders
        Border border = src.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
        if (border != null) {
            throw new OrderException(Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()));
        }

        // a.2
        dest = dest.getValidatedWithMove(srcUnitType, src);

        // check that we can transit into destination (check borders)
        border = dest.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
        if (border != null) {
            throw new OrderException(Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()));
        }

        // Determine convoying intent for nonadjacent moves that are not explicitly
        // convoyed (e.g., isViaConvoy() == false). All nonadjacent fleet moves
        // fail. All nonadjacent army moves without theoretical convoy paths
        // also fail. _isConvoyIntent is set if there is a theoretical convoy route
        // and we are not explicitly ordered to convoy.
        //
        if (!src.isAdjacent(dest)) {
            // nonadjacent moves with Fleets/Wings always fail (cannot convoy fleets)
            if (srcUnitType != Unit.Type.ARMY) {
                throw new OrderException(Utils.getLocalString(MOVE_VAL_UNIT_ADJ, srcUnitType.getFullNameWithArticle()));
            }

            // determine if explicit/implicit convoys are required
            final RuleOptions.OptionValue convoyRule = ruleOpts.getOptionValue(RuleOptions.OPTION_CONVOYED_MOVES);
            if (convoyRule == RuleOptions.VALUE_PATHS_EXPLICIT && convoyRoutes == null) {
                // no explicit route defined, and at least one should be
                throw new OrderException(Utils.getLocalString(CONVOY_PATH_MUST_BE_EXPLICIT));
            }
            if (convoyRule == RuleOptions.VALUE_PATHS_IMPLICIT && convoyRoutes != null) {
                // explicit route IS defined, and shouldn't be
                throw new OrderException(Utils.getLocalString(CONVOY_PATH_MUST_BE_IMPLICIT));
            }

            // nonadjacent moves must have a theoretical convoy path!
            // (this throws an exception if there is no theoretical convoy path)
            validateTheoreticalConvoyRoute(position);

            // we didn't fail; thus, we intend to convoy (because it is at least possible).
            if (!isViaConvoy()) {
                isConvoyIntent = true;
            }
        } else {
            // we are adjacent
            //
            // _isAdjWithPossibleConvoy is true iff we are both adjacent
            // (after all validation/borders checked) and an army, and
            // there is a theoretical convoy path from src->dest. Also,
            // this CANNOT be true if we are EXPLICITLY being convoyed
            // (isViaConvoy() == true); in that case, the convoy is preferred
            // and will be used despite the land route.
            //
            if (!isViaConvoy()
                    && srcUnitType == Unit.Type.ARMY) {
                Path path = new Path(position);
                isAdjWithPossibleConvoy = path.isPossibleConvoyRoute(src, dest);
            }

            // for order format:
            // set _fmtIsAdjWithConvoy iff we are EXPLICITLY ordered to convoy,
            // and we are an adjacent move (we are an adjacent move if we
            // reached this point in the code)
            fmtIsAdjWithConvoy = isViaConvoy();

            // set if we can move via a land route.
            hasLandRoute = true;
        }
    }// validate()


    /**
     * Determines if this move has a theoretical explicit or implicit
     * convoy route. Throws an exception if
     * <p>
     * This will only throw an OrderException if there is an Explicit
     * convoy path that is bad (doesn't contain src and dest in route,
     * or doesn't form a route from src->dest), or if there is no
     * implicit theoretical route from src->dest.
     * <p>
     * An implicit route is assumed if no explicit route has been set.
     */
    protected void validateTheoreticalConvoyRoute(Position position)
            throws OrderException {
        if (convoyRoutes != null) {
            // if we have defined routes, check all of them to make sure
            // they are (all) theoretically valid
            for (final Province[] route : convoyRoutes) {
                // check that src, dest are included in path
                if (route[0] != src.getProvince()
                        || route[route.length - 1] != dest.getProvince()) {
                    throw new OrderException(Utils.getLocalString(MOVE_VAL_BAD_ROUTE_SRCDEST,
                            formatConvoyRoute(route, true, false)));
                }

                // check route validity
                if (!Path.isRouteValid(position, src, dest, route)) {
                    throw new OrderException(Utils.getLocalString(MOVE_VAL_BAD_ROUTE,
                            formatConvoyRoute(route, true, false)));
                }
            }
        } else {
            // check that a *possible* convoy path exists
            // (enough fleets to span src-dest)
            Path path = new Path(position);
            if (!path.isPossibleConvoyRoute(src, dest)) {
                throw new OrderException(Utils.getLocalString(MOVE_VAL_ADJ_UNLESS_CONVOY));
            }
        }
    }// validateTheoreticalConvoyRoute()


    /**
     * Format a convoy route into a String
     */
    protected String formatConvoyRoute(final Province[] route, boolean isBrief, boolean useHyphen) {
        StringBuilder sb = new StringBuilder(128);
        formatConvoyRoute(sb, route, isBrief, useHyphen);
        return sb.toString();
    }// formatConvoyRoute()


    /**
     * Format a convoy route into a StringBuilder
     */
    protected void formatConvoyRoute(StringBuilder sb, final Province[] route, boolean isBrief, boolean useHyphen) {
        if (isBrief) {
            sb.append(route[0].getShortName());
        } else {
            sb.append(route[0].getFullName());
        }

        for (int i = 1; i < route.length; i++) {
            sb.append(useHyphen ? '-' : " -> ");

            if (isBrief) {
                sb.append(route[i].getShortName());
            } else {
                sb.append(route[i].getFullName());
            }
        }
    }// formatConvoyRoute


    /**
     * Verify this move given completely-known game state.
     * <p>
     * Verification must always be performed after strict order validation.
     * <p>
     * Verify does the following for Move orders:
     * <ul>
     * <li>Moves involving Fleets or Wings always verify successfully.</li>
     * <li>Moves involving armies without theoretical convoy routes
     * always verify successfully.</li>
     * <li>Moves with theoretical convoy routes are evaluated as such:</li>
     * <ul>
     * <li>Moves with explicit convoy routes or "convoy-preferred" moves
     * (isViaConvoy() is true) are evaluated to make sure there is
     * a Legal convoy route. If there is no convoy route, but a
     * land route is available, the land route is used (DATC case
     * 6.G.8)</li>
     * <li>Adjacent moves that have a possible ("theoretical") convoy route
     * (therefore, isAdjWithPossibleConvoy() is true) are evaluated to
     * determine intent (2000 rules/DATC 4.A.3);
     * an army will move by land unless there is "intent to convoy".
     * Intent to Convoy is true iff there is both a Legal convoy
     * route <b>and</b> one of the Fleets in that route is of the
     * same Power as the Move order we are evaluating.</li>
     * </ul>
     * </ul>
     * <p>
     * <b>Legal</b> convoy routes are defined as a possible (or "theoretical")
     * convoy route (i.e., an unbroken chain of adjacent fleets briding the source
     * and	destination), that also have Convoy orders that match this Move.
     */
    public void verify(Adjudicator adjudicator) {
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        if (thisOS.isVerified()) {
            return;
        }

        // if we have already failed, do not evaluate.
        if (thisOS.getEvalState() != Tristate.UNCERTAIN) {
            thisOS.setVerified(true);
            return;
        }

        if (isConvoying())    // intent to convoy already determined (e.g., _isViaConvoy is true, so _isConvoyIntent initiall is true)
        {
            if (convoyRoutes != null) // static (explicit) paths
            {
                // if we have multiple routes, we don't fail until *all* paths fail.
                boolean overall = false;
                for (final Province[] route : convoyRoutes) {
                    overall = Path.isRouteLegal(adjudicator, route);
                    if (overall)    // if at least one is true, then we are OK
                    {
                        break;
                    }
                }

                if (!overall) {
                    // if we are explicitly being convoyed, and there is a land route,
                    // but no convoy route, we use the land route.
                    //
                    if (isViaConvoy() && hasLandRoute) {
                        // we don't fail, but mention that there is no convoy route. (text order result)
                        isConvoyIntent = false;
                        adjudicator.addResult(thisOS, Utils.getLocalString(MOVE_VER_NO_ROUTE));
                    } else {
                        // all paths failed.
                        thisOS.setEvalState(Tristate.FAILURE);
                        adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_VER_NO_ROUTE));
                    }
                }
            } else    // implicit path
            {
                Path path = new Path(adjudicator);
                if (!path.isLegalConvoyRoute(getSource(), getDest())) {
                    // As for static (explicit) paths, if we are explicitly
                    // ordered to convoy ("by convoy") and there is a land route,
                    // but no convoy route, we use the land route.
                    //
                    if (isViaConvoy() && hasLandRoute) {
                        isConvoyIntent = false;
                        adjudicator.addResult(thisOS, Utils.getLocalString(MOVE_VER_NO_ROUTE));
                    } else {
                        thisOS.setEvalState(Tristate.FAILURE);
                        adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_VER_NO_ROUTE));
                    }
                }
            }
        } else if (isAdjWithPossibleConvoy())    // intent must be determined
        {
            // first, we need to find all paths with possible convoy orders
            // between src and dest. If we have an order, by the same power,
            // on ONE of these paths, then intent to convoy will be 'true'
            //
            // Note: this could be put in validate(), where _isAdjWithPossibleConvoy
            // is set, for efficiency reasons. However, it is more appropriate and
            // makes more sense here.
            //
            final Province srcProv = getSource().getProvince();
            final Province destProv = getDest().getProvince();
            final Position pos = adjudicator.getTurnState().getPosition();
            Path.FAPEvaluator evaluator = new Path.FleetFAPEvaluator(pos);
            Province[][] paths = Path.findAllSeaPaths(evaluator, srcProv, destProv);

            // now, we need to evaluate each path, to see if that province
            // has a fleet of the same power as this order in any legal path.
            // If so, the intent is to convoy.
            for (Province[] path : paths) {
                Province p = evalPath(adjudicator, path);
                if (p != null) {
                    isConvoyIntent = true;
                    adjudicator.addResult(thisOS, ResultType.TEXT, Utils.getLocalString(MOVE_VER_CONVOY_INTENT, p));
                    break;
                }
            }
        }

        // we have been verified.
        thisOS.setVerified(true);
    }// verify()


    /**
     * Evaluate a Province path (length must be >= 3)
     * for the presence of a Fleet of the given Power.
     * <p>
     * This does NOT check to see if the Fleet was ordered
     * to convoy, or if that convoy order matches a particular
     * Move order.
     * <p>
     * Returns the Province with the Fleet of own Power if found;
     * otherwise returns null..
     */
    private Province evalPath(Position pos, final Province[] path, Power fleetPower) {
        if (path.length >= 3) {
            for (int i = 1; i < (path.length - 1); i++) {
                Unit unit = pos.getUnit(path[i]);
                if (unit.getPower().equals(fleetPower)) {
                    return path[i];
                }
            }
        }

        return null;
    }// evalPath()


    /**
     * Evaluate a Province path (length must be >= 3)
     * for the presence of a Fleet of the given Power with
     * appropriate convoy orders. This assumes the given Path
     * contains provinces with Fleets.
     * <p>
     * This <b>does</b> check to see if the Fleet was ordered
     * to convoy, and that that convoy order matches <b>this</b>
     * Move order.
     * <p>
     * Returns the Province with the Fleet of own Power if found;
     * otherwise returns null..
     */
    private Province evalPath(Adjudicator adj, final Province[] path) {
        if (path.length < 3) {
            return null;
        }
        
        final Position pos = adj.getTurnState().getPosition();

        for (int i = 1; i < (path.length - 1); i++) {
            Province prov = path[i];
            Unit unit = pos.getUnit(path[i]);
            if (unit.getPower().equals(this.getPower())) {
                final OrderState os = adj.findOrderStateBySrc(prov);
                final Order order = os.getOrder();
                if (order instanceof Convoy) {
                    final Convoy convoy = (Convoy) order;
                    if (convoy.getConvoySrc().isProvinceEqual(this.getSource())
                            && convoy.getConvoyDest().isProvinceEqual(this.getDest())) {
                        return prov;
                    }
                }
            }
        }

        return null;
    }// evalPath()


    /**
     * Dependencies for a Move order:
     * <ol>
     * <li><b>NOT ADDED:</b>
     * <ol>
     * <li>convoy route, if it is a convoyed move<br>
     * note that while a move would depend upon a convoy route,
     * individual convoy orders are not helpful because there may be
     * multiple paths to a destination. A Path object and path iterator
     * is used to determine convoy-dependency, as required.
     *
     * <li>Moves to this space<br>
     * we are not concerned with moves to this space, unless it is a head-to-head
     * move, which is taken care of below, by setting OrderState appropriately.
     * <p>
     * Move is a special case. Since calculating the 'moves' is the difficult
     * part, when a move is evaluated, the move looks at it's destination space.
     * </ol>
     * <li><b>ADDED:</b>
     * <ol>
     * <li>Supports of this move
     * <li>Moves to destination space
     * </ol>
     * </ol>
     * <p>
     * If destination is a move order, then that move order must be evaluated
     * for an order to succeed. (taken care of by evaluate)
     * <p>
     * We also determine if this is a head-to-head move order; if so,
     * the head-to-head flag of OrderState is set. A Head-To-Head move is
     * defined as:
     * <pre>
     * 	 	A->B and B->A, where neither B nor A is convoyed.
     * 	</pre>
     * Note that head-to-head determination may not be complete until
     * verification is complete, as it depends upon whether this and/or an
     * opposing move is convoyed.
     */
    public void determineDependencies(Adjudicator adjudicator) {
        // add moves to destination space, and supports of this space
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        ArrayList<OrderState> depMTDest = null;
        ArrayList<OrderState> depSup = null;
        ArrayList<OrderState> depSelfSup = null;

        OrderState[] orderStates = adjudicator.getOrderStates();
        for (OrderState dependentOS : orderStates) {
            Order order = dependentOS.getOrder();

            if (order instanceof Move && order != this) {
                Move move = (Move) order;

                // move to *destination* space (that are not this order)
                if (move.getDest().isProvinceEqual(this.getDest())) {
                    if (depMTDest == null) {
                        depMTDest = new ArrayList<>(5);
                    }
                    depMTDest.add(dependentOS);
                }

                // check if this is a head-to-head move
                // note that isConvoying() may not yet be properly set, so the
                // "headToHeadness" will have to be re-evaluated sometime AFTER
                // order verification (via verify()) has been performed.
                if (move.getDest().isProvinceEqual(this.getSource())
                        && move.getSource().isProvinceEqual(this.getDest())
                        && !this.isConvoying() && !move.isConvoying()) {
                    logger.info("Head2Head possible between {} and {}.", this, dependentOS.getOrder());
                    thisOS.setHeadToHead(dependentOS);
                }
            } else if (order instanceof Support) {
                Support support = (Support) order;
                if (support.getSupportedSrc().isProvinceEqual(this.getSource())
                        && support.getSupportedDest().isProvinceEqual(this.getDest())) {
                    if (adjudicator.isSelfSupportedMove(dependentOS)) {
                        if (depSelfSup == null) {
                            depSelfSup = new ArrayList<>(5);
                        }
                        depSelfSup.add(dependentOS);
                    } else {
                        if (depSup == null) {
                            depSup = new ArrayList<>(5);
                        }
                        depSup.add(dependentOS);
                    }
                }
            }
        }

        // set supports / competing moves in OrderState
        if (depMTDest != null) {
            thisOS.setDependentMovesToDestination(depMTDest);
        }

        if (depSup != null) {
            thisOS.setDependentSupports(depSup);
        }

        if (depSelfSup != null) {
            thisOS.setDependentSelfSupports(depSelfSup);
        }
    }// determineDependencies()


    /**
     * NOTE: this description may be slightly out of date
     * <pre>
     *
     * evaluation of Move orders. The algorithm is as follows:
     * ======================================================
     * 1) calculate support of this move (certain & uncertain)
     *
     * 2) if this is a convoyed move, evaluate convoy route
     *     a) if convoy route fails, move fails
     *     b) if convoy route uncertain, cannot evaluate move yet
     *     c) if convoy route ok, move can be evaluated.
     *
     * 3) determine order of unit in destination space
     *     a) no unit, or unit with Support/Convoy/Hold order
     *         1) calculate strengths of all other moves to destination (if present)
     *            this is calculated by the evaluate() method for each Move order, so
     *            several iterations may be required before strengths are in the 'useful'
     *            area.
     *         2) calculate defending unit strength (if present; if not, defense_max == 0)
     *            this is calculated by the evaluate() method for the respective order, similar
     *            to 3.a.1
     *         3) compare attack_certain (of this move) to attack_max of all other attacks,
     *            and defense_max of destination.
     *             a) if another Move order to destination succeeded, then FAILURE
     *             b) if attack_certain > *all* of the defenders' (attack_max && defense_max)
     *                SUCCESS, unless defender is of the same power, in which case FAILURE
     *                if SUCCESS, defender (if present) is dislodged
     *             c) if attack_max <= *any* of the defenders' attack_certain or defense_certain
     *                FAILURE (since there would be no way to overcome this difference,
     *                regardless of support!)
     *                this is a "BOUNCE" [note: this is a key point]
     *             d) otherwise, we remain UNCERTAIN
     *     b) self support
     *        in cases with self support, the following changes to 3.a are noted below:
     *         1) self support is used to determine strength against other moves (standoffs)
     *            to the destination province.
     *         2) self support is NOT used to determine strength against *this* move to the
     *            destination province. Self-support can never be used to dislodge a unit,
     *            however, if a unit has enough strength to dislodge, self-support does not
     *            prohibit dislodgement.
     *
     *     MODIFICATION (6/2/02): Self support *may* be used to determine the strength
     *     of this move to the destination province; if the unit in the destination province
     *     has succesfully moved out, we must compare against all other moves to dest (as in 1) but the
     *     self support can cause us to prevail against other moves to dest as well. Self-support
     *     cannot be used in the dislodge calculation, nor can it prohibit dislodgement.
     *
     *     c) unit with Move order, NOT head-to-head (see below for definition)
     *        evaluate as 3.a.1-3 however:
     *         1)	if we are stronger: (guaranteed success, unless self)
     *             a) if destination move FAILURE, unit is dislodged, unless self; if self, we fail
     *             b) if destination move SUCCESS, unit is not dislodged, we succeed (if self or not)
     *             c) if destination move UNCERTAIN, unit is "maybe" dislodged, unless self;
     *                if self, we remain uncertain
     *         2)	if we are not stronger  (equal-strength)
     *             a) we fail, ONLY if we are 'definately' not stronger (atk.max < def.certain)
     *             b) if destination move SUCCESS, we succeed
     *             c) if destination move UNCERTAIN, we remain uncertain.
     *     d) unit is a head-to-head MOVE order
     *        definition: 	destination unit is moving to this space, and NEITHER unit is convoyed
     *        (note: this is set when dependencies are determined)
     *         1) evaluate as 3.a.1-3, with the following caveats applied to this vs. head-to-head move:
     *            - we use atk_certain/atk_max of 'head-to-head' unit
     *             a) same as 3.a.3.a
     *             b) same as 3.a.3.b [opposing unit dislodged; NOT a 'maybe' dislodged]
     *                BUT, opposing unit move is marked FAILURE
     *             c) same as 3.a.3.c
     *             d) same as 3.a.3.d
     *     e) comparing against head to head:
     *        if comparing against a head-to-head battle, where a unit may be dislodged, remain
     *        uncertain until we know if the unit is dislodged; if unit dislodged by head-to-head
     *        player, it cannot affect other battles
     *        A->B
     *        B->A
     *        D->B	(and dislodges B)	: no change
     *        HOWEVER,
     *        A->B, B->A, C->A, and A dislodges B (head to head), C can move to A.
     *        B does not standoff C because it was dislodged by A in a head-to-head battle.
     *        This is seen in DATC cases 5.A and 7.H (if no "by convoy" is used).
     *         1) isHeadToHead() && EvalState == UNCERTAIN *or* head-to-head part unevaluated:
     *            UNCERTAIN result.
     *         2) isHeadToHead() && EvalState == FAILURE:
     *            if disloger == head-to-head, then ignore (do nothing)
     *         3) otherwise, process normally.
     *
     *
     *
     *
     * IMPORTANT: 	A move that is 'dislodged' or 'fails' may still offset other moves, and
     * still cut support.
     *
     * ALSO IMPORTANT: we create dislodged results, instead of failed results, for moves
     * that are definately dislodged. The adjudicator will create, later,
     * dislodged results for 'maybe' dislodged orders.
     * </pre>
     */
    public void evaluate(Adjudicator adjudicator) {
        logger.trace( "--- evaluate() info.jdip.order.Move ---");

        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        // 1) calculate support of this Move
        Move order = (Move) thisOS.getOrder();
        int mod = order.getDest().getProvince().getBaseMoveModifier(getSource());
        // If the move isn't by convoy, perhaps there are border issues to deal with.
        if (!isConvoying()) {
            thisOS.setAtkMax(thisOS.getSupport(false));
            thisOS.setAtkCertain(thisOS.getSupport(true));
        } else {
            // Bypass the modification to AtkMax, AtkCertain
            // If the mod was positive, subtracting it will take it away.
            // If the mod was negitive, subtracting it will add it back.
            thisOS.setAtkMax(thisOS.getSupport(false) - mod);
            thisOS.setAtkCertain(thisOS.getSupport(true) - mod);
        }
        thisOS.setAtkSelfSupportMax(thisOS.getSelfSupport(false));
        thisOS.setAtkSelfSupportCertain(thisOS.getSelfSupport(true));

        logger.debug("Order: {}", this);
        logger.debug("Initial evalstate: {}, atk-max: {}, atk-cert: {}, self-atk-max: {}, self-atk-cert: {}, nonself supports: {}, self supports {}, dislodged: {}",
                thisOS.getEvalState(),
                thisOS.getAtkMax(),
                thisOS.getAtkCertain(),
                thisOS.getAtkSelfSupportMax(),
                thisOS.getAtkSelfSupportCertain(),
                thisOS.getDependentSupports().length,
                thisOS.getDependentSelfSupports().length,
                thisOS.getDislodgedState()
        );

        // evaluate
        if (thisOS.getEvalState() != Tristate.UNCERTAIN) {
            // If we have been marked as a 'maybe dislodged' and we are successfull,
            // we cannot be dislodged.
            if (thisOS.getEvalState() == Tristate.SUCCESS && thisOS.getDislodgedState() == Tristate.MAYBE) {
                logger.info( "successfull; MAYBE dislodged converted to NOT dislodged.");
                thisOS.setDislodgedState(Tristate.NO);
            }
            return;
        }

        logger.debug("final evalState(): {}", thisOS.getEvalState());
        // moves to impassable spaces fail
        if(order.getDest().getProvince().isImpassable())
        {
            thisOS.setEvalState(Tristate.FAILURE);
            logger.debug("Failed. (destination is impassable)");
            adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED_IMPASSABLE));
            return;
        }

        // re-evaluate head-to-head status. we may be convoyed, so,
        // this could be a head-to-head move.
        //
        if (thisOS.isHeadToHead()) {
            Move h2hMove = (Move) thisOS.getHeadToHead().getOrder();
            if (this.isConvoying() || h2hMove.isConvoying()) {
                // we need to change h2h status!
                logger.debug( "HeadToHead removed (convoy detected)");
                thisOS.setHeadToHead(null);
            }
        }

        // 2.a-c
        if (isConvoying()) {
            // NOTE: convoy path result may return 'false' if we are uncertain.
            Path path = new Path(adjudicator);
            Tristate convoyPathResult = path.getConvoyRouteEvaluation(this, null, null);

            logger.debug("isByConvoy() true; convoyPathRouteEval(): {}", convoyPathResult);

            if (convoyPathResult == Tristate.FAILURE) {
                // 2.a
                thisOS.setEvalState(Tristate.FAILURE);
                adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_EVAL_BAD_ROUTE));
                return;
            } else if (convoyPathResult == Tristate.UNCERTAIN) {
                return;    // 2.b (can't evaluate this move yet!)
            }
            
            if (!thisOS.hasFoundConvoyPath()) {
                // else: we just continue (2.c)
                // HOWEVER, we can indicate the path taken as a result of this move,
                // if we haven't already.
                //
                List<Province> validPath = new ArrayList<>(10);
                path.getConvoyRouteEvaluation(this, null, validPath);
                adjudicator.addResult(new ConvoyPathResult(this, validPath));
                thisOS.setFoundConvoyPath(true);
            }
        }

        // setup: 3.a, 3.b, and 3.c are very similar, except for how dislodged units are
        // handled. To use the same basic logic, we must determine some things up front.
        OrderState destOS = adjudicator.findOrderStateBySrc(getDest());

        logger.debug("isDestAMove: {}, isDestEmpty: {}, isHeadToHead: {}",
                destOS != null && destOS.getOrder() instanceof Move, destOS == null, thisOS.isHeadToHead());

        // 3.a.3
        //
        // note: this block will complete 3.a.3.a (only applies to other moves to destination)
        // first, compare to 'all other' moves to the destination province
        // this must be done for all cases
        // "dml" = "destination move list"
        boolean isBetterThanAllOtherMoves = true;

        OrderState[] dml = thisOS.getDependentMovesToDestination();

        logger.info("Dependent moves to destination: {}", dml.length);

        for (OrderState os : dml) {
            Tristate[] compareResults = compareWithOrder(adjudicator, thisOS, os);
            thisOS.setEvalState(compareResults[0]);
            isBetterThanAllOtherMoves &= (compareResults[1] == Tristate.TRUE);
            if (compareResults[2] == Tristate.YES) {
                return;
            }
        }// while()

        logger.debug("isBetterThanAllOtherMoves: {}", isBetterThanAllOtherMoves);


        // Note that if we are not better than all other moves to the destination province, we
        // cannot be successful, but we can be *unsuccessful* if we are definately (certainly)
        // worse then the defending unit, if any.
        //
        // 3.a.3.c: for defending unit (if present)
        // see if we are "definately worse"
        // we don't check destination NON-head-to-head moves, since they defend at strength==1. And
        // we attack at (minimum) strength==1. Thus we can never be "definately worse", (but we can tie).
        if (destOS != null) {
            if (thisOS.isHeadToHead()) {
                if (thisOS.getAtkMax() <= thisOS.getHeadToHead().getAtkCertain() + thisOS.getHeadToHead().getAtkSelfSupportCertain()) {
                    thisOS.setEvalState(Tristate.FAILURE);
                    adjudicator.addBouncedResult(thisOS, thisOS.getHeadToHead());
                    logger.debug("(hth) final evalState(): {}", thisOS.getEvalState());
                    return;
                }
            } else if (!(destOS.getOrder() instanceof Move) && thisOS.getAtkMax() <= destOS.getDefCertain()) { // less priority than isHeadToHead()
                thisOS.setEvalState(Tristate.FAILURE);
                adjudicator.addBouncedResult(thisOS, destOS);
                logger.debug("(dam) final evalState(): {}", thisOS.getEvalState());
                return;
            }
        }

        // at this point, 3.a.3.a is complete, and 3.a.3.c is complete (for defender & other move orders).
        // however, we must complete 3.a.3.b
        //
        // now compare to the destination province.
        // there are 4 cases: 1) empty, 2) Move, 3) head-to-head Move, and 4) (support/hold/convoy)
        // each is similar, but case 1 always succeeds (depending on other moves, above), cases
        // 2 & 4 are similar except for dislodge calculations, and case 3 is similar except the
        // 'attack' instead of 'defense' parameter is used, since it itself is a move.
        if (isBetterThanAllOtherMoves) {
            thisOS.setEvalState(compareWithDestination(adjudicator, thisOS));
        }// if(isBetterThanAllOtherMoves)

        // If we have been marked as a 'maybe dislodged' and we are successfull,
        // we cannot be dislodged.
        if (thisOS.getEvalState() == Tristate.SUCCESS && thisOS.getDislodgedState() == Tristate.MAYBE) {
            logger.info( "successfull; MAYBE dislodged converted to NOT dislodged.");
            thisOS.setDislodgedState(Tristate.NO);
        }

        logger.debug("final evalState(): {}", thisOS.getEvalState());
    }// evaluate()

    // Returns a 3-member Tristate array. The first is the order success,
    // the second is if isBetterThanAllOtherMoves should become false,
    // and the third is if we should stop evaluation.
    private Tristate[] compareWithOrder(Adjudicator adjudicator, OrderState thisOS, OrderState os)
    {
        logger.debug("Checking against dependent move:{} ", os.getOrder());
        logger.debug("atkMax: {}, atkCertain: {}, selfAtkMax: {}, selfAtkCertain: {}, isHeadToHead: {}, evalState: {}, dislodger: {}",
                os.getAtkMax(), os.getAtkCertain(),
                os.getAtkSelfSupportMax(), os.getAtkSelfSupportCertain(),
                os.isHeadToHead(), os.getEvalState(),
                os.getDislodger() != null ? os.getDislodger().getOrder() : null
        );

        if (os.getEvalState() == Tristate.SUCCESS) {
            // 3.a.3.a: someone's already better than us.
            logger.debug( "they're better than us!");
            adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED));
            return new Tristate[] {Tristate.FAILURE, Tristate.FALSE, Tristate.YES};
        }
        // other order is UNCERTAIN or FAILURE eval state
        // 3.d
        if (os.isHeadToHead() && (os.getEvalState() == Tristate.UNCERTAIN || !isDependentHTHResolved(os))) {
            // we can't evaluate yet; remain uncertain (3.d.1)
            logger.debug( "can't tell if head-to-head battle caused dislodgement!");
            return new Tristate[] {thisOS.getEvalState(), Tristate.TRUE, Tristate.NO};
        }
        
        // os.isHeadToHead() && os.getDislodger() == os.getHeadToHead() :: we ignore the unit! (3.d.2)
        if (os.isHeadToHead() && os.getDislodger() == os.getHeadToHead()) {
            return new Tristate[] {thisOS.getEvalState(), Tristate.TRUE, Tristate.NO};
        }

        /*
            This section has been re-written to take care of bugs
            1116568 & 1053458 (which are the same bug). 
            
            TODO: clean up/simplify
        */

        // 3.b.1, 3.b.2 are accounted for within this else block
        //
        if ((thisOS.getAtkMax() + thisOS.getAtkSelfSupportMax())
            <= (os.getAtkCertain() + os.getAtkSelfSupportCertain())) {
            logger.debug( "attack_max <= os.getAtkCertain() + getAtkSelfSupportCertain() ...");
            
            /*
                If the other move has not found a convoy route, 
                then we will *not* automatically fail.
                
                remember: with logical and (&&) we only evaluate the
                second argument if the first is true.
            */
            if (((Move) os.getOrder()).isConvoying() && !os.hasFoundConvoyPath()) {
                logger.debug( "however, no convoy route for dependent move exists (or exists yet).");
            } else {
                logger.debug( "so we must fail.");
                // 3.a.3.c: we can never be better than this pairing. Ever. Fail, unless destination
                // is part of a head-to-head battle which was dislodged by a unit involved in the
                // head-to-head battle. [3.d]
                adjudicator.addBouncedResult(thisOS, os);
                return new Tristate[] {Tristate.FAILURE, Tristate.FALSE, Tristate.YES};
            }
        }

        if ((thisOS.getAtkCertain() + thisOS.getAtkSelfSupportCertain())
            <= (os.getAtkMax() + os.getAtkSelfSupportMax())) {
            /*
                We will ignore the compared move if it is a convoying move
                and no convoy route was found.
                (isconvoying == true, hasFoundConvoyPath == false, and evalstate == false)
            */
            if (((Move) os.getOrder()).isConvoying() && !os.hasFoundConvoyPath()
                    && os.getEvalState() == Tristate.FAILURE) {
                logger.debug( "dependent move ignored (no valid corresponding convoy path).");
            } else {
                // 3.a.3.b: we are not better than *all* the unevaluated moves to destination
                // this doesn't mean we fail, though, since the other moves strength calculations
                // may not be final
                logger.debug( "atk_certain <= os.getAtkMax() + getAtkSelfSupportMax(); not conclusively better!");
                return new Tristate[] {thisOS.getEvalState(), Tristate.FALSE, Tristate.NO};
            }
        }
        // "else" 
        return new Tristate[] {thisOS.getEvalState(), Tristate.TRUE, Tristate.NO};
    }

    private Tristate compareWithDestination(Adjudicator adjudicator, OrderState thisOS)
    {
        OrderState destOS = adjudicator.findOrderStateBySrc(getDest());
        // see if we are better w/o self support.
        // this will influence dislodges
        final boolean isBwoss = isBetterWithoutSelfSupport(thisOS);
        logger.debug("isBetterWithoutSelfSupport(): {}", isBwoss);

        if (destOS == null) {
            // 3.a.3.b: case 1. [empty province: special case of 3.a.3.b]
            logger.debug("isDestEmpty(): prior eval state: {}", thisOS.getEvalState());
            return Tristate.SUCCESS;
        }
        
        // 3.a.3.b: case 3. [also known as: 3.a.3.c.1.b]
        // CHANGED: 10/2002 to fix a couple of bugs
        if (thisOS.isHeadToHead()) {
            logger.debug( "isHTH evaluation");
            OrderState hthOS = thisOS.getHeadToHead();
            if (thisOS.getAtkCertain() > (hthOS.getAtkMax() + hthOS.getAtkSelfSupportMax())) {
                if (!isBwoss || isDestSamePower(hthOS)) {
                    adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED_NO_SELF_DISLODGE));
                    return Tristate.FAILURE; // we fail--no self dislodgement!
                }
                hthOS.setDislodgedState(Tristate.YES);    // they are dislodged
                hthOS.setDislodger(thisOS);
                adjudicator.addDislodgedResult(hthOS);

                if (hthOS.getEvalState() == Tristate.UNCERTAIN) {
                    hthOS.setEvalState(Tristate.FAILURE);    // they lose
                }
                return Tristate.SUCCESS;        // we win
            }
            return Tristate.UNCERTAIN;
        }
        
        if (destOS.getOrder() instanceof Move) {
            logger.debug( "dest is a Move");
            if (destOS.getEvalState() == Tristate.SUCCESS) {
                // regardless of our strength (1 or >1) we will succeed if destination unit moved out.
                // this covers parts of 3.a.3.b/4 and 3.b.2 self support
                return Tristate.SUCCESS;
            }
            
            if (thisOS.getAtkCertain() == 1) {
                // 3.a.3.b: case 4	[typical case of 3.a.3.b]
                // if destination evalstate is uncertain, we too are uncertain
                // we only fail for certain iff we are 'definately weaker'
                // which is defined as attack_max <= defense_certain
                // remember, our "certain support" could increase later
                if (destOS.getEvalState() == Tristate.FAILURE && thisOS.getAtkMax() <= 1) {
                    adjudicator.addResult(new DependentMoveFailedResult(thisOS.getOrder(), destOS.getOrder()));
                    return Tristate.FAILURE;
                }
                return Tristate.UNCERTAIN;    // else: we remain uncertain.
            }
            // now we are covering attack_certain > 1, and dest eval state is not a success
            //
            // 3.a.3.b.1: we are stronger; we will succeed, regardless of destination move result.
            // unless, of course, we could be dislodging ourselves. In that case, we cannot
            // complete the evaluation.
            if (isDestSamePower(destOS)) {
                logger.debug( "dest is the same power!");

                // cannot dislodge self; but we will succeed unless other unit failed; if
                // other unit is uncertain, then we remain uncertain.
                if (destOS.getEvalState() == Tristate.SUCCESS) {
                    logger.debug( "but left the province.");
                    return Tristate.SUCCESS;
                }
                
                if (destOS.getEvalState() == Tristate.FAILURE) {
                    logger.debug( "and failed, so we can't self-dislodged!.");
                    adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED_NO_SELF_DISLODGE));
                    return Tristate.FAILURE;
                }

                return Tristate.UNCERTAIN;
            }

            if (isBwoss) {
                if (destOS.getEvalState() == Tristate.FAILURE) {
                    destOS.setDislodgedState(Tristate.YES);
                    destOS.setDislodger(thisOS);
                    logger.debug( "Dislodged. (3.a.3.b.1)");
                    adjudicator.addDislodgedResult(destOS);
                } else if (destOS.getEvalState() == Tristate.UNCERTAIN) {
                    destOS.setDislodgedState(Tristate.MAYBE);
                    destOS.setDislodger(thisOS);
                }
                return Tristate.SUCCESS;
            }
            
            if (destOS.getEvalState() == Tristate.UNCERTAIN) {
                // we are better than all other moves, but with
                // self-support. This normally fails, unless the
                // unit actually moves out (which includes a convoy,
                // if head-to-head).
                logger.debug( "Dest unit not eval'd; remaining uncertain.");
                return Tristate.UNCERTAIN;
            }
            logger.debug( "Failed. (not better w/o self support)");
            adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED));
            return Tristate.FAILURE;
        }

        logger.debug( "dest is not a Move");
        // 3.a.3.b: case 4	[typical case of 3.a.3.b]
        if (thisOS.getAtkCertain() > destOS.getDefMax()) {
            if (!isBwoss || isDestSamePower(destOS)) {
                adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED_NO_SELF_DISLODGE));
                return Tristate.FAILURE;
            }
            destOS.setDislodgedState(Tristate.YES);
            destOS.setDislodger(thisOS);
            logger.debug( "Dislodged. (3.a.3.b typical)");
            adjudicator.addDislodgedResult(destOS);
            return Tristate.SUCCESS;
        }
        return Tristate.UNCERTAIN;
    }

    /**
     * Determines if the given orderstate is the same Power as this order
     */
    private boolean isDestSamePower(OrderState os) {
        if (os != null) {
            return os.getPower().equals(this.getPower());
        }

        return false;
    }// isDestSamePower()


    /**
     * Used to determine some beleagured-garrison cases.
     * <p>
     * Compares this move to all other moves to destination, but does NOT
     * use self-support when calculating.
     * <p>
     * Returns 'false' if not better than all other moves w/o self support
     * Returns 'true' if it is (thus we will likely dislodge)
     * <p>
     * NOTE: this contains some code from 3.d in move algorithm.
     * <p>
     * we should probably create this with a true/false boolean to determine if
     * we should use self support or not--would reduce code duplicatio & bugs.
     * It would be used instead of the dml iterator code in move.evaluate()
     */
    private boolean isBetterWithoutSelfSupport(OrderState thisOS) {
        OrderState[] dml = thisOS.getDependentMovesToDestination();
        logger.debug("Dependent moves to destination: {}", dml.length);

        for (OrderState os : dml) {
            // 3.d
            if (os.isHeadToHead() && (os.getEvalState() == Tristate.UNCERTAIN || !isDependentHTHResolved(os))) {
                // we can't evaluate yet; remain uncertain
                logger.debug("-- but we're not sure yet...");
                return false;
            } else if (!os.isHeadToHead()
                    || (os.isHeadToHead() && os.getDislodger() != os.getHeadToHead())) {
                logger.debug("-- checking atkCertain <= atk max + atk selfsupport max");
                if (thisOS.getAtkCertain() <= (os.getAtkMax() + os.getAtkSelfSupportMax())) {
                    logger.debug("-- but we're definately worse...");
                    return false;
                }
            }
            // else.......
        }// while()

        return true;
    }// isBetterWithoutSelfSupport()


    /**
     * Given a dependent head-to-head orderstate,
     * see if the opposing battle has been resolved.
     */
    private boolean isDependentHTHResolved(OrderState depOS) {
        OrderState opposingOS = depOS.getHeadToHead();
        if (opposingOS != null) {
            return (opposingOS.getEvalState() != Tristate.UNCERTAIN);
        }

        // non head-to-head OrderState;
        throw new IllegalArgumentException("non head to head orderstate");
    }// isDependentHTHResolved()


}// class Move
