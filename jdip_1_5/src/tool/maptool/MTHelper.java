//
//  @(#)MTHelper.java		6/2003
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
package maptool;

import dip.gui.*;
import dip.gui.dialog.TextViewer;
import dip.gui.map.*;
import dip.gui.map.RenderCommandFactory.RenderCommand;
import dip.gui.map.MapMetadata.InfoEntry;
import dip.misc.*;
import dip.world.*;

import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;
import java.util.*;
import java.awt.event.*;
import java.net.URI;
import java.text.*;

import org.w3c.dom.*;                  

import org.apache.batik.swing.JSVGCanvas; 
import org.w3c.dom.events.MouseEvent;
import org.apache.batik.dom.events.DOMKeyEvent;

import org.apache.batik.util.CSSConstants;
import org.apache.batik.*;
import org.apache.batik.dom.*;
import org.apache.batik.util.*;
import org.w3c.dom.svg.*;
import org.apache.batik.dom.svg.*;
import org.apache.batik.swing.svg.*;
import org.apache.batik.util.*;
import org.apache.batik.bridge.*;

/**
*	Outputs placement, label data properly XML (SVG) formatted.
*	
*	currently outputs to stdout. Will output to textviewer in future
*	for easy cut-and-paste
*	
*	
*/

public class MTHelper
{
	final static Point2D.Float ORIGIN = new Point2D.Float(0,0);
	final static DecimalFormat df = new DecimalFormat("0.#");
	
	final java.util.Map textMap;
	final ClientFrame clientFrame;
	final World world;
	final dip.world.Map worldMap;
	final Power[] allPowers;
	final Province[] allProvs;
	final MapMetadata mmd;
	final MapPanel mapPanel;
	final Position position;
	final TurnState turnState;
	final SVGDocument doc;
	final DefaultMapRenderer2 dmr2;
	
	
	/** MTOutput constructor */
	public MTHelper(ClientFrame cf)
	{
		clientFrame = cf;
		world = cf.getWorld();
		worldMap = world.getMap();
		allPowers = worldMap.getPowers();
		allProvs = worldMap.getProvinces();
		mapPanel = cf.getMapPanel();
		mmd = mapPanel.getMapRenderer().getMapMetadata();
		turnState = cf.getTurnState();
		position = turnState.getPosition();
		doc = mapPanel.getSVGDocument();
		
		textMap = new HashMap(119);
		dmr2 = (DefaultMapRenderer2) mapPanel.getMapRenderer();
	}// MTHelper()
	
	
	/** Create all units, dislodged units for a position */
	public void setAllUnits()
	{
		long time = System.currentTimeMillis();
		Log.printTimed(time, "MTHelper::setAllUnits() start");
		for(int i=0; i<allProvs.length; i++)
		{
			Unit unit = null;
			Unit unit2 = null;
			
			if(allProvs[i].isSea())
			{
				unit = new Unit(allPowers[0], Unit.Type.FLEET);
				unit2 = new Unit(allPowers[0], Unit.Type.FLEET);
				unit.setCoast(Coast.SEA);
				unit2.setCoast(Coast.SEA);
			}
			else
			{
				unit = new Unit(allPowers[0], Unit.Type.ARMY);
				unit2 = new Unit(allPowers[0], Unit.Type.ARMY);
				unit.setCoast(Coast.LAND);
				unit2.setCoast(Coast.LAND);
			}
			
			position.setUnit(allProvs[i], unit);
			position.setDislodgedUnit(allProvs[i], unit2);
		}
		Log.printTimed(time, "MTHelper::setAllUnits() complete");
	}// setAllUnits()
	
