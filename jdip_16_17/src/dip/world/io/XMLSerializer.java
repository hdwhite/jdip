//
//  @(#)XMLSerializer.java		9/2004
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

import dip.world.*;

import dip.world.io.converter.*;

import dip.order.Orderable;
import dip.order.result.Result;

import java.io.Writer;
import java.io.Reader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.net.URI;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
*	Serializes World objects to XML, and vice-versa. This method
*	is the preferred method for saved games. XML files are compressed
*	with GZIP (via a GZIPOutputStream) automatically.
*	
*/
public class XMLSerializer
{
	/** Specification Level : 1.0 */
	public static final String SPECIFICATION_FORMAT_1_0 = "FORMAT_1_0";
	
	
	private static final String KEY_SERIALIZER = "KEY_SERIALIZER";
	private static final String VALUE_NONE = "none";
	private static final String VALUE_UNDEFINED = "undefined";
	private static final List converterList = new ArrayList();
	private static int BUFFER_SIZE = 2048;
	
	// instance variables
	private World world = null;
	private dip.world.Map map = null;
	private int turnNum = 0;
	private IdentityHashMap orderMap;
	private int unique = 0;
	
	/** Create an XMLSerializer */
	private XMLSerializer()
	{
		orderMap = new IdentityHashMap(61);
	}// XMLSerializer()
	
	
	/**
	*	Write the World object to the given Writer.
	*	The stream is not closed when complete. No compression is applied.
	*	<p>
	*	Null arguments are not permitted.
	*/
	public static void toXML(World world, Writer writer,
		String creatorName, String creatorVersion, String specification)
	throws IOException
	{
		XStream xstream = new XStream();
		xstream.setMode(XStream.NO_REFERENCES);
		registerConverters(xstream, creatorName, creatorVersion, specification);
		writeHeader(writer);
		xstream.toXML(world, writer);
	}// toXML()
	
	
	/**
	*	Write the World object to a File.
	*	Null arguments are not permitted.
	*/
	public static void toXML(World world, File file, boolean compress,
		String creatorName, String creatorVersion, String specification)
	throws IOException
	{
		OutputStreamWriter osw = null;
		
		try
		{
			GZIPOutputStream gzos = null;
			
			if(compress)
			{
				gzos = new GZIPOutputStream(new FileOutputStream(file), BUFFER_SIZE);
				osw = new OutputStreamWriter(gzos);
			}
			else
			{
				osw = new OutputStreamWriter(new BufferedOutputStream(
					new FileOutputStream(file), BUFFER_SIZE));
			}
			
			toXML(world, osw, creatorName, creatorVersion, specification);
			osw.flush();
			
			if(compress)
			{
				gzos.finish();
			}
		}
		catch(IOException ioe)
		{
			throw ioe;
		}
		catch(Exception e)
		{
			// wrap non-IOExceptions in an IOException, since we are doing IO
			IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		}
		finally
		{
			if(osw != null)
			{
				osw.close();
			}
		}
	}// toXML()
	
	
	/**
	*	Register the given Converter with XMLSerializer. All Converters
	*	must be subclasses of AbstractConverter, to ensure proper 
	*	initialization.
	*/
	public static void registerConverter(AbstractConverter converter)
	{
		if(!converterList.contains(converter))
		{
			converterList.add(converter);
		}
	}// registerConverter()
	
	/**
	*	Add the &lt;?xml...%gt; header that is expected by most XML parsers.
	*/
	private static void writeHeader(Writer w)
	throws IOException
	{
		w.write("<?xml version=\"1.0\"?>\n");
		// TODO: write the DTD or doctype reference....
	}// writeHeader()
	
	/**
	*	Read the World object from a given Reader.
	*/
	public static World fromXML(Reader reader)
	{
		return null;
	}// fromXML()
	
	
	/**
	*	Allows Converters to get the XMLSerializer.
	*/
	public synchronized static XMLSerializer get(DataHolder dh)
	{
		XMLSerializer xms = (XMLSerializer) dh.get(KEY_SERIALIZER);
		if(xms == null)
		{
			xms = new XMLSerializer();
			dh.put(KEY_SERIALIZER, xms);
		}
		
		return xms;
	}// get()
	
	
	
	/** Convert a String to a Province */
	public Province getProvince(String in)
	{
		if(map == null) { throw new IllegalStateException(); }
		return map.getProvince(in);
	}// getProvince()
	
