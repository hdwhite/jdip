//
//  @(#)UndoEditSCOwner.java	1.00	8/2002
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
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


/**
 * UndoEditSCOwner is created any time a supply center changes ownership.
 */
public class UndoEditSCOwner extends XAbstractUndoableEdit {
    private static final String PRESENTATION_NAME = "Undo.edit.changescowner";

    // instance variables
    private final Position position;
    private final Province province;
    private final Power oldPower;
    private final Power newPower;


    public UndoEditSCOwner(UndoRedoManager urm, Position position, Province province, Power oldPower, Power newPower) {
        super(urm);
        this.position = position;
        this.province = province;
        this.oldPower = oldPower;
        this.newPower = newPower;
    }// UndoEditSCOwner

    /**
     * helper method: change SC owner
     */
    private static void changeSCOwner(UndoRedoManager urm, Position pos, Province prov, Power newPow) {
        pos.setSupplyCenterOwner(prov, newPow);

        // re-render province & set changed flag
        urm.getClientFrame().fireStateModified();
        urm.getClientFrame().getMapPanel().updateProvince(prov);

        urm.getOrderDisplayPanel().revalidateAllOrders();
    }// changeSCOwner()

    public String getPresentationName() {
        return Utils.getLocalString(PRESENTATION_NAME);
    }// getPresentationName()

    public void redo()
            throws CannotRedoException {
        super.redo();
        UndoEditSCOwner.changeSCOwner(undoRedoManager, position, province, newPower);
    }// redo()

    public void undo()
            throws CannotUndoException {
        super.undo();
        UndoEditSCOwner.changeSCOwner(undoRedoManager, position, province, oldPower);
    }// undo()


}// class UndoEditSCOwner

