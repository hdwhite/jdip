//
//  @(#)XJScrollPane.java	1/2004
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
package info.jdip.gui.swing;

import info.jdip.misc.Utils;

import javax.swing.*;
import java.awt.*;

/**
 * eXtended JScrollPane.
 * <p>
 * Modified to always show scrollbars (both) on Mac OS X. Other platforms
 * default to AS_NEEDED.
 */
public class XJScrollPane extends JScrollPane {
    private static final int DEFAULT_HSB_POLICY = ((Utils.isOSX()) ?
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private static final int DEFAULT_VSB_POLICY = ((Utils.isOSX()) ?
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    /**
     * Create an XJScrollPane
     */
    public XJScrollPane() {
        super(DEFAULT_VSB_POLICY, DEFAULT_HSB_POLICY);
    }// XJScrollPane()

    /**
     * Create an XJScrollPane
     */
    public XJScrollPane(Component view) {
        super(view, DEFAULT_VSB_POLICY, DEFAULT_HSB_POLICY);
    }// XJScrollPane()

}// class XJScrollPane
