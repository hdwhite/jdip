//
//  @(#)MapConvert.java 	2004
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
package jdip.conversion;

import java.lang.*;
import java.io.*;
import java.util.*;


/**
	Converts JUDGE map files to jDip XML adjacency format.
	<p>
	This must be run from the command line<br>
	e.g., <code>java dip.misc.Convert [input-file] [output-file-prefix]</code>
	<p>
	This fully supports:
	<ul>
		<li>Convoyable coasts</li>
		<li>Ice provinces</li>
		<li>Islands (the older version of MapConvert did not)</li>
		<li>The 'mx' modifier (e.g., in Loeb9); movement with one less support</li>
		<li>Provinces defined but without connectivity (e.g., Switzerland)</li>
	</ul>
*/
public class MapConvert
{
	
	private static final String VARIANT_TEMPLATE 	= "jdip/conversion/VariantXMLTemplate.txt";
	private static final String ADJACENCY_TEMPLATE 	= "jdip/conversion/AdjacencyXMLTemplate.txt";
	private static final String UNKNOWN_DATA = "_UNKNOWN_";
	
	private final Map provMap;		// map of short-names (lower case) to objects
	private final List provList; 	// list of province objects
	
	
	/** Convert a Judge map to jDip Adjacency format. */
	public static void main(String args[])
	throws Exception
	{
		if(args.length != 2)
		{
			System.err.println("\njudgeconvert: converts judge-style variant map files to jDip format.");
			System.err.println("\nUSAGE: judgeconvert <input_file> <output_file>");
			System.err.println("\nThis will create: ");
			System.err.println("   output_file_adjacency.xml (a jDip variant adjacency format)");
			System.err.println("   variants.xml (the variant definition file)");
			System.err.println("\nNote that the variants.xml file will not be complete, since");
			System.err.println("a judge-style variant map does not contain enough information");
			System.err.println("to completely specify a jDip variant.");
			System.exit(1);
		}
		
		String in = args[0];
		String out = args[1];
		
		try
		{
			File inFile = new File(in);
			File outFile = new File(out+"_adjacency.xml");
			MapConvert mc = new MapConvert(inFile, outFile);
			
			// write variants.xml data
			mc.makeVariantXMLFile(mc.getProvObjList(), outFile);
			System.out.println("Conversion successful.");
		}
		catch(IOException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			System.exit(1);
		}
	}// main()
	
	
	/**
	*	Converts a Judge-style Map (also used by RealPolitik) to 
	*	a jDip-style XML Adjacency data format. Calling this method
	*	does NOT produce a variants.xml file.
	*/
	public MapConvert(File input, File adjacencyOutput)
	throws IOException
	{
		// load templates
		final ClassLoader cl = this.getClass().getClassLoader();
		final Template adjacencyTemplate = new Template(
			cl.getResourceAsStream(ADJACENCY_TEMPLATE) );
			
		provMap = new HashMap(211);		// map of short-names (lower case) to objects
		provList = new LinkedList();		// list of province objects
		
		FileLine[] lines = null;
		
		
		// read all lines.
		LineNumberReader lnr = null;
		try
		{
			ArrayList tmpLines = new ArrayList(500);
			lnr = new LineNumberReader(new BufferedReader(new FileReader(input)));
			String line = lnr.readLine();
			int lineNum = lnr.getLineNumber();
			
			while(line != null)
			{
				FileLine fl = FileLine.makeFileLine(line, lineNum);
				if(fl != null)
				{
					tmpLines.add(fl);
				}
				
				line = lnr.readLine();
				lineNum = lnr.getLineNumber();
			}
			
			lines = (FileLine[]) tmpLines.toArray(new FileLine[tmpLines.size()]);
			System.out.println("Read "+lineNum+" lines from "+input);
		}
		finally
		{
			if(lnr != null)
			{
				lnr.close();
			}
		}
		
		
		
		int sectionIdx = 0;
		
		// create Province objects from first section (beginning of file
		// to the first section marker)
		//
		
		for(int i=sectionIdx; i<lines.length; i++)
		{
			FileLine fl = lines[i];
			if(fl.isSectionMarker())
			{
				sectionIdx = i;
				break;
			}
			
			// add to both map AND list!
			ProvObj po = ProvObj.makeProvObj(fl);
			provMap.put(po.getSN(), po);
			provList.add(po);
		}
		
		System.out.println("Province parsing complete.");
		
		
		// Adjacency data
		//
		sectionIdx++;
		for(int i=sectionIdx; i<lines.length; i++)
		{
			FileLine fl = lines[i];
			if(fl.isSectionMarker())
			{
				sectionIdx = i;
				break;
			}
			
			Adjacency adj = Adjacency.makeAdj(fl);
			
			// note that if we cannot find the shortName, we will iterate
			// through the map (slow!) until we find a matching name. If we don't,
			// then we'll throw an exception.
			final String adjName = adj.getLoc().getShortName();
			ProvObj po = (ProvObj) provMap.get(adjName);
			if(po == null)
			{
				Iterator iter = provList.iterator();
				while(iter.hasNext())
				{
					po = (ProvObj) iter.next();
					if(po.isSN(adjName))
					{
						break;	// we found it
					}
				}
			}
			
			if(po == null)
			{
				fl.makeError("ERROR: adjacency province "+adjName+" not previously defined!");
			}
			else
			{
				po.addAdjacency(adj, fl);
			}
		}
		
		System.out.println("Adjacency parsing complete.");		
		
		// write everything out
		// 
		BufferedWriter bw = null;
		try
		{
			bw = new BufferedWriter(new FileWriter(adjacencyOutput));
			
			StringBuffer sb = new StringBuffer(4096);
			makeXMLData(sb, provList);
			
			// write adjacency data
			HashMap templateData = new HashMap();
			templateData.put("xmldata", sb.toString());
			bw.write( adjacencyTemplate.parse(templateData) );
			System.out.println("File created: "+adjacencyOutput);
			
		}
		finally
		{
			if(bw != null)
			{
				bw.close();
			}
		}
	}// MapConvert()
	
	
	/** Get a List of ProvObj objects. This is modifiable. */
	public List getProvObjList()
	{
		return provList;
	}// getProvObjList()
	