	/** Convert a String to a Location */
	public Location getLocation(String in)
	{
		if(map == null) { throw new IllegalStateException(); }
		return map.parseLocationStrict(in);
	}// getLocation()
	
	/** Convert a String to a Power */
	public Power getPower(String in)
	{
		if(map == null) { throw new IllegalStateException(); }
		return map.getPower(in);
	}// getPower()
	
	/** Convert a String to a Location */
	public Unit.Type getUnitType(String in)
	{
		return Unit.Type.parse(in);
	}// getUnitType()
	
	
	/** Null-safe toString(); null is converted to "none" */
	public String toString(String s)
	{
		if(s != null)
		{
			return s;
		}
		
		return VALUE_NONE;
	}// toString()
	
	/** Convert a Province to a String. Null values are converted to "none" */
	public String toString(Province province)
	{
		if(province != null)
		{
			return province.getShortName();
		}
		return VALUE_NONE;
	}// toString()
	
	/** Convert a Location to a String. Null values are converted to "none" */
	public String toString(Location loc)
	{
		if(loc != null)
		{
			StringBuffer sb = new StringBuffer(8);
			sb.append(loc.getProvince().getShortName());
			if(loc.getCoast().isDirectional())
			{
				sb.append('/');
				sb.append(loc.getCoast().getAbbreviation());
			}
			return sb.toString();
		}
		return VALUE_NONE;
	}// toString()
	
	/** Convert a Power to a String. Null values are converted to "none" */
	public String toString(Power power)
	{
		if(power != null)
		{
			return power.getName();
		}
		return VALUE_NONE;
	}// toString()
	
	/** Convert a Unit.Type to a String. Null values are converted to "none" */
	public String toString(Unit.Type unitType)
	{
		if(unitType != null)
		{
			return unitType.getInternalName();
		}
		return VALUE_NONE;
	}// toString()
	
	/** 
	*	Format an Array of Locations into a String (separated by a space).
	*	Note that null becomes "undefined" while a zero-length array
	*	becomes "none".
	*/
	public String toString(final Location[] locs)
	{
		if(locs == null)
		{
			return VALUE_UNDEFINED;
		}
		else if(locs.length == 0)
		{
			return VALUE_NONE;
		}
		
		// e.g.: "xxx/nc yyy zzz/sc"
		StringBuffer sb = new StringBuffer(64);
		for(int i=0; i<locs.length; i++)
		{
			sb.append( toString(locs[i]) );
			if((i+1)<locs.length)
			{
				sb.append(' ');
			}
		}
		return sb.toString();
	}// toString()
	
	/** 
	*	Format an Array of Provinces into a String (separated by a space).
	*	Note that null becomes "undefined" while a zero-length array
	*	becomes "none".
	*/
	public String toString(final Province[] provs)
	{
		if(provs == null)
		{
			return VALUE_UNDEFINED;
		}
		else if(provs.length == 0)
		{
			return VALUE_NONE;
		}
		
		// e.g.: "xxx yyy zzz"
		StringBuffer sb = new StringBuffer(64);
		for(int i=0; i<provs.length; i++)
		{
			sb.append( toString(provs[i]) );
			if((i+1)<provs.length)
			{
				sb.append(' ');
			}
		}
		
		return sb.toString();
	}// toString()

	
	/** 
	*	Get a reference to an Orderable. If the Orderable has not been
	*	prevously defined, null is returned. However, if the Orderable
	*	itself is null, "none" is returned.
	*/
	public String toString(Orderable order)
	{
		if(order == null)
		{
			return VALUE_NONE;
		}
		else
		{
			return (String) getOrderMap().get(order);
		}
	}// toString()
	
	/** Convert a Power to a String. Null values are converted to "none". */
	public String toString(URI uri)
	{
		if(uri == null)
		{
			return VALUE_NONE;
		}
		
		return uri.toString();
	}// toString()
	
	
	/** Write an Order, framed appropriately with start/end elements. */
	public void writeOrder(Orderable order, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		hsw.startNode("order");
		context.convertAnother(order);
		hsw.endNode();
	}// writeOrder()
	
	
	/** Write a NameValuePair if the value is <b>not</b> null, empty, or "none". */
	public void writeNVP(String name, String value, ClassMapper cm, 
		HierarchicalStreamWriter hsw, MarshallingContext context)
	{           
		if(value != null && !"".equals(value) && !VALUE_NONE.equals(value))
		{
			final NameValuePair nvp = new NameValuePair(name, value);
			lookupAndWriteNode(nvp, cm, hsw, context);
		}
	}// writeNVP()
	
