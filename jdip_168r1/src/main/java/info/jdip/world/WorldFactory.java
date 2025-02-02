//
//  @(#)WorldFactory.java		4/2002
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
package info.jdip.world;

import info.jdip.misc.Utils;
import info.jdip.order.OrderException;
import info.jdip.world.variant.data.BorderData;
import info.jdip.world.variant.data.InitialState;
import info.jdip.world.variant.data.Influence;
import info.jdip.world.variant.data.ProvinceData;
import info.jdip.world.variant.data.SupplyCenter;
import info.jdip.world.variant.data.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;


/**
 * A WorldFactory creates World objects from XML map data.
 */
public class WorldFactory {
    private static final Logger logger = LoggerFactory.getLogger(WorldFactory.class);

    // il8n
    private static final String WF_PROV_NON_UNIQUE = "WF_PROV_NON_UNIQUE";
    private static final String WF_PROV_MISMATCH = "WF_PROV_MISMATCH";
    //private static final String WF_VARIANT_NOTFOUND = "WF_VARIANT_NOTFOUND";
    private static final String WF_BAD_STARTINGTIME = "WF_BAD_STARTINGTIME";
    private static final String WF_BAD_SC_PROVINCE = "WF_BAD_SC_PROVINCE";
    private static final String WF_BAD_SC_HOMEPOWER = "WF_BAD_SC_HOMEPOWER";
    private static final String WF_BAD_SC_OWNER = "WF_BAD_SC_OWNER";
    private static final String WF_BAD_IS_POWER = "WF_BAD_IS_POWER";
    private static final String WF_BAD_IS_PROVINCE = "WF_BAD_IS_PROVINCE";
    private static final String WF_BAD_IS_UNIT_LOC = "WF_BAD_IS_UNIT_LOC";
    private static final String WF_BAD_IS_UNIT = "WF_BAD_IS_UNIT";
    private static final String WF_BAD_VC = "WF_BAD_VC";
    private static final String WF_ADJ_BAD_TYPE = "WF_ADJ_BAD_TYPE";
    private static final String WF_ADJ_BAD_PROVINCE = "WF_ADJ_BAD_PROVINCE";
    private static final String WF_ADJ_INVALID = "WF_ADJ_INVALID";
    //private static final String WF_BAD_BUILDOPTION = "WF_BADBUILDOPTION";
    private static final String WF_BAD_BORDER_NAME = "WF_BAD_BORDER_NAME";
    private static final String WF_BAD_BORDER_LOCATION = "WF_BAD_BORDER_LOCATION";


    // class variables
    private static WorldFactory instance = null;


    private WorldFactory() {
    }// WorldFactory()


    /**
     * Get an instance of the WorldFactory
     */
    public static synchronized WorldFactory getInstance() {
        if (instance == null) {
            instance = new WorldFactory();
        }

        return instance;
    }// getInstance()


