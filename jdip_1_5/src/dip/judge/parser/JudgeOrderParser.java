//
//  @(#)JudgeOrderParser.java	1.00	6/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
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
package dip.judge.parser;

import dip.world.Phase.PhaseType;
import dip.world.Map;
import dip.world.Power;
import dip.order.result.Result;
import dip.misc.Utils;

import java.io.*;
import java.util.regex.*;
import java.util.*;


/**
*
*	Parses Move, Retreat, and Adjustment phase orders.
*	<p>
*	This assumes that orders never span lines.  "Pending" build orders are not recognized...yet
*
*	<p>
*	Move and Retreat orders: found by a valid power token (alphanumeric, '-', and '_') followed
*	by a colon (':'), and ending with a period or "*)". They are then parsed into orders/results.
*
*
*
*/
public class JudgeOrderParser
{
	// i18n
	private static final String WAIVED_BUILDS = "JOP.adjust.waived";
	private static final String UNUSABLE_WAIVED = "JOP.adjust.unusable.waived";
	private static final String UNUSABLE_PENDING = "JOP.adjust.unusable.pending";
	
	
	// regular expressions
	/** Parse Season and Year for these orders */
	public static final String SEASON_YEAR_REGEX = "(([0-9]+))"; // "(\\S+)\\s+of\\s+(([0-9]+))";
	
	/** Header for recognizing the start of game block */
	public static final String GAME_STARTING_HEADER = "(?i)you\\s+have\\s+been\\s+selected\\s+as";
	
	/** Recognize the starting position header */
	public static final String STARTING_POSITION_REGEX = "Starting\\s+position\\s+for\\s+(\\p{Alpha}+)\\s+of\\s+((\\p{Digit}+))";
	
	/** Header for recognizing the movement order block */
	public static final String MOVE_ORDER_HEADER = "(?i)movement\\s+results\\s+for";
	/** 
	*	Recognize the general format of a movement-phase order. 
	*	power-token: (order...).
	*/
	public static final String MOVE_ORDER_REGEX = "^\\s*[\\p{Alnum}\\-\\_]+:.+";
	
	
	/** Header for recognizing the retreat order block */
	public static final String RETREAT_ORDER_HEADER = "(?i)retreat\\s+results\\s+for";
	/** Header for recognizing the retreat order block for older versions of nJudge*/
	public static final String RETREAT_ORDER_HEADER_OLD = "(?i)retreat\\s+orders\\s+for";
	
	
	/** Header for recognizing the adjustment order block */
	public static final String ADJUSTMENT_ORDER_HEADER = "(?i)adjustment\\s+results\\s+for";
	/** Header for recognizing the adjustment order block for older versions of nJudge */
	public static final String ADJUSTMENT_ORDER_HEADER_OLD = "(?i)adjustment\\s+orders\\s+for";
	/** 
	*	Regex for parsing Adjustment orders, since they have to be rewritten for the order 
	*	parser (JudgeOrderParser) to understand them.
	*	<p>
	*	Capture groups: 1:power, 2:remove/build 3:army/fleet 4:location<br>
	*	Note: capture groups may be of mixed case.
	*/
	public static final String ADJUSTMENT_ORDER_REGEX= "(?i)^([\\p{Alnum}\\-\\_]+):.*(remove|build).*(army|fleet|wing).*(?:in\\sthe|in)\\s+(([^\\.]+))\\.$";
	
	/** Waived builds. Second capture group may be empty. */
	public static final String ADJUSTMENT_WAIVE_REGEX= "(?i)^([\\p{Alnum}\\-\\_]+):\\s+(\\d*)\\s+(?:build|builds)\\s+waived\\.$";
	
	/** Also covers unusable pending builds */
	public static final String ADJUSTMENT_UNUSABLE_WAIVE_REGEX= "(?i)^([\\p{Alnum}\\-\\_]+):\\s+(\\d+)\\s+unusable\\s+(?:build|builds)\\s+(waived|pending)\\.$";
	
	
	// instance variables
	private OrderInfo[] orderInfo = null;
	private PhaseType phaseType = null;
	private List results = null;
	private dip.world.Map map = null;
	
