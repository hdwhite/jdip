//  
//  @(#)Retreat.java	4/2002
//  
//  Copyright 2002 Zachary DelProposto. All rights reserved.
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
import info.jdip.order.result.OrderResult.ResultType;
import info.jdip.process.Adjudicator;
import info.jdip.process.OrderState;
import info.jdip.process.RetreatChecker;
import info.jdip.process.Tristate;
import info.jdip.world.Border;
import info.jdip.world.Location;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Implementation of the Retreat order.
 * <p>
 * Note that a Retreat order is a Move order. Retreat orders are issued
 * instead of Move orders during the Retreat phase, by OrderParser.
 * <p>
 * Convoy options are not valid in Retreat orders.
 */

public class Retreat extends Move {
    private static final Logger logger = LoggerFactory.getLogger(Retreat.class);
    // il8n constants
    private static final String RETREAT_SRC_EQ_DEST = "RETREAT_SRC_EQ_DEST";
    private static final String RETREAT_CANNOT = "RETREAT_CANNOT";
    private static final String RETREAT_FAIL_DPB = "RETREAT_FAIL_DPB";
    private static final String RETREAT_FAIL_MULTIPLE = "RETREAT_FAIL_MULTIPLE";
    private static final String orderNameFull = "Retreat";    // brief order name is still 'M'
    private Tristate evalResult = Tristate.UNCERTAIN;


    /**
     * Creates a Retreat order
     */
    protected Retreat(Power power, Location src, Unit.Type srcUnitType, Location dest) {
        super(power, src, srcUnitType, dest, false);
    }// Move()

    /**
     * Creates a Retreat order
     */
    protected Retreat() {
        super();
    }// Retreat()

    /**
     * Retreats are never convoyed; this will always return false.
     */
    public boolean isByConvoy() {
        return false;
    }// isByConvoy()


