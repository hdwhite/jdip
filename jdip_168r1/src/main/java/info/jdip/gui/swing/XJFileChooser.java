//
//  @(#)XJFileChooser.java		7/2003
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
package info.jdip.gui.swing;

import info.jdip.misc.SimpleFileFilter;
import info.jdip.misc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;

/**
 * A simplified and extended JFileChooser for single-file (only!)
 * selections. It is also cached, so that it displays faster.
 * <p>
 * SimpleFileFilter support is integrated, so that file extensions
 * are automatically appended (unless "all files" is selected).
 * <p>
 * Futhermore, if saving a file, checking is done to determine
 * if a file will be overwritten; if so, a confirmation dialog
 * is displayed.
 * <p>
 * On the mac, the default file chooser (using AWT) is used.
 * <p>
 */
public class XJFileChooser {
    private static final Logger logger = LoggerFactory.getLogger(XJFileChooser.class);
    public static final String BTN_DIR_SELECT = "XJFileChooser.button.dir.select";
    // constants
    private static final String OVERWRITE_TEXT = "XJFileChooser.dialog.overwrite.text.location";
    private static final String OVERWRITE_TITLE = "XJFileChooser.dialog.overwrite.title";
    private static final String TITLE_SAVE_AS = "XJFileChooser.title.saveas";

    // class variables
    private static final XJFileChooser instance = new XJFileChooser();
    private static int refcount = 0;

    // instance variables
    private final FileChooser chooser;

    /**
     * Constructor
     */
    private XJFileChooser() {
        if (Utils.isOSX()) {
            chooser = new AWTFileChooser();
        } else {
            chooser = new CheckedJFileChooser();
        }
    }// XJFileChooser()


    /**
     * Can be used to initialize the XJFileChooser when called
     * to provide faster response later
     */
    public static synchronized void init() {
        // does nothing; no longer needed
    }// init()


    /**
     * Gets the XJFileChooser (only one exists -- this must be
     * enforced by you!).
     * <p>
     * The file filters are reset when this is called, which
     * means that (usually) only the AcceptAll file filter
     * (which is not a SimpleFileFilter) remains.
     */
    public static synchronized XJFileChooser getXJFileChooser() {
        refcount++;
        if (refcount > 1) {
            throw new IllegalStateException("cannot re-use getXJFileChooser(): " + refcount);
        }

        instance.reset();
        return instance;
    }// getXJFileChooser()


    /**
     * Disposes the in-use XJFileChooser. This should ALWAYS be called
     * after a display() method has been called. The number of dispose()
     * and getXJFileChooser() methods should be balanced.
     */
    public static synchronized void dispose() {
        refcount--;
        if (refcount < 0) {
            throw new IllegalStateException("XJFileChooser too many dispose() calls");
        }
    }// dispose()

    /**
     * Appends an extension, if appropriate
     */
    private static File fixFileExtension(FileFilter ff, File file) {
        if (ff instanceof SimpleFileFilter) {
            return ((SimpleFileFilter) ff).appendExtension(file);
        }

        return file;
    }// fixFileExtension()

    /**
     * Adds a SimpleFileFilter to the list of available
     * file filters.
     */
    public void addFileFilter(SimpleFileFilter filter) {
        chooser.addFileFilter(filter);
    }// addFileFilter()

    /**
     * Sets the default file filter. If null, sets the
     * 'accept all' file filter.
     */
    public void setFileFilter(SimpleFileFilter filter) {
        chooser.setFileFilter(filter);
    }// addFileFilter()

    /**
     * Set the current directory. May be set to null ('home' directory)
     */
    public void setCurrentDirectory(File file) {
        chooser.setCurrentDirectory(file);
    }// setCurrentDirectory()

    /**
     * Sets the suggested file name. By default, no file name
     * is suggested.
     */
    public void setSelectedFile(File file) {
        chooser.setSelectedFile(file);
    }// setSuggestedFileName()

