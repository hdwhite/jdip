//
//  @(#)DisplayPreferencePanel.java		2/2003
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

package dip.gui.dialog.prefs;

import dip.gui.ClientFrame;
import dip.gui.swing.GradientJLabel;
import dip.order.OrderFormat;
import dip.order.OrderFormat.OrderFormatOptions;
import dip.misc.SharedPrefs;
import dip.misc.Utils;

// HIGLayout
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Component;

/**
*	Display preferences
*	<p>
*	Static methods are included to access (in a controlled manner) preference-controlled
*	functionality.
*
*
*/
public class DisplayPreferencePanel extends PreferencePanel
{
	// i18n keys
	private static final String I18N_TAB_NAME		= "DPP.tab.text";
	
	private static final String I18N_FULL_NAME 		= "DPP.radiobutton.value.full";	
	private static final String I18N_BRIEF_NAME		= "DPP.radiobutton.value.brief";
	
	private static final String I18N_ORDFMT_UNIT		= "DPP.radiobutton.unit.text";
	private static final String I18N_ORDFMT_COAST		= "DPP.radiobutton.coast.text";
	private static final String I18N_ORDFMT_PROVINCE	= "DPP.radiobutton.province.text";
	private static final String I18N_ORDFMT_ORDERNAME	= "DPP.radiobutton.ordername.text";
	private static final String I18N_ORDFMT_GROUP_NAME = "DPP.orderformat.group.text";
	
	
	// Preference Node Keys
	private static final String NODE_ORDERFORMAT_COAST 		= "orderFormat.Coast";
	private static final String NODE_ORDERFORMAT_PROVINCE	= "orderFormat.Province";
	private static final String NODE_ORDERFORMAT_ORDERNAME	= "orderFormat.OrderName";
	private static final String NODE_ORDERFORMAT_UNIT 		= "orderFormat.UnitType";
	
	// misc
	private static final int BORDER = 10;
	private static final int INDENT = 20;
	private ClientFrame	clientFrame = null;
	private OrderFormatOptions orderFormat = null;
	
	// GUI items
	private ChoiceSelection		csUnitFormat;
	private ChoiceSelection		csOrderNameFormat;
	private ChoiceSelection		csProvinceFormat;
	private ChoiceSelection		csCoastFormat;
	private JLabel example;
	
	
	
	public DisplayPreferencePanel(final ClientFrame cf)
	{
		super();
		clientFrame = cf;
		
		// get OFO from client
		orderFormat = clientFrame.getOFO();
		
		// create GUI components
		makeChoiceSelections();
		example = new JLabel("");
		updateExampleAndFormatOptions();
		
		// layout 
		int h1[] = { BORDER, 0,5,  0,5,  0,1, 0,1, 0,1, 0,1,   0, BORDER };
		int w1[] = { BORDER, INDENT, 0,5,  0,5,  0, 0,  BORDER };
		
		HIGLayout l1 = new HIGLayout(w1, h1);
		l1.setColumnWeight(8, 1);
		l1.setRowWeight(14, 1);
		setLayout(l1);
		
		HIGConstraints c = new HIGConstraints();
		
		add(new GradientJLabel(Utils.getLocalString(I18N_ORDFMT_GROUP_NAME)), c.rcwh(2,2,7,1,"lr"));
		
		add(example, c.rcwh(4,3,7,1,"l"));
		
		add(csProvinceFormat.getLabel(), c.rcwh(6,3,1,1,"l"));
		add(csProvinceFormat.getChoice1(), c.rcwh(6,5,1,1,"l"));
		add(csProvinceFormat.getChoice2(), c.rcwh(6,7,1,1,"l"));
		
		add(csCoastFormat.getLabel(), c.rcwh(8,3,1,1,"l"));
		add(csCoastFormat.getChoice1(), c.rcwh(8,5,1,1,"l"));
		add(csCoastFormat.getChoice2(), c.rcwh(8,7,1,1,"l"));
		
		add(csUnitFormat.getLabel(), c.rcwh(10,3,1,1,"l"));
		add(csUnitFormat.getChoice1(), c.rcwh(10,5,1,1,"l"));
		add(csUnitFormat.getChoice2(), c.rcwh(10,7,1,1,"l"));
		
		add(csOrderNameFormat.getLabel(), c.rcwh(12,3,1,1,"l"));
		add(csOrderNameFormat.getChoice1(), c.rcwh(12,5,1,1,"l"));
		add(csOrderNameFormat.getChoice2(), c.rcwh(12,7,1,1,"l"));
	}// DisplayPreferencePanel()
	
	
	
	
	
	
	
	
	public String getName()
	{
		return Utils.getLocalString(I18N_TAB_NAME);
	}// getName()
	
	
	public void apply()
	{
		Preferences prefs = SharedPrefs.getUserNode();
		
		// set preference nodes
		prefs.putBoolean(NODE_ORDERFORMAT_COAST, csCoastFormat.getValue());
		prefs.putBoolean(NODE_ORDERFORMAT_PROVINCE, csProvinceFormat.getValue());
		prefs.putBoolean(NODE_ORDERFORMAT_ORDERNAME, csOrderNameFormat.getValue());
		prefs.putBoolean(NODE_ORDERFORMAT_UNIT, csUnitFormat.getValue());
		
		// apply OrderFormat to ClientFrame
		clientFrame.setOFO(orderFormat);
		
		try { prefs.flush(); } catch (BackingStoreException bse) {}
	}// apply()
	
	
	public void setDefault()
	{
		orderFormat = OrderFormatOptions.DEFAULT;
	}// setDefault()
	
	
	public void cancel()
	{
		// do nothing
	}// cancel()
	
	
	/**
	*	Get the OrderFormatOptions from the stored preferences, or
	*	return a default set of OrderFormatOptions.
	*/
	public static OrderFormatOptions getOrderFormatOptions()
	{
		Preferences prefs = SharedPrefs.getUserNode();
		try { prefs.sync(); } catch (BackingStoreException bse) {}
		
		OrderFormatOptions defaultOFO = OrderFormatOptions.DEFAULT;
		OrderFormatOptions ofo = new OrderFormatOptions(
			prefs.getBoolean(NODE_ORDERFORMAT_UNIT, defaultOFO.getBriefUnitType()),
			prefs.getBoolean(NODE_ORDERFORMAT_ORDERNAME, defaultOFO.getBriefOrderName()),
			prefs.getBoolean(NODE_ORDERFORMAT_PROVINCE, defaultOFO.getBriefProvince()),
			prefs.getBoolean(NODE_ORDERFORMAT_COAST, defaultOFO.getBriefCoast())
		);
		
		return ofo;
	}// getOrderFormatOptions()
	
	
	
