//
//  @(#)StatusBar.java	1.00	4/1/2002
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
package info.jdip.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * StatusBar
 */
public class StatusBar extends JPanel {
    private static final String INITIAL_MESSAGE = "";
    private final JTextField text;
    private final JTextField mode;
    private JProgressBar pb = null;


    /**
     * Creates the StatusBar
     */
    public StatusBar() {
        super(new BorderLayout());

        add(new javax.swing.JSeparator(), BorderLayout.NORTH);

        text = new JTextField(INITIAL_MESSAGE) {
            @Override
            public boolean isFocusable() {
                return false;
            }
        };

        text.setEditable(false);
        text.setBorder(new EmptyBorder(3, 3, 3, 3));
        text.setHighlighter(null);
        text.setOpaque(false);
        add(text, BorderLayout.CENTER);

        mode = new JTextField("", 8) {
            @Override
            public boolean isFocusable() {
                return false;
            }
        };
        mode.setHorizontalAlignment(SwingConstants.RIGHT);
        mode.setEditable(false);
        mode.setHighlighter(null);
        mode.setOpaque(false);
        mode.setBorder(new EmptyBorder(3, 3, 3, 3));
        mode.setMinimumSize(new Dimension(45, (int) text.getPreferredSize().getHeight()));
        mode.setMaximumSize(mode.getMinimumSize());
        add(mode, BorderLayout.EAST);
    }// StatusBar()


    /**
     * Clear the text
     */
    public void clearText() {
        setText("");
    }// clear()


    /**
     * Set the text
     */
    public void setText(final String s) {
        text.setText(s);
    }// setText()

    /**
     * Clear Mode indicator text
     */
    public void clearModeText() {
        mode.setText("");
    }// clearModeText();

    /**
     * Get the Mode indicator text
     */
    public String getModeText() {
        return mode.getText();
    }// getModeText()

    /**
     * Set Mode indicator text
     */
    public void setModeText(String s) {
        mode.setText(s);
    }// setModeText()

    /**
     * Shows the Progress Bar (in place of the Mode setting)
     */
    public void showPB(int min, int max) {
        if (pb == null) {
            pb = new JProgressBar(min, max);
            pb.setStringPainted(false);
            pb.setIndeterminate(false);
            pb.setBorderPainted(true);
        }

        pb.setValue(min);

        remove(mode);
        add(pb, BorderLayout.EAST);
        validate();
    }// showPB()

    /**
     * Sets the progress bar displayed value.
     */
    public void setPBValue(int value) {
        if (pb != null) {
            pb.setValue(value);
        }
    }// setPB()


    /**
     * Increment the progress bar's displayed value by 1
     */
    public void incPBValue() {
        if (pb != null) {
            pb.setValue(pb.getValue() + 1);
        }
    }// incPBValue()

    /**
     * Hides the Progress Bar (and puts the Mode setting back)
     */
    public void hidePB() {
        if (pb != null) {
            remove(pb);
            add(mode, BorderLayout.EAST);
            validate();
            mode.repaint();
            pb = null;
        }
    }// hidePB()

}// class StatusBar
