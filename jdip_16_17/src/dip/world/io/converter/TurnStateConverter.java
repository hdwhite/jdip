//
//  @(#)TurnStateConverter.java		9/2004
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
package dip.world.io.converter;

import dip.world.io.XMLSerializer;  

import dip.world.*;
import dip.order.*;
import dip.order.result.*;
import dip.process.RetreatChecker;
import dip.process.Adjustment;
import dip.process.Adjustment.AdjustmentInfo;
import dip.process.Adjustment.AdjustmentInfoMap;
import dip.order.Orderable;

import dip.misc.Log;

import java.util.*;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;

/**
*	XStream Converter
*/
public class TurnStateConverter implements Converter
{
	private final ClassMapper cm;
	private final Comparator comparator;
	
	public TurnStateConverter(ClassMapper cm)
	{
		this.cm = cm;
		cm.alias("turn", TurnState.class, TurnState.class);
		comparator = new OrderResult.SubstitutedFirstComparator();
	}// TurnStateConverter()
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final TurnState ts = (TurnState) source;
		final List results = new ArrayList(ts.getResultList());
		final List powerList =  ts.getWorld().getMap().getPowerList();

		// clear order-map identities 
		// (we shouldn't be holding order references beyond 1 turn)
		xs.getMarshallingOrderMap().clear();
		
		// workaround: loaded games do not have world() set for each
		// turnstate, when loaded via Java serialization
		// TODO: this can probably be removed once XML serialization is proper.
		if(ts.getWorld() == null)
		{
			ts.setWorld(xs.getWorld());
		}
		
		hsw.addAttribute("season", ts.getPhase().getSeasonType().toString());
		hsw.addAttribute("phase",  ts.getPhase().getPhaseType().toString());
		hsw.addAttribute("year",  ts.getPhase().getYearType().toString());
		hsw.addAttribute("resolved", String.valueOf(ts.isResolved()));
		
		if(ts.isEnded())
		{
			hsw.addAttribute("ended", String.valueOf(ts.isEnded()));
		}
			
			// positions
			xs.lookupAndWriteNode(ts.getPosition(), cm, hsw, context);
			
			// adjustments
			// (only in ADJUSTMENT phase)
			if(Phase.PhaseType.ADJUSTMENT.equals(ts.getPhase().getPhaseType()))
			{
				AdjustmentInfoMap aim = Adjustment.getAdjustmentInfo(ts,
					xs.getWorld().getRuleOptions(), powerList);
				
				xs.lookupAndWriteNode(aim, cm, hsw, context);
			}
			else
			{
				// empty node, otherwise
				hsw.startNode("adjustments");
				hsw.endNode();
			}
			
			// orders
			hsw.startNode("orders");
			Iterator iter = powerList.iterator();
			while(iter.hasNext());
			{
				final Power power = (Power) iter.next();
				List orders = new ArrayList(ts.getOrders(power));
				if(!orders.isEmpty())
				{
					hsw.startNode("orderSet");
					hsw.addAttribute("power", xs.toString(power));
					
					Iterator orderIter = orders.iterator();
					while(orderIter.hasNext())
					{
						final Orderable order = (Orderable) orderIter.next();
						xs.writeOrder(order, hsw, context);
					}
					
					hsw.endNode();
				}
			}
			hsw.endNode();
			
			
			// we MUST sort results, to ensure that SubstitutedResults always
			// occur before all other results, since other OrderResults may
			// reference orders first defined in a SubstitutedResult
			hsw.startNode("results");
			Collections.sort(results, comparator);
			iter = results.iterator();
			while(iter.hasNext())
			{
				Result result = (Result) iter.next();
				xs.lookupAndWriteNode(result, cm, hsw, context);
			}
			hsw.endNode();
			
			
			hsw.startNode("dislodgedUnits");
			if(Phase.PhaseType.MOVEMENT.equals(ts.getPhase().getPhaseType()))
			{
				iter = results.iterator();
				while(iter.hasNext())
				{
					RetreatChecker rc = null;
					
					Object obj = iter.next();
					if(obj instanceof OrderResult)
					{
						OrderResult ordRes = (OrderResult) obj;
						if(OrderResult.ResultType.DISLODGED.equals(ordRes.getResultType()))
						{
							if(rc == null)
							{
								rc = new RetreatChecker(ts);
							}
							
							final Location src = ordRes.getOrder().getSource();
							final Location[] retreatLocs = rc.getValidLocations(src);
							
							hsw.startNode("dislodged");
							hsw.addAttribute("location", 
								xs.toString( src ) );
							hsw.addAttribute("power",
								xs.toString( ordRes.getOrder().getPower() ) );
							hsw.addAttribute("unitType",
								xs.toString( ordRes.getOrder().getSourceUnitType() ) );
							hsw.addAttribute("retreats",
								xs.toString( retreatLocs ) );
							hsw.endNode();
						}
					}
				}
			}
			hsw.endNode();
			
			hsw.startNode("powerInfo");
			addEliminatedPowers(ts, hsw, xs);
			hsw.endNode();
		
