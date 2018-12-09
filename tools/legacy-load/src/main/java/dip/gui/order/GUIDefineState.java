//
//  @(#)GUIDefineState.java	12/2002
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
package dip.gui.order;

import dip.order.DefineState;
import dip.world.Location;
import dip.world.Power;
import dip.world.Unit;

/**
 * GUIOrder subclass of DefineState order.
 * <p>
 * This is essentially a placeholder. It is incomplete, and should only
 * be used (derived from) existing DefineState orders. No locations may
 * be set via GUIOrder methods, and the order will not be valid if
 * created without derivation. This may change in future implementations.
 */
public class GUIDefineState extends DefineState implements GUIOrder {

    /**
     * Creates a GUIDefineState
     */
    protected GUIDefineState() {
        super();
    }// GUIDefineState()


    /**
     * Creates a GUIDefineState
     */
    protected GUIDefineState(Power power, Location source, Unit.Type sourceUnitType) {
        super(power, source, sourceUnitType);
    }// GUIDefineState()


}// class GUIDefineState