	/**
	*	Get the java.util.Map of ProvObj objects. These are mapped
	*	by the first shortname of the ProvObj. This is modifiable.
	*/
	public Map getProvObjMap()
	{
		return provMap;
	}// getProvObjMap()
	
	
	/** Write province data to stringbuffer */
	private void makeXMLData(StringBuffer sb, List provList)
	{
		// STEP 1: check for ice borders; insert appropriate border
		// STEP 2: check for /mx coasts; insert appropriate borders
		writeBorders(sb, provList);
		
		// STEP 3: check for islands/isolated provinces (e.g., switzerland)
		// 
		// if NO adjacency data is present, connect province to itself.
		// example: switzerland
		//
		// if isLand or isConvoyableCoast and no -mv adjacency is present, 
		// create a link to itself
		// example: variant: loeb9, Ireland
		//
		Iterator iter = provList.iterator();
		while(iter.hasNext())
		{
			((ProvObj) iter.next()).fixAdjacency();
		}
		
		// STEP 4: actually write out province data
		// ** do not sort **; if we do sort, do it 
		// in a new List.
		//
		iter = provList.iterator();
		while(iter.hasNext())
		{
			sb.append( ((ProvObj) iter.next()).toXML() );
		}
	}// makeXMLData()
	
	
	private void writeBorders(StringBuffer sb, List provinces)
	{
		boolean hasIce = false;
		boolean hasMX = false;
		
		Iterator iter = provinces.iterator();
		while(iter.hasNext())
		{
			ProvObj po = (ProvObj) iter.next();
			if(po.isIce())
			{
				hasIce = true;
			}
			
			if(po.hasMX())
			{
				hasMX = true;
			}
		}
		
		
		if(hasIce || hasMX)
		{
			sb.append("\n\t<BORDER_DEFINITIONS>\n");
			
			if(hasIce)
			{
				sb.append("\t\t<BORDER id=\"ice\" description=\"Ice prevents all orders in the Fall.\" season=\"fall\" />\n");
			}
			
			if(hasMX)
			{
				// HashMap is used to keep track of borders, so we don't
				// create dupes.
				//
				HashMap borderMap = new HashMap();
						
				iter = provinces.iterator();
				while(iter.hasNext())
				{
					ProvObj po = (ProvObj) iter.next();
					if(po.hasMX())
					{
						Iterator mxIter = po.getMXProvs().iterator();
						while(mxIter.hasNext())
						{
							final String provinceName = (String) mxIter.next();
							final String borderName = ProvObj.BORDER_NAME_PREFIX + provinceName.toUpperCase();
							
							if(!borderMap.containsKey(borderName))
							{
								borderMap.put(borderName, Boolean.TRUE);
								sb.append("\t\t<BORDER id=\"");
								sb.append(borderName);
								sb.append("\" from=\"");
								sb.append(provinceName);
								sb.append("\" description=\"\" baseMoveModifier=\"-1\" />\n"
								);
							}
						}
					}
				}
			}
			
			sb.append("\t</BORDER_DEFINITIONS>\n\n");
		}
	}// writeBorders()
	
	
	
