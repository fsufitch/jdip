//
//  @(#)WorldConverter.java		9/2004
//
//  Copyright 2004 Zachary DelProposto. All rights reserved.
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
package dip.world.io.converter;

import dip.world.io.XMLSerializer;  

import dip.world.Phase;
import dip.world.Power;
import dip.world.InvalidWorldException;
import dip.world.TurnState;
import dip.world.VictoryConditions;
import dip.world.GameSetup;
import dip.world.World;
import dip.world.World.VariantInfo;
import dip.world.WorldFactory;

import dip.world.variant.data.Variant;
import dip.world.variant.VariantManager;

import dip.world.metadata.GameMetadata;
import dip.world.metadata.PlayerMetadata;

import dip.order.OrderFactory;

import dip.misc.Log;

import java.util.*;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;
import com.thoughtworks.xstream.alias.CannotResolveClassException;

/**
*	XStream Converter
*/
public class WorldConverter implements Converter
{
	private final ClassMapper cm;
	private final String creator;
	private final String creatorVersion;
	private final String specification;
	private final OrderFactory orderFactory;
	
	public WorldConverter(ClassMapper cm, OrderFactory orderFactory, 
		String creator, String creatorVersion, String specification)
	{
		this.cm = cm;
		this.orderFactory = orderFactory;
		this.creator = creator;
		this.creatorVersion = creatorVersion;
		this.specification = specification;
		cm.alias("game", World.class, World.class);
	}// WorldConverter()
	
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		Log.println("WorldConverter().marshal()");
		final World world = (World) source;
		final XMLSerializer xs = XMLSerializer.get(context);
		
		// set World and Map in XMLSerializer
		xs.setMap(world.getMap());
		xs.setWorld(world);
		
		hsw.addAttribute("creator", creator);
		hsw.addAttribute("creatorVersion", creatorVersion);
		hsw.addAttribute("specification", specification);
		
		Log.println("  - writing <variant>");
		// <variant> element
		xs.lookupAndWriteNode(world.getVariantInfo(), cm, hsw, context);
		
		// <info> element
		Log.println("  - writing <info>");
		hsw.startNode("info");
		xs.lookupAndWriteNode(world.getGameMetadata(), cm, hsw, context);
		
		Iterator iter = world.getMap().getPowerList().iterator();
		while(iter.hasNext())
		{
			final Power power = (Power) iter.next(); 
			final PlayerMetadata pmd = world.getPlayerMetadata(power);
			xs.lookupAndWriteNode(pmd, cm, hsw, context);
		}
		
		hsw.endNode();
		
		// <setup> element
		Log.println("  - writing <setup>");
		hsw.startNode("setup");
		xs.lookupAndWriteNode(world.getGameSetup(), cm, hsw, context);
		hsw.endNode();
		
		// <turn> element(s)
		Log.println("  - writing <turn> elements");
		iter = world.getAllTurnStates().iterator();
		while(iter.hasNext())
		{
			final TurnState ts = (TurnState) iter.next();
			Log.println("     <turn> : ", ts.getPhase());
			xs.lookupAndWriteNode(ts, cm, hsw, context);
		}
		
