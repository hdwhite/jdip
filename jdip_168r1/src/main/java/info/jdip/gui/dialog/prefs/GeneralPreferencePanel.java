//
//  @(#)GeneralPreferencePanel.java		4/2002
//
//  Copyright 2002-2004 Zachary DelProposto. All rights reserved.
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

package info.jdip.gui.dialog.prefs;

import com.l2fprod.common.swing.JDirectoryChooser;
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import info.jdip.gui.ClientFrame;
import info.jdip.gui.OrderDisplayPanel;
import info.jdip.gui.map.MapRenderer2;
import info.jdip.gui.swing.AssocJComboBox;
import info.jdip.gui.swing.XJFileChooser;
import info.jdip.misc.LRUCache;
import info.jdip.misc.SharedPrefs;
import info.jdip.misc.Utils;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * General preferences.
 * <p>
 * Static methods are included to access (in a controlled manner) preference-controlled
 * functionality.
 */
public class GeneralPreferencePanel extends PreferencePanel {
    private static final Logger logger = getLogger(GeneralPreferencePanel.class);

    // constants
    public static final int BORDER = 10;

    // preference nodes keys
    public static final String NODE_DEFAULT_GAME_DIR = "defaultGameDir";
    public static final String NODE_SHOW_RESOLUTION_RESULTS = "showResolutionResults";

    // recent files (w/o #) [if enabled]
    public static final String NODE_RECENT_FILE = "lastFile";

    // window settings [if enabled]
    public static final String NODE_WINDOW_X = "windowX";
    public static final String NODE_WINDOW_Y = "windowY";
    public static final String NODE_WINDOW_WIDTH = "windowWidth";
    public static final String NODE_WINDOW_HEIGHT = "windowHeight";

    // order sort preference
    public static final String NODE_ORDER_SORTING = "orders.list.sorting";
    public static final String NODE_ORDER_SORTING_REVERSE = "orders.list.sorting.reversed";    // boolean

    // map label preference
    public static final String NODE_MAP_LABEL_LEVEL = "map.label.level";

    // variant dir
    public static final String NODE_VARIANT_DIR = "variant.dir";

    // # of recent files to save
    private static final int NUM_RECENT_FILES = 5;
    // UI text i18n constants
    private static final String TAB_NAME = "GPP.tabname";
    private static final String GPP_SAVE_DIR_TEXT = "GPP.save_dir_text";
    private static final String GPP_SAVE_DIR_BUTTON = "GPP.save_dir_button";
    private static final String GPP_SHOW_RESOLUTION_RESULTS = "GPP.show_resolution_results";
    private static final String GPP_CLEAR_MRU_BUTTON = "GPP.clearmru.button";
    private static final String GPP_CLEAR_MRU_TEXT = "GPP.clearmru.text";
    private static final String GPP_MAP_LABEL_PREFIX = "GPP.map.label.level.";
    private static final String GPP_ORDER_SORT_PREFIX = "GPP.order.sort.type.";
    private static final String GPP_MAP_LABEL = "GPP.map.label.text";
    private static final String GPP_MAP_LABEL_NOTE = "GPP.map.label.text.note";
    private static final String GPP_ORDER_SORT_LABEL = "GPP.order.sort.label";
    private static final String GPP_ORDER_SORT_DIRECTION = "GPP.order.sort.direction";
    private static final String DIALOG_TITLE = "GPP.filedialog.title";
    private static final String SELECT_VARIANT_DIALOG_TITLE = "GPP.selectvariantdir.title";
    // LRU cache of files
    private static LRUCache<String, File> fileCache = null;
    private static Collator collator = null;
    // UI Elements
    private final JTextField saveDir;                        // default save-files directory
    private final JButton browseSaveDir;                    // browse button for setting directory
    private final JButton clearMRU;                        // clears most-recently-used file list
    private final JCheckBox reverseSort;    // reverse the order sort direction
    private final AssocJComboBox orderSorting;    // order sorting type
    private final AssocJComboBox mapLabels;        // map label level
    private final JCheckBox showResolution;
    private ClientFrame cf = null;


