//
//  @(#)Adjacency.java 	2004
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/** Adjacency object */
class Adjacency
{
	// e.g.: 
	// rum-xc: sev bla bul/ec
	// loc: Loc Loc Loc 
	// 
	
	final private Loc loc;				// initial Loc
	final private LinkedList<Loc> adjLocs;		// list of Loc objects
	private boolean hasMX = false;
	
	private Adjacency(Loc initial)
	{
		if(initial == null)
		{
			throw new IllegalArgumentException();
		}
		
		loc = initial;
		adjLocs = new LinkedList<>();
	}// Adjacency
	
	
	/** Make a self-referential "-mv" adjacency */
	public static Adjacency makeSelfMV(ProvObj po)
	{
		Adjacency adj = new Adjacency(new Loc(po.getSN(), "mv"));
		adj.adjLocs.add(new Loc(po.getSN(), null));
		return adj;
	}// makeSelfMV()
	
	public static Adjacency makeAdj(final FileLine line)
	throws IOException
	{
		Loc firstLoc = null;
		
		StringTokenizer st = new StringTokenizer(line.getLine(), ": \t\n\r");
		if(st.hasMoreTokens())
		{
			firstLoc = Loc.makeLoc(st.nextToken(), line);
		}
		else
		{
			line.makeError("Cannot parse location.");
		}
		
		if(firstLoc.isMX())
		{
			line.makeError("Cannot have an -mx coast as an adjacency type; it can only be in adjency list following the colon.");
		}
		
		Adjacency adj = new Adjacency(firstLoc);
		
		while(st.hasMoreTokens())
		{
			Loc tmp = Loc.makeLoc(st.nextToken(), line);
			if(tmp.isMX())
			{
				adj.hasMX = true;
			}
			
			adj.adjLocs.add(tmp);
		}
		
		if(adj.adjLocs.isEmpty())
		{
			line.makeError("No adjacent locations specified for this coast; omit the line if this is correct.");
		}
		
		return adj;
	}// makeAdj()
	
	/** Do we contain an 'mx' coast in the list of adjacent provinces? */
	public boolean hasMX()
	{
		return hasMX;
	}
	
	/** initial Loc */
	public Loc getLoc()
	{
		return loc;
	}
	
	/** List of adjacent locations */
    public List<Loc> getAdjLocs()
	{
		return Collections.unmodifiableList(adjLocs);
	}
	
	/** 
	*	Write adjacency info to XML.
	*/
	public String toXML()
	{
		StringBuilder sb = new StringBuilder(128);
		
		sb.append("\t<ADJACENCY type=\"");
		sb.append(getLoc().getCoastType());
		sb.append("\" refs=\"");
		
		Iterator iter = getAdjLocs().iterator();
		while(iter.hasNext())
		{
			sb.append( iter.next() );
			
			if(iter.hasNext())
			{
				sb.append(" ");
			}
		}
			
		sb.append("\"/>\n");
		return sb.toString();
	}// toXML()
	
	/** For debugging */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getName());
		sb.append("[");
		sb.append(loc);
		sb.append(",");
		sb.append(hasMX);
		sb.append(",");
		sb.append( adjLocs );
		return sb.toString();
	}// toSTring()
}// class Adjacency