	/** Get the DMR2 renderer */
	public DefaultMapRenderer2 getDMR2()
	{
		return dmr2;
	}
	
	
	/** Re-render a province; forces update of unit/dislodged unit info */
	public void renderProvince(Province p)
	{
		MapRenderer2 mr2 = mapPanel.getMapRenderer();
		RenderCommand rc = 
			((DMR2RenderCommandFactory) mr2.getRenderCommandFactory()).createRCRenderProvinceForced(mr2, p);
		mr2.execRenderCommand(rc);
	}// renderProvince()
	
	
	/** Re-render all provinces */
	public void renderAllProvinces()                            
	{
		MapRenderer2 mr2 = mapPanel.getMapRenderer();
		RenderCommand rc = mr2.getRenderCommandFactory().createRCRenderAll(mr2);
		mr2.execRenderCommand(rc);
	}// renderAllProvinces()
	
	
	/** 
	*	Adds, deletes, or updates a unit/dislodged unit position
	*	re-renders province after changing. DOES NOT handle
	*	province with multiple coasts.
	*/
	public void changeUnit(Province p, float x, float y, boolean dislodged, boolean delete)
	{
		InfoEntry ie = mmd.getInfoEntry(p);			// old infoentry
		InfoEntry nie = null;						// new infoentry
		
		if(p == null)
		{
			return;
		}
		
		if(delete)
		{
			nie = new InfoEntry(ORIGIN, ORIGIN, ORIGIN);
		}
		else
		{
			// note: doesn't matter what coast we use for below
			// but it's safe to use the LAND coast, since it will exist
			// even if a province is multicoastal
			//
			if(dislodged)
			{
				nie = new InfoEntry(
						ie.getUnitPt(Coast.LAND),
						new Point2D.Float(x,y),
						ie.getSCPt()
				);
			}
			else
			{
				nie = new InfoEntry(
						new Point2D.Float(x,y),
						ie.getDislodgedUnitPt(Coast.LAND),
						ie.getSCPt()
				);
				
			}
		}
		
		mmd.setInfoEntry(p, nie);
		renderProvince(p);
	}// changeUnit()
	
	
	/**
	*	Change a unit, but use the given coast. This is only intended
	*	for multi-coastal provinces.
	*/
	public void changeUnit(Province p, Coast coast, float x, float y, boolean dislodged, boolean delete)
	{
		if(p == null || coast == null)
		{
			throw new IllegalArgumentException();
		}
		
		// the coast mappings are mutable, unlike the other
		// non-mapped featuers of InfoEntry.
		//
		InfoEntry ie = mmd.getInfoEntry(p);
		
		// determine the point. If delete, point == origin.
		Point2D.Float pt = (delete) ? ORIGIN : new Point2D.Float(x,y);
		
		// get the unit
		Unit unit = null;
		
		// set the info
		if(dislodged)
		{
			ie.addCoastMapping(coast, 
				ie.getUnitPt(coast),
				pt );
				
			unit = position.getDislodgedUnit(p);
		}
		else
		{
			ie.addCoastMapping(coast, 
				pt,
				ie.getDislodgedUnitPt(coast) );
				
			unit = position.getUnit(p);
		}
		
		// update unit coast
		unit.setCoast(coast);
		
		renderProvince(p);
	}// changeUnit()	
	
	
	/** 
	*	Changes the position of a supply center.
	*	Updates the map.
	*/
	public void changeSC(Province p, float x, float y)
	{
		InfoEntry ie = mmd.getInfoEntry(p);			// old infoentry
		InfoEntry nie = null;						// new infoentry
		
		if(p == null)
		{
			return;
		}
		
		nie = new InfoEntry(
				ie.getUnitPt(Coast.LAND),
				ie.getDislodgedUnitPt(Coast.LAND),
				new Point2D.Float(x,y)
		);
		
		mmd.setInfoEntry(p, nie);
		
		MapRenderer2 mr2 = mapPanel.getMapRenderer();
		RenderCommand rc = 
			((DMR2RenderCommandFactory) mr2.getRenderCommandFactory()).createRCUpdateSC(mr2, p);
		mr2.execRenderCommand(rc);
	}// changeSC()
	
	
	
	
	/** Format a float; restrict to (at most) one decimal place */
	public static String formatFloat(double f)
	{
		return df.format(f);
	}// formatFloat()
	
	
}// class MTHelper




