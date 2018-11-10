package info.jdip.plugin.maptool;//
//  @(#)info.jdip.plugin.maptool.MTLabeler.java		9/2003
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

import info.jdip.world.Province;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGTextElement;

import java.awt.geom.Point2D;


/**
 * Simplifies the handling of SVG Text elements.
 * <p>
 * transforms (rotate and translate only) are supported.
 * Note that angle, when specified, is always in degrees,
 * and may be positive or negative.
 */
class TextInfo {
    // constants: prefixed to IDs
    private static final String PREFIX_BRIEF = "brf_";
    private static final String PREFIX_FULL = "ful_";

    // fields
    private final Province p;
    private final SVGTextElement te;
    private final boolean isBrief;

    private String text = null;
    private float x = 0.0f;
    private float y = 0.0f;
    private float rot = 0.0f;    // rotation angle, in degrees


    /**
     * Create an empty info.jdip.plugin.maptool.TextInfo Object for the given province.
     */
    public TextInfo(SVGDocument doc, Province p, boolean isBrief) {
        if (p == null || doc == null) {
            throw new IllegalArgumentException();
        }

        this.p = p;
        this.isBrief = isBrief;

        te = (SVGTextElement) doc.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI,
                SVGConstants.SVG_TEXT_TAG);

        // set ID
        String id = (isBrief) ? (PREFIX_BRIEF + p.getShortName()) : (PREFIX_FULL + p.getShortName());
        te.setAttributeNS(null, SVGConstants.SVG_ID_ATTRIBUTE, id);

        // create text node, and add to text element
        text = (isBrief) ? (p.getShortName()) : (p.getFullName());
        te.appendChild(doc.createTextNode(text));

