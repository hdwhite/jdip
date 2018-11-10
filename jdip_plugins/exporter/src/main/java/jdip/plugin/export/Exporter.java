//
//  @(#)Exporter.java		9/2003
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
package jdip.plugin.export;

import dip.gui.ClientFrame;
import dip.gui.dialog.ErrorDialog;
import dip.gui.swing.XJFileChooser;
import dip.tool.Tool;
import dip.tool.ToolProxy;
import dip.world.TurnState;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.List;


public class Exporter implements Tool {
    ClientFrame clientFrame = null;
    JMenuItem exportCurrent = null;
    JMenuItem exportAll = null;

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
        return "Exporter helps export turn data.";
    }

    /**
     * Get the Tool name. Never should return null.
     */
    public String getName() {
        return "Exporter";
    }


    /**
     * Creates a JMenuItem (or JMenu for sub-items)
     */
    public JMenuItem registerJMenuItem() {
        // our menu item is a Menu (thus it will be a submenu)
        JMenu subMenu = new JMenu("Export");

        exportCurrent = new JMenuItem("Current Turn");
        exportCurrent.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                XJFileChooser xjfc = XJFileChooser.getXJFileChooser();
                File file = xjfc.displaySaveAs(clientFrame);
                XJFileChooser.dispose();
                if (file != null) {
                    TurnState ts = clientFrame.getTurnState();
                    exportToFile(file, new TurnState[]{ts});
                }
            }
        });
        subMenu.add(exportCurrent);

        exportAll = new JMenuItem("All Turns");
        exportAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                XJFileChooser xjfc = XJFileChooser.getXJFileChooser();
                File file = xjfc.displaySaveAs(clientFrame);
                XJFileChooser.dispose();
                if (file != null) {
                    List<TurnState> tsList = clientFrame.getWorld().getAllTurnStates();
                    TurnState[] ts = tsList.toArray(new TurnState[0]);
                    exportToFile(file, ts);
                }
            }
        });
        subMenu.add(exportAll);

        exportAll.setEnabled(false);
        exportCurrent.setEnabled(false);

        return subMenu;
    }// registerJMenuItem()


    /**
     * Gets the ToolProxy object which allows a Tool access to internal data structures
     */
    public void setToolProxy(ToolProxy toolProxy) {
        clientFrame = toolProxy.getClient();
        registerListener();
    }// setToolProxy()


    /**
     * Register a listener to see when a World object exists
     */
    private void registerListener() {
        clientFrame.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final String evtName = evt.getPropertyName();

                if (evtName.equals(ClientFrame.EVT_WORLD_CREATED)) {
                    // activate menu items
                    exportAll.setEnabled(true);
                    exportCurrent.setEnabled(true);
                } else if (evtName.equals(ClientFrame.EVT_WORLD_DESTROYED)) {
                    // deactivate menu items
                    exportAll.setEnabled(false);
                    exportCurrent.setEnabled(false);
                }
            }// propertyChange()
        });
    }// registerListener()


    /**
     * xport as a file
     */
    private void exportToFile(File file, TurnState[] turnStates) {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {

            for (TurnState turnState : turnStates) {
                bw.write(XMLExport.export(turnState, ""));
            }
        } catch (IOException e) {
            ErrorDialog.displayFileIO(clientFrame, e, file.toString());
        }
    }// exportToFile()


}// interface Tool
