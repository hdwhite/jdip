//
//  @(#)Convert.java 	2/2003
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
package dip.misc;

import java.lang.*;
import java.io.*;
import java.util.*;


/**
	Converts JUDGE map files to jDip XML adjacency format.
	<p>
	Also (if only 1 argument specified) converts JUDGE map files
	to a list of abbreviated/long names, and a list of supply centers,
	for easier cut and paste.
	<p>
	This must be run from the command line<br>
	e.g., <code>java dip.misc.Convert [arguments]</code>
	<p>
	NOTE:<br>
	<ul>
	<li>-mx modifiers are not handled (MUST ADD to '-mv' and add borders manually)
	<li>ice coasts are not handled (must add borders manually)
	<li>convoyable coasts are not handled (must add isConvoyableCoast="true" manually)
	<li>islands are not detected; if a province is an island, you must manually insert
		a link to itself in the -mv adjacency.
	</ul>
	
*/
/*
	NOTE: this was VERY hastily made, and is no way representative of the
	jDip codebase 
	
	
	letter = The (generally) single letter that is used to define what type of region it is - where:
		l = land
		w = water. This letter can also be appended to land provinces, or home or neutral supply centers, to indicate a coastal space that can be used by fleets to convoy (for example, Baleares in Ancient Med), e.g. lw or Aw or xw
		<capital initial> = home supply center (assumed to be land) for a given power using the capital initial defined for that power in the Variant.cnt file
		x = supply center that starts the game neutral (assumed to be land)
		v = ice (used in some rule variants, for example Loeb9) 
	
*/
public class Convert
{
	private static Map map = new LinkedHashMap(211);	// map of short-names (lower case) to objects
	
	
	
