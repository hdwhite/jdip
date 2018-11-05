//
//  @(#)RPConvert.java 	2004
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
package jdip.tool.conversion;

import java.io.*;
import java.util.*;

/**
	Converts Realpolitik variants to jDip variants.
	<p>
	This must be run from the command line<br>
	e.g., <code>java dip.misc.RPConvert [input-file]</code>
	</p>
	<p>
	This will create, with the exception of graphical map information,
	a fully-specified "variants.xml" file and a fully-specified adjacency
	data file.
	</p>
	<p>
	This fully supports:
    </p>
	<ul>
		<li>Convoyable coasts</li>
		<li>Ice provinces</li>
		<li>Islands (the older version of MapConvert did not)</li>
		<li>The 'mx' modifier (e.g., in Loeb9); movement with one less support</li>
		<li>Provinces defined but without connectivity (e.g., Switzerland)</li>
	</ul>
	<p>
	This does <b>not</b> yet support:
    </p>
    <ul>
		<li>SVG Map creation from Realpolitik maps</li>
	</ul>
*/
public class RPConvert
{
	private static final String VARIANT_TEMPLATE 	= "jdip/tool/conversion/VariantXMLTemplate.txt";
	private static final String UNKNOWN_DATA = "_UNKNOWN_";


	public static void main(String args[])
	{
		if(args.length != 1)
		{
			System.err.println("\nrpconvert: converts realpolitik variant files to jDip format.");
			System.err.println("\nUSAGE: rpconvert <input_file>");
			System.err.println("\nEXAMPLE: rpconvert \"\\Programs\\Realpolitik\\Variant Files\\Modern\\Modern.var\"");
			System.err.println("\nThis will create: ");
			System.err.println("   [variantName]_adjacency.xml (a jDip variant adjacency file)");
			System.err.println("   variants.xml (the variant definition file)");
			System.err.println("\nThe created variants.xml file will be nearly-completely specified.");
			System.exit(1);
		}

		String in = args[0];
		if(!in.endsWith(".var"))
		{
			System.err.println("Variant files must end with \".var\"");
			System.exit(1);
		}


		try
		{
			File inFile = new File(in);
			new RPConvert(inFile);
		}
		catch(IOException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
		catch(NumberFormatException e2)
		{
			System.err.println(e2.getMessage());
			System.exit(1);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			System.exit(1);
		}

		System.out.println("Conversion successful.");
	}// main()


	public RPConvert(File variantFile)
	throws IOException
	{
		// read Variant (.var)
		Variant variant = new Variant(variantFile);
		System.out.println("Variant file read OK. ("+variantFile.getName()+")");

		// read Countries (.cnt)
		Country[] countries = Country.makeCountries(variant.getCountryFile());
		System.out.println("Country file read OK. ("+variant.getCountryFile().getName()+")");

		// read Game file (.gam)
		parseGameFile(variant, countries);
		System.out.println("Game file read OK. ("+variant.getGameFile().getName()+")");

		// read info file (.txt, .info)
		String info = infoToHTML(variant.getInfoFile());
		System.out.println("Description file read OK. ("+variant.getInfoFile().getName()+")");
		variant.setInfo(info);

		// get variant name prefix.
		final String variantName = variant.getName();

		// parse .map file (use MapConvert) and create adjacency
		//
		File adjFile = new File(variantName+"_adjacency.xml");
		MapConvert mc = new MapConvert(variant.getMapFile(), adjFile);
		System.out.println("Map file read successfully");
		System.out.println("jDip Adjacency file created.");

		// create variant.xml file
		makeVariantXMLFile(mc, variant, countries);
		System.out.println("jdip variants.xml file created.");

	}// RPConvert()


	private void makeVariantXMLFile(MapConvert mc, Variant variant, Country[] countries)
	throws IOException
	{
		if(variant == null || countries == null)
		{
			throw new IllegalArgumentException();
		}

		final ClassLoader cl = this.getClass().getClassLoader();
		final Template variantTemplate = new Template(
			cl.getResourceAsStream(VARIANT_TEMPLATE) );

		File out = new File("variants.xml");

		BufferedWriter bw = null;
		try
		{
			bw = new BufferedWriter(new FileWriter(out));

			// write adjacency data
            HashMap<String, String> templateData = new HashMap<>();
			makeVariantXMLTemplateData(templateData, mc, variant, countries);
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
	}// makeVariantXMLFile()

    private void makeVariantXMLTemplateData(HashMap<String, String> td, MapConvert mc,
                                            Variant variant, Country[] countries)
	throws IOException
	{
		// DEFAULT unknown data
		td.put("mapTitle", UNKNOWN_DATA);
		td.put("mapThumbURI", UNKNOWN_DATA);
		td.put("mapURI", UNKNOWN_DATA);
		td.put("mapDescription", UNKNOWN_DATA);


		// simple data
		td.put("variantName", variant.getName());
		td.put("variantDescription", variant.getInfo());
		td.put("startingtime",
			"\t\t<STARTINGTIME turn=\""+variant.getStartingPhase()+"\" />\n"
		);

		// adjacency file name (NO path info!)
		td.put("adjacencyURI", variant.getName()+"_adjacency.xml");

		// winning SC
		int winningSC = variant.getWinningSCCount();
		if(winningSC == 0)
		{
			Iterator iter = mc.getProvObjList().iterator();
			int scCount = 0;
			while(iter.hasNext())
			{
				ProvObj po = (ProvObj) iter.next();
				if(po.isSC())
				{
					scCount++;
				}
			}

			winningSC = (scCount / 2) + 1;
		}

		td.put("victoryconditions",
			"\t\t<VICTORYCONDITIONS>\n"+
				"\t\t\t<WINNING_SUPPLY_CENTERS value=\""+winningSC+"\" />\n"+
			"\t\t</VICTORYCONDITIONS>\n"
		);

		// power (countries)
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<countries.length; i++)
		{
			Country country = countries[i];
			sb.append("\t\t<POWER name=\"");
			sb.append(country.getName());
			sb.append("\" active=\"true\" adjective=\"");
			sb.append(country.getAdjective());
			sb.append("\"/>\n");
		}
		td.put("powers", sb.toString());

		// rule options (only build rule is currently set)
		String buildType = "VALUE_BUILDS_HOME_ONLY";
		if(variant.getBuildType() == Variant.BUILD_CHAOS)
		{
			buildType = "VALUE_BUILDS_ANY_OWNED";
		}
		else if(variant.getBuildType() == Variant.BUILD_ABERRATION)
		{
			buildType = "VALUE_BUILDS_ANY_IF_HOME_OWNED";
		}

		td.put("ruleoptions",
			"\t\t<RULEOPTION name=\"OPTION_BUILDS\" value=\""+buildType+"\"/>"
		);


		// create mapping of Country initials to Country
        final HashMap<String, Country> i2cMap = new HashMap<>();
		for(int i=0; i<countries.length; i++)
		{
			i2cMap.put(countries[i].getInitial(), countries[i]);
		}

		// SC
		sb = new StringBuffer();
		Iterator iter = mc.getProvObjList().iterator();
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

				// find country
                Country c = i2cMap.get(po.getHomeSC());
				if(c == null)
				{
					throw new IOException("Cannot find a Country for initial "+po.getHomeSC()+".\n");
				}

				sb.append("\" homepower=\"");
				sb.append(c.getName());

				// if owned, set owner
				for(int i=0; i<countries.length; i++)
				{
					Country country = countries[i];
					Iterator cIter = countries[i].getSC().iterator();
					while(cIter.hasNext())
					{
						Loc loc = (Loc) cIter.next();
						if(loc.getShortName().equalsIgnoreCase(po.getSN()))
						{
							sb.append("\" owner=\"");
							sb.append(c.getName());
						}
					}
				}

				sb.append("\"/>\n");
			}
		}
		td.put("supplycenters", sb.toString());

