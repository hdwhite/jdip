//
//  @(#)ConvoyPathResultConverter.java		9/2004
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
import dip.world.io.XMLSerializer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.converters.ConversionException;

public class ConvoyPathResultConverter extends OrderResultConverter
{
	
	public ConvoyPathResultConverter(ClassMapper cm)
	{
		super(cm);
	}// ConvoyPathResultConverter()
	
	public void alias(ClassMapper cm)
	{
		cm.alias("convoyPathOrderResult", ConvoyPathResult.class, ConvoyPathResult.class);
	}// alias()
	
	public void write(OrderResult orderResult, XMLSerializer xs,
		HierarchicalStreamWriter hsw, MarshallingContext context)
	{
		ConvoyPathResult cpr = (ConvoyPathResult) orderResult;
		
		hsw.startNode("path");
		hsw.setValue( xs.toString(cpr.getConvoyPath()) );
		hsw.endNode();
	}// write()
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(ConvoyPathResult.class);
	}// canConvert()
	
	public Object read(Power power, OrderResult.ResultType type, Orderable order, String message,
		XMLSerializer xs, HierarchicalStreamReader reader, UnmarshallingContext context)
	{
		OrderResult result = null;
		
		if(!reader.hasMoreChildren())
		{
			throw new ConversionException("truncated convoy path result");
		}
		
		reader.moveDown();
		result = new ConvoyPathResult(order, xs.getProvinces(reader.getValue()));
		reader.moveUp();
		
		return result;
	}// read()
	
}// class ConvoyPathResultConverter


