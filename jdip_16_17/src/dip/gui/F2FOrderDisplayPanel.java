//
//  @(#)F2FOrderDisplayPanel.java		6/2003
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
package dip.gui;

import dip.order.*;
import dip.world.*;
import dip.gui.undo.*;
import dip.gui.order.GUIOrder;
import dip.gui.map.SVGColorParser;
import dip.gui.swing.ColorRectIcon;
import dip.misc.Utils;
import dip.process.Adjustment;
import dip.misc.Log;
import dip.gui.map.MapMetadata;

import dip.order.result.Result;
import dip.order.result.OrderResult;

import cz.autel.dmi.*;		// HIGLayout

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.undo.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.GridLayout;
import java.awt.BorderLayout;       
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.event.*;
import java.util.*;
import java.beans.*;
import java.text.MessageFormat;


/**
*	The F2FOrderDisplayPanel: displayer of orders for Face-to-Face (F2F) games.
*	<p>
*	This is a subclass of ODP that manages F2F games.
*
*/
public class F2FOrderDisplayPanel extends OrderDisplayPanel
{
	// i18n constants
	private static final String SUBMIT_BUTTON_TEXT 	= "F2FODP.button.submit.text";
	private static final String SUBMIT_BUTTON_TIP 	= "F2FODP.button.submit.tooltip";
	private static final String ALLPOWERS_TAB_LABEL	= "F2FODP.tab.label.allpowers";
	private static final String CONFIRM_TITLE		= "F2FODP.confirm.title";
	private static final String CONFIRM_TEXT		= "F2FODP.confirm.text";
	private static final String ENTER_ORDERS_TEXT	= "F2FODP.button.enterorders.text";
	private static final String ENTER_ORDERS_TIP	= "F2FODP.button.enterorders.tooltip";
	
	// internal constant for 'all' tab
	private static final String TAB_ALL 			= new String("_all_");
	
	// instance fields
	private JTabbedPane tabPane = null;
	private JPanel main = null;
	private JButton submit = null;
	private JButton enterOrders = null;
	private F2FState tempState = null;
	private final F2FState entryState;
	private MapMetadata mmd = null;
	private TabListener tabListener = null;
	private JPanel buttonPanel = null;				// holds submit/enter orders button
	private final HashMap tabMap;
	
	// hold resolved and next TurnStates
	private TurnState resolvedTS = null;
	private TurnState nextTS = null;
	private boolean isReviewingResolvedTS = false;
	
	
	/**
	*	Creates an F2FOrderDisplayPanel
	*/
	public F2FOrderDisplayPanel(ClientFrame clientFrame)
	{
		super(clientFrame);
		tabMap = new HashMap(11);
		entryState = new F2FState();
		makeF2FLayout();
	}// F2FOrderDisplayPanel()

    /**	Cleanup */
	public void close()
	{
		super.close();
	}// close()
	
	
	/** Handle the Submit button events */
	private class SubmissionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			// confirm submission
			final int result = JOptionPane.showConfirmDialog(
							clientFrame, 
							Utils.getLocalString(CONFIRM_TEXT),
							Utils.getLocalString(CONFIRM_TITLE), 
							JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE );
			
			if(result != JOptionPane.YES_OPTION)
			{
				return;
			}
			
			// we are confirmed
			//
			// a submission (really, just the first) disables the
			// 'all' powers tab from being selected
			setTabEnabled(TAB_ALL, false);
			
			// filter out undo actions, so they are not seen by other powers.
			// limit so that a power cannot undo the turn resolution once 
			// the 'all' tab is locked
			clientFrame.getUndoRedoManager().filterF2F();
			
			// disable this power tab
			final int idx = tabPane.getSelectedIndex();
			if(idx == 0) { throw new IllegalStateException(); }
			tabPane.setEnabledAt(idx, false);
			
			// bring up a random enabled next power. If all powers
			// have submitted orders, resolve.
			// we do this by checking which tabs are (or are not) enabled.
			// when all tabs have been disabled, resolution takes place. Since
			// eliminated powers don't have tabs, this works nicely.
			int nextAvailable = selectNextRandomTab();
			