	// for testing only
	/*
	public static void main(String args[])
	throws IOException, PatternSyntaxException
	{
		// some crap at the beginning
		String in = 
		"sdkflaakdljf fdakjd slkjdfsa klfd\n"+
		"dslkafflddfskj fdakjd slkjdfsa klfd\n"+
		"kldsjfkdskajfdsak fdakjd slkjdfsa klfd\n"+
		"\n"+
		
		
		// for movement
		
		"Movement results for Spring of 1901.  (ferret.001)\n"+
		"\n"+
		"Austria: Army Budapest -> Serbia.\n"+
		"Austria: Army Vienna -> Galicia. (*bounce,  disloged*)\n"+
		"Austria: Fleet Trieste -> Albania.\n"+
		"\n"+
		"Russia: Fleet St. Petersburg (south coast) -> Gulf of Bothnia.\n"+
		"Russia: Fleet Bul (sc) -> NewProvince.\n"+
		"Russia: Fleet Ang/sc (sc) -> NewProvince. (*BOUNCE*)\n"+
		"\n"+
		"Turkey: Army Constantinople -> Bulgaria.\n"+
		"Turkey: Army Smyrna -> Constantinople. (*bounce*)\n"+
		"Turkey: Fleet Ankara -> Black Sea.\n"+
		"\n"+
		
		// for retreats
		"Retreat orders for Fall of 1902.  (ferret.006)\n"+
		"\n"+
		"France:    	Army  Belgium -> Burgundy.\n"+
		"Russia:    	Army  Warsaw -> Moscow.\n"+
		"Russia:    	Fleet Sweden -> Gulf of Bothnia.\n"+
		"\n"+
		
		
		// for adjustments
		"Adjustment orders for Winter of 1902.  (ferret.007)\n"+
		"\n"+
		"France:    	Removes the fleet in Brest.\n"+
		"Russia:    	Removes the fleet in the Gulf of Bothnia.\n"+
		"Turkey:    	Removes the fleet in Armenia.\n"+
		"Austria:   	Builds a fleet in Trieste.\n"+
		"Austria:   	Builds an army in Vienna.\n"+
		"Austria:   	Builds an army in Budapest.\n"+
		"England:   	Builds a fleet in Liverpool.\n"+
		"Germany:   	Builds an army in Berlin.\n"+
		"Germany:   	Builds an army in Munich.\n"+
		"Russia:    	1 unusable build pending.\n"+
		"Kenya:         3 unusable builds pending.\n"+
		"\n"+
		
		// yet another adjustment
		
		"Adjustment results for Winter of 1903.  (narwhal.011)\n"+
		"Austria:                  Removes the fleet in Greece.\n"+
		"England:                  Builds an army in London.\n"+
		"England:                  Build waived.\n"+
		"France:                   Builds an army in Paris.\n"+
		"France:                   Builds a fleet in Marseilles.\n"+
		"Germany:                  Removes the fleet in the Helgoland Bight.\n"+
		"Germany:                  Removes the army in Prussia.\n"+
		"Russia:                   Removes the army in Galicia.\n"+
		"Turkey:                   Builds a fleet in Smyrna.\n"+
		"Turkey:                   Builds an army in Ankara.\n"+
		"Turkey:                   1 unusable build waived.\n"+
		"\n"+			
		
		// ending text 1
		"The next phase of 'ferret' will be Movement for Fall of 1901.\n"+
		"The deadline for orders will be Tue Jan 22 2002 23:30:00 -0500.\n";
		
		// ending text 2;
		//"Ownership of supply centers:		\n";
		
		JudgeOrderParser op = new JudgeOrderParser(in, null);
		System.out.println("phase: "+op.getPhaseType());
		
		OrderInfo[] oi = op.getOrderInfo();
		System.out.println("# of orders: "+oi.length);
		for(int i=0; i<oi.length; i++)
		{
			System.out.println("  "+oi[i]);
		}
		
		// test positionderiver if we are in movement phase
		if(op.getPhaseType() == PhaseType.MOVEMENT)
		{
			System.out.println("MOVEMENT phase detected. Testing order position derivation.");
			System.out.println("");
			
			PositionDeriver pd = new PositionDeriver(oi);
			PositionParser.PositionInfo[] pi = pd.getPositionInfo();
			System.out.println("# of parsed positions: "+pi.length);
			
			for(int i=0; i<pi.length; i++)
			{
				System.out.println("  "+pi[i]);
			}
			
		}
		
		
	}// main()
	// end testing method
	*/
	
	
	
