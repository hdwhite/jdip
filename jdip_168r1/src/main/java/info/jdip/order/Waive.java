//
// 	@(#)Waive.java	1	2/2003
//
// 	Copyright 2003 Zachary DelProposto. All rights reserved.
// 	Use is subject to license terms.
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
import info.jdip.process.Adjudicator;
import info.jdip.process.OrderState;
import info.jdip.process.Tristate;
import info.jdip.world.Location;
import info.jdip.world.Power;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Waive order; a power may explicitly choose not to build a unit.
 * in that case, a Waive order may be issued.
 */
public class Waive extends Order {
    private static final Logger logger = LoggerFactory.getLogger(Waive.class);
    // il8n constants
    private static final String WAIVE_FORMAT = "WAIVE_FORMAT";

    // constants: names
    private static final String ORDER_NAME_BRIEF = "W";
    private static final String ORDER_NAME_FULL = "Waive";
    private static final transient String ORDER_FORMAT_STRING = Utils.getLocalString(WAIVE_FORMAT);


    /**
     * Creates a Waive order
     */
    protected Waive(Power power, Location src) {
        super(power, src, Unit.Type.UNDEFINED);
    }// Waive()

    /**
     * Creates a Waive order
     */
    protected Waive() {
        super();
        srcUnitType = Unit.Type.UNDEFINED;
    }// Waive()

    public String getFullName() {
        return ORDER_NAME_FULL;
    }// getName()

    public String getBriefName() {
        return ORDER_NAME_BRIEF;
    }// getBriefName()


    public String getDefaultFormat() {
        return ORDER_FORMAT_STRING;
    }// getFormatBrief()


    public String toBriefString() {
        StringBuilder sb = new StringBuilder(64);

        sb.append(power);
        sb.append(": ");
        sb.append(ORDER_NAME_BRIEF);
        sb.append(" build in ");
        src.appendBrief(sb);

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuilder sb = new StringBuilder(128);

        sb.append(power);
        sb.append(": ");
        sb.append(ORDER_NAME_FULL);
        sb.append(" build in ");
        src.appendFull(sb);

        return sb.toString();
    }// toFullString()


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Waive) {
            return super.equals(obj);
        }
        return false;
    }// equals()


    /**
     * Very little is done to validate Waive orders; we only check the power
     * and season. The adjudicator must check tricky situations, such as too
     * many or too few build orders.
     */
    @Override
    public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
            throws OrderException {
        checkSeasonAdjustment(state, ORDER_NAME_FULL);
        checkPower(power, state, true);

        // not much else to validate; adjudiator must take care of tricky situations.
        // such as too many or too few build orders
    }// validate()


    /**
     * Waive orders do not require verification.
     */
    public void verify(Adjudicator adjudicator) {
        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
        thisOS.setVerified(true);
    }// verify()

    /**
     * Empty method: Waive orders do not require dependency determination.
     */
    public void determineDependencies(Adjudicator adjudicator) {
      // Waive orders do not require dependency determination.
    }


    /**
     * Waive orders are always successful.
     * <p>
     * If there are too few waive orders, that is ok.
     * If there are too many waive or build orders,
     * <p>
     * extra build orders
     * must be discarded by the Adjustment adjudicator as appropriate.
     * <p>
     * Extra build orders are NOT considered in the evaluate() method here.
     */
    public void evaluate(Adjudicator adjudicator) {
        logger.debug("Order: {}", this);

        OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());

        if (thisOS.getEvalState() == Tristate.UNCERTAIN) {
            thisOS.setEvalState(Tristate.SUCCESS);
        }
    }// evaluate()


}// class Waive


