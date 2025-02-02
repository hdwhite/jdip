package info.jdip.plugin.maptool;//
//  @(#)info.jdip.plugin.maptool.MTOutput.java		6/2003
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

import info.jdip.gui.ClientFrame;
import info.jdip.gui.dialog.TextViewer;
import info.jdip.gui.map.MapMetadata;
import info.jdip.gui.map.MapPanel;
import info.jdip.world.Coast;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.World;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.geom.Point2D;


/**
 * Outputs placement, label data properly XML (SVG) formatted.
 * <p>
 * currently outputs to stdout. Will output to textviewer in future
 * for easy cut-and-paste
 */

public class MTOutput {
    final World world;
    final info.jdip.world.Map worldMap;
    final Power[] allPowers;
    final Province[] allProvs;
    final MapMetadata mmd;
    final MapPanel mapPanel;
    final MTHelper mth;
    final MTLabeler mtl;
    ClientFrame clientFrame = null;

    /**
     * info.jdip.plugin.maptool.MTOutput constructor
     */
    public MTOutput(ClientFrame cf, MTHelper mth, MTLabeler mtl) {
        clientFrame = cf;
        this.mth = mth;
        this.mtl = mtl;
        world = cf.getWorld();
        worldMap = world.getMap();
        allPowers = worldMap.getPowers();
        allProvs = worldMap.getProvinces();
        mapPanel = cf.getMapPanel();
        mmd = mapPanel.getMapRenderer().getMapMetadata();
    }// info.jdip.plugin.maptool.MTOutput()

    /**
     * Show the output
     */
    public static void showOutput(ClientFrame cf, MTHelper helper, MTLabeler mtl) {
        MTOutput mto = new MTOutput(cf, helper, mtl);

        // show in a text viewer
        TextViewer tv = new TextViewer(cf, true);
        tv.addSingleButton(tv.makeOKButton());
        tv.setEditable(false);
        tv.setTitle("Map Editing Results");
        tv.setHeaderVisible(false);
        tv.setContentType("text/plain");
        tv.setFont(new Font("mono", Font.PLAIN, 10));
        tv.setText(mto.getOutput());
        tv.displayDialog();
    }// showOutput()

    /**
     * Get properly formatted output as a String
     */
    private String getOutput() {
        StringBuilder sb = new StringBuilder(16384);

        sb.append(getPlacements());

        sb.append("\n\n\n");

        sb.append(getLabelData());


        return sb.toString();
    }// getOutput()

    /**
     * Write placements as they should be formatted.
     */
    private String getPlacements() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<jdipNS:PROVINCE_DATA>\n");

        for (Province p : allProvs) {
            MapMetadata.InfoEntry ie = mmd.getInfoEntry(p);

            // print standard coast info first (shouldn't matter: Coast.LAND or Coast.SEA
            sb.append("   ");
            sb.append(makePROVINCEtag(ie, p, Coast.LAND));
            sb.append("\n");


            // if we are multi-coastal, we must print all coast data.
            Coast[] multiCoasts = p.getValidDirectionalCoasts();
            for (Coast multiCoast : multiCoasts) {
                sb.append("   ");
                sb.append(makePROVINCEtag(ie, p, multiCoast));
                sb.append("\n");
            }
        }

