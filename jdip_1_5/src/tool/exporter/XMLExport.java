//
//  @(#)XMLExport.java		9/2003
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
package exporter; 

import dip.tool.*;
import dip.gui.*;
import dip.gui.map.*;
import dip.misc.*;
import dip.world.*;
import dip.process.*;
import dip.order.*;
import dip.order.result.*;

import javax.swing.*;
import java.awt.event.*;
import java.net.URI;
import java.io.*;
import java.util.*;


/**
*	A VERY simple XML export facility.
*	<p>
*	No attempt has been made for performance. This is a very simple utility.
*
*/
class XMLExport
{
	// xml template constants
	//
	private static final String HEADER = "<?xml version=\"1.0\" ?>";
	private static final String TEMPLATE_MAIN = 
		"<jdip game=\"{0}\" year=\"{1}\" season=\"{2}\">"+
		"<moves>\n"+
		"{3}"+						// move and other orders
		"</moves>\n"+
		"<adjustments>\n"+
		"{4}"+						// build/remove orders (if any)
		"</adjustments>\n"+
		"<sc>\n"+
		"{5}"+						// done
		"</sc>"+
		"</jdip>\n";
		
	// for moves, adjustmenets
	private static final String TEMPLATE_POWER = 
	"\t<country name=\"{0}\">\n{1}</country>\n";
		
	private static final String TEMPLATE_MOVE__RESULT_SUCCESS = 
	"\t\t<move result=\"success\" retreat=\"{0}\" disband=\"{1}\">\n\t\t\t{2}\n\t\t</move>\n";
		
	private static final String TEMPLATE_MOVE_RESULT_FAILURE = 
	"\t\t<move result=\"failure\">\n\t\t\t{0}\n\t\t</move>\n";
		
	private static final String TEMPLATE_ADJ_BUILD = 
	"\t\t<build type=\"{0}\">\n\t\t\t{1}\n\t\t</build>\n";
		
	private static final String TEMPLATE_ADJ_DISBAND = 
	"\t\t<disband>\n\t\t\t{0}\n\t\t</disband>\n";
		
	private static final String SC_POWER = 
	"\t<country name=\"{0}\" total=\"{1}\" diff=\"{2}\">\n{2}</country>\n";
	
	private static final String SC_LOCATION = 
	"\t\t<sc name=\"{0}\" diff=\"{1}\"/>\n";	// diff: gained or lost
	
	
	
	/** Export the given TurnState as very simple XML */
	public static String export(TurnState ts, String gameName)
	{
		/*
		// orders (if any; otherwise empty)
		String orders = getOrders(ts);
		
		// adjustments (if any; otherwise empty)
		String adjustments = "";
		if(ts.getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT)
		{
			adjustments = getAdjustments(ts);
		}
		
		// retreats (if any; otherwise empty)
		String retreats = "";
		if(ts.getPhase().getPhaseType() == Phase.PhaseType.RETREAT)
		{
			retreats = getSC(ts);
		}
		
		// format all of the above into the main template
		//
		String[] templateArgs = new String[]
		{
			// jdip attributes
			gameName,
			String.valueOf(ts.getPhase().getYear()),
			ts.getPhase().getSeasonType().toString().substring(0,1),
			orders,
			adjustments,
			sc
		};
		
		// we've already converted everything to XML-compatible
		StringBuffer sb = new StringBuffer(4096);
		sb.append(HEADER);
		sb.append(Utils.format(TEMPLATE_MAIN, templateArgs));
		return sb.toString();
		*/
		return "";
	}// export()
	
	
	
