//
//  @(#)OrderResultConverter.java		9/2004
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

import dip.world.Power;

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

public class OrderResultConverter implements Converter
{
	
	
	public OrderResultConverter(ClassMapper cm)
	{
		alias(cm);
	}// OrderResultConverter()
	
	
	protected void alias(ClassMapper cm)
	{
		cm.alias("orderResult", OrderResult.class, OrderResult.class);
	}// alias()
	
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final OrderResult orderResult = (OrderResult) source;
		final Class type = source.getClass();
		
		hsw.addAttribute("power", xs.toString(orderResult.getPower()));
		hsw.addAttribute("category", orderResult.getResultType().toString());
		
		// orderID can be null for substituted orders, if a power didn't 
		// submit an order.
		hsw.addAttribute("order", xs.toString(orderResult.getOrder()));
				
		hsw.startNode("message");
		hsw.setValue(orderResult.getMessage());
		hsw.endNode();
		
		write(orderResult, xs, hsw, context);
	}// marshal()
	
	/**
	*	Subclasses need to override this, to add additional
	*	information when marshalling.
	*/
	public void write(OrderResult orderResult, XMLSerializer xs,
		HierarchicalStreamWriter hsw, MarshallingContext context)
	{
	}// write()
	
	/**
	*	Subclasses also need to override this method.
	*/
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(OrderResult.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		
		final Power power = xs.getPower( reader.getAttribute("power") );
		final OrderResult.ResultType type = OrderResult.ResultType.parse( 
			xs.getString( reader.getAttribute("category") ));
		final Orderable order = xs.getOrder( reader.getAttribute("order") );
		
		if(type == null)
		{
			throw new ConversionException("unknown result type: "+type);
		}
		
		if(!reader.hasMoreChildren())
		{
			throw new ConversionException("truncated orderresult/subclass");
		}
		
		reader.moveDown();
		final String message = xs.getString( reader.getValue() );
		reader.moveUp();
		
		return read(power, type, order, message, xs, reader, context);
	}// unmarshal()
	
	/**
	*	Subclasses need to override this, to add additional
	*	information when marshalling.
	*/
	public Object read(Power power, OrderResult.ResultType type, Orderable order, 
		String message, XMLSerializer xs, HierarchicalStreamReader reader, 
		UnmarshallingContext context)
	{
		return new OrderResult(order, type, message);
	}// read()
	
}// OrderResultConverter