			if(nextAvailable == -1)
			{
				saveEntryState();
				clientFrame.resolveOrders();
    		}
			else
			{
				setPowersDisplayed(nextAvailable);
				tabPane.setSelectedIndex(nextAvailable);
				saveEntryState();
				setSubmitEnabled();
			}
		}// actionPerformed()
	}// inner class SubmissionListener
	
	
	/** Handle the "Enter Orders" button event */
	private class EnterOrdersListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			isReviewingResolvedTS = false;
			resolvedTS = null;
			final TurnState tmpTS = nextTS;
			nextTS = null;
			if(tmpTS != null)
			{
				clientFrame.fireTurnstateChanged(tmpTS);
			}
			changeButton(submit);
		}
	}// inner class EnterOrdersListener
	
	
	/** Handle Tab Pane events */
	private class TabListener implements ChangeListener
	{
		private boolean isEnabled = true;
		
		public synchronized void setEnabled(boolean value)
		{
			isEnabled = value;
		}// setEnabled()
		
		public synchronized void forceUpdate()
		{
			if(isEnabled)
			{
				update();
			}
		}// forceUpdate()
		
		public synchronized void stateChanged(ChangeEvent e) 
		{
			if(isEnabled)
			{
				update();
			}
		}// stateChanged()
		
		private void update()
		{
			// set the panel
			final int idx = tabPane.getSelectedIndex();
			if(idx != -1)
			{
				JPanel panel = (JPanel) tabPane.getComponentAt(idx);
				panel.add(main, BorderLayout.CENTER);
			}
			
			// set what we can and cannot display
			if(turnState != null)
			{
				setSubmitEnabled();
				setPowersDisplayed(idx);
				saveEntryState();
			}
		}// update()
	}// inner class TabListener
	
	
	/** Extended F2FPropertyListener */
	protected class F2FPropertyListener extends ODPPropertyListener
	{
		public void actionTurnstateChanged(TurnState ts)
		{
			if(resolvedTS != null && !isReviewingResolvedTS)
			{
				isReviewingResolvedTS = true;
				changeButton(enterOrders);
				enterOrders.setEnabled( (nextTS != null) );
				
				// we're in the fireTurnstateChanged() thread/event loop;
				// fire this event outside, so that everyone can receive it.
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						clientFrame.fireTurnstateChanged(resolvedTS);
					}
				});
			}
			else
			{
				// if we use "history | next" to go to the post-resolved state
				// instead of clicking 'enter orders' button, reset the state as
				// if we had pressed the button.
				if(isReviewingResolvedTS && ts == nextTS)
				{
					enterOrders.doClick();	// this will call actionTurnstateChanged() again
					return;					// so that's why we can return
				}
				
				super.actionTurnstateChanged(ts);
				
				createTabs();
				if(tempState != null)
				{
					setupState(tempState);
					tempState = null;
				}
				
				if(!turnState.isResolved() && entryState != null)
				{
					setupState(entryState);
				}
				
				setSubmitEnabled();
			}
		}// actionTurnstateChanged()
		
		public void actionTurnstateResolved(TurnState ts)
		{
			super.actionTurnstateResolved(ts);
			resolvedTS = ts;
		}// actionTurnstateResolved()
		
		public void actionTurnstateAdded(TurnState ts)
		{
			super.actionTurnstateAdded(ts);
			nextTS = ts;
		}// actionTurnstateAdded()
		
		public void actionMMDReady(MapMetadata mmd)
		{
			super.actionMMDReady(mmd);
			F2FOrderDisplayPanel.this.mmd = mmd;
			setTabIcons();
		}// actionMMDReady()
		
		public void actionModeChanged(String mode)
		{
			super.actionModeChanged(mode);
			if(mode == ClientFrame.MODE_ORDER)
			{
				// disable some menu options
				// when in order mode.
				ClientMenu cm = clientFrame.getClientMenu();
				cm.setEnabled(ClientMenu.ORDERS_RESOLVE, false);
			}
		}// actionModeChanged()
	}// nested class F2FPropertyListener
	
	
	/** Sets the tab icons for each power. */
	private void setTabIcons()
	{
		if(mmd != null)
		{
			final Power[] allPowers = world.getMap().getPowers();
		   	for(int i=0; i<allPowers.length; i++)
            {
				final int tabIdx = getTabIndex(allPowers[i]);
				if(tabIdx > 0)
				{
					Color color = SVGColorParser.parseColor(mmd.getPowerColor(allPowers[i]));
					tabPane.setIconAt(tabIdx, new ColorRectIcon(12,12, color));
				}
           }
		}
	}// setTabIcons()
	
	
	/** 
	*	Determines when Submit button should be enabled or not.
	*	Disabled when looking at (reviewing) old turns, or 
	*	if the 'all' tab is selected and looking at the current
	*	turn.
	*/
	private void setSubmitEnabled()
	{
		assert(turnState != null);
		
		submit.setEnabled(false);
		
		// if a tab is selected that is enabled and
		// not the 'all' tab, submit should be enabled.
		if(!turnState.isResolved())
		{
			int idx = tabPane.getSelectedIndex();
			if( idx > 0 && tabPane.isEnabledAt(idx) )
			{
				submit.setEnabled(true);
			}
		}
	}// setSubmitEnabled()
	
	
	/** Change the Button in the ButtonPanel */
	private void changeButton(JButton button)
	{
		buttonPanel.removeAll();
		buttonPanel.add(button);
		buttonPanel.validate();
	}// changeButton()
	
	
	/** 
	*	Fires which powers are displayable for the given tab.
	*	Handles the All tab appropriately. (index 0). 
	*/
	private void setPowersDisplayed(int tabIdx)
	{
		if(tabIdx == 0)
		{
			Power[] allPowers = world.getMap().getPowers();
			clientFrame.fireDisplayablePowersChanged(clientFrame.getDisplayablePowers(), allPowers);
			clientFrame.fireOrderablePowersChanged(clientFrame.getOrderablePowers(), new Power[0]);
		}
		else
		{
			// need to match by tab name, since if a power was eliminated 
			// the index will not correspond to Map.getPowers()
			Power selectedPower = world.getMap().getPower( tabPane.getTitleAt(tabIdx) );
			Power[] powerArray = new Power[]  { selectedPower };
			clientFrame.fireDisplayablePowersChanged(clientFrame.getDisplayablePowers(), powerArray);
			clientFrame.fireOrderablePowersChanged(clientFrame.getOrderablePowers(), powerArray);
		}
	}// setPowersDisplayed()
	
	
	/** 
	*	Creates the Power tabs. Tabs are created for each
	*	Power that has not been eliminated or are inactive.
	*/
	private void createTabs()
	{
		assert(world != null);
		assert(turnState != null);
		
		// disable tab events
		tabListener.setEnabled(false);
		
		// change orderable powers
		clientFrame.fireOrderablePowersChanged(clientFrame.getOrderablePowers(), new Power[0]);
		
		// remove old tabs (except for 'all' tab)
		tabPane.removeAll();
		tabMap.clear();
		
		// create ALL_POWERS tab
		tabPane.addTab(Utils.getLocalString(ALLPOWERS_TAB_LABEL), new JPanel(new BorderLayout()));
		tabMap.put(TAB_ALL, new Integer(0));
		
		// appropriately enable the ALL_POWERS tab.
		setTabEnabled(TAB_ALL, turnState.isResolved());
		
		// create new power tabs
		// disable tabs for powers that don't require orders during
		// retreat or adjustment phases, if appropriate.
		final Position pos = turnState.getPosition();
		final Power[] allPowers = world.getMap().getPowers();
		Adjustment.AdjustmentInfoMap f2fAdjMap = Adjustment.getAdjustmentInfo(turnState, 
				world.getRuleOptions(), allPowers);
		
		for(int i=0; i<allPowers.length; i++)
		{
			if( !pos.isEliminated(allPowers[i]) && allPowers[i].isActive() )
			{
				// first tab added is at index 1 (0 is TAB_ALL)
				final Power power = allPowers[i];
				final int tabIdx = (i+1);
				Icon icon = null;
				
				if(mmd != null)
				{
					Color color = SVGColorParser.parseColor(mmd.getPowerColor(power));
					icon = new ColorRectIcon(12,12, color);
				}
				
				tabMap.put(allPowers[i], new Integer(tabIdx));
				tabPane.addTab(allPowers[i].getName(), icon, new JPanel(new BorderLayout()), "");
				
				Adjustment.AdjustmentInfo adjInfo = f2fAdjMap.get(allPowers[i]);
				if(turnState.getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT)
				{
					if(adjInfo.getAdjustmentAmount() == 0)
					{
						tabPane.setEnabledAt(tabIdx, false);
					}
				}
				else if(turnState.getPhase().getPhaseType() == Phase.PhaseType.RETREAT)
				{
					if(adjInfo.getDislodgedUnitCount() == 0)
					{
						tabPane.setEnabledAt(tabIdx, false);
					}
				}
			}
		}
		
		// enable tab events
		tabListener.setEnabled(true);
		
		// if not resolved, first tab is a randomly selected tab.
		// that is not disabled. Otherwise, we will select the 'all' tab.
		if(turnState.isResolved())
		{
			// at this point, tabPane.getSelectedIndex() == 0; thus if we set the
			// index to 0, no 'changeevent' will be fired. We must force an update.
			//
			tabPane.setSelectedIndex(0);	// doesn't force an update...
			tabListener.forceUpdate();		// but this will
		}
		else
		{
			// disable tabs of powers that have already submitted orders!
			if(entryState != null)
			{
				setupState(entryState);
			}
			else
			{
				// select a random power (not "all") tab
				tabPane.setSelectedIndex(selectNextRandomTab());
			}
		}
	}// createTabs()
	
	
	
     /**
     * Get the index of an unselected Power tab in a random way.
     *
     * @param tabPane the JTabbedPane containing the tabs
     * @return the index of the selected tab, or -1 if no next random tab is available.
     */
    private int selectNextRandomTab()
	{
 		// find Power tabs that are not disabled
		final Power[] allPowers = world.getMap().getPowers();
		List tabSelectionOrderList = new ArrayList(allPowers.length);
 		
		for(int i=0; i<allPowers.length; i++)
		{
			final int tabIdx = getTabIndex(allPowers[i]);
			if(tabIdx >= 0 && tabPane.isEnabledAt(tabIdx))
			{
				tabSelectionOrderList.add(allPowers[i]);
			}
	   }
	   
	   if(!tabSelectionOrderList.isEmpty())
	   {
		   // shuffle, return first on list.
		   Collections.shuffle(tabSelectionOrderList);
		   return getTabIndex((Power) tabSelectionOrderList.get(0));
	   }
	   else
	   {
		   return -1;
	   }
    }// selectNextRandomTab()


    /** Create an extended property listener. */
    protected AbstractCFPListener createPropertyListener()
    {
        return new F2FPropertyListener();
    }// createPropertyListener()


    /** Make the F2F layout. */
	private void makeF2FLayout()
	{
		// submit button
		submit = new JButton(Utils.getLocalString(SUBMIT_BUTTON_TEXT));
		submit.setToolTipText(Utils.getLocalString(SUBMIT_BUTTON_TIP));
		submit.addActionListener(new SubmissionListener());
		
		enterOrders = new JButton(Utils.getLocalString(ENTER_ORDERS_TEXT));
		enterOrders.setToolTipText(Utils.getLocalString(ENTER_ORDERS_TIP));
		enterOrders.addActionListener(new EnterOrdersListener());
		
		// center the buttonPanel button
		buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(submit);
		
		// we want to share the main panel between all tabs
		// main panel layout
		main = new JPanel();
		int w1[] = { 0 };
		int h1[] = { 0, 5, 0, 10, 0};	
		
		HIGLayout hl = new HIGLayout(w1, h1);
		hl.setColumnWeight(1, 1);
		hl.setRowWeight(1, 1);
		main.setLayout(hl);
		
		HIGConstraints c = new HIGConstraints();
		
		main.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		main.add(orderListScrollPane, c.rc(1,1,"lrtb"));
		main.add(makeSortPanel(), c.rc(3,1));
		main.add(buttonPanel, c.rc(5,1));
		
		tabPane = new JTabbedPane();
		tabListener = new TabListener();
		tabPane.addChangeListener(tabListener);
		tabPane.setTabPlacement(JTabbedPane.TOP);
		
		// set the layout of F2FODP
		setLayout(new BorderLayout());
		add(tabPane, BorderLayout.CENTER);
	}// makeF2FLayout()
	
	
	/** Do nothing. We have our own layout method. */
	protected void makeLayout()
	{
		// do nothing.
	}// makeLayout()
	
	
	
	/** 
	*	Enables/Disables a given tab. Obj must be a Power or the 
	*	internal constant TAB_ALL.
	*/
	private void setTabEnabled(Object obj, boolean value)
	{
		// sanity check
		if(obj != TAB_ALL && !(obj instanceof Power))
		{
			throw new IllegalArgumentException();
		}
		
		final int idx = getTabIndex(obj);
		if(idx >= 0)
		{
			tabPane.setEnabledAt(idx, value);
		}
	}// setTabEnabled()
	
	
	/** 
	*	Checks if a tab is Enabled/Disabled. Obj must be a Power or 
	*	internal constant TAB_ALL.
	*/
	private boolean isTabEnabled(Object obj)
	{
		// sanity check
		if(obj != TAB_ALL && !(obj instanceof Power))
		{
			throw new IllegalArgumentException();
		}
		
		final int idx = getTabIndex(obj);
		if(idx >= 0)
		{
			return tabPane.isEnabledAt(idx);
		}
		
		return false;
	}// isTabEnabled()
	
	
	/**
	*	Get the index of a tab. Obj must be a Power or 
	*	internal constant TAB_ALL. If the tab is not found,
	*	-1 is returned.
	*/
	private int getTabIndex(Object obj)
	{
		// sanity check
		if(obj != TAB_ALL && !(obj instanceof Power))
		{
			throw new IllegalArgumentException();
		}
		
		Integer i = (Integer) tabMap.get(obj);
		return (i == null) ? -1 : i.intValue();
	}// getTabIndex()
	
	
	
	
	
	
	
	
	
	
	/** Actually setup the state. */
	private void setupState(F2FState state)
	{
		if(turnState != null)
		{
			// set enabled tabs (submitted == disabled)
			boolean aSubmit = false;
			final Power[] allPowers = world.getMap().getPowers();
		   	for(int i=0; i<allPowers.length; i++)
            {
				boolean value = state.getSubmitted(allPowers[i]);
				aSubmit = (value) ? true : aSubmit;
				setTabEnabled(allPowers[i], !value);
			}
			
			// set TAB_ALL enabled (if aSubmit == true, at least one turn
			// was submitted, and thus we must disable)
			setTabEnabled(TAB_ALL, !aSubmit);
			
			// set selected tab
			if(state.getCurrentPower() == null)
			{
				// if no tab selected, select 'all' (if resolved); otherwise, 
				// select a random tab.
				if(turnState.isResolved())
				{
					tabPane.setSelectedIndex(getTabIndex(TAB_ALL));
				}
				else
				{
					tabPane.setSelectedIndex(selectNextRandomTab());
				}
			}
			else
			{
				tabPane.setSelectedIndex( getTabIndex(state.getCurrentPower()) );
			}
		}
	}// setupState()
	
	
	
	/** Saves the current state, if appropriate. */
	private void saveEntryState()
	{
		assert(turnState != null);
		if(turnState.isResolved())
		{
			entryState.clearSubmitted();
			entryState.setCurrentPower(null);
		}
		else
		{
			entryState.setCurrentPower(null);
			final int selectedIdx = tabPane.getSelectedIndex();
			final Power[] allPowers = world.getMap().getPowers();
		   	for(int i=0; i<allPowers.length; i++)
            {
				final Power power = allPowers[i];
				entryState.setSubmitted(power, !isTabEnabled(power));
				if(selectedIdx >= 0 && selectedIdx == getTabIndex(power))
				{
					entryState.setCurrentPower(power);
				}
			}
		}
	}// saveEntryState()
	
	
	/** Restore the state */
	public void restoreState(F2FState state)
	{
		if(turnState != null)
		{
			setupState(state);
		}
		else
		{
			// temporarily save, until
			// we are able to restore.
			tempState = state;
		}
	}// restoreState()
	
	
	/** 
	*	Get the state, so it may be restored later. The returned object
	*	is a copy; manipulating it will have no effect upon the internal
	*	state.
	*/
	public F2FState getState()
	{
		return new F2FState(entryState);
	}// getState()
	
	
	/** The F2F Statekeeping object, for saving */
	public static class F2FState
	{
		private final HashMap submittedMap;
		private Power currentPower;
		
		/** Create an F2FState object */
		public F2FState()
		{
			submittedMap = new HashMap(11);
		}// F2FState()
		
		/** Create an F2FState object from an existing F2FState object */
		public F2FState(F2FState f2fs)
		{
			if(f2fs == null) { throw new IllegalArgumentException(); }
			
			currentPower = f2fs.getCurrentPower();
			synchronized(f2fs)
			{
				submittedMap = (HashMap) f2fs.submittedMap.clone();
			}
		}// F2FState()
		
		/** The current power (or null) who is entering orders. */
		public synchronized Power getCurrentPower()
		{ 
			return currentPower; 
		}// getCurrentPower()
		
		/** Set the current power (or null) who is entering orders. */
		public synchronized void setCurrentPower(Power power)
		{
			currentPower = power;
		}// setCurrentPower()
		
		/**
		*	Get if the Power has submitted orders.
		*/
		public synchronized boolean getSubmitted(Power power)
		{
			if(power == null) { throw new IllegalArgumentException(); }
			return Boolean.TRUE.equals(submittedMap.get(power));
		}// getSubmitted()
		
		/** Set if a power has submitted orders */
		public synchronized void setSubmitted(Power power, boolean value)
		{
			if(power == null) { throw new IllegalArgumentException(); }
			submittedMap.put(power, Boolean.valueOf(value)); 
		}// setSubmitted()
		
		/** Reset all powers to "not submitted" state. */
		public synchronized void clearSubmitted()
		{
			submittedMap.clear();
		}// clearSubmitted()
		
		/** Get an iterator. Warning: not synchronized. Should only be used on a copy. */
		public Iterator iterator()
		{
			return submittedMap.entrySet().iterator();
		}
	}// nested class F2FState
	
}// class F2FOrderDisplayPanel