	/** Make ChoiceSelections, from preferences (or default values). */
	private void makeChoiceSelections()
	{
		Preferences prefs = SharedPrefs.getUserNode();
		try { prefs.sync(); } catch (BackingStoreException bse) {}
		
		// defaults: 
		final OrderFormatOptions ofo = OrderFormatOptions.DEFAULT;
		
		// change listener
		ExampleChangeListener ecl = new ExampleChangeListener();
		
		// order-format radiobutton setings
		csUnitFormat = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_UNIT),
			prefs.getBoolean(NODE_ORDERFORMAT_UNIT, ofo.getBriefUnitType()), ecl);
			
		csOrderNameFormat = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_ORDERNAME),
			prefs.getBoolean(NODE_ORDERFORMAT_ORDERNAME, ofo.getBriefOrderName()), ecl);
			
		csProvinceFormat = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_PROVINCE),
			prefs.getBoolean(NODE_ORDERFORMAT_PROVINCE, ofo.getBriefProvince()), ecl);
			
		csCoastFormat = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_COAST),
			prefs.getBoolean(NODE_ORDERFORMAT_COAST, ofo.getBriefCoast()), ecl);
			
	}// makeChoiceSelections()
	
	
	/** 
	*	Updates the Example text order.
	*
	*/
	private void updateExampleAndFormatOptions()
	{
		orderFormat = new OrderFormatOptions(
			csProvinceFormat.getValue(),
			csCoastFormat.getValue(),
			csOrderNameFormat.getValue(),
			csUnitFormat.getValue()
		);
		
		// update the example text
		StringBuffer sb = new StringBuffer(128);
		sb.append("<html><b>");
		sb.append(OrderFormat.getFormatExample(orderFormat, clientFrame.getGUIOrderFactory()));
		sb.append("</b></html>");
		example.setText( sb.toString() );
	}// updateExampleAndFormatOptions()
	
	
	
	/** 
	*	Inner class that implements a description plus 2 choices
	*	which are radiobuttons. Allows the getting() and setting()
	*	of the radiobuttons based upon OrderFormat FMT constants.
	*	<p>
	*	Also takes care of proper display (layout).
	*/
	private class ChoiceSelection extends JPanel
	{
		private ButtonGroup bg;
		private JRadioButton brief;
		private JRadioButton full;
		private JLabel label;
		
		/** 
		*	Create a ChoiceSelection with the given category label "label", and
		*	the given format value: brief (true) or full (false)).
		*/
		public ChoiceSelection(String labelText, boolean fmtValue, ChangeListener cl)
		{
			super();
			
			// create components
			bg = new ButtonGroup();
			full = new JRadioButton(Utils.getLocalString(I18N_FULL_NAME));
			full.getModel().addChangeListener(cl);
			brief = new JRadioButton(Utils.getLocalString(I18N_BRIEF_NAME));
			brief.getModel().addChangeListener(cl);
			
			bg.add(full);
			bg.add(brief);
			
			if(fmtValue)
			{
				brief.setSelected(true);
			}
			else
			{
				full.setSelected(true);
			}
			
			// label
			label = new JLabel(labelText);
		}// ChoiceSelection()
		
		
		/** Get label */
		public Component getLabel()			{ return label; }
		/** Get choice 1 */
		public Component getChoice1()		{ return full; }
		/** Get choice 2 */
		public Component getChoice2()		{ return brief; }
		
		
		/** 
		*	Returns true or false, depending 
		*	upon the JRadioButton selected.
		*/
		public boolean getValue()
		{
			if(bg.isSelected(brief.getModel()))
			{
				return true;
			}
			if(bg.isSelected(full.getModel()))
			{
				return false;
			}
			else
			{
				throw new IllegalStateException();
			}
		}// getValue()
		
	}// inner class ChoiceSelection
	
	
	/** Listens for radiobutton changes, and updates the Example text. */
	private class ExampleChangeListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent e)
		{
			if(example != null)
			{
				updateExampleAndFormatOptions();
			}
		}// stateChanged()
	}// ExampleChangeListener()
	
}// class DisplayPreferencePanel
