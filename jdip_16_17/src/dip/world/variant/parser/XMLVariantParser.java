//
//  @(#)XMLVariantParser.java		7/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
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
package dip.world.variant.parser;

import dip.world.variant.VariantManager;

import dip.world.variant.data.Variant;
import dip.world.variant.data.SupplyCenter;
import dip.world.variant.data.InitialState;
import dip.world.variant.data.MapGraphic;
import dip.world.variant.data.ProvinceData;
import dip.world.variant.data.BorderData;

import dip.world.Phase;
import dip.world.Power;
import dip.world.Unit;
import dip.world.Coast;

import dip.misc.LRUCache;
import dip.misc.Utils;
import dip.misc.Log;
import dip.misc.XMLUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.*;


/**
*	Parses an XML Variant description.
*
*/
public class XMLVariantParser implements VariantParser
{
	// XML Element constants
	public static final String EL_VARIANTS = "VARIANTS";
	public static final String EL_VARIANT = "VARIANT";
	public static final String EL_DESCRIPTION = "DESCRIPTION";
	public static final String EL_MAP = "MAP";
	public static final String EL_STARTINGTIME = "STARTINGTIME";
	public static final String EL_INITIALSTATE = "INITIALSTATE";
	public static final String EL_SUPPLYCENTER = "SUPPLYCENTER";
	public static final String EL_POWER = "POWER";
	public static final String EL_MAP_DEFINITION = "MAP_DEFINITION";
	public static final String EL_MAP_GRAPHIC = "MAP_GRAPHIC";
	public static final String EL_VICTORYCONDITIONS = "VICTORYCONDITIONS";
	public static final String EL_GAME_LENGTH = "GAME_LENGTH";
	public static final String EL_YEARS_WITHOUT_SC_CAPTURE = "YEARS_WITHOUT_SC_CAPTURE";
	public static final String EL_WINNING_SUPPLY_CENTERS = "WINNING_SUPPLY_CENTERS";
	public static final String EL_RULEOPTIONS = "RULEOPTIONS";
	public static final String EL_RULEOPTION = "RULEOPTION";
	
	
	// XML Attribute constants
	public static final String ATT_ALIASES = "aliases";
	public static final String ATT_VERSION = "version";
	public static final String ATT_URI = "URI";
	public static final String ATT_DEFAULT = "default";
	public static final String ATT_TITLE = "title";
	public static final String ATT_DESCRIPTION = "description";
	public static final String ATT_THUMBURI = "thumbURI";
	public static final String ATT_ADJACENCYURI = "adjacencyURI";
	public static final String ATT_NAME = "name";
	public static final String ATT_ACTIVE = "active";
	public static final String ATT_ADJECTIVE = "adjective";
	public static final String ATT_ALTNAMES = "altnames";
	public static final String ATT_TURN = "turn";
	public static final String ATT_VALUE = "value";
	public static final String ATT_PROVINCE = "province";
	public static final String ATT_HOMEPOWER = "homepower";
	public static final String ATT_OWNER = "owner";
	public static final String ATT_POWER = "power";
	public static final String ATT_UNIT = "unit";
	public static final String ATT_UNITCOAST = "unitcoast";
	public static final String ATT_ALLOW_BC_YEARS = "allowBCYears";
	public static final String ATT_PREFERRED_UNIT_STYLE = "preferredUnitStyle";
	public static final String ATT_ID	= "id";
	public static final String ATT_REF	= "ref";
	
	
	// il8n error message constants
	private static final String ERR_NO_ELEMENT = "XMLVariantParser.noelement";
	
	// instance variables
	private Document doc = null;
	private DocumentBuilder docBuilder = null;
	private List variantList = null;
	private XMLProvinceParser provinceParser = null;
	
	
	/** Create an XMLVariantParser */
	public XMLVariantParser(final DocumentBuilderFactory dbf)
	throws ParserConfigurationException
	{
		docBuilder = dbf.newDocumentBuilder();
		docBuilder.setErrorHandler(new XMLErrorHandler());
		FastEntityResolver.attach(docBuilder);
		provinceParser = new XMLProvinceParser(dbf);
		
		variantList = new LinkedList();
		AdjCache.init(provinceParser);
	}// XMLVariantParser()
	
	
	
