//
//  @(#)OrderDisplayPanel.java		5/2003
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

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;
import info.jdip.gui.dialog.prefs.GeneralPreferencePanel;
import info.jdip.gui.swing.XJScrollPane;
import info.jdip.gui.undo.UndoAddMultipleOrders;
import info.jdip.gui.undo.UndoAddOrder;
import info.jdip.gui.undo.UndoClearAll;
import info.jdip.gui.undo.UndoDeleteMultipleOrders;
import info.jdip.gui.undo.UndoDeleteOrder;
import info.jdip.gui.undo.UndoRedoManager;
import info.jdip.misc.Utils;
import info.jdip.order.Order;
import info.jdip.order.OrderException;
import info.jdip.order.OrderParser;
import info.jdip.order.OrderWarning;
import info.jdip.order.Orderable;
import info.jdip.order.ValidationOptions;
import info.jdip.process.Adjustment;
import info.jdip.world.Phase;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.TurnState;
import info.jdip.world.World;

import javax.swing.*;
import javax.swing.undo.CompoundEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The OrderDisplayPanel: displayer of orders.
 * <p>
 * OrderDisplayPanel provides a wrapper for viewing orders within a TurnState object,
 * as well as adding orders to a TurnState object.
 * <p>
 * Advantages of using OrderDisplayPanel to modify TurnState include:
 * <ul>
 * <li>Less code to add a single order to the TurnState</li>
 * <li>Validates orders using current validation option settings</li>
 * <li>Prevents duplicate orders from being added</li>
 * <li>Prevents more than one order being issued from one province</li>
 * <li>Allows optional Undo/Redo support for actions</li>
 * <li>Checks for too many build/remove orders in Adjustment phase
 * <li>Checks that added order is part of orderable powers group (array)</li>
 * </ul>
 * It is recommended that all orders are entered via the OrderDisplayPanel. Note
 * that if orders are entered into the TurnState directly, approrpriate ClientFrame
 * fire() methods must be called to inform the OrderDisplayPanel of updates. Also,
 * care must be taken to avoid entering duplicate orders, and that only one order per
 * unit (per province).
 * <p>
 * <b>Important Note:</b> The add/remove/removeAll methods will only operate on
 * orderable power. Thus if an order is added and the power is not in the orderable
 * power list, it will fail. This applies to removes as well. For removeAll, only
 * orderable powers orders are removed.
 */
public class OrderDisplayPanel extends JPanel {
    // sorting constants
    /**
     * Sort Orders by Power
     */
    public static final String SORT_POWER = "SORT_POWER";
    /**
     * Sort Orders by Province
     */
    public static final String SORT_PROVINCE = "SORT_PROVINCE";
    /**
     * Sort Orders by Unit
     */
    public static final String SORT_UNIT = "SORT_UNIT";
    /**
     * Sort Orders by Order type
     */
    public static final String SORT_ORDER = "SORT_ORDER";
    public static final String LABEL_SORT_POWER = "OP.sort.button.power";
    public static final String LABEL_SORT_PROVINCE = "OP.sort.button.province";
    public static final String LABEL_SORT_UNIT = "OP.sort.button.unit";
    public static final String LABEL_SORT_ORDER = "OP.sort.button.order";
    // color constants
    private static final Color BG_DEFAULT = UIManager.getColor("List.background");
    private static final Color BG_HILITE = new Color(230, 238, 240);
    // i18n constants
    private static final String ORD_ERR_AMBIGUOUS = "OP.order.err.ambiguous";
    private static final String ORD_ERR_INVALID = "OP.order.err.invalid";
    private static final String ORD_ERR_UNEDITABLE = "OP.order.err.uneditable";
    private static final String ORD_ERR_NOT_ORDERABLE = "OP.order.err.notorderable";
    private static final String LABEL_SORT = "OP.label.sort";
    private static final String DLG_TOOMANY_TEXT_LOCATION = "OP.dlg.toomany.text.location";


    // instance variables
    protected AbstractCFPListener propListener = null;
    protected ClientFrame clientFrame = null;
    protected World world = null;
    protected TurnState turnState = null;
    protected final JScrollPane orderListScrollPane;
    private ValidationOptions valOpts = null;
    private OrderParser orderParser = null;
    private Power[] displayablePowers = null;
    private Power[] orderablePowers = null;
    private OrderListModel orderListModel = null;
    private boolean isEditable = false;
    private Adjustment.AdjustmentInfoMap adjMap = null;    // non-null only in Adjustment phase
    private UndoRedoManager undoManager = null;
    // GUI component instance variables
    private final JList<DisplayOrder> orderList;


    /**
     * Creates an OrderDisplayPanel
     */
    public OrderDisplayPanel(ClientFrame clientFrame) {
        // init
        this.clientFrame = clientFrame;
        orderParser = OrderParser.getInstance();

        // order list basic setup
        orderListModel = new OrderListModel();
        orderList = new JList<>(orderListModel);
        orderList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        orderList.setDragEnabled(false);
        orderList.setCellRenderer(new OrderListRenderer());

        // set order list sort preference
        setSorting(
                GeneralPreferencePanel.getOrderSortMode(),
                GeneralPreferencePanel.getOrderSortReverse()
        );

        orderListScrollPane = new XJScrollPane(orderList);
        orderListScrollPane.getViewport().setBackground(orderList.getBackground());    // match table & scroll backgrounds

        // setup property change listener
        propListener = createPropertyListener();
        clientFrame.addPropertyChangeListener(propListener);

        // layout
        makeLayout();
    }// OrderDisplayPanel()

