//
//  @(#)Order.java	12/2002
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

package dip.order;

import dip.world.Location;
import dip.world.Power;
import dip.world.Unit;

/**
 * All Order objects must implement this interface.
 * <p>
 * All classes that use Orders must use Orderable objects, rather than
 * Order objects, for compatibility with the OrderFactory system.
 * <p>
 * Please note that the Order class provides default implementations
 * for some of these methods, and a number of protected internal methods
 * for convenience.
 */
public interface Orderable {
    //
    // Basic Order Information
    //

    /**
     * Gets the Location of the ordered unit
     */
    Location getSource();

    /**
     * Gets the Type of the ordered unit
     */
    Unit.Type getSourceUnitType();

    /**
     * Gets the Power ordering the ordered Source unit
     */
    Power getPower();


    //
    // Order Name output
    //

    /**
     * Returns the Full name of the Order (e.g., "Hold" for a Hold order)
     */
    String getFullName();

    /**
     * Returns the Brief name of the Order (e.g., "H" for a Hold order)
     */
    String getBriefName();

    /**
     * Prints the entire order, in a brief syntax
     */
    String toBriefString();

    /**
     * Prints the entire order, in a verbose syntax
     */
    String toFullString();


}// interface Order