    /**
     * Display the file chooser, with the given title and Accept button
     * text. No file filters are added. The type (JFileChooser.OPEN_DIALOG or
     * SAVE_DIALOG) must be specified. If the acceptButtonText and/or title is null, the default
     * button text (for OPEN_DIALOG or SAVE_DIALOG) and/or title is used.
     *
     * @return the selected File, or null
     */
    public File display(Frame parent, String title, String acceptButtonText, int type, int mode) {
        if (parent == null) {
            // this could lead to nonmodal behavior, and possibly we could
            // increment the refcount since we are not locked here...
            logger.warn( "XJFileChooser.display() called with NULL parent!");
        }

        return chooser.display(parent, title, acceptButtonText, type, mode);
    }// display()

    /**
     * The typical "Open" dialog. No filters are added.
     *
     * @return the selected File, or null
     */
    public File displayOpen(Frame parent) {
        return display(parent, null, null, JFileChooser.OPEN_DIALOG, JFileChooser.FILES_ONLY);
    }// displayOpen()

    /**
     * The typical "Open" dialog. No filters are added.
     * A title may be specified.
     *
     * @return the selected File, or null
     */
    public File displayOpen(Frame parent, String title) {
        return display(parent, title, null, JFileChooser.OPEN_DIALOG, JFileChooser.FILES_ONLY);
    }// displayOpen()


