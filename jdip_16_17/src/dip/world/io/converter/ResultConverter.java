//
//  @(#)ResultConverter.java		9/2004
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

import dip.world.io.XMLSerializer;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.converters.ConversionException;

public class ResultConverter implements Converter
{
	
	
	public ResultConverter(ClassMapper cm)
	{
        cm.alias("result", Result.class, Result.class);
	}
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final Result result = (Result) source;
		
		writeBasic(result, xs, hsw);
	}// marshal()
	
	public static void writeBasic(Result result, XMLSerializer xs, HierarchicalStreamWriter hsw)
	{
		hsw.addAttribute("power", xs.toString( result.getPower() ));
		hsw.startNode("message");
		hsw.setValue(result.getMessage());
		hsw.endNode();
	}// writeBasic()
	
	public boolean canConvert(Class type)
	{
        return type.equals(Result.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		return null;
	}// unmarshal()	
}// ResultConverter