	/** Parse the given input stream; parsed data available via <code>getVariants()</code>
		<p>
		Note that when this method is called, any previous Variants (if any exist) are
		cleared.
	*/
	public void parse(InputStream is, URL variantPackageURL)
	throws IOException, SAXException
	{
		Log.println("XMLVariantParser().parse(): ", variantPackageURL);
		
		long time = System.currentTimeMillis();
		
		// cleanup cache (very important to remove references!)
		AdjCache.clear();
		variantList.clear();
		
		if(variantPackageURL == null)
		{
			throw new IllegalArgumentException();
		}
		
		AdjCache.setVariantPackageURL(variantPackageURL);
		doc = docBuilder.parse(is);
		
		procVariants();
		Log.printTimed(time, "   time: ");
	}// parse()
	
	
	
	
	/** Cleanup, clearing any references/resources */
	public void close()
	{
		AdjCache.clear();
		variantList.clear();
	}// close()
	
	
	
	/** Returns an array of Variant objects.
		<p>
		Will never return null. Note that parse() must be called before
		this will return any information.
	*/
	public Variant[] getVariants()
	{
		return (Variant[]) variantList.toArray(new Variant[variantList.size()]); 			
	}// getVariants()
	
	
	
				
	/** processes each VARIANT element node */
	private void procVariantElement(final Element elVariant, final HashMap mapDefTbl)
	throws IOException, SAXException
	{
		Variant variant = new Variant();
					
		// VARIANT attributes
		variant.setName( elVariant.getAttribute(ATT_NAME) );
		variant.setDefault( Boolean.valueOf(elVariant.getAttribute(ATT_DEFAULT)).booleanValue() );
		variant.setVersion( parseFloat(elVariant.getAttribute(ATT_VERSION)) ); 
		variant.setAliases( Utils.parseCSV(elVariant.getAttribute(ATT_ALIASES)) );
		
		// description
		Element element = XMLUtils.findFirstChild(elVariant, EL_DESCRIPTION);
		checkElement(element, EL_DESCRIPTION, variant.getName());
		Node text = element.getFirstChild();
		variant.setDescription( text.getNodeValue() );
		
		// starting time
		element = XMLUtils.findFirstChild(elVariant, EL_STARTINGTIME);
		checkElement(element, EL_STARTINGTIME, variant.getName());
		variant.setStartingPhase( Phase.parse(element.getAttribute(ATT_TURN)) );
		variant.setBCYearsAllowed( Boolean.valueOf(element.getAttribute(ATT_ALLOW_BC_YEARS)).booleanValue() );
			
		// if start is BC, and BC years are not allowed, then BC years ARE allowed.
		if(variant.getStartingPhase().getYear() < 0)
		{
			Log.println("WARNING: BC years are used, though not explicitly enabled in variant: ", variant.getName());
			variant.setBCYearsAllowed(true);
		}
		
		// victory conditions (single, with single subitems)
		element = XMLUtils.findFirstChild(elVariant, EL_VICTORYCONDITIONS);
		checkElement(element, EL_VICTORYCONDITIONS, variant.getName());
		Element vcSubElement = XMLUtils.findFirstChild(element, EL_WINNING_SUPPLY_CENTERS);
		if(vcSubElement != null)
		{
			variant.setNumSCForVictory( parseInt(vcSubElement.getAttribute(ATT_VALUE)) );
		}
		
		vcSubElement = XMLUtils.findFirstChild(element, EL_YEARS_WITHOUT_SC_CAPTURE);
		if(vcSubElement != null)
		{
			variant.setMaxYearsNoSCChange( parseInt(vcSubElement.getAttribute(ATT_VALUE)) );
		}
			
		vcSubElement = XMLUtils.findFirstChild(element, EL_GAME_LENGTH);
		if(vcSubElement != null)
		{
			variant.setMaxGameTimeYears( parseInt(vcSubElement.getAttribute(ATT_VALUE)) );
		}
		
		// powers (multiple)
		int idx = 0;
		element = XMLUtils.findFirstChild(elVariant, EL_POWER);
		List tmpList = new ArrayList(16);
		while(element != null)
		{
			final String name = element.getAttribute(ATT_NAME);
			final boolean isActive =  Boolean.valueOf( element.getAttribute(ATT_ACTIVE) ).booleanValue();
			final String adjective = element.getAttribute(ATT_ADJECTIVE);
			final String[] altNames = Utils.parseCSVXE( element.getAttribute(ATT_ALTNAMES) );
			final String[] names = new String[altNames.length + 1];
			names[0] = name;
			System.arraycopy(altNames, 0, names, 1, altNames.length);
			
			Power power = new Power(names, adjective, idx, isActive);
			tmpList.add(power);
			
			element = XMLUtils.findNextSiblingElement(element, EL_POWER);
			idx++;
		}
		variant.setPowers(tmpList);
		
		// supply centers
		element = XMLUtils.findFirstChild(elVariant, EL_SUPPLYCENTER);
		tmpList = new ArrayList(64);
		while(element != null)
		{
			SupplyCenter supplyCenter = new SupplyCenter();
			supplyCenter.setProvinceName( element.getAttribute(ATT_PROVINCE) );
			supplyCenter.setHomePowerName( element.getAttribute(ATT_HOMEPOWER) );
			supplyCenter.setOwnerName( element.getAttribute(ATT_OWNER) );
			tmpList.add(supplyCenter);
			element = XMLUtils.findNextSiblingElement(element, EL_SUPPLYCENTER);
		}
		variant.setSupplyCenters(tmpList);
		
		// initial state (multiple)
		element = XMLUtils.findFirstChild(elVariant, EL_INITIALSTATE);
		tmpList = new ArrayList(64);
		while(element != null)
		{
			InitialState initialState = new InitialState();
			initialState.setProvinceName( element.getAttribute(ATT_PROVINCE) );
			initialState.setPowerName( element.getAttribute(ATT_POWER) );
			initialState.setUnitType( Unit.Type.parse(element.getAttribute(ATT_UNIT)) );
			initialState.setCoast( Coast.parse(element.getAttribute(ATT_UNITCOAST)) );
			tmpList.add(initialState);
			element = XMLUtils.findNextSiblingElement(element, EL_INITIALSTATE);
		}
		variant.setInitialStates(tmpList);
		
		// MAP element
		final Element mapElement = XMLUtils.findFirstChild(elVariant, EL_MAP);
		
		// MAP adjacency URI; process it using ProvinceData parser
		try
		{
			//long time = System.currentTimeMillis();
			URI adjacencyURI = new URI(mapElement.getAttribute(ATT_ADJACENCYURI));
			variant.setProvinceData( AdjCache.getProvinceData(adjacencyURI) );
			variant.setBorderData( AdjCache.getBorderData(adjacencyURI) );
			//Log.printTimed(time, "    Includes province acquisition time: ");
		}
		catch(URISyntaxException e)
		{
			throw new IOException(e.getMessage());
		}
		
		// MAP::MAP_GRAPHIC element (multiple)
		element = XMLUtils.findFirstChild(mapElement, EL_MAP_GRAPHIC);
		tmpList = new ArrayList(8);
		while(element != null)
		{
			final String refID = element.getAttribute(ATT_REF);
			final boolean isDefault = Boolean.valueOf(element.getAttribute(ATT_DEFAULT)).booleanValue();
			final String preferredUnitStyle = element.getAttribute(ATT_PREFERRED_UNIT_STYLE);
			
			// lookup; if we didn't find it, throw an exception
			MapDef md = (MapDef) mapDefTbl.get(refID);
			if(md == null)
			{
				throw new IOException("MAP_GRAPHIC refers to unknown ID: \""+refID+"\"");						
			}
			
			// create the MapGraphic object
			MapGraphic mapGraphic = new MapGraphic(
				md.getMapURI(),
				isDefault,
				md.getTitle(),
				md.getDescription(),
				md.getThumbURI(),
				("".equals(preferredUnitStyle)) ? md.getPrefUnitStyle() : preferredUnitStyle );
			
			tmpList.add(mapGraphic);
			
			element = XMLUtils.findNextSiblingElement(element, EL_MAP_GRAPHIC);
		}
		variant.setMapGraphics(tmpList);
		
		
		// rule options (if any have been set)
		// this element is optional.
		element = XMLUtils.findFirstChild(elVariant, EL_RULEOPTIONS);
		tmpList = new ArrayList(8);
		if(element != null)
		{
			
			// all subElements are always RULEOPTION elements
			Element child =  XMLUtils.getFirstChildElement(element);
			while(child != null)
			{
				Variant.NameValuePair nvp = new Variant.NameValuePair(
												child.getAttribute(ATT_NAME),
												child.getAttribute(ATT_VALUE)
											);
				tmpList.add(nvp);
				child =  XMLUtils.getNextSiblingElement(child);
			}
		}
		variant.setRuleOptionNVPs(tmpList);
		
		// add variant to list of variants
		variantList.add(variant);
	}// procVariantElement
	
	
	/** Process the Variant list description file */
	private void procVariants()
	throws IOException, SAXException
	{
		//long time = System.currentTimeMillis();
		
		// setup map definition ID hashmap
		HashMap mapDefTable = new HashMap(7);	// maps String ID -> MapDef
		
		
		// find the root element (VARIANTS), and all VARIANT elements underneath.
		Element root = doc.getDocumentElement();
		
		// process children (these are only MAP_DEFINITION and VARIANT elements)
		Element rootChild =  XMLUtils.getFirstChildElement(root);
		while(rootChild != null)
		{
			final String name = rootChild.getNodeName();
			
			if(EL_VARIANT.equals(name))
			{
				procVariantElement(rootChild, mapDefTable);
			}
			else if(EL_MAP_DEFINITION.equals(name))
			{
				// remember, MAP_DEFINITION elements should come first
				
				// get description, if present
				String description = null;
				Element el =  XMLUtils.getFirstChildElement(rootChild);
				if(EL_DESCRIPTION.equals(el.getNodeName()))
				{
					Node text = el.getFirstChild();
					description = text.getNodeValue();
				}
				
				// create MapDef
				MapDef md = new MapDef( 
					rootChild.getAttribute(ATT_ID),
					rootChild.getAttribute(ATT_TITLE),
					rootChild.getAttribute(ATT_URI),
					rootChild.getAttribute(ATT_THUMBURI),
					rootChild.getAttribute(ATT_PREFERRED_UNIT_STYLE),
					description );
				
				// if no title, error!
				if("".equals(md.getTitle()))
				{
					throw new IOException("map id="+md.getID()+" missing a title (name)");
				}
				
				// map it.
				mapDefTable.put(md.getID(), md);
			}
			
			rootChild =  XMLUtils.getNextSiblingElement(rootChild); 
		}
		
		//Log.printTimed(time, "   procVariants() time: ");
	}// procVariants()
	
	
	/** Checks that an element is present */
	private void checkElement(Element element, String name, String variant)
	throws SAXException
	{
		if(element == null)
		{
			throw new SAXException(Utils.getLocalString(ERR_NO_ELEMENT, name, variant));
		}
	}// checkElement()
	
	
	
	/** Integer parser; throws an exception if number cannot be parsed. */
	private int parseInt(String value)
	throws IOException
	{
		String message = "";
		
		try
		{
			return Integer.parseInt(value);
		}
		catch(NumberFormatException e)
		{
			message = e.toString();
		}
		
		throw new IOException(message);
	}// parseInt()
	
	
	/** Float parser; throws an exception if number cannot be parsed. Value must be >= 0.0 */
	private float parseFloat(String value)
	throws IOException
	{
		String message = "";
		
		try
		{
			final float floatValue = Float.parseFloat(value);
			if(floatValue < 0.0f)
			{
				throw new NumberFormatException("Value must be >= 0");
			}
			
			return floatValue;
		}
		catch(NumberFormatException e)
		{
			message = e.toString();
		}
		
		throw new IOException(message);
	}// parseInt()
	
	
	/** 
	*	Inner class which caches XML adjacency data (ProvinceData and BorderData), 
	*	which may be shared between different variants (if the variants use the
	*	same adjacency data).
	*	<p>
	*	NOTE: this depends on the XMLVariantParser variable "adjCache", since inner classes
	*	cannot have statics (unless the inner class is static, which just creates more problems;
	*	this is a simpler solution)
	*
	*/
	private static class AdjCache
	{
		private static URL vpURL = null;
		private static XMLProvinceParser pp = null;
		private static LRUCache adjCache = null;	// URI -> AdjCache objects
		
