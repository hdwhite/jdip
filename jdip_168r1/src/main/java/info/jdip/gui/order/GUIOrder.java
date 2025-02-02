//
//  @(#)GUIOrder.java	12/2002
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

package info.jdip.gui.order;

import info.jdip.gui.ClientFrame;
import info.jdip.gui.map.MapMetadata;
import info.jdip.order.Orderable;
import info.jdip.order.ValidationOptions;
import info.jdip.process.Adjustment.AdjustmentInfoMap;
import info.jdip.process.RetreatChecker;
import info.jdip.world.Location;
import info.jdip.world.Phase;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGGElement;


/**
 * GUI extension of Orderable that is used for GUI input/handling
 * and GUI order rendering.
 * <p>
 * It is recommended that toString() is not used for GUI orders, as
 * the formatting is not as precise (or controllable) as with
 * toFormattedString() (or using OrderFormat.format()).
 */
public interface GUIOrder extends Orderable {
    // public, numeric, Z-Order constants
    /**
     * Z-order layer for orders that are drawn over units
     */
    int LAYER_HIGHEST = 0;        // build, remove, disband
    /**
     * Typical Z-order layer; drawn under units
     */
    int LAYER_TYPICAL = 1;        // most orders
    /**
     * Lowest Z-order units; all other order layers drawn over this layer.
     */
    int LAYER_LOWEST = 2;        // convoy, supports


    //
    //	Common Message Constants
    //
    //
    /**
     * Message displayed when an order may be issued by a mouse click
     */
    String CLICK_TO_ISSUE = "GUIOrder.common.click";
    /**
     * Message displayed when there is no unit in given location
     */
    String NO_UNIT = "GUIOrder.common.nounit";
    /**
     * Message displayed when there is no dislodged unit in given location
     */
    String NO_DISLODGED_UNIT = "GUIOrder.common.nodislodgedunit";
    /**
     * Message displayed when a power (set by setLockedPower()) does not control the given unit
     */
    String NOT_OWNER = "GUIOrder.common.notowner";
    /**
     * Message displayed when order entry is complete.
     */
    String COMPLETE = "GUIOrder.common.complete";
    /**
     * Message displayed when order is cancelled.
     */
    String CANCELED = "GUIOrder.common.canceled";
    /**
     * Message indicating click to cancel order.
     */
    String CLICK_TO_CANCEL = "GUIOrder.common.clickcancel";
    /**
     * Message indicating cannot give order due to a Border constraint.
     */
    String BORDER_INVALID = "GUIOrder.common.badborder";
    /**
     * Message indicating that pointer is not over a province
     */
    String NOT_IN_PROVINCE = "GUIOrder.common.notprovince";

    //
    // Methods for factory class
    //
    //

    /**
     * Derive all fields from given Order. Sets completion flag.
     */
    void deriveFrom(Orderable order);

    //
    //	Location setting/testing methods
    //
    //

    /**
     * Tests if a given location is valid. Returns validity, and appends valid/invalid message to StringBuilder.
     */
    boolean testLocation(StateInfo stateInfo, Location location, StringBuilder sb);

    /**
     * Clear all set locations. If no locations have been set, this has no effect.
     */
    boolean clearLocations();

    /**
     * Sets the current location. Otherwise similar to testLocation.
     */
    boolean setLocation(StateInfo stateInfo, Location location, StringBuilder sb);

    /**
     * Returns if all Locations have been set and the order is complete.
     */
    boolean isComplete();

    /**
     * Returns the number of required Location set points.
     */
    int getNumRequiredLocations();

    /**
     * Returns the current Location set point, or 0 if no Locations have been set.
     */
    int getCurrentLocationNum();


    /**
     * Sets a given parameter. Throws an IllegalArgumentException if the parameter or value is illegal.
     *
     * @throws IllegalArgumentException if the parameter or value is illegal
     */
    void setParam(Parameter param, Object value);

    /**
     * Checks if a given parameter has been set.
     *
     * @return set parameter, or null if no parameter has been set.
     * @throws IllegalArgumentException if the parameter is illegal
     */
    Object getParam(Parameter param);


    //
    //	Order Drawing methods (for DOM updating)
    //
    //

    /**
     * Indicates if this order is dependent upon the state of other orders for drawing.
     */
    boolean isDependent();

    /**
     * Updates the DOM, given the given parameters. Adds if appropriate.
     */
    void updateDOM(MapInfo mapInfo);

    /**
     * Removes order from the DOM
     */
    void removeFromDOM(MapInfo mapInfo);


