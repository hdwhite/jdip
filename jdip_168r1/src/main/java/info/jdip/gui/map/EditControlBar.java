//
//  @(#)EditControlBar.java		4/2002
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
package info.jdip.gui.map;

import info.jdip.gui.OrderDisplayPanel;
import info.jdip.gui.order.GUIOrder;
import info.jdip.gui.undo.UndoEditAddUnit;
import info.jdip.gui.undo.UndoEditRemoveUnit;
import info.jdip.gui.undo.UndoEditSCOwner;
import info.jdip.gui.undo.UndoRedoManager;
import info.jdip.misc.Utils;
import info.jdip.order.result.TimeResult;
import info.jdip.world.Coast;
import info.jdip.world.Location;
import info.jdip.world.Phase;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import org.w3c.dom.events.MouseEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;


/**
 * Creates a ControlBar that allows units to be placed or removed, Supply Centers to
 * change ownership.
 * <p>
 */
public class EditControlBar extends ViewControlBar {
    // i18n constants
    private static final String POWER_LABEL = "EdtConBar.label.power";
    private static final String BUTTON_TEXT_ARMY = "EdtConBar.button.text.army";
    private static final String BUTTON_TEXT_FLEET = "EdtConBar.button.text.fleet";
    private static final String BUTTON_TEXT_WING = "EdtConBar.button.text.wing";
    private static final String BUTTON_TEXT_OWNER = "EdtConBar.button.text.owner";
    private static final String BUTTON_TEXT_REMOVE = "EdtConBar.button.text.remove";
    private static final String TOOLTIP_ARMY = "EdtConBar.tooltip.army";
    private static final String TOOLTIP_FLEET = "EdtConBar.tooltip.fleet";
    private static final String TOOLTIP_WING = "EdtConBar.tooltip.wing";
    private static final String TOOLTIP_OWNER = "EdtConBar.tooltip.owner";
    private static final String TOOLTIP_REMOVE = "EdtConBar.tooltip.remove";
    private static final String BUTTON_TEXT_DISLODGED = "EdtConBar.button.text.dislodged";
    private static final String TOOLTIP_DISLODGED = "EdtConBar.tooltip.dislodged";
    private static final String EDIT_TIME_STAMP_MSG = "TimeResult.game.edited";

    private static final String ERR_NO_ARMY_IN_SEA = "EdtConBar.err.noarmyinsea";
    private static final String ERR_NO_FLEET_IN_LANDLOCKED = "EdtConBar.err.nofleetland";
    private static final String ERR_NO_UNIT_TO_REMOVE = "EdtConBar.err.nounit_remove";
    private static final String ERR_NO_SC = "EdtConBar.err.no_sc";
    private static final String CLICK_TO_SET_OWNER = "EdtConBar.click.set_owner";
    private static final String CLICK_TO_REMOVE = "EdtConBar.click.remove";
    private static final String CLICK_TO_ADD_FLEET = "EdtConBar.click.add_fleet";
    private static final String CLICK_TO_ADD_ARMY = "EdtConBar.click.add_army";
    private static final String CLICK_TO_ADD_WING = "EdtConBar.click.add_wing";
	/* no longer used
	private static final String ERR_DISLODGED_UNIT_MUST_BE_REMOVED	= "EdtConBar.err.remove_dislodged";
	private static final String ERR_UNIT_MUST_BE_REMOVED			= "EdtConBar.err.remove_unit";
	private static final String ERR_FLEET_MUST_BE_ON_COAST			= "EdtConBar.err.fleetcoastal";
	private static final String ERR_NO_DISLODGED_UNIT_TO_REMOVE		= "EdtConBar.err.nounit_remove_dislodged";
	private static final String CLICK_TO_REMOVE_DISLODGED			= "EdtConBar.click.remove_dislodged";
	private static final String CLICK_TO_ADD_DISLODGED_FLEET		= "EdtConBar.click.add_dislodged_fleet";
	private static final String CLICK_TO_ADD_DISLODGED_ARMY			= "EdtConBar.click.add_dislodged_army";
	private static final String CLICK_TO_ADD_DISLODGED_WING			= "EdtConBar.click.add_dislodged_wing";
	*/


    // 'no power' list item
    private static final String POWER_NONE = Utils.getLocalString("EdtConBar.list.nopower");

    // instance variables
    private static java.awt.Cursor defaultCursor;
    private final Position position;
    private final TurnState turnState;
    private final UndoRedoManager undoManager;
    private final OrderDisplayPanel orderDisplayPanel;

    private Power currentPower = null;
    private JToggleButton selectedButton = null;
    private boolean didEdit = false;

    // Mouse button handling
    private String currentAction = null;
    private String defaultAction = null;

