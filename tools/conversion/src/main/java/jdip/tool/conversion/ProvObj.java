//
//  @(#)ProvObj.java 	2004
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
package jdip.tool.conversion;

import java.io.IOException;
import java.util.*;

/** Province object */
class ProvObj implements Comparable
{
	public static final String BORDER_NAME_PREFIX = "mxFrom";
	
	private String 		fullName = null;
	private ArrayList 	shortNames;       	// contains strings
	private ArrayList	adjList;          	// contains 'Adj' objects
	private String 		type = null;		// e.g.: "l", "lw", "Aw", etc.
	private boolean 	hasMX = false;		// if contains an 'mx' coast.
	
	// type information
	private boolean isConvoyableCoast = false;	// lw, xw, or (Capital)w
	private boolean isLand = false;			// if 'l', or capital letter
	private boolean isWater = false;		// if 'w'
	private boolean isIce = false;			// if 'v'
	private boolean isNeutralSC = false;	// if 'x'
	private String 	homeSC = null;			// letter of country for home SC
	
	private ProvObj()
	{
		shortNames = new ArrayList(10);
		adjList = new ArrayList(5);
	}// ProvObj()
	
	
	/** Create a ProvObj from a FileLine */
	public static ProvObj makeProvObj(FileLine line)
	throws IOException
	{
		if(line == null || line.isSectionMarker())
		{
			throw new IllegalArgumentException();
		}
		
		
		ProvObj prov = new ProvObj();
		
		// full name: start to first comma. Name may contain spaces.
		StringTokenizer st = new StringTokenizer(line.getLine(), ",");
		if(!st.hasMoreTokens())
		{
			line.makeError("Missing comma / Province name");
		}
		
		prov.fullName = st.nextToken();
		
		// check to see that fullname is at least 1 char long
		if(prov.fullName.length() < 1)
		{
			line.makeError("Province full name is too short!");
		}
		
		// get type:
		if(st.hasMoreTokens())
		{
			prov.type = st.nextToken(",\t\r\n ");
		}
		else
		{
			line.makeError("No province type information (e.g., water, land, etc.)");
		}
		
		// get abbreviated names. Ensure that we have at least 1.
		while(st.hasMoreTokens())
		{
			prov.shortNames.add( st.nextToken() );
		}
		
		if(prov.shortNames.isEmpty())
		{
			line.makeError("No abbreviated names found! At least one is required.");
		}
		
		prov.parseType(line);
		
		return prov;
	}// makeProvObj()
	
	
	/** Get the FIRST shortname. */
	public String getSN()
	{
		return (String) shortNames.get(0);
	}// getSN()
	
	/** Checks that the given short name (NO coast!!) is a valid shortname. */
	public boolean isSN(String in)
	{
		for(int i=0; i<shortNames.size(); i++)
		{
			String sn = (String) shortNames.get(i);
			if(sn.equalsIgnoreCase(in))
			{
				return true;
			}
		}
		
		return false;
	}// isSN()
	
	/** 
	*	Add Adjacency. Checks for dupes. Also,
	*	checks that initial coast in the list is NOT null.
	*	Also checks that the initial state is the same as that
	*	returned by getSN().
	*	
	*/
	public void addAdjacency(Adjacency adj, FileLine fl)
	throws IOException
	{
		if(adj == null || fl == null)
		{
			throw new IllegalArgumentException();
		}
		
		final String coast = adj.getLoc().getCoastType();
		
		// short name MUST match province short name returned by getSN()!
		if(!isSN( adj.getLoc().getShortName() ))
		{
			String msg = "Adding adjacency to wrong province!\n"+
						 "ProvObj: "+getSN()+"\n"+
						 "Adding: "+adj+"\n";
			throw new IllegalStateException(msg);
		}
			
		// no coast?
		if(coast == null)
		{
			fl.makeError("A coast MUST be specified for the first province in the adjacency list!");
		}
		
		// check for dupes
		for(int i=0; i<adjList.size(); i++)
		{
			Adjacency tmp = (Adjacency) adjList.get(i);
			if(coast.equals(tmp.getLoc().getCoastType()))
			{
				fl.makeError("An -"+coast+" coast type was already specified for this province ("+getSN()+")");
			}
		}
		
		// set hasMX flag
		if(adj.hasMX())
		{
			hasMX = true;
		}
		
		// all is good
		adjList.add(adj);
	}// addAdjacency()
	
	
	// if NO adjacency data is present, connect province to itself.
	// example: switzerland
	//
	// if isLand or isConvoyableCoast and no -mv adjacency is present, 
	// create a link to itself
	// example: variant: loeb9, IrelandfixAdjacency
	//
	/** 
	*	Fix adjacency for islands & isolated provinces 
	*	This should ONLY be called after all adjacency data has been
	*	set.
	*/
	public void fixAdjacency()
	{
		if(adjList.size() == 0)
		{
			adjList.add( Adjacency.makeSelfMV(this) );
			System.out.println("   isolated province: "+getSN());
		}
		else if(isLand || isConvoyableCoast)
		{
			// make sure we have a "-mv" adjacency
			boolean foundMV = false;
			for(int i=0; i<adjList.size(); i++)
			{
				Adjacency tmp = (Adjacency) adjList.get(i);
				if("mv".equals(tmp.getLoc().getCoastType()))
				{
					foundMV = true;
					break;
				}
			}
			
			if(!foundMV)
			{
				adjList.add( Adjacency.makeSelfMV(this) );
				System.out.println("   created island: "+getSN());
			}
		}
	}
	
