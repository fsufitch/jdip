//  
//  @(#)Retreat.java	4/2002
//  
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;

import dip.world.Location;
import dip.world.Power;
import dip.world.Unit;

/**
 * Implementation of the Retreat order.
 * <p>
 * Note that a Retreat order is a Move order. Retreat orders are issued
 * instead of Move orders during the Retreat phase, by OrderParser.
 * <p>
 * Convoy options are not valid in Retreat orders.
 */

public class Retreat extends Move {
    // il8n constants
    private static final String RETREAT_SRC_EQ_DEST = "RETREAT_SRC_EQ_DEST";
    private static final String RETREAT_CANNOT = "RETREAT_CANNOT";
    private static final String RETREAT_FAIL_DPB = "RETREAT_FAIL_DPB";
    private static final String RETREAT_FAIL_MULTIPLE = "RETREAT_FAIL_MULTIPLE";
    private static final String orderNameFull = "Retreat";    // brief order name is still 'M'


    /**
     * Creates a Retreat order
     */
    protected Retreat(Power power, Location src, Unit.Type srcUnitType, Location dest) {
        super(power, src, srcUnitType, dest, false);
    }// Move()

    /**
     * Creates a Retreat order
     */
    protected Retreat() {
        super();
    }// Retreat()

    /**
     * Retreats are never convoyed; this will always return false.
     */
    public boolean isByConvoy() {
        return false;
    }// isByConvoy()


    public String getFullName() {
        return orderNameFull;
    }// getFullName()


    public boolean equals(Object obj) {
        if (obj instanceof Retreat) {
            Retreat retreat = (Retreat) obj;
            return super.equals(retreat)
                    && this.dest.equals(retreat.dest);
        }
        return false;
    }// equals()


}// class Retreat