    public GeneralPreferencePanel(final ClientFrame cf) {
        this.cf = cf;

        // create UI elements
        showResolution = new JCheckBox(Utils.getLocalString(GPP_SHOW_RESOLUTION_RESULTS));

        saveDir = new JTextField();
        saveDir.setEditable(false);
        browseSaveDir = new JButton(Utils.getLocalString(GPP_SAVE_DIR_BUTTON));
        browseSaveDir.addActionListener((ActionEvent e) -> directoryBrowse());


        clearMRU = new JButton(Utils.getLocalString(GPP_CLEAR_MRU_BUTTON));
        clearMRU.addActionListener((ActionEvent e) -> {
            clearFileList();
            cf.getClientMenu().updateRecentFiles();
        });

        // setup associative arrays for comboboxes
        String[] arr = new String[]{
                MapRenderer2.VALUE_LABELS_NONE,
                MapRenderer2.VALUE_LABELS_BRIEF,
                MapRenderer2.VALUE_LABELS_FULL
        };

        AssocJComboBox.AssociatedObj[] assocObjs =
                AssocJComboBox.AssociatedObj.createAssociatedObjects(arr,
                        GPP_MAP_LABEL_PREFIX, MapRenderer2.VALUE_LABELS_NONE, true);
        mapLabels = new AssocJComboBox(assocObjs);


        // order sorting :: implement
        arr = new String[]{
                OrderDisplayPanel.SORT_POWER, OrderDisplayPanel.SORT_PROVINCE,
                OrderDisplayPanel.SORT_UNIT, OrderDisplayPanel.SORT_ORDER
        };

        String[] arr2 = new String[]{
                Utils.getLocalString(OrderDisplayPanel.LABEL_SORT_POWER),
                Utils.getLocalString(OrderDisplayPanel.LABEL_SORT_PROVINCE),
                Utils.getLocalString(OrderDisplayPanel.LABEL_SORT_UNIT),
                Utils.getLocalString(OrderDisplayPanel.LABEL_SORT_ORDER)
        };

        assocObjs = AssocJComboBox.AssociatedObj.createAssociatedObjects(arr,
                arr2, OrderDisplayPanel.SORT_POWER, true);
        orderSorting = new AssocJComboBox(assocObjs);

        reverseSort = new JCheckBox(Utils.getLocalString(GPP_ORDER_SORT_DIRECTION));

        // update components
        getSettings();

        // mini-panels
        JPanel clrPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clrPanel.add(new JLabel(Utils.getLocalString(GPP_CLEAR_MRU_TEXT)));
        clrPanel.add(Box.createHorizontalStrut(5));
        clrPanel.add(clearMRU);

        JPanel orderP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        orderP.add(new JLabel(Utils.getLocalString(GPP_ORDER_SORT_LABEL)));
        orderP.add(Box.createHorizontalStrut(5));
        orderP.add(orderSorting);
        orderP.add(Box.createHorizontalStrut(10));
        orderP.add(reverseSort);

        JPanel mapP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        mapP.add(new JLabel(Utils.getLocalString(GPP_MAP_LABEL)));
        mapP.add(Box.createHorizontalStrut(5));
        mapP.add(mapLabels);

        // layout
        int[] h1 = {BORDER, 0, 8, 0, 8, 0, 8, 0, 3, 0, 7, 36,    // upto row 12
                0, 3, 0, 10, 0, 8, 0, BORDER};
        int[] w1 = {BORDER, 10, 0, 5, 0, BORDER};

        HIGLayout l1 = new HIGLayout(w1, h1);
        l1.setColumnWeight(3, 1);
        l1.setRowWeight(19, 1);
        setLayout(l1);


        HIGConstraints c = new HIGConstraints();
        add(showResolution, c.rcwh(4, 2, 4, 1, "l"));

        add(orderP, c.rcwh(6, 2, 4, 1, "l"));

        add(mapP, c.rcwh(8, 2, 4, 1, "l"));
        add(new JLabel(Utils.getLocalString(GPP_MAP_LABEL_NOTE)),
                c.rcwh(10, 3, 3, 1, "ltb"));

        // separator bar (12)
        add(new JSeparator(), c.rcwh(12, 2, 4, 1, "lr"));

        // save dir
        add(new JLabel(Utils.getLocalString(GPP_SAVE_DIR_TEXT)),
                c.rcwh(13, 2, 4, 1, "l"));
        add(saveDir, c.rcwh(15, 3, 1, 1, "lr"));
        add(browseSaveDir, c.rcwh(15, 5, 1, 1, "lrtb"));

        // clear MRU
        add(clrPanel, c.rcwh(17, 2, 4, 1, "l"));

    }// GeneralPreferencePanel()

