//
//  @(#)PlayerMetadataConverter.java		9/2004
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

import dip.world.metadata.PlayerMetadata;

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
public class PlayerMetadataConverter implements Converter
{
	private final ClassMapper cm;
	
	public PlayerMetadataConverter(ClassMapper cm)
	{
		this.cm = cm;
		cm.alias("player", PlayerMetadata.class, PlayerMetadata.class);
	}// PlayerMetadataConverter()
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final PlayerMetadata pmd = (PlayerMetadata) source;
		final XMLSerializer xs = XMLSerializer.get(context);
		
		hsw.addAttribute("power", xs.toString(pmd.getPower()));
		hsw.addAttribute("type", xs.toString(pmd.getType()));
		
		xs.writeNVP("name", pmd.getName(), cm, hsw, context);
		xs.writeNVP("nick", pmd.getNickname(), cm, hsw, context);
		xs.writeNVP("uri", xs.toString(pmd.getURI()), cm, hsw, context);
		
		final String[] emails = pmd.getEmailAddresses();
		for(int i=0; i<emails.length; i++)
		{
			final String name = (i == 0) ? "email" : "email"+String.valueOf(i);
			xs.writeNVP(name, emails[i], cm, hsw, context);
		}
		
		hsw.startNode("notes");
		hsw.setValue(pmd.getNotes());
		hsw.endNode();
	}// marshal()
	
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(PlayerMetadata.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		return null;
	}// unmarshal()		
}// PlayerMetadataConverter
