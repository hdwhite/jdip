//
//  @(#)ParticipantMetadata.java		9/2004
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
package dip.world.metadata;


import java.net.URI;

/**
*	This is the base metadata object for non-game data.
*	
*	
*/
public class ParticipantMetadata
{
	/** Role: Player */
	public static final String ROLE_PLAYER		= "player";
	/** Role: Observer */
	public static final String ROLE_OBSERVER	= "observer";
	/** Role: Moderator */
	public static final String ROLE_MODERATOR	= "moderator";
	/** Role: Unspecified */
	public static final String ROLE_UNSPECIFIED	= "unspecified";
	
	/** Type: Human */
	public static final String TYPE_HUMAN		= "human";
	/** Type: Computer */
	public static final String TYPE_COMPUTER	= "computer";
	/** Type: Unknown */
	public static final String TYPE_UNSPECIFIED		= "unspecified";
	/** Type: Inactive Computer Power (e.g., Italy in a 6 or 7 player game) */
	public static final String TYPE_INACTIVE_COMPUTER	= "inactive";
	
	
	private static final String EMPTY = "";
	
	// MetaData
	private String 			name = EMPTY;
	private String[] 		email = new String[0];
	private URI 			uri = null;
	private String			notes = EMPTY;
	private String			type = TYPE_UNSPECIFIED;
	private String 			role = ROLE_UNSPECIFIED;
	private String			nick = EMPTY;
	
	/** Create a ParticipantMetadata object */
	public ParticipantMetadata()
	{
	}// ParticipantMetadata()
	
	/** Get participant's nickname. Never null */
	public String getNickname()					{ return nick; }
	
	/** Get the Participant Type. Should be a TYPE constant. */
	public String getType()						{ return type; }
	
	/** Get the Participant Role. Should be a ROLE constant. */
	public String getRole()						{ return role; }
	
	/** Gets participant name. Never null. May be empty. */
	public String getName()						{ return name; }
	
	/** Gets participant email addresses. Never null. */
	public String[] getEmailAddresses()			
	{ 
		// we can clone... strings are immutable
		return (String[]) email.clone();
	}// getEmailAddresses()
		
	/** Gets participant contact URI. May be null. */
	public URI getURI()							{ return uri; }
	
	/** Gets textual notes. Never null. May be empty. */
	public String getNotes()					{ return notes; }
	
	
	
	/** Sets the participant name. A null value will create an empty string. */
	public void setName(String value)			{ name = (value == null) ? EMPTY : value; }
	
	/** Sets the participants nickname.  A null value will create an empty string. */
	public void setNickname(String value)		{ nick = (value == null) ? EMPTY : value; }
	
	/** Set the Participant's Type. Should be a recognized constant. */
	public final void setType(String value)			
	{
		final String tmp = parseType(value);
		if(tmp != null)
		{
			type = tmp;
		}
		else
		{
			throw new IllegalArgumentException(value);
		}
	}// setType()
	
	/** Set the Participant's Role. Should be a recognized constant. */
	public final void setRole(String value)			
	{
		final String tmp = parseRole(value);
		if(tmp != null)
		{
			role = tmp;
		}
		else
		{
			throw new IllegalArgumentException(value);
		}
	}// setRole()
	
	
	/** Sets the participant's email addresses. A <code>null</code> value clear all stored values.*/
	public synchronized void setEmailAddresses(final String[] values)	
	{ 
		if(values != null && values.length > 0)
		{
			email = (String[]) values.clone();
		}
		else
		{
			email = new String[0];
		}
	}// setEmailAddresses()
	
	/** Sets the participant contact URI. Null values are permissable. */
	public void setURI(URI value)					{ uri = value; }
	
	/** Sets notes. Null values will create an empty string. */
	public void setNotes(String value)				{ notes = (value == null) ? EMPTY : value; }
	
	/** Parse the type into a constant; if not recognized, returns null. */
	public static String parseType(String in)
	{
		if(TYPE_HUMAN.equals(in))
		{
			return TYPE_HUMAN;
		}
		else if(TYPE_COMPUTER.equals(in))
		{
			return TYPE_COMPUTER;
		}
		else if(TYPE_UNSPECIFIED.equals(in))
		{
			return TYPE_UNSPECIFIED;
		}
		else if(TYPE_INACTIVE_COMPUTER.equals(in))
		{
			return TYPE_INACTIVE_COMPUTER;
		}
		
		return null;
	}// parseType()
	
	/** Parse the role into a constant; if not recognized, returns null. */
	public static String parseRole(String in)
	{
		if(ROLE_PLAYER.equals(in))
		{
			return ROLE_PLAYER;
		}
		else if(ROLE_MODERATOR.equals(in))
		{
			return ROLE_MODERATOR;
		}
		else if(ROLE_OBSERVER.equals(in))
		{
			return ROLE_OBSERVER;
		}
		else if(ROLE_UNSPECIFIED.equals(in))
		{
			return ROLE_UNSPECIFIED;
		}
		
		return null;
	}// parseRole()
	
}// class ParticipantMetadata