    /**
     * Get the Map label-level setting, as set by the User in
     * Preferences. Never returns null.
     */
    public static String getMapLabelSetting() {
        Preferences prefs = SharedPrefs.getUserNode();
        final String mlSetting = MapRenderer2.parseLabelValue(prefs.get(NODE_MAP_LABEL_LEVEL,
                null), MapRenderer2.VALUE_LABELS_NONE);
        assert (mlSetting != null);
        return mlSetting;
    }// getMapLabelSetting()

    /**
     * Get the order-sorting direction, as set by the user.
     * Returns <code>true</code> if sort direction is reversed.
     */
    public static boolean getOrderSortReverse() {
        Preferences prefs = SharedPrefs.getUserNode();
        return prefs.getBoolean(NODE_ORDER_SORTING_REVERSE, false);
    }// getOrderSortReverse()

    /**
     * Get the order-sorting mode, as set by the user.
     * Never returns null.
     */
    public static String getOrderSortMode() {
        Preferences prefs = SharedPrefs.getUserNode();
        return OrderDisplayPanel.parseSortValue(
                prefs.get(NODE_ORDER_SORTING, null), OrderDisplayPanel.SORT_PROVINCE);
    }// getOrderSortMode()

    // sort before returning; case insensitive
    // CALL THIS the first time
    public static String[] getRecentFileNamesFromPrefs() {
        if (fileCache == null) {
            // load from preferences
            Preferences prefs = SharedPrefs.getUserNode();

            // get files
            ArrayList<File> al = new ArrayList<>(NUM_RECENT_FILES);
            for (int i = 0; i < NUM_RECENT_FILES; i++) {
                String s = prefs.get(NODE_RECENT_FILE + String.valueOf(i), "");
                if (s != null && s.length() > 0) {
                    // do NOT add file if it doesn't exist.
                    File file = new File(s);
                    if (file.exists()) {
                        al.add(file);
                    }
                }
            }

            // add to cache & create a String of just the name
            fileCache = new LRUCache<>(NUM_RECENT_FILES);
            String[] s = new String[al.size()];
            for (int i = 0; i < al.size(); i++) {
                File file = al.get(i);
                fileCache.put(file.getName(), file);
                s[i] = file.getName();
            }

            // sort [note; should use CollationKey if sorting more items...]
            if (collator == null) {
                collator = Collator.getInstance();
                collator.setStrength(Collator.PRIMARY);        // ignore case
            }
            Arrays.sort(s, collator);
            return s;
        }

        return new String[0];
    }// getRecentFileNamesFromPrefs()

    // this may return null!!
    public static File getFileFromName(String name) {
        if (fileCache != null) {
            return fileCache.get(name);
        }
        return null;
    }// getFileFromName()

