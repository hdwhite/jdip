//
//  @(#)VictoryConditionsConverter.java		9/2004
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

import dip.world.VictoryConditions;

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
public class VictoryConditionsConverter implements Converter
{
	
	
	public VictoryConditionsConverter(ClassMapper cm)
	{
		cm.alias("victoryConditions", VictoryConditions.class, VictoryConditions.class);
	}
	
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		VictoryConditions vc = (VictoryConditions) source;
		
		hsw.startNode("winningSupplyCenters");
		hsw.addAttribute("value", 
			String.valueOf(vc.getSCsRequiredForVictory()));
		hsw.endNode();
		
		hsw.startNode("yearsWithoutSCCapture");
		hsw.addAttribute("value",
			String.valueOf(vc.getYearsWithoutSCChange()));
		hsw.endNode();
		
		hsw.startNode("maxGameLengthInYears");
		hsw.addAttribute("value",
			String.valueOf(vc.getMaxGameDurationYears()));
		hsw.endNode();
		
		hsw.startNode("initialYear");
		hsw.addAttribute("value",
			String.valueOf(vc.getInitialYear()));
		hsw.endNode();
	}// marshal()
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(VictoryConditions.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		final XMLSerializer xs = XMLSerializer.get(context);
        
		Map elements = new HashMap();
		
		while(reader.hasMoreChildren())
		{
            reader.moveDown();
            elements.put(reader.getNodeName(), Integer.valueOf(reader.getAttribute("value")));
            reader.moveUp();
		}
		
		return new VictoryConditions(
			((Integer) elements.get("winningSupplyCenters")).intValue(),
			((Integer) elements.get("yearsWithoutSCCapture")).intValue(),
			((Integer) elements.get("maxGameLengthInYears")).intValue(),
			((Integer) elements.get("initialYear")).intValue()
		);
	}// unmarshal()		
}// VictoryConditionsConverter
