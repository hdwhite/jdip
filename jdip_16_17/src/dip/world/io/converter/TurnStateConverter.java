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

import java.util.*;

import com.thoughtworks.xstream.converters.Converter;
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
		final Power[] powers = xs.getMap().getPowers();
		final List results = new ArrayList(ts.getResultList());
		
		// workaround: loaded games do not have world() set for each
		// turnstate, when loaded via Java serialization
		// TODO: this can probably be removed once XML serialization is proper.
		if(ts.getWorld() == null)
		{
			ts.setWorld(xs.getWorld());
		}
		
		// clear order-map identities 
		// (we shouldn't be holding order references beyond 1 turn)
		xs.getOrderMap().clear();
		
		hsw.addAttribute("season", ts.getPhase().getSeasonType().toString());
		hsw.addAttribute("phase",  ts.getPhase().getPhaseType().toString());
		hsw.addAttribute("year",  ts.getPhase().getYearType().toString());
		hsw.addAttribute("resolved", String.valueOf(ts.isResolved()));
			
			hsw.startNode("initialPositions");
			context.convertAnother( ts.getPosition() );
			hsw.endNode();
			
			hsw.startNode("adjustments");
			if(Phase.PhaseType.ADJUSTMENT.equals(ts.getPhase().getPhaseType()))
			{
				AdjustmentInfoMap aim = Adjustment.getAdjustmentInfo(ts,
					xs.getWorld().getRuleOptions(), powers);
					
				for(int i=0; i<powers.length; i++)
				{
					AdjustmentInfo ai = aim.get(powers[i]);
					
					hsw.startNode("adjustment");
					hsw.addAttribute("power", xs.toString(powers[i]));
					hsw.addAttribute("scCount", String.valueOf(ai.getSupplyCenterCount()));
					hsw.addAttribute("unitCount", String.valueOf(ai.getUnitCount()));
					hsw.addAttribute("adj", String.valueOf(ai.getAdjustmentAmount()));
					hsw.endNode();
				}
			}
			hsw.endNode();
			
			
			hsw.startNode("orders");
			for(int i=0; i<powers.length; i++)
			{
				List orders = new ArrayList(ts.getOrders(powers[i]));
				if(!orders.isEmpty())
				{
					hsw.startNode("orderSet");
					hsw.addAttribute("power", xs.toString(powers[i]));
					
					Iterator iter = orders.iterator();
					while(iter.hasNext())
					{
						final Orderable order = (Orderable) iter.next();
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
			Iterator iter = results.iterator();
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
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(TurnState.class);
	}
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		return null;
	}
	
	private void addEliminatedPowers(TurnState ts, 
		HierarchicalStreamWriter hsw, XMLSerializer xs)
	{
		StringBuffer sb = new StringBuffer(64);
		final Power[] powers = xs.getMap().getPowers();
		for(int i=0; i<powers.length; i++)
		{
			if(ts.getPosition().isEliminated(powers[i]))
			{
				if(sb.length() > 0)
				{
					sb.append(' ');	// not the first. separate with a space.
				}
				sb.append( xs.toString(powers[i]) );
			}
		}
		
		
		if(sb.length() > 0)
		{
			hsw.startNode("eliminatedPowers");
			hsw.addAttribute("powers", sb.toString());
			hsw.endNode();				
		}
	}// addEliminatedPowers()
	
}// class TurnStateConverter
