//
//  @(#)NextPreviousTextViewer.java		9/2004
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
package dip.gui.dialog;

import dip.gui.ClientMenu;
import dip.gui.swing.XJTextPane;
import dip.gui.swing.XJEditorPane;
import dip.gui.swing.XJScrollPane;
import dip.gui.dialog.ErrorDialog;
import dip.gui.dialog.prefs.GeneralPreferencePanel;
import dip.misc.Utils;
import dip.misc.SimpleFileFilter;
import dip.gui.swing.XJFileChooser;
import dip.misc.Log;

import java.awt.Font;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;
import javax.swing.text.html.*;
import java.util.regex.*;

import java.io.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;

/**
*	Displays Text inside a HeaderDialog.
*	<p>
*	This adds current / next / previous buttons to the
*	CommandBar of the dialog, allowing the user to jump
*	between text displays. 
*
*/
public class NextPreviousTextViewer extends TextViewer
{
	private JButton bPrev;
	private JButton bNext;
	private JButton bCurrent;
	private NextPreviousListener npl = null;
	
	// Command bar icons
	private static final String ICON_PREV 	= "resource/common/icons/24x24/stock_left_arrow_24.png";
	private static final String ICON_NEXT 	= "resource/common/icons/24x24/stock_right_arrow_24.png";
	private static final String ICON_CURRENT= "resource/common/icons/24x24/stock_refresh_24.png";
	
	// Command bar tooltips
	private static final String TT_PREV 	= "NextPreviousTextViewer.prev.tooltip";
	private static final String TT_NEXT 	= "NextPreviousTextViewer.next.tooltip";
	private static final String TT_CURRENT	= "NextPreviousTextViewer.current.tooltip";
	
	
	/** Create a NextPreviousTextViewer, which is always non-modal */
	public NextPreviousTextViewer(final JFrame parent)
	{
		super(parent, false, true);
	}// NextPreviousTextViewer()
	
	/** Set the NextPreviousListener */
	public void setNextPreviousListener(NextPreviousListener listener)
	{
		this.npl = listener;
	}// setNextPreviousListener()
	
	
	
	/** CommandBar additions */
	protected void setupCommandBar(JToolBar jtb, 
		TextViewer.JTextComponentActionListener listener)
	{
		super.setupCommandBar(jtb, listener);
		
		jtb.add(Box.createHorizontalGlue());
		NPTVListener nptvl = new NPTVListener();
		
		bPrev = new JButton(Utils.getIcon(ICON_PREV));
		bPrev.addActionListener(nptvl);
		bPrev.setToolTipText(Utils.getLocalString(TT_PREV));
		bPrev.setEnabled(false);
		jtb.add(bPrev);
		
		bCurrent = new JButton(Utils.getIcon(ICON_CURRENT));
		bCurrent.addActionListener(nptvl);
		bCurrent.setToolTipText(Utils.getLocalString(TT_CURRENT));
		jtb.add(bCurrent);
		
		bNext = new JButton(Utils.getIcon(ICON_NEXT));
		bNext.addActionListener(nptvl);
		bNext.setToolTipText(Utils.getLocalString(TT_NEXT));
		bNext.setEnabled(false);
		jtb.add(bNext);
	}// setupCommandBar()
	
	
	
	private class NPTVListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if(npl != null)
			{
				if(e.getSource() == bPrev)
				{
					executeAction( npl.npActionPerformed(-1) );
				}
				else if(e.getSource() == bNext)
				{
					executeAction( npl.npActionPerformed(+1) );
				}
				else if(e.getSource() == bCurrent)
				{
					executeAction( npl.npActionPerformed(0) );
				}
			}
		}// actionPerformed()
	}// inner class NPTVListener
	
	
	
	/** 
	*	Load text into the dialog (threaded), and set next/previous
	*	button enabled status.
	*	<p>
	*	This method should also be used to load the initial dialog text.
	*/
	public void executeAction(NextPrevousAction npa)
	{
		// no change; do nothing.
		if(npa == null)
		{
			return;
		}
		
		// disabled/enable buttons
		bPrev.setEnabled(npa.hasPrevious());
		bNext.setEnabled(npa.hasNext());
		
		// set title
		setTitle(npa.getTitle());
		
		// kill existing thread, if any... and join()
		// TODO (?? if we need to ??)
		
		// start new thread
		// display dialog, too, if it isn't being shown.
		if(isShowing())
		{
			lazyDialogTextUpdate(npa.getRunnable());
		}
		else
		{
			lazyLoadDisplayDialog(npa.getRunnable());
		}
	}// executeAction()
	
	
	
	public interface NextPreviousListener
	{
		public NextPrevousAction npActionPerformed(int increment);
	}// nested interface NextPreviousListener
	
	
	
	public static class NextPrevousAction
	{
		private final TVRunnable runnable;
		private final String title;
		private final boolean hasNext;
		private final boolean hasPrevious;
		
		/** 
		*	Create a NextPreviousAction. Note that <b>none</b> of the
		*	arguments are allowed to be null.
		*/
		public NextPrevousAction(TVRunnable runnable, String title,
			boolean next, boolean previous)
		{
			if(runnable == null || title == null)
			{
				throw new IllegalArgumentException();
			}
			
			this.runnable = runnable;
			this.title = title;
			this.hasNext = next;
			this.hasPrevious = previous;
		}// NextPrevousAction()
		
		/** */
		public TVRunnable getRunnable()	{ return runnable; }
		/** */
		public String getTitle()		{ return title; }
		/** */
		public boolean hasNext() 		{ return hasNext; }
		/** */
		public boolean hasPrevious() 	{ return hasPrevious; }
	}// nested class NextPrevousAction
	
	
	
}// class NextPreviousTextViewer
