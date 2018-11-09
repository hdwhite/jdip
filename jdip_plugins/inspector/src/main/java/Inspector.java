//
//  @(#)Inspector.java		6/2004
//
//  Copyright 2004 Zachary DelProposto. All rights reserved.
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
import dip.order.OrderFormatOptions;
import dip.order.Orderable;
import dip.order.result.OrderResult;
import dip.order.result.Result;
import dip.tool.Tool;
import dip.tool.ToolProxy;
import dip.world.Phase;
import dip.world.Position;
import dip.world.Province;
import dip.world.TurnState;
import dip.world.Unit;
import dip.world.World;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Tool that aides debugging by looking at various
 * internal settings.
 */
public class Inspector implements Tool {
    final OrderFormatOptions OFO = OrderFormatOptions.createDebug();
    final OrderFormatOptions TERSE_OFO = OrderFormatOptions.createTerse();
    ClientFrame clientFrame = null;

    /**
     * Get the current Tool version
     */
    public float getVersion() {
        return 1.0f;
    }

    /**
     * Get the Tool Copyright Information (authors, etc.). Never should return null.
     */
    public String getCopyrightInfo() {
        return "Copyright 2003 Zach DelProposto";
    }

    /**
     * Get the Tool Web URI (web address, ftp address, etc.). Never should return null.
     */
    public URI getWebURI() {
        try {
            return new URI("http://jdip.sf.net");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the Email addresses. Never should return null.
     */
    public URI[] getEmailURIs() {
        return new URI[0];
    }

    /**
     * Get the Tool comment. Never should return null.
     */
    public String getComment() {
        return "See Instructions for Usage";
    }

    /**
     * Get the Tool Description. Never should return null.
     */
    public String getDescription() {
        return "Inspector helps analyze internal data structures";
    }

    /**
     * Get the Tool name. Never should return null.
     */
    public String getName() {
        return "Inspector";
    }


    // registration methods

    /**
     * Creates a JMenuItem (or JMenu for sub-items)
     */
    public JMenuItem registerJMenuItem() {
        // our menu item is a Menu (thus it will be a submenu)
        JMenu subMenu = new JMenu("Inspector");
        JMenuItem item = null;

        item = new JMenuItem("Make Testcase");
        subMenu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (clientFrame.getWorld() != null) {
                    String s = makeTestCase();
                    displayText(s);
                }
            }
        });