    /**
     * Return a sort constant (identity) by parsing a sort-constant
     * string. This makes sort-constant serialization safer.
     * Does a case-insensitive compare. Instance equality is preserved.
     * Returns the given default if parsing fails.
     * <p>
     * defaultValue cannot be null.
     */
    public static String parseSortValue(String in, String defaultValue) {
        if (!SORT_POWER.equals(defaultValue)
                && !SORT_PROVINCE.equals(defaultValue)
                && !SORT_ORDER.equals(defaultValue)
                && !SORT_UNIT.equals(defaultValue)
                && defaultValue != null) {
            throw new IllegalArgumentException();
        }

        if (SORT_POWER.equalsIgnoreCase(in)) {
            return SORT_POWER;
        } else if (SORT_PROVINCE.equalsIgnoreCase(in)) {
            return SORT_PROVINCE;
        } else if (SORT_ORDER.equalsIgnoreCase(in)) {
            return SORT_ORDER;
        } else if (SORT_UNIT.equalsIgnoreCase(in)) {
            return SORT_UNIT;
        }

        return defaultValue;
    }// parseSortValue()

    /**
     * OrderDisplayPanel cleanup.
     */
    public void close() {
        clientFrame.removePropertyChangeListener(propListener);
    }// close()

    /**
     * Create the ODPPropertyListener used to receive events.
     */
    protected AbstractCFPListener createPropertyListener() {
        return new ODPPropertyListener();
    }// createPropertyListener()

    /**
     * Add an order to the order list, possibly with undo/redo support,
     * and only succeeding if the order passes validation given the
     * validation constraints. Duplicate orders are prevented.
     * <p>
     * This differs from addOrderRaw in that popup dialogs are displayed
     * with an error message, instead of throwing OrderExceptions.
     *
     * @param order    - <b>true</b> Orderable object
     * @param undoable - <b>true</b> if this is an undoable action
     * @return <b>true</b> if the order was accepted
     */
    public synchronized boolean addOrder(Orderable order, boolean undoable) {
        try {
            addOrderRaw(order, undoable);
            orderList.clearSelection();
            return true;
        } catch (OrderWarning ow) {
            // order is still acceptable (and was accepted), but issue a warning message.
            Utils.popupError(clientFrame, Utils.getLocalString(ORD_ERR_AMBIGUOUS), ow.getMessage());
            return true;
        } catch (OrderException e) {
            // invalid order; we must exit method here
            Utils.popupError(clientFrame, Utils.getLocalString(ORD_ERR_INVALID), e.getMessage());
        }

        return false;
    }// addOrder()

    /**
     * Parses and then adds an order to the order list, possibly with undo/redo support,
     * and only succeeding if the order passes validation given the
     * validation constraints. Duplicate orders are prevented.
     * <p>
     * This differs from addOrderRaw in that popup dialogs are displayed
     * with an error message, instead of throwing OrderExceptions.
     *
     * @param orderText - <b>true</b> order text to parse
     * @param undoable  - <b>true</b> if this is an undoable action
     * @return <b>true</b> if the order was accepted
     */
    public synchronized boolean addOrder(String orderText, boolean undoable) {
        // null orders not permitted.
        if (orderText == null) {
            throw new IllegalArgumentException();
        }

        try {
            addOrderRaw(orderText, undoable);
            orderList.clearSelection();
            return true;
        } catch (OrderWarning ow) {
            // order is still acceptable (and was accepted), but issue a warning message.
            Utils.popupError(clientFrame, Utils.getLocalString(ORD_ERR_AMBIGUOUS), ow.getMessage());
            return true;
        } catch (OrderException e) {
            // invalid order; we must exit method here
            Utils.popupError(clientFrame, Utils.getLocalString(ORD_ERR_INVALID), e.getMessage());
        }

        return false;
    }// addOrder()

    /**
     * Parses and then adds an order to the TurnState,
     * possibly with undo/redo support,
     * and only succeeding if the order passes validation given the
     * validation constraints. Duplicate orders are prevented.
     *
     * @param orderText - <b>true</b> Order text to parse
     * @param undoable  - <b>true</b> if this is an undoable action
     * @throws OrderException - if the order fails validation
     */
    public synchronized void addOrderRaw(String orderText, boolean undoable)
            throws OrderException {
        // null orders not permitted.
        if (orderText == null) {
            throw new IllegalArgumentException();
        }

        // not editable! should not be adding orders.
        if (!isEditable) {
            throw new OrderException(Utils.getLocalString(ORD_ERR_UNEDITABLE));
        }

        // parse order
        Orderable order = orderParser.parse(clientFrame.getGUIOrderFactory(),
                orderText, null, turnState, false, true);

        // now use addOrderRaw()
        addOrderRaw(order, undoable);
    }// addOrderRaw()

