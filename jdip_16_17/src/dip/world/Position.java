//
//  @(#)Position.java		2/2003
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
package dip.world;

import dip.world.Province;
import dip.world.Power;
import dip.world.Unit;
import dip.order.OrderException;

import dip.world.io.XMLSerializer;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Iterator;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;

/**
*
*	Stores all the mutable (state) information for a given TurnState.
*	Immutable data is retained in the Power/Province/etc. objects; only
*	mutable data is stored here.
*	<p>
*	This object can be cloned, and should be cloned, when creating a new 
*	Position based upon previous Position data. Several clone methods are
*	available, each optimized for speed and cloning requirements.
*	<p>
*	WARNING: this code is not MT (Multithread) safe!
*	<p>
*	This class is heavily optimized, as adjudicator performance is highly dependent
*	upon the performance of this class.
*	<p>
*	The clone() methods are not strictly implemented; they call a constructor
*	to assist in cloning rather than call super.clone(). This is done for 
*	performance reasons.
*/
public class Position implements Cloneable
{
	/*
		Implementation note when a tmpProvArray is used:
		
		Copying references into a large temporary array, then creating an array
		(that is the correct size) and copying the temp array into the correctly-sized 
		array via System.arraycopy() is about twice as fast as using an ArrayList, and 
		almost twice as fast as double-iterating (one to gauge the array size,
		and one to copy data).
		
		This method creates a sufficiently large array to hold temporary data.
	
	*/
	
	
	
	
	// instance variables
	protected final ProvinceData[] provArray;
	protected final PowerData[] powerArray;
	protected final dip.world.Map map;
	
	
	public Position(dip.world.Map map)
	{
		if(map == null)
		{
			throw new IllegalArgumentException();
		}
		
		this.map = map;
		this.provArray = new ProvinceData[map.getProvinceCount()];
		this.powerArray = new PowerData[map.getPowerCount()];
	}// Position()
	
	
	
	
	/** The Number of Provinces in this Position */
	public final int size()
	{
		return provArray.length;
	}// size()
	
	
	/** 
	*	Convenience method: Returns an array of Provinces 
	*	*potentially unsafe*
	*	@deprecated 		use Map.getProvinceList() instead
	*/
	public final Province[] getProvinces()
	{
		return map.getProvinces();
	}// getProvinces()
	
	/** 
	*	Return the Map
	*/
	public final dip.world.Map getMap()
	{
		return map;
	}// getMap()
	