    // GUI elements
    private JComboBox<Object> powerBox;  //todo: this should not be so generic...
    private JToggleButton bSC;
    private JToggleButton bArmy;
    private JToggleButton bFleet;
    private JToggleButton bWing;        // may be null
    private JToggleButton bRemove;
    private JCheckBox cbDislodged;    // not available if not in retreat phase
    private ButtonGroup bg;


    /**
     * Create an EditControlBar
     */
    public EditControlBar(MapPanel mp) {
        super(mp);
        defaultCursor = java.awt.Cursor.getDefaultCursor();
        position = mapPanel.getPosition();
        turnState = mapPanel.getTurnState();
        orderDisplayPanel = mapPanel.getClientFrame().getOrderDisplayPanel();
        undoManager = mapPanel.getClientFrame().getUndoRedoManager();
        makeLayout();
    }// ViewControlBar()


    /**
     * Do the layout
     */
    private void makeLayout() {
        addSeparator();
        addSeparator(new Dimension(10, 0));

        // create power label, and create combo box with all powers + "none"
        add(new JLabel(Utils.getLocalString(POWER_LABEL)));
        addSeparator(new Dimension(5, 0));
        powerBox = new JComboBox<>(mapPanel.getClientFrame().getWorld().getMap().getPowers());
        powerBox.insertItemAt(POWER_NONE, 0);
        powerBox.setEditable(false);
        powerBox.addItemListener((ItemEvent e) -> {
            currentPower = getSelectedPower();

            if (currentPower == null) {
                // deselect & disable Army / Fleet buttons
                bArmy.setEnabled(false);
                bFleet.setEnabled(false);
                mapPanel.getJSVGCanvas().setCursor(defaultCursor);
            } else {
                bArmy.setEnabled(true);
                bFleet.setEnabled(true);
            }
        });

        powerBox.setMaximumSize(powerBox.getPreferredSize());

        add(powerBox);

        // Toggle-Button Listener
        ToggleListener tl = new ToggleListener();

        // button group
        bg = new ButtonGroup();

        // create buttons
        addSeparator(new Dimension(10, 0));
        bArmy = new JToggleButton(Utils.getLocalString(BUTTON_TEXT_ARMY));
        bArmy.setToolTipText(Utils.getLocalString(TOOLTIP_ARMY));
        bArmy.addActionListener(tl);
        add(bArmy);
        bg.add(bArmy);

        bFleet = new JToggleButton(Utils.getLocalString(BUTTON_TEXT_FLEET));
        bFleet.setToolTipText(Utils.getLocalString(TOOLTIP_FLEET));
        bFleet.addActionListener(tl);
        add(bFleet);
        bg.add(bFleet);

        // if WING units enabled, add a WING unit button
        RuleOptions ro = mapPanel.getWorld().getRuleOptions();
        if (ro.getOptionValue(RuleOptions.OPTION_WINGS) == RuleOptions.VALUE_WINGS_ENABLED) {
            bWing = new JToggleButton(Utils.getLocalString(BUTTON_TEXT_WING));
            bWing.setToolTipText(Utils.getLocalString(TOOLTIP_WING));
            bWing.addActionListener(tl);
            add(bWing);
            bg.add(bWing);
        }


        bSC = new JToggleButton(Utils.getLocalString(BUTTON_TEXT_OWNER));
        bSC.setToolTipText(Utils.getLocalString(TOOLTIP_OWNER));
        bSC.addActionListener(tl);
        add(bSC);
        bg.add(bSC);

        bRemove = new JToggleButton(Utils.getLocalString(BUTTON_TEXT_REMOVE));
        bRemove.setToolTipText(Utils.getLocalString(TOOLTIP_REMOVE));
        bRemove.addActionListener(tl);
        add(bRemove);
        bg.add(bRemove);


        cbDislodged = new JCheckBox(Utils.getLocalString(BUTTON_TEXT_DISLODGED), false);
        cbDislodged.setToolTipText(Utils.getLocalString(TOOLTIP_DISLODGED));
        addSeparator(new Dimension(5, 0));

        // do not add dislodged if we are not in a retreat phase.
        if (turnState.getPhase().getPhaseType() == Phase.PhaseType.RETREAT) {
            addSeparator();
            addSeparator(new Dimension(5, 0));
            add(cbDislodged);
        }

        // set current Power
        currentPower = getSelectedPower();
    }// makeLayout()


    /**
     * Get the selected power from the combo box, null if "none" selected.
     */
    private Power getSelectedPower() {
        Object obj = powerBox.getSelectedItem();
        if (obj == POWER_NONE) {
            return null;
        } else {
            return (Power) obj;
        }
    }// getSelectedPower();


