//
//  @(#)StateWriter.java		6/2003
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
package info.jdip.gui.report;

import info.jdip.gui.ClientFrame;
import info.jdip.gui.dialog.TextViewer;
import info.jdip.misc.Help;
import info.jdip.misc.Utils;
import info.jdip.order.Order;
import info.jdip.order.OrderFormatOptions;
import info.jdip.order.Orderable;
import info.jdip.process.Adjustment;
import info.jdip.world.Phase;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


/**
 * Writes the current game state as HTML.
 * <p>
 * Includes:
 * <ol>
 * <li>Current Phase
 * <li>Current Unit Positions
 * <li>Current Orders
 * <li>Current Supply Center owners
 * <li>Current Adjustments <b>or</b> Current Retreats
 * </ol>
 */
public class StateWriter {
    // i18n constants
    private static final String HTML_TEMPLATE = "StateWriter.template";
    private static final String MSG_NO_ORDERS_SUBMITTED = "StateWriter.order.notsubmitted";
    private static final String MSG_POWER_ELIMINATED = "StateWriter.order.eliminated";
    private static final String MSG_UNAVAILABLE = "StateWriter.order.unavailable";
    private static final String MSG_NONE = "StateWriter.quantity.none";
    private static final String DISLODGED_HEADER_TEXT = "StateWriter.header.dislodged";
    private static final String ADJUSTMENT_HEADER_TEXT = "StateWriter.header.adjustment";
    private static final String NO_DISLODGED_UNITS = "StateWriter.dislodged.none";
    private static final String ADJ_BUILD_TEXT = "StateWriter.adjustment.text.build";
    private static final String ADJ_REMOVE_TEXT = "StateWriter.adjustment.text.remove";
    private static final String ADJ_BASIC_TEXT = "StateWriter.adjustment.text";
    private static final String ADJ_NOCHANGE_TEXT = "StateWriter.adjustment.text.nochange";
    private static final String ADJ_BLOCKED_BUILD_TEXT = "StateWriter.adjustment.text.blockedbuilds";
    private static final String SC_NUM = "StateWriter.sc.number";
    private static final String ORD_TOO_FEW = "StateWriter.order.toofew";

    // i18n dialog constants
    private static final String DIALOG_TITLE = "StateWriter.dialog.title";


    // instance constants
    private final Power[] displayablePowers;
    private final TurnState turnState;
    private final Power[] allPowers;
    private final java.util.Map<Power, List<String>> powerMap;
    private final Adjustment.AdjustmentInfoMap adjMap;
    private final OrderFormatOptions ofo;


    /**
     * StateWriter constructor
     */
    private StateWriter(ClientFrame cf, TurnState ts) {
        assert (cf != null);
        turnState = ts;
        allPowers = ts.getWorld().getMap().getPowers();
        displayablePowers = (cf == null) ? allPowers : cf.getDisplayablePowers();
        powerMap = getUnitsByPower();
        adjMap = Adjustment.getAdjustmentInfo(turnState,
                turnState.getWorld().getRuleOptions(), allPowers);
        ofo = cf.getOFO();
    }// StateWriter()

    /**
     * Displays a summary of the current game state as HTML.
     * Obeys the displayablePowers setting (obtained from
     * ClientFrame). If no ClientFrame supplied, all displayable
     * powers are shown.
     */
    public static String stateToHTML(ClientFrame cf, TurnState ts) {
        StateWriter sw = new StateWriter(cf, ts);
        return sw.getStateAsHTML();
    }// stateToHTML()

    /**
     * Returns the HTML-encoded current state inside a dialog.
     */
    public static void displayDialog(final ClientFrame clientFrame,
                                     final TurnState ts) {
        final StringBuilder title = new StringBuilder(64);
        title.append(Utils.getLocalString(DIALOG_TITLE));
        title.append(": ");
        title.append(ts.getPhase());

        TextViewer tv = new TextViewer(clientFrame);
        tv.setEditable(false);
        tv.addSingleButton(tv.makeOKButton());
        tv.setTitle(title.toString());
        tv.setHelpID(Help.HelpID.Dialog_StatusReport);
        tv.setHeaderVisible(false);
        tv.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        tv.lazyLoadDisplayDialog(new TextViewer.TVRunnable() {
            public void run() {
                setText(stateToHTML(clientFrame, ts));
            }
        });
    }// displayDialog()

    /**
     * Write state as HTML
     */
    private String getStateAsHTML() {
        // get template
        String templateText = Utils.getText(Utils.getLocalString(HTML_TEMPLATE));

        // write retreat or adjustment information, if appropriate.
        String header = "";
        String info = "";

        if (turnState.getPhase().getPhaseType() == Phase.PhaseType.RETREAT) {
            header = Utils.getLocalString(DISLODGED_HEADER_TEXT);
            info = getDislodgedInfo();
        } else if (turnState.getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT) {
            header = Utils.getLocalString(ADJUSTMENT_HEADER_TEXT);
            info = getAdjustmentInfo();
        }


        // get template objects
        Object[] templateData = new Object[]
                {
                        turnState.getPhase(),    // {0} : Phase
                        getUnitLocationTable(),    // {1} : Unit Location Table
                        getOrders(),            // {2} : Orders, by power. Non-displayed powers listed as "unknown"
                        getSCInfo(),            // {3} : Supply Center Information
                        header,                    // {4} : Dislodged/Adjustment header (or empty)
                        info                    // {5} : Dislodged/Adjustment info (or empty)
                };

        // format into template
        return Utils.format(templateText, templateData);
    }// getState()

    /**
     * Unit Location Table<p>
     * The positions of all units -- for all powers -- are always available.
     * A light highlight color for the rows is applied for readability.
     */
    private String getUnitLocationTable() {
        StringBuilder sb = new StringBuilder(1024);
        int nRows = -1;    // max # of rows

        sb.append("<table cellpadding=\"3\" cellspacing\"3\">");

        // column headers (the power name)
        sb.append("<tr>");
        for (int i = 0; i < allPowers.length; i++) {
            // odd columns have bgcolor highlights
            sb.append((i & 1) == 0 ? "<th bgcolor=\"F0F8FF\">" : "<th>");
            sb.append("<u>");
            sb.append(allPowers[i]);
            sb.append("</u></th>");

            // determine the maximum number of rows we will have (not including
            // the power name)
            List<String> list = powerMap.get(allPowers[i]);
            if (list.size() > nRows) {
                nRows = list.size();
            }
        }
        sb.append("</tr>");

        // column data (unit locations)
        for (int i = 0; i < nRows; i++) {
            sb.append("<tr>");

            for (int j = 0; j < allPowers.length; j++) {
                // odd columns have bg color
                sb.append ((j & 1) == 0 ? "<td bgcolor=\"F0F8FF\">" : "<td>");

                List<String> list = powerMap.get(allPowers[j]);
                if (i < list.size()) {
                    sb.append(list.get(i));
                }

                sb.append("</td>");
            }

            sb.append("</tr>");
        }


        sb.append("</table>");

        return sb.toString();
    }// getUnitLocationTable()


    /**
     * Write order information. Doesn't show orders for
     * powers that are hidden (not displayable)
     */
    private String getOrders() {
        StringBuilder sb = new StringBuilder(2048);
        Position position = turnState.getPosition();

        for (Power power : allPowers) {
            // print power name
            sb.append("<div class=\"indent1cm\"><b>");
            sb.append(power);
            sb.append("</b></div>");

            // if power is not displayable, mention that.
            boolean canShow = false;
            for (Power displayablePower : displayablePowers) {
                if (power == displayablePower) {
                    canShow = true;
                    break;
                }
            }

            sb.append("<div class=\"indent2cm\">");
            if (canShow) {
                sb.append(getOrderForPower(power));
            } else {
                // (not available), unless eliminated
                if (position.isEliminated(power)) {
                    sb.append(Utils.getLocalString(MSG_POWER_ELIMINATED));
                } else {
                    sb.append(Utils.getLocalString(MSG_UNAVAILABLE));
                }

                sb.append("<br>\n");
            }

            sb.append("</div>");
        }

        return sb.toString();
    }// getOrders()

