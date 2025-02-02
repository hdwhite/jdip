//
//  @(#)UndoClearAll.java	1.00	4/1/2002
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

import info.jdip.misc.Utils;

import javax.swing.*;
import javax.swing.undo.CompoundEdit;

/**
 * UndoClearAll
 * <p>
 * Just a fancy name for a compound edit.
 */
public class UndoClearAll extends CompoundEdit {
    private static final String PRESENTATION_NAME = "Undo.order.clearall";

    public UndoClearAll() {
        super();
    }// UndoClearAll()

    @Override
    public String getPresentationName() {
        return Utils.getLocalString(PRESENTATION_NAME);
    }// getPresentationName()

    @Override
    public String getRedoPresentationName() {
        return UIManager.getString("AbstractUndoableEdit.redoText") + " " + Utils.getLocalString(PRESENTATION_NAME);
    }// getRedoPresentationName()

    @Override
    public String getUndoPresentationName() {
        return UIManager.getString("AbstractUndoableEdit.undoText") + " " + Utils.getLocalString(PRESENTATION_NAME);
    }// getUndoPresentationName()


}// class UndoClearAll
