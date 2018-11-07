//
//  @(#)MTChecker.java		6/2003
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
import dip.gui.dialog.TextViewer;
import dip.gui.map.MapMetadata;
import dip.gui.map.MapPanel;
import dip.world.Coast;
import dip.world.Power;
import dip.world.Province;
import dip.world.World;

import java.awt.*;


/**
 * Outputs placement, label data properly XML (SVG) formatted.
 * <p>
 * currently outputs to stdout. Will output to textviewer in future
 * for easy cut-and-paste
 */

public class MTChecker {
    final World world;
    final dip.world.Map worldMap;
    final Power[] allPowers;
    final Province[] allProvs;
    final MapMetadata mmd;
    final MapPanel mapPanel;
    final MTHelper mth;
    final MTLabeler mtl;
    ClientFrame clientFrame = null;


    /**
     * MTOutput constructor
     */
    public MTChecker(ClientFrame cf, MTHelper mth, MTLabeler mtl) {
        clientFrame = cf;
        this.mth = mth;
        this.mtl = mtl;
        world = cf.getWorld();
        worldMap = world.getMap();
        allPowers = worldMap.getPowers();
        allProvs = worldMap.getProvinces();
        mapPanel = cf.getMapPanel();
        mmd = mapPanel.getMapRenderer().getMapMetadata();
    }// MTOutput()

    /**
     * Show the checking output
     */
    public static void checkMap(ClientFrame cf, MTHelper helper, MTLabeler mtl) {
        MTChecker mtc = new MTChecker(cf, helper, mtl);

        // show in a text viewer
        TextViewer tv = new TextViewer(cf, true);
        tv.addSingleButton(tv.makeOKButton());
        tv.setEditable(false);
        tv.setTitle("Map Check Results");
        tv.setHeaderVisible(false);
        tv.setContentType("text/plain");
        tv.setFont(new Font("mono", Font.PLAIN, 10));
        tv.setText(mtc.getOutput());
        tv.displayDialog();
    }// showOutput()

    /**
     * Get properly formatted output as a String
     */
    private String getOutput() {
        StringBuffer sb = new StringBuffer(16384);


        sb.append(checkSC());

        sb.append("\n\n\n");

        sb.append(checkUnits());

        sb.append("\n\n\n");

        sb.append(checkDislodgedUnits());

        sb.append("\n\n\n");
        sb.append("Missing Brief Province Names:\n");
        sb.append(checkProvinceNames(true));

        sb.append("\n\n\n");
        sb.append("Missing Full Province Names:\n");
        sb.append(checkProvinceNames(false));

        return sb.toString();
    }// getOutput()

    /**
     * Check that all Provinces that require an SC have an SC placed
     */
    private String checkSC() {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("Provinces that Require an SC (for this variant):\n");
        boolean allOK = true;

        for (int i = 0; i < allProvs.length; i++) {
            Province p = allProvs[i];
            if (p.hasSupplyCenter()) {
                MapMetadata.InfoEntry ie = mmd.getInfoEntry(p);
                if (ie.getSCPt().equals(MTHelper.ORIGIN)) {
                    // yikes! we didn't assign!
                    allOK = false;
                    sb.append("    ");
                    sb.append(p.getShortName());
                    sb.append("\n");
                }
            }
        }

        if (allOK) {
            sb.append("    All required SC have been set.\n");
        }

        return sb.toString();
    }// checkSC()

