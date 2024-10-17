//
//  @(#)ProvinceData.java	1.00	7/2002
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

import java.util.List;

/**




 */
public class ProvinceData {
    private String fullName = null;
    private String[] shortNames = null;
    private String[] adjProvinces = null;
    private String[] adjTypes = null;
    private boolean isConvoyableCoast = false;
    private boolean isImpassable = false;
    private String[] borders = null;

    /**
     * Full name of Province (e.g., Mid-Atlantic Ocean)
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Set full name of province.
     */
    public void setFullName(String value) {
        fullName = value;
    }

    /**
     * Short (abbreviated) name of Province; (e.g., "mao" or "mid-atlantic")
     */
    public String[] getShortNames() {
        return shortNames;
    }

    /**
     * Set all short (abbreviated) names, from a List.
     */
    public void setShortNames(List<String> list) {
        shortNames = list.toArray(new String[list.size()]);
    }// setShortNames()

    /**
     * Province Adjacency array.
     */
    public String[] getAdjacentProvinceNames() {
        return adjProvinces;
    }

    /**
     * Set all adjacent province names.
     */
    public void setAdjacentProvinceNames(String[] values) {
        adjProvinces = values;
    }

    /**
     * Prvoince Adjacency type array.
     */
    public String[] getAdjacentProvinceTypes() {
        return adjTypes;
    }

    /**
     * Set all adjacent province types.
     */
    public void setAdjacentProvinceTypes(String[] values) {
        adjTypes = values;
    }

    /**
     * Gets whether this Province is a convoyable coastal province.
     */
    public boolean getConvoyableCoast() {
        return isConvoyableCoast;
    }

    /**
     * Sets whether this Province is a convoyable coastal province.
     */
    public void setConvoyableCoast(boolean value) {
        isConvoyableCoast = value;
    }

    /**
     * Gets whether this Province is impassable.
     */
    public boolean getImpassable() {
        return isImpassable;
    }

    /**
     * Sets whether this Province is impassable.
     */
    public void setImpassable(boolean value) {
        isImpassable = value;
    }


    /**
     * Gets the Border ID names for this province (if any)
     */
    public String[] getBorders() {
        return borders;
    }

    /**
     * Sets the Border ID names for this province (if any)
     */
    public void setBorders(List<String> list) {
        borders = list.toArray(new String[list.size()]);
    }// setBorders()

    /**
     * For debugging only!
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append(this.getClass().getName());
        sb.append('[');
        sb.append("fullName=");
        sb.append(fullName);
        sb.append(",#shortNames=");
        sb.append(shortNames.length);
        sb.append(",#adj_provinces=");
        sb.append(adjProvinces.length);
        sb.append(",#adj_types=");
        sb.append(adjTypes.length);
        sb.append(",isConvoyableCoast=");
        sb.append(isConvoyableCoast);
        sb.append(",isImpassable=");
        sb.append(isImpassable);
        sb.append(",#borders=");
        sb.append(borders.length);
        sb.append(']');
        return sb.toString();
    }// toString()
}// nested class ProvinceData	

