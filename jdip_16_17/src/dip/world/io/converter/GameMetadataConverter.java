//
//  @(#)GameMetadataConverter.java		9/2004
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

import dip.world.metadata.GameMetadata;

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
public class GameMetadataConverter implements Converter
{
	private final ClassMapper cm;
	
	public GameMetadataConverter(ClassMapper cm)
	{
		this.cm = cm;
		cm.alias("gameInfo", GameMetadata.class, GameMetadata.class);
	}// GameMetadataConverter()
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final GameMetadata gmd = (GameMetadata) source;
		final XMLSerializer xs = XMLSerializer.get(context);
		
		writeNode(hsw, "name", gmd.getGameName());
		writeNode(hsw, "gameID", gmd.getGameID());
		writeNode(hsw, "URI", xs.toString(gmd.getGameURI()));
		writeNode(hsw, "judge", xs.toString(gmd.getJudgeName()));
		writeNode(hsw, "comment", gmd.getComment());
		writeNode(hsw, "notes", gmd.getNotes());
	}// marshal()
	
	private void writeNode(HierarchicalStreamWriter hsw, String name, String value)
	{
		hsw.startNode(name);
		hsw.setValue(value);
		hsw.endNode();
	}// writeNode()
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(GameMetadata.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		return null;
	}// unmarshal()		
}// GameMetadataConverter
