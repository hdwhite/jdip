//
//  @(#)XAbstractUndoableEdit.java		1/2003
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
package info.jdip.gui.undo;

import javax.swing.undo.AbstractUndoableEdit;


/**
 * XAbstractUndoableEdit is the base class for all jDip undoable edits.
 * it provides base functionality and ensures serialization goes smoothly.
 */
public abstract class XAbstractUndoableEdit extends AbstractUndoableEdit {
    protected UndoRedoManager undoRedoManager = null;

    /**
     * Constructs an XAbstractUndoableEdit object.<p>
     * UndoRedoManager must not be null.
     */
    public XAbstractUndoableEdit(UndoRedoManager urm) {
        if (urm == null) {
            throw new IllegalArgumentException();
        }

        undoRedoManager = urm;
    }// XAbstractUndoableEdit()
}// class XAbstractUndoableEdit