	private void makeVariantXMLFile(List provList, File adjacencyFile)
	throws IOException
	{
		final ClassLoader cl = this.getClass().getClassLoader();
		final Template variantTemplate = new Template(
			cl.getResourceAsStream(VARIANT_TEMPLATE) );
		
		File out = new File(adjacencyFile.getPath(), "variants.xml");
		if(adjacencyFile.getPath().equals(adjacencyFile.getName()))
		{
			out = new File("variants.xml");
		}
			
		
		BufferedWriter bw = null;
		try
		{
			bw = new BufferedWriter(new FileWriter(out));
			
			// write adjacency data
			HashMap templateData = new HashMap();
			makeVariantXMLTemplateData(templateData, provList, adjacencyFile);
			bw.write( variantTemplate.parse(templateData) );
			System.out.println("File created: "+out);
		}
		finally
		{
			if(bw != null)
			{
				bw.close();
			}
		}
		
	}// makeVariantFile()
	
	
	private void makeVariantXMLTemplateData(HashMap td, List provList, File adjacencyFile)
	{
		// DEFAULT unknown data
		td.put("mapTitle", UNKNOWN_DATA);
		td.put("mapThumbURI", UNKNOWN_DATA);
		td.put("mapURI", UNKNOWN_DATA);
		td.put("mapDescription", UNKNOWN_DATA);
		td.put("variantName", UNKNOWN_DATA);
		td.put("variantDescription", "\t\t\t"+UNKNOWN_DATA);
		
		
		
		// DEFAULT data (use sensible defaults)
		td.put("startingtime", 
			"\t\t<STARTINGTIME turn=\"Spring, 1901, Movement\" />\n"
		);
		
		td.put("victoryconditions", 
			"\t\t<VICTORYCONDITIONS>\n"+
				"\t\t\t<WINNING_SUPPLY_CENTERS value=\""+UNKNOWN_DATA+"\"/>\n"+
			"\t\t</VICTORYCONDITIONS>\n"
		);
		
		td.put("ruleoptions", 
			"\t\t<RULEOPTION name=\"OPTION_BUILDS\" value=\"VALUE_BUILDS_HOME_ONLY\"/>\n"
		);
		

		// powers
		// these are derived from the chars
		StringBuffer sb = new StringBuffer();
		Iterator iter = provList.iterator();
		HashMap powerMap = new HashMap();
		while(iter.hasNext())
		{
			ProvObj po = (ProvObj) iter.next();
			final String homeSCPower = po.getHomeSC();
			if(homeSCPower != null)
			{
				if(!powerMap.containsKey(homeSCPower))
				{
					powerMap.put(homeSCPower, Boolean.TRUE);
					
					sb.append("\t\t<POWER name=\"");
					sb.append(homeSCPower);
					sb.append("\" active=\"true\" adjective=\"");
					sb.append(UNKNOWN_DATA);
					sb.append("\"/>\n");
				}
			}
		}	
		td.put("powers", sb.toString());
		
		// SC
		sb = new StringBuffer();
		iter = provList.iterator();
		while(iter.hasNext())
		{
			ProvObj po = (ProvObj) iter.next();
			
			if(po.isNeutralSC())
			{
				sb.append("\t\t<SUPPLYCENTER province=\"");
				sb.append(po.getSN());
				sb.append("\"/>\n");
			}
			else if(po.isHomeSC())
			{
				sb.append("\t\t<SUPPLYCENTER province=\"");
				sb.append(po.getSN());
				sb.append("\" homepower=\"");
				sb.append(po.getHomeSC());
				sb.append("\" owner=\"");
				sb.append(po.getHomeSC());
				sb.append("\"/>\n");
			}
		}
		td.put("supplycenters", sb.toString());
		
		
		// initial unit positions
		// (this is not known)
		td.put("initialstate", 
			"\t\t<!-- INITIALSTATE objects cannot be filled in by MapConvert -->");
		
		// adjacency file name (NO path info!)
		td.put("adjacencyURI", adjacencyFile.getName());
	}// makeVariantXMLTemplateData()
	
	
}// class MapConvert
