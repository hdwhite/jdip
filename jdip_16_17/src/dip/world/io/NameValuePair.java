//
//  @(#)NameValuePair.java		9/2004
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
package dip.world.io;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;

/**
*	
*	A class which holds a Name and Value.
*	
*/
public class NameValuePair implements Comparable
{
	private final String name;
	private final String value;
	
	/**
	*	Create a Name-Value pairing.
	*/
	public NameValuePair(String name, String value)
	{
		if(name == null)
		{
			throw new IllegalArgumentException();
		}
		
		this.name = name;
		this.value = value;
	}// NameValuePair()
	
	/** Get the Name */
	public String getName()
	{
		return name;
	}// getName()
	
	/** Get the Value */
	public String getValue()
	{
		return value;
	}// getValue()
	
	
	/** Compare to another (based on Name value only) */
	public int compareTo(Object o) 
	{
		NameValuePair nvp = (NameValuePair) o;
		return this.getName().compareTo(nvp.getName());
	}// compareTo()
	
	
	/**
	*	XStream Converter
	*/
	public static class NameValuePairConverter implements Converter
	{
		private final ClassMapper cm;
		
		public NameValuePairConverter(ClassMapper cm)
		{
			this.cm = cm;
			cm.alias("valuePair", NameValuePair.class, NameValuePair.class);
		}// NameValuePairConverter()
		
		public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
			MarshallingContext context)
		{
			final NameValuePair nvp = (NameValuePair) source;
			final XMLSerializer xs = XMLSerializer.get(context);
			
			hsw.addAttribute("name", xs.toString(nvp.getName()));
			hsw.addAttribute("value", xs.toString(nvp.getValue()));
		}// marshal()
		
		public boolean canConvert(java.lang.Class type)
		{
			return type.equals(NameValuePair.class);
		}// canConvert()
		
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
		{
			return null;
		}// unmarshal()		
	}// class NameValuePairConverter

}// NameValuePair