        sb.append("</jdipNS:PROVINCE_DATA>\n");
        return sb.toString();
    }// getPlacements()

    /**
     * Creates a PROVINCE xml tag and children
     */
    private String makePROVINCEtag(MapMetadata.InfoEntry ie, Province p, Coast coast) {
        StringBuilder sb = new StringBuilder(256);

        // province: <jdipNS:PROVINCE name="ank">
        sb.append("<jdipNS:PROVINCE name=\"");

        sb.append(p.getShortName());

        if (coast.isDirectional()) {
            sb.append("-");
            sb.append(coast.getAbbreviation());
        }

        sb.append("\">");

        // unit: <jdipNS:UNIT x="1313" y="1120"/>
        // DO NOT print if 0,0
        if (!ie.getUnitPt(coast).equals(MTHelper.ORIGIN)) {
            sb.append("<jdipNS:UNIT ");
            sb.append(makeXY(ie.getUnitPt(coast)));
            sb.append("/>");
        }

        // dislodged unit: <jdipNS:DISLODGED_UNIT x="1300.5" y="1107.5"/>
        if (!ie.getDislodgedUnitPt(coast).equals(MTHelper.ORIGIN)) {
            sb.append("<jdipNS:DISLODGED_UNIT ");
            sb.append(makeXY(ie.getDislodgedUnitPt(coast)));
            sb.append("/>");
        }

        // supply center: <jdipNS:SUPPLY_CENTER x="1271" y="1144"/>
        // hide if we are directional
        if (!coast.isDirectional()) {
            if (!ie.getSCPt().equals(MTHelper.ORIGIN)) {
                sb.append("<jdipNS:SUPPLY_CENTER ");
                sb.append(makeXY(ie.getSCPt()));
                sb.append("/>");
            }
        }

        // close & exit
        sb.append("</jdipNS:PROVINCE>");
        return sb.toString();
    }// makePROVINCEtag()

    /**
     * Converts a Point2D to a x="" y="" style text point.
     * Truncates to a single decimal place, or none if 0.
     */
    public StringBuilder makeXY(Point2D point) {
        StringBuilder sb = new StringBuilder(32);
        sb.append("x=\"");
        sb.append(MTHelper.formatFloat(point.getX()));
        sb.append("\" y=\"");
        sb.append(MTHelper.formatFloat(point.getY()));
        sb.append("\"");
        return sb;
    }// makeXY()

    /**
     * Print label layer SVG (XML) data (suitable for pasting)
     */
    private String getLabelData() {
        if (mth == null) {
            return "\n[Label Data not Available]\n";
        }

        StringBuilder sb = new StringBuilder(8192);

        sb.append("\n\n*** PROVINCE BRIEF NAME LAYER ***\n");
        for (Province province : allProvs) {
            sb.append("   ");
            sb.append(mtl.getBriefTextInfo(province).toString());
            sb.append("\n");
        }


        sb.append("\n\n*** PROVINCE FULL NAME LAYER ***\n");
        for (Province province : allProvs) {
            sb.append("   ");
            sb.append(mtl.getFullTextInfo(province).toString());
            sb.append("\n");
        }


        return sb.toString();
    }// getLabelData()


    /**
     * Handles Writing of the DOM
     */
    private class DOMWriter {
        StringBuilder sb = null;        // output buffer

        /**
         * Constructor
         */
        public DOMWriter() {
        }// DOMWriter()

        /**
         * writeTree
         */
        public String writeTree(Node node) {
            sb = new StringBuilder(4096);
            writeNode(node);
            return sb.toString();
        }// writeTree()


        /**
         * Recursively writes node. Only handles major node types, not including entitity nodes.
         */
        private void writeNode(Node node) {
            int type = node.getNodeType();

            if (type == Node.ELEMENT_NODE) {
                sb.append("<");
                sb.append(node.getNodeName());

                // check for attributes
                if (node.hasAttributes()) {
                    NamedNodeMap AttributesList = node.getAttributes();
                    for (int j = 0; j < AttributesList.getLength(); j++) {
                        sb.append(' ');
                        sb.append(AttributesList.item(j).getNodeName());
                        sb.append("=\"");
                        sb.append(AttributesList.item(j).getNodeValue());
                        sb.append("\"");
                    }
                }

                sb.append(">\n");
            } else if (type == Node.TEXT_NODE) {
                String content = node.getNodeValue();
                if (!content.trim().equals("")) {
                    sb.append(content);
                }
            } else if (type == Node.COMMENT_NODE) {
                String content = node.getNodeValue();
                if (!content.trim().equals("")) {
                    sb.append("<!-- ");
                    sb.append(content);
                    sb.append("-->\n");
                }
            }

            // check if current node has any children
            NodeList children = node.getChildNodes();
            if (children != null) {
                for (int i = 0; i < children.getLength(); i++) {
                    // recurse
                    writeNode(children.item(i));
                }
            }
        }// writeNode()

    }// DOMWriter()


}// class info.jdip.plugin.maptool.MTOutput

