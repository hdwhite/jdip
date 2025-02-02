//
//  @(#)OrderState.java	1.00	4/1/2002
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
package info.jdip.process;

import info.jdip.order.Move;
import info.jdip.order.Order;
import info.jdip.order.Support;
import info.jdip.world.Location;
import info.jdip.world.Power;
import info.jdip.world.Province;

import java.util.List;


/**
 * OrderState objects keep track of the state of an order, during adjudication.
 * OrderStates are not designed to be serialized.
 * <p>
 * Each OrderState represents a decision. All OrderStates initially start out with an evaluation state
 * of Tristate.UNCERTAIN. Once a decision has been made, based upon the internal state, the evaluation
 * state is finalized (Tristate.SUCCESS or Tristate.FAILURE). Ince finalized, a decision can never be reversed.
 * Irreversibility is enforced by the OrderState object.
 * <p>
 * _max values only become smaller.<br>
 * _certain values only become larger.<br>
 * <p>
 * defense: the strength to stay in the same position (hold strength)	<br>
 * attack: the strength required to dislodge a defender from it's position; a separate value
 * is computed for "self" attacks.
 * <p>
 * Dependent lists include OrderStates representing:
 * <ul>
 * <li> Support</li>
 * <li> Moves to Destination province</li>
 * <li> Moves to this (source) province</li>
 * </ul>
 * <p>
 * In Java code, OrderState variables typically have the suffix "OS".
 * <p>
 * NOTE: <b>When debugging, assertions should be enabled.</b>
 * <p>
 * DESIGN NOTE: this class is currently marked final but may not be in future versions.
 */
public final class OrderState {
    /**
     * NOTE: 'max' values start at an arbitrarily high value (something that is so large
     * that it could never occur in a game) and move down. Note that setting the value
     * to Integer.MAX_VALUE is NOT a good idea, because if we add two max values together
     * (e.g., when calculating support), we will overflow (to negative values)
     * So, we will use our own MAX_VALUE, which is still quite high.
     */
    public static final int MAX_VALUE = 9999;

    /**
     * MINIMUM value for a strength. Only used for Retreat Strength.
     */
    public static final int MIN_VALUE = -9999;

    /**
     * Internal constant: Empty orderstate array
     */
    private static final OrderState[] OS_EMPTY = new OrderState[0];


    private Order order = null;


    private int DEFENSE_MAX = MAX_VALUE;
    private int DEFENSE_CERTAIN = 1;                    // always can defend at 1

    private int ATTACK_MAX = MAX_VALUE;
    private int ATTACK_CERTAIN = 0;                    // can't always can attack at 1. E.g DPB

    private int SELF_SUPPORT_ATK_MAX = MAX_VALUE;
    private int SELF_SUPPORT_ATK_CERTAIN = 0;            // we may not be self-supporting, so start at 0

    private int retreatStr = MIN_VALUE;                // retreat strength (for DPB support)

    private boolean isCircular = false;
    private boolean isLegal = true;                    // assume order is legal

    private Tristate evalState = Tristate.UNCERTAIN;
    private Tristate dislodged = Tristate.NO;                // not-dislodged is default

    private OrderState[] dependentSelfSupports = OS_EMPTY;    // only contains Support orders
    private OrderState[] dependentSupports = OS_EMPTY;    // only contains Support orders
    private OrderState[] dependentMovesToSource = OS_EMPTY;    // only contains Move orders
    private OrderState[] dependentMovesToDestination = OS_EMPTY;    // only contains Move orders

    private OrderState headToHead = null;        // if it's a head-to-head move
    private OrderState dislodgedBy = null;    // orderstate which dislodged this unit
    private boolean foundConvoyPath = false;    // if move found a convoy path
    private boolean isVerified = false;            // has this order been verified() yet?


    /**
     * Create an OrderState. This is protected, because only subclasses of
     * this and adjudicators should be able to create new OrderState objects,
     * although other classes can definately use them.
     */
    protected OrderState(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("null order");
        }