	/** Returns true if this Power has been eliminated. False by default. */
	public boolean isEliminated(Power power)
	{
		synchronized(powerArray)
		{
			PowerData pd = powerArray[power.getIndex()];
			if(pd != null)
			{
				return pd.isEliminated();
			}
			
			return false;
		}
	}// isEliminated()
	
	
	/** Set whether this Power has been eliminated. */
	public void setEliminated(Power power, boolean value)
	{
		synchronized(powerArray)
		{
			PowerData pd = getPowerData(power);
			pd.setEliminated(value);
		}
	}// setEliminated()
	
	
	/** 
	*	Scans the Position; sets/unsets elimination depending upon if a given
	* 	Power has any units (including dislodged units) or supply centers on the map 
	*/
	public void setEliminationStatus(final Power[] powers)
	{
		// value array; indexed by power.getIndex();
		// if value == 0, no SC, unit, or dislodged unit for that power.
		final int[] counts = new int[powers.length];
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				final ProvinceData pd = provArray[i];
				
				if(pd != null)
				{
					Unit unit = pd.getUnit();
					if(unit != null)		// first check non-dislodged units
					{
						counts[unit.getPower().getIndex()]++;
					}
					
					// then see if there's a dislodged unit
					unit = pd.getDislodgedUnit();
					if(unit != null)
					{
						counts[unit.getPower().getIndex()]++;
					}
					
					// finally, see if we own a supply center
					Power power = pd.getSCOwner();
					if(power != null)
					{
						counts[power.getIndex()]++;
					}
				}
			}
		}
		
		synchronized(powerArray)
		{
			for(int i=0; i<powers.length; i++)
			{
				PowerData pd = getPowerData(powers[i]);
				pd.setEliminated( (counts[powers[i].getIndex()] == 0) );
			}
		}
	}// setEliminationStatus()
	
	
	/** Set the owner of the supply center. */
	public void setSupplyCenterOwner(Province province, Power power)	 		
	{
		synchronized(provArray)
		{
			ProvinceData pd = getProvinceData(province);
			pd.setSCOwner(power);
		}
	}// setSupplyCenterOwner()
	
	
	/** Set the owner of a home supply center. */
	public void setSupplyCenterHomePower(Province province, Power power)	 		
	{
		synchronized(provArray)
		{
			ProvinceData pd = getProvinceData(province);
			pd.setSCHomePower(power);
		}
	}// setSupplyCenterHomePower()
	
	
	/** Determine if this Province contains a supply center */
	public boolean hasSupplyCenterOwner(Province province)
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.isSCOwned();
			}
			return false;
		}
	}// hasSupplyCenterOwner()
	
	
	/** Determine if this Province contains a Home supply center */
	public boolean isSupplyCenterAHome(Province province)
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.isSCAHome();
			}
			return false;
		}
	}// isSupplyCenterAHome()
	
	
	/** Get the home power of the supply center; null if no supply center or home power */
	public Power getSupplyCenterHomePower(Province province)
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.getSCHomePower();
			}
			return null;
		}
	}// getSupplyCenterHomePower()
	
	
	/** Get the owner of the supply center; null if no owner or no supply center. */
	public Power getSupplyCenterOwner(Province province)	
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.getSCOwner();
			}
			return null;
		}
	}// getSupplyCenterOwner()
	
	
	
	
	// non-dislodged unit	
	/** Set the unit contained in this province; null to eliminate an existing unit. */
	public void setUnit(Province province, Unit unit)
	{
		synchronized(provArray)
		{
			ProvinceData pd = getProvinceData(province);
			pd.setUnit(unit);
		}
	}// setUnit()
	
	/** Determines if there is a unit present in this province. */
	public boolean hasUnit(Province province)
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.hasUnit();
			}
			return false;
		}
	}// hasUnit()
	
	/** Get the unit contained in this Province. Returns null if no unit exists. */
	public Unit getUnit(Province province)
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.getUnit();
			}
			return null;
		}
	}// getUnit()
	
	
	/** Test if the given type of unit is contained in this Province. */
	public boolean hasUnit(Province province, Unit.Type unitType)
	{
		synchronized(provArray)
		{
			Unit unit = getUnit(province);
			if(unit != null)
			{
				return(unit.getType().equals(unitType));
			}
			return false;
		}
	}// hasUnit()
	
	/** Test if the given type of unit is contained in this Province. */
	public boolean hasDislodgedUnit(Province province, Unit.Type unitType)
	{
		synchronized(provArray)
		{
			Unit unit = getDislodgedUnit(province);
			if(unit != null)
			{
				return(unit.getType().equals(unitType));
			}
			return false;
		}
	}// hasDislodgedUnit()
	
	
	
	
	// dislodged unit	
	/** Set the dislodged unit contained in this province; null to eliminate an existing unit. */
	public void setDislodgedUnit(Province province, Unit unit)
	{
		synchronized(provArray)
		{
			ProvinceData pd = getProvinceData(province);
			pd.setDislodgedUnit(unit);
		}
	}// setDislodgedUnit()
	
	
	/** Get the dislodged unit in this Province. Returns null if no dislodged unit exists. */
	public Unit getDislodgedUnit(Province province)
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.getDislodgedUnit();
			}
			return null;
		}
	}// getDislodgedUnit()
	
	
	// last occupier
	/** 
	*	Sets the Power that last occupied a given space. Note that this
	*	is not intended to be used for Supply Center ownership (which only
	*	changes in the Fall season); use setSupplyCenterOwner() instead.
	*/
	public void setLastOccupier(Province province, Power power)
	{
		synchronized(provArray)
		{
			ProvinceData pd = getProvinceData(province);
			pd.setLastOccupier(power);
		}
	}// setLastOccupier()
	
	
	/** 
	*	Returns the Power that last occupied a given space. Note that this
	*	is not intended to be used for Supply Center ownership (which only
	*	changes in the Fall season); use getSupplyCenterOwner() instead.
	*/
	public Power getLastOccupier(Province province)
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.getLastOccupier();
			}
			return null;
		}
	}// getLastOccupier()	
	
	
	/** Determines if there is a dislodged unit present in this province. */
	public boolean hasDislodgedUnit(Province province)
	{
		synchronized(provArray)
		{
			ProvinceData pd = provArray[province.getIndex()];
			if(pd != null)
			{
				return pd.hasDislodgedUnit();
			}
			return false;
		}
	}// hasDislodgedUnit()
	
	
	
	
	/** Returns an array of provinces with non-dislodged units */
	public Province[] getUnitProvinces()
	{
		final Province[] tmpProvArray = new Province[provArray.length];
		int arrSize = 0;
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null && pd.hasUnit())
				{
					tmpProvArray[arrSize] = map.getProvince(i);
					arrSize++;
				}
			}
		}
		
		Province[] p = new Province[arrSize];
		System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
		return p;
	}// getUnitProvinces()
	
	
	
	/** Returns an array of provinces with dislodged units */
	public Province[] getDislodgedUnitProvinces()
	{
		final Province[] tmpProvArray = new Province[provArray.length];
		int arrSize = 0;
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null && pd.hasDislodgedUnit())
				{
					tmpProvArray[arrSize] = map.getProvince(i);
					arrSize++;
				}
			}
		}
		
		Province[] p = new Province[arrSize];
		System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
		return p;
	}// getDislodgedUnitProvinces()
	
	
	/** Returns the number of provinces with non-dislodged units */
	public int getUnitCount()
	{
		int count = 0;		
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null && pd.hasUnit())
				{
					count++;
				}
			}
		}
		return count;
	}// getUnitCount()
	
	
	/** Returns the number of provinces with dislodged units */
	public int getDislodgedUnitCount()
	{
		int count = 0;		
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null && pd.hasDislodgedUnit())
				{
					count++;
				}
			}
		}
		return count;
	}// getDislodgedUnitCount()
	
	
	/** Returns an array of provinces with home supply centers */
	public Province[] getHomeSupplyCenters()
	{
		final Province[] tmpProvArray = new Province[provArray.length];
		int arrSize = 0;
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null && pd.isSCAHome())
				{
					tmpProvArray[arrSize] = map.getProvince(i);
					arrSize++;
				}
			}
		}
		
		Province[] p = new Province[arrSize];
		System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
		return p;
	}// getHomeSupplyCenters()
	
	
	/** Returns an Array of the Home Supply Centers for a given power (whether or not they are owned by that power) */
	public Province[] getHomeSupplyCenters(Power power)
	{
		final Province[] tmpProvArray = new Province[provArray.length];
		
		int arrSize = 0;
		for(int i=0; i<provArray.length; i++)
		{
			ProvinceData pd = provArray[i];
			if(pd != null && pd.getSCHomePower() == power)
			{
				tmpProvArray[arrSize] = map.getProvince(i);
				arrSize++;
			}
		}
		
		Province[] p = new Province[arrSize];
		System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
		return p;
	}// getHomeSupplyCenters()
	
	
	/** 
	*	Determines if a Power has at least one owned Home Supply Center. 
	*	<p>
	*	An owned home supply center need not have a unit present.
	*/
	public boolean hasAnOwnedHomeSC(Power power)
	{
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null && pd.getSCHomePower() == power && pd.getSCOwner() == power)
				{
					return true;
				}
			}
			
			return false;
		}
	}// hasAnOwnedHomeSC()
	
	
	/** Returns an Array of the owned Supply Centers for a given Power (whether or not they are home supply centers) */
	public Province[] getOwnedSupplyCenters(Power power)
	{
		final Province[] tmpProvArray = new Province[provArray.length];
		int arrSize = 0;
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null && pd.getSCOwner() == power)
				{
					tmpProvArray[arrSize] = map.getProvince(i);
					arrSize++;
				}
			}
		}
		
		Province[] p = new Province[arrSize];
		System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
		return p;
	}// getOwnedSupplyCenters()
	
	
	/** Returns an array of provinces with owned supply centers */
	public Province[] getOwnedSupplyCenters()
	{
		final Province[] tmpProvArray = new Province[provArray.length];
		int arrSize = 0;
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null && pd.isSCOwned())
				{
					tmpProvArray[arrSize] = map.getProvince(i);
					arrSize++;
				}
			}
		}
		
		Province[] p = new Province[arrSize];
		System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
		return p;
	}// getOwnedSupplyCenters()
	
	
	
	/** 
	*	Deep clone of the contents of this Position. 
	*/
	public Object clone()
	{
		final Position pos = new Position(map);
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				
				if(pd != null)
				{
					pos.provArray[i] = pd.normClone();
				}
			}
		}
		
		synchronized(powerArray)
		{
			for(int i=0; i<powerArray.length; i++)
			{
				PowerData pd = powerArray[i];
				if(pd != null)
				{
					pos.powerArray[i] = pd.normClone();
				}
			}
		}
		
		return pos;
	}// clone()
	
	/** 
	*	Deep clone of everything *except* dislodged & non-dislodged units;
	*	(e.g., SC ownership, Power Info, etc.)
	*/
	public Position cloneExceptUnits()
	{
		final Position pos = new Position(map);
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				
				if(pd != null)
				{
					pos.provArray[i] = pd.cloneExceptUnits();
				}
			}
		}
		
		synchronized(powerArray)
		{
			for(int i=0; i<powerArray.length; i++)
			{
				PowerData pd = powerArray[i];
				if(pd != null)
				{
					pos.powerArray[i] = pd.normClone();
				}
			}
		}
		
		return pos;
	}// cloneExceptUnits()
	
	
	/** Deep clone of everything <b>except</b> dislodged units. */
	public Position cloneExceptDislodged()
	{
		final Position pos = new Position(map);
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				
				if(pd != null)
				{
					pos.provArray[i] = pd.cloneExceptDislodged();
				}
			}
		}
		
		synchronized(powerArray)
		{
			for(int i=0; i<powerArray.length; i++)
			{
				PowerData pd = powerArray[i];
				if(pd != null)
				{
					pos.powerArray[i] = pd.normClone();
				}
			}
		}
		
		return pos;
	}// cloneExceptDislodged()
	
	
	/**
	*	Gets all the Provinces with non-dislodged 
	*	Units for a particular power.
	*
	*/
	public Province[] getUnitProvinces(Power power)
	{
		final Province[] tmpProvArray = new Province[provArray.length];
		int arrSize = 0;
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null)
				{
					Unit unit = pd.getUnit();
					if(unit != null && unit.getPower() == power)
					{
						tmpProvArray[arrSize] = map.getProvince(i);
						arrSize++;
					}
				}
			}
		}
		
		Province[] p = new Province[arrSize];
		System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
		return p;
	}// getUnitProvinces()
	
	
	/**
	*	Gets all the Provinces with dislodged 
	*	Units for a particular power.
	*/
	public Province[] getDislodgedUnitProvinces(Power power)
	{
		final Province[] tmpProvArray = new Province[provArray.length];
		int arrSize = 0;
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData pd = provArray[i];
				if(pd != null)
				{
					Unit unit = pd.getDislodgedUnit();
					if(unit != null && unit.getPower() == power)
					{
						tmpProvArray[arrSize] = map.getProvince(i);
						arrSize++;
					}
				}
			}
		}
		
		Province[] p = new Province[arrSize];
		System.arraycopy(tmpProvArray, 0, p, 0, arrSize);
		return p;
	}// getDislodgedUnitProvinces()
	
	
	/**
	*	Checks the current position with another position 
	*	(NOTE: both positions must be from the same World!)
	*	for a change in Supply Center ownership. Returns 
	*	true if a ownership change was detected.
	*/
	public boolean isSCChanged(Position p)
	{
		// sanity check
		if( (p.provArray.length != provArray.length)
			|| (p.map != map) )
		{
			throw new IllegalArgumentException();
		}
		
		synchronized(provArray)
		{
			for(int i=0; i<provArray.length; i++)
			{
				ProvinceData this_pd = provArray[i];
				ProvinceData p_pd = p.provArray[i];
				
				Power thisPower = (this_pd == null) ? null : this_pd.getSCOwner();
				Power pPower = (p_pd == null) ? null : p_pd.getSCOwner();
				
				if(thisPower != pPower)
				{
					return true;
				}
			}
			
			return false;
		}
	}// isSCChanged()
	
	
	/**
	*	Call this method FIRST before any set(); thus if ProvinceData
	*	does not exist, we will add one to the map.
	*	<p>
	*	*unsynchronized*
	*/
	private ProvinceData getProvinceData(Province province)
	{
		final int idx = province.getIndex();
		ProvinceData pd = provArray[idx];
		if(pd == null)
		{
			pd = new ProvinceData(idx);
			provArray[idx] = pd;
		}
		
		return pd;
	}// getProvinceData()
	
	/** Same type of functionality as getProvinceData() but for PowerData objects */
	private PowerData getPowerData(Power power)
	{
		final int idx = power.getIndex();
		PowerData pd = powerArray[idx];
		if(pd == null)
		{
			pd = new PowerData(idx);
			powerArray[idx] = pd;
		}
		
		return pd;
	}// getPowerData()
	
	
	/** All mutable Province data is kept here */
	private static class ProvinceData
	{
		// instance variables
		private Unit 	unit = null;
		private Unit 	dislodgedUnit = null;
		private Power 	SCOwner = null;
		private Power	SCHomePower = null;
		private Power	lastOccupier = null;
		private final int provIdx;
		
		
		public ProvinceData(int provIdx)
		{
			this.provIdx = provIdx;
		}// ProvinceData()
		
		// get the index
		public int getIdx()						{ return provIdx; }
		
		// unit set/get
		public boolean hasUnit() 				{ return (unit != null); }
		public boolean hasDislodgedUnit()		{ return (dislodgedUnit != null); }
		
		public Unit getUnit()					{ return unit; }
		public Unit getDislodgedUnit()			{ return dislodgedUnit; }
		
		public void setUnit(Unit u)  			{ unit = u; }
		public void setDislodgedUnit(Unit u) 	{ dislodgedUnit = u; }
		
		
		// SC set/get
		public boolean isSCAHome() 			{ return (SCHomePower != null); }
		public boolean isSCOwned()			{ return (SCOwner != null); }
		
		public void setSCOwner(Power p)		{ SCOwner = p; }
		public void setSCHomePower(Power p)	{ SCHomePower = p; }
		
		public Power getSCOwner()			{ return SCOwner; }
		public Power getSCHomePower()		{ return SCHomePower; }
		
		// occupier set/get
		public Power getLastOccupier()			{ return lastOccupier; }
		public void setLastOccupier(Power p)	{ lastOccupier = p; }
		
		// determine if all fields are null!.
		public boolean isAllNull()
		{
			return( unit==null && dislodgedUnit==null 
					&& SCOwner==null && SCHomePower==null
					&& lastOccupier==null );
		}// isImportant()
		
		// normal clone
		public ProvinceData normClone()
		{
			ProvinceData pd = new ProvinceData(getIdx());
			
			// deep copy unit information
			if(unit != null)
			{
				pd.unit = (Unit) unit.clone();
			}
			
			if(dislodgedUnit != null)
			{
				pd.dislodgedUnit = (Unit) dislodgedUnit.clone();
			}
			
			// shallow copy Powers [Power is immutable]
			pd.SCOwner = this.SCOwner;
			pd.SCHomePower = this.SCHomePower;
			pd.lastOccupier = this.lastOccupier;
			
			return pd;
		}// normClone()
		
		
		/** Returns null if no non-ownership information exists */
		public ProvinceData cloneExceptUnits()
		{
			// don't create an object if there is no ownership info.
			// this also compacts the Position map!
			if(SCOwner == null && SCHomePower == null && lastOccupier == null)
			{
				return null;
			}
			
			// create a ProvinceData object
			ProvinceData pd = new ProvinceData(getIdx());
			
			// shallow copy Power [Power is immutable]
			pd.SCOwner = this.SCOwner;
			pd.SCHomePower = this.SCHomePower;
			pd.lastOccupier = this.lastOccupier;
			
			return pd;
		}// cloneExceptUnits()
		
		/** Returns null if no ownership info/unit exists */
		public ProvinceData cloneExceptDislodged()
		{
			// don't create an object if there is no ownership info.
			// this also compacts the Position map!
			if(SCOwner == null && SCHomePower == null && unit == null  && lastOccupier == null)
			{
				return null;
			}
			
			// create a ProvinceData object
			ProvinceData pd = new ProvinceData(getIdx());
			
			// shallow copy Power [Power is immutable]
			pd.SCOwner = this.SCOwner;
			pd.SCHomePower = this.SCHomePower;
			pd.lastOccupier = this.lastOccupier;
			
			// deep copy unit
			if(unit != null)
			{
				pd.unit = (Unit) unit.clone();
			}
			
			return pd;
		}// cloneExceptUnits()
		
		
	}// inner class ProvinceData
	
	
	/** All mutable Power data is kept here */
	private static class PowerData
	{
		// instance variables
		private boolean isEliminated = false;
		private final int index;
		
		public PowerData(int index)
		{
			this.index = index;
		}// PowerData()
		
		public int getIndex()						{ return index; }
		public boolean isEliminated() 				{ return isEliminated; }
		public void setEliminated(boolean value)	{ isEliminated = value; }
		
		public PowerData normClone()
		{
			PowerData pd = new PowerData(this.index);
			pd.isEliminated = this.isEliminated;
			return pd;
		}// normClone()
	}// inner class PowerData
	
	
	
	/**
	*	XStream serialization
	*/
	public static class ProvinceDataConverter implements Converter
	{
		public ProvinceDataConverter(ClassMapper cm)
		{
			cm.alias("position", ProvinceData.class, ProvinceData.class);
		}// ProvinceDataConverter()
		
		
		public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
			MarshallingContext context)
		{
			final XMLSerializer xs = XMLSerializer.get(context);
			final ProvinceData pd = (ProvinceData) source;
			
			hsw.addAttribute( "province", 
				xs.toString( xs.getMap().getProvince(pd.getIdx()) ) );
			
			if(pd.getLastOccupier() != null)
			{
				hsw.addAttribute( "lastOccupier", xs.toString(pd.getLastOccupier()) );
			}
			
			if(pd.hasUnit())
			{
				addUnit(hsw, xs, pd.getUnit(), false);
			}
			
			if(pd.hasDislodgedUnit())
			{
				addUnit(hsw, xs, pd.getDislodgedUnit(), true);
			}
			
			if(pd.isSCAHome() || pd.isSCOwned())
			{
				hsw.startNode("SC");
				hsw.addAttribute("owner", xs.toString(pd.getSCOwner()));
				hsw.addAttribute("homePower", xs.toString(pd.getSCHomePower()));
				hsw.endNode();
			}
		}// marshal()
		
		private void addUnit(HierarchicalStreamWriter hsw, XMLSerializer xs, 
			Unit unit, boolean isDislodged)
		{
			hsw.startNode("unit");
			
			hsw.addAttribute("power", xs.toString(unit.getPower()));
			
			if(unit.getCoast().isDirectional())
			{
				hsw.addAttribute("coast", unit.getCoast().getAbbreviation());
			}
			
			hsw.addAttribute("type", xs.toString(unit.getType()));
			
			// assume not dislodged.
			if(isDislodged)
			{
				hsw.addAttribute("dislodged", String.valueOf(isDislodged));
			}
			
			hsw.endNode();
		}// addUnit()
		
		public boolean canConvert(java.lang.Class type)
		{
			return type.equals(ProvinceData.class);
		}// canConvert()
		
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
		{
			final XMLSerializer xs = XMLSerializer.get(context);
			
			final Province prov = xs.getProvince(reader.getAttribute("province"));
			final ProvinceData pd = new ProvinceData(prov.getIndex());
			
			String tmp = reader.getAttribute("lastOccupier");
			if(tmp != null)
			{
				pd.setLastOccupier( xs.getPower(tmp) );
			}
			
			while(reader.hasMoreChildren())
			{
				reader.moveDown();
				
				final String nodeName = reader.getNodeName();
				
				if("SC".equals(nodeName))
				{
					pd.setSCOwner( xs.getPower(reader.getAttribute("owner")) );
					pd.setSCHomePower( xs.getPower(reader.getAttribute("homePower")) );
				}
				else if("unit".equals(nodeName))
				{
					// <unit power="Italy" type="fleet" coast="" dislodged="true"/>
					// COAST and DISLODGED are optional
					
					final Unit unit = new Unit(
						xs.getPower(reader.getAttribute("power")),
						xs.getUnitType(reader.getAttribute("type")) );
					
					Coast coast = Coast.UNDEFINED;
					tmp = reader.getAttribute("coast");
					if(tmp != null)
					{
						coast = xs.getCoast(tmp);
					}
					
					final boolean isDislodged = Boolean.valueOf(reader.getAttribute("dislodged")).booleanValue();
					
					// validate unit coast (absolutely required if coast not specified)
					try
					{
						Location loc = new Location(prov, coast);
						loc = loc.getValidated(unit.getType());
						unit.setCoast(loc.getCoast());
					}
					catch(OrderException e)
					{
						throw new ConversionException(e.getMessage());
					}
					
					// add unit
					if(isDislodged)
					{
						pd.setDislodgedUnit(unit);
					}
					else
					{              
						pd.setUnit(unit);
					}
				}
				
				reader.moveUp();
			}
			
			return pd;
		}// unmarshal()
			
	}// ProvinceDataConverter()
	
	/**
	*	XStream serialization
	*/
	public static class PositionConverter implements Converter
	{
		private final ClassMapper cm;
		
		public PositionConverter(ClassMapper cm)
		{
			this.cm = cm;
			cm.alias("positions", Position.class, Position.class);
		}// PositionConverter()
		
		public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
			MarshallingContext context)
		{
			Position pos = (Position) source;
			
			for(int i=0; i<pos.provArray.length; i++)
			{
				final ProvinceData pd = pos.provArray[i];
				if(pd != null && !pd.isAllNull())
				{
					hsw.startNode(cm.lookupName(pd.getClass()));
					context.convertAnother(pd);
					hsw.endNode();
				}
			}
		}
		
		public boolean canConvert(java.lang.Class type)
		{
			return type.equals(Position.class);
		}
		
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
		{
			final XMLSerializer xs = XMLSerializer.get(context);
			final Position position = new Position(xs.getMap());
			
			while(reader.hasMoreChildren())
			{
				reader.moveDown();
				final Class cls = cm.lookupType(reader.getNodeName());
				final ProvinceData pd = (ProvinceData) context.convertAnother(context, cls);
				position.provArray[pd.getIdx()] = pd;
				reader.moveUp();
			}
			
			return position;
		}// unmarshal()
			
	}// PositionConverter()	
}// class Position