    /**
     * Add an order to the TurnState, possibly with undo/redo support,
     * and only succeeding if the order passes validation given the
     * validation constraints. Duplicate orders are prevented.
     *
     * @param order    - <b>true</b> Orderable object
     * @param undoable - <b>true</b> if this is an undoable action
     * @throws OrderException - if the order fails validation
     */
    public synchronized void addOrderRaw(Orderable order, boolean undoable)
            throws OrderException {
        // null orders not permitted.
        if (order == null) {
            throw new IllegalArgumentException();
        }

        // not editable! should not be adding orders.
        if (!isEditable) {
            throw new OrderException(Utils.getLocalString(ORD_ERR_UNEDITABLE));
        }

        // not orderable! cannot add this order.
        if (!isOrderable(order)) {
            throw new OrderException(Utils.getLocalString(ORD_ERR_NOT_ORDERABLE));
        }


        // check the order; if a warning is thrown, catch and hold for later.
        // an order with a warning is still acceptable.
        OrderWarning orderWarning = null;

        try {
            order.validate(turnState, valOpts, world.getRuleOptions());
        } catch (OrderWarning ow) {
            orderWarning = ow;
        }


        // check that adjustments are in-line (not too many have been issued)
        checkAdjustments(order);

        // note: if exception is thrown, we won't get to here.
        //
        // actually add/replace the order in the turnstate
        // if an order already exists for this province (unit),
        // the old order is returned.
        Orderable replacedOrder = addOrderToTS(order);

        // HOWEVER, if the new order is an exact duplicate of the old
        // order, the addOrderToTS() will not replace the old order.
        // to determine this, we must compare the order it found have found
        // with the current order. Remember, replacedOrder may be null.
        if (!order.equals(replacedOrder)) {
            if (undoable) {
                if (replacedOrder == null) {
                    undoManager.addEdit(new UndoAddOrder(undoManager, order));
                } else {
                    // if old order is EXACTLY equivalent to new order,
                    // we don't do anything;

                    // 2 actions involved in this case...
                    CompoundEdit miniEdit = new CompoundEdit();
                    miniEdit.addEdit(new UndoDeleteOrder(undoManager, replacedOrder));
                    miniEdit.addEdit(new UndoAddOrder(undoManager, order));
                    miniEdit.end();
                    undoManager.addEdit(miniEdit);
                }
            }

            // inform everyone that an order has been created
            // (and possibly an existing order has been deleted)
            if (replacedOrder != null) {
                clientFrame.fireOrderDeleted(replacedOrder);
            }

            clientFrame.fireOrderCreated(order);
            clientFrame.fireStateModified();
        }

        // an acceptable, but possibly ambiguous, order occured;
        // the exception was held until now.
        if (orderWarning != null) {
            throw orderWarning;
        }
    }// addOrderRaw()

    /**
     * Adds multiple orders to the TurnState. If failures occur, we
     * return a Map of exceptions (key is order, value is the
     * order exception). If no exceptions were created, a null HashMap
     * is returned. All orders that do not fail are added to the TurnState.
     * Warnings are also returned. If we are not in an editable state,
     * an IllegalStateException is thrown. Orders that exist as duplicates
     * within the TurnState are ignored.
     *
     * @param orders   - <b>true</b> Orderable object array
     * @param undoable - <b>true</b> if this is an undoable action
     * @return A list of OrderExceptions, or null
     */
    public synchronized java.util.Map<Orderable, OrderException> addOrdersRaw(Orderable[] orders, boolean undoable) {
        // null orders not permitted.
        if (orders == null) {
            throw new IllegalArgumentException();
        }

        // not editable! should not be adding orders.
        if (!isEditable) {
            throw new IllegalStateException("not in editable state");
        }

        LinkedHashMap<Orderable, OrderException> map = new LinkedHashMap<>(19);
        ArrayList<Orderable> ordersAdded = new ArrayList<>(orders.length);
        ArrayList<Orderable> ordersDeleted = new ArrayList<>(orders.length);

        for (Orderable order : orders) {
            try {
                order.validate(turnState, valOpts, world.getRuleOptions());
                checkAdjustments(order);
            } catch (OrderWarning ow) {
                map.put(order, ow);
            } catch (OrderException oe) {
                map.put(order, oe);
                continue;
            }

            Orderable replacedOrder = addOrderToTS(order);

            // check for duplicates; these are ignored
            if (!order.equals(replacedOrder)) {
                ordersAdded.add(order);

                if (replacedOrder != null) {
                    ordersDeleted.add(replacedOrder);
                }
            }
        }

        if (!ordersAdded.isEmpty())    // make sure we added at least one order
        {
            Orderable[] tmpDel = null;
            if (!ordersDeleted.isEmpty()) {
                tmpDel = ordersDeleted.toArray(new Orderable[ordersAdded.size()]);
                clientFrame.fireMultipleOrdersDeleted(tmpDel);
            }

            Orderable[] tmpAdd = ordersAdded.toArray(new Orderable[ordersAdded.size()]);
            clientFrame.fireMultipleOrdersCreated(tmpAdd);

            if (undoable) {
                if (tmpDel == null) {
                    undoManager.addEdit(new UndoAddMultipleOrders(undoManager, tmpAdd));
                } else {
                    CompoundEdit bigEdit = new CompoundEdit();
                    bigEdit.addEdit(new UndoDeleteMultipleOrders(undoManager, tmpDel));
                    bigEdit.addEdit(new UndoAddMultipleOrders(undoManager, tmpAdd));
                    bigEdit.end();
                    undoManager.addEdit(bigEdit);
                }
            }

            clientFrame.fireStateModified();
        }

        return ((map.size() > 0) ? map : null);
    }// addOrdersRaw()

    /**
     * Remove the given order from the TurnState. Fails is order is not
     * given by an orderable power.
     *
     * @param order    - <b>true</b> Orderable object
     * @param undoable - <b>true</b> if this is an undoable action
     * @return <b>true</b> if the order was found and removed
     */
    public synchronized boolean removeOrder(Orderable order, boolean undoable) {
        if (!isOrderable(order)) {
            return false;
        }

        boolean found = removeOrderFromTS(order);
        assert (found);

        if (undoable && found) {
            undoManager.addEdit(new UndoDeleteOrder(undoManager, order));
        }

        if (found) {
            clientFrame.fireOrderDeleted(order);
            clientFrame.fireStateModified();
            orderList.clearSelection();
        }

        return found;
    }// removeOrder()

    /**
     * Removes the given orders. Note that if an order is not
     * a member of the orderablePowers group, it will not be
     * removed.
     *
     * @param orders   - <b>true</b> Orderable object array
     * @param undoable - <b>true</b> if this is an undoable action
     * @return <b>true</b> if <i>all</i> orders were found and removed
     */
    public synchronized boolean removeOrders(Orderable[] orders, boolean undoable) {
        int count = 0;
        ArrayList<Orderable> deletedOrderList = new ArrayList<>(orders.length);

        for (Orderable order : orders) {
            if (isOrderable(order)) {
                assert (removeOrderFromTS(order));
                deletedOrderList.add(order);
                count++;
            }
        }

        Orderable[] deletedOrders = deletedOrderList.toArray(new Orderable[deletedOrderList.size()]);

        if (undoable) {
            if (count > 1) {
                undoManager.addEdit(new UndoDeleteMultipleOrders(undoManager, deletedOrders));
            } else if (count == 1) {
                undoManager.addEdit(new UndoDeleteOrder(undoManager, deletedOrders[0]));
            }
        }


        if (count > 0) {
            clientFrame.fireMultipleOrdersDeleted(deletedOrders);
            clientFrame.fireStateModified();
            orderList.clearSelection();
        }

        return (count == orders.length);
    }// removeOrders()

