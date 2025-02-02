//
//  @(#)FlocImportDialog.java	9/2003
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

package info.jdip.judge.gui;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import info.jdip.gui.ClientFrame;
import info.jdip.gui.dialog.ErrorDialog;
import info.jdip.gui.dialog.HeaderDialog;
import info.jdip.judge.net.FlocImporter;
import info.jdip.judge.net.FlocImporter.FlocImportCallback;
import info.jdip.misc.Help;
import info.jdip.misc.SharedPrefs;
import info.jdip.misc.Utils;
import info.jdip.world.World;

import javax.swing.*;
import java.io.IOException;
import java.util.prefs.Preferences;


/**
 * Dialog for importing a game from floc.net
 */
public class FlocImportDialog extends HeaderDialog implements FlocImportCallback {
    // i18n constants
    private static final String TITLE = "FlocImportDialog.title";
    private static final String HEADER_LOCATION = "FlocImportDialog.header.location";
    private static final String LABEL_JUDGES = "FlocImportDialog.label.judges";
    private static final String LABEL_GAMENAME = "FlocImportDialog.label.gamename";
    private static final String NOT_REGISTERED_TEXT = "FlocImportDialog.notregistered.text";
    private static final String NOT_REGISTERED_TITLE = "FlocImportDialog.notregistered.title";
    private static final String INVALID_INPUT_TEXT = "FlocImportDialog.badname.text";
    private static final String INVALID_JUDGE_TEXT = "FlocImportDialog.badname.judge.text";


    // pref constants
    private static final String PREFS_LAST_JUDGE_USED = "FlocImport.judge.last";

    // common constants
    private static final String JUDGE_NAMES = "JUDGE_NAMES";

    // misc constants
    private static final int BORDER = 25;

    // vars
    private final ClientFrame clientFrame;
    private World world = null;
    private final JComboBox<String> cbJudges;
    private final JTextField tfGameName;
    private JProgressBar progressBar = null;
    private FlocImporter fi = null;

    /**
     * Create the dialog.
     */
    private FlocImportDialog(ClientFrame parent) {
        super(parent, Utils.getLocalString(TITLE), true);
        this.clientFrame = parent;

        // get last judge used (unless empty; if so, ignore)
        Preferences prefs = SharedPrefs.getUserNode();
        final String lastJudgeUsed = prefs.get(PREFS_LAST_JUDGE_USED, "");

        // component creation
        tfGameName = Utils.createWordTextField(25);
        cbJudges = new JComboBox<>(Utils.getCommonStringArray(JUDGE_NAMES));
        cbJudges.setPrototypeDisplayValue("MMMMM");    // wide enough for any 4-letter judge
        cbJudges.setEditable(true);
        cbJudges.setSelectedIndex(0);    // default, if setSelectedItem() fails
        cbJudges.setSelectedItem(lastJudgeUsed);

        progressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        enableProgressBar(false);

        // dialog setup
        setHeaderText(Utils.getText(Utils.getLocalString(HEADER_LOCATION)));
        setContentPane(makeContent());
        addToButtonPanel(progressBar);    // add first (leftmost)
        addTwoButtons(makeCancelButton(), makeOKButton(), false, true);
        setSeparatorVisible(true, BTN_BAR_EDGE, 0);
        setHelpID(Help.HelpID.Dialog_ImportFloc);
    }// FlocImportDialog()

    /**
     * Display the dialog. Returns the succesfully imported
     * World object, or null if failure occured.
     */
    public static World displayDialog(ClientFrame parent) {
        final FlocImportDialog fid = new FlocImportDialog(parent);
        fid.pack();
        fid.setSize(Utils.getScreenSize(0.4f, 0.5f));
        Utils.centerInScreen(fid);
        fid.setVisible(true);
        return fid.getWorld();
    }// displayDialog()

    /**
     * Return the World object
     */
    private World getWorld() {
        return world;
    }

    /**
     * Returns a JPanel with the dialog layout.
     */
    private JPanel makeContent() {

        int[] w1 = {BORDER, 0, 5, 0, BORDER};
        int[] h1 = {BORDER, 0, 20, 0, 0, BORDER, 0};

        HIGLayout l1 = new HIGLayout(w1, h1);
        l1.setColumnWeight(1, 1);
        l1.setRowWeight(1, 2);
        l1.setColumnWeight(5, 1);
        l1.setRowWeight(5, 3);

        JPanel content = new JPanel(l1);

        HIGConstraints c = new HIGConstraints();
        content.add(new JLabel(Utils.getLocalString(LABEL_JUDGES)), c.rcwh(2, 2, 1, 1, "r"));
        content.add(cbJudges, c.rcwh(2, 4, 1, 1, "l"));
        content.add(new JLabel(Utils.getLocalString(LABEL_GAMENAME)), c.rcwh(4, 2, 1, 1, "r"));
        content.add(tfGameName, c.rcwh(4, 4, 1, 1, "lrtb"));

        return content;
    }// makeContent()