	/** Comparable implementation */
	public int compareTo(Object obj) 
	{
		if(obj instanceof ProvObj)
		{
			return fullName.compareTo( ((ProvObj) obj).fullName );
		}
		
		throw new IllegalArgumentException();
	}// compareTo()
	
	
	public String getFullName()
	{
		return fullName;
	}
	
	public String[] getShortNames()
	{
		return (String[]) shortNames.toArray(new String[shortNames.size()]);
	}
	
	public boolean isConvoyableCoast()
	{
		return isConvoyableCoast;
	}
	
	public boolean isIce()
	{
		return isIce;
	}
	
	public boolean isNeutralSC()
	{
		return isNeutralSC;
	}
	
	public boolean isHomeSC()
	{
		return (homeSC != null);
	}
	
	public boolean isSC()
	{
		return (isNeutralSC() || isHomeSC());
	}
	
	/** returns null if no home SC country modifier */
	public String getHomeSC()
	{
		return homeSC;
	}
	
	public boolean hasMX()
	{
		return hasMX;
	}
	
	/** Parse the type into flags. */
	private void parseType(final FileLine fl)
	throws IOException
	{
		for(int i=0; i<type.length(); i++)
		{
			final char c = type.charAt(i);
			
			if(Character.isUpperCase(c))
			{
				homeSC = new String(new char[]{c});
				isLand = true; 	// (owned) SC are assumed to be on land
			}
			else if(c == 'w')
			{
				isWater = true;
			}
			else if(c == 'l')
			{
				isLand = true;
			}
			else if(c == 'v')
			{
				isIce = true;
			}
			else if(c == 'x')
			{
				isNeutralSC = true;
				isLand = true;	// SC are assumed to be on land
			}
			else
			{
				fl.makeError("Unrecognized province type modifier: \""+c+"\"");
			}
		}
		
		if(isLand && isWater)
		{
			isConvoyableCoast = true;
		}
	}// parseType()
	
	/** Writes XML text of this province */
	public String toXML()
	{
		StringBuffer sb = new StringBuffer(512);
		
		/*
			if 'mx' coast, need to create appropriate border reference
			also, needs to be merged in with mv data.
		*/
		
		// <PROVINCE> tag
		sb.append("\t<PROVINCE fullname=\"");
		sb.append(fullName);
		sb.append("\" shortname=\"");
		sb.append(getSN());
		sb.append("\"");
		
		if(isConvoyableCoast())
		{
			sb.append(" isConvoyableCoast=\"true\"");
		}
		
		if(hasMX || isIce())
		{
			sb.append(" borders=\"");
			
			if(hasMX)
			{
				sb.append( makeBorderNames() );
				if(isIce())
				{
					sb.append(" ");
				}
			}
			
			if(isIce())
			{
				sb.append("ice");
			}
			
			sb.append("\"");
		}
		
		sb.append(">\n");
		
		// 'unique' abbreviated names. (if any)
		if(shortNames.size() > 1)
		{
			for(int i=1; i<shortNames.size(); i++)
			{
				sb.append("\t\t<UNIQUENAME name=\"");
				sb.append( (String) shortNames.get(i) );
				sb.append("\" />\n");
			}
		}
		
		// write adjacency information
		for(int i=0; i<adjList.size(); i++)
		{
			sb.append("\t");
			final Adjacency adj = (Adjacency) adjList.get(i);
			sb.append( adj.toXML() );
		}
		
		// <PROVINCE> end
		sb.append("\t</PROVINCE>\n");
		
		return sb.toString();
	}// writeProvince()
	
	/** 
	*	Create -mx border names:
	*
	*		mxFromXXX (XXX is all caps)
	*
	*/
	private String makeBorderNames()
	{
		ArrayList borders = new ArrayList();
		
		for(int i=0; i<adjList.size(); i++)
		{
			Adjacency adj = (Adjacency) adjList.get(i);
			if(adj.hasMX())
			{
				Iterator iter = adj.getAdjLocs().iterator();
				while(iter.hasNext())
				{
					Loc loc = (Loc) iter.next();
					if(loc.isMX())
					{
						String name = BORDER_NAME_PREFIX;
						name += loc.getShortName().toUpperCase();
						if(!borders.contains(name))
						{
							borders.add(name);
						}
					}
				}
			}
		}
		
		StringBuffer sb = new StringBuffer(64);
		for(int i=0; i<borders.size(); i++)
		{
			sb.append( borders.get(i) );
			if(i < borders.size()-1)
			{
				sb.append(" ");
			}
		}
		
		return sb.toString();
	}// makeBorderNames()
	
	
	/** 
	*	Get -mx border types (if any)
	*	empty list if none
	*/
	List getMXProvs()
	{
		LinkedList mxList = new LinkedList();
		
		for(int i=0; i<adjList.size(); i++)
		{
			Adjacency adj = (Adjacency) adjList.get(i);
			if(adj.hasMX())
			{
				Iterator iter = adj.getAdjLocs().iterator();
				while(iter.hasNext())
				{
					Loc loc = (Loc) iter.next();
					if(loc.isMX())
					{
						mxList.add(loc.getShortName());
					}
				}
			}
		}
		
		return mxList;
	}// makeBorderNames()
	
}// inner class ProvObj


