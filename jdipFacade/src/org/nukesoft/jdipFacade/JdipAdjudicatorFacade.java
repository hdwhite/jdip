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

import java.io.*;

import org.nukesoft.jdipFacade.exception.*;

import dip.process.*;
import dip.world.*;
import dip.world.variant.VariantManager;


/**
 * This class provides a simplified and centeralized access point to the core functionality
 * of the <a href="http://soruceforge.net/projects/jdip">jdip diplomacy adjudicator</a>.
 * @author Ryan Michela
 */
public class JdipAdjudicatorFacade
{
	private static String jdipRootPath;
	private static boolean isVariantManagerInitialized = false;
	private static ImplementationStrategy strategy;
	
	/**
	 * Instantiates a <code>JdipAdjudicatorFacade</code> and initializes the <code>VariantManager</code>.
	 * Disables graphics but does not explicitly disable AWT.
	 * Must be called before any other methods in this class.
	 * @param jdipRootPath the filesystem path to a standard Jdip installation.
	 * @throws JdipException
	 */
	public static void initializeJdip(String jdipRootPath) 
	throws JdipException
	{
		initializeJdip(jdipRootPath, new HeadlessImplementationStrategy(false));
	}
	
	/**
	 * Instantiates a <code>JdipAdjudicatorFacade</code> and initializes the <code>VariantManager</code>.
	 * Must be called before any other methods in this class.
	 * @param jdipRootPath the filesystem path to a standard Jdip installation.
	 * @param strategy the <code>ImplementationStrategy</code> to use.
	 * @throws JdipException
	 */
	public static void initializeJdip(String jdipRootPath, ImplementationStrategy strategy) 
		throws JdipException
	{
		JdipAdjudicatorFacade.strategy = strategy;
		JdipAdjudicatorFacade.jdipRootPath = jdipRootPath;
		try
		{
			if(!isVariantManagerInitialized)
			{
				VariantManager.init(new File[]{new File(jdipRootPath, "/variants")}, true);
				isVariantManagerInitialized = true;
			}
			else
			{
				throw new StateError("Cannot set jdipRootPath twice.");
			}
		}
		catch(Exception e)
		{
			throw new JdipException("Error initializing VariantManager in JdipAdjudicatorFacade", e);
		}
	}

	/**
	 * Gets the world factory with which to create worlds.
	 * @return the <code>JdipWorldFactory</code>
	 */
	public static JdipWorldFactory getJdipWorldFactory()
	{
		if(isVariantManagerInitialized)
		{
			return new JdipWorldFactory(strategy);
		}
		else
		{
			throw new StateError("Cannot create a world factory without first setting jdipRootPath.");
		}
	}
	
	/**
	 * Adjudicate a world.
	 * @param worldFacade the world to adjudicate
	 * @return <code>true</code> if the game has been won, <code>false</code> if otherwise.
	 */
	public static boolean adjudicate(JdipWorld worldFacade)
	{
		if(isVariantManagerInitialized)
		{
			World world = worldFacade.getWorld();
			//adjudicate
			Adjudicator stdJudge = new StdAdjudicator(worldFacade.getStrategy().getOrderFactory(), world.getLastTurnState());
			stdJudge.process();
			//advance turn state
			TurnState nextTurnState = stdJudge.getNextTurnState();
			if(nextTurnState != null)
			{
				world.setTurnState(nextTurnState);
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			throw new StateError("Cannot adjudicate a world factory without first setting jdipRootPath.");
		}
	}
}
