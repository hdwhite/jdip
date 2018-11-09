//
//  @(#)MTLabeler.java		9/2003
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

import dip.gui.ClientFrame;
import dip.gui.map.DefaultMapRenderer2;
import dip.gui.map.MapMetadata;
import dip.world.Province;
import org.apache.batik.util.RunnableQueue;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGGElement;
import org.w3c.dom.svg.SVGTextElement;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashMap;

/**
 * Deals with the label text.
 * <p>
 * Assigns unique ID attributes to label text. If no IDs assigned,
 * attempts to assign on on basis of shortname or full-name match.
 * This works unless a name is split across multiple lines, and has
 * no ID attribute.
 * <p>
 */

public class MTLabeler {
    // constants
    private static final Point2D.Float ORIGIN = new Point2D.Float(0, 0);

    // fields
    private final java.util.Map<Province, TIMapEntry> map;
    private final dip.world.Map gameMap;
    private final MapMetadata mmd;
    private final DefaultMapRenderer2 dmr2;
    private final SVGGElement briefLayer;
    private final SVGGElement fullLayer;
    private final SVGDocument doc;
    private final RunnableQueue rq;
    private final MTCBar toolBar;

    /**
     * Create a MT (MapTool) Labeler object
     */
    public MTLabeler(ClientFrame cf, MTCBar toolBar) {
        map = Collections.synchronizedMap(new HashMap<>(111));
        gameMap = cf.getWorld().getMap();
        dmr2 = (DefaultMapRenderer2) cf.getMapPanel().getMapRenderer();
        mmd = dmr2.getMapMetadata();
        doc = cf.getMapPanel().getSVGDocument();
        this.toolBar = toolBar;

        briefLayer = dmr2.getLayer(DefaultMapRenderer2.LABEL_LAYER_BRIEF);
        fullLayer = dmr2.getLayer(DefaultMapRenderer2.LABEL_LAYER_FULL);

        // nice line
        rq = cf.getMapPanel().getJSVGCanvas().getUpdateManager().getUpdateRunnableQueue();

        // threadsafe setup
        rq.invokeLater(new Runnable() {
            public void run() {
                setup();
            }// run()
        });
    }// MTLabeler()


    /**
     * Get Brief TextInfo associaed with the province. Never null.
     * <b>DO NOT use setAttributes() from this TextInfo unless in a RunnableQueue</b>
     */
    public TextInfo getBriefTextInfo(Province p) {
        assert (map.get(p).brief != null);
        return map.get(p).brief;
    }// getBriefTextInfo()


    /**
     * Get Full TextInfo associated with the province. Never null.
     * <b>DO NOT use setAttributes() from this TextInfo unless in a RunnableQueue</b>
     */
    public TextInfo getFullTextInfo(Province p) {
        assert (map.get(p).full != null);
        return map.get(p).full;
    }// getFullTextInfo()


    /**
     * Easily change the label text position or style, for a brief label. Renders changes.
     */
    public void setBriefAttributes(Province p, final float x, final float y, final float rot,
                                   String cssClass, String cssStyle) {
        setAttributes(getBriefTextInfo(p), x, y, rot, cssClass, cssStyle);
    }// setBriefAttributes()


    /**
     * Easily change the label text position or style, for a full label
     */
    public void setFullAttributes(Province p, final float x, final float y, final float rot,
                                  String cssClass, String cssStyle) {
        setAttributes(getFullTextInfo(p), x, y, rot, cssClass, cssStyle);
    }// setFullAttributes()


    /**
     * Batik threadsafe attribute setter.
     */
    private void setAttributes(final TextInfo ti, final float x, final float y, final float rot,
                               final String cssClass, final String cssStyle) {
        rq.invokeLater(new Runnable() {
            public void run() {
                ti.setAttributes(x, y, rot, cssClass, cssStyle);
                //System.out.println(ti.toString());
            }// run()
        });
    }// setFullAttributes()


    /**
     * Reads in existing and/or creates new labels. THIS MUST occur in a RunnableQueue
     */
    private void setup() {
        final Province[] allProvs = gameMap.getProvinces();

        // setup map entries
        for (int i = 0; i < allProvs.length; i++) {
            map.put(allProvs[i], new TIMapEntry());
        }

        // read in existing map labels.
        // we only check children; sub-children are NOT checked.
        Node child = briefLayer.getFirstChild();
        while (child != null) {
            if (child instanceof SVGTextElement) {
                TextInfo ti = TextInfo.createTextInfo(gameMap, doc, (SVGTextElement) child);
                if (ti != null) {
                    TIMapEntry me = map.get(ti.getProvince());
                    if (ti.isBrief()) {
                        me.brief = ti;
                    } else {
                        me.full = ti;
                    }
                }
            }

            // look for another...
            child = child.getNextSibling();
        }

        child = fullLayer.getFirstChild();
        while (child != null) {
            if (child instanceof SVGTextElement) {
                TextInfo ti = TextInfo.createTextInfo(gameMap, doc, (SVGTextElement) child);
                if (ti != null) {
                    TIMapEntry me = map.get(ti.getProvince());
                    if (ti.isBrief()) {
                        me.brief = ti;
                    } else {
                        me.full = ti;
                    }
                }
            }

            // look for another...
            child = child.getNextSibling();
        }

        // check to see which map labels aren't read. Create empty labels.
        for (int i = 0; i < allProvs.length; i++) {
            TIMapEntry me = map.get(allProvs[i]);

            if (me.brief == null) {
                me.brief = new TextInfo(doc, allProvs[i], true);
            }

            if (me.full == null) {
                me.full = new TextInfo(doc, allProvs[i], false);
            }
        }

        // remove old nodes in layers, and replace with our shiny new nodes.
        removeChildren(briefLayer);
        removeChildren(fullLayer);

        for (int i = 0; i < allProvs.length; i++) {
            TIMapEntry me = map.get(allProvs[i]);
            briefLayer.appendChild(me.brief.getTextElement());
            fullLayer.appendChild(me.full.getTextElement());
        }

        toolBar.setTextInputEnabled(true);
    }// setup()


    /**
     * Remove all children under a Node
     */
    private void removeChildren(Node parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            parent.removeChild(child);
            child = parent.getFirstChild();
        }
    }// removeChildren()


    /**
     * Map entry class
     */
    private class TIMapEntry {
        public TextInfo full = null;
        public TextInfo brief = null;
    }// inner class TIMapEntry


}// class MTLabeler
