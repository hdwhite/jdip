//
//  @(#)Border.java		10/2002
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
package dip.world;

import dip.order.Order;
import dip.misc.Utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
*
*	A Border limits movement or support between 2 provinces.
*	
*	A Border object is immutable.
*	
*	
*
*	
*/
public class Border implements Serializable
{
	// constants
	/** Constant indicating that transit is not limited during any particular PhaseType */
	public static final Phase.PhaseType[] ANY_PHASE		= new Phase.PhaseType[0];
	/** Constant indicating that transit is not limited during any particular Season */
	public static final Phase.SeasonType[] ANY_SEASON	= new Phase.SeasonType[0];
	/** Constant indicating that transit limitation applies from any Location */
	public static final Location[] ANY_LOCATION			= new Location[0];
	/** Constant indicating that transit limitation applies to any Order subclass */
	public static final Class[] ANY_ORDER 				= new Class[0];
	/** Constant indicating that transit limitation applies to any Unit.Type subclass */
	public static final Unit.Type[] ANY_UNIT 			= new Unit.Type[0];
	
	/** Constant indicating year less-than */
	private static final int YEAR_TYPE_BEFORE	= -1;
	/** Constant indicating year greater-than */
	private static final int YEAR_TYPE_AFTER	= 1;
	/** Constant indicating year exactly */
	private static final int YEAR_TYPE_EXACT	= 0;
	/** Constant indicating year exactly */
	private static final int YEAR_TYPE_UNUSED	= -666;
	
	
	/** Constant indicating that the misc field is not used */
	private static final int MISC_NOT_USED	= -1;
	/** Constant indicating that the transit is always denied */
	private static final int MISC_ALWAYS		= 0;
	/** Constant indicating that the transit is denied during odd years */
	private static final int MISC_ODD			= 1;
	/** Constant indicating that the transit is denied during even years */
	private static final int MISC_EVEN		= 2;
	
	// Private constants for token parsing
	// SeasonType/PhaseType parsers 
	private static final String TOK_MISC_ODD	= "odd";
	private static final String TOK_MISC_EVEN	= "even";
	private static final String TOK_MISC_ALWAYS	= "always";
	// all year type tokes MUST have the same length
	private static final String TOK_YEAR_EQ 	= "eq";
	private static final String TOK_YEAR_GT 	= "gt";
	private static final String TOK_YEAR_LT 	= "lt";
	
	
	// instance fields
	//
	private final Location[]			from;		// location(s) from which this transit limit applies; 
													// if zero-length, applies to all 'from' locations.
													// may specify coasts; if coast not defined, any coast used
													
	private final Phase.SeasonType[]	seasons;	// [0] length if not used
	private final Phase.PhaseType[]		phases;		// [0] length if not used
	private final int 					yearType;	// [< = or > : -1,0,+1]
	private final int					year;		// negative if not used
	private final int					misc;		// odd/even/always
	private final Unit.Type[] 			unitTypes;	// unit type to which this limitation applies
	private final String 				description; // description
	private final Class[]				orderClasses;	// disallowed for these order types
	private final int 					baseMoveModifier;	// support modifier (defaults to 0)
	private final String 				id;			// identifying name
	
	
	/**
	*	Constructor. The String arguments are parsed; if they are not valid,
	*	an InvalidBorderException will be thrown. It is not recommended that 
	*	null arguments are given. Instead, use empty strings or public constants
	*	where appropriate.
	*
	*	@throws InvalidBorderException		if any arguments are invalid.
	*	@throws IllegalArgumentException	if id, description, or prohibited is null
	*/
	public Border(String id, String description, String units, Location[] from, 
					String orders, String baseMoveModifier, String prohibited)
	throws InvalidBorderException
	{
		if(id == null || description == null || prohibited == null)
		{
			throw new IllegalArgumentException();
		}
		
		// set id. This is used by error messages, so must be set early.
		this.id = id;
		
		// parse allowed orderClasses via order classes; must specify package [case sensitive]
		// e.g.: dip.order.Move
		// these may be separated by spaces or commas (or both)
		orderClasses = parseOrders(orders);
		
		// parse unitTypes; must specify package [case sensitive]
		// e.g.: ARMY; must be a declared unit constant in dip.world.Unit 
		unitTypes = parseUnitTypes(units);
		
		// parse year / type
		// (find first index of <,=,>; if not found, ignored)
		
		// parse seasons, phases, and misc
		prohibited = prohibited.toLowerCase();
		
		this.seasons = parseProhibitedSeasons(prohibited);
		this.phases = parseProhibitedPhases(prohibited);
		this.misc = parseMisc(prohibited);
		this.yearType = parseYearType(prohibited);
		this.year = parseYear(prohibited);
		this.baseMoveModifier = parseBaseMoveModifier(baseMoveModifier);
		
		// fields we don't need to parse
		this.from = (from == null) ? ANY_LOCATION : from;
		this.description = description;
	}// Border()
	
	
	/** Parses the prohibited SeasonTypes (uses Phase.SeasonTypes.parse()) */
	private Phase.SeasonType[] parseProhibitedSeasons(String in)
	{
		StringTokenizer st = new StringTokenizer(in, ",");
		ArrayList list = new ArrayList();
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken().trim();
			Phase.SeasonType season = Phase.SeasonType.parse(tok);
			if(season != null)
			{
				list.add(season);
			}
		}
		
