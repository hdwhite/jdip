//
//  @(#)UndoDeleteOrder.java	1.00	4/1/2002
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
import info.jdip.order.Orderable;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


/**
 * UndoAddMultipleOrders is created any time multiple orders are
 * added. This can be used instead of a CompoundEdit of UndoAddOrders.
 */
public class UndoAddMultipleOrders extends XAbstractUndoableEdit {
    // instance variables
    private static final String PRESENTATION_NAME_PREFIX = "Undo.order.add.multiple";
    private final Orderable[] orders;


    public UndoAddMultipleOrders(UndoRedoManager urm, Orderable[] orders) {
        super(urm);
        this.orders = orders;
    }// UndoAddMultipleOrders()

    @Override
    public String getPresentationName() {
        return Utils.getLocalString(PRESENTATION_NAME_PREFIX);
    }// getPresentationName()

    @Override
    public void redo()
            throws CannotRedoException {
        super.redo();
        undoRedoManager.getOrderDisplayPanel().addOrdersRaw(orders, false);
    }// redo()

    @Override
    public void undo()
            throws CannotUndoException {
        super.undo();
        undoRedoManager.getOrderDisplayPanel().removeOrders(orders, false);
    }// undo()

}// class UndoAddMultipleOrders
