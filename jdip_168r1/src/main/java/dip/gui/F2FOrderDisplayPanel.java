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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import dip.gui.map.MapMetadata;
import dip.gui.map.SVGColorParser;
import dip.gui.swing.ColorRectIcon;
import dip.misc.Utils;
import dip.process.Adjustment;
import dip.world.Phase;
import dip.world.Position;
import dip.world.Power;
import dip.world.TurnState;


/**
*	The F2FOrderDisplayPanel: displayer of orders for Face-to-Face (F2F) games.
*	<p>
*	This is a subclass of ODP that manages F2F games.
*
*/
public class F2FOrderDisplayPanel extends OrderDisplayPanel
{
	/*
	
	This version if from the old, unused (16_17) source tree, and is revision 1.3.
	This fixes several NPEs and other F2F issues.
	
	*/
	
	// i18n constants
	private static final String SUBMIT_BUTTON_TEXT 	= "F2FODP.button.submit.text";
	private static final String SUBMIT_BUTTON_TIP 	= "F2FODP.button.submit.tooltip";
	private static final String ALLPOWERS_TAB_LABEL	= "F2FODP.tab.label.allpowers";
	private static final String CONFIRM_TITLE		= "F2FODP.confirm.title";
	private static final String CONFIRM_TEXT		= "F2FODP.confirm.text";
	private static final String ENTER_ORDERS_TEXT	= "F2FODP.button.enterorders.text";
	private static final String ENTER_ORDERS_TIP	= "F2FODP.button.enterorders.tooltip";
	
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
			setTabEnabled(null, false);
			
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
			TabComponent nextAvailable = selectNextRandomTab();
			
