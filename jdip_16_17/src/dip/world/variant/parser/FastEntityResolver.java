//
//  @(#)FastEntityResolver.java			12/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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

import dip.misc.Log;
import dip.misc.Utils;
import dip.misc.LRUCache;

import java.io.StringReader;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
/**
*	Resolves Entity definitions to an empty InputSource, unless validation
*	is enabled, in which case we actually do resolve the entities.
*	<p>
*	Not looking for entity resolutions improves startup time.
*/
public class FastEntityResolver implements EntityResolver
{
	private final boolean isValidating;
	
	private static final String[] SUPPORTED_DTDS = {
		"-//JDIP//VARIANTS DTD//1.0//DEFAULT",
		"-//JDIP//PROVINCES DTD//1.0//DEFAULT"
	};
	
	private static final String[] SUPPORTED_DTD_LOCATIONS = 
	{
		"resource/common/dtd/variants.dtd",
		"resource/common/dtd/provinces.dtd"
	};
	
	
	/** Construct a FastEntityResolver */
	public FastEntityResolver(boolean isValidating)
	{
		this.isValidating = isValidating;
	}// FastEntityResolver()
	
	
	/** 
	*	Attach a FastEntityResolver to a DocumentBuilder, setting
	*	validation as appropriate.
	*/
	public static void attach(DocumentBuilder db)
	{
		db.setEntityResolver(new FastEntityResolver(db.isValidating()));
	}// attach()
	
	
	/** Resolve the Entity */
	public InputSource resolveEntity(String publicID, String systemID)
	throws SAXException, IOException	
	{
		InputSource is = loadDTD(publicID, systemID);
		
		if(is == null && !isValidating)
		{
			// log the request
			Log.println("XML:Entity resolution ignored: ", publicID, "; ", systemID);
		
			// return an empty InputSource
			is = new InputSource(new StringReader(""));
			is.setPublicId(publicID);
			is.setSystemId(systemID);
			return is;
		}
		
		// by default, return null, or the loaded DTD
		return is;
	}// resolveEntity()
	
	
	// TODO: implement caching. 
	private static InputSource loadDTD(String publicID, String systemID)
	throws IOException
	{
		InputSource is = null;
		
		for(int i=0; i<SUPPORTED_DTDS.length; i++)
		{
			if(publicID.equals(SUPPORTED_DTDS[i]))
			{
				is = new InputSource(
					Utils.getInputStreamReader(SUPPORTED_DTD_LOCATIONS[i])
				);
				is.setPublicId(publicID);
				is.setSystemId(systemID);
				return is;
			}
		}
		
		return null;
	}// loadDTD()
	
	
}// class FastEntityResolver
