/*
 *  @(#)DefineState.java	1.00	4/1/2002
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
import info.jdip.world.Location;
import info.jdip.world.Power;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;

/**
 * Implementation of the Setup (DefineState) order.
 * <p>
 * This order is used to 'build' units, but typically
 * cannot be issued.
 */
public class DefineState extends Order {
    // il8n constants
    private static final String DEFSTATE_NO_UNIT_TYPE = "DEFSTATE_NO_UNIT_TYPE";
    private static final String DEFSTATE_FORMAT = "DEFSTATE_FORMAT";
    private static final String DEFSTATE_VAL_DEFAULT = "DEFSTATE_VAL_DEFAULT";


    // constants: names
    private static final String ORDER_NAME_BRIEF = "";
    private static final String ORDER_NAME_FULL = "Setup";
    private static final transient String ORDER_FORMAT_STRING = Utils.getLocalString(DEFSTATE_FORMAT);


    protected DefineState(Power power, Location src, Unit.Type srcUnit)
            throws OrderException {
        super(power, src, srcUnit);

        if (srcUnit.equals(Unit.Type.UNDEFINED)) {
            throw new OrderException(Utils.getLocalString(DEFSTATE_NO_UNIT_TYPE));
        }
    }// InitialState()


    protected DefineState() {
        super();
    }// DefineState()

    public String getFullName() {
        return ORDER_NAME_FULL;
    }// getName()

    public String getBriefName() {
        return ORDER_NAME_BRIEF;
    }// getBriefName()


    // order formatting
    public String getDefaultFormat() {
        return ORDER_FORMAT_STRING;
    }// getFormatBrief()


    public String toBriefString() {
        StringBuilder sb = new StringBuilder(64);
        super.appendBrief(sb);
        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuilder sb = new StringBuilder(128);
        super.appendFull(sb);
        return sb.toString();
    }// toFullString()


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefineState) {
            DefineState ds = (DefineState) obj;
            return super.equals(ds);
        }
        return false;
    }// equals()

    /**
     * DefineState orders will <b>always fail</b> validation.
     * <p>
     * Their use is mainly for certain types of order parsing (like setting up
     * a game state). For example, info.jdip.misc.TestSuite uses DefineState orders
     * to define the units and their positions for a test scenario.
     */
    @Override
    public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
            throws OrderException {
        // DefineState orders always fail validation.
        throw new OrderException(Utils.getLocalString(DEFSTATE_VAL_DEFAULT));
    }// validate()

    /**
     * DefineState orders do not require verification.
     */
    public void verify(Adjudicator adjudicator) {
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()

    /**
     * Empty method: DefineState orders do not require dependency determination.
     */
    public void determineDependencies(Adjudicator adjudicator) {
      // DefineState orders do not require dependency determination.
    }

    /**
     * Empty method: DefineState orders do not require evaluation logic.
     */
    public void evaluate(Adjudicator adjudicator) {
        // do nothing
    }// evaluate()

}// class DefineState

