//
//  @(#)GUIDisband.java	12/2002
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

import info.jdip.gui.map.DefaultMapRenderer2;
import info.jdip.gui.map.MapMetadata;
import info.jdip.gui.map.SVGUtils;
import info.jdip.misc.Utils;
import info.jdip.order.Disband;
import info.jdip.order.Orderable;
import info.jdip.world.Location;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.Unit;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder implementation of Disband order.
 */
public class GUIDisband extends Disband implements GUIOrder {
    // instance variables
    private static final transient int REQ_LOC = 1;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;


    /**
     * Creates a GUIDisband
     */
    protected GUIDisband() {
        super();
    }// GUIDisband()

    /**
     * Creates a GUIDisband
     */
    protected GUIDisband(Power power, Location source, Unit.Type sourceUnitType) {
        super(power, source, sourceUnitType);
    }// GUIDisband()

    /**
     * This only accepts Disband orders. All others will throw an IllegalArgumentException.
     */
    public void deriveFrom(Orderable order) {
        if (!(order instanceof Disband)) {
            throw new IllegalArgumentException();
        }

        Disband disband = (Disband) order;
        power = disband.getPower();
        src = disband.getSource();
        srcUnitType = disband.getSourceUnitType();

        // set completed
        currentLocNum = REQ_LOC;
    }// deriveFrom()


    public boolean testLocation(StateInfo stateInfo, Location location, StringBuilder sb) {
        sb.setLength(0);

        if (isComplete()) {
            sb.append(Utils.getLocalString(GUIOrder.COMPLETE, getFullName()));
            return false;
        }


        Position position = stateInfo.getPosition();
        Province province = location.getProvince();
        Unit unit = position.getDislodgedUnit(province);

        if (unit != null) {
            if (!stateInfo.canIssueOrder(unit.getPower())) {
                sb.append(Utils.getLocalString(GUIOrder.NOT_OWNER, unit.getPower()));
                return false;
            }

            // acceptable
            sb.append(Utils.getLocalString(GUIOrder.CLICK_TO_ISSUE, getFullName()));
            return true;
        }

        // no *dislodged* unit in province
        sb.append(Utils.getLocalString(GUIOrder.NO_DISLODGED_UNIT, getFullName()));
        return false;
    }// testLocation()


    public boolean clearLocations() {
        if (isComplete()) {
            return false;
        }

        currentLocNum = 0;
        power = null;
        src = null;
        srcUnitType = null;

        return true;
    }// clearLocations()


    public boolean setLocation(StateInfo stateInfo, Location location, StringBuilder sb) {
        if (testLocation(stateInfo, location, sb)) {
            currentLocNum++;

            Unit unit = stateInfo.getPosition().getDislodgedUnit(location.getProvince());
            src = new Location(location.getProvince(), unit.getCoast());
            power = unit.getPower();
            srcUnitType = unit.getType();

            sb.setLength(0);
            sb.append(Utils.getLocalString(GUIOrder.COMPLETE, getFullName()));
            return true;
        }

        return false;
    }// setLocation()


    public boolean isComplete() {
        assert (currentLocNum <= getNumRequiredLocations());
        return (currentLocNum == getNumRequiredLocations());
    }// isComplete()

    public int getNumRequiredLocations() {
        return REQ_LOC;
    }

    public int getCurrentLocationNum() {
        return currentLocNum;
    }

    /**
     * Always throws an IllegalArgumentException
     */
    public void setParam(Parameter param, Object value) {
        throw new IllegalArgumentException();
    }

    /**
     * Always throws an IllegalArgumentException
     */
    public Object getParam(Parameter param) {
        throw new IllegalArgumentException();
    }


    public void removeFromDOM(MapInfo mapInfo) {
        if (group != null) {
            SVGGElement powerGroup = mapInfo.getPowerSVGGElement(power, LAYER_HIGHEST);
            GUIOrderUtils.removeChild(powerGroup, group);
            group = null;
        }
    }// removeFromDOM()


    /**
     * Draws a circle with an X in it
     */
    public void updateDOM(MapInfo mapInfo) {
        // if we are not displayable, we exit, after remove the order (if
        // it was created)
        if (!GUIOrderUtils.isDisplayable(power, mapInfo)) {
            removeFromDOM(mapInfo);
            return;
        }

        // determine if any change has occured. If no change has occured,
        // we will not change the DOM.
        //
        // we have nothing (yet) to check for change; isDependent() == false.
        // so just return if we have not been drawn.
        if (group != null) {
            return;
        }

        // there has been a change, if we are at this point.
        //

        // if we've not yet been created, we will create; if we've
        // already been created, we must remove the existing elements
        // in our group
        if (group == null) {
            // create group
            group = (SVGGElement) mapInfo.getDocument().createElementNS(
                    SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_G_TAG);

            mapInfo.getPowerSVGGElement(power, LAYER_HIGHEST).appendChild(group);
        } else {
            // remove group children
            GUIOrderUtils.deleteChildren(group);
        }

        // now, render the order
        //
        group.appendChild(drawOrder(mapInfo));

        // draw 'failed' marker, if appropriate.
        if (!mapInfo.getTurnState().isOrderSuccessful(this)) {
            SVGElement useElement = GUIOrderUtils.createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
            group.appendChild(useElement);
        }
    }// updateDOM()


    private SVGElement drawOrder(MapInfo mapInfo) {
        MapMetadata mmd = mapInfo.getMapMetadata();
        Point2D.Float srcPt = mmd.getDislodgedUnitPt(src.getProvince(), src.getCoast());    // dislodged unit!
        float radius = mmd.getOrderRadius(MapMetadata.EL_DISBAND, mapInfo.getSymbolName(srcUnitType));

        // calculate (but don't yet use) failPt
        // it will be on the right-hand part of the circle (r,0)
        failPt = new Point2D.Float(srcPt.x + radius, srcPt.y);

        // get symbolsize
        MapMetadata.SymbolSize symbolSize = mmd.getSymbolSize(DefaultMapRenderer2.SYMBOL_REMOVEUNIT)
                                               .getScaledSymbolSize(1 / mmd.getZoomFactor());

        // create RemoveUnit symbol via a USE element

        return SVGUtils.createUseElement(
                mapInfo.getDocument(),
                "#" + DefaultMapRenderer2.SYMBOL_REMOVEUNIT,
                null,    // no ID
                null,    // no special style
                srcPt.x,
                srcPt.y,
                symbolSize);
    }// drawOrder()


    public boolean isDependent() {
        return false;
    }


}// class GUIDisband
