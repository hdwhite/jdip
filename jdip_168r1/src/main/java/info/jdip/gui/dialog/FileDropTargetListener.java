//
//  @(#)FileDropTargetListener.java		9/2004
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
package info.jdip.gui.dialog;

import info.jdip.misc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * DND (drag-and-drop) DropTarget listener that can accept drops
 * from files.
 * <p>
 * Usage:
 * <code>  new DropTarget(this, new FileDropTargetListener());</code>, after
 * subclassing and implementing <code>processDroppedFiles()</code>
 * <p>
 * TODO: get (on windows) ".lnk" file targets so shortcuts work correctly.
 */
public abstract class FileDropTargetListener extends DropTargetAdapter {
    private static final Logger logger = LoggerFactory.getLogger(FileDropTargetListener.class);

    public void drop(DropTargetDropEvent dtde) {
        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable xfer = dtde.getTransferable();
                Object obj = xfer.getTransferData(DataFlavor.javaFileListFlavor);

                if (obj instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) obj;
                    if (!list.isEmpty()) {
                        ArrayList<File> fileList = new ArrayList<>(list.size());

                        Iterator<?> iter = list.iterator();
                        while (iter.hasNext()) {
                            File originalFile = (File) (iter.next());
                            File file = convertFile(originalFile);
                            if (file != null) {
                                fileList.add(file);
                            }
                        }

                        processDroppedFiles(fileList.toArray(new File[fileList.size()]));
                    }

                    dtde.dropComplete(true);
                    return;
                }
            } catch (UnsupportedFlavorException | IOException e) {
                // fail silently
                logger.warn("There was a problem when accepting file",e);
            }
        }

        // all done
        dtde.acceptDrop(DnDConstants.ACTION_NONE);
        dtde.dropComplete(true);
    }// drop()

    /**
     * NOTE: this method will find the targets of  ".lnk" files on Windows
     * as the sun.awt.shell.ShellFolder.getShellFolder(File).getLinkLocation()
     * method does not work correctly. Instead, it will return 'null' for links.
     */
    protected File convertFile(File file) {
        if (Utils.isWindows() &&
                file.getPath().toLowerCase().endsWith(".lnk")) {
			/*
			try
			{
				return sun.awt.shell.ShellFolder.getShellFolder(file).getLinkLocation();
			}	
			catch(Throwable t)
			{
				// do nothing; we are not running in the SUN JVM
			}
			*/
            return null;
        }

        return file;
    }// converFile()


    /**
     * Subclasses must override this method.
     * The array of Files will not be null.
     * <p>
     * To avoid GUI freeze, processing should be performed in another thread.
     */
    public abstract void processDroppedFiles(File[] files);
}// class FileDropTargetListener

