/*
*  @(#)OrderFormat.java	1.00	4/2002
*
*  Copyright 2002 Zachary DelProposto. All rights reserved.
*  Use is subject to license terms.
*/
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;

import dip.world.*;

import java.util.*;
import java.lang.reflect.*;

/**
*	OrderFormat formats orders according to the specified format string. Format string options
*	are shown below.
*	<p>
*	While OrderFormat is more flexible than using Order.toBriefString() or Order.toFullString(), it
*	is also considerably slower. This is should not generally be a problem, unless one is doing multiple
*	adjudications (e.g., for an AI).	
*	<p>
*	<pre>
*				formatting:
*				{variablename:arg:arg:arg}
*				
*				modifiers allowed depend on type
*				
*				Types: 		Modifiers:
*				=====================================================
*				Location	xx (see below)
*				Power 		title, lower, upper, plural
*				Unit.Type 	brief, full, default, title, lower, upper, plural
*				orderName	title, lower, upper, default, brief, full
*				
*				Boolean 		[true]:[false]:(modifiers)
*				
*				examples (for, source unit type field "srcUnitType")
*					{srcUnitType}					// prints full unit type name, e.g., "Army"
*					{srcUnitType:brief}				// brief name
*					{srcUnitType:full:upper}		// "ARMY"
*					{srcUnitType:full:title:plural}	// "Armys"	[note that the plural just adds an 's'!] not "Armies"
*				
*				brief: 	print abbreviated form; override default
*				full: 	print full form; overrides default
*				title: 	capitalize first letter of the name (title case)
*				lower,upper: to upper/lower case
*				default:  print brief or full, depending upon default setting 
*				plural: will pluralize a full name, but not a brief name.
*				
*				NOTE: if neither brief nor full is specified, 'default' is assumed.
*				
*				xx[+] : for Location printing
*					first is for province, next is for coast (if present)
*					province:
*					x = (b | f | -)
*						b = brief
*						d = user-specified default (brief or full)
*						f = full
*						- = do not print
*					coast: (b | f | -) (+)
*						b = brief	(e.g., "nc")
*						d = user-specified default (brief or full)
*						- = do not print
*						+ = ALWAYS print coast specifier (even if land or undefined) e.g.: "bb" vs. "bb+"
*						
*						examples:    src:--    src:ff    src:fb    src:bb+
*						
*						
*				boolean: example:  {value:lalala:nonono}
*					if value true: prints "lalala"
*					if false, prints "nonono"
*	</pre>			
*/
public class OrderFormat
{
	// internal constants
	private static final String XFORM_TITLE 	= "title";
	private static final String XFORM_LOWER 	= "lower";
	private static final String XFORM_UPPER 	= "upper";
	private static final String XFORM_PLURAL 	= "plural";
	private static final String FMT_BRIEF_KEY 	= "brief";
	private static final String FMT_FULL_KEY 	= "full";
	private static final String KEY_ORDERNAME 	= "orderName";
	private static final String KEY_CONVOYPATH  = "convoyPath";
	
	
	/** Class for controlling order formatting options */
	public static class OrderFormatOptions
	{
		// if true, these are brief
		private final boolean briefProvince;
		private final boolean briefCoast;
		private final boolean briefOrderName;
		private final boolean briefUnitType;
		private final boolean forceCoast;
		
		/** Terse (brief) format */
		public static final OrderFormatOptions TERSE	= new OrderFormatOptions(true,true,true,true);
		/** Verbose format */
		public static final OrderFormatOptions VERBOSE	= new OrderFormatOptions(false,false,false,false);
		/** Default format */
		public static final OrderFormatOptions DEFAULT	= new OrderFormatOptions(true,true,false,false);
		/** Debug format (abbreviations + coasts forced) */
		public static final OrderFormatOptions DEBUG	= new OrderFormatOptions(true,true,false,false,true);
		
		
		/** Create an OrderFormatOptions with the given typical options */
		public OrderFormatOptions(boolean briefProvince, boolean briefCoast, 
			boolean briefOrderName, boolean briefUnitType)
		{
			this(briefProvince, briefCoast, briefOrderName, briefUnitType, false);
		}// OrderFormatOptions()
		
		/** Create an OrderFormatOptions with the given options */
		public OrderFormatOptions(boolean briefProvince, boolean briefCoast, 
			boolean briefOrderName, boolean briefUnitType, boolean forceCoast)
		{
			this.briefProvince 	= briefProvince;
			this.briefCoast		= briefCoast;
			this.briefOrderName	= briefOrderName;
			this.briefUnitType	= briefUnitType;
			this.forceCoast		= forceCoast;
		}// OrderFormatOptions()
		
