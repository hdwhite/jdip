//
//  @(#)GUIConvoy.java		12/2002
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

import info.jdip.gui.map.MapMetadata;
import info.jdip.misc.Utils;
import info.jdip.order.Convoy;
import info.jdip.order.Orderable;
import info.jdip.order.ValidationOptions;
import info.jdip.world.Coast;
import info.jdip.world.Location;
import info.jdip.world.Path;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.Unit;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGGElement;
import org.w3c.dom.svg.SVGLineElement;
import org.w3c.dom.svg.SVGPolygonElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder subclass of Convoy order.
 */
public class GUIConvoy extends Convoy implements GUIOrder {
    // i18n keys
    private static final String ONLY_SEA_OR_CC_FLEETS_CAN_CONVOY = "GUIConvoy.only_fleets_can_convoy";
    private static final String CLICK_TO_CONVOY = "GUIConvoy.click_to_convoy";
    private static final String NO_UNIT = "GUIConvoy.no_unit";
    private static final String CLICK_TO_CONVOY_ARMY = "GUIConvoy.click_to_convoy_army";
    private static final String CANNOT_CONVOY_LANDLOCKED = "GUIConvoy.no_convoy_landlocked";
    private static final String MUST_CONVOY_FROM_COAST = "GUIConvoy.must_convoy_from_coast";
    private static final String CLICK_TO_CONVOY_FROM = "GUIConvoy.click_to_convoy_from";
    private static final String NO_POSSIBLE_CONVOY_PATH = "GUIConvoy.no_path";
    private static final String MUST_CONVOY_TO_COAST = "GUIConvoy.must_convoy_to_coast";

    // instance variables
    private static final transient int REQ_LOC = 3;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;


    /**
     * Creates a GUIConvoy
     */
    protected GUIConvoy() {
        super();
    }// GUIConvoy()

    /**
     * Creates a GUIConvoy
     */
    protected GUIConvoy(Power power, Location src, Unit.Type srcUnitType,
                        Location convoySrc, Power convoyPower, Unit.Type convoySrcUnitType,
                        Location convoyDest) {
        super(power, src, srcUnitType, convoySrc, convoyPower,
                convoySrcUnitType, convoyDest);
    }// GUIConvoy()

    /**
     * This only accepts Convoy orders. All others will throw an IllegalArgumentException.
     */
    public void deriveFrom(Orderable order) {
        if (!(order instanceof Convoy)) {
            throw new IllegalArgumentException();
        }

        Convoy convoy = (Convoy) order;
        power = convoy.getPower();
        src = convoy.getSource();
        srcUnitType = convoy.getSourceUnitType();

        convoySrc = convoy.getConvoySrc();
        convoyDest = convoy.getConvoyDest();
        convoyPower = convoy.getConvoyedPower();
        convoyUnitType = convoy.getConvoyUnitType();

        // set completed
        currentLocNum = REQ_LOC;
    }// deriveFrom()


    public boolean testLocation(StateInfo stateInfo, Location location, StringBuilder sb) {
        sb.setLength(0);

        if (isComplete()) {
            sb.append(Utils.getLocalString(GUIOrder.COMPLETE, getFullName()));
            return false;
        }

        switch (currentLocNum) {
            case 0:
                return testConvoyOrigin(stateInfo, location, sb);
            case 1:
                return testConvoySource(stateInfo, location, sb);
            case 2:
                return testConvoyDestination(stateInfo, location, sb);
            default:
                // should not occur.
                throw new IllegalStateException();
        }
    }// testLocation()

    private boolean testConvoyOrigin(StateInfo stateInfo, Location location, StringBuilder sb) {
        Position position = stateInfo.getPosition();
        Province province = location.getProvince();
        // set Convoy origin (supporting unit)
        // We will check unit ownership too, if appropriate
        Unit unit = position.getUnit(province);
        if (unit == null) {
            // no unit in province
            sb.append(Utils.getLocalString(GUIOrder.NO_UNIT, getFullName()));
            return false;
        }

        if (!stateInfo.canIssueOrder(unit.getPower())) {
            sb.append(Utils.getLocalString(GUIOrder.NOT_OWNER, unit.getPower()));
            return false;
        }

        // we require a Fleet in a sea space or convoyable coast to be present.
        if (unit.getType() != Unit.Type.FLEET ||
            (!province.isSea() && !province.isConvoyableCoast())) {
            sb.append(Utils.getLocalString(ONLY_SEA_OR_CC_FLEETS_CAN_CONVOY));
            return false;
        }

        // check borders
        if (!GUIOrderUtils.checkBorder(this, new Location(province, unit.getCoast()), unit.getType(), stateInfo.getPhase(), sb)) {
            return false;
        }

        // order is acceptable
        sb.append(Utils.getLocalString(GUIOrder.CLICK_TO_ISSUE, getFullName()));
        return true;
    }