    /**
     * Typesafe Enum base class for Order object parameters.	<br>
     * GUIOrders which require Parameters must subclass this.
     */
    abstract class Parameter {
        private final transient String name;

        /**
         * Constructor
         */
        public Parameter(String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }

            this.name = name;
        }// Parameter()

        /**
         * gets the name of Parameter
         */
        public String toString() {
            return name;
        }// toString()

        /**
         * hashCode implementation
         */
        public int hashCode() {
            return name.hashCode();
        }// hashCode()

    }// nested class Parameter


    /**
     * StateInfo is the object which is passed to GUIOrder subclasses
     * that contains various information about the current game state.
     * <p>
     * This information is 'relatively constant' in that it does not
     * typically change until the TurnState changes (or validation options
     * change).
     */
    class StateInfo {
        private AdjustmentInfoMap adjMap = null;
        private TurnState turnState = null;
        private RuleOptions ruleOpts = null;
        private ValidationOptions valOpts = null;
        private ClientFrame cf = null;
        private RetreatChecker rc = null;


        public StateInfo() {
        }

        public void setClientFrame(ClientFrame cf) {
            this.cf = cf;
        }

        public TurnState getTurnState() {
            return turnState;
        }

        public void setTurnState(TurnState turnState) {
            this.turnState = turnState;
        }

        public AdjustmentInfoMap getAdjustmenInfoMap() {
            return adjMap;
        }

        public void setAdjustmenInfoMap(AdjustmentInfoMap adjMap) {
            this.adjMap = adjMap;
        }

        public RuleOptions getRuleOptions() {
            return ruleOpts;
        }

        public void setRuleOptions(RuleOptions ruleOpts) {
            this.ruleOpts = ruleOpts;
        }

        public ValidationOptions getValidationOptions() {
            return valOpts;
        }

        public void setValidationOptions(ValidationOptions valOpts) {
            this.valOpts = valOpts;
        }

        /**
         * Determines if the player can issue orders for this power
         */
        public boolean canIssueOrder(final Power power) {
            final Power[] powers = cf.getOrderablePowers();
            for (Power orderablePower : powers) {
                if (orderablePower == power) {
                    return true;
                }
            }

            return false;
        }// canIssueOrder()

        /**
         * Convenience method to get Position from TurnState
         */
        public Position getPosition() {
            return turnState.getPosition();
        }

        /**
         * Convenience method to get Phase from TurnState
         */
        public Phase getPhase() {
            return turnState.getPhase();
        }

        /**
         * Gets a RetreatChecker object (that can be shared between orders)
         */
        public synchronized RetreatChecker getRetreatChecker() {
            if (rc == null) {
                rc = new RetreatChecker(getTurnState());
            }

            return rc;
        }// getRetreatChecker()

    }// nested class StateInfo


    /**
     * Allows GUIOrder objects to ascertain information about other GUIOrders and
     * obtain Map metadata information.
     */
    abstract class MapInfo {
        protected TurnState ts = null;

        /**
         * Creates a MapInfo object
         */
        public MapInfo(TurnState ts) {
            this.ts = ts;
        }// MapInfo()

        /**
         * Do-nothing constructor
         */
        protected MapInfo() {
        }

        /**
         * Get MapMetadata information
         */
        public abstract MapMetadata getMapMetadata();

        /**
         * Get TurnState (to get Orders, Phase, Position, etc.)
         */
        public TurnState getTurnState() {
            return ts;
        }

        /**
         * Gets the CSS style for a given Power
         */
        public abstract String getPowerCSS(Power power);

        /**
         * Gets the CSS style for a given Power's units
         */
        public abstract String getUnitCSS(Power power);

        /**
         * Gets the Symbol Name for a given unit type
         */
        public abstract String getSymbolName(Unit.Type unitType);

        /**
         * Gets the SVG Document
         */
        public abstract SVGDocument getDocument();

        /**
         * Flag indicating if we are in review mode
         */
        public boolean isReviewMode() {
            return ts.isResolved();
        }// isReviewMode()

        /**
         * Array of Powers whose orders are displayed; if not in array,
         * order is hidden. Should not return null. If we are in a
         * resolved turnstate, this should reuturn all powers. This method
         * should never return null. This method must be overridden.
         */
        public Power[] getDisplayablePowers() {
            if (isReviewMode()) {
                return ts.getWorld().getMap().getPowers();
            }

            return null;
        }// getDisplayablePowers()


        /**
         * Gets the SVG G Element for this power, under which an order should be drawn.
         */
        public abstract SVGGElement getPowerSVGGElement(Power p, int z);
    }// nested class MapInfo


}// interface GUIOrder