    // CALL THIS most of the time; use to update the names for the menu
    public static String[] getRecentFileNamesFromCache() {
        if (fileCache != null) {
            // load from cache; sort and check for file existence before returning
            ArrayList<String> names = new ArrayList<>(NUM_RECENT_FILES);
            for (Map.Entry<String, File> mapEntry : fileCache.entrySet()) {
                File file = mapEntry.getValue();
                if (file.exists()) {
                    names.add(mapEntry.getKey());
                }
            }

            String[] fileNames = names.toArray(new String[names.size()]);
            Arrays.sort(fileNames, collator);
            return fileNames;
        }


        return new String[0];
    }// getRecentFilesNames()

    /**
     * This method updates the recent file name preferences.
     * <p>
     * It should be called every time a file is opened, and
     * every time "Save As" is called.
     * <p>
     * Don't forget to update menus as well, after calling this
     * method.
     */
    public static void setRecentFileName(File file) {
        if (fileCache != null) {
            String name = file.getName();
            if (fileCache.containsKey(name)) {
                fileCache.get(name);    // update access order (if entry exists)
            } else {
                fileCache.put(name, file);
            }
            saveRecentFileNames();
        }
    }// setRecentFileName()

    /**
     * Clears the MRU file list. Does not update menu.
     */
    public static void clearFileList() {
        if (fileCache != null) {
            fileCache.clear();
            Preferences prefs = SharedPrefs.getUserNode();
            for (int i = 0; i < NUM_RECENT_FILES; i++) {
                prefs.remove(NODE_RECENT_FILE + String.valueOf(i));
            }

            try {
                prefs.flush();
            } catch (BackingStoreException bse) {
            }
        }
    }// clearFileList()

    /**
     * Saves the recent file names
     */
    private static void saveRecentFileNames() {
        if (fileCache != null) {
            // get node
            Preferences prefs = SharedPrefs.getUserNode();

            // use an iterator to preerve access-order.
            // save in reverse-order
            int idx = NUM_RECENT_FILES - 1;
            for (Map.Entry<String, File> mapEntry : fileCache.entrySet()) {
                File file = mapEntry.getValue();

                prefs.put((NODE_RECENT_FILE + String.valueOf(idx)), file.getPath());
                idx--;
            }

            // delete any empty entries.
            while (idx > 0) {
                prefs.remove(NODE_RECENT_FILE + String.valueOf(idx));
                idx--;
            }

            try {
                prefs.flush();
            } catch (BackingStoreException bse) {
            }
        }
    }// saveRecentFileNames()

    /**
     * Returns the default save-game directory, or an empty directory ("") if none.
     */
    public static File getDefaultGameDir() {
        Preferences prefs = SharedPrefs.getUserNode();
        return new File(prefs.get(NODE_DEFAULT_GAME_DIR, ""));
    }// getDefaultGameDir()

    /**
     * Returns if we should automatically bring up the resolution results dialog
     */
    public static boolean getShowResolutionResults() {
        return getSetting(NODE_SHOW_RESOLUTION_RESULTS, true);
    }

    /**
     * Returns a given setting from preferences
     */
    private static boolean getSetting(String key, boolean defaultValue) {
        Preferences prefs = SharedPrefs.getUserNode();
        try {
            prefs.sync();
        } catch (BackingStoreException bse) {
        }
        return prefs.getBoolean(key, defaultValue);
    }// getSetting()

    /**
     * Gets the user-set variant directory, if set. If not set, returns null.
     * This will also return null if the variant directory is not actually a
     * directory.
     */
    public static File getVariantDir() {
        Preferences prefs = SharedPrefs.getUserNode();
        File file = null;
        final String text = prefs.get(NODE_VARIANT_DIR, null);
        if (text != null) {
            file = new File(text);
            if (!file.isDirectory()) {
                logger.debug("Location {} is not a directory", text);
                return null;
            }
        }

        return file;
    }// getVariantDir()

