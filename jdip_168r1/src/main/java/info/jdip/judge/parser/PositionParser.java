//
//  @(#)PositionParser.java	1.00	6/2002
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
package info.jdip.judge.parser;

import info.jdip.misc.Utils;
import info.jdip.world.Phase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parses static position list, if present. This is in game listings and also
 * sometimes in history files (if from the start).
 * <p>
 */
public class PositionParser {
    /**
     * Header phrase 1<br>
     * Capture groups: 1:season 2:year <br>
     * PhaseType is assumed to be movement.
     */
    public static final String HEADER_REGEX_1 = "(?i)^starting\\s+position\\s+for\\s+([\\p{Alnum}]+)\\s+of\\s+((\\d+))\\.";
    /**
     * Header phrase 2<br>
     * Capture groups: 1:PhaseType 2:SeasonType 3:year<br>
     */
    public static final String HEADER_REGEX_2 = "(?i)^status\\s+of\\s+the\\s+([\\p{Alnum}]+)\\s+phase\\s+for\\s+([\\p{Alnum}]+)\\s+of\\s+(\\d+)\\.";
    /**
     * Capture groups: 1:power, 2:unit, 3:province<br>
     * Note: a space is not always present between the colon and the unit type.
     */
    public static final String PARSE_REGEX = "^([\\p{Alnum}\\-\\_]+):\\s*([\\p{Alnum}\\-\\_]+)\\s+(([^\\.]+))\\.";
    // il8n constants
    private static final String PP_UNKNOWN_PHASE = "JP.posparser.badphase";
    // instance variables
    private PositionInfo[] posInfo = null;
    private Phase phase = null;
	
    /**
     * Parses the input for Position information, if any is present.
     */
    public PositionParser(String input)
            throws IOException, PatternSyntaxException {
        parseInput(input);
    }// PositionParser()


    /**
     * Returns PositionInfo, or a zero-length array
     */
    public PositionInfo[] getPositionInfo() {
        return posInfo;
    }// getPositionInfo()

    /**
     * Returns the Phase; this will be null if getPositionInfo() is a zero-length array
     */
    public Phase getPhase() {
        return phase;
    }// getPhase()

    /**
     * Parses the input; looks for the appropriate header. Then parses the position list
     * until no more positions can be found.
     */
    private void parseInput(String input)
            throws IOException, PatternSyntaxException {
        // search for header input. once found, shuttle all input to the appropriate
        // handler type.
        Pattern pp1 = Pattern.compile(HEADER_REGEX_1);
        Pattern pp2 = Pattern.compile(HEADER_REGEX_2);

        // init
        List<PositionInfo> posList = new LinkedList<>();
        BufferedReader br = new BufferedReader(new StringReader(input));

        // header parse loop
        String line = ParserUtils.getNextLongLine(br);
        while (line != null) {
            Matcher m = pp1.matcher(line);
            if (m.lookingAt()) {
                phase = makePhase(null, m.group(1), m.group(2));
                parsePositions(br, posList);
                break;
            }

            m = pp2.matcher(line);
            if (m.lookingAt()) {
                phase = makePhase(m.group(1), m.group(2), m.group(3));
                parsePositions(br, posList);
                break;
            }

            line = ParserUtils.getNextLongLine(br);
        }

        // cleanup & create array
        br.close();
        posInfo = posList.toArray(new PositionInfo[posList.size()]);
    }// parseInput()

    /**
     * Parses the positions.
     */
    private void parsePositions(BufferedReader br, List<PositionInfo> posList)
            throws IOException, PatternSyntaxException {
        Pattern mrp = Pattern.compile(PARSE_REGEX);

        String line = ParserUtils.getNextLongLine(br);
        while (line != null) {
            Matcher m = mrp.matcher(line);
            if (m.find()) {
                posList.add(new PositionInfo(m.group(1), m.group(2), ParserUtils.filter(m.group(3))));
            } else {
                // parse failed; break out of loop
                break;
            }

            line = ParserUtils.getNextLongLine(br);
        }
    }// parsePositions()

    /**
     * Makes the phase; throws an exception if we cannot.
     */
    private Phase makePhase(String phaseType, String seasonType, String year)
            throws IOException {
        phaseType = (phaseType == null) ? "Movement" : phaseType;

        StringBuilder sb = new StringBuilder();
        sb.append(phaseType);
        sb.append(' ');
        sb.append(seasonType);
        sb.append(' ');
        sb.append(year);

        try {
            return Phase.parse(sb.toString());
        } catch (Exception e) {
            throw new IOException(Utils.getLocalString(PP_UNKNOWN_PHASE, sb.toString()));
        }
    }// makePhase()

    /**
     * Details the position information
     */
    public static class PositionInfo {
        private final String power;
        private final String unit;
        private final String location;

        /**
         * Creates a PositionInfo object
         */
        public PositionInfo(String power, String unit, String location) {
            this.power = power;
            this.unit = unit;
            this.location = location;
        }// PositionInfo()

        /**
         * Gets the Power
         */
        public String getPowerName() {
            return power;
        }

        /**
         * Gets the Unit
         */
        public String getUnitName() {
            return unit;
        }

        /**
         * Gets the location name
         */
        public String getLocationName() {
            return location;
        }

        /**
         * For debugging only; this may change between versions.
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PositionInfo[power=");
            sb.append(power);
            sb.append(",unit=");
            sb.append(unit);
            sb.append(",location=");
            sb.append(location);
            sb.append(']');
            return sb.toString();
        }// toString()
    }// nested class PositionInfo

}// class PositionParser