		/** Get if Province is Brief or Verbose */
		public boolean getBriefProvince()			{ return briefProvince; }
		/** Get if Coast is Brief or Verbose */
		public boolean getBriefCoast()				{ return briefCoast; }
		/** Get if Order Name is Brief or Verbose */
		public boolean getBriefOrderName()			{ return briefOrderName; }
		/** Get if Unit Type is Brief or Verbose */
		public boolean getBriefUnitType()			{ return briefUnitType; }
		/** Get if we are forcing the printing of coasts (mostly for debugging) */
		public boolean getForceCoast()				{ return forceCoast; }
	}// nested class OrderFormatOptions
	
	
	/**
	*	Format a Coast given the order formatting parameters.
	*	Does not handle null.
	*/
	public static String format(OrderFormatOptions ofo, Coast coast)
	{
		if(Coast.isDisplayable(coast))
		{
			if(ofo.getBriefCoast())
			{
				return coast.getAbbreviation();
			}
			else
			{
				return coast.getName();
			}
		}
		else if(ofo.getForceCoast())
		{
			if(ofo.getBriefCoast())
			{
				return coast.getAbbreviation();
			}
			else
			{
				return coast.getName();
			}
		}
		
		return "";
	}// format()
	
	
	/**
	*	Format a Province given the order formatting parameters.
	*	Does not handle null.
	*/
	public static String format(OrderFormatOptions ofo, Province province)
	{
		return ofo.getBriefProvince() ? province.getShortName() : province.getFullName();
	}// format()
	
	
	/**
	*	Format a Unit Type given the order formatting parameters.
	*	Does not handle null.
	*/
	public static String format(OrderFormatOptions ofo, Unit.Type unitType)
	{
		return ofo.getBriefUnitType() ? unitType.getShortName() : unitType.getFullName();
	}// format()
	
	/**
	*	Format a Location given the order formatting parameters.
	*	Does not handle null.
	*/
	public static String format(OrderFormatOptions ofo, Location loc)
	{
		StringBuffer sb = new StringBuffer(32);
		sb.append(format(ofo, loc.getProvince()));
		
		// use '/' if brief coast AND brief province
		// use '(COAST)' for all other cases.
		final boolean brief = (ofo.getBriefProvince() && ofo.getBriefCoast());
		final String coast = format(ofo, loc.getCoast());
		
		// if coast is non-displayable (""), we don't want to print it.
		if(coast.length() > 0)
		{
			if(brief)
			{
				sb.append("/");
				sb.append(coast);
			}
			else
			{
				sb.append(" (");
				sb.append(coast);
				sb.append(")");
			}
		}
		
		return sb.toString();
	}// format()
	
	/**
	*	Formats an Order according to the specified order format specifier 
	*	String and specified order format options. Does not handle null 
	*	orders.
	*/
	public static String format(OrderFormatOptions ofo, Order order, String format)
	{
		Class clazz = order.getClass();
		StringBuffer output = new StringBuffer(256);
		StringBuffer accum = new StringBuffer(64);
		
		boolean inBrace = false;
		StringTokenizer st = new StringTokenizer(format, "{}", true);
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			if("{".equals(tok) && !inBrace)
			{
				inBrace = true;
			}
			else if("}".equals(tok) && inBrace)
			{
				inBrace = false;
				lookupAndFormat(ofo, output, accum.toString(), clazz, order);
				accum.setLength(0);
			}
			else
			{
				if(inBrace)
				{
					accum.append(tok);
				}
				else
				{
					output.append(tok);
				}
			}
		}
		