    /**
     * Determines if a Province is click-worthy, depending upon the selected mode.
     * <p>
     * Adds a brief status bar message as to why a click will or will not be accepted.
     */
    public void mouseOver(MouseEvent me, Location loc) {
        // by default, can't accept
        mapPanel.getJSVGCanvas().setCursor(MapPanel.BAD_ACTION);

        // bad location
        if (loc == null) {
            mapPanel.getStatusBarUtils().setText(Utils.getLocalString(GUIOrder.NOT_IN_PROVINCE));
            return;
        }


        if (currentAction != null) {
            if (checkValidity(loc)) {
                mapPanel.getJSVGCanvas().setCursor(defaultCursor);
                mapPanel.getStatusBarUtils().displayProvinceInfo(loc, Utils.getLocalString(currentAction));
            }
        } else {
            mapPanel.getStatusBarUtils().displayProvinceInfo(loc);
        }
    }// mouseOver()


    /**
     * Handles mouseOut()
     */
    public void mouseOut(MouseEvent me, Location loc) {
        mapPanel.getStatusBar().clearText();
        mapPanel.getJSVGCanvas().setCursor(defaultCursor);
    }// mouseOver()


    /**
     * Handle mouse clicks on the map
     */
    public void mouseClicked(MouseEvent me, Location loc) {
        if (loc != null) {
            if (DOMUIEventListener.isRMBorMetaLMB(me)) {
                // make RMB add the 'other unit' that is selected,
                // if an army or fleet is selected. if a wing or anything
                // else is selected, RMB doesn't apply
                if (CLICK_TO_ADD_FLEET.equals(currentAction)) {
                    currentAction = CLICK_TO_ADD_ARMY;
                } else if (CLICK_TO_ADD_ARMY.equals(currentAction)) {
                    currentAction = CLICK_TO_ADD_FLEET;
                } else {
                    currentAction = defaultAction;
                }
            } else if (DOMUIEventListener.isMMBorControlLMB(me)) {
                currentAction = CLICK_TO_REMOVE;
            } else {
                currentAction = defaultAction;
            }

            doAction(me, loc);
        }
    }// mouseClicked()


    public void doAction(MouseEvent me, Location loc) {
        // bad location
        if (loc == null) {
            mapPanel.getStatusBarUtils().setText(Utils.getLocalString(GUIOrder.NOT_IN_PROVINCE));
            return;
        }

        if (checkValidity(loc)) {
            Province province = loc.getProvince();
            // Removes first
            if ((CLICK_TO_ADD_FLEET.equals(currentAction) ||
                    CLICK_TO_ADD_ARMY.equals(currentAction) ||
                    CLICK_TO_ADD_WING.equals(currentAction) ||
                    CLICK_TO_REMOVE.equals(currentAction)) &&
                hasUnit(loc.getProvince())) {
                // get old unit
                Unit oldUnit = (isDislodged()) ? position.getDislodgedUnit(province) : position.getUnit(province);

                // remove an army or fleet
                removeUnit(province, isDislodged());
                undoManager.addEdit(new UndoEditRemoveUnit(undoManager, position, province, oldUnit, isDislodged()));
            }

            if (CLICK_TO_ADD_ARMY.equals(currentAction)) {
                // add an army
                Unit army = new Unit(currentPower, Unit.Type.ARMY);
                army.setCoast(Coast.NONE);
                addUnit(province, army, isDislodged());

                undoManager.addEdit(new UndoEditAddUnit(undoManager, position, province, army, isDislodged()));
            }
            if (CLICK_TO_ADD_FLEET.equals(currentAction)) {
                // add a fleet
                Unit fleet = new Unit(currentPower, Unit.Type.FLEET);
                if (province.isMultiCoastal()) {
                    Coast coast = loc.getCoast();
                    if (!coast.isDirectional()) {
                        return;
                    }
                    fleet.setCoast(coast);
                } else {
                    fleet.setCoast(Coast.SINGLE);
                }

                addUnit(province, fleet, isDislodged());
                undoManager.addEdit(new UndoEditAddUnit(undoManager, position, province, fleet, isDislodged()));
            }
            if (CLICK_TO_ADD_WING.equals(currentAction)) {
                // add a Wing
                Unit wing = new Unit(currentPower, Unit.Type.WING);
                wing.setCoast(Coast.WING);
                addUnit(province, wing, isDislodged());

                undoManager.addEdit(new UndoEditAddUnit(undoManager, position, province, wing, isDislodged()));
            }
            if (CLICK_TO_SET_OWNER.equals(currentAction)) {
                // change supply center ownership
                Power oldPower = position.getSupplyCenterOwner(province);
                changeSCOwner(province, currentPower);
                undoManager.addEdit(new UndoEditSCOwner(undoManager, position, province, oldPower, currentPower));
            }
        }

        // add edit result if we haven't already
        if (!didEdit) {
            turnState.getResultList().add(new TimeResult(EDIT_TIME_STAMP_MSG));
            didEdit = true;
        }

        // re-call mouseOver to avoid successive clicks
        mouseOver(me, loc);
    }// mouseClicked()

