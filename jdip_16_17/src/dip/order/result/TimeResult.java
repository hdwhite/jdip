//	
//	@(#)Result.java	4/2002
//	
//	Copyright 2002 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
//	
//	This program is free software; you can redistribute it and/or modify
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order.result;


import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.world.Power;
import dip.misc.Utils;

import java.util.Date;

import dip.world.io.XMLSerializer;
import dip.world.io.converter.ResultConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.converters.ConversionException;

/**
*		TimeResult<p> 
*		Timestamp result, with an optional message.
*		Time is always in UTC.
*/
public class TimeResult extends Result
{
	// instance variables
	protected long timeStamp;	// milliseconds since midnight, January 1, 1970 UTC.
	
	
	/** A TimeStamped result, applicable to a particular power. 
	*	<p>
	*	Note that resource must correspond to an il8n resource!
	*
	*/
	public TimeResult(Power power, String resource)
	{
		super(power, resource);
		
		// create timestamp
		timeStamp = System.currentTimeMillis();
	}// Result()
	
	
	/** A TimeStamped result, applicable to all powers. */
	public TimeResult(String resource)
	{
		this(null, resource);
	}// Result()
	
	
	/** Get the milliseconds since midnight, January 1, 1970 UTC. */
	public long getGMTMillis()
	{
		return timeStamp;
	}// getGMTMillis()
	
	
	/** 
	*	Converts the Resource to a properly-internationlized text message.
	*	argument {0} is always the time. 
	*/
	public String getMessage(OrderFormatOptions ofo)
	{
		return Utils.getLocalString(message, new Date(timeStamp));
	}// getMessage()
	
	
	/** Convert the output to a String */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(128);
		
		if(power == null)
		{
			sb.append("(none)");
		}
		else
		{
			sb.append(power);
		}
		
		sb.append(": ");
		sb.append(getMessage());
		return sb.toString();
	}// toString()
	
	
	/** For XStream serialization */
	public static class TimeResultConverter implements Converter
	{
		
		public TimeResultConverter(ClassMapper cm)
		{
			cm.alias("timeResult", TimeResult.class, TimeResult.class);
		}
		
		
		public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
			MarshallingContext context)
		{
			final XMLSerializer xs = XMLSerializer.get(context);
			final TimeResult timeResult = (TimeResult) source;
			
			ResultConverter.writeBasic(timeResult, xs, hsw);
			
			hsw.startNode("time");
			hsw.setValue(String.valueOf(timeResult.getGMTMillis()));
			hsw.endNode();
		}// marshal()
		
		public boolean canConvert(java.lang.Class type)
		{
			return type.equals(TimeResult.class);
		}// canConvert()
		
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
		{
			final XMLSerializer xs = XMLSerializer.get(context);
			
			final Power power = xs.getPower( reader.getAttribute("power") );
			
			assert(reader.hasMoreChildren());
			
			reader.moveDown();
			final String message = xs.getString( reader.getValue() );
			reader.moveUp();
			
			assert(reader.hasMoreChildren());
			
			reader.moveDown();
			final long time = xs.getLong( reader.getValue(), 0L );
			reader.moveUp();
			
			TimeResult tr = new TimeResult(power, message);
			tr.timeStamp = time;
			return tr;
		}// unmarshal()	
	}// TimeResultConverter

}// class TimeResult
