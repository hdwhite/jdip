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
package org.nukesoft;

import java.io.*;

import org.nukesoft.jdipFacade.*;
import org.nukesoft.jdipFacade.exception.*;


/**
 * Must specify path to Jdip install root as command line parameter
 * @author Ryan Michela
 */
public class JdipTest
{

	public static void main(String[] args)
	throws IOException
	{
		//System.setProperty("java.awt.headless", "true");
		try
		{
			String jdipRootPath = args[0];
			JdipAdjudicatorFacade.initializeJdip(jdipRootPath, new HeadlessImplementationStrategy(true));
			JdipWorldFactory worldFactory = JdipAdjudicatorFacade.getJdipWorldFactory();
			String[] variants = worldFactory.getVariantNames();
			System.out.println("*Loaded Variants:");
			for(int i = 0; i < variants.length; i++)
			{
				System.out.println("--- " + variants[i]);
			}
			String variant = "Standard";
			System.out.println("Using Variant = " + variant);
			
			System.out.println("*Creating world");
			JdipWorld world = worldFactory.createWorld(variant);
			System.out.println("*Issuing orders");
			
			String power = new String();
			String order = new String();
			String[] powers = world.getPowerNames();
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			while(true)
			{
				System.out.println(world.getCurrentPhaseTitle());
				System.out.print("Enter power (or exit or stats or provinceinfo or go) - ");
				for(int i = 0; i < powers.length; i++)
				{
					if(world.getPower(powers[i]).isEliminated())
					{
						System.out.print("(X)");
					}
					if((world.getPower(powers[i]).getOptimalUnitAdjustments() != 0))
					{
						System.out.print("(" + world.getPower(powers[i]).getActualUnitAdjustments() 
								+ "/" 
								+ world.getPower(powers[i]).getOptimalUnitAdjustments() 
								+ ")");
					}
					if(world.getPower(powers[i]).getDislodgedUnitCount() != 0)
					{
						System.out.print("(D)");
					}
					System.out.print(powers[i] + " ");
				}
				System.out.println();
				System.out.print("> ");
				power = br.readLine();
				
				//exit
				if(power.equals("exit"))
				{
					System.out.println("Done.");
					break;
				}
				//adjudicate
				else if(power.equals("go"))
				{
					System.out.println("*Adjudicating");
					JdipAdjudicatorFacade.adjudicate(world);
					System.out.println("Results for " + world.getLastPhaseTitle());
					JdipResult[] general = world.getAllGeneralResults(JdipWorld.ORDER_FORMAT_VERBOSE);					
					System.out.println("==========General==========");
					for(int i = 0; i < general.length; i++)
					{
						System.out.println(general[i].getMessage());
					}
					for(int p = 0; p < powers.length; p++)
					{
						System.out.println("==========" + powers[p] + "==========");
						JdipResult[] powerResults = world.getAllResultsForPower(world.getPower(powers[p]), JdipWorld.ORDER_FORMAT_VERBOSE);
						for(int i = 0; i < powerResults.length; i++)
						{
							JdipResult result = powerResults[i];
							if(result.isGeneralResult())
							{
								System.out.println(result.getMessage());
							}
							else
							{
								if(result.isOrderLinkedResult())
								{
									if(result.isSuccessfull())
									{
										System.out.print("+++ ");
									}
									else
									{
										System.out.print("--- ");
									}
									String message = result.getMessage();
									if(result.isOrderLinkedResult())
									{
										System.out.println(result.getFormattedOrder());
										if(!message.equals("")) System.out.println("    " + message);
									}
								}
								else
								{
									System.out.println(result.getMessage());
								}
							}
						}
					}
				}
				//print player statistics
				else if(power.equals("stats"))
				{
					System.out.println("*Printing power statistics");
					for(int p = 0; p < powers.length; p++)
					{
						System.out.println("==========" + powers[p] + "==========");
						System.out.println("Number of units: " + world.getPower(powers[p]).getUnitCount());
						System.out.println("Number of supply centers: " + world.getPower(powers[p]).getSupplyCenterCount());
						System.out.println("Number of home supply centers: " + world.getPower(powers[p]).getHomeSupplyCenterCount());
						System.out.println("Number of dislodged units: " + world.getPower(powers[p]).getDislodgedUnitCount());
						System.out.println("Number of optimal adjustments: " + world.getPower(powers[p]).getOptimalUnitAdjustments());
						System.out.println("Number of actual adjustments: " + world.getPower(powers[p]).getActualUnitAdjustments());
					}
				}
				else if(power.equals("provinceinfo"))
				{
					System.out.print("Enter province > ");
					String prov = br.readLine();
					int unitType = -1;
					try
					{
						unitType = world.getMapInfo().getUnitTypeForProvince(prov);
					}
					catch(UnitNotFoundException e){}
					System.out.print("Unit at province " + prov + ": ");
					switch(unitType)
					{
						case JdipMapInfo.ARMY_TYPE: System.out.println("Army"); break;
						case JdipMapInfo.FLEET_TYPE: System.out.println("Fleet"); break;
						case JdipMapInfo.WING_TYPE: System.out.println("Wing"); break;
						case -1: System.out.println("None"); break;
					}
					int provType = world.getMapInfo().getProvinceType(prov);
					System.out.print("Type of province: ");
					switch(provType)
					{
						case JdipMapInfo.LAND_TYPE: System.out.println("Land"); break;
						case JdipMapInfo.COAST_TYPE: System.out.println("Coast"); break;
						case JdipMapInfo.SEA_TYPE: System.out.println("Sea"); break;
					}
					String[] adj = world.getMapInfo().getAdjacentProvinceNames(prov, JdipMapInfo.SHORT_NAMES);
					System.out.print("Adjacent provinces: ");
					for(int p = 0; p < adj.length; p++)
					{
						System.out.print(adj[p] + " ");
					}
					System.out.println();
				}
				else //get orders
				{
					System.out.print("Selected: " + power);
					if(world.getCurrentPhaseType() == JdipWorld.PHASE_TYPE_ADJUSTMENT)
					{
						System.out.println(" Adjustments: " + world.getPower(power).getActualUnitAdjustments());
					}
					else if(world.getCurrentPhaseType() == JdipWorld.PHASE_TYPE_RETREAT)
					{
						System.out.println(" Dislodgements: " + world.getPower(power).getDislodgedUnitCount());
					}
					else
					{
						System.out.println();
					}
					System.out.println("Enter orders (or done or list)");
					System.out.print("> ");
					String[] orders = new String[world.getPower(power).getUnitCount()];
					int i = 0;
					order = br.readLine();
					while(!order.equals("done") && (i < world.getPower(power).getUnitCount()))
					{
						if(order.equals("list"))
						{
							System.out.print("All valid provinces: ");
							String[] provinces = world.getMapInfo().getAllProvinceNames(JdipMapInfo.SHORT_NAMES);
							for(int p = 0; p < provinces.length; p++)
							{
								System.out.print(provinces[p] + ", ");
							}
							System.out.println();
							System.out.print("All provinces containing non-dislodged units: ");
							provinces = world.getMapInfo().getAllUnitPositionProvinceNames(JdipMapInfo.SHORT_NAMES);
							for(int p = 0; p < provinces.length; p++)
							{
								System.out.print(provinces[p] + ", ");
							}
							System.out.println();
							System.out.print("All convoy endpoint provinces: ");
							provinces = world.getMapInfo().getConvoyEndpointProvinces(JdipMapInfo.SHORT_NAMES);
							for(int p = 0; p < provinces.length; p++)
							{
								System.out.print(provinces[p] + ", ");
							}
							System.out.println();
							System.out.print("All " + world.getPower(power).getAdjective() + " provinces: ");
							provinces = world.getMapInfo().getUnitPositionProvinceNamesForPower(world.getPower(power), JdipMapInfo.SHORT_NAMES);
							for(int p = 0; p < provinces.length; p++)
							{
								System.out.print(provinces[p] + ", ");
							}
							System.out.println();
							System.out.print("All dislodged " + world.getPower(power).getAdjective() + " provinces: ");
							provinces = world.getMapInfo().getDislodgedUnitProvinceNames(world.getPower(power), JdipMapInfo.SHORT_NAMES);
							for(int p = 0; p < provinces.length; p++)
							{
								System.out.print(provinces[p] + ", ");
							}
							System.out.println();
							System.out.print("All " + world.getPower(power).getAdjective() + " home supply centers: ");
							provinces = world.getMapInfo().getHomeSupplyCenterProvinceNames(world.getPower(power), JdipMapInfo.SHORT_NAMES);
							for(int p = 0; p < provinces.length; p++)
							{
								System.out.print(provinces[p] + ", ");
							}
							System.out.println();
						}
						else
						{
							orders[i] = order;
							i++;
						}
						System.out.print("> ");
						order = br.readLine();
					}
					System.out.println(world.getPower(power).getAdjective() + " orders submitted.");
					String[] truncOrders = new String[i];
					for(int j = 0; j < i; j++)
					{
						truncOrders[j] = orders[j];
					}
					world.setOrders(truncOrders, world.getPower(power));
				}
			}
		}
		catch(JdipFacadeException e)
		{
			System.out.println(e.getMessage());
		}
	}
}