    private StringBuilder getOrderForPower(Power power) {
        StringBuilder sb = new StringBuilder(2048);
        Position position = turnState.getPosition();

        // print submission/elimination information
        List<Orderable> orders = turnState.getOrders(power);
        if (!orders.isEmpty()) {
            for (Orderable orderable : orders) {
                Order order = (Order) orderable;
                sb.append(order.toFormattedString(ofo));
                sb.append("<br>\n");
            }

            // but do we have orders for all units?
            // indicate if we do not.
            // this is phase dependent
            Adjustment.AdjustmentInfo adjInfo = adjMap.get(power);
            int diff = 0;
            if (turnState.getPhase().getPhaseType() == Phase.PhaseType.RETREAT) {
                diff = adjInfo.getDislodgedUnitCount() - orders.size();
            } else if (turnState.getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT) {
                diff = Math.abs(adjInfo.getAdjustmentAmount()) - orders.size();
            } else if (turnState.getPhase().getPhaseType() == Phase.PhaseType.MOVEMENT) {
                diff = adjInfo.getUnitCount() - orders.size();
            }

            if (diff > 0) {
                sb.append(Utils.getLocalString(ORD_TOO_FEW, diff));
                sb.append("<br>\n");
            }

        } else {
            // if no orders are submitted, we must mention that, unless power
            // has been eliminated....
            if (position.isEliminated(power)) {
                sb.append(Utils.getLocalString(MSG_POWER_ELIMINATED));
            } else {
                sb.append(Utils.getLocalString(MSG_NO_ORDERS_SUBMITTED));
            }

            sb.append("<br>\n");
        }

        return sb;
    }

