//
//  @(#)AssocJComboBox.java		9/2004
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
package dip.gui.swing;

import dip.misc.Utils;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import javax.swing.Icon;

import javax.swing.KeyStroke;
import javax.swing.JMenuItem;

import org.jdesktop.swing.actions.BoundAction;
import org.jdesktop.swing.actions.AbstractActionExt;
import org.jdesktop.swing.actions.ActionFactory;
import org.jdesktop.swing.actions.ActionManager;

public class ActionMaker
{
	/* constants */
	private static final String BLANK_ICON = "resource/common/icons/16x16/blank.gif";
	private static final int MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	
	/* suffixes */
	private static final String SMALL_ICON 	= "_icon";
	private static final String LARGE_ICON 	= "_largeicon";
	private static final String TOOLTIP 	= "_tooltip";
	private static final String ACCELERATOR	= "_accelerator";
	private static final String MNEMONIC 	= "_mnemonic";
	
	
	private static final Icon blankIcon = Utils.getIcon(BLANK_ICON);
	
	/**
	*	Creates an action, with the given command name (cmdName) from the
	*	given propertyName. Note that groupName may be null. If toggle is true,
	*	a radiobutton-type (mutually exclusive, within the group) action will 
	*	be created if a group is specified; if no group is specified, a regular
	*	toggle button (similar to JToggleButton) will be created.
	*	<p>
	*	All actions are automatically added to the ActionManager.
	*/
	public static BoundAction create(final String cmdName, 
		final String propertyName, final String groupName, final boolean toggle)
	{
		if(cmdName == null || propertyName == null)
		{
			throw new IllegalArgumentException();
		}
		
		final BoundAction action = new BoundAction(
			Utils.getLocalString(propertyName), cmdName);
			
		String s = Utils.getLocalStringNoEx(propertyName + MNEMONIC);
		if(s != null)
		{
			action.setMnemonic(KeyStroke.getKeyStroke(s).getKeyCode());
		}
		
		s = Utils.getLocalStringNoEx(propertyName + ACCELERATOR);
		if(s != null)
		{
			KeyStroke accelKey = KeyStroke.getKeyStroke(s);
			if(Utils.isOSX())
			{
				// substitute COMMAND for CTRL modifier.
				if((accelKey.getModifiers() & InputEvent.CTRL_MASK) > 0)
				{
					accelKey = KeyStroke.getKeyStroke(accelKey.getKeyCode(), MASK);
				}
			}
			
			action.setAccelerator(accelKey);
		}
		
		s = Utils.getLocalStringNoEx(propertyName + SMALL_ICON);
		if(s != null)
		{
			action.setSmallIcon(Utils.getIcon(s));
		}
		
		s = Utils.getLocalStringNoEx(propertyName + LARGE_ICON);
		if(s != null)
		{
			action.setLargeIcon(Utils.getIcon(s));
		}
		
		s = Utils.getLocalStringNoEx(propertyName + TOOLTIP);
		action.setShortDescription(s);
		
		action.setStateAction(toggle);
		
		if(groupName != null)
		{
			action.setGroup(groupName);
		}
		
		ActionManager manager = ActionManager.getInstance();
        return (BoundAction) manager.addAction(action);
	}// create()
	
	
	
	/** Make a menu item from an AbstractActionExt object, indenting if required */
	public static JMenuItem makeMenuItem(AbstractActionExt action, boolean indent)
	{
		JMenuItem menuItem = ActionManager.getInstance().getFactory().createMenuItem(action);
		
		if(indent && (action.getSmallIcon() == null))
		{
			menuItem.setIcon(blankIcon);
		}
		
		return menuItem;
	}// makeMenuItem()
	
	/** Make a menu item from an AbstractActionExt object */
	public static JMenuItem makeMenuItem(AbstractActionExt action)
	{
		return makeMenuItem(action, false);
	}// makeMenuItem()
	
	
	
}// class ActionMaker


