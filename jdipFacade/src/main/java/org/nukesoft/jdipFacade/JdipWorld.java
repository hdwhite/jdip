/*  Copyright (C) 2004  Ryan Michela
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.nukesoft.jdipFacade;

import dip.order.Order;
import dip.order.OrderException;
import dip.order.OrderFactory;
import dip.order.OrderFormatOptions;
import dip.order.OrderParser;
import dip.order.Orderable;
import dip.order.result.Result;
import dip.world.Phase.PhaseType;
import dip.world.Power;
import dip.world.TurnState;
import dip.world.World;
import org.nukesoft.jdipFacade.exception.JdipException;
import org.nukesoft.jdipFacade.exception.PowerNotFoundException;
import org.nukesoft.jdipFacade.exception.StateError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * The <code>JdipWorld</code> shields the user from unnecesary contact with the Jdip API
 * while exposing only the necessary features of the world
 *
 * @author Ryan Michela
 */
public class JdipWorld {
    //Constants
    public static final int ORDER_FORMAT_DEFAULT = 1;
    public static final int ORDER_FORMAT_TERSE = 2;
    public static final int ORDER_FORMAT_VERBOSE = 3;
    public static final int ORDER_FORMAT_DEBUG = 4;

    public static final int PHASE_TYPE_ADJUSTMENT = 1;
    public static final int PHASE_TYPE_MOVEMENT = 2;
    public static final int PHASE_TYPE_RETREAT = 3;

    private final ImplementationStrategy strategy;
    private final World world;

    /**
     * Builds a <code>JdipWorld</code>. Don't try this at home, kids.
     *
     * @param world the world to use
     */
    JdipWorld(World world, ImplementationStrategy strategy) {
        this.strategy = strategy;
        this.world = world;
    }

    /**
     * Gets the world.
     *
     * @return the world
     */
    World getWorld() {
        return world;
    }

    /**
     * Fetches a list of all valid power names.
     *
     * @return an array containing the name of each power
     */
    public String[] getPowerNames() {
        Power[] powers = world.getMap().getPowers();
        String[] powerNames = new String[powers.length];
        for (int i = 0; i < powers.length; i++) {
            powerNames[i] = powers[i].getName();
        }
        return powerNames;
    }

    /**
     * Loads orders for a particular power into the most recent turn state.
     * Overwrites any existing orders for the given power.
     *
     * @param orderStrings the orders to load
     * @param powerToSet   the power to load against
     * @throws PowerNotFoundException
     * @throws JdipException
     */
    public void setOrders(String[] orderStrings, JdipPower powerToSet)
            throws PowerNotFoundException, JdipException {
        //fetch the power
        Power power = powerToSet.getPower();
        if (power == null) {
            throw new PowerNotFoundException();
        }
        //parse the orders
        ArrayList<Orderable> orders = new ArrayList<>();
        OrderParser parser = OrderParser.getInstance();
        OrderFactory of = strategy.getOrderFactory();
        try {
            for (int i = 0; i < orderStrings.length; i++) {
                Order o = parser.parse(of, power.getName() + " " + orderStrings[i], power, world.getLastTurnState(), true, false);
                orders.add(o);
            }
        } catch (OrderException e) {
            throw new JdipException(e.getMessage() + " <" + e.getOrder() + ">", e);
        }
        //set the orders
        world.getLastTurnState().setOrders(power, orders);
    }

    /**
     * Returns an array of all general messages from the last adjudicated turn state.
     *
     * @param format one of the format constants
     * @return an array of <code>JdipResultAdapter</code>s
     */
    public JdipResult[] getAllGeneralResults(int format) {
        OrderFormatOptions ofo = this.ceateOrderFormat(format);

        TurnState state = world.getPreviousTurnState(world.getLastTurnState());
        List allResults = state.getResultList();
        ArrayList<JdipResult> generalResults = new ArrayList<>();
        Iterator it = allResults.iterator();

        while (it.hasNext()) {
            JdipResult r = new JdipResult((Result) it.next(), ofo);
            //find all general resluts
            if (r.isGeneralResult()) {
                generalResults.add(r);
            }
        }

        return toJdipResultArray(generalResults.toArray());
    }

    /**
     * Returns an array of all general messages from the last adjudicated turn state.
     *
     * @param power  the power to get results for
     * @param format one of the format constants
     * @return an array of <code>JdipResultAdapter</code>s
     */
    public JdipResult[] getAllResultsForPower(JdipPower power, int format) {
        OrderFormatOptions ofo = this.ceateOrderFormat(format);

        TurnState state = world.getPreviousTurnState(world.getLastTurnState());
        List allResults = state.getResultList();
        ArrayList<JdipResult> generalResults = new ArrayList<>();
        Iterator it = allResults.iterator();

        while (it.hasNext()) {
            JdipResult r = new JdipResult((Result) it.next(), ofo);
            //find all resluts for given power
            if (!r.isGeneralResult() && r.getPower().equals(power.getPowerName())) {
                generalResults.add(r);
            }
        }

        return toJdipResultArray(generalResults.toArray());
    }

    private JdipResult[] toJdipResultArray(Object[] o) {
        //change the type of the result array. Danm java won't let me do array casting. Eat for loop bitch!
        JdipResult[] result = new JdipResult[o.length];
        for (int i = 0; i < o.length; i++) {
            result[i] = (JdipResult) o[i];
        }
        return result;
    }

    /**
     * Gets the expanded name of the current phase.
     *
     * @return phase name
     */
    public String getCurrentPhaseTitle() {
        return world.getLastTurnState().getPhase().toString();
    }

    /**
     * Gets the expanded name of the previous phase. This is the name of the
     * phase associated with the currently available results.
     *
     * @return phase name
     */
    public String getLastPhaseTitle() {
        return world.getPreviousTurnState(world.getLastTurnState()).getPhase().toString();
    }

    /**
     * Gets the current type (Adjustment, Movement, Retreat) of the current phase.
     *
     * @return one of the phase type constants.
     */
    public int getCurrentPhaseType() {
        PhaseType current = world.getLastTurnState().getPhase().getPhaseType();
        if (current == PhaseType.ADJUSTMENT) {
            return PHASE_TYPE_ADJUSTMENT;
        } else if (current == PhaseType.MOVEMENT) {
            return PHASE_TYPE_MOVEMENT;
        } else if (current == PhaseType.RETREAT) {
            return PHASE_TYPE_RETREAT;
        } else {
            throw new StateError("TurnState phase is not valid! Something is wrong with JDip.");
        }
    }

    /**
     * Gets a <code>JdipPower</code> object for the desired power. The object can be used
     * to fetch information about the power.
     *
     * @param power the power to fetch
     * @return a <code>JdipPower</code> object
     * @throws PowerNotFoundException
     */
    public JdipPower getPower(String power)
            throws PowerNotFoundException {
        return new JdipPower(world, power);
    }

    /**
     * Gets a <code>JdipMapInfo</code> object that can be used to fetch information about
     * the map.
     *
     * @return a <code>JdipMapInfo</code> object
     */
    public JdipMapInfo getMapInfo() {
        return new JdipMapInfo(world);
    }

    /**
     * Determines if the game is over
     *
     * @return <code>true</code>/<code>false</code>
     */
    public boolean isGameOver() {
        return world.getLastTurnState().isEnded();
    }


    /**
     * Creates an <code>OrderFormatOptions</code> object based on the input constant.
     *
     * @param format the constant to use
     * @return the associated <code>OrderFormatOptions</code> object
     */
    private OrderFormatOptions ceateOrderFormat(int format) {
        //create order format
        OrderFormatOptions ofo;
        switch (format) {
            case ORDER_FORMAT_DEFAULT:
                ofo = OrderFormatOptions.createDefault();
                break;
            case ORDER_FORMAT_TERSE:
                ofo = OrderFormatOptions.createTerse();
                break;
            case ORDER_FORMAT_VERBOSE:
                ofo = OrderFormatOptions.createVerbose();
                break;
            case ORDER_FORMAT_DEBUG:
                ofo = OrderFormatOptions.createDebug();
                break;
            default:
                ofo = OrderFormatOptions.createDefault();
                break;
        }
        return ofo;
    }

    ImplementationStrategy getStrategy() {
        return this.strategy;
    }
}