    private boolean testConvoySource(StateInfo stateInfo, Location location, StringBuilder sb) {
        Position position = stateInfo.getPosition();
        Province province = location.getProvince();
        // set Convoy source (unit being convoyed)
        // - If we are not validating, any location with a unit is acceptable (even source)
        // - If we are validating,
        //
        if (stateInfo.getValidationOptions().getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE)) {
            // lenient parsing enabled; we'll take anything with a unit!
            if (position.hasUnit(province)) {
                sb.append(Utils.getLocalString(CLICK_TO_CONVOY));
                return true;
            }

            // no unit in province
            sb.append(Utils.getLocalString(NO_UNIT));
            return false;
        }

        // strict parsing is enabled. We are more selective.
        // The location must contain a coastal Army unit
        //
        Unit unit = position.getUnit(province);
        if (unit == null) {
            // no unit in province
            sb.append(Utils.getLocalString(NO_UNIT));
            return false;
        }

        if (unit.getType() != Unit.Type.ARMY) {
            sb.append(Utils.getLocalString(MUST_CONVOY_FROM_COAST));
            return false;
        }
        
        if (!province.isCoastal()) {
            sb.append(Utils.getLocalString(CANNOT_CONVOY_LANDLOCKED));
            return false;
        }
        // check borders
        if (!GUIOrderUtils.checkBorder(this, new Location(province, unit.getCoast()), unit.getType(), stateInfo.getPhase(), sb)) {
            return false;
        }

