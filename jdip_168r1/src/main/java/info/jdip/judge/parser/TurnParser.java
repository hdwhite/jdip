//
//  @(#)TurnParser.java	1.00	6/2002
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parses the Turns of a History file.
 * <p>
 */
public class TurnParser {
    /**
     * Pattern for matching the phase in the Subject: line<br>
     * xDDDDx pattern. Entire group is used.
     */
    public static final String SUBJ_PHASE_REGEX = "\\s+\\p{Alpha}\\d{4}\\p{Alpha}\\s+";
    /**
     * Pattern for matching the phase in the Subject: line<br>
     * xDDDDx pattern. Entire group is used.
     * For older versions of nJudge
     */
    public static final String SUBJ_PHASE_REGEX_OLD = "\\s+\\p{Alpha}\\d{4}\\p{Alpha}\\s*$";
    /**
     * This text (or pattern) must be present in the Subject: line. If it is not present,
     * the turn is ignored.
     */
    public static final String RESULT_SUBJ_REGEX = "(?i)results";
    // il8n constants
    private static final String TP_NO_SUBJECT = "JP.turn.nosubject";
    private static final String TP_BAD_PHASE = "JP.turn.badphase";
    private static final String TP_BAD_SUBJECT = "JP.turn.badsubject";
    // instance variables
    private Turn[] turns = null;


    /**
     * Create the TurnParser and perform parsing.
     */
    public TurnParser(String input)
            throws IOException, PatternSyntaxException {
        parseTurns(input);
    }// TurnParser()


    /**
     * Returns the turns. If not parsed, or an error occured, it may return null.
     */
    public Turn[] getTurns() {
        return turns;
    }// getTurns()


    /**
     * Creates Turn objects.
     */
    private void parseTurns(String input)
            throws IOException, PatternSyntaxException {
        // patterns
        Pattern subjPhasePattern = Pattern.compile(SUBJ_PHASE_REGEX);
        Pattern subjPhasePatternOld = Pattern.compile(SUBJ_PHASE_REGEX_OLD);
        Pattern isResultsPattern = Pattern.compile(RESULT_SUBJ_REGEX);


        LinkedList<Turn> turnList = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new StringReader(input));

        String line = reader.readLine();
        StringBuilder sb = null;
        Turn turn = new Turn();            // current turn
        Turn lastTurn = null;            // previous turn

        while (line != null) {
            int pos = line.toLowerCase().indexOf("date:");
            if (pos >= 0 && pos < 10) {
                turn.setDateLine(line);

                // set the subject line; if not present, throw an error (shouldn't occur)
                String nextLine = reader.readLine();
                if (!nextLine.toLowerCase().contains("subject:")) {
                    throw new IOException(Utils.getLocalString(TP_NO_SUBJECT));
                } else if (isResultsPattern.matcher(nextLine).find()) {
                    turn.setSubjectLine(nextLine);

                    // regex parse the subject line
                    Matcher m = subjPhasePattern.matcher(nextLine);
                    Matcher m_o = subjPhasePatternOld.matcher(nextLine);
                    if (m.find()) {
                        Phase phase = Phase.parse(m.group(0).trim());
                        if (phase == null) {
                            throw new IOException(Utils.getLocalString(TP_BAD_PHASE, m.group(0).trim()));
                        }

                        turn.setPhase(phase);
                    } else if (m_o.find()) {
                        Phase phase = Phase.parse(m_o.group(0).trim());
                        if (phase == null) {
                            throw new IOException(Utils.getLocalString(TP_BAD_PHASE, m_o.group(0).trim()));
                        }

                        turn.setPhase(phase);
                    } else {
                        throw new IOException(Utils.getLocalString(TP_BAD_SUBJECT, nextLine));
                    }

                    if (lastTurn != null) {
                        lastTurn.setText(sb.toString());
                        turnList.add(lastTurn);
                        lastTurn = null;
                    }

                    lastTurn = turn;
                    turn = new Turn();
                    sb = new StringBuilder(512);
                }
            } else if (sb != null) {
                // accumulate text
                sb.append(line);
                sb.append('\n');
            }

            line = reader.readLine();
        }

        // add last turn
        if (lastTurn != null) {
            lastTurn.setText(sb.toString());
            turnList.add(lastTurn);
        }

        // convert to array
        turns = turnList.toArray(new Turn[turnList.size()]);
    }// parseTurns()


    /**
     * A Turn object is created for each Turn detected in the History file.
     */
    public static class Turn {
        private String dateLine;
        private String subjectLine;
        private String text;
        private Phase phase;

        public Turn() {
        }

        /**
         * Returns the unparsed Date: line
         */
        public String getDateLine() {
            return dateLine;
        }

        /**
         * Sets the unparsed Date: line
         */
        public void setDateLine(String value) {
            dateLine = value;
        }

        /**
         * Returns the unparsed Subject: line
         */
        public String getSubjectLine() {
            return subjectLine;
        }

        /**
         * Sets the unparsed Subject: line
         */
        public void setSubjectLine(String value) {
            subjectLine = value;
        }

        /**
         * Returns the PhaseType, or null if it cannot be detected.
         */
        public Phase getPhase() {
            return phase;
        }

        /**
         * Sets the PhaseType
         */
        public void setPhase(Phase value) {
            phase = value;
        }

        /**
         * Returns the text between Subject: upto (but not including) the next Date: line
         */
        public String getText() {
            return text;
        }

        /**
         * Sets the text between Subject: and upto (but not including) the next Date: line
         */
        public void setText(String value) {
            text = value;
        }


    }// nested class Turn

}// class TurnParser