		// initial unit positions
		// (this is not known)
		sb = new StringBuffer();
		for(int i=0; i<countries.length; i++)
		{
			/*
				<INITIALSTATE province="stp" power="russia" unit="fleet" unitcoast="sc" />
			*/
			Country country = countries[i];
			iter = country.getUnits().iterator();
			while(iter.hasNext())
			{
				Unit unit = (Unit) iter.next();

				sb.append("\t\t<INITIALSTATE province=\"");
				sb.append(unit.getLoc().getShortName());
				sb.append("\" power=\"");
				sb.append(country.getName());
				sb.append("\" unit=\"");
				sb.append(unit.getType());
				if(unit.getLoc().getCoastType() != null)
				{
					sb.append("\" unitcoast=\"");
					sb.append(unit.getLoc().getCoastType());
				}
				sb.append("\"/>\n");
			}
		}
		td.put("initialstate", sb.toString());

	}// makeVariantXMLTemplateData()


	/**
	*	Finds a non-comment line to process; returns data
	*	after the ":", trimmed. Null if no remaining data
	*	lines left; throws exception if nullOK is 'false')
	*/
	private static String getNextAssignmentLine(LineNumberReader lnr, boolean nullOK)
	throws IOException
	{
		String line = lnr.readLine();
		while(line != null)
		{
			line = line.trim();
			if(!line.startsWith("#"))
			{
				int idx = line.indexOf(":");
				if(idx >= 0)
				{
					return line.substring(idx+1).trim();
				}
				else
				{
					throw new IOException("No \":\" found in line. Invalid line.");
				}
			}

			line = lnr.readLine();
		}

		if(nullOK)
		{
			return null;
		}
		else
		{
			throw new EOFException();
		}
	}// getNextAssignmentLine()


	/**
	*	Finds a non-comment line to process; that line after it is trimmed().
	*/
	private static String getNextDataLine(LineNumberReader lnr)
	throws IOException
	{
		String line = lnr.readLine();
		while(line != null)
		{
			line = line.trim();
			if(!line.startsWith("#"))
			{
				return line;
			}

			line = lnr.readLine();
		}

		throw new EOFException();
	}// getNextDataLine()



	/**
	*	Converts a description file (.info, .txt)to appropriate
	*	jdip-compatable description HTML. This basically puts
	*	paragraph markers around paragraphs.
	*/
	private String infoToHTML(File file)
	throws IOException
	{
		if(file == null)
		{
			throw new IllegalArgumentException();
		}

		StringBuffer sb = new StringBuffer(4096);
		BufferedReader br = null;

		try
		{
			br = new BufferedReader(new FileReader(file));

			sb.append("\t\t<p>\n");

			String line = br.readLine();
			while(line != null)
			{
				line = line.trim();
				if("".equals(line))
				{
					sb.append("\t\t</p>\n\t\t<p>\n");
				}
				else
				{
					sb.append("\t\t\t");
					sb.append(line);
					sb.append("\n");
				}

				line = br.readLine();
			}

			sb.append("\t\t</p>\n");
		}
		finally
		{
			if(br != null)
			{
				br.close();
			}
		}

		return sb.toString();
	}// infoToHTML()


	/**
	*	Parses the .gam file, and adds appropriate data to the Variant
	*	or Country object.
	*/
	private void parseGameFile(Variant variant, Country[] countries)
	throws IOException
	{
			if(variant == null || countries == null)
			{
				throw new IllegalArgumentException();
			}

			boolean isDislodgedPhase = false;
			boolean isAdjustmentPhase = false;
			String startingPhase = null;

			LineNumberReader lnr = null;
			try
			{
				lnr = new LineNumberReader(new BufferedReader(
					new FileReader(variant.getGameFile())));

				String value = null;
				// VERSION
				value = getNextDataLine(lnr);
				if(!"1".equals(value))
				{
					throw new IOException("Only version 1 game (.gam) files can be read.");
				}

				// GAME NAME (ignored)
				value = getNextDataLine(lnr);

				// VARIANT NAME
				value = getNextDataLine(lnr);
				if(!variant.getName().equalsIgnoreCase(value))
				{
					throw new IOException(
						"Variant name Mismatch!\n"+
						"   .var name: "+variant.getName()+"\n"+
						"   .gam variant name: "+value+"\n"
					);
				}

				// START TIME
				startingPhase = getNextDataLine(lnr);

				// # OF ADJUSTMENTS
				value = getNextDataLine(lnr);
				if( Integer.parseInt(value) > 0 )
				{
					isAdjustmentPhase = true;
				}

				// # OF COUNTRIES
				value = getNextDataLine(lnr);
				int nCountries = Integer.parseInt(value);
				if(nCountries != countries.length)
				{
					throw new IOException(
						"Country Number Mismatch!!\n"+
						"   number of countries in .cnt file: "+countries.length+"\n"+
						"   number of countries specified in .gam file: "+value+"\n"
					);
				}


				// read each country (assume order is same as Countries[] array)
				//
				for(int i=0; i<countries.length; i++)
				{
					// we don't care about adjustments
					value = getNextDataLine(lnr);

					// HOME SC
					value = getNextDataLine(lnr);
					StringTokenizer st = new StringTokenizer(value, " ;,\n\r");
					while(st.hasMoreTokens())
					{
						final Loc loc = Loc.makeLoc(st.nextToken(), null);
						countries[i].addSC(loc);
					}

					// UNITS
					value = getNextDataLine(lnr);
					Unit[] units = Unit.makeUnits(value);
					countries[i].addUnits(units);
				}

				// # OF DISLODGES
				value = getNextDataLine(lnr);
				if( Integer.parseInt(value) > 0 )
				{
					isDislodgedPhase = true;
				}

				// phase-check
				if(isDislodgedPhase && isAdjustmentPhase)
				{
					throw new IOException("Adjustments and dislodges cannot coexist!");
				}

				// set starting phase in Variant
				//
				if(isDislodgedPhase)
				{
					startingPhase += " Retreat";
				}
				else if(isAdjustmentPhase)
				{
					startingPhase += " Adjustment";
				}
				else
				{
					startingPhase += " Movement";
				}

				variant.setStartingPhase(startingPhase);
			}
			catch(Exception e)
			{
				if(lnr != null)
				{
					throw new IOException(e.getMessage());
				}
				else
				{
					throw new IOException("ERROR parsing: "+variant.getGameFile()+
						"\n  line: "+lnr.getLineNumber()+
						"\n  "+e.getMessage());
				}
			}
			finally
			{
				if(lnr != null)
				{
					lnr.close();
				}
			}
	}// parseGameFile()


	static class Variant
	{
		public static final String BUILD_STANDARD		= "Standard";
		public static final String BUILD_ABERRATION	= "Aberration";
		public static final String BUILD_CHAOS			= "Chaos";

		private File mapFile;
		private File countryFile;
		private File gameFile;
		private File infoFile;
		private File regionFile;
		private String name;
		private String buildType;
		private int sc;
		private File variantFile;

		// data filled in later
		private String startingPhase;
		private String info;

		/**
		*	Parses the .var file. This does not check the
		*	file extension.
		*/
		public Variant(File file)
		throws IOException
		{
			if(file == null)
			{
				throw new IllegalArgumentException();
			}

			variantFile = file;
			LineNumberReader lnr = null;
			try
			{
				lnr = new LineNumberReader(new BufferedReader(new FileReader(file)));

				String value = null;
				// VERSION
				value = getNextAssignmentLine(lnr, false);
				if(!"1".equals(value))
				{
					throw new IOException("Only version 1 Variant (.var) files can be read.");
				}

				// NAME
				name = getNextAssignmentLine(lnr, false);

				// MAP DATA
				value = getNextAssignmentLine(lnr, false);
				mapFile = makeFile(value);

				// COUNTRIES
				value = getNextAssignmentLine(lnr, false);
				countryFile = makeFile(value);

				// GAME
				value = getNextAssignmentLine(lnr, false);
				gameFile = makeFile(value);

				// BWMAP (ignored)
				value = getNextAssignmentLine(lnr, false);

				// COLORMAP (ignored)
				value = getNextAssignmentLine(lnr, false);

				// REGIONS
				value = getNextAssignmentLine(lnr, false);
				regionFile = makeFile(value);

				// INFO
				value = getNextAssignmentLine(lnr, false);
				infoFile = makeFile(value);

				// BUILD
				value = getNextAssignmentLine(lnr, false);
				if(BUILD_STANDARD.equalsIgnoreCase(value))
				{
					buildType = BUILD_STANDARD;
				}
				else if(BUILD_CHAOS.equalsIgnoreCase(value))
				{
					buildType = BUILD_CHAOS;
				}
				else if(BUILD_ABERRATION.equalsIgnoreCase(value))
				{
					buildType = BUILD_ABERRATION;
				}
				else
				{
					throw new IOException("Illegal build type: "+value);
				}

				// CENTERS
				value = getNextAssignmentLine(lnr, false);
				sc = Integer.parseInt(value);

				// FLAGS (ignored)
				value = getNextAssignmentLine(lnr, false);
			}
			catch(Exception e)
			{
				if(lnr != null)
				{
					throw new IOException(e.getMessage());
				}
				else
				{
					throw new IOException("ERROR parsing: "+file+
						"\n  line: "+lnr.getLineNumber()+
						"\n  "+e.getMessage());
				}
			}
			finally
			{
				if(lnr != null)
				{
					lnr.close();
				}
			}
		}// pareFromFile()

		public void setStartingPhase(String value)
		{
			startingPhase = value;
		}

		public String getStartingPhase()	{ return startingPhase; }

		public void setInfo(String value)
		{
			info = value;
		}

		public String getInfo()	{ return info; }

		/** Gets the name */
		public String getName()		{ return name; }

		/** Returns the .map file */
		public File getMapFile()		{ return mapFile; }

		/** Returns the .cnt file */
		public File getCountryFile()		{ return countryFile; }

		/** Returns the .gam file */
		public File getGameFile()		{ return gameFile; }

		/** Returns the .rgn file */
		public File getRegionFile()		{ return regionFile; }

		/** Returns the .info file */
		public File getInfoFile()		{ return infoFile; }

		/** Returns the build type constant */
		public String getBuildType()		{ return buildType; }

		/**
		*	Supply Centers required to win. This may return 0; if so,
		*	Need to calculate winning SC based on: <br>
		*		(totalSC / 2) + 1; rounding down.
		*/
		public int getWinningSCCount()		{ return sc; }



		/**
		*	Ensures that the file is normalized. RP files can
		*	refer to other files within the directory "Variant Files",
		*	so we need to ensure that we have at least 2 directories
		*	above if that's the case. Otherwise, we use the file
		*	specified in the current directory.
		*/
		private File makeFile(String value)
		throws IOException
		{
			boolean isReferenced = false;
			if(value.indexOf("/") >= 0 || value.indexOf("\"") >= 0)
			{
				isReferenced = true;
			}

			// relative file.
			//
			File root = variantFile.getParentFile();
			if(isReferenced)
			{
				if(root != null)
				{
					root = root.getParentFile();
					if(root != null)
					{
						return new File(root, value);
					}
				}

				throw new IOException(
					"Reference to file: \""+value+"\" cannot be made.\n"+
					"Make sure your directory structure is correct!\n"+
					"If all else fails, put all the files in the same directory\n"+
					"And make sure the .var file reflects those changes."
				);
			}
			else if(root != null)
			{
				return new File(root, value);
			}
			else
			{
				return new File(value);
			}
		}// makeFile()
	}// class Variant


	static class Country
	{
		private String name;
		private String adjective;
		private String initial;
		private String patternName;
		private String colorName;

        private List<Loc> sc;
        private List<Unit> units;
        private List<Unit> dislodgedUnits;

		private Country()
		{
            sc = new LinkedList<>();
            units = new LinkedList<>();
            dislodgedUnits = new LinkedList<>();
		}


        public List<Unit> getUnits() {
            return units;
        }

        public List<Loc> getSC() {
            return sc;
        }


		/** Parses all Country data from a .cnt file; */
		public static Country[] makeCountries(File file)
		throws IOException
		{
            ArrayList<Country> al = new ArrayList<>();

			if(file == null)
			{
				throw new IllegalArgumentException();
			}

			LineNumberReader lnr = null;
			try
			{
				lnr = new LineNumberReader(new BufferedReader(new FileReader(file)));

				String value = null;

				// VERSION
				value = getNextDataLine(lnr);
				if(!"1".equals(value))
				{
					throw new IOException("Only version 1 Country (.cnt) files can be read.");
				}

				// # of countries
				value = getNextDataLine(lnr);
				final int nCountries = Integer.parseInt(value);

				for(int i=0; i<nCountries; i++)
				{
					value = getNextDataLine(lnr);

					StringTokenizer st = new StringTokenizer(value, " ");
					Country country = new Country();
					String tokName = null;
					try
					{
						tokName = "name";
						country.name = st.nextToken();
						tokName = "adjective";
						country.adjective = st.nextToken();
						tokName = "(capital) initial";
						country.initial = st.nextToken();
						tokName = "pattern";
						country.patternName = st.nextToken();
						tokName = "color";
						country.colorName = st.nextToken();
					}
					catch(NoSuchElementException e)
					{
						throw new Exception("Counry data error: Could not find: "+tokName);
					}

					al.add(country);
				}
			}
			catch(Exception e)
			{
				if(lnr == null)
				{
					throw new IOException(e.getMessage());
				}
				else
				{
					throw new IOException("ERROR parsing: "+file+
						"\n  line: "+lnr.getLineNumber()+
						"\n  "+e.getMessage());
				}
			}
			finally
			{
				if(lnr != null)
				{
					lnr.close();
				}
			}

			assert (al.size() > 0);
            return al.toArray(new Country[al.size()]);
		}// makeCountries()


		public String getName()			{ return name; }
		public String getAdjective()	{ return adjective; }
		public String getInitial()		{ return initial; }
		public String getPatternName()	{ return patternName; }
		public String getColorName()	{ return colorName; }


		public void addSC(Loc loc)
		{
			if(loc == null)
			{
				throw new IllegalArgumentException();
			}
			sc.add(loc);
		}// addSC()

		public void addUnit(Unit unit)
		{
			if(unit == null)
			{
				throw new IllegalArgumentException();
			}

			units.add(unit);
		}// addUnit()

		public void addUnits(Unit[] units)
		{
			if(units == null)
			{
				throw new IllegalArgumentException();
			}

			this.units.addAll(Arrays.asList(units));
		}// addUnits()

		public void addDislodgedUnit(Unit unit)
		{
			if(unit == null)
			{
				throw new IllegalArgumentException();
			}
			dislodgedUnits.add(unit);
		}// addDislodgedUnit()
	}// makeCountry()

	/** A Unit has a unit type and a location. */
	static class Unit
	{
		public final static String UNIT_WING	= "wing";
		public final static String UNIT_FLEET	= "fleet";
		public final static String UNIT_ARMY	= "army";

		private String type;
		private Loc location;

		/** TYPE must be a constant defined by this class */
		public Unit(String type, Loc location)
		{
			if(type == null || location == null)
			{
				throw new IllegalArgumentException();
			}

			if(type != UNIT_WING && type != UNIT_FLEET && type
				!= UNIT_ARMY)
			{
				throw new IllegalArgumentException("invalid type");
			}


			this.type = type;
			this.location = location;
		}// Unit()


		public String getType()
		{
			return type;
		}// getType()


		public Loc getLoc()
		{
			return location;
		}// getLoc()

		/**
		*	Parses text into a Unit: e.g.:<br>
		*	x yyy<br>
		*	where 'x' is the unit type (single letter)<br>
		*	yyy is the location; coast is optional.<br>
		*	Whitespace separators are used<br>.
		*	x yyy x zzz x yyy format.
		* 	An even # of tokens is required. If no tokens, 0-length array
		*	is returned. This will not return 'null'.
		*/
		public static Unit[] makeUnits(String input)
		throws IOException
		{
            ArrayList<Unit> al = new ArrayList<>(10);

			StringTokenizer st = new StringTokenizer(input, " \t\n\r,;");
			while(st.hasMoreTokens())
			{
				String unitType = null;

				if(st.hasMoreTokens())
				{
					final String tok = st.nextToken().toLowerCase();
					if("a".equals(tok))
					{
						unitType = UNIT_ARMY;
					}
					else if("f".equals(tok))
					{
						unitType = UNIT_FLEET;
					}
					else if("w".equals(tok))
					{
						unitType = UNIT_WING;
					}
					else
					{
						throw new IOException("Unknown Unit Type: \""+tok+"\" found");
					}
				}
				else
				{
					throw new IOException("Expected unit type (a, f, w)");
				}

				Loc unitLoc = null;

				if(st.hasMoreTokens())
				{
					final String tok = st.nextToken().toLowerCase();
					unitLoc = Loc.makeLoc(tok, null);
				}
				else
				{
					throw new IOException("Expected unit location following unit type!");
				}

				assert (unitLoc != null);
				assert (unitType != null);
				al.add(new Unit(unitType, unitLoc));
			}

            return al.toArray(new Unit[al.size()]);
		}// makeUnit()
	}// Unit


}// class RPConvert
