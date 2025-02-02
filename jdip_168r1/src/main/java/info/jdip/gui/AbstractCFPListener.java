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
package info.jdip.gui;

import info.jdip.gui.map.MapMetadata;
import info.jdip.order.Orderable;
import info.jdip.order.ValidationOptions;
import info.jdip.world.Power;
import info.jdip.world.TurnState;
import info.jdip.world.World;

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
        switch (evt.getPropertyName()) {
            case ClientFrame.EVT_WORLD_CREATED: {
                final World w = (World) evt.getNewValue();
                actionWorldCreated(w);
                break;
            }
            case ClientFrame.EVT_WORLD_DESTROYED: {
                final World w = (World) evt.getOldValue();
                actionWorldDestroyed(w);
                break;
            }
            case ClientFrame.EVT_MODIFIED_STATE: {
                actionStateModified();
                break;
            }
            case ClientFrame.EVT_TURNSTATE_CHANGED: {
                final TurnState ts = (TurnState) evt.getNewValue();
                actionTurnstateChanged(ts);
                break;
            }
            case ClientFrame.EVT_TURNSTATE_ADDED: {
                final TurnState ts = (TurnState) evt.getNewValue();
                actionTurnstateAdded(ts);
                break;
            }
            case ClientFrame.EVT_TURNSTATE_REMOVED: {
                actionTurnstateRemoved();
                break;
            }
            case ClientFrame.EVT_TURNSTATE_RESOLVED: {
                final TurnState ts = (TurnState) evt.getNewValue();
                actionTurnstateResolved(ts);
                break;
            }
            case ClientFrame.EVT_MODE_CHANGED: {
                final String mode = (String) evt.getNewValue();
                actionModeChanged(mode);
                break;
            }
            case ClientFrame.EVT_VALOPTS_CHANGED: {
                final ValidationOptions options = (ValidationOptions) evt.getNewValue();
                actionValOptsChanged(options);
                break;
            }
            case ClientFrame.EVT_MMD_READY: {
                final MapMetadata mmd = (MapMetadata) evt.getNewValue();
                actionMMDReady(mmd);
                break;
            }
            case ClientFrame.EVT_ORDER_CREATED: {
                final Orderable order = (Orderable) evt.getNewValue();
                actionOrderCreated(order);
                break;
            }
            case ClientFrame.EVT_ORDER_DELETED: {
                final Orderable order = (Orderable) evt.getOldValue();
                actionOrderDeleted(order);
                break;
            } case ClientFrame.EVT_MULTIPLE_ORDERS_CREATED: {
                final Orderable[] orders = (Orderable[]) evt.getNewValue();
                actionOrdersCreated(orders);
                break;
            }
            case ClientFrame.EVT_MULTIPLE_ORDERS_DELETED: {
                final Orderable[] orders = (Orderable[]) evt.getOldValue();
                actionOrdersDeleted(orders);
                break;
            }
            case ClientFrame.EVT_DISPLAYABLE_POWERS_CHANGED: {
                final Power[] newPowers = (Power[]) evt.getNewValue();
                final Power[] oldPowers = (Power[]) evt.getOldValue();
                actionDisplayablePowersChanged(oldPowers, newPowers);
                break;
            }
            case ClientFrame.EVT_ORDERABLE_POWERS_CHANGED: {
                final Power[] newPowers = (Power[]) evt.getNewValue();
                final Power[] oldPowers = (Power[]) evt.getOldValue();
                actionOrderablePowersChanged(oldPowers, newPowers);
                break;
            }
            default:
                break;
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

