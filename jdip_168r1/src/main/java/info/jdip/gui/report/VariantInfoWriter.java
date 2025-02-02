//
//  @(#)VariantInfoWriter.java		5/2004
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
package info.jdip.gui.report;

import info.jdip.gui.ClientFrame;
import info.jdip.gui.dialog.TextViewer;
import info.jdip.misc.Utils;
import info.jdip.world.RuleOptions;
import info.jdip.world.World;
import info.jdip.world.variant.VariantManager;
import info.jdip.world.variant.data.Variant;

import javax.swing.*;
import java.util.Set;


/**
 * Writes information about the current Variant as HTML, and, also
 * displays the current rule settings (RuleOptions).
 */
public class VariantInfoWriter {
    // i18n constants
    private static final String TEMPLATE_LOCATION = "VariantInfoWriter.template.location";
    private static final String DIALOG_TITLE = "VariantInfoWriter.dialog.title";

    // instance fields
    private World world = null;


    /**
     * StateWriter constructor
     */
    private VariantInfoWriter(World w) {
        this.world = w;
    }// VariantInfoWriter()

    /**
     * Gets information about the variant and current rule
     * options. Result is in HTML.
     */
    public static String variantInfoToHTML(World w) {
        VariantInfoWriter viw = new VariantInfoWriter(w);
        return viw.getInfoAsHTML();
    }// stateToHTML()

    /**
     * Returns the HTML-encoded variant information inside a dialog.
     */
    public static void displayDialog(final ClientFrame clientFrame,
                                     final World w) {
        TextViewer tv = new TextViewer(clientFrame);
        tv.setEditable(false);
        tv.addSingleButton(tv.makeOKButton());
        tv.setTitle(Utils.getLocalString(DIALOG_TITLE));
        tv.setHeaderVisible(false);
        tv.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        tv.lazyLoadDisplayDialog(new TextViewer.TVRunnable() {
            public void run() {
                setText(variantInfoToHTML(w));
            }
        });
    }// displayDialog()

    /**
     * Writes info as HTML
     */
    private String getInfoAsHTML() {
        // get template
        String template = Utils.getText(Utils.getLocalString(TEMPLATE_LOCATION));
        if (template == null) {
            template = "ERROR: missing template resource " + Utils.getLocalString(TEMPLATE_LOCATION);
        }

        // get selcted variant
        World.VariantInfo vi = world.getVariantInfo();
        Variant variant = VariantManager.getVariant(vi.getVariantName(), vi.getVariantVersion());

        // get 8 main arguments
        Object[] oldArgs = variant.getHTMLSummaryArguments();

        // make extra space
        Object[] newArgs = new Object[oldArgs.length + 1];
        System.arraycopy(oldArgs, 0, newArgs, 0, oldArgs.length);

        // make 9th argument: 'rules settings'
        StringBuilder sb = new StringBuilder(256);

        RuleOptions ro = world.getRuleOptions();
        Set<RuleOptions.Option> allOpts = ro.getAllOptions();
        for (RuleOptions.Option opt : allOpts) {
            RuleOptions.OptionValue optVal = ro.getOptionValue(opt);

            sb.append("<p><b>");
            sb.append(opt.getNameI18N());
            sb.append(":</b> ");
            sb.append(optVal.getNameI18N());
            sb.append('\n');
        }

        newArgs[newArgs.length - 1] = sb.toString();

        // return text.
        return Utils.format(template, newArgs);
    }// getInfoAsHTML()


}// class VariantInfoWriter