    /**
     * Write SC ownership information
     */
    private String getSCInfo() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<div class=\"indent1cm\">");

        Position position = turnState.getPosition();

        // we're going to do this the slow, but simple way
        for (Power allPower : allPowers) {
            // create a sorted list of owned supply centers for this power.
            Province[] ownedSCs = position.getOwnedSupplyCenters(allPower);
            Arrays.sort(ownedSCs);

            // print the power name
            sb.append("<b>");
            sb.append(allPower);
            sb.append(":</b> ");

            // print out the provinces
            if (ownedSCs.length > 0) {
                for (Province ownedSC : ownedSCs) {
                    sb.append(ownedSC.getFullName());
                    sb.append(", ");
                }

                // delete the last ", "
                sb.delete(sb.length() - 2, sb.length());

                sb.append("  ");
                sb.append(Utils.getLocalString(SC_NUM, ownedSCs.length));
            } else {
                sb.append(Utils.getLocalString(MSG_NONE));
            }

            sb.append('.');
            sb.append("<br>\n");
        }

        sb.append("</div>");

        return sb.toString();
    }// getSCInfo()


    /**
     * Write dislodged unit information.
     */
    private String getDislodgedInfo() {
        // write dislodged units / powers. if none are
        // dislodged, indicate. Not super-efficient
        // ordered by powers (like SC ownership)
        boolean anyDislodged = false;
        Position position = turnState.getPosition();
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<div class=\"indent1cm\">");

        for (Power allPower : allPowers) {
            Province[] dislodged = position.getDislodgedUnitProvinces(allPower);
            if (dislodged.length > 0) {
                anyDislodged = true;

                // print power name
                sb.append("<b>");
                sb.append(allPower);
                sb.append(":</b> ");


                // print unit information, for each unit.
                // comma-separate.
                for (int z = 0; z < dislodged.length - 1; z++) {
                    Unit unit = position.getDislodgedUnit(dislodged[z]);

                    sb.append(' ');
                    sb.append(unit.getType().getFullName());
                    sb.append(' ');
                    sb.append(dislodged[z].getFullName());
                    sb.append(',');
                }

                // print last (no comma afterwards)
                Unit unit = position.getDislodgedUnit(dislodged[dislodged.length - 1]);
                sb.append(' ');
                sb.append(unit.getType().getFullName());
                sb.append(' ');
                sb.append(dislodged[dislodged.length - 1].getFullName());

                // finish the line.
                sb.append('.');
                sb.append("<br>\n");
            }
        }

        if (!anyDislodged) {
            sb.append(Utils.getLocalString(NO_DISLODGED_UNITS));
        }

        sb.append("</div>");

        return sb.toString();
    }// getDislodgedInfo()


    /**
     * Write adjustment information.
     */
    private String getAdjustmentInfo() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<div class=\"indent1cm\">");

        // format using format string
        // many args...
        for (Power power : allPowers) {
            Adjustment.AdjustmentInfo adjInfo = adjMap.get(power);


            // determine build/remove/nochange text, and blocked builds
            String adjustmentText = null;            // never null after below
            String blockedBuildMessage = "";        // empty if no builds are blocked

            int adjAmount = adjInfo.getAdjustmentAmount();
            if (adjAmount > 0) {
                adjustmentText = Utils.getLocalString(ADJ_BUILD_TEXT, Math.abs(adjAmount));
            } else if (adjAmount < 0) {
                adjustmentText = Utils.getLocalString(ADJ_REMOVE_TEXT, Math.abs(adjAmount));
            } else {
                adjustmentText = Utils.getLocalString(ADJ_NOCHANGE_TEXT);
            }

            // blocked builds?
            int shouldBuild = (adjInfo.getSupplyCenterCount() - adjInfo.getUnitCount());
            if (adjAmount >= 0 && shouldBuild > adjAmount) {
                blockedBuildMessage = Utils.getLocalString(ADJ_BLOCKED_BUILD_TEXT,
                        shouldBuild - adjAmount);
            }

            Object[] args = new Object[]
                    {
                            power,            // {0} : Power
                            adjInfo.getSupplyCenterCount(),    // {1} : # SC (including home SC) controlled
                            adjInfo.getUnitCount(),            // {2} : # units controlled
                            adjustmentText,            // {3} : build or remove (or no change) message
                            blockedBuildMessage,    // {4} : misc text (blocked builds), or empty
                    };

            sb.append(Utils.getLocalString(ADJ_BASIC_TEXT, args));
            sb.append("<br>\n");
        }

        sb.append("</div>");
        return sb.toString();
    }// getAdjustmentInfo()


    /**
     * Returns a Map of Power=>(List of Unit location names)
     * Dislodged units are underlined. Abbreviations
     * for province names are always used.
     */
    private java.util.Map<Power, List<String>> getUnitsByPower() {
        java.util.Map<Power, List<String>> pmap = new HashMap<>();
        for (Power power : allPowers) {
            pmap.put(power, new LinkedList<>());
        }

        Position position = turnState.getPosition();
        Province[] provinces = position.getProvinces();
        for (Province province : provinces) {
            if (position.hasUnit(province)) {
                Unit unit = position.getUnit(province);
                List<String> uList = pmap.get(unit.getPower());
                StringBuilder sb = new StringBuilder(16);
                sb.append(unit.getType().getShortName());
                sb.append(' ');
                sb.append(province.getShortName());
                uList.add(sb.toString());
            }

            if (position.hasDislodgedUnit(province)) {
                // dislodged units are underlined
                Unit unit = position.getDislodgedUnit(province);
                List<String> uList = pmap.get(unit.getPower());
                StringBuilder sb = new StringBuilder(16);
                sb.append("<u>");
                sb.append(unit.getType().getShortName());
                sb.append("</u> <u>");
                sb.append(province.getShortName());
                sb.append("</u>");
                uList.add(sb.toString());
            }

        }

        // sort the lists.
        for (Power power : allPowers) {
            List<String> list = pmap.get(power);
            Collections.sort(list);
        }

        return pmap;
    }// getUnitsByPower()


}// class StateWriter()

