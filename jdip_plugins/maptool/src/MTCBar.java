//
//  @(#)MTCBar.java		6/2003
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

import dip.world.*;

import dip.misc.*;
import dip.gui.*;
import dip.gui.map.*;
import dip.gui.order.*;
import dip.gui.map.RenderCommandFactory.RenderCommand;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.border.*;
import org.apache.batik.swing.*;
import java.awt.geom.AffineTransform;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.text.*;

import org.apache.batik.swing.JSVGCanvas; 
import org.w3c.dom.events.MouseEvent;
import org.apache.batik.dom.events.DOMKeyEvent;

import org.apache.batik.*;
import org.apache.batik.dom.*;
import org.apache.batik.util.*;
import org.w3c.dom.svg.*;
import org.apache.batik.dom.svg.SVGOMPoint;
import org.w3c.dom.*;

import org.w3c.dom.svg.SVGUseElement;


/**
*	MapTool control bar
*
*/
public class MTCBar extends ViewControlBar
{
	// modes
	final static int SET_UNIT = 0;
	final static int SET_DISLODGED_UNIT = 1;
	final static int SET_SC = 2;
	final static int SET_SHORT_LABEL = 3;
	final static int SET_LONG_LABEL = 4;
	
	int currentMode = 0;
	Coast currentCoast = null;
	
	String placeText = "";
	
	MTHelper mth = null;
	MTLabeler mtl = null;
	
	
	
	// coastal toggle buttons
	JToggleButton cMV;
	JToggleButton cN;
	JToggleButton cS;
	JToggleButton cW;
	JToggleButton cE;
	ButtonGroup coastGroup;
	
	// other toggles
	JToggleButton labelBrief;
	JToggleButton labelFull;
	JToggleButton placeUnit;
	JToggleButton placeDislodgedUnit;
	JToggleButton placeSC;
	
	
	// row 2 stuff
	JTextField cssClass;
	JTextField cssStyle;
	JTextField rotate;
	//JButton viewCSS;
	
	
	/** Creates MTCBar. */
	public MTCBar(MapPanel mp)
	{
		super(mp);
		makeLayout();
		mth = new MTHelper(mp.getClientFrame());
		mtl = new MTLabeler(mp.getClientFrame(), this);
		
		// adds all units, dislodged units to map
		mth.setAllUnits();
		mth.renderAllProvinces();
	}// MTCBar()
	
	/** Get MTHelper */
	public MTHelper getMTHelper() { return mth; }
	
	/** Get MTLabeler */
	public MTLabeler getMTLabeler() { return mtl; }
	
	/** Called when the mouse pointer enters a province */
	public void mouseOver(MouseEvent me, Location loc)
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(placeText);
		
		if(loc == null)
		{
			mapPanel.getStatusBarUtils().setText( Utils.getLocalString(GUIOrder.NOT_IN_PROVINCE) );
			mapPanel.getJSVGCanvas().setCursor(MapPanel.BAD_ACTION);
			return;
		}
		else if(currentMode == SET_SC)
		{
			if(loc.getProvince().hasSupplyCenter())
			{
				mapPanel.getJSVGCanvas().setCursor(java.awt.Cursor.getDefaultCursor());
				mapPanel.getStatusBarUtils().displayProvinceInfo(loc, sb.toString());
				return;
			}
			else
			{
				mapPanel.getStatusBarUtils().displayProvinceInfo(loc, "No supply center here.");
				mapPanel.getJSVGCanvas().setCursor(MapPanel.BAD_ACTION);
				return;
			}
		}
		else if(currentMode == SET_SHORT_LABEL)
		{
			TextInfo ti = mtl.getBriefTextInfo(loc.getProvince());
			sb.append(" current element");
			
			if(ti.getRotation() != 0.0f)
			{
				sb.append(" rotation: ");
				sb.append( MTHelper.formatFloat(ti.getRotation()) );
			}
			
			sb.append(" CSS: class=\"");
			sb.append(ti.getCSSClass());
			sb.append("\"; style=\"");
			sb.append(ti.getCSSStyle());
			sb.append("\"");
		}
		else if(currentMode == SET_LONG_LABEL)
		{
			TextInfo ti = mtl.getFullTextInfo(loc.getProvince());
			sb.append(" current element");
			
			if(ti.getRotation() != 0.0f)
			{
				sb.append(" rotation: ");
				sb.append( MTHelper.formatFloat(ti.getRotation()) );
			}
			
			sb.append(" CSS: class=\"");
			sb.append(ti.getCSSClass());
			sb.append("\"; style=\"");
			sb.append(ti.getCSSStyle());
			sb.append("\"");
		}
		else if( (currentMode == SET_UNIT 
			|| currentMode == SET_DISLODGED_UNIT)
			&& loc.getProvince().isMultiCoastal() )
		{
			// add 'exact' coast we're over, if multicoastal
			// and in set unit/dislodgedunit mode
			sb.append(" [Coast: ");
			sb.append(loc.getCoast());
			sb.append("]");
		}
		
