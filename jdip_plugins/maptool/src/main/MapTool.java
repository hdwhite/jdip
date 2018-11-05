//
//  @(#)MapTool.java		6/2003
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
package jdip.plugin.maptool;

import dip.tool.*;
import dip.gui.*;
import dip.gui.map.*;
import dip.misc.*;

import javax.swing.*;
import java.awt.event.*;
import java.net.URI;

public class MapTool implements Tool
{
	ClientFrame clientFrame = null;
	MTCBar		mapBar = null;
	private 	boolean isActivated = false;
	private		JMenuItem menuResults;
	private		JMenuItem menuToolbar;
	private 	JMenuItem menuCheck;
	
	/** Get the current Tool version */
	public float getVersion()			{ return 1.0f; }
	/** Get the Tool Copyright Information (authors, etc.). Never should return null. */
	public String getCopyrightInfo()	{ return "Copyright 2003 Zach DelProposto"; }
	/** Get the Tool Web URI (web address, ftp address, etc.). Never should return null. */
	public URI	getWebURI()			{ try { return new URI("http://jdip.sf.net"); } catch(Exception e) { return null; } }
	/** Get the Email addresses. Never should return null. */
	public URI[] getEmailURIs()		{ return new URI[0]; }
	/** Get the Tool comment. Never should return null. */
	public String getComment()		{ return "See Instructions for Usage"; }
	/** Get the Tool Description. Never should return null. */
	public String getDescription()	{ return "MapTool helps create maps"; }
	/** Get the Tool name. Never should return null. */
	public String getName()			{ return "MapTool"; }
	
	
	// registration methods
	/** Creates a JMenuItem (or JMenu for sub-items) */
	public JMenuItem registerJMenuItem()
	{
		// our menu item is a Menu (thus it will be a submenu)
		JMenu subMenu = new JMenu("MapTool");
		
		menuResults = new JMenuItem("Results");
		menuResults.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(checkAction())
				{
					MTOutput.showOutput(MapTool.this.clientFrame, mapBar.getMTHelper(), mapBar.getMTLabeler());
				}
			}
		});
		
		
		menuCheck = new JMenuItem("Check Map");
		menuCheck.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(checkAction())
				{
					MTChecker.checkMap(MapTool.this.clientFrame, mapBar.getMTHelper(), mapBar.getMTLabeler());
				}
			}
		});
		
		menuToolbar = new JMenuItem("Set Toolbar");
		menuToolbar.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(MapTool.this.clientFrame.getMapPanel() == null)
				{
					Utils.popupError(MapTool.this.clientFrame, "Error", "Must have a map loaded.");
				}
				else
				{
					MapPanel mp = MapTool.this.clientFrame.getMapPanel();
					if(mp.getControlBar() instanceof MTCBar)
					{
						Utils.popupError(MapTool.this.clientFrame, "Error", "Map Toolbar already set!");
					}
					else
					{
						mapBar = new MTCBar(mp);
						mp.setControlBar(mapBar);
					}
				}
			}
		});
	
		JCheckBoxMenuItem item = new JCheckBoxMenuItem("Suppress errors", false);
		item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// set MMD error flag in clientframe
				MapTool.this.isActivated = !MapTool.this.isActivated;
				clientFrame.setMMDSuppressed(MapTool.this.isActivated);
			}
		});
		
		
		subMenu.add(menuCheck);
		subMenu.add(menuResults);
		subMenu.add(item);
		subMenu.add(new JSeparator());
		subMenu.add(menuToolbar);
		
		return subMenu;
	}// registerJMenuItem()
					
	/** Gets the ToolProxy object which allows a Tool access to internal data structures */
	public void setToolProxy(ToolProxy toolProxy)
	{
		clientFrame = toolProxy.getClient();
	}// setToolProxy()
	
	/** Check to see if we can do the action */
	private boolean checkAction()
	{
		if(MapTool.this.clientFrame.getWorld() == null)
		{
			Utils.popupError(MapTool.this.clientFrame, "Error", "Must have a map loaded.");
			return false;
		}
		else if(mapBar == null)
		{
			Utils.popupError(MapTool.this.clientFrame, "Error", "Must do \"Set Toolbar\" first.");
			return false;
		}
		
		return true;
	}// checkAction()
	
	
}// interface Tool