	// for testing
	public static void main(String args[])
	throws Exception
	{
		if(args.length == 1)
		{
			String in = args[0];
			makeNames(in);
			System.exit(0);
		}
		else if(args.length != 2)
		{
			System.err.println("USAGE: convert file-in file-out");
			System.err.println("USAGE: convert file-in   [just prints name list & supply-centers to stdout");
			System.exit(1);
		}
		
		System.out.println("NOTE: does not support convoyable coasts, ice, etc. That must be added manually.");
		System.out.println("NOTE: islands must have an -mv coast link to itself. That must also be added manually.");
		System.out.println("If present, -mx adjacencies are ignored, and you will be warned");
		
		String in = args[0];
		String out = args[1];
		
		doConvert(in, out);
	}// main()
	
	
	private static void makeNames(String sin)
	throws Exception
	{
		File in = new File(sin);
		BufferedReader br = new BufferedReader(new FileReader(in));
		
		// read until '-' is reached, or null
		// this is the first part (creates ProvObjs)
		// read until '-' is reached, or null
		// this is the first part (creates ProvObjs)
		String line = br.readLine();
		while(line != null && !line.startsWith("-"))
		{
			makeProvObj(line);
			line = br.readLine();
		}
		
		// read until '-' is reached, or null
		// make adjacency info
		line = br.readLine();
		while(line != null && !line.startsWith("-"))
		{
			makeAdj(line);
			line = br.readLine();
		}
			
		System.out.println("** BEGIN list of province names (most suited for gridmaps)");
		
		Set set = map.entrySet();
		int total = 0;
		int count = 0;
		Iterator iter = set.iterator();
		while(iter.hasNext())
		{
			Map.Entry me = (Map.Entry) iter.next();
			ProvObj po = (ProvObj) me.getValue();
			
			System.out.print(po.getSN());
			
			System.out.print("     ");
			System.out.print(po.fullName);
			System.out.println("                        ");
			count++;
			total++;
			printBlank(count);
			
			// print out coast lines (if any)
			for(int i=0; i<po.adjList.size(); i++)
			{
				Adj adj = (Adj) po.adjList.get(i);
				
				if(!adj.coastType.equals("xc") && !adj.coastType.equals("mv"))
				{
					System.out.print(po.getSN());
					System.out.print("-");
					System.out.println(adj.coastType);
					count++;
					printBlank(count);
				}
			}
		}	
		
		System.out.println("** END Total: "+total);
		
		
		// sort thru SCs
		
		List ownedSCList = new ArrayList(100);		// owned sc
		List neutralSCList= new ArrayList(100);		// neutral sc
		List waterList = new ArrayList(100);		// sea-provinces
		
		set = map.entrySet();
		count = 0;
		iter = set.iterator();
		while(iter.hasNext())
		{
			Map.Entry me = (Map.Entry) iter.next();
			ProvObj po = (ProvObj) me.getValue();
			
			boolean isNeutralSC = false;
			boolean isOwnedSC = false;
			char ownedSCChar = ' ';
			
			// parse 'type' for an 'x'
			int idx = po.type.indexOf("x");
			isNeutralSC = (idx < 0) ? false : true;
			
			// parse for capital letter, if an 'x' wasn't found
			for(int i=0; i<po.type.length(); i++)
			{
				if(Character.isUpperCase(po.type.charAt(i)))
				{
					ownedSCChar = po.type.charAt(i);
					isOwnedSC = true;
					break;
				}
			}
			
			// put into lists
			if(isNeutralSC)
			{
				neutralSCList.add(po.getSN());
			}
			else if(isOwnedSC)
			{
				ownedSCList.add(new SCComp(po.getSN(), ownedSCChar));
			}
			
			
			// is this a water province or a land province?
			/*
				we are land if (any one of below):
					a) capital letter present
					b) 'x' present [maps currently do not support water supply center provinces]
					c) 'l' present
			*/
			if(idx < 0 && !isOwnedSC && po.type.indexOf("l") < 0)
			{
				waterList.add(po.getSN());
			}
			
		}
		
		// sort lists
		Collections.sort(neutralSCList);
		Collections.sort(ownedSCList);
		Collections.sort(waterList);
		
		
		System.out.println("** BEGIN supply center list: owned supply centers");
		total = 0;
		iter = ownedSCList.iterator();
		while(iter.hasNext())
		{
			System.out.println(iter.next());
			total++;
		}
		System.out.println("** END: Total: "+total);
		
		System.out.println("** BEGIN: supply center list: neutral supply centers");
		total = 0;
		iter = neutralSCList.iterator();
		while(iter.hasNext())
		{
			System.out.println(iter.next());
			total++;
		}
		System.out.println("** END: Total: "+total);
		System.out.println("** BEGIN: sea province list");
		total = 0;
		iter = waterList.iterator();
		while(iter.hasNext())
		{
			System.out.println(iter.next());
			total++;
		}
		System.out.println("** END: Total: "+total);
	}
	
	
	private static void printBlank(int count)
	{
		if(count % 10 == 0)
		{
			System.out.println("");
		}
	}
	
	
	private static void doConvert(String sin, String sout)
	throws Exception
	{
		File in = new File(sin);
		File out = new File(sout);
		
		
		BufferedReader br = new BufferedReader(new FileReader(in));
		
		// read until '-' is reached, or null
		// this is the first part (creates ProvObjs)
		String line = br.readLine();
		while(line != null && !line.startsWith("-"))
		{
			makeProvObj(line);
			line = br.readLine();
		}
		
		// read until '-' is reached, or null
		// make adjacency info
		line = br.readLine();
		while(line != null && !line.startsWith("-"))
		{
			makeAdj(line);
			line = br.readLine();
		}
		
		
		// write everything out
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		
		bw.write("<PROVINCES>");
		bw.newLine();
		
		Set set = map.entrySet();
		Iterator iter = set.iterator();
		while(iter.hasNext())
		{
			Map.Entry me = (Map.Entry) iter.next();
			ProvObj po = (ProvObj) me.getValue();
			
			bw.write("\t<PROVINCE fullname=\"");
			bw.write(po.fullName);
			bw.write("\" shortname=\"");
			bw.write(po.getSN());
			bw.write("\">");
			bw.newLine();
			
			// uniq names
			if(po.shortNames.size() > 1)
			{
				for(int i=1; i<po.shortNames.size(); i++)
				{
					bw.write("\t\t<UNIQUENAME name=\"");
					bw.write( (String) po.shortNames.get(i) );
					bw.write("\" />");
					bw.newLine();
				}
			}
			
			// adjacency
			for(int i=0; i<po.adjList.size(); i++)
			{
				Adj adj = (Adj) po.adjList.get(i);
				
				bw.write("\t\t<ADJACENCY type=\"");
				bw.write(adj.coastType);
				bw.write("\" refs=\"");
				
				for(int j=0; j<adj.names.size(); j++)
				{
					bw.write( (String) adj.names.get(j));
					if(j != adj.names.size()-1)
					{
						bw.write(' ');
					}
				}
				
				bw.write("\"/>");
				bw.newLine();
			}
			
			
			
			// close
			bw.write("\t</PROVINCE>");
			bw.newLine();
		}
		
		
		bw.write("</PROVINCES>");
		bw.newLine();
		
		bw.close();
		br.close();
		
		
	}// convert()
	
