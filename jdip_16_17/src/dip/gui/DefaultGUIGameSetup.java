//
//  @(#)GUIGameSetup.java		6/2003
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
import dip.world.io.converter.AbstractConverter;

import dip.gui.map.*;
import dip.gui.undo.UndoRedoManager;

import java.awt.*;

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
public class DefaultGUIGameSetup implements GUIGameSetup
{
	/* static setup */
	static
	{
		XMLSerializer.registerConverter(new DefaultGUIGameSetupConverter());
	}
	
	/** Setup the game. */
	public void setup(ClientFrame cf, World world)
	{
		// create right-panel components
		OrderDisplayPanel odp = new OrderDisplayPanel(cf);
		OrderStatusPanel osp = new OrderStatusPanel(cf);
		
		cf.setOrderDisplayPanel( odp );
		cf.setOrderStatusPanel( osp );
		
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
		final Power[] powers = Power.toArray( world.getMap().getPowerList() );
		cf.fireDisplayablePowersChanged(cf.getDisplayablePowers(), powers);
		cf.fireOrderablePowersChanged(cf.getOrderablePowers(), powers);
		cf.fireTurnstateChanged( world.getLastTurnState() );
	}// setup()
	
	
	/** We do not need to save any data. */
	public void save(ClientFrame cf)	{}
	
	/** For XStream serialization */
	private static class DefaultGUIGameSetupConverter extends AbstractConverter
	{
		
		public void alias()
		{
			getCM().alias("setup-default", DefaultGUIGameSetup.class, 
				DefaultGUIGameSetup.class);
		}// alias()
		
		public boolean canConvert(Class type)
		{
			return type.equals(DefaultGUIGameSetup.class);
		}// canConvert()
		
		public void marshal(Object source, 
			HierarchicalStreamWriter hsw, MarshallingContext context)
		{
			// do nothing;
		}// marshal()
			
		public Object unmarshal(HierarchicalStreamReader reader, 
			UnmarshallingContext context)
		{
			return new DefaultGUIGameSetup();
		}// unmarshal()
			
	}// inner class DefaultGUIGameSetupConverter
}// class DefaultGUIGameSetup
