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

import dip.world.Power;
import dip.process.Adjustment;
import dip.process.Adjustment.AdjustmentInfo;
import dip.process.Adjustment.AdjustmentInfoMap;
import dip.order.Orderable;

import dip.misc.Log;

import java.util.*;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;

/**
*	XStream Converter
*/
public class AdjustmentInfoMapConverter implements Converter
{
	
	public AdjustmentInfoMapConverter(ClassMapper cm)
	{
		cm.alias("adjustments", AdjustmentInfoMap.class, AdjustmentInfoMap.class);
	}// AdjustmentInfoMapConverter()
	
	public void marshal(Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final AdjustmentInfoMap aim = (AdjustmentInfoMap) source;
		
		Iterator iter = aim.iterator();
		while(iter.hasNext())
		{
			final Map.Entry entry = (Map.Entry) iter.next();
			final AdjustmentInfo ai = (AdjustmentInfo) entry.getValue();
			
			hsw.startNode("adjustment");
			hsw.addAttribute("power", xs.toString((Power) entry.getKey()));
			hsw.addAttribute("scCount", String.valueOf(ai.getSupplyCenterCount()));
			hsw.addAttribute("unitCount", String.valueOf(ai.getUnitCount()));
			hsw.addAttribute("adjust", String.valueOf(ai.getAdjustmentAmount()));
			hsw.endNode();
		}
	}// marshal()
	
	public boolean canConvert(Class type)
	{
		return type.equals(AdjustmentInfoMap.class);
	}// canConvert()
	
	/** Returns null. We don't need to read in Adjustments for jDip */
	public Object unmarshal(HierarchicalStreamReader reader, 
		UnmarshallingContext context)
	{
		return null;
	}// unmarshal()
	
	
}// class AdjustmentInfoMapConverter

