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

import dip.world.World;
import dip.world.World.VariantInfo;
import dip.world.RuleOptions;
import dip.world.VictoryConditions;

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
		
		xs.lookupAndWriteNode(vi.getVictoryConditions(), cm, hsw, context);
		
		xs.lookupAndWriteNode(vi.getRuleOptions(), cm, hsw, context);
	}// marshal()
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(VariantInfo.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final VariantInfo vi = new World.VariantInfo();
		
		Map elements = new HashMap();
		
		vi.setVariantName( xs.getString(reader.getAttribute("name"), null) );
		vi.setVariantVersion( xs.getFloat(reader.getAttribute("version"), 1.0f) );
		
		while(reader.hasMoreChildren())
		{
			reader.moveDown();
			final String nodeName = reader.getNodeName();
			
			if("symbols".equals(nodeName))
			{
				vi.setSymbolPackName( xs.getString(reader.getAttribute("name"), null) );
				vi.setSymbolPackVersion( xs.getFloat(reader.getAttribute("version"), 1.0f) );
			}
			else if("map".equals(nodeName))
			{
				vi.setMapName( xs.getString(reader.getAttribute("name"), null) );
			}
			else
			{
				final Class cls = cm.lookupType(nodeName);
				final Object obj = context.convertAnother(context, cls);
				
				if(obj instanceof RuleOptions)
				{
					vi.setRuleOptions((RuleOptions) obj);
				}
				else if(obj instanceof VictoryConditions)
				{
					// NOTE: we need to fix the victoryconditions object,
					// since we haven't defined the initial-year yet.
					// This can only be done after we read the first Turn,
					// which contains the initial year for the game.
					vi.setVictoryConditions((VictoryConditions) obj);
				}
			}
			
			reader.moveUp();
		}
		
		return vi;
	}// unmarshal()		
}// VariantInfoConverter