    /**
     * Removes all orders for all powers within the orderablePowers group
     * (see ClientFrame for more information).
     *
     * @param undoable - <b>true</b> if this is an undoable action
     */
    public synchronized void removeAllOrders(boolean undoable) {
        Orderable[] deletedOrderArray = null;

        //synchronized(clientFrame.getLock())
        //{
            // clear the orders from the turnstate.
            // keep cleared orders in a temporary arraylist
            ArrayList<Orderable> deletedOrders = new ArrayList<>(100);
            for (Power orderablePower : orderablePowers) {
                List<Orderable> orders = turnState.getOrders(orderablePower);
                if (!orders.isEmpty()) {
                    deletedOrders.addAll(orders);
                    orders.clear();
                }
            }

            // create a temporary order array
            deletedOrderArray = deletedOrders.toArray(new Orderable[deletedOrders.size()]);
        //}


        // if we didn't actually delete anything, don't fire or create
        // any undo events.
        if (deletedOrderArray.length > 0) {
            // fire!
            clientFrame.fireMultipleOrdersDeleted(deletedOrderArray);

            if (undoable) {
                // Use "Clear All" as a display name. Note, though, that
                // it doesn't nescessarily mean that we clear *all* orders, though,
                // [depends upon orderablePowers setting]
                CompoundEdit ce = new UndoClearAll();
                ce.addEdit(new UndoDeleteMultipleOrders(undoManager, deletedOrderArray));
                ce.end();
                undoManager.addEdit(ce);
            }

            clientFrame.fireStateModified();
        }

        orderList.clearSelection();
    }// removeAllOrders()

    /**
     * Deletes the orders from the order list that are
     * selected, and that are members of the orderablePowers
     * group. This is <b>always</b> an undoable action.
     */
    public synchronized void removeSelected() {
        List<DisplayOrder> selected = orderList.getSelectedValuesList();
        if (selected.isEmpty()) {
            return;
        }

        Orderable[] selectedOrders = new Orderable[selected.size()];

        // selected objects correspond to those in the order list
        // they should all be DisplayOrder objects.
        for (int i = 0; i < selected.size(); i++) {
            selectedOrders[i] = (selected.get(i)).getOrder();
            removeOrderFromTS(selectedOrders[i]);
        }

        if (selectedOrders.length == 1) {
            undoManager.addEdit(new UndoDeleteOrder(undoManager, selectedOrders[0]));
        } else {
            undoManager.addEdit(new UndoDeleteMultipleOrders(undoManager, selectedOrders));
        }

        clientFrame.fireMultipleOrdersDeleted(selectedOrders);
        clientFrame.fireStateModified();
        orderList.clearSelection();
    }// removeSelected()

    /**
     * Select all orders in the list.
     */
    public void selectAll() {
        orderList.setSelectionInterval(0, orderListModel.getSize() - 1);
    }// selectAll()

    /**
     * Select none of the orders in the list
     */
    public void selectNone() {
        orderList.clearSelection();
    }// selectNone()

    /**
     * Force revalidation of all orders.
     */
    public void revalidateAllOrders() {
        orderListModel.revalidateAllOrders();
    }// revalidateAllOrders()

    /**
     * Refresh / revalidate the display
     */
    public void refresh() {
        orderList.repaint();
    }// refresh()

    /**
     * Set how orders are sorted. A SORT_ constant must be specified
     * for this to work properly. <code>null</code> values are
     * not permitted.
     */
    public void setSorting(String sortType, boolean reversed) {
        if (sortType != null) {
            DOComparator sortComparator = null;

            if (SORT_POWER.equals(sortType)) {
                sortComparator = new DOSortPower();
            } else if (SORT_PROVINCE.equals(sortType)) {
                sortComparator = new DOSortProvince();
            } else if (SORT_UNIT.equals(sortType)) {
                sortComparator = new DOSortUnit();
            } else if (SORT_ORDER.equals(sortType)) {
                sortComparator = new DOSortOrder();
            } else {
                throw new IllegalArgumentException("unsupported type");
            }

            sortComparator.setAscending(!reversed);
            orderListModel.sort(sortComparator);
        } else {
            throw new IllegalArgumentException();
        }
    }// setSorting()

    /**
     * Overriden to return the preferred size. This ensures that
     * we resize properly in a JSplitPane.
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(getPreferredSize());
    }// getMinimumSize()


    /**
     * Adds an order to the TurnState. If an order for that province
     * already exists, it is deleted, and returned by that message.
     * If no order exists for the province, the new order is added,
     * and we return null. If an order for a province exist,
     * only the first order encountered in list traversal will be
     * deleted.
     * <p>
     * <b>NOTE:</b> If the new order to add is the same (via equals())
     * as the order we are replacing, the turnstate is not modified.
     * The old order is still returned. Thus if the passed order is
     * equal to the returned order, it was an exact duplicate.
     * <p>
     * The main reason for this behavior is reduce rendering events
     * and undo/redo actions.
     */
    private Orderable addOrderToTS(Orderable order) {
        boolean isDuplicate = false;
        Orderable replacedOrder = null;

        //synchronized(clientFrame.getLock())
        //{
            List<Orderable> orders = turnState.getOrders(order.getPower());
            Iterator<Orderable> iter = orders.iterator();
            while (iter.hasNext()) {
                Orderable listOrder = iter.next();
                if (listOrder.getSource().isProvinceEqual(order.getSource())) {
                    replacedOrder = listOrder;
                    isDuplicate = listOrder.equals(order);

                    if (!isDuplicate) {
                        iter.remove();
                    }

                    break;
                }
            }

            // don't add a duplicate order, since we didn't remove it!
            if (!isDuplicate) {
                orders.add(order);
            }
        //}

        return replacedOrder;
    }// addOrderToTS()