        this.order = order;
    }// OrderState()


    // GET methods

    /**
     * Get the Order for this OrderState.
     */
    public Order getOrder() {
        return order;
    }

    /**
     * Set the Order. NOTE: this may be eliminated/deprecated, as it must be used with extreme care.
     */
    protected void setOrder(Order value) {
        order = value;
    }    // consider eliminating

    /**
     * Get if we are part of a circular movement chain.
     */
    public boolean isCircular() {
        return isCircular;
    }

    /**
     * Set if this is part of a chain of circular movements.
     */
    public void setCircular(boolean value) {
        isCircular = value;
    }

    /**
     * Get the evaluation state.
     */
    public Tristate getEvalState() {
        return evalState;
    }

    /**
     * Set the evaluation state. Note that once set (e.g., value != UNCERTAIN),
     * it cannot be altered.
     */
    public void setEvalState(Tristate value) {
        // ENSURE irreversibility
        if (evalState != Tristate.UNCERTAIN) {
            throw new IllegalStateException("EvalState is irreversible, once set.");
        }

        evalState = value;
    }// setEvalState()

    /**
     * Get dislodged state.
     */
    public Tristate getDislodgedState() {
        return dislodged;
    }

    /**
     * Set the dislodged state.
     */
    public void setDislodgedState(Tristate value) {
        dislodged = value;
    }

    /**
     * Get maximum defense value
     */
    public int getDefMax() {
        return DEFENSE_MAX;
    }

    /**
     * Set the maximum defense.
     */
    public void setDefMax(int value) {
        DEFENSE_MAX = value;
    }

    /**
     * Get certain defense value
     */
    public int getDefCertain() {
        return DEFENSE_CERTAIN;
    }

    /**
     * Set the certain defense.
     */
    public void setDefCertain(int value) {
        DEFENSE_CERTAIN = value;
    }

    /**
     * Get maximum attack value
     */
    public int getAtkMax() {
        return ATTACK_MAX;
    }

    /**
     * Set the maximum attack value.
     */
    public void setAtkMax(int value) {
        ATTACK_MAX = value;
    }

    /**
     * Get certain attack value
     */
    public int getAtkCertain() {
        return ATTACK_CERTAIN;
    }

    /**
     * Set the certain attack value.
     */
    public void setAtkCertain(int value) {
        ATTACK_CERTAIN = value;
    }

    /**
     * Get maximum self-support attack value
     */
    public int getAtkSelfSupportMax() {
        return SELF_SUPPORT_ATK_MAX;
    }

    /**
     * Set the attack max including self-support
     */
    public void setAtkSelfSupportMax(int value) {
        SELF_SUPPORT_ATK_MAX = value;
    }

    /**
     * Get certain self-support attack value.
     */
    public int getAtkSelfSupportCertain() {
        return SELF_SUPPORT_ATK_CERTAIN;
    }

    /**
     * Set the attack certain including self-support
     */
    public void setAtkSelfSupportCertain(int value) {
        SELF_SUPPORT_ATK_CERTAIN = value;
    }

    /**
     * Determines if this is part of a head-to-head Move.
     */
    public boolean isHeadToHead() {
        return (headToHead != null);
    }

    /**
     * Get the order that we are in a head-to-head Move with.
     */
    public OrderState getHeadToHead() {
        return headToHead;
    }

    /**
     * if move is a head-to-head move, set which move we are moving head-to-head against here.
     */
    public void setHeadToHead(OrderState os) {
        if (os != null && !(os.order instanceof Move)) {
            throw new IllegalArgumentException("h2h orderstate must be set with a Move order");
        }
        headToHead = os;
    }// setHeadToHead()

    /**
     * Get the order legality (default is true).
     */
    public boolean isLegal() {
        return isLegal;
    }

    /**
     * Sets if an Order is legal. By default, orders are legal.
     */
    public void setLegal(boolean value) {
        isLegal = value;
    }

    /**
     * Get the dislodger, if any.
     */
    public OrderState getDislodger() {
        return dislodgedBy;
    }

    /**
     * Set the dislodger. We do some sanity checks; this can only
     * be set if dislodged == MAYBE or YES. Furthermore, dislodger
     * must be a Move order.
     */
    public void setDislodger(OrderState os) {
        assert (os.order instanceof Move && dislodged != Tristate.NO);
        dislodgedBy = os;
    }// setDislodger()

    /**
     * Get if we have found a convoy path.
     */
    public boolean hasFoundConvoyPath() {
        return foundConvoyPath;
    }

    /**
     * Get retreat strength
     */
    public int getRetreatStrength() {
        return retreatStr;
    }

    /**
     * Set the retreat strength
     */
    public void setRetreatStrength(int value) {
        retreatStr = value;
    }

    /**
     * Indicates if the retreat strength has been set.
     */
    public boolean isRetreatStrengthSet() {
        return (retreatStr != MIN_VALUE);
    }

    /**
     * Indicate if Order has been Verified
     */
    public boolean isVerified() {
        return isVerified;
    }

    /**
     * Set if an order has been verified. Once set to true, cannot be set to false.
     */
    public void setVerified(boolean value) {
        if (!value && isVerified) {
            throw new IllegalStateException("setVerified() is irreversible, once set to true.");
        }

        isVerified = value;
    }// setVerified()

    /**
     * Gets the dependent Support orders for this order
     */
    public OrderState[] getDependentSupports() {
        return dependentSupports;
    }

    /**
     * Adds the given list, which must only contain Dependent Support OrderStates.
     * <p>
     * If assertions are enabled, the list is verified to contain only Support
     * OrderStates
     */
    public void setDependentSupports(List<OrderState> osList) {
        assert (verifyListSupport(osList));
        dependentSupports = osList.toArray(new OrderState[osList.size()]);
    }// setDependentSupports()

    /**
     * Get the Move orders that are moving to the Source Location of this order.
     */
    public OrderState[] getDependentMovesToSource() {
        return dependentMovesToSource;
    }

    /**
     * Adds a List of the Dependent Move Orderstates to the Source Province of this Orderstate.
     * <p>If asserts are enabled, all OrderStates are verified to contain only Move orders.
     */
    public void setDependentMovesToSource(List<OrderState> osList) {
        assert (verifyListMove(osList));
        dependentMovesToSource = osList.toArray(new OrderState[osList.size()]);
    }// addDependentMoveToSource()

    /**
     * Get the Move orders that are moving to the Destination Location of this order.
     */
    public OrderState[] getDependentMovesToDestination() {
        return dependentMovesToDestination;
    }

    /**
     * Adds a List of the Dependent Move Orderstates to the Destination Province of this Orderstate
     * <p>If asserts are enabled, all OrderStates are verified to contain only Move orders.
     */
    public void setDependentMovesToDestination(List<OrderState> osList) {
        assert (verifyListMove(osList));
        dependentMovesToDestination = osList.toArray(new OrderState[osList.size()]);
    }// addDependentMoveToDestination()

    /**
     * Gets the dependent self-Support orders for this order
     */
    public OrderState[] getDependentSelfSupports() {
        return dependentSelfSupports;
    }

    /**
     * Criteria:
     * <ol>
     * <li>Support order
     * <li>Supported MOVE (not any other) [supportedSrc != supportedDest]
     * <li>unit in supportedDest must be present
     * <li>power of unit in supported dest == power of support order
     * </ol>
     * <b>NOTE:</b> OrderState only checks criteria #1 and #2, and only if
     * asserts are enabled.
     */
    public void setDependentSelfSupports(List<OrderState> osList) {
        assert (verifyListSelfSupport(osList));
        dependentSelfSupports = osList.toArray(new OrderState[osList.size()]);
    }// addDependentSupport()

    /**
     * Set if we have found a convoy path
     */
    public void setFoundConvoyPath(boolean value) {
        foundConvoyPath = value;
    }

    /**
     * Convenicent method: get the order source Location
     */
    public Location getSource() {
        return order.getSource();
    }

    /**
     * Convenicent method: get the order source Province
     */
    public Province getSourceProvince() {
        return getSource().getProvince();
    }

    /**
     * Convenicent method: get the order Power
     */
    public Power getPower() {
        return order.getPower();
    }

    /**
     * For each support in the dependent list, the total support is
     * determined in the following manner:
     * <ul>
     * <li>if isCertain == true:
     * <ul>
     * <li>+1 if support evalState == TriSTate.SUCCESS
     * <li>+0 if evalState is UNCERTAIN or FAILURE
     * </ul>
     *
     * <li>if isCertain == false;	[calculates 'max' support]
     * <ul>
     * <li>+1 if support evalState == SUCCESS or UNCERTAIN
     * <li>+0 if support evalState == FAILURE
     * </ul>
     * </ul>
     * <p>
     * <b>Note:</b><br>
     * It is assumed that all support is 'appropriate'; and that invalid supports have
     * already been marked as such (e.g., if order is Move A-B, and we have to supports:
     * support 1: C sup A-B, and support 2: C sup A) that support 2 (illegal for a Move order)
     * is marked FAILURE with it's evalstate.
     * <p>
     * If there is NO support, we return 1 (since all units have a default strength of 1),
     * unless the base move modifier (due to a difficult passable border) changes this amount.
     */
    public int getSupport(boolean isCertain) {
        // Determine support, but based on the fact that provinces could have special borders
        int mod;

        // Just figuring out how to get each border, and what its BaseMoveModifier is.
        if (this.getOrder() instanceof Move) {
            Move order = (Move) this.getOrder();
            mod = order.getDest().getProvince().getBaseMoveModifier(order.getSource());
        } else if (this.getOrder() instanceof Support) {
            Support order = (Support) this.getOrder();
            mod = order.getSupportedDest().getProvince().getBaseMoveModifier(order.getSource());
            // If the order is a convoy, hold,etc there is not BaseMoveModifier
        } else {
            mod = 0;
        }

        // Get the strength
        return getSupport(isCertain, dependentSupports, 1 + mod);
    }// getSupport()


    /**
     * Similar to getSupport(), but, determines self-support (if any).
     * If there is no self support, returns '0'.
     */
    public int getSelfSupport(boolean isCertain) {
        return getSupport(isCertain, dependentSelfSupports, 0);
    }// getSelfSupport()


    /**
     * Helper method, used by other getSupport() and getSelfSupport() methods.
     */
    protected int getSupport(boolean isCertain, OrderState[] supportList, int defaultStrength) {
        int strength = defaultStrength;

        for (OrderState os : supportList) {
            if (os.getEvalState() == Tristate.SUCCESS
                    || (!isCertain && os.getEvalState() == Tristate.UNCERTAIN)) {
                strength++;
            }
        }

        // if it is 0 or above, great! If not, make it 0!
        if (strength >= 0) {
            return strength;
        } else {
            return 0;
        }
    }// getSupport()


    /**
     * Verifies that given list ONLY contains Move orderstates
     */
    private boolean verifyListMove(List<OrderState> list) {
        for (OrderState os : list) {
            if (!(os.getOrder() instanceof Move)) {
                return false;
            }
        }

        return true;
    }// verifyListMove()


    /**
     * Verifies that given list ONLY contains Support orderstates
     */
    private boolean verifyListSupport(List<OrderState> list) {
        for (OrderState os : list) {
            if (!(os.getOrder() instanceof Support)) {
                return false;
            }
        }

        return true;
    }// verifyListSupport()


    /**
     * Verifies that given list ONLY contains Self Support orderstates
     */
    private boolean verifyListSelfSupport(List<OrderState> list) {
        for (OrderState os : list) {
            if (os.getOrder() instanceof Support) {
                Support support = (Support) os.getOrder();
                if (support.isSupportingHold()) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }// verifyListSelfSupport()

}// class OrderState