    @Override
    public String getFullName() {
        return orderNameFull;
    }// getFullName()

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Retreat) {
            Retreat retreat = (Retreat) obj;
            return super.equals(retreat)
                    && this.dest.equals(retreat.dest);
        }
        return false;
    }// equals()


    @Override
    public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
            throws OrderException {
        // step 0
        checkSeasonRetreat(state, getFullName());
        checkPower(power, state, true);


        // 1
        Position position = state.getPosition();
        Unit unit = position.getDislodgedUnit(src.getProvince());
        super.validate(valOpts, unit);

        if (valOpts.getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_STRICT)) {
            // 2
            if (src.isProvinceEqual(dest)) {
                throw new OrderException(Utils.getLocalString(RETREAT_SRC_EQ_DEST));
            }

            // validate Borders
            Border border = src.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
            if (border != null) {
                throw new OrderException(Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()));
            }

            // 3: validate destination [must be a legal move] This is important, because
            // the coast of the destination needs to be known (if unit is a fleet).
            // otherwise, RetreatChecker.isValid() won't work.
            dest = dest.getValidatedWithMove(srcUnitType, src);

            // check that we can transit into destination (check borders)
            border = dest.getProvince().getTransit(dest, srcUnitType, state.getPhase(), this.getClass());
            if (border != null) {
                throw new OrderException(Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()));
            }

            // 4
            RetreatChecker rc = new RetreatChecker(state);
            if (!rc.isValid(src, dest)) {
                throw new OrderException(Utils.getLocalString(RETREAT_CANNOT, dest));
            }
        }
    }// validate()


    /**
     * No verification is performed.
     */
    @Override
    public void verify(Adjudicator adjudicator) {
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()


    /**
     * Retreat orders are only dependent on other
     * retreat orders that are moving to the same destination.
     */
    @Override
    public void determineDependencies(Adjudicator adjudicator) {
        // add moves to destination space, and supports of this space
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        ArrayList<OrderState> depMTDest = null;

        OrderState[] orderStates = adjudicator.getOrderStates();
        for (OrderState dependentOS : orderStates) {
            Order order = dependentOS.getOrder();

            if (order instanceof Retreat
                    && order != this) {
                Retreat retreat = (Retreat) order;

                if (retreat.getDest().isProvinceEqual(this.getDest())) {
                    if (depMTDest == null) {
                        depMTDest = new ArrayList<>(4);
                    }
                    depMTDest.add(dependentOS);
                }
            }
        }

        // set dependent moves to destination
        if (depMTDest != null) {
            thisOS.setDependentMovesToDestination(depMTDest);
        }
    }// determineDependencies()


    /**
     * If a retreat is valid, it will be successfull unless
     * there exists one or more retreats to the same destination.
     */
    @Override
    public void evaluate(Adjudicator adjudicator) {
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        // while Retreat orders cannot be supported, we DO need to determine
        // if we are retrating across a difficult passable border.
        // We only need to set 'max' support. For the typical case (base move
        // modifier == -1), this will return "1" unless over a DPB (base move
        // modifier -1) which will return "0".
        //
        thisOS.setRetreatStrength(thisOS.getSupport(false));

        logger.info("Order: {}", this);
        logger.info("Retreat strength: [dpb check]: {}", thisOS.getRetreatStrength());
        logger.info("Moves to dest: {}", thisOS.getDependentMovesToDestination().length);

        // if other retreats to destination, we will succeed; otherwise, we will
        // probably fail.
        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            OrderState[] depMovesToDest = thisOS.getDependentMovesToDestination();

            if (depMovesToDest.length == 0) {
                // typical case
                logger.info("SUCCESS!");
                thisOS.setEvalState(Tristate.SUCCESS);
                logger.debug("final evalState() = {}", thisOS.getEvalState());
                return;
            }
            // Modified for Difficult Passsable Border (see DATC 16-dec-03 10.K)
            // But must differentiate DPB from multiple retreats to same location.
            //
            // Furthermore, we need to account for the possibility of 3 retreats
            // to 1 dest, where 2 retreats are over a normal border and 1 is over
            // a DPB; that would cause all retreats to fail.
            //
            // Thus, if we are <= strength of others, we fail. Otherwise we are >, and
            // successful.
            //
            // This logic *MUST* be run in an evaluation loop.
            //
            Tristate evalResult = Tristate.UNCERTAIN;
            boolean isStrongerThanAllOthers = false;

            for (OrderState depMoveOS : depMovesToDest) {
                if (depMoveOS.isRetreatStrengthSet()) {
                    if (thisOS.getRetreatStrength() > depMoveOS.getRetreatStrength()) {
                        // we *may* be stronger than all others
                        logger.debug( "We may be stronger than all others...");
                        isStrongerThanAllOthers = true;
                        continue;
                    }
                    else if (thisOS.getRetreatStrength() == depMoveOS.getRetreatStrength()) {
                        // the usual case
                        logger.debug("FAILURE! (==) {}", depMoveOS.getOrder());
                        adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(RETREAT_FAIL_MULTIPLE));
                    } else {
                        // only can be less when considering DPBs
                        logger.debug("FAILURE! (<) {}", depMoveOS.getOrder());
                        adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(RETREAT_FAIL_DPB));
                    }
                    evalResult = Tristate.FAILURE;
                    isStrongerThanAllOthers = false;
                    break;
                } else {
                    // remain uncertain. Dependent orderstate not yet evaluated.
                    logger.info("Uncertain: {} not yet evaluated.", depMoveOS.getOrder());
                    isStrongerThanAllOthers = false;    // we don't know yet.
                    evalResult = Tristate.UNCERTAIN;
                }
            }// for()

            // if we are stronger than all others, we will succeed.
            if (isStrongerThanAllOthers) {
                logger.debug( "SUCCESS! [stronger than all others]");
                evalResult = Tristate.SUCCESS;
            }

            thisOS.setEvalState(evalResult);
        }

        logger.debug("final evalState() = {}", thisOS.getEvalState());
    }// evaluate()

}// class Retreat