    /**
     * Generates a World given the supplied Variant information
     */
    public World createWorld(Variant variant)
            throws InvalidWorldException {
        if (variant == null) {
            throw new IllegalArgumentException();
        }

        logger.info("Creating world for the variant: {} - {} (aliases: {})", variant.getName(),variant.getVersion(),(Object) variant.getAliases());

        List<Province> provinces = new ArrayList<>(100);
        HashMap<String, Province> provNameMap = new HashMap<>();    // mapping of names->provinces

        // gather all province data, and create provinces
        ProvinceData[] provinceDataArray = variant.getProvinceData();
        for (int i = 0; i < provinceDataArray.length; i++) {
            ProvinceData provinceData = provinceDataArray[i];

            // get short names
            String[] shortNames = provinceData.getShortNames();

            // verify uniqueness of names
            if (!isUnique(provNameMap, provinceData.getFullName(), shortNames)) {
                throw new InvalidWorldException(Utils.getLocalString(WF_PROV_NON_UNIQUE, provinceData.getFullName()));
            }

            // create Province object
            Province province = new Province(provinceData.getFullName(), shortNames, i, provinceData.getConvoyableCoast(), provinceData.getImpassable());

            // add Province data to list
            provinces.add(province);

            // add Province names (all) to our name->province map
            provNameMap.put(province.getFullName().toLowerCase(), province);
            String[] lcProvNames = province.getShortNames();
            for (String lcProvName : lcProvNames) {
                provNameMap.put(lcProvName.toLowerCase(), province);
            }
        }

        // gather all adjacency data
        // parse adjacency data for all provinces
        // keep a list of the locations parsed below
        ArrayList<Location> locationList = new ArrayList<>(16);
        for (ProvinceData provinceData : provinceDataArray) {
            String[] adjProvinceTypes = provinceData.getAdjacentProvinceTypes();
            String[] adjProvinceNames = provinceData.getAdjacentProvinceNames();

            if (adjProvinceTypes.length != adjProvinceNames.length) {
                throw new InvalidWorldException(Utils.getLocalString(WF_PROV_MISMATCH));
            }

            // get the Province to which this adjacency data refers
            Province province = provNameMap.get(provinceData.getFullName().toLowerCase());

            // get the Adjacency data structure from the Province
            Province.Adjacency adjacency = province.getAdjacency();

            // parse adjacency data, then set it for this province
            for (int adjIdx = 0; adjIdx < adjProvinceTypes.length; adjIdx++) {
                // get the coast type.
                Coast coast = Coast.parse(adjProvinceTypes[adjIdx]);

                // clear the location list (we re-use it)
                locationList.clear();

                // parse provinces, making locations for each
                // provinces must be seperated by " " or "," or ";" or ":"
                String input = adjProvinceNames[adjIdx].trim().toLowerCase();
                StringTokenizer st = new StringTokenizer(input, " ,;:\t\n\r", false);
                while (st.hasMoreTokens()) {
                    // makeLocation() will change the coast, as needed, and verify the province
                    Location location = makeLocation(provNameMap, st.nextToken(), coast);
                    locationList.add(location);
                }

                // add data to adjacency table after unwrapping collection
                Location[] locations = locationList.toArray(new Location[locationList.size()]);
                adjacency.setLocations(coast, locations);
            }


            // validate adjacency data
            if (!adjacency.validate(province)) {
                throw new InvalidWorldException(Utils.getLocalString(WF_ADJ_INVALID, provinceData.getFullName()));
            }

            // create wing coast
            adjacency.createWingCoasts();
        }

        // Process BorderData. This requires the Provinces to be known and
        // successfully parsed. They are mapped to the ID name, stored in the borderMap.
        HashMap<String, Border> borderMap = new HashMap<>(11);
        try {
            BorderData[] borderDataArray = variant.getBorderData();
            for (BorderData bd : borderDataArray) {
                Location[] fromLocs = makeBorderLocations(bd.getFrom(), provNameMap);

                Border border = new Border(bd.getID(), bd.getDescription(),
                        bd.getUnitTypes(), fromLocs, bd.getOrderTypes(),
                        bd.getBaseMoveModifier(), bd.getSeason(),
                        bd.getPhase(), bd.getYear());

                borderMap.put(bd.getID(), border);
            }
        } catch (InvalidBorderException ibe) {
            throw new InvalidWorldException(ibe.getMessage());
        }

        // set the Border data (if any) for each province.
        {
            ArrayList<Border> list = new ArrayList<>(10);

            for (ProvinceData provinceData : provinceDataArray) {
                list.clear();
                Province province = provNameMap.get(provinceData.getFullName().toLowerCase());

                String[] borderNames = provinceData.getBorders();
                for (String borderName : borderNames) {
                    Border border = borderMap.get(borderName);
                    if (border == null) {
                        throw new InvalidWorldException(Utils.getLocalString(WF_BAD_BORDER_NAME, province.getShortName(), borderName));
                    }

                    list.add(border);
                }

                if (!list.isEmpty()) {
                    province.setBorders(list.toArray(new Border[list.size()]));
                }
            }
        }

        // Now that we know the variant, we know the powers, and can
        // create the Map.
        info.jdip.world.Map map = new info.jdip.world.Map(
                variant.getPowers(),
                provinces.toArray(new Province[provinces.size()]));

        // create the World object as well, now that we have the Map
        World world = new World(map);
        borderMap.clear();

        // create initial turn state based on starting game time
        Phase phase = variant.getStartingPhase();
        if (phase == null) {
            throw new InvalidWorldException(Utils.getLocalString(WF_BAD_STARTINGTIME));
        }

        // create the Position object, as we will need it for various game state
        Position pos = new Position(map);

        // define supply centers
        SupplyCenter[] supplyCenters = variant.getSupplyCenters();
        for (SupplyCenter supplyCenter : supplyCenters) {
            Province province = map.getProvince(supplyCenter.getProvinceName());
            if (province == null) {
                throw new InvalidWorldException(Utils.getLocalString(WF_BAD_SC_PROVINCE, supplyCenter.getProvinceName()));
            }

            province.setSupplyCenter(true);

            String hpName = supplyCenter.getHomePowerName();
            if (!"none".equalsIgnoreCase(hpName)) {
                Power power = map.getPower(hpName);
                if (power == null) {
                    throw new InvalidWorldException(Utils.getLocalString(WF_BAD_SC_HOMEPOWER, hpName));
                }

                pos.setSupplyCenterHomePower(province, power);
            }

            // define current owner of supply center, if any
            String scOwner = supplyCenter.getOwnerName();
            if (!"none".equalsIgnoreCase(scOwner)) {
                Power power = map.getPower(scOwner);
                if (power == null) {
                    throw new InvalidWorldException(Utils.getLocalString(WF_BAD_SC_OWNER, scOwner));
                }

                pos.setSupplyCenterOwner(province, power);
            }
        }


        // set initial state [derived from INITIALSTATE elements in XML file]
        InitialState[] initStates = variant.getInitialStates();
        for (InitialState initState : initStates) {
            // a province and power is required, no matter what, unless
            // we are ONLY setting the supply center (which we do above)
            Power power = map.getPowerMatching(initState.getPowerName());
            Province province = map.getProvinceMatching(initState.getProvinceName());

            // n/a if we use a validating parser
            if (power == null) {
                throw new InvalidWorldException(Utils.getLocalString(WF_BAD_IS_POWER));
            }

            // n/a if we use a validating parser
            if (province == null) {
                throw new InvalidWorldException(Utils.getLocalString(WF_BAD_IS_PROVINCE));
            }

            Unit.Type unitType = initState.getUnitType();

            if (unitType == null) {
                throw new InvalidWorldException(Utils.getLocalString(WF_BAD_IS_UNIT, initState.getProvinceName()));
            }
            // create unit in province, if location is valid
            Coast coast = initState.getCoast();

            Unit unit = new Unit(power, unitType);
            Location location = new Location(province, coast);
            try {
                location = location.getValidatedSetup(unitType);
                unit.setCoast(location.getCoast());
                pos.setUnit(province, unit);

                // set 'lastOccupier' for unit
                pos.setLastOccupier(province, unit.getPower());
            } catch (OrderException e) {
                throw new InvalidWorldException(Utils.getLocalString(WF_BAD_IS_UNIT_LOC,
                        initState.getProvinceName(), e.getMessage()));
            }
        }
        
		// set initial influences [derived from INFLUENCE elements in XML file]
		Influence[] influences = variant.getInfluences();
		for (Influence curProvince : influences)
		{
			// a province and power is required, no matter what
			Power power = map.getPowerMatching(curProvince.getPowerName());
			Province province = map.getProvinceMatching(curProvince.getProvinceName());

			// n/a if we use a validating parser
			if(power == null)
			{
				throw new InvalidWorldException(Utils.getLocalString(WF_BAD_IS_POWER));
			}

			// n/a if we use a validating parser
			if(province == null)
			{
				throw new InvalidWorldException(Utils.getLocalString(WF_BAD_IS_PROVINCE));
			}

			// set 'lastOccupier' for province
			pos.setLastOccupier(province, power);
		}

        // set the victory conditions
        // make sure we have at least one victory condition!
        if (variant.getNumSCForVictory() <= 0
                && variant.getMaxYearsNoSCChange() <= 0
                && variant.getMaxGameTimeYears() <= 0) {
            throw new InvalidWorldException(Utils.getLocalString(WF_BAD_VC));
        }

        VictoryConditions vc = new VictoryConditions(variant.getNumSCForVictory(), variant.getMaxYearsNoSCChange(),
                variant.getMaxGameTimeYears(), phase);
        world.setVictoryConditions(vc);

        // set TurnState / Map / complete World creation.
        TurnState turnState = new TurnState(phase);
        turnState.setPosition(pos);
        turnState.setWorld(world);
        world.setTurnState(turnState);

        return world;
    }// makeWorld()