		Log.println("WorldConverter().marshal() complete");
	}// marshal()
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(World.class);
	}// canConvert()
	
	/** Returns a World object */
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		// setup XMLSerializer
		final XMLSerializer xs = XMLSerializer.get(context);
		xs.setOrderFactory(orderFactory);
		
		// Creator / CreatorVersion from file. 
		// we don't do anything with these yet.
		final String readCreator = reader.getAttribute("creator");
		final String readCreatorVersion = reader.getAttribute("creatorVersion");
		
		// check specification
		final String readSpec = reader.getAttribute("specification");
		if(!specification.equals(readSpec))
		{
			throw new ConversionException("Specification "+readSpec+" not supported.");
		}
		
		// Start reading children. The <variant> element *must* come first.
		if(reader.hasMoreChildren())
		{
			reader.moveDown();
			try
			{
				createWorld((VariantInfo) context.convertAnother(context, 
					VariantInfo.class), xs, context);
			}
			catch(CannotResolveClassException e)
			{
				throw new ConversionException("Expected first element, \"variant\", invalid or missing.");
			}
			reader.moveUp();
		}
		
		boolean victoryConditionsSet = false;
		
		// Start reading the other children.
		while(reader.hasMoreChildren())
		{
			reader.moveDown();
			final String nodeName = reader.getNodeName();
			
			if("info".equals(nodeName))
			{
				while(reader.hasMoreChildren())
				{
					reader.moveDown();
					Object obj = xs.lookupAndReadNode(cm, reader, context);
					
					if(obj instanceof PlayerMetadata)
					{
						PlayerMetadata pmd = (PlayerMetadata) obj;
						xs.getWorld().setPlayerMetadata(pmd.getPower(), pmd);
					}
					else if(obj instanceof GameMetadata)
					{
						GameMetadata gmd = (GameMetadata) obj;
						xs.getWorld().setGameMetadata(gmd);
					}
					else
					{
						Log.println("<info> ignored unknown node: ", nodeName);
					}
					
					reader.moveUp();
				}
			}
			else if("setup".equals(nodeName))
			{
				// if no children, do not set.
				while(reader.hasMoreChildren())
				{
					reader.moveDown();
					
					Object obj = xs.lookupAndReadNode(cm, reader, context);
					if(obj instanceof GameSetup)
					{
						xs.getWorld().setGameSetup((GameSetup) obj);
					}
					else
					{
						Log.println("<setup> ignored unknown node: ", nodeName);
					}
					
					reader.moveUp();
				}
			}
			else
			{
				// should only be <turn> elements....
				Class cls = null;
				
				try
				{
					cls = cm.lookupType(nodeName);
				}
				catch(CannotResolveClassException e)
				{
					Log.println("<turn> expected. Ignored unresolvable node: ", nodeName);
				}
					
				if(cls != null)
				{
					final Object obj = context.convertAnother(context, cls);
					
					if(cls.equals(TurnState.class))
					{
						final TurnState ts = (TurnState) obj;
						ts.setWorld(xs.getWorld());
						xs.getWorld().setTurnState(ts);
						
						// using phase from first turn, set the VictoryConditions
						if(!victoryConditionsSet)
						{
							VictoryConditions tmpVC = xs.getWorld().getVictoryConditions();
							
							VictoryConditions vc = new VictoryConditions(
								tmpVC.getSCsRequiredForVictory(), 
								tmpVC.getYearsWithoutSCChange(),
								tmpVC.getMaxGameDurationYears(), 
								ts.getPhase()	/* This is what we are fixing */
							);
							
							xs.getWorld().setVictoryConditions(vc);
							victoryConditionsSet = true;
						}
					}
					else
					{
						Log.println("<turn> expected. Ignored resolved node: ", nodeName);
					}
				}
			}
			
			reader.moveUp();
		}// while()
		
		// set turn flags
		setSCChanged(xs.getWorld());
		
		return xs.getWorld();
	}// unmarshal()
	
	
	/** Create the Map and World */
	private void createWorld(VariantInfo vi, XMLSerializer xs, 
		UnmarshallingContext context)
	{
		World world = null;
		
		try
		{
			Variant variant = VariantManager.getVariant(vi.getVariantName(),
				vi.getVariantVersion());
			
			// if null, try a newer version....
			if(variant == null)
			{
				Log.println("WorldConverter: not found: variant ", 
					vi.getVariantName(), "; name: ", String.valueOf(vi.getVariantVersion()));
				Log.println("WorldConverter: attempting to use newer variant");
				
				variant = VariantManager.getVariant(vi.getVariantName(), 
					VariantManager.VERSION_NEWEST);
				
				if(variant == null)
				{
					throw new ConversionException("variant not found: "+vi.getVariantName());
				}
			}
			
			WorldFactory wf = WorldFactory.getInstance();
			world = wf.createWorld( variant );
			
			// set basic variant parameters. The version may have changed, 
			// for example, if we couldn't find it above.
			World.VariantInfo variantInfo = world.getVariantInfo();
			variantInfo.setVariantVersion( variant.getVersion() );
		}
		catch(InvalidWorldException iwe)
		{
			throw new ConversionException(iwe.getMessage());
		}
		
		// world setup
		world.setVariantInfo(vi);
		
		// XMLSerializer final setup
		// (other converters depend upon these values)
		xs.setWorld(world);
		xs.setMap(world.getMap());
	}// createWorld()
	
	
	/** 
	*	Set SC changed flag on TurnState objects, after all TurnState objects
	*	have been loaded.
	*/
	private void setSCChanged(World w)
	{
		/*
			Algorithm: 
				Iterate through each FALL turnstate.
				1) set flag to 'true' if there is an ADJUSTMENT phase.
					that is sufficient for that year.
				
				2) if there is no ADJUSTMENT phase, there may have been
					a zero-sum change of supply centers, or none at all.
					We then have to compare the turnstate with the previous
					FALL turnstate.
		
		*/
		TurnState lastFallTS = null;
		TurnState currentFallTS = null;
		
		Iterator iter = w.getAllTurnStates().iterator();
		while(iter.hasNext())
		{
			final TurnState ts = (TurnState) iter.next();
			boolean hasAdjustmentPhase = false;
			if(Phase.SeasonType.FALL.equals(ts.getPhase().getSeasonType()))
			{
				currentFallTS = ts;
				hasAdjustmentPhase = Phase.PhaseType.ADJUSTMENT.equals(ts.getPhase().getPhaseType());
			}
			
			if(hasAdjustmentPhase)
			{
				currentFallTS.setSCOwnerChanged(true);
			}
			else
			{
				// more complex. We need to compare to prior.
				if(lastFallTS != null)
				{
					currentFallTS.setSCOwnerChanged(
						currentFallTS.getPosition().isSCChanged(
							lastFallTS.getPosition()) );
				}
			}
			
			lastFallTS = currentFallTS;
		}
	}// setSCChanged()
	
}// WorldConverter()		