        // set default position.
        te.setAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE, String.valueOf(x));
        te.setAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE, String.valueOf(y));
    }// info.jdip.plugin.maptool.TextInfo()


    /**
     * Creates a info.jdip.plugin.maptool.TextInfo given a TextElement; returns null
     * if info.jdip.plugin.maptool.TextInfo cannot be created because no ID found or
     * no match.
     */
    public static TextInfo createTextInfo(info.jdip.world.Map map, SVGDocument doc, SVGTextElement el) {
        // check params
        if (map == null || doc == null || el == null) {
            throw new IllegalArgumentException("null param(s)");
        }

        TextInfo ti = null;

        // first, check ID
        final String id = el.getAttributeNS(null, SVGConstants.SVG_ID_ATTRIBUTE);
        if (id.startsWith(PREFIX_BRIEF)) {
            Province prov = map.getProvince(id.substring(PREFIX_BRIEF.length()));
            if (prov == null) {
                return null;
            }
            ti = new TextInfo(doc, prov, true);
        } else if (id.startsWith(PREFIX_FULL)) {
            Province prov = map.getProvince(id.substring(PREFIX_FULL.length()));
            if (prov == null) {
                return null;
            }
            ti = new TextInfo(doc, prov, false);
        } else {
            String nodeText = null;

            // get node text
            el.normalize();
            Node node = el.getFirstChild();
            if (node != null && node.getNodeType() == Node.TEXT_NODE) {
                nodeText = node.getNodeValue();
            } else {
                return null;
            }

            // if no ID, attempt to match against short and long province names
            // if no match, we're null.
            Province prov = map.getProvinceMatching(nodeText);
            if (prov == null) {
                return null;
            }

            nodeText = nodeText.trim();
            boolean nodeBrief = false;
            for (int i = 0; i < prov.getShortNames().length; i++) {
                if (prov.getShortNames()[i].equalsIgnoreCase(nodeText)) {
                    nodeBrief = true;
                    break;
                }
            }

            ti = new TextInfo(doc, prov, nodeBrief);
        }

        // for either case (unless not found), set style info
        if (ti != null) {
            try {
                final String xform = el.getAttributeNS(null, SVGConstants.SVG_TRANSFORM_ATTRIBUTE).trim();
                if (xform.equals("")) {
                    final float px = Float.parseFloat(el.getAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE));
                    final float py = Float.parseFloat(el.getAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE));

                    ti.setAttributes(
                            px, py, 0.0f,
                            el.getAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE),
                            el.getAttributeNS(null, SVGConstants.SVG_STYLE_ATTRIBUTE));
                } else {
                    // transform present? if so, parsing is different.
                    // (we parse the transform instead)
                    //
                    SVGMatrix mat = el.getTransformToElement(doc.getRootElement());
					
					/*   
					   a	cos(angle) 
					   b	sin(angle)
					   c	-sin(angle)
					   d	cos(angle) 
					   e	tx
					   f	ty
					
						Math.acos() range: 0->pi (0->180)
						So if in quadrants --,+- (cos,sin) subtract from 360.
							(thus if sin() is negative... we are in that quadrant)
					
					*/
                    // angle, in degrees
                    float angle = (float) Math.toDegrees(Math.acos(mat.getA()));

                    // quadrant-correction
                    angle = (mat.getB() >= 0.0f) ? angle : (360.0f - angle);

                    ti.setAttributes(
                            mat.getE(), mat.getF(), angle,
                            el.getAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE),
                            el.getAttributeNS(null, SVGConstants.SVG_STYLE_ATTRIBUTE));
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return ti;
    }// info.jdip.plugin.maptool.TextInfo()


    /**
     * Print out as XML
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(128);

        sb.append("<text id=\"");
        sb.append(te.getAttributeNS(null, SVGConstants.SVG_ID_ATTRIBUTE));
        sb.append("\"");

        if (rot == 0.0f) {
            sb.append(" x=\"");
            sb.append(MTHelper.formatFloat(x));
            sb.append("\" y=\"");
            sb.append(MTHelper.formatFloat(y));
            sb.append("\"");
        } else {
            sb.append(" transform=\"");
            sb.append(makeXForm());
            sb.append("\" x=\"0\" y=\"0\"");
        }

        String cssClass = te.getAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE);
        if (!cssClass.equals("")) {
            sb.append(" class=\"");
            sb.append(cssClass);
            sb.append("\"");
        }

        String cssStyle = te.getAttributeNS(null, SVGConstants.SVG_STYLE_ATTRIBUTE);
        if (!cssStyle.equals("")) {
            sb.append(" style=\"");
            sb.append(cssStyle);
            sb.append("\"");
        }

        sb.append(">");
        sb.append(text);
        sb.append("</text>");

        return sb.toString();
    }// toString()


    /**
     * Create the transform string
     */
    private String makeXForm() {
        // example: translate(1090, 710), rotate(335)
        StringBuilder sb = new StringBuilder(128);
        sb.append("translate(");
        sb.append(MTHelper.formatFloat(x));
        sb.append(",");
        sb.append(MTHelper.formatFloat(y));
        sb.append("), rotate(");
        sb.append(MTHelper.formatFloat(rot));
        sb.append(")");
        return sb.toString();
    }// makeXForm()


    /**
     * Set the Style and Location of the element
     * as well as all other required data.
     * The text is chosen based on brief/full.
     * <p>
     * cssClass, cssStyle may not be null (use empty string instead)
     * <p>
     *
     * <b>WARNING: DO NOT use setAttributes() on an element within a live Batik SVG
     * document unless in a RunnableQueue</b>
     */
    public void setAttributes(final float x, final float y, final float rot, String cssClass, String cssStyle) {
        if (cssClass == null || cssStyle == null) {
            throw new IllegalArgumentException("style/class strings must not be null.");
        }

        // set member fields for easy access...
        this.x = x;
        this.y = y;
        this.rot = rot;

        // set attributes
        if (rot == 0.0f) {
            te.setAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE, String.valueOf(x));
            te.setAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE, String.valueOf(y));
            te.setAttributeNS(null, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, null);
        } else {
            te.setAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE, String.valueOf(0));
            te.setAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE, String.valueOf(0));
            te.setAttributeNS(null, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, makeXForm());
        }

        te.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, cssClass);
        te.setAttributeNS(null, SVGConstants.SVG_STYLE_ATTRIBUTE, cssStyle);
    }// setAttributes()

    /**
     * Get the Text Element
     */
    public SVGTextElement getTextElement() {
        return te;
    }

    /**
     * Get CSS Style, or empty string if none.
     */
    public String getCSSStyle() {
        if (te != null) {
            return te.getAttributeNS(null, SVGConstants.SVG_STYLE_ATTRIBUTE);
        }
        return "";
    }// getCSSStyle()

    /**
     * Get CSS Class, or empty string if none.
     */
    public String getCSSClass() {
        if (te != null) {
            return te.getAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE);
        }
        return "";
    }// getCSSClass()

    /**
     * Get Location, or (0,0) if none.
     */
    public Point2D.Float getLocation() {
        return new Point2D.Float(x, y);
    }

    /**
     * Get Rotation, or 0.0f if none; in degrees
     */
    public float getRotation() {
        return rot;
    }

    /**
     * Get Province we refer to; never is null.
     */
    public Province getProvince() {
        return p;
    }

    /**
     * Determine if we are Brief or Full
     */
    public boolean isBrief() {
        return isBrief;
    }

    /**
     * Determine if we have placed this element (true if location is NOT (0,0) and NOT rotated)
     */
    public boolean isPlaced() {
        return (!(x == 0.0f && y == 0.0f && rot == 0.0f));
    }
}// info.jdip.plugin.maptool.TextInfo

