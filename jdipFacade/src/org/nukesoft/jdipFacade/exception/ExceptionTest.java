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
 * Tests exceptions
 * @author Ryan Michela
 */
public class ExceptionTest
{

	public static void main(String[] args)
	{
		try
		{
			throw new PowerNotFoundException();
		}
		catch(PowerNotFoundException e)
		{
			System.out.println(e.getMessage());
		}
		System.out.println();
		try
		{
			die();
		}
		catch(JdipException e)
		{
			System.out.println(e.getMessage());
		}
		System.out.println();
		try
		{
			die2();
		}
		catch(JdipException e)
		{
			System.out.println(e.getMessage());
		}
		System.out.println();
		try
		{
			die3();
		}
		catch(JdipException e)
		{
			System.out.println(e.getMessage());
		}
	}
	
	private static void die() throws JdipException
	{
		try
		{
			throw new ResourceLoadException("A load failed.");
		}
		catch(ResourceLoadException e)
		{
			throw new JdipException("Problem with method call", e);
		}
	}
	
	private static void die2() throws JdipException
	{
		try
		{
			die();
		}
		catch(JdipException e)
		{
			throw new JdipException("Problem with method call", e);
		}
	}
	
	private static void die3() throws JdipException
	{
		try
		{
			throw new Exception("Foo");
		}
		catch(Exception e)
		{
			throw new JdipException("Problem with method call", e);
		}
	}
}
