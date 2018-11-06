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
 * Top level exception for all facade related errors.
 * @author Ryan Michela
 */
public class JdipFacadeException extends Exception
{
	/**
	 * 
	 */
	public JdipFacadeException()
	{
		super();
	}
	/**
	 * @param arg0
	 */
	public JdipFacadeException(String arg0)
	{
		super(arg0);
	}
	/**
	 * @param arg0
	 * @param arg1
	 */
	public JdipFacadeException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}
	/**
	 * @param arg0
	 */
	public JdipFacadeException(Throwable arg0)
	{
		super(arg0);
	}
	/**
	 * Automaticaly adds the error header to the message.
	 * @see java.lang.Throwable#getMessage()
	 */
	public String getMessage()
	{
		return getMessage(0);
	}
	
	/**
	 * @param tabs the number of tabs to indent
	 * @return
	 */
	String getMessage(int tabs)
	{
		String message = identify();
		if(super.getMessage() != null)
		{
			message += ("::" + super.getMessage());
		}
		if(this.getCause() != null)
		{
			message += "\n";
			//insert tabs
			for(int i = -1; i < tabs; i++)
			{
				message += "   ";
			}
			message += "Root Cause::";
			if(this.getCause() instanceof JdipFacadeException)
			{
				message += ((JdipFacadeException)this.getCause()).getMessage(tabs + 1);
			}
			else
			{
				message += this.getCause().toString();
			}
		}
		return message;
	}
	/**
	 * Makes an error header in the format 
	 * [ExceptionClassName @ ErroringClass : BadMethod (LineNumber)] :: error text
	 * @return the error header
	 */
	private String identify()
	{
		StackTraceElement currentFrame = getStackTrace()[0];
		return "[" + getClass().getName() + "@" + currentFrame.getClassName() + ":" 
				+ currentFrame.getMethodName() + "(" + currentFrame.getLineNumber() +")]";
	}
}
