//
//  @(#)F2FGUIGameSetup.java		6/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.gui;

import dip.world.GameSetup;
import dip.world.TurnState;
import dip.world.World;
import dip.world.Power;
import dip.world.io.XMLSerializer;
import dip.world.io.NameValuePair;
import dip.world.io.converter.AbstractConverter;

import dip.gui.F2FOrderDisplayPanel;
import dip.gui.F2FOrderDisplayPanel.F2FState;
import dip.gui.map.*;
import dip.gui.undo.UndoRedoManager;

import dip.misc.Log;

import java.awt.*;
import java.util.Iterator;
import java.util.Map;
import javax.swing.*;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;

/**
*	The Default GameSetup. This is used when we are not in face-
*	to-face or a network mode. All powers may have their orders 
*	entered and displayed. The last turnstate is always made the
*	current turnstate.
*/
public class F2FGUIGameSetup implements GUIGameSetup
{
	/* static setup */
	static
	{
		XMLSerializer.registerConverter(new F2FGUIGameSetupConverter());
	}
	
	/* instance fields */
	private F2FState state;
	
	
	/** Create a F2FGUIGameSetup */
	public F2FGUIGameSetup()
	{
		// set default state
		state = new F2FOrderDisplayPanel.F2FState();
	}// F2FGUIGameSetup()
	
	
	/** Setup the game. */
	public void setup(ClientFrame cf, World world)
	{
		// create right-panel components
		F2FOrderDisplayPanel odp = new F2FOrderDisplayPanel(cf);
		OrderStatusPanel osp = new OrderStatusPanel(cf);
		
		cf.setOrderDisplayPanel( odp );
		cf.setOrderStatusPanel( osp );
		
		// restore as appropriate
		odp.restoreState(getState());
		
		// right-panel layout
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		rightPanel.add(osp, BorderLayout.NORTH);
		rightPanel.add(odp, BorderLayout.CENTER);
		cf.getJSplitPane().setRightComponent(rightPanel);
		
		// setup map panel (left-panel)
		MapPanel mp = new MapPanel(cf);
		cf.setMapPanel( mp );
		cf.getJSplitPane().setLeftComponent( mp );
		
		// create the undo/redo manager
		cf.setUndoRedoManager(new UndoRedoManager(cf, odp));
		
		cf.getJSplitPane().setVisible(true);
		
		// inform everybody about the World
		cf.fireWorldCreated(world);
		cf.getUndoRedoManager().reconstitute();
		
		// set turnstate and powers
		cf.fireDisplayablePowersChanged(cf.getDisplayablePowers(), world.getMap().getPowers());
		cf.fireOrderablePowersChanged(cf.getOrderablePowers(), world.getMap().getPowers());
		cf.fireTurnstateChanged( world.getLastTurnState() );
	}// setup()
	
	
	/** Save the Current Power */
	public void save(ClientFrame cf)
	{
		F2FOrderDisplayPanel fodp = (F2FOrderDisplayPanel) cf.getOrderDisplayPanel();
		setState( fodp.getState() );
	}// save()
	
	/** Safe Set */
	private synchronized void setState(F2FState value)
	{
		this.state = value;
	}// setState()
	
	/** Safe Get : NOTE: does not copy */
	private synchronized F2FState getState()
	{
		return state;
	}// getState()
	
	
	/** For XStream serialization */
	private static class F2FGUIGameSetupConverter extends AbstractConverter
	{
		
		public void alias()
		{
			getCM().alias("setup-f2f", F2FGUIGameSetup.class, 
				F2FGUIGameSetup.class);
		}// alias()
		
		public boolean canConvert(Class type)
		{
			return type.equals(F2FGUIGameSetup.class);
		}// canConvert()
		
		public void marshal(Object source, 
			HierarchicalStreamWriter hsw, MarshallingContext context)
		{
			final F2FGUIGameSetup ggs = (F2FGUIGameSetup) source;
			final XMLSerializer xs = XMLSerializer.get(context);
			final Power[] powers = xs.getWorld().getMap().getPowers();
			final ClassMapper cm = getCM();
			
			xs.writeNVP("currentPower", xs.toString(ggs.getState().getCurrentPower()), cm, hsw, context);
			
			hsw.startNode("submittedOrders");
			Iterator iter = ggs.getState().iterator();
			while(iter.hasNext())
			{
				Map.Entry entry = (Map.Entry) iter.next();
				xs.writeNVP(
					xs.toString((Power) entry.getKey()), 
					String.valueOf(entry.getValue()), 
					cm, hsw, context);
			}
			hsw.endNode();
		}// marshal()
			
		public Object unmarshal(HierarchicalStreamReader reader, 
			UnmarshallingContext context)
		{
			final XMLSerializer xs = XMLSerializer.get(context);
			final F2FGUIGameSetup setup = new F2FGUIGameSetup();
			final F2FState state = new F2FState();
			
			while(reader.hasMoreChildren())
			{
				reader.moveDown();
				
				final String nodeName = reader.getNodeName();
				if("submittedOrders".equals(nodeName))
				{
					while(reader.hasMoreChildren())
					{
						reader.moveDown();
						NameValuePair nvp = (NameValuePair) xs.lookupAndReadNode(getCM(), reader, context);
						state.setSubmitted(
							xs.getPower(nvp.getName()),
							Boolean.valueOf(nvp.getValue()).booleanValue()
						);
						reader.moveUp();
					}
				}
				else
				{
					NameValuePair nvp = (NameValuePair) xs.lookupAndReadNode(getCM(), reader, context);
					if("currentPower".equals(nvp.getName()))
					{
						state.setCurrentPower(xs.getPower(nvp.getValue()));
					}
					else
					{
						Log.println("<setup-f2f> ignored unrecognized NVP: ", nvp.getName());
					}
				}
				
				reader.moveUp();
			}
			
			setup.setState(state);
			return setup;
		}// unmarshal()
			
	}// inner class F2FGUIGameSetupConverter
}// class F2FGUIGameSetup
