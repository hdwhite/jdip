//
//  @(#)VariantInfoConverter.java		9/2004
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

import dip.world.World.VariantInfo;
import dip.world.World;

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
public class VariantInfoConverter implements Converter
{
	private final ClassMapper cm;
	
	public VariantInfoConverter(ClassMapper cm)
	{
		this.cm = cm;
		cm.alias("variant", VariantInfo.class, VariantInfo.class);
	}// VariantInfoConverter()
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final VariantInfo vi = (VariantInfo) source;
		final XMLSerializer xs = XMLSerializer.get(context);
		
		if(vi.getVariantName() == null)
		{
			throw new ConversionException("null variant name");
		}
		
		hsw.addAttribute("name", vi.getVariantName());
		hsw.addAttribute("version", String.valueOf(vi.getVariantVersion()));
		
		hsw.startNode("symbols");
		hsw.addAttribute("name", xs.toString(vi.getSymbolPackName()));
		hsw.addAttribute("version", String.valueOf(vi.getSymbolPackVersion()));
		hsw.endNode();
		
		hsw.startNode("map");
		hsw.addAttribute("name", xs.toString(vi.getMapName()));
		hsw.endNode();
		
		// victory conditions
		// (this is sort of a hack)
		// since it's not really part of this object....
		xs.lookupAndWriteNode(xs.getWorld().getVictoryConditions(), cm, hsw, context);
		
		// rule options
		xs.lookupAndWriteNode(vi.getRuleOptions(), cm, hsw, context);
	}// marshal()
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(VariantInfo.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		return null;
	}// unmarshal()		
}// VariantInfoConverter
