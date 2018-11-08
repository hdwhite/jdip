/*  Copyright (C) 2004  Ryan Michela
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.nukesoft.jdipFacade;

import dip.world.Coast;
import dip.world.Location;
import dip.world.Province;
import dip.world.Unit;
import dip.world.World;
import org.nukesoft.jdipFacade.exception.ProvinceNotFoundException;
import org.nukesoft.jdipFacade.exception.StateError;
import org.nukesoft.jdipFacade.exception.UnitNotFoundException;

import java.util.LinkedList;
import java.util.List;


/**
 * Encapsulates information about the Jdip Map. Provides lists of province names
 * falling into various usefull categories.
 *
 * @author Ryan Michela
 */
public class JdipMapInfo {
    //name length constants
    public static final int LONG_NAMES = 0;
    public static final int SHORT_NAMES = 1;
    //province type constants
    public static final int LAND_TYPE = 10;
    public static final int SEA_TYPE = 11;
    public static final int COAST_TYPE = 12;
    //unit type constants
    public static final int ARMY_TYPE = 100;
    public static final int FLEET_TYPE = 101;
    public static final int WING_TYPE = 102;

    private final World world;

    JdipMapInfo(World world) {
        this.world = world;
    }

    /**
     * Fetches a list of all valid province names, including coasts.
     *
     * @param nameLength one of the name length constants
     * @return an array containing the name of each province
     */
    public String[] getAllProvinceNames(int nameLength) {
        Province[] provinces = world.getMap().getProvinces();
        List<String> provWithCoast = new LinkedList<>();
        for (int i = 0; i < provinces.length; i++) {
            if (provinces[i].isCoastal()) {
                Coast[] coasts = provinces[i].getValidDirectionalCoasts();
                if (coasts.length == 0) {
                    provWithCoast.add(getProvinceName(provinces[i], nameLength));
                } else {
                    for (int j = 0; j < coasts.length; j++) {
                        provWithCoast.add(getProvinceName(provinces[i], nameLength) + "/" + coasts[j].getAbbreviation());
                    }
                }
            } else {
                provWithCoast.add(getProvinceName(provinces[i], nameLength));
            }
        }
        //copy result into string array
        Object[] o = provWithCoast.toArray();
        String[] r = new String[o.length];
        for (int i = 0; i < o.length; i++) {
            r[i] = (String) o[i];
        }
        return r;
    }

    /**
     * Fetches a list of all province names for a given power with currently dislodged units.
     * Usefull for creating retreat orders.
     *
     * @param power      the power to search by
     * @param nameLength one of the name length constants
     * @return an array containing the name of each province
     */
    public String[] getDislodgedUnitProvinceNames(JdipPower power, int nameLength) {
        Province[] provinces = world.getLastTurnState().getPosition().getDislodgedUnitProvinces(power.getPower());
        return convertProvincesToStrings(provinces, nameLength);
    }

    /**
     * Fetches a list of all the province names for a given power that are home supply centers for that power.
     * Usefull for creating build orders.
     *
     * @param power      the power to search by
     * @param nameLength one of the name length constants
     * @return an array containing the name of each province
     */
    public String[] getHomeSupplyCenterProvinceNames(JdipPower power, int nameLength) {
        Province[] provinces = world.getLastTurnState().getPosition().getHomeSupplyCenters(power.getPower());
        return convertProvincesToStrings(provinces, nameLength);
    }

    /**
     * Fetches a list of names for all provinces that contain a unit.
     * Usefull for support and convoy orders.
     *
     * @param nameLength one of the name length constants
     * @return an array containing the name of each province
     */
    public String[] getAllUnitPositionProvinceNames(int nameLength) {
        Province[] provinces = world.getLastTurnState().getPosition().getUnitProvinces();
        return convertProvincesToStrings(provinces, nameLength);
    }

    /**
     * Fetches a list of all the province names for a given power that contain a unit.
     * Usefull for move, hold, support, convoy, and disband orders.
     *
     * @param power      the power to search by
     * @param nameLength one of the name length constants
     * @return and array containing the name of each province
     */
    public String[] getUnitPositionProvinceNamesForPower(JdipPower power, int nameLength) {
        Province[] provinces = world.getLastTurnState().getPosition().getUnitProvinces(power.getPower());
        return convertProvincesToStrings(provinces, nameLength);
    }