	/** Gets the Positions (units and SC) */
	private static String getPositions(TurnState ts)
	{
		/*
		StringBuffer sb = new StringBuffer(4096);
		Position pos = ts.getPosition();
		
		final Province[] allProvs = pos.getProvinces();
		for(int i=0; i<allProvs.length; i++)
		{
			Province p = allProvs[i];
			if(pos.hasUnit(p))
			{
				Unit u = pos.getUnit(p);
				assert (u != null);
				sb.append( format(TEMPLATE_UNIT, 
					u.getType().getFullName(),
					p.getShortName(),
					u.getCoast().getAbbreviation(),
					"false"
					));
			}
			
			if(pos.hasDislodgedUnit(p))
			{
				Unit u = pos.getDislodgedUnit(p);
				assert (u != null);
				sb.append( format(TEMPLATE_UNIT, 
					u.getType().getFullName(),
					p.getShortName(),
					u.getCoast().getAbbreviation(),
					"true"
					));
			}
		}
		
		
		for(int i=0; i<allProvs.length; i++)
		{
			Province p = allProvs[i];
			if(p.hasSupplyCenter())
			{
				Power power = pos.getSupplyCenterOwner(p);
				String powerName = (power == null) ? NONE : power.toString();
				sb.append( format(TEMPLATE_SC, powerName) );
			}
		}
		
		return sb.toString();
		*/
		return "";
	}// getPositions()
	
	
	/** Gets the Orders, and (if appropriate) results */
	private static String getOrders(TurnState ts)
	{
		/*
		// this is somewhat complicated if we're resolved
		// we need to get results and match them to orders
		// (similar to ResultWriter.java)
		//
		// we then create a map: key:order, value:ArrayList (of results)
		//
		List orders = ts.getAllOrders();
		List results = ts.getResultList();
		
		// init the order-result map
		HashMap orMap = new HashMap(53);
		Iterator iter = orders.iterator();
		while(iter.hasNext())
		{
			orMap.put(iter.next(), new ArrayList());
		}
		
		// map the results. Unmapped (un-matched) results go into unmappedResults list.
		List unmappedResults = new ArrayList();
		iter = results.iterator();
		while(iter.hasNext())
		{
			Result res = (Result) iter.next();
			if(res instanceof OrderResult)
			{
				OrderResult or = (OrderResult) res;
				List list = (List) orMap.get( or.getOrder() );
				if(list != null)
				{
					list.add(or);
				}
				else
				{
					// shouldn't happen...
					unmappedResults.add(res);
				}
			}
			else
			{
				unmappedResults.add(res);
			}
		}
		
		// output buffer
		StringBuffer sb = new StringBuffer(4096);
		
		// first, write out orders and their results.
		iter = orders.iterator();
		while(iter.hasNext())
		{
			Orderable order = (Orderable) iter.next();
			List list = (List) orMap.get( order );
			
			sb.append( format(TEMPLATE_ORDER_OPEN, 
				order.getPower().toString(),
				order.getSource().getProvince().getShortName(),
				order.toBriefString()) );
			
			Iterator iter2 = list.iterator();
			while(iter2.hasNext())
			{
				OrderResult or = (OrderResult) iter2.next();
				sb.append( format(TEMPLATE_ORDER_RESULT, 
					or.getResultType().toString(),
					or.getMessage()) );
			}
			
			sb.append(TEMPLATE_ORDER_CLOSE);
		}
		
		// then, write out unmapped results
		iter = unmappedResults.iterator();
		while(iter.hasNext())
		{
			Result res = (Result) iter.next();
			
			sb.append( format(TEMPLATE_GENERAL_RESULT, 
				(res.getPower() == null) ? NONE : res.getPower().toString(),
				res.getMessage()) );
		}
		
		return sb.toString();
		*/
		return "";
	}// getOrdersAndResults()
	
	
	/** Gets the retreating units */
	private static String getRetreats(TurnState ts)
	{
		/*
		RetreatChecker rc = new RetreatChecker(ts);
		StringBuffer sb = new StringBuffer(1024);
		Position pos = ts.getPosition();
		
		final Province[] allProvs = pos.getProvinces();
		for(int i=0; i<allProvs.length; i++)
		{
			Province p = allProvs[i];
			if(pos.hasDislodgedUnit(p))
			{
				Unit u = pos.getDislodgedUnit(p);
				Location loc = new Location(p, u.getCoast());
				Location[] retreatLocs = rc.getValidLocations(loc);
				
				StringBuffer locations = new StringBuffer(128);
				for(int ri=0; ri<retreatLocs.length; ri++)
				{
					retreatLocs[ri].appendBrief(locations);
					if(ri < (retreatLocs.length - 1))
					{
						locations.append(", ");
					}
				}
				
				sb.append( format(TEMPLATE_RETREAT, 
					u.getType().getFullName(),
					p.getShortName(),
					(retreatLocs.length == 0) ? NONE : locations.toString()) );
			}
		}
		
		return sb.toString();
		*/
		return "";
	}// getRetreats()
	
	
	/** Gets the SC */
	private static String getSC(TurnState ts)
	{
		/*
		StringBuffer sb = new StringBuffer(4096);
		TurnState priorTS = ts.getWorld().getPreviousTurnState();
		priorTS = (priorTS == null) ? ts : priorTS;
		
		final Power[] allPowers = ts.getWorld().getMap().getPowers();
		Adjustment.AdjustmentInfoMap adjMap = 
				Adjustment.getAdjustmentInfo(ts, 
					ts.getWorld().getRuleOptions(), allPowers);
		
		final Position newPos = ts.getPosition();
		final Positoin oldPos = priorTS.getPosition();
		
		for(int i=0; i<allPowers.length; i++)
		{
			StringBuffer scList = new StringBuffer(1024);
			
			// get all locations; compare to previous, to determine:
			// 'lost' 'gained' 'same'
			//
			Power power = allPowers[i];
			Province[] newSC = newPos.getOwnedSupplyCenters(power);
			Province[] oldSC = oldPos.getOwnedSupplyCenters(power);   
			
			// iterate through newSC; if in oldSC, it's "nochange"
			// else "gained"
			for(int j=0; j<newSC.length; j++)
			{
				boolean found = false;
				for(int k=0; k<oldSC.length; k++)
				{
					if(newSC[j] == oldSC[k])
					{
						found = true;
						break;
					}
				}
				
				String diff = (found) ? "nochange" : "gained";
				scList.append(format(SC_LOCATION, newSC[j].getShortName(), diff));
			}
			
			
			// iterate through oldSC; if in newSC, ignore; otherwise, it's "lost"
			for(int j=0; j<oldSC.length; j++)
			{
				boolean found = false;
				for(int k=0; k<oldSC.length; k++)
				{
					if(newSC[j] == oldSC[k])
					{
						break;
					}
				}
				
				if(!found)
				{
					scList.append(format(SC_LOCATION, newSC[j].getShortName(), "lost"));
				}
			}
			
			// create encapsulating country
			Adjustment.AdjustmentInfo adjInfo = adjMap.get(allPowers[i]);
			sb.append(format(SC_POWER,
				power.getName(),
				String.valueOf(adjInfo.getSupplyCenterCount()),
				String.valueOf(adjInfo.getAdjustmentAmount()),
				scList.toString()
				));
		}
		
 		return sb.toString();
		*/
		return "";
	}// getSC()
	
	
	
