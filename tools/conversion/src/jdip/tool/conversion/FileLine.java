//
//  @(#)FileLine.java 	2004
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

import java.lang.*;
import java.io.*;
import java.util.*;


/** 
*	Represents a Line of data, read from a file. 
*	This does it's own preparsing.
*/
class FileLine
{
	private final int lineNumber;
	private final String line;
	
	
	/** Constructor. No processing is done. */
	public FileLine(final String line, final int lineNumber)
	{
		this.line = line;
		this.lineNumber = lineNumber;
	}// FileLine()
	
	/**
	*	Returns a Line from a file. Returns 'null' if 
	*	line was comment. Otherwise, each line is trim()'d.
	*	If input was null, also returns null. 
	*/	
	public static FileLine makeFileLine(String in, int n)
	{
		if(n <= 0)
		{
			throw new IllegalArgumentException();
		}
		
		if(in == null)
		{
			return null;
		}
		else
		{
			final String txt = in.trim();
			if(txt.startsWith("#") || "".equals(txt))
			{
				return null;
			}
			else
			{
				return new FileLine(txt, n);
			}
		}
	}// makeFileLine()
	
	/** 
	*	Returns 'true' if a section marker; this is a "-1" in 
	*	the files that we parse. 
	*/
	public boolean isSectionMarker()
	{
		return ("-1".equals(line));
	}// isSectionMarker()
	
	/** 
	*	This will return '-1' if the line is a section marker. 
	*	This will *never* return a comment line.
	*/
	public String getLine()
	{
		return line;
	}
	
	
	public int getLineNumber()
	{
		return lineNumber;
	}
	
	public void makeError(String text)
	throws IOException
	{
		String msg 	 = "ERROR: "+text+"\n";
		msg 		+= "LINE: "+getLineNumber()+"\n";
		msg 		+= getLine()+"\n";
		throw new IOException(msg);
	}// makeError()
}// inner class FileLine