    /**
     * Parses the Adjacency data and converts it into the Location objects
     * e.g.:
     * <p>
     * The default coast is corrected as follows:
     * <br>	'mv' : stays 'mv' (Coast.LAND)
     * <br>	'xc' : stays 'xc' (Coast.SINGLE)
     * <br>	'nc', 'wc', 'ec', 'sc' : converted to 'xc' (Coast.SINGLE)
     * <p>
     * The default coast is over-ridden if a coast is specified
     * after a ref. thus given: &lt;ADJACENCY type="xc" refs="xxx yyy/sc"/&gt;
     * "yyy" will have a coast of "sc" (Coast.SOUTH)
     * <p>
     * The reason is because look at the following (for bulgaria)
     * <pre>
     * 	&lt;PROVINCE shortname="bul" fullname="Bulgaria"&gt;
     * 		&lt;ADJACENCY type="mv" refs="gre con ser rum" /&gt;
     * 		&lt;ADJACENCY type="ec" refs="con bla rum" /&gt;
     * 		&lt;ADJACENCY type="sc" refs="gre aeg con" /&gt;
     * 	&lt;/PROVINCE&gt;
     * </pre>
     * If we do not convert directional coasts (nc/ec/wc/sc) to (xc),
     * bul/ec would then be linked to con/ec, bla/ec, rum/ec which
     * do not even exist.
     */
    private Location makeLocation(HashMap<String, Province> provNameMap, String name, Coast theDefaultCoast)
            throws InvalidWorldException {
        Coast defaultCoast = theDefaultCoast;

        if (defaultCoast.equals(Coast.UNDEFINED)) {
            throw new InvalidWorldException(Utils.getLocalString(WF_ADJ_BAD_TYPE, name));
        } else if (defaultCoast.equals(Coast.NORTH) || defaultCoast.equals(Coast.WEST)
                || defaultCoast.equals(Coast.SOUTH) || defaultCoast.equals(Coast.EAST)) {
            defaultCoast = Coast.SINGLE;
        }

        Coast coast = Coast.parse(name);
        String provinceName = Coast.getProvinceName(name);

        if (coast.equals(Coast.UNDEFINED)) {
            coast = defaultCoast;
        }

        // name lookup
        Province province = provNameMap.get(provinceName.toLowerCase());
        if (province == null) {
            throw new InvalidWorldException(Utils.getLocalString(WF_ADJ_BAD_PROVINCE,
                    name, provinceName, defaultCoast));
        }

        // create Location
        return new Location(province, coast);
    }// makeLocation()