		xs.incrementTurnNumber();
	}// marshal()
	
	public boolean canConvert(Class type)
	{
		return type.equals(TurnState.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		
		// clear order map
		xs.getUnmarshallingOrderMap().clear();
		
		// create TurnState, set Resolution status
		final Phase tsPhase = new Phase(
			Phase.SeasonType.parse(reader.getAttribute("season")),
			xs.getInt(reader.getAttribute("year"), 0),
			Phase.PhaseType.parse(reader.getAttribute("phase")) );
			
		final TurnState ts = new TurnState(tsPhase);
		
		String t = reader.getAttribute("resolved");
		boolean v = Boolean.getBoolean(t);
		ts.setResolved(Boolean.valueOf(reader.getAttribute("resolved")).booleanValue());
		ts.setEnded(Boolean.valueOf(reader.getAttribute("ended")).booleanValue());
		ts.setWorld(xs.getWorld());
		xs.setCurrentTurnState(ts);
		
		boolean posSet = false;
		
		while(reader.hasMoreChildren())
		{
			reader.moveDown();
			final String nodeName = reader.getNodeName();
			
			if("powerInfo".equals(nodeName))
			{
				assert (posSet);
				
				while(reader.hasMoreChildren())
				{
					reader.moveDown();
					
					final String node = reader.getNodeName();
					if("eliminatedPowers".equals(node))
					{
						List powers = xs.getPowers(reader.getAttribute("powers"));
						Iterator iter = powers.iterator();
						while(iter.hasNext())
						{
							ts.getPosition().setEliminated((Power) iter.next(), true);
						}
					}
					else
					{
						Log.println("(powerinfo) Unrecognized element: ", node);
					}
					
					reader.moveUp();
				}
			}
			else if("dislodgedUnits".equals(nodeName))
			{
				// do nothing; we don't use the adjustment info.
				// The RetreatChecker Class and results take care of this
			}
			else if("results".equals(nodeName))
			{
				parseResults(ts, xs, reader, context);
			}
			else if("orders".equals(nodeName))
			{
				parseOrders(ts, xs, reader, context);
			}
			else if("adjustments".equals(nodeName))
			{
				// do nothing; we don't use the adjustment info.
				// The Adjustment Class and results take care of this
			}
			else
			{
				final Class cls = cm.lookupType(nodeName);
				final Object obj = context.convertAnother(context, cls);
				
				if(cls.equals(Position.class))
				{
					ts.setPosition((Position) obj);
					posSet = true;
				}
			}
			reader.moveUp();
		}
		
		// so that we throw an NPE if something is using this when it shouldn't be
		xs.setCurrentTurnState(null);
		
		return ts;
	}// unmarshal()
	
	private void addEliminatedPowers(TurnState ts, 
		HierarchicalStreamWriter hsw, XMLSerializer xs)
	{
		StringBuffer sb = new StringBuffer(64);
		Iterator iter = xs.getMap().getPowerList().iterator();
		while(iter.hasNext())
		{
			final Power power = (Power) iter.next();
			if(ts.getPosition().isEliminated(power))
			{
				if(sb.length() > 0)
				{
					sb.append(' ');	// not the first. separate with a space.
				}
				sb.append( xs.toString(power) );
			}
		}
		
		
		if(sb.length() > 0)
		{
			hsw.startNode("eliminatedPowers");
			hsw.addAttribute("powers", sb.toString());
			hsw.endNode();				
		}
	}// addEliminatedPowers()
	
	private void parseOrders(TurnState ts, XMLSerializer xs, 
		HierarchicalStreamReader reader, UnmarshallingContext context)
	{
		while(reader.hasMoreChildren())
		{
			reader.moveDown();
			
			String nodeName = reader.getNodeName();
			if("orderSet".equals(nodeName))
			{
				List orderList = new ArrayList(32);
				
				final Power power = xs.getPower(reader.getAttribute("power"));
				if(power == null) { throw new ConversionException("null power"); }
				
				while(reader.hasMoreChildren())
				{
					reader.moveDown();
					nodeName = reader.getNodeName();
					Orderable order = (Orderable) context.convertAnother(context, Orderable.class);
					orderList.add(order);
					reader.moveUp();
				}
				
				ts.setOrders(power, orderList);
			}
			else
			{
				Log.println("(orderSet) unrecognized element: ", nodeName);
			}
			
			reader.moveUp();
		}
	}// parseOrders()
	
	private void parseResults(TurnState ts, XMLSerializer xs, 
		HierarchicalStreamReader reader, UnmarshallingContext context)
	{
		List resultList = new ArrayList(64);
		
		while(reader.hasMoreChildren())
		{
			reader.moveDown();
			final Class cls = cm.lookupType(reader.getNodeName());
			final Result result = (Result) context.convertAnother(context, cls);
			resultList.add(result);
			reader.moveUp();
		}
		
		ts.setResultList(resultList);
	}// parseResults()
}// class TurnStateConverter
