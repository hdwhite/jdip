//
//  @(#)PlayerMetadata.java	1.00	6/2002
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
package dip.world.metadata;

import dip.world.Power;


import java.net.URI;

/**
*	Only one PlayerMetadata object can exist for a Power.
*	This is equivalent to a ParticipantMetadata, however, 
*	we also keep the Power.
*	
*	
*/
public class PlayerMetadata extends ParticipantMetadata
{
	private final Power power;
	
	/** 
	*	Create a PlayerMetadata object. A <code>null</code> Power is not permitted. 
	*	The default role is ROLE_PLAYER and default type is TYPE_HUMAN. 
	*/
	public PlayerMetadata(Power power)
	{
		if(power == null) 
		{
			throw new IllegalArgumentException();
		}
		
		this.power = power;
		
		setRole(ROLE_PLAYER);
		setType(TYPE_HUMAN);
	}// PlayerMetadata()
	
	/** Get the player's power. Never null. */
	public Power getPower()		
	{ 
		return power; 
	}// getPower()
	
}// class PlayerMetadata