    /**
     * Sets the variant directory. This is a GUI-based set method, and can
     * be used outside of the preference panel. This will return the selected
     * file. Note that if 'immediateCommit' is true, the returned File will be
     * written immediately to the preferences backing store (this is useful
     * when not used within the Preference dialog, but does not allow for
     * the undo operation).
     * <p>
     * This will return null if no file was selected.
     */
    public static File setVariantDir(JFrame frame, boolean immediateCommit) {
        JDirectoryChooser chooser = new JDirectoryChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(Utils.getLocalString(SELECT_VARIANT_DIALOG_TITLE));

        File file = null;
        int choice = chooser.showDialog(frame, Utils.getLocalString(XJFileChooser.BTN_DIR_SELECT));
        if (choice == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile().getAbsoluteFile();
            if (immediateCommit && file != null) {
                Preferences prefs = SharedPrefs.getUserNode();
                prefs.put(NODE_VARIANT_DIR, file.toString());
                try {
                    prefs.sync();
                } catch (BackingStoreException bse) {
                }
            }
        }

        return file;
    }// setVariantDir()

    private void directoryBrowse() {
        // setup
        JDirectoryChooser chooser = new JDirectoryChooser();
        chooser.setMultiSelectionEnabled(false);

        // set directory
        String path = saveDir.getText();
        if (!"".equals(path)) {
            chooser.setCurrentDirectory(new File(path));
        }

        chooser.setDialogTitle(Utils.getLocalString(DIALOG_TITLE));
        int choice = chooser.showDialog(cf,
                Utils.getLocalString(XJFileChooser.BTN_DIR_SELECT));
        if (choice == JFileChooser.APPROVE_OPTION) {
            saveDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }// directoryBrowse()

    public void apply() {
        Preferences prefs = SharedPrefs.getUserNode();

        // apply settings
        prefs.put(NODE_DEFAULT_GAME_DIR, saveDir.getText());
        prefs.putBoolean(NODE_SHOW_RESOLUTION_RESULTS, showResolution.isSelected());

        // map settings
        prefs.put(NODE_MAP_LABEL_LEVEL, (String) mapLabels.getSelectedValue());

        // order settings
        prefs.putBoolean(NODE_ORDER_SORTING_REVERSE, reverseSort.isSelected());
        prefs.put(NODE_ORDER_SORTING, (String) orderSorting.getSelectedValue());

        try {
            prefs.flush();
        } catch (BackingStoreException bse) {
        }
    }// apply()

    public void cancel() {
        // do nothing
    }// cancel()

    public void setDefault() {
        showResolution.setSelected(false);
        saveDir.setText("");

        reverseSort.setSelected(false);
        orderSorting.reset();
        mapLabels.reset();
    }// applyDefault()

    public String getName() {
        return Utils.getLocalString(TAB_NAME);
    }// getName()

    private void getSettings() {
        Preferences prefs = SharedPrefs.getUserNode();
        try {
            prefs.sync();
        } catch (BackingStoreException bse) {
        }

        showResolution.setSelected(prefs.getBoolean(NODE_SHOW_RESOLUTION_RESULTS, true));
        saveDir.setText(prefs.get(NODE_DEFAULT_GAME_DIR, ""));

        reverseSort.setSelected(prefs.getBoolean(NODE_ORDER_SORTING_REVERSE, false));

        // get MapLabels setting
        final String mlSetting = MapRenderer2.parseLabelValue(
                prefs.get(NODE_MAP_LABEL_LEVEL, null), MapRenderer2.VALUE_LABELS_NONE);
        assert (mlSetting != null);
        mapLabels.setSelectedItem(mlSetting);

        // get order sorting setting
        final String osSetting = OrderDisplayPanel.parseSortValue(
                prefs.get(NODE_ORDER_SORTING, null), OrderDisplayPanel.SORT_PROVINCE);
        assert (mlSetting != null);
        orderSorting.setSelectedItem(osSetting);
    }// getSettings()

}// class GeneralPreferencePanel