	public JudgeOrderParser(String input, dip.world.Map map)
	throws IOException, PatternSyntaxException
	{
		this.map = map;
		results = new LinkedList();
		parseInput(input);
	}// JudgeOrderParser
	
	
	/** Returns the phase of the processed orders. This is null when getOrderInfo() is zero-length. */
	public PhaseType getPhaseType()
	{
		return phaseType;
	}// getPhaseType()
	
	/** Returns the OrderInfo after parsing. This is never null, but may be a zero-length array. */
	public OrderInfo[] getOrderInfo()
	{
		return orderInfo;
	}// getOrderInfo()
	
	/** 
	*	Returns a list of results (from order processing). This usually indicates 
	*	parsing errors or orders that are not handled.
	*/
	public List getResults()
	{
		return results;
	}// getResults()
	
	
	/** Add a general result. Message cannot be null, but powerName can be.*/
	private void addGeneralResult(String powerName, String message)
	throws IOException
	{
		if(message == null)
		{
			throw new IllegalArgumentException();
		}
		
		Power power = null;
		if(powerName != null)
		{
			power = map.getClosestPower(powerName);
			if(power == null)
			{
				throw new IOException("Parse error: power "+powerName+" not identified.");
			}
		}
		
		results.add(new Result(power, message));
	}// addGeneralResult()
	
	
	/** Class that holds Order and Result text. */
	public static class OrderInfo
	{
		private final String order;
		private final String result;
		
		/** Creates an OrderInfo object, with a null order result. */
		public OrderInfo(String order)
		{
			this(order, null);
		}// OrderInfo()
		
		
		/** Creates an OrderInfo object, with the specified order text and result */
		public OrderInfo(String order, String result)
		{
			this.order = order;
			this.result = result;
		}// OrderInfo()
		