    /**
     * Add a unit to a province; does not generate an undo/redo event. Revalidates orders.
     */
    public void addUnit(Province province, Unit unit, boolean isDislodged) {
        if (isDislodged) {
            position.setDislodgedUnit(province, unit);
        } else {
            position.setUnit(province, unit);
        }

        update(province);
        orderDisplayPanel.revalidateAllOrders();
    }// addUnit()


    /**
     * Remove a unit from a province; does not generate an undo/redo event. Revalidates orders.
     */
    public void removeUnit(Province province, boolean isDislodged) {
        if (isDislodged) {
            position.setDislodgedUnit(province, null);
        } else {
            position.setUnit(province, null);
        }

        update(province);
        orderDisplayPanel.revalidateAllOrders();
    }// removeUnit()


    /**
     * Change the supply center owner of a province; does not generate an undo/redo event. Revalidates orders.
     */
    public void changeSCOwner(Province province, Power newPower) {
        if(province.hasSupplyCenter()) {
            position.setSupplyCenterOwner(province, newPower);
        }
        position.setLastOccupier(province, newPower);

        update(province);
        orderDisplayPanel.revalidateAllOrders();
    }// changeSCOwner()


    /**
     * re-render SVG, set changed flag on game state
     */
    private void update(Province province) {
        mapPanel.getClientFrame().fireStateModified();
        mapPanel.updateProvince(province);
    }// update()


    /**
     * convenience method to check dislodged checkbox
     */
    private boolean isDislodged() {
        return cbDislodged.isSelected();
    }// isDislodged()


    /**
     * convenience method to check if a Province has a unit (or dislodged unit, if isDislodged()==true)
     */
    private boolean hasUnit(Province p) {
        if (isDislodged()) {
            return position.hasDislodgedUnit(p);
        } else {
            return position.hasUnit(p);
        }
    }// hasUnit()

    public boolean checkValidity(Location loc) {
        // determine validity
        Province province = loc.getProvince();
        if (CLICK_TO_ADD_ARMY.equals(currentAction) && currentPower != null) {
            if (province.isSea()) {
                mapPanel.statusBarUtils.displayProvinceInfo(loc, Utils.getLocalString(ERR_NO_ARMY_IN_SEA));
                return false;
            }
            return true;
        }
        
        if (CLICK_TO_ADD_FLEET.equals(currentAction) && currentPower != null) {
            if (province.isLandLocked()) {
                mapPanel.statusBarUtils.displayProvinceInfo(loc, Utils.getLocalString(ERR_NO_FLEET_IN_LANDLOCKED));
                return false;
            }
            return true;
        }
        
        if (CLICK_TO_ADD_WING.equals(currentAction) && currentPower != null) {
            return true;
        }
        
        if (CLICK_TO_SET_OWNER.equals(currentAction)) {
            if (!province.hasSupplyCenter() && province.isSea()) {
                mapPanel.statusBarUtils.displayProvinceInfo(loc, Utils.getLocalString(ERR_NO_SC));
                return false;
            }
            return true;
        }
        
        if (CLICK_TO_REMOVE.equals(currentAction)) {
            if (!position.hasUnit(province)) {
                mapPanel.statusBarUtils.displayProvinceInfo(loc, Utils.getLocalString(ERR_NO_UNIT_TO_REMOVE));
                return false;
            }
            return true;
        }

        return false;
    }// checkValidity()

    /**
     * Listens for toggle events; sets which button is selected
     */
    private class ToggleListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            selectedButton = (JToggleButton) e.getSource();
            if (selectedButton == bArmy) {
                defaultAction = CLICK_TO_ADD_ARMY;
            } else if (selectedButton == bWing) {
                defaultAction = CLICK_TO_ADD_WING;
            } else if (selectedButton == bFleet) {
                defaultAction = CLICK_TO_ADD_FLEET;
            } else if (selectedButton == bRemove) {
                powerBox.setSelectedItem(POWER_NONE);
                defaultAction = CLICK_TO_REMOVE;
            } else if (selectedButton == bSC) {
                defaultAction = CLICK_TO_SET_OWNER;
            }

            currentAction = defaultAction;

            // disable dislodged checkbox, iff Supply Center button selected,
            // since supply center ownership has no relationship to dislodged
            cbDislodged.setEnabled(selectedButton != bSC);
        }// actionPerformed()
    }// inner class ToggleListener

}// EditControlBar
