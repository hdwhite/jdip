//
//  @(#)GUIUtils.java		9/2004
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
package dip.gui.swing;

import java.awt.*;
import javax.swing.*;

/** 
*	Useful GUI Utilities	
*/
public class GUIUtils
{
	
	
	/** Get the Brightness of a Color, using the W3C formula. */
	public static float getW3CBright(Color c)
	{
		return (((c.getRed() * 299) + (c.getGreen() * 587) + (c.getBlue() * 114)) / 1000);
	}// getW3CBrite()
	
	
	
	/** 
	*	Gets the absolute difference of W3C brightness between two colors.
	*	<b>Note:</b> for best legibility, this should be greater than 125.
	*/
	public static float getW3CBrightnessDifference(Color c1, Color c2)
	{
		return Math.abs(getW3CBright(c1) - getW3CBright(c2));
	}// getW3CBrightnessDifference()
	
	/**
	*	Gets the (absolute) Hue difference between two colors.
	*	<b>Note:</b> for best legitibility, this should be greater than 500.
	*/
	public static float getW3CHueDifference(Color c1, Color c2)
	{
		float val = (Math.max(c1.getRed(), c2.getRed()) - Math.min(c1.getRed(), c2.getRed()));
		val += (Math.max(c1.getGreen(), c2.getGreen()) - Math.min(c1.getGreen(), c2.getGreen()));
		val += (Math.max(c1.getBlue(), c2.getBlue()) - Math.min(c1.getBlue(), c2.getBlue()));
		return val;
	}// getW3CHueDifference()
	
	/**
	*	Given an array of colors ("options"), find the one with the
	*	highest Hue difference compared to the "test" color.
	*	
	*/
	public static Color findBestHue(Color test, Color[] options)
	{
		float max = Float.MIN_VALUE;
		Color best = null;
		
		for(int i=0; i<options.length; i++)
		{
			float val = getW3CHueDifference(options[i], test);
			if(val > max)
			{
				max = val;
				best = options[i];
			}
		}
		
		return best;
	}// findBestHue()
	
	/**
	*	Given an array of colors ("options"), find the one with the
	*	highest brightness difference compared to the "test" color.
	*	
	*/
	public static Color findBestBright(Color test, Color[] options)
	{
		float max = Float.MIN_VALUE;
		Color best = null;
		
		for(int i=0; i<options.length; i++)
		{
			float val = getW3CBrightnessDifference(options[i], test);
			if(val > max)
			{
				max = val;
				best = options[i];
			}
		}
		
		return best;
	}// findBestBright()
	
	
}// class GUIUtils

