//
//  @(#)AbstractCFPListener.java	12/2003
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
package dip.gui;

import dip.gui.map.MapMetadata;
import dip.order.Orderable;
import dip.order.ValidationOptions;
import dip.world.Power;
import dip.world.TurnState;
import dip.world.World;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * An Abstract ClientFrame PropertyChangeListener.
 * <p>
 * The advantage of using this class is cleaner code, and easier
 * subclassing. Errors are reduced, because only appropriate
 * and type-correct arguments are passed to the handler methods.
 * <p>
 * The propertyChange() method is final; it dispatches
 * property change events to the handler methods; there should be no need to
 * override this method in subclasses.
 * <p>
 * All handler methods do nothing by default.
 */
public abstract class AbstractCFPListener implements PropertyChangeListener {


    /**
     * Process PropertyChangeEvents into appropriate sub-methods. Marked final for safety.
     */
    public final void propertyChange(PropertyChangeEvent evt) {
        final String name = evt.getPropertyName();
        if (ClientFrame.EVT_WORLD_CREATED.equals(name)) {
            final World w = (World) evt.getNewValue();
            actionWorldCreated(w);
        } else if (ClientFrame.EVT_WORLD_DESTROYED.equals(name)) {
            final World w = (World) evt.getOldValue();
            actionWorldDestroyed(w);
        } else if (ClientFrame.EVT_MODIFIED_STATE.equals(name)) {
            actionStateModified();
        } else if (ClientFrame.EVT_TURNSTATE_CHANGED.equals(name)) {
            final TurnState ts = (TurnState) evt.getNewValue();
            actionTurnstateChanged(ts);
        } else if (ClientFrame.EVT_TURNSTATE_ADDED.equals(name)) {
            final TurnState ts = (TurnState) evt.getNewValue();
            actionTurnstateAdded(ts);
        } else if (ClientFrame.EVT_TURNSTATE_REMOVED.equals(name)) {
            actionTurnstateRemoved();
        } else if (ClientFrame.EVT_TURNSTATE_RESOLVED.equals(name)) {
            final TurnState ts = (TurnState) evt.getNewValue();
            actionTurnstateResolved(ts);
        } else if (ClientFrame.EVT_MODE_CHANGED.equals(name)) {
            final String mode = (String) evt.getNewValue();
            actionModeChanged(mode);
        } else if (ClientFrame.EVT_VALOPTS_CHANGED.equals(name)) {
            final ValidationOptions options = (ValidationOptions) evt.getNewValue();
            actionValOptsChanged(options);
        } else if (ClientFrame.EVT_MMD_READY.equals(name)) {
            final MapMetadata mmd = (MapMetadata) evt.getNewValue();
            actionMMDReady(mmd);
        } else if (ClientFrame.EVT_ORDER_CREATED.equals(name)) {
            final Orderable order = (Orderable) evt.getNewValue();
            actionOrderCreated(order);
        } else if (ClientFrame.EVT_ORDER_DELETED.equals(name)) {
            final Orderable order = (Orderable) evt.getOldValue();
            actionOrderDeleted(order);
        } else if (ClientFrame.EVT_MULTIPLE_ORDERS_CREATED.equals(name)) {
            final Orderable[] orders = (Orderable[]) evt.getNewValue();
            actionOrdersCreated(orders);
        } else if (ClientFrame.EVT_MULTIPLE_ORDERS_DELETED.equals(name)) {
            final Orderable[] orders = (Orderable[]) evt.getOldValue();
            actionOrdersDeleted(orders);
        } else if (ClientFrame.EVT_DISPLAYABLE_POWERS_CHANGED.equals(name)) {
            final Power[] newPowers = (Power[]) evt.getNewValue();
            final Power[] oldPowers = (Power[]) evt.getOldValue();
            actionDisplayablePowersChanged(oldPowers, newPowers);
        } else if (ClientFrame.EVT_ORDERABLE_POWERS_CHANGED.equals(name)) {
            final Power[] newPowers = (Power[]) evt.getNewValue();
            final Power[] oldPowers = (Power[]) evt.getOldValue();
            actionOrderablePowersChanged(oldPowers, newPowers);
        }
    }// propertyChange()

    /**
     * Invoked when a World is created
     */
    public void actionWorldCreated(World w) {
    }

    /**
     * Invoked when a World is destroyed
     */
    public void actionWorldDestroyed(World w) {
    }

    /**
     * Invoked when state changes from saved state
     */
    public void actionStateModified() {
    }

    /**
     * Invoked when a TurnState changes
     */
    public void actionTurnstateChanged(TurnState ts) {
    }

    /**
     * Invoked when a TurnState is added to the World
     */
    public void actionTurnstateAdded(TurnState ts) {
    }

    /**
     * Invoked when a TurnState is removed from the World
     */
    public void actionTurnstateRemoved() {
    }

    /**
     * Invoked when a TurnState has been resolved
     */
    public void actionTurnstateResolved(TurnState ts) {
    }

    /**
     * Invoked when Validation Options have changed
     */
    public void actionValOptsChanged(ValidationOptions options) {
    }

    /**
     * Invoked when the Mode has changed (see ClientFrame)
     */
    public void actionModeChanged(String mode) {
    }

    /**
     * Invoked when MapMetadata information is ready/not ready
     */
    public void actionMMDReady(MapMetadata mmd) {
    }

    /**
     * Invoked when an Order is created
     */
    public void actionOrderCreated(Orderable order) {
    }

    /**
     * Invoked when an Order is deleted
     */
    public void actionOrderDeleted(Orderable order) {
    }

    /**
     * Invoked when multiple Orders are created
     */
    public void actionOrdersCreated(Orderable[] orders) {
    }

    /**
     * Invoked when multiple Orders are deleted
     */
    public void actionOrdersDeleted(Orderable[] orders) {
    }

    /**
     * Invoked when the orderable Powers have changed
     */
    public void actionOrderablePowersChanged(Power[] oldPowers, Power[] newPowers) {
    }

    /**
     * Invoked when the displayable Powers have changed
     */
    public void actionDisplayablePowersChanged(Power[] oldPowers, Power[] newPowers) {
    }

}// class AbstractCFPListener

