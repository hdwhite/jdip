/*
 *  @(#)Disband.java	1.00	4/1/2002
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
import info.jdip.world.Location;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Disband order.
 */
public class Disband extends Order {
    private static final Logger logger = LoggerFactory.getLogger(Disband.class);
    // il8n
    private static final String DISBAND_FORMAT = "DISBAND_FORMAT";

    // constants: names
    private static final String OrderNameBrief = "D";
    private static final String OrderNameFull = "Disband";
    private static final transient String OrderFormatString = Utils.getLocalString(DISBAND_FORMAT);


    /**
     * Creates a Disband order
     */
    protected Disband(Power power, Location src, Unit.Type srcUnit) {
        super(power, src, srcUnit);
    }// Disband()

    /**
     * Creates a Disband order
     */
    protected Disband() {
        super();
    }// Disband()

    public String getFullName() {
        return OrderNameFull;
    }// getName()

    public String getBriefName() {
        return OrderNameBrief;
    }// getBriefName()


    // order formatting
    public String getDefaultFormat() {
        return OrderFormatString;
    }// getFormatBrief()


    public String toBriefString() {
        StringBuilder sb = new StringBuilder(64);

        super.appendBrief(sb);
        sb.append(' ');
        sb.append(OrderNameBrief);

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuilder sb = new StringBuilder(128);

        super.appendFull(sb);
        sb.append(' ');
        sb.append(OrderNameFull);

        return sb.toString();
    }// toFullString()


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Disband) {
            return super.equals(obj);
        }
        return false;
    }// equals()


    @Override
    public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
            throws OrderException {
        // step 0
        checkSeasonRetreat(state, OrderNameFull);
        checkPower(power, state, false);    // inactive units can disband!

        // step 1
        Position position = state.getPosition();
        Unit unit = position.getDislodgedUnit(src.getProvince());
        super.validate(valOpts, unit);
    }// validate()


    /**
     * Disband orders do not require verification.
     */
    public void verify(Adjudicator adjudicator) {
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()

    /**
     * Empty method: Disband orders do not require dependency determination.
     */
    public void determineDependencies(Adjudicator adjudicator) {
      // Disband orders do not require dependency determination.
    }


    /**
     * Disband orders are always successful.
     */
    public void evaluate(Adjudicator adjudicator) {
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        logger.debug("Order: {}", this);

        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            thisOS.setEvalState(Tristate.SUCCESS);
        }

        logger.debug("Result: {}", thisOS.getEvalState());
    }// evaluate()

}// class Disband
