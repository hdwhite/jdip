//
//  @(#)GameMetadata.java		6/2002
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


import java.net.URI;
/**
*	Contains MetaData about the entire game.
*	<p>
*	Only one GameMetadata object exists for an entire game.
*	
*	
*	
*	
*/
public class GameMetadata
{
	// constants
	private static final String EMPTY = "";
	
	// MetaData
	private String comment = EMPTY;
	private String gameName = EMPTY;
	private String judgeName = null;
	private URI gameURI = null;
	private String notes = EMPTY;
	private String id = EMPTY;		
	
	/** Create a GameMetadata object */
	public GameMetadata()
	{
	}// GameMetadata()
	
	
	/** Gets game comment. Never null. May be empty. */
	public String getComment()						{ return comment; }
	
	/** Gets game name. Never null, may be empty. */
	public String getGameName()						{ return gameName; }
	
	/** Gets textual notes. Never null, but may be empty. */
	public String getNotes()						{ return notes; }
	
	/** Gets the Judge name; may be null */
	public String getJudgeName()					{ return judgeName; }
	
	/** Gets the game URI; may be null */
	public URI getGameURI()							{ return gameURI; }
	
	/** Gets the game ID: e.g., Boardman, Miller, or EPNum. Never null.*/
	public String getGameID()						{ return id; }
	
	
	
	
	/** Sets game comment. Never null. May be empty. */
	public void setComment(String value)			{ comment = (value == null) ? EMPTY : value; }
	
	/** Sets game name. Never null, may be empty. */
	public void setGameName(String value)			{ gameName = (value == null) ? EMPTY : value; }
	
	/** Sets textual notes. Never null, but may be empty. */
	public void setNotes(String value)				{ notes = (value == null) ? EMPTY : value; }
	
	/** Sets the Judge name; may be null */
	public void setJudgeName(String value)			{ judgeName = value; }
	
	/** Sets the game URI; may be null */
	public void setGameURI(URI value)				{ gameURI = value; }
	
	/** Sets the game ID: e.g., Boardman, Miller, or EPNum. Never null. */
	public void setGameID(String value)				{ id = (value == null) ? EMPTY : value; }
	
}// class GameMetadata
