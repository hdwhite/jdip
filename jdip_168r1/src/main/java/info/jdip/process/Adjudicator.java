//
//  @(#)Adjudicator.java	8/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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

import info.jdip.order.result.OrderResult.ResultType;
import info.jdip.order.result.Result;
import info.jdip.world.Location;
import info.jdip.world.Province;
import info.jdip.world.TurnState;

import java.util.List;


/**
 * Adjudicator interface.
 */
public interface Adjudicator {
    //
    //	Basic methods, used by everyone
    //

    /**
     * Get the TurnState that is currently being adjudicated, and
     * that will have its results and flags set appropriately when
     * adjudication is complete.
     */
    TurnState getTurnState();

    /**
     * Start adjudication.
     */
    void process();

    /**
     * Returns <code>true</code> if an unresolved paradox was detected.
     */
    boolean isUnresolvedParadox();

    /**
     * Returns the next TurnState, or <code>null</code>
     * if an error occured or the game has been won.
     */
    TurnState getNextTurnState();

    /**
     * Enable or disable reporting of failure statistics.
     */
    void setStatReporting(boolean value);

    /**
     * If enabled, checks to make sure that each Power's
     * list of orders only contains orders from that Power.
     * This is important for networked games, to prevent
     * illicit order injection.
     */
    void setPowerOrderChecking(boolean value);

    //
    //	Methods used by Orders and Adjudicator implementations
    //

    /**
     * Find the OrderState with the given source Province. Returns null if
     * no corresponding order was found. <b>Note:</b> Coast is not relevent
     * here; only the Province in the given Location is used.
     */
    OrderState findOrderStateBySrc(Location location);

    /**
     * Find the OrderState with the given source Province. Returns null if
     * no corresponding order was found.
     */
    OrderState findOrderStateBySrc(Province src);

    /**
     * Get all OrderStates
     */
    OrderState[] getOrderStates();

    /**
     * Returns 'true' if The Orderstate in question is a support order
     * that is supporting a move against itself.
     * <ol>
     * <li>Support order is supporting a Move
     * <li>unit in supportedDest must be present
     * <li>power of unit in supported dest == power of support order
     * </ol>
     */
    boolean isSelfSupportedMove(OrderState os);


    /**
     * Returns a list of substituted orders. This is a list of OrderStates.
     * Note that all OrderStates in this list will be marked "illegal". Also
     * note that this will <b>not</b> contain 'null' substitutions (e.g.,
     * no order was specified, and a Hold order was automatically generated).
     */
    List<OrderState> getSubstitutedOrderStates();

    //
    //	Result-adding methods
    //

    /**
     * Add a Result to the result list
     */
    void addResult(Result result);

    /**
     * Add a BouncedResult to the result list
     */
    void addBouncedResult(OrderState os, OrderState bouncer);

    /**
     * Add a DislodgedResult to the result list
     */
    void addDislodgedResult(OrderState os);

    /**
     * Add a Result to the result list
     */
    void addResult(OrderState os, String message);

    /**
     * Add a Result to the result list
     */
    void addResult(OrderState os, ResultType type, String message);
}// interface Adjudicator