    /**
     * Handle OK and CANCEL selections
     */
    @Override
    public void close(String actionCommand) {
        if (isCloseOrCancel(actionCommand) && fi != null) {
            fi.abort();
            fi = null;
            clientFrame.getStatusBar().clearText();
            super.close(actionCommand);
            return;
        }

        if (!isOKorAccept(actionCommand)) 
        {
            clientFrame.getStatusBar().clearText();
            super.close(actionCommand);
            return;
        }
        // check game name text
        final String judgeName = ((String) cbJudges.getSelectedItem()).trim().toUpperCase();
        final String gameName = tfGameName.getText().trim();

        boolean invalidJudgeName = (judgeName == null || judgeName.length() != 4);
        if (!invalidJudgeName) {
            for (int i = 0; i < judgeName.length(); i++) {
                if (!Character.isLetterOrDigit(judgeName.charAt(i))) {
                    invalidJudgeName = true;
                }
            }
        }

        if (invalidJudgeName) {
            Utils.popupError(clientFrame,
                    Utils.getLocalString(NOT_REGISTERED_TITLE),
                    Utils.getLocalString(INVALID_JUDGE_TEXT));
            return;
        }

        if (gameName.length() == 0) {
            Utils.popupError(clientFrame,
                    Utils.getLocalString(NOT_REGISTERED_TITLE),
                    Utils.getLocalString(INVALID_INPUT_TEXT));
            return;
        }

        for (int i = 0; i < gameName.length(); i++) {
            if (!Character.isLetterOrDigit(gameName.charAt(i)) && (gameName.charAt(i) != '_')) {
                Utils.popupError(clientFrame,
                        Utils.getLocalString(NOT_REGISTERED_TITLE),
                        Utils.getLocalString(INVALID_INPUT_TEXT));
                return;
            }
        }

        // save selected judge
        Preferences prefs = SharedPrefs.getUserNode();
        prefs.put(PREFS_LAST_JUDGE_USED, judgeName);
        SharedPrefs.savePrefs(prefs);

        // disable 'ok', show progress
        getButton(0).setEnabled(false);
        enableProgressBar(true);

        // import!
        fi = new FlocImporter(
                gameName,
                judgeName,
                clientFrame.getGUIOrderFactory(),
                this);

        fi.start();
    }// close()


    /**
     * FlocImportCallback implementation
     */
    public void flocImportException(IOException e) {
        // enhance our error report
        ErrorDialog.BugReportInfo bri = new ErrorDialog.BugReportInfo(e);
        bri.add("contact_site", "http://www.floc.net/");
        bri.add("judge_name", (String) cbJudges.getSelectedItem());
        bri.add("game_name", tfGameName.getText());
        bri.add("world_open?", String.valueOf((clientFrame.getWorld() != null)));

        ErrorDialog.displayNetIO(clientFrame, "www.floc.net", bri);
        enableProgressBar(false);
        close(ACTION_CANCEL);
    }// flocImportException()


    /**
     * FlocImportCallback implementation
     */
    public boolean flocTextImportComplete(String text) {
        // we don't do anything with the raw text.
        // and don't close the dialog, either
        return true;
    }// flocImportComplete()

    /**
     * FlocImportCallback implementation
     */
    public void flocWorldImportComplete(World w) {
        this.world = w;
        enableProgressBar(false);
        close(ACTION_CANCEL);
    }// flocImportComplete()

    /**
     * FlocImportCallback implementation
     */
    public void flocImportMessage(String message) {
        clientFrame.getStatusBar().setText(message);
    }// flocImportMessage()

    /**
     * FlocImportCallback implementation
     */
    public void flocImportUnregistered() {
        enableProgressBar(false);

        Utils.popupError(clientFrame,
                Utils.getLocalString(NOT_REGISTERED_TITLE),
                Utils.getLocalString(NOT_REGISTERED_TEXT,
                        tfGameName.getText().trim(),
                        cbJudges.getSelectedItem()));

        clientFrame.getStatusBar().clearText();

        // do not close the dialog! allow the user to try again.
        // re-enable the button
        getButton(0).setEnabled(true);
    }// flocImportUnregistered()

    private synchronized void enableProgressBar(boolean value) {
        progressBar.setVisible(value);
        progressBar.setString("");
    }// enableProgressBar()


}// class FlocImportDialog