	private static void makeProvObj(String line)
	throws Exception
	{
		if(line.startsWith("#"))
		{
			return;
		}
		
		int comma = line.indexOf(",");
		if(comma < 0)
		{
			throw new IOException("ERROR: no comma in province definition line: "+line);
		}
		
		ProvObj po = new ProvObj();
		
		po.fullName = line.substring(0, comma);
		StringTokenizer st = new StringTokenizer(line.substring(comma+1), "\t\r\n ,");
		po.type = st.nextToken();		// may or may not be used later
		
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			po.shortNames.add(tok);
		}
		
		if(po.shortNames.size() == 0)
		{
			throw new IOException("ERROR: no short names found in line: "+line);
		}
		
		// map it
		map.put(po.getSN(), po);
	}// makeProvObj()
	
	
	private static void makeAdj(String line)
	throws Exception
	{
		if(line.startsWith("#"))
		{
			return;
		}
		
		int colon = line.indexOf(":");
		int firstHyphen = line.indexOf("-");
		if(colon < 0 || firstHyphen < 0 || colon < firstHyphen)
		{
			throw new IOException("ERROR: bad adjacency line: "+line);
		}
		
		String firstPart = line.substring(0, colon);
		String lastPart = line.substring(colon+1);
		
		String shortName = firstPart.substring(0, firstHyphen).toLowerCase();
		String coastType = firstPart.substring(firstHyphen+1, firstHyphen+3).toLowerCase();;
		
		if(coastType.equals("mx"))
		{
			System.out.println("WARNING: line: "+line+" contains an -mx coast specifier. This is ignored.");
			return;
		}
		
		Adj adj = new Adj();
		adj.shortName = shortName;
		adj.coastType = coastType;
		
		StringTokenizer st = new StringTokenizer(lastPart, "\t\r\n ,");
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			adj.names.add(tok.toLowerCase());
		}
		
		if(adj.names.size() == 0)
		{
			throw new IOException("ERROR: adjacent provinces in line: "+line);
		}
		
		// map. note that if we cannot find the shortName, we will iterate
		// through the map (slow!) until we find a matching name. If we don't,
		// then we'll throw an exception.
		ProvObj po = (ProvObj) map.get(shortName);
		if(po == null)
		{
			Set set = map.entrySet();
			Iterator iter = set.iterator();
			while(iter.hasNext())
			{
				Map.Entry me = (Map.Entry) iter.next();
				po = (ProvObj) me.getValue();
				
				for(int i=0; i<po.shortNames.size(); i++)
				{
					String sn = (String) po.shortNames.get(i);
					if(shortName.equals(sn))
					{
						po.adjList.add(adj);
						return;
					}
				}
			}
			
			throw new IOException("ERROR: mapping failed: cannot find province: "+shortName);
		}
		else
		{
			po.adjList.add(adj);
			return;
		}
	}// makeAdj()
	
	
	
	private static class ProvObj
	{
		String 		fullName;
		ArrayList 	shortNames;
		ArrayList	adjList;
		String 		type;		// e.g.: "l", "lw", "Aw", etc.
		
		public ProvObj()
		{
			shortNames = new ArrayList(10);
			adjList = new ArrayList(5);
		}
		
		public String getSN()
		{
			return (String) shortNames.get(0);
		}
		
	}// inner class ProvObj
	
	
	private static class Adj
	{
		String shortName;		// 'aeg' part of, say, 'aeg-xc'
		String coastType;		// -xc, -mv, etc.
		ArrayList names;		// province names, With coast modifier (-xx) if appropriate
		
		public Adj()
		{
			names = new ArrayList(10);
		}
	}// inner class Adj
	
	
	// for sorting supply centers
	// sort by 1) owner then 2) province name
	private static class SCComp implements Comparable
	{
		private String name;
		private Character owner;
		
		public SCComp(String name, char owner)
		{
			this.name = name;
			this.owner = new Character(owner);
		}
		
		
		public int compareTo(Object obj)
		{
			SCComp cmp = (SCComp) obj;
			int val = this.owner.compareTo(cmp.owner);
			if(val == 0)
			{
				return this.name.compareTo(cmp.name);
			}
			
			return val;
		}// compareTo()
		
		
		public String toString()
		{
			return name + "  " + owner;
		}
		
	}
	
	
}// class Convert