    /**
     * Removes an order from the TurnState. If duplicate orders
     * for a unit (province) exist, only the first will be removed.
     * This returns <b>true</b> if the order was found and removed.
     */
    private boolean removeOrderFromTS(Orderable order) {
        //synchronized(clientFrame.getLock())
        //{
            List<Orderable> orders = turnState.getOrders(order.getPower());
            return orders.remove(order);
        //}
    }// removeOrderFromTS()


    /**
     * If power is not in the list of Orderable powers, return false.
     * Also return false if turnstate has been resolved.
     */
    private boolean isOrderable(Orderable order) {
        // we are reviewing orders
        if (turnState.isResolved()) {
            return false;
        }

        for (Power orderablePower : orderablePowers) {
            if (orderablePower == order.getPower()) {
                return true;
            }
        }

        return false;
    }// isOrderable()


    /**
     * Checks if player has issued too many build or remove orders; if so,
     * throws an OrderException.
     * <p>
     * Note that there is no way to check against too few adjustment orders being
     * issued, and that issuing too few Build orders is acceptable.
     * <p>
     * This only works during the Adjustment phase.
     */
    private void checkAdjustments(Orderable order)
            throws OrderException {
        Power power = order.getPower();
        boolean isReplacingOrder = false;

        List<Orderable> orders = turnState.getOrders(order.getPower());
        Iterator<Orderable> iter = orders.iterator();
        while (iter.hasNext()) {
            Orderable listOrder = iter.next();
            if (listOrder.getSource().isProvinceEqual(order.getSource())) {
                isReplacingOrder = true;
                break;
            }
        }

        if (adjMap != null) {
            Adjustment.AdjustmentInfo adjInfo = adjMap.get(power);
            int numOrders = turnState.getOrders(power).size();
            int max = Math.abs(adjInfo.getAdjustmentAmount());

            if (numOrders - (isReplacingOrder ? 1 : 0) >= max) {
                String dlgtext = MessageFormat.format(Utils.getText(Utils.getLocalString(DLG_TOOMANY_TEXT_LOCATION)),
                        power, max);

                throw new OrderException(dlgtext);
            }
        }
    }// checkAdjustments()

    /**
     * Perform layout, and create GUI elements for
     * sort buttons. No border is created around
     * the OrderDisplayPanel. Note that this is called
     * by the constructor.
     */
    protected void makeLayout() {
        // start layout
        int[] w1 = {0};
        int[] h1 = {0, 5, 0};    // 3 pixels between scroll list & sort buttons

        HIGLayout hl = new HIGLayout(w1, h1);
        hl.setColumnWeight(1, 1);
        hl.setRowWeight(1, 1);
        setLayout(hl);

        HIGConstraints c = new HIGConstraints();

        add(orderListScrollPane, c.rc(1, 1, "lrtb"));
        add(makeSortPanel(), c.rc(3, 1));
    }// makeLayout()

    /**
     * Makes the panel containing the sort buttons.
     * Defines actions for these buttons as well.
     */
    protected JPanel makeSortPanel() {
        // label
        JLabel label = new JLabel(Utils.getLocalString(LABEL_SORT));

        // combobox
        JComboBox<String> sortCombo = new JComboBox<>();
        sortCombo.setEditable(false);
        sortCombo.addItem(Utils.getLocalString(LABEL_SORT_POWER));
        sortCombo.addItem(Utils.getLocalString(LABEL_SORT_PROVINCE));
        sortCombo.addItem(Utils.getLocalString(LABEL_SORT_UNIT));
        sortCombo.addItem(Utils.getLocalString(LABEL_SORT_ORDER));
        sortCombo.setSelectedItem(Utils.getLocalString(LABEL_SORT_PROVINCE));
        sortCombo.addActionListener((ActionEvent e) -> {
            String item = (String) ((JComboBox<?>) e.getSource()).getSelectedItem();
            if (item.equals(Utils.getLocalString(LABEL_SORT_POWER))) {
                orderListModel.sort(new DOSortPower());
            } else if (item.equals(Utils.getLocalString(LABEL_SORT_PROVINCE))) {
                orderListModel.sort(new DOSortProvince());
            } else if (item.equals(Utils.getLocalString(LABEL_SORT_UNIT))) {
                orderListModel.sort(new DOSortUnit());
            } else if (item.equals(Utils.getLocalString(LABEL_SORT_ORDER))) {
                orderListModel.sort(new DOSortOrder());
            }
        });

        // layout
        JPanel sortPanel = new JPanel(null);
        sortPanel.setLayout(new BoxLayout(sortPanel, BoxLayout.X_AXIS));
        sortPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        sortPanel.add(label);
        sortPanel.add(Box.createHorizontalStrut(5));
        sortPanel.add(sortCombo);

        return sortPanel;
    }// makeSortPanel()

    /**
     * Property change listener
     */
    protected class ODPPropertyListener extends AbstractCFPListener {

        @Override
        public void actionOrderCreated(Orderable order) {
            orderListModel.addOrder(order);
        }// actionOrderCreated()

        @Override
        public void actionOrderDeleted(Orderable order) {
            orderListModel.removeOrder(order);
        }// actionOrderDeleted()

        @Override
        public void actionOrdersCreated(Orderable[] orders) {
            orderListModel.addOrders(orders);
        }// actionOrdersCreated()

