//
//  @(#)Template.java 	2004
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
package info.jdip.tool.conversion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
*	Given an InputStream, loads a template, and replaces named values
*	(denoted by "{variable}" with the variable as set in the hashmap.
*	<p>
*	If a required value cannot be found, an exception is thrown.
*/
class Template
{
	private String template = null;
	
	
	/** Load the template for parsing. Closes inputstream. */
	public Template(InputStream is)
	throws IOException
	{
		if(is == null)
		{
			throw new IllegalArgumentException();
		}

		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			StringBuilder sb = new StringBuilder(4096);

			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				sb.append('\n');
				line = br.readLine();
			}

			template = sb.toString();
		}
	}// Template()
	
	
	/** Parse the template */
	public String parse(HashMap values)
	throws IOException
	{
		if(values == null)
		{
			throw new IllegalArgumentException();
		}
		
		assert (template != null);
		
		StringBuffer sb = new StringBuffer(2*template.length());
		StringBuffer accum = new StringBuffer(1024);
		
		boolean inBrace = false;
		StringTokenizer st = new StringTokenizer(template, "{}", true);
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			if("{".equals(tok) && !inBrace)
			{
				inBrace = true;
			}
			else if("}".equals(tok) && inBrace)
			{
				inBrace = false;
				parseBetweenBraces(accum.toString(), sb, values);
				accum = new StringBuffer(1024);
			}
			else
			{
				if(inBrace)
				{
					accum.append(tok);
				}
				else
				{
					sb.append(tok);
				}
			}
		}
		
		return sb.toString();
	}// parse()
	
	/** Handle text between braces */
	private void parseBetweenBraces(String text, StringBuffer sb, HashMap values)
	throws IOException
	{
		Object obj = values.get(text);
		if(obj == null)
		{
			throw new IOException("Variable {"+text+"} in template was not found.");
		}
		
		sb.append(obj.toString());
	}// parseBetweenBraces()

	
	
}// class Template
