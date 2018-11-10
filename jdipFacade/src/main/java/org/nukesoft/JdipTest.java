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

import org.nukesoft.jdipFacade.HeadlessImplementationStrategy;
import org.nukesoft.jdipFacade.JdipAdjudicatorFacade;
import org.nukesoft.jdipFacade.JdipMapInfo;
import org.nukesoft.jdipFacade.JdipResult;
import org.nukesoft.jdipFacade.JdipWorld;
import org.nukesoft.jdipFacade.JdipWorldFactory;
import org.nukesoft.jdipFacade.exception.JdipFacadeException;
import org.nukesoft.jdipFacade.exception.UnitNotFoundException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Must specify path to Jdip install root as command line parameter
 *
 * @author Ryan Michela
 */
public class JdipTest {

    public static void main(String[] args)
            throws IOException {
        //System.setProperty("java.awt.headless", "true");
        try {
            String jdipRootPath = args[0];
            JdipAdjudicatorFacade.initializeJdip(jdipRootPath, new HeadlessImplementationStrategy(true));
            JdipWorldFactory worldFactory = JdipAdjudicatorFacade.getJdipWorldFactory();
            String[] variants = worldFactory.getVariantNames();
            System.out.println("*Loaded Variants:");
            for (String variant : variants) {
                System.out.println("--- " + variant);
            }
            String variant = "Standard";
            System.out.println("Using Variant = " + variant);

            System.out.println("*Creating world");
            JdipWorld world = worldFactory.createWorld(variant);
            System.out.println("*Issuing orders");

            String powerName;
            String order;
            String[] powers = world.getPowerNames();
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.println(world.getCurrentPhaseTitle());
                System.out.print("Enter power (or exit or stats or provinceinfo or go) - ");
                for (String power : powers) {
                    if (world.getPower(power).isEliminated()) {
                        System.out.print("(X)");
                    }
                    if ((world.getPower(power).getOptimalUnitAdjustments() != 0)) {
                        System.out.print("(" + world.getPower(power).getActualUnitAdjustments()
                                + "/"
                                + world.getPower(power).getOptimalUnitAdjustments()
                                + ")");
                    }
                    if (world.getPower(power).getDislodgedUnitCount() != 0) {
                        System.out.print("(D)");
                    }
                    System.out.print(power + " ");
                }
                System.out.println();
                System.out.print("> ");
                powerName = br.readLine();

                //exit
                if (powerName.equals("exit")) {
                    System.out.println("Done.");
                    break;
                }
                //adjudicate
                else if (powerName.equals("go")) {
                    System.out.println("*Adjudicating");
                    JdipAdjudicatorFacade.adjudicate(world);
                    System.out.println("Results for " + world.getLastPhaseTitle());
                    JdipResult[] general = world.getAllGeneralResults(JdipWorld.ORDER_FORMAT_VERBOSE);
                    System.out.println("==========General==========");
                    for (JdipResult aGeneral : general) {
                        System.out.println(aGeneral.getMessage());
                    }
                    for (String power : powers) {
                        System.out.println("==========" + power + "==========");
                        JdipResult[] powerResults = world.getAllResultsForPower(world.getPower(power), JdipWorld.ORDER_FORMAT_VERBOSE);
                        for (JdipResult result : powerResults) {
                            if (result.isGeneralResult()) {
                                System.out.println(result.getMessage());
                            } else {
                                if (result.isOrderLinkedResult()) {
                                    if (result.isSuccessfull()) {
                                        System.out.print("+++ ");
                                    } else {
                                        System.out.print("--- ");
                                    }
                                    String message = result.getMessage();
                                    if (result.isOrderLinkedResult()) {
                                        System.out.println(result.getFormattedOrder());
                                        if (!message.equals("")) System.out.println("    " + message);
                                    }
                                } else {
                                    System.out.println(result.getMessage());
                                }
                            }
                        }
                    }
                }
                //print player statistics
                else if (powerName.equals("stats")) {
                    System.out.println("*Printing power statistics");
                    for (String power : powers) {
                        System.out.println("==========" + power + "==========");
                        System.out.println("Number of units: " + world.getPower(power).getUnitCount());
                        System.out.println("Number of supply centers: " + world.getPower(power).getSupplyCenterCount());
                        System.out.println("Number of home supply centers: " + world.getPower(power).getHomeSupplyCenterCount());
                        System.out.println("Number of dislodged units: " + world.getPower(power).getDislodgedUnitCount());
                        System.out.println("Number of optimal adjustments: " + world.getPower(power).getOptimalUnitAdjustments());
                        System.out.println("Number of actual adjustments: " + world.getPower(power).getActualUnitAdjustments());
                    }
                } else if (powerName.equals("provinceinfo")) {
                    System.out.print("Enter province > ");
                    String prov = br.readLine();
                    int unitType = -1;
                    try {
                        unitType = world.getMapInfo().getUnitTypeForProvince(prov);
                    } catch (UnitNotFoundException e) {
                    }
                    System.out.print("Unit at province " + prov + ": ");
                    switch (unitType) {
                        case JdipMapInfo.ARMY_TYPE:
                            System.out.println("Army");
                            break;
                        case JdipMapInfo.FLEET_TYPE:
                            System.out.println("Fleet");
                            break;
                        case JdipMapInfo.WING_TYPE:
                            System.out.println("Wing");
                            break;
                        case -1:
                            System.out.println("None");
                            break;
                    }
                    int provType = world.getMapInfo().getProvinceType(prov);
                    System.out.print("Type of province: ");
                    switch (provType) {
                        case JdipMapInfo.LAND_TYPE:
                            System.out.println("Land");
                            break;
                        case JdipMapInfo.COAST_TYPE:
                            System.out.println("Coast");
                            break;
                        case JdipMapInfo.SEA_TYPE:
                            System.out.println("Sea");
                            break;
                    }
                    String[] adj = world.getMapInfo().getAdjacentProvinceNames(prov, JdipMapInfo.SHORT_NAMES);
                    System.out.print("Adjacent provinces: ");
                    for (String anAdj : adj) {
                        System.out.print(anAdj + " ");
                    }
                    System.out.println();
                } else //get orders
                {
                    System.out.print("Selected: " + powerName);
                    if (world.getCurrentPhaseType() == JdipWorld.PHASE_TYPE_ADJUSTMENT) {
                        System.out.println(" Adjustments: " + world.getPower(powerName).getActualUnitAdjustments());
                    } else if (world.getCurrentPhaseType() == JdipWorld.PHASE_TYPE_RETREAT) {
                        System.out.println(" Dislodgements: " + world.getPower(powerName).getDislodgedUnitCount());
                    } else {
                        System.out.println();
                    }
                    System.out.println("Enter orders (or done or list)");
                    System.out.print("> ");
                    String[] orders = new String[world.getPower(powerName).getUnitCount()];
                    int i = 0;
                    order = br.readLine();
                    while (!order.equals("done") && (i < world.getPower(powerName).getUnitCount())) {
                        if (order.equals("list")) {
                            System.out.print("All valid provinces: ");
                            String[] provinces = world.getMapInfo().getAllProvinceNames(JdipMapInfo.SHORT_NAMES);
                            for (String province : provinces) {
                                System.out.print(province + ", ");
                            }
                            System.out.println();
                            System.out.print("All provinces containing non-dislodged units: ");
                            provinces = world.getMapInfo().getAllUnitPositionProvinceNames(JdipMapInfo.SHORT_NAMES);
                            for (String province : provinces) {
                                System.out.print(province + ", ");
                            }
                            System.out.println();
                            System.out.print("All convoy endpoint provinces: ");
                            provinces = world.getMapInfo().getConvoyEndpointProvinces(JdipMapInfo.SHORT_NAMES);
                            for (String province : provinces) {
                                System.out.print(province + ", ");
                            }
                            System.out.println();
                            System.out.print("All " + world.getPower(powerName).getAdjective() + " provinces: ");
                            provinces = world.getMapInfo().getUnitPositionProvinceNamesForPower(world.getPower(powerName), JdipMapInfo.SHORT_NAMES);
                            for (String province : provinces) {
                                System.out.print(province + ", ");
                            }
                            System.out.println();
                            System.out.print("All dislodged " + world.getPower(powerName).getAdjective() + " provinces: ");
                            provinces = world.getMapInfo().getDislodgedUnitProvinceNames(world.getPower(powerName), JdipMapInfo.SHORT_NAMES);
                            for (String province : provinces) {
                                System.out.print(province + ", ");
                            }
                            System.out.println();
                            System.out.print("All " + world.getPower(powerName).getAdjective() + " home supply centers: ");
                            provinces = world.getMapInfo().getHomeSupplyCenterProvinceNames(world.getPower(powerName), JdipMapInfo.SHORT_NAMES);
                            for (String province : provinces) {
                                System.out.print(province + ", ");
                            }
                            System.out.println();
                        } else {
                            orders[i] = order;
                            i++;
                        }
                        System.out.print("> ");
                        order = br.readLine();
                    }
                    System.out.println(world.getPower(powerName).getAdjective() + " orders submitted.");
                    String[] truncOrders = new String[i];
                    for (int j = 0; j < i; j++) {
                        truncOrders[j] = orders[j];
                    }
                    world.setOrders(truncOrders, world.getPower(powerName));
                }
            }
        } catch (JdipFacadeException e) {
            System.out.println(e.getMessage());
        }
    }
}