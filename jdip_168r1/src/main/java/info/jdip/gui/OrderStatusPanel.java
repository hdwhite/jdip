//
//  @(#)OrderStatusPanel.java		5/2003
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
package info.jdip.gui;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import info.jdip.gui.swing.XJPanel;
import info.jdip.misc.Utils;
import info.jdip.order.Orderable;
import info.jdip.world.Phase;
import info.jdip.world.TurnState;
import info.jdip.world.World;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * OrderStatusPanel: contains a label that displays the current phase,
 * if a game is loaded. Also displays (when appropriate) a text field
 * where the user may enter orders in text format.
 */
public class OrderStatusPanel extends XJPanel {
    // i18n constnats
    private static final String LABEL_ORDER = "OP.label.order";
    private static final String EMPTY = "";

    // instance variables
    private final JLabel orderFieldLabel;
    private final JLabel phase;
    private final JTextField orderField;
    private OSPPropertyListener propListener = null;
    private ClientFrame cf = null;


    /**
     * Creates an OrderStatusPanel object.
     */
    public OrderStatusPanel(ClientFrame clientFrame) {
        this.cf = clientFrame;

        // setup labels
        phase = new JLabel(EMPTY);
        orderFieldLabel = new JLabel(Utils.getLocalString(LABEL_ORDER));


        // setup text field
        orderField = new info.jdip.gui.swing.XJTextField();
        orderField.addActionListener((ActionEvent e) -> {
            String text = orderField.getText();

            if (text.equals(EMPTY)) {
                return;
            }

            // add an order; if no error occured, clear
            // the textfield
            if (cf.getOrderDisplayPanel() != null &&
                cf.getOrderDisplayPanel().addOrder(text, true)) {
                orderField.setText(EMPTY);
                orderField.repaint();
            }
        });

        // setup propety listener
        propListener = new OSPPropertyListener();
        cf.addPropertyChangeListener(propListener);

        // do layout
        makeLayout();
    }// OrderStatusPanel()


    /**
     * Performs any cleanup.
     */
    public void close() {
        cf.removePropertyChangeListener(propListener);
    }// close()


    /**
     * Sets the text in the order text field.
     * <p>
     * Note that this does not parse the text;
     * however this text is "live", and the user may edit it.
     */
    public void setOrderText(String value) {
        orderField.setText(value);
    }// setOrderText()


    /**
     * Clears the order TextField of any text
     */
    public void clearOrderText() {
        orderField.setText(EMPTY);
    }// clearOrderText()

    /**
     * Layout components
     */
    private void makeLayout() {
        // start layout
        int[] w1 = {0, 5, 0};
        int[] h1 = {5, 0, 25, 0, 10};

        HIGLayout hl = new HIGLayout(w1, h1);
        hl.setColumnWeight(3, 1);
        hl.setRowWeight(2, 1);
        setLayout(hl);

        HIGConstraints c = new HIGConstraints();

        add(phase, c.rcwh(2, 1, 3, 1, "lr"));
        add(orderFieldLabel, c.rc(4, 1, "l"));
        add(orderField, c.rc(4, 3, "lr"));
    }// makeLayout()

    /**
     * Property change event listener
     */
    private class OSPPropertyListener extends AbstractCFPListener {
        @Override
        public void actionOrderCreated(Orderable order) {
            clearOrderText();
        }

        @Override
        public void actionOrderDeleted(Orderable order) {
            clearOrderText();
        }

        @Override
        public void actionOrdersCreated(Orderable[] orders) {
            clearOrderText();
        }

        @Override
        public void actionOrdersDeleted(Orderable[] orders) {
            clearOrderText();
        }

        @Override
        public void actionModeChanged(String mode) {
            orderField.setVisible(ClientFrame.MODE_ORDER.equals(mode));
            orderFieldLabel.setVisible(ClientFrame.MODE_ORDER.equals(mode));
        }// actionModeChanged()

        @Override
        public void actionTurnstateChanged(TurnState turnState) {
            Phase tsPhase = turnState.getPhase();

            // set game time
            StringBuilder sb = new StringBuilder(32);
            sb.append("<html><h2>");
            sb.append(tsPhase.toString());
            sb.append("</h2></html>");
            phase.setText(sb.toString());
        }// actionTurnstateChanged()


        @Override
        public void actionWorldCreated(World w) {
            phase.setText(EMPTY);
        }

        @Override
        public void actionWorldDestroyed(World w) {
            phase.setText(EMPTY);
        }
    }// inner class OSPPropertyListener

}// class OrderStatusPanel