		/** Gets the text of an order.*/
		public String getOrderText()	{ return order; }
		/** Gets the text of an order result. This may be null. */
		public String getResultText()	{ return result; }
		
		
		/** For debugging only: may change format between versions. */
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append("OrderInfo[order=");
			sb.append(order);
			sb.append(",result=");
			sb.append(result);
			sb.append(']');
			return sb.toString();
		}// toString()
	}// OrderInfo()
	
	
	
	/**
	*	Looks for the header line. When the appropriate phase header is found, this method 
	*	sets the PhaseType and calls the phase-specific parser. This analyzes input line-by-line.
	*
	*/
	private void parseInput(String input)
	throws IOException, PatternSyntaxException
	{
		// search for header input. once found, shuttle all input to the appropriate 
		// handler type.
		Pattern hm = Pattern.compile(MOVE_ORDER_HEADER);
		Pattern hr = Pattern.compile(RETREAT_ORDER_HEADER);
		Pattern hr_o = Pattern.compile(RETREAT_ORDER_HEADER_OLD);
		Pattern ha = Pattern.compile(ADJUSTMENT_ORDER_HEADER);
		Pattern ha_o = Pattern.compile(ADJUSTMENT_ORDER_HEADER_OLD);
		
		// create List
		List oiList = new LinkedList();
		
		BufferedReader br = new BufferedReader(new StringReader(input));
		String line = ParserUtils.getNextLongLine(br);
		while(line != null)
		{
			Matcher m = hm.matcher(line);
			Matcher m_o;
			if(m.lookingAt())
			{
				phaseType = PhaseType.MOVEMENT;
				break;
			}
			
			m = hr.matcher(line);
			m_o = hr_o.matcher(line);
			if(m.lookingAt() || m_o.lookingAt())
			{
				phaseType = PhaseType.RETREAT;
				break;
			}
			
			m = ha.matcher(line);
			m_o = ha_o.matcher(line);
			if(m.lookingAt() || m_o.lookingAt())
			{
				phaseType = PhaseType.ADJUSTMENT;
				break;
			}
			
			line = ParserUtils.getNextLongLine(br);
		}
		
		// parse based on type
		if(phaseType == PhaseType.ADJUSTMENT)
		{
			adjustmentParser(br, oiList);
		}
		else if(phaseType == PhaseType.MOVEMENT || phaseType == PhaseType.RETREAT)
		{
			moveAndRetreatParser(br, oiList);
		}
		
		// cleanup
		br.close();
		
		// create array
		orderInfo = (OrderInfo[]) oiList.toArray(new OrderInfo[oiList.size()]);
	}// parseInput()
	
	
	
	
	/** Parse move and retreat orders */
	private void moveAndRetreatParser(BufferedReader br, List oiList)
	throws IOException, PatternSyntaxException
	{
		Pattern mrp = Pattern.compile(MOVE_ORDER_REGEX);
		
		String line = ParserUtils.getNextLongLine(br).trim();
		while(line != null)
		{
			Matcher m = mrp.matcher(line);
			final boolean found = m.find();
			if(found && line.endsWith("."))
			{
				// remove trailing "."
				// no results.
				//
				oiList.add(new OrderInfo(line.substring(0,line.length()-1), null));
			}
			else if(found && line.endsWith("*)"))
			{
				// parse results (between the "(*" and "*)")
				// then remove the trailing "."
				//
				int rStart = line.indexOf("(*");
				int rEnd = line.indexOf("*)");
				if(rEnd <= rStart || rStart == -1)
				{
					throw new IOException("Missing (* *) for results on order: "+line);
				}
				
				String results = line.substring(rStart+2, rEnd);
				String order = line.substring(0, rStart).trim();
				
				if(order.endsWith("."))
				{
					order = order.substring(0, order.length()-1);
				}
				
				oiList.add(new OrderInfo(order, results));
			}
			else
			{
				// parse failed; break out of loop
				break;
			}
			
			line = ParserUtils.getNextLongLine(br);
		}
	}// moveAndRetreatParser()
	
	
	
	
	/** Parse adjustment orders */
	private void adjustmentParser(BufferedReader br, List oiList)
	throws IOException, PatternSyntaxException
	{
		Pattern ap = Pattern.compile(ADJUSTMENT_ORDER_REGEX);
		Pattern aWaive = Pattern.compile(ADJUSTMENT_WAIVE_REGEX);
		Pattern aUnusable = Pattern.compile(ADJUSTMENT_UNUSABLE_WAIVE_REGEX);
		
		String line = ParserUtils.getNextLongLine(br);
		while(line != null)
		{
			Matcher m = ap.matcher(line);
			if(m.find())
			{
				oiList.add( rewriteAdjustmentOrder(m.group(1), m.group(2), m.group(3), m.group(4)) );
			}
			else
			{
				// check for waives
				//
				Matcher mw = aWaive.matcher(line);
				if(mw.find())
				{
					// waived build (or builds); # is passed. # may be 0.
					//
					int num = 0;
					try
					{
						num = Integer.parseInt(mw.group(2).trim());
					}
					catch(NumberFormatException e)
					{
						// do nothing
					}
					
					addGeneralResult(mw.group(1).trim(), Utils.getLocalString(WAIVED_BUILDS, new Integer(num)));
				}
				else
				{
					Matcher mUnusable = aUnusable.matcher(line);
					if(mUnusable.find())
					{
						int num = 0;
						try
						{
							num = Integer.parseInt(mUnusable.group(2).trim());
						}
						catch(NumberFormatException e)
						{
							// do nothing
						}
						
						if("pending".equalsIgnoreCase(mUnusable.group(3)))
						{
							addGeneralResult(mUnusable.group(1).trim(), Utils.getLocalString(UNUSABLE_PENDING, new Integer(num)));
						}
						else if("waived".equalsIgnoreCase(mUnusable.group(3)))
						{
							addGeneralResult(mUnusable.group(1).trim(), Utils.getLocalString(UNUSABLE_WAIVED, new Integer(num)));
						}
					}
				}
			}
			
			line = ParserUtils.getNextLongLine(br);
		}
	}// adjustmentParser()
	
	
	/** Rewrites the too-verbose Judge adjustment orders into an JudgeOrderParser compatable format. */
	private OrderInfo rewriteAdjustmentOrder(String power, String action, String unit, String location)
	{
		StringBuffer sb = new StringBuffer(64);
		sb.append(power);
		sb.append(": ");
		sb.append(action);
		sb.append(' ');
		sb.append(unit);
		sb.append(' ');
		sb.append(location);
		return new OrderInfo(sb.toString());
	}// rewriteAdjustmentOrder()
	
}// class JudgeOrderParser