		// instance variables
		private ProvinceData[] 	provinceData;
		private BorderData[]	borderData;
		
		
		public AdjCache()
		{
		}// AdjCache()
		
		/** initialization */
		public static void init(XMLProvinceParser provinceParser)
		{
			pp = provinceParser;
			adjCache = new LRUCache(6);
		}// AdjCache()
		
		/** Sets the variant package URL */
		public static void setVariantPackageURL(URL variantPackageURL)
		{
			vpURL = variantPackageURL;
		}// setVariantPackageURL()
		
		
		/** Clears the cache. */
		public static void clear()
		{
			adjCache.clear();
		}// clear()
		
		
		/** Gets the ProvinceData for a given adjacency URI */
		public static ProvinceData[] getProvinceData(URI adjacencyURI)
		throws IOException, SAXException
		{
			AdjCache ac = get(adjacencyURI);
			return ac.provinceData;
		}// getProvinceData()
		
		
		/** Gets the BorderData for a given adjacency URI */
		public static BorderData[] getBorderData(URI adjacencyURI)
		throws IOException, SAXException
		{
			AdjCache ac = get(adjacencyURI);
			return ac.borderData;
		}// getBorderData()
		
		
		/** Gets the AdjCache object from the cache, or parses from the URI, as appropriate */
		private static AdjCache get(URI adjacencyURI)
		throws IOException, SAXException
		{
			// see if we already have the URI data cached.
			if(adjCache.get(adjacencyURI) != null)
			{
				return (AdjCache) adjCache.get(adjacencyURI);
			}
			
			// it's not cached. resolve URI.
			URL url = VariantManager.getResource(vpURL, adjacencyURI);
			if(url == null)
			{
				throw new IOException("Could not convert URI: "+adjacencyURI+" from variant package: "+vpURL);
			}
			
			// parse resolved URI
			//Log.println("  AdjCache: adding to cache...: ", adjacencyURI);
			InputStream is = null;
			try
			{
				is = new BufferedInputStream(url.openStream());
				pp.parse(is);
			}
			finally
			{
				if(is != null)
				{
					try { is.close(); } catch (IOException e) {}
				}
			}
			
			// cache and return parsed data.
			AdjCache ac = new AdjCache();
			ac.provinceData	= pp.getProvinceData();
			ac.borderData 	= pp.getBorderData();
			adjCache.put(adjacencyURI, ac);
			return ac;
		}// get()
		
	}// inner class AdjCache
	
	
	/** 
	*	Class that holds MAP_DEFINITION data, which is 
	*	inserted into a hashtable for later recall. 
	*/
	private class MapDef
	{
		private final String id;
		private final String title;
		private final String mapURI;
		private final String thumbURI;
		private final String preferredUnitStyle;
		private final String description;
		
		public MapDef(String id, String title, String mapURI, String thumbURI, 
			String preferredUnitStyle, String description)
		{
			this.id = id;
			this.title = title;
			this.mapURI = mapURI;
			this.thumbURI = thumbURI;
			this.preferredUnitStyle = preferredUnitStyle;
			this.description = description;
		}// MapDef()
		
		public String getID() 	{ return id; }
		public String getTitle() 	{ return title; }
		public String getMapURI() 	{ return mapURI; }
		public String getThumbURI() 	{ return thumbURI; }
		public String getPrefUnitStyle() 	{ return preferredUnitStyle; }
		public String getDescription() 	{ return description; }
	}// inner class MapDef
	
}// class XMLVariantParser