        item = new JMenuItem("Show all orders");
        subMenu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (clientFrame.getWorld() != null) {
                    StringBuffer sb = new StringBuffer(1024);

                    TurnState ts = clientFrame.getTurnState();
                    appendTSInfo(sb, ts);

                    List<Orderable> orders = ts.getAllOrders();
                    for (Orderable o : orders) {
                        sb.append(o.toFormattedString(OFO));

                        sb.append(" [");
                        sb.append(o.getClass().getName());
                        sb.append("]\n");
                    }


                    displayText(sb.toString());
                }
            }
        });


        item = new JMenuItem("Show all results");
        subMenu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (clientFrame.getWorld() != null) {
                    StringBuffer sb = new StringBuffer(1024);

                    TurnState ts = clientFrame.getTurnState();
                    appendTSInfo(sb, ts);
                    List<Result> results = ts.getResultList();
                    for (Result result : results) {
                        sb.append(result);
                        sb.append(" [");
                        sb.append(result.getClass().getName());
                        sb.append("]\n");
                    }


                    displayText(sb.toString());
                }
            }
        });

        item = new JMenuItem("Show positions");
        subMenu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (clientFrame.getWorld() != null) {
                    StringBuffer sb = new StringBuffer(1024);

                    TurnState ts = clientFrame.getTurnState();
                    appendTSInfo(sb, ts);

                    // we print
                    // POWER: UNIT PROVINCE/COAST (DISLODGED)
                    // coasts are always forced
                    final Position pos = ts.getPosition();
                    final Province[] allProvinces = pos.getProvinces();
                    for (int i = 0; i < allProvinces.length; i++) {
                        Unit unit = pos.getUnit(allProvinces[i]);
                        if (unit != null) {
                            sb.append(allProvinces[i].getShortName());
                            sb.append("/");
                            sb.append(unit.getCoast().getAbbreviation());
                            sb.append(": ");
                            sb.append(unit.getType().getShortName());
                            sb.append(" ");
                            sb.append(unit.getPower().getName());
                            sb.append("\n");
                        }

                        unit = pos.getDislodgedUnit(allProvinces[i]);
                        if (unit != null) {
                            sb.append(allProvinces[i].getShortName());
                            sb.append("/");
                            sb.append(unit.getCoast().getAbbreviation());
                            sb.append(": ");
                            sb.append(unit.getType().getShortName());
                            sb.append(" ");
                            sb.append(unit.getPower().getName());
                            sb.append(" *DISLODGED*\n");
                        }
                    }

                    displayText(sb.toString());
                }
            }
        });

        item = new JMenuItem("Show mode and groups");
        subMenu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StringBuilder sb = new StringBuilder(512);
                sb.append("Current Mode: ");
                sb.append(clientFrame.getMode());
                sb.append("\n\n");
                sb.append("Orderable Powers:\n");
                sb.append(Arrays.asList(clientFrame.getOrderablePowers()));
                sb.append("\n\n");
                sb.append("Displayable Powers:\n");
                sb.append(Arrays.asList(clientFrame.getDisplayablePowers()));
                sb.append("\n\n");

                displayText(sb.toString());
            }
        });

        item = new JMenuItem("Print listeners (stdout)");
        subMenu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clientFrame.dbgPrintListeners();
            }
        });
		
		/*
		item = new JMenuItem("Misc....");
		subMenu.add(item);
		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(clientFrame.getWorld() != null)
				{
					// do stuff here
				}
			}
		});
		*/

        return subMenu;
    }// registerJMenuItem()

    /**
     * Appends TurnState info
     */
    private void appendTSInfo(StringBuffer sb, TurnState ts) {
        sb.append("Phase: ");
        sb.append(ts.getPhase());
        sb.append("\n  resolved?: ");
        sb.append(ts.isResolved());
        sb.append("\n  game over?: ");
        sb.append(ts.isEnded());
        sb.append("\n  SC owner change?: ");
        sb.append(ts.getSCOwnerChanged());
        sb.append("\n\n");
    }


    /**
     * Gets the ToolProxy object which allows a Tool access to internal data structures
     */
    public void setToolProxy(ToolProxy toolProxy) {
        clientFrame = toolProxy.getClient();
    }// setToolProxy()


    /**
     * Displays the given text
     */
    private void displayText(String text) {
        TextViewer tv = new TextViewer(clientFrame, false);
        tv.addSingleButton(tv.makeOKButton());
        tv.setEditable(false);
        tv.setTitle("Info");
        tv.setHeaderVisible(false);
        tv.setContentType("text/plain");
        tv.setFont(new Font("mono", Font.PLAIN, 12));
        tv.setText(text);
        tv.displayDialog();
    }// displayText()

    /**
     * Create a TestCase from the current turn
     */
    private String makeTestCase() {
        final World w = clientFrame.getWorld();
        final TurnState ts = clientFrame.getTurnState();
        final Position pos = ts.getPosition();
        final Province[] allProvinces = pos.getProvinces();

        StringBuilder sb = new StringBuilder(1024);
        sb.append("VARIANT_ALL ");
        sb.append(w.getVariantInfo().getVariantName());
        sb.append("\n");

        sb.append("CASE generated testcase:");
        sb.append(new Date());
        sb.append("\n");

        sb.append("PRESTATE_SETPHASE ");
        sb.append(ts.getPhase());
        sb.append("\n");

        sb.append("PRESTATE\n");
        for (int i = 0; i < allProvinces.length; i++) {
            Unit unit = pos.getUnit(allProvinces[i]);
            if (unit != null) {
                sb.append("\t");
                sb.append(unit.getPower().getName());
                sb.append(": ");
                sb.append(unit.getType().getShortName());
                sb.append(" ");
                sb.append(allProvinces[i].getShortName());
                if (unit.getType() == Unit.Type.FLEET
                        && allProvinces[i].isMultiCoastal()) {
                    sb.append("/");
                    sb.append(unit.getCoast().getAbbreviation());
                }
                sb.append("\n");
            }
        }

        sb.append("PRESTATE_DISLODGED\n");
        for (int i = 0; i < allProvinces.length; i++) {
            Unit unit = pos.getDislodgedUnit(allProvinces[i]);
            if (unit != null) {
                sb.append("\t");
                sb.append(unit.getPower().getName());
                sb.append(": ");
                sb.append(unit.getType().getShortName());
                sb.append(" ");
                sb.append(allProvinces[i].getShortName());
                if (unit.getType() == Unit.Type.FLEET
                        && allProvinces[i].isMultiCoastal()) {
                    sb.append("/");
                    sb.append(unit.getCoast().getAbbreviation());
                }
                sb.append("\n");
            }
        }

        // PRESTATE_RESULTS phase
        if (ts.getPhase().getPhaseType() == Phase.PhaseType.RETREAT) {
            sb.append("PRESTATE_RESULTS\n");

            final TurnState prevTS = w.getPreviousTurnState(ts);    // null if not available
            if (prevTS != null) {
                // inefficient but simple
                // we assume all orders fail UNLESS a success result for that order
                // exists. NOTE THAT ALL THESE RESULTS are from the PRIOR turnstate.
                //
                List<Result> results = prevTS.getResultList();

                List<Orderable> orders = prevTS.getAllOrders();
                for (Orderable o : orders) {
                    sb.append("\t");

                    boolean isSuccess = false;
                    for (Result result : results) {
                        if (result instanceof OrderResult) {
                            OrderResult or = (OrderResult) result;
                            if (or.getOrder() == o
                                    && or.getResultType() == OrderResult.ResultType.SUCCESS) {
                                isSuccess = true;
                            }
                        }
                    }

                    if (isSuccess) {
                        sb.append("SUCCESS: ");
                    } else {
                        sb.append("FAILURE: ");
                    }

                    sb.append(o.toFormattedString(TERSE_OFO));
                    sb.append("\n");
                }
            } else {
                sb.append("\t***\n");
                sb.append("\t*** Could not generate PRESTATE_RESULTS; there is\n");
                sb.append("\t*** no prior turn from which to get MOVE results.\n");
                sb.append("\t***\n");
            }
        }

        sb.append("ORDERS\n");
        List<Orderable> orders = ts.getAllOrders();
        for (Orderable o : orders) {
            sb.append("\t");
            sb.append(o.toFormattedString(TERSE_OFO));
            sb.append("\n");
        }

        // post-state, and poststate-dislodged are from the NEXT turn;
        // that's why we must be resolved.
        if (ts.isResolved()) {
            final TurnState nextTS = w.getNextTurnState(ts);    // null if not yet resolved.
            final Position nextPos = nextTS.getPosition();

            sb.append("POSTSTATE\n");
            for (int i = 0; i < allProvinces.length; i++) {
                Unit unit = nextPos.getUnit(allProvinces[i]);
                if (unit != null) {
                    sb.append("\t");
                    sb.append(unit.getPower().getName());
                    sb.append(": ");
                    sb.append(unit.getType().getShortName());
                    sb.append(" ");
                    sb.append(allProvinces[i].getShortName());
                    if (unit.getType() == Unit.Type.FLEET
                            && allProvinces[i].isMultiCoastal()) {
                        sb.append("/");
                        sb.append(unit.getCoast().getAbbreviation());
                    }
                    sb.append("\n");
                }
            }

            sb.append("POSTSTATE_DISLODGED\n");
            for (int i = 0; i < allProvinces.length; i++) {
                Unit unit = nextPos.getDislodgedUnit(allProvinces[i]);
                if (unit != null) {
                    sb.append("\t");
                    sb.append(unit.getPower().getName());
                    sb.append(": ");
                    sb.append(unit.getType().getShortName());
                    sb.append(" ");
                    sb.append(allProvinces[i].getShortName());
                    if (unit.getType() == Unit.Type.FLEET
                            && allProvinces[i].isMultiCoastal()) {
                        sb.append("/");
                        sb.append(unit.getCoast().getAbbreviation());
                    }
                    sb.append("\n");
                }
            }
        } else {
            sb.append("\t***\n");
            sb.append("\t*** POSTSTATE and POSTSTATE_DISLODGED cannot be determined\n");
            sb.append("\t*** until the turn has been resolved!\n");
            sb.append("\t***\n");
        }


        sb.append("END\n");
        return sb.toString();
    }// makeTestCase()
	
	
/*
VARIANT_ALL Youngstown
CASE Retreat.Test
PRESTATE_SETPHASE Spring 1909, Retreat
PRESTATE
	Austria: A ser
	Italy: A ven
	Italy: A tri
	Italy: F gre
	Italy: F aeg
PRESTATE_DISLODGED
	Austria: F tri
	Turkey: F gre
PRESTATE_RESULTS	# only in retreat phase
	FAILURE: Austria: F tri H
	SUCCESS: Austria: A ser H
	FAILURE: Turkey: F gre H
	SUCCESS: Italy: A ven S A tyr-tri
	SUCCESS: Italy: A tyr-tri
	SUCCESS: Italy: F ion-gre
	SUCCESS: Italy: F aeg S F ion-gre
ORDERS
	Austria: F tri-alb		# retreat
	Austria: A ser S F tri-alb	# this is illegal
	Turkey: F gre-alb		# retreat
POSTSTATE
	Austria: A ser
	Italy: A ven
	Italy: A tri
	Italy: F gre
	Italy: F aeg
POSTSTATE_DISLODGED
	France: F eng
	Russia: F nth
END	
*/

}// class Inspector
