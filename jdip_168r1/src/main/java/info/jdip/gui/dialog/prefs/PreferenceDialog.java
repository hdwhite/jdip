//
//  @(#)PreferenceDialog.java	1.00	4/1/2002
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

package info.jdip.gui.dialog.prefs;

import info.jdip.gui.ClientFrame;
import info.jdip.gui.dialog.HeaderDialog;
import info.jdip.misc.Utils;

import javax.swing.*;
import java.awt.*;


/**
 * Creates a Preferences Dialog.
 * <p>
 * Loads PreferencePanels, displays on a page.
 * <p>
 * In the future, these will be loadable at run-time.
 */
public class PreferenceDialog extends HeaderDialog {
    // constants
    private static final String TITLE = "PreferenceDialog.title";
    private static final String HEADER_LOCATION = "PreferenceDialog.header";

    // GUI
    private final ClientFrame parentFrame;
    private final JTabbedPane tabPane;
    private PreferencePanel[] tabbedPanels;        // make static, & create just once?


    private PreferenceDialog(ClientFrame parent) {
        super(parent, Utils.getLocalString(TITLE), true);
        this.parentFrame = parent;

        tabPane = new JTabbedPane(SwingConstants.TOP);

        setHeaderText(Utils.getText(Utils.getLocalString(HEADER_LOCATION)));
        setContentPane(tabPane);
        createDefaultContentBorder(tabPane);
        addTwoButtons(makeCancelButton(), makeOKButton(), false, true);
        setHelpID(info.jdip.misc.Help.HelpID.Preferences);
    }// AboutDialog()

    /**
     * Show the Preferences dialog.
     */
    public static void displayDialog(ClientFrame parent) {
        PreferenceDialog pd = new PreferenceDialog(parent);
        pd.createPanels();
        pd.pack();
        pd.setSize(new Dimension(450, 550));
        Utils.centerInScreen(pd);
        pd.setVisible(true);
    }// displayDialog()

    private void createPanels() {
        tabbedPanels = new PreferencePanel[] {
            new GeneralPreferencePanel(parentFrame),
            new DisplayPreferencePanel(parentFrame),
            new ExportPreferencePanel(parentFrame)
        };

        for (PreferencePanel panel : tabbedPanels) {
            tabPane.addTab(panel.getName(), panel);
        }
    }// createPanels()


    /**
     * Apply or Cancel settings after closing dialog
     */
    @Override
    public void close(String actionCommand) {
        super.close(actionCommand);

        if (isCloseOrCancel(actionCommand)) {
            for (PreferencePanel panel : tabbedPanels) {
                panel.cancel();
            }
        } else {
            for (PreferencePanel panel : tabbedPanels) {
                panel.apply();
            }
        }
    }// close()


}// class PreferenceDialog
