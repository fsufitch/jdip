//
//  @(#)RuleOptionsConverter.java		9/2004
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

import dip.world.RuleOptions;
import dip.world.RuleOptions.Option;
import dip.world.RuleOptions.OptionValue;
import dip.misc.Log;

import java.util.*;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.alias.ClassMapper;

/**
*	XStream Converter
*/
public class RuleOptionsConverter implements Converter
{
	
	
	public RuleOptionsConverter(ClassMapper cm)
	{
		cm.alias("ruleOptions", RuleOptions.class, RuleOptions.class);
	}// RuleOptionsConverter()
	
	
	public void marshal(java.lang.Object source, HierarchicalStreamWriter hsw, 
		MarshallingContext context)
	{
		RuleOptions ro = (RuleOptions) source;
		
		Iterator iter = ro.getAllOptions().iterator();
		while(iter.hasNext())
		{
			Option opt = (Option) iter.next();
			
			hsw.startNode("option");
			hsw.addAttribute("name", opt.getName());
			hsw.addAttribute("value", ro.getOptionValue(opt).getName());
			hsw.endNode();
			
		}
	}// marshal()
	
	public boolean canConvert(java.lang.Class type)
	{
		return type.equals(RuleOptions.class);
	}// canConvert()
	
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) 
	{
		final XMLSerializer xs = XMLSerializer.get(context);
		final RuleOptions ro = new RuleOptions();
		
		while(reader.hasMoreChildren())
		{
			reader.moveDown();
			assert( "option".equals(reader.getNodeName()) );
			
			final String name = xs.getString(reader.getAttribute("name"), "");
			final String value = xs.getString(reader.getAttribute("value"), "");
			
			final Option opt = Option.parse( name );
			final OptionValue optVal = OptionValue.parse( value );
			
			// skip unrecognized options or values, bug log them
			if(opt != null && optVal != null)
			{
				ro.setOption(opt, optVal);
			}
			else
			{
				Log.println("RuleOptionsConverter: unrecognized ruleoption: ");
				Log.println("   name: ", name);
				Log.println("   value: ", value);
			}
			
			reader.moveUp();
		}
		
		return ro;
	}// unmarshal()		
}// class RuleOptionsConverter