    /**
     * Makes a Border location. This uses the already-generated Provinces and Adjacency data,
     * which help error checking. It also will create "undefined" coasts by default. If the
     * coast does not exist for a Province (but is not Undefined) then this will create an
     * Exception.
     * <p>
     * Input is a space and/or comma-seperated list.
     * <p>
     * This will return null if there are no border locations, instead of
     * a zero-length array.
     */
    private Location[] makeBorderLocations(String in, HashMap<String, Province> provNameMap)
            throws InvalidWorldException {
        ArrayList<Location> al = new ArrayList<>(6);

        StringTokenizer st = new StringTokenizer(in.trim(), ";, ");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();

            Coast coast = Coast.parse(tok);
            Province province = provNameMap.get(Coast.getProvinceName(tok).toLowerCase());
            if (province == null) {
                throw new InvalidWorldException(Utils.getLocalString(WF_BAD_BORDER_LOCATION, tok));
            }

            al.add(new Location(province, coast));
        }

        if (al.isEmpty()) {
            return null;
        } else {
            return al.toArray(new Location[al.size()]);
        }
    }// makeBorderLocation()


    // verify all names are unique. (hasn't yet been added to the map)
    private boolean isUnique(HashMap<String, Province> provNameMap, String fullname, String[] shortnames) {
        if (provNameMap.get(fullname.toLowerCase()) != null) {
            return false;
        }

        for (String shortname : shortnames) {
            if (provNameMap.get(shortname.toLowerCase()) != null) {
                return false;
            }
        }

        return true;
    }// isUnique()

}// class MapFactory

