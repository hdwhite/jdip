//
//  @(#)AbstractConverter.java		9/2004
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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;

/**
*	XStream AbstractConverter
*	<p>
*	This is for converters registered using the 
*	<code>XMLSerializer.registerConverter()</code> method.
*	<p>
*	ClassMapper is guaranteed to be set before any marshal() or unmarshal() 
*	call, and before alias() is called.
*/
public abstract class AbstractConverter implements Converter
{
	private ClassMapper cm;
	
	
	/** Sets the ClassMapper. It can only be set once. */
	public final void setClassMapper(ClassMapper cm)
	{
		this.cm = cm;
	}// setClassMapper()
	
	
	/** Returns the ClassMapper */
	final protected ClassMapper getCM()
	{
		return cm;
	}// getCM()
	
	
	/** Override this method to set aliases. ClassMapper will have been set. */
	public void alias()
	{
	}// alias()
	
	public abstract void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context);
	
	public abstract boolean canConvert(java.lang.Class type);
	
	public abstract Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context); 
	
}// AbstractConverter