    /**
     * Check that unit positions (including coasts, for multi-coastal) have been set.
     */
    private String checkUnits() {
        StringBuffer sbMain = new StringBuffer(1024);
        sbMain.append("Provinces That Require NON-dislodged Unit Positions:\n");
        boolean allOK = true;

        for (int i = 0; i < allProvs.length; i++) {
            Province p = allProvs[i];
            MapMetadata.InfoEntry ie = mmd.getInfoEntry(p);
            boolean isOK = true;

            StringBuffer sb = new StringBuffer(128);
            sb.append("    ");
            sb.append(p.getShortName());
            sb.append(": missing unit position(s): ");

            // non-dislodged: check
            // NOTE: the way MapMetadata works: we have only 'directional' coast info
            // and NON-directional coast info. Thus Coast.Land == Coast.Sea == Coast.Single
            // and are distinct from any directional coasts.
            //
            if (ie.getUnitPt(Coast.LAND).equals(MTHelper.ORIGIN)) {
                sb.append("main (land/sea/single); ");
                allOK = false;
                isOK = false;
            }

            // if we are multi-coastal, we must print all coast data.
            Coast[] multiCoasts = p.getValidDirectionalCoasts();
            for (int mcIdx = 0; mcIdx < multiCoasts.length; mcIdx++) {
                if (ie.getUnitPt(multiCoasts[mcIdx]).equals(MTHelper.ORIGIN)) {
                    sb.append(multiCoasts[mcIdx].getName());
                    sb.append("; ");
                    allOK = false;
                    isOK = false;
                }
            }

            if (!isOK) {
                sb.append("\n");
                sbMain.append(sb);
            }
        }

        if (allOK) {
            sbMain.append("    All required non-dislodged unit positions have been set.\n");
        }


        return sbMain.toString();
    }// checkUnits()


    /**
     * Check that unit positions (including coasts, for multi-coastal) have been set.
     * For DISLODGED units.
     */
    private String checkDislodgedUnits() {
        StringBuffer sbMain = new StringBuffer(1024);
        sbMain.append("Provinces That Require Dislodged Unit Positions:\n");
        boolean allOK = true;

        for (int i = 0; i < allProvs.length; i++) {
            Province p = allProvs[i];
            MapMetadata.InfoEntry ie = mmd.getInfoEntry(p);
            boolean isOK = true;

            StringBuffer sb = new StringBuffer(128);
            sb.append("    ");
            sb.append(p.getShortName());
            sb.append(": missing unit position(s): ");

            // non-dislodged: check
            // NOTE: the way MapMetadata works: we have only 'directional' coast info
            // and NON-directional coast info. Thus Coast.Land == Coast.Sea == Coast.Single
            // and are distinct from any directional coasts.
            //
            if (ie.getDislodgedUnitPt(Coast.LAND).equals(MTHelper.ORIGIN)) {
                sb.append("main (land/sea/single); ");
                allOK = false;
                isOK = false;
            }

            // if we are multi-coastal, we must print all coast data.
            Coast[] multiCoasts = p.getValidDirectionalCoasts();
            for (int mcIdx = 0; mcIdx < multiCoasts.length; mcIdx++) {
                if (ie.getDislodgedUnitPt(multiCoasts[mcIdx]).equals(MTHelper.ORIGIN)) {
                    sb.append(multiCoasts[mcIdx].getName());
                    sb.append("; ");
                    allOK = false;
                    isOK = false;
                }
            }

            if (!isOK) {
                sb.append("\n");
                sbMain.append(sb);
            }
        }

        if (allOK) {
            sbMain.append("    All required dislodged unit positions have been set.\n");
        }


        return sbMain.toString();
    }// checkDislodgedUnits()

    /**
     * Check that all Provinces have labels; isBrief = brief (or full) label
     */
    private String checkProvinceNames(boolean isBrief) {
        StringBuffer sb = new StringBuffer(1024);
        boolean allOK = true;

        for (int i = 0; i < allProvs.length; i++) {
            TextInfo ti = (isBrief) ? mtl.getBriefTextInfo(allProvs[i]) : mtl.getFullTextInfo(allProvs[i]);
            if (!ti.isPlaced()) {
                sb.append("    ");
                sb.append(allProvs[i].getShortName());
                if (isBrief) {
                    sb.append(": missing brief label");
                } else {
                    sb.append(": missing full (long) label");
                }
                allOK = false;
            }
        }

        if (allOK) {
            if (isBrief) {
                sb.append("     All provinces have brief labels set.");
            } else {
                sb.append("     All provinces have full (long) labels set.");
            }
        }

        return sb.toString();
    }// checkProvinceNames()


}// class MTChecker