        @Override
        public void actionOrdersDeleted(Orderable[] orders) {
            orderListModel.removeOrders(orders);
        }// actionOrdersDeleted()

        @Override
        public void actionOrderablePowersChanged(Power[] oldPowers, Power[] newPowers) {
            orderablePowers = newPowers;
        }// actionOrderablePowersChanged()

        @Override
        public void actionDisplayablePowersChanged(Power[] oldPowers, Power[] newPowers) {
            displayablePowers = newPowers;
            if (turnState != null) {
                orderListModel.updateFromTurnState();
            }
        }// actionDisplayablePowersChanged()

        @Override
        public void actionValOptsChanged(ValidationOptions options) {
            valOpts = options;
            if (turnState != null) {
                orderListModel.revalidateAllOrders();
            }
        }// actionValOptsChanged()

        @Override
        public synchronized void actionWorldCreated(World w) {
            world = w;
            undoManager = clientFrame.getUndoRedoManager();
            valOpts = clientFrame.getValidationOptions();
        }// actionWorldCreated()

        @Override
        public void actionWorldDestroyed(World w) {
            orderListModel.removeAllOrders();
            orderListModel.setSortComparator(new DOSortProvince());

            // we are now entering a dangerous state
            turnState = null;
            world = null;
            displayablePowers = null;
            orderablePowers = null;
        }// actionWorldDestroyed()

        @Override
        public void actionTurnstateChanged(TurnState ts) {
            turnState = ts;
            if (turnState.getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT) {
                adjMap = Adjustment.getAdjustmentInfo(turnState,
                        world.getRuleOptions(), world.getMap().getPowers());
            } else {
                adjMap = null;
            }

            orderListModel.updateFromTurnState();
            orderList.clearSelection();
        }// actionTurnstateChanged()

        @Override
        public synchronized void actionModeChanged(String newMode) {
            if (ClientFrame.MODE_ORDER.equals(newMode)) {
                isEditable = true;
                orderList.setEnabled(true);
                orderList.clearSelection();
                orderablePowers = clientFrame.getOrderablePowers();
                displayablePowers = clientFrame.getDisplayablePowers();

                if (turnState != null) {
                    orderListModel.updateFromTurnState();
                }
            } else if (ClientFrame.MODE_EDIT.equals(newMode)
                    || ClientFrame.MODE_NONE.equals(newMode)) {
                isEditable = false;
                orderList.setEnabled(false);
                orderList.clearSelection();
            } else {
                isEditable = false;
                displayablePowers = world.getMap().getPowers();
                orderablePowers = new Power[0];
                orderList.setEnabled(false);
                orderList.clearSelection();
            }

            orderListModel.activateMenu();

            if (undoManager != null) {
                undoManager.refreshMenu();
            }
        }// actionModeChanged()
    }// inner class ODPPropertyListener

    /**
     * Keeps a the list of orders that is displayed in the order list.
     * Handles sorting and updating of the list. No protection for
     * duplicate DisplayOrders occurs here. However, we do take the
     * displayablePowers into account.
     */
    private class OrderListModel extends AbstractListModel<DisplayOrder> {
        private final ArrayList<DisplayOrder> list;
        private DOComparator comparator;

        /**
         * Create an OrderListModel object
         */
        public OrderListModel() {
            list = new ArrayList<>(50);
            comparator = new DOSortProvince();    // default comparator
        }// OrderListModel()

        /**
         * Return the Size of the list.
         */
        public int getSize() {
            return list.size();
        }// getSize()

        /**
         * Returns the object (DisplayOrder) at the given index.
         */
        public DisplayOrder getElementAt(int index) {
            return list.get(index);
        }// getElementAt()

        /**
         * Set the Sort Comparator. Null comparators are not allowed.
         */
        public void setSortComparator(DOComparator comp) {
            if (comp == null) {
                throw new IllegalArgumentException("null comp");
            }

            synchronized (this) {
                this.comparator = comp;
            }
        }// setSortComparator()

        /**
         * Public, synchronized sort and update. This method
         * sets the comparator as well; if the same type of
         * comparator has already been set, it reverses
         * the direction sort direction.
         * <p>
         * A null comparator is not allowed.
         */
        public void sort(DOComparator comp) {
            if (comp == null) {
                throw new IllegalArgumentException("null comp");
            }

            synchronized (this) {
                if (comparator.equals(comp)) {
                    comparator.setAscending(!comparator.isAscending());
                } else {
                    this.comparator = comp;
                }
            }

            synchronized (list) {
                sort();
            }
        }// sort()


        /**
         * Sort the list, and fires a ContentsChanged message so the list is updated.
         */
        private void sort() {
            synchronized (this) {
                Collections.sort(list, comparator);                // sort
                comparator.setHighlighting(list.iterator());    // mark hilites
                fireContentsChanged(this, 0, getSize() - 1);    // update entire list
                activateMenu();
            }
        }// sort()

        /**
         * Adds a DisplayOrder to the list. Only an order
         * that is in the list of Powers that are allowed to be displayed
         * will be added to the list.
         */
        public void addOrder(Orderable order) {
            if (isDisplayable(order)) {
                synchronized (list) {
                    list.add(createDisplayOrder(order));
                    sort();
                }
            }
        }// addDisplayOrder()

        /**
         * Adds multiple DisplayOrders to the list. Only orders
         * that are in the list of Powers that are allowed to be displayed
         * are added to the list.
         */
        public void addOrders(Orderable[] orders) {
            int addCount = 0;

            synchronized (list) {
                for (Orderable order : orders) {
                    if (isDisplayable(order)) {
                        list.add(createDisplayOrder(order));
                        addCount++;
                    }
                }

                // no updating required iff no orders added
                if (addCount > 0) {
                    sort();
                }
            }
        }// addDisplayOrders()

