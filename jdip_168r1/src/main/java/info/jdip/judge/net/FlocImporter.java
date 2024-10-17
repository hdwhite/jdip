//
//  @(#)FlocImporter.java		9/2003
//
//  Copyright 2003 the jDip development team. All rights reserved.
//  Use is subject to license terms.
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
package info.jdip.judge.net;

import info.jdip.judge.parser.JudgeImport;
import info.jdip.misc.Utils;
import info.jdip.order.OrderFactory;
import info.jdip.world.World;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

/**
 * @author Mannkind aka Dustin Brewer, Zach DelProposto
 * <p>
 * FlocImporter: Imports games from floc.net.
 * Designed as a Runnable so it can be threaded.
 */
public class FlocImporter implements Runnable {
    // i18n constants (for messages)
    private static final String READING_CONTACT = "FlocImporter.message.contact";
    private static final String READING_FROM_NET = "FlocImporter.message.netread";
    private static final String CREATING_WORLD = "FlocImporter.message.createworld";

    // how to tell if game is registered or not.
    private static final String NOT_REGISTERED = "This game is not registered";

    // instance fields
    private final String gameName;
    private final String judgeName;
    private final FlocImportCallback fic;
    private final OrderFactory orderFactory;
    private Thread netThread = null;

    private boolean isInProgress = false;

    /**
     * Create a floc.net importer. Null parameters are not accepted.
     */
    public FlocImporter(String gameName, String judgeName, OrderFactory orderFactory, FlocImportCallback fic) {
        if (gameName == null || judgeName == null || fic == null || orderFactory == null) {
            throw new IllegalArgumentException();
        }

        this.gameName = gameName;
        this.judgeName = judgeName;
        this.orderFactory = orderFactory;
        this.fic = fic;
    }// FlocImporter()


    /**
     * Start a FlocImporter in a new Thread.
     */
    public void start() {
        if (netThread != null) {
            throw new IllegalStateException("thread already started.");
        }

        netThread = new Thread(this);
        netThread.start();
    }// start()


    /**
     * Abort an in-progress FlocImporter that was started
     */
    public void abort() {
        isInProgress = false;
        if (netThread != null) {
            netThread = null;
        }
    }// abort()

    /**
     * Do the work (import text)
     */
    public void run() {
        isInProgress = true;

        try {
            final String text = getGameInfo();

            if (!isInProgress) {
                return;
            }

            // see if game is registered
            if (text.length() == 0 || text.contains(NOT_REGISTERED)) {
                fic.flocImportUnregistered();
                return;
            }

            // game is registered
            if (fic.flocTextImportComplete(text)) {
                // now, process into a World object
                fic.flocImportMessage(Utils.getLocalString(CREATING_WORLD));

                JudgeImport ji = new JudgeImport(orderFactory, new StringReader(text), null);

                if (!isInProgress) {
                    return;
                }

                fic.flocWorldImportComplete(ji.getWorld());
            }
        } catch (IOException e) {
            fic.flocImportException(e);
        } catch (Exception e) {
            // do nothing (catches any thread-abort errors)
        }
    }// run()

    /**
     * Get the game information.
     * <p>
     * Returns the imported text.
     */
    private String getGameInfo()
            throws IOException {
        final StringBuilder gameInformation = new StringBuilder(16384);

        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(
                "http://www.floc.net/observer.py?judge="
                    + judgeName
                    + "&game="
                    + gameName
                    + "&page=history&history_from=0&history_to=999999").openStream()));
        ){
            fic.flocImportMessage(Utils.getLocalString(READING_CONTACT));

            // output is in HTML, so using the HTML editor kit parser removes
            // HTML cruft.
            //
            
            if (!isInProgress) {
                return "";
            }

            ParserDelegator parser = new ParserDelegator();
            parser.parse(reader, new HTMLEditorKit.ParserCallback() {
                @Override
                public void handleText(char[] text, int pos) {
                    if (!isInProgress) {
                        gameInformation.setLength(0);    // abort!
                        return;
                    }

                    fic.flocImportMessage(Utils.getLocalString(READING_FROM_NET));
                    gameInformation.append(text);
                    gameInformation.append("\n");
                }// handleText()
            },
            false);
        }

        return gameInformation.toString();
    }// getGameInfo()


    /**
     * This is the interface which defines the callback
     */
    public interface FlocImportCallback {
        /**
         * An exception (given as an argument) occured during import.
         */
        void flocImportException(IOException e);

        /**
         * Floc Text import completed successfully.
         * String contains imported text, and should never be null.
         * <p>
         * Note: return <code>true</code> if processor should continue on
         * to convert text into a World object; return <code>false</code> if
         * FlocImport is complete and automatic World object creation is
         * not required.
         */
        boolean flocTextImportComplete(String text);

        /**
         * Floc World import completed successfully. World should never be null.
         * <p>
         * Note: this method only excecutes if flocTextImportComplete() returns
         * <code>true</code>
         */
        void flocWorldImportComplete(World world);


        /**
         * Floc import progress message (displays import progress)
         */
        void flocImportMessage(String message);


        /**
         * Import failed; the game is not registered.
         */
        void flocImportUnregistered();
    }// interface FlocImportCallback

}// class FlocImporter
