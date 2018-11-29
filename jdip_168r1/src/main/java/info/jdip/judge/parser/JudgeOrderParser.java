//
//  @(#)JudgeOrderParser.java	1.00	6/2002
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

import info.jdip.order.NJudgeOrderParser;
import info.jdip.order.NJudgeOrderParser.NJudgeOrder;
import info.jdip.order.OrderException;
import info.jdip.order.OrderFactory;
import info.jdip.world.Phase.PhaseType;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Parses Move, Retreat, and Adjustment phase orders.
 * <p>
 * This assumes that orders never span lines.
 */
public class JudgeOrderParser {
    private static final Logger logger = getLogger(JudgeOrderParser.class);

    /**
     * Parse Season and Year for these orders
     */
    public static final String SEASON_YEAR_REGEX = "(([0-9]+))"; // "(\\S+)\\s+of\\s+(([0-9]+))";
    /**
     * Header for recognizing the start of game block
     */
    public static final String GAME_STARTING_HEADER = "(?i)you\\s+have\\s+been\\s+selected\\s+as";
    /**
     * Recognize the starting position header
     */
    public static final String STARTING_POSITION_REGEX = "Starting\\s+position\\s+for\\s+(\\p{Alpha}+)\\s+of\\s+((\\p{Digit}+))";


    // regular expressions
    /**
     * Header for recognizing the movement order block
     */
    public static final String MOVE_ORDER_HEADER = "(?i)movement\\s+results\\s+for";
    /**
     * Header for recognizing the retreat order block.<br>
     * Note that older nJudge versions use "retreat orders for" while newer
     * versions use "retreat results for".
     */
    public static final String RETREAT_ORDER_HEADER = "(?i)retreat\\s+(?:orders|results)\\s+for";
    /**
     * Header for recognizing the adjustment order block.<br>
     * Note that older nJudge versions use "adjustment orders for" while newer
     * versions use "adjustment results for".
     */
    public static final String ADJUSTMENT_ORDER_HEADER = "(?i)adjustment\\s+(?:results|orders)\\s+for";
    // i18n
    private static final String WAIVED_BUILDS = "JOP.adjust.waived";
    private static final String UNUSABLE_WAIVED = "JOP.adjust.unusable.waived";
    private static final String UNUSABLE_PENDING = "JOP.adjust.unusable.pending";
    /**
     * Recognize an order line; all orders must begin with this.
     */
    private static final String ORDER_PREFIX = "^\\s*[\\p{Alnum}\\-\\_]+:\\s+";
    private final info.jdip.world.Map map;
    private final NJudgeOrderParser parser;
    private final OrderFactory orderFactory;
    // instance variables
    private NJudgeOrder[] nJudgeOrders = null;
    private PhaseType phaseType = null;

    /**
     * Create a JudgeOrderParser
     */
    public JudgeOrderParser(final info.jdip.world.Map map,
                            final OrderFactory orderFactory, final String input)
            throws IOException, PatternSyntaxException {
        this.map = map;
        this.parser = new NJudgeOrderParser();
        this.orderFactory = orderFactory;
        parseInput(input);
    }// JudgeOrderParser


    /**
     * Returns the phase of the processed orders. This is null when getOrderInfo() is zero-length.
     */
    public PhaseType getPhaseType() {
        return phaseType;
    }// getPhaseType()

    /**
     * Returns the NJudgeOrder(s) after parsing. This is never null, but may be a zero-length array.
     */
    public NJudgeOrder[] getNJudgeOrders() {
        return nJudgeOrders;
    }// getNJudgeOrders()


    /**
     * Looks for the header line. When the appropriate phase header is found, this method
     * sets the PhaseType and calls the phase-specific parser. This analyzes input line-by-line.
     */
    private void parseInput(String input)
            throws IOException, PatternSyntaxException {
        // search for header input. once found, shuttle all input to the appropriate
        // handler type.
        Pattern hm = Pattern.compile(MOVE_ORDER_HEADER);
        Pattern hr = Pattern.compile(RETREAT_ORDER_HEADER);
        Pattern ha = Pattern.compile(ADJUSTMENT_ORDER_HEADER);

        // create List
        List<NJudgeOrder> orderList = new ArrayList<>(64);

        BufferedReader br = new BufferedReader(new StringReader(input));
        String line = ParserUtils.getNextLongLine(br);
        while (line != null) {
            Matcher m = hm.matcher(line);
            if (m.lookingAt()) {
                phaseType = PhaseType.MOVEMENT;
                break;
            }

            m = hr.matcher(line);
            if (m.lookingAt()) {
                phaseType = PhaseType.RETREAT;
                break;
            }

            m = ha.matcher(line);
            if (m.lookingAt()) {
                phaseType = PhaseType.ADJUSTMENT;
                break;
            }

            line = ParserUtils.getNextLongLine(br);
        }

        // parse based on type
        parseOrders(br, phaseType, orderList);

        // cleanup
        br.close();

        // create array
        nJudgeOrders = orderList.toArray(new NJudgeOrder[orderList.size()]);
    }// parseInput()


    /**
     * Parse move and retreat orders
     */
    private void parseOrders(BufferedReader br,
                             PhaseType phaseType, List<NJudgeOrder> orderList)
            throws IOException, PatternSyntaxException {
        final Pattern prefix = Pattern.compile(ORDER_PREFIX);

        String line = ParserUtils.getNextLongLine(br).trim();

        try {
            while (line != null) {
                // only parse lines starting with ORDER_PREFIX
                Matcher m = prefix.matcher(line);
                if (m.lookingAt()) {
                    orderList.add(parser.parse(map, orderFactory, phaseType, line));
                } else {
                    logger.debug("stopped at line: {}", line);
                    break;
                }

                line = ParserUtils.getNextLongLine(br);
            }
        } catch (OrderException oe) {
            throw new IOException(oe.getMessage(), oe);
        }
    }// moveAndRetreatParser()

}// class JudgeOrderParser
