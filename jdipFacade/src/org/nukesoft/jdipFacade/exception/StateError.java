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
package org.nukesoft.jdipFacade.exception;


/**
 * Thrown when a class's methods are accessed out of sequence or when the internal
 * state of an object is in question. Since this is an error rather than an exception,
 * there is no need to catch this class.
 * @author Ryan Michela
 */
public class StateError extends Error
{

	public StateError()
	{
		super();
	}
	public StateError(String arg0)
	{
		super(arg0);
	}
	public StateError(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}
	public StateError(Throwable arg0)
	{
		super(arg0);
	}
}
