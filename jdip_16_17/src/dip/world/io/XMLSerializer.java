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
import dip.order.OrderFactory;
import dip.order.result.Result;
import dip.order.result.TimeResult;

import java.io.Writer;
import java.io.Reader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import java.net.URI;
import java.net.URISyntaxException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.*;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.alias.CannotResolveClassException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
*	Serializes World objects to XML, and vice-versa. This method
*	is the preferred method for saved games. XML files are compressed
*	with GZIP (via a GZIPOutputStream) automatically.
*	<p>
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
	private static final int BUFFER_SIZE = 2048;
	private static final String TOKENS = " ,";
	
	// instance variables
	private World world;
	private dip.world.Map map;
	private int turnNum;
	private final IdentityHashMap marshallingOrderMap;
	private final HashMap unmarshallingOrderMap;
	private int unique;
	private OrderFactory orderFactory;
	private TurnState turnState;
	
	/** Create an XMLSerializer */
	private XMLSerializer()
	{
		marshallingOrderMap = new IdentityHashMap(61);
		unmarshallingOrderMap = new HashMap(61);
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
		registerConverters(xstream, null, creatorName, creatorVersion, specification);
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
	public static World fromXML(Reader reader, OrderFactory orderFactory,
		String specification)
	{
		XStream xstream = new XStream();
		xstream.setMode(XStream.NO_REFERENCES);
		registerConverters(xstream, orderFactory, "", "", specification);
		return (World) xstream.fromXML(reader);
	}// fromXML()
	
	
	/**
	*	Read the World object from a given File. This automatically determines
	*	if the file is compressed (with GZIP), and if so, attempts to 
	*	uncompress it as it is reading it.
	*/
	public static World fromXML(File file, OrderFactory orderFactory, 
		String specification)
	throws IOException
	{
		if(file == null)
		{
			throw new IllegalArgumentException();
		}
		
		InputStreamReader isr = null;
		
		try
		{
			BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(file), BUFFER_SIZE);
			
			// read GZIP_MAGIC
			bis.mark(32);
			int magic = bis.read();
			magic = (bis.read() << 8) | magic;	// intel byte order
			bis.reset();
			
			// assign general InputStreamReader based on magic
			if(GZIPInputStream.GZIP_MAGIC == magic)
			{
				isr = new InputStreamReader(new GZIPInputStream(bis, BUFFER_SIZE));
			}
			else
			{
				isr = new InputStreamReader(bis);
			}
			
			return fromXML(isr, orderFactory, specification);
		}
		catch(IOException ioe)
		{
			throw ioe;
		}
		catch(Exception e)
		{
			
System.out.println(e);			
			
			// wrap non-IOExceptions in an IOException, since we are doing IO
			IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		}
		finally
		{
			if(isr != null)
			{
				isr.close();
			}
		}		
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
	
	/** Get a string. "none" is converted to null. */
	public String getString(String in)
	{
		if(VALUE_NONE.equals(in))
		{
			return null;
		}
		
		return in;
	}// getString()
	
	/** Get a URI. "none" or an invalid URI is converted to null. */
	public URI getURI(String in)
	{
		if(VALUE_NONE.equals(in))
		{
			return null;
		}
		
		try
		{
			return new URI(in);
		}
		catch(URISyntaxException e)
		{
			return null;
		}
	}// getURI()
	
	/** Convert a String to a Province. "none" converts to null.*/
	public Province getProvince(String in)
	{
		if(map == null) { throw new IllegalStateException(); }
		if(VALUE_NONE.equals(in))
		{
			return null;
		}
		return map.getProvince(in);
	}// getProvince()
	
	/** Convert a String to a Province. "none" converts to null.*/
	public Coast getCoast(String in)
	{
		if(map == null) { throw new IllegalStateException(); }
		if(VALUE_NONE.equals(in))
		{
			return null;
		}
		return Coast.parse(in);
	}// getProvince()
	
	/** Convert a String to a Coast. "none" converts to null. */
	public Location getLocation(String in)
	{
		if(map == null) { throw new IllegalStateException(); }
		if(VALUE_NONE.equals(in))
		{
			return null;
		}
		return map.parseLocationStrict(in);
	}// getLocation()
	
	/** Convert a String to a Power. "none" converts to null. */
	public Power getPower(String in)
	{
		if(map == null) { throw new IllegalStateException(); }
		if(VALUE_NONE.equals(in))
		{
			return null;
		}
		return map.getPower(in);
	}// getPower()
	
	/** Convert a String to a Location. "none" converts to null. */
	public Unit.Type getUnitType(String in)
	{
		if(VALUE_NONE.equals(in))
		{
			return null;
		}
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
	*	Format an Array of Powers into a String (separated by a space).
	*	Note that null becomes "undefined" while a zero-length array
	*	becomes "none".
	*/
	public String toString(final Power[] powers)
	{
		if(powers == null)
		{
			return VALUE_UNDEFINED;
		}
		else if(powers.length == 0)
		{
			return VALUE_NONE;
		}
		
		// e.g.: "xxx yyy zzz"
		StringBuffer sb = new StringBuffer(64);
		for(int i=0; i<powers.length; i++)
		{
			sb.append( toString(powers[i]) );
			if((i+1)<powers.length)
			{
				sb.append(' ');
			}
		}
		
		return sb.toString();
	}// toString()
	
	/** Parse a String of powers into a List */
	public List getPowers(String in)
	{
		if(VALUE_UNDEFINED.equals(in))
		{
			return null;
		}
		else if(VALUE_NONE.equals(in))
		{
			return Collections.EMPTY_LIST;
		}
		
		ArrayList list = new ArrayList();
		StringTokenizer st = new StringTokenizer(in, TOKENS);
		while(st.hasMoreTokens())
		{
			list.add( getPower(st.nextToken()) );
		}
		
		return list;
	}// getPowers()
	
	/** Parse a String of Provinces into a List */
	public List getProvinces(String in)
	{
		if(VALUE_UNDEFINED.equals(in))
		{
			return null;
		}
		else if(VALUE_NONE.equals(in))
		{
			return Collections.EMPTY_LIST;
		}
		
		ArrayList list = new ArrayList();
		StringTokenizer st = new StringTokenizer(in, TOKENS);
		while(st.hasMoreTokens())
		{
			list.add( getProvince(st.nextToken()) );
		}
		
		return list;
	}// getProvinces()
	
	/** Parse a String of Locations into a List */
	public List getLocations(String in)
	{
		if(VALUE_UNDEFINED.equals(in))
		{
			return null;
		}
		else if(VALUE_NONE.equals(in))
		{
			return Collections.EMPTY_LIST;
		}
		
		ArrayList list = new ArrayList();
		StringTokenizer st = new StringTokenizer(in, TOKENS);
		while(st.hasMoreTokens())
		{
			list.add( getLocation(st.nextToken()) );
		}
		
		return list;
	}// getProvinces()
	
	
	/** 
	*	Get a reference to an Orderable. If the Orderable has not been
	*	prevously defined, null is returned. However, if the Orderable
	*	itself is null, "none" is returned.
	*	<p>
	*	<b>This method should only be used during marshalling</b>
	*/
	public String toString(Orderable order)
	{
		if(order == null)
		{
			return VALUE_NONE;
		}
		else
		{
			return (String) getMarshallingOrderMap().get(order);
		}
	}// toString()
	
	/**
	*	Get an order reference; returns null if the 
	*	the input was "none". If no order reference
	*	was found, a ConversionException is thrown.
	*	<p>
	*	<b>This method should only be used during unmarshalling</b>
	*/
	public Orderable getOrder(String orderRef)
	{
		if(VALUE_NONE.equals(orderRef))
		{
			return null;
		}
		else
		{
			Orderable order = (Orderable) getUnmarshallingOrderMap().get(orderRef);
			if(order == null)
			{
				throw new ConversionException("no reference for order ID: "+orderRef);
			}
			return order;
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
	
	
	/**
	*	Reads a Node, using the ClassMapper to get the node name,
	*	name, and convertAnother to read the object. This is similar
	*	to the following:
	*	<code>
	*		String nodeName = reader.getNodeName();<br>
	*		Class cls = cm.lookupType(nodeName);<br>
	*		Object obj = context.convertAnother(reader, cls);<br>
	*	</code>
	*	<p>
	*	Note, however, that this will not throw an exception; instead,
	*	a <code>null</code> object will be returned.
	*/
	public Object lookupAndReadNode(final ClassMapper cm, 
		final HierarchicalStreamReader reader, final UnmarshallingContext context)
	{
		String nodeName = reader.getNodeName();
		
		try
		{
			Class cls = cm.lookupType(nodeName);
			return context.convertAnother(reader, cls);
		}
		catch(CannotResolveClassException e)
		{
			return null;
		}
	}// lookupAndReadNode()
	
	
	/** 
	*	Safely get a String value. If the value is empty or "none",
	*	the default value is used.
	*/
	public String getString(String value, String defaultValue)
	{
		if("".equals(value) || VALUE_NONE.equals(value))
		{
			return defaultValue;
		}
		
		return value;
	}// getString()
	
	/**
	*	Safely get an float value. If the value is empty or "none",
	*	or conversion to a float fails, the default value is used.
	*/
	public float getFloat(String value, float defaultValue)
	{
		if(value != null && value.length() > 0)
		{
			try
			{
				return Float.parseFloat(value);
			}
			catch(NumberFormatException e)
			{
			}
		}
		
		return defaultValue;
	}// getFloat()
	
	
	/**
	*	Safely get an Integer value. If the value is empty or "none",
	*	or conversion to an Integer fails, the default value is used.
	*/
	public int getInt(String value, int defaultValue)
	{
		if(value != null && value.length() > 0)
		{
			try
			{
				return Integer.parseInt(value);
			}
			catch(NumberFormatException e)
			{
			}
		}
		
		return defaultValue;
	}// getInt()
	
	/**
	*	Safely get a Long value. If the value is empty or "none",
	*	or conversion to a Long fails, the default value is used.
	*/
	public long getLong(String value, long defaultValue)
	{
		if(value != null && value.length() > 0)
		{
			try
			{
				return Long.parseLong(value);
			}
			catch(NumberFormatException e)
			{
			}
		}
		
		return defaultValue;
	}// getLong()
	
	/**
	*	Safely get an Integer value. If the value is empty or "none",
	*	or conversion to an Integer fails, the default value is used.
	*	<p>
	*	Also, if the value is out of the given range, the default value
	*	is used. Min and Max are inclusive.
	*/
	public int getInt(String value, int defaultValue, int min, int max)
	{
		if(min > max)
		{
			throw new IllegalArgumentException("min > max!");
		}
		
		if(value != null && value.length() > 0)
		{
			try
			{
				int i = Integer.parseInt(value);
				if(i >= min && i <= max)
				{
					return i;
				}
			}
			catch(NumberFormatException e)
			{
			}
		}
		
		return defaultValue;
	}// getInt()	
	
	
	
	
	/** Get the dip.world.Map object */
	public dip.world.Map getMap()
	{
		return map;
	}// getMap()
	
	/** Get the World object */
	public World getWorld()
	{
		return world;
	}// getWorld()
	
	/** 
	*	Get the HashMap which maps Orderable objects to unique IDs 
	*	during marshalling.
	*/
	public IdentityHashMap getMarshallingOrderMap()
	{
		return marshallingOrderMap;
	}// getMarshallingOrderMap()
	
	/** 
	*	Get the HashMap which maps Orderable objects to unique IDs 
	*	during <b>un</b>marshalling. This is not an IdentityHashMap..
	*/
	public HashMap getUnmarshallingOrderMap()
	{
		return unmarshallingOrderMap;
	}// getUnmarshallingOrderMap()
	
	/** Get the OrderFactory; only set when unmarshalling. */
	public OrderFactory getOrderFactory()
	{
		return orderFactory;
	}// getOrderFactory()
	
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
	
	/** Set the current TurnState object */
	public TurnState getCurrentTurnState()
	{
		return turnState;
	}// getCurrentTurnState()
	
	/** Get the current TurnState object */
	public void setCurrentTurnState(TurnState ts)
	{
		this.turnState = ts;
	}// setCurrentTurnState()
	
	/** Sets the World. This may be set only once. */
	public void setWorld(World w)
	{
		if(w == null) { throw new IllegalArgumentException(); }
		if(this.world != null) { throw new IllegalStateException(); }
		world = w;
	}// setWorld()
	
	/** Sets the Map. This may be set only once. */
	public void setMap(dip.world.Map worldMap)
	{
		if(worldMap == null) { throw new IllegalArgumentException(); }
		if(this.map != null) { throw new IllegalStateException(); }
		map = worldMap;
	}// setMap()
	
	/** Sets the OrderFactory. This may be set only once. */
	public void setOrderFactory(OrderFactory factory)
	{
		if(this.orderFactory != null) { throw new IllegalStateException("non-null OrderFactory"); }
		this.orderFactory = factory;
	}// getOrderFactory()
	
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
	private static void registerConverters(XStream xstream, OrderFactory of,
		String creator, String creatorVersion, String specification)
	{
		final ClassMapper cm = xstream.getClassMapper();
		
		// basic types
		xstream.registerConverter(new PhaseConverter(cm));
		xstream.registerConverter(new ProvinceConverter(cm));
		xstream.registerConverter(new LocationConverter(cm));
		xstream.registerConverter(new UnitTypeConverter(cm));
		xstream.registerConverter(new NameValuePair.NameValuePairConverter(cm));
		xstream.registerConverter(new AdjustmentInfoMapConverter(cm));
		
		// main turn types
		xstream.registerConverter(new Position.PositionConverter(cm));
		xstream.registerConverter(new Position.ProvinceDataConverter(cm));
		xstream.registerConverter(new TurnStateConverter(cm));
		
		xstream.registerConverter(new WorldConverter(cm, of,
			creator, creatorVersion, specification));
		
		xstream.registerConverter(new VariantInfoConverter(cm));
		xstream.registerConverter(new RuleOptionsConverter(cm));
		xstream.registerConverter(new VictoryConditionsConverter(cm));
		
		// for *all* Orderable subclasses
		xstream.registerConverter(new OrderableConverter(cm));
		
		// results
		xstream.registerConverter(new ResultConverter(cm));
		xstream.registerConverter(new TimeResult.TimeResultConverter(cm));
		
		// order results
		xstream.registerConverter(new OrderResultConverter(cm));
		xstream.registerConverter(new BouncedResultConverter(cm));
		xstream.registerConverter(new ConvoyPathResultConverter(cm));
		xstream.registerConverter(new DependentMoveFailedResultConverter(cm));
		xstream.registerConverter(new DislodgedResultConverter(cm));
		xstream.registerConverter(new SubstitutedResultConverter(cm));
		
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
