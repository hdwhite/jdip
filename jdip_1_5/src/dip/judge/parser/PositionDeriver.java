//
//  @(#)PositionDeriver.java	1.00	6/2002
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

import dip.world.Phase;
import dip.judge.parser.PositionParser.PositionInfo;
import dip.judge.parser.JudgeOrderParser.OrderInfo;

import java.io.*;
import java.util.regex.*;
import java.util.*;
/**
*
*	Creates PositionInfo array, as a PositionParser would, but from a set of
*	movement orders.
*	<p>
*	
*
*
*
*/
public class PositionDeriver
{
	// constants
	/** 
	*	Order-parsing regex.<br>
	* 	Capture groups: 1:power, 2:unit, 3:location<br>
	*	Note: reluctant qualifier +? REQUIRED after '.' for complex order matching<br>
	*	'stop' tokens are: -&gt;, hold, support, convoy<br>
	*	case insensitive, single-line.
	*/
	public static final String PARSE_REGEX = "(?i)^([\\p{Alnum}\\-\\_]+):\\s*([\\p{Alnum}\\-\\_]+)\\s+((.+?))\\s+(?:hold|support|convoy|->)";
		
	// instance variables
	private PositionInfo[] posInfo = null;
	
	/** Creates position information from the given orders. 
	*	This is only guaranteed to work for the Move phase. */
	public PositionDeriver(OrderInfo[] orderInfo)
	{
		if(orderInfo == null)
		{
			throw new IllegalArgumentException("null input");
		}
		
		derive(orderInfo);
	}// PositionParser()
	
	
	/** Returns PositionInfo, or a zero-length array. If array is not the same length as the orderInfo input, error! */
	public PositionInfo[] getPositionInfo()
	{
		return posInfo;
	}// getPositionInfo()
	
	
	/** 
	*	Derives positions from OrderInfo array
	*/
	private void derive(OrderInfo[] orderInfo)
	{
		List posList = new LinkedList();
		Pattern pr = Pattern.compile(PARSE_REGEX);
		
		for(int i=0; i<orderInfo.length; i++)
		{
			Matcher m = pr.matcher(orderInfo[i].getOrderText());
			
			if(m.find())
			{
				posList.add( new PositionInfo(
					m.group(1), 
					m.group(2), 
					ParserUtils.filter(m.group(3))) );
			}
			else
			{
				// parse failed; break out of loop
				break;
			}
		}
		
		posInfo = (PositionInfo[]) posList.toArray(new PositionInfo[posList.size()]);
	}// derive()
	
}// class PositionDeriver