	/** 
	*	Writes out a Node, using the ClassMapper to get the node 
	*	name, and convertAnother to write the object. This is similar
	*	to the following:
	*	<code>
	*		hsw.startNode(cm.lookupName(object.getClass()));<br>
	*		context.convertAnother(object);<br>
	*		hsw.endNode();<br>
	*	</code>
	*/
	public void lookupAndWriteNode(final Object obj, 
		final ClassMapper cm, final HierarchicalStreamWriter hsw, 
		final MarshallingContext context)
	{
		hsw.startNode(cm.lookupName(obj.getClass()));
		context.convertAnother( obj );
		hsw.endNode();
	}// lookupAndWriteNode()
	
	
	/** Get the dip.world.Map object */
	public dip.world.Map getMap()
	{
		return map;
	}// getMap()
	
	/** Get the dip.world.World object */
	public World getWorld()
	{
		return world;
	}// getWorld()
	
	/** Get the HashMap which maps Orderable objects to unique IDs */
	public IdentityHashMap getOrderMap()
	{
		return orderMap;
	}// getOrderMap()
	
	/** Get the current Turn number. Never negative. */
	public synchronized int getTurnNumber()
	{
		return turnNum;
	}// getTurnNumber()
	
	/** Increment the Turn number by 1. */
	public synchronized void incrementTurnNumber()
	{
		turnNum++;
	}// setTurnNumber()
	
	
	/** Sets the World. This may be set only once. */
	public void setWorld(World w)
	{
		if(w == null) { throw new IllegalArgumentException(); }
		if(world != null) { throw new IllegalStateException(); }
		world = w;
	}// setWorld()
	
	/** Sets the Map. This may be set only once. */
	public void setMap(dip.world.Map worldMap)
	{
		if(worldMap == null) { throw new IllegalArgumentException(); }
		if(map != null) { throw new IllegalStateException(); }
		map = worldMap;
	}// setMap()
	
	
	/** 
	*	Get a unique ID (for the life of an XMLSerializer).
	*	This returns an increasing integer, starting with 0.
	*/
	public synchronized int getUnique()
	{
		int val = unique;
		unique++;
		return val;
	}// getUnique()
	
	
	
	/** Register default converters */
	private static void registerConverters(XStream xstream, String creator,
		String creatorVersion, String specification)
	{
		final ClassMapper cm = xstream.getClassMapper();
		
		// basic types
		xstream.registerConverter(new PhaseConverter(cm));
		xstream.registerConverter(new ProvinceConverter(cm));
		xstream.registerConverter(new LocationConverter(cm));
		xstream.registerConverter(new UnitTypeConverter(cm));
		xstream.registerConverter(new NameValuePair.NameValuePairConverter(cm));
		
		// main turn types
		xstream.registerConverter(new Position.PositionConverter(cm));
		xstream.registerConverter(new Position.ProvinceDataConverter(cm));
		xstream.registerConverter(new TurnStateConverter(cm));
		
		xstream.registerConverter(new WorldConverter(cm,
			creator, creatorVersion, specification));
		
		xstream.registerConverter(new VariantInfoConverter(cm));
		xstream.registerConverter(new RuleOptionsConverter(cm));
		xstream.registerConverter(new VictoryConditionsConverter(cm));
		
		// for *all* Orderable subclasses
		xstream.registerConverter(new OrderableConverter());
		
		// results
		xstream.registerConverter(new ResultConverter(cm));
		xstream.registerConverter(new TimeResultConverter(cm));
		
		// order results
		xstream.registerConverter(new OrderResultConverter(cm));
		xstream.registerConverter(new BouncedResultConverter());
		xstream.registerConverter(new ConvoyPathResultConverter());
		xstream.registerConverter(new DependentMoveFailedResultConverter());
		xstream.registerConverter(new DislodgedResultConverter());
		xstream.registerConverter(new SubstitutedResultConverter());
		
		// metadata
		xstream.registerConverter(new GameMetadataConverter(cm));
		xstream.registerConverter(new PlayerMetadataConverter(cm));
		
		// registered converters
		Iterator iter = converterList.iterator();
		while(iter.hasNext())
		{
			AbstractConverter converter = (AbstractConverter) iter.next();
			converter.setClassMapper(cm);
			converter.alias();
			xstream.registerConverter(converter);
		}
	}// registerConverters()
	
}// class XMLSerializer
