/*  Copyright (C) 2004  Ryan Michela
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.nukesoft.jdipFacade;

import dip.world.InvalidWorldException;
import dip.world.RuleOptions;
import dip.world.World;
import dip.world.WorldFactory;
import dip.world.variant.VariantManager;
import dip.world.variant.data.Variant;
import org.nukesoft.jdipFacade.exception.JdipException;
import org.nukesoft.jdipFacade.exception.ResourceLoadException;


/**
 * Creates <code>JdipWorld</code> objects.
 *
 * @author Ryan Michela
 */
public class JdipWorldFactory {
    private final ImplementationStrategy strategy;

    //singelton constructor
    JdipWorldFactory(ImplementationStrategy strategy) {
        this.strategy = strategy;
    }

    //variant related methods

    /**
     * Fetches the name of all loaded variants.
     *
     * @return a <code>String[]</code> of variant names.
     */
    public String[] getVariantNames() {
        Variant[] variants = VariantManager.getVariants();
        String[] variantNames = new String[variants.length];
        for (int i = 0; i < variants.length; i++) {
            variantNames[i] = variants[i].getName();
        }
        return variantNames;
    }

    /**
     * Loads a world for the Standard variant. Uses the default headless implementation strategy.
     *
     * @return a <code>World</code> object
     * @throws ResourceLoadException if the <code>VariantManager</code> failed to initialize
     * @throws JdipException         if the <code>World</code> failed to load
     */
    public JdipWorld createWorld()
            throws JdipException, ResourceLoadException {
        return new JdipWorld(getWorldFromVariant("Standard"), strategy);
    }

    /**
     * Loads a world for a given variant name. Uses the default headless implementation strategy.
     *
     * @param variantName the variant to load
     * @return a <code>World</code> object
     * @throws ResourceLoadException if the <code>VariantManager</code> failed to initialize
     * @throws JdipException         if the <code>World</code> failed to load
     */
    public JdipWorld createWorld(String variantName)
            throws JdipException, ResourceLoadException {
        return new JdipWorld(getWorldFromVariant(variantName), strategy);
    }

    /**
     * Creates a world object.
     *
     * @param variantName
     * @return a world
     * @throws ResourceLoadException
     * @throws InvalidWorldException
     */
    private World getWorldFromVariant(String variantName)
            throws ResourceLoadException, JdipException {
        try {
            //initialize the variant
            Variant v = VariantManager.getVariant(variantName, VariantManager.VERSION_NEWEST);
            if (v == null) {
                throw new ResourceLoadException("Failed to load variant \"" +
                        variantName + "\"");
            }
            //create the world and establish the rules
            World w = WorldFactory.getInstance().createWorld(v);
            w.setRuleOptions(RuleOptions.createFromVariant(v));

            return w;
        } catch (InvalidWorldException e) {
            throw new JdipException("Failed to load world", e);
        }
    }
}