	/** Convenience format() */
	private static String format(final String template, String arg0)
	{
		return format(template, new String[] {arg0});
	}
	
	private static String format(final String template, String arg0, String arg1)
	{
		return format(template, new String[] {arg0, arg1});
	}
	
	private static String format(final String template, String arg0, String arg1, String arg2)
	{
		return format(template, new String[] {arg0, arg1, arg2});
	}
	
	private static String format(final String template, String arg0, String arg1, String arg2, String arg3)
	{
		return format(template, new String[] {arg0, arg1, arg2, arg3});
	}
	
	/**
	*	Formats args into template, after making sure args
	*	have been converted with toXMLString()
	*/
	private static String format(final String template, final String[] args)
	{
		final Object[] templateArgs = new Object[args.length];
		for(int i=0; i<templateArgs.length; i++)
		{
			templateArgs[i] = toXMLString(args[i]);
		}
		
		return Utils.format(template, templateArgs);		
	}// format()
	
	
	/**
	*	Converts a string to XML compatible string; expands
	*	entities like &amp; to &amp;amp; and quotes (double
	*	and single) as well as brackets.
	*/
	private static String toXMLString(CharSequence in)
	{
		StringBuffer sb = new StringBuffer(in.toString());
		replaceAll(sb, "&", "&amp;");	// this must come first!!
		replaceAll(sb, "<", "&lt;");
		replaceAll(sb, ">", "&gt;");
		replaceAll(sb, "\'", "&apos;");
		replaceAll(sb, "\"", "&quot;");
		return sb.toString();
	}// toXMLString()
	
	/** replace all instances of "search" with "replace" in StringBuffer sb */
	private static void replaceAll(StringBuffer sb, String search, String replace)
	{
		int start = sb.indexOf(search, 0);
		while(start != -1)
		{
			final int end = start + search.length();
			sb.replace(start, end, replace);
			
			// repeat search
			start = sb.indexOf(search, (start + replace.length()));
		}
	}// replaceAll()

	
}// class XMLExport