        /**
         * Removes all DisplayOrders matching this order.
         */
        public void removeOrder(Orderable order) {
            // note: if duplicates are present, this will remove duplicates
            // no displayed-power checking is required here
            synchronized (list) {
                Iterator<DisplayOrder> iter = list.iterator();
                while (iter.hasNext()) {
                    DisplayOrder displayOrder = iter.next();
                    if (displayOrder.getOrder() == order) {
                        iter.remove();
                    }
                }

                sort();
            }
        }// removeDisplayOrder()

        /**
         * Removes all DisplayOrder matching the given array of orders.
         */
        public void removeOrders(final Orderable[] orders) {
            // note: if duplicates are present, this will remove duplicates
            // no displayed-power checking is required here
            synchronized (list) {
                Iterator<DisplayOrder> iter = list.iterator();
                while (iter.hasNext()) {
                    DisplayOrder displayOrder = iter.next();
                    final Orderable doOrder = displayOrder.getOrder();

                    for (Orderable order : orders) {
                        if (doOrder == order) {
                            iter.remove();
                        }
                    }
                }

                sort();
            }
        }// removeDisplayOrders()

        /**
         * Removes all DisplayOrders from the list
         */
        public void removeAllOrders() {
            synchronized (list) {
                list.clear();
                sort();
            }
        }// removeAllDisplayOrders()

        /**
         * Update displayed powers from TurnState
         */
        public void updateFromTurnState() {
            // when the displayed powers change, we need to completely
            // recreate the displayed power list from the turnstate, adding
            // only the 'allowed' powers.
            //
            assert (turnState != null);

            synchronized (list) {
                list.clear();

                for (Orderable order : turnState.getAllOrders()) {
                    if (isDisplayable(order)) {
                        list.add(createDisplayOrder(order));
                    }
                }

                sort();
            }
        }// updateFromTurnState()


        /**
         * Revalidate all displayed orders
         */
        public void revalidateAllOrders() {
            synchronized (list) {
                for (DisplayOrder displayOrder : list) {
                    try {
                        displayOrder.getOrder().validate(turnState, valOpts, world.getRuleOptions());
                        displayOrder.setInvalid(false);
                    } catch (OrderException e) {
                        displayOrder.setInvalid(true);
                    }
                }

                sort();
            }
        }// revalidateAllOrders()

        /**
         * Activates the edit menu, depending on if we are in an
         * editable state and if there are any items in the order list.
         */
        public void activateMenu() {
            clientFrame.getClientMenu().setEditItemsEnabled(((getSize() > 0) && isEditable));
        }// activateMenu()


        /**
         * Checks if an Order is in the Displayed Power array.
         */
        private boolean isDisplayable(final Orderable order) {
            for (Power displayablePower : displayablePowers) {
                if (order.getPower() == displayablePower) {
                    return true;
                }
            }

            return false;
        }// isDisplayable()

        /**
         * Creates a DisplayOrder and sets whether it is valid
         * and/or if it has failed.
         */
        private DisplayOrder createDisplayOrder(Orderable order) {
            DisplayOrder displayOrder = new DisplayOrder(order);

            // validate
            try {
                order.validate(turnState, valOpts, world.getRuleOptions());
            } catch (OrderException e) {
                displayOrder.setInvalid(true);
            }

            // see if failed [only applies when turnstate is resolved]
            if (turnState.isResolved()) {
                displayOrder.setFailed(!turnState.isOrderSuccessful(order));
            }

            return displayOrder;
        }// createDisplayOrder()
    }// inner class OrderListModel

    /**
     * Custom List Renderer that is used to render the background
     * highlights created by sorting.
     */
    private class OrderListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // set background color, if we are not selected
            if (!isSelected) {
                if (((DisplayOrder) value).isHighlighted()) {
                    setBackground(BG_HILITE);
                } else {
                    setBackground(BG_DEFAULT);
                }
            }

            // set text, w/ or w/o conversion, depending upon if we are unicode-aware
            Font f = component.getFont();
            if (!f.canDisplay('\u2192')) {
                // search for unicode-arrow; replace with "->"
                String text = this.getText();
                if (text != null) {
                    StringBuilder buffer = new StringBuilder(text);
                    boolean isChanged = false;

                    for (int i = buffer.length() - 1; i >= 0; i--) {
                        final char c = buffer.charAt(i);

                        if (c == '\u2192') {
                            buffer.deleteCharAt(i);
                            buffer.insert(i, "->");
                            isChanged = true;
                        }
                    }

                    if (isChanged) {
                        this.setText(buffer.toString());
                    }
                }
            }

