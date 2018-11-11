//
//  @(#)InitialState.java	1.00	7/2002
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
package info.jdip.world.variant.data;

import info.jdip.world.Coast;
import info.jdip.world.Unit;

/**
 * Sets the Initial State (position) for a province.
 * <p>
 */
public class InitialState {
    private String provinceName = null;
    private String power = null;
    private Unit.Type unit = null;
    private Coast coast = null;

    /**
     * Name of province to which this InitialState refers.
     */
    public String getProvinceName() {
        return provinceName;
    }

    /**
     * Set the Province name
     */
    public void setProvinceName(String value) {
        provinceName = value;
    }

    /**
     * Power of unit owner
     */
    public String getPowerName() {
        return power;
    }

    /**
     * Set the Power name
     */
    public void setPowerName(String value) {
        power = value;
    }

    /**
     * Type of unit
     */
    public Unit.Type getUnitType() {
        return unit;
    }

    /**
     * Sets the unit type.
     */
    public void setUnitType(Unit.Type value) {
        unit = value;
    }

    /**
     * Coast of unit
     */
    public Coast getCoast() {
        return coast;
    }

    /**
     * Sets the coast for the unit.
     */
    public void setCoast(Coast value) {
        coast = value;
    }


    /**
     * For debugging only!
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append(this.getClass().getName());
        sb.append('[');
        sb.append("provinceName=");
        sb.append(provinceName);
        sb.append(",power=");
        sb.append(power);
        sb.append(",unit=");
        sb.append(unit);
        sb.append(",coast=");
        sb.append(coast);
        sb.append(']');
        return sb.toString();
    }// toString()
}// nested class InitialState