    /**
     * The typical "Save" dialog. No filters are added.
     *
     * @return the selected File, or null
     */
    public File displaySave(Frame parent) {
        return display(parent, null, null, JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
    }// displaySave()

    /**
     * The typical "Save" dialog. No filters are added.
     * A different title may be specified.
     *
     * @return the selected File, or null
     */
    public File displaySave(Frame parent, String title) {
        return display(parent, title, null, JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
    }// displaySave()


    /**
     * The typical "Save As" dialog. No filters are added.
     *
     * @return the selected File, or null
     */
    public File displaySaveAs(Frame parent) {
        final String title = Utils.getLocalString(TITLE_SAVE_AS);
        return display(parent, title, null, JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
    }// displaySaveAs()

    /**
     *	Display a filechooser that only allows the selection
     *	of a single directory.
     *
     *    @return the selected directory, or null
     */
	/*
	
    /**
     * resets the XJFileChooser to its default state
     */
    private void reset() {
        chooser.reset();
    }// reset()


    /**
     * FileChooser interface
     */
    private interface FileChooser {
        void reset();

        void addFileFilter(SimpleFileFilter filter);

        void setFileFilter(SimpleFileFilter filter);

        void setCurrentDirectory(File file);

        void setSelectedFile(File file);

        File display(Frame parent, String title, String acceptButtonText, int type, int mode);
    }// interface FileChooser


    /**
     * FileChooser implementation: AWT
     */
    private class AWTFileChooser implements FileChooser {
        private SimpleFileFilter awtFilter = null;
        private String selectedDir = null;
        private String selectedFile = null;

        public AWTFileChooser() {
        }// AWTFileChooser()

        public void reset() {
            awtFilter = null;
            selectedDir = null;
            selectedFile = null;
        }// reset()

        // LIMITATION: we can have only 1 file filter
        public void addFileFilter(SimpleFileFilter filter) {
            awtFilter = filter;
        }// addFileFilter()

        public void setFileFilter(SimpleFileFilter filter) {
            awtFilter = filter;
        }// setFileFilter()

        public void setCurrentDirectory(File file) {
            selectedDir = (file.isDirectory()) ? file.toString() : file.getPath();
        }// setCurrentDirectory()

        public void setSelectedFile(File file) {
            selectedFile = (file == null) ? null : file.getName();
        }// setSelectedFile()

        public File display(Frame parent, String title, String acceptButtonText, int type, int mode) {
            FileDialog fd = new FileDialog(parent);
            fd.setModal(true);
            fd.setResizable(true);

            if (type == JFileChooser.OPEN_DIALOG) {
                fd.setMode(FileDialog.LOAD);
            } else if (type == JFileChooser.SAVE_DIALOG) {
                fd.setMode(FileDialog.SAVE);
            } else {
                throw new IllegalArgumentException("invalid type: " + type);
            }

            if (title != null) {
                fd.setTitle(title);
            }

            if (awtFilter != null) {
                fd.setFilenameFilter(awtFilter);
            }

            if (selectedDir != null) {
                fd.setDirectory(selectedDir);
            }

            if (selectedFile != null) {
                fd.setFile(selectedFile);
            }

            fd.setVisible(true);
            fd.dispose();

            if (fd.getFile() != null) {
                File file = new File(fd.getDirectory(), fd.getFile());

                // wfix filename if we can/should (adds appropriate extension if it is missing)
                if (type == JFileChooser.SAVE_DIALOG && (fd.getFilenameFilter() instanceof SimpleFileFilter)) {
                    return fixFileExtension((SimpleFileFilter) fd.getFilenameFilter(),
                            file);
                } else {
                    return file;
                }
            }

            return null;
        }// display()


    }// inner class AWTFileChooser

    /**
     * FileChooser implementation: CheckedJFileChooser
     */
    private class CheckedJFileChooser extends JFileChooser implements FileChooser {
        public CheckedJFileChooser() {
            super((File) null);
            setAcceptAllFileFilterUsed(true);
        }

        // Override to check for overwrite confirmation
        @Override
        public void approveSelection() {
            if (getDialogType() != JFileChooser.OPEN_DIALOG) {
                File selectedFile = fixFileExtension(this.getFileFilter(), this.getSelectedFile());
                if (selectedFile != null && selectedFile.exists()) {
                    String message = Utils.getText(Utils.getLocalString(OVERWRITE_TEXT), selectedFile.getName());

                    int result = JOptionPane.showConfirmDialog(getParent(),
                            message,
                            Utils.getLocalString(OVERWRITE_TITLE),
                            JOptionPane.YES_NO_OPTION);

                    if (result != JOptionPane.YES_OPTION) {
                        cancelSelection();
                        return;
                    }
                    // fall thru
                }
            }

            super.approveSelection();
        }// approveSelection()

        public void reset() {
            this.setSelectedFile(new File(""));
            this.resetChoosableFileFilters();
            this.setMultiSelectionEnabled(false);
            this.setCurrentDirectory(null);
        }// reset()

        public void addFileFilter(SimpleFileFilter filter) {
            this.addChoosableFileFilter(filter);
        }// addFileFilter()

        public void setFileFilter(SimpleFileFilter filter) {
            if (filter != null) {
                super.setFileFilter(filter);
            } else {
                super.setFileFilter(this.getAcceptAllFileFilter());
            }
        }// setFileFilter()


        @Override
        public void setSelectedFile(File file) {
            if (file == null) {
                super.setSelectedFile(new File(""));
            } else {
                super.setSelectedFile(file);
            }
        }// setSelectedFile()

        public File display(Frame parent, String title, String acceptButtonText, int type, int mode) {
            if (type != JFileChooser.OPEN_DIALOG && type != JFileChooser.SAVE_DIALOG) {
                throw new IllegalArgumentException("invalid type");
            }

            this.setDialogType(type);

            if (acceptButtonText != null) {
                this.setApproveButtonText(acceptButtonText);
            }

            if (title != null) {
                this.setDialogTitle(title);
            }

            this.setFileSelectionMode(mode);

            if (this.showDialog(parent, null) == JFileChooser.APPROVE_OPTION) {
                if (this.getDialogType() != JFileChooser.OPEN_DIALOG) {
                    return fixFileExtension(this.getFileFilter(), this.getSelectedFile());
                } else {
                    return this.getSelectedFile();
                }
            }

            return null;
        }// display()
    }// class CheckedJFileChooser

}// class XJFileChooser

