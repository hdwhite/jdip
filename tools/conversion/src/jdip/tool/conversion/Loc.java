//
//  @(#)Loc.java 	2004
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
package jdip.conversion;

import java.lang.*;
import java.io.*;
import java.util.*;



/** 
*	Holds short name and (if any) coast. This is somewhat similar
*	to a dip.world.Location 
*/
class Loc
{
	private final static String[] COAST_TYPES = {"mv","nc","sc","wc","ec","xc","mx"};
	private final static String[] DIRECTIONAL_COASTS = {"nc","sc","wc","ec"};
	private final String shortName;
	private final String coastType;
	
	/**
	*	ShortName CANNOT be null.
	*	CoastType MAY be null.
	*/
	Loc(String shortName, String coastType)
	{
		if(shortName == null)
		{
			throw new IllegalArgumentException();
		}
		
		this.shortName = shortName;
		this.coastType = coastType;
	}// Loc()
	
	/**
	*	Parse from a string. Accepts '-' or '/' as a delim for
	*	a coast. Everything is lower-cased. A FileLine is only
	*	needed for error info. if null, it won't be used.
	*	Non-spaced parenthetical coasts e.g.: "xxx(sc)" are also OK.
	*/
	public static Loc makeLoc(final String in, final FileLine errorInfo)
	throws IOException
	{
		if(in == null)
		{
			throw new IllegalArgumentException();
		}
		
		String text = in.trim().toLowerCase();
		boolean isParenthetical = false;
		int idx = text.indexOf('-');
		if(idx == -1)
		{
			idx = text.indexOf('/');
		}
		if(idx == -1)
		{
			idx = text.indexOf('(');
			isParenthetical = true;
		}
		
		String name = (idx == -1) ? text : text.substring(0, idx);
		String coast = null;
		
		if(idx != -1)
		{
			if(isParenthetical && (idx+1 < text.length()-1))
			{
				coast = text.substring(idx+1, text.length()-1);
			}
			else if(idx+1 < text.length())
			{
				coast = text.substring(idx+1, text.length());
			}
			else
			{
				if(errorInfo != null)
				{
					errorInfo.makeError("Coast \""+coast+"\" for \""+in+"\" not recognized.");
				}
				else
				{
					throw new IOException("ERROR: coast for \""+in+"\" not recognized.");
				}
			}
		}
		
		if(coast != null)
		{
			boolean found = false;
			for(int i=0; i<COAST_TYPES.length; i++)
			{
				if(COAST_TYPES[i].equals(coast))
				{
					found = true;
					break;
				}
			}
			
			if(!found)
			{
				if(errorInfo != null)
				{
					errorInfo.makeError("Coast \""+coast+"\" for \""+in+"\" not recognized.");
				}
				else
				{
					throw new IOException("ERROR: coast for \""+in+"\" not recognized.");
				}
			}
		}
		
		return new Loc(name, coast);
	}// makeLoc()
	
	
	public String getShortName()
	{
		return shortName;
	}
	
	/** May return null */
	public String getCoastType()
	{
		return coastType;
	}
	
	public boolean isMX()
	{
		return ("mx".equals(coastType));
	}
	
	/** Defaults to NOT printing the coast */
	public String toString()
	{
		return toString(false);
	}
	
	/** 
	*	If true, give province and any coast type (unless null).
	*	If false, only directional coasts (nc/wc/ec/sc) are added.
	*/
	public String toString(boolean printAnyCoast)
	{
		StringBuffer sb = new StringBuffer(8);
		sb.append(shortName);
		
		if(coastType != null)
		{
			if(printAnyCoast)
			{
				sb.append("-");
				sb.append(coastType);
			}
			else
			{
				for(int i=0; i<DIRECTIONAL_COASTS.length; i++)
				{
					if(DIRECTIONAL_COASTS[i].equals(coastType))
					{
						sb.append("-");
						sb.append(coastType);
					}
				}
			}
		}
		
		return sb.toString();
	}// toString()

}// class Loc


