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

import dip.order.*;
import dip.order.result.*;
import dip.order.result.OrderResult.ResultType;
import dip.world.Power;


/**
 * The <code>JdipResult</code> class provides access to the results of
 * an adjudicated order.
 * @author Ryan Michela
 */
public class JdipResult
{
	private Result result;
	private OrderFormatOptions format;
	private Power power;
	
	/**
	 * Package constructor. Don't try this at home, kids.
	 * @param result
	 * @param format
	 * @param t
	 */
	JdipResult(Result result, OrderFormatOptions format)
	{
		this.result = result;
		this.format = format;
		this.power = result.getPower();
	}
	/**
	 * Determines whether this result is a general result or an order-linked result.
	 * @return <code>true</code> if general, <code>false</code> if otherwise.
	 */
	public boolean isGeneralResult()
	{
		return (power == null);
	}
	/**
	 * Determines whether this result is tied to an actual order. If not, it is either a system
	 * generated result (ex. order substitution) or a general result.
	 * @return <code>true</code> if order linked, <code>false</code> if otherwise.
	 */
	public boolean isOrderLinkedResult()
	{
		if(result instanceof OrderResult)
		{
			Orderable o = ((OrderResult)result).getOrder();
			return (o != null);
		}
		else
		{
			return false;
		}
	}
	/**
	 * Determines whether this result succeeded.
	 * @return <code>true</code> for success or general order, <code>false</code> for failure.
	 */
	public boolean isSuccessfull()
	{
		if(result instanceof OrderResult)
		{
			ResultType rt = ((OrderResult)result).getResultType();
			boolean failure = ( rt == ResultType.FAILURE ||
							   rt == ResultType.DISLODGED ||
							   rt == ResultType.VALIDATION_FAILURE );
			return !failure;
		}
		else
		{
			return true;
		}
	}
	/**
	 * Returns the power associated with this order.
	 * @return The name of the power, or <code>null</code> if this is a general order.
	 */
	public String getPower()
	{
		return power.getName();
	}
	/**
	 * Returns a formatted version of the order that created this result.
	 * @return the order or an empty string if called on a non-order-linked
	 * result
	 */
	public String getFormattedOrder()
	{
		if(!this.isGeneralResult() && this.isOrderLinkedResult())
		{
			return ((OrderResult)result).getOrder().toFormattedString(format);
		}
		else
		{
			return "";
		}
	}
	/**
	 * Returns the message associated with this result.
	 * @return the message
	 */
	public String getMessage()
	{
		return result.getMessage(format);
	}
}