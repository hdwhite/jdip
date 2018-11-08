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

import dip.process.Adjustment;
import dip.world.Power;
import dip.world.TurnState;
import dip.world.World;
import org.nukesoft.jdipFacade.exception.PowerNotFoundException;


/**
 * Encapsulates the methods surrounding a Power object
 *
 * @author Ryan Michela
 */
public class JdipPower {
    private Power power;
    private World world;

    JdipPower(World world, String power)
            throws PowerNotFoundException {
        this.world = world;
        this.power = getPowerFromKey(power);
    }

    public String getPowerName() {
        return power.getName();
    }

    Power getPower() {
        return power;
    }

    /**
     * Gets an <code>AdjustmentInfo</code> object for a given power
     *
     * @return
     */
    private Adjustment.AdjustmentInfo getAdjustmentInfo() {
        //fetch adjustment info
        TurnState state = world.getLastTurnState();
        Power[] powers = world.getMap().getPowers();
        Adjustment.AdjustmentInfoMap adjMap = Adjustment.getAdjustmentInfo(state, state.getWorld().getRuleOptions(), powers);

        return adjMap.get(power);
    }

    /**
     * Returns the actual number of current unit adjustment -/0/+ for this power,
     * taking availiable supply centers into account.
     *
     * @return the number of adjustments
     */
    public int getActualUnitAdjustments() {
        //get the adjustment
        return getAdjustmentInfo().getAdjustmentAmount();
    }

    /**
     * Returns the optimal number of current unit adjustments -/0/+ for this power.
     * The optimal adjustment does not take supply center restrictions into consideration.
     *
     * @return the difference between the maximum number of units this player can control
     * and the actual number of units controlled.
     */
    public int getOptimalUnitAdjustments() {
        return getSupplyCenterCount() - getUnitCount();
    }

    /**
     * Returns the dislodged unit count for the given power.
     *
     * @return the number of dislodgements
     */
    public int getDislodgedUnitCount() {
        //get the adjustment
        return getAdjustmentInfo().getDislodgedUnitCount();
    }

    /**
     * Returns the supply center count for the given power.
     *
     * @return the number of supply centers
     */
    public int getSupplyCenterCount() {
        //get the adjustment
        return getAdjustmentInfo().getSupplyCenterCount();
    }

    /**
     * Returns the home supply center count for the given power.
     *
     * @return the number of home supply centers
     */
    public int getHomeSupplyCenterCount() {
        //get the adjustment
        return getAdjustmentInfo().getHomeSupplyCenterCount();
    }

    /**
     * Returns the number of units this power owns
     */
    public int getUnitCount() {
        return getAdjustmentInfo().getUnitCount();
    }

    /**
     * Determines if this power has been eliminated
     */
    public boolean isEliminated() {
        return world.getLastTurnState().getPosition().isEliminated(power);
    }

    /**
     * Gets this power's adjective (ex. France = French)
     */
    public String getAdjective() {
        return power.getAdjective();
    }

    /**
     * Converts a key into a power
     *
     * @param power the power name to look up
     * @return the associated <code>Power</code> object
     * @throws PowerNotFoundException
     */
    private Power getPowerFromKey(String power)
            throws PowerNotFoundException {
        Power[] powers = world.getMap().getPowers();
        //get the power for the provided key
        Power p = null;
        for (int i = 0; i < powers.length; i++) {
            if (powers[i].getName().equals(power)) {
                p = powers[i];
                break;
            }
        }
        if (p != null) {
            return p;
        } else {
            throw new PowerNotFoundException("Could not find power: " + power);
        }
    }
}