		// fall-through to here, if not a bad action.
		// display 'good' cursor, and text message
		mapPanel.getStatusBarUtils().displayProvinceInfo(loc, sb.toString());
		mapPanel.getJSVGCanvas().setCursor(java.awt.Cursor.getDefaultCursor());
	}// mouseOver()
	
	
	/** Called when the mouse pointer leaves a province */
	public void mouseOut(MouseEvent me, Location loc)
	{
		mapPanel.getStatusBarUtils().clearText();
	}// mouseOut()
	
	
	/** Called when the mouse pointer is clicked */
	public void mouseClicked(MouseEvent me, Location loc)
	{
		if(loc == null)
		{
			Log.println("MapTool: invalid location; ignored.");
			return;
		}
		
		final short button = me.getButton();
		final boolean delete = (button == DOMUIEventListener.BUTTON_RIGHT);
		
		Province p = loc.getProvince();
		
		// get point
		final int x = me.getClientX();
		final int y = me.getClientY();
		
		// transform point into SVG coordinates.
		// accounting for scale and what not.
		//
		JSVGCanvas canvas = mapPanel.getJSVGCanvas();
		SVGSVGElement rootSVG = canvas.getSVGDocument().getRootElement();
		SVGMatrix mat = rootSVG.getScreenCTM();
		SVGMatrix imat = mat.inverse();
		SVGPoint pt = rootSVG.createSVGPoint();
		pt.setX(x);
		pt.setY(y);
		pt = pt.matrixTransform(imat);
		
		//System.out.println("Location: client: ("+x+","+y+"); xformed: x="+pt.getX()+", y="+pt.getY()+";");
		
		if( currentMode == SET_SHORT_LABEL 
			|| currentMode == SET_LONG_LABEL )
		{
			if(delete)
			{
				return;	// do nothing for RMB clicks
			}
			
			final String cssClassText = cssClass.getText().trim();
			final String cssStyleText = cssStyle.getText().trim();
			float angle = 0.0f;
			
			try
			{
				angle = Float.parseFloat(rotate.getText().trim());
			}
			catch(NumberFormatException e)
			{
				angle = 0.0f;
			}
			
			if(currentMode == SET_SHORT_LABEL)
			{
				mtl.setBriefAttributes(p, pt.getX(), pt.getY(), angle, cssClassText, cssStyleText);
			}
			else
			{
				mtl.setFullAttributes(p, pt.getX(), pt.getY(), angle, cssClassText, cssStyleText);
			}
		}
		else if(currentMode == SET_SC)
		{
			if(p.hasSupplyCenter())
			{
				mth.changeSC(p, pt.getX(), pt.getY());
			}
		}
		else if( currentMode == SET_UNIT 
				 || currentMode == SET_DISLODGED_UNIT )
		{
			boolean dislodged = (currentMode == SET_DISLODGED_UNIT);
			
			if(!p.isMultiCoastal())
			{
				mth.changeUnit(p, pt.getX(), pt.getY(), dislodged, delete);
			}
			else
			{
				if(currentCoast == null)
				{
					Utils.popupError(mapPanel.getClientFrame(), "Error", "Multiple coasts for this province; please select one.");
				}
				else if(!p.isCoastValid(currentCoast))
				{
					Utils.popupError(mapPanel.getClientFrame(), "Error", "This is not a valid coast for this province.");
				}
				else
				{
					// use currentCoast
					mth.changeUnit(p, currentCoast, pt.getX(), pt.getY(), dislodged, delete);
				}
			}
		}
		else
		{
			System.out.println("** unknown mode!!");
		}
	}// mouseClicked()
	
	
	/** Enable/Disable text input widgets */
	public synchronized void setTextInputEnabled(boolean value)
	{
		if(!value && (currentMode == SET_LONG_LABEL || currentMode == SET_SHORT_LABEL))
		{
			placeSC.doClick();
		}
		
		cssClass.setEnabled(value);
		cssStyle.setEnabled(value);
		rotate.setEnabled(value);
		//viewCSS.setEnabled(value);
		labelBrief.setEnabled(value);
		labelFull.setEnabled(value);
	}// setTextInputEnabled()
	
	
	/** Add control bar icons */
	private void makeLayout()
	{
		// we add everything to our OWN mini-toolbar.
		// including all component-buttons of our parent toolbar.
		// this allows us to have a dual-row toolbar.
		// layout the buttons...in our own mini-toolbar
		JToolBar row1 = new JToolBar();
		row1.setMargin(new Insets(1,1,1,1));
		row1.setFloatable(false);
		row1.setRollover(true);
		
		// add parent components.
		int idx = 0;
		Component c = this.getComponentAtIndex(idx);
		while(c != null)
		{
			this.remove(c);
			row1.add(c);
			c = this.getComponentAtIndex(idx);
		}
		
		// spacer
		row1.addSeparator();
		
		// listener
		ButtonGroup bg = new ButtonGroup();
		
		// create toggle buttons.
		labelBrief = new JToggleButton("BriefLabel", true);
		labelBrief.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				placeText = " Click to add a brief label here.";
				currentMode = SET_SHORT_LABEL;
				// change view mode
				RenderCommand rc = mth.getDMR2().getRenderCommandFactory().createRCSetLabel(mapPanel.getMapRenderer(), MapRenderer2.VALUE_LABELS_BRIEF);
				mth.getDMR2().execRenderCommand(rc);
			}
		});
		bg.add(labelBrief);
		row1.add(labelBrief);
		
		labelFull = new JToggleButton("LongLabel", false);
		labelFull.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				placeText = " Click to add a long label here.";
				currentMode = SET_LONG_LABEL;
				// change view mode
				RenderCommand rc = mth.getDMR2().getRenderCommandFactory().createRCSetLabel(mapPanel.getMapRenderer(), MapRenderer2.VALUE_LABELS_FULL);
				mth.getDMR2().execRenderCommand(rc);
			}
		});
		bg.add(labelFull);
		row1.add(labelFull);
		
		placeUnit = new JToggleButton("Unit", false);
		placeUnit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				placeText = " Click to set the unit position.";
				currentMode = SET_UNIT;
			}
		});
		bg.add(placeUnit);
		row1.add(placeUnit);
		
		placeDislodgedUnit = new JToggleButton("Dislodged", false);
		placeDislodgedUnit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				placeText = " Click to set the dislodged unit position.";
				currentMode = SET_DISLODGED_UNIT;
			}
		});
		bg.add(placeDislodgedUnit);
		row1.add(placeDislodgedUnit);
		
		placeSC = new JToggleButton("SC", false);
		placeSC.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				placeText = " Click to set the supply center position.";
				currentMode = SET_SC;
			}
		});
		bg.add(placeSC);
		row1.add(placeSC);
		
		row1.addSeparator();
		coastGroup = new ButtonGroup();
		
		cMV = new JToggleButton("[Land]", false);
		cMV.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				currentCoast = Coast.LAND;
			}
		});
		coastGroup.add(cMV);
		row1.add(cMV);
		
		cN = new JToggleButton("[N]", false);
		cN.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				currentCoast = Coast.NORTH;
			}
		});
		coastGroup.add(cN);
		row1.add(cN);
		
		cS = new JToggleButton("[S]", false);
		cS.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				currentCoast = Coast.SOUTH;
			}
		});
		coastGroup.add(cS);
		row1.add(cS);
		
		cW = new JToggleButton("[W]", false);
		cW.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				currentCoast = Coast.WEST;
			}
		});
		coastGroup.add(cW);
		row1.add(cW);
		
		cE = new JToggleButton("[E]", false);
		cE.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				currentCoast = Coast.EAST;
			}
		});
		coastGroup.add(cE);
		row1.add(cE);
		
		row1.addSeparator();
		
		
		// create row2
		// 
		JToolBar row2 = new JToolBar();
		row2.setMargin(new Insets(1,1,1,1));
		row2.setFloatable(false);
		row2.setRollover(true);
		
		// row2 components
		row2.add(new JLabel("CSS class: "));
		
		cssClass = new JTextField("", 16);
		row2.add(cssClass);
		
		row2.addSeparator(new Dimension(15,10));
		row2.add(new JLabel("CSS style(s) (optional): "));
		
		cssStyle = new JTextField("",20);
		row2.add(cssStyle);
		row2.addSeparator(new Dimension(15,10));
		
		row2.add(new JLabel("Angle: "));
		rotate = new JTextField("", 4);
		row2.add(rotate);
		
		//viewCSS = new JButton(" View CSS ");
		//row2.add(viewCSS);
		
		// set initial state (via a 'click')
		placeSC.doClick();
		
		// create panels
		JPanel subPanel = new JPanel(new BorderLayout());
		subPanel.setBorder(new EmptyBorder(5,5,5,5));
		subPanel.add(row1, BorderLayout.NORTH);
		subPanel.add(row2, BorderLayout.SOUTH);
		add(subPanel);
	}// makeLayout()
	
}// class MTCBar




