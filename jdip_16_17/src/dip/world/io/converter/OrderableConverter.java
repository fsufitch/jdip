//
//  @(#)OrderableConverter.java		9/2004
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

import java.io.*;
import java.util.*;

import dip.world.TurnState;

import dip.order.Orderable;
import dip.order.OrderException;
import dip.order.OrderFactory;
import dip.order.OrderFormatOptions;
import dip.order.OrderParser;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;

public class OrderableConverter implements Converter
{
	
	private final OrderFormatOptions ofo;
	private final OrderParser parser;
	
	public OrderableConverter(ClassMapper cm)
	{
		ofo = OrderFormatOptions.createDefault();
		parser = OrderParser.getInstance();
		cm.alias("order", Orderable.class, Orderable.class);
	}// OrderableConverter()
	
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final Orderable ord = (Orderable) source;
		final Map map = xs.getMarshallingOrderMap();
		
		String id = (String) map.get(ord);
		if(id == null)
		{
			// value-type
			id = makeOrderID(ord, xs);
			map.put(ord, id);
			
			String text = ord.toFormattedString(ofo);
			hsw.addAttribute("id", id);
			hsw.addAttribute("text", text);
		}
		else
		{
			// reference-type
			hsw.addAttribute("idref", id);
		}
	}// marshal()
	
	
	private String makeOrderID(Orderable ord, XMLSerializer xs)
	{
		// format: #first letter of powername . turn# . unique
		StringBuffer sb = new StringBuffer(32);
		sb.append( xs.toString(ord.getPower()).charAt(0) );
		sb.append('.');
		sb.append( xs.getTurnNumber() );
		sb.append('.');
		sb.append( xs.getUnique() );
		return sb.toString();
	}// makeOrderID()
	
	
	/**
	*	Allows conversion from any Orderable implementation
	*/
	public boolean canConvert(java.lang.Class type)
	{
		return Orderable.class.isAssignableFrom(type);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final Map map = xs.getUnmarshallingOrderMap();
		final TurnState ts = xs.getCurrentTurnState();
		final OrderFactory orderFactory = xs.getOrderFactory();
		
		Orderable order = null;
		
		final String idref = reader.getAttribute("idref");
		if(idref != null)
		{
			// reference type
			order = (Orderable) map.get(idref);
			if(order == null) { throw new ConversionException("order ID \""+idref+"\" not yet defined"); }
		}
		else
		{
			// order type
			final String id = reader.getAttribute("id");
			if(id == null) { throw new ConversionException("missing order ID"); }
			
			final String txt = reader.getAttribute("text");
			if(txt == null) { throw new ConversionException("missing order text"); }
			
			try
			{
				order = parser.parse(orderFactory, txt, null, 
							xs.getCurrentTurnState(), false, false);
			}
			catch(OrderException e)
			{
				throw new ConversionException(e.getMessage());
			}
			
			// add to map
			map.put(id, order);
		}
		
		return order;
	}// unmarshal()	
}// OrderableConverter
