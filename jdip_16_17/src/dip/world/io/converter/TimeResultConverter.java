//
//  @(#)TimeResultConverter.java		9/2004
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

import dip.world.*;
import dip.order.*;
import dip.order.result.*;

import dip.world.io.XMLSerializer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.converters.ConversionException;

public class TimeResultConverter implements Converter
{
	
	
	public TimeResultConverter(ClassMapper cm)
	{
		cm.alias("timeResult", TimeResult.class, TimeResult.class);
	}
	
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final TimeResult timeResult = (TimeResult) source;
		
		ResultConverter.writeBasic(timeResult, xs, hsw);
		
		hsw.startNode("time");
		hsw.setValue(String.valueOf(timeResult.getGMTMillis()));
		hsw.endNode();
		
	}// marshal()
	
	public boolean canConvert(java.lang.Class type)
	{
        return type.equals(TimeResult.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		
		final Power power = xs.getPower( reader.getAttribute("power") );
		
		assert(reader.hasMoreChildren());
		
		/*
		
		reader.moveDown();
		final String message = xs.getString( reader.getValue() );
		reader.moveUp();
		
		
		*/
		
		System.out.println("TODO: TimeResult: needs to set time; must be internal static class....");
		
		return null;
	}// unmarshal()	
}// ResultConverter
