//
//  @(#)DislodgedResultConverter.java		9/2004
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

import java.io.*;
import java.util.*;
import java.lang.reflect.*;


import dip.order.*;
import dip.order.result.*;

import dip.world.Power;
import dip.world.Province;
import dip.world.Location;
import dip.world.io.XMLSerializer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.converters.ConversionException;

public class DislodgedResultConverter extends OrderResultConverter
{
	
	public DislodgedResultConverter(ClassMapper cm)
	{
		super(cm);
	}// DislodgedResultConverter()
	
	public void alias(ClassMapper cm)
	{
		cm.alias("dislodgedOrderResult", DislodgedResult.class, DislodgedResult.class);
	}// alias()
	
	public void write(OrderResult orderResult, XMLSerializer xs,
		HierarchicalStreamWriter hsw, MarshallingContext context)
	{
		DislodgedResult dr = (DislodgedResult) orderResult;
		
		hsw.startNode("atk");
		hsw.setValue(String.valueOf( dr.getAttackStrength() ));
		hsw.endNode();
		
		hsw.startNode("def");
		hsw.setValue(String.valueOf( dr.getDefenseStrength() ));
		hsw.endNode();
		
		hsw.startNode("dislodger");
		hsw.setValue( xs.toString(dr.getDislodger()) );
		hsw.endNode();
		
		hsw.startNode("retreats");
		hsw.setValue( xs.toString(dr.getRetreatLocations()) );
		hsw.endNode();
	}// write()
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(DislodgedResult.class);
	}// canConvert()
	
	public Object read(Power power, OrderResult.ResultType type, Orderable order, String message,
		XMLSerializer xs, HierarchicalStreamReader reader, UnmarshallingContext context)
	{
		DislodgedResult result = null;
		
		while(reader.hasMoreChildren())
		{
			reader.moveDown();
			
			int atk = -1;
			int def = -1;
			Province dislodger = null;
			Location[] retreats = null;
			
			final String nodeName = reader.getNodeName();
			
			if("atk".equals(nodeName))
			{
				atk = xs.getInt(reader.getValue(), -1);
			}
			else if("def".equals(nodeName))
			{
				def = xs.getInt(reader.getValue(), -1);
			}
			else if("dislodger".equals(nodeName))
			{
				dislodger = xs.getProvince(reader.getValue());
			}
			else if("retreats".equals(nodeName))
			{
				List list = xs.getLocations(reader.getValue());
				if(list != null)
				{
					retreats = (Location[]) list.toArray(new Location[list.size()]);
				}
			}
			
			result = new DislodgedResult(order, message, retreats);
			result.setAttackStrength(atk);
			result.setDefenseStrength(def);
			result.setDislodger(dislodger);
			
			reader.moveUp();
		}
		
		return result;
	}// read()
	
}// class DislodgedResultConverter