            return component;
        }// getListCellRendererComponent()

        /**
         * Overridden for performance
         */
        @Override
        public void invalidate() {
        }

        /**
         * Overriden for performance
         */
        @Override
        public void repaint() {
        }

    }// inner class OrderListRenderer

    /**
     * One DisplayOrder object is created for each order in the
     * TurnState that is displayed (i.e., in displayablePowers).
     * <p>
     * The Orderable object of a DisplayOrder is immutable.
     */
    private final class DisplayOrder {
        // instance variables
        private final Orderable order;
        private boolean isInvalid = false;
        private boolean isFailed = false;
        private boolean isHilite = false;

        /**
         * Create a DisplayOrder
         */
        public DisplayOrder(Orderable order) {
            if (order == null) {
                throw new IllegalArgumentException("null order");
            }

            this.order = order;
        }// DisplayOrder()

        /**
         * Get the Order
         */
        public Orderable getOrder() {
            return order;
        }

        /**
         * Returns if the order is invalid (failed validation)
         */
        public boolean isInvalid() {
            return isInvalid;
        }

        /**
         * Set if the order is invalid (failed validation)
         */
        public void setInvalid(boolean value) {
            isInvalid = value;
        }

        /**
         * Returns if the order should be highlighted
         */
        public boolean isHighlighted() {
            return isHilite;
        }

        /**
         * Set if the order should be highlighted
         */
        public void setHighlighted(boolean value) {
            isHilite = value;
        }

        /**
         * Set if the order failed
         */
        public void setFailed(boolean value) {
            isFailed = value;
        }

        /**
         * Return HTML formatted text to display.
         */
        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("<html>");

            if (isInvalid) {
                sb.append("<font color=\"#EE0000\">");
            }

            if (isFailed) {
                sb.append("<u>");
            }

            sb.append(order.toFormattedString(clientFrame.getOFO()));

            // we do not put closing tags for <font>, <u>, or <html>

            return sb.toString();
        }// toString()
    }// inner class DisplayOrder

    /**
     * DOComparator class offers highlight-marking support
     */
    private abstract class DOComparator implements Comparator<DisplayOrder> {
        private boolean isAscending = true;

        /**
         * Return the object we need to compare to make setHighlighting work.
         */
        protected abstract Object getComparisonObject(DisplayOrder displayedOrder);

        /**
         * The method we need to make compare() work
         */
        protected abstract int compareDisplayOrders(DisplayOrder do1, DisplayOrder do2);

        /**
         * The compare method. This essentially returns the result of
         * compareDisplayOrders() unless the sort is reversed.
         */
        public final int compare(DisplayOrder o1, DisplayOrder o2) {
            final int result = compareDisplayOrders(o1, o2);
            return ((isAscending) ? result : -result);
        }// compare()

        /**
         * Returns if the sort order is ascending or not
         */
        public final boolean isAscending() {
            return isAscending;
        }

        /**
         * Sets the sort direction. Ascending by default.
         */
        public final void setAscending(boolean value) {
            isAscending = value;
        }

        /**
         * Mark the highlighted items in a collection of orders.
         * NOTE: the Iterator must return DisplayOrder objects.
         */
        public void setHighlighting(Iterator<DisplayOrder> iter) {
            boolean toHilite = true;
            Object lastObject = null;

            if (iter.hasNext()) {
                DisplayOrder first = iter.next();
                first.setHighlighted(toHilite);
                lastObject = getComparisonObject(first);
            }

            while (iter.hasNext()) {
                DisplayOrder next = iter.next();
                Object nextObject = getComparisonObject(next);

                if (!lastObject.equals(nextObject)) {
                    toHilite = !toHilite;
                    lastObject = nextObject;
                }

                next.setHighlighted(toHilite);
            }
        }// setHighlighting()
    }// inner abstract class DOComparator

    /**
     * Comparator that sorts DisplayOrder by Power
     */
    private class DOSortPower extends DOComparator {
        /**
         * Determine if we are the same Comparator type
         */
        public boolean equals(Object obj) {
            return (obj instanceof DOSortPower);
        }// equals()

        /**
         * DOComparator Implementation. Passed parameters are
         * assumed to be DisplayOrder objects.
         */
        protected int compareDisplayOrders(DisplayOrder do1, DisplayOrder do2) {
            Power p1 = do1.getOrder().getPower();
            Power p2 = do2.getOrder().getPower();

            return p1.compareTo(p2);
        }// compare()

        protected Object getComparisonObject(DisplayOrder displayedOrder) {
            return displayedOrder.getOrder().getPower();
        }// getComparisonObject()
    }// inner class DOSortPower

    /**
     * Comparator that sorts DisplayOrder by Province
     */
    private class DOSortProvince extends DOComparator {
        /**
         * Determine if we are the same Comparator type
         */
        public boolean equals(Object obj) {
            return (obj instanceof DOSortProvince);
        }// equals()

        /**
         * DOComparator Implementation.
         */
        protected int compareDisplayOrders(DisplayOrder do1, DisplayOrder do2) {
            Province pr1 = do1.getOrder().getSource().getProvince();
            Province pr2 = do2.getOrder().getSource().getProvince();

            return pr1.compareTo(pr2);
        }// compare()

        /**
         * DOComparator Implementation.
         */
        protected Object getComparisonObject(DisplayOrder displayedOrder) {
            return displayedOrder.getOrder().getSource().getProvince();
        }// getComparisonObject()
    }// inner class DOSortProvince

    /**
     * Comparator that sorts DisplayOrder by Unit
     */
    private class DOSortUnit extends DOComparator {
        /**
         * Determine if we are the same Comparator type
         */
        public boolean equals(Object obj) {
            return (obj instanceof DOSortUnit);
        }// equals()

        /**
         * DOComparator Implementation.
         */
        protected int compareDisplayOrders(DisplayOrder do1, DisplayOrder do2) {
            String name1 = do1.getOrder().getSourceUnitType().getFullName();
            String name2 = do2.getOrder().getSourceUnitType().getFullName();

            return name1.compareTo(name2);
        }// compare()

        /**
         * DOComparator Implementation.
         */
        protected Object getComparisonObject(DisplayOrder displayedOrder) {
            return displayedOrder.getOrder().getSourceUnitType();
        }// getComparisonObject()
    }// inner class DOSortUnit

    /**
     * Comparator that sorts DisplayOrder by Order Type
     */
    private class DOSortOrder extends DOComparator {
        /**
         * Determine if we are the same Comparator type
         */
        public boolean equals(Object obj) {
            return (obj instanceof DOSortOrder);
        }// equals()

        /**
         * DOComparator Implementation.
         */
        protected int compareDisplayOrders(DisplayOrder do1, DisplayOrder do2) {
            String ordName1 = do1.getOrder().getFullName();
            String ordName2 = do2.getOrder().getFullName();

            return ordName1.compareTo(ordName2);
        }// compare()

        /**
         * DOComparator Implementation.
         */
        protected Object getComparisonObject(DisplayOrder displayedOrder) {
            return displayedOrder.getOrder().getBriefName();
        }// getComparisonObject()
    }// inner class DOSortOrder


}// class OrderDisplayPanel
