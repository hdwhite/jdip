/*
 *  @(#)Hold.java	1.00	4/1/2002
 *
 *  Copyright 2002 Zachary DelProposto. All rights reserved.
 *  Use is subject to license terms.
 */
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
import info.jdip.process.Adjudicator;
import info.jdip.process.OrderState;
import info.jdip.process.Tristate;
import info.jdip.world.Border;
import info.jdip.world.Location;
import info.jdip.world.Power;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Hold order.
 */
public class Hold extends Order {
    private static final Logger logger = LoggerFactory.getLogger(Hold.class);
    // il8n
    private static final String HOLD_FORMAT = "HOLD_FORMAT";

    // constants: names
    private static final String orderNameBrief = "H";
    private static final String orderNameFull = "Hold";
    private static final transient String orderFormatString = Utils.getLocalString(HOLD_FORMAT);


    /**
     * Creates a Hold order
     */
    protected Hold(Power power, Location src, Unit.Type srcUnit) {
        super(power, src, srcUnit);
    }// Hold()

    /**
     * Creates a Hold order
     */
    protected Hold() {
        super();
    }// Hold()


    public String getFullName() {
        return orderNameFull;
    }// getName()

    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    // format-strings for orders
    public String getDefaultFormat() {
        return orderFormatString;
    }// getFormatBrief()


    public String toBriefString() {
        StringBuffer sb = new StringBuffer(64);

        super.appendBrief(sb);
        sb.append(' ');
        sb.append(orderNameBrief);

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuffer sb = new StringBuffer(128);

        super.appendFull(sb);
        sb.append(' ');
        sb.append(orderNameFull);

        return sb.toString();
    }// toFullString()


    public boolean equals(Object obj) {
        return obj instanceof Hold && super.equals(obj);
    }// equals()


    public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
            throws OrderException {
        checkSeasonMovement(state, orderNameFull);
        checkPower(power, state, false);    // inactive powers can issue Hold orders
        super.validate(state, valOpts, ruleOpts);

        // validate Borders
        Border border = src.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
        if (border != null) {
            throw new OrderException(Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()));
        }
    }// validate();


    /**
     * No verification is required for Hold orders.
     */
    public void verify(Adjudicator adjudicator) {
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()


    /**
     * Dependencies for a Hold order:
     * <ol>
     * <li>Supports to this space
     * <li>Moves to this space
     * </ol>
     */
    public void determineDependencies(Adjudicator adjudicator) {
        addSupportsOfAndMovesToSource(adjudicator);
    }// determineDependencies()

    /**
     * Hold order evaluation logic.
     */
    public void evaluate(Adjudicator adjudicator) {
        logger.trace( "--- evaluate() info.jdip.order.Hold ---");

        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        // calculate support
        thisOS.setDefMax(thisOS.getSupport(false));
        thisOS.setDefCertain(thisOS.getSupport(true));

        logger.debug("Order: {}", this);
        logger.debug("initial evalstate: {}, def-max: {}, def-cert: {}, supports: {}, dislodged: {}",
                thisOS.getEvalState(),
                thisOS.getDefMax(),
                thisOS.getDefCertain(),
                thisOS.getDependentSupports().length,
                thisOS.getDislodgedState()
        );

        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            // if no moves against this order, we must succeed.
            // Otherwise, MOVE orders will determine if we are dislodged and thus fail.
            if (thisOS.getDependentMovesToSource().length == 0) {
                thisOS.setEvalState(Tristate.SUCCESS);
                thisOS.setDislodgedState(Tristate.NO);
            }
        }

        logger.debug("Final evalState: {}", thisOS.getEvalState());
    }// evaluate()

}// class Hold