		return (Phase.SeasonType[]) list.toArray(new Phase.SeasonType[list.size()]);
	}// parseProhibitedSeasons()
	
	/** Parses the prohibited PhaseTypes (uses Phase.PhaseType.parse()) */
	private Phase.PhaseType[] parseProhibitedPhases(String in)
	{
		StringTokenizer st = new StringTokenizer(in, ",");
		ArrayList list = new ArrayList();
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken().trim();
			Phase.PhaseType phase = Phase.PhaseType.parse(tok);
			if(phase != null)
			{
				list.add(phase);
			}
		}
		
		return (Phase.PhaseType[]) list.toArray(new Phase.PhaseType[list.size()]);
	}// parseProhibitedPhases()
	
	/** 
	*	Parses the miscellaneous attributes (e.g., always, odd, even, etc.) 
	*	Note that no specified behavior should exist if more than one of odd/even/etc
	*	are specified; they are to be mutually exclusive.
	*/
	private int parseMisc(String in)
	{
		if(in.indexOf(TOK_MISC_ODD) >= 0)
		{
			return MISC_ODD;
		}
		else if(in.indexOf(TOK_MISC_EVEN) >= 0)
		{
			return MISC_EVEN;
		}
		else if(in.indexOf(TOK_MISC_ALWAYS) >= 0)
		{
			return MISC_ALWAYS;
		}
		
		return MISC_NOT_USED;
	}// parseMisc()
	
	/** Parses the year value (integer) */
	private int parseYear(String in)
	throws InvalidBorderException
	{
		final int idx = getYearIndex(in);
		if(idx == -1)
		{
			return -1;
		}
		
		// get all digits up until first non-digit.
		StringBuffer sb = new StringBuffer(16);
		int pos = idx+2;
		boolean isDigit = true;
		while(pos < in.length() && isDigit)
		{
			final char c = in.charAt(pos);
			isDigit = Character.isDigit(c);
			if(isDigit)
			{
				sb.append(c);
			}
		}
		
		// test digit string
		try
		{
			return Integer.parseInt(sb.toString());
		}
		catch(NumberFormatException e)
		{
			throw new InvalidBorderException(Utils.getLocalString("Border.error.badyear", id, e.getMessage()));
		}
	}// parseYear()
	
	/** Parses the year type (gt,lt,eq) */
	private int parseYearType(String in)
	throws InvalidBorderException
	{
		final int idx = getYearIndex(in);
		if(idx == -1)
		{
			return YEAR_TYPE_UNUSED;
		}
		
		if(in.regionMatches(0, TOK_YEAR_LT, 0, 2))
		{
			return YEAR_TYPE_BEFORE;
		}
		else if(in.regionMatches(0, TOK_YEAR_GT, 0, 2))
		{
			return YEAR_TYPE_AFTER;
		}
		else if(in.regionMatches(0, TOK_YEAR_EQ, 0, 2))
		{
			return YEAR_TYPE_EXACT;
		}
		
		throw new IllegalStateException("Internal Error");
	}// parseYearType()
	
	
	/** Gets the position of a string starting with -,+,or = */
	private int getYearIndex(String in)
	{
		int idx = in.indexOf(TOK_YEAR_EQ);
		
		if(idx == -1)
		{
			idx = in.indexOf(TOK_YEAR_GT);
		}
		
		if(idx == -1)
		{
			idx = in.indexOf(TOK_YEAR_LT);
		}
		
		if(idx == -1)
		{
			return -1;
		}
		
		return idx;
	}// getYearIndex()
	
	
	/** Parses the unit types */
	private Unit.Type[] parseUnitTypes(String in)
	throws InvalidBorderException
	{
		Class superClass = null;
		
		try
		{
			superClass = Class.forName("dip.world.Unit$Type");	// Type is a nested class in Unit
		}
		catch(ClassNotFoundException e)
		{
			throw new InvalidBorderException(Utils.getLocalString("Border.error.internal", "parseUnitTypes()", e.getMessage()));
		}
		
		
		ArrayList list = new ArrayList(10);
		StringTokenizer st = new StringTokenizer(in,", ");
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			try
			{
				Field field = superClass.getField(tok);
				list.add( (Unit.Type) field.get(null) );
			}
			catch(Exception e)
			{
				throw new InvalidBorderException(Utils.getLocalString("Border.error.badunit", id, tok));
			}
		}
		
		if(list.isEmpty())
		{
			return ANY_UNIT;
		}
		
		return (Unit.Type[]) list.toArray(new Unit.Type[list.size()]);
	}// parseUnitTypes()
	
	
	/** Parses the order types */
	private Class[] parseOrders(String in)
	throws InvalidBorderException
	{
		Class[] classes = parseClasses2Objs(in, "dip.order.Order");
		
		if(classes.length == 0)
		{
			return ANY_ORDER;
		}
		
		return classes;
	}// parseOrders()
	
	
	/** Internal parser helper method */
	private Class[] parseClasses2Objs(String in, String superClassName)
	throws InvalidBorderException
	{
		Class superClass = null;
		try
		{
			superClass = Class.forName(superClassName);
		}
		catch(ClassNotFoundException e)
		{
			throw new InvalidBorderException(Utils.getLocalString("Border.error.internal", "parseClasses2Objs()", e.getMessage()));
		}
		
		ArrayList list = new ArrayList(10);
		StringTokenizer st = new StringTokenizer(in,", ");
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			Class cls = null;
			
			try
			{
				cls = Class.forName(tok);
			}
			catch(ClassNotFoundException cnfe)
			{
				throw new InvalidBorderException(Utils.getLocalString("Border.error.badclass", id, tok));
			}
			
			if( !superClass.isAssignableFrom(cls) )
			{
				throw new InvalidBorderException(Utils.getLocalString("Border.error.badderivation", id, cls.getName(), superClass.getName()));
			}
			
			list.add(cls);
		}
		
		return (Class[]) list.toArray(new Class[list.size()]);
	}// parseClasses2Objs()
	
	
	/** 
	*	Parses the base move modifier. If string is empty, defaults to 0.
	*	The format is just a positive or negative (or 0) integer.
	*/
	private int parseBaseMoveModifier(String in)
	throws InvalidBorderException
	{
		in = in.trim();
		
		if(in.length() == 0)
		{
			return 0;
		}
		
		try
		{
			return Integer.parseInt(in);
		}
		catch(NumberFormatException e)
		{
			// fall through to exception, below
		}
		
		throw new InvalidBorderException(Utils.getLocalString("Border.error.badmovemod", id, in));
	}// parseBaseMoveModifier()
	
	
	/**
	*	Determines if a unit can transit from a location to this location.
	*	<p>
	*	Convenience method for more verbose canTransit() method. No arguments may
	*	be null.
	*/
	public boolean canTransit(Phase phase, Order order)
	{
		return canTransit(order.getSource(), order.getSourceUnitType(), phase, order.getClass());
	}// canTransit()
	
	
	/**
	*	Determines if a unit can transit from a location to this location.
	*	<p>
	*	Null arguments are not permitted.
	*/
	public boolean canTransit(Location fromLoc, Unit.Type unit, Phase phase, Class orderClass)
	{
		// check from
		for(int i=0; i<from.length; i++)
		{
			if(from[i].equalsLoosely(fromLoc))
			{
				return false;
			}
		}
		
		// check unit type
		for(int i=0; i<unitTypes.length; i++)
		{
			if(unitTypes[i] == unit)
			{
				return false;
			}
		}
		
		// check order
		for(int i=0; i<orderClasses.length; i++)
		{
			if(orderClass == orderClasses[i])
			{
				return false;
			}
		}
		
		// check phase (season, phase, and year)
		for(int i=0; i<seasons.length; i++)
		{
			if(seasons[i] == phase.getSeasonType())
			{
				return false;
			}
		}
		
		for(int i=0; i<phases.length; i++)
		{
			if(phases[i] == phase.getPhaseType())
			{
				return false;
			}
		}
		
		final int theYear = phase.getYear();
		if(year > 0 && yearType != YEAR_TYPE_UNUSED)
		{
			if( (yearType == YEAR_TYPE_BEFORE && theYear < year)
				|| (yearType == YEAR_TYPE_AFTER && theYear > year)
				|| (yearType == YEAR_TYPE_EXACT && theYear == year) )
			{	
				return false;
			}
		}
		
		// NOTE: n & 1 == 0 if n is even
		if(	(misc == MISC_ALWAYS)
			|| (misc == MISC_ODD && ((year & 1) == 0))
			|| (misc == MISC_EVEN && ((year & 1) == 1)) )
		{	
			return false;
		}	
		
		// we pass!
		return true;
	}// canTransit()
	
	
	/** Gets the base move modifier. Requires a non-null from location. */
	public int getBaseMoveModifier(Location moveFrom)
	{
		if(from.length == 0)
		{
			// if no locations defined, modifier is good for all locations.
			return baseMoveModifier;
		}
		else
		{
			for(int i=0; i<from.length; i++)
			{
				if(from[i].equalsLoosely(moveFrom))
				{
					return baseMoveModifier;
				}
			}
		}
		
		// if not from the given location, no change in support.
		return 0;
	}// getBaseMoveModifier()
	
	
	/** Returns the description */
	public String getDescription()
	{
		return description;
	}// getDescription()
	
}// class Border