		return output.toString();
	}// format()
	
	
	/**
	*	Gets an example of a format. Uses the Support order
	*	as an example. Format example depends upon current
	*	settings / default settings.
	*	
	*/
	public static String getFormatExample(OrderFormatOptions ofo, OrderFactory of)
	{
		// this is about the ONLY time Province or Power objects are
		// created using 'new'
		Province prov1 = new Province("Livonia", new String[]{"lvn"}, 0, false);
		Province prov2 = new Province("St. Petersburg", new String[]{"stp"}, 0, false);
		
		Power power1 = new Power(new String[]{"Russia"}, "Russian", true);
		
		Location src = 		new Location(prov1, Coast.SEA);
		Location supSrc = 	new Location(prov2, Coast.SOUTH);
		
		Support support = of.createSupport(power1, src, Unit.Type.FLEET, 
			supSrc, power1, Unit.Type.FLEET);
		return format(ofo, support, support.getDefaultFormat());
	}// getFormatExample()
	
	
	// looks at the text in between braces {}
	private static void lookupAndFormat(OrderFormatOptions ofo, StringBuffer output, String input, Class clazz, Order order)
	{
		// get arguments; args[0] MUST be variable name..
		StringTokenizer st = new StringTokenizer(input, ":", false);
		String[] args = new String[st.countTokens()];	// this is inefficient...
		int count = 0;
		while(st.hasMoreTokens())
		{
			args[count] = st.nextToken();
			count++;
		}
		
		if(args.length == 0)
		{
			output.append("?var?");
			return;
		}
		
		// check for keywords; if failed as a keyword,
		// try via reflection
		String result = getAsKeyword(ofo, args, order);
		
		if(result == null)
		{
			result = getFromField(ofo, args, clazz, order);
		}
		
		if(result == null)
		{
			// for debugging
			System.err.println("OrderFormat: bad input (not keyword/field): "+input);
			output.append("?badvariable?");
			return;
		}
		
		// any type may have "_title", "_lower", or "_upper" modifiers
		output.append(result);
	}// lookupObject()
	
	
	// searches for keywords; these are NOT valid fields in 
	// Order or subclasses
	private static String getAsKeyword(OrderFormatOptions ofo, String args[], Order order)
	{
		String result = null;
		
		if(args[0].equals(KEY_ORDERNAME))
		{
			final boolean isBrief = isFormatBrief(args, ofo.getBriefOrderName());
			if(isBrief)
			{
				result = transform(order.getBriefName(), args, isBrief);
			}
			else
			{
				result = transform(order.getFullName(), args, isBrief);
			}
		}
		else if(args[0].equals(KEY_CONVOYPATH))
		{
			// get convoy path(s)
			if(order instanceof Move)
			{
				Move move = (Move) order;
				List routes = move.getConvoyRoutes();
				if(routes == null)
				{
					result = "ERR:(no routes!)";
				}
				else
				{
					StringBuffer sb = new StringBuffer(256);
					Iterator iter = routes.iterator();
					while(iter.hasNext())
					{
						final Province[] path = (Province[]) iter.next();
						move.formatConvoyRoute(sb, path, ofo.getBriefProvince(), false);
						
						// prepare for next path, if any
						if(iter.hasNext())
						{
							sb.append(", ");
						}
					}
					
					result = transform(sb.toString(), args, true);
				}
			}
			else
			{
				result = "ERR:(convoyPath not valid here)";
			}
		}
		
		return result;
	}// getAsKeyword()
	
	
	
	// searches for valid field in Order or subclass, and processes the result
	// accordingly
	private static String getFromField(OrderFormatOptions ofo, String args[], Class clazz, Order order)
	{
		Object obj = getField(args[0], clazz, order);
		String result = null;
		
		// based on type, do some stuff
		if(obj instanceof Location)
		{
			result = (args.length > 1) ? formatLocation(ofo, (Location) obj, args[1] ) : ((Location)obj).toString();
		}
		else if(obj instanceof Power)
		{
			// no specific options: 
			result = transform( ((Power)obj).getName(), args, false );
		}
		else if(obj instanceof Unit.Type)
		{
			Unit.Type uType = (Unit.Type) obj;
			
			if(uType.equals(Unit.Type.UNDEFINED))
			{
				result = "";
			}
			else
			{
				final boolean isBrief = isFormatBrief(args, ofo.getBriefUnitType());
				if(isBrief)
				{
					result = transform(uType.getShortName(), args, isBrief);				
				}
				else
				{
					result = transform(uType.getFullName(), args, isBrief);
				}
			}
		}
		else if(obj instanceof Boolean)
		{
			// args[1] == true result, args[2] == false result
			// if only 2 args (no 'false' result specified) false = "" 
			if(args.length >= 2)
			{
				if( ((Boolean)obj).booleanValue() )
				{
					result = transform(args[1], args, false);
				}
				else
				{
					if(args.length >= 3)
					{
						result = transform(args[2], args, false);
					}
					else
					{
						result = "";
					}
				}
			}
			else
			{
				result = "?boolean?";				
			}
		}
		else if(obj instanceof String)
		{
			result = transform((String) obj, args, false);
		}
		else
		{
			// for debugging
			System.err.println("OrderFormat: args[0] = \""+args[0]+"\" result: obj = \""+obj+"\" unknown type.");
		}
		
		return result;
	}// getFromField()
	
	/**
	*	transforms the given string
	*	
	*	Format Type: brief (false) or verbose (true).
	*	
	*	
	*/
	private static String transform(String input, String[] arglist, boolean isBrief)
	{
		StringBuffer sb = new StringBuffer(input.length() + 1);
		
		// first: check plural; we don't plural though if we 
		// are brief.
		if(hasMatch(arglist, XFORM_PLURAL) && !isBrief)
		{
			sb.append(input);
			sb.append('s');
			input = sb.toString();
			sb.setLength(0);
		}
		
		// transform case
		if(hasMatch(arglist, XFORM_TITLE))
		{
			sb.append(toTitleCase(input));
		}
		else if(hasMatch(arglist, XFORM_LOWER))
		{
			sb.append(input.toLowerCase());
		}
		else if(hasMatch(arglist, XFORM_UPPER))
		{
			sb.append(input.toUpperCase());
		}
		else
		{
			sb.append(input);
		}
		
		return sb.toString();
	}// transform()
	
	
	
	/**
	*	Formats a Location using the given Location format specifier 
	*	String. (e.g., "bb+").
	*/
	public static String formatLocation(OrderFormatOptions ofo, Location loc, String fmt)
	{
		// check format specifier
		if(fmt.length() < 2)
		{
			return "?locfmt?";
		}
		
		// setup
		StringBuffer sb = new StringBuffer(64);
		
		// get province format (none, full, brief == -,f,b)
		char c = fmt.charAt(0);
		
		// if default, select.
		if(c == 'd')
		{
			c = (ofo.getBriefProvince()) ? 'b' : 'f';
		}
		
		// not a default
		if(c == 'f')
		{
			sb.append( loc.getProvince().getFullName() );
		}
		else if(c == 'b')
		{
			sb.append( loc.getProvince().getShortName() );
		}
		else if(c != '-')
		{
			// error! invalid format specifier
			return "?locfmt:province?";
		}
		
		// if '+' specified, we always print coasts (even for land or water spaces)
		// but we don't check if we've already been configured to force coast printing.
		boolean forceCoast = ofo.getForceCoast();
		if(!forceCoast)
		{
			if(fmt.length() >= 3)
			{
				c = fmt.charAt(2);
				forceCoast = (c == '+') ? true : false;
			}
		}
		
		// get coast format (none, full, brief, always-specified == -,f,b 
		c = fmt.charAt(1);
		
		// if default, select.
		if(c == 'd')
		{
			c = (ofo.getBriefCoast()) ? 'b' : 'f';
		}
		
		// process coast format
		if(c == 'f' || c == 'b')
		{
			Coast coast = loc.getCoast();
			if( forceCoast 
				|| (!coast.equals(Coast.NONE) && !coast.equals(Coast.SINGLE) 
				&& !coast.equals(Coast.WING) && !coast.equals(Coast.UNDEFINED)) )
			{
				// only print a "/" if we *did* print a province
				if(sb.length() > 0)
				{
					sb.append('/');
				}
				
				if(c == 'f')
				{
					sb.append( coast.getName() );
				}
				else
				{
					sb.append( coast.getAbbreviation() );
				}
			}
		}
		else if(c != '-')
		{
			// error! invalid format specifier
			return "?locfmt:coast?";
		}
		
		return sb.toString();
	}// formatLocation()
	
	
	private static boolean hasMatch(String[] arglist, String arg)
	{
		for(int i=0; i<arglist.length; i++)
		{
			if(arglist[i].equalsIgnoreCase(arg))
			{
				return true;
			}
		}
	
		return false;
	}// hasMatch()
	
	/**
	*	Returns if the format specifier is BRIEF (true) or FULL (verbose, false)
	*
	*/
	private static boolean isFormatBrief(String[] arglist, boolean defaultValue)
	{
		for(int i=0; i<arglist.length; i++)
		{
			if(arglist[i].equalsIgnoreCase(FMT_BRIEF_KEY))
			{
				return true;	// brief
			}
			else if(arglist[i].equalsIgnoreCase(FMT_FULL_KEY))
			{
				return false;	// verbose
			}
		}
		
		return defaultValue;
	}// isFormatBrief()
	
	// recurse superclasses until find field OR 'Order' (base class) reached
	private static Object getField(String name, Class clazz, Order order)
	{
		while(clazz != null)
		{
			try
			{
				return clazz.getDeclaredField(name).get(order);
			}
			catch(Exception e)
			{
				return getField(name, clazz.getSuperclass(), order);
			}
		}
		
		return null;
	}// getField()
	
	
	/** Converts a String to Title case */
	private static String toTitleCase(String in)
	{
		StringBuffer sb = new StringBuffer(in.length());
		sb.append(in.toLowerCase());
		sb.setCharAt(0, Character.toTitleCase(sb.charAt(0)));
		return sb.toString();
	}// toTitleCase()
	
}// class OrderFormat