        sb.append(Utils.getLocalString(CLICK_TO_CONVOY_ARMY));
        return true;
    }

    private boolean testConvoyDestination(StateInfo stateInfo, Location location, StringBuilder sb) {
        Position position = stateInfo.getPosition();
        Province province = location.getProvince();
        
        // set Convoy destination
        // - If we are not validating, any destination is acceptable (even source)
        // - If we are validating, we check that a theoretical (possible) convoy route to
        // 		the destination exists (could exist)
        //
        if (stateInfo.getValidationOptions().getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE)) {
            // lenient parsing enabled; we'll take anything!
            sb.append(Utils.getLocalString(CLICK_TO_CONVOY_FROM, province.getFullName()));
            return true;
        }

        // strict parsing is enabled. We are more selective. Check for a possible convoy route.
        if (!province.isCoastal()) {
            sb.append(Utils.getLocalString(MUST_CONVOY_TO_COAST));
            return false;
        }

        Path path = new Path(position);
        if (!path.isPossibleConvoyRoute(convoySrc, new Location(province, Coast.NONE))) {        
            sb.append(Utils.getLocalString(NO_POSSIBLE_CONVOY_PATH, convoySrc.getProvince().getFullName()));
            return false;
        }

        // check borders
        if (!GUIOrderUtils.checkBorder(this, location, convoyUnitType, stateInfo.getPhase(), sb)) {
            return false;
        }

        sb.append(Utils.getLocalString(CLICK_TO_CONVOY_FROM, province.getFullName()));
        return true;
    }

    public boolean clearLocations() {
        if (isComplete()) {
            return false;
        }

        currentLocNum = 0;
        power = null;
        src = null;
        srcUnitType = null;
        convoySrc = null;
        convoyDest = null;
        convoyPower = null;
        convoyUnitType = null;

        return true;
    }// clearLocations()


    public boolean setLocation(StateInfo stateInfo, Location location, StringBuilder sb) {
        if (isComplete()) {
            return false;
        }

        if (!testLocation(stateInfo, location, sb)) {
            return false;
        }

        switch (currentLocNum) {
            case 0:
                Unit unit = stateInfo.getPosition().getUnit(location.getProvince());
                src = new Location(location.getProvince(), unit.getCoast());
                power = unit.getPower();
                srcUnitType = unit.getType();
                currentLocNum++;
                return true;
            
            case 1:
                unit = stateInfo.getPosition().getUnit(location.getProvince());
                convoySrc = new Location(location.getProvince(), unit.getCoast());
                convoyUnitType = unit.getType();

                sb.setLength(0);
                sb.append("Convoying this unit.");
                currentLocNum++;
                return true;
            
            case 2:
                convoyDest = new Location(location.getProvince(), location.getCoast());

                sb.setLength(0);
                sb.append(Utils.getLocalString(GUIOrder.COMPLETE, getFullName()));
                currentLocNum++;
                return true;
            
            default:
                return false;
        }
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
            SVGGElement powerGroup = mapInfo.getPowerSVGGElement(power, LAYER_LOWEST);
            GUIOrderUtils.removeChild(powerGroup, group);
            group = null;
        }
    }// removeFromDOM()


    /**
     * Draws a dashed line to a triangle surrounding convoyed unit, and then a
     * dashed line from convoyed unit to destination.
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

            mapInfo.getPowerSVGGElement(power, LAYER_LOWEST).appendChild(group);
        } else {
            // remove group children
            GUIOrderUtils.deleteChildren(group);
        }

        // now, render the order
        //
        SVGElement[] elements = null;

        // create hilight line
        String cssStyle = mapInfo.getMapMetadata().getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_HILIGHT_CLASS);
        if (!cssStyle.equalsIgnoreCase("none")) {
            float offset = mapInfo.getMapMetadata().getOrderParamFloat(MapMetadata.EL_CONVOY, MapMetadata.ATT_HILIGHT_OFFSET)
                         / mapInfo.getMapMetadata().getZoomFactor();
            elements = drawOrder(mapInfo, offset, false);
            GUIOrderUtils.makeHilight(elements, mapInfo.getMapMetadata(), MapMetadata.EL_CONVOY);
            float width = Float.parseFloat(mapInfo.getMapMetadata().getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_SHADOW_WIDTH))
                                         / mapInfo.getMapMetadata().getZoomFactor();
            for (int i = 0; i < elements.length ; i++) {
                elements[i].setAttributeNS(null, SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, GUIOrderUtils.floatToString(width));
                group.appendChild(elements[i]);
            }
        }

        // create real line
        elements = drawOrder(mapInfo, 0, true);
        GUIOrderUtils.makeStyled(elements, mapInfo.getMapMetadata(), MapMetadata.EL_CONVOY, power);
        float width = Float.parseFloat(mapInfo.getMapMetadata().getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_WIDTH))
                                     / mapInfo.getMapMetadata().getZoomFactor();
        for (int i = 0; i < elements.length ; i++) {
            elements[i].setAttributeNS(null, SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, GUIOrderUtils.floatToString(width));
            group.appendChild(elements[i]);
        }

        // draw 'failed' marker, if appropriate.
        if (!mapInfo.getTurnState().isOrderSuccessful(this)) {
            SVGElement useElement = GUIOrderUtils.createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
            group.appendChild(useElement);
        }
    }// updateDOM()


    private SVGElement[] drawOrder(MapInfo mapInfo, float offset, boolean addMarker) {
        // setup
        SVGElement[] elements = new SVGElement[3];

        Position position = mapInfo.getTurnState().getPosition();
        MapMetadata mmd = mapInfo.getMapMetadata();
        Point2D.Float ptSrc = mmd.getUnitPt(src.getProvince(), src.getCoast());
        Point2D.Float ptConvoySrc = mmd.getUnitPt(convoySrc.getProvince(), convoySrc.getCoast());
        Point2D.Float ptConvoyDest = mmd.getUnitPt(convoyDest.getProvince(), convoyDest.getCoast());

        ptSrc.x += offset;
        ptSrc.y += offset;
        ptConvoySrc.x += offset;
        ptConvoySrc.y += offset;
        ptConvoyDest.x += offset;
        ptConvoyDest.y += offset;

        // radius
        float radius = mmd.getOrderRadius(MapMetadata.EL_CONVOY, mapInfo.getSymbolName(getConvoyUnitType()));

        // draw line to convoyed unit
        Point2D.Float newPtTo = GUIOrderUtils.getLineCircleIntersection(ptSrc.x, ptSrc.y,
                ptConvoySrc.x, ptConvoySrc.y, ptConvoySrc.x, ptConvoySrc.y, radius);

        elements[0] = (SVGLineElement)
                mapInfo.getDocument().createElementNS(
                        SVGDOMImplementation.SVG_NAMESPACE_URI,
                        SVGConstants.SVG_LINE_TAG);

        elements[0].setAttributeNS(null, SVGConstants.SVG_X1_ATTRIBUTE, GUIOrderUtils.floatToString(ptSrc.x));
        elements[0].setAttributeNS(null, SVGConstants.SVG_Y1_ATTRIBUTE, GUIOrderUtils.floatToString(ptSrc.y));
        elements[0].setAttributeNS(null, SVGConstants.SVG_X2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.x));
        elements[0].setAttributeNS(null, SVGConstants.SVG_Y2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.y));
        elements[0].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE,
                mmd.getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_STROKESTYLE));

        // draw triangle around supported unit
        Point2D.Float[] triPts = GUIOrderUtils.makeTriangle(ptConvoySrc, radius);

        StringBuilder sb = new StringBuilder(160);
        for (Point2D.Float triPt : triPts) {
            GUIOrderUtils.appendFloat(sb, triPt.x);
            sb.append(',');
            GUIOrderUtils.appendFloat(sb, triPt.y);
            sb.append(' ');
        }


        elements[1] = (SVGPolygonElement)
                mapInfo.getDocument().createElementNS(
                        SVGDOMImplementation.SVG_NAMESPACE_URI,
                        SVGConstants.SVG_POLYGON_TAG);

        elements[1].setAttributeNS(null, SVGConstants.SVG_POINTS_ATTRIBUTE, sb.toString());
        elements[1].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, mmd.getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_STROKESTYLE));

        // failPt will be a triangle vertext (12 o'clock vertex)
        failPt = new Point2D.Float(triPts[0].x, triPts[0].y);

        // draw line from triangle to convoyDest.
        // line will come from the closest triangular vertext, by distance.
        //
        Point2D.Float newPtFrom = null;
        float maxDistSquared = 0.0f;
        for (Point2D.Float triPt : triPts) {
            float distSquared = (float) (Math.pow((ptConvoyDest.x - triPt.x), 2.0) + Math.pow((ptConvoyDest.y - triPt.y), 2.0));
            if (distSquared > maxDistSquared) {
                maxDistSquared = distSquared;
                newPtFrom = triPt;
            }
        }

        // only respect convoyDest iff there is a unit present.
        if (position.hasUnit(convoyDest.getProvince())) {
            // use 'move' (EL_MOVE) order radius; 'hold' could also be appropriate.
            // we do this because the destination unit may have an order, and this
            // results in a better display.
            //
            Unit.Type destUnitType = position.getUnit(convoyDest.getProvince()).getType();
            float moveRadius = mmd.getOrderRadius(MapMetadata.EL_MOVE, mapInfo.getSymbolName(destUnitType));
            newPtTo = GUIOrderUtils.getLineCircleIntersection(newPtFrom.x, newPtFrom.y, ptConvoyDest.x, ptConvoyDest.y, ptConvoyDest.x, ptConvoyDest.y, moveRadius);
        } else {
            newPtTo = ptConvoyDest;
        }

        elements[2] = (SVGLineElement)
                mapInfo.getDocument().createElementNS(
                        SVGDOMImplementation.SVG_NAMESPACE_URI,
                        SVGConstants.SVG_LINE_TAG);

        elements[2].setAttributeNS(null, SVGConstants.SVG_X1_ATTRIBUTE, GUIOrderUtils.floatToString(newPtFrom.x));
        elements[2].setAttributeNS(null, SVGConstants.SVG_Y1_ATTRIBUTE, GUIOrderUtils.floatToString(newPtFrom.y));
        elements[2].setAttributeNS(null, SVGConstants.SVG_X2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.x));
        elements[2].setAttributeNS(null, SVGConstants.SVG_Y2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.y));
        elements[2].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE,
                mmd.getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_STROKESTYLE));

        // marker
        if (addMarker || offset != 0.0f) {
            GUIOrderUtils.addMarker(elements[2], mmd, MapMetadata.EL_CONVOY);
        }


        // add to parent
        return elements;
    }// drawOrder()


    public boolean isDependent() {
        return false;
    }


}// class GUIConvoy