    /**
     * Fetches a list of all the province names of provinces adjacent to a given province.
     * Usefull for move and support orders.
     *
     * @param provinceShortName the short name of the province to search by
     * @param nameLength        one of the name length constants
     * @return an array containing the name of each province
     * @throws ProvinceNotFoundException thrown if no province can be found for the given name
     */
    public String[] getAdjacentProvinceNames(String provinceShortName, int nameLength)
            throws ProvinceNotFoundException {
        Province centeralProvince = getProvinceObject(provinceShortName);
        Location[] adjacentLocations = centeralProvince.getAdjacentLocations(Coast.TOUCHING);
        LinkedList<String> provsWithCoasts = new LinkedList<>();
        for (int i = 0; i < adjacentLocations.length; i++) {
            Province p = adjacentLocations[i].getProvince();
            if (p.isCoastal()) {
                Coast[] coasts = p.getValidDirectionalCoasts();
                if (coasts.length == 0) {
                    provsWithCoasts.add(getProvinceName(p, nameLength));
                } else {
                    for (int j = 0; j < coasts.length; j++) {
                        if (p.isAdjacent(coasts[j], centeralProvince)) {
                            provsWithCoasts.add(getProvinceName(p, nameLength) + "/" + coasts[j].getAbbreviation());
                        }
                    }
                }
            } else {
                provsWithCoasts.add(getProvinceName(p, nameLength));
            }
        }
        //copy result into string array
        Object[] o = provsWithCoasts.toArray();
        String[] r = new String[o.length];
        for (int i = 0; i < o.length; i++) {
            r[i] = (String) o[i];
        }
        return r;
    }

    /**
     * Fetches a list of all province names that can serve as endpoints for convoy orders.
     * Usefull for convoy orders.
     *
     * @param nameLength one of the name length constants
     * @return an array containing the name of each province
     */
    public String[] getConvoyEndpointProvinces(int nameLength) {
        Province[] allProvinces = world.getMap().getProvinces();
        LinkedList<Province> convoyableProvinces = new LinkedList<>();
        for (int i = 0; i < allProvinces.length; i++) {
            if (allProvinces[i].isCoastal()) {
                convoyableProvinces.add(allProvinces[i]);
            }
        }
        return convertProvincesToStrings(convoyableProvinces.toArray(), nameLength);
    }

    /**
     * Returns the type of unit (Army, Fleet, or Wing) located at a given province.
     *
     * @param provinceShortName the name of the province to search for
     * @return one of the unit type constants
     * @throws ProvinceNotFoundException thrown if no province can be found for the given name
     * @throws UnitNotFoundException     thrown if no unit is located at the given province
     */
    public int getUnitTypeForProvince(String provinceShortName)
            throws ProvinceNotFoundException, UnitNotFoundException {
        //fetch the province
        Province unitProvince = getProvinceObject(provinceShortName);
        //fetch the unit
        Unit unit = world.getLastTurnState().getPosition().getUnit(unitProvince);
        if (unit == null) {
            throw new UnitNotFoundException("Cannot find unit at province " + provinceShortName);
        }
        //classify the unit type
        Unit.Type unitType = unit.getType();
        if (unitType.equals(Unit.Type.ARMY)) {
            return JdipMapInfo.ARMY_TYPE;
        } else if (unitType.equals(Unit.Type.FLEET)) {
            return JdipMapInfo.FLEET_TYPE;
        } else if (unitType.equals(Unit.Type.WING)) {
            return JdipMapInfo.WING_TYPE;
        } else {
            throw new StateError("Unknown unit type at province " + provinceShortName);
        }
    }

    /**
     * Returns the type of a province (Land, Sea, or Coast).
     *
     * @param provinceShortName the name of the province to search by
     * @return one of the province type constants
     * @throws ProvinceNotFoundException thrown if no province can be found for the given name
     */
    public int getProvinceType(String provinceShortName)
            throws ProvinceNotFoundException {
        Province province = getProvinceObject(provinceShortName);
        if (province.isLand()) {
            return JdipMapInfo.LAND_TYPE;
        } else if (province.isSea()) {
            return JdipMapInfo.SEA_TYPE;
        } else if (province.isCoastal()) {
            return JdipMapInfo.COAST_TYPE;
        } else {
            throw new StateError("Province " + provinceShortName + " is neither land, sea, or coast.");
        }
    }

    /**
     * Converts a province array to a string array of province names
     */
    private String[] convertProvincesToStrings(Object[] provinces, int nameLength) {
        String[] names = new String[provinces.length];
        for (int i = 0; i < provinces.length; i++) {
            names[i] = getProvinceName((Province) (provinces[i]), nameLength);
        }
        return names;
    }

    /**
     * Gets a province's name
     */
    private String getProvinceName(Province province, int nameLength) {
        if (nameLength == JdipMapInfo.LONG_NAMES) {
            return province.getFullName();
        } else {
            return province.getShortName();
        }
    }

    /**
     * Gets a <code>Province</code> for a given name
     */
    private Province getProvinceObject(String provinceShortName)
            throws ProvinceNotFoundException {
        //fetch the province
        Province province = world.getMap().getProvince(provinceShortName);
        if (province == null) {
            throw new ProvinceNotFoundException("Could not find province: " + provinceShortName);
        }
        return province;
    }
}
