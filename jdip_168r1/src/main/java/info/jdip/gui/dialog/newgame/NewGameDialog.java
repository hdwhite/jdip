//
//  @(#)NewGameDialog.java		4/2002
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
package info.jdip.gui.dialog.newgame;

import info.jdip.gui.ClientFrame;
import info.jdip.gui.dialog.HeaderDialog;
import info.jdip.gui.swing.SwingWorker;
import info.jdip.misc.Help;
import info.jdip.misc.Utils;
import info.jdip.world.World;
import info.jdip.world.variant.data.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The New game dialog, for starting new games.
 * <p>
 * A cached copy is kept, and is initially created at startup.
 */
public class NewGameDialog extends HeaderDialog {
    private static final Logger logger = LoggerFactory.getLogger(NewGameDialog.class);
    // i18n constants
    public static final String TITLE_F2F = "NGD.title.f2f";
    private static final String TITLE = "NGD.title";
    private static final String HEADER_LOCATION = "NGD.location.header";
    private static JButton defaultButton;
    // class variables
    private static NewGameDialog dialogInstance = null;
    private static SwingWorker loader = null;
    // instance variables
    private World world = null;
    private final ClientFrame clientFrame;
    private final JTabbedPane tabPane;
    private final NGDStartOptions tabOptions;
    private final NGDVariantSelect tabVariant;
    private final NGDRuleOptions tabRuleOpts;
    private final NGDMapAndUnits tabMapAndUnits;


    /**
     * Create a NewGameDialog
     */
    private NewGameDialog(ClientFrame parent) {
        super(parent, Utils.getLocalString(TITLE), true);
        this.clientFrame = parent;

        // create tab pane
        tabPane = new JTabbedPane(SwingConstants.TOP);

        // create tabbed panels
        tabMapAndUnits = new NGDMapAndUnits();
        tabOptions = new NGDStartOptions();
        tabRuleOpts = new NGDRuleOptions();
        tabVariant = new NGDVariantSelect(clientFrame, this);

        // tab setup
        addTab(tabVariant);
        addTab(tabMapAndUnits);
        addTab(tabOptions);
        addTab(tabRuleOpts);

        setTabsEnabled(false);

        // get initial variant selection
        setTabsVariant(tabVariant.getDefaultVariant());
        setTabsEnabled(true);

        // dialog setup
        setHeaderText(Utils.getText(Utils.getLocalString(HEADER_LOCATION)));
        addTwoButtons(makeCancelButton(), makeOKButton(), false, true);
        createDefaultContentBorder(tabPane);
        setContentPane(tabPane);

        // ensure list selection is visible
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                tabVariant.ensureSelectionIsVisible();
            }
        });
    }// NewGameDialog()

    /**
     * Displays a (usually cached) NewGameDialog.
     * Returns a valid World or <code>null</code>
     * depending upon selections.
     */
    public static World displayDialog(ClientFrame parent) {
        return displayDialog(parent, Utils.getLocalString(TITLE), info.jdip.misc.Help.HelpID.NewGame);
    }// displayDialog()

    /**
     * Displays a (usually cached) NewGameDialog.
     * Returns a valid World or <code>null</code>
     * depending upon selections.
     * <p>
     * This method allows the title to be altered. A
     * <code>null</code> title is not allowed. A null
     * helpID will use the default NGD help.
     */
    public static World displayDialog(ClientFrame parent, String title, Help.HelpID helpID) {
        if (title == null) {
            throw new IllegalArgumentException();
        }
        createCachedDialog(parent);

        dialogInstance.setTitle(title);
        dialogInstance.setHelpID(helpID == null ? info.jdip.misc.Help.HelpID.NewGame : helpID);

        Utils.centerIn(dialogInstance, parent);
        dialogInstance.tabPane.setSelectedIndex(0);    // always reset to first tab
        dialogInstance.getRootPane().setDefaultButton(defaultButton); // ok button should be default button
        dialogInstance.tabPane.requestFocusInWindow();
        dialogInstance.setVisible(true);
        return dialogInstance.getWorld();
    }// displayDialog()

    /**
     * Create a dialog, and place in cache
     */
    public static synchronized void createCachedDialog(final ClientFrame parent) {
        if (dialogInstance == null) {
            if (loader == null) {
                loader = new SwingWorker() {
                    public Object construct() {
                        logger.trace("NGD construct() started.");
                        NewGameDialog ngd = new NewGameDialog(parent);
                        ngd.pack();
                        ngd.setSize(Utils.getScreenSize(0.67f, 0.82f));
                        logger.trace("NGD construct() complete.");
                        return ngd;
                    }// construct()
                };

                loader.start(Thread.MIN_PRIORITY);
            } else {
                logger.trace( "NGD waiting...");
                dialogInstance = (NewGameDialog) loader.get();
                loader = null;
            }
        }
    }// createCachedDialog()

    /**
     * Handle dialog closing
     */
    @Override
    public void close(String actionCommand) {
        if (isOKorAccept(actionCommand)) {
            doOK();
        } else {
            world = null;
        }

        super.close(actionCommand);
    }// close()


    /**
     * Handle the OK button
     */
    private void doOK() {
        world = tabVariant.getWorld();
        dispose();
    }// doOK()

    /**
     * Get the world object (or null)
     */
    private World getWorld() {
        return world;
    }// getWorld()

    protected NGDStartOptions getStartOptionsPanel()
    {
        return tabOptions;
    }// getStartOptionsPanel()

    protected NGDRuleOptions getRuleOptionsPanel() {
        return tabRuleOpts;
    }// getStartOptionsPanel()

    protected NGDMapAndUnits getMAUPanel() {
        return tabMapAndUnits;
    }// getMAUPanel()

    /**
     * Adds a tab
     */
    synchronized void addTab(NGDTabPane tab) {
        if (!(tab instanceof Component)) {
            throw new IllegalArgumentException();
        }
        tabPane.add(tab.getTabName(), (Component) tab);
    }// addTab()


    /**
     * Set the variant for all tabs.
     */
    synchronized void setTabsVariant(Variant variant) {
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            ((NGDTabPane) tabPane.getComponentAt(i)).variantChanged(variant);
        }
    }// setTabsVariant()


    /**
     * set the enabled status for all tabs
     */
    synchronized void setTabsEnabled(boolean value) {
        for (int i = 0; i < tabPane.getTabCount(); i++) {
            NGDTabPane tp = (NGDTabPane) tabPane.getComponentAt(i);
            tp.enablingChanged(value);
        }
    }// setTabsEnabled()

    /**
     * All tabs must implement this interface. This helps
     * control and standardizes common tab functions.
     * <p>
     * An object that implements NGDTabPane <b>must</b>
     * be a suclass of java.awt.Component
     */
    interface NGDTabPane {
        /**
         * Get the name of the tab.
         */
        String getTabName();

        /**
         * Called when the variant has changed. Variant may be null.
         */
        void variantChanged(Variant variant);

        /**
         * Sets the enabling/disabling of the tab
         */
        void enablingChanged(boolean enabled);

    }// interface NGDTabPane
}// class NewGameDialog