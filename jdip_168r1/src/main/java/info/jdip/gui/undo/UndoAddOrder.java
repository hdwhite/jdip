//
//  @(#)UndoAddOrder.java		4/2002
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

import info.jdip.order.Orderable;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;


/**
 * UndoAddOrder is created any time an order is entered.
 */
public class UndoAddOrder extends XAbstractUndoableEdit {
    // instance variables
    private final Orderable order;


    public UndoAddOrder(UndoRedoManager urm, Orderable order) {
        super(urm);
        this.order = order;
    }// UndoAddOrder

    @Override
    public String getPresentationName() {
        return order.getFullName();
    }// getPresentationName()

    @Override
    public void redo()
            throws CannotRedoException {
        super.redo();
        undoRedoManager.getOrderDisplayPanel().addOrder(order, false);
    }// redo()

    @Override
    public void undo()
            throws CannotUndoException {
        super.undo();
        undoRedoManager.getOrderDisplayPanel().removeOrder(order, false);
    }// undo()

}// class UndoAddOrder

