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
import dip.world.io.NameValuePair; 

import dip.world.metadata.ParticipantMetadata;
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
public class PlayerMetadataConverter extends ParticipantMetadataConverter
{
	
	
	public PlayerMetadataConverter(ClassMapper cm)
	{
		super(cm);
		cm.alias("player", PlayerMetadata.class, PlayerMetadata.class);
	}// ParticipantMetadataConverter()
	
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final PlayerMetadata pmd = (PlayerMetadata) source;
		final XMLSerializer xs = XMLSerializer.get(context);
		
		hsw.addAttribute("power", xs.toString(pmd.getPower()));
		
		write(pmd, xs, hsw, context);
	}// marshal()
	
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(PlayerMetadata.class);
	}// canConvert()
	
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final PlayerMetadata pmd = new PlayerMetadata(
			xs.getPower( reader.getAttribute("power") ));
			
		return read(pmd, xs, reader, context);
	}// unmarshal()
	
}// PlayerMetadataConverter