			if(nextAvailable == null)
			{
				saveEntryState();
				clientFrame.resolveOrders();
    		}
			else
			{
				setPowersDisplayed(nextAvailable);
				tabPane.setSelectedComponent(nextAvailable);
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
			final TabComponent tc = (TabComponent) tabPane.getSelectedComponent();
			if(tc != null)
			{
				tc.add(main, BorderLayout.CENTER);
			}
			
			// set what we can and cannot display
			if(turnState != null)
			{
				setSubmitEnabled();
				setPowersDisplayed(tc);
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
			for(int i=0; i<tabPane.getTabCount(); i++)
			{
				final TabComponent tc = (TabComponent) tabPane.getComponentAt(i);
				if(tc.getPower() != null)
				{
					final Color color = SVGColorParser.parseColor(mmd.getPowerColor(tc.getPower()));
					tabPane.setIconAt(i, new ColorRectIcon(12,12, color));
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
	private void setPowersDisplayed(final TabComponent tc)
	{
		if(tc == null)
		{
			return;
		}
		
		if(tc.getPower() == null)
		{
			// "all powers"
			final Power[] powers = world.getMap().getPowers();
			clientFrame.fireDisplayablePowersChanged(clientFrame.getDisplayablePowers(), powers);
			clientFrame.fireOrderablePowersChanged(clientFrame.getOrderablePowers(), Power.EMPTY_ARRAY);
		}
		else
		{
			// need to match by tab name, since if a power was eliminated 
			// the index will not correspond to Map.getPowers()
			final Power[] powerArray = new Power[]  { tc.getPower() };
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
		clientFrame.fireOrderablePowersChanged(clientFrame.getOrderablePowers(), Power.EMPTY_ARRAY);
		
		// remove old tabs (except for 'all' tab)
		tabPane.removeAll();
		
		// create ALL_POWERS tab
		tabPane.addTab(Utils.getLocalString(ALLPOWERS_TAB_LABEL), new TabComponent(null));
		
		// appropriately enable the ALL_POWERS tab.
		setTabEnabled(null, turnState.isResolved());
		
		// create new power tabs
		// disable tabs for powers that don't require orders during
		// retreat or adjustment phases, if appropriate.
		final Position pos = turnState.getPosition();
		final Power[] powers = world.getMap().getPowers();
		
		Adjustment.AdjustmentInfoMap f2fAdjMap = Adjustment.getAdjustmentInfo(turnState, 
				world.getRuleOptions(), powers);
		
		for(int i=0; i<powers.length; i++)
		{
			final Power power = powers[i];
			if( !pos.isEliminated(power) && power.isActive() )
			{
				// create icon, if possible
				Icon icon = null;
				if(mmd != null)
				{
					Color color = SVGColorParser.parseColor(mmd.getPowerColor(power));
					icon = new ColorRectIcon(12,12, color);
				}
				
				// create tab
				tabPane.addTab(power.getName(), icon, new TabComponent(power), "");
				
				// disable tabs if appropriate
				Adjustment.AdjustmentInfo adjInfo = f2fAdjMap.get(power);
				if(turnState.getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT)
				{
					if(adjInfo.getAdjustmentAmount() == 0)
					{
						setTabEnabled(power, false);
					}
				}
				else if(turnState.getPhase().getPhaseType() == Phase.PhaseType.RETREAT)
				{
					if(adjInfo.getDislodgedUnitCount() == 0)
					{
						setTabEnabled(power, false);
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
			// when resolved, we want to view 'all' orders. We do not want to 
			// view individual power orders.
			tabPane.setSelectedIndex(0);
			
			// disable individual power orders
			for(int i=1; i<tabPane.getTabCount(); i++)
			{
				tabPane.setEnabledAt(i, false);
			}
			
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
				tabPane.setSelectedComponent(selectNextRandomTab());
			}
		}
	}// createTabs()
	
	
	
     /**
     * Get the index of an unselected Power tab in a random way.
     *
     * @param tabPane the JTabbedPane containing the tabs
     * @return the index of the selected tab, or null if no next random tab is available.
     */
    private TabComponent selectNextRandomTab()
	{
 		// find Power tabs that are not disabled
		final Power[] powers = world.getMap().getPowers();
		List tabSelectionOrderList = new ArrayList(powers.length);
 		
		for(int i=0; i<powers.length; i++)
		{
			TabComponent tc = getTabComponent(powers[i]);
			if( tabPane.isEnabledAt(tabPane.indexOfComponent(tc)) )
			{
				tabSelectionOrderList.add(tc);
			}
		}
	   
	   if(tabSelectionOrderList.isEmpty())
	   {
		   return null; // no tabs left!
	   }
	   else
	   {
		   // shuffle, return first on list.
		   Collections.shuffle(tabSelectionOrderList);
		   return (TabComponent) tabSelectionOrderList.get(0);
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
	*	Enables/Disables a given tab. Obj must be a Power or 
	*	null, which corresponds to the "All" tab.
	*/
	private void setTabEnabled(final Power p, final boolean value)
	{
		final TabComponent tc = getTabComponent(p);
		tabPane.setEnabledAt(tabPane.indexOfComponent(tc), value);
	}// setTabEnabled()
	
	
	/** 
	*	Checks if a tab is Enabled/Disabled. Obj must be a Power or 
	*	internal constant TAB_ALL.
	*/
	private boolean isTabEnabled(final Power p)
	{
		final TabComponent tc = getTabComponent(p);
		return tabPane.isEnabledAt(tabPane.indexOfComponent(tc));
	}// isTabEnabled()
	
	
	/**
	*	Gets a TabComponent for the given Power; does not return null
	*/
	private TabComponent getTabComponent(final Power p)
	{
		for(int i=0; i<tabPane.getTabCount(); i++)
		{
			TabComponent tc = (TabComponent) tabPane.getComponentAt(i);
			if(tc == null)
			{
				throw new IllegalStateException("null TabComponent set for power: "+p);
			}
			else if(Utils.areEqual(p, tc.getPower()))
			{
				return tc;
			}
		}
		
		throw new IllegalStateException("power "+p+" is not in the tabpane");
	}// getTabComponent()
	
	
	
	
	
	/** Actually setup the state. */
	private void setupState(F2FState state)
	{
		if(turnState != null)
		{
			// set enabled tabs (submitted == disabled)
			boolean aSubmit = false;
			final Power[] powers = world.getMap().getPowers();
			for(int i=0; i<powers.length; i++)
			{
				final Power power = powers[i];
				boolean value = state.getSubmitted(power);
				aSubmit = (value) ? true : aSubmit;
				setTabEnabled(power, !value);
			}
			
			// TAB_ALL is enabled iff the turn has been resolved. Otherwise,
			// it is disabled.
			setTabEnabled(null, turnState.isResolved());
			
			// set selected tab
			if(state.getCurrentPower() == null)
			{
				// if no tab selected, select 'all' (if resolved); otherwise, 
				// select a random tab.
				if(turnState.isResolved())
				{
					// set to index 0 : TAB_ALL
					tabPane.setSelectedIndex(0);
				}
				else
				{
					tabPane.setSelectedComponent(selectNextRandomTab());
				}
			}
			else
			{
				tabPane.setSelectedComponent( getTabComponent(state.getCurrentPower()) );
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
			final Power[] powers = world.getMap().getPowers();
			
			// set submitted
			for(int i=0; i<powers.length; i++)
			{
				final Power power = powers[i];
				entryState.setSubmitted(power, !isTabEnabled(power));
			}
			
			// set current power
			final TabComponent tc = (TabComponent) tabPane.getSelectedComponent();
			if(tc.getPower() != null)
			{
				entryState.setCurrentPower(tc.getPower());
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
			
			synchronized(f2fs)
			{
				currentPower = f2fs.getCurrentPower();
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
		
		/** 
		*	Get an iterator. Note that this <b>always</b> returns an iterator
		*	on a <b>copy</b> of the F2FState.
		*/
		public synchronized Iterator iterator()
		{
			final F2FState copy = new F2FState(this); 
			return copy.submittedMap.entrySet().iterator();
		}// iterator()
	}// nested class F2FState
	
	
	/** The component of a Tab. Has a BorderLayout. */
	class TabComponent extends JPanel
	{
		private final Power power;
		
		public TabComponent(Power power)
		{
			super();
			this.power = power;
			setLayout(new BorderLayout());
		}// TabComponent()
		
		/** Gets a Power associated with a component, or null if the 'all' tab */
		public Power getPower()
		{
			return power;
		}// get()
	}// class TabComponent
	
}// class F2FOrderDisplayPanel